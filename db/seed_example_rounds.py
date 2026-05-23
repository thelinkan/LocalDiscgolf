#!/usr/bin/env python3
import argparse
import json
from pathlib import Path
from datetime import datetime

import mysql.connector

DATETIME_FMT = "%Y-%m-%d %H:%M"


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def get_conn(args):
    return mysql.connector.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset="utf8mb4",
        use_unicode=True,
        autocommit=False,
    )


def fetch_one(cur, sql, params):
    cur.execute(sql, params)
    return cur.fetchone()


def fetch_id(cur, sql, params):
    row = fetch_one(cur, sql, params)
    return row[0] if row else None


def parse_dt(value):
    if value is None:
        return None
    return datetime.strptime(value, DATETIME_FMT)


def get_course_id(cur, course_name):
    course_id = fetch_id(cur, "SELECT id FROM course WHERE name = %s", (course_name,))
    if course_id is None:
        raise ValueError(f"Course not found: {course_name}")
    return course_id


def get_layout_id(cur, course_id, layout_name):
    layout_id = fetch_id(
        cur,
        "SELECT id FROM layout WHERE course_id = %s AND name = %s",
        (course_id, layout_name),
    )
    if layout_id is None:
        raise ValueError(f"Layout not found: course_id={course_id}, layout={layout_name}")
    return layout_id


def get_player_id(cur, player_name):
    player_id = fetch_id(cur, "SELECT id FROM player WHERE name = %s", (player_name,))
    if player_id is None:
        raise ValueError(f"Player not found: {player_name}")
    return player_id


def get_user_id_by_username(cur, username):
    user_id = fetch_id(cur, "SELECT id FROM user_account WHERE username = %s", (username,))
    if user_id is None:
        raise ValueError(f"User not found: {username}")
    return user_id


def get_layout_holes(cur, layout_id):
    cur.execute(
        '''
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
        WHERE lh.layout_id = %s
        ORDER BY lh.sequence_number
        ''',
        (layout_id,),
    )
    rows = cur.fetchall()
    return [
        {
            "sequence_number": r[0],
            "hole_id": r[1],
            "hole_variant_id": r[2],
            "hole_number": r[3],
            "hole_name": r[4],
            "tee_name": r[5],
            "basket_name": r[6],
            "length_meters": r[7],
            "par_value": r[8],
        }
        for r in rows
    ]


def determine_permission(cur, source_user_id, target_player_id, target_owner_user_id, is_guest, created_by_user_id):
    if is_guest:
        if created_by_user_id == source_user_id:
            return False, "approved", source_user_id
        raise ValueError("Guest player can only be used by the user who created the guest player.")

    if target_owner_user_id == source_user_id:
        return False, "approved", source_user_id

    row = fetch_one(
        cur,
        '''
        SELECT permission_level
        FROM user_player_permission
        WHERE source_user_id = %s AND target_player_id = %s
        ''',
        (source_user_id, target_player_id),
    )
    permission_level = row[0] if row else "none"

    if permission_level == "auto_approve":
        return False, "approved", source_user_id
    if permission_level == "propose":
        return True, "pending", None

    raise ValueError(
        f"User {source_user_id} is not allowed to score for player {target_player_id}."
    )


def get_player_meta(cur, player_id):
    row = fetch_one(
        cur,
        '''
        SELECT name, owner_user_id, is_guest, created_by_user_id
        FROM player
        WHERE id = %s
        ''',
        (player_id,),
    )
    if row is None:
        raise ValueError(f"Player id not found: {player_id}")
    return {
        "name": row[0],
        "owner_user_id": row[1],
        "is_guest": bool(row[2]),
        "created_by_user_id": row[3],
    }


def insert_play_session(cur, course_id, created_by_user_id, started_at, ended_at, status):
    cur.execute(
        '''
        INSERT INTO play_session (
            course_id, created_by_user_id, started_at, ended_at, status
        )
        VALUES (%s, %s, %s, %s, %s)
        ''',
        (course_id, created_by_user_id, started_at, ended_at, status),
    )
    return cur.lastrowid


def insert_session_player(
    cur, play_session_id, player_id, layout_id, display_name_snapshot,
    start_order, added_by_user_id, approval_required, approval_state,
    approved_by_user_id, approved_at
):
    cur.execute(
        '''
        INSERT INTO session_player (
            play_session_id, player_id, layout_id, display_name_snapshot,
            start_order, added_by_user_id, approval_required, approval_state,
            approved_by_user_id, approved_at
        )
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ''',
        (
            play_session_id, player_id, layout_id, display_name_snapshot,
            start_order, added_by_user_id, approval_required, approval_state,
            approved_by_user_id, approved_at
        ),
    )
    return cur.lastrowid


def insert_session_player_hole(cur, session_player_id, course_id, layout_hole, score):
    is_completed = score is not None
    cur.execute(
        '''
        INSERT INTO session_player_hole (
            session_player_id, sequence_number, course_id, hole_id, hole_variant_id,
            hole_number_snapshot, hole_name_snapshot, tee_name_snapshot, basket_name_snapshot,
            length_snapshot_meters, par_snapshot, throws_count, is_completed
        )
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ''',
        (
            session_player_id,
            layout_hole["sequence_number"],
            course_id,
            layout_hole["hole_id"],
            layout_hole["hole_variant_id"],
            layout_hole["hole_number"],
            layout_hole["hole_name"],
            layout_hole["tee_name"],
            layout_hole["basket_name"],
            layout_hole["length_meters"],
            layout_hole["par_value"],
            score,
            is_completed,
        ),
    )


def import_rounds(conn, payload):
    cur = conn.cursor()
    try:
        for rnd in payload["rounds"]:
            course_id = get_course_id(cur, rnd["course_name"])
            layout_id = get_layout_id(cur, course_id, rnd["layout_name"])
            layout_holes = get_layout_holes(cur, layout_id)

            created_by_user_id = get_user_id_by_username(cur, rnd["created_by_username"])
            started_at = parse_dt(rnd["started_at"])
            ended_at = parse_dt(rnd.get("ended_at"))
            status = rnd["status"]

            play_session_id = insert_play_session(
                cur, course_id, created_by_user_id, started_at, ended_at, status
            )

            for idx, player_round in enumerate(rnd["players"], start=1):
                player_id = get_player_id(cur, player_round["player_name"])
                player_meta = get_player_meta(cur, player_id)

                approval_required, approval_state, approved_by_user_id = determine_permission(
                    cur=cur,
                    source_user_id=created_by_user_id,
                    target_player_id=player_id,
                    target_owner_user_id=player_meta["owner_user_id"],
                    is_guest=player_meta["is_guest"],
                    created_by_user_id=player_meta["created_by_user_id"],
                )

                approved_at = started_at if approval_state == "approved" else None

                scores = player_round["scores"]
                if len(scores) != len(layout_holes):
                    raise ValueError(
                        f"Score length mismatch for {player_round['player_name']} "
                        f"in round {rnd['course_name']} / {rnd['layout_name']}: "
                        f"{len(scores)} scores, expected {len(layout_holes)}"
                    )

                session_player_id = insert_session_player(
                    cur=cur,
                    play_session_id=play_session_id,
                    player_id=player_id,
                    layout_id=layout_id,
                    display_name_snapshot=player_meta["name"],
                    start_order=idx,
                    added_by_user_id=created_by_user_id,
                    approval_required=approval_required,
                    approval_state=approval_state,
                    approved_by_user_id=approved_by_user_id,
                    approved_at=approved_at,
                )

                for layout_hole, score in zip(layout_holes, scores):
                    insert_session_player_hole(
                        cur=cur,
                        session_player_id=session_player_id,
                        course_id=course_id,
                        layout_hole=layout_hole,
                        score=score,
                    )

        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        cur.close()


def main():
    parser = argparse.ArgumentParser(description="Seed example rounds into MariaDB.")
    load_dotenv()
    parser.add_argument("json_file", help="Path to seed JSON file")

    parser.add_argument("--host", default=os.getenv("DB_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("DB_PORT", "3306")))
    parser.add_argument("--user", default=os.getenv("DB_USER"))
    parser.add_argument("--password", default=os.getenv("DB_PASSWORD"))
    parser.add_argument("--database", default=os.getenv("DB_NAME"))
    args = parser.parse_args()

    payload = load_json(Path(args.json_file))
    conn = get_conn(args)
    try:
        import_rounds(conn, payload)
        print(f"Imported {len(payload['rounds'])} rounds successfully.")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
