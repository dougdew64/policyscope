package io.github.dougdew64.policyscope.corpus;

import java.util.List;
import java.util.Objects;

/**
 * A standard document shared by many policies: a base coverage form or an endorsement. A policy
 * names the documents it is built from; the documents themselves hold no customer details.
 */
public record PolicyDocument(String id, Kind kind, String title, String edition, List<Clause> clauses) {

    public enum Kind { FORM, ENDORSEMENT }

    public PolicyDocument {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, () -> "kind of " + id);
        clauses = List.copyOf(clauses);
    }
}
