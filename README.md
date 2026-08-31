# Lab Rumiología — Detección de equipos (UTEQ)

Aplicación Android que detecta en tiempo real equipos del Laboratorio de Rumiología,
muestra ficha técnica y consulta un asistente RAG. El LLM responde con las **guías y
manuales de ese equipo** (FileSearch de OpenAI).

## Cómo funciona

1. **Etiquetado (Label Studio):** cada recuadro en la foto es un equipo; la clase es el nombre del equipo.
2. **YOLO:** se entrena con esas imágenes + archivos `.txt` (ubicación y clase). La app identifica el equipo en cámara.
3. **RAG / FileSearch:** según el equipo detectado, el backend inyecta los `vector_store_ids` de sus manuales. El modelo (GPT, u otro proveedor) busca **solo** en esa base de conocimiento.

```
Foto del lab  →  Label Studio  →  dataset YOLO  →  entrenamiento  →  TFLite en Android
                                                                      │
Guías PDF/MD  →  vector store por equipo (OpenAI)  ←  clase detectada ┘
                                                                      │
                                                               FileSearch + LLM
```

## Estructura

```
app/          App Android (CameraX + TFLite + chat)
ml/           Dataset, Label Studio, entrenamiento y export TFLite
backend/      FastAPI + FileSearch (OpenAI) + fallback Chroma/Gemini
docs/         Dataset, entregables, evidencias
```

## 1. Etiquetar con Label Studio

Instale [Label Studio](https://labelstud.io/) y cree un proyecto de *Object Detection with Bounding Boxes*.

- Interfaz lista: `ml/labelstudio/config.xml` (las clases coinciden con `ml/dataset/data.yaml`).
- Importe fotos reales del laboratorio (autorización previa; ver `docs/DATASET.md`).
- Dibuje un bounding box por equipo visible.
- **Export → YOLO** (genera `images/`, `labels/` con `class cx cy w h` y `classes.txt`).

```bash
python ml/scripts/import_labelstudio.py --src ruta/al/export.zip --split
```

## 2. Entrenar YOLO y copiar a Android

El enunciado cita YOLOv15. Ultralytics no publica un peso con ese nombre; se usa la
familia actual (YOLO26 por defecto, misma API). Puede pasar otro checkpoint con `--model`.

```bash
pip install -r ml/requirements.txt
python ml/scripts/train_yolo.py --data ml/dataset/data.yaml
python ml/scripts/export_tflite.py --weights ml/models/best.pt
```

## 3. Base de conocimiento (RAG / FileSearch)

Coloque las guías elaboradas por los laboratoristas en `backend/data/docs/<clase>/`.
Ejemplo: `backend/data/docs/balanza_analitica/manual_ohaus.pdf`.

```bash
cd backend
pip install -r requirements.txt
copy .env.example .env
# OPENAI_API_KEY=...
python -m scripts.upload_vector_stores
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

La app envía `{ question, equipment_class }`. El backend resuelve los IDs y llama:

```python
tools=[{"type": "file_search", "vector_store_ids": ["<vector_store_del_equipo>"]}]
```

Si detecta p. ej. `ohaus_pr224`, un alias lo mapea a `balanza_analitica` (véase
`backend/data/equipment_knowledge.json`).

## 4. Android

Abra el proyecto en Android Studio, sync Gradle, instale en dispositivo/emulador.

## Clases por defecto

`incubadora`, `agitador_orbital`, `balanza_analitica`, `phmetro`, `centrifugadora`, `estufa_secado`, `banio_maria`, `microscopio`

Ajuste la lista en Label Studio y en `data.yaml` si el laboratorio usa otros equipos (p. ej. contador de colonias).
