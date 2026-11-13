# policyscope

**Answering insurance coverage questions from policy wording, with every part of the machinery
built, explained and measured.**

Built in stages, each ending in a measurement; [docs/PLAN.md](docs/PLAN.md) has the stages and
where each one stands.

## What it is
A coverage question ("is water that backs up through a drain covered?") is answered from the
wording of one policy: its coverage forms, its endorsements and its defined terms, as they stood on
the date of loss. policyscope is a retrieval-augmented answering system for such questions, built to
learn and measure how each part of that kind of system works:

1. **A corpus and a golden set:** policy clauses, and questions with the clauses that decide them.
2. **Embeddings and nearest-neighbor search:** brute force, then HNSW written from scratch beside
   [Apache Lucene](https://lucene.apache.org/)'s.
3. **Keyword search and hybrid retrieval:** BM25 from scratch beside Lucene's, rank fusion, a
   reranker.
4. **Answers from a language model** (Claude), with cited quotes checked in code, endorsements and
   definitions added deterministically, and "insufficient" as a correct answer.
5. **Long context against retrieval, and an agent loop**, measured against the fixed pipeline.

Every stage ends in a measurement on the golden set: recall, correctness, latency and cost.

## How it's built
- Java 25, Maven; Lucene as the reference implementation for search.
- Language models through Anthropic's Java SDK: a small, cheap model while developing, a larger one
  for the measured runs.
- **How AI is used:** the owner sets the idea, the design, the direction and what is measured;
  Claude (Anthropic's model, through Claude Code) writes the code and the tests. Every commit Claude
  makes says so (`Co-Authored-By: Claude`).

## Documents
| Document | What it holds |
|---|---|
| [docs/CORPUS.md](docs/CORPUS.md) | The policy corpus and the golden set: what's in them, and why |
| [docs/CLAIMS.md](docs/CLAIMS.md) | How a claim is processed, and the step policyscope builds |
| [docs/PLAN.md](docs/PLAN.md) | The stages, and what each one measures |
| [docs/DECISIONS.md](docs/DECISIONS.md) | The decisions, newest first |
| [CLAUDE.md](CLAUDE.md) | How Claude works in this repository |

## License
MIT ([LICENSE](LICENSE)).
