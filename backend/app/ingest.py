from __future__ import annotations

import hashlib
import re
from pathlib import Path

from pypdf import PdfReader


def _chunk_text(text: str, size: int = 800, overlap: int = 120) -> list[str]:
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return []
    chunks = []
    start = 0
    while start < len(text):
        end = min(len(text), start + size)
        chunks.append(text[start:end])
        if end == len(text):
            break
        start = max(0, end - overlap)
    return chunks


def _read_file(path: Path) -> list[tuple[str, int | None]]:
    """Returns list of (text, page)."""
    if path.suffix.lower() == ".pdf":
        reader = PdfReader(str(path))
        pages = []
        for i, page in enumerate(reader.pages, start=1):
            pages.append((page.extract_text() or "", i))
        return pages
    content = path.read_text(encoding="utf-8", errors="ignore")
    return [(content, None)]


def load_documents(docs_dir: Path) -> list[dict]:
    docs_dir.mkdir(parents=True, exist_ok=True)
    records: list[dict] = []
    patterns = ("*.md", "*.txt", "*.pdf")
    files: list[Path] = []
    for pat in patterns:
        files.extend(docs_dir.glob(pat))

    for path in sorted(files):
        for text, page in _read_file(path):
            for idx, chunk in enumerate(_chunk_text(text)):
                equipment = _guess_equipment(chunk)
                uid = hashlib.sha1(f"{path.name}|{page}|{idx}|{chunk[:40]}".encode()).hexdigest()
                records.append(
                    {
                        "id": uid,
                        "text": chunk,
                        "metadata": {
                            "title": path.name,
                            "page": page if page is not None else 0,
                            "equipment_class": equipment or "",
                            "source_path": str(path),
                        },
                    }
                )
    return records


def _guess_equipment(text: str) -> str | None:
    classes = [
        "incubadora",
        "agitador_orbital",
        "balanza_analitica",
        "phmetro",
        "centrifugadora",
        "estufa_secado",
        "banio_maria",
        "microscopio",
    ]
    lower = text.lower()
    for c in classes:
        token = c.replace("_", " ")
        if c in lower or token in lower:
            return c
    return None
