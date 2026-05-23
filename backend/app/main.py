from datetime import datetime
from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from sqlalchemy import text
from app.db import fetch_all, fetch_one, execute_write, run_in_transaction

import bcrypt
app = FastAPI(title="LocalDiscgolf API")


# =========================
# Pydantic-modeller
# =========================

class RoundPlayerCreate(BaseModel):
    player_id: int
    layout_id: int


class CreateRoundRequest(BaseModel):
    created_by_username: str
    course_id: int
    started_at: datetime
    players: list[RoundPlayerCreate]


class HoleScoreUpdate(BaseModel):
    player_id: int
    throws_count: int | None = None


class UpdateHoleRequest(BaseModel):
    updated_by_username: str
    scores: list[HoleScoreUpdate]


class CompleteRoundRequest(BaseModel):
    completed_by_username: str
    ended_at: datetime

class LoginRequest(BaseModel):
    username: str
    password: str


class ChangePasswordRequest(BaseModel):
    username: str
    current_password: str
    new_password: str = Field(min_length=8)

# =========================
# Hjälpfunktioner
# =========================

def tx_fetch_one(conn, sql: str, params: dict | None = None) -> dict | None:
    result = conn.execute(text(sql), params or {})
    row = result.first()
    return dict(row._mapping) if row else None


def tx_fetch_all(conn, sql: str, params: dict | None = None) -> list[dict]:
    result = conn.execute(text(sql), params or {})
    return [dict(row._mapping) for row in result]


def tx_execute_write(conn, sql: str, params: dict | None = None) -> int:
    result = conn.execute(text(sql), params or {})
    try:
        return result.lastrowid or 0
    except Exception:
        return 0
        
def get_round_progress_summary(round_id: int, up_to_sequence_number: int) -> list[dict]:
    sql = """
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
    """
    return fetch_all(
        sql,
        {
            "round_id": round_id,
            "up_to_sequence_number": up_to_sequence_number,
        },
    )
    
def get_user_by_username(username: str) -> dict:
    user = fetch_one(
        """
        SELECT id, username, role, is_active
        FROM user_account
        WHERE username = :username
        """,
        {"username": username},
    )
    if not user:
        raise HTTPException(status_code=404, detail=f"User not found: {username}")
    if not user["is_active"]:
        raise HTTPException(status_code=400, detail=f"User is inactive: {username}")
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
    if layout["course_id"] != course_id:
        raise HTTPException(status_code=400, detail="Layout does not belong to course")
    if not layout["is_active"]:
        raise HTTPException(status_code=400, detail="Layout is inactive")
    return layout


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
            hv.length_meters,
            hv.par_value
        FROM layout_hole lh
        INNER JOIN hole h ON h.id = lh.hole_id
        LEFT JOIN hole_variant hv ON hv.id = lh.hole_variant_id
        LEFT JOIN hole_tee ht ON ht.id = hv.tee_id
        LEFT JOIN hole_basket hb ON hb.id = hv.basket_id
        WHERE lh.layout_id = :layout_id
        ORDER BY lh.sequence_number
        """,
        {"layout_id": layout_id},
    )
    if not holes:
        raise HTTPException(status_code=400, detail="Layout has no holes")
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


def determine_approval(source_user_id: int, player: dict) -> tuple[bool, str, int | None]:
    if player["is_guest"]:
        if player["created_by_user_id"] == source_user_id:
            return False, "approved", source_user_id
        raise HTTPException(
            status_code=403,
            detail=f"Guest player {player['name']} can only be used by the creator",
        )

    if player["owner_user_id"] == source_user_id:
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

def get_user_with_password(username: str) -> dict:
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
        {"username": username},
    )
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if not user["is_active"]:
        raise HTTPException(status_code=400, detail="User is inactive")
    return user


def verify_password(plain_password: str, password_hash: str) -> bool:
    return bcrypt.checkpw(
        plain_password.encode("utf-8"),
        password_hash.encode("utf-8"),
    )


def hash_password(plain_password: str) -> str:
    return bcrypt.hashpw(
        plain_password.encode("utf-8"),
        bcrypt.gensalt()
    ).decode("utf-8")

# =========================
# Befintliga GET-endpoints
# =========================

@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.get("/courses")
def get_courses() -> list[dict]:
    sql = """
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
    return fetch_all(sql)


@app.get("/courses/{course_id}")
def get_course_endpoint(course_id: int) -> dict:
    return get_course(course_id)


@app.get("/courses/{course_id}/layouts")
def get_course_layouts(course_id: int) -> list[dict]:
    course = fetch_one(
        """
        SELECT id, name, is_active
        FROM course
        WHERE id = :course_id
        """,
        {"course_id": course_id},
    )
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")

    sql = """
        SELECT
            l.id,
            l.course_id,
            c.name AS course_name,
            l.name,
            l.description,
            l.is_active,
            COUNT(lh.id) AS hole_count,
            COALESCE(SUM(hv.par_value), 0) AS total_par,
            COALESCE(SUM(hv.length_meters), 0) AS total_length_meters
        FROM layout l
        INNER JOIN course c ON c.id = l.course_id
        LEFT JOIN layout_hole lh ON lh.layout_id = l.id
        LEFT JOIN hole_variant hv ON hv.id = lh.hole_variant_id
        WHERE l.course_id = :course_id
          AND l.is_active = 1
        GROUP BY
            l.id,
            l.course_id,
            c.name,
            l.name,
            l.description,
            l.is_active
        ORDER BY l.name
    """
    return fetch_all(sql, {"course_id": course_id})


@app.get("/courses/{course_id}/holes")
def get_course_holes(course_id: int) -> list[dict]:
    sql = """
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
    """
    return fetch_all(sql, {"course_id": course_id})

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
    layout = fetch_one(
        """
        SELECT id
        FROM layout
        WHERE id = :layout_id
        """,
        {"layout_id": layout_id},
    )
    if not layout:
        raise HTTPException(status_code=404, detail="Layout not found")

    sql = """
        SELECT
            lh.sequence_number,
            h.id AS hole_id,
            h.hole_number,
            h.name AS hole_name,
            hv.id AS hole_variant_id,
            ht.name AS tee_name,
            hb.name AS basket_name,
            hv.length_meters,
            hv.par_value
        FROM layout_hole lh
        INNER JOIN hole h ON h.id = lh.hole_id
        LEFT JOIN hole_variant hv ON hv.id = lh.hole_variant_id
        LEFT JOIN hole_tee ht ON ht.id = hv.tee_id
        LEFT JOIN hole_basket hb ON hb.id = hv.basket_id
        WHERE lh.layout_id = :layout_id
        ORDER BY lh.sequence_number
    """
    return fetch_all(sql, {"layout_id": layout_id})    

@app.get("/players")
def get_players() -> list[dict]:
    sql = """
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
    return fetch_all(sql)


@app.get("/rounds")
def get_rounds() -> list[dict]:
    sql = """
        SELECT
            ps.id,
            c.name AS course_name,
            u.username AS created_by_username,
            ps.started_at,
            ps.ended_at,
            ps.status,
            (
                SELECT COUNT(*)
                FROM session_player sp
                WHERE sp.play_session_id = ps.id
            ) AS player_count
        FROM play_session ps
        INNER JOIN course c ON c.id = ps.course_id
        INNER JOIN user_account u ON u.id = ps.created_by_user_id
        ORDER BY ps.started_at DESC, ps.id DESC
    """
    return fetch_all(sql)


@app.get("/rounds/{round_id}")
def get_round_endpoint(round_id: int) -> dict:
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

    holes_by_session_player = {}
    for hole in holes:
        holes_by_session_player.setdefault(hole["session_player_id"], []).append(hole)

    for player in players:
        player["holes"] = holes_by_session_player.get(player["id"], [])

    round_row["players"] = players
    return round_row

@app.get("/rounds/{round_id}/current")
def get_current_round_state(round_id: int) -> dict:
    rnd = get_round(round_id)

    players = fetch_all(
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

    holes = fetch_all(
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

    if not holes:
        raise HTTPException(status_code=400, detail="Round has no holes")

    # Gruppindela per hålnummer i layoutsekvensen
    holes_by_sequence = {}
    for row in holes:
        holes_by_sequence.setdefault(row["sequence_number"], []).append(row)

    ordered_sequences = sorted(holes_by_sequence.keys())

    # Ett hål räknas som helt klart när alla spelare på det hålet har throws_count
    completed_sequences = []
    next_sequence_number = None

    for seq in ordered_sequences:
        seq_rows = holes_by_sequence[seq]
        if all(r["throws_count"] is not None for r in seq_rows):
            completed_sequences.append(seq)
        else:
            next_sequence_number = seq
            break

    if next_sequence_number is None:
        # Allt ifyllt
        next_sequence_number = ordered_sequences[-1]

    current_hole_rows = holes_by_sequence[next_sequence_number]
    current_hole_info = current_hole_rows[0]

    progress_summary = get_round_progress_summary(
        round_id=round_id,
        up_to_sequence_number=max(completed_sequences) if completed_sequences else 0,
    )

    return {
        "round": {
            "id": rnd["id"],
            "course_id": rnd["course_id"],
            "course_name": rnd["course_name"],
            "created_by_user_id": rnd["created_by_user_id"],
            "created_by_username": rnd["created_by_username"],
            "started_at": rnd["started_at"],
            "ended_at": rnd["ended_at"],
            "status": rnd["status"],
        },
        "progress": {
            "total_holes": len(ordered_sequences),
            "completed_holes": len(completed_sequences),
            "current_sequence_number": next_sequence_number,
            "is_finished_by_scores": len(completed_sequences) == len(ordered_sequences),
        },
        "players": players,
        "summary_to_previous_hole": progress_summary,
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

@app.get("/users/{username}/players")
def get_user_players(username: str) -> dict:
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

    own_player = fetch_one(
        """
        SELECT
            p.id,
            p.name,
            p.owner_user_id,
            p.created_by_user_id,
            p.is_guest,
            p.is_active
        FROM player p
        WHERE p.owner_user_id = :user_id
          AND p.is_active = 1
        """,
        {"user_id": user["id"]},
    )

    guest_players = fetch_all(
        """
        SELECT
            p.id,
            p.name,
            p.owner_user_id,
            p.created_by_user_id,
            p.is_guest,
            p.is_active
        FROM player p
        WHERE p.created_by_user_id = :user_id
          AND p.is_guest = 1
          AND p.is_active = 1
        ORDER BY p.name
        """,
        {"user_id": user["id"]},
    )

    scoreable_players = fetch_all(
        """
        SELECT
            p.id,
            p.name,
            p.owner_user_id,
            p.created_by_user_id,
            p.is_guest,
            p.is_active,
            upp.permission_level
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
    
    
# =========================
# Nya skriv-endpoints
# =========================

@app.post("/change-password")
def change_password(request: ChangePasswordRequest) -> dict:
    user = get_user_with_password(request.username)

    if not verify_password(request.current_password, user["password_hash"]):
        raise HTTPException(status_code=401, detail="Current password is incorrect")

    if request.current_password == request.new_password:
        raise HTTPException(status_code=400, detail="New password must be different from current password")

    new_password_hash = hash_password(request.new_password)

    execute_write(
        """
        UPDATE user_account
        SET
            password_hash = :password_hash,
            must_change_password = 0
        WHERE id = :user_id
        """,
        {
            "password_hash": new_password_hash,
            "user_id": user["id"],
        },
    )

    return {
        "message": "Password changed successfully",
        "username": user["username"],
        "must_change_password": False,
    }

@app.post("/login")
def login(request: LoginRequest) -> dict:
    user = get_user_with_password(request.username)

    if not verify_password(request.password, user["password_hash"]):
        raise HTTPException(status_code=401, detail="Invalid username or password")

    return {
        "user_id": user["id"],
        "username": user["username"],
        "role": user["role"],
        "must_change_password": bool(user["must_change_password"]),
    }

@app.post("/rounds")
def create_round(request: CreateRoundRequest) -> dict:
    created_by = get_user_by_username(request.created_by_username)
    get_course(request.course_id)

    if not request.players:
        raise HTTPException(status_code=400, detail="Round must contain at least one player")

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
                "created_by_user_id": created_by["id"],
                "started_at": request.started_at,
            },
        )

        seen_player_ids = set()

        for idx, round_player in enumerate(request.players, start=1):
            if round_player.player_id in seen_player_ids:
                raise HTTPException(status_code=400, detail="Duplicate player in round")
            seen_player_ids.add(round_player.player_id)

            player = get_player(round_player.player_id)
            layout = get_layout(round_player.layout_id, request.course_id)
            layout_holes = get_layout_holes(round_player.layout_id)

            approval_required, approval_state, approved_by_user_id = determine_approval(
                source_user_id=created_by["id"],
                player=player,
            )

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
                    "start_order": idx,
                    "added_by_user_id": created_by["id"],
                    "approval_required": approval_required,
                    "approval_state": approval_state,
                    "approved_by_user_id": approved_by_user_id,
                    "approved_at": request.started_at if approval_state == "approved" else None,
                },
            )

            for hole in layout_holes:
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

    play_session_id = run_in_transaction(_create_round_tx)
    return get_round_endpoint(int(play_session_id))


@app.patch("/rounds/{round_id}/holes/{sequence_number}")
def update_round_hole(round_id: int, sequence_number: int, request: UpdateHoleRequest) -> dict:
    rnd = get_round(round_id)

    if rnd["status"] != "in_progress":
        raise HTTPException(status_code=400, detail="Only in-progress rounds can be updated")

    updated_by = get_user_by_username(request.updated_by_username)

    if not request.scores:
        raise HTTPException(status_code=400, detail="No scores provided")

    for score_update in request.scores:
        session_player = get_session_player(round_id, score_update.player_id)
        if not session_player:
            raise HTTPException(
                status_code=404,
                detail=f"Player {score_update.player_id} is not part of round {round_id}",
            )

        player = get_player(score_update.player_id)

        # Samma behörighetsregel som vid skapande
        determine_approval(source_user_id=updated_by["id"], player=player)

        updated_rows = execute_write(
            """
            UPDATE session_player_hole
            SET
                throws_count = :throws_count,
                is_completed = :is_completed
            WHERE session_player_id = :session_player_id
              AND sequence_number = :sequence_number
            """,
            {
                "throws_count": score_update.throws_count,
                "is_completed": 1 if score_update.throws_count is not None else 0,
                "session_player_id": session_player["id"],
                "sequence_number": sequence_number,
            },
        )

    return get_round_endpoint(round_id)


@app.post("/rounds/{round_id}/complete")
def complete_round(round_id: int, request: CompleteRoundRequest) -> dict:
    rnd = get_round(round_id)

    if rnd["status"] != "in_progress":
        raise HTTPException(status_code=400, detail="Round is not in progress")

    completed_by = get_user_by_username(request.completed_by_username)

    # Enkelt första steg: samma användare som skapade rundan får avsluta den
    if completed_by["id"] != rnd["created_by_user_id"]:
        raise HTTPException(status_code=403, detail="Only the creator can complete the round")

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

    return get_round_endpoint(round_id)