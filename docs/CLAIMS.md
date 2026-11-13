# How a claim is processed

**Purpose:** the steps an insurer takes to handle a claim, where AI is used in each, and which step
policyscope builds. **Status:** reference. **Read when:** placing any part of policyscope in the
larger process, or meeting a claims term for the first time.

These are the **claims steps**, the process an insurer runs. They are not the **build stages**,
which are the order policyscope is built in and live in [PLAN.md](PLAN.md). Every build stage
works on step 3.

The steps below are a general outline of property and casualty claims handling. Insurers differ in
how they divide and name them.

## The steps

| Step | What happens | Where AI is used | Its output |
|---|---|---|---|
| **1. First notice of loss (FNOL)** | The policyholder reports a loss by phone, web form, app or through an agent | Pulling structured facts (who, what, when, where) out of free text, transcripts, photos and documents | A claim record: policy number, date of loss, description of the loss |
| **2. Triage** | The claim is routed to the right handler, or fast-tracked if it is simple | Classifying the type of loss; scoring severity and complexity | A route: automated handling, a desk adjuster, a field adjuster, a specialist |
| **3. Coverage check** | Deciding whether the policy, as it stood on the date of loss, covers this loss | Finding the clauses that apply and reading them against the facts | Covered, not covered, or not settled by the policy, with the clauses that decide it |
| **4. Investigation** | Confirming the facts, and checking for fraud | Anomaly detection; comparing the claim against other claims and outside data | Confirmed facts; a fraud score; a referral to a special investigations unit if warranted |
| **5. Valuing and paying** | Estimating the cost of the loss, setting money aside for it, and paying | Estimating damage from photos; predicting the claim's final cost | An estimate, a reserve, a payment |
| **6. Throughout: oversight** | A person reviews uncertain or high-stakes decisions; every decision is recorded with its reasons | Flagging decisions for review; explaining them | An audit trail that answers "why was this claim paid, or denied?" |

## The terms
- **Policy:** the contract. It is made of the **declarations** (who and what is insured, the
  limits, the dates), **coverage forms** (what is covered), **exclusions** (what is not),
  **endorsements** (amendments that add, remove or change coverage), and **defined terms** (words
  the policy gives a specific meaning, often in quotation marks or bold).
- **Date of loss:** when the loss happened. It decides which version of the policy applies, since
  policies are renewed and amended over time.
- **Adjuster:** the person who handles a claim: investigates it, values it and recommends payment
  or denial.
- **Reserve:** money the insurer sets aside for what a claim is expected to cost before it is paid.
- **Subrogation:** after paying, the insurer recovering its cost from whoever caused the loss.

## Where policyscope fits
policyscope builds **step 3, the coverage check**, and only that step. It is where a language model
is most useful, because the answer depends on reading wording, and most dangerous, because a
confident wrong answer means paying a claim that isn't covered or denying one that is. That is why
policyscope checks every citation in code and treats "the policy doesn't settle it" as a correct
answer.

The wording decides real cases. Water that backs up through a drain and water from a flood are
both water damage, yet standard homeowners forms commonly exclude both, and the backup can usually
be added back by an endorsement while flood needs separate insurance. Which clause applies depends
on how the loss is described, which is why step 1's output matters to step 3.

**Steps 1 and 2 are not built.** Hand-written inputs in the golden set stand in for their output,
so step 3 is measured on its own; the exact shape of those inputs is settled in build stage 1. The cost: hand-written descriptions are cleaner
than real ones, so results on them will look better than they would on real claims. **Steps 4 to 6
are not built either;** they are outside what policyscope measures.
