#!/usr/bin/env python3
"""Small cross-platform helpers shared by the performance tools."""

from __future__ import annotations

import os
from pathlib import Path
import shutil
import subprocess
import sys
from typing import Any


def configure_utf8_stdio() -> None:
    """Use deterministic UTF-8 for redirected output and CI logs."""
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if callable(reconfigure):
            reconfigure(encoding="utf-8", errors="replace")


def gradle_wrapper(project_root: Path) -> Path:
    """Return the native Gradle wrapper for the current operating system."""
    name = "gradlew.bat" if os.name == "nt" else "gradlew"
    wrapper = project_root / name
    if not wrapper.is_file():
        raise FileNotFoundError(f"Gradle wrapper not found: {wrapper}")
    if os.name != "nt" and not os.access(wrapper, os.X_OK):
        raise PermissionError(f"Gradle wrapper is not executable: {wrapper}")
    return wrapper


def find_jdk_tool(name: str, *, required: bool = True) -> Path | None:
    """Find a JDK executable through JAVA_HOME, PATH, or Gradle toolchains."""
    executable_names = (f"{name}.exe", name) if os.name == "nt" else (name, f"{name}.exe")
    candidates: list[Path] = []

    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidates.extend(Path(java_home) / "bin" / executable for executable in executable_names)

    for executable in executable_names:
        command = shutil.which(executable)
        if command:
            candidates.append(Path(command))

    gradle_jdks = Path.home() / ".gradle" / "jdks"
    if gradle_jdks.is_dir():
        for executable in executable_names:
            candidates.extend(sorted(gradle_jdks.glob(f"**/bin/{executable}"), reverse=True))

    tool = next(
        (
            candidate
            for candidate in candidates
            if candidate.is_file() and os.access(candidate, os.X_OK)
        ),
        None,
    )
    if tool is None and required:
        raise FileNotFoundError(
            f"A JDK containing {name!r} is required. Set JAVA_HOME or add it to PATH."
        )
    return tool


def hidden_process_startup() -> Any | None:
    """Hide the Gradle console on Windows; return no platform option elsewhere."""
    if os.name != "nt":
        return None
    startup = getattr(subprocess, "STARTUPINFO")()
    startup.dwFlags |= getattr(subprocess, "STARTF_USESHOWWINDOW")
    return startup
