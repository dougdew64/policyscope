# CLAUDE.md

**Purpose:** how Claude works in this repository. **Status:** procedure, read at the start of every
session. **Read when:** always, first.

The decisions are in [docs/DECISIONS.md](docs/DECISIONS.md); the stages in
[docs/PLAN.md](docs/PLAN.md). If a `CLAUDE.local.md` is present, read it next.

## This repository is public
Everything committed is published, history included. So:
- Write every document and commit message for a public reader.
- Nothing personal beyond what a professional profile would show; no employers' or other
  companies' names unless they are public sources being cited.
- No secrets, ever: the API key comes from the environment, never from a file here.
- Source documents are committed only when their terms allow it; until then they stay in the
  ignored `data/raw/`.

## Who writes what
The owner decides the design, the direction and what is measured; **Claude writes the code and the
tests** and explains any of it on request. Every commit Claude makes carries the trailer
`Co-Authored-By: Claude`.

## Every session
1. `git pull`; commit and push every commit at once, without asking.
2. Use `date` for today's date.

## Craft
- **Measure, don't assert.** Every claim about recall, speed or cost in a document comes from a run
  recorded under `results/`, with the model, the settings and the date.
- **Explain the mechanism.** The owner learns by asking why: answer with how the code works and
  why, with the code, before what to do next.
- **Cheap while developing:** develop and debug against the small model; use the larger model only
  for runs whose results are recorded, batched where possible. Report each stage's API cost.
- **Every document opens with three lines:** Purpose, Status, Read when. One owner per fact.
- Build and test with `mvn -B verify` before every commit.
