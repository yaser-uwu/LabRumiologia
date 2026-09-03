# Pipeline de etiquetado — Lab Rumiología
# Ejecute desde la raíz del proyecto en PowerShell.

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $Root

Write-Host "=== 1. Copiar fotos del laboratorio ===" -ForegroundColor Cyan
$src = Read-Host "Ruta a 'Modelos lab' (Enter = ruta del inventario)"
if ([string]::IsNullOrWhiteSpace($src)) {
    python ml/scripts/sync_photos_from_inventario.py
} else {
    python ml/scripts/sync_photos_from_inventario.py --src $src
}

Write-Host "`n=== 2. Etiquetas iniciales (bootstrap) ===" -ForegroundColor Cyan
python ml/scripts/auto_label_from_folders.py

Write-Host "`n=== 3. Preparar Label Studio ===" -ForegroundColor Cyan
python ml/scripts/prepare_labelstudio.py

Write-Host "`n=== Siguiente: abra Label Studio ===" -ForegroundColor Green
Write-Host "  label-studio start"
Write-Host "  Guía: docs/LABEL_STUDIO.md"
Write-Host ""
Write-Host "Tras exportar YOLO desde Label Studio:" -ForegroundColor Yellow
Write-Host "  python ml/scripts/import_labelstudio.py --src ml/dataset/exports/labelstudio_yolo.zip --split"
Write-Host "  python ml/scripts/train_yolo.py --data ml/dataset/data.yaml --epochs 80"
Write-Host "  python ml/scripts/export_tflite.py"
