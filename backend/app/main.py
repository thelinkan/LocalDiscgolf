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