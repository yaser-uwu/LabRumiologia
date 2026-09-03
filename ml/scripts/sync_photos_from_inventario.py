"""
Copia las fotos del laboratorio (carpetas MAQUINA 1..12) a ml/dataset/raw/<clase_yolo>/.

Uso:
  python ml/scripts/sync_photos_from_inventario.py
  python ml/scripts/sync_photos_from_inventario.py --src "C:\\Users\\...\\Modelos lab"
"""
from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "dataset"
INVENTARIO = DATASET / "inventario_equipos.json"
RAW = DATASET / "raw"
IMG_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp", ".heic"}


def load_inventario(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def unique_name(dest_dir: Path, src: Path, prefix: str) -> Path:
    base = f"{prefix}_{src.stem}{src.suffix.lower()}"
    out = dest_dir / base
    n = 1
    while out.exists():
        out = dest_dir / f"{prefix}_{src.stem}_{n}{src.suffix.lower()}"
        n += 1
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description="Copiar fotos del inventario a raw/<clase>/")
    parser.add_argument("--src", type=Path, default=None, help="Carpeta 'Modelos lab' (MAQUINA 1..12)")
    parser.add_argument("--inventario", type=Path, default=INVENTARIO)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    inv = load_inventario(args.inventario)
    src_root = args.src
    if src_root is None:
        for key in ("fuente_fotos", "fuente_fotos_alternativa"):
            candidate = Path(inv.get(key, ""))
            if candidate.exists():
                src_root = candidate
                break
        if src_root is None:
            src_root = Path(inv.get("fuente_fotos", ""))
    if not src_root or not src_root.exists():
        raise SystemExit(
            "No se encontró la carpeta de fotos.\n"
            f"  Esperada: {inv.get('fuente_fotos')}\n"
            "  Indique la ruta con: --src \"C:\\ruta\\Modelos lab\""
        )

    total = 0
    for eq in inv.get("equipos", []):
        clase = eq["clase_yolo"]
        carpeta = eq["carpeta_origen"]
        origen = src_root / carpeta
        destino = RAW / clase
        destino.mkdir(parents=True, exist_ok=True)

        if not origen.is_dir():
            print(f"Omitido (no existe): {origen}")
            continue

        count = 0
        for img in sorted(origen.rglob("*")):
            if img.suffix.lower() not in IMG_EXTS:
                continue
            dest = unique_name(destino, img, carpeta.replace(" ", "_"))
            if args.dry_run:
                print(f"[dry-run] {img} -> {dest}")
            else:
                shutil.copy2(img, dest)
            count += 1
        print(f"{clase}: +{count} desde {carpeta}")
        total += count

    if total == 0:
        raise SystemExit("No se copió ninguna imagen. Revise --src y el inventario.")
    print(f"\nListo: {total} fotos en {RAW}/<clase>/")
    print("Siguiente paso:")
    print("  python ml/scripts/auto_label_from_folders.py")
    print("  python ml/scripts/prepare_labelstudio.py")


if __name__ == "__main__":
    main()
