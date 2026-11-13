# The plan

**Purpose:** the stages, what each builds and what each measures. **Status:** live plan.
**Read when:** choosing what to work on next.

Each stage ends with a measurement on the golden set, recorded under `results/`.

| Stage | Builds | Measures | Status |
|---|---|---|---|
| 1 | **The corpus and the golden set:** synthetic homeowners policies modeled on the structure of real ones (declarations, coverage forms, endorsements, defined terms), about 6 policies and 400-600 clauses, with hard cases planted: an endorsement that deletes an exclusion, a defined term that decides the answer, two policy versions with different dates. About 80 claim records, some with vague variants, each with its answer and deciding clauses ([DECISIONS.md](DECISIONS.md)) | The yardstick for every later stage | next |
| 2 | **Embeddings and brute-force search:** a local open embedding model; cosine similarity over every chunk | Recall at 10; time per query | |
| 3 | **HNSW from scratch**, beside Lucene's HNSW | Recall against speed as the search breadth (`ef`) varies; memory; build time | |
| 4 | **BM25 from scratch**, beside Lucene's; reciprocal rank fusion with the embeddings; a reranker | Recall at 10 for each method alone and fused | |
| 5 | **Answers:** the model answers only from the retrieved clauses, as structured output with cited quotes; code checks every citation; endorsements and definitions added before the model sees the clauses; "insufficient" when the policy doesn't settle it | Correctness, citation precision, correct refusals; cost per question | |
| 6 | **Long context against retrieval:** the whole policy in context, on the same questions | Correctness and cost, side by side | |
| 7 | **An agent loop** with read-only tools (find the policy in force, search clauses, look up a definition, list the endorsements of a section) | Where it beats the fixed pipeline, and what it costs | |

## The API budget
Development runs use a small model; recorded runs a larger one, batched where possible. The
estimate for all seven stages is $100-150; the cap is $200. Each stage's actual cost is recorded
with its results.
