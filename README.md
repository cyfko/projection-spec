# Projection Spec

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cyfko/projection-spec.svg)](https://search.maven.org/artifact/io.github.cyfko/projection-spec)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.org/)

**Projection Spec** is a framework-agnostic Java annotation specification for declaratively mapping source objects to Data Transfer Objects (DTOs). It provides a clean, type-safe way to define projections with computed fields, transformations, and collection aggregations—all validated at compile time.

This specification is designed for annotation processors, frameworks, and tools that implement projection semantics, eliminating boilerplate mapping code while maintaining full compile-time safety.

> **Framework Agnostic:** While examples may reference JPA entities, this specification works with **any Java class**: JPA entities, MongoDB documents, domain objects, value objects, POJOs, or even other DTOs.

---

## 📋 Table of Contents

- [Installation](#-installation)
- [Why Projection Spec?](#-why-projection-spec)
- [Architecture Overview](#-architecture-overview)
- [Quick Start](#-quick-start)
- [Core Concepts](#-core-concepts)
  - [Field Mapping Strategies](#field-mapping-strategies)
  - [Computed Fields](#computed-fields)
  - [Transformation Pipeline](#transformation-pipeline)
  - [Collection Aggregation](#collection-aggregation)
- [Annotations Reference](#-annotations-reference)
- [Provider System](#-provider-system)
- [Advanced Features](#-advanced-features)
- [Best Practices](#-best-practices)
- [Design Philosophy](#-design-philosophy)
- [Contributing](#-contributing)

---

## 📦 Installation

**Maven:**
```xml
<dependency>
    <groupId>io.github.cyfko</groupId>
    <artifactId>projection-spec</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("io.github.cyfko:projection-spec:2.0.0")
```

**Gradle (Groovy):**
```groovy
implementation 'io.github.cyfko:projection-spec:2.0.0'
```

> **⚠️ Important:** This library provides only annotations with `SOURCE` retention. You need an annotation processor implementation to generate actual projection code.

---

## 🎯 Why Projection Spec?

When building APIs or transforming data between layers, you face common challenges:

| Challenge | Projection Spec Solution |
|-----------|--------------------------|
| **Boilerplate mapping code** | Declarative annotations eliminate manual mappers |
| **Field renaming** | `@Projected(from = "...")` handles source-to-DTO name differences |
| **Computed/derived fields** | `@Computed` with provider methods for business logic |
| **Type conversions** | Transformation pipeline with `@Computed(then = ...)` |
| **Framework coupling** | Framework-agnostic specification (works with any IoC container) |
| **Runtime errors** | Full compile-time validation catches errors early |
| **Collection aggregation** | Built-in reducers for SUM, AVG, COUNT, etc. |

**Key Benefits:**

- ✅ **Declarative:** Define all mappings directly on your DTO interface
- ✅ **Type-safe:** Compile-time validation of field paths, types, and method signatures
- ✅ **Flexible:** Supports static methods, IoC-managed beans, and custom transformations
- ✅ **Zero runtime overhead:** All processing happens during compilation
- ✅ **Source-agnostic:** Works with any Java object (entities, POJOs, documents, DTOs)
- ✅ **Immutable by design:** Interface-based DTOs encourage immutability

---

## 🏗️ Architecture Overview

The projection system is built around five core annotations working in harmony:

```
┌──────────────────────────────────────────────────────────────────┐
│                          @Projection                              │
│  ┌────────────────────┐  ┌──────────────────────────────────────┐│
│  │  from = Source     │  │  providers = { @Provider(...) }      ││
│  └────────────────────┘  └──────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
                                   │
                 ┌─────────────────┴─────────────────┐
                 ▼                                   ▼
       ┌───────────────────┐              ┌───────────────────┐
       │    @Projected     │              │    @Computed      │
       │  Direct Mapping   │              │  Derived Value    │
       └───────────────────┘              └─────────┬─────────┘
                                                    │
                                   ┌────────────────┴────────────────┐
                                   ▼                                 ▼
                        ┌─────────────────────┐        ┌─────────────────────┐
                        │  @Method            │        │  @Method (then)     │
                        │  (computedBy)       │        │  Transformation     │
                        │  Business Logic     │        │  Pure Function      │
                        └─────────────────────┘        └─────────────────────┘
```

**Two-Stage Computation Pipeline:**
1. **computedBy:** Business logic using source fields → intermediate result
2. **then:** Pure transformation → final type (optional)

---

## 🚀 Quick Start

### Step 1: Define Your Source Class

```java
// Can be any Java class: JPA entity, MongoDB document, POJO, etc.
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
    
    @OneToMany
    private List<Order> orders;
}
```

### Step 2: Create Your DTO with Projections

```java
@Projection(
    from = User.class,
    providers = { 
        @Provider(UserComputations.class),
        @Provider(Formatters.class)
    }
)
public interface UserDTO {
    
    // === Implicit mapping (name match) ===
    Long getId();
    String getEmail();
    
    // === Explicit mapping (renamed) ===
    @Projected(from = "createdAt")
    LocalDateTime getRegistrationDate();
    
    // === Nested field access ===
    @Projected(from = "department.name")
    String getDepartmentName();
    
    // === Computed field (business logic) ===
    @Computed(dependsOn = {"firstName", "lastName"})
    String getFullName();
    
    // === Computed + Transformation (type conversion) ===
    @Computed(
        dependsOn = "createdAt",
        computedBy = @Method(value = "toInstant"),
        then = @Method(value = "formatIso")
    )
    String getFormattedCreationDate();
    
    // === Collection aggregation ===
    @Computed(
        dependsOn = "orders.total",
        reducers = {Computed.Reduce.SUM}
    )
    BigDecimal getTotalOrderValue();
}
```

### Step 3: Implement Provider Logic

```java
// Static provider (pure functions)
public class UserComputations {
    
    // Convention: to[FieldName] matches computed field
    public static String toFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        return String.format("%s %s", 
            firstName != null ? firstName : "", 
            lastName != null ? lastName : ""
        ).trim();
    }
    
    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC);
    }
}

// Transformation utilities (must be static)
public class Formatters {
    
    public static String formatIso(Instant instant) {
        return instant.toString();
    }
}
```

### Step 4: Use Your DTO

The annotation processor generates the implementation. Usage depends on your implementation, but typically:

```java
UserDTO dto = ProjectionMapper.map(userEntity, UserDTO.class);
// or via framework integration (Spring, Micronaut, etc.)
```

---

## 💡 Core Concepts

### Field Mapping Strategies

Projection Spec supports three mapping strategies:

#### 1. Implicit Mapping (Name Match)

When no annotation is present, field names are matched exactly:

```java
@Projection(from = User.class)
public interface UserDTO {
    Long getId();          // Maps to User.id
    String getEmail();     // Maps to User.email
    String getPhoneNumber(); // Maps to User.phoneNumber
}
```

#### 2. Explicit Mapping (@Projected)

Use `@Projected` to rename fields or access nested properties:

```java
@Projection(from = User.class)
public interface UserDTO {
    
    // Rename: createdAt → registrationDate
    @Projected(from = "createdAt")
    LocalDateTime getRegistrationDate();
    
    // Flatten: access nested object
    @Projected(from = "address.city")
    String getCity();
    
    // Deep nesting
    @Projected(from = "profile.avatar.url")
    String getAvatarUrl();
}
```

#### 3. Computed Fields (@Computed)

Derive new values from one or more source fields:

```java
@Projection(from = User.class, providers = { @Provider(UserComputations.class) })
public interface UserDTO {
    
    // Concatenation
    @Computed(dependsOn = {"firstName", "lastName"})
    String getFullName();
    
    // Calculation
    @Computed(dependsOn = "birthDate")
    Integer getAge();
    
    // Multiple dependencies
    @Computed(dependsOn = {"basePrice", "taxRate"})
    BigDecimal getTotalPrice();
}
```

---

### Computed Fields

Computed fields derive values through a **two-stage pipeline**:

```
Source Fields → [computedBy] → Intermediate Result → [then] → Final Value
                (business logic)                     (transformation)
```

#### Stage 1: Business Logic (computedBy)

The `computedBy` stage performs domain calculations:

```java
@Computed(
    dependsOn = {"basePrice", "discount"},
    computedBy = @Method(value = "calculateFinalPrice")
)
BigDecimal getFinalPrice();

// Provider
public class PriceCalculator {
    public static BigDecimal calculateFinalPrice(BigDecimal base, BigDecimal discount) {
        return base.subtract(discount);
    }
}
```

**Features:**
- Can be static or instance methods (if provider is IoC-managed)
- Supports dependency injection when needed
- Follows naming convention `to[FieldName]` if not explicitly specified

#### Stage 2: Transformation (then)

The `then` stage applies pure type conversions:

```java
@Computed(
    dependsOn = "createdAt",
    computedBy = @Method(value = "toInstant"),      // LocalDateTime → Instant
    then = @Method(value = "formatIso")             // Instant → String
)
String getFormattedDate();

// Provider
public class DateUtils {
    public static Instant toInstant(LocalDateTime dt) {
        return dt.toInstant(ZoneOffset.UTC);
    }
}

public class Formatters {
    // Transformation methods MUST be static (pure functions)
    public static String formatIso(Instant instant) {
        return instant.toString();
    }
}
```

**Features:**
- **Must** be static (pure functions, no side effects)
- Enables code reuse (same transformer for different fields)
- Method name must be explicitly specified (no convention)

---

### Transformation Pipeline

The transformation pipeline enables elegant type conversions:

#### Example: Date Formatting

```java
@Projection(from = Event.class, providers = {
    @Provider(DateUtils.class),
    @Provider(StringFormatters.class)
})
public interface EventDTO {
    
    @Computed(
        dependsOn = "startDate",
        computedBy = @Method(value = "toInstant"),
        then = @Method(value = "toIsoString")
    )
    String getFormattedStartDate();
}
```

#### Example: Price Formatting

```java
@Computed(
    dependsOn = {"amount", "currency"},
    computedBy = @Method(value = "convertToUSD"),  // BigDecimal
    then = @Method(value = "formatCurrency")       // String
)
String getDisplayPrice();
```

#### Example: Multiple Transformations

Different fields can reuse the same transformation:

```java
public interface ProductDTO {
    
    @Computed(
        dependsOn = "internalCode",
        computedBy = @Method(value = "normalize"),
        then = @Method(value = "uppercase")
    )
    String getProductCode();
    
    @Computed(
        dependsOn = "category",
        then = @Method(value = "uppercase")  // Reuse same transformer
    )
    String getCategoryCode();
}
```

---

### Collection Aggregation

Reduce collections to single values using built-in reducers:

#### Standard Reducers

```java
Computed.Reduce.SUM            // Sum of values
Computed.Reduce.AVG            // Average (arithmetic mean)
Computed.Reduce.COUNT          // Count of elements
Computed.Reduce.MIN            // Minimum value
Computed.Reduce.MAX            // Maximum value
Computed.Reduce.COUNT_DISTINCT // Count of distinct values
```

#### Simple Aggregation

```java
@Projection(from = User.class)
public interface UserStatsDTO {
    
    // Sum all order totals
    @Computed(
        dependsOn = "orders.total",
        reducers = {Computed.Reduce.SUM}
    )
    BigDecimal getTotalOrderValue();
    
    // Count orders
    @Computed(
        dependsOn = "orders.id",
        reducers = {Computed.Reduce.COUNT}
    )
    Long getOrderCount();
}
```

#### Nested Collection Traversal

```java
@Projection(from = Company.class)
public interface CompanyStatsDTO {
    
    // Average salary across all nested employees
    @Computed(
        dependsOn = "departments.teams.employees.salary",
        reducers = {Computed.Reduce.AVG}
    )
    BigDecimal getAverageSalary();
}
```

#### Multiple Aggregations

```java
@Computed(
    dependsOn = {"id", "name", "orders.total", "refunds.amount"},
    //          scalar  scalar  collection     collection
    reducers = {Computed.Reduce.SUM, Computed.Reduce.SUM}
    //          ↑ orders.total       ↑ refunds.amount
)
String getFinancialReport();
```

**Rules:**
- Collection paths **must** end with a field (not the collection itself)
- Reducers array length = number of collection-traversing dependencies
- Scalar fields don't require reducers

---

## 📚 Annotations Reference

### @Projection

**Target:** Interface or Class  
**Purpose:** Declares a DTO as a projection from a source class

```java
@Projection(
    from = User.class,                    // Required: source class
    providers = {                         // Optional: computation providers
        @Provider(UserComputations.class),
        @Provider(value = DateService.class, bean = "dateFormatter")
    }
)
public interface UserDTO { ... }
```

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `from` | `Class<?>` | ✅ Yes | Source class to project from |
| `providers` | `Provider[]` | ❌ No | Provider classes for computed fields and transformations |

---

### @Provider

**Target:** Nested in `@Projection.providers`  
**Purpose:** Registers a class containing computation methods

```java
// Static provider
@Provider(MathUtils.class)

// IoC-managed provider (by type)
@Provider(TaxService.class)

// IoC-managed provider (by name)
@Provider(value = PriceCalculator.class, bean = "premiumPricing")
```

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `value` | `Class<?>` | ✅ Yes | Provider class |
| `bean` | `String` | ❌ No | Bean name for IoC lookup (empty = resolve by type) |

**Resolution Logic:**
- If method is **static** → invoke directly (no IoC)
- If method is **instance** and `bean=""` → resolve from IoC by **type**
- If method is **instance** and `bean="name"` → resolve from IoC by **name**

---

### @Projected

**Target:** Method or Field  
**Purpose:** Maps a DTO field directly from a source field

```java
// Simple renaming
@Projected(from = "createdAt")
LocalDateTime getRegistrationDate();

// Nested path
@Projected(from = "department.manager.email")
String getManagerEmail();

// Deep nesting
@Projected(from = "profile.settings.theme.primaryColor")
String getThemeColor();
```

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `from` | `String` | ✅ Yes | Source field path (supports dot notation) |

---

### @Computed

**Target:** Method or Field  
**Purpose:** Declares a derived value computed from source fields

```java
@Computed(
    dependsOn = {"firstName", "lastName"},
    computedBy = @Method(value = "concatenate"),  // Optional
    then = @Method(value = "uppercase"),          // Optional
    reducers = {}                                 // For collections
)
String getFullNameUpper();
```

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `dependsOn` | `String[]` | ✅ Yes | Source fields this computation depends on |
| `computedBy` | `Method` | ❌ No | Explicit computation method reference |
| `then` | `Method` | ❌ No | Transformation to apply to `computedBy` result |
| `reducers` | `String[]` | ❌ No | Reducers for collection-traversing dependencies |

**Two-Stage Pipeline:**
1. **computedBy:** Business logic (source fields → intermediate result)
2. **then:** Transformation (intermediate → final type) — **must be static**

---

### @Method

**Target:** Used within `@Computed`  
**Purpose:** References a specific method in a provider class

```java
// Convention-based (no attributes)
@Method()  // Searches for to[FieldName]

// Explicit method name
@Method(value = "buildFullName")

// Explicit class
@Method(type = StringUtils.class)

// Fully qualified
@Method(type = StringUtils.class, value = "concatenate")
```

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | `Class<?>` | ❌ No | Provider class (default: search all providers) |
| `value` | `String` | ❌ No | Method name (default: convention `to[FieldName]`) |

**Resolution Strategy:**

| `type` | `value` | Resolution |
|--------|---------|------------|
| Not set | Not set | Search all providers for `to[FieldName]` |
| Not set | Set | Search all providers for specified method |
| Set | Not set | Search specified provider for `to[FieldName]` |
| Set | Set | Use exactly that method in that provider |

---

## 🔧 Provider System

Providers are the computation engine, containing business logic and transformations.

### Provider Types

#### 1. Static Provider (Utility Methods)

```java
public class StringUtils {
    
    public static String uppercase(String str) {
        return str != null ? str.toUpperCase() : null;
    }
    
    public static String concatenate(String a, String b) {
        return (a != null ? a : "") + " " + (b != null ? b : "");
    }
}
```

**Usage:**
```java
@Projection(from = User.class, providers = { @Provider(StringUtils.class) })
```

**Characteristics:**
- ✅ Zero overhead (direct method calls)
- ✅ No IoC container required
- ✅ Perfect for pure functions

#### 2. IoC Provider (Dependency Injection)

```java
@Component  // or @Service, @Named, etc.
public class TaxCalculator {
    
    @Inject
    private TaxRateRepository taxRateRepo;
    
    // Instance method - can use injected dependencies
    public BigDecimal toTotalWithTax(BigDecimal amount, String region) {
        BigDecimal rate = taxRateRepo.findByRegion(region);
        return amount.multiply(BigDecimal.ONE.add(rate));
    }
}
```

**Usage:**
```java
@Projection(from = Order.class, providers = { @Provider(TaxCalculator.class) })
```

**Characteristics:**
- ✅ Access to application context
- ✅ Can inject repositories, services, configuration
- ⚠️ First call requires IoC lookup (then cached)

#### 3. Named Bean Provider (Disambiguation)

When multiple implementations exist:

```java
@Named("premiumPricing")
public class PremiumPriceCalculator implements PriceCalculator {
    public BigDecimal toFinalPrice(BigDecimal base) { ... }
}

@Named("standardPricing")
public class StandardPriceCalculator implements PriceCalculator {
    public BigDecimal toFinalPrice(BigDecimal base) { ... }
}
```

**Usage:**
```java
@Projection(
    from = Product.class,
    providers = { 
        @Provider(value = PriceCalculator.class, bean = "premiumPricing")
    }
)
```

### Provider Search Order

Providers are searched in **declaration order** (first-match-wins):

```java
@Projection(
    from = User.class,
    providers = {
        @Provider(CustomComputations.class),   // 1. Checked first
        @Provider(CommonComputations.class),   // 2. Checked second
        @Provider(FallbackUtils.class)         // 3. Checked last
    }
)
```

**Best Practice:** Order providers from most specific to most general.

### Mixed Providers Example

```java
@Projection(
    from = Employee.class,
    providers = {
        @Provider(SalaryComputations.class),              // Static utilities
        @Provider(value = TaxService.class,               // IoC by name
                  bean = "corporateTaxService"),
        @Provider(HRService.class)                        // IoC by type
    }
)
public interface EmployeeDTO {
    
    // Uses SalaryComputations (static)
    @Computed(dependsOn = {"baseSalary", "bonus"})
    BigDecimal getTotalCompensation();
    
    // Uses TaxService bean
    @Computed(dependsOn = {"totalCompensation", "region"})
    BigDecimal getNetSalary();
    
    // Uses HRService (IoC by type)
    @Computed(dependsOn = "employeeId")
    Integer getVacationDaysRemaining();
}
```

---

## 🎨 Advanced Features

### Transformation Chains

Chain business logic and type conversion:

```java
@Computed(
    dependsOn = "timestamp",
    computedBy = @Method(value = "toZonedDateTime"),  // long → ZonedDateTime
    then = @Method(value = "formatReadable")          // ZonedDateTime → String
)
String getReadableTimestamp();
```

### Reusing Transformations

Single transformation function used by multiple fields:

```java
public interface ProductDTO {
    
    @Computed(
        dependsOn = "internalCode",
        then = @Method(value = "sanitize")
    )
    String getProductCode();
    
    @Computed(
        dependsOn = "manufacturerRef",
        then = @Method(value = "sanitize")  // Same transformer
    )
    String getManufacturerCode();
}
```

### DTO-to-DTO Projection (Chaining)

Project from another DTO:

```java
// Internal DTO (full data)
@Projection(from = User.class)
public interface UserInternalDTO {
    Long getId();
    String getEmail();
    String getPasswordHash();  // Sensitive
}

// Public API DTO (filtered)
@Projection(from = UserInternalDTO.class)  // Project from DTO!
public interface UserPublicDTO {
    Long getId();
    String getEmail();
    // passwordHash excluded
}
```

### Multi-Level Aggregation

```java
@Projection(from = University.class)
public interface UniversityStatsDTO {
    
    // Nested collection: universities → departments → students
    @Computed(
        dependsOn = "departments.students.gpa",
        reducers = {Computed.Reduce.AVG}
    )
    Double getAverageGPA();
    
    // Count across nested collections
    @Computed(
        dependsOn = "departments.students.id",
        reducers = {Computed.Reduce.COUNT}
    )
    Long getTotalStudents();
}
```

---

## ✨ Best Practices

### 1. Use Interfaces for Read-Only DTOs

```java
// ✅ Good: Interface enforces immutability
@Projection(from = User.class)
public interface UserDTO {
    Long getId();
    String getName();
}

// ❌ Avoid: Class with setters allows mutation
@Projection(from = User.class)
public class UserDTO {
    private Long id;
    public void setId(Long id) { ... }  // Mutable!
}
```

### 2. Order Providers by Specificity

```java
@Projection(
    from = Order.class,
    providers = {
        @Provider(OrderSpecificComputations.class),  // Specific first
        @Provider(CommonCalculations.class),         // Generic last
    }
)
```

### 3. Keep Transformations Pure and Static

```java
// ✅ Good: Pure function, static
public class Formatters {
    public static String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance().format(amount);
    }
}

// ❌ Bad: Instance method with state
public class Formatters {
    private Locale locale;  // State!
    
    public String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(locale).format(amount);
    }
}
```

### 4. Use Explicit @Method for Clarity

```java
// When multiple providers have similar methods, be explicit:
@Computed(
    dependsOn = "birthDate",
    computedBy = @Method(type = ModernDateUtils.class, value = "toAge")
)
Integer getAge();
```

### 5. Document Complex Computations

```java
public class OrderComputations {
    
    /**
     * Calculates order total including tax and shipping.
     * 
     * @param subtotal Order subtotal before fees
     * @param taxRate Tax rate as decimal (0.20 = 20%)
     * @param shippingFee Flat shipping fee
     * @return Total amount including all fees
     */
    public static BigDecimal toTotal(
            BigDecimal subtotal, 
            BigDecimal taxRate, 
            BigDecimal shippingFee) {
        return subtotal.multiply(BigDecimal.ONE.add(taxRate)).add(shippingFee);
    }
}
```

### 6. Separate Read and Write DTOs

```java
// Read DTO (projection)
@Projection(from = User.class, providers = { @Provider(UserComputations.class) })
public interface UserReadDTO {
    Long getId();
    
    @Computed(dependsOn = {"firstName", "lastName"})
    String getFullName();
}

// Write DTO (plain class with validation)
public class UserCreateRequest {
    @NotBlank
    private String firstName;
    
    @NotBlank
    private String lastName;
    
    @Email
    private String email;
    
    // Getters and setters
}
```

---

## 🧠 Design Philosophy

### Source Retention (Zero Runtime Overhead)

All annotations use `SOURCE` retention—they're discarded after compilation:

```java
@Retention(RetentionPolicy.SOURCE)
public @interface Projection { ... }
```

**Benefits:**
- ✅ No runtime reflection
- ✅ No annotation scanning overhead
- ✅ Smaller bytecode size
- ✅ Full compile-time validation

### Source-Only Dependencies

Computed fields can **only** depend on source fields, not other computed fields:

```java
// ✅ Good: Both depend on source fields
@Computed(dependsOn = {"firstName", "lastName"})
String getFullName();

@Computed(
    dependsOn = {"firstName", "lastName"},
    then = @Method(value = "uppercase")
)
String getFullNameUpper();

// ❌ Bad: Circular dependency
@Computed(dependsOn = "fullName")  // fullName is computed!
String getUppercaseName();
```

**Rationale:**
- Eliminates complex dependency graphs
- Prevents circular dependencies
- Ensures independent computation order
- Simplifies code generation

### First-Match-Wins Provider Resolution

Providers are searched in order, first match is used:

```java
providers = {
    @Provider(HighPriorityComputations.class),  // Searched first
    @Provider(FallbackComputations.class)       // Searched if not found above
}
```

**Rationale:**
- Predictable, deterministic behavior
- Easy to reason about
- Enables override patterns
- No ambiguity

### Immutability by Design

Interface-based DTOs encourage immutability:

```java
public interface UserDTO {
    Long getId();           // Read-only
    String getName();       // No setters possible
}
```

**Benefits:**
- ✅ Thread-safe by default
- ✅ Prevents accidental mutation
- ✅ Easier to reason about
- ✅ Cacheable

---

## 🔍 Comparison Table

### @Projected vs @Computed

| Aspect | @Projected | @Computed |
|--------|------------|-----------|
| **Purpose** | Direct source-to-DTO mapping | Derived value from computation |
| **Processing** | Simple field copy or path traversal | Method invocation with dependencies |
| **Dependencies** | Single source field (or path) | Multiple source fields |
| **Use Cases** | Renaming, nested access, flattening | Concatenation, formatting, calculations |
| **Provider** | Not required | Required for computation methods |

### computedBy vs then

| Aspect | computedBy | then |
|--------|------------|------|
| **Purpose** | Business logic / domain computation | Type conversion / post-processing |
| **Input** | Source fields from `dependsOn` | Output of `computedBy` |
| **Naming Convention** | `to[FieldName](...)` if not specified | No convention - method name required |
| **Method Type** | Static or IoC bean instance method | Static only (pure function) |
| **Complexity** | Can be complex (business rules, dependencies) | Should be simple (1:1 transformation) |
| **Example** | `calculateTotalPrice(base, tax)` | `formatCurrency(bigDecimal)` |

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

For major changes, please open an issue first to discuss what you would like to change.

**Guidelines:**
- Follow existing code style
- Add tests for new features
- Update documentation
- Keep commits focused and atomic

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
- [Full Javadoc](https://javadoc.io/doc/io.github.cyfko/projection-spec)

---

## 🙏 Acknowledgments

Special thanks to all contributors and users who provide feedback and improvements to make this specification better!
