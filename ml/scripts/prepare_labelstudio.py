"""
Prepara un proyecto Label Studio: copia imágenes y genera tasks.json con pre-anotaciones YOLO.

Las cajas iniciales son aproximadas (centro). Debe corregirlas en Label Studio antes de exportar.

Uso:
  python ml/scripts/auto_label_from_folders.py
  python ml/scripts/prepare_labelstudio.py
  label-studio start
  # En Label Studio: Settings → Labeling Interface → pegar ml/labelstudio/config.xml
  # Import → tasks.json
"""
from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "dataset"
LS_DIR = ROOT / "labelstudio"
LS_IMAGES = LS_DIR / "images"
TASKS_JSON = LS_DIR / "tasks.json"
CONFIG_XML = LS_DIR / "config.xml"
DATA_YAML = DATASET / "data.yaml"
IMG_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def load_names(path: Path) -> list[str]:
    cfg = yaml.safe_load(path.read_text(encoding="utf-8"))
    names = cfg.get("names", [])
    if isinstance(names, dict):
        return [names[i] for i in sorted(names, key=lambda k: int(k))]
    return list(names)


def yolo_to_ls(cx: float, cy: float, w: float, h: float) -> dict:
    return {
        "x": (cx - w / 2) * 100,
        "y": (cy - h / 2) * 100,
        "width": w * 100,
        "height": h * 100,
        "rotation": 0,
    }


def read_yolo_boxes(lbl_path: Path, names: list[str]) -> list[dict]:
    if not lbl_path.exists():
        return []
    results = []
    for line in lbl_path.read_text(encoding="utf-8").splitlines():
        parts = line.strip().split()
        if len(parts) < 5:
            continue
        cid = int(float(parts[0]))
        if cid < 0 or cid >= len(names):
            continue
        cx, cy, w, h = map(float, parts[1:5])
        value = yolo_to_ls(cx, cy, w, h)
        value["rectanglelabels"] = [names[cid]]
        results.append(
            {
                "from_name": "label",
                "to_name": "image",
                "type": "rectanglelabels",
                "value": value,
            }
        )
    return results


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--images", type=Path, default=DATASET / "all" / "images")
    parser.add_argument("--labels", type=Path, default=DATASET / "all" / "labels")
    parser.add_argument("--max", type=int, default=0, help="Límite de imágenes (0 = todas)")
    args = parser.parse_args()

    if not args.images.is_dir():
        raise SystemExit(
            f"No hay imágenes en {args.images}.\n"
            "Ejecute antes:\n"
            "  python ml/scripts/sync_photos_from_inventario.py --src \"...\\Modelos lab\"\n"
            "  python ml/scripts/auto_label_from_folders.py"
        )

    names = load_names(DATA_YAML)
    if LS_IMAGES.exists():
        shutil.rmtree(LS_IMAGES)
    LS_IMAGES.mkdir(parents=True, exist_ok=True)

    tasks = []
    imgs = sorted(
        p for p in args.images.iterdir() if p.suffix.lower() in IMG_EXTS
    )
    if args.max > 0:
        imgs = imgs[: args.max]

    for img in imgs:
        dest = LS_IMAGES / img.name
        shutil.copy2(img, dest)
        lbl = args.labels / f"{img.stem}.txt"
        boxes = read_yolo_boxes(lbl, names)
        task: dict = {
            "data": {"image": f"/data/local-files/?d={dest.resolve().as_posix()}"},
        }
        if boxes:
            task["predictions"] = [{"model_version": "yolo_bootstrap", "result": boxes}]
        tasks.append(task)

    TASKS_JSON.write_text(json.dumps(tasks, ensure_ascii=False, indent=2), encoding="utf-8")
    (LS_DIR / "classes.txt").write_text("\n".join(names) + "\n", encoding="utf-8")

    print(f"Preparado: {len(tasks)} tareas en {TASKS_JSON}")
    print(f"Imágenes: {LS_IMAGES}")
    print(f"Config:   {CONFIG_XML}")
    print("\n--- Label Studio (pasos) ---")
    print("1. pip install label-studio")
    print("2. label-studio start")
    print("3. Nuevo proyecto -> Object Detection with Bounding Boxes")
    print("4. Settings → Labeling Interface → pegar ml/labelstudio/config.xml")
    print("5. Settings → Cloud Storage → Local files")
    print(f"   Ruta: {LS_IMAGES.resolve()}")
    print("6. Import → Upload Files → elegir tasks.json")
    print("7. Revise cada recuadro → Submit")
    print("8. Export → YOLO → guardar como ml/dataset/exports/labelstudio_yolo.zip")
    print("9. python ml/scripts/import_labelstudio.py --src ml/dataset/exports/labelstudio_yolo.zip --split")


if __name__ == "__main__":
    main()
