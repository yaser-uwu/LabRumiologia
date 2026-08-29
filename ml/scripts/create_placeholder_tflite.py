"""
Crea un TFLite mínimo compatible con la app (entrada 640x640x3, salida [1, 4+nc, 8400])
para compilar/probar la tubería sin un modelo entrenado. NO sirve para detección real.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
import tensorflow as tf


NC = 8
IMGSZ = 640
ANCHORS = 8400


def build():
    inp = tf.keras.Input(shape=(IMGSZ, IMGSZ, 3), name="images")
    x = tf.keras.layers.GlobalAveragePooling2D()(inp)
    x = tf.keras.layers.Dense(64, activation="relu")(x)
    # Produce vector y lo remodelamos a [4+nc, anchors]
    x = tf.keras.layers.Dense((4 + NC) * ANCHORS)(x)
    out = tf.keras.layers.Reshape((4 + NC, ANCHORS), name="output0")(x)
    return tf.keras.Model(inp, out)


def main():
    model = build()
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = []
    tflite_model = converter.convert()

    out = Path("ml/models/model.tflite")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(tflite_model)

    assets = Path("app/src/main/assets/model.tflite")
    assets.write_bytes(tflite_model)
    print(f"Placeholder escrito en {out} y {assets}")


if __name__ == "__main__":
    main()
