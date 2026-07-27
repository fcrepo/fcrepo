#!/usr/bin/env python3
"""Claude Code SessionEnd hook: record token/model usage for this session into
the fcrepo/fcrepo-llm-usage audit repo (one JSON file per session).

Opt-in and fail-safe by design:
  * Does nothing unless FCREPO_LLM_USAGE=1 is set (explicit consent).
  * Writes via `gh api` using the contributor's existing GitHub auth. If `gh`
    is missing, unauthenticated, or lacks write access (e.g. external
    contributors), it silently no-ops — it never blocks session end and never
    prints secrets.
  * Records only counts + metadata. Never prompts, code, or transcript text.

Registered in .claude/settings.json (or settings.local.json) as:
  {"hooks": {"SessionEnd": [{"type": "command",
     "command": "python3 .claude/hooks/log_llm_usage.py"}]}}
"""
from __future__ import annotations

import base64
import json
import os
import subprocess
import sys
import time
from datetime import datetime, timezone

AUDIT_REPO = os.environ.get("FCREPO_LLM_USAGE_REPO", "fcrepo/fcrepo-llm-usage")


def _bail(msg=None):
    if msg:
        print(msg, file=sys.stderr)
    sys.exit(0)  # never fail the session


def _run(args, cwd=None, stdin=None):
    return subprocess.run(
        args, cwd=cwd, input=stdin, capture_output=True, text=True, timeout=20
    )


def _git(cwd, *args):
    try:
        r = _run(["git", "-C", cwd, *args])
        return r.stdout.strip() if r.returncode == 0 else ""
    except Exception:
        return ""


def _repo_slug(cwd):
    url = _git(cwd, "remote", "get-url", "origin")
    if not url:
        return ""
    url = url.strip()
    if url.startswith("git@"):          # git@github.com:owner/repo.git
        url = url.split(":", 1)[-1]
    elif "://" in url:                  # https://github.com/owner/repo.git
        url = url.split("://", 1)[-1].split("/", 1)[-1]
    if url.endswith(".git"):
        url = url[:-4]
    parts = [p for p in url.split("/") if p]
    return "/".join(parts[-2:]) if len(parts) >= 2 else ""


def _parse_usage(transcript_path):
    """Sum input/output/cache tokens per model across the transcript.

    NOTE: the transcript JSONL is internal to Claude Code and its shape can
    change between versions. We read defensively: usage may sit at
    `.message.usage` (current) or `.usage`, and the model at `.message.model`
    or `.model`. Validate against your installed CLI if numbers look off.
    """
    agg = {}
    try:
        fh = open(transcript_path, encoding="utf-8")
    except OSError:
        return agg
    with fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
            except ValueError:
                continue
            msg = obj.get("message") if isinstance(obj.get("message"), dict) else {}
            usage = msg.get("usage") or obj.get("usage")
            if not isinstance(usage, dict):
                continue
            model = msg.get("model") or obj.get("model") or "unknown"
            d = agg.setdefault(
                model,
                {
                    "input_tokens": 0,
                    "output_tokens": 0,
                    "cache_read_tokens": 0,
                    "cache_creation_tokens": 0,
                },
            )
            d["input_tokens"] += int(usage.get("input_tokens", 0) or 0)
            d["output_tokens"] += int(usage.get("output_tokens", 0) or 0)
            d["cache_read_tokens"] += int(usage.get("cache_read_input_tokens", 0) or 0)
            d["cache_creation_tokens"] += int(
                usage.get("cache_creation_input_tokens", 0) or 0
            )
    # Drop models with no recorded tokens.
    return {m: v for m, v in agg.items() if any(v.values())}


def main():
    if os.environ.get("FCREPO_LLM_USAGE") != "1":
        _bail()  # not opted in

    try:
        event = json.load(sys.stdin)
    except ValueError:
        _bail()

    transcript_path = event.get("transcript_path") or ""
    session_id = event.get("session_id") or "unknown"
    cwd = event.get("cwd") or os.getcwd()

    models = _parse_usage(transcript_path)
    if not models:
        _bail()  # nothing to record

    record = {
        "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "repo": _repo_slug(cwd),
        "branch": _git(cwd, "rev-parse", "--abbrev-ref", "HEAD"),
        "session_id": session_id,
        "tool": "claude-code",
        "models": models,
    }

    content = json.dumps(record, indent=2, sort_keys=True) + "\n"
    fname = f"data/{session_id}-{int(time.time())}.json"
    body = json.dumps(
        {
            "message": f"usage: {record['repo'] or 'unknown'} {session_id}",
            "content": base64.b64encode(content.encode()).decode(),
        }
    )

    try:
        r = _run(
            ["gh", "api", "-X", "PUT", f"repos/{AUDIT_REPO}/contents/{fname}", "--input", "-"],
            stdin=body,
        )
    except FileNotFoundError:
        _bail()  # gh not installed
    except Exception:
        _bail()
    # 401/403 (no auth / no write access) or any error -> silent no-op.
    _bail()


if __name__ == "__main__":
    main()
