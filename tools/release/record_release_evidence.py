#!/usr/bin/env python3
"""Record a non-destructive release audit under build/reports/release."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]
GENERATED_PREFIXES = (".gradle/", "build/", "logs/", "run/", "run-server/", "src/generated/")


def command(*args: str) -> str:
    return subprocess.check_output(args, cwd=PROJECT_ROOT, text=True).strip()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def classify(path: str) -> str:
    normalized = path.replace("\\", "/")
    if normalized.startswith(GENERATED_PREFIXES):
        return "generated"
    if normalized.startswith("src/main/java/"):
        return "source_java"
    if normalized.startswith("src/main/resources/"):
        return "source_resources"
    if normalized.startswith(("src/gameTest/", "src/test/")):
        return "tests"
    if normalized.startswith("tools/"):
        return "tools"
    if normalized.startswith("docs/") or normalized.endswith(".md"):
        return "documentation"
    return "project_configuration"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--release-check-passed", action="store_true")
    args = parser.parse_args()

    jar = PROJECT_ROOT / "build/libs/trading_cells-1.0.0.jar"
    if not jar.is_file():
        raise SystemExit(f"Missing release artifact: {jar}")

    status_lines = command("git", "status", "--porcelain=v1").splitlines()
    entries: list[dict[str, str]] = []
    for line in status_lines:
        path = line[3:]
        if " -> " in path:
            path = path.split(" -> ", 1)[1]
        entries.append({"status": line[:2], "path": path, "category": classify(path)})

    category_counts = Counter(entry["category"] for entry in entries)
    report = {
        "captured_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "git_head": command("git", "rev-parse", "HEAD"),
        "worktree": {
            "dirty_entries": len(entries),
            "category_counts": dict(sorted(category_counts.items())),
            "generated_entries": sum(entry["category"] == "generated" for entry in entries),
            "entries": entries,
        },
        "artifact": {
            "path": str(jar.relative_to(PROJECT_ROOT)).replace("\\", "/"),
            "size_bytes": jar.stat().st_size,
            "sha256": sha256(jar),
        },
        "automated_checks": {"release_check": args.release_check_passed},
        "manual_matrix": "pending; see docs/RELEASE_CHECKLIST.md",
    }

    output = args.output if args.output.is_absolute() else PROJECT_ROOT / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    markdown = output.with_suffix(".md")
    lines = [
        "# Release audit 1.0.0",
        "",
        f"- Captured: `{report['captured_at']}`",
        f"- Git HEAD: `{report['git_head']}`",
        f"- Dirty entries preserved: `{len(entries)}`",
        f"- Generated dirty entries: `{report['worktree']['generated_entries']}`",
        f"- JAR size: `{report['artifact']['size_bytes']}` bytes",
        f"- JAR SHA-256: `{report['artifact']['sha256']}`",
        f"- Automated release check: `{'passed' if args.release_check_passed else 'not recorded'}`",
        "- Manual matrix: `pending`",
        "",
        "## Worktree categories",
        "",
    ]
    lines.extend(f"- `{category}`: {count}" for category, count in sorted(category_counts.items()))
    markdown.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(output)
    print(markdown)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
