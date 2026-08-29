"""
Entrena YOLOv8n con Ultralytics y guarda best.pt en ml/models/.
"""
from __future__ import annotations

import argparse
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=Path("ml/dataset/data.yaml"))
    parser.add_argument("--epochs", type=int, default=50)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--batch", type=int, default=8)
    parser.add_argument("--model", type=str, default="yolov8n.pt")
    parser.add_argument("--project", type=Path, default=Path("ml/runs"))
    parser.add_argument("--name", type=str, default="rumiologia")
    args = parser.parse_args()

    from ultralytics import YOLO

    data_yaml = args.data.resolve()
    # Ultralytics resuelve 'path: .' respecto al YAML; forzar path absoluto en runtime
    import yaml

    cfg = yaml.safe_load(data_yaml.read_text(encoding="utf-8"))
    cfg["path"] = str(data_yaml.parent)
    runtime_yaml = data_yaml.parent / "_runtime_data.yaml"
    runtime_yaml.write_text(yaml.safe_dump(cfg, sort_keys=False), encoding="utf-8")

    model = YOLO(args.model)
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
