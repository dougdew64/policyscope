# Decisions

**Purpose:** the decisions, with their reasons, newest first. **Status:** record (append-only).
**Read when:** before changing anything a decision covers.

| Date | Decision | Why |
|---|---|---|
| 2026-11-13 | **Golden-set inputs are claim records:** policy number, date of loss, loss description, question; some entries also get a vague variant in a claimant's own words. Labels are *covered*, *not covered* and *not settled by the policy*, with the deciding clauses named by stable clause IDs | The records stand in for the unbuilt claims steps 1 and 2 ([CLAIMS.md](CLAIMS.md)); the date of loss is needed to test policy versions; the vague variants measure how much the coverage check depends on the quality of its input |
| 2026-11-13 | **Medium size:** about 6 policies (one with two versions), 400-600 clauses, about 80 questions | Enough for recall numbers to mean something, small enough to review by hand |
| 2026-11-13 | **Homeowners policies only** | Familiar losses, rich in exclusions and endorsements; one vocabulary to learn |
| 2026-11-13 | **The corpus is synthetic** (supersedes "public sample policies if their terms allow" below) | Standard forms are copyrighted; synthetic wording can be published and lets hard cases be planted on purpose |
| 2026-11-13 | **Java 25 and Maven; Apache Lucene as the reference implementation** for BM25 and HNSW | Lucene is what production search systems (Elasticsearch, OpenSearch) are built on, so each from-scratch part is measured against the real thing. If a Java setup for a local embedding model or reranker fails, a small Python helper may serve that one piece |
| 2026-11-13 | **The corpus: policy clauses,** public sample policies if their terms allow publishing, otherwise synthetic (superseded above: synthetic) | A public repository can't publish copyrighted standard forms; synthetic clauses also let the hard cases be planted on purpose |
| 2026-11-13 | **A small model while developing, a larger one for recorded runs,** batched where possible; a $200 cap | Most runs are debugging; only recorded results need the larger model |
| 2026-11-13 | **Built in stages, each ending in a measurement** | The point is to learn how each part works and what it buys, with numbers, not to assemble parts |
