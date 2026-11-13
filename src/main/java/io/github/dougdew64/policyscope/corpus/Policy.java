package io.github.dougdew64.policyscope.corpus;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One customer's policy over time. Each renewal or mid-term change is a new {@link Version}; the
 * date of loss picks the version that decides a claim.
 */
public record Policy(String policyNumber, String namedInsured, String residencePremises, List<Version> versions) {

    public Policy {
        Objects.requireNonNull(policyNumber, "policyNumber");
        versions = List.copyOf(versions);
    }

    /** The version in force on {@code date}, if any. */
    public Optional<Version> versionOn(LocalDate date) {
        return versions.stream().filter(v -> v.inForceOn(date)).findFirst();
    }

    /**
     * One policy period: the documents the policy is built from, and its declarations.
     *
     * @param effective    the first day covered
     * @param expires      the first day <em>not</em> covered; a policy period runs from 12:01 a.m.
     *                     on the effective date to 12:01 a.m. on the expiration date, so a loss on
     *                     the expiration date belongs to the renewal
     * @param forms        ids of the base forms, in order
     * @param endorsements ids of the endorsements, in order
     * @param declarations this period's declarations, as clauses
     */
    public record Version(int version, LocalDate effective, LocalDate expires, List<String> forms,
                          List<String> endorsements, List<Clause> declarations) {

        public Version {
            Objects.requireNonNull(effective, "effective");
            Objects.requireNonNull(expires, "expires");
            forms = List.copyOf(forms);
            endorsements = endorsements == null ? List.of() : List.copyOf(endorsements);
            declarations = List.copyOf(declarations);
        }

        public boolean inForceOn(LocalDate date) {
            return !date.isBefore(effective) && date.isBefore(expires);
        }
    }
}
