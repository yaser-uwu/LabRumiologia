# Lab Rumiología — Detección de equipos (UTEQ)

Aplicación Android que detecta en tiempo real equipos del Laboratorio de Rumiología (YOLO → TFLite), muestra ficha técnica y consulta un asistente RAG vía backend/MCP.

## Estructura

```
app/          App Android (CameraX + TFLite + chat)
ml/           Dataset, scripts de entrenamiento y export TFLite
backend/      FastAPI + Chroma + Gemini + servidor MCP
docs/         Dataset, entregables, evidencias
```

## Flujo rápido

1. **Dataset:** ver [docs/DATASET.md](docs/DATASET.md). Coloque fotos etiquetadas y ejecute `python ml/scripts/split_dataset.py`.
2. **Entrenar / exportar:**
   ```bash
   pip install -r ml/requirements.txt
   python ml/scripts/train_yolo.py --data ml/dataset/data.yaml
   python ml/scripts/export_tflite.py --weights ml/models/best.pt
   ```
3. **Backend RAG:**
   ```bash
   cd backend
   pip install -r requirements.txt
   copy .env.example .env   # configure GEMINI_API_KEY
   uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```
4. **Android:** abra el proyecto en Android Studio, sync Gradle, instale en dispositivo/emulador.

## Clases por defecto

`incubadora`, `agitador_orbital`, `balanza_analitica`, `phmetro`, `centrifugadora`, `estufa_secado`, `banio_maria`, `microscopio`

## Arquitectura

- Detección on-device (TFLite). El LLM **no** identifica equipos.
- Android envía solo `{ question, equipment_class }` al backend.
- El backend/MCP recupera fragmentos de manuales y genera la respuesta con fuentes.
