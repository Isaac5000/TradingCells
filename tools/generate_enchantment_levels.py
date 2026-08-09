import json
from pathlib import Path


FIRST_CUSTOM_LEVEL = 11
MAXIMUM_ENCHANTMENT_LEVEL = 255
LANGUAGE_FILES = ("en_us.json", "es_es.json")
ROMAN_NUMERALS = (
    (1000, "M"),
    (900, "CM"),
    (500, "D"),
    (400, "CD"),
    (100, "C"),
    (90, "XC"),
    (50, "L"),
    (40, "XL"),
    (10, "X"),
    (9, "IX"),
    (5, "V"),
    (4, "IV"),
    (1, "I"),
)


def roman_numeral(value: int) -> str:
    remaining = value
    parts = []
    for amount, numeral in ROMAN_NUMERALS:
        count, remaining = divmod(remaining, amount)
        parts.extend([numeral] * count)
    return "".join(parts)


def update_language_file(path: Path) -> None:
    translations = json.loads(path.read_text(encoding="utf-8"))
    for level in range(FIRST_CUSTOM_LEVEL, MAXIMUM_ENCHANTMENT_LEVEL + 1):
        translations[f"enchantment.level.{level}"] = roman_numeral(level)
    content = json.dumps(translations, ensure_ascii=False, indent=2) + "\n"
    with path.open("w", encoding="utf-8", newline="\n") as language_file:
        language_file.write(content)


def main() -> None:
    language_directory = (
        Path(__file__).resolve().parents[1]
        / "src"
        / "main"
        / "resources"
        / "assets"
        / "trading_cells"
        / "lang"
    )
    for filename in LANGUAGE_FILES:
        update_language_file(language_directory / filename)


if __name__ == "__main__":
    main()
