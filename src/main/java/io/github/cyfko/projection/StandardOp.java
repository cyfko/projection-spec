package io.github.cyfko.projection;

/**
 * Standard operators that any compliant implementation of projection-spec
 * MUST support.
 *
 * <p>These constants are plain {@code String} values, keeping the specification
 * entirely agnostic of query language, persistence mechanism, and framework.
 * Implementations translate each operator into their own native construct
 * according to their context.
 *
 * <p>Implementations MAY support additional operators beyond this standard set.
 * Custom operators should be documented by the implementation and handled
 * through the implementation's own extension mechanism.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @ExposedAs(value = "NAME", operators = {StandardOp.EQ, StandardOp.MATCHES, StandardOp.IN})
 * String getName();
 *
 * @ExposedAs(value = "AGE", operators = {StandardOp.GTE, StandardOp.LTE, StandardOp.RANGE})
 * Integer getAge();
 * }</pre>
 *
 * @since 3.0.0
 * @author Frank KOSSI
 * @see ExposedAs
 */
public interface StandardOp {

    // ── Equality ────────────────────────────────────────────────────────────

    /** Strict equality: {@code field = value} */
    String EQ = "EQ";

    /** Strict inequality: {@code field != value} */
    String NE = "NE";

    // ── Comparison ──────────────────────────────────────────────────────────

    /** Strictly greater than: {@code field > value} */
    String GT = "GT";

    /** Greater than or equal: {@code field >= value} */
    String GTE = "GTE";

    /** Strictly less than: {@code field < value} */
    String LT = "LT";

    /** Less than or equal: {@code field <= value} */
    String LTE = "LTE";

    // ── Range ───────────────────────────────────────────────────────────────

    /** Inclusive range: {@code field BETWEEN min AND max} */
    String RANGE = "RANGE";

    /** Outside inclusive range: {@code field NOT BETWEEN min AND max} */
    String NOT_RANGE = "NOT_RANGE";

    // ── Pattern ─────────────────────────────────────────────────────────────

    /** Case-insensitive contains: {@code LOWER(field) LIKE '%value%'} */
    String MATCHES = "MATCHES";

    /** Case-insensitive does not contain: {@code LOWER(field) NOT LIKE '%value%'} */
    String NOT_MATCHES = "NOT_MATCHES";

    // ── List ────────────────────────────────────────────────────────────────

    /** Field value is in the provided list: {@code field IN (v1, v2, ...)} */
    String IN = "IN";

    /** Field value is not in the provided list: {@code field NOT IN (v1, v2, ...)} */
    String NOT_IN = "NOT_IN";

    // ── Nullity ─────────────────────────────────────────────────────────────

    /** Field is null: {@code field IS NULL} */
    String IS_NULL = "IS_NULL";

    /** Field is not null: {@code field IS NOT NULL} */
    String NOT_NULL = "NOT_NULL";
}
