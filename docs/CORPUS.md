# The corpus and the golden set

**Purpose:** what the policy corpus and the golden set contain, how they are laid out, and why.
**Status:** reference. **Read when:** reading or changing anything under `data/`, or interpreting a
recall or correctness number.

The corpus is the text that every later stage searches; the golden set is the yardstick every
stage is scored against. The decisions that shaped both are in [DECISIONS.md](DECISIONS.md); the
claims process they model is in [CLAIMS.md](CLAIMS.md).

## What it is
- **Synthetic homeowners policies,** written for policyscope by Claude in November 2026. They follow
  the usual structure of a US homeowners policy (declarations, a coverage form with property and
  liability sections, exclusions, conditions, defined terms, endorsements) but the wording is
  original, not copied from any insurer's or rating bureau's forms. Because the wording is
  synthetic, the answers in the golden set are right *for this wording*. They are not statements
  about real policies, and nothing here is legal or insurance advice.
- **525 clauses,** each the smallest numbered provision, with a stable id.
- **83 claim records,** plus **15 vague variants**, each with its answer and the clauses that
  decide it.

The counts are checked by `GoldenSetTest`, so this page and the data can't drift apart silently.

## How real policies are assembled, and so how this corpus is
An insurer writes a few standard **forms** and **endorsements** and reuses them. A customer's
policy is a set of **declarations** (who and what is insured, the limits, the deductible, the
dates) plus the forms and endorsements listed in them. The same exclusion therefore appears word for
word in many policies, and a search must find the right clause *in the right policy as it stood on
the date of loss*, not merely the best-matching text.

### Two base forms
| Form | Clauses | How coverage works |
|---|---|---|
| `PS-OP` Open Perils | 204 | The dwelling and other structures are covered against any direct physical loss **unless excluded**; personal property only for 16 named perils |
| `PS-NP` Named Perils | 187 | Dwelling, other structures and personal property are all covered **only for the listed perils** |

The two forms share every section except the perils section. That's how real form families
work, and it plants near-duplicate text throughout the corpus.

### Eight endorsements
| Endorsement | What it changes |
|---|---|
| `PS-WB` Water Backup | Covers water backing up through drains or overflowing a sump, deleting exclusion 3.b; flood stays excluded |
| `PS-EB` Equipment Breakdown | Covers sudden mechanical and electrical breakdown of home systems and appliances, which the base form excludes |
| `PS-SP` Scheduled Personal Property | Covers listed jewelry for agreed amounts, including mysterious disappearance, free of the $1,500 theft limit |
| `PS-HB` Home Business | Covers one declared home business: its structure, property, income and liability |
| `PS-ID` Identity Fraud Expense | Pays the expenses of recovering from identity fraud, but not the money stolen |
| `PS-OL` Ordinance or Law | Raises the coverage for building-code upgrade costs from 10% to a declared percentage |
| `PS-RC` Personal Property Replacement Cost | Settles personal property at replacement cost instead of depreciated value, except antiques and similar |
| `PS-SL` Service Line | Covers underground utility lines on the property against wear, roots and breakage |

### Six policies
| Policy | Built from | What it tests |
|---|---|---|
| HO-1001 | `PS-OP` alone | The baseline |
| HO-1002 | `PS-OP` + `PS-WB` + `PS-RC` | An endorsement that deletes an exclusion: the drain backup that HO-1001 excludes is covered here |
| HO-1003 | v1: `PS-OP` + `PS-ID`; v2 (renewal on 2026-05-01): adds `PS-EB`, deductible $1,000 → $2,500 | Two versions: the date of loss decides which applies |
| HO-1004 | `PS-NP` + `PS-OL` | Named perils: losses the open-perils form would cover are not covered here |
| HO-1005 | `PS-OP` + `PS-SP` + `PS-RC` + `PS-SL` | Special limits, and the scheduled jewelry that escapes them |
| HO-1006 | `PS-NP` + `PS-HB` + `PS-ID`, two-family dwelling with a tenant | Defined terms decide it: "business", "home business", "insured" |

The people, addresses and lenders in the declarations are invented.

## The golden set
Each record is a claim record (the output of claims steps 1 and 2, which policyscope does not
build): a policy number, a date of loss, a description of the loss, a question, and then the
**answer**, the **deciding clauses**, and the **reasoning**, clause by clause.

**Answers** are one of three:
- `COVERED` (36 records);
- `NOT_COVERED` (33);
- `NOT_SETTLED` (14): the policy wording does not settle it, because a fact is missing (where did
  the water come from?) or because the question is outside the policy (will my premium go up?).
  Saying so is a correct answer, and later stages are scored on it.

A `COVERED` answer can still be limited: a covered theft of jewelry pays at most $1,500. The
reasoning states any such limit.

**Deciding clauses** are the clauses an answer needs, by id. Retrieval is measured by whether it
finds them; answering is measured by whether it cites them.

### The planted hard cases
50 of the 83 records are labeled with the hard case they test:

| Hard case | Records | What makes it hard | Example |
|---|---|---|---|
| `endorsement-changes-form` | 13 | The base form gives one answer; an endorsement on this policy reverses it | Drain backup: not covered on HO-1001 (GS-003), covered on HO-1002 (GS-024) |
| `endorsement-scope` | 13 | An endorsement is present but doesn't reach this loss | Flood water that comes up a drain stays excluded under water backup (GS-025) |
| `policy-version` | 6 | The date of loss picks the version | The same furnace failure: not covered in v1 (GS-035), covered in v2 (GS-036) |
| `defined-term` | 6 | A defined term decides the answer | A teenager's lawn mowing earns under $3,000, so it isn't a "business" (GS-013) |
| `named-vs-open-perils` | 5 | The same loss under the two forms | A hole knocked in a wall: covered on HO-1001 (GS-019), not on HO-1004 (GS-048) |
| `special-limit` | 4 | Covered, but capped, or freed of a cap | Unscheduled earrings capped at $1,500; the scheduled watch is not (GS-061, GS-062) |
| `ensuing-loss` | 3 | An excluded cause leads to a covered result | Earthquake excluded, but the fire it starts is covered (GS-007) |

### Vague variants
15 records are restated as a claimant would say them: no policy vocabulary, same facts. "Dirty water came
gushing up out of the hole in my basement floor" instead of "water backed up through the floor
drain". The correct answer is unchanged, so comparing a system's score on a variant with its score
on the original measures how much it depends on the description using the policy's own words. A
variant names its original in `variantOf`.

## Files and format
```
data/corpus/forms/*.json         base forms            id, kind FORM, title, edition, clauses
data/corpus/endorsements/*.json  endorsements          id, kind ENDORSEMENT, title, edition, clauses
data/corpus/policies/*.json      policies              policyNumber, namedInsured, residencePremises, versions
data/golden/claims.json          the golden set        claims
```
- **A clause** is `{id, heading, text}`, plus `defines` on a definition and `amends` on an
  endorsement clause that changes a base-form clause.
- **Clause ids** are `<document>/<local id>` for forms and endorsements (`PS-OP/I.EXCL.3.b`: Section I,
  Exclusions, paragraph 3.b), and `<policy>/v<n>/DEC.<item>` for declarations
  (`HO-1003/v2/DEC.deductible`).
- **`amends`** uses local ids (`I.EXCL.3.b`), so one endorsement can amend either base form. It
  records in data what the endorsement's text says in words. Build stage 5 uses it to put each
  endorsement beside the clause it changes before the model reads them.
- **A policy version** runs from its `effective` date up to, but not including, its `expires` date,
  as a policy period runs from 12:01 a.m. on one date to 12:01 a.m. on the other.

The Java loader is `Corpus` in `io.github.dougdew64.policyscope.corpus`. `Corpus.problems()` lists
every inconsistency: duplicate ids, a deciding clause not in force on the date of loss, an
amendment whose target isn't in the policy's forms, a variant that disagrees with its original.
`GoldenSetTest` asserts the list is empty, so `mvn verify` fails on a bad edit to the data.

## Known limits
- **Written by a Claude model, and later answered by Claude models.** The questions may suit the way
  these models read. The owner reviews the hard cases; nothing else checks for this bias.
- **One reviewer of the answers.** The deciding clauses and answers were written once and checked by
  code for consistency, not by a second insurance reader for correctness.
- **Small for search speed.** 525 clauses is enough to measure recall, but too few to make an
  approximate nearest-neighbor index faster than exhaustive search. Build stage 3 will need a larger
  vector set.
