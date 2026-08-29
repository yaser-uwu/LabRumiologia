# Checklist de entregables

Según el enunciado del proyecto:

- [x] Repositorio del proyecto Android (este repo)
- [x] Conjunto de datos organizado y documentado (`ml/dataset/`, `docs/DATASET.md`)
- [x] Anotaciones con bounding boxes (YOLO en `ml/dataset/labels/`; reemplazar sintéticas por fotos reales del lab)
- [x] Código / cuaderno de entrenamiento (`ml/scripts/`, `ml/notebooks/train_yolo.ipynb`)
- [x] Modelo original `.pt` (`ml/models/best.pt`)
- [x] Modelo `.tflite` (`ml/models/model.tflite` y `app/src/main/assets/model.tflite`)
- [x] Aplicación Android instalable (`app/build/outputs/apk/debug/` tras `gradlew assembleDebug`)
- [ ] Evidencias de funcionamiento en teléfono real (`docs/evidencias/`)
- [ ] Video de demostración (`docs/evidencias/demo.mp4`)
- [x] Backend RAG/MCP (`backend/`)
- [x] Documentos de ejemplo para corpus (`backend/data/docs/`) — reemplazar por manuales UTEQ

## Evidencias sugeridas

Coloque en `docs/evidencias/`:

1. Captura con varios equipos detectados (boxes + nombre + %).
2. Captura de ficha técnica.
3. Captura de chat con fuente citada.
4. Captura del caso “sin información suficiente”.
5. Video corto en el laboratorio real.
6. Constancia de autorización para fotografiar.

## Notas

- El dataset actual incluye un **pipeline sintético de prueba**. Debe reemplazarse por fotos reales del Laboratorio de Rumiología (80–150 por clase).
- En Windows, la exportación TFLite puede fallar en el paso de metadata; el `.tflite` generado por onnx2tf en `ml/models/best_saved_model/` es válido. Preferible re-entrenar/exportar en Colab/Linux con fotos reales.
- Configure `GEMINI_API_KEY` en `backend/.env` para respuestas LLM reales.
