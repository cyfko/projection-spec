package io.github.cyfko.projection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a DTO interface or class as a projection from a source entity or domain object,
 * establishing the complete mapping strategy between source fields and DTO fields.
 *
 * <p>This annotation serves as the central orchestration point for the entire projection system.
 * It defines the source class, registers provider classes for computed fields, and enables
 * the annotation processor or runtime framework to generate the necessary projection code.</p>
 *
 * <p><b>Framework Agnostic:</b> While commonly used with JPA entities, this specification
 * is not tied to any persistence framework. The source class can be any Java object with
 * accessible fields: JPA entities, MongoDB documents, domain objects, value objects, POJOs,
 * or even other DTOs.</p>
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><b>Source class:</b> The origin entity/object being projected (specified via {@link #from()})</li>
 *   <li><b>DTO:</b> The interface or class annotated with {@code @Projection}</li>
 *   <li><b>Projected fields:</b> Direct mappings from source (via {@link Projected} or name matching)</li>
 *   <li><b>Computed fields:</b> Derived values calculated from source fields (via {@link Computed})</li>
 *   <li><b>Providers:</b> Classes containing computation and transformation logic (via {@link #providers()})</li>
 * </ul>
 *
 * <h2>Architecture Principles</h2>
 *
 * <h3>1. Declarative Mapping</h3>
 * <p>The DTO alone declares all projection requirements. No external configuration files
 * or separate mapping classes are needed:</p>
 * <pre>{@code
 * @Projection(from = User.class)
 * public interface UserDTO {
 *     // All mapping declared here
 * }
 * }</pre>
 *
 * <h3>2. Source-Centric Dependencies</h3>
 * <p>All field dependencies reference the source class directly. Computed fields depend
 * on source fields, not on other computed fields:</p>
 * <pre>{@code
 * @Computed(dependsOn = {"firstName", "lastName"})  // Source fields
 * String getFullName();
 *
 * // ❌ NOT: dependsOn = {"fullName"}  // Cannot depend on computed fields
 * }</pre>
 *
 * <h3>3. Separation of Concerns</h3>
 * <p>Computation logic is externalized to dedicated provider classes, keeping DTOs
 * clean and focused on structure:</p>
 * <pre>{@code
 * // DTO: Structure only
 * @Projection(from = User.class, providers = { @Provider(UserComputations.class) })
 * public interface UserDTO {
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();
 * }
 *
 * // Provider: Logic only
 * public class UserComputations {
 *     public static String toFullName(String first, String last) {
 *         return first + " " + last;
 *     }
 * }
 * }</pre>
 *
 * <h3>4. IoC Integration</h3>
 * <p>Supports both static utility methods and IoC-managed beans, enabling providers
 * to access application context and dependencies when needed.</p>
 *
 * <h2>Field Mapping Strategies</h2>
 *
 * <h3>Strategy 1: Implicit Name Matching (Default)</h3>
 * <p>When no {@link Projected} annotation is present, the DTO field name is assumed
 * to match the source field name exactly:</p>
 *
 * <pre>{@code
 * // Source entity
 * public class User {
 *     private Long id;
 *     private String email;
 *     private String phoneNumber;
 * }
 *
 * // DTO with implicit mapping
 * @Projection(from = User.class)
 * public interface UserDTO {
 *     Long getId();          // Maps to User.id (name match)
 *     String getEmail();     // Maps to User.email (name match)
 *     String getPhoneNumber(); // Maps to User.phoneNumber (name match)
 * }
 * }</pre>
 *
 * <h3>Strategy 2: Explicit Mapping with {@link Projected}</h3>
 * <p>Use {@code @Projected} to rename fields or access nested properties:</p>
 *
 * <pre>{@code
 * @Projection(from = User.class)
 * public interface UserDTO {
 *
 *     @Projected(from = "email")
 *     String getEmailAddress();  // Rename: email → emailAddress
 *
 *     @Projected(from = "address.city")
 *     String getCity();  // Flatten: nested access
 *
 *     @Projected(from = "profile.avatar.url")
 *     String getAvatarUrl();  // Deep nesting
 * }
 * }</pre>
 *
 * <h3>Strategy 3: Computed Fields with {@link Computed}</h3>
 * <p>Derive new values from one or more source fields:</p>
 *
 * <pre>{@code
 * @Projection(from = User.class, providers = { @Provider(UserComputations.class) })
 * public interface UserDTO {
 *
 *     // Concatenation
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();
 *
 *     // Calculation
 *     @Computed(dependsOn = "birthDate")
 *     Integer getAge();
 *
 *     // Type transformation
 *     @Computed(
 *         dependsOn = "createdAt",
 *         computedBy = @Method(value = "toInstant"),
 *         then = @Method(value = "formatIso")
 *     )
 *     String getFormattedDate();
 * }
 * }</pre>
 *
 * <h2>Complete Example</h2>
 *
 * <pre>{@code
 * // Source entity
 * @Entity
 * public class User {
 *     private Long id;
 *     private String firstName;
 *     private String lastName;
 *     private String email;
 *     private LocalDateTime createdAt;
 *     private LocalDate birthDate;
 *     private Address address;
 *
 *     @OneToMany
 *     private List<Order> orders;
 * }
 *
 * // DTO with complete projection strategy
 * @Projection(
 *     from = User.class,
 *     providers = {
 *         @Provider(UserComputations.class),          // Static utility methods
 *         @Provider(value = DateService.class,        // IoC-managed service
 *                   bean = "dateFormatter")
 *     }
 * )
 * public interface UserDTO {
 *
 *     // === Implicit mapping (name match) ===
 *     Long getId();
 *     String getFirstName();
 *     String getLastName();
 *
 *     // === Explicit mapping (renamed) ===
 *     @Projected(from = "email")
 *     String getEmailAddress();
 *
 *     // === Nested field access ===
 *     @Projected(from = "address.city")
 *     String getCity();
 *
 *     @Projected(from = "address.country")
 *     String getCountry();
 *
 *     // === Computed fields ===
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();
 *
 *     @Computed(dependsOn = "birthDate")
 *     Integer getAge();
 *
 *     @Computed(
 *         dependsOn = "createdAt",
 *         computedBy = @Method(value = "toInstant"),
 *         then = @Method(value = "formatIso")
 *     )
 *     String getFormattedCreationDate();
 *
 *     // === Collection aggregation ===
 *     @Computed(
 *         dependsOn = "orders.total",
 *         reducers = {Computed.Reduce.SUM}
 *     )
 *     BigDecimal getTotalOrderValue();
 * }
 *
 * // Static provider
 * public class UserComputations {
 *
 *     public static String toFullName(String firstName, String lastName) {
 *         return firstName + " " + lastName;
 *     }
 *
 *     public static Integer toAge(LocalDate birthDate) {
 *         return Period.between(birthDate, LocalDate.now()).getYears();
 *     }
 *
 *     public static Instant toInstant(LocalDateTime dateTime) {
 *         return dateTime.toInstant(ZoneOffset.UTC);
 *     }
 * }
 *
 * // IoC-managed provider
 * @Component("dateFormatter")
 * public class DateService {
 *
 *     @Inject
 *     private LocaleService localeService;
 *
 *     public static String formatIso(Instant instant) {
 *         return instant.toString();
 *     }
 * }
 * }</pre>
 *
 * <h2>Provider Resolution Strategy</h2>
 *
 * <p>When resolving methods for {@link Computed} fields, the system searches providers
 * in this order:</p>
 *
 * <ol>
 *   <li><b>DTO interface itself:</b> Check for static methods in the annotated interface</li>
 *   <li><b>Declared providers:</b> Search all providers from {@link #providers()} in
 *       declaration order</li>
 *   <li><b>First-match-wins:</b> Use the first method matching the required signature</li>
 * </ol>
 *
 * <h3>Method Matching Rules</h3>
 * <p>A provider method matches a {@link Computed} field if:</p>
 * <ul>
 *   <li><b>Name:</b> Follows convention {@code to[FieldName]} or matches explicit
 *       {@link Computed#computedBy()} reference</li>
 *   <li><b>Parameters:</b> Match types of {@link Computed#dependsOn()} fields in order</li>
 *   <li><b>Return type:</b> Compatible with field type (or with {@link Computed#then()}
 *       input type if transformation is used)</li>
 * </ul>
 *
 * <h3>Invocation Strategy</h3>
 * <p>Method invocation depends on whether the matched method is static or an instance method:</p>
 * <ul>
 *   <li><b>Static method:</b> Invoked directly (no IoC lookup)</li>
 *   <li><b>Instance method:</b> Provider instance resolved from IoC container:
 *     <ul>
 *       <li>If {@link Provider#bean()} is specified → resolve by name</li>
 *       <li>If {@link Provider#bean()} is empty → resolve by type ({@link Provider#value()})</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Use Cases and Patterns</h2>
 *
 * <h3>Use Case 1: Simple Data Transfer</h3>
 * <pre>{@code
 * // Expose only safe fields from entity
 * @Projection(from = User.class)
 * public interface PublicUserDTO {
 *     Long getId();
 *     String getUsername();
 *     String getEmail();
 *     // Password intentionally excluded
 * }
 * }</pre>
 *
 * <h3>Use Case 2: Flattening Complex Structures</h3>
 * <pre>{@code
 * // Flatten nested relationships for API response
 * @Projection(from = Order.class)
 * public interface OrderSummaryDTO {
 *     Long getId();
 *
 *     @Projected(from = "customer.name")
 *     String getCustomerName();
 *
 *     @Projected(from = "customer.email")
 *     String getCustomerEmail();
 *
 *     @Projected(from = "shippingAddress.city")
 *     String getShippingCity();
 * }
 * }</pre>
 *
 * <h3>Use Case 3: API Versioning</h3>
 * <pre>{@code
 * // V1 API
 * @Projection(from = Product.class)
 * public interface ProductDTOv1 {
 *     Long getId();
 *     String getName();
 *     BigDecimal getPrice();
 * }
 *
 * // V2 API (extended)
 * @Projection(from = Product.class, providers = { @Provider(ProductComputations.class) })
 * public interface ProductDTOv2 {
 *     Long getId();
 *     String getName();
 *
 *     @Projected(from = "price")
 *     BigDecimal getBasePrice();
 *
 *     @Computed(dependsOn = {"price", "taxRate"})
 *     BigDecimal getPriceWithTax();
 * }
 * }</pre>
 *
 * <h3>Use Case 4: Read-Only vs Write DTOs</h3>
 * <pre>{@code
 * // Read DTO (projection)
 * @Projection(from = User.class, providers = { @Provider(UserComputations.class) })
 * public interface UserReadDTO {
 *     Long getId();
 *     String getEmail();
 *
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();
 * }
 *
 * // Write DTO (plain class with validation)
 * public class UserCreateRequest {
 *     @NotBlank
 *     private String firstName;
 *
 *     @NotBlank
 *     private String lastName;
 *
 *     @Email
 *     private String email;
 *
 *     // Getters and setters
 * }
 * }</pre>
 *
 * <h3>Use Case 5: Multi-Provider Organization</h3>
 * <pre>{@code
 * @Projection(
 *     from = Employee.class,
 *     providers = {
 *         @Provider(NameComputations.class),      // Name formatting
 *         @Provider(DateComputations.class),      // Date formatting
 *         @Provider(SalaryComputations.class),    // Salary calculations
 *         @Provider(value = SecurityService.class, // Context-aware security
 *                   bean = "securityService")
 *     }
 * )
 * public interface EmployeeDTO {
 *     // Uses NameComputations
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();
 *
 *     // Uses DateComputations
 *     @Computed(dependsOn = "hireDate")
 *     String getFormattedHireDate();
 *
 *     // Uses SalaryComputations
 *     @Computed(dependsOn = {"baseSalary", "bonus"})
 *     BigDecimal getTotalCompensation();
 *
 *     // Uses SecurityService (IoC bean)
 *     @Computed(dependsOn = {"salary", "userId"})
 *     String getMaskedSalary();
 * }
 * }</pre>
 *
 * <h2>Advanced Features</h2>
 *
 * <h3>Collection Aggregation</h3>
 * <p>Reduce collections to single values using {@link Computed#reducers()}:</p>
 * <pre>{@code
 * @Projection(from = User.class)
 * public interface UserStatsDTO {
 *
 *     @Computed(
 *         dependsOn = "orders.total",
 *         reducers = {Computed.Reduce.SUM}
 *     )
 *     BigDecimal getTotalOrderValue();
 *
 *     @Computed(
 *         dependsOn = "orders.id",
 *         reducers = {Computed.Reduce.COUNT}
 *     )
 *     Long getOrderCount();
 * }
 * }</pre>
 *
 * <h3>Transformation Chains</h3>
 * <p>Chain computation and transformation using {@link Computed#then()}:</p>
 * <pre>{@code
 * @Projection(from = Product.class, providers = {
 *     @Provider(PriceCalculator.class),
 *     @Provider(Formatters.class)
 * })
 * public interface ProductDTO {
 *
 *     @Computed(
 *         dependsOn = {"basePrice", "discount"},
 *         computedBy = @Method(value = "calculateFinalPrice"),  // BigDecimal
 *         then = @Method(value = "formatCurrency")              // BigDecimal → String
 *     )
 *     String getFormattedPrice();
 * }
 * }</pre>
 *
 * <h2>Implementation Notes</h2>
 *
 * <h3>Type Compatibility</h3>
 * <p>The source class specified in {@link #from()} can be:</p>
 * <ul>
 *   <li>JPA Entity ({@code @Entity})</li>
 *   <li>MongoDB Document ({@code @Document})</li>
 *   <li>Domain object (plain Java class)</li>
 *   <li>Value object (immutable data holder)</li>
 *   <li>Another DTO (projection chaining)</li>
 * </ul>
 *
 * <h3>DTO Type</h3>
 * <p>The annotated class can be:</p>
 * <ul>
 *   <li><b>Interface:</b> Recommended for read-only projections (immutable by nature)</li>
 *   <li><b>Class:</b> Supported, but typically used for write DTOs (with setters and validation)</li>
 *   <li><b>Record:</b> Implementation-dependent (Java 14+ records)</li>
 * </ul>
 *
 * <h3>Provider Extensibility</h3>
 * <p>While providers are primarily used for {@link Computed} field resolution, implementations
 * may extend their usage for additional projection-related behaviors such as:</p>
 * <ul>
 *   <li>Virtual filter fields (custom query filters)</li>
 *   <li>Custom type converters</li>
 *   <li>Validation hooks</li>
 *   <li>Post-processing transformations</li>
 * </ul>
 * <p>Such extensions are implementation-specific and outside the scope of this specification.</p>
 *
 * <h2>Best Practices</h2>
 *
 * <ol>
 *   <li><b>Use interfaces for read-only DTOs:</b> Enforces immutability naturally</li>
 *   <li><b>Organize providers by concern:</b> Separate name, date, calculation logic</li>
 *   <li><b>Prefer static methods for pure logic:</b> Avoid IoC overhead when unnecessary</li>
 *   <li><b>Use computed fields for derived data:</b> Don't expose entity methods directly</li>
 *   <li><b>Keep transformation methods static:</b> Even in IoC-managed providers</li>
 *   <li><b>Document complex projections:</b> Comment non-obvious field mappings</li>
 *   <li><b>Version DTOs separately:</b> Create v1, v2 DTOs instead of modifying existing</li>
 * </ol>
 *
 * <h2>Common Pitfalls</h2>
 *
 * <h3>❌ Computed-to-Computed Dependencies</h3>
 * <pre>{@code
 * // WRONG: Cannot depend on other computed fields
 * @Computed(dependsOn = {"firstName", "lastName"})
 * String getFullName();
 *
 * @Computed(dependsOn = "fullName")  // ❌ fullName is computed!
 * String getUppercaseName();
 *
 * // CORRECT: Both depend on source fields
 * @Computed(dependsOn = {"firstName", "lastName"})
 * String getFullName();
 *
 * @Computed(
 *     dependsOn = {"firstName", "lastName"},
 *     computedBy = @Method(value = "toFullName"),
 *     then = @Method(value = "uppercase")
 * )
 * String getUppercaseName();
 * }</pre>
 *
 * <h3>❌ Missing Provider for Computed Field</h3>
 * <pre>{@code
 * // WRONG: No provider declared
 * @Projection(from = User.class)  // Missing providers!
 * public interface UserDTO {
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();  // ❌ No provider to resolve toFullName()
 * }
 *
 * // CORRECT: Provider declared
 * @Projection(from = User.class, providers = { @Provider(UserComputations.class) })
 * public interface UserDTO {
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();  // ✅ Resolved from UserComputations
 * }
 * }</pre>
 *
 * <h3>❌ Instance Method Without IoC Registration</h3>
 * <pre>{@code
 * // WRONG: Instance method but class not registered in IoC
 * @Provider(MyComputations.class)
 *
 * // Not registered in IoC container!
 * public class MyComputations {
 *     public String toFullName(String first, String last) {  // Instance method
 *         return first + " " + last;
 *     }
 * }
 *
 * // CORRECT: Either make it static or register in IoC
 * public class MyComputations {
 *     public static String toFullName(String first, String last) {  // Static
 *         return first + " " + last;
 *     }
 * }
 * // Or:
 * @Component
 * public class MyComputations {
 *     public String toFullName(String first, String last) {  // Instance + IoC
 *         return first + " " + last;
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Frank KOSSI
 * @see Provider
 * @see Computed
 * @see Projected
 * @see Method
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Projection {

    /**
     * The source class from which this DTO projects.
     *
     * <p>All field mappings—whether via {@link Projected}, implicit name matching, or
     * {@link Computed} dependencies—must reference fields that exist in this source class.</p>
     *
     * <h2>Source Class Requirements</h2>
     * <p>The source class must:</p>
     * <ul>
     *   <li>Have accessible getter methods or public fields for projected properties</li>
     *   <li>Follow JavaBeans naming convention ({@code getXxx()}, {@code isXxx()}) for best compatibility</li>
     *   <li>Be a concrete class (not an interface, unless implementation provides support)</li>
     * </ul>
     *
     * <h2>Supported Source Types</h2>
     * <p>The source class can be any Java type with accessible fields:</p>
     * <ul>
     *   <li><b>JPA Entities:</b> {@code @Entity} classes with ORM mappings</li>
     *   <li><b>MongoDB Documents:</b> {@code @Document} classes</li>
     *   <li><b>Domain Objects:</b> Plain business objects (POJO)</li>
     *   <li><b>Value Objects:</b> Immutable data holders</li>
     *   <li><b>DTOs:</b> Other data transfer objects (projection chaining)</li>
     *   <li><b>Records:</b> Java 14+ record classes (implementation-dependent)</li>
     * </ul>
     *
     * <h2>Field Access Strategy</h2>
     * <p>How fields are accessed from the source class is implementation-specific, but
     * typically follows this priority:</p>
     * <ol>
     *   <li>Public getter method ({@code getFieldName()}, {@code isFieldName()})</li>
     *   <li>Public field (if getter not found)</li>
     *   <li>Compile-time error if neither exists</li>
     * </ol>
     *
     * <h2>Nested Field Access</h2>
     * <p>The source class's relationships are traversed for {@link Projected} paths:</p>
     * <pre>{@code
     * // Source
     * public class User {
     *     private Address address;  // One-to-one relationship
     * }
     *
     * public class Address {
     *     private String city;
     *     private String country;
     * }
     *
     * // DTO
     * @Projection(from = User.class)
     * public interface UserDTO {
     *     @Projected(from = "address.city")    // Accesses User.getAddress().getCity()
     *     String getCity();
     * }
     * }</pre>
     *
     * <h2>Examples</h2>
     *
     * <h3>JPA Entity Source</h3>
     * <pre>{@code
     * @Entity
     * @Table(name = "users")
     * public class User {
     *     @Id
     *     private Long id;
     *
     *     @Column(nullable = false)
     *     private String email;
     *
     *     @ManyToOne
     *     private Department department;
     * }
     *
     * @Projection(from = User.class)
     * public interface UserDTO {
     *     Long getId();
     *     String getEmail();
     *
     *     @Projected(from = "department.name")
     *     String getDepartmentName();
     * }
     * }</pre>
     *
     * <h3>Plain POJO Source</h3>
     * <pre>{@code
     * public class Customer {
     *     private String firstName;
     *     private String lastName;
     *     private LocalDate birthDate;
     *
     *     // Getters
     * }
     *
     * @Projection(from = Customer.class, providers = { @Provider(CustomerComputations.class) })
     * public interface CustomerDTO {
     *     String getFirstName();
     *     String getLastName();
     *
     *     @Computed(dependsOn = "birthDate")
     *     Integer getAge();
     * }
     * }</pre>
     *
     * <h3>DTO-to-DTO Projection (Chaining)</h3>
     * <pre>{@code
     * // Internal DTO
     * @Projection(from = User.class)
     * public interface UserInternalDTO {
     *     Long getId();
     *     String getEmail();
     *     String getPasswordHash();  // Sensitive field
     * }
     *
     * // Public API DTO (projects from internal DTO)
     * @Projection(from = UserInternalDTO.class)
     * public interface UserPublicDTO {
     *     Long getId();
     *     String getEmail();
     *     // passwordHash excluded
     * }
     * }</pre>
     *
     * <h2>Validation</h2>
     * <p>The annotation processor validates at compile-time:</p>
     * <ul>
     *   <li>Source class exists and is accessible</li>
     *   <li>All {@link Projected} paths reference valid fields in the source class</li>
     *   <li>All {@link Computed} dependencies reference existing source fields</li>
     *   <li>Field types are compatible between source and DTO</li>
     * </ul>
     *
     * @return the source class to project from
     */
    Class<?> from();

    /**
     * Provider classes that supply computation methods, transformation functions,
     * and extension logic for this projection.
     *
     * <p>Providers are the computation engine of the projection system. They contain
     * the methods that:</p>
     * <ul>
     *   <li>Compute values for {@link Computed} fields</li>
     *   <li>Transform types via {@link Computed#then()}</li>
     *   <li>Implement custom projection behaviors (implementation-specific)</li>
     * </ul>
     *
     * <h2>Resolution Order</h2>
     * <p>Providers are searched in declaration order using a <b>first-match-wins</b> strategy:</p>
     * <ol>
     *   <li>DTO interface itself (static methods)</li>
     *   <li>First provider in array</li>
     *   <li>Second provider in array</li>
     *   <li>... and so on</li>
     * </ol>
     *
     * <pre>{@code
     * @Projection(
     *     from = User.class,
     *     providers = {
     *         @Provider(HighPriorityComputations.class),  // Checked first
     *         @Provider(MediumPriorityComputations.class),// Checked second
     *         @Provider(FallbackComputations.class)       // Checked last
     *     }
     * )
     * }</pre>
     *
     * <h2>Provider Types</h2>
     *
     * <h3>Static Providers</h3>
     * <p>Classes with static methods (no IoC registration required):</p>
     * <pre>{@code
     * @Provider(StringUtils.class)
     *
     * public class StringUtils {
     *     public static String uppercase(String str) {
     *         return str != null ? str.toUpperCase() : null;
     *     }
     * }
     * }</pre>
     *
     * <h3>IoC-Managed Providers (by type)</h3>
     * <p>Instance methods, provider resolved by class type:</p>
     * <pre>{@code
     * @Provider(TaxService.class)  // Resolved by type
     *
     * @Component
     * public class TaxService {
     *     @Inject
     *     private TaxRepository taxRepo;
     *
     *     public BigDecimal toTotalWithTax(BigDecimal amount) {
     *         return amount.multiply(taxRepo.getRate());
     *     }
     * }
     * }</pre>
     *
     * <h3>IoC-Managed Providers (by name)</h3>
     * <p>Explicitly specify bean name for disambiguation:</p>
     * <pre>{@code
     * @Provider(value = PriceCalculator.class, bean = "premiumPricing")
     *
     * @Named("premiumPricing")
     * public class PremiumPriceCalculator implements PriceCalculator {
     *     public BigDecimal toFinalPrice(BigDecimal base) { ... }
     * }
     *
     * @Named("standardPricing")
     * public class StandardPriceCalculator implements PriceCalculator {
     *     public BigDecimal toFinalPrice(BigDecimal base) { ... }
     * }
     * }</pre>
     *
     * <h2>Method Resolution</h2>
     * <p>For each {@link Computed} field, the system searches providers for a matching method:</p>
     * <ul>
     *   <li><b>Convention-based:</b> {@code to[FieldName](dependsOn...)} if
     *       {@link Computed#computedBy()} not specified</li>
     *   <li><b>Explicit reference:</b> Method specified in {@link Computed#computedBy()}</li>
     *   <li><b>Parameter matching:</b> Method parameters must match {@link Computed#dependsOn()}
     *       field types in order</li>
     *   <li><b>Return type:</b> Compatible with field type (or {@link Computed#then()} input)</li>
     * </ul>
     *
     * <h2>Invocation Strategy</h2>
     * <p>How a matched method is invoked depends on whether it's static or instance:</p>
     * <table border="1">
     *   <tr>
     *     <th>Method Type</th>
     *     <th>Invocation</th>
     *   </tr>
     *   <tr>
     *     <td>{@code static}</td>
     *     <td>Direct call (no IoC lookup)</td>
     *   </tr>
     *   <tr>
     *     <td>Instance + {@code bean=""}</td>
     *     <td>Resolve provider by type from IoC</td>
     *   </tr>
     *   <tr>
     *     <td>Instance + {@code bean="name"}</td>
     *     <td>Resolve provider by name from IoC</td>
     *   </tr>
     * </table>
     *
     * <h2>Examples</h2>
     *
     * <h3>Single Static Provider</h3>
     * <pre>{@code
     * @Projection(
     *     from = User.class,
     *     providers = { @Provider(UserComputations.class) }
     * )
     * public interface UserDTO {
     *     @Computed(dependsOn = {"firstName", "lastName"})
     *     String getFullName();  // Resolved from UserComputations.toFullName()
     * }
     * }</pre>
     *
     * <h3>Multiple Providers with Priority</h3>
     * <pre>{@code
     * @Projection(
     *     from = Order.class,
     *     providers = {
     *         @Provider(CustomPricing.class),      // Custom logic (checked first)
     *         @Provider(StandardPricing.class)     // Fallback (checked second)
     *     }
     * )
     * public interface OrderDTO {
     *     @Computed(dependsOn = "amount")
     *     BigDecimal getFinalPrice();
     *     // Uses CustomPricing.toFinalPrice() if exists,
     *     // otherwise StandardPricing.toFinalPrice()
     * }
     * }</pre>
     *
     * <h3>Mixed Static and IoC Providers</h3>
     * <pre>{@code
     * @Projection(
     *     from = Product.class,
     *     providers = {
     *         @Provider(StaticUtils.class),                     // Static methods
     *         @Provider(value = PriceService.class,             // IoC by name
     *                   bean = "productPricing"),
     *         @Provider(InventoryService.class)                 // IoC by type
     *     }
     * )
     * public interface ProductDTO { ... }
     * }</pre>
     *
     * <h2>Extensibility</h2>
     * <p>While providers are primarily used for resolving {@link Computed} fields,
     * implementations may extend their usage for additional projection-related behaviors
     * such as:</p>
     * <ul>
     *   <li>Virtual filter fields for dynamic querying</li>
     *   <li>Custom type converters for special data types</li>
     *   <li>Validation hooks executed during projection</li>
     *   <li>Post-processing transformations</li>
     * </ul>
     * <p><b>Note:</b> Such extensions are implementation-specific and outside the scope
     * of this specification. Consult your implementation's documentation for details.</p>
     *
     * <h2>Best Practices</h2>
     * <ol>
     *   <li><b>Organize by concern:</b> Separate providers for names, dates, calculations</li>
     *   <li><b>Order matters:</b> Place specific providers before generic ones</li>
     *   <li><b>Prefer static for pure logic:</b> Avoid IoC overhead when possible</li>
     *   <li><b>Use beans for context:</b> Use IoC only when dependencies are needed</li>
     *   <li><b>Document custom providers:</b> Comment non-obvious provider purposes</li>
     * </ol>
     *
     * @return the array of provider declarations
     * @see Provider
     * @see Computed
     * @see Computed#computedBy()
     * @see Computed#then()
     */
    Provider[] providers() default {};
}