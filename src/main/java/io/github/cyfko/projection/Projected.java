package io.github.cyfko.projection;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a DTO field as directly projected from a corresponding source field.
 *
 * <p><b>Important:</b> It is to the implementation to defines whether this annotation apply both on fields and methods
 * or to choose one side between them.</p>
 *
 * <p>
 * This annotation establishes a straightforward mapping between a source field
 * and
 * a DTO field, optionally allowing the DTO field to have a different name than
 * its
 * source field.
 * </p>
 *
 * <h2>Mapping Behavior</h2>
 *
 * <h3>Automatic Name Matching</h3>
 * <p>
 * When {@link Projected} is not specified, the system assumes the DTO
 * field name exactly matches the source field name:
 * </p>
 * 
 * <pre>{@code
 * public class User {
 *     private String email;
 *     private String phoneNumber;
 * }
 *
 * @Projection(from = User.class)
 * public interface UserDTO {
 *     // Maps to User.email (same name)
 *     public String getEmail();
 *
 *     // Maps to User.phoneNumber (same name)
 *     public String getPhoneNumber();
 * }
 * }</pre>
 *
 * <h3>Explicit Field Mapping</h3>
 * <p>
 * When {@code @Projected} is specified, the DTO field is mapped {@link #from()}
 * the explicitly
 * named source field, enabling field renaming:
 * </p>
 * 
 * <pre>{@code
 *     @Projection(from = User.class)
 *     public class UserDTO {
 *         @Projected(from = "email") // DTO field has different name
 *         public String getEmailAddress();
 *
 *         @Projected(from = "createdAt") // DTO field has different name
 *         private LocalDateTime getRegistrationDate();
 *     }
 * }
 * </pre>
 *
 * <h3>Nested Field Access</h3>
 * <p>
 * The {@link #from()} path supports dot notation for accessing nested object
 * relationships:
 * </p>
 * <pre>{@code
 * public class User {
 *     private Address address;
 * }
 *
 * public class Address {
 *     private String city;
 *     private String country;
 * }
 *
 * @Projection(from = User.class)
 * public class UserDTO {
 *     @Projected(from = "address.city")
 *     private String city;
 *
 *     @Projected(from = "address.country")
 *     private String country;
 * }
 * }
 * </pre>
 *
 * <h2>Composed Criterion Inheritance</h2>
 * <p>
 * When a {@code @Projected} field returns a type annotated with {@link Projection},
 * its queryable properties (declared via {@link ExposedAs}) are automatically
 * inherited by the parent DTO under a logical prefix. The prefix is determined by
 * {@link #as()}, defaulting to the SCREAMING_SNAKE_CASE form of the method name.
 * </p>
 *
 * <h3>Cycle Prevention</h3>
 * <p>
 * If two projections reference each other (A → B → A), a cycle is created.
 * Use {@link #cycleBreak()} to break the cycle on one side:
 * </p>
 * <pre>{@code
 * @Projection(from = Department.class)
 * public interface DepartmentDTO {
 *     @Projected(from = "manager")
 *     EmployeeDTO getManager();
 * }
 *
 * @Projection(from = Employee.class)
 * public interface EmployeeDTO {
 *     @Projected(from = "department", cycleBreak = true)
 *     DepartmentDTO getDepartment();  // Projected but NOT inherited for querying
 * }
 * }</pre>
 *
 * <h2>Comparison with @Computed</h2>
 * <table border="1">
 * <tr>
 * <th>Aspect</th>
 * <th>@Projected</th>
 * <th>@Computed</th>
 * </tr>
 * <tr>
 * <td>Purpose</td>
 * <td>Direct source-to-DTO field mapping</td>
 * <td>Derived value through computation</td>
 * </tr>
 * <tr>
 * <td>Processing</td>
 * <td>Simple field copy/type conversion</td>
 * <td>Invokes provider method with dependencies</td>
 * </tr>
 * <tr>
 * <td>Dependencies</td>
 * <td>Single source field (or path)</td>
 * <td>Multiple source fields as inputs</td>
 * </tr>
 * <tr>
 * <td>Use Case</td>
 * <td>Exposing source data as-is or renamed</td>
 * <td>Formatting, concatenation, calculations</td>
 * </tr>
 * </table>
 *
 * <h2>Type Safety</h2>
 * <p>
 * The annotation processor validates that:
 * </p>
 * <ul>
 * <li>The source field specified in {@link #from()} exists</li>
 * <li>The source field type is compatible with the DTO field type</li>
 * <li>For nested paths, each segment in the path is a valid field or
 * relationship</li>
 * </ul>
 *
 * <h2>Common Patterns</h2>
 *
 * <h3>Selective Field Exposure</h3>
 * <pre>{@code
 * public class User {
 *     private Long id;
 *     private String email;
 *     private String passwordHash;  // Sensitive field
 * }
 *
 * @Projection(from = User.class)
 * public class UserDTO {
 *     @Projected
 *     private Long id;
 *
 *     @Projected
 *     private String email;
 *
 *     // passwordHash is intentionally NOT projected
 * }
 * }
 * </pre>
 *
 * <h3>API Contract Alignment</h3>
 * <pre>{@code
 * @Projection(from = Product.class)
 * public class ProductDTO {
 *     @Projected(from = "sku")
 *     private String productCode;  // API uses different terminology
 *
 *     @Projected(from = "name")
 *     private String displayName;
 * }
 * }
 * </pre>
 *
 * @since 1.0.0
 * @author Frank KOSSI
 * @see Projection
 * @see Computed
 * @see ExposedAs
 * @see Exposure
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Projected {

    /**
     * The path to the source field in the source class.
     *
     * <p>
     * This path can be:
     * </p>
     * <ul>
     * <li><b>Simple field name:</b> {@code "email"} maps to
     * {@code source.getEmail()}</li>
     * <li><b>Nested path:</b> {@code "address.city"} maps to
     * {@code source.getAddress().getCity()}</li>
     * </ul>
     *
     * <h3>Path Resolution</h3>
     * <p>
     * Paths are resolved left-to-right, validating each segment:
     * </p>
     * 
     * <pre>{@code
     * @Projected(from = "department.manager.email")
     * private String managerEmail;
     *
     * // Resolves as:
     * // 1. source.getDepartment() → Department
     * // 2. department.getManager() → Employee
     * // 3. employee.getEmail() → String
     * }</pre>
     *
     * <h3>Null Safety Considerations</h3>
     * <p>
     * For nested paths, intermediate values may be null. The generated projection
     * code
     * should handle null navigation appropriately (implementation-dependent).
     * </p>
     *
     * <h3>When to Use vs. Default Behavior</h3>
     * 
     * <pre>{@code
     *     // Use default (no @Projected) when names match
     *     private String email; // Assumes source also has 'email' field.
     *
     *     // Specify 'from' when:
     *     // 1. Renaming for clarity
     *     @Projected(from = "createdAt")
     *     private LocalDateTime registeredOn;
     *
     *     // 2. Navigating relationships
     *     @Projected(from = "profile.bio")
     *     private String biography;
     *
     *     // 3. Avoiding naming conflicts
     *     @Projected(from = "internalCode")
     *     private String code; // 'code' might mean something else in DTO context
     * }
     * </pre>
     *
     * @return the source field path, or empty string to use the DTO field name
     */
    String from();

    /**
     * Logical prefix used when this field's nested {@link Projection} type
     * participates in composed criterion inheritance.
     *
     * <p>If empty, defaults to the SCREAMING_SNAKE_CASE form of the method name.
     * Has no effect if the return type is not a {@link Projection} type.
     *
     * <p>The prefix is joined to the inherited criterion name using the
     * double underscore ({@code __}) composition separator. For example,
     * {@code as = "CLIENT"} + criterion {@code "NAME"} → {@code "CLIENT__NAME"}.
     *
     * <h3>Naming constraints</h3>
     * <p>The same naming rules that apply to {@link ExposedAs#value()} apply here,
     * because the prefix participates in the same naming namespace:</p>
     * <ul>
     *   <li>Must not contain a double underscore ({@code __}) — reserved as composition separator</li>
     *   <li>Must not start with an underscore ({@code _CLIENT})</li>
     *   <li>Must not end with an underscore ({@code CLIENT_})</li>
     * </ul>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * @Projection(from = Order.class)
     * @Exposure(value = "orders", namespace = "api")
     * public interface OrderDTO {
     *
     *     @Projected(from = "customer", as = "CLIENT")
     *     CustomerDTO getCustomer();
     *     // Inherited criteria: CLIENT__NAME, CLIENT__EMAIL, etc.
     * }
     * }</pre>
     *
     * @return the logical prefix for composed criterion inheritance, or empty
     *         string to use the default SCREAMING_SNAKE_CASE method name
     * @since 3.0.0
     */
    String as() default "";

    /**
     * If {@code true}, excludes this field from composed criterion inheritance,
     * breaking potential cycles.
     *
     * <p>The field remains available for projection (read), but its
     * {@link Projection} type's queryable properties are NOT inherited
     * by the parent DTO.
     *
     * <p>Required when a cycle is detected at compile time:
     * <pre>
     * A → B → A  (cycle: set cycleBreak = true on one side)
     * </pre>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * @Projection(from = Employee.class)
     * public interface EmployeeDTO {
     *
     *     @Projected(from = "department", cycleBreak = true)
     *     DepartmentDTO getDepartment();
     *     // DepartmentDTO's criteria are NOT inherited here
     * }
     * }</pre>
     *
     * @return {@code true} to break criterion composition cycle
     * @since 3.0.0
     */
    boolean cycleBreak() default false;
}