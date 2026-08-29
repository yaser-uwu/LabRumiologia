from __future__ import annotations

import os
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()


class Settings:
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    host: str = os.getenv("HOST", "0.0.0.0")
    port: int = int(os.getenv("PORT", "8000"))
    chroma_dir: Path = Path(os.getenv("CHROMA_DIR", "./data/chroma")).resolve()
    docs_dir: Path = Path(os.getenv("DOCS_DIR", "./data/docs")).resolve()
    embedding_model: str = os.getenv("EMBEDDING_MODEL", "models/text-embedding-004")
    llm_model: str = os.getenv("LLM_MODEL", "gemini-2.0-flash")
    top_k: int = int(os.getenv("TOP_K", "4"))


@lru_cache
def get_settings() -> Settings:
    return Settings()
