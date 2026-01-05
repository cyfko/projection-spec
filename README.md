# Projection Spec

## 1. Project Overview

Projection Spec is a Java annotation specification for declaratively describing Data Transfer Object (DTO) projections from JPA entities. It defines a stable, implementation-agnostic set of annotations for expressing how entity fields are exposed, renamed, or computed in DTOs, supporting both direct and computed field mappings. This specification is intended for use by annotation processors, frameworks, or tools that implement or consume projection semantics.

## 2. Architecture Overview

- **Annotation-Driven Specification**: All configuration is performed via Java annotations defined by this specification.
- **Core Annotations**:
  - `@Projection`: Declares a DTO as a projection of a JPA entity.
  - `@Provider`: Registers classes responsible for computed or virtual fields.
  - `@Projected`: Maps a DTO field directly from an entity field.
  - `@Computed`: Declares a DTO field as computed from one or more entity fields.
  - `@MethodReference`: Explicitly specifies the computation method for a computed field.
- **Provider Resolution**: Supports both static classes and IoC-managed beans, as defined by the specification.

## 3. Core Concepts

- **Projection**: A DTO class annotated with `@Projection`, specifying the source entity and computation providers.
- **Provider**: A class (static or bean) supplying methods for computed or virtual fields.
- **Projected Field**: A DTO field mapped directly from an entity field, optionally renamed or using a nested path.
- **Computed Field**: A DTO field whose value is derived from one or more entity fields via a provider method.
- **Method Reference**: Mechanism to override default method resolution for computed fields.

## 4. How It Works

1. **Declare a DTO** with `@Projection`, specifying the entity and providers.
2. **Map fields**:
   - Use `@Projected` for direct mappings (or rely on name matching).
   - Use `@Computed` for fields requiring computation, specifying dependencies.
3. **Computation Resolution**:
   - By default, the processor looks for a method named `get[FieldName]` in providers.
   - `@MethodReference` can override the method name and/or provider.
   - Providers are searched in declaration order; first match is used.
   - If a provider bean name is specified, it is resolved from the IoC container; otherwise, static methods are used.

## 5. Usage Guide

- **Direct Mapping**:
  ```java
  @Projection(entity = User.class)
  public class UserDTO {
      private String email; // Maps to User.email
      @Projected(from = "createdAt")
      private LocalDateTime registrationDate; // Maps to User.createdAt
  }
  ```

- **Computed Field**:
  ```java
  @Projection(
      entity = User.class,
      providers = {@Provider(UserComputations.class)}
  )
  public class UserDTO {
      @Computed(dependsOn = {"firstName", "lastName"})
      private String fullName;
  }

  public class UserComputations {
      public static String getFullName(String firstName, String lastName) {
          return firstName + " " + lastName;
      }
  }
  ```

- **Bean-based Provider**:
  ```java
  @Projection(
      entity = User.class,
      providers = {@Provider(value = DateFormatter.class, bean = "isoDateFormatter")}
  )
  public class UserDTO {
      @Computed(dependsOn = {"createdAt"})
      private String formattedDate;
  }
  ```

- **Explicit Method Reference**:
  ```java
  @Computed(
      dependsOn = {"firstName", "lastName"},
      computedBy = @MethodReference(method = "formatFullName")
  )
  private String displayName;
  ```

## 6. Limitations & Constraints

- **Source Retention**: All annotations are `RetentionPolicy.SOURCE`; no runtime reflection or logic is present.
- **Entity-Only Dependencies**: Computed fields can only depend on fields from the source entity, not on other computed or DTO fields.
- **No Circular Dependencies**: The design prohibits computed-to-computed dependencies, eliminating dependency graph complexity.
- **No Runtime Processing**: All logic is intended for annotation processors; the specification does not provide runtime mapping.

## 7. Extension & Improvement Notes (OPTIONAL RECOMMENDATIONS)

- **Optional**: Consider providing reference implementations or processor guidelines for common frameworks.
- **Optional**: Enhance documentation with more real-world examples and integration guides for popular frameworks.
- **Optional**: Provide validation utilities to assist annotation processor authors.

---

**All documentation above is strictly aligned with the current specification. No features or behaviors are described beyond what is present in the codebase.**

## 🤝 Contributing

Contributions are welcome! Please open an issue to discuss major changes before submitting a pull request.

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 👤 Author

**Frank KOSSI**

- Email: frank.kossi@kunrin.com, frank.kossi@sprint-pay.com
- Organization: [Kunrin SA](https://www.kunrin.com), [Sprint-Pay SA](https://www.sprint-pay.com)

## 🔗 Links

- [GitHub Repository](https://github.com/cyfko/jpa-metamodel-processor)
- [Issue Tracker](https://github.com/cyfko/jpa-metamodel-processor/issues)
- [Maven Central](https://search.maven.org/artifact/io.github.cyfko/jpa-metamodel-processor)