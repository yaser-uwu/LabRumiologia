"""
Genera un dataset sintético YOLO (para probar el pipeline).
Reemplazar por fotos reales del laboratorio antes de la entrega.
"""
from __future__ import annotations

import random
from pathlib import Path

from PIL import Image, ImageDraw

CLASSES = [
    "incubadora",
    "agitador_orbital",
    "balanza_analitica",
    "phmetro",
    "centrifugadora",
    "estufa_secado",
    "banio_maria",
    "microscopio",
]

COLORS = [
    (220, 20, 60),
    (30, 144, 255),
    (50, 205, 50),
    (255, 140, 0),
    (138, 43, 226),
    (255, 215, 0),
    (0, 206, 209),
    (255, 105, 180),
]

ROOT = Path(__file__).resolve().parents[1] / "dataset"
ALL_IMG = ROOT / "all" / "images"
ALL_LBL = ROOT / "all" / "labels"


def make_image(class_id: int, idx: int, seed: int) -> None:
    rng = random.Random(seed)
    w, h = 640, 480
    img = Image.new("RGB", (w, h), (40 + class_id * 8, 40, 50))
    draw = ImageDraw.Draw(img)
    bw = rng.randint(120, 280)
    bh = rng.randint(100, 240)
    x1 = rng.randint(20, w - bw - 20)
    y1 = rng.randint(20, h - bh - 20)
    x2, y2 = x1 + bw, y1 + bh
    draw.rectangle([x1, y1, x2, y2], fill=COLORS[class_id], outline=(255, 255, 255), width=3)
    draw.text((x1 + 8, y1 + 8), CLASSES[class_id], fill=(0, 0, 0))

    name = f"{CLASSES[class_id]}_{idx:03d}"
    ALL_IMG.mkdir(parents=True, exist_ok=True)
    ALL_LBL.mkdir(parents=True, exist_ok=True)
    img.save(ALL_IMG / f"{name}.jpg", quality=90)

    cx = ((x1 + x2) / 2) / w
    cy = ((y1 + y2) / 2) / h
    nw = (x2 - x1) / w
    nh = (y2 - y1) / h
    (ALL_LBL / f"{name}.txt").write_text(f"{class_id} {cx:.6f} {cy:.6f} {nw:.6f} {nh:.6f}\n", encoding="utf-8")


def main(per_class: int = 20):
    n = 0
    for cid, _ in enumerate(CLASSES):
        for i in range(per_class):
            make_image(cid, i, seed=cid * 1000 + i)
            n += 1
    print(f"Generadas {n} imágenes sintéticas en {ALL_IMG}")
    print("Ejecute: python ml/scripts/split_dataset.py")


if __name__ == "__main__":
    main()
