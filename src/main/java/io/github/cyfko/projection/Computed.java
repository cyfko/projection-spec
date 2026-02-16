package io.github.cyfko.projection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a DTO method as a computed field whose value is derived from one or more
 * source fields through a computation method.
 *
 * <h2>Design Rationale</h2>
 * <p>The annotated method (a.k.a the <b>informative method</b>) itself <b>SHOULD NOT</b> expect parameters. The annotated
 * method is considered as the expected outcome, not the computation itself. The computation resolution mechanism
 * (a.k.a <b>the resolution method</b>) should be deferred to another method. This design add more flexibility and allow
 * code reuse and usage of dependencies on the resolution method side.</p>
 *
 * <p><b>Important:</b> It is to the implementation to defines whether this annotation apply both on fields and methods
 * or to choose one side between them.</p>
 *
 * <h2>Method Resolution</h2>
 * <p>The computation method dependencies types are infered from the source {@link Projection#from()} of the projection.</p>
 * <p>The computation method can be resolved in two ways:</p>
 *
 * <h3>1 Convention-based (default)</h3>
 * <p>By default, the system searches for a resolution method named {@code to[FullName]}:</p>
 * <pre>{@code
 * @Computed(dependsOn = {"firstName", "lastName"})
 * public String getFullName();
 *
 * // Automatically looks for: public String toFullName(String firstName, String lastName)
 * }</pre>
 *
 * <h3>2. Explicit method reference (override)</h3>
 * <p>Use {@link #computedBy()} to explicitly specify which method to use:</p>
 * <pre>{@code
 * @Computed(
 *   dependsOn = {"warehouse", "line", "itemId"},
 *   computedBy = @Method(method = "buildProductReference")
 * )
 * public String getProductReference();
 *
 * // Explicitly uses: String buildProductReference(String warehouse, int line, UUID itemId)
 * }</pre>
 *
 * <h3>Resolution Order</h3>
 * <ol>
 *   <li>If {@code computedBy.type()} is specified: search in that specific provider class</li>
 *   <li>If only {@code computedBy.method()} is specified: search in all providers for that method name</li>
 *   <li>If {@code computedBy} is not specified: use convention {@code get[FieldName]} in all providers</li>
 * </ol>
 *
 * <h2>Use Cases for Method Override</h2>
 *
 * <h3>Use Case 1: Avoid Naming Conflicts</h3>
 * <pre>{@code
 * public class UserDTO {
 *   // Two fields need the same source data but different formatting
 *
 *   @Computed(
 *     dependsOn = {"firstName", "lastName"},
 *     computedBy = @Method(method = "formatFullName")
 *   )
 *   public String getFullName();  // "John Doe"
 *
 *   @Computed(
 *     dependsOn = {"firstName", "lastName"},
 *     computedBy = @Method(method = "formatFullNameReversed")
 *   )
 *   public String getReversedName();  // "Doe, John"
 * }
 *
 * public class UserComputations {
 *   public static String formatFullName(String first, String last) {
 *     return first + " " + last;
 *   }
 *
 *   public static String formatFullNameReversed(String first, String last) {
 *     return last + ", " + first;
 *   }
 * }
 * }</pre>
 *
 * <h3>Use Case 2: Multiple Fields Using Same Computation</h3>
 * <pre>{@code
 * public class OrderDTO {
 *   @Computed(
 *     dependsOn = {"amount", "currency"},
 *     computedBy = @Method(method = "convertToUSD")
 *   )
 *   public BigDecimal amountInUSD;
 *
 *   @Computed(
 *     dependsOn = {"taxes", "currency"},
 *     computedBy = @Method(method = "convertToUSD")
 *   )
 *   public BigDecimal taxesInUSD;
 *
 *   @Computed(
 *     dependsOn = {"shipping", "currency"},
 *     computedBy = @Method(method = "convertToUSD")
 *   )
 *   public BigDecimal shippingInUSD;
 * }
 *
 * public class CurrencyUtils {
 *   // Same method reused for different fields!
 *   public static BigDecimal convertToUSD(BigDecimal amount, String currency) {
 *     // conversion logic
 *   }
 * }
 * }</pre>
 *
 * <h3>Use Case 3: Target Specific Provider</h3>
 * <pre>{@code
 * @Projection(
 *   from = User.class,
 *   providers = {
 *     @Provider(LegacyComputations.class),    // Has getAge() returning String
 *     @Provider(ModernComputations.class)     // Has getAge() returning Integer
 *   }
 * )
 * public class UserDTO {
 *   // Without override: might use wrong provider (first match)
 *   @Computed(dependsOn = {"birthDate"})
 *   public Integer age;  // Could accidentally use LegacyComputations.getAge()!
 *
 *   // With override: explicitly targets the correct provider
 *   @Computed(
 *     dependsOn = {"birthDate"},
 *     computedBy = @Method(
 *       type = ModernComputations.class,
 *       method = "getAge"
 *     )
 *   )
 *   public Integer age;  // Guaranteed to use ModernComputations.getAge()
 * }
 * }</pre>
 *
 * <h3>Use Case 4: Reuse Generic Methods</h3>
 * <pre>{@code
 * public class StringUtils {
 *   public static String uppercase(String input) {
 *     return input != null ? input.toUpperCase() : null;
 *   }
 *
 *   public static String lowercase(String input) {
 *     return input != null ? input.toLowerCase() : null;
 *   }
 * }
 *
 * public class UserDTO {
 *   @Computed(
 *     dependsOn = {"username"},
 *     computedBy = @Method(
 *       type = StringUtils.class,
 *       method = "uppercase"
 *     )
 *   )
 *   public String displayName;
 *
 *   @Computed(
 *     dependsOn = {"email"},
 *     computedBy = @Method(
 *       type = StringUtils.class,
 *       method = "lowercase"
 *     )
 *   )
 *   public String normalizedEmail;
 * }
 * }</pre>
 *
 * <h2>Method Resolution Strategy</h2>
 * <table border="1">
 *   <caption>How the processor finds the computation method</caption>
 *   <tr>
 *     <th>{@code computedBy} specified?</th>
 *     <th>{@code type} in computedBy?</th>
 *     <th>{@code method} in computedBy?</th>
 *     <th>Resolution Strategy</th>
 *   </tr>
 *   <tr>
 *     <td>❌ No</td>
 *     <td>N/A</td>
 *     <td>N/A</td>
 *     <td>Search all providers for {@code to[FieldName](...)}</td>
 *   </tr>
 *   <tr>
 *     <td>✅ Yes</td>
 *     <td>❌ No</td>
 *     <td>❌ No</td>
 *     <td>Same as not specified (convention)</td>
 *   </tr>
 *   <tr>
 *     <td>✅ Yes</td>
 *     <td>❌ No</td>
 *     <td>✅ Yes</td>
 *     <td>Search all providers for specified method name</td>
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
 * <h2>Validation Rules</h2>
 * <p>Whether using convention or explicit reference, the method must:</p>
 * <ul>
 *   <li>Have parameters matching {@code dependsOn} types <b>in order</b></li>
 *   <li>Return a type compatible with the computed field type</li>
 *   <li>Be either static or instance (resolved at runtime via IoC)</li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <h3>Example 1: Convention (Simple)</h3>
 * <pre>{@code
 * @Projection(
 *   from = User.class,
 *   providers = {@Provider(UserComputations.class)}
 * )
 * public class UserDTO {
 *   @Computed(dependsOn = {"firstName", "lastName"})
 *   public String fullName;  // Looks for: getFullName(String, String)
 * }
 * }</pre>
 *
 * <h3>Example 2: Override Method Name</h3>
 * <pre>{@code
 * @Projection(
 *   from = User.class,
 *   providers = {@Provider(UserComputations.class)}
 * )
 * public class UserDTO {
 *   @Computed(
 *     dependsOn = {"firstName", "lastName"},
 *     computedBy = @Method(method = "formatUserName")
 *   )
 *   public String fullName;  // Looks for: formatUserName(String, String)
 * }
 * }</pre>
 *
 * <h3>Example 3: Target Specific Provider</h3>
 * <pre>{@code
 * @Projection(
 *   from = User.class,
 *   providers = {
 *     @Provider(LegacyUtils.class),
 *     @Provider(ModernUtils.class)
 *   }
 * )
 * public class UserDTO {
 *   @Computed(
 *     dependsOn = {"birthDate"},
 *     computedBy = @Method(
 *       type = ModernUtils.class,
 *       method = "calculateAge"
 *     )
 *   )
 *   public Integer getAge();  // Only searches in ModernUtils.calculateAge(LocalDate)
 * }
 * }</pre>
 *
 * <h3>Example 4: Reuse Same Method for Multiple Fields</h3>
 * <pre>{@code
 * @Projection(
 *   from = Product.class,
 *   providers = {@Provider(FormatUtils.class)}
 * )
 * public class ProductDTO {
 *   @Computed(
 *     dependsOn = {"name"},
 *     computedBy = @Method(method = "uppercase")
 *   )
 *   public String getDisplayName();
 *
 *   @Computed(
 *     dependsOn = {"category"},
 *     computedBy = @Method(method = "uppercase")
 *   )
 *   public String getDisplayCategory();
 * }
 *
 * public class FormatUtils {
 *   public static String uppercase(String input) {
 *     return input != null ? input.toUpperCase() : null;
 *   }
 * }
 * }</pre>
 *
 * <h2>Compilation Errors</h2>
 * <p>The processor generates errors for:</p>
 * <ul>
 *   <li><b>Method not found:</b> Specified method doesn't exist in target provider(s)</li>
 *   <li><b>Provider not found:</b> Specified {@code type} not in {@link Projection#providers()}</li>
 *   <li><b>Parameter mismatch:</b> Method parameters don't match {@code dependsOn} types</li>
 *   <li><b>Return type mismatch:</b> Method return type incompatible with field type</li>
 * </ul>
 *
 * <h3>Example Error Messages</h3>
 * <pre>
 * ❌ Method 'formatUserName' not found in any provider.
 *    Searched in: UserComputations, CommonUtils
 *    Expected signature: String formatUserName(String, String)
 *
 * ❌ Provider 'ModernUtils' not found.
 *    Available providers: LegacyUtils, CommonUtils
 *    Add @Provider(ModernUtils.class) to @Projection
 *
 * ❌ Method 'calculateAge' found in ModernUtils but with wrong signature.
 *    Expected: Integer calculateAge(LocalDate)
 *    Found: String calculateAge(String)
 * </pre>
 *
 * <h2>Best Practices</h2>
 * <ol>
 *   <li><b>Prefer Convention:</b> Use {@code computedBy} only when necessary</li>
 *   <li><b>Document Overrides:</b> Comment why you're overriding the convention</li>
 *   <li><b>Avoid Ambiguity:</b> If multiple providers have same method, use {@code type} to specify</li>
 *   <li><b>Generic Methods:</b> Create reusable utility methods for common transformations</li>
 *   <li><b>Provider Order:</b> Put most specific providers first in {@link Projection#providers()}</li>
 * </ol>
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
     * Array of source field names that this computed field depends on.
     *
     * <p><b>Critical constraint:</b> All field names must reference fields from the source
     * source class declared in {@link Projection#from()}. References to other computed fields
     * or DTO-specific fields are not permitted.</p>
     *
     * <p>This array defines:</p>
     * <ul>
     *   <li>Which source fields will be passed to the provider method</li>
     *   <li>The expected parameter types and order for the provider method</li>
     *   <li>The validation contract for compile-time checking</li>
     * </ul>
     *
     * <h3>Order Matters</h3>
     * <p>The order of field names must match the parameter order in the provider method:</p>
     * <pre>{@code
     * @Computed(dependsOn = {"firstName", "lastName"})  // Order: firstName, then lastName
     * String getFullName();
     *
     * // Correct method signature
     * public static String getFullName(String firstName, String lastName) { ... }
     *
     * // WRONG - parameters in wrong order
     * public static String getFullName(String lastName, String firstName) { ... }
     * }</pre>
     *
     * <h3>Why Entity-Only Dependencies?</h3>
     * <p>Restricting dependencies to source fields (no computed-to-computed dependencies):</p>
     * <ul>
     *   <li>Eliminates dependency graph resolution complexity</li>
     *   <li>Prevents circular dependency issues</li>
     *   <li>Ensures each field can be computed independently</li>
     *   <li>Simplifies code generation and debugging</li>
     * </ul>
     *
     * <p><b>Design rationale:</b> If a computed field needs values that would come from
     * another computed field, both should declare their dependencies directly from the source.
     * The provider method can perform any necessary intermediate calculations internally.</p>
     *
     * <h3>Examples</h3>
     * <pre>{@code
     * // Simple dependency
     * @Computed(dependsOn = {"createdAt"})
     * public String formattedDate;
     *
     * // Multiple dependencies
     * @Computed(dependsOn = {"firstName", "lastName", "middleName"})
     * public String fullNameWithMiddle;
     *
     * // Nested source field access (assuming User has Address relationship)
     * @Computed(dependsOn = {"address.city", "address.country"})
     * public String location;
     * }</pre>
     *
     * @return array of source field paths that this computed field requires
     */
    String[] dependsOn();

    /**
     * Optional explicit reference to the computation method.
     *
     * <p>Use this to override the default convention-based method resolution.</p>
     *
     * <h3>When to use:</h3>
     * <ul>
     *   <li>Multiple fields need similar computations with different method names</li>
     *   <li>Reusing the same generic method for multiple fields</li>
     *   <li>Disambiguating when multiple providers have methods with same name</li>
     *   <li>Targeting a specific provider among many</li>
     * </ul>
     *
     * <h3>Resolution behavior:</h3>
     * <table border="1">
     *   <tr>
     *     <th>Configuration</th>
     *     <th>Resolution Strategy</th>
     *   </tr>
     *   <tr>
     *     <td>Not specified (default)</td>
     *     <td>Search all providers for {@code get[FieldName]}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code @Method(method = "myMethod")}</td>
     *     <td>Search all providers for {@code myMethod}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code @Method(type = MyProvider.class)}</td>
     *     <td>Search only {@code MyProvider} for {@code get[FieldName]}</td>
     *   </tr>
     *   <tr>
     *     <td>{@code @Method(type = MyProvider.class, method = "myMethod")}</td>
     *     <td>Search only {@code MyProvider.myMethod}</td>
     *   </tr>
     * </table>
     *
     * <h3>Examples:</h3>
     * <pre>{@code
     * // Override method name only
     * @Computed(
     *   dependsOn = {"firstName", "lastName"},
     *   computedBy = @Method(method = "buildDisplayName")
     * )
     * public String fullName;
     *
     * // Target specific provider
     * @Computed(
     *   dependsOn = {"amount"},
     *   computedBy = @Method(type = ModernCalculator.class)
     * )
     * public BigDecimal total;  // Uses ModernCalculator.getTotal(...)
     *
     * // Both type and method
     * @Computed(
     *   dependsOn = {"value"},
     *   computedBy = @Method(
     *     type = StringUtils.class,
     *     method = "uppercase"
     *   )
     * )
     * public String normalized;
     * }</pre>
     *
     * @return method reference for explicit computation method resolution
     * @since 1.1.0
     */
    Method computedBy() default @Method;

    /**
     * Array of reducer functions to apply to dependencies that traverse collections.
     *
     * <p>When a dependency path traverses one or more collections (e.g., {@code "orders.total"}),
     * a reducer <b>must</b> be specified to aggregate the multiple values into a single result.</p>
     *
     * <h3>Correspondence Rule</h3>
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
     * <h3>Path Semantics</h3>
     * <p>A path containing dots is <b>not</b> necessarily a collection traversal:</p>
     * <ul>
     *   <li>{@code "address.city"} → scalar nested field ({@code @Embedded} or {@code @ManyToOne})</li>
     *   <li>{@code "orders.total"} → collection traversal ({@code @OneToMany})</li>
     * </ul>
     * <p>The distinction depends on the source model and is determined by the implementation.</p>
     *
     * <h3>Collection Path Rule</h3>
     * <p>A path traversing a collection <b>must</b> end with a simple field:</p>
     * <pre>{@code
     * // ✅ VALID: traverses collection(s), ends with field
     * "orders.total"                       // traverses orders, ends with total
     * "departments.teams.employees.salary" // traverses 3 collections, ends with salary
     *
     * // ❌ INVALID: ends with a collection
     * "orders"                             // no final field
     * "departments.teams.employees"        // ends with collection
     * }</pre>
     *
     * <h3>Standard Reducers</h3>
     * <p>The {@link Reduce} interface provides standard reducer constants. Implementations
     * may support additional custom reducers.</p>
     *
     * <h3>Examples</h3>
     * <pre>{@code
     * // Simple sum reduction
     * @Computed(
     *     dependsOn = {"orders.total"},
     *     reducers = {Reduce.SUM}
     * )
     * public BigDecimal totalOrders;
     *
     * // Count elements (must end with a field!)
     * @Computed(
     *     dependsOn = {"orders.id"},
     *     reducers = {Reduce.COUNT}
     * )
     * public Long orderCount;
     *
     * // Nested collections
     * @Computed(
     *     dependsOn = {"departments.teams.employees.salary"},
     *     reducers = {Reduce.AVG}
     * )
     * public BigDecimal avgSalary;
     *
     * // Mix of scalars and collections
     * @Computed(
     *     dependsOn = {"id", "name", "orders.total", "refunds.amount"},
     *     reducers = {Reduce.SUM, Reduce.SUM}
     * )
     * public String financialSummary;
     *
     * // Custom reducer (implementation-specific)
     * @Computed(
     *     dependsOn = {"transactions.amount"},
     *     reducers = {"STDDEV"}
     * )
     * public Double standardDeviation;
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
     * <h3>Usage</h3>
     * <pre>{@code
     * @Computed(
     *     dependsOn = {"orders.total"},
     *     reducers = {Computed.Reduce.SUM}
     * )
     * public BigDecimal totalOrders;
     * }</pre>
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