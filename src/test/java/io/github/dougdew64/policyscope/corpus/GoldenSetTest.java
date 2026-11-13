package io.github.dougdew64.policyscope.corpus;

import io.github.dougdew64.policyscope.corpus.ClaimRecord.Answer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real corpus and golden set under {@code data/}: consistent, and shaped as docs/CORPUS.md
 * describes. A bad edit to the data fails the build here.
 */
class GoldenSetTest {

    /** The planted hard cases; docs/CORPUS.md explains each. */
    static final Set<String> HARD_CASES = Set.of(
            "endorsement-changes-form", "endorsement-scope", "policy-version", "defined-term",
            "named-vs-open-perils", "special-limit", "ensuing-loss");

    private static Corpus corpus;

    @BeforeAll
    static void load() {
        corpus = Corpus.load(Path.of("data"));
    }

    @Test
    void hasNoProblems() {
        assertEquals(List.of(), corpus.problems());
    }

    @Test
    void hasTheDecidedSize() {
        assertEquals(6, corpus.policies().size(), "policies");
        assertEquals(2, corpus.documents().stream().filter(d -> d.kind() == PolicyDocument.Kind.FORM).count(), "forms");
        assertEquals(8, corpus.documents().stream().filter(d -> d.kind() == PolicyDocument.Kind.ENDORSEMENT).count(), "endorsements");
        int clauses = corpus.allClauses().size();
        assertTrue(clauses >= 400 && clauses <= 600, "clauses: " + clauses);
        long records = corpus.claims().stream().filter(c -> c.variantOf() == null).count();
        assertTrue(records >= 70 && records <= 90, "claim records: " + records);
    }

    @Test
    void onePolicyHasTwoVersions() {
        assertEquals(List.of("HO-1003"), corpus.policies().stream()
                .filter(p -> p.versions().size() == 2).map(Policy::policyNumber).toList());
    }

    @Test
    void everyAnswerAndHardCaseIsRepresented() {
        assertEquals(EnumSet.allOf(Answer.class),
                corpus.claims().stream().map(ClaimRecord::answer).collect(Collectors.toCollection(() -> EnumSet.noneOf(Answer.class))));
        Set<String> used = corpus.claims().stream().map(ClaimRecord::hardCase).filter(h -> h != null).collect(Collectors.toSet());
        assertEquals(HARD_CASES, used);
    }

    @Test
    void variantsAreWorded() {
        // A variant restates its record's facts in other words; a copy would measure nothing.
        for (ClaimRecord variant : corpus.claims()) {
            if (variant.variantOf() != null) {
                ClaimRecord original = corpus.claims().stream().filter(c -> c.id().equals(variant.variantOf())).findFirst().orElseThrow();
                assertTrue(!variant.lossDescription().equals(original.lossDescription()), variant.id() + " repeats " + original.id());
            }
        }
    }

    @Test
    void noLossIsInTheFuture() {
        LocalDate built = LocalDate.parse("2026-11-13");
        corpus.claims().forEach(c -> assertTrue(!c.dateOfLoss().isAfter(built), c.id() + " " + c.dateOfLoss()));
    }
}
