package io.github.cyfko.projection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a DTO field as computed from one or more source fields through a computation method,
 * with optional post-processing transformation.
 *
 * <p>This annotation enables derived values that don't exist in the source entity,
 * such as formatted strings, calculations, concatenations, or type conversions.</p>
 *
 * <p><b>Important:</b> Whether this annotation applies to fields, methods, or both is
 * implementation-dependent. The examples below show both usages, but consult your
 * implementation's documentation for specific requirements.</p>
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><b>Informative method:</b> The annotated getter/field that declares <i>what</i>
 *       value should be computed (e.g., {@code getFullName()})</li>
 *   <li><b>Resolution method:</b> The provider method that performs the actual computation
 *       (e.g., {@code toConcatenate(String, String)})</li>
 *   <li><b>Transformation method:</b> Optional pure function to convert the result type
 *       (e.g., {@code Instant → String})</li>
 * </ul>
 *
 * <h2>Architecture Overview</h2>
 *
 * <h3>Two-Stage Computation Pipeline</h3>
 * <pre>{@code
 * Source Fields → [computedBy] → Intermediate Result → [then] → Final Value
 *                  (business logic)                     (type conversion)
 * }</pre>
 *
 * <p><b>Stage 1 ({@link #computedBy()}):</b> Business logic computation</p>
 * <ul>
 *   <li>Input: Source fields specified in {@link #dependsOn()}</li>
 *   <li>Output: Intermediate or final result</li>
 *   <li>Can be static or IoC-managed bean method</li>
 *   <li>Searched in providers using convention or explicit reference</li>
 * </ul>
 *
 * <p><b>Stage 2 ({@link #then()}):</b> Optional type transformation</p>
 * <ul>
 *   <li>Input: Output of {@code computedBy}</li>
 *   <li>Output: Final value matching field type</li>
 *   <li>Must be static (pure function)</li>
 *   <li>Enables reuse of existing methods with different return types</li>
 * </ul>
 *
 * <h2>Method Resolution Strategy</h2>
 *
 * <h3>1. Convention-Based Resolution (Default)</h3>
 * <p>When {@link #computedBy()} is not specified, the system searches for a method
 * named {@code to[FieldName]} in all providers:</p>
 *
 * <pre>{@code
 * @Projection(from = User.class, providers = { @Provider(UserComputations.class) })
 * public interface UserDTO {
 *
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();
 *     // Looks for: toFullName(String firstName, String lastName)
 * }
 *
 * public class UserComputations {
 *     public static String toFullName(String firstName, String lastName) {
 *         return firstName + " " + lastName;
 *     }
 * }
 * }</pre>
 *
 * <h3>2. Explicit Method Reference (Override)</h3>
 * <p>Use {@link #computedBy()} to explicitly specify which method to use:</p>
 *
 * <pre>{@code
 * @Computed(
 *     dependsOn = {"warehouse", "line", "itemId"},
 *     computedBy = @Method(value = "buildProductReference")
 * )
 * String getProductReference();
 * // Explicitly uses: buildProductReference(String, int, UUID)
 * }</pre>
 *
 * <h3>3. Provider Lookup Order</h3>
 * <p>Resolution follows this search order (first-match-wins):</p>
 * <ol>
 *   <li>Search in the DTO interface itself (if method is static)</li>
 *   <li>Search in all {@link Projection#providers()} in declaration order</li>
 *   <li>Stop at first method matching signature (name + parameter types)</li>
 * </ol>
 *
 * <h2>Complete Examples</h2>
 *
 * <h3>Example 1: Simple Computation (Convention)</h3>
 * <pre>{@code
 * @Projection(from = User.class, providers = { @Provider(UserComputations.class) })
 * public interface UserDTO {
 *
 *     @Computed(dependsOn = {"firstName", "lastName"})
 *     String getFullName();
 * }
 *
 * public class UserComputations {
 *     public static String toFullName(String firstName, String lastName) {
 *         return firstName + " " + lastName;
 *     }
 * }
 *
 * // Generated code:
 * public String getFullName() {
 *     String firstName = source.getFirstName();
 *     String lastName = source.getLastName();
 *     return UserComputations.toFullName(firstName, lastName);
 * }
 * }</pre>
 *
 * <h3>Example 2: Computation + Transformation</h3>
 * <pre>{@code
 * @Projection(from = User.class, providers = {
 *     @Provider(DateUtils.class),
 *     @Provider(Formatters.class)
 * })
 * public interface UserDTO {
 *
 *     @Computed(
 *         dependsOn = "createdAt",
 *         computedBy = @Method(value = "toInstant"),
 *         then = @Method(value = "toIsoString")
 *     )
 *     String getFormattedDate();
 * }
 *
 * public class DateUtils {
 *     public static Instant toInstant(LocalDateTime dt) {
 *         return dt.toInstant(ZoneOffset.UTC);
 *     }
 * }
 *
 * public class Formatters {
 *     public static String toIsoString(Instant instant) {
 *         return instant.toString();
 *     }
 * }
 *
 * // Generated code:
 * public String getFormattedDate() {
 *     LocalDateTime createdAt = source.getCreatedAt();
 *     Instant intermediate = DateUtils.toInstant(createdAt);
 *     String result = Formatters.toIsoString(intermediate);
 *     return result;
 * }
 * }</pre>
 *
 * <h3>Example 3: Explicit Provider Selection</h3>
 * <pre>{@code
 * @Projection(from = User.class, providers = {
 *     @Provider(LegacyCalculator.class),  // Has toAge() returning String
 *     @Provider(ModernCalculator.class)   // Has toAge() returning Integer
 * })
 * public interface UserDTO {
 *
 *     // Without explicit provider: uses LegacyCalculator (first match)
 *     @Computed(dependsOn = "birthDate")
 *     Integer getAge();  // Type mismatch error!
 *
 *     // With explicit provider: guaranteed correct method
 *     @Computed(
 *         dependsOn = "birthDate",
 *         computedBy = @Method(type = ModernCalculator.class, value = "toAge")
 *     )
 *     Integer getAge();  // ✅ Uses ModernCalculator.toAge() → Integer
 * }
 * }</pre>
 *
 * <h3>Example 4: Method Reuse Across Fields</h3>
 * <pre>{@code
 * public class StringUtils {
 *     public static String uppercase(String input) {
 *         return input != null ? input.toUpperCase() : null;
 *     }
 * }
 *
 * @Projection(from = User.class, providers = { @Provider(StringUtils.class) })
 * public interface UserDTO {
 *
 *     @Computed(
 *         dependsOn = "username",
 *         computedBy = @Method(value = "uppercase")
 *     )
 *     String getDisplayName();
 *
 *     @Computed(
 *         dependsOn = "department",
 *         computedBy = @Method(value = "uppercase")
 *     )
 *     String getDepartmentCode();
 *
 *     // Same method reused for different fields!
 * }
 * }</pre>
 *
 * <h3>Example 5: IoC-Managed Provider (Bean)</h3>
 * <pre>{@code
 * @Projection(from = Order.class, providers = {
 *     @Provider(value = TaxCalculator.class, bean = "taxService")
 * })
 * public interface OrderDTO {
 *
 *     @Computed(dependsOn = {"subtotal", "region"})
 *     BigDecimal getTotalWithTax();
 * }
 *
 * @Service("taxService")
 * public class TaxCalculator {
 *
 *     @Autowired
 *     private TaxRateRepository taxRateRepo;
 *
 *     // Instance method - can use injected dependencies
 *     public BigDecimal toTotalWithTax(BigDecimal subtotal, String region) {
 *         BigDecimal rate = taxRateRepo.findByRegion(region);
 *         return subtotal.multiply(BigDecimal.ONE.add(rate));
 *     }
 * }
 * }</pre>
 *
 * <h2>Resolution Strategy Reference</h2>
 *
 * <table border="1">
 *   <caption>How the annotation processor resolves computation methods</caption>
 *   <tr>
 *     <th>{@code computedBy} specified?</th>
 *     <th>{@code type} in {@code computedBy}?</th>
 *     <th>{@code value} in {@code computedBy}?</th>
 *     <th>Resolution Strategy</th>
 *   </tr>
 *   <tr>
 *     <td>❌ No</td>
 *     <td>N/A</td>
 *     <td>N/A</td>
 *     <td>Search DTO + all providers for {@code to[FieldName](...)}</td>
 *   </tr>
 *   <tr>
 *     <td>✅ Yes</td>
 *     <td>❌ No ({@code void.class})</td>
 *     <td>❌ No (empty)</td>
 *     <td>Same as not specified (convention-based)</td>
 *   </tr>
 *   <tr>
 *     <td>✅ Yes</td>
 *     <td>❌ No</td>
 *     <td>✅ Yes</td>
 *     <td>Search DTO + all providers for specified method name</td>
 *   </tr>
 *   <tr>
 *     <td>✅ Yes</td>
 *     <td>✅ Yes</td>
 *     <td>❌ No</td>
 *     <td>Search only specified provider for {@code to[FieldName](...)}</td>
 *   </tr>
 *   <tr>
 *     <td>✅ Yes</td>
 *     <td>✅ Yes</td>
 *     <td>✅ Yes</td>
 *     <td>Search only specified provider for specified method</td>
 *   </tr>
 * </table>
 *
 * <h2>Use Cases for {@code computedBy} Override</h2>
 *
 * <h3>Use Case 1: Avoid Naming Conflicts</h3>
 * <pre>{@code
 * public interface UserDTO {
 *     // Two fields, same dependencies, different formatting
 *
 *     @Computed(
 *         dependsOn = {"firstName", "lastName"},
 *         computedBy = @Method(value = "formatNormal")
 *     )
 *     String getFullName();  // "John Doe"
 *
 *     @Computed(
 *         dependsOn = {"firstName", "lastName"},
 *         computedBy = @Method(value = "formatReversed")
 *     )
 *     String getReversedName();  // "Doe, John"
 * }
 * }</pre>
 *
 * <h3>Use Case 2: Type Conversion Chain</h3>
 * <pre>{@code
 * @Computed(
 *     dependsOn = "createdAt",
 *     computedBy = @Method(value = "toTimestamp"),      // LocalDateTime → long
 *     then = @Method(value = "formatTimestamp")         // long → String
 * )
 * String getReadableDate();
 * }</pre>
 *
 * <h3>Use Case 3: Reuse Generic Utilities</h3>
 * <pre>{@code
 * // Multiple fields use the same currency conversion
 * @Computed(
 *     dependsOn = {"amount", "currency"},
 *     computedBy = @Method(value = "convertToUSD")
 * )
 * BigDecimal getAmountUSD();
 *
 * @Computed(
 *     dependsOn = {"tax", "currency"},
 *     computedBy = @Method(value = "convertToUSD")
 * )
 * BigDecimal getTaxUSD();
 * }</pre>
 *
 * <h2>Validation Rules</h2>
 * <p>The annotation processor enforces these constraints at compile-time:</p>
 *
 * <h3>For {@code computedBy} Methods</h3>
 * <ul>
 *   <li>Parameter types must match {@link #dependsOn()} field types <b>in order</b></li>
 *   <li>Return type must be compatible with field type (if no {@code then}) or with
 *       {@code then} input type (if {@code then} is specified)</li>
 *   <li>Can be static or instance (if provider is an IoC bean)</li>
 * </ul>
 *
 * <h3>For {@code then} Methods</h3>
 * <ul>
 *   <li>Must be {@code public static} (pure function)</li>
 *   <li>Must accept exactly one parameter (output of {@code computedBy})</li>
 *   <li>Return type must match the computed field type</li>
 *   <li>Method name must be explicitly specified (no convention)</li>
 * </ul>
 *
 * <h2>Comparison: computedBy vs then</h2>
 * <table border="1">
 *   <tr>
 *     <th>Aspect</th>
 *     <th>{@code computedBy}</th>
 *     <th>{@code then}</th>
 *   </tr>
 *   <tr>
 *     <td><b>Purpose</b></td>
 *     <td>Business logic / domain computation</td>
 *     <td>Type conversion / post-processing</td>
 *   </tr>
 *   <tr>
 *     <td><b>Input</b></td>
 *     <td>Source fields from {@code dependsOn}</td>
 *     <td>Output of {@code computedBy}</td>
 *   </tr>
 *   <tr>
 *     <td><b>Naming convention</b></td>
 *     <td>{@code to[FieldName](...)} if not specified</td>
 *     <td>No convention - method name required</td>
 *   </tr>
 *   <tr>
 *     <td><b>Method type</b></td>
 *     <td>Static or IoC bean instance method</td>
 *     <td>Static only (pure function)</td>
 *   </tr>
 *   <tr>
 *     <td><b>Complexity</b></td>
 *     <td>Can be complex (business rules, dependencies)</td>
 *     <td>Should be simple (1:1 transformation)</td>
 *   </tr>
 *   <tr>
 *     <td><b>Example</b></td>
 *     <td>{@code calculateTotalPrice(price, tax)}</td>
 *     <td>{@code toCurrency(bigDecimal)}</td>
 *   </tr>
 * </table>
 *
 * @since 1.0.0
 * @author Frank KOSSI
 * @see Projection
 * @see Provider
 * @see Projected
 * @see Method
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Computed {

    /**
     * Source fields that this computed field depends on.
     *
     * <p>Each dependency is a path from the source entity to a field value.
     * Dependencies are resolved at computation time and passed to the resolution
     * method in the order declared.</p>
     *
     * <h2>Path Syntax</h2>
     * <ul>
     *   <li><b>Simple field:</b> {@code "email"} → {@code source.getEmail()}</li>
     *   <li><b>Nested field:</b> {@code "address.city"} → {@code source.getAddress().getCity()}</li>
     *   <li><b>Collection traversal:</b> {@code "orders.total"} → requires {@link #reducers()}</li>
     * </ul>
     *
     * <h2>Dependency Rules</h2>
     * <ul>
     *   <li>All dependencies must reference fields in the {@link Projection#from()} source class</li>
     *   <li>Dependencies <b>cannot</b> reference other {@code @Computed} fields</li>
     *   <li>Parameter types are inferred from the source entity model</li>
     *   <li>Order matters: dependencies are passed to the method in this order</li>
     * </ul>
     *
     * <h2>Why Entity-Only Dependencies?</h2>
     * <p>Restricting dependencies to source fields (no computed-to-computed dependencies):</p>
     * <ul>
     *   <li>Eliminates dependency graph resolution complexity</li>
     *   <li>Prevents circular dependency issues</li>
     *   <li>Ensures each field can be computed independently</li>
     *   <li>Simplifies code generation and debugging</li>
     * </ul>
     *
     * <p><b>Design rationale:</b> If a computed field needs values derived from
     * another computation, both fields should declare their dependencies directly
     * from the source. The provider method can perform intermediate calculations internally.</p>
     *
     * <h2>Examples</h2>
     * <pre>{@code
     * // Simple dependency
     * @Computed(dependsOn = "createdAt")
     * String getFormattedDate();
     *
     * // Multiple dependencies
     * @Computed(dependsOn = {"firstName", "lastName", "middleName"})
     * String getFullName();
     *
     * // Nested field access
     * @Computed(dependsOn = {"address.city", "address.country"})
     * String getLocation();
     *
     * // Collection traversal (requires reducer)
     * @Computed(
     *     dependsOn = "orders.total",
     *     reducers = {Reduce.SUM}
     * )
     * BigDecimal getTotalOrders();
     * }</pre>
     *
     * @return array of source field paths that this computed field requires
     */
    String[] dependsOn();

    /**
     * Explicit reference to the computation method.
     *
     * <p>Use this to override the default convention-based method resolution
     * ({@code to[FieldName]}).</p>
     *
     * <h2>When to Use</h2>
     * <ul>
     *   <li>Multiple fields need similar computations with different method names</li>
     *   <li>Reusing a generic method for multiple fields</li>
     *   <li>Disambiguating when multiple providers have methods with the same name</li>
     *   <li>Targeting a specific provider among many</li>
     * </ul>
     *
     * <h2>Resolution Behavior</h2>
     * <table border="1">
     *   <tr>
     *     <th>Configuration</th>
     *     <th>Resolution Strategy</th>
     *   </tr>
     *   <tr>
     *     <td>Not specified (default)</td>
     *     <td>Search DTO + all providers for {@code to[FieldName]}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code @Method(value = "myMethod")}</td>
     *     <td>Search DTO + all providers for {@code myMethod}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code @Method(type = MyProvider.class)}</td>
     *     <td>Search only {@code MyProvider} for {@code to[FieldName]}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code @Method(type = MyProvider.class, value = "myMethod")}</td>
     *     <td>Search only {@code MyProvider.myMethod}</td>
     *   </tr>
     * </table>
     *
     * <h2>Examples</h2>
     * <pre>{@code
     * // Override method name only
     * @Computed(
     *     dependsOn = {"firstName", "lastName"},
     *     computedBy = @Method(value = "buildDisplayName")
     * )
     * String getFullName();
     *
     * // Target specific provider
     * @Computed(
     *     dependsOn = "amount",
     *     computedBy = @Method(type = ModernCalculator.class)
     * )
     * BigDecimal getTotal();  // Uses ModernCalculator.toTotal(...)
     *
     * // Both type and method
     * @Computed(
     *     dependsOn = "value",
     *     computedBy = @Method(type = StringUtils.class, value = "uppercase")
     * )
     * String getNormalized();
     * }</pre>
     *
     * @return method reference for explicit computation method resolution
     * @since 1.1.0
     * @see Method
     */
    Method computedBy() default @Method;

    /**
     * Optional pure transformation to apply to the result of {@link #computedBy()}.
     *
     * <p>This allows chaining a type conversion or post-processing function after
     * the initial computation, enabling better code reuse when existing methods
     * return a different type than what the DTO expects.</p>
     *
     * <h2>Method Resolution Strategy</h2>
     * <p>The transformation method name must be explicitly specified via {@link Method#value()}.
     * Unlike {@link #computedBy()}, no naming convention applies because the transformation
     * is type-driven (e.g., {@code Instant → String}) rather than field-driven.</p>
     *
     * <p>When {@link Method#type()} is not specified (default {@code void.class}),
     * the transformation method is searched using the same lookup strategy as
     * {@link #computedBy()}:</p>
     * <ol>
     *   <li>Search in the DTO interface itself (if the method is static)</li>
     *   <li>Search in all {@link Projection#providers()} in declaration order</li>
     *   <li>First match wins</li>
     * </ol>
     *
     * <p>When {@link Method#type()} is explicitly specified, the search is
     * restricted to that class only.</p>
     *
     * <h2>Requirements for Transformation Methods</h2>
     * <p>Methods referenced by {@code then} <b>must</b> satisfy these constraints:</p>
     * <ul>
     *   <li><b>Visibility:</b> Must be {@code public static}</li>
     *   <li><b>Method name:</b> Must be explicitly specified in {@link Method#value()}
     *       (no naming convention applies)</li>
     *   <li><b>Parameters:</b> Must accept exactly one parameter (the output type of {@link #computedBy()})</li>
     *   <li><b>Return type:</b> Must match the type expected by this computed field</li>
     *   <li><b>Purity:</b> Must be a pure function (no side effects, no state, deterministic)</li>
     *   <li><b>IoC:</b> IoC-managed beans are <b>not allowed</b> — transformations must be static</li>
     * </ul>
     *
     * <h2>Examples</h2>
     *
     * <h3>Type Conversion with Explicit Class</h3>
     * <pre>{@code
     * @Projection(from = User.class, providers = { @Provider(DateUtils.class) })
     * public interface UserDTO {
     *
     *     @Computed(
     *         dependsOn = "createdAt",
     *         computedBy = @Method(value = "toInstant"),
     *         then = @Method(type = StringUtils.class, value = "instantToIso")
     *     )
     *     String getFormattedDate();
     * }
     *
     * public class DateUtils {
     *     public static Instant toInstant(LocalDateTime dt) {
     *         return dt.toInstant(ZoneOffset.UTC);
     *     }
     * }
     *
     * public class StringUtils {
     *     public static String instantToIso(Instant instant) {
     *         return instant.toString();
     *     }
     * }
     * }</pre>
     *
     * <h3>Provider-Based Lookup</h3>
     * <pre>{@code
     * @Projection(from = Product.class, providers = {
     *     @Provider(PriceCalculator.class),
     *     @Provider(Formatters.class)
     * })
     * public interface ProductDTO {
     *
     *     @Computed(
     *         dependsOn = {"basePrice", "taxRate"},
     *         computedBy = @Method(value = "toCalculateTotal"),
     *         then = @Method(value = "toCurrency")  // Searched in providers
     *     )
     *     String getFormattedPrice();
     * }
     *
     * public class PriceCalculator {
     *     public static BigDecimal toCalculateTotal(BigDecimal base, BigDecimal tax) {
     *         return base.multiply(BigDecimal.ONE.add(tax));
     *     }
     * }
     *
     * public class Formatters {
     *     public static String toCurrency(BigDecimal amount) {
     *         return NumberFormat.getCurrencyInstance().format(amount);
     *     }
     * }
     * }</pre>
     *
     * <h3>Transformation in DTO Interface</h3>
     * <pre>{@code
     * @Projection(from = User.class, providers = { @Provider(UserComputations.class) })
     * public interface UserDTO {
     *
     *     @Computed(
     *         dependsOn = {"firstName", "lastName"},
     *         computedBy = @Method(value = "toConcatenate"),
     *         then = @Method(value = "toUpperCase")
     *     )
     *     String getFullNameUpper();
     *
     *     static String toUpperCase(String str) {
     *         return str == null ? null : str.toUpperCase();
     *     }
     * }
     * }</pre>
     *
     * <h2>Common Use Cases</h2>
     * <table border="1">
     *   <tr>
     *     <th>Use Case</th>
     *     <th>Example</th>
     *   </tr>
     *   <tr>
     *     <td>Type conversion</td>
     *     <td>{@code Instant → String}</td>
     *   </tr>
     *   <tr>
     *     <td>Formatting</td>
     *     <td>{@code BigDecimal → "€12.34"}</td>
     *   </tr>
     *   <tr>
     *     <td>Wrapping</td>
     *     <td>{@code String → Optional<String>}</td>
     *   </tr>
     *   <tr>
     *     <td>Post-processing</td>
     *     <td>{@code String → String.trim().toUpperCase()}</td>
     *   </tr>
     *   <tr>
     *     <td>Encoding</td>
     *     <td>{@code byte[] → Base64 String}</td>
     *   </tr>
     * </table>
     *
     * <h2>Why Pure Functions Only?</h2>
     * <p>Transformations must be pure (static, no side effects) because:</p>
     * <ul>
     *   <li><b>Predictability:</b> Same input always produces same output</li>
     *   <li><b>Testability:</b> Easy to unit test in isolation</li>
     *   <li><b>Composability:</b> Can be safely chained without hidden dependencies</li>
     *   <li><b>Performance:</b> No IoC lookup overhead, can be inlined by JIT</li>
     *   <li><b>Simplicity:</b> No lifecycle management, no injection concerns</li>
     * </ul>
     *
     * @return the transformation method reference, or {@code @Method} (default) for no transformation
     * @since 1.2.0
     * @see Method
     * @see #computedBy()
     */
    Method then() default @Method;

    /**
     * Reducer functions to apply to dependencies that traverse collections.
     *
     * <p>When a dependency path traverses one or more collections (e.g., {@code "orders.total"}),
     * a reducer <b>must</b> be specified to aggregate the multiple values into a single result.</p>
     *
     * <h2>Correspondence Rule</h2>
     * <p>Reducers correspond <b>only</b> to dependencies that traverse collections, in order:</p>
     * <pre>{@code
     * @Computed(
     *     dependsOn = {"id", "address.city", "orders.total", "orders.quantity"},
     *     //          scalar  scalar nested   collection     collection
     *     reducers = {Reduce.SUM, Reduce.COUNT}
     *     //          ↑ orders.total  ↑ orders.quantity
     * )
     * }</pre>
     *
     * <p><b>Constraint:</b> {@code reducers.length} must equal the number of dependencies
     * that traverse collections (as determined by the implementation based on the source model).</p>
     *
     * <h2>Path Semantics</h2>
     * <p>A path containing dots is <b>not</b> necessarily a collection traversal:</p>
     * <ul>
     *   <li>{@code "address.city"} → scalar nested field ({@code @Embedded} or {@code @ManyToOne})</li>
     *   <li>{@code "orders.total"} → collection traversal ({@code @OneToMany})</li>
     * </ul>
     * <p>The distinction depends on the source model and is determined by the implementation.</p>
     *
     * <h2>Collection Path Rule</h2>
     * <p>A path traversing a collection <b>must</b> end with a simple field:</p>
     * <pre>{@code
     * // ✅ VALID: traverses collection(s), ends with field
     * "orders.total"
     * "departments.teams.employees.salary"
     *
     * // ❌ INVALID: ends with a collection
     * "orders"
     * "departments.teams.employees"
     * }</pre>
     *
     * <h2>Standard Reducers</h2>
     * <p>The {@link Reduce} interface provides standard reducer constants. Implementations
     * may support additional custom reducers.</p>
     *
     * <h2>Examples</h2>
     * <pre>{@code
     * // Simple sum reduction
     * @Computed(
     *     dependsOn = "orders.total",
     *     reducers = {Reduce.SUM}
     * )
     * BigDecimal getTotalOrders();
     *
     * // Count elements
     * @Computed(
     *     dependsOn = "orders.id",
     *     reducers = {Reduce.COUNT}
     * )
     * Long getOrderCount();
     *
     * // Nested collections
     * @Computed(
     *     dependsOn = "departments.teams.employees.salary",
     *     reducers = {Reduce.AVG}
     * )
     * BigDecimal getAvgSalary();
     *
     * // Mix of scalars and collections
     * @Computed(
     *     dependsOn = {"id", "name", "orders.total", "refunds.amount"},
     *     reducers = {Reduce.SUM, Reduce.SUM}
     * )
     * String getFinancialSummary();
     * }</pre>
     *
     * @return array of reducer names for collection-traversing dependencies
     * @since 1.1.0
     * @see Reduce
     */
    String[] reducers() default {};

    /**
     * Standard reducer constants for aggregating collection values.
     *
     * <p>These constants represent common aggregation functions. Implementations
     * must support at least these standard reducers. Additional custom reducers
     * may be supported depending on the implementation.</p>
     *
     * @since 1.1.0
     */
    interface Reduce {
        /** Sum of all values in the collection. */
        String SUM = "SUM";

        /** Average (arithmetic mean) of all values in the collection. */
        String AVG = "AVG";

        /** Count of elements in the collection. */
        String COUNT = "COUNT";

        /** Minimum value in the collection. */
        String MIN = "MIN";

        /** Maximum value in the collection. */
        String MAX = "MAX";

        /** Count of distinct values in the collection. */
        String COUNT_DISTINCT = "COUNT_DISTINCT";
    }
}