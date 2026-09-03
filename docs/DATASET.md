# Dataset — Laboratorio de Rumiología (UTEQ)

Guía completa de etiquetado: **`docs/LABEL_STUDIO.md`**

## Autorización

Antes de fotografiar, solicite autorización al responsable del Laboratorio de Rumiología.
Conserve evidencias (correo o constancia) en `docs/evidencias/`.

## Clases (equipos = clase YOLO)

La **clase YOLO es el equipo**. Debe coincidir con Label Studio, `data.yaml` y
`backend/data/docs/<clase>/`.

| id | clase YOLO | Equipo |
|----|------------|--------|
| 0 | ankom_daisy_ii | Incubadora ANKOM Daisy II |
| 1 | aquasearcher | OHAUS AquaSearcher |
| 2 | ohaus_pr | Balanza OHAUS PR Series |
| 3 | contador_colonias | Contador POL-EKO LKB 2002 |
| 4 | ohaus_pa214 | Balanza OHAUS Pioneer PA214 |
| 5 | banio_maria_memmert | Baño María Memmert |
| 6 | ankom_a200 | ANKOM 200 Fiber Analyzer |
| 7 | shimadzu_gc2014 | Cromatógrafo Shimadzu GC-2014 |
| 8 | estufa_secado | Estufa de secado |
| 9 | selladora_aie200 | Selladora AIE-200 |
| 10 | desecador | Desecador de vidrio |

Inventario de fotos por carpeta: `ml/dataset/inventario_equipos.json`.

## Label Studio (resumen)

1. `python ml/scripts/sync_photos_from_inventario.py --src "...\Modelos lab"`
2. `python ml/scripts/auto_label_from_folders.py`
3. `python ml/scripts/prepare_labelstudio.py` → `label-studio start`
4. Etiquetar con `ml/labelstudio/config.xml`
5. Export → YOLO → `ml/dataset/exports/labelstudio_yolo.zip`
6. `python ml/scripts/import_labelstudio.py --src ... --split`

Cada `.txt` de etiqueta tiene: `class_id cx cy width height` (0–1).

## Estructura

```
ml/dataset/
  raw/<clase>/          # fotos sin etiquetar (por carpeta)
  all/images|labels/    # pool antes del split
  images/train|val|test/
  labels/train|val|test/
  data.yaml
```

## Split

- 70 % train · 15 % val · 15 % test (estratificado por clase)

## Conteos (actualice tras etiquetar)

Ejecute: `python ml/scripts/count_dataset.py --split`

| Clase | Total | Train | Val | Test |
|-------|-------|-------|-----|------|
| ankom_daisy_ii | | | | |
| aquasearcher | | | | |
| ohaus_pr | | | | |
| contador_colonias | | | | |
| ohaus_pa214 | | | | |
| banio_maria_memmert | | | | |
| ankom_a200 | | | | |
| shimadzu_gc2014 | | | | |
| estufa_secado | | | | |
| selladora_aie200 | | | | |
| desecador | | | | |

> El dataset sintético (`generate_sample_dataset.py`) solo valida el pipeline.
> Para la entrega use fotos reales etiquetadas en Label Studio.
