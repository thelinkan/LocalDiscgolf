#!/usr/bin/env python3
import argparse
import json
from pathlib import Path
import bcrypt

import mysql.connector


DEFAULT_PASSWORD = "changethis"

def hash_password(plain_password: str) -> str:
    return bcrypt.hashpw(
        plain_password.encode("utf-8"),
        bcrypt.gensalt()
    ).decode("utf-8")

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


def ensure_user(cur, username, email, role, password_hash):
    cur.execute(
        """
        INSERT INTO user_account (email, username, password_hash, role, is_active)
        VALUES (%s, %s, %s, %s, TRUE)
        ON DUPLICATE KEY UPDATE
            email = VALUES(email),
            password_hash = VALUES(password_hash),
            role = VALUES(role),
            is_active = TRUE
        """,
        (email, username, password_hash, role),
    )
    return fetch_id(cur, "SELECT id FROM user_account WHERE username = %s", (username,))


def ensure_player(cur, name, owner_user_id, created_by_user_id, is_guest):
    if owner_user_id is not None:
        cur.execute(
            """
            INSERT INTO player (name, owner_user_id, created_by_user_id, is_guest, is_active)
            VALUES (%s, %s, %s, %s, TRUE)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                created_by_user_id = VALUES(created_by_user_id),
                is_guest = VALUES(is_guest),
                is_active = TRUE
            """,
            (name, owner_user_id, created_by_user_id, is_guest),
        )
        return fetch_id(cur, "SELECT id FROM player WHERE owner_user_id = %s", (owner_user_id,))
    else:
        cur.execute(
            """
            INSERT INTO player (name, owner_user_id, created_by_user_id, is_guest, is_active)
            VALUES (%s, NULL, %s, %s, TRUE)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                created_by_user_id = VALUES(created_by_user_id),
                is_guest = VALUES(is_guest),
                is_active = TRUE
            """,
            (name, created_by_user_id, is_guest),
        )
        return fetch_id(
            cur,
            """
            SELECT id
            FROM player
            WHERE owner_user_id IS NULL
              AND name = %s
              AND COALESCE(created_by_user_id, -1) = COALESCE(%s, -1)
            ORDER BY id
            LIMIT 1
            """,
            (name, created_by_user_id),
        )


def main():
    password_hash = hash_password(args.default_password)
    parser = argparse.ArgumentParser(description="Seed users and players into MariaDB.")
    parser.add_argument("json_file", help="Path to seed JSON file")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--database", required=True)
    parser.add_argument("--default-password", default=DEFAULT_PASSWORD,
                    help="Plain password to use for all seeded users")
    args = parser.parse_args()

    payload = load_json(Path(args.json_file))
    conn = get_conn(args)
    cur = conn.cursor()

    try:
        user_ids = {}
        for user in payload["users"]:
            user_id = ensure_user(
                cur,
                username=user["username"],
                email=user["email"],
                role=user["role"],
                password_hash=password_hash,
            )
            user_ids[user["username"]] = user_id

        player_ids = {}
        for player in payload["players"]:
            owner_username = player.get("owner_username")
            created_by_username = player.get("created_by_username")

            owner_user_id = user_ids.get(owner_username) if owner_username else None
            created_by_user_id = user_ids.get(created_by_username) if created_by_username else None

            player_id = ensure_player(
                cur,
                name=player["name"],
                owner_user_id=owner_user_id,
                created_by_user_id=created_by_user_id,
                is_guest=bool(player.get("is_guest", False)),
            )
            player_ids[player["name"]] = player_id

        conn.commit()

        print("Seed complete.")
        print("Users:")
        for username, user_id in user_ids.items():
            print(f"  {username}: {user_id}")
        print("Players:")
        for name, player_id in player_ids.items():
            print(f"  {name}: {player_id}")
        print()
        print("Note: the default password hash in this script is a placeholder.")
        print("Replace it with a real bcrypt/argon2 hash for 'changethis' before real use.")
    except Exception:
        conn.rollback()
        raise
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()
