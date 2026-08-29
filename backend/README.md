# Backend RAG + MCP — Laboratorio de Rumiología

## Requisitos

```bash
cd backend
python -m venv .venv
# Windows:
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
# Edite GEMINI_API_KEY en .env
```

## Indexar documentos

Coloque PDF/MD/TXT en `data/docs/` y ejecute:

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
# o reindexar:
curl -X POST http://127.0.0.1:8000/ingest
```

## API Android

- `GET /health`
- `POST /chat` body: `{ "question": "...", "equipment_class": "incubadora" }`
- `POST /tools/search_lab_docs` (herramienta de recuperación)

La app **no** envía documentos completos: solo pregunta + clase de equipo.

## Servidor MCP

```bash
python -m app.mcp_server
```

Herramienta: `search_lab_docs(query, equipment_class, top_k)`.

## Emulador Android

`BuildConfig.RAG_BASE_URL` apunta a `http://10.0.2.2:8000/` (localhost del host).
En teléfono físico, cambie la URL a la IP LAN de su PC en `app/build.gradle.kts`.
