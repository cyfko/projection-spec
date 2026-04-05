package io.github.cyfko.projection;

import java.lang.annotation.*;

/**
 * Declares that a {@link Projection} interface should be exposed as a
 * queryable resource, enabling criteria-based access to its data.
 *
 * <p>This annotation is <strong>entirely agnostic</strong> of transport protocol
 * (HTTP, gRPC, GraphQL, messaging...), delivery mechanism (REST controller,
 * resolver, handler, consumer...), and response format. These concerns are
 * entirely delegated to the annotation processor implementation.
 *
 * <p>The concepts expressed here — resource identity, logical namespace,
 * result cardinality, transformation pipeline, and custom handling — are
 * abstract and must be interpreted by each implementation according to
 * its own conventions.
 *
 * <p>Has no effect if used without {@link Projection}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Projection(from = User.class)
 * @Exposure(value = "users", namespace = "api")
 * public interface UserDTO {
 *     // ...
 * }
 * }</pre>
 *
 * <h2>Use Cases</h2>
 *
 * <h3>Basic resource exposure</h3>
 * <pre>{@code
 * @Projection(from = Product.class)
 * @Exposure("products")
 * public interface ProductDTO { ... }
 * }</pre>
 *
 * <h3>Resource within a namespace</h3>
 * <pre>{@code
 * @Projection(from = Invoice.class)
 * @Exposure(value = "invoices", namespace = "billing")
 * public interface InvoiceDTO { ... }
 * }</pre>
 *
 * <h3>Full result set with transformation pipeline</h3>
 * <pre>{@code
 * @Projection(from = AuditEntry.class)
 * @Exposure(
 *     value = "audit-logs",
 *     strategy = Exposure.Strategy.FULL,
 *     pipes = { @Method("enforceCurrentTenant") }
 * )
 * public interface AuditLogDTO { ... }
 * }</pre>
 *
 * <h3>Custom handler with pipeline</h3>
 * <pre>{@code
 * @Projection(from = Report.class)
 * @Exposure(
 *     value = "reports",
 *     strategy = Exposure.Strategy.CUSTOM,
 *     pipes = {
 *         @Method("enforceAccessControl"),
 *         @Method("applyDateRange")
 *     },
 *     handler = @Method(type = ReportService.class, value = "generate")
 * )
 * public interface ReportDTO { ... }
 * }</pre>
 *
 * @since 3.0.0
 * @author Frank KOSSI
 * @see Projection
 * @see ExposedAs
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Exposure {

    /**
     * Logical name identifying the exposed resource within the system.
     *
     * <p>Implementations may use this to derive any resource identifier
     * meaningful in their context: a route segment, a topic name, a service
     * operation, a GraphQL type, or any other naming construct.
     *
     * <p>Defaults to a normalized form of the entity class name if not specified.
     *
     * <h3>Interpretation examples by implementation</h3>
     * <ul>
     *   <li>A REST implementation may map {@code "users"} to {@code /users/search}</li>
     *   <li>A GraphQL implementation may generate a {@code users} query type</li>
     *   <li>A messaging implementation may publish to a {@code users.query} topic</li>
     * </ul>
     */
    String value() default "";

    /**
     * Optional logical namespace grouping this resource with related resources.
     *
     * <p>The concept of namespace is intentionally abstract. Implementations
     * may interpret this as a module, a domain boundary, a service group,
     * a version scope, or any other organizational unit meaningful in their
     * context. No format or structure is enforced by this specification.
     *
     * <h3>Interpretation examples by implementation</h3>
     * <ul>
     *   <li>A REST implementation may use it as a URI prefix: {@code "admin"} → {@code /admin/users}</li>
     *   <li>A GraphQL implementation may use it as a schema module or namespace</li>
     *   <li>A messaging implementation may prepend it to topic names: {@code "billing"} → {@code billing.invoices}</li>
     *   <li>A versioned API may use it as a version scope: {@code "v2"}</li>
     * </ul>
     */
    String namespace() default "";

    /**
     * Defines the result cardinality strategy for the exposed resource.
     *
     * <p>Cardinality describes the intent regarding <em>how much</em> of
     * the matching result set is returned to the consumer, and whether the
     * consumer controls a window over it. Implementations translate this
     * concept into their own concrete response shapes.
     *
     * @see Strategy
     */
    Strategy strategy() default Strategy.WINDOWED;

    /**
     * Ordered pipeline of query transformations applied before the handler.
     *
     * <p>Each pipe is a pure transformation unit: it receives a query
     * context and returns a (possibly modified) query context. Pipes are
     * applied in declaration order:
     * <pre>
     * QueryContext → Pipe1 → Pipe2 → ... → Handler
     * </pre>
     *
     * <p>Pipes express cross-cutting concerns: tenant isolation, access
     * control, input enrichment, constraint enforcement, etc. The method
     * name is always required for pipes.
     *
     * <h3>Typical use cases</h3>
     * <ul>
     *   <li><b>Tenant isolation:</b> inject a mandatory tenant criterion so
     *       that each consumer only sees its own data</li>
     *   <li><b>Access control:</b> restrict results based on the current
     *       user's permissions or roles</li>
     *   <li><b>Input enrichment:</b> add default sorting, impose maximum
     *       result limits, or normalize incoming criteria</li>
     *   <li><b>Constraint enforcement:</b> ensure a date range is always
     *       present, or reject queries that are too broad</li>
     * </ul>
     *
     * @see Method
     */
    Method[] pipes() default {};

    /**
     * Reference to the method that consumes the query context and produces
     * the resource result.
     *
     * <p>If not specified, the implementation generates a default handler
     * according to its own conventions. When specified, the implementation
     * delegates execution to this method and may propagate any metadata
     * present on it (security markers, caching hints, observability, etc.).
     *
     * <h3>Typical use cases</h3>
     * <ul>
     *   <li><b>Custom data source:</b> query a search engine, a cache layer,
     *       or an external service instead of the default data source</li>
     *   <li><b>Aggregated results:</b> combine data from multiple sources
     *       before returning the result</li>
     *   <li><b>Side effects:</b> trigger auditing, metrics, or notifications
     *       alongside the query execution</li>
     * </ul>
     *
     * @see Method
     */
    Method handler() default @Method();

    /**
     * Defines the result cardinality concept for an exposed resource.
     *
     * <p>These values express abstract cardinality contracts. Implementations
     * are responsible for translating each concept into a concrete response
     * shape, delivery mechanism, or streaming strategy appropriate to their
     * context. No implementation detail is implied or mandated by these values.
     */
    enum Strategy {

        /**
         * The consumer controls a <em>window</em> over the result set.
         *
         * <p>The intent is that the full matching result set may exceed what
         * is returned in a single response, and that the consumer can express
         * a bounded view over it. How the window is defined, communicated,
         * and navigated is entirely up to the implementation.
         *
         * <h3>Interpretation examples by implementation</h3>
         * <ul>
         *   <li>A REST implementation may use offset/limit or cursor-based pagination</li>
         *   <li>A GraphQL implementation may use Relay-style connections with edges and cursors</li>
         *   <li>A streaming implementation may deliver results in bounded chunks</li>
         * </ul>
         */
        WINDOWED,

        /**
         * The full matching result set is returned in a single response.
         *
         * <p>No windowing or consumer-driven navigation is involved.
         * The implementation may apply internal safety bounds, but the
         * declared intent is to return all matching results at once.
         *
         * <h3>Typical use cases</h3>
         * <ul>
         *   <li>Reference data lookups (countries, currencies, status codes)</li>
         *   <li>Configuration entries with known small cardinality</li>
         *   <li>Export operations where completeness is required</li>
         * </ul>
         */
        FULL,

        /**
         * The result cardinality and shape are entirely defined by the handler.
         *
         * <p>The implementation does not enforce any cardinality contract.
         * The handler is the sole authority over the result shape.
         *
         * <h3>Typical use cases</h3>
         * <ul>
         *   <li>Aggregated dashboards returning statistics instead of entities</li>
         *   <li>Report generation producing a file or a structured summary</li>
         *   <li>Hybrid responses mixing entities with computed metadata</li>
         * </ul>
         */
        CUSTOM
    }
}
