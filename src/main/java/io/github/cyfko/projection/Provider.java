package io.github.cyfko.projection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a provider class that supplies computation methods, transformation functions,
 * and other projection-related functionality.
 *
 * <p>Providers are the workhorses of the projection system, containing the logic to compute
 * derived fields, transform types, and handle custom projection behaviors. They are declared
 * in {@link Projection#providers()} and searched in order when resolving methods for
 * {@link Computed} fields.</p>
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><b>Provider class:</b> A class containing methods that compute or transform values</li>
 *   <li><b>Static methods:</b> Invoked directly without IoC lookup (no dependencies)</li>
 *   <li><b>Instance methods:</b> Require an instance, resolved from IoC container if available</li>
 *   <li><b>First-match-wins:</b> Providers are searched in declaration order; first matching
 *       method is used</li>
 * </ul>
 *
 * <h2>Method Resolution Strategy</h2>
 *
 * <p>When a computation or transformation method is found in a provider, the invocation
 * strategy depends on the method's signature:</p>
 *
 * <h3>Static Methods (Direct Invocation)</h3>
 * <p>If the matched method is {@code static}, it is invoked directly without any
 * IoC container lookup:</p>
 *
 * <pre>{@code
 * @Projection(from = User.class, providers = { @Provider(UserComputations.class) })
 * public interface UserDTO {
 *
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();
 * }
 *
 * // Provider with static method
 * public class UserComputations {
 *
 *     // Static → invoked directly
 *     public static String toFullName(String firstName, String lastName) {
 *         return firstName + " " + lastName;
 *     }
 * }
 * }</pre>
 *
 * <p><b>Characteristics:</b></p>
 * <ul>
 *   <li>No IoC lookup required</li>
 *   <li>Zero runtime overhead</li>
 *   <li>Ideal for pure computations and transformations</li>
 * </ul>
 *
 * <h3>Instance Methods (IoC Resolution)</h3>
 * <p>If the matched method is an instance method, an instance of the provider class
 * is obtained from the IoC container (if available):</p>
 *
 * <pre>{@code
 * @Projection(from = Order.class, providers = { @Provider(OrderComputations.class) })
 * public interface OrderDTO {
 *
 *     @Computed(dependsOn = {"subtotal", "region"})
 *     BigDecimal getTotalWithTax();
 * }
 *
 * // Provider as IoC-managed component (framework-specific annotations)
 * @Component  // or @Service, @Named, etc. depending on IoC framework
 * public class OrderComputations {
 *
 *     @Inject  // or @Autowired, etc.
 *     private TaxRepository taxRepo;
 *
 *     // Instance method → OrderComputations resolved from IoC container
 *     public BigDecimal toTotalWithTax(BigDecimal subtotal, String region) {
 *         BigDecimal taxRate = taxRepo.findByRegion(region);
 *         return subtotal.multiply(BigDecimal.ONE.add(taxRate));
 *     }
 * }
 * }</pre>
 *
 * <p><b>IoC Resolution Behavior:</b></p>
 * <ul>
 *   <li>If {@link #bean()} is specified → resolve by name</li>
 *   <li>If {@link #bean()} is empty (default) → resolve by type ({@link #value()})</li>
 *   <li>Resolution occurs once on first access, then the instance is cached</li>
 *   <li>If no instance is found, behavior is implementation-specific (typically throws exception)</li>
 * </ul>
 *
 * <h2>Provider Types</h2>
 *
 * <h3>1. Utility Provider (Static Methods Only)</h3>
 * <p>A class containing only static methods, requiring no IoC management:</p>
 *
 * <pre>{@code
 * @Provider(StringUtils.class)
 *
 * public class StringUtils {
 *
 *     public static String uppercase(String str) {
 *         return str != null ? str.toUpperCase() : null;
 *     }
 *
 *     public static String lowercase(String str) {
 *         return str != null ? str.toLowerCase() : null;
 *     }
 * }
 * }</pre>
 *
 * <h3>2. Mixed Provider (Static + Instance Methods)</h3>
 * <p>A class with both static and instance methods. Static methods are invoked directly,
 * instance methods trigger IoC resolution:</p>
 *
 * <pre>{@code
 * @Provider(UserComputations.class)
 *
 * @Component
 * public class UserComputations {
 *
 *     @Inject
 *     private UserRepository userRepo;
 *
 *     // Static → direct invocation
 *     public static String toInitials(String first, String last) {
 *         return first.charAt(0) + "." + last.charAt(0) + ".";
 *     }
 *
 *     // Instance → IoC resolution required
 *     public Integer toFriendCount(Long userId) {
 *         return userRepo.countFriendsByUserId(userId);
 *     }
 * }
 * }</pre>
 *
 * <h3>3. IoC-Managed Provider (Instance Methods Only)</h3>
 * <p>A component fully managed by the IoC container, with only instance methods:</p>
 *
 * <pre>{@code
 * @Provider(value = TaxCalculator.class, bean = "taxService")
 *
 * @Named("taxService")  // CDI example
 * public class TaxCalculator {
 *
 *     @Inject
 *     private ConfigurationService config;
 *
 *     public BigDecimal toTotalWithTax(BigDecimal amount, String region) {
 *         BigDecimal rate = config.getTaxRate(region);
 *         return amount.multiply(BigDecimal.ONE.add(rate));
 *     }
 * }
 * }</pre>
 *
 * <h2>The {@code bean} Attribute</h2>
 *
 * <h3>When {@code bean} is NOT Specified (Default)</h3>
 * <p>The system behaves intelligently based on the method type:</p>
 * <ul>
 *   <li>If a <b>static method</b> is matched → invoked directly (no IoC)</li>
 *   <li>If an <b>instance method</b> is matched → resolve provider by <b>type</b>
 *       ({@link #value()}) from IoC container</li>
 * </ul>
 *
 * <pre>{@code
 * @Provider(OrderService.class)  // No bean name
 *
 * @Component  // Registered in IoC as OrderService.class
 * public class OrderService {
 *
 *     @Inject
 *     private DiscountRepository discounts;
 *
 *     // Instance method → IoC resolves OrderService by type
 *     public BigDecimal toDiscountedPrice(BigDecimal price, Long customerId) {
 *         return price.subtract(discounts.findByCustomer(customerId));
 *     }
 * }
 * }</pre>
 *
 * <h3>When {@code bean} IS Specified</h3>
 * <p>The provider instance is resolved from the IoC container by the specified name,
 * regardless of method type:</p>
 *
 * <pre>{@code
 * @Provider(value = PriceCalculator.class, bean = "premiumPricing")
 *
 * @Named("premiumPricing")
 * public class PremiumPriceCalculator implements PriceCalculator {
 *
 *     public BigDecimal toFinalPrice(BigDecimal base) {
 *         // Premium pricing logic
 *     }
 * }
 *
 * @Named("standardPricing")
 * public class StandardPriceCalculator implements PriceCalculator {
 *
 *     public BigDecimal toFinalPrice(BigDecimal base) {
 *         // Standard pricing logic
 *     }
 * }
 * }</pre>
 *
 * <h2>Resolution Behavior Summary</h2>
 *
 * <table border="1">
 *   <tr>
 *     <th>{@code bean} attribute</th>
 *     <th>Method type</th>
 *     <th>Resolution strategy</th>
 *   </tr>
 *   <tr>
 *     <td rowspan="2">Empty (default)</td>
 *     <td>{@code static}</td>
 *     <td>Direct invocation (no IoC)</td>
 *   </tr>
 *   <tr>
 *     <td>Instance</td>
 *     <td>Resolve from IoC by type ({@link #value()})</td>
 *   </tr>
 *   <tr>
 *     <td rowspan="2">Specified</td>
 *     <td>{@code static}</td>
 *     <td>Direct invocation (bean name ignored)</td>
 *   </tr>
 *   <tr>
 *     <td>Instance</td>
 *     <td>Resolve from IoC by name ({@link #bean()})</td>
 *   </tr>
 * </table>
 *
 * <h2>Provider Search Order</h2>
 *
 * <p>When resolving a method for a {@link Computed} field, the system searches providers
 * in this order:</p>
 * <ol>
 *   <li><b>DTO interface itself:</b> Static methods in the DTO (if present)</li>
 *   <li><b>Declared providers:</b> All providers from {@link Projection#providers()},
 *       in declaration order</li>
 *   <li><b>First match wins:</b> The first method matching the signature is used</li>
 * </ol>
 *
 * <pre>{@code
 * @Projection(
 *     from = User.class,
 *     providers = {
 *         @Provider(PrimaryComputations.class),    // Searched first
 *         @Provider(FallbackComputations.class),   // Searched second
 *         @Provider(GenericUtils.class)            // Searched third
 *     }
 * )
 * public interface UserDTO {
 *
 *     @Computed(dependsOn = "birthDate")
 *     Integer getAge();
 *     // Resolution:
 *     // 1. Check UserDTO for static toAge(LocalDate)
 *     // 2. Check PrimaryComputations for toAge(LocalDate)
 *     // 3. Check FallbackComputations for toAge(LocalDate)
 *     // 4. Check GenericUtils for toAge(LocalDate)
 *     // → Use first match (static or instance, resolved accordingly)
 * }
 * }</pre>
 *
 * <h2>Use Cases and Best Practices</h2>
 *
 * <h3>Use Case 1: Organized Code Structure</h3>
 * <p>Group related computations into logical provider classes:</p>
 *
 * <pre>{@code
 * @Projection(
 *     from = User.class,
 *     providers = {
 *         @Provider(UserNameComputations.class),     // Name-related logic
 *         @Provider(UserDateComputations.class),     // Date formatting
 *         @Provider(UserSecurityComputations.class)  // Security/privacy
 *     }
 * )
 * public interface UserDTO { ... }
 * }</pre>
 *
 * <h3>Use Case 2: Prioritized Fallback Logic</h3>
 * <p>Use provider order to implement fallback strategies:</p>
 *
 * <pre>{@code
 * @Projection(
 *     from = Product.class,
 *     providers = {
 *         @Provider(PremiumPriceCalculator.class),   // Try premium logic first
 *         @Provider(StandardPriceCalculator.class)   // Fallback to standard
 *     }
 * )
 * public interface ProductDTO {
 *
 *     @Computed(dependsOn = {"basePrice", "discount"})
 *     BigDecimal getFinalPrice();
 * }
 * }</pre>
 *
 * <h3>Use Case 3: Reusable Generic Utilities</h3>
 * <p>Share common transformations across multiple DTOs:</p>
 *
 * <pre>{@code
 * // Shared utility provider
 * public class CommonTransformations {
 *     public static String uppercase(String str) {
 *         return str != null ? str.toUpperCase() : null;
 *     }
 *
 *     public static String trim(String str) {
 *         return str != null ? str.trim() : null;
 *     }
 * }
 *
 * // Used in multiple DTOs
 * @Projection(from = User.class, providers = { @Provider(CommonTransformations.class) })
 * public interface UserDTO { ... }
 *
 * @Projection(from = Product.class, providers = { @Provider(CommonTransformations.class) })
 * public interface ProductDTO { ... }
 * }</pre>
 *
 * <h3>Use Case 4: Disambiguating Multiple Beans of Same Type</h3>
 * <p>Use explicit bean names when multiple implementations exist:</p>
 *
 * <pre>{@code
 * @Projection(
 *     from = Order.class,
 *     providers = {
 *         @Provider(value = PriceCalculator.class, bean = "premiumCalculator"),
 *         @Provider(value = PriceCalculator.class, bean = "basicCalculator")
 *     }
 * )
 * public interface OrderDTO { ... }
 * }</pre>
 *
 * <h2>Provider Responsibilities</h2>
 *
 * <p>Providers can contain methods for:</p>
 *
 * <h3>1. Computation Methods ({@link Computed#computedBy()})</h3>
 * <ul>
 *   <li>Business logic and domain calculations</li>
 *   <li>Convention: {@code to[FieldName](dependsOn...)} or explicit via {@link Method}</li>
 *   <li>Can be static or instance methods</li>
 * </ul>
 *
 * <h3>2. Transformation Methods ({@link Computed#then()})</h3>
 * <ul>
 *   <li>Pure type conversions and post-processing</li>
 *   <li><b>Must</b> be {@code static} (pure functions)</li>
 *   <li>Explicitly referenced via {@link Method#value()}</li>
 * </ul>
 *
 * <h3>3. Extension Points (Implementation-Specific)</h3>
 * <ul>
 *   <li>Virtual filter fields (e.g., custom filter resolvers)</li>
 *   <li>Custom validation hooks</li>
 *   <li>Data converters</li>
 * </ul>
 *
 * <p><b>Note:</b> Extension behaviors beyond {@link Computed} are implementation-specific
 * and outside the scope of this specification.</p>
 *
 * <h2>Complete Example</h2>
 *
 * <pre>{@code
 * // DTO with multiple providers
 * @Projection(
 *     from = User.class,
 *     providers = {
 *         @Provider(UserComputations.class),                     // Mixed static/instance
 *         @Provider(value = DateFormatter.class, bean = "fmt"),  // Bean by name
 *         @Provider(StringUtils.class)                           // Static utilities
 *     }
 * )
 * public interface UserDTO {
 *
 *     // Uses UserComputations.toFullName (static)
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();
 *
 *     // Uses DateFormatter.toInstant (instance, IoC by name)
 *     // then StringUtils.uppercase (static)
 *     @Computed(
 *         dependsOn = "createdAt",
 *         computedBy = @Method(value = "toInstant"),
 *         then = @Method(value = "uppercase")
 *     )
 *     String getFormattedDate();
 * }
 *
 * // Provider with static methods
 * public class UserComputations {
 *     public static String toFullName(String firstName, String lastName) {
 *         return firstName + " " + lastName;
 *     }
 * }
 *
 * // IoC-managed provider (framework-agnostic)
 * @Component  // or @Named, @Singleton, etc.
 * public class DateFormatter {
 *
 *     @Inject
 *     private TimeZoneConfig config;
 *
 *     public Instant toInstant(LocalDateTime dt) {
 *         return dt.toInstant(config.getDefaultZoneOffset());
 *     }
 * }
 *
 * // Static transformation utilities
 * public class StringUtils {
 *     public static String uppercase(String str) {
 *         return str != null ? str.toUpperCase() : null;
 *     }
 * }
 * }</pre>
 *
 * <h2>Common Patterns</h2>
 *
 * <h3>Pattern 1: Single-Responsibility Providers</h3>
 * <pre>{@code
 * providers = {
 *     @Provider(NameComputations.class),      // Only name-related
 *     @Provider(DateComputations.class),      // Only date-related
 *     @Provider(PriceComputations.class)      // Only price-related
 * }
 * }</pre>
 *
 * <h3>Pattern 2: Layered Providers (Priority Order)</h3>
 * <pre>{@code
 * providers = {
 *     @Provider(CustomOverrides.class),       // Custom logic (highest priority)
 *     @Provider(DomainComputations.class),    // Domain-specific
 *     @Provider(GenericUtils.class)           // Generic fallbacks (lowest priority)
 * }
 * }</pre>
 *
 * <h3>Pattern 3: Mixed Static + IoC Providers</h3>
 * <pre>{@code
 * providers = {
 *     @Provider(StaticUtils.class),                          // Pure static methods
 *     @Provider(value = ContextService.class, bean = "ctx")  // IoC-managed
 * }
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 *
 * <ul>
 *   <li><b>Static methods:</b> Zero overhead (direct method calls)</li>
 *   <li><b>Instance methods (type resolution):</b> First access requires IoC lookup,
 *       subsequent calls use cached instance</li>
 *   <li><b>Instance methods (name resolution):</b> Same as type resolution, but by name</li>
 *   <li><b>Provider order:</b> Earlier providers are checked first; place common methods
 *       in early providers</li>
 * </ul>
 *
 * <h2>IoC Framework Compatibility</h2>
 *
 * <p>The bean resolution mechanism is implementation-specific. The annotation processor
 * or runtime framework may support various IoC containers. Common examples include:</p>
 * <ul>
 *   <li>JSR-330 (Dependency Injection for Java): {@code @Named}, {@code @Inject}</li>
 *   <li>JSR-299 (CDI): {@code @Named}, {@code @ApplicationScoped}, {@code @RequestScoped}</li>
 *   <li>Framework-specific annotations (implementation-dependent)</li>
 * </ul>
 *
 * <p><b>Note:</b> Consult your implementation's documentation for supported IoC frameworks
 * and annotation requirements.</p>
 *
 * @since 1.0.0
 * @author Frank KOSSI
 * @see Projection#providers()
 * @see Computed
 * @see Computed#computedBy()
 * @see Computed#then()
 * @see Method
 */
@Retention(RetentionPolicy.SOURCE)
@Target({}) // Only usable within @Projection
public @interface Provider {

    /**
     * The class containing provider methods.
     *
     * <p>This class will be searched for methods matching computation or transformation
     * requirements. The invocation strategy depends on whether the matched method is
     * static or an instance method:</p>
     *
     * <h3>Static Methods</h3>
     * <p>Invoked directly without IoC container involvement:</p>
     * <pre>{@code
     * @Provider(MathUtils.class)
     *
     * public class MathUtils {
     *     public static BigDecimal toPercentage(BigDecimal value) {
     *         return value.multiply(new BigDecimal("100"));
     *     }
     * }
     * }</pre>
     *
     * <h3>Instance Methods</h3>
     * <p>Require an instance resolved from the IoC container:</p>
     * <pre>{@code
     * @Provider(TaxService.class)  // Or @Provider(value = TaxService.class, bean = "taxCalc")
     *
     * @Component  // IoC-managed component
     * public class TaxService {
     *
     *     @Inject
     *     private TaxRepository taxRepo;
     *
     *     // Instance method → TaxService resolved from IoC
     *     public BigDecimal toTotalWithTax(BigDecimal amount) {
     *         return amount.multiply(taxRepo.getDefaultRate());
     *     }
     * }
     * }</pre>
     *
     * <h3>Method Resolution</h3>
     * <p>The provider class is searched for methods matching:</p>
     * <ul>
     *   <li><b>Computation methods:</b> Convention {@code to[FieldName](...)} for
     *       {@link Computed} fields, or explicitly via {@link Computed#computedBy()}</li>
     *   <li><b>Transformation methods:</b> Explicitly referenced via {@link Computed#then()}.
     *       Note: transformation methods <b>must be static</b></li>
     *   <li><b>Extension methods:</b> Implementation-specific (e.g., virtual field resolvers)</li>
     * </ul>
     *
     * @return the provider class
     */
    Class<?> value();

    /**
     * Optional bean name for IoC container lookup.
     *
     * <p>This attribute controls how instance methods are resolved when a provider
     * instance is needed:</p>
     *
     * <h2>Resolution Behavior</h2>
     * <table border="1">
     *   <tr>
     *     <th>Attribute value</th>
     *     <th>Method type</th>
     *     <th>Resolution strategy</th>
     *   </tr>
     *   <tr>
     *     <td rowspan="2">{@code ""} (default)</td>
     *     <td>{@code static}</td>
     *     <td>Direct invocation (no IoC)</td>
     *   </tr>
     *   <tr>
     *     <td>Instance</td>
     *     <td>Resolve from IoC by <b>type</b> ({@link #value()})</td>
     *   </tr>
     *   <tr>
     *     <td rowspan="2">Bean name specified</td>
     *     <td>{@code static}</td>
     *     <td>Direct invocation (bean name ignored)</td>
     *   </tr>
     *   <tr>
     *     <td>Instance</td>
     *     <td>Resolve from IoC by <b>name</b> (this attribute)</td>
     *   </tr>
     * </table>
     *
     * <h2>When to Specify Bean Name</h2>
     * <p>Explicitly specify a bean name when:</p>
     * <ul>
     *   <li>Multiple beans of the same type exist in the IoC container</li>
     *   <li>The bean name doesn't match the class name convention</li>
     *   <li>Disambiguating between different implementations</li>
     * </ul>
     *
     * <h2>Examples</h2>
     *
     * <h3>Resolution by Type (Default)</h3>
     * <pre>{@code
     * @Provider(TaxService.class)  // bean = "" (default)
     *
     * @Component  // Registered as TaxService.class
     * public class TaxService {
     *     public BigDecimal toTotalWithTax(BigDecimal amount) { ... }
     * }
     * // Resolution: IoC container lookup by type TaxService.class
     * }</pre>
     *
     * <h3>Resolution by Name (Explicit)</h3>
     * <pre>{@code
     * @Provider(value = PriceCalculator.class, bean = "premiumCalculator")
     *
     * @Named("premiumCalculator")
     * public class PremiumPriceCalculator implements PriceCalculator {
     *     public BigDecimal toFinalPrice(BigDecimal base) { ... }
     * }
     *
     * @Named("basicCalculator")
     * public class BasicPriceCalculator implements PriceCalculator {
     *     public BigDecimal toFinalPrice(BigDecimal base) { ... }
     * }
     * // Resolution: IoC container lookup by name "premiumCalculator"
     * }</pre>
     *
     * <h2>Runtime Behavior</h2>
     * <ul>
     *   <li>IoC lookup occurs once on first access, then the instance is cached</li>
     *   <li>If the bean is not found, behavior is implementation-specific (typically
     *       throws a runtime exception)</li>
     *   <li>Bean lifecycle (scope, destruction) is managed by the IoC container</li>
     * </ul>
     *
     * @return the bean name for IoC lookup, or empty string for type-based resolution
     */
    String bean() default "";
}