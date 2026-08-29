# Dataset — Laboratorio de Rumiología (UTEQ)

## Autorización

Antes de fotografiar, solicite autorización al responsable del Laboratorio de Rumiología.
Conserve evidencias (correo o constancia) en `docs/evidencias/`.

## Clases (8 equipos)

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

Ajuste esta lista si su captura real usa otros equipos (6–10 clases).

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

Formato de anotación: **YOLO** (`class cx cy w h` normalizado).
Herramientas: Label Studio, CVAT, Roboflow o LabelImg.

## Split

- 70 % train
- 15 % val
- 15 % test

Estratificar por clase. No mezclar la misma foto (o recortes casi idénticos) en varios splits.

## Cómo organizar fotos existentes

1. Copie sus imágenes a `ml/dataset/raw/<clase>/`.
2. Etiquete y exporte a formato YOLO.
3. Ejecute `python ml/scripts/split_dataset.py` para generar train/val/test.
4. Verifique `data.yaml`.

## Conteos

Complete la tabla tras etiquetar:

| Clase | Total | Train | Val | Test |
|-------|-------|-------|-----|------|
| incubadora | 20 | 14 | 3 | 3 |
| agitador_orbital | 20 | 14 | 3 | 3 |
| balanza_analitica | 20 | 14 | 3 | 3 |
| phmetro | 20 | 14 | 3 | 3 |
| centrifugadora | 20 | 14 | 3 | 3 |
| estufa_secado | 20 | 14 | 3 | 3 |
| banio_maria | 20 | 14 | 3 | 3 |
| microscopio | 20 | 14 | 3 | 3 |

> **Nota:** conteos sintéticos de prueba. Reemplace con sus fotos reales del laboratorio.
