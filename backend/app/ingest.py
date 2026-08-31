from __future__ import annotations

import re
from pathlib import Path

from pypdf import PdfReader


def _chunk_text(text: str, size: int = 800, overlap: int = 120) -> list[str]:
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return []
    chunks: list[str] = []
    start = 0
    while start < len(text):
        end = min(len(text), start + size)
        chunks.append(text[start:end])
        if end == len(text):
            break
        start = max(0, end - overlap)
    return chunks


def _read_file(path: Path) -> list[tuple[str, int | None]]:
    if path.suffix.lower() == ".pdf":
        reader = PdfReader(str(path))
        return [(page.extract_text() or "", i) for i, page in enumerate(reader.pages, start=1)]
    return [(path.read_text(encoding="utf-8", errors="ignore"), None)]
