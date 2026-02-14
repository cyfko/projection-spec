package io.github.cyfko.projection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * References a specific method within a class.
 *
 * <p>
 * This annotation provides a flexible way to point to a method by specifying
 * its name and/or the class containing it. It supports both convention-based
 * and explicit method resolution.
 * </p>
 *
 * <h2>Resolution Strategy</h2>
 *
 * <table border="1">
 * <caption>How the target method is resolved based on specified elements</caption>
 * <tr>
 *   <th>{@code type}</th>
 *   <th>{@code value}</th>
 *   <th>Resolution</th>
 * </tr>
 * <tr>
 *   <td>Not set</td>
 *   <td>Not set</td>
 *   <td>Use convention-based resolution in the current or default context</td>
 * </tr>
 * <tr>
 *   <td>Not set</td>
 *   <td>Specified</td>
 *   <td>Search for the specified method name in the current or default context</td>
 * </tr>
 * <tr>
 *   <td>Specified</td>
 *   <td>Not set</td>
 *   <td>Use convention-based resolution in the specified class</td>
 * </tr>
 * <tr>
 *   <td>Specified</td>
 *   <td>Specified</td>
 *   <td>Use exactly the specified method in the specified class</td>
 * </tr>
 * </table>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Reference by Method Name Only</h3>
 * <pre>{@code
 * @Method("processData")
 * }</pre>
 *
 * <h3>Reference by Class Only</h3>
 * <pre>{@code
 * @Method(type = DataProcessor.class)
 * }</pre>
 *
 * <h3>Fully Qualified Reference</h3>
 * <pre>{@code
 * @Method(type = DataProcessor.class, value = "transform")
 * }</pre>
 *
 * <h3>Convention-Based (Empty)</h3>
 * <pre>{@code
 * @Method()  // Uses naming conventions from context
 * }</pre>
 *
 * @since 1.0.0
 * @author Frank KOSSI
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Method {

    /**
     * The class containing the target method.
     *
     * <p>
     * When specified, the method search is restricted to this class only.
     * </p>
     *
     * <p>
     * When not specified (default {@code void.class}), the resolution depends
     * on the annotation's usage context.
     * </p>
     *
     * @return the target class, or {@code void.class} for context-dependent resolution
     */
    Class<?> type() default void.class;

    /**
     * The name of the target method.
     *
     * <p>
     * When specified, references the method with this exact name.
     * </p>
     *
     * <p>
     * When not specified (empty string), convention-based naming is used
     * depending on the annotation's usage context.
     * </p>
     *
     * @return the method name, or empty string for convention-based resolution
     */
    String value() default "";
}