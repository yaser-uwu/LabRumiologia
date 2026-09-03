@echo off
REM Inicia Label Studio con soporte para archivos locales (evita subir 320 fotos por arrastre)
set LABEL_STUDIO_LOCAL_FILES_SERVING_ENABLED=true
set LABEL_STUDIO_LOCAL_FILES_DOCUMENT_ROOT=%~dp0..\labelstudio\images
echo Document root: %LABEL_STUDIO_LOCAL_FILES_DOCUMENT_ROOT%
label-studio start --port 8080
