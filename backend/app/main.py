from datetime import datetime
from collections import defaultdict

import bcrypt
from pydantic import BaseModel, Field
from fastapi import Depends, FastAPI, HTTPException
from sqlalchemy import text

from app.db import fetch_all, fetch_one, execute_write, run_in_transaction
from app.auth import create_access_token, get_current_user, require_admin


app = FastAPI(title="LocalDiscgolf API")


# =========================
# Pydantic-modeller
# =========================

class LoginRequest(BaseModel):
    username: str
    password: str


class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: int
    username: str
    role: str
    must_change_password: bool


class ChangePasswordRequest(BaseModel):
    # username behålls valfritt för kompatibilitet med den befintliga Android-klienten.
    # Backend använder alltid den autentiserade användaren.
    username: str | None = None
    current_password: str
    new_password: str = Field(min_length=8)


class RoundPlayerCreate(BaseModel):
    player_id: int = Field(gt=0)
    layout_id: int = Field(gt=0)


class CreateRoundRequest(BaseModel):
    course_id: int = Field(gt=0)
    started_at: datetime
    players: list[RoundPlayerCreate] = Field(min_length=1)


class HoleScoreUpdate(BaseModel):
    player_id: int = Field(gt=0)
    throws_count: int | None = Field(default=None, ge=1)


class UpdateHoleRequest(BaseModel):
    scores: list[HoleScoreUpdate] = Field(min_length=1)


class CompleteRoundRequest(BaseModel):
    ended_at: datetime


class CourseCreateRequest(BaseModel):
    name: str


class CourseUpdateRequest(BaseModel):
    name: str | None = None
    is_active: bool | None = None


class HoleCreateRequest(BaseModel):
    hole_number: int
    name: str | None = None
    length_meters: int
    par_value: int
    notes: str | None = None


class HoleUpdateRequest(BaseModel):
    hole_number: int | None = None
    name: str | None = None
    length_meters: int | None = None
    par_value: int | None = None
    notes: str | None = None
    is_active: bool | None = None


class HoleTeeCreateRequest(BaseModel):
    name: str


class HoleBasketCreateRequest(BaseModel):
    name: str


class HoleVariantCreateRequest(BaseModel):
    tee_id: int
    basket_id: int
    length_meters: int
    par_value: int


class LayoutHoleCreateRequest(BaseModel):
    hole_id: int
    hole_variant_id: int | None = None
    sequence_number: int | None = None


class LayoutCreateRequest(BaseModel):
    name: str
    description: str | None = None
    holes: list[LayoutHoleCreateRequest]


# =========================
# Databashjälp för transaktioner
# =========================

def tx_execute_write(conn, sql: str, params: dict | None = None) -> int:
    result = conn.execute(text(sql), params or {})
    return int(result.lastrowid or 0)


# =========================
# Hämtning och behörighet
# =========================

def get_user_with_password_by_id(user_id: int) -> dict:
    user = fetch_one(
        """
        SELECT
            id,
            username,
            password_hash,
            role,
            is_active,
            must_change_password
        FROM user_account
        WHERE id = :user_id
        """,
        {"user_id": user_id},
    )
    if not user or not user["is_active"]:
        raise HTTPException(status_code=401, detail="User is not active")
    return user


def get_player(player_id: int) -> dict:
    player = fetch_one(
        """
        SELECT id, name, owner_user_id, created_by_user_id, is_guest, is_active
        FROM player
        WHERE id = :player_id
        """,
        {"player_id": player_id},
    )
    if not player:
        raise HTTPException(status_code=404, detail=f"Player not found: {player_id}")
    if not player["is_active"]:
        raise HTTPException(status_code=400, detail=f"Player is inactive: {player_id}")
    return player


def get_course(course_id: int) -> dict:
    course = fetch_one(
        """
        SELECT id, name, is_active
        FROM course
        WHERE id = :course_id
        """,
        {"course_id": course_id},
    )
    if not course:
        raise HTTPException(status_code=404, detail=f"Course not found: {course_id}")
    if not course["is_active"]:
        raise HTTPException(status_code=400, detail=f"Course is inactive: {course_id}")
    return course


def get_layout(layout_id: int, course_id: int) -> dict:
    layout = fetch_one(
        """
        SELECT id, course_id, name, is_active
        FROM layout
        WHERE id = :layout_id
        """,
        {"layout_id": layout_id},
    )
    if not layout:
        raise HTTPException(status_code=404, detail=f"Layout not found: {layout_id}")
    if int(layout["course_id"]) != int(course_id):
        raise HTTPException(status_code=400, detail="Layout does not belong to course")
    if not layout["is_active"]:
        raise HTTPException(status_code=400, detail="Layout is inactive")
    return layout


def get_layout_holes_for_round(layout_id: int) -> list[dict]:
    holes = fetch_all(
        """
        SELECT
            lh.sequence_number,
            h.id AS hole_id,
            hv.id AS hole_variant_id,
            h.hole_number,
            h.name AS hole_name,
            ht.name AS tee_name,
            hb.name AS basket_name,
            hv.length_meters,
            hv.par_value
        FROM layout_hole lh
        INNER JOIN hole h ON h.id = lh.hole_id
        INNER JOIN hole_variant hv ON hv.id = lh.hole_variant_id
        LEFT JOIN hole_tee ht ON ht.id = hv.tee_id
        LEFT JOIN hole_basket hb ON hb.id = hv.basket_id
        WHERE lh.layout_id = :layout_id
          AND h.is_active = 1
          AND hv.is_active = 1
        ORDER BY lh.sequence_number
        """,
        {"layout_id": layout_id},
    )
    if not holes:
        raise HTTPException(status_code=400, detail="Layout has no active holes")
    return holes


def get_permission_level(source_user_id: int, target_player_id: int) -> str:
    row = fetch_one(
        """
        SELECT permission_level
        FROM user_player_permission
        WHERE source_user_id = :source_user_id
          AND target_player_id = :target_player_id
        """,
        {
            "source_user_id": source_user_id,
            "target_player_id": target_player_id,
        },
    )
    return row["permission_level"] if row else "none"


def can_access_player(current_user: dict, player: dict) -> bool:
    if current_user["role"] == "admin":
        return True

    current_user_id = int(current_user["id"])

    if bool(player["is_guest"]):
        return int(player["created_by_user_id"]) == current_user_id

    if player["owner_user_id"] is not None:
        if int(player["owner_user_id"]) == current_user_id:
            return True

    return get_permission_level(current_user_id, player["id"]) in (
        "auto_approve",
        "propose",
    )


def determine_approval(source_user_id: int, player: dict) -> tuple[bool, str, int | None]:
    source_user_id = int(source_user_id)
    owner_user_id = (
        int(player["owner_user_id"]) if player["owner_user_id"] is not None else None
    )
    created_by_user_id = (
        int(player["created_by_user_id"])
        if player["created_by_user_id"] is not None
        else None
    )

    if bool(player["is_guest"]):
        if created_by_user_id == source_user_id:
            return False, "approved", source_user_id
        raise HTTPException(
            status_code=403,
            detail=f"Guest player {player['name']} can only be used by the creator",
        )

    if owner_user_id == source_user_id:
        return False, "approved", source_user_id

    permission_level = get_permission_level(source_user_id, player["id"])

    if permission_level == "auto_approve":
        return False, "approved", source_user_id

    if permission_level == "propose":
        return True, "pending", None

    raise HTTPException(
        status_code=403,
        detail=f"User is not allowed to score for player {player['name']}",
    )


def get_round(round_id: int) -> dict:
    rnd = fetch_one(
        """
        SELECT
            ps.id,
            ps.course_id,
            c.name AS course_name,
            ps.created_by_user_id,
            u.username AS created_by_username,
            ps.started_at,
            ps.ended_at,
            ps.status
        FROM play_session ps
        INNER JOIN course c ON c.id = ps.course_id
        INNER JOIN user_account u ON u.id = ps.created_by_user_id
        WHERE ps.id = :round_id
        """,
        {"round_id": round_id},
    )
    if not rnd:
        raise HTTPException(status_code=404, detail="Round not found")
    return rnd


def require_round_editor(round_id: int, current_user: dict) -> dict:
    rnd = get_round(round_id)
    if (
        current_user["role"] != "admin"
        and int(current_user["id"]) != int(rnd["created_by_user_id"])
    ):
        raise HTTPException(
            status_code=403,
            detail="Only the scorer of the round can edit it",
        )
    return rnd


def require_round_viewer(round_id: int, current_user: dict) -> dict:
    rnd = get_round(round_id)

    if current_user["role"] == "admin":
        return rnd

    if int(current_user["id"]) == int(rnd["created_by_user_id"]):
        return rnd

    players = fetch_all(
        """
        SELECT
            p.id,
            p.name,
            p.owner_user_id,
            p.created_by_user_id,
            p.is_guest,
            p.is_active
        FROM session_player sp
        INNER JOIN player p ON p.id = sp.player_id
        WHERE sp.play_session_id = :round_id
        """,
        {"round_id": round_id},
    )

    if any(can_access_player(current_user, player) for player in players):
        return rnd

    raise HTTPException(status_code=403, detail="Not allowed to view this round")


def get_session_player(round_id: int, player_id: int) -> dict | None:
    return fetch_one(
        """
        SELECT
            sp.id,
            sp.play_session_id,
            sp.player_id,
            sp.layout_id,
            sp.display_name_snapshot,
            sp.start_order,
            sp.added_by_user_id,
            sp.approval_required,
            sp.approval_state,
            sp.approved_by_user_id,
            sp.approved_at
        FROM session_player sp
        WHERE sp.play_session_id = :round_id
          AND sp.player_id = :player_id
        """,
        {"round_id": round_id, "player_id": player_id},
    )

def require_round_owner_or_admin(current_user: dict, round_row: dict) -> None:
    if current_user["role"] == "admin":
        return

    if int(current_user["id"]) == int(round_row["created_by_user_id"]):
        return

    raise HTTPException(
        status_code=403,
        detail="Only the round creator or admin can do this",
    )

# =========================
# Round response-hjälpare
# =========================

def get_round_progress_summary(round_id: int, up_to_sequence_number: int) -> list[dict]:
    return fetch_all(
        """
        SELECT
            sp.id AS session_player_id,
            sp.player_id,
            p.name AS player_name,
            sp.start_order,
            COALESCE(SUM(CASE
                WHEN sph.sequence_number <= :up_to_sequence_number
                 AND sph.throws_count IS NOT NULL
                THEN sph.throws_count
                ELSE 0
            END), 0) AS total_throws,
            COALESCE(SUM(CASE
                WHEN sph.sequence_number <= :up_to_sequence_number
                 AND sph.throws_count IS NOT NULL
                THEN sph.throws_count - sph.par_snapshot
                ELSE 0
            END), 0) AS relative_to_par,
            COALESCE(SUM(CASE
                WHEN sph.sequence_number <= :up_to_sequence_number
                 AND sph.throws_count IS NOT NULL
                THEN 1
                ELSE 0
            END), 0) AS played_holes
        FROM session_player sp
        INNER JOIN player p ON p.id = sp.player_id
        LEFT JOIN session_player_hole sph ON sph.session_player_id = sp.id
        WHERE sp.play_session_id = :round_id
        GROUP BY sp.id, sp.player_id, p.name, sp.start_order
        ORDER BY sp.start_order
        """,
        {
            "round_id": round_id,
            "up_to_sequence_number": up_to_sequence_number,
        },
    )


def get_round_players_for_response(round_id: int) -> list[dict]:
    return fetch_all(
        """
        SELECT
            sp.id AS session_player_id,
            sp.player_id,
            p.name AS player_name,
            sp.layout_id,
            l.name AS layout_name,
            sp.start_order,
            sp.approval_required,
            sp.approval_state
        FROM session_player sp
        INNER JOIN player p ON p.id = sp.player_id
        LEFT JOIN layout l ON l.id = sp.layout_id
        WHERE sp.play_session_id = :round_id
        ORDER BY sp.start_order
        """,
        {"round_id": round_id},
    )


def get_round_hole_rows(round_id: int) -> list[dict]:
    return fetch_all(
        """
        SELECT
            sp.id AS session_player_id,
            sp.player_id,
            p.name AS player_name,
            sp.start_order,
            sph.sequence_number,
            sph.hole_id,
            sph.hole_variant_id,
            sph.hole_number_snapshot,
            sph.hole_name_snapshot,
            sph.tee_name_snapshot,
            sph.basket_name_snapshot,
            sph.length_snapshot_meters,
            sph.par_snapshot,
            sph.throws_count,
            sph.is_completed
        FROM session_player_hole sph
        INNER JOIN session_player sp ON sp.id = sph.session_player_id
        INNER JOIN player p ON p.id = sp.player_id
        WHERE sp.play_session_id = :round_id
        ORDER BY sph.sequence_number, sp.start_order
        """,
        {"round_id": round_id},
    )


def build_round_hole_response(round_id: int, sequence_number: int) -> dict:
    rnd = get_round(round_id)
    players = get_round_players_for_response(round_id)
    holes = get_round_hole_rows(round_id)

    if not holes:
        raise HTTPException(status_code=400, detail="Round has no holes")

    holes_by_sequence: dict[int, list[dict]] = defaultdict(list)
    for row in holes:
        holes_by_sequence[int(row["sequence_number"])].append(row)

    if sequence_number not in holes_by_sequence:
        raise HTTPException(status_code=404, detail="Hole not found in round")

    ordered_sequences = sorted(holes_by_sequence)
    completed_sequences = [
        sequence
        for sequence in ordered_sequences
        if all(row["throws_count"] is not None for row in holes_by_sequence[sequence])
    ]

    current_hole_rows = holes_by_sequence[sequence_number]
    current_hole_info = current_hole_rows[0]

    return {
        "round": rnd,
        "progress": {
            "total_holes": len(ordered_sequences),
            "completed_holes": len(completed_sequences),
            "current_sequence_number": sequence_number,
            "is_finished_by_scores": len(completed_sequences) == len(ordered_sequences),
        },
        "players": players,
        "summary_to_previous_hole": get_round_progress_summary(
            round_id=round_id,
            up_to_sequence_number=sequence_number - 1,
        ),
        "current_hole": {
            "sequence_number": current_hole_info["sequence_number"],
            "hole_id": current_hole_info["hole_id"],
            "hole_variant_id": current_hole_info["hole_variant_id"],
            "hole_number": current_hole_info["hole_number_snapshot"],
            "hole_name": current_hole_info["hole_name_snapshot"],
            "tee_name": current_hole_info["tee_name_snapshot"],
            "basket_name": current_hole_info["basket_name_snapshot"],
            "length_meters": current_hole_info["length_snapshot_meters"],
            "par_value": current_hole_info["par_snapshot"],
            "scores": [
                {
                    "session_player_id": row["session_player_id"],
                    "player_id": row["player_id"],
                    "player_name": row["player_name"],
                    "start_order": row["start_order"],
                    "throws_count": row["throws_count"],
                    "is_completed": bool(row["is_completed"]),
                }
                for row in current_hole_rows
            ],
        },
    }


def get_resume_sequence_number(round_id: int) -> int:
    holes = get_round_hole_rows(round_id)
    if not holes:
        raise HTTPException(status_code=400, detail="Round has no holes")

    holes_by_sequence: dict[int, list[dict]] = defaultdict(list)
    for row in holes:
        holes_by_sequence[int(row["sequence_number"])].append(row)

    for sequence_number in sorted(holes_by_sequence):
        rows = holes_by_sequence[sequence_number]
        if not all(row["throws_count"] is not None for row in rows):
            return sequence_number

    return max(holes_by_sequence)


def build_round_detail_response(round_id: int) -> dict:
    round_row = get_round(round_id)

    players = fetch_all(
        """
        SELECT
            sp.id,
            sp.play_session_id,
            sp.player_id,
            p.name AS player_name,
            sp.layout_id,
            l.name AS layout_name,
            sp.display_name_snapshot,
            sp.start_order,
            sp.added_by_user_id,
            au.username AS added_by_username,
            sp.approval_required,
            sp.approval_state,
            sp.approved_by_user_id,
            apu.username AS approved_by_username,
            sp.approved_at
        FROM session_player sp
        INNER JOIN player p ON p.id = sp.player_id
        LEFT JOIN layout l ON l.id = sp.layout_id
        INNER JOIN user_account au ON au.id = sp.added_by_user_id
        LEFT JOIN user_account apu ON apu.id = sp.approved_by_user_id
        WHERE sp.play_session_id = :round_id
        ORDER BY sp.start_order
        """,
        {"round_id": round_id},
    )

    holes = fetch_all(
        """
        SELECT
            sph.id,
            sph.session_player_id,
            sph.sequence_number,
            sph.course_id,
            sph.hole_id,
            sph.hole_variant_id,
            sph.hole_number_snapshot,
            sph.hole_name_snapshot,
            sph.tee_name_snapshot,
            sph.basket_name_snapshot,
            sph.length_snapshot_meters,
            sph.par_snapshot,
            sph.throws_count,
            sph.is_completed
        FROM session_player_hole sph
        INNER JOIN session_player sp ON sp.id = sph.session_player_id
        WHERE sp.play_session_id = :round_id
        ORDER BY sp.start_order, sph.sequence_number
        """,
        {"round_id": round_id},
    )

    holes_by_session_player: dict[int, list[dict]] = defaultdict(list)
    for hole in holes:
        holes_by_session_player[int(hole["session_player_id"])].append(hole)

    for player in players:
        player["holes"] = holes_by_session_player.get(int(player["id"]), [])

    round_row["players"] = players
    return round_row


# =========================
# Statistik-hjälpare
# =========================

def calculate_streak(results_newest_first: list[dict]) -> int:
    """
    Positiv svit: antal par eller bättre i rad från senaste spelade hålet.
    Negativ svit: antal bogey eller sämre i rad från senaste spelade hålet.
    """
    if not results_newest_first:
        return 0

    latest_relative = (
        int(results_newest_first[0]["throws_count"])
        - int(results_newest_first[0]["par_value"])
    )

    counts_non_positive = latest_relative <= 0
    streak = 0

    for result in results_newest_first:
        relative = int(result["throws_count"]) - int(result["par_value"])
        is_non_positive = relative <= 0
        if is_non_positive != counts_non_positive:
            break
        streak += 1

    return streak if counts_non_positive else -streak

# =========================
# Hål och varianter
# =========================

def get_hole(hole_id: int) -> dict:
    hole = fetch_one(
        """
        SELECT
            id,
            course_id,
            hole_number,
            name,
            length_meters,
            par_value,
            notes,
            is_active
        FROM hole
        WHERE id = :hole_id
        """,
        {"hole_id": hole_id},
    )

    if not hole:
        raise HTTPException(status_code=404, detail="Hole not found")

    return hole


def get_hole_tee(tee_id: int) -> dict:
    tee = fetch_one(
        """
        SELECT
            id,
            hole_id,
            name,
            sort_order,
            is_active
        FROM hole_tee
        WHERE id = :tee_id
        """,
        {"tee_id": tee_id},
    )

    if not tee:
        raise HTTPException(status_code=404, detail="Tee not found")

    return tee


def get_hole_basket(basket_id: int) -> dict:
    basket = fetch_one(
        """
        SELECT
            id,
            hole_id,
            name,
            sort_order,
            is_active
        FROM hole_basket
        WHERE id = :basket_id
        """,
        {"basket_id": basket_id},
    )

    if not basket:
        raise HTTPException(status_code=404, detail="Basket not found")

    return basket


def get_hole_variant(variant_id: int) -> dict:
    variant = fetch_one(
        """
        SELECT
            id,
            hole_id,
            tee_id,
            basket_id,
            length_meters,
            par_value,
            is_active
        FROM hole_variant
        WHERE id = :variant_id
        """,
        {"variant_id": variant_id},
    )

    if not variant:
        raise HTTPException(status_code=404, detail="Hole variant not found")

    return variant


def ensure_hole_belongs_to_course(hole: dict, course_id: int) -> None:
    if int(hole["course_id"]) != int(course_id):
        raise HTTPException(
            status_code=400,
            detail="Hole does not belong to course",
        )


def ensure_variant_belongs_to_hole(variant: dict, hole_id: int) -> None:
    if int(variant["hole_id"]) != int(hole_id):
        raise HTTPException(
            status_code=400,
            detail="Hole variant does not belong to hole",
        )

def get_single_active_variant_for_hole(hole_id: int) -> dict:
    variants = fetch_all(
        """
        SELECT
            id,
            hole_id,
            tee_id,
            basket_id,
            length_meters,
            par_value,
            is_active
        FROM hole_variant
        WHERE hole_id = :hole_id
          AND is_active = 1
        ORDER BY id
        """,
        {"hole_id": hole_id},
    )

    if not variants:
        raise HTTPException(
            status_code=400,
            detail=f"Hole {hole_id} has no active hole variants",
        )

    if len(variants) > 1:
        raise HTTPException(
            status_code=400,
            detail=(
                f"Hole {hole_id} has multiple active variants. "
                "hole_variant_id must be specified."
            ),
        )

    return variants[0]    

def get_layout_holes(layout_id: int) -> list[dict]:
    holes = fetch_all(
        """
        SELECT
            lh.sequence_number,
            h.id AS hole_id,
            hv.id AS hole_variant_id,
            h.hole_number,
            h.name AS hole_name,
            ht.name AS tee_name,
            hb.name AS basket_name,
            COALESCE(hv.length_meters, h.length_meters) AS length_meters,
            COALESCE(hv.par_value, h.par_value) AS par_value
        FROM layout_hole lh
        INNER JOIN hole h
            ON h.id = lh.hole_id
        LEFT JOIN hole_variant hv
            ON hv.id = lh.hole_variant_id
        LEFT JOIN hole_tee ht
            ON ht.id = hv.tee_id
        LEFT JOIN hole_basket hb
            ON hb.id = hv.basket_id
        WHERE lh.layout_id = :layout_id
        ORDER BY lh.sequence_number
        """,
        {"layout_id": layout_id},
    )

    if not holes:
        raise HTTPException(status_code=400, detail="Layout has no holes")

    return holes

# =========================
# Lösenord
# =========================

def verify_password(plain_password: str, password_hash: str) -> bool:
    return bcrypt.checkpw(
        plain_password.encode("utf-8"),
        password_hash.encode("utf-8"),
    )


def hash_password(plain_password: str) -> str:
    return bcrypt.hashpw(
        plain_password.encode("utf-8"),
        bcrypt.gensalt(),
    ).decode("utf-8")


# =========================
# Publika GET-endpoints
# =========================

@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.get("/courses")
def get_courses() -> list[dict]:
    return fetch_all(
        """
        SELECT
            c.id,
            c.name,
            c.is_active,
            (
                SELECT COUNT(*)
                FROM hole h
                WHERE h.course_id = c.id
                  AND h.is_active = 1
            ) AS hole_count,
            (
                SELECT COUNT(*)
                FROM layout l
                WHERE l.course_id = c.id
                  AND l.is_active = 1
            ) AS layout_count
        FROM course c
        WHERE c.is_active = 1
        ORDER BY c.name
        """
    )


@app.get("/courses/{course_id}")
def get_course_endpoint(course_id: int) -> dict:
    return get_course(course_id)


@app.get("/courses/{course_id}/layouts")
def get_course_layouts(course_id: int, include_inactive: bool = False) -> list[dict]:
    get_course(course_id)

    return fetch_all(
        """
        SELECT
            l.id,
            l.course_id,
            c.name AS course_name,
            l.name,
            l.description,
            l.is_active,
            COUNT(lh.id) AS hole_count,
            COALESCE(SUM(COALESCE(hv.par_value, h.par_value)), 0) AS total_par,
            COALESCE(SUM(COALESCE(hv.length_meters, h.length_meters)), 0) AS total_length_meters
        FROM layout l
        INNER JOIN course c
            ON c.id = l.course_id
        LEFT JOIN layout_hole lh
            ON lh.layout_id = l.id
        LEFT JOIN hole h
            ON h.id = lh.hole_id
        LEFT JOIN hole_variant hv
            ON hv.id = lh.hole_variant_id
        WHERE l.course_id = :course_id
        AND (:include_inactive = 1 OR l.is_active = 1)
        GROUP BY
            l.id,
            l.course_id,
            c.name,
            l.name,
            l.description,
            l.is_active
        ORDER BY
            l.is_active DESC,
            l.name
        """,
        {
            "course_id": course_id,
            "include_inactive": 1 if include_inactive else 0,
        },
    )


@app.get("/courses/{course_id}/holes")
def get_course_holes(course_id: int) -> list[dict]:
    get_course(course_id)
    return fetch_all(
        """
        SELECT
            h.id,
            h.course_id,
            h.hole_number,
            h.name,
            h.length_meters,
            h.par_value,
            h.notes,
            h.is_active
        FROM hole h
        WHERE h.course_id = :course_id
          AND h.is_active = 1
        ORDER BY h.hole_number
        """,
        {"course_id": course_id},
    )


@app.get("/layouts/{layout_id}")
def get_layout_endpoint(layout_id: int) -> dict:
    layout = fetch_one(
        """
        SELECT
            l.id,
            l.course_id,
            c.name AS course_name,
            l.name,
            l.description,
            l.is_active
        FROM layout l
        INNER JOIN course c ON c.id = l.course_id
        WHERE l.id = :layout_id
        """,
        {"layout_id": layout_id},
    )
    if not layout:
        raise HTTPException(status_code=404, detail="Layout not found")
    return layout


@app.get("/layouts/{layout_id}/holes")
def get_layout_holes_endpoint(layout_id: int) -> list[dict]:
    if not fetch_one("SELECT id FROM layout WHERE id = :layout_id", {"layout_id": layout_id}):
        raise HTTPException(status_code=404, detail="Layout not found")

    return fetch_all(
        """
        SELECT
            lh.sequence_number,
            h.id AS hole_id,
            h.hole_number,
            h.name AS hole_name,
            hv.id AS hole_variant_id,
            ht.name AS tee_name,
            hb.name AS basket_name,
            COALESCE(hv.length_meters, h.length_meters) AS length_meters,
            COALESCE(hv.par_value, h.par_value) AS par_value
        FROM layout_hole lh
        INNER JOIN hole h
            ON h.id = lh.hole_id
        LEFT JOIN hole_variant hv
            ON hv.id = lh.hole_variant_id
        LEFT JOIN hole_tee ht
            ON ht.id = hv.tee_id
        LEFT JOIN hole_basket hb
            ON hb.id = hv.basket_id
        WHERE lh.layout_id = :layout_id
        ORDER BY lh.sequence_number
        """,
        {"layout_id": layout_id},
    )


# =========================
# Autentisering och användare
# =========================

@app.post("/login", response_model=LoginResponse)
def login(request: LoginRequest) -> LoginResponse:
    user = fetch_one(
        """
        SELECT
            id,
            username,
            password_hash,
            role,
            is_active,
            must_change_password
        FROM user_account
        WHERE username = :username
        """,
        {"username": request.username},
    )

    if (
        not user
        or not user["is_active"]
        or not verify_password(request.password, user["password_hash"])
    ):
        raise HTTPException(status_code=401, detail="Invalid username or password")

    token = create_access_token(
        user_id=user["id"],
        username=user["username"],
        role=user["role"],
    )

    return LoginResponse(
        access_token=token,
        user_id=user["id"],
        username=user["username"],
        role=user["role"],
        must_change_password=bool(user["must_change_password"]),
    )


@app.get("/me")
def get_me(current_user: dict = Depends(get_current_user)) -> dict:
    return current_user


@app.post("/change-password")
def change_password(
    request: ChangePasswordRequest,
    current_user: dict = Depends(get_current_user),
) -> dict:
    if request.username and request.username != current_user["username"]:
        raise HTTPException(status_code=403, detail="Cannot change another user's password")

    user = get_user_with_password_by_id(current_user["id"])

    if not verify_password(request.current_password, user["password_hash"]):
        raise HTTPException(status_code=401, detail="Current password is incorrect")

    if request.current_password == request.new_password:
        raise HTTPException(
            status_code=400,
            detail="New password must be different from current password",
        )

    execute_write(
        """
        UPDATE user_account
        SET
            password_hash = :password_hash,
            must_change_password = 0
        WHERE id = :user_id
        """,
        {
            "password_hash": hash_password(request.new_password),
            "user_id": user["id"],
        },
    )

    return {
        "message": "Password changed successfully",
        "username": user["username"],
        "must_change_password": False,
    }


@app.get("/me/in-progress-rounds")
def get_my_in_progress_rounds(
    current_user: dict = Depends(get_current_user),
) -> list[dict]:
    return fetch_all(
        """
        SELECT
            ps.id,
            c.name AS course_name,
            ps.started_at,
            ps.status,
            (
                SELECT GROUP_CONCAT(DISTINCT l.name ORDER BY l.name SEPARATOR ', ')
                FROM session_player sp2
                INNER JOIN layout l ON l.id = sp2.layout_id
                WHERE sp2.play_session_id = ps.id
            ) AS layout_name,
            (
                SELECT COUNT(*)
                FROM session_player sp3
                WHERE sp3.play_session_id = ps.id
            ) AS player_count
        FROM play_session ps
        INNER JOIN course c ON c.id = ps.course_id
        WHERE ps.created_by_user_id = :user_id
          AND ps.status = 'in_progress'
        ORDER BY ps.started_at DESC, ps.id DESC
        """,
        {"user_id": current_user["id"]},
    )


@app.get("/users/{username}/players")
def get_user_players(
    username: str,
    current_user: dict = Depends(get_current_user),
) -> dict:
    if current_user["username"] != username and current_user["role"] != "admin":
        raise HTTPException(status_code=403, detail="Not allowed to view this user's players")

    user = fetch_one(
        """
        SELECT id, username, role, is_active
        FROM user_account
        WHERE username = :username
        """,
        {"username": username},
    )
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    round_count_sql = """
        (
            SELECT COUNT(*)
            FROM session_player sp
            INNER JOIN play_session ps ON ps.id = sp.play_session_id
            WHERE sp.player_id = p.id
              AND ps.status = 'completed'
              AND sp.approval_state = 'approved'
        ) AS round_count
    """

    own_player = fetch_one(
        f"""
        SELECT
            p.id,
            p.name,
            p.owner_user_id,
            p.created_by_user_id,
            p.is_guest,
            p.is_active,
            {round_count_sql}
        FROM player p
        WHERE p.owner_user_id = :user_id
          AND p.is_active = 1
        """,
        {"user_id": user["id"]},
    )

    guest_players = fetch_all(
        f"""
        SELECT
            p.id,
            p.name,
            p.owner_user_id,
            p.created_by_user_id,
            p.is_guest,
            p.is_active,
            {round_count_sql}
        FROM player p
        WHERE p.created_by_user_id = :user_id
          AND p.is_guest = 1
          AND p.is_active = 1
        ORDER BY p.name
        """,
        {"user_id": user["id"]},
    )

    scoreable_players = fetch_all(
        f"""
        SELECT
            p.id,
            p.name,
            p.owner_user_id,
            p.created_by_user_id,
            p.is_guest,
            p.is_active,
            upp.permission_level,
            {round_count_sql}
        FROM user_player_permission upp
        INNER JOIN player p ON p.id = upp.target_player_id
        WHERE upp.source_user_id = :user_id
          AND p.is_active = 1
          AND upp.permission_level <> 'none'
        ORDER BY p.name
        """,
        {"user_id": user["id"]},
    )

    return {
        "user": user,
        "own_player": own_player,
        "guest_players": guest_players,
        "scoreable_players": scoreable_players,
    }


@app.get("/players")
def get_players(current_user: dict = Depends(get_current_user)) -> list[dict]:
    if current_user["role"] != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")

    return fetch_all(
        """
        SELECT
            p.id,
            p.name,
            p.owner_user_id,
            p.created_by_user_id,
            p.is_guest,
            p.is_active
        FROM player p
        WHERE p.is_active = 1
        ORDER BY p.name
        """
    )


# =========================
# Rundor
# =========================

@app.post("/rounds")
def create_round(
    request: CreateRoundRequest,
    current_user: dict = Depends(get_current_user),
) -> dict:
    get_course(request.course_id)

    player_ids = [player.player_id for player in request.players]
    if len(set(player_ids)) != len(player_ids):
        raise HTTPException(status_code=400, detail="Duplicate player in round")

    layout_ids = {player.layout_id for player in request.players}
    if len(layout_ids) > 1:
        raise HTTPException(
            status_code=400,
            detail="Different layouts per player are not yet supported in round display",
        )

    prepared_players: list[dict] = []

    # All validering görs före första INSERT, så att en misslyckad spelare
    # inte håller en påbörjad skrivtransaktion öppen.
    for start_order, round_player in enumerate(request.players, start=1):
        player = get_player(round_player.player_id)
        layout = get_layout(round_player.layout_id, request.course_id)
        layout_holes = get_layout_holes_for_round(round_player.layout_id)
        approval_required, approval_state, approved_by_user_id = determine_approval(
            current_user["id"],
            player,
        )

        prepared_players.append(
            {
                "player": player,
                "layout": layout,
                "layout_holes": layout_holes,
                "start_order": start_order,
                "approval_required": approval_required,
                "approval_state": approval_state,
                "approved_by_user_id": approved_by_user_id,
            }
        )

    def _create_round_tx(conn):
        play_session_id = tx_execute_write(
            conn,
            """
            INSERT INTO play_session (
                course_id, created_by_user_id, started_at, ended_at, status
            )
            VALUES (
                :course_id, :created_by_user_id, :started_at, NULL, 'in_progress'
            )
            """,
            {
                "course_id": request.course_id,
                "created_by_user_id": current_user["id"],
                "started_at": request.started_at,
            },
        )

        for prepared in prepared_players:
            player = prepared["player"]
            layout = prepared["layout"]

            session_player_id = tx_execute_write(
                conn,
                """
                INSERT INTO session_player (
                    play_session_id, player_id, layout_id, display_name_snapshot,
                    start_order, added_by_user_id, approval_required, approval_state,
                    approved_by_user_id, approved_at
                )
                VALUES (
                    :play_session_id, :player_id, :layout_id, :display_name_snapshot,
                    :start_order, :added_by_user_id, :approval_required, :approval_state,
                    :approved_by_user_id, :approved_at
                )
                """,
                {
                    "play_session_id": play_session_id,
                    "player_id": player["id"],
                    "layout_id": layout["id"],
                    "display_name_snapshot": player["name"],
                    "start_order": prepared["start_order"],
                    "added_by_user_id": current_user["id"],
                    "approval_required": prepared["approval_required"],
                    "approval_state": prepared["approval_state"],
                    "approved_by_user_id": prepared["approved_by_user_id"],
                    "approved_at": (
                        request.started_at
                        if prepared["approval_state"] == "approved"
                        else None
                    ),
                },
            )

            for hole in prepared["layout_holes"]:
                tx_execute_write(
                    conn,
                    """
                    INSERT INTO session_player_hole (
                        session_player_id, sequence_number, course_id, hole_id, hole_variant_id,
                        hole_number_snapshot, hole_name_snapshot, tee_name_snapshot, basket_name_snapshot,
                        length_snapshot_meters, par_snapshot, throws_count, is_completed
                    )
                    VALUES (
                        :session_player_id, :sequence_number, :course_id, :hole_id, :hole_variant_id,
                        :hole_number_snapshot, :hole_name_snapshot, :tee_name_snapshot, :basket_name_snapshot,
                        :length_snapshot_meters, :par_snapshot, NULL, 0
                    )
                    """,
                    {
                        "session_player_id": session_player_id,
                        "sequence_number": hole["sequence_number"],
                        "course_id": request.course_id,
                        "hole_id": hole["hole_id"],
                        "hole_variant_id": hole["hole_variant_id"],
                        "hole_number_snapshot": hole["hole_number"],
                        "hole_name_snapshot": hole["hole_name"],
                        "tee_name_snapshot": hole["tee_name"],
                        "basket_name_snapshot": hole["basket_name"],
                        "length_snapshot_meters": hole["length_meters"],
                        "par_snapshot": hole["par_value"],
                    },
                )

        return play_session_id

    play_session_id = int(run_in_transaction(_create_round_tx))
    return build_round_detail_response(play_session_id)


@app.get("/rounds/{round_id}")
def get_round_endpoint(
    round_id: int,
    current_user: dict = Depends(get_current_user),
) -> dict:
    require_round_viewer(round_id, current_user)
    return build_round_detail_response(round_id)


@app.get("/rounds/{round_id}/current")
def get_current_round_state(
    round_id: int,
    current_user: dict = Depends(get_current_user),
) -> dict:
    require_round_editor(round_id, current_user)
    sequence_number = get_resume_sequence_number(round_id)
    return build_round_hole_response(round_id, sequence_number)


@app.get("/rounds/{round_id}/holes/{sequence_number}")
def get_round_hole(
    round_id: int,
    sequence_number: int,
    current_user: dict = Depends(get_current_user),
) -> dict:
    require_round_editor(round_id, current_user)
    return build_round_hole_response(round_id, sequence_number)


@app.patch("/rounds/{round_id}/holes/{sequence_number}")
def update_round_hole(
    round_id: int,
    sequence_number: int,
    request: UpdateHoleRequest,
    current_user: dict = Depends(get_current_user),
) -> dict:
    rnd = require_round_editor(round_id, current_user)

    if rnd["status"] != "in_progress":
        raise HTTPException(status_code=400, detail="Only in-progress rounds can be updated")

    player_ids = [score.player_id for score in request.scores]
    if len(set(player_ids)) != len(player_ids):
        raise HTTPException(status_code=400, detail="Duplicate player in score update")

    prepared_updates: list[dict] = []
    for score_update in request.scores:
        session_player = get_session_player(round_id, score_update.player_id)
        if not session_player:
            raise HTTPException(
                status_code=404,
                detail=f"Player {score_update.player_id} is not part of round {round_id}",
            )

        hole = fetch_one(
            """
            SELECT id
            FROM session_player_hole
            WHERE session_player_id = :session_player_id
              AND sequence_number = :sequence_number
            """,
            {
                "session_player_id": session_player["id"],
                "sequence_number": sequence_number,
            },
        )
        if not hole:
            raise HTTPException(status_code=404, detail="Hole not found in round")

        prepared_updates.append(
            {
                "session_player_id": session_player["id"],
                "throws_count": score_update.throws_count,
            }
        )

    def _update_hole_tx(conn):
        for update in prepared_updates:
            tx_execute_write(
                conn,
                """
                UPDATE session_player_hole
                SET
                    throws_count = :throws_count,
                    is_completed = :is_completed
                WHERE session_player_id = :session_player_id
                  AND sequence_number = :sequence_number
                """,
                {
                    "throws_count": update["throws_count"],
                    "is_completed": 1 if update["throws_count"] is not None else 0,
                    "session_player_id": update["session_player_id"],
                    "sequence_number": sequence_number,
                },
            )

    run_in_transaction(_update_hole_tx)
    return build_round_detail_response(round_id)


@app.post("/rounds/{round_id}/complete")
def complete_round(
    round_id: int,
    request: CompleteRoundRequest,
    current_user: dict = Depends(get_current_user),
) -> dict:
    rnd = get_round(round_id)

    if rnd["status"] != "in_progress":
        raise HTTPException(status_code=400, detail="Round is not in progress")

    require_round_owner_or_admin(current_user, rnd)

    execute_write(
        """
        UPDATE play_session
        SET
            status = 'completed',
            ended_at = :ended_at
        WHERE id = :round_id
        """,
        {
            "round_id": round_id,
            "ended_at": request.ended_at,
        },
    )

    return get_round_endpoint(round_id, current_user)


# =========================
# Rundlistor och statistik
# =========================

@app.get("/players/{player_id}/rounds")
def get_player_rounds(
    player_id: int,
    current_user: dict = Depends(get_current_user),
) -> list[dict]:
    player = get_player(player_id)

    if not can_access_player(current_user, player):
        raise HTTPException(status_code=403, detail="Not allowed to view this player's rounds")

    return fetch_all(
        """
        SELECT
            ps.id,
            c.name AS course_name,
            l.name AS layout_name,
            ps.started_at,
            ps.ended_at,
            ps.status,
            sp.approval_required,
            sp.approval_state,
            SUM(CASE WHEN sph.throws_count IS NOT NULL THEN sph.throws_count ELSE 0 END) AS total_throws,
            SUM(CASE WHEN sph.throws_count IS NOT NULL THEN sph.par_snapshot ELSE 0 END) AS total_par,
            SUM(CASE WHEN sph.throws_count IS NOT NULL THEN 1 ELSE 0 END) AS played_holes,
            COUNT(*) AS layout_hole_count,
            (
                SELECT COUNT(*)
                FROM session_player sp2
                WHERE sp2.play_session_id = ps.id
            ) AS player_count
        FROM session_player sp
        INNER JOIN play_session ps ON ps.id = sp.play_session_id
        INNER JOIN course c ON c.id = ps.course_id
        LEFT JOIN layout l ON l.id = sp.layout_id
        LEFT JOIN session_player_hole sph ON sph.session_player_id = sp.id
        WHERE sp.player_id = :player_id
        GROUP BY
            ps.id,
            c.name,
            l.name,
            ps.started_at,
            ps.ended_at,
            ps.status,
            sp.approval_required,
            sp.approval_state
        ORDER BY ps.started_at DESC, ps.id DESC
        """,
        {"player_id": player_id},
    )


@app.get("/players/{player_id}/stats/holes")
def get_player_hole_stats(
    player_id: int,
    course_id: int | None = None,
    current_user: dict = Depends(get_current_user),
) -> list[dict]:
    player = get_player(player_id)

    if not can_access_player(current_user, player):
        raise HTTPException(
            status_code=403,
            detail="Not allowed to view this player's statistics",
        )

    rows = fetch_all(
        """
        SELECT
            ps.id AS round_id,
            ps.started_at,
            c.id AS course_id,
            c.name AS course_name,
            sph.hole_id,
            sph.hole_variant_id,
            sph.hole_number_snapshot AS hole_number,
            sph.hole_name_snapshot AS hole_name,
            sph.tee_name_snapshot AS tee_name,
            sph.basket_name_snapshot AS basket_name,
            sph.length_snapshot_meters AS length_meters,
            sph.par_snapshot AS par_value,
            sph.throws_count
        FROM session_player sp
        INNER JOIN play_session ps ON ps.id = sp.play_session_id
        INNER JOIN course c ON c.id = ps.course_id
        INNER JOIN session_player_hole sph ON sph.session_player_id = sp.id
        WHERE sp.player_id = :player_id
          AND ps.status = 'completed'
          AND sp.approval_state = 'approved'
          AND sph.throws_count IS NOT NULL
          AND (:course_id IS NULL OR ps.course_id = :course_id)
        ORDER BY ps.started_at DESC, ps.id DESC
        """,
        {"player_id": player_id, "course_id": course_id},
    )

    rows_by_hole: dict[tuple[int, int, int | None], list[dict]] = defaultdict(list)
    for row in rows:
        key = (
            int(row["course_id"]),
            int(row["hole_id"]),
            int(row["hole_variant_id"]) if row["hole_variant_id"] is not None else None,
        )
        rows_by_hole[key].append(row)

    result: list[dict] = []
    for hole_rows in rows_by_hole.values():
        newest_row = hole_rows[0]
        played_count = len(hole_rows)
        throws_values = [int(row["throws_count"]) for row in hole_rows]
        relative_values = [
            int(row["throws_count"]) - int(row["par_value"])
            for row in hole_rows
        ]

        result.append(
            {
                "course_id": int(newest_row["course_id"]),
                "course_name": newest_row["course_name"],
                "hole_id": int(newest_row["hole_id"]),
                "hole_variant_id": (
                    int(newest_row["hole_variant_id"])
                    if newest_row["hole_variant_id"] is not None
                    else None
                ),
                "hole_number": int(newest_row["hole_number"]),
                "hole_name": newest_row["hole_name"],
                "tee_name": newest_row["tee_name"],
                "basket_name": newest_row["basket_name"],
                "length_meters": int(newest_row["length_meters"]),
                "par_value": int(newest_row["par_value"]),
                "played_count": played_count,
                "personal_best_throws": min(throws_values),
                "streak": calculate_streak(hole_rows),
                "average_throws": round(sum(throws_values) / played_count, 2),
                "last_10_average_throws": (
                    round(sum(throws_values[:10]) / 10, 2)
                    if played_count >= 11
                    else None
                ),
                "birdie_or_better_count": sum(1 for value in relative_values if value <= -1),
                "par_count": sum(1 for value in relative_values if value == 0),
                "bogey_count": sum(1 for value in relative_values if value == 1),
                "double_bogey_count": sum(1 for value in relative_values if value == 2),
                "triple_bogey_or_worse_count": sum(1 for value in relative_values if value >= 3),
            }
        )

    return sorted(
        result,
        key=lambda row: (
            row["course_name"],
            row["hole_number"],
            row["tee_name"] or "",
            row["basket_name"] or "",
        ),
    )


@app.get("/players/{player_id}/stats/layouts")
def get_player_layout_stats(
    player_id: int,
    course_id: int | None = None,
    current_user: dict = Depends(get_current_user),
) -> list[dict]:
    player = get_player(player_id)

    if not can_access_player(current_user, player):
        raise HTTPException(
            status_code=403,
            detail="Not allowed to view this player's statistics",
        )

    rows = fetch_all(
        """
        SELECT
            ps.id AS round_id,
            ps.started_at,
            c.id AS course_id,
            c.name AS course_name,
            l.id AS layout_id,
            l.name AS layout_name,
            COUNT(sph.id) AS hole_count,
            SUM(sph.par_snapshot) AS total_par,
            SUM(sph.length_snapshot_meters) AS total_length_meters,
            SUM(sph.throws_count) AS total_throws,
            SUM(CASE WHEN sph.throws_count IS NOT NULL THEN 1 ELSE 0 END) AS played_holes
        FROM session_player sp
        INNER JOIN play_session ps ON ps.id = sp.play_session_id
        INNER JOIN course c ON c.id = ps.course_id
        INNER JOIN layout l ON l.id = sp.layout_id
        INNER JOIN session_player_hole sph ON sph.session_player_id = sp.id
        WHERE sp.player_id = :player_id
          AND ps.status = 'completed'
          AND sp.approval_state = 'approved'
          AND (:course_id IS NULL OR ps.course_id = :course_id)
        GROUP BY
            ps.id,
            ps.started_at,
            c.id,
            c.name,
            l.id,
            l.name
        HAVING COUNT(sph.id) = SUM(
            CASE WHEN sph.throws_count IS NOT NULL THEN 1 ELSE 0 END
        )
        ORDER BY ps.started_at DESC, ps.id DESC
        """,
        {"player_id": player_id, "course_id": course_id},
    )

    rows_by_layout: dict[int, list[dict]] = defaultdict(list)
    for row in rows:
        rows_by_layout[int(row["layout_id"])].append(row)

    result: list[dict] = []
    for layout_rows in rows_by_layout.values():
        newest_row = layout_rows[0]
        round_count = len(layout_rows)
        throws_values = [int(row["total_throws"]) for row in layout_rows]
        relative_values = [
            int(row["total_throws"]) - int(row["total_par"])
            for row in layout_rows
        ]
        best_index = min(
            range(round_count),
            key=lambda index: (throws_values[index], relative_values[index]),
        )

        result.append(
            {
                "course_id": int(newest_row["course_id"]),
                "course_name": newest_row["course_name"],
                "layout_id": int(newest_row["layout_id"]),
                "layout_name": newest_row["layout_name"],
                "total_par": int(newest_row["total_par"]),
                "hole_count": int(newest_row["hole_count"]),
                "total_length_meters": int(newest_row["total_length_meters"]),
                "round_count": round_count,
                "personal_best_throws": throws_values[best_index],
                "personal_best_relative_to_par": relative_values[best_index],
                "average_throws": round(sum(throws_values) / round_count, 2),
                "average_relative_to_par": round(sum(relative_values) / round_count, 2),
                "last_10_average_throws": (
                    round(sum(throws_values[:10]) / 10, 2)
                    if round_count >= 11
                    else None
                ),
                "last_10_average_relative_to_par": (
                    round(sum(relative_values[:10]) / 10, 2)
                    if round_count >= 11
                    else None
                ),
            }
        )

    return sorted(result, key=lambda row: (row["course_name"], row["layout_name"]))

# =========================
# Skapa/uppdatera/radera banor
# =========================

@app.post("/courses")
def create_course(
    request: CourseCreateRequest,
    current_user: dict = Depends(require_admin),
) -> dict:
    course_id = execute_write(
        """
        INSERT INTO course (
            name,
            is_active,
            created_by_user_id,
            updated_by_user_id
        )
        VALUES (
            :name,
            1,
            :user_id,
            :user_id
        )
        """,
        {
            "name": request.name,
            "user_id": current_user["id"],
        },
    )

    return get_course_endpoint(int(course_id))


@app.patch("/courses/{course_id}")
def update_course(
    course_id: int,
    request: CourseUpdateRequest,
    current_user: dict = Depends(require_admin),
) -> dict:
    get_course(course_id)

    if request.name is None and request.is_active is None:
        raise HTTPException(status_code=400, detail="No changes provided")

    execute_write(
        """
        UPDATE course
        SET
            name = COALESCE(:name, name),
            is_active = COALESCE(:is_active, is_active),
            updated_by_user_id = :user_id
        WHERE id = :course_id
        """,
        {
            "course_id": course_id,
            "name": request.name,
            "is_active": (
                None
                if request.is_active is None
                else 1 if request.is_active else 0
            ),
            "user_id": current_user["id"],
        },
    )

    return get_course_endpoint(course_id)


@app.delete("/courses/{course_id}")
def delete_course(
    course_id: int,
    current_user: dict = Depends(require_admin),
) -> dict:
    course = fetch_one(
        """
        SELECT id, name
        FROM course
        WHERE id = :course_id
        """,
        {"course_id": course_id},
    )

    if not course:
        raise HTTPException(status_code=404, detail="Course not found")

    hole_count = fetch_one(
        """
        SELECT COUNT(*) AS count_value
        FROM hole
        WHERE course_id = :course_id
        """,
        {"course_id": course_id},
    )

    if hole_count and int(hole_count["count_value"]) > 0:
        raise HTTPException(
            status_code=400,
            detail="Course has holes and cannot be deleted",
        )

    execute_write(
        """
        DELETE FROM course
        WHERE id = :course_id
        """,
        {"course_id": course_id},
    )

    return {
        "message": "Course deleted",
        "id": course_id,
        "name": course["name"],
    }

# =========================
# Skapa/uppdatera/radera hål och varianter
# =========================

@app.post("/courses/{course_id}/holes")
def create_hole(
    course_id: int,
    request: HoleCreateRequest,
    current_user: dict = Depends(require_admin),
) -> dict:
    get_course(course_id)

    def _create_hole_tx(conn):
        hole_id = tx_execute_write(
            conn,
            """
            INSERT INTO hole (
                course_id,
                hole_number,
                name,
                length_meters,
                par_value,
                notes,
                is_active,
                created_by_user_id,
                updated_by_user_id
            )
            VALUES (
                :course_id,
                :hole_number,
                :name,
                :length_meters,
                :par_value,
                :notes,
                1,
                :user_id,
                :user_id
            )
            """,
            {
                "course_id": course_id,
                "hole_number": request.hole_number,
                "name": request.name,
                "length_meters": request.length_meters,
                "par_value": request.par_value,
                "notes": request.notes,
                "user_id": current_user["id"],
            },
        )

        tee_id = tx_execute_write(
            conn,
            """
            INSERT INTO hole_tee (
                hole_id,
                name,
                sort_order,
                is_active
            )
            VALUES (
                :hole_id,
                'Standard',
                1,
                1
            )
            """,
            {"hole_id": hole_id},
        )

        basket_id = tx_execute_write(
            conn,
            """
            INSERT INTO hole_basket (
                hole_id,
                name,
                sort_order,
                is_active
            )
            VALUES (
                :hole_id,
                'Standard',
                1,
                1
            )
            """,
            {"hole_id": hole_id},
        )

        tx_execute_write(
            conn,
            """
            INSERT INTO hole_variant (
                hole_id,
                tee_id,
                basket_id,
                length_meters,
                par_value,
                is_active
            )
            VALUES (
                :hole_id,
                :tee_id,
                :basket_id,
                :length_meters,
                :par_value,
                1
            )
            """,
            {
                "hole_id": hole_id,
                "tee_id": tee_id,
                "basket_id": basket_id,
                "length_meters": request.length_meters,
                "par_value": request.par_value,
            },
        )

        return hole_id

    hole_id = run_in_transaction(_create_hole_tx)
    return get_hole(int(hole_id))


@app.patch("/holes/{hole_id}")
def update_hole(
    hole_id: int,
    request: HoleUpdateRequest,
    current_user: dict = Depends(require_admin),
) -> dict:
    get_hole(hole_id)

    if (
        request.hole_number is None
        and request.name is None
        and request.length_meters is None
        and request.par_value is None
        and request.notes is None
        and request.is_active is None
    ):
        raise HTTPException(status_code=400, detail="No changes provided")

    execute_write(
        """
        UPDATE hole
        SET
            hole_number = COALESCE(:hole_number, hole_number),
            name = COALESCE(:name, name),
            length_meters = COALESCE(:length_meters, length_meters),
            par_value = COALESCE(:par_value, par_value),
            notes = COALESCE(:notes, notes),
            is_active = COALESCE(:is_active, is_active),
            updated_by_user_id = :user_id
        WHERE id = :hole_id
        """,
        {
            "hole_id": hole_id,
            "hole_number": request.hole_number,
            "name": request.name,
            "length_meters": request.length_meters,
            "par_value": request.par_value,
            "notes": request.notes,
            "is_active": (
                None
                if request.is_active is None
                else 1 if request.is_active else 0
            ),
            "user_id": current_user["id"],
        },
    )

    return get_hole(hole_id)

@app.get("/holes/{hole_id}/tees")
def get_hole_tees(hole_id: int) -> list[dict]:
    get_hole(hole_id)

    return fetch_all(
        """
        SELECT
            id,
            hole_id,
            name,
            sort_order,
            is_active
        FROM hole_tee
        WHERE hole_id = :hole_id
        ORDER BY sort_order, name
        """,
        {"hole_id": hole_id},
    )


@app.get("/holes/{hole_id}/baskets")
def get_hole_baskets(hole_id: int) -> list[dict]:
    get_hole(hole_id)

    return fetch_all(
        """
        SELECT
            id,
            hole_id,
            name,
            sort_order,
            is_active
        FROM hole_basket
        WHERE hole_id = :hole_id
        ORDER BY sort_order, name
        """,
        {"hole_id": hole_id},
    )


@app.get("/holes/{hole_id}/variants")
def get_hole_variants(hole_id: int) -> list[dict]:
    get_hole(hole_id)

    return fetch_all(
        """
        SELECT
            hv.id,
            hv.hole_id,
            hv.tee_id,
            ht.name AS tee_name,
            hv.basket_id,
            hb.name AS basket_name,
            hv.length_meters,
            hv.par_value,
            hv.is_active
        FROM hole_variant hv
        LEFT JOIN hole_tee ht
            ON ht.id = hv.tee_id
        LEFT JOIN hole_basket hb
            ON hb.id = hv.basket_id
        WHERE hv.hole_id = :hole_id
        ORDER BY
            ht.sort_order,
            hb.sort_order,
            hv.id
        """,
        {"hole_id": hole_id},
    )

@app.post("/holes/{hole_id}/tees")
def create_hole_tee(
    hole_id: int,
    request: HoleTeeCreateRequest,
    current_user: dict = Depends(require_admin),
) -> dict:
    get_hole(hole_id)

    max_sort = fetch_one(
        """
        SELECT COALESCE(MAX(sort_order), 0) AS max_sort_order
        FROM hole_tee
        WHERE hole_id = :hole_id
        """,
        {"hole_id": hole_id},
    )

    next_sort_order = int(max_sort["max_sort_order"]) + 1 if max_sort else 1

    tee_id = execute_write(
        """
        INSERT INTO hole_tee (
            hole_id,
            name,
            sort_order,
            is_active
        )
        VALUES (
            :hole_id,
            :name,
            :sort_order,
            1
        )
        """,
        {
            "hole_id": hole_id,
            "name": request.name,
            "sort_order": next_sort_order,
        },
    )

    return get_hole_tee(int(tee_id))


@app.post("/holes/{hole_id}/baskets")
def create_hole_basket(
    hole_id: int,
    request: HoleBasketCreateRequest,
    current_user: dict = Depends(require_admin),
) -> dict:
    get_hole(hole_id)

    max_sort = fetch_one(
        """
        SELECT COALESCE(MAX(sort_order), 0) AS max_sort_order
        FROM hole_basket
        WHERE hole_id = :hole_id
        """,
        {"hole_id": hole_id},
    )

    next_sort_order = int(max_sort["max_sort_order"]) + 1 if max_sort else 1

    basket_id = execute_write(
        """
        INSERT INTO hole_basket (
            hole_id,
            name,
            sort_order,
            is_active
        )
        VALUES (
            :hole_id,
            :name,
            :sort_order,
            1
        )
        """,
        {
            "hole_id": hole_id,
            "name": request.name,
            "sort_order": next_sort_order,
        },
    )

    return get_hole_basket(int(basket_id))

@app.post("/holes/{hole_id}/variants")
def create_hole_variant(
    hole_id: int,
    request: HoleVariantCreateRequest,
    current_user: dict = Depends(require_admin),
) -> dict:
    get_hole(hole_id)

    tee = get_hole_tee(request.tee_id)
    basket = get_hole_basket(request.basket_id)

    if int(tee["hole_id"]) != int(hole_id):
        raise HTTPException(
            status_code=400,
            detail="Tee does not belong to hole",
        )

    if int(basket["hole_id"]) != int(hole_id):
        raise HTTPException(
            status_code=400,
            detail="Basket does not belong to hole",
        )

    variant_id = execute_write(
        """
        INSERT INTO hole_variant (
            hole_id,
            tee_id,
            basket_id,
            length_meters,
            par_value,
            is_active
        )
        VALUES (
            :hole_id,
            :tee_id,
            :basket_id,
            :length_meters,
            :par_value,
            1
        )
        """,
        {
            "hole_id": hole_id,
            "tee_id": request.tee_id,
            "basket_id": request.basket_id,
            "length_meters": request.length_meters,
            "par_value": request.par_value,
        },
    )

    return get_hole_variant(int(variant_id))

@app.post("/courses/{course_id}/layouts")
def create_layout(
    course_id: int,
    request: LayoutCreateRequest,
    current_user: dict = Depends(require_admin),
) -> dict:
    get_course(course_id)

    if not request.holes:
        raise HTTPException(
            status_code=400,
            detail="Layout must contain at least one hole",
        )

    seen_sequences: set[int] = set()
    prepared_holes: list[dict] = []

    for index, layout_hole in enumerate(request.holes, start=1):
        sequence_number = layout_hole.sequence_number or index

        if sequence_number in seen_sequences:
            raise HTTPException(
                status_code=400,
                detail=f"Duplicate sequence number: {sequence_number}",
            )

        seen_sequences.add(sequence_number)

        hole = get_hole(layout_hole.hole_id)
        ensure_hole_belongs_to_course(hole, course_id)

        if not bool(hole["is_active"]):
            raise HTTPException(
                status_code=400,
                detail=f"Hole {layout_hole.hole_id} is inactive",
            )

        if layout_hole.hole_variant_id is None:
            variant = get_single_active_variant_for_hole(layout_hole.hole_id)
            hole_variant_id = int(variant["id"])
        else:
            variant = get_hole_variant(layout_hole.hole_variant_id)
            ensure_variant_belongs_to_hole(variant, layout_hole.hole_id)

            if not bool(variant["is_active"]):
                raise HTTPException(
                    status_code=400,
                    detail=f"Hole variant {layout_hole.hole_variant_id} is inactive",
                )

            hole_variant_id = layout_hole.hole_variant_id

        prepared_holes.append(
            {
                "sequence_number": sequence_number,
                "hole_id": layout_hole.hole_id,
                "hole_variant_id": hole_variant_id,
            }
        )

    def _create_layout_tx(conn):
        layout_id = tx_execute_write(
            conn,
            """
            INSERT INTO layout (
                course_id,
                name,
                description,
                is_active
            )
            VALUES (
                :course_id,
                :name,
                :description,
                1
            )
            """,
            {
                "course_id": course_id,
                "name": request.name,
                "description": request.description,
            },
        )

        for prepared_hole in prepared_holes:
            tx_execute_write(
                conn,
                """
                INSERT INTO layout_hole (
                    layout_id,
                    sequence_number,
                    hole_id,
                    hole_variant_id
                )
                VALUES (
                    :layout_id,
                    :sequence_number,
                    :hole_id,
                    :hole_variant_id
                )
                """,
                {
                    "layout_id": layout_id,
                    "sequence_number": prepared_hole["sequence_number"],
                    "hole_id": prepared_hole["hole_id"],
                    "hole_variant_id": prepared_hole["hole_variant_id"],
                },
            )

        return layout_id

    layout_id = run_in_transaction(_create_layout_tx)
    return get_layout_endpoint(int(layout_id))