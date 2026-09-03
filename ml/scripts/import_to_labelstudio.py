"""
Importa fotos a Label Studio en lotes pequeños (evita el error de upload masivo).

Requisitos:
  1. Label Studio corriendo en http://localhost:8080
  2. Token API: Label Studio -> icono usuario -> Account & Settings -> Access Token

Uso:
  python ml/scripts/import_to_labelstudio.py --token TU_TOKEN --project 1
  python ml/scripts/import_to_labelstudio.py --token TU_TOKEN --project 1 --batch 15
"""
from __future__ import annotations

import argparse
import json
import time
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parents[1]
IMAGES = ROOT / "labelstudio" / "images"
IMG_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def chunks(items: list[Path], size: int):
    for i in range(0, len(items), size):
        yield items[i : i + size]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://localhost:8080")
    parser.add_argument("--token", required=True, help="Access Token de Label Studio")
    parser.add_argument("--project", type=int, default=1)
    parser.add_argument("--images", type=Path, default=IMAGES)
    parser.add_argument("--batch", type=int, default=15, help="Fotos por lote")
    args = parser.parse_args()

    if not args.images.is_dir():
        raise SystemExit(f"No existe {args.images}. Ejecute prepare_labelstudio.py primero.")

    imgs = sorted(p for p in args.images.iterdir() if p.suffix.lower() in IMG_EXTS)
    if not imgs:
        raise SystemExit(f"Sin imagenes en {args.images}")

    headers = {"Authorization": f"Token {args.token}"}
    base = args.url.rstrip("/")
    import_url = f"{base}/api/projects/{args.project}/import"

    # Verificar proyecto
    r = requests.get(f"{base}/api/projects/{args.project}", headers=headers, timeout=30)
    r.raise_for_status()
    print(f"Proyecto: {r.json().get('title', args.project)}")
    print(f"Importando {len(imgs)} imagenes en lotes de {args.batch}...")

    ok = 0
    for n, batch in enumerate(chunks(imgs, args.batch), start=1):
        files = []
        for p in batch:
            files.append(("file", (p.name, p.read_bytes(), "image/jpeg")))
        try:
            resp = requests.post(
                import_url,
                headers=headers,
                files=files,
                params={"commit_to_project": "true"},
                timeout=120,
            )
            if resp.status_code >= 400:
                print(f"Lote {n} ERROR {resp.status_code}: {resp.text[:200]}")
                continue
            data = resp.json() if resp.text else {}
            count = data.get("task_count") or data.get("import") or len(batch)
            ok += len(batch)
            print(f"Lote {n}: {len(batch)} fotos OK (total ~{ok})")
        except Exception as e:
            print(f"Lote {n} fallo: {e}")
        time.sleep(0.5)

    print(f"\nListo. Revise en Label Studio -> proyecto -> deberia haber ~{ok} tareas.")
    print("Si faltan fotos, reduzca --batch a 5 e intente de nuevo.")


if __name__ == "__main__":
    main()
