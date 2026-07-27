# AGENTS.md

Guidance for AI coding agents (Claude Code, Cursor, Copilot, etc.) working in
this repository. Human contributors should read the
[Guide for New Developers](https://wiki.lyrasis.org/spaces/FF/pages/34654611/Guide+for+New+Developers)
and the [Code Style Guide](https://wiki.lyrasis.org/spaces/FF/pages/34649681/Code+Style+Guide).

> This file is the **canonical, org-wide** agent guidance for the Fedora
> (`fcrepo` / `fcrepo-exts`) projects. Everything above the
> "Repo-specific notes" heading is synced across repositories — edit it in the
> canonical source repo, not in downstream copies. Add anything repo-specific
> below that heading.

---

## The AI Contribution Policy applies to everything you produce here

This project has an **AI Contribution Policy**. If you are an agent, you are a
tool operated by a human contributor who remains fully accountable for the
result. You MUST help that human comply with the policy — not work around it.

Key obligations you must uphold:

- **Human accountability.** The human operator is the sole party responsible for
  the contribution. They must be able to understand and explain every line.
  Prefer changes small and clear enough for a human to fully review. Flag
  anything you generate that would be hard for the operator to justify.
- **No autonomous contributions.** Contributions initiated by autonomous AI
  agents are **prohibited**. A human must direct, review, and submit the work.
  Do not open PRs, push branches, or post review sign-offs on your own
  initiative.
- **PR descriptions are drafts for human review.** You may draft a PR
  description, but present it as a draft for the operator to review, edit, and
  approve — the human owns the final text. Never fabricate the
  disclosure/validation checkboxes on the human's behalf.
- **You cannot approve or sign off.** An AI agent cannot approve or sign off on a
  pull request. Automated AI review comments may be ignored and are never the
  final arbiter of a merge.
- **No hallucinated work.** Do not create issues or PRs for features, bugs, or
  APIs that don't exist. Verify against the actual code before claiming
  something is present, broken, or fixed.
- **No secrets.** Never include or transmit API keys, credentials, or sensitive
  data. Do not paste them into prompts, code, tests, or commits.
- **Licensing & provenance.** Do not introduce code you cannot vouch for the
  provenance of. Do not reproduce large verbatim blocks from external libraries
  with licenses incompatible with this repository (Apache License 2.0). Every
  new source file needs the project license header (enforced by the build).

### Disclosure you must produce

When AI generated or materially assisted a contribution, the human must disclose
it. Help them by producing the required artifacts:

- **Commit trailers.** Add one of the following to commit messages when
  applicable (the commit must be authored under the human's own account):
  - `Assisted-by: <AI tool> [model]` — e.g. `Assisted-by: Claude Code [claude-opus-4-8]`
  - `Generated-by: <AI tool> [model]` — for substantially generated content
- **PR disclosure & validation.** The repo PR template includes an AI Usage
  Disclosure and AI Validation Check section. Remind the operator to fill it in
  honestly; do not pre-tick the boxes for them.

Routine assistive use (grammar, spelling, phrasing) does not require disclosure.
Significant generation does.

### Prohibited — do not do these

- Initiate or submit contributions autonomously (without a directing human).
- Sign off / approve PRs, or present AI review as authoritative.
- Invent features/bugs/APIs based on assumptions about the project.
- Handle or embed secrets, keys, or sensitive data.

Full policy reference:
[Mastodon AI policy](https://github.com/mastodon/.github/blob/main/AI_POLICY.md) ·
[Fedora Project AI contribution policy](https://docs.fedoraproject.org/en-US/council/policy/ai-contribution-policy/).

---

## Build & test

These are Maven projects. All child repos inherit from `org.fcrepo:fcrepo-parent`,
so the toolchain and conventions below are consistent across the org.

- **Java:** 21 (`project.java.source`). Do not use language features newer than
  the configured release, and do not bump the Java version.
- **Build tool:** Maven. Use the wrapper (`./mvnw`) if present; otherwise `mvn`.

Common commands:

```bash
# Full build with tests (what CI runs)
mvn -B -U clean install

# Build without tests (quick compile check)
mvn -B -U clean install -DskipTests

# Run a single module's tests
mvn -B test -pl <module> -am

# Verify style/license before pushing (see below)
mvn -B checkstyle:check license:check
```

Some integration tests run against a real database via the `db-test` profile,
e.g.:

```bash
mvn -B -U -Dfcrepo.db.url="jdbc:postgresql://localhost:5432/fcrepo" \
  -Dfcrepo.db.user="fcrepo-user" -Dfcrepo.db.password="fcrepo-pw" \
  clean install -P db-test
```

Do not commit changes that break `mvn clean install`. If you cannot run the full
build, say so explicitly in your summary to the operator rather than implying it
passed.

## Code style & conventions

- **Checkstyle is enforced.** Config lives in `fcrepo-checkstyle/checkstyle.xml`
  (with `checkstyle-suppressions.xml`). Match the existing style; run
  `mvn checkstyle:check` before proposing a change is done.
- **License headers are enforced.** Every new source file must carry the Apache
  2.0 header from `fcrepo-license/LICENSE_HEADER.txt`. `mvn license:check` will
  fail otherwise; `mvn license:format` can apply it.
- **Match surrounding code.** Follow the naming, structure, and idioms already
  present in the module you are editing rather than introducing new patterns.
- **Tests.** Add or update tests for behavior changes; the project cares about
  coverage (Codecov is wired into CI). Prefer the existing test style
  (JUnit + the module's existing test utilities).

## Commits, branches & pull requests

- **Ticket references.** Work is tracked in JIRA under the `FCREPO` project.
  Reference the ticket in the branch name and/or commit subject where one exists
  (e.g. `FCREPO-4084 Fix OCFL S3 5GB issue`). Keep the PR title to a brief
  (~50 char) description.
- **Commit authorship.** Commits must be authored under the human contributor's
  own account, with AI disclosure trailers added when applicable (see above).
- **PR descriptions are drafts for human review.** Use the repo PR template. You
  may draft the description, but the human reviews, edits, and approves it before
  it is submitted, and fills in the JIRA link, testing instructions, interested
  parties, and the AI disclosure / validation sections.
- **Do not push or open PRs unprompted.** Only push branches or open PRs when
  the human operator explicitly asks you to in this session.

## Token & model usage logging

The project records **token and model usage** alongside contributions so we can
understand the cost and footprint of AI-assisted work. Where possible this is
captured automatically; where not, disclose it manually.

Quantitative usage stats are stored **privately** in the
[`fcrepo/fcrepo-llm-usage`](https://github.com/fcrepo/fcrepo-llm-usage) audit
repo. The public PR carries only the qualitative AI disclosure the policy
requires; token/cost numbers are not published per-PR or per-contributor.

- **Automated (Claude Code).** A `SessionEnd` hook (`.claude/hooks/log_llm_usage.py`)
  parses the session transcript, sums token usage per model, and writes **one
  JSON file per session** to `fcrepo-llm-usage` via your existing `gh` auth. A
  workflow there consolidates records into `usage.csv` and regenerates the
  README totals. Each record holds repo, branch, `session_id`, timestamp, model
  id(s), and input / output / cache token counts — nothing else.
- **Opt-in.** The hook does nothing unless you set `FCREPO_LLM_USAGE=1`. Register
  it in your `.claude/settings.local.json`:

  ```json
  {
    "hooks": {
      "SessionEnd": [
        { "type": "command", "command": "python3 .claude/hooks/log_llm_usage.py" }
      ]
    }
  }
  ```

  It writes using your GitHub credentials — no secret is distributed. If you lack
  write access to the audit repo (e.g. external contributors), it silently
  no-ops; log your usage manually in the PR disclosure instead.
- **Other AI tools.** If your tool doesn't support the hook, record the model
  used and approximate token/usage figures in the PR's AI Usage Disclosure
  section manually.
- **Correlating to a PR.** Because one PR may span multiple sessions, records are
  keyed by **repo + branch**. Reference the branch in the PR so usage can be
  matched; do not paste raw token dumps into the PR body.
- **No secrets in the log.** Records contain only counts and metadata — never
  transcript contents, prompts, code, or credentials.

## Repository layout (this repo)

Multi-module Maven build rooted at `pom.xml`. Notable modules:

- `fcrepo-kernel-api` / `fcrepo-kernel-impl` — core repository model & logic
- `fcrepo-http-api` / `fcrepo-http-commons` — REST/HTTP layer
- `fcrepo-persistence-*` / `fcrepo-persistence-ocfl` — storage (OCFL) layer
- `fcrepo-auth-common` / `fcrepo-auth-webac` — authN/Z (WebAC)
- `fcrepo-search-*`, `fcrepo-stats-*` — search & metrics
- `fcrepo-webapp` — deployable web application
- `fcrepo-parent` — Maven parent POM (build config shared by fcrepo-exts repos)

---

## Repo-specific notes

_Add guidance unique to this repository below. Downstream repos (fcrepo-exts/*)
override module lists, extra build steps, or service dependencies here; the
canonical sections above are synced from the source repo and should not be
edited in place._
