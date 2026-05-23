from fastapi import FastAPI, HTTPException
from app.db import fetch_all, fetch_one

app = FastAPI(title="LocalDiscgolf API")


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
                  AND h.is_active = TRUE
            ) AS hole_count,
            (
                SELECT COUNT(*)
                FROM layout l
                WHERE l.course_id = c.id
                  AND l.is_active = TRUE
            ) AS layout_count
        FROM course c
        WHERE c.is_active = TRUE
        ORDER BY c.name
    """
    return fetch_all(sql)


@app.get("/courses/{course_id}")
def get_course(course_id: int) -> dict:
    sql = """
        SELECT
            c.id,
            c.name,
            c.is_active
        FROM course c
        WHERE c.id = :course_id
    """
    course = fetch_one(sql, {"course_id": course_id})
    if not course:
        raise HTTPException(status_code=404, detail="Course not found")
    return course


@app.get("/courses/{course_id}/layouts")
def get_course_layouts(course_id: int) -> list[dict]:
    sql = """
        SELECT
            l.id,
            l.course_id,
            l.name,
            l.description,
            l.is_active,
            (
                SELECT COUNT(*)
                FROM layout_hole lh
                WHERE lh.layout_id = l.id
            ) AS hole_count
        FROM layout l
        WHERE l.course_id = :course_id
          AND l.is_active = TRUE
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
          AND h.is_active = TRUE
        ORDER BY h.hole_number
    """
    return fetch_all(sql, {"course_id": course_id})
    
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
def get_round(round_id: int) -> dict:
    round_sql = """
        SELECT
            ps.id,
            c.id AS course_id,
            c.name AS course_name,
            u.id AS created_by_user_id,
            u.username AS created_by_username,
            ps.started_at,
            ps.ended_at,
            ps.status
        FROM play_session ps
        INNER JOIN course c ON c.id = ps.course_id
        INNER JOIN user_account u ON u.id = ps.created_by_user_id
        WHERE ps.id = :round_id
    """
    round_row = fetch_one(round_sql, {"round_id": round_id})
    if not round_row:
        raise HTTPException(status_code=404, detail="Round not found")

    players_sql = """
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
    """
    players = fetch_all(players_sql, {"round_id": round_id})

    holes_sql = """
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
    """
    holes = fetch_all(holes_sql, {"round_id": round_id})

    holes_by_session_player = {}
    for hole in holes:
        holes_by_session_player.setdefault(hole["session_player_id"], []).append(hole)

    for player in players:
        player["holes"] = holes_by_session_player.get(player["id"], [])

    round_row["players"] = players
    return round_row