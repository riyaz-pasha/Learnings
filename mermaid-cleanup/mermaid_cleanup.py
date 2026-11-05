#!/usr/bin/env python3
"""Find, reconstruct, replace, and validate ChatGPT Mermaid artifacts in Markdown.

The script is intentionally dependency-free (Python standard library only).
Rendering requires the Mermaid CLI (`mmdc`) and a Chromium-based browser.

Typical workflow
----------------
1. python3 mermaid_cleanup.py scan FILE
2. Create one `<line-number>.mmd` file per artifact in a work directory.
3. python3 mermaid_cleanup.py validate-blocks WORKDIR
4. python3 mermaid_cleanup.py apply FILE --blocks WORKDIR --validate
5. python3 mermaid_cleanup.py validate FILE
6. python3 mermaid_cleanup.py check FILE
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from dataclasses import asdict, dataclass
from pathlib import Path

ARTIFACT_MARKER = "chatgpt-mermaid"
IGNORE_FILE_MARKER = "mermaid-cleanup: ignore-file"
LABEL_RE = re.compile(r"--mermaid-font-family:[^}]*\}")
HEADING_RE = re.compile(r"^#{1,6}\s+(.*\S)\s*$")
MAX_NORMAL_LINE = 500
LEFTOVER_PATTERNS = (
    "chatgpt-mermaid",
    "<svg",
    "@keyframes",
    "--mermaid-font-family",
    "data-look=",
    "font-family:-apple-system",
)


# ---------------------------------------------------------------------------
# File / Markdown helpers
# ---------------------------------------------------------------------------


def load_markdown(path: Path) -> tuple[list[str], str, bool]:
    """Return (lines, newline, had_trailing_newline), preserving line endings."""
    text = path.read_bytes().decode("utf-8")
    if "\r\n" in text:
        newline = "\r\n"
    elif "\r" in text:
        newline = "\r"
    else:
        newline = "\n"
    had_trailing = text.endswith(("\n", "\r"))
    lines = text.split(newline)
    if had_trailing and lines and lines[-1] == "":
        lines.pop()
    return lines, newline, had_trailing


def write_markdown(path: Path, lines: list[str], newline: str, had_trailing: bool) -> None:
    text = newline.join(lines)
    if had_trailing:
        text += newline
    with path.open("w", encoding="utf-8", newline="") as handle:
        handle.write(text)


def is_artifact(line: str) -> bool:
    return ARTIFACT_MARKER in line and (
        len(line) > MAX_NORMAL_LINE or "--mermaid-font-family" in line
    )


def artifact_kind(line: str) -> str:
    if any(token in line for token in (".actor", ".messageText", "sequenceNumber")):
        return "sequence"
    if any(token in line for token in (".mindmap", ".mindmap-node")):
        return "mindmap"
    if any(token in line for token in (".timeline", ".timeline-node")):
        return "timeline"
    if any(
        token in line
        for token in (".cluster", ".mermaid-decision", ".node rect", "flowchart-link")
    ):
        return "flowchart"
    if any(token in line for token in ("stateGroup", ".statediagram", "state-start")):
        return "state"
    return "unknown"


def extract_labels(line: str) -> str:
    matches = list(LABEL_RE.finditer(line))
    if not matches:
        return ""
    return line[matches[-1].end() :]


def extract_participant_prefix(line: str) -> str:
    index = line.find(ARTIFACT_MARKER)
    if index < 0:
        return ""
    prefix = line[:index].replace("\\#", "#").strip()
    return prefix.strip("\\_ #").strip()


def section_for(lines: list[str], artifact_index: int) -> str:
    for index in range(artifact_index - 1, -1, -1):
        match = HEADING_RE.match(lines[index])
        if match:
            return match.group(1).strip()
    return ""


def truncate(value: str, limit: int = 240) -> str:
    if len(value) <= limit:
        return value
    return value[: limit - 3] + "..."


def context_for(lines: list[str], artifact_index: int, radius: int) -> list[str]:
    if radius <= 0:
        return []
    start = max(0, artifact_index - radius)
    end = min(len(lines), artifact_index + radius + 1)
    result: list[str] = []
    for index in range(start, end):
        if index == artifact_index:
            result.append(f"{index + 1}: <<<ARTIFACT>>>")
        else:
            result.append(f"{index + 1}: {truncate(lines[index])}")
    return result


# ---------------------------------------------------------------------------
# Artifact and fenced-block scanning
# ---------------------------------------------------------------------------


@dataclass
class ArtifactInfo:
    line: int
    length: int
    kind: str
    labels: str
    participant_prefix: str
    section: str
    context: list[str]


@dataclass
class MermaidBlock:
    index: int
    start: int
    end: int
    body: str


def has_ignore_directive(lines: list[str]) -> bool:
    return any(IGNORE_FILE_MARKER in line for line in lines)


def find_artifacts(path: Path, context: int = 6) -> list[ArtifactInfo]:
    lines, _, _ = load_markdown(path)
    if has_ignore_directive(lines):
        return []
    artifacts: list[ArtifactInfo] = []
    for index, line in enumerate(lines):
        if not is_artifact(line):
            continue
        kind = artifact_kind(line)
        artifacts.append(
            ArtifactInfo(
                line=index + 1,
                length=len(line),
                kind=kind,
                labels=extract_labels(line),
                participant_prefix=(
                    extract_participant_prefix(line) if kind == "sequence" else ""
                ),
                section=section_for(lines, index),
                context=context_for(lines, index, context),
            )
        )
    return artifacts


def extract_mermaid_blocks(path: Path) -> list[MermaidBlock]:
    lines, _, _ = load_markdown(path)
    blocks: list[MermaidBlock] = []
    in_block = False
    start = 0
    body: list[str] = []
    for index, line in enumerate(lines):
        stripped = line.strip()
        if not in_block and stripped == "```mermaid":
            in_block = True
            start = index + 1
            body = []
            continue
        if in_block and stripped == "```":
            blocks.append(
                MermaidBlock(
                    index=len(blocks) + 1,
                    start=start,
                    end=index + 1,
                    body="\n".join(body),
                )
            )
            in_block = False
            body = []
            continue
        if in_block:
            body.append(line)
    return blocks


def scan_payload(path: Path, context: int = 6) -> dict:
    lines, _, _ = load_markdown(path)
    ignored = has_ignore_directive(lines)
    artifacts = find_artifacts(path, context=context)
    blocks = extract_mermaid_blocks(path)
    long_lines = [
        {"line": index + 1, "length": len(line), "artifact": is_artifact(line)}
        for index, line in enumerate(lines)
        if len(line) > MAX_NORMAL_LINE
    ]
    leftovers: list[dict] = []
    if not ignored:
        for index, line in enumerate(lines):
            for pattern in LEFTOVER_PATTERNS:
                if pattern in line:
                    leftovers.append({"line": index + 1, "pattern": pattern})
    fence_lines = sum(1 for line in lines if line.lstrip().startswith("```"))
    return {
        "path": str(path),
        "ignored": ignored,
        "total_lines": len(lines),
        "artifact_count": len(artifacts),
        "mermaid_block_count": len(blocks),
        "fence_lines": fence_lines,
        "fences_balanced": fence_lines % 2 == 0,
        "long_lines": long_lines,
        "leftover_markers": leftovers,
        "artifacts": [asdict(artifact) for artifact in artifacts],
    }


def print_scan(payload: dict) -> None:
    print(f"FILE: {payload['path']}")
    if payload.get("ignored"):
        print(f"ignored: yes ({IGNORE_FILE_MARKER})")
    print(f"lines: {payload['total_lines']}")
    print(f"artifacts: {payload['artifact_count']}")
    print(f"mermaid blocks: {payload['mermaid_block_count']}")
    print(f"fence lines: {payload['fence_lines']} ({'balanced' if payload['fences_balanced'] else 'UNBALANCED'})")
    print(f"CSS/SVG marker occurrences: {len(payload['leftover_markers'])}")
    print(f"lines over {MAX_NORMAL_LINE} chars: {len(payload['long_lines'])}")

    for number, artifact in enumerate(payload["artifacts"], 1):
        print(
            f"\n[{number}] line {artifact['line']} "
            f"({artifact['kind']}, {artifact['length']} chars)"
        )
        if artifact["section"]:
            print(f"    section: {artifact['section']}")
        if artifact["participant_prefix"]:
            print(f"    participant_prefix: {truncate(artifact['participant_prefix'], 400)}")
        print(f"    labels: {truncate(artifact['labels'], 1200)}")
        if artifact["context"]:
            print("    context:")
            for line in artifact["context"]:
                print(f"      {line}")


def check_payload(path: Path) -> dict:
    payload = scan_payload(path, context=0)
    errors: list[str] = []
    warnings: list[str] = []
    if payload["artifact_count"]:
        errors.append(
            f"{payload['artifact_count']} ChatGPT artifact(s) still present "
            f"(lines: {', '.join(str(a['line']) for a in payload['artifacts'])})"
        )
    if payload["leftover_markers"]:
        errors.append(f"{len(payload['leftover_markers'])} leftover CSS/SVG marker(s)")
    if not payload["fences_balanced"]:
        errors.append("Markdown code fences are unbalanced")
    if payload["long_lines"]:
        warnings.append(f"{len(payload['long_lines'])} line(s) over {MAX_NORMAL_LINE} chars")
    payload["errors"] = errors
    payload["warnings"] = warnings
    return payload


def print_check(payload: dict) -> None:
    print(f"FILE: {payload['path']}")
    if payload.get("ignored"):
        print(f"ignored: yes ({IGNORE_FILE_MARKER})")
    print(f"mermaid blocks: {payload['mermaid_block_count']}")
    print(f"fence lines: {payload['fence_lines']} ({'balanced' if payload['fences_balanced'] else 'UNBALANCED'})")
    print(f"ChatGPT artifacts: {payload['artifact_count']}")
    print(f"leftover CSS/SVG markers: {len(payload['leftover_markers'])}")
    print(f"long lines: {len(payload['long_lines'])}")
    if payload["errors"]:
        print("\nERRORS:")
        for error in payload["errors"]:
            print(f"  - {error}")
    if payload["warnings"]:
        print("\nWARNINGS:")
        for warning in payload["warnings"]:
            print(f"  - {warning}")
    if not payload["errors"]:
        print("\nOK: no ChatGPT Mermaid artifacts found")


# ---------------------------------------------------------------------------
# Mermaid CLI / Puppeteer helpers
# ---------------------------------------------------------------------------


def find_mmdc() -> str:
    mmdc = shutil.which("mmdc")
    if not mmdc:
        raise SystemExit(
            "Mermaid CLI not found. Install it with:\n"
            "  npm install -g @mermaid-js/mermaid-cli"
        )
    return mmdc


def find_chrome() -> str | None:
    env_path = os.environ.get("PUPPETEER_EXECUTABLE_PATH") or os.environ.get("CHROME_PATH")
    if env_path and Path(env_path).exists():
        return env_path

    candidates = [
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
        "/Applications/Chromium.app/Contents/MacOS/Chromium",
        "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
        "/usr/bin/google-chrome",
        "/usr/bin/google-chrome-stable",
        "/usr/bin/chromium",
        "/usr/bin/chromium-browser",
        "/snap/bin/chromium",
    ]
    for candidate in candidates:
        if Path(candidate).exists():
            return candidate

    for name in ("google-chrome", "google-chrome-stable", "chromium", "chromium-browser"):
        found = shutil.which(name)
        if found:
            return found
    return None


def puppeteer_config(explicit: str | None) -> Path | None:
    if explicit:
        path = Path(explicit).expanduser()
        if not path.exists():
            raise SystemExit(f"Puppeteer config not found: {path}")
        return path

    chrome = find_chrome()
    if not chrome:
        return None

    config_path = Path(tempfile.gettempdir()) / "mermaid-cleanup" / "puppeteer.json"
    config_path.parent.mkdir(parents=True, exist_ok=True)
    config_path.write_text(
        json.dumps({"executablePath": chrome, "args": ["--no-sandbox"]}, indent=2) + "\n",
        encoding="utf-8",
    )
    return config_path


def render_mermaid(
    source: Path,
    output: Path,
    config: Path | None,
    timeout: int = 180,
) -> tuple[bool, str]:
    mmdc = find_mmdc()
    command = [mmdc, "-i", str(source), "-o", str(output), "--quiet"]
    if config:
        command.extend(["-p", str(config)])
    try:
        result = subprocess.run(
            command,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
    except subprocess.TimeoutExpired:
        return False, f"render timed out after {timeout}s"
    if result.returncode != 0:
        message = (result.stderr or result.stdout or "unknown mmdc error").strip()
        return False, message
    return True, ""


def validate_block_files(
    directory: Path,
    config: Path | None,
    timeout: int = 180,
    keep_output: bool = False,
) -> bool:
    files = sorted(
        directory.glob("*.mmd"),
        key=lambda path: int(path.stem) if path.stem.isdigit() else path.name,
    )
    if not files:
        print(f"No .mmd files found in {directory}", file=sys.stderr)
        return False

    temp_dir: tempfile.TemporaryDirectory[str] | None = None
    if keep_output:
        output_dir = directory / "rendered"
        output_dir.mkdir(exist_ok=True)
    else:
        temp_dir = tempfile.TemporaryDirectory(prefix="mermaid-validate-")
        output_dir = Path(temp_dir.name)

    all_ok = True
    try:
        for source in files:
            target = output_dir / f"{source.stem}.svg"
            ok, error = render_mermaid(source, target, config, timeout)
            if ok:
                print(f"OK   {source.name}")
            else:
                all_ok = False
                print(f"FAIL {source.name}")
                print(indent(error[:4000]))
    finally:
        if temp_dir is not None:
            temp_dir.cleanup()
    return all_ok


def validate_markdown(
    path: Path,
    config: Path | None,
    timeout: int = 180,
    keep_output: bool = False,
) -> bool:
    blocks = extract_mermaid_blocks(path)
    if not blocks:
        print("No fenced mermaid blocks found.")
        return True

    temp_dir: tempfile.TemporaryDirectory[str] | None = None
    if keep_output:
        output_dir = path.parent / f".mermaid-rendered-{path.stem}"
        output_dir.mkdir(exist_ok=True)
    else:
        temp_dir = tempfile.TemporaryDirectory(prefix="mermaid-validate-")
        output_dir = Path(temp_dir.name)

    all_ok = True
    try:
        for block in blocks:
            source = output_dir / f"block-{block.index:02d}.mmd"
            target = output_dir / f"block-{block.index:02d}.svg"
            source.write_text(block.body + "\n", encoding="utf-8")
            ok, error = render_mermaid(source, target, config, timeout)
            if ok:
                print(f"OK   block {block.index:02d} (line {block.start})")
            else:
                all_ok = False
                print(f"FAIL block {block.index:02d} (line {block.start})")
                print(indent(error[:4000]))
    finally:
        if temp_dir is not None:
            temp_dir.cleanup()
    return all_ok


def indent(text: str, spaces: int = 4) -> str:
    prefix = " " * spaces
    return "\n".join(prefix + line for line in text.splitlines())


# ---------------------------------------------------------------------------
# Apply replacements
# ---------------------------------------------------------------------------


def numeric_sort_key(path: Path) -> tuple[int, str]:
    return (int(path.stem), path.name) if path.stem.isdigit() else (sys.maxsize, path.name)


def load_replacement_blocks(path: Path) -> dict[int, str]:
    if path.is_dir():
        blocks: dict[int, str] = {}
        for file in sorted(path.glob("*.mmd"), key=numeric_sort_key):
            if not file.stem.isdigit():
                print(f"Skipping non-numeric block file: {file.name}", file=sys.stderr)
                continue
            blocks[int(file.stem)] = file.read_text(encoding="utf-8")
        return blocks

    if path.suffix.lower() == ".json":
        data = json.loads(path.read_text(encoding="utf-8"))
        if isinstance(data, dict):
            return {int(key): str(value) for key, value in data.items()}
        if isinstance(data, list):
            blocks = {}
            for item in data:
                blocks[int(item["line"])] = str(item["body"])
            return blocks
        raise SystemExit("JSON blocks must be an object of line -> body, or a list of {line, body}")

    raise SystemExit(f"Blocks path must be a directory or a .json file: {path}")


def normalize_body(body: str) -> str:
    body = body.strip("\r\n")
    if body.lstrip().startswith("```"):
        raise ValueError("Replacement body must not contain Markdown code fences")
    if not body.strip():
        raise ValueError("Replacement body is empty")
    return body


def backup_file(path: Path) -> Path:
    backup_dir = Path(tempfile.gettempdir()) / "mermaid-cleanup-backups"
    backup_dir.mkdir(parents=True, exist_ok=True)
    stamp = time.strftime("%Y%m%d-%H%M%S")
    backup = backup_dir / f"{path.stem}-{stamp}{path.suffix}.bak"
    shutil.copy2(path, backup)
    return backup


def validate_single_body(
    body: str,
    config: Path | None,
    timeout: int,
) -> tuple[bool, str]:
    with tempfile.TemporaryDirectory(prefix="mermaid-block-") as temp:
        source = Path(temp) / "candidate.mmd"
        target = Path(temp) / "candidate.svg"
        source.write_text(body + "\n", encoding="utf-8")
        return render_mermaid(source, target, config, timeout)


def apply_replacements(
    path: Path,
    blocks_path: Path,
    config: Path | None = None,
    validate: bool = False,
    timeout: int = 180,
    line_filter: set[int] | None = None,
    backup: bool = True,
) -> bool:
    lines, newline, had_trailing = load_markdown(path)
    replacements = load_replacement_blocks(blocks_path)
    if line_filter:
        replacements = {
            line: body for line, body in replacements.items() if line in line_filter
        }
    if not replacements:
        print("No replacement blocks selected.", file=sys.stderr)
        return False

    errors: list[str] = []
    normalized: dict[int, str] = {}
    for line, body in replacements.items():
        if line < 1 or line > len(lines):
            errors.append(f"line {line}: outside file range 1..{len(lines)}")
            continue
        if not is_artifact(lines[line - 1]):
            errors.append(f"line {line}: target line is not a ChatGPT Mermaid artifact")
            continue
        try:
            normalized[line] = normalize_body(body)
        except ValueError as error:
            errors.append(f"line {line}: {error}")

    if errors:
        print("Refusing to modify file:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return False

    if validate:
        print("Validating replacement blocks before applying...")
        for line in sorted(normalized):
            ok, error = validate_single_body(normalized[line], config, timeout)
            if not ok:
                print(f"FAIL block for line {line}", file=sys.stderr)
                print(indent(error[:4000]), file=sys.stderr)
                return False
            print(f"OK   block for line {line}")

    backup_path = backup_file(path) if backup else None

    for line in sorted(normalized, reverse=True):
        body_lines = normalized[line].splitlines()
        lines[line - 1 : line] = ["```mermaid", *body_lines, "```"]

    write_markdown(path, lines, newline, had_trailing)
    print(f"\nReplaced {len(normalized)} artifact(s) in {path}")
    if backup_path:
        print(f"Backup: {backup_path}")

    remaining = sum(1 for line in lines if is_artifact(line))
    mermaid_count = len(extract_mermaid_blocks(path))
    print(f"Remaining artifacts: {remaining}")
    print(f"Mermaid blocks: {mermaid_count}")
    return remaining == 0


# ---------------------------------------------------------------------------
# Extract existing blocks
# ---------------------------------------------------------------------------


def extract_existing_blocks(path: Path, output_dir: Path) -> bool:
    blocks = extract_mermaid_blocks(path)
    if not blocks:
        print("No fenced mermaid blocks found.", file=sys.stderr)
        return False
    output_dir.mkdir(parents=True, exist_ok=True)
    manifest = []
    for block in blocks:
        file = output_dir / f"{block.start}.mmd"
        file.write_text(block.body + "\n", encoding="utf-8")
        manifest.append(
            {
                "index": block.index,
                "start_line": block.start,
                "end_line": block.end,
                "file": file.name,
            }
        )
    (output_dir / "manifest.json").write_text(
        json.dumps(manifest, indent=2) + "\n", encoding="utf-8"
    )
    print(f"Extracted {len(blocks)} block(s) to {output_dir}")
    return True


# ---------------------------------------------------------------------------
# Doctor
# ---------------------------------------------------------------------------


def run_doctor() -> bool:
    print(f"Python: {sys.version.split()[0]}")
    print(f"Working directory: {Path.cwd()}")

    mmdc = shutil.which("mmdc")
    if not mmdc:
        print("mmdc: NOT FOUND")
        print("Install with: npm install -g @mermaid-js/mermaid-cli")
        return False
    version = subprocess.run(
        [mmdc, "--version"], capture_output=True, text=True
    ).stdout.strip()
    print(f"mmdc: {mmdc} ({version or 'unknown version'})")

    chrome = find_chrome()
    print(f"Chrome: {chrome or 'NOT FOUND'}")
    if not chrome:
        print(
            "Set PUPPETEER_EXECUTABLE_PATH or CHROME_PATH to a Chromium-based browser."
        )

    config = puppeteer_config(None)
    print(f"Puppeteer config: {config or 'not generated (mmdc will use its bundled browser)'}")

    with tempfile.TemporaryDirectory(prefix="mermaid-doctor-") as temp:
        source = Path(temp) / "sample.mmd"
        target = Path(temp) / "sample.svg"
        source.write_text("flowchart LR\n    A[A] --> B[B]\n", encoding="utf-8")
        ok, error = render_mermaid(source, target, config, timeout=120)
        if ok:
            print("Sample render: OK")
        else:
            print("Sample render: FAIL")
            print(indent(error[:4000]))
            print(
                "\nHints:\n"
                "  - Install a Chromium-based browser, or set PUPPETEER_EXECUTABLE_PATH.\n"
                "  - Re-run with --puppeteer-config if you already have a working config.\n"
                "  - On Linux CI, add --no-sandbox via the generated config."
            )
            return False
    return True


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Clean ChatGPT-rendered Mermaid artifacts in Markdown files.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    scan = subparsers.add_parser("scan", help="Report artifacts, labels, and local context")
    scan.add_argument("file", type=Path)
    scan.add_argument("--context", type=int, default=6, help="Context lines around each artifact")
    scan.add_argument("--json", action="store_true", help="Emit machine-readable JSON")

    check = subparsers.add_parser("check", help="Check a cleaned file for remaining problems")
    check.add_argument("file", type=Path)
    check.add_argument("--json", action="store_true", help="Emit machine-readable JSON")

    extract = subparsers.add_parser("extract", help="Extract existing mermaid blocks")
    extract.add_argument("file", type=Path)
    extract.add_argument("--out", type=Path, required=True)

    apply_cmd = subparsers.add_parser("apply", help="Replace artifacts with validated mmd blocks")
    apply_cmd.add_argument("file", type=Path)
    apply_cmd.add_argument(
        "--blocks",
        type=Path,
        required=True,
        help="Directory of <line>.mmd files, or a JSON mapping",
    )
    apply_cmd.add_argument("--validate", action="store_true", help="Render each block before applying")
    apply_cmd.add_argument("--puppeteer-config", type=Path)
    apply_cmd.add_argument("--timeout", type=int, default=180)
    apply_cmd.add_argument("--lines", help="Comma-separated artifact line numbers to apply")
    apply_cmd.add_argument("--no-backup", action="store_true")

    validate = subparsers.add_parser("validate", help="Render every mermaid block in a Markdown file")
    validate.add_argument("file", type=Path)
    validate.add_argument("--puppeteer-config", type=Path)
    validate.add_argument("--timeout", type=int, default=180)
    validate.add_argument("--keep", action="store_true", help="Keep rendered SVG/MMD output")

    validate_blocks = subparsers.add_parser(
        "validate-blocks", help="Render every .mmd file in a directory"
    )
    validate_blocks.add_argument("directory", type=Path)
    validate_blocks.add_argument("--puppeteer-config", type=Path)
    validate_blocks.add_argument("--timeout", type=int, default=180)
    validate_blocks.add_argument("--keep", action="store_true", help="Keep rendered SVG output")

    subparsers.add_parser("doctor", help="Check mmdc, browser, and sample rendering")

    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)

    if args.command == "scan":
        payload = scan_payload(args.file, context=args.context)
        if args.json:
            print(json.dumps(payload, indent=2))
        else:
            print_scan(payload)
        return 0

    if args.command == "check":
        payload = check_payload(args.file)
        if args.json:
            print(json.dumps(payload, indent=2))
        else:
            print_check(payload)
        return 1 if payload["errors"] else 0

    if args.command == "extract":
        return 0 if extract_existing_blocks(args.file, args.out) else 1

    if args.command == "apply":
        line_filter = None
        if args.lines:
            line_filter = {int(value.strip()) for value in args.lines.split(",") if value.strip()}
        config = puppeteer_config(str(args.puppeteer_config) if args.puppeteer_config else None)
        return (
            0
            if apply_replacements(
                args.file,
                args.blocks,
                config=config,
                validate=args.validate,
                timeout=args.timeout,
                line_filter=line_filter,
                backup=not args.no_backup,
            )
            else 1
        )

    if args.command == "validate":
        config = puppeteer_config(str(args.puppeteer_config) if args.puppeteer_config else None)
        return (
            0
            if validate_markdown(
                args.file, config=config, timeout=args.timeout, keep_output=args.keep
            )
            else 1
        )

    if args.command == "validate-blocks":
        config = puppeteer_config(str(args.puppeteer_config) if args.puppeteer_config else None)
        return (
            0
            if validate_block_files(
                args.directory, config=config, timeout=args.timeout, keep_output=args.keep
            )
            else 1
        )

    if args.command == "doctor":
        return 0 if run_doctor() else 1

    return 2


if __name__ == "__main__":
    raise SystemExit(main())
