package io.smallrye.config;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}.
 *
 * <p>These tests focus on the public behaviour of the context that can be exercised
 * without needing a full MicroProfile configuration implementation.  Where necessary
 * we use reflection to reach private static helpers (e.g. {@code indexOfDashes},
 * {@code isPropertyInRoot}).  The {@link SmallRyeConfig} instance is created using
 * its public no‑arg constructor – this is sufficient for the parts of the API
 * exercised here because the tests do not depend on actual configuration values
 * being read.</p>
 *
 * <p>Each test method targets a single method/behaviour of {@code ConfigMappingContext}
 * and includes both typical and edge‑case scenarios.</p>
 */
public class ConfigMappingContextTest {

    private SmallRyeConfig config;
    private ConfigMappingContext context;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor in the library.
        config = new SmallRyeConfig();
        // Provide an empty mappings map – the constructor will initialise the
        // internal structures without trying to map any prefixes.
        Map<Class<?>, Set<String>> emptyMappings = new HashMap<>();
        context = new ConfigMappingContext(config, emptyMappings);
    }

    @Test
    public void testApplyPrefixUpdatesNameBuilder() {
        context.applyPrefix("my.prefix");
        assertEquals("my.prefix", context.getNameBuilder().toString());

        // Applying a second prefix should replace the previous content
        context.applyPrefix("other");
        assertEquals("other", context.getNameBuilder().toString());
    }

    @Test
    public void testApplyNamingStrategyChangesStrategy() {
        // default is KEBAB_CASE
        assertSame(NamingStrategy.KEBAB_CASE, context.propertyName());

        // Apply a different strategy
        context.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
        assertSame(NamingStrategy.SNAKE_CASE, context.propertyName());

        // Null argument must be ignored – strategy stays the same
        context.applyNamingStrategy(null);
        assertSame(NamingStrategy.SNAKE_CASE, context.propertyName());
    }

    @Test
    public void testApplyBeanStyleGettersToggle() {
        // default is false – propertyName returns the naming strategy directly
        assertSame(NamingStrategy.KEBAB_CASE, context.propertyName());

        // Enable bean‑style getters
        context.applyBeanStyleGetters(Boolean.TRUE);
        // The returned Function should be a composition of BEAN_STYLE_GETTERS and the naming strategy
        // Verify the behaviour with a sample getter name
        Function<String, String> fn = context.propertyName();
        String transformed = fn.apply("getMyValue");
        // Bean style removes "get" and lower‑cases first char, then KEBAB_CASE adds dashes
        // "MyValue" -> "myValue" -> "my-value"
        assertEquals("my-value", transformed);

        // Disable again
        context.applyBeanStyleGetters(Boolean.FALSE);
        assertSame(NamingStrategy.KEBAB_CASE, context.propertyName());
    }

    @Test
    public void testReportProblemAddsProblem() {
        RuntimeException ex = new RuntimeException("test problem");
        context.reportProblem(ex);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals("java.lang.RuntimeException: test problem", problems.get(0).getMessage());

        // Adding another problem should increase the list
        context.reportProblem(new IllegalArgumentException("another"));
        assertEquals(2, context.getProblems().size());
    }

    @Test
    public void testGetMappingsInitiallyEmpty() {
        // With no mappings supplied to the constructor we expect an empty map
        assertTrue(context.getMappings().isEmpty());
    }

    @Test
    public void testIndexOfDashesVariousScenarios() throws Exception {
        Method method = ConfigMappingContext.class.getDeclaredMethod("indexOfDashes", String.class, String.class);
        method.setAccessible(true);

        // 1. Exact match – no dashes to replace
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(null, "foo.bar", "foo.bar");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // 2. Dash in mapped property – should report position of '.' in env property
        result = (List<Integer>) method.invoke(null, "foo-bar", "foo.bar");
        assertNotNull(result);
        assertEquals(Collections.singletonList(3), result); // '.' at index 3

        // 3. Mismatch length – should return null
        result = (List<Integer>) method.invoke(null, "very.long.property", "short");
        assertNull(result);

        // 4. Mapping with wildcard '*' (map) – should succeed without dash positions
        result = (List<Integer>) method.invoke(null, "foo.*.baz", "foo.bar.baz");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // 5. Mapping with collection ']' – should succeed
        result = (List<Integer>) method.invoke(null, "list[0].value", "list[0].value");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIsPropertyInRootScenarios() throws Exception {
        Method method = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        method.setAccessible(true);

        // Exact match
        Boolean match = (Boolean) method.invoke(null, "root", "root");
        assertTrue(match);

        // Property is a direct child (dot separator)
        match = (Boolean) method.invoke(null, "root.child", "root");
        assertTrue(match);

        // Property is a direct child (array/list separator)
        match = (Boolean) method.invoke(null, "root[0]", "root");
        assertTrue(match);

        // Property starts with same prefix but not separated correctly
        match = (Boolean) method.invoke(null, "rootchild", "root");
        assertFalse(match);

        // Property shorter than root – cannot be in root
        match = (Boolean) method.invoke(null, "ro", "root");
        assertFalse(match);
    }

    @Test
    public void testConstructObjectReturnsObjectCreatorAndValueRetrieval() {
        // The constructObject method should return a non‑null ObjectCreator
        ConfigMappingContext.ObjectCreator<String> creator = context.constructObject("test.path");
        assertNotNull(creator);
        assertEquals("test.path", creator.toString()); // toString is not overridden; just ensure no NPE

        // Use the ObjectCreator to retrieve a simple value using the static helper.
        // Since SmallRyeConfig has no actual values, the call will return null.
        // The test only verifies that no exception is thrown and the method chain works.
        ConfigMappingContext.ObjectCreator<String> valueCreator = creator.value(String.class, null);
        assertNotNull(valueCreator);
        // Getting the root value should be null (no config entry)
        assertNull(valueCreator.get());
    }
}
