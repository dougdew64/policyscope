package io.github.dougdew64.policyscope.corpus;

import io.github.dougdew64.policyscope.corpus.ClaimRecord.Answer;
import io.github.dougdew64.policyscope.corpus.PolicyDocument.Kind;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The whole corpus, the policies built from it and the golden set, loaded from a data directory:
 *
 * <pre>
 * corpus/forms/*.json         base coverage forms        ({@link PolicyDocument}, kind FORM)
 * corpus/endorsements/*.json  endorsements               ({@link PolicyDocument}, kind ENDORSEMENT)
 * corpus/policies/*.json      customers' policies        ({@link Policy})
 * golden/claims.json          the golden set             ({@link GoldenSet})
 * </pre>
 *
 * Loading checks only that each file parses into its record; {@link #problems()} checks that the
 * pieces agree with each other.
 */
public final class Corpus {

    /** The golden-set file's top level. */
    public record GoldenSet(List<ClaimRecord> claims) {
        public GoldenSet {
            claims = List.copyOf(claims);
        }
    }

    // Unknown fields fail the load, so a misspelled field name ("decidingClause") is caught at
    // once instead of silently reading as null.
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final List<PolicyDocument> documents;
    private final List<Policy> policies;
    private final List<ClaimRecord> claims;
    private final Map<String, PolicyDocument> documentsById;
    private final Map<String, Policy> policiesByNumber;

    public Corpus(List<PolicyDocument> documents, List<Policy> policies, List<ClaimRecord> claims) {
        this.documents = List.copyOf(documents);
        this.policies = List.copyOf(policies);
        this.claims = List.copyOf(claims);
        this.documentsById = index(this.documents, PolicyDocument::id);
        this.policiesByNumber = index(this.policies, Policy::policyNumber);
    }

    public static Corpus load(Path dataDir) {
        List<PolicyDocument> documents = new ArrayList<>();
        documents.addAll(readAll(dataDir.resolve("corpus/forms"), PolicyDocument.class));
        documents.addAll(readAll(dataDir.resolve("corpus/endorsements"), PolicyDocument.class));
        List<Policy> policies = readAll(dataDir.resolve("corpus/policies"), Policy.class);
        Path golden = dataDir.resolve("golden/claims.json");
        List<ClaimRecord> claims = Files.exists(golden) ? read(golden, GoldenSet.class).claims() : List.of();
        return new Corpus(documents, policies, claims);
    }

    public List<PolicyDocument> documents() { return documents; }

    public List<Policy> policies() { return policies; }

    public List<ClaimRecord> claims() { return claims; }

    public Optional<PolicyDocument> document(String id) { return Optional.ofNullable(documentsById.get(id)); }

    public Optional<Policy> policy(String policyNumber) { return Optional.ofNullable(policiesByNumber.get(policyNumber)); }

    /**
     * Every clause of the policy as it stood on {@code date}: the declarations, then the base
     * forms, then the endorsements. This is the text a coverage check may draw on.
     *
     * @throws IllegalArgumentException if there is no such policy, or no version in force on that date
     */
    public List<Clause> clausesInForce(String policyNumber, LocalDate date) {
        Policy policy = policy(policyNumber)
                .orElseThrow(() -> new IllegalArgumentException("no policy " + policyNumber));
        Policy.Version version = policy.versionOn(date)
                .orElseThrow(() -> new IllegalArgumentException(policyNumber + " not in force on " + date));
        return clausesOf(version);
    }

    /** Every distinct clause in the corpus: each document's clauses once, then every declarations clause. */
    public List<Clause> allClauses() {
        Stream<Clause> fromDocuments = documents.stream().flatMap(d -> d.clauses().stream());
        Stream<Clause> fromDeclarations = policies.stream()
                .flatMap(p -> p.versions().stream())
                .flatMap(v -> v.declarations().stream());
        return Stream.concat(fromDocuments, fromDeclarations).toList();
    }

    /**
     * Everything that makes the corpus or golden set inconsistent, one message each; empty when
     * the data is sound. A test asserts this is empty, so a bad edit to the data fails the build.
     */
    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        checkUnique(problems, "document id", documents.stream().map(PolicyDocument::id).toList());
        checkUnique(problems, "policy number", policies.stream().map(Policy::policyNumber).toList());
        checkUnique(problems, "claim id", claims.stream().map(ClaimRecord::id).toList());
        checkUnique(problems, "clause id", allClauses().stream().map(Clause::id).toList());
        documents.forEach(d -> checkDocument(problems, d));
        policies.forEach(p -> checkPolicy(problems, p));
        Map<String, ClaimRecord> claimsById = index(claims, ClaimRecord::id);
        claims.forEach(c -> checkClaim(problems, c, claimsById));
        return problems;
    }

    private void checkDocument(List<String> problems, PolicyDocument document) {
        if (document.clauses().isEmpty()) {
            problems.add(document.id() + ": no clauses");
        }
        for (Clause clause : document.clauses()) {
            checkClauseText(problems, clause, document.id() + "/");
            if (document.kind() == Kind.FORM && !clause.amends().isEmpty()) {
                problems.add(clause.id() + ": a form clause amends nothing; only endorsements amend");
            }
        }
    }

    private void checkPolicy(List<String> problems, Policy policy) {
        if (policy.versions().isEmpty()) {
            problems.add(policy.policyNumber() + ": no versions");
        }
        Policy.Version previous = null;
        for (Policy.Version version : policy.versions()) {
            String name = policy.policyNumber() + " v" + version.version();
            if (!version.effective().isBefore(version.expires())) {
                problems.add(name + ": effective " + version.effective() + " is not before expires " + version.expires());
            }
            if (previous != null && version.effective().isBefore(previous.expires())) {
                problems.add(name + ": overlaps v" + previous.version());
            }
            previous = version;
            version.declarations().forEach(c -> checkClauseText(problems, c, policy.policyNumber() + "/v" + version.version() + "/"));
            checkReferences(problems, name, version.forms(), Kind.FORM);
            checkReferences(problems, name, version.endorsements(), Kind.ENDORSEMENT);
            checkAmendments(problems, name, version);
        }
    }

    /** Every clause an endorsement amends must exist in one of the base forms it is attached to. */
    private void checkAmendments(List<String> problems, String name, Policy.Version version) {
        Set<String> formLocalIds = new HashSet<>();
        version.forms().stream().map(documentsById::get).filter(d -> d != null)
                .flatMap(d -> d.clauses().stream()).forEach(c -> formLocalIds.add(c.localId()));
        for (String endorsementId : version.endorsements()) {
            PolicyDocument endorsement = documentsById.get(endorsementId);
            if (endorsement == null) {
                continue;
            }
            for (Clause clause : endorsement.clauses()) {
                for (String target : clause.amends()) {
                    if (!formLocalIds.contains(target)) {
                        problems.add(name + ": " + clause.id() + " amends " + target + ", which its forms don't contain");
                    }
                }
            }
        }
    }

    private void checkReferences(List<String> problems, String name, List<String> ids, Kind kind) {
        for (String id : ids) {
            PolicyDocument document = documentsById.get(id);
            if (document == null) {
                problems.add(name + ": no document " + id);
            } else if (document.kind() != kind) {
                problems.add(name + ": " + id + " is a " + document.kind() + ", listed as a " + kind);
            }
        }
    }

    private void checkClaim(List<String> problems, ClaimRecord claim, Map<String, ClaimRecord> claimsById) {
        String id = claim.id();
        if (isBlank(claim.lossDescription()) || isBlank(claim.question()) || isBlank(claim.reasoning())) {
            problems.add(id + ": missing description, question or reasoning");
        }
        if (claim.answer() != Answer.NOT_SETTLED && claim.decidingClauses().isEmpty()) {
            problems.add(id + ": answered " + claim.answer() + " but names no deciding clauses");
        }
        Policy policy = policiesByNumber.get(claim.policyNumber());
        Optional<Policy.Version> version = policy == null ? Optional.empty() : policy.versionOn(claim.dateOfLoss());
        if (version.isEmpty()) {
            problems.add(id + ": no policy " + claim.policyNumber() + " in force on " + claim.dateOfLoss());
        } else {
            Set<String> inForce = new HashSet<>();
            clausesOf(version.get()).forEach(c -> inForce.add(c.id()));
            for (String clauseId : claim.decidingClauses()) {
                if (!inForce.contains(clauseId)) {
                    problems.add(id + ": deciding clause " + clauseId + " is not in " + claim.policyNumber()
                            + " as in force on " + claim.dateOfLoss());
                }
            }
        }
        if (claim.variantOf() != null) {
            ClaimRecord original = claimsById.get(claim.variantOf());
            if (original == null) {
                problems.add(id + ": variant of unknown claim " + claim.variantOf());
            } else if (original.variantOf() != null) {
                problems.add(id + ": variant of a variant (" + original.id() + ")");
            } else if (!original.policyNumber().equals(claim.policyNumber())
                    || !original.dateOfLoss().equals(claim.dateOfLoss())
                    || original.answer() != claim.answer()
                    || !Set.copyOf(original.decidingClauses()).equals(Set.copyOf(claim.decidingClauses()))) {
                problems.add(id + ": must match " + original.id() + " in policy, date of loss, answer and deciding clauses");
            }
        }
    }

    private static void checkClauseText(List<String> problems, Clause clause, String expectedPrefix) {
        if (!clause.id().startsWith(expectedPrefix) || clause.id().length() == expectedPrefix.length()) {
            problems.add(clause.id() + ": id must start with " + expectedPrefix);
        }
        if (isBlank(clause.text())) {
            problems.add(clause.id() + ": no text");
        }
    }

    private List<Clause> clausesOf(Policy.Version version) {
        List<Clause> clauses = new ArrayList<>(version.declarations());
        Stream.concat(version.forms().stream(), version.endorsements().stream())
                .map(documentsById::get)
                .filter(d -> d != null)
                .forEach(d -> clauses.addAll(d.clauses()));
        return clauses;
    }

    private static void checkUnique(List<String> problems, String what, List<String> values) {
        Set<String> seen = new HashSet<>();
        for (String value : values) {
            if (!seen.add(value)) {
                problems.add("duplicate " + what + " " + value);
            }
        }
    }

    /** First one wins; duplicates are reported by {@link #problems()}, not thrown. */
    private static <T> Map<String, T> index(List<T> items, Function<T, String> key) {
        Map<String, T> map = new LinkedHashMap<>();
        items.forEach(item -> map.putIfAbsent(key.apply(item), item));
        return map;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static <T> List<T> readAll(Path dir, Class<T> type) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.toString().endsWith(".json")).sorted().map(f -> read(f, type)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static <T> T read(Path file, Class<T> type) {
        try {
            return JSON.readValue(file.toFile(), type);
        } catch (RuntimeException e) {
            throw new IllegalStateException("cannot read " + file + ": " + e.getMessage(), e);
        }
    }
}
