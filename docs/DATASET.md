# Dataset — Laboratorio de Rumiología (UTEQ)

## Autorización

Antes de fotografiar, solicite autorización al responsable del Laboratorio de Rumiología.
Conserve evidencias (correo o constancia) en `docs/evidencias/`.

## Clases (equipos)

La **clase YOLO es el equipo**. Debe coincidir con el `value` en Label Studio y con
la carpeta de manuales en `backend/data/docs/<clase>/`.

| id YOLO | Nombre |
|---------|--------|
| incubadora | Incubadora |
| agitador_orbital | Agitador orbital |
| balanza_analitica | Balanza analítica |
| phmetro | pHmetro |
| centrifugadora | Centrifugadora |
| estufa_secado | Estufa de secado |
| banio_maria | Baño María |
| microscopio | Microscopio |

Ajuste esta lista si su captura real usa otros equipos (6–10 clases), por ejemplo
modelos comerciales (`ohaus_pr224`). En ese caso añada la clase en Label Studio,
`data.yaml` y `backend/data/equipment_knowledge.json`.

## Label Studio

YOLO necesita, por cada foto, un `.txt` con la **ubicación del recuadro** y el **id de clase**.

1. Instale Label Studio: https://labelstud.io/ (`pip install label-studio` y `label-studio`).
2. Nuevo proyecto → plantilla *Object Detection with Bounding Boxes*.
3. Pegue `ml/labelstudio/config.xml` en *Labeling Interface*.
4. Importe las fotos del laboratorio.
5. Etiquete: un recuadro por equipo visible; elija la clase correcta.
6. *Export* → formato **YOLO** (ZIP con `images/`, `labels/`, `classes.txt`).
7. Importe al repo:

```bash
python ml/scripts/import_labelstudio.py --src export-labelstudio.zip --split
```

Cada línea de `labels/*.txt` tiene el formato YOLO: `class_id cx cy width height` (valores 0–1).

## Captura

- Meta: **80–150 fotografías por clase**.
- Variar: ángulo, distancia, iluminación, oclusión parcial, fondo del lab.
- Preferir fotos reales del laboratorio (no stock).

## Estructura

```
ml/dataset/
  images/
    train/
    val/
    test/
  labels/
    train/
    val/
    test/
  data.yaml
```

## Split

- 70 % train
- 15 % val
- 15 % test

Estratificar por clase. No mezclar la misma foto (o recortes casi idénticos) en varios splits.

`--split` en el importador llama a `ml/scripts/split_dataset.py`.

## Conteos

Complete la tabla tras etiquetar:

| Clase | Total | Train | Val | Test |
|-------|-------|-------|-----|------|
| incubadora | | | | |
| agitador_orbital | | | | |
| balanza_analitica | | | | |
| phmetro | | | | |
| centrifugadora | | | | |
| estufa_secado | | | | |
| banio_maria | | | | |
| microscopio | | | | |

> El dataset sintético de prueba (`generate_sample_dataset.py`) solo sirve para validar el pipeline. Reemplácelo con fotos reales etiquetadas en Label Studio.
