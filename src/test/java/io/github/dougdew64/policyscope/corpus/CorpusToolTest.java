package io.github.dougdew64.policyscope.corpus;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The command-line tool's output, on the test fixture. */
class CorpusToolTest {

    private static Corpus fixture() {
        try {
            return Corpus.load(Path.of(CorpusToolTest.class.getResource("/fixture").toURI()));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void summaryCountsAndReportsNoProblems() {
        String summary = CorpusTool.summary(fixture());
        assertTrue(summary.contains("Clauses: 6"), summary);
        assertTrue(summary.contains("Claim records: 2, plus 1 vague variants"), summary);
        assertTrue(summary.contains("Problems: none"), summary);
    }

    @Test
    void explainShowsTheVersionInForceAndTheDecidingText() {
        String explained = CorpusTool.explain(fixture(), "T002");
        assertTrue(explained.contains("In force: v2 (2026-01-01 to 2027-01-01)"), explained);
        assertTrue(explained.contains("T-END/1"), explained);
        assertTrue(explained.contains("Exclusion 3.b. is deleted."), explained);
    }

    @Test
    void explainRejectsAnUnknownClaim() {
        assertThrows(IllegalArgumentException.class, () -> CorpusTool.explain(fixture(), "NOPE"));
    }
}
