# Projection Spec

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cyfko/projection-spec.svg)](https://search.maven.org/artifact/io.github.cyfko/projection-spec)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.org/)

**Projection Spec** is a Java annotation specification for declaratively describing Data Transfer Object (DTO) projections from source objects. It defines a stable, implementation-agnostic set of annotations for expressing how source fields are exposed, renamed, or computed in DTOs.

This specification is intended for use by annotation processors, frameworks, or tools that implement projection semantics at compile time.

> **Note:** While examples in this documentation use JPA entities as source objects (a common use case), the specification is **not limited to JPA**. It can be applied to any Java class as a source: domain objects, value objects, POJOs, or any class with accessible fields.

---

## Table of Contents

- [Installation](#installation)
- [Why Projection Spec?](#why-projection-spec)
- [Architecture Overview](#architecture-overview)
- [Quick Start](#quick-start)
- [Annotations Reference](#annotations-reference)
  - [@Projection](#projection)
  - [@Provider](#provider)
  - [@Projected](#projected)
  - [@Computed](#computed)
  - [@MethodReference](#methodreference)
- [How It Works](#how-it-works)
- [Provider Resolution](#provider-resolution)
- [Best Practices](#best-practices)
- [Design Constraints](#design-constraints)
- [Contributing](#-contributing)

---

## Installation

**Maven:**
```xml
<dependency>
    <groupId>io.github.cyfko</groupId>
    <artifactId>projection-spec</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("io.github.cyfko:projection-spec:1.0.0")
```

**Gradle (Groovy):**
```groovy
implementation 'io.github.cyfko:projection-spec:1.0.0'
```

> **Note:** This library provides only annotations with `SOURCE` retention. You will need an annotation processor that implements the projection logic.

---

## Why Projection Spec?

When building APIs or transforming data between layers, you often need to map source objects to DTOs. Common challenges include:

| Challenge | Projection Spec Solution |
|-----------|--------------------------|
| Boilerplate mapping code | Declarative annotations eliminate manual mapping |
| Inconsistent naming between source and API | `@Projected(from = "...")` handles renaming |
| Computed/derived fields | `@Computed` with provider methods |
| Coupling to specific frameworks | Framework-agnostic specification |
| Compile-time safety | Annotation processors can validate at build time |

**Key Benefits:**
- ✅ **Declarative**: Define mappings directly on your DTO
- ✅ **Type-safe**: All validations happen at compile time
- ✅ **Flexible**: Supports both static methods and IoC-managed beans
- ✅ **Zero runtime overhead**: All processing happens during compilation
- ✅ **Source-agnostic**: Works with any Java class (entities, domain objects, POJOs)

---

## Architecture Overview

The projection system is built around five core annotations that work together:

```
┌─────────────────────────────────────────────────────────────────┐
│                         @Projection                             │
│  ┌───────────────────┐  ┌─────────────────────────────────────┐ │
│  │  from = Source    │  │  providers = { @Provider(...) }     │ │
│  └───────────────────┘  └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
          ┌─────────────────┐         ┌─────────────────┐
          │   @Projected    │         │    @Computed    │
          │  Direct Mapping │         │  Derived Value  │
          └─────────────────┘         └────────┬────────┘
                                               │
                                               ▼
                                    ┌─────────────────────┐
                                    │  @MethodReference   │
                                    │ (optional override) │
                                    └─────────────────────┘
```

**Flow:**
1. `@Projection` declares the source class and computation providers
2. Fields use `@Projected` for direct mapping or `@Computed` for derived values
3. `@MethodReference` optionally overrides method resolution for computed fields

---

## Quick Start

### Step 1: Define Your Source Class

```java
// Example: JPA Entity (but could be any POJO)
@Entity
public class User {
    @Id
    private Long id;
    
    private String firstName;
    private String lastName;
    private String email;
    
    private LocalDateTime createdAt;
    
    @ManyToOne
    private Department department;
}
```

### Step 2: Create Your DTO with Projections

```java
@Projection(
    from = User.class,
    providers = { @Provider(UserComputations.class) }
)
public class UserDTO {
    
    // Direct mapping: same name as source field
    private Long id;
    private String email;
    
    // Renamed mapping: different name in DTO
    @Projected(from = "createdAt")
    private LocalDateTime registrationDate;
    
    // Nested path: access related object fields
    @Projected(from = "department.name")
    private String departmentName;
    
    // Computed field: derived from multiple source fields
    @Computed(dependsOn = {"firstName", "lastName"})
    private String fullName;
}
```

### Step 3: Implement Computation Logic

```java
public class UserComputations {
    
    /**
     * Method naming convention: get[FieldName]
     * Parameters must match dependsOn order and types
     */
    public static String getFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return null;
        }
        return String.format("%s %s", 
            firstName != null ? firstName : "", 
            lastName != null ? lastName : ""
        ).trim();
    }
}
```

---

## Annotations Reference

### @Projection

**Target:** Class  
**Purpose:** Declares a DTO as a projection of a source class

```java
@Projection(
    from = User.class,                              // Required: source class
    providers = { @Provider(UserComputations.class) } // Optional: computation providers
)
public class UserDTO { ... }
```

| Element | Type | Required | Description |
|---------|------|----------|-------------|
| `from` | `Class<?>` | ✅ Yes | The source class to project from |
| `providers` | `Provider[]` | ❌ No | Array of provider classes for computed fields |

---

### @Provider

**Target:** Nested in `@Projection.providers`  
**Purpose:** Registers a class that provides computation methods

```java
// Static provider (no bean name)
@Provider(UserComputations.class)

// IoC-managed bean provider
@Provider(value = DateFormatter.class, bean = "isoDateFormatter")
```

| Element | Type | Required | Description |
|---------|------|----------|-------------|
| `value` | `Class<?>` | ✅ Yes | The provider class |
| `bean` | `String` | ❌ No | Bean name for IoC lookup (empty = static methods) |

---

### @Projected

**Target:** Field  
**Purpose:** Maps a DTO field directly from a source field

```java
// Simple renaming
@Projected(from = "createdAt")
private LocalDateTime registrationDate;

// Nested path (traversing object graphs)
@Projected(from = "department.manager.email")
private String managerEmail;
```

| Element | Type | Required | Description |
|---------|------|----------|-------------|
| `from` | `String` | ✅ Yes | Source field path (supports dot notation) |

**Implicit Mapping:** Fields without `@Projected` or `@Computed` are implicitly mapped by name:
```java
private String email;  // Automatically maps to source.getEmail()
```

---

### @Computed

**Target:** Field  
**Purpose:** Declares a field whose value is computed from source fields

```java
// Simple computation
@Computed(dependsOn = {"firstName", "lastName"})
private String fullName;

// With explicit method reference
@Computed(
    dependsOn = {"price", "quantity"},
    computedBy = @MethodReference(method = "calculateTotal")
)
private BigDecimal totalAmount;
```

| Element | Type | Required | Description |
|---------|------|----------|-------------|
| `dependsOn` | `String[]` | ✅ Yes | Source field names (in parameter order) |
| `computedBy` | `MethodReference` | ❌ No | Explicit method override |

**Important:** The order of fields in `dependsOn` must match the parameter order in the provider method.

---

### @MethodReference

**Target:** Nested in `@Computed.computedBy`  
**Purpose:** Overrides default method resolution

```java
// Override method name only
@MethodReference(method = "formatDisplayName")

// Target specific provider
@MethodReference(type = CurrencyUtils.class)

// Fully explicit
@MethodReference(type = StringUtils.class, method = "uppercase")
```

| Element | Type | Required | Description |
|---------|------|----------|-------------|
| `type` | `Class<?>` | ❌ No | Target provider class |
| `method` | `String` | ❌ No | Method name (default: `get[FieldName]`) |

---

## How It Works

### 1. Annotation Processor Discovery

At compile time, an annotation processor scans classes annotated with `@Projection` and:
- Identifies the source class
- Collects all field mappings (implicit, explicit, and computed)
- Validates field paths exist in the source
- Resolves computation methods in providers

### 2. Field Classification

Each DTO field is classified into one of three categories:

| Category | Annotation | Resolution |
|----------|------------|------------|
| **Implicit** | None | Maps by matching field name |
| **Explicit** | `@Projected` | Maps using the `from` path |
| **Computed** | `@Computed` | Calls provider method with dependencies |

### 3. Method Resolution for @Computed

When resolving a computation method, the processor follows this algorithm:

```
1. If @MethodReference specifies both type and method:
   └─> Use exactly that method in that provider

2. If @MethodReference specifies only method:
   └─> Search all providers for that method name

3. If @MethodReference specifies only type:
   └─> Search that provider for get[FieldName]

4. If no @MethodReference (default):
   └─> Search all providers for get[FieldName]
```

**First-match-wins:** Providers are searched in declaration order.

---

## Provider Resolution

### Static vs Bean-Based Providers

| Type | Declaration | Method Type | Use Case |
|------|-------------|-------------|----------|
| **Static** | `@Provider(MyClass.class)` | Static methods | Stateless transformations |
| **Bean** | `@Provider(value = MyClass.class, bean = "myBean")` | Instance methods | Injected dependencies needed |

### Example: Static Provider

```java
public class StringUtils {
    
    public static String uppercase(String input) {
        return input != null ? input.toUpperCase() : null;
    }
    
    public static String concatenate(String a, String b) {
        return a + " " + b;
    }
}
```

### Example: Bean-Based Provider (Spring)

```java
@Service("currencyConverter")
public class CurrencyConverter {
    
    private final ExchangeRateService exchangeRateService;
    
    public CurrencyConverter(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }
    
    public BigDecimal convertToUSD(BigDecimal amount, String currency) {
        BigDecimal rate = exchangeRateService.getRate(currency, "USD");
        return amount.multiply(rate);
    }
}
```

```java
@Projection(
    from = Order.class,
    providers = { @Provider(value = CurrencyConverter.class, bean = "currencyConverter") }
)
public class OrderDTO {
    
    @Computed(dependsOn = {"amount", "currency"})
    private BigDecimal amountInUSD;
}
```

---

## Best Practices

### 1. Order Providers by Specificity

```java
@Projection(
    from = User.class,
    providers = {
        @Provider(UserSpecificComputations.class),  // Most specific first
        @Provider(CommonComputations.class),        // General utilities last
    }
)
```

### 2. Use Explicit @MethodReference When Ambiguous

```java
// If multiple providers have getAge(), be explicit:
@Computed(
    dependsOn = {"birthDate"},
    computedBy = @MethodReference(type = ModernDateUtils.class)
)
private Integer age;
```

### 3. Keep Computed Fields Simple

```java
// ✅ Good: Each computed field has clear dependencies
@Computed(dependsOn = {"firstName", "lastName"})
private String fullName;

// ❌ Avoid: Don't try to chain computed fields
// (computed-to-computed dependencies are not supported)
```

### 4. Document Provider Methods

```java
public class OrderComputations {
    
    /**
     * Calculates the total order amount including tax.
     * 
     * @param subtotal Order subtotal before tax
     * @param taxRate  Tax rate as decimal (e.g., 0.20 for 20%)
     * @return Total amount including tax
     */
    public static BigDecimal getTotal(BigDecimal subtotal, BigDecimal taxRate) {
        return subtotal.multiply(BigDecimal.ONE.add(taxRate));
    }
}
```

---

## Design Constraints

| Constraint | Description | Rationale |
|------------|-------------|-----------|
| **SOURCE Retention** | Annotations are discarded after compilation | No runtime reflection overhead |
| **Source-Only Dependencies** | `@Computed` can only depend on source fields | Prevents complex dependency graphs |
| **No Circular Dependencies** | Computed fields cannot depend on other computed fields | Simplifies resolution, prevents infinite loops |
| **First-Match-Wins** | Providers are searched in declaration order | Predictable, deterministic behavior |
| **Parameter Order Matters** | `dependsOn` order must match method parameters | Ensures correct value passing |

---

## Comparison: @Projected vs @Computed

| Aspect | @Projected | @Computed |
|--------|------------|-----------|
| **Purpose** | Direct source-to-DTO mapping | Derived value computation |
| **Processing** | Simple field copy or path traversal | Method invocation with dependencies |
| **Dependencies** | Single source field (or path) | Multiple source fields |
| **Use Cases** | Renaming, nested access | Concatenation, formatting, calculations |

**Examples:**

```java
// @Projected: Direct mapping with renaming
@Projected(from = "createdAt")
private LocalDateTime registrationDate;

// @Computed: Derived from multiple fields
@Computed(dependsOn = {"price", "quantity"})
private BigDecimal lineTotal;
```

---

## 🤝 Contributing

Contributions are welcome! Please open an issue to discuss major changes before submitting a pull request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Frank KOSSI**

- Email: frank.kossi@kunrin.com, frank.kossi@sprint-pay.com
- Organization: [Kunrin SA](https://www.kunrin.com), [Sprint-Pay SA](https://www.sprint-pay.com)

---

## 🔗 Links

- [GitHub Repository](https://github.com/cyfko/projection-spec)
- [Issue Tracker](https://github.com/cyfko/projection-spec/issues)
- [Maven Central](https://search.maven.org/artifact/io.github.cyfko/projection-spec)