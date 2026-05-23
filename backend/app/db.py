from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine
from dotenv import load_dotenv
import os

load_dotenv()

DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")
DB_HOST = os.getenv("DB_HOST", "127.0.0.1")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_NAME = os.getenv("DB_NAME")

if not all([DB_USER, DB_PASSWORD, DB_NAME]):
    raise ValueError("DB_USER, DB_PASSWORD och DB_NAME måste finnas i .env")

DATABASE_URL = (
    f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"
)

engine: Engine = create_engine(
    DATABASE_URL,
    pool_pre_ping=True,
    future=True,
)


def fetch_all(sql: str, params: dict | None = None) -> list[dict]:
    with engine.connect() as conn:
        result = conn.execute(text(sql), params or {})
        return [dict(row._mapping) for row in result]


def fetch_one(sql: str, params: dict | None = None) -> dict | None:
    with engine.connect() as conn:
        result = conn.execute(text(sql), params or {})
        row = result.first()
        return dict(row._mapping) if row else None


def execute_write(sql: str, params: dict | None = None) -> int:
    with engine.begin() as conn:
        result = conn.execute(text(sql), params or {})
        try:
            return result.lastrowid or 0
        except Exception:
            return 0