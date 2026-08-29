#!/usr/bin/env python3
"""List Block Entities stored in an Anvil world without starting Minecraft."""

from __future__ import annotations

import argparse
from collections import Counter
import gzip
from io import BytesIO
from pathlib import Path
import struct
import sys
import zlib

from platform_tools import configure_utf8_stdio


class NbtReader:
    def __init__(self, payload: bytes) -> None:
        self.stream = BytesIO(payload)

    def read(self, size: int) -> bytes:
        value = self.stream.read(size)
        if len(value) != size:
            raise EOFError("NBT truncado")
        return value

    def unpack(self, format_: str):
        return struct.unpack(">" + format_, self.read(struct.calcsize(">" + format_)))[0]

    def string(self) -> str:
        return self.read(self.unpack("H")).decode("utf-8", errors="replace")

    def payload(self, tag_type: int):
        if tag_type == 1:
            return self.unpack("b")
        if tag_type == 2:
            return self.unpack("h")
        if tag_type == 3:
            return self.unpack("i")
        if tag_type == 4:
            return self.unpack("q")
        if tag_type == 5:
            return self.unpack("f")
        if tag_type == 6:
            return self.unpack("d")
        if tag_type == 7:
            return self.read(self.unpack("i"))
        if tag_type == 8:
            return self.string()
        if tag_type == 9:
            element_type = self.unpack("B")
            return [self.payload(element_type) for _ in range(self.unpack("i"))]
        if tag_type == 10:
            return self.compound()
        if tag_type == 11:
            return [self.unpack("i") for _ in range(self.unpack("i"))]
        if tag_type == 12:
            return [self.unpack("q") for _ in range(self.unpack("i"))]
        raise ValueError(f"Tipo NBT desconocido: {tag_type}")

    def root(self) -> dict:
        tag_type = self.unpack("B")
        if tag_type != 10:
            raise ValueError(f"La raíz NBT no es un Compound: {tag_type}")
        self.string()
        return self.compound(allow_eof=True)

    def compound(self, allow_eof: bool = False) -> dict:
        compound = {}
        while True:
            marker = self.stream.read(1)
            if not marker:
                if allow_eof:
                    return compound
                raise EOFError("NBT compuesto truncado")
            child_type = marker[0]
            if child_type == 0:
                return compound
            name = self.string()
            compound[name] = self.payload(child_type)


def decompress_chunk(compression: int, payload: bytes) -> bytes:
    if compression & 0x80:
        raise ValueError("Los chunks externos no están soportados")
    if compression == 1:
        return gzip.decompress(payload)
    if compression == 2:
        return zlib.decompress(payload)
    if compression == 3:
        return payload
    raise ValueError(f"Compresión de chunk desconocida: {compression}")


def region_chunks(path: Path):
    with path.open("rb") as handle:
        locations = handle.read(4096)
        if len(locations) != 4096:
            return
        for index in range(1024):
            location = int.from_bytes(locations[index * 4:index * 4 + 4], "big")
            sector = location >> 8
            if sector == 0:
                continue
            handle.seek(sector * 4096)
            length_data = handle.read(4)
            if len(length_data) != 4:
                continue
            length = int.from_bytes(length_data, "big")
            compression_data = handle.read(1)
            payload = handle.read(max(0, length - 1))
            if not compression_data or len(payload) != length - 1:
                continue
            try:
                yield NbtReader(decompress_chunk(compression_data[0], payload)).root()
            except (EOFError, ValueError, zlib.error, gzip.BadGzipFile) as error:
                print(f"# Aviso: {path.name} chunk {index}: {error}", file=sys.stderr)


def block_entities(chunk: dict) -> list[dict]:
    direct = chunk.get("block_entities")
    if isinstance(direct, list):
        return direct
    level = chunk.get("Level")
    if isinstance(level, dict):
        legacy = level.get("TileEntities")
        if isinstance(legacy, list):
            return legacy
    return []


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("world", type=Path)
    parser.add_argument("--namespace", default="trading_cells")
    parser.add_argument("--id", default="", help="Substring required in the Block Entity id")
    parser.add_argument("--summary", action="store_true", help="Group matches by id")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    world = args.world.resolve()
    regions = sorted(world.rglob("region/*.mca"))
    matches = []
    for region in regions:
        dimension = region.parent.parent.relative_to(world).as_posix() or "overworld"
        for chunk in region_chunks(region):
            for block_entity in block_entities(chunk):
                identifier = str(block_entity.get("id", ""))
                if args.namespace and not identifier.startswith(args.namespace + ":"):
                    continue
                if args.id and args.id not in identifier:
                    continue
                matches.append((
                    identifier,
                    int(block_entity.get("x", 0)),
                    int(block_entity.get("y", 0)),
                    int(block_entity.get("z", 0)),
                    dimension,
                ))
    matches.sort(key=lambda entry: (entry[4], entry[0], entry[1], entry[2], entry[3]))
    if args.summary:
        counts = Counter(entry[0] for entry in matches)
        print("id,count,example_x,example_y,example_z,dimension")
        for identifier in sorted(counts):
            example = next(entry for entry in matches if entry[0] == identifier)
            print(
                f"{identifier},{counts[identifier]},{example[1]},{example[2]},"
                f"{example[3]},{example[4]}"
            )
        print(f"# {len(matches)} Block Entities coincidentes en {len(regions)} regiones")
        return
    print("id,x,y,z,dimension")
    for identifier, x, y, z, dimension in matches:
        print(f"{identifier},{x},{y},{z},{dimension}")
    print(f"# {len(matches)} Block Entities coincidentes en {len(regions)} regiones")


if __name__ == "__main__":
    configure_utf8_stdio()
    main()
