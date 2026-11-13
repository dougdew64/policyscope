package io.github.dougdew64.policyscope.corpus;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * One golden-set entry: a claim record standing in for the output of the unbuilt claims steps 1
 * and 2 (notice of loss and triage), with the correct answer and the clauses that decide it.
 *
 * @param decidingClauses the clause ids that settle the answer; a retriever that misses any of
 *                        them has not given the answering model what it needs
 * @param hardCase        which planted hard case this tests, or null for a straightforward record
 * @param variantOf       for a vague variant: the id of the precise record it restates in a
 *                        claimant's own words; same policy, date, answer and deciding clauses
 * @param reasoning       why the answer is what it is, clause by clause
 */
public record ClaimRecord(String id, String policyNumber, LocalDate dateOfLoss, String lossDescription,
                          String question, Answer answer, List<String> decidingClauses, String hardCase,
                          String variantOf, String reasoning) {

    /** What the policy says about the question. */
    public enum Answer {
        COVERED,
        NOT_COVERED,
        /** The policy wording doesn't settle it: facts are missing, or the question is outside the policy. */
        NOT_SETTLED
    }

    public ClaimRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(answer, () -> "answer of " + id);
        decidingClauses = decidingClauses == null ? List.of() : List.copyOf(decidingClauses);
    }
}
