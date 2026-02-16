package io.github.cyfko.projection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a DTO class as a projection from a source class, establishing the
 * mapping
 * between source fields and DTO fields, including computed fields.
 *
 * <p>
 * This annotation serves as the central declaration point for the entire
 * projection
 * strategy. It defines the source class and registers all provider classes
 * responsible
 * for calculating derived fields.
 * </p>
 *
 * <p>
 * <b>Note:</b> While commonly used with JPA entities, this specification is not
 * limited
 * to JPA. The source class can be any Java class with accessible fields
 * (entities, domain
 * objects, value objects, POJOs, etc.).
 * </p>
 *
 * <h2>Architecture Overview</h2>
 * <p>
 * The projection system follows these principles:
 * </p>
 * <ul>
 * <li><b>Declarative Mapping:</b> The DTO alone declares all projection
 * requirements</li>
 * <li><b>Source-Centric Dependencies:</b> All field dependencies reference the
 * source class directly</li>
 * <li><b>Separation of Concerns:</b> Computation logic is externalized to
 * dedicated provider classes</li>
 * <li><b>IoC Integration:</b> Supports both static methods and
 * dependency-injected beans</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 *     @Projection(from = User.class, providers = {
 *             @Provider(UserComputations.class),
 *             @Provider(value = DateFormatter.class, bean = "isoDateFormatter")
 *     })
 *     public interface UserDTO {
 *         @Projected(from = "email")
 *         String getEmailAddress();
 *
 *         // Implicitly mapped by name matching
 *         String getFirstName();
 *         String getLastName();
 *
 *         @Computed(dependsOn = { "firstName", "lastName" })
 *         String getFullName();
 *
 *         @Computed(dependsOn = { "createdAt" })
 *         String getFormattedDate();
 *     }
 *
 *     // Static provider
 *     public class UserComputations {
 *         public static String toFullName(String firstName, String lastName) {
 *             return firstName + " " + lastName;
 *         }
 *     }
 *
 *     // Bean-based provider (Hypothetic framework example)
 *     @Service("isoDateFormatter")
 *     public class DateFormatter {
 *         public String toFormattedDate(LocalDateTime createdAt) {
 *             return createdAt.format(DateTimeFormatter.ISO_DATE);
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Provider Resolution Strategy</h2>
 * <p>
 * When resolving a computed field, the system searches for a matching method
 * across all registered providers in declaration order:
 * </p>
 * <ol>
 * <li>Method name must follow the convention: {@code to[FieldName](...)} if the {@link Computed} side does not
 *   explicitly reference them via {@link Computed#computedBy()} property</li>
 * <li>Method parameters must match the types of the {@code dependsOn}
 * fields</li>
 * <li>The first matching method found is used (first-match-wins strategy)</li>
 * <li>If {@code bean} is specified, the provider is resolved from the IoC
 * container;
 * otherwise, a static method is required</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Frank KOSSI
 * @see Provider
 * @see Computed
 * @see Projected
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Projection {

    /**
     * The source class from which this DTO projects.
     *
     * <p>
     * All {@link Projected} and {@link Computed} field dependencies must reference
     * fields that exist in this source class.
     * </p>
     *
     * <p>
     * The source class can be any Java class with accessible fields, including but
     * not limited to: JPA entities, domain objects, value objects, or any POJO.
     * </p>
     *
     * @return the source class to project from
     */
    Class<?> from();

    /**
     * Array of provider classes that supply computation logic for this projection.
     *
     * <p>
     * Providers are evaluated in declaration order using a first-match-wins
     * strategy.
     * </p>
     *
     * <p>
     * <b>Provider Types:</b>
     * </p>
     * <ul>
     * <li><b>Static Providers:</b> Classes containing static methods matching
     * {@code get[FieldName](...)} for {@link Computed} fields</li>
     * <li><b>Bean Providers:</b> IoC-managed beans with instance methods,
     * resolved by the {@link Provider#bean()} name</li>
     * </ul>
     *
     * <p>
     * <b>Example:</b>
     * </p>
     * 
     * <pre>{@code
     * providers = {
     *     @Provider(UserComputations.class),                              // Static
     *     @Provider(value = DateFormatter.class, bean = "isoFormatter")   // Bean
     * }
     * }</pre>
     *
     * 
    <p>
     * <b>Extensibility:</b> While providers are primarily used for resolving
     * {@link Computed} fields, implementations may use them for additional
     * projection-related behaviors. Such extensions are outside the scope of
     * this specification.
     * 
    </p>
     *
     * @return the array of {@link Provider} declarations
     */
    Provider[] providers() default {};
}