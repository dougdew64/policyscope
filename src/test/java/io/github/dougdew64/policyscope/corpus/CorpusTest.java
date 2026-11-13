package io.github.dougdew64.policyscope.corpus;

import io.github.dougdew64.policyscope.corpus.ClaimRecord.Answer;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The loader and the integrity checks, on a three-clause fixture. */
class CorpusTest {

    private static Corpus fixture() {
        try {
            return Corpus.load(Path.of(CorpusTest.class.getResource("/fixture").toURI()));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void loadsEveryPart() {
        Corpus corpus = fixture();
        assertEquals(2, corpus.documents().size());
        assertEquals(1, corpus.policies().size());
        assertEquals(3, corpus.claims().size());
        assertEquals(6, corpus.allClauses().size(), "3 form + 1 endorsement + 2 declarations");
        assertEquals("residence premises", corpus.allClauses().getFirst().defines());
        assertEquals(List.of("I.EXCL.3.b"), corpus.document("T-END").orElseThrow().clauses().getFirst().amends());
    }

    @Test
    void fixtureHasNoProblems() {
        assertEquals(List.of(), fixture().problems());
    }

    @Test
    void dateOfLossPicksTheVersion() {
        Corpus corpus = fixture();
        List<String> v1 = ids(corpus.clausesInForce("T-1", LocalDate.parse("2025-12-31")));
        List<String> v2 = ids(corpus.clausesInForce("T-1", LocalDate.parse("2026-01-01")));
        assertEquals(List.of("T-1/v1/DEC.deductible", "T-FORM/DEF.1", "T-FORM/I.A.1", "T-FORM/I.EXCL.3.b"), v1);
        assertEquals(List.of("T-1/v2/DEC.deductible", "T-FORM/DEF.1", "T-FORM/I.A.1", "T-FORM/I.EXCL.3.b", "T-END/1"), v2,
                "the expiration date belongs to the renewal");
    }

    @Test
    void noVersionOutsideThePolicyPeriods() {
        Corpus corpus = fixture();
        assertThrows(IllegalArgumentException.class, () -> corpus.clausesInForce("T-1", LocalDate.parse("2027-01-01")));
        assertThrows(IllegalArgumentException.class, () -> corpus.clausesInForce("T-9", LocalDate.parse("2025-06-01")));
    }

    @Test
    void localIdDropsTheDocumentPrefix() {
        assertEquals("I.EXCL.3.b", new Clause("PS-OP/I.EXCL.3.b", "h", "t", null, null).localId());
        assertEquals("DEC.deductible", new Clause("HO-1003/v2/DEC.deductible", "h", "t", null, null).localId());
    }

    @Test
    void reportsADecidingClauseNotInForceOnTheDateOfLoss() {
        // The endorsement clause exists, but only from v2; a v1 loss can't be decided by it.
        Corpus corpus = withClaim(claim("X1", "2025-06-01", Answer.COVERED, List.of("T-END/1"), null));
        assertEquals(List.of("X1: deciding clause T-END/1 is not in T-1 as in force on 2025-06-01"), corpus.problems());
    }

    @Test
    void reportsALossOutsideEveryPolicyPeriod() {
        Corpus corpus = withClaim(claim("X1", "2024-06-01", Answer.NOT_COVERED, List.of("T-FORM/I.EXCL.3.b"), null));
        assertEquals(List.of("X1: no policy T-1 in force on 2024-06-01"), corpus.problems());
    }

    @Test
    void reportsAnAnswerWithoutDecidingClauses() {
        Corpus corpus = withClaim(claim("X1", "2025-06-01", Answer.COVERED, List.of(), null));
        assertEquals(List.of("X1: answered COVERED but names no deciding clauses"), corpus.problems());
    }

    @Test
    void allowsNotSettledWithoutDecidingClauses() {
        Corpus corpus = withClaim(claim("X1", "2025-06-01", Answer.NOT_SETTLED, List.of(), null));
        assertEquals(List.of(), corpus.problems());
    }

    @Test
    void reportsAVariantThatDisagreesWithItsOriginal() {
        Corpus corpus = withClaim(claim("X1", "2026-06-01", Answer.NOT_COVERED, List.of("T-FORM/I.EXCL.3.b"), "T002"));
        assertEquals(List.of("X1: must match T002 in policy, date of loss, answer and deciding clauses"), corpus.problems());
    }

    @Test
    void reportsDuplicateClauseIds() {
        Corpus base = fixture();
        List<PolicyDocument> documents = new ArrayList<>(base.documents());
        documents.add(new PolicyDocument("T-END2", PolicyDocument.Kind.ENDORSEMENT, "Copy", "2025-01",
                List.of(new Clause("T-END/1", "h", "t", null, null))));
        List<String> problems = new Corpus(documents, base.policies(), base.claims()).problems();
        assertTrue(problems.contains("duplicate clause id T-END/1"), problems.toString());
        assertTrue(problems.contains("T-END/1: id must start with T-END2/"), problems.toString());
    }

    @Test
    void reportsAnAmendmentWhoseTargetIsNotInTheForms() {
        Corpus base = fixture();
        List<PolicyDocument> documents = new ArrayList<>(base.documents().subList(0, 1));
        documents.add(new PolicyDocument("T-END", PolicyDocument.Kind.ENDORSEMENT, "Bad", "2025-01",
                List.of(new Clause("T-END/1", "h", "t", List.of("I.EXCL.9"), null))));
        assertEquals(List.of("T-1 v2: T-END/1 amends I.EXCL.9, which its forms don't contain"),
                new Corpus(documents, base.policies(), base.claims()).problems());
    }

    private static Corpus withClaim(ClaimRecord claim) {
        Corpus base = fixture();
        List<ClaimRecord> claims = new ArrayList<>(base.claims());
        claims.add(claim);
        return new Corpus(base.documents(), base.policies(), claims);
    }

    private static ClaimRecord claim(String id, String date, Answer answer, List<String> deciding, String variantOf) {
        return new ClaimRecord(id, "T-1", LocalDate.parse(date), "Water came in.", "Covered?", answer, deciding,
                null, variantOf, "Because.");
    }

    private static List<String> ids(List<Clause> clauses) {
        return clauses.stream().map(Clause::id).toList();
    }
}
