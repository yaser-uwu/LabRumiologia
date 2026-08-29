from __future__ import annotations

from typing import Any

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from .config import get_settings
from .rag import get_rag

app = FastAPI(title="Lab Rumiología RAG/MCP", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatRequest(BaseModel):
    question: str = Field(..., min_length=1)
    equipment_class: str | None = None


class ChatSource(BaseModel):
    title: str
    page: int | None = None
    snippet: str | None = None


class ChatResponse(BaseModel):
    answer: str
    sources: list[ChatSource]


class SearchRequest(BaseModel):
    query: str
    equipment_class: str | None = None
    top_k: int | None = None


@app.on_event("startup")
def startup():
    rag = get_rag()
    if rag._collection.count() == 0:
        rag.ingest()


@app.get("/health")
def health() -> dict[str, Any]:
    settings = get_settings()
    rag = get_rag()
    return {
        "status": "ok",
        "docs_indexed": rag._collection.count(),
        "gemini_configured": bool(
            settings.gemini_api_key
            and settings.gemini_api_key not in {"", "your_gemini_api_key_here"}
        ),
    }


@app.post("/ingest")
def ingest() -> dict[str, Any]:
    n = get_rag().ingest()
    return {"indexed_chunks": n}


@app.post("/chat", response_model=ChatResponse)
def chat(body: ChatRequest) -> ChatResponse:
    result = get_rag().chat(body.question, body.equipment_class)
    return ChatResponse(
        answer=result["answer"],
        sources=[ChatSource(**s) for s in result["sources"]],
    )


@app.post("/tools/search_lab_docs")
def search_lab_docs(body: SearchRequest) -> dict[str, Any]:
    """Herramienta MCP/HTTP: recupera fragmentos, nunca documentos completos."""
    hits = get_rag().search_lab_docs(body.query, body.equipment_class, body.top_k)
    # No devolver el texto completo al cliente Android por este endpoint de tool debug
    slim = [
        {"title": h["title"], "page": h["page"], "snippet": h["snippet"], "score": h.get("score")}
        for h in hits
    ]
    return {"results": slim}
