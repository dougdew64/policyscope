# Backlog

**Purpose:** ideas and open questions not yet decided, each with why it might matter.
**Status:** live list. **Read when:** looking for what to decide or try next.

An item leaves this list when the owner decides it: the decision goes to
[DECISIONS.md](DECISIONS.md), or the work becomes part of a stage in [PLAN.md](PLAN.md).

## Open questions
| Added | Question | Why it matters |
|---|---|---|
| 2026-11-23 | **Stage 2: which embedding model, and which runtime** to run it in Java (for example ONNX Runtime) | It sets the recall stage 2 can reach, and whether the Python fallback in DECISIONS.md is needed |
| 2026-11-23 | **Stage 2: what is embedded:** a clause's text alone, or its heading and text | Many clauses ("3.b.") mean little without the heading above them |
| 2026-11-23 | **Stage 2: what a search covers:** the whole corpus, or only the clauses in force on the date of loss | Filtering first is what a real system does; searching everything measures how often the wrong policy's clause is found |
| 2026-11-23 | **Stage 2: how recall at 10 is counted:** every deciding clause in the top 10, or the fraction found; and how *not settled* records, with no deciding clauses, are scored | The definition decides what the number means, and must be fixed before the first run |
| 2026-11-23 | **Debugging tests from Run and Debug:** JUnit console-launcher configurations in place of the Maven ones, or both | The Maven route matches `mvn verify`; the direct launch is simpler and faster |

## Reviews pending
| Added | Review | Why it matters |
|---|---|---|
| 2026-11-23 | **The owner's review of the hard cases** in [CORPUS.md](CORPUS.md) | The golden set is the yardstick for every stage; its answers have had one reader ([CORPUS.md](CORPUS.md), known limits) |

## Ideas
| Added | Idea | Why it might matter |
|---|---|---|
| 2026-11-23 | **A wording stress test:** generate many loss scenarios, run the coverage check on each, and report (1) answers the drafter didn't expect, *covered* or *not settled* where *not covered* was meant, and (2) the scenarios whose answer changes between two wordings: the open-perils and named-perils forms, or a form with and without an endorsement. Weighting the changed answers by how often and how large such losses are would put a rough cost on each gap | Ambiguous wording is read against the insurer, so a gap is exposure that the premium never priced; checking wordings against scenarios is done by people today, slowly and not exhaustively. It reuses the coverage check from stage 5 and the golden set's form of record |
