"""
Divide un dataset YOLO ya etiquetado en train/val/test (70/15/15) estratificado por clase.

Entrada esperada:
  ml/dataset/all/images/*.jpg
  ml/dataset/all/labels/*.txt

O bien carpetas por clase en raw + labels paralelos en all/.
"""
from __future__ import annotations

import argparse
import random
import shutil
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1] / "dataset"


def primary_class(label_path: Path) -> int:
    lines = label_path.read_text(encoding="utf-8").strip().splitlines()
    if not lines:
        return -1
    return int(lines[0].split()[0])


def split_files(pairs: list[tuple[Path, Path]], seed: int = 42):
    by_class: dict[int, list[tuple[Path, Path]]] = defaultdict(list)
    for img, lbl in pairs:
        by_class[primary_class(lbl)].append((img, lbl))

    rng = random.Random(seed)
    splits = {"train": [], "val": [], "test": []}
    for items in by_class.values():
        rng.shuffle(items)
        n = len(items)
        n_train = int(n * 0.70)
        n_val = int(n * 0.15)
        splits["train"].extend(items[:n_train])
        splits["val"].extend(items[n_train : n_train + n_val])
        splits["test"].extend(items[n_train + n_val :])
    return splits


def copy_split(name: str, items: list[tuple[Path, Path]], out: Path):
    img_dir = out / "images" / name
    lbl_dir = out / "labels" / name
    img_dir.mkdir(parents=True, exist_ok=True)
    lbl_dir.mkdir(parents=True, exist_ok=True)
    for img, lbl in items:
        shutil.copy2(img, img_dir / img.name)
        shutil.copy2(lbl, lbl_dir / lbl.name)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--images", type=Path, default=ROOT / "all" / "images")
    parser.add_argument("--labels", type=Path, default=ROOT / "all" / "labels")
    parser.add_argument("--out", type=Path, default=ROOT)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    pairs = []
    for img in sorted(args.images.glob("*")):
        if img.suffix.lower() not in {".jpg", ".jpeg", ".png", ".bmp", ".webp"}:
            continue
        lbl = args.labels / f"{img.stem}.txt"
        if lbl.exists():
            pairs.append((img, lbl))

    if not pairs:
        raise SystemExit(
            f"No se encontraron pares imagen/etiqueta en {args.images} y {args.labels}.\n"
            "Etiquete primero y deje los .txt YOLO con el mismo nombre base."
        )

    splits = split_files(pairs, args.seed)
    for name, items in splits.items():
        copy_split(name, items, args.out)
        print(f"{name}: {len(items)} imágenes")


if __name__ == "__main__":
    main()
