#!/usr/bin/env python3
"""Write portable text summaries for a Java Flight Recorder recording."""

from __future__ import annotations

import argparse
from pathlib import Path
import subprocess

from platform_tools import configure_utf8_stdio, find_jdk_tool


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("recording", type=Path)
    parser.add_argument("--output-directory", type=Path)
    return parser.parse_args()


def write_report(jfr: Path, arguments: tuple[str, ...], recording: Path, output: Path) -> None:
    completed = subprocess.run(
        [str(jfr), *arguments, str(recording)],
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    output.write_text(completed.stdout, encoding="utf-8")


def main() -> None:
    args = parse_args()
    recording = args.recording.resolve()
    if not recording.is_file():
        raise FileNotFoundError(f"JFR recording not found: {recording}")
    output = (args.output_directory or recording.parent).resolve()
    output.mkdir(parents=True, exist_ok=True)
    jfr = find_jdk_tool("jfr")
    assert jfr is not None

    reports = (
        (("summary",), output / "jfr-summary.txt"),
        (("view", "hot-methods"), output / "jfr-hot-methods.txt"),
        (("view", "allocation-by-site"), output / "jfr-allocation-by-site.txt"),
    )
    for arguments, destination in reports:
        write_report(jfr, arguments, recording, destination)
        print(destination)


if __name__ == "__main__":
    configure_utf8_stdio()
    main()
