package io.github.dougdew64.policyscope.corpus;

import io.github.dougdew64.policyscope.corpus.ClaimRecord.Answer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A look inside the corpus from the command line.
 *
 * <pre>
 * CorpusTool                 a summary of the corpus and golden set, and any problems
 * CorpusTool GS-024          one claim record: the policy version in force, and its deciding clauses' text
 * CorpusTool GS-024 path     the same, reading the data from another directory (default: data)
 * </pre>
 */
public final class CorpusTool {

    private CorpusTool() {
    }

    public static void main(String[] args) {
        String claimId = args.length > 0 && !args[0].isBlank() ? args[0].trim() : null;
        Path dataDir = Path.of(args.length > 1 ? args[1] : "data");
        Corpus corpus = Corpus.load(dataDir);
        System.out.print(claimId == null ? summary(corpus) : explain(corpus, claimId));
    }

    /** Counts of everything loaded, and the result of the integrity checks. */
    static String summary(Corpus corpus) {
        StringBuilder out = new StringBuilder();
        Map<PolicyDocument.Kind, Long> kinds = count(corpus.documents(), PolicyDocument::kind);
        out.append("Documents: ").append(kinds).append('\n');
        out.append("Clauses: ").append(corpus.allClauses().size()).append('\n');
        for (Policy policy : corpus.policies()) {
            out.append("  ").append(policy.policyNumber()).append(": ");
            out.append(policy.versions().stream()
                    .map(v -> "v" + v.version() + " " + v.effective() + " to " + v.expires() + " " + v.forms() + v.endorsements())
                    .collect(Collectors.joining("; ")));
            out.append('\n');
        }
        List<ClaimRecord> records = corpus.claims().stream().filter(c -> c.variantOf() == null).toList();
        out.append("Claim records: ").append(records.size())
                .append(", plus ").append(corpus.claims().size() - records.size()).append(" vague variants\n");
        out.append("  answers: ").append(count(records, ClaimRecord::answer)).append('\n');
        out.append("  hard cases: ").append(count(records, c -> c.hardCase() == null ? "(none)" : c.hardCase())).append('\n');
        List<String> problems = corpus.problems();
        out.append("Problems: ").append(problems.isEmpty() ? "none" : problems.size()).append('\n');
        problems.forEach(p -> out.append("  ").append(p).append('\n'));
        return out.toString();
    }

    /** One claim record, the policy version its date of loss selects, and the text of its deciding clauses. */
    static String explain(Corpus corpus, String claimId) {
        ClaimRecord claim = corpus.claims().stream().filter(c -> c.id().equals(claimId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no claim record " + claimId));
        StringBuilder out = new StringBuilder();
        out.append(claim.id()).append(claim.variantOf() == null ? "" : " (vague variant of " + claim.variantOf() + ")").append('\n');
        out.append("Policy ").append(claim.policyNumber()).append(", loss on ").append(claim.dateOfLoss()).append('\n');
        out.append("Loss: ").append(claim.lossDescription()).append('\n');
        out.append("Question: ").append(claim.question()).append('\n');

        Policy policy = corpus.policy(claim.policyNumber()).orElseThrow();
        Policy.Version version = policy.versionOn(claim.dateOfLoss()).orElseThrow();
        List<Clause> inForce = corpus.clausesInForce(claim.policyNumber(), claim.dateOfLoss());
        out.append("In force: v").append(version.version()).append(" (").append(version.effective()).append(" to ")
                .append(version.expires()).append("), ").append(version.forms()).append(version.endorsements())
                .append(", ").append(inForce.size()).append(" clauses\n");

        out.append("Answer: ").append(claim.answer()).append(claim.hardCase() == null ? "" : "  [hard case: " + claim.hardCase() + "]").append('\n');
        out.append("Deciding clauses:\n");
        if (claim.decidingClauses().isEmpty()) {
            out.append("  (none: ").append(claim.answer() == Answer.NOT_SETTLED ? "the policy doesn't address it" : "missing").append(")\n");
        }
        Map<String, Clause> byId = inForce.stream().collect(Collectors.toMap(Clause::id, c -> c));
        for (String id : claim.decidingClauses()) {
            Clause clause = byId.get(id);
            out.append("  ").append(id).append("  ").append(clause == null ? "(not in force!)" : clause.heading()).append('\n');
            if (clause != null) {
                out.append("    ").append(clause.text()).append('\n');
            }
        }
        out.append("Reasoning: ").append(claim.reasoning()).append('\n');
        return out.toString();
    }

    private static <T, K> Map<K, Long> count(List<T> items, Function<T, K> key) {
        return items.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.counting()));
    }
}
