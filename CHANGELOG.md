# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - Unreleased

### Added

#### `StandardOp` — Standard filter operator constants
- New interface `io.github.cyfko.projection.StandardOp` containing `String` constants for standard filter operators
- 14 operators: `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE`, `RANGE`, `NOT_RANGE`, `MATCHES`, `NOT_MATCHES`, `IN`, `NOT_IN`, `IS_NULL`, `NOT_NULL`
- Framework-agnostic replacement for the `Op` enum from `filterql-core`
- Implementations MAY define additional custom operators as plain `String` values

#### `@ExposedAs` — Filterable field declaration
- New annotation `io.github.cyfko.projection.ExposedAs` (migrated from `filterql-spring`)
- Declares a DTO getter method as filterable with an optional symbolic name and operator set
- `String value()` — symbolic name (defaults to logical name derived from method name, casing is implementation-defined)
- `String[] operators()` — allowed operators using `StandardOp` constants or custom strings
- `boolean exposed()` — controls public API visibility (defaults to `true`)
- **Forbidden** on methods returning a `@Projection` type (compile-time error)

#### `@Exposure` — Queryable resource declaration
- New annotation `io.github.cyfko.projection.Exposure` (migrated from `filterql-spring`)
- Declares a `@Projection` interface as a queryable resource with dynamic filtering
- Entirely agnostic of transport protocol, delivery mechanism, and response format
- `String value()` — logical resource name
- `String namespace()` — logical namespace grouping (replaces `basePath`)
- `Strategy strategy()` — result cardinality (`WINDOWED`, `FULL`, `CUSTOM`)
- `Method[] pipes()` — ordered filter context transformation pipeline
- `Method handler()` — custom handler producing the resource result

### Changed

#### `@Projected` — New attributes for composed filter inheritance
- **BREAKING**: Added `String as() default ""` — logical prefix for composed filter inheritance
- **BREAKING**: Added `boolean cycleBreak() default false` — breaks cycles in bidirectional projections
- Added `@Documented` meta-annotation
- `from()` remains mandatory (no default value)

### Migration Guide (from `filterql-spring` v4.x)

Replace imports only — no logic changes required:

```
Find:    import io.github.cyfko.filterql.core.api.Op;
Replace: import io.github.cyfko.projection.StandardOp;

Find:    import io.github.cyfko.filterql.spring.ExposedAs;
Replace: import io.github.cyfko.projection.ExposedAs;

Find:    import io.github.cyfko.filterql.spring.Exposure;
Replace: import io.github.cyfko.projection.Exposure;

Find:    Op.EQ
Replace: StandardOp.EQ
(repeat for all Op.* references)
```

Additionally, update renamed attributes and strategies:

```
basePath       → namespace
endpointName   → supprimé (redondant avec @Method)
Strategy.PAGINATED → Strategy.WINDOWED
Strategy.LIST      → Strategy.FULL
```

---

## [2.0.1] - Unreleased

#### `@MethodReference` renamed to `@Method`
- **BREAKING**: `@MethodReference` annotation renamed to `@Method` for improved readability and brevity
- Shorter, more intuitive name that better reflects its purpose
- No conflicts with existing Java/Jakarta EE frameworks or JDK classes
- The `method` attribute has been renamed to `value` for idiomatic Java annotation usage
- Allows shorthand syntax: `@Method("methodName")` instead of `@Method(method = "methodName")`

### Migration Guide

#### Automated Migration (Recommended)

Use find-and-replace in your IDE or build script:

1. **Simple replacement:**
```
   Find:    @MethodReference
   Replace: @Method
```

2. **Import statement:**
```
   Find:    import io.github.cyfko.projection.MethodReference;
   Replace: import io.github.cyfko.projection.Method;
```

3. **Attribute rename:**
```
   Find:    @Method(method = "
   Replace: @Method(value = "
```
Or simply:
```
   Find:    @Method(method = 
   Replace: @Method(value = 
```

#### Manual Migration Examples

**Before:**
```java
import io.github.cyfko.projection.MethodReference;

@Computed(
    dependsOn = {"firstName", "lastName"}, 
    computedBy = @MethodReference(method = "formatDisplayName")
)
private String fullName;

@Computed(
    dependsOn = {"birthDate"}, 
    computedBy = @MethodReference(type = ModernCalculator.class)
)
private Integer age;

@Computed(
    dependsOn = {"amount", "currency"}, 
    computedBy = @MethodReference(
        type = CurrencyUtils.class, 
        method = "convertToUSD"
    )
)
private BigDecimal amountUSD;
```

**After:**
```java
import io.github.cyfko.projection.Method;

@Computed(
    dependsOn = {"firstName", "lastName"}, 
    computedBy = @Method(value = "formatDisplayName")
)
private String fullName;

// Or using shorthand syntax:
@Computed(
    dependsOn = {"firstName", "lastName"}, 
    computedBy = @Method("formatDisplayName")
)
private String fullName;

@Computed(
    dependsOn = {"birthDate"}, 
    computedBy = @Method(type = ModernCalculator.class)
)
private Integer age;

@Computed(
    dependsOn = {"amount", "currency"}, 
    computedBy = @Method(
        type = CurrencyUtils.class, 
        value = "convertToUSD"
    )
)
private BigDecimal amountUSD;

// Or using shorthand when only method name is specified:
@Computed(
    dependsOn = {"amount", "currency"}, 
    computedBy = @Method(
        type = CurrencyUtils.class, 
        "convertToUSD"
    )
)
private BigDecimal amountUSD;
```

### Deprecation Notice

- `@MethodReference` annotation is **removed** in this version
- No deprecation period provided as this is a pre-1.0 breaking change
- All references must be updated to `@Method`

### Rationale

This change was made to:
1. **Improve developer experience** - Shorter annotation name reduces verbosity
2. **Follow Java conventions** - Using `value` as the primary attribute enables cleaner shorthand syntax
3. **Enhance readability** - `@Method("name")` is more natural than `@MethodReference(method = "name")`
4. **Maintain clarity** - The name `@Method` is unambiguous in the projection/computation context

### Compatibility

- ✅ No conflicts with `java.lang.reflect.Method` (class, not annotation)
- ✅ No conflicts with Spring Framework annotations
- ✅ No conflicts with Jakarta EE / Java EE annotations
- ✅ No conflicts with JAX-RS, JPA, or other major frameworks
- ✅ Verified against JUnit, Mockito, Lombok, and other common libraries

---

## Previous Versions

### [1.0.0] - YYYY-MM-DD
- Initial release with `@MethodReference` annotation