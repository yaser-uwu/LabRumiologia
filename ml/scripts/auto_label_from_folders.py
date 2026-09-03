"""
Genera etiquetas YOLO a partir de fotos ya organizadas por carpeta/clase.

Cada foto de ml/dataset/raw/<clase>/ recibe un recuadro centrado (el equipo
ocupa casi todo el encuadre en estas tomas). Sirve para un primer entrenamiento.
Luego conviene corregir en Label Studio.

  python ml/scripts/auto_label_from_folders.py --split
"""
from __future__ import annotations

import argparse
import random
import shutil
from collections import defaultdict
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "dataset"
RAW = DATASET / "raw"
DATA_YAML = DATASET / "data.yaml"
IMG_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}

# Recuadro normalizado YOLO: class cx cy w h (el objeto principal suele ir al centro)
DEFAULT_BOX = (0.50, 0.50, 0.86, 0.82)


def load_names(path: Path) -> list[str]:
    cfg = yaml.safe_load(path.read_text(encoding="utf-8"))
    names = cfg.get("names", [])
    if isinstance(names, dict):
        return [names[i] for i in sorted(names, key=lambda k: int(k))]
    return list(names)


def collect(raw: Path, names: list[str]) -> dict[str, list[Path]]:
    by_class: dict[str, list[Path]] = {}
    for name in names:
        folder = raw / name
        if not folder.is_dir():
            by_class[name] = []
            continue
        imgs = [p for p in folder.iterdir() if p.suffix.lower() in IMG_EXTS]
        by_class[name] = sorted(imgs)
    return by_class


def write_pair(img: Path, dest_img: Path, dest_lbl: Path, class_id: int, box: tuple[float, float, float, float]) -> None:
    dest_img.parent.mkdir(parents=True, exist_ok=True)
    dest_lbl.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(img, dest_img)
    cx, cy, w, h = box
    dest_lbl.write_text(f"{class_id} {cx:.6f} {cy:.6f} {w:.6f} {h:.6f}\n", encoding="utf-8")


def split_pairs(pairs: list[tuple[Path, Path, int]], seed: int = 42) -> dict[str, list[tuple[Path, Path, int]]]:
    by_class: dict[int, list[tuple[Path, Path, int]]] = defaultdict(list)
    for item in pairs:
        by_class[item[2]].append(item)
    rng = random.Random(seed)
    out = {"train": [], "val": [], "test": []}
    for items in by_class.values():
        rng.shuffle(items)
        n = len(items)
        n_train = max(1, int(n * 0.70)) if n >= 4 else max(1, n - 1)
        n_val = max(1, int(n * 0.15)) if n >= 6 else (1 if n >= 3 else 0)
        if n_train + n_val >= n:
            n_train = max(1, n - 2) if n >= 3 else n
            n_val = 1 if n >= 3 else 0
        out["train"].extend(items[:n_train])
        out["val"].extend(items[n_train : n_train + n_val])
        out["test"].extend(items[n_train + n_val :])
        if n >= 2 and not out["val"]:
            out["val"].append(out["train"].pop())
    return out


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--raw", type=Path, default=RAW)
    parser.add_argument("--data", type=Path, default=DATA_YAML)
    parser.add_argument("--split", action="store_true")
    args = parser.parse_args()

    names = load_names(args.data)
    collected = collect(args.raw, names)
    all_img = DATASET / "all" / "images"
    all_lbl = DATASET / "all" / "labels"
    if all_img.exists():
        shutil.rmtree(all_img)
    if all_lbl.exists():
        shutil.rmtree(all_lbl)

    pairs: list[tuple[Path, Path, int]] = []
    for class_id, name in enumerate(names):
        imgs = collected.get(name) or []
        print(f"{name}: {len(imgs)} fotos")
        for img in imgs:
            dest_img = all_img / img.name
            dest_lbl = all_lbl / f"{img.stem}.txt"
            write_pair(img, dest_img, dest_lbl, class_id, DEFAULT_BOX)
            pairs.append((dest_img, dest_lbl, class_id))

    if not pairs:
        raise SystemExit(f"No hay fotos en {args.raw}/<clase>/")

    print(f"Etiquetas iniciales: {len(pairs)} en {DATASET / 'all'}")
    print("Siguiente: python ml/scripts/prepare_labelstudio.py")
    print("          label-studio start   (ver docs/LABEL_STUDIO.md)")
    if not args.split:
        return

    splits = split_pairs(pairs)
    for split_name, items in splits.items():
        img_dir = DATASET / "images" / split_name
        lbl_dir = DATASET / "labels" / split_name
        if img_dir.exists():
            shutil.rmtree(img_dir)
        if lbl_dir.exists():
            shutil.rmtree(lbl_dir)
        img_dir.mkdir(parents=True, exist_ok=True)
        lbl_dir.mkdir(parents=True, exist_ok=True)
        for img, lbl, _ in items:
            shutil.copy2(img, img_dir / img.name)
            shutil.copy2(lbl, lbl_dir / lbl.name)
        print(f"{split_name}: {len(items)}")


if __name__ == "__main__":
    main()
