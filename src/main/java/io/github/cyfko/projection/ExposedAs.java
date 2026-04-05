package io.github.cyfko.projection;

import java.lang.annotation.*;

/**
 * Declares a DTO field as queryable, and optionally customizes its symbolic name
 * and the set of operators allowed for selection.
 *
 * <p>This annotation is framework-agnostic. Operator values are plain strings,
 * allowing implementations to define their own operator sets while remaining
 * compatible with the standard operators defined in {@link StandardOp}.
 *
 * <p><strong>Valid targets:</strong>
 * <ul>
 *   <li>A getter method on a {@link Projection} interface returning a
 *       <em>scalar-compatible type</em> (primitives, String, temporal types,
 *       enums, and other value types with no nested queryable structure)</li>
 *   <li>A method in a {@link Provider} class defining a <em>virtual queryable
 *       field</em> — a selection criterion with custom resolution logic that
 *       does not map directly to a single source field</li>
 * </ul>
 *
 * <p><strong>Forbidden target:</strong> A method returning a type annotated
 * with {@link Projection}. Since such a type participates in
 * <em>composed criterion inheritance</em>, applying {@code @ExposedAs} directly
 * on it is a compile-time error. The queryable properties of the nested
 * projection are inherited automatically under the field's logical prefix.
 *
 * <h2>Use Cases</h2>
 *
 * <h3>Basic queryable field</h3>
 * <pre>{@code
 * @ExposedAs(operators = {StandardOp.EQ, StandardOp.MATCHES})
 * String getName();
 * }</pre>
 *
 * <h3>Custom symbolic name</h3>
 * <pre>{@code
 * // The method is named getCreatedAt, but the symbolic name
 * // exposed to consumers is REGISTRATION_DATE
 * @ExposedAs(value = "REGISTRATION_DATE", operators = {StandardOp.GTE, StandardOp.LTE})
 * LocalDateTime getCreatedAt();
 * }</pre>
 *
 * <h3>Mixing standard and custom operators</h3>
 * <pre>{@code
 * @ExposedAs(value = "LOCATION", operators = {StandardOp.EQ, "GEO_WITHIN", "GEO_NEAR"})
 * String getLocation();
 * }</pre>
 *
 * <h3>Internal-only field</h3>
 * <pre>{@code
 * // Queryable by the system internally (e.g. in pipes), but not
 * // exposed to external consumers
 * @ExposedAs(value = "TENANT_ID", operators = {StandardOp.EQ}, exposed = false)
 * String getTenantId();
 * }</pre>
 *
 * @since 3.0.0
 * @author Frank KOSSI
 * @see Projection
 * @see Projected
 * @see StandardOp
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ExposedAs {

    /**
     * The symbolic name exposed in selection criteria.
     * Defaults to a logical name derived from the method name if empty.
     * The casing convention is implementation-defined.
     *
     * <h3>Naming constraints</h3>
     * <p>The double underscore ({@code __}) is a <b>reserved separator</b> used
     * by composed criterion inheritance to mark composition level boundaries.
     * For example, when a {@link Projected} field with {@code as = "SOURCE_SITE"}
     * composes a projection containing a criterion named {@code "SITE_NAME"},
     * the inherited criterion becomes {@code "SOURCE_SITE__SITE_NAME"}.</p>
     *
     * <p>To prevent ambiguity between user-defined names and composition boundaries,
     * the following naming rules are enforced at compile time:</p>
     * <ul>
     *   <li>The value <b>must not</b> contain a double underscore ({@code __})</li>
     *   <li>The value <b>must not</b> start with an underscore ({@code _NAME})</li>
     *   <li>The value <b>must not</b> end with an underscore ({@code NAME_})</li>
     * </ul>
     *
     * <h3>Valid and invalid examples</h3>
     * <pre>{@code
     * @ExposedAs("SITE_NAME")         // ✅ single underscore = word separator
     * @ExposedAs("CREATED_AT")        // ✅
     * @ExposedAs("SOURCE_SITE__NAME") // ❌ double underscore is reserved
     * @ExposedAs("_SITE_NAME")        // ❌ leading underscore
     * @ExposedAs("SITE_NAME_")        // ❌ trailing underscore
     * }</pre>
     */
    String value() default "";

    /**
     * Supported operators for this field.
     *
     * <p>Values are plain strings. Use constants from {@link StandardOp} for
     * portability, or define implementation-specific strings for custom operators.
     *
     * <p>Implementations MUST support all operators listed in {@link StandardOp}.
     * Custom operator strings are passed through to the implementation's
     * extension mechanism without validation by the processor.
     *
     * <h3>Example with standard operators:</h3>
     * <pre>{@code
     * @ExposedAs(value = "NAME", operators = {StandardOp.EQ, StandardOp.MATCHES})
     * String getName();
     * }</pre>
     *
     * <h3>Example with custom operators:</h3>
     * <pre>{@code
     * @ExposedAs(value = "LOCATION", operators = {StandardOp.EQ, "GEO_WITHIN"})
     * String getLocation();
     * }</pre>
     */
    String[] operators() default {};

    /**
     * If {@code false}, this field is defined for internal use only and will
     * not be exposed in the public API.
     *
     * <p>Internal fields can still be used programmatically by pipes or
     * handlers (e.g. for tenant isolation or access control), but are not
     * visible to external consumers.
     */
    boolean exposed() default true;
}
