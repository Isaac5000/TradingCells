#!/usr/bin/env python3
"""Reject accidental operating-system dependencies in project tooling and tests."""

from __future__ import annotations

import argparse
from collections import defaultdict
from pathlib import Path
import re
import subprocess


EXCLUDED_DIRECTORIES = {
    ".git",
    ".gradle",
    ".idea",
    ".pytest_cache",
    ".run",
    ".vscode",
    "__pycache__",
    "artifacts",
    "build",
    "logs",
    "out",
    "repo",
    "run",
    "run-server",
}
OPERATIONAL_SUFFIXES = {".gradle", ".java", ".properties", ".py", ".toml", ".yaml", ".yml"}
ABSOLUTE_PATH_PATTERNS = (
    re.compile(r"(?<![A-Za-z0-9_])[A-Za-z]:[\\/]"),
    re.compile(r"(?<![:A-Za-z0-9_])/(?:home|Users|tmp|var/tmp)/"),
)
SHELL_COMMAND_PATTERN = re.compile(
    r"['\"](?:powershell|pwsh|cmd(?:\.exe)?|bash|sh|sed|awk|grep|find|xargs|"
    r"cp|mv|rm|chmod|readlink|realpath)['\"]",
    re.IGNORECASE,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    return parser.parse_args()


def project_files(root: Path) -> list[Path]:
    return sorted(
        path
        for path in root.rglob("*")
        if path.is_file()
        and not any(part in EXCLUDED_DIRECTORIES for part in path.relative_to(root).parts)
    )


def source_line(path: Path, offset: int, text: str) -> str:
    line = text.count("\n", 0, offset) + 1
    return f"{path}:{line}"


def tracked_files(root: Path) -> list[Path]:
    if not (root / ".git").exists():
        return []
    try:
        completed = subprocess.run(
            ["git", "ls-files", "-z"],
            cwd=root,
            check=True,
            capture_output=True,
        )
    except (OSError, subprocess.CalledProcessError):
        return []
    return [Path(value.decode("utf-8")) for value in completed.stdout.split(b"\0") if value]


def main() -> None:
    root = parse_args().root.resolve()
    if not root.is_dir():
        raise FileNotFoundError(f"Project root not found: {root}")
    files = project_files(root)
    violations: list[str] = []

    for relative in tracked_files(root):
        if (root / relative).exists() and (
            relative.suffix.lower() in {".pyc", ".pyo"} or "__pycache__" in relative.parts
        ):
            violations.append(f"generated Python artifact is versioned: {relative}")

    required_wrapper_files = (
        root / "gradlew",
        root / "gradlew.bat",
        root / "gradle/wrapper/gradle-wrapper.jar",
        root / "gradle/wrapper/gradle-wrapper.properties",
    )
    for path in required_wrapper_files:
        if not path.is_file():
            violations.append(f"missing Gradle wrapper file: {path.relative_to(root)}")
    if (root / "gradlew").is_file() and b"\r\n" in (root / "gradlew").read_bytes():
        violations.append("gradlew must use LF line endings")

    for path in files:
        relative = path.relative_to(root)
        if path.suffix.lower() in {".pyc", ".pyo"} or "__pycache__" in relative.parts:
            violations.append(f"generated Python artifact is versioned: {relative}")
        if relative.parts and relative.parts[0] == "tools" and path.suffix.lower() in {
            ".bat",
            ".cmd",
            ".ps1",
            ".sh",
        }:
            violations.append(f"OS-specific tool requires a portable replacement: {relative}")

    by_casefold: dict[str, list[Path]] = defaultdict(list)
    for path in files:
        relative = path.relative_to(root)
        by_casefold[relative.as_posix().casefold()].append(relative)
    for matches in by_casefold.values():
        spellings = sorted({path.as_posix() for path in matches})
        if len(spellings) > 1:
            violations.append(f"case-colliding paths: {', '.join(spellings)}")

    checker = Path(__file__).resolve()
    platform_helper = root / "tools/performance/platform_tools.py"
    for path in files:
        if path.suffix.lower() not in OPERATIONAL_SUFFIXES:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeError as error:
            violations.append(f"non-UTF-8 operational file: {path.relative_to(root)} ({error})")
            continue
        for pattern in ABSOLUTE_PATH_PATTERNS:
            for match in pattern.finditer(text):
                violations.append(
                    f"hardcoded absolute path at {source_line(path.relative_to(root), match.start(), text)}"
                )
        if path.suffix.lower() != ".py" or path.resolve() == checker:
            continue
        for match in SHELL_COMMAND_PATTERN.finditer(text):
            violations.append(
                f"external shell command at {source_line(path.relative_to(root), match.start(), text)}"
            )
        for pattern, description in (
            (re.compile(r"\bos\.system\s*\("), "os.system"),
            (re.compile(r"\bshell\s*=\s*True\b"), "shell=True"),
            (re.compile(r"\bsubprocess\.STARTUPINFO\b"), "unguarded Windows process API"),
        ):
            for match in pattern.finditer(text):
                violations.append(
                    f"{description} at {source_line(path.relative_to(root), match.start(), text)}"
                )
        if path.resolve() != platform_helper.resolve() and "gradlew.bat" in text:
            violations.append(f"hardcoded Windows Gradle wrapper: {path.relative_to(root)}")

    if violations:
        raise SystemExit("Portability validation failed:\n - " + "\n - ".join(violations))
    print(f"Portability validation passed ({len(files)} project files inspected).")


if __name__ == "__main__":
    main()
