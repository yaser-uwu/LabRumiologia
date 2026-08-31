"""
Entrena un detector YOLO (Ultralytics) con el dataset etiquetado en Label Studio.

El enunciado menciona YOLOv15 para identificar equipos. Ultralytics no publica un
checkpoint llamado yolov15; la familia actual (2026) es YOLO26, con la misma API
de entrenamiento sobre anotaciones YOLO (class cx cy w h). Por defecto se usa
yolo26n.pt (nano, adecuado para exportar a TFLite / Android).

  python ml/scripts/train_yolo.py --data ml/dataset/data.yaml
  python ml/scripts/train_yolo.py --model yolo11n.pt --epochs 80
"""
from __future__ import annotations

import argparse
from pathlib import Path


FALLBACK_MODELS = ("yolo26n.pt", "yolo11n.pt", "yolov8n.pt")


def load_model(requested: str):
    from ultralytics import YOLO

    candidates = [requested, *[m for m in FALLBACK_MODELS if m != requested]]
    last_error: Exception | None = None
    for name in candidates:
        try:
            print(f"Cargando modelo base: {name}")
            return YOLO(name)
        except Exception as exc:  # noqa: BLE001 — probar siguiente checkpoint
            last_error = exc
            print(f"No se pudo cargar {name}: {exc}")
    raise SystemExit(f"No se pudo cargar ningún checkpoint YOLO. Último error: {last_error}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=Path("ml/dataset/data.yaml"))
    parser.add_argument("--epochs", type=int, default=50)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--batch", type=int, default=8)
    parser.add_argument(
        "--model",
        type=str,
        default="yolo26n.pt",
        help="Checkpoint Ultralytics (yolo26n.pt, yolo11n.pt, yolov8n.pt, ...)",
    )
    parser.add_argument("--project", type=Path, default=Path("ml/runs"))
    parser.add_argument("--name", type=str, default="rumiologia")
    args = parser.parse_args()

    import yaml

    data_yaml = args.data.resolve()
    cfg = yaml.safe_load(data_yaml.read_text(encoding="utf-8"))
    cfg["path"] = str(data_yaml.parent)
    runtime_yaml = data_yaml.parent / "_runtime_data.yaml"
    runtime_yaml.write_text(yaml.safe_dump(cfg, sort_keys=False), encoding="utf-8")

    model = load_model(args.model)
    results = model.train(
        data=str(runtime_yaml),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        project=str(args.project.resolve()),
        name=args.name,
        exist_ok=True,
    )

    best = Path(results.save_dir) / "weights" / "best.pt"
    models_dir = Path("ml/models")
    models_dir.mkdir(parents=True, exist_ok=True)
    target = models_dir / "best.pt"
    target.write_bytes(best.read_bytes())
    print(f"Modelo guardado en {target}")

    metrics = model.val(data=str(runtime_yaml), split="test")
    print(metrics)


if __name__ == "__main__":
    main()
