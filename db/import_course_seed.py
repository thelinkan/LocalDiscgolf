#!/usr/bin/env python3
import argparse
import json
import os
from pathlib import Path

from dotenv import load_dotenv
import mysql.connector


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


def fetch_one_id(cur, sql, params):
    cur.execute(sql, params)
    row = cur.fetchone()
    return row[0] if row else None


def ensure_course(cur, name, created_by_user_id=None):
    cur.execute(
        '''
        INSERT INTO course (name, is_active, created_by_user_id, updated_by_user_id)
        VALUES (%s, TRUE, %s, %s)
        ON DUPLICATE KEY UPDATE
            is_active = TRUE,
            updated_by_user_id = VALUES(updated_by_user_id)
        ''',
        (name, created_by_user_id, created_by_user_id),
    )
    return fetch_one_id(cur, 'SELECT id FROM course WHERE name = %s', (name,))


def ensure_hole(cur, course_id, hole_number, name, length_meters, par_value, notes, updated_by_user_id=None):
    cur.execute(
        '''
        INSERT INTO hole (
            course_id, hole_number, name, length_meters, par_value, notes,
            is_active, created_by_user_id, updated_by_user_id
        )
        VALUES (%s, %s, %s, %s, %s, %s, TRUE, %s, %s)
        ON DUPLICATE KEY UPDATE
            name = VALUES(name),
            length_meters = VALUES(length_meters),
            par_value = VALUES(par_value),
            notes = VALUES(notes),
            is_active = TRUE,
            updated_by_user_id = VALUES(updated_by_user_id)
        ''',
        (
            course_id, hole_number, name, length_meters, par_value, notes,
            updated_by_user_id, updated_by_user_id
        ),
    )
    return fetch_one_id(
        cur,
        'SELECT id FROM hole WHERE course_id = %s AND hole_number = %s',
        (course_id, hole_number),
    )


def ensure_hole_tee(cur, hole_id, name, sort_order):
    cur.execute(
        '''
        INSERT INTO hole_tee (hole_id, name, sort_order, is_active)
        VALUES (%s, %s, %s, TRUE)
        ON DUPLICATE KEY UPDATE
            sort_order = VALUES(sort_order),
            is_active = TRUE
        ''',
        (hole_id, name, sort_order),
    )
    return fetch_one_id(
        cur,
        'SELECT id FROM hole_tee WHERE hole_id = %s AND name = %s',
        (hole_id, name),
    )


def ensure_hole_basket(cur, hole_id, name, sort_order):
    cur.execute(
        '''
        INSERT INTO hole_basket (hole_id, name, sort_order, is_active)
        VALUES (%s, %s, %s, TRUE)
        ON DUPLICATE KEY UPDATE
            sort_order = VALUES(sort_order),
            is_active = TRUE
        ''',
        (hole_id, name, sort_order),
    )
    return fetch_one_id(
        cur,
        'SELECT id FROM hole_basket WHERE hole_id = %s AND name = %s',
        (hole_id, name),
    )


def ensure_hole_variant(cur, hole_id, tee_id, basket_id, length_meters, par_value):
    cur.execute(
        '''
        INSERT INTO hole_variant (
            hole_id, tee_id, basket_id, length_meters, par_value, is_active
        )
        VALUES (%s, %s, %s, %s, %s, TRUE)
        ON DUPLICATE KEY UPDATE
            length_meters = VALUES(length_meters),
            par_value = VALUES(par_value),
            is_active = TRUE
        ''',
        (hole_id, tee_id, basket_id, length_meters, par_value),
    )
    return fetch_one_id(
        cur,
        '''
        SELECT id
        FROM hole_variant
        WHERE hole_id = %s AND tee_id = %s AND basket_id = %s
        ''',
        (hole_id, tee_id, basket_id),
    )


def ensure_layout(cur, course_id, name, description, updated_by_user_id=None):
    cur.execute(
        '''
        INSERT INTO layout (
            course_id, name, description, is_active, created_by_user_id, updated_by_user_id
        )
        VALUES (%s, %s, %s, TRUE, %s, %s)
        ON DUPLICATE KEY UPDATE
            description = VALUES(description),
            is_active = TRUE,
            updated_by_user_id = VALUES(updated_by_user_id)
        ''',
        (course_id, name, description, updated_by_user_id, updated_by_user_id),
    )
    return fetch_one_id(
        cur,
        'SELECT id FROM layout WHERE course_id = %s AND name = %s',
        (course_id, name),
    )


def replace_layout_holes(cur, layout_id, layout_holes):
    cur.execute('DELETE FROM layout_hole WHERE layout_id = %s', (layout_id,))
    for row in layout_holes:
        cur.execute(
            '''
            INSERT INTO layout_hole (layout_id, sequence_number, hole_id, hole_variant_id)
            VALUES (%s, %s, %s, %s)
            ''',
            (layout_id, row['sequence_number'], row['hole_id'], row['hole_variant_id']),
        )


def import_course(conn, payload: dict, created_by_user_id=None):
    course = payload['course']
    cur = conn.cursor()

    try:
        course_id = ensure_course(cur, course['name'], created_by_user_id)

        holes_by_number = {}
        for hole in course['holes']:
            default_variant = hole['variants'][0]
            hole_id = ensure_hole(
                cur=cur,
                course_id=course_id,
                hole_number=hole['hole_number'],
                name=hole.get('name'),
                length_meters=default_variant['length_meters'],
                par_value=default_variant['par_value'],
                notes=hole.get('notes'),
                updated_by_user_id=created_by_user_id,
            )

            tee_ids = {}
            for tee in hole.get('tees', []):
                tee_ids[tee['key']] = ensure_hole_tee(
                    cur=cur,
                    hole_id=hole_id,
                    name=tee['name'],
                    sort_order=tee.get('sort_order', 0),
                )

            basket_ids = {}
            for basket in hole.get('baskets', []):
                basket_ids[basket['key']] = ensure_hole_basket(
                    cur=cur,
                    hole_id=hole_id,
                    name=basket['name'],
                    sort_order=basket.get('sort_order', 0),
                )

            variant_ids = {}
            for variant in hole.get('variants', []):
                tee_id = tee_ids[variant['tee_key']]
                basket_id = basket_ids[variant['basket_key']]
                variant_ids[variant['key']] = ensure_hole_variant(
                    cur=cur,
                    hole_id=hole_id,
                    tee_id=tee_id,
                    basket_id=basket_id,
                    length_meters=variant['length_meters'],
                    par_value=variant['par_value'],
                )

            holes_by_number[hole['hole_number']] = {
                'hole_id': hole_id,
                'variant_ids': variant_ids,
            }

        for layout in course.get('layouts', []):
            layout_id = ensure_layout(
                cur=cur,
                course_id=course_id,
                name=layout['name'],
                description=layout.get('description'),
                updated_by_user_id=created_by_user_id,
            )

            resolved_layout_holes = []
            for item in layout['holes']:
                hole_info = holes_by_number[item['hole_number']]
                resolved_layout_holes.append(
                    {
                        'sequence_number': item['sequence_number'],
                        'hole_id': hole_info['hole_id'],
                        'hole_variant_id': hole_info['variant_ids'][item['variant_key']],
                    }
                )

            replace_layout_holes(cur, layout_id, resolved_layout_holes)

        conn.commit()
        return course_id
    except Exception:
        conn.rollback()
        raise
    finally:
        cur.close()


def main():
    parser = argparse.ArgumentParser(description='Import a course JSON into MariaDB.')
    load_dotenv()
    parser.add_argument("json_file", help="Path to seed JSON file")

    parser.add_argument("--host", default=os.getenv("DB_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("DB_PORT", "3306")))
    parser.add_argument("--user", default=os.getenv("DB_USER"))
    parser.add_argument("--password", default=os.getenv("DB_PASSWORD"))
    parser.add_argument("--database", default=os.getenv("DB_NAME"))
    parser.add_argument('--created-by-user-id', type=int, default=None)
    args = parser.parse_args()

    payload = load_json(Path(args.json_file))
    conn = get_conn(args)
    try:
        course_id = import_course(conn, payload, created_by_user_id=args.created_by_user_id)
        print(f'Imported/updated course successfully. course_id={course_id}')
    finally:
        conn.close()


if __name__ == '__main__':
    main()
