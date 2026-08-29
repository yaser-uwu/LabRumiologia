# Entrenamiento YOLOv8 — Laboratorio de Rumiología
#
# Requisitos: pip install ultralytics pyyaml
#
# 1. Coloque imágenes y etiquetas YOLO en ml/dataset (ver docs/DATASET.md)
# 2. Ejecute celdas en orden o use:
#      python ml/scripts/train_yolo.py
#      python ml/scripts/export_tflite.py

from pathlib import Path
from ultralytics import YOLO

DATA = Path("../dataset/data.yaml")
PROJECT = Path("../runs")
MODELS = Path("../models")
MODELS.mkdir(parents=True, exist_ok=True)

model = YOLO("yolov8n.pt")
results = model.train(
    data=str(DATA.resolve()),
    epochs=50,
    imgsz=640,
    batch=8,
    project=str(PROJECT),
    name="rumiologia",
    exist_ok=True,
)

best = Path(results.save_dir) / "weights" / "best.pt"
(MODELS / "best.pt").write_bytes(best.read_bytes())

# Evaluación en split test
metrics = model.val(data=str(DATA.resolve()), split="test")
print(metrics)

# Export TFLite
exported = model.export(format="tflite", imgsz=640)
print("Exportado:", exported)
