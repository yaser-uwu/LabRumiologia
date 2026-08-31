from __future__ import annotations

import os
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

BACKEND_ROOT = Path(__file__).resolve().parents[1]
load_dotenv(BACKEND_ROOT / ".env")
load_dotenv()


def _path_from_env(key: str, default: Path) -> Path:
    raw = os.getenv(key)
    if not raw:
        return default.resolve()
    path = Path(raw)
    if not path.is_absolute():
        path = BACKEND_ROOT / path
    return path.resolve()


class Settings:
    openai_api_key: str = os.getenv("OPENAI_API_KEY", "")
    openai_model: str = os.getenv("OPENAI_MODEL", "gpt-5.6")
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    host: str = os.getenv("HOST", "0.0.0.0")
    port: int = int(os.getenv("PORT", "8000"))
    docs_dir: Path = _path_from_env("DOCS_DIR", BACKEND_ROOT / "data" / "docs")
    knowledge_path: Path = _path_from_env(
        "KNOWLEDGE_PATH", BACKEND_ROOT / "data" / "equipment_knowledge.json"
    )
    llm_model: str = os.getenv("LLM_MODEL", "gemini-3.1-flash-lite")
    top_k: int = int(os.getenv("TOP_K", "4"))
    llm_provider: str = os.getenv("LLM_PROVIDER", "auto").strip().lower()

    @property
    def openai_configured(self) -> bool:
        key = (self.openai_api_key or "").strip()
        return bool(key) and key not in {"your_openai_api_key_here"}

    @property
    def gemini_configured(self) -> bool:
        key = (self.gemini_api_key or "").strip()
        return bool(key) and key not in {"your_gemini_api_key_here"}

    @property
    def active_provider(self) -> str:
        requested = self.llm_provider
        if requested in {"offline", "local", "docs"}:
            return "offline"
        if requested in {"openai", "openai_filesearch"}:
            return "openai" if self.openai_configured else "offline"
        if requested in {"gemini", "chroma"}:
            return "gemini" if self.gemini_configured else "offline"
        # auto: no gastar OpenAI; usar documentos locales. Gemini solo si hay clave.
        if self.gemini_configured:
            return "gemini"
        return "offline"


@lru_cache
def get_settings() -> Settings:
    return Settings()
