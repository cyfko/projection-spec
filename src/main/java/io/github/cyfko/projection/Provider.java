package io.github.cyfko.projection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a provider class responsible for computed fields or other
 * projection-related functionality.
 *
 * <p>
 * Providers supply the computation logic for {@link Computed} fields. They can
 * be
 * either static utility classes or IoC-managed beans.
 * </p>
 *
 * <h2>Provider Types</h2>
 *
 * <h3>1. Static Provider (default)</h3>
 * <p>
 * When no {@link #bean()} is specified, the provider class is expected to
 * contain
 * static methods:
 * </p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Projection(from = User.class, providers = { @Provider(UserComputations.class) })
 *     public class UserDTO {
 *         @Computed(dependsOn = { "firstName", "lastName" })
 *         private String fullName;
 *     }
 *
 *     public class UserComputations {
 *         public static String getFullName(String firstName, String lastName) {
 *             return firstName + " " + lastName;
 *         }
 *     }
 * }
 * </pre>
 *
 * <h3>2. IoC-Managed Bean Provider</h3>
 * <p>
 * When {@link #bean()} is specified, the provider is resolved from the IoC
 * container
 * (e.g., Spring) and instance methods are used:
 * </p>
 * <pre>{@code
 * &#64;Projection(
 *     from = User.class,
 *     providers = { @Provider(value = DateFormatter.class, bean = "isoDateFormatter") }
 * )
 * public class UserDTO {
 *     @Computed(dependsOn = {"createdAt"})
 *     private String formattedDate;
 * }
 *
 * &#64;Service("isoDateFormatter")
 * public class DateFormatter {
 *     public String getFormattedDate(LocalDateTime createdAt) {
 *         return createdAt.format(DateTimeFormatter.ISO_DATE);
 *     }
 * }
 * }
 * </pre>
 *
 * <h2>Resolution Order</h2>
 * <p>
 * When multiple providers are declared, they are searched in declaration order
 * using a <b>first-match-wins</b> strategy:
 * </p>
 * 
 * <pre>{@code
 * providers = {
 *     &#64;Provider(HighPriorityComputations.class),  // Searched first
 *     &#64;Provider(FallbackComputations.class)       // Searched second
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Frank KOSSI
 * @see Projection#providers()
 * @see Computed
 * @see MethodReference
 */
@Retention(RetentionPolicy.SOURCE)
@Target({}) // Only usable within @Projection
public @interface Provider {

    /**
     * The class containing provider methods.
     *
     * <p>
     * This class will be searched for methods matching the computation
     * requirements:
     * </p>
     * <ul>
     * <li>Methods matching {@code get[FieldName](...)} for computed fields</li>
     * <li>Methods explicitly referenced via {@link MethodReference}</li>
     * </ul>
     *
     * @return the provider class
     */
    Class<?> value();

    /**
     * The bean name for IoC container lookup (optional).
     *
     * <p>
     * Resolution behavior:
     * </p>
     * <table border="1">
     * <tr>
     * <th>Value</th>
     * <th>Resolution Strategy</th>
     * </tr>
     * <tr>
     * <td>{@code ""} (default)</td>
     * <td>Static methods only; no IoC lookup</td>
     * </tr>
     * <tr>
     * <td>Bean name specified</td>
     * <td>Resolve bean by name from IoC container; use instance methods</td>
     * </tr>
     * </table>
     *
     * @return the bean name for IoC lookup, or empty string for static method
     *         resolution
     */
    String bean() default "";
}