"""
Servidor MCP opcional con search_lab_docs.
Requiere: pip install mcp
Uso: python -m app.mcp_server
La app Android usa HTTP (/chat); este proceso expone la misma recuperación vía MCP.
"""
from __future__ import annotations

import asyncio
import json
import sys

try:
    from mcp.server import Server
    from mcp.server.stdio import stdio_server
    from mcp.types import TextContent, Tool
except ImportError:
    print("Instale el paquete 'mcp' para usar el servidor MCP: pip install mcp", file=sys.stderr)
    raise SystemExit(1)

from .rag import get_rag

server = Server("lab-rumiologia-rag")


@server.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="search_lab_docs",
            description=(
                "Busca fragmentos relevantes en manuales, guías, protocolos y normas "
                "del Laboratorio de Rumiología. Devuelve solo snippets, no documentos completos."
            ),
            inputSchema={
                "type": "object",
                "properties": {
                    "query": {"type": "string"},
                    "equipment_class": {"type": "string"},
                    "top_k": {"type": "integer", "default": 4},
                },
                "required": ["query"],
            },
        )
    ]


@server.call_tool()
async def call_tool(name: str, arguments: dict):
    if name != "search_lab_docs":
        return [TextContent(type="text", text=f"Herramienta desconocida: {name}")]
    hits = get_rag().search_lab_docs(
        query=arguments.get("query", ""),
        equipment_class=arguments.get("equipment_class"),
        top_k=arguments.get("top_k"),
    )
    slim = [
        {"title": h["title"], "page": h["page"], "snippet": h["snippet"], "score": h.get("score")}
        for h in hits
    ]
    return [TextContent(type="text", text=json.dumps(slim, ensure_ascii=False, indent=2))]


async def main():
    async with stdio_server() as (read_stream, write_stream):
        await server.run(read_stream, write_stream, server.create_initialization_options())


if __name__ == "__main__":
    asyncio.run(main())
