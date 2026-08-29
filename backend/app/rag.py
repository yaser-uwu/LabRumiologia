from __future__ import annotations

from typing import Any

import chromadb
import google.generativeai as genai
from chromadb.api.models.Collection import Collection

from .config import Settings, get_settings
from .ingest import load_documents


class RagService:
    def __init__(self, settings: Settings | None = None):
        self.settings = settings or get_settings()
        self.settings.chroma_dir.mkdir(parents=True, exist_ok=True)
        self._client = chromadb.PersistentClient(path=str(self.settings.chroma_dir))
        self._collection: Collection = self._client.get_or_create_collection(
            name="lab_rumiologia",
            metadata={"hnsw:space": "cosine"},
        )
        self._use_gemini = bool(
            self.settings.gemini_api_key
            and self.settings.gemini_api_key not in {"", "your_gemini_api_key_here"}
        )
        if self._use_gemini:
            genai.configure(api_key=self.settings.gemini_api_key)

    def ingest(self) -> int:
        records = load_documents(self.settings.docs_dir)
        if not records:
            return 0
        # Reindex simple: drop and recreate
        self._client.delete_collection("lab_rumiologia")
        self._collection = self._client.get_or_create_collection(
            name="lab_rumiologia",
            metadata={"hnsw:space": "cosine"},
        )
        ids = [r["id"] for r in records]
        documents = [r["text"] for r in records]
        metadatas = [r["metadata"] for r in records]
        embeddings = [self._embed(t) for t in documents]
        self._collection.add(ids=ids, documents=documents, metadatas=metadatas, embeddings=embeddings)
        return len(records)

    def search_lab_docs(self, query: str, equipment_class: str | None = None, top_k: int | None = None) -> list[dict[str, Any]]:
        k = top_k or self.settings.top_k
        if not self._use_gemini:
            return self._keyword_search(query, equipment_class, k)

        where = None
        if equipment_class:
            where = {"equipment_class": equipment_class}
        try:
            result = self._collection.query(
                query_embeddings=[self._embed(query)],
                n_results=k,
                where=where,
                include=["documents", "metadatas", "distances"],
            )
        except Exception:
            result = self._collection.query(
                query_embeddings=[self._embed(query)],
                n_results=k,
                include=["documents", "metadatas", "distances"],
            )

        return self._format_hits(result)

    def _keyword_search(self, query: str, equipment_class: str | None, k: int) -> list[dict[str, Any]]:
        """Búsqueda léxica para modo demo sin embeddings de Gemini."""
        data = self._collection.get(include=["documents", "metadatas"])
        docs = data.get("documents") or []
        metas = data.get("metadatas") or []
        q = query.lower()
        tokens = [t for t in q.replace("?", " ").split() if len(t) > 2]
        scored: list[tuple[float, dict[str, Any]]] = []
        for doc, meta in zip(docs, metas):
            text = (doc or "").lower()
            meta = meta or {}
            if equipment_class and meta.get("equipment_class") and meta.get("equipment_class") != equipment_class:
                # sigue permitiendo coincidencia si el texto menciona el equipo
                if equipment_class.replace("_", " ") not in text and equipment_class not in text:
                    continue
            score = 0.0
            if equipment_class and (equipment_class in text or equipment_class.replace("_", " ") in text):
                score += 3.0
            for t in tokens:
                if t in text:
                    score += 1.0
            if score > 0:
                scored.append(
                    (
                        score,
                        {
                            "title": meta.get("title", "documento"),
                            "page": meta.get("page") or None,
                            "snippet": (doc or "")[:240],
                            "equipment_class": meta.get("equipment_class") or None,
                            "score": score,
                            "text": doc or "",
                        },
                    )
                )
        scored.sort(key=lambda x: x[0], reverse=True)
        return [h for _, h in scored[:k]]

    def _format_hits(self, result: dict[str, Any]) -> list[dict[str, Any]]:
        docs = result.get("documents", [[]])[0]
        metas = result.get("metadatas", [[]])[0]
        dists = result.get("distances", [[]])[0]
        out = []
        for doc, meta, dist in zip(docs, metas, dists):
            out.append(
                {
                    "title": meta.get("title", "documento"),
                    "page": meta.get("page") or None,
                    "snippet": doc[:240],
                    "equipment_class": meta.get("equipment_class") or None,
                    "score": float(1.0 - dist) if dist is not None else None,
                    "text": doc,
                }
            )
        return out

    def chat(self, question: str, equipment_class: str | None = None) -> dict[str, Any]:
        hits = self.search_lab_docs(question, equipment_class)
        if not hits:
            return {
                "answer": (
                    "No dispongo de información suficiente en los documentos del laboratorio "
                    "para responder esa consulta. Recomiendo consultar al docente o al "
                    "responsable del Laboratorio de Rumiología."
                ),
                "sources": [],
            }

        context = "\n\n".join(
            f"Fuente: {h['title']} (página {h['page']})\n{h['text']}" for h in hits
        )
        prompt = f"""Eres el asistente del Laboratorio de Rumiología de la UTEQ.
Responde SOLO con base en el contexto recuperado. Si no alcanza, di explícitamente que no
dispone de información suficiente y recomiende consultar al docente o responsable del laboratorio.
Cita las fuentes usadas (nombre de documento y página si existe).
Equipo detectado: {equipment_class or 'no especificado'}

Contexto:
{context}

Pregunta del estudiante:
{question}
"""
        answer = self._generate(prompt)
        sources = [{"title": h["title"], "page": h["page"] if h["page"] else None, "snippet": h["snippet"]} for h in hits]
        return {"answer": answer, "sources": sources}

    def _embed(self, text: str) -> list[float]:
        if not self._use_gemini:
            return self._fallback_embed(text)
        try:
            result = genai.embed_content(model=self.settings.embedding_model, content=text)
            return list(result["embedding"])
        except Exception:
            return self._fallback_embed(text)

    def _generate(self, prompt: str) -> str:
        if not self._use_gemini:
            return self._offline_answer(prompt)
        try:
            model = genai.GenerativeModel(self.settings.llm_model)
            response = model.generate_content(prompt)
            return (response.text or "").strip()
        except Exception:
            return self._offline_answer(prompt)

    def _offline_answer(self, prompt: str) -> str:
        # Extrae el contexto del prompt y responde con extractos (modo demo sin API).
        if "Contexto:" in prompt and "Pregunta del estudiante:" in prompt:
            ctx = prompt.split("Contexto:", 1)[1].split("Pregunta del estudiante:", 1)[0].strip()
            question = prompt.split("Pregunta del estudiante:", 1)[1].strip()
            if not ctx:
                return (
                    "No dispongo de información suficiente en los documentos del laboratorio "
                    "para responder esa consulta. Recomiendo consultar al docente o al "
                    "responsable del Laboratorio de Rumiología."
                )
            snippet = ctx[:900]
            return (
                f"(Modo demo sin GEMINI_API_KEY) Según los documentos recuperados para "
                f"«{question}»:\n\n{snippet}\n\n"
                "Configure GEMINI_API_KEY en backend/.env para respuestas generadas por el LLM. "
                "Si la información no aparece en los documentos, consulte al docente."
            )
        return (
            "Backend en modo demostración (sin GEMINI_API_KEY válida). "
            "Configure GEMINI_API_KEY en backend/.env."
        )

    @staticmethod
    def _fallback_embed(text: str, dim: int = 64) -> list[float]:
        vec = [0.0] * dim
        for i, ch in enumerate(text.encode("utf-8")):
            vec[i % dim] += (ch % 31) / 31.0
        norm = sum(v * v for v in vec) ** 0.5 or 1.0
        return [v / norm for v in vec]


_rag: RagService | None = None


def get_rag() -> RagService:
    global _rag
    if _rag is None:
        _rag = RagService()
    return _rag
