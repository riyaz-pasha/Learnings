import contextlib
import io
import shutil
import tempfile
import unittest
from pathlib import Path

import mermaid_cleanup as mc

ARTIFACT = (
    "\\#chatgpt-mermaid-test_{font-family:-apple-system-body;font-size:16px;}"
    "@keyframes dash{to{stroke-dashoffset:0;}}"
    "#chatgpt-mermaid-test_ .node rect{fill:rgb(0, 40, 77);}"
    "#chatgpt-mermaid-test_ .edgeLabel{text-align:center;}"
    "#chatgpt-mermaid-test_ :root{--mermaid-font-family:test;}"
    "ClientNode ANode B"
)


class MermaidCleanupTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def make_markdown(self, content: str) -> Path:
        path = self.root / "chapter.md"
        path.write_text(content, encoding="utf-8")
        return path

    def test_scan_detects_artifact_and_labels(self) -> None:
        path = self.make_markdown(f"# Title\n\nBefore.\n\n{ARTIFACT}\n\nAfter.\n")
        artifacts = mc.find_artifacts(path, context=2)
        self.assertEqual(len(artifacts), 1)
        artifact = artifacts[0]
        self.assertEqual(artifact.line, 5)
        self.assertEqual(artifact.kind, "flowchart")
        self.assertEqual(artifact.labels, "ClientNode ANode B")
        self.assertEqual(artifact.participant_prefix, "")
        self.assertEqual(artifact.section, "Title")
        self.assertTrue(any("Before." in line for line in artifact.context))

    def test_ignore_directive_skips_documentation_examples(self) -> None:
        path = self.make_markdown(
            f"# Docs\n\n<!-- {mc.IGNORE_FILE_MARKER} -->\n\n{ARTIFACT}\n"
        )
        self.assertEqual(mc.find_artifacts(path), [])

        payload = mc.check_payload(path)
        self.assertTrue(payload["ignored"])
        self.assertEqual(payload["errors"], [])

    def test_apply_replaces_only_the_artifact(self) -> None:
        path = self.make_markdown(f"# Title\n\nBefore.\n\n{ARTIFACT}\n\nAfter.\n")
        blocks = self.root / "blocks"
        blocks.mkdir()
        (blocks / "5.mmd").write_text("flowchart LR\n    A --> B\n", encoding="utf-8")

        with contextlib.redirect_stdout(io.StringIO()):
            ok = mc.apply_replacements(path, blocks, validate=False, backup=False)
        self.assertTrue(ok)

        text = path.read_text(encoding="utf-8")
        self.assertNotIn("chatgpt-mermaid", text)
        self.assertIn("```mermaid\nflowchart LR\n    A --> B\n```", text)
        self.assertIn("Before.", text)
        self.assertIn("After.", text)

        payload = mc.check_payload(path)
        self.assertEqual(payload["errors"], [])
        self.assertEqual(payload["artifact_count"], 0)
        self.assertEqual(payload["mermaid_block_count"], 1)

    def test_apply_preserves_crlf_line_endings(self) -> None:
        path = self.root / "crlf.md"
        content = f"# Title\r\n\r\nBefore.\r\n\r\n{ARTIFACT}\r\n\r\nAfter.\r\n"
        path.write_bytes(content.encode("utf-8"))
        blocks = self.root / "blocks"
        blocks.mkdir()
        (blocks / "5.mmd").write_text("flowchart LR\n    A --> B\n", encoding="utf-8")

        with contextlib.redirect_stdout(io.StringIO()):
            ok = mc.apply_replacements(path, blocks, validate=False, backup=False)
        self.assertTrue(ok)

        data = path.read_bytes()
        self.assertNotIn("chatgpt-mermaid", data.decode("utf-8"))
        self.assertTrue(data.startswith(b"# Title\r\n"))
        self.assertIn(b"```mermaid\r\n", data)
        self.assertIn(b"After.\r\n", data)
        self.assertNotIn(b"\n", data.replace(b"\r\n", b""))

    def test_apply_refuses_non_artifact_lines(self) -> None:
        original = "# Title\n\nBefore.\n\nAfter.\n"
        path = self.make_markdown(original)
        blocks = self.root / "blocks"
        blocks.mkdir()
        (blocks / "3.mmd").write_text("flowchart LR\n    A --> B\n", encoding="utf-8")

        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            ok = mc.apply_replacements(path, blocks, validate=False, backup=False)
        self.assertFalse(ok)
        self.assertEqual(path.read_text(encoding="utf-8"), original)

    def test_extract_existing_blocks(self) -> None:
        path = self.make_markdown(
            "# Title\n\n```mermaid\nflowchart LR\n    A --> B\n```\n"
        )
        blocks = mc.extract_mermaid_blocks(path)
        self.assertEqual(len(blocks), 1)
        self.assertEqual(blocks[0].start, 3)
        self.assertIn("A --> B", blocks[0].body)

        output = self.root / "out"
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertTrue(mc.extract_existing_blocks(path, output))
        self.assertTrue((output / "3.mmd").exists())
        self.assertTrue((output / "manifest.json").exists())

    @unittest.skipUnless(shutil.which("mmdc"), "Mermaid CLI is not installed")
    def test_validate_markdown(self) -> None:
        path = self.make_markdown(
            "# Title\n\n```mermaid\nflowchart LR\n    A[A] --> B[B]\n```\n"
        )
        config = mc.puppeteer_config(None)
        self.assertTrue(mc.validate_markdown(path, config=config))


if __name__ == "__main__":
    unittest.main()
