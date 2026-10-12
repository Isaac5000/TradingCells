"""Export the supplied electricity recording as positional mono Vorbis."""
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tools/assets/storm/electricity.ogg"
OUTPUT = ROOT / "src/main/resources/assets/trading_cells/sounds/electricity.ogg"


if __name__ == "__main__":
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([
        "ffmpeg", "-y", "-loglevel", "error", "-i", str(SOURCE),
        "-ac", "1", "-c:a", "libvorbis", "-q:a", "5", str(OUTPUT),
    ], check=True)
    # A melee swing lasts six ticks; do not play the full two-second beam recording.
    subprocess.run([
        "ffmpeg", "-y", "-loglevel", "error", "-i", str(SOURCE),
        "-t", "0.3", "-af", "afade=t=out:st=0.26:d=0.04", "-ac", "1",
        "-c:a", "libvorbis", "-q:a", "5", str(OUTPUT.with_name("electric_baton_hit.ogg")),
    ], check=True)
