#!/usr/bin/env python3
"""Remap Forge SRG member names in decompiled Java sources to Mojang names."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

SRG_TOKEN = re.compile(r"\b(?:m|f)_\d+_\b")
CLASS_LINE = re.compile(r"^(.*?) -> (.*):$")
METHOD_LINE = re.compile(r"^(?:(\d+):(\d+):)?(.+?)\s+([^\s(]+)\((.*?)\)$")

PRIMITIVES = {
    "void": "V",
    "boolean": "Z",
    "byte": "B",
    "char": "C",
    "short": "S",
    "int": "I",
    "long": "J",
    "float": "F",
    "double": "D",
}


def jvm_type(type_name: str, classes: dict[str, str]) -> str:
    type_name = type_name.strip()
    dimensions = 0
    while type_name.endswith("[]"):
        dimensions += 1
        type_name = type_name[:-2].strip()
    if type_name.endswith("..."):
        dimensions += 1
        type_name = type_name[:-3].strip()
    prefix = "[" * dimensions
    if type_name in PRIMITIVES:
        return prefix + PRIMITIVES[type_name]
    internal = classes.get(type_name, type_name).replace(".", "/")
    return prefix + "L" + internal + ";"


def read_official_mappings(path: Path):
    official_to_obf: dict[str, str] = {}
    members: dict[tuple[str, str, str | None], str] = {}
    lines = path.read_text(encoding="utf-8").splitlines()
    for raw in lines:
        if raw and not raw[0].isspace():
            match = CLASS_LINE.match(raw)
            if match:
                official_to_obf[match.group(1)] = match.group(2).replace(".", "/")

    current_official: str | None = None
    current_obf: str | None = None

    for raw in lines:
        if not raw or raw.startswith("#"):
            continue
        if not raw[0].isspace():
            match = CLASS_LINE.match(raw)
            if not match:
                continue
            current_official, current_obf = match.groups()
            current_obf = current_obf.replace(".", "/")
            continue
        if current_official is None or current_obf is None:
            continue
        left, sep, obf_name = raw.strip().rpartition(" -> ")
        if not sep:
            continue
        method = METHOD_LINE.match(left)
        if method:
            _, _, return_type, official_name, parameter_text = method.groups()
            parameter_types = [] if not parameter_text.strip() else [p.strip() for p in parameter_text.split(",")]
            descriptor = "(" + "".join(jvm_type(p, official_to_obf) for p in parameter_types) + ")"
            descriptor += jvm_type(return_type, official_to_obf)
            members[(current_obf, obf_name, descriptor)] = official_name
        else:
            field = re.match(r"^(.+?)\s+([^\s]+)$", left)
            if field:
                members[(current_obf, obf_name, None)] = field.group(2)

    return official_to_obf, members


def read_srg_mappings(path: Path):
    members: list[tuple[str, str, str, str | None]] = []
    current_obf: str | None = None
    for raw in path.read_text(encoding="utf-8").splitlines():
        if not raw or raw.startswith("tsrg2"):
            continue
        if not raw[0].isspace():
            parts = raw.split()
            current_obf = parts[0] if parts else None
            continue
        if current_obf is None:
            continue
        parts = raw.split()
        if len(parts) >= 4 and parts[1].startswith("("):
            members.append((current_obf, parts[0], parts[2], parts[1]))
        elif len(parts) >= 2:
            members.append((current_obf, parts[0], parts[1], None))
    return members


def build_member_map(tsrg: Path, official: Path):
    _, official_members = read_official_mappings(official)
    result: dict[str, tuple[str, str, str, str | None]] = {}
    conflicts: list[tuple[str, str, str]] = []
    unmatched = 0
    for owner, obf_name, srg_name, descriptor in read_srg_mappings(tsrg):
        if descriptor is None:
            name = official_members.get((owner, obf_name, None))
            if name is None:
                continue
        else:
            name = official_members.get((owner, obf_name, descriptor))
            if name is None:
                unmatched += 1
                continue
        prior = result.get(srg_name)
        row = (name, owner, obf_name, descriptor)
        if prior is not None and prior[0] != name:
            conflicts.append((srg_name, prior[0], name))
        else:
            result[srg_name] = row
    if conflicts:
        sample = "\n".join(f"{key}: {old} / {new}" for key, old, new in conflicts[:20])
        raise RuntimeError(f"Conflicting SRG mappings ({len(conflicts)}):\n{sample}")
    return result, unmatched


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True, help="Directory containing Java sources")
    parser.add_argument("--destination", type=Path, required=True, help="Directory for remapped sources")
    parser.add_argument("--tsrg", type=Path, required=True, help="Forge joined.tsrg")
    parser.add_argument("--official", type=Path, required=True, help="Mojang client_mappings.txt")
    parser.add_argument("--mapping-output", type=Path, required=True, help="Write the joined SRG to official TSV")
    args = parser.parse_args()

    mapping, unmatched_srg = build_member_map(args.tsrg, args.official)
    args.mapping_output.parent.mkdir(parents=True, exist_ok=True)
    with args.mapping_output.open("w", encoding="utf-8", newline="\n") as out:
        out.write("srg\tofficial\tobf_owner\tobf_name\tdescriptor\n")
        for srg, (name, owner, obf, desc) in sorted(mapping.items()):
            out.write(f"{srg}\t{name}\t{owner}\t{obf}\t{desc or ''}\n")

    source_files = sorted(args.source.rglob("*.java"))
    if not source_files:
        raise RuntimeError(f"No Java files found under {args.source}")
    args.destination.mkdir(parents=True, exist_ok=True)
    used: set[str] = set()
    missing: dict[str, int] = {}
    changed_files = 0

    for source_file in source_files:
        text = source_file.read_text(encoding="utf-8")

        def replace(match: re.Match[str]) -> str:
            nonlocal used
            token = match.group(0)
            row = mapping.get(token)
            if row is None:
                missing[token] = missing.get(token, 0) + 1
                return token
            used.add(token)
            return row[0]

        remapped = SRG_TOKEN.sub(replace, text)
        if remapped != text:
            changed_files += 1
        output_file = args.destination / source_file.relative_to(args.source)
        output_file.parent.mkdir(parents=True, exist_ok=True)
        output_file.write_text(remapped, encoding="utf-8", newline="")

    print(f"Source files: {len(source_files)}; changed: {changed_files}")
    print(f"Joined member mappings: {len(mapping)}; used: {len(used)}; unmatched SRG declarations: {unmatched_srg}")
    print(f"Unmapped source tokens: {sum(missing.values())} occurrences across {len(missing)} names")
    for token, count in sorted(missing.items()):
        print(f"UNMAPPED\t{token}\t{count}")
    if missing:
        return 2
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)
