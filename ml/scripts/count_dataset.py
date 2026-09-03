"""
Cuenta imágenes por clase en el dataset YOLO (train/val/test o all/).

  python ml/scripts/count_dataset.py
  python ml/scripts/count_dataset.py --split
"""
from __future__ import annotations

import argparse
from collections import defaultdict
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1] / "dataset"
DATA_YAML = ROOT / "data.yaml"


def load_names(path: Path) -> list[str]:
    cfg = yaml.safe_load(path.read_text(encoding="utf-8"))
    names = cfg.get("names", [])
    if isinstance(names, dict):
        return [names[i] for i in sorted(names, key=lambda k: int(k))]
    return list(names)


def count_split(lbl_dir: Path) -> dict[int, int]:
    counts: dict[int, int] = defaultdict(int)
    if not lbl_dir.is_dir():
        return counts
    for lbl in lbl_dir.glob("*.txt"):
        for line in lbl.read_text(encoding="utf-8").splitlines():
            parts = line.strip().split()
            if len(parts) >= 5:
                counts[int(float(parts[0]))] += 1
    return counts


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=DATA_YAML)
    parser.add_argument("--split", action="store_true", help="Mostrar train/val/test")
    args = parser.parse_args()

    names = load_names(args.data)
    splits = ["train", "val", "test"] if args.split else ["all"]
    totals = defaultdict(int)
    split_totals: dict[str, dict[int, int]] = {}

    if "all" in splits:
        split_totals["all"] = count_split(ROOT / "all" / "labels")
    if args.split:
        for s in ["train", "val", "test"]:
            split_totals[s] = count_split(ROOT / "labels" / s)

    print(f"{'Clase':<22} {'Total':>6}  ", end="")
    if args.split:
        print(f"{'Train':>6} {'Val':>6} {'Test':>6}")
    else:
        print()

    for i, name in enumerate(names):
        if args.split:
            tr = split_totals.get("train", {}).get(i, 0)
            va = split_totals.get("val", {}).get(i, 0)
            te = split_totals.get("test", {}).get(i, 0)
            tot = tr + va + te
            print(f"{name:<22} {tot:>6}  {tr:>6} {va:>6} {te:>6}")
        else:
            tot = split_totals.get("all", {}).get(i, 0)
            print(f"{name:<22} {tot:>6}")
        totals[name] = tot

    print(f"\nImágenes únicas (aprox.): {sum(totals.values())} cajas en labels")


if __name__ == "__main__":
    main()
