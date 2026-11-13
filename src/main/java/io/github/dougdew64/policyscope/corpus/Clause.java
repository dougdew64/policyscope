package io.github.dougdew64.policyscope.corpus;

import java.util.List;
import java.util.Objects;

/**
 * The smallest numbered provision of a policy document: the unit that retrieval finds and that the
 * golden set cites.
 *
 * @param id      stable and unique across the corpus: {@code <document>/<local id>} for a form or
 *                endorsement ({@code PS-OP/I.EXCL.3.b}), {@code <policy>/v<n>/<local id>} for
 *                declarations ({@code HO-1003/v2/DEC.deductible})
 * @param heading where the clause sits, outermost first ("Section I - Exclusions > Water")
 * @param text    the wording
 * @param amends  for an endorsement clause: the local ids of the base-form clauses it changes
 *                ({@code I.EXCL.3.b}); local, so one endorsement can amend either base form
 * @param defines for a definition: the term it defines; otherwise null
 */
public record Clause(String id, String heading, String text, List<String> amends, String defines) {

    public Clause {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(heading, () -> "heading of " + id);
        Objects.requireNonNull(text, () -> "text of " + id);
        amends = amends == null ? List.of() : List.copyOf(amends);
    }

    /** The id without its document prefix: {@code PS-OP/I.EXCL.3.b} becomes {@code I.EXCL.3.b}. */
    public String localId() {
        return id.substring(id.lastIndexOf('/') + 1);
    }
}
