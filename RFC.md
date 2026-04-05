# RFC — Migration de `@ExposedAs` et `@Exposure` vers `projection-spec`

**Projet** : projection-spec  
**Version cible** : 3.0.0  
**Statut** : Proposition  
**Auteur** : Kunrin SA  
**Date** : 2026-04-05  
**Dépendances impactées** : `filterql-spring`, `filterql-core`

---

## 1. Contexte

`projection-spec` se définit comme une **spécification d'annotations framework-agnostique** pour le mapping déclaratif de DTOs. Elle contient aujourd'hui `@Projection`, `@Projected`, `@Computed`, `@Provider`, `@Method`.

`@ExposedAs` et `@Exposure` résident actuellement dans `filterql-spring`, un module spécifique à Spring Boot. Or ces deux annotations :

- sont **sémantiquement liées** à `@Projected` et `@Projection` (déjà dans `projection-spec`)
- ne contiennent **aucune dépendance Spring** dans leur structure intrinsèque
- devraient pouvoir être lues par **tout adapter** (Spring Boot, Micronaut, Quarkus, ou même un processeur standalone)

De plus, `@ExposedAs` référence actuellement `Op` (enum dans `filterql-core`), ce qui crée un couplage vers une implémentation concrète et empêche l'agnosticisme.

---

## 2. Problèmes identifiés

**Mauvais placement** — `@ExposedAs` et `@Exposure` dans `filterql-spring` signifie qu'un adapter non-Spring devrait dépendre d'un module Spring pour lire des métadonnées de projection. C'est une inversion de dépendance.

**Couplage via `Op`** — `@ExposedAs(operators = {Op.EQ, Op.MATCHES})` lie statiquement la spec à l'enum `Op` de `filterql-core`. Un adapter alternatif ne peut pas étendre ou remplacer les opérateurs sans modifier `filterql-core`.

**Extensibilité bloquée** — Si un adapter souhaite supporter des opérateurs custom (`SOUNDEX`, `GEO_WITHIN`, `FULL_TEXT`...), il n'existe aucun point d'extension dans la spec actuelle.

---

## 3. Proposition de Solution

### 3.1 Migration de `@ExposedAs` dans `projection-spec`

`@ExposedAs` est reformulé avec `String[] operators()` au lieu de `Op[] operators()`. Chaque implémentation déclare ses propres constantes d'opérateurs. La spec fournit un ensemble d'opérateurs **standards** sous forme de constantes `String` dans une interface `StandardOp`.

```java
package io.github.cyfko.projection;

import java.lang.annotation.*;

/**
 * Declares a DTO field as filterable, and optionally customizes its symbolic name
 * and the set of operators allowed for filtering.
 *
 * <p>This annotation is framework-agnostic. Operator values are plain strings,
 * allowing implementations to define their own operator sets while remaining
 * compatible with the standard operators defined in {@link StandardOp}.
 *
 * <p><strong>Valid targets:</strong>
 * <ul>
 *   <li>A getter method on a {@link Projection} interface returning a
 *       <em>scalar-compatible type</em> (primitives, String, temporal types,
 *       enums, and other value types with no nested filterable structure)</li>
 *   <li>A method in a {@link Provider} class defining a <em>virtual filter
 *       field</em> — a filterable criterion with custom resolution logic that
 *       does not map directly to a single source field</li>
 * </ul>
 *
 * <p><strong>Forbidden target:</strong> A method returning a type annotated
 * with {@link Projection}. Since such a type participates in
 * <em>composed filter inheritance</em>, applying {@code @ExposedAs} directly
 * on it is a compile-time error. The filterable properties of the nested
 * projection are inherited automatically under the field's logical prefix.
 *
 * @see Projection
 * @see Projected
 * @see StandardOp
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ExposedAs {

    /**
     * The symbolic name exposed in filter criteria (e.g. "SITE_NAME", "CREATED_ON").
     * Defaults to the SCREAMING_SNAKE_CASE form of the method name if empty.
     */
    String value() default "";

    /**
     * Supported filter operators for this field.
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
     * not be exposed in the public filter API.
     */
    boolean exposed() default true;
}
```

### 3.2 Ajout de `StandardOp` dans `projection-spec`

`StandardOp` est une interface de constantes `String` définissant les opérateurs que **toute implémentation doit supporter**. Les implémentations peuvent étendre cette liste librement.

```java
package io.github.cyfko.projection;

/**
 * Standard filter operators that any compliant implementation of projection-spec
 * MUST support.
 *
 * <p>These constants are plain {@code String} values, keeping the specification
 * entirely agnostic of query language, persistence mechanism, and framework.
 * Implementations translate each operator into their own native filtering
 * construct according to their context.
 *
 * <p>Implementations MAY support additional operators beyond this standard set.
 * Custom operators should be documented by the implementation and handled
 * through the implementation's own extension mechanism.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @ExposedAs(value = "NAME", operators = {StandardOp.EQ, StandardOp.MATCHES, StandardOp.IN})
 * String getName();
 *
 * @ExposedAs(value = "AGE", operators = {StandardOp.GTE, StandardOp.LTE, StandardOp.RANGE})
 * Integer getAge();
 * }</pre>
 *
 * @see ExposedAs
 */
public interface StandardOp {

    // ── Equality ────────────────────────────────────────────────────────────

    /** Strict equality: {@code field = value} */
    String EQ = "EQ";

    /** Strict inequality: {@code field != value} */
    String NE = "NE";

    // ── Comparison ──────────────────────────────────────────────────────────

    /** Strictly greater than: {@code field > value} */
    String GT = "GT";

    /** Greater than or equal: {@code field >= value} */
    String GTE = "GTE";

    /** Strictly less than: {@code field < value} */
    String LT = "LT";

    /** Less than or equal: {@code field <= value} */
    String LTE = "LTE";

    // ── Range ───────────────────────────────────────────────────────────────

    /** Inclusive range: {@code field BETWEEN min AND max} */
    String RANGE = "RANGE";

    /** Outside inclusive range: {@code field NOT BETWEEN min AND max} */
    String NOT_RANGE = "NOT_RANGE";

    // ── Pattern ─────────────────────────────────────────────────────────────

    /** Case-insensitive contains: {@code LOWER(field) LIKE '%value%'} */
    String MATCHES = "MATCHES";

    /** Case-insensitive does not contain: {@code LOWER(field) NOT LIKE '%value%'} */
    String NOT_MATCHES = "NOT_MATCHES";

    // ── List ────────────────────────────────────────────────────────────────

    /** Field value is in the provided list: {@code field IN (v1, v2, ...)} */
    String IN = "IN";

    /** Field value is not in the provided list: {@code field NOT IN (v1, v2, ...)} */
    String NOT_IN = "NOT_IN";

    // ── Nullity ─────────────────────────────────────────────────────────────

    /** Field is null: {@code field IS NULL} */
    String IS_NULL = "IS_NULL";

    /** Field is not null: {@code field IS NOT NULL} */
    String NOT_NULL = "NOT_NULL";
}
```

### 3.3 Migration de `@Exposure` dans `projection-spec`

`@Exposure` est reformulé au niveau des **concepts** uniquement. Il ne présuppose aucun protocole de transport (HTTP, gRPC, GraphQL...), aucun format de réponse, et aucun mécanisme de livraison. Chaque implémentation interprète ces concepts dans son propre contexte.

Changements notables :
- `basePath` → `namespace` (concept logique, pas un chemin URI)
- `endpointName` supprimé (redondant avec `@Method` qui fournit déjà un nom)
- `PAGINATED` → `WINDOWED` (concept de fenêtre, pas de pagination avec metadata)
- `LIST` → `FULL` (concept de résultat complet, pas de liste au sens Java)
- Toute la documentation évite les termes HTTP, URI, REST, page, metadata

```java
package io.github.cyfko.projection;

import java.lang.annotation.*;

/**
 * Declares that a {@link Projection} interface should be exposed as a
 * queryable resource, enabling dynamic filtering on its data.
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
     */
    String value() default "";

    /**
     * Optional logical namespace grouping this resource with related resources.
     *
     * <p>The concept of namespace is intentionally abstract. Implementations
     * may interpret this as a module, a domain boundary, a service group,
     * a version scope, or any other organizational unit meaningful in their
     * context. No format or structure is enforced by this specification.
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
     * Ordered pipeline of filter transformations applied before the handler.
     *
     * <p>Each pipe is a pure transformation unit: it receives a filter
     * context and returns a (possibly modified) filter context. Pipes are
     * applied in declaration order:
     * <pre>
     * FilterContext → Pipe1 → Pipe2 → ... → Handler
     * </pre>
     *
     * <p>Pipes express cross-cutting concerns: tenant isolation, access
     * control, input enrichment, constraint enforcement, etc. The method
     * name is always required for pipes.
     *
     * @see Method
     */
    Method[] pipes() default {};

    /**
     * Reference to the method that consumes the filter context and produces
     * the resource result.
     *
     * <p>If not specified, the implementation generates a default handler
     * according to its own conventions. When specified, the implementation
     * delegates execution to this method and may propagate any metadata
     * present on it (security markers, caching hints, observability, etc.).
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
         */
        WINDOWED,

        /**
         * The full matching result set is returned in a single response.
         *
         * <p>No windowing or consumer-driven navigation is involved.
         * The implementation may apply internal safety bounds, but the
         * declared intent is to return all matching results at once.
         */
        FULL,

        /**
         * The result cardinality and shape are entirely defined by the handler.
         *
         * <p>The implementation does not enforce any cardinality contract.
         * The handler is the sole authority over the result shape.
         */
        CUSTOM
    }
}
```

---

## 4. Impact sur `@Projected` — Attributs `as` et `cycleBreak`

Dans le cadre du RFC sur le filtrage composé par héritage de projection, `@Projected` reçoit deux nouveaux attributs directement dans `projection-spec` :

```java
public @interface Projected {

    /** Source field path (dot notation supported). */
    String from() default "";

    /**
     * Logical prefix used when this field's nested @Projection type
     * participates in composed filter inheritance.
     *
     * <p>If empty, defaults to the SCREAMING_SNAKE_CASE form of the method name.
     * Has no effect if the return type is not a {@link Projection} type.
     */
    String as() default "";

    /**
     * If {@code true}, excludes this field from composed filter inheritance,
     * breaking potential cycles.
     *
     * <p>The field remains available for projection (read), but its
     * {@link Projection} type's filterable properties are NOT inherited
     * by the parent DTO.
     *
     * <p>Required when a cycle is detected at compile time:
     * <pre>
     * A → B → A  (cycle: set cycleBreak = true on one side)
     * </pre>
     */
    boolean cycleBreak() default false;
}
```

---

## 5. Règles de Validation Compile-Time Ajoutées

Ces règles s'appliquent au processor qui implémente `projection-spec` :

| Règle | Niveau | Description |
|-------|--------|-------------|
| `@ExposedAs` sur type `@Projection` | **ERREUR** | Interdit — la composition est automatique |
| Cycle de composition sans `cycleBreak` | **ERREUR** | Chemin complet du cycle affiché |
| Conflit de préfixe `as` dans un même DTO | **ERREUR** | Deux méthodes génèrent le même préfixe |
| `@Exposure` sans `@Projection` | **WARNING** | Annotation sans effet |
| Opérateur vide dans `operators` | **WARNING** | Champ déclaré filtrable sans opérateur |

---

## 6. Impact sur les Dépendances

### `projection-spec` (v3.0.0)
- ✅ Ajoute : `@ExposedAs`, `@Exposure`, `StandardOp`
- ✅ Modifie : `@Projected` (ajout de `as` et `cycleBreak`)
- ❌ Aucune dépendance externe ajoutée — reste `SOURCE` retention only

### `filterql-core`
- `Op` (enum) est **déprécié** → remplacé par `StandardOp` de `projection-spec`
- Maintenu pour compatibilité jusqu'à `filterql` v6.0.0

### `filterql-spring` (v5.0.0)
- `@ExposedAs` et `@Exposure` **supprimés** → imports migrés vers `projection-spec`
- `Op` remplacé par `StandardOp` dans tous les exemples et la doc
- Les opérateurs custom restent supportés via le mécanisme d'extension propre à l'implémentation

---

## 7. Guide de Migration pour les Utilisateurs

### Avant (filterql-spring v4.x)
```java
import io.github.cyfko.filterql.core.api.Op;
import io.github.cyfko.filterql.spring.ExposedAs;
import io.github.cyfko.filterql.spring.Exposure;

@Projection(from = User.class)
@Exposure(value = "users", basePath = "/api")
public interface UserDTO {

    @ExposedAs(value = "NAME", operators = {Op.EQ, Op.MATCHES})
    String getName();
}
```

### Après (filterql-spring v5.x + projection-spec v3.x)
```java
import io.github.cyfko.projection.ExposedAs;
import io.github.cyfko.projection.Exposure;
import io.github.cyfko.projection.StandardOp;

@Projection(from = User.class)
@Exposure(value = "users", namespace = "api")
public interface UserDTO {

    @ExposedAs(value = "NAME", operators = {StandardOp.EQ, StandardOp.MATCHES})
    String getName();
}
```

**Changements :** uniquement les imports. Aucune modification de logique.

---

## 8. Compatibilité et Stratégie de Dépréciation

| Version | `Op` (filterql-core) | `@ExposedAs` / `@Exposure` (filterql-spring) |
|---------|----------------------|----------------------------------------------|
| filterql v4.x | Actif | Actif |
| filterql v5.x | Déprécié (`@Deprecated`) | Supprimé → `projection-spec` v3.x |
| filterql v6.x | Supprimé | — |

---

## 9. Résumé des Changements

| Élément | Avant | Après |
|---------|-------|-------|
| `@ExposedAs` | `filterql-spring` | `projection-spec` |
| `@Exposure` | `filterql-spring` | `projection-spec` |
| `Op` | enum `filterql-core` | déprécié → `StandardOp` (interface `projection-spec`) |
| `operators` type | `Op[]` | `String[]` |
| `@Exposure.basePath` | `String` (URI) | `namespace` (concept logique) |
| `@Exposure.endpointName` | nom de méthode HTTP | supprimé (redondant avec `@Method`) |
| `Strategy.PAGINATED` | pagination avec metadata | `WINDOWED` (fenêtre consumer-driven) |
| `Strategy.LIST` | liste Java sans pagination | `FULL` (résultat complet) |
| `@Projected.as` | inexistant | `projection-spec` |
| `@Projected.cycleBreak` | inexistant | `projection-spec` |