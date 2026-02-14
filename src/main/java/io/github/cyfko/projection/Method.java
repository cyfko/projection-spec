package io.github.cyfko.projection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Explicit reference to a computation method, overriding the default
 * convention-based
 * resolution.
 *
 * <p>
 * By default, {@link Computed} fields resolve their computation method using
 * the
 * naming convention {@code get[FieldName](...)}. This annotation allows
 * overriding
 * that behavior by specifying an explicit method name and/or target provider
 * class.
 * </p>
 *
 * <h2>Resolution Strategy</h2>
 *
 * <table border="1">
 * <caption>How the computation method is resolved based on specified
 * elements</caption>
 * <tr>
 * <th>{@code type}</th>
 * <th>{@code method}</th>
 * <th>Resolution</th>
 * </tr>
 * <tr>
 * <td>Not set</td>
 * <td>Not set</td>
 * <td>Search all providers for {@code get[FieldName](...)}</td>
 * </tr>
 * <tr>
 * <td>Not set</td>
 * <td>Specified</td>
 * <td>Search all providers for the specified method name</td>
 * </tr>
 * <tr>
 * <td>Specified</td>
 * <td>Not set</td>
 * <td>Search only the specified provider for {@code get[FieldName](...)}</td>
 * </tr>
 * <tr>
 * <td>Specified</td>
 * <td>Specified</td>
 * <td>Use exactly the specified method in the specified provider</td>
 * </tr>
 * </table>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Override Method Name</h3>
 * 
 * <pre>{@code
 * @Computed(dependsOn = { "firstName", "lastName" }, computedBy = @MethodReference(method = "formatDisplayName"))
 * private String fullName;
 * }</pre>
 *
 * <h3>Target Specific Provider</h3>
 * 
 * <pre>{@code
 * @Computed(dependsOn = { "birthDate" }, computedBy = @MethodReference(type = ModernCalculator.class))
 * private Integer age; // Uses ModernCalculator.getAge(...)
 * }</pre>
 *
 * <h3>Fully Explicit Reference</h3>
 * 
 * <pre>{@code
 * @Computed(dependsOn = { "amount",
 *         "currency" }, computedBy = @MethodReference(type = CurrencyUtils.class, method = "convertToUSD"))
 * private BigDecimal amountUSD;
 * }</pre>
 *
 * <h3>Reuse Generic Methods</h3>
 * 
 * <pre>{@code
 * // Same method used for multiple fields
 * @Computed(dependsOn = { "username" }, computedBy = @MethodReference(type = StringUtils.class, method = "uppercase"))
 * private String displayName;
 *
 * @Computed(dependsOn = { "email" }, computedBy = @MethodReference(type = StringUtils.class, method = "lowercase"))
 * private String normalizedEmail;
 * }</pre>
 *
 * @since 1.0.0
 * @author Frank KOSSI
 * @see Computed#computedBy()
 * @see Provider
 */
@Retention(RetentionPolicy.SOURCE)
public @interface MethodReference {

    /**
     * The provider class containing the target method.
     *
     * <p>
     * When specified, the method search is restricted to this class only.
     * The class must be registered in {@link Projection#providers()}, otherwise
     * a compilation error will occur.
     * </p>
     *
     * <p>
     * When not specified (default {@code void.class}), all registered providers
     * are searched in declaration order.
     * </p>
     *
     * @return the target provider class, or {@code void.class} to search all
     *         providers
     */
    Class<?> type() default void.class;

    /**
     * The name of the computation method.
     *
     * <p>
     * When specified, overrides the default naming convention
     * {@code get[FieldName]}.
     * </p>
     *
     * <p>
     * The method must:
     * </p>
     * <ul>
     * <li>Have parameters matching the {@link Computed#dependsOn()} field types in
     * order</li>
     * <li>Return a type compatible with the computed field type</li>
     * <li>Be static (if provider has no bean) or instance (if provider is a
     * bean)</li>
     * </ul>
     *
     * @return the method name, or empty string to use the default convention
     */
    String method() default "";
}