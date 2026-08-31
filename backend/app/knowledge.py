from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any

from .config import get_settings

DOC_EXTENSIONS = {".md", ".txt", ".pdf", ".docx"}


def catalog_path() -> Path:
    return get_settings().knowledge_path


def local_catalog_path() -> Path:
    p = catalog_path()
    return p.with_name("equipment_knowledge.local.json")


def _deep_merge(base: dict[str, Any], overlay: dict[str, Any]) -> dict[str, Any]:
    out = dict(base)
    for key, value in overlay.items():
        if key in out and isinstance(out[key], dict) and isinstance(value, dict):
            merged = dict(out[key])
            for inner_k, inner_v in value.items():
                if inner_k in merged and isinstance(merged[inner_k], dict) and isinstance(inner_v, dict):
                    merged[inner_k] = {**merged[inner_k], **inner_v}
                else:
                    merged[inner_k] = inner_v
            out[key] = merged
        else:
            out[key] = value
    return out


@lru_cache
def load_catalog() -> dict[str, Any]:
    path = catalog_path()
    if not path.exists():
        return {"equipments": {}, "aliases": {}, "shared_vector_store_ids": []}
    data = json.loads(path.read_text(encoding="utf-8"))
    local = local_catalog_path()
    if local.exists():
        overlay = json.loads(local.read_text(encoding="utf-8"))
        data = _deep_merge(data, overlay)
    return data


def reload_catalog() -> dict[str, Any]:
    load_catalog.cache_clear()
    return load_catalog()


def canonical_equipment_id(raw: str | None) -> str | None:
    if not raw:
        return None
    key = raw.strip().lower().replace(" ", "_")
    catalog = load_catalog()
    aliases: dict[str, str] = {str(k).lower().replace(" ", "_"): str(v) for k, v in (catalog.get("aliases") or {}).items()}
    if key in aliases:
        key = aliases[key]
    equipments = catalog.get("equipments") or {}
    if key in equipments:
        return key
    return key if key else None


def equipment_entry(equipment_class: str | None) -> dict[str, Any] | None:
    cid = canonical_equipment_id(equipment_class)
    if not cid:
        return None
    return (load_catalog().get("equipments") or {}).get(cid)


def resolve_vector_store_ids(equipment_class: str | None) -> list[str]:
    catalog = load_catalog()
    shared = [s for s in (catalog.get("shared_vector_store_ids") or []) if s]
    entry = equipment_entry(equipment_class)
    specific = [s for s in (entry or {}).get("vector_store_ids") or [] if s]
    # IDs del equipo primero: FileSearch prioriza esa base de conocimiento
    seen: list[str] = []
    for vid in specific + shared:
        if vid not in seen:
            seen.append(vid)
    return seen


def resolve_file_ids(equipment_class: str | None) -> list[str]:
    entry = equipment_entry(equipment_class)
    return [s for s in (entry or {}).get("file_ids") or [] if s]


def display_name(equipment_class: str | None) -> str:
    entry = equipment_entry(equipment_class)
    if entry and entry.get("name"):
        return str(entry["name"])
    return equipment_class or "no especificado"


def list_doc_files(docs_dir: Path, relative: str) -> list[Path]:
    folder = docs_dir / relative
    if not folder.is_dir():
        return []
    files: list[Path] = []
    for path in sorted(folder.rglob("*")):
        if path.is_file() and path.suffix.lower() in DOC_EXTENSIONS:
            files.append(path)
    return files
