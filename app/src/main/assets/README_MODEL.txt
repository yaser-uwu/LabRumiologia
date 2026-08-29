# Coloque aquí el modelo exportado tras el entrenamiento:
#   ml/models/model.tflite  ->  copiar a este directorio como model.tflite
#
# Comando sugerido (desde la raíz del repo):
#   python ml/scripts/train_yolo.py --data ml/dataset/data.yaml --epochs 50
#   python ml/scripts/export_tflite.py --weights ml/models/best.pt
#
# Hasta entonces la app muestra un aviso y no ejecuta inferencia.
