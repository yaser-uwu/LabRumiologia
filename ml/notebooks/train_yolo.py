# Entrenamiento YOLO — Laboratorio de Rumiología
#
# Flujo: Label Studio (bounding boxes) → export YOLO → este cuaderno.
# Checkpoint por defecto: YOLO26n (Ultralytics). El enunciado cita YOLOv15;
# Ultralytics no publica ese nombre; la API de train() es la misma.
#
#   python ml/scripts/train_yolo.py
#   python ml/scripts/export_tflite.py

from pathlib import Path

from ultralytics import YOLO

DATA = Path("../dataset/data.yaml")
PROJECT = Path("../runs")
MODELS = Path("../models")
MODELS.mkdir(parents=True, exist_ok=True)

model = YOLO("yolo26n.pt")
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

metrics = model.val(data=str(DATA.resolve()), split="test")
print(metrics)

exported = model.export(format="tflite", imgsz=640)
print("Exportado:", exported)
