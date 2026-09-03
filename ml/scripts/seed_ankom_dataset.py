"""
Dataset mínimo para arrancar detección de ankom_a200 con fotos reales del usuario.
Genera varias vistas recortadas/augmentadas a partir de capturas de la app.
"""
from __future__ import annotations

import random
import shutil
from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps

ROOT = Path(__file__).resolve().parents[1] / "dataset"
OUT_IMG = ROOT / "yolo_ls" / "images" / "train"
OUT_LBL = ROOT / "yolo_ls" / "labels" / "train"
VAL_IMG = ROOT / "yolo_ls" / "images" / "val"
VAL_LBL = ROOT / "yolo_ls" / "labels" / "val"

# ankom_a200 según ml/dataset/data.yaml
CLASS_ID = 6

# Bbox normalizada (cx, cy, w, h) del equipo ANKOM en el área de cámara
DEFAULT_BOX = (0.50, 0.42, 0.88, 0.62)

SOURCES = [
    Path(
        r"C:\Users\Dispositivo\.cursor\projects\c-Users-Dispositivo-AndroidStudioProjects-deteccionderostro\assets"
        r"\c__Users_Dispositivo_AppData_Roaming_Cursor_User_workspaceStorage_72a9852777d91a0829732f7d085b213c_images_image-48ceb89d-5e34-4808-aeed-6c927eccdf30.png"
    ),
    Path(
        r"C:\Users\Dispositivo\.cursor\projects\c-Users-Dispositivo-AndroidStudioProjects-deteccionderostro\assets"
        r"\c__Users_Dispositivo_AppData_Roaming_Cursor_User_workspaceStorage_72a9852777d91a0829732f7d085b213c_images_image-6e7941ee-05cd-48f8-945f-d3255b28e00e.png"
    ),
]


def crop_camera(img: Image.Image) -> Image.Image:
    w, h = img.size
    top = int(h * 0.04)
    bottom = int(h * 0.74)
    return img.crop((0, top, w, bottom))


def write_label(path: Path, box: tuple[float, float, float, float]) -> None:
    cx, cy, bw, bh = box
    path.write_text(f"{CLASS_ID} {cx:.6f} {cy:.6f} {bw:.6f} {bh:.6f}\n", encoding="utf-8")


def augment(img: Image.Image, seed: int) -> Image.Image:
    rng = random.Random(seed)
    out = img.copy()
    if rng.random() < 0.5:
        out = ImageOps.mirror(out)
    angle = rng.uniform(-8, 8)
    out = out.rotate(angle, resample=Image.BILINEAR, expand=False, fillcolor=(114, 114, 114))
    out = ImageEnhance.Brightness(out).enhance(rng.uniform(0.85, 1.15))
    out = ImageEnhance.Contrast(out).enhance(rng.uniform(0.9, 1.1))
    return out


def main() -> None:
    for d in (OUT_IMG, OUT_LBL, VAL_IMG, VAL_LBL):
        if d.exists():
            shutil.rmtree(d)
        d.mkdir(parents=True, exist_ok=True)

    idx = 0
    for src in SOURCES:
        if not src.exists():
            print(f"Omitido (no existe): {src}")
            continue
        base = crop_camera(Image.open(src).convert("RGB"))
        raw_dir = ROOT / "raw"
        raw_dir.mkdir(parents=True, exist_ok=True)
        base.save(raw_dir / f"ankom_a200_source_{idx:02d}.jpg", quality=92)

        for j in range(24):
            img = augment(base, seed=idx * 100 + j) if j else base
            name = f"ankom_a200_{idx:03d}_{j:02d}"
            target_img = OUT_IMG if j < 20 else VAL_IMG
            target_lbl = OUT_LBL if j < 20 else VAL_LBL
            img.save(target_img / f"{name}.jpg", quality=92)
            write_label(target_lbl / f"{name}.txt", DEFAULT_BOX)
        idx += 1

    print(f"Seed dataset en {ROOT / 'yolo_ls'} ({idx} fuentes)")


if __name__ == "__main__":
    main()
