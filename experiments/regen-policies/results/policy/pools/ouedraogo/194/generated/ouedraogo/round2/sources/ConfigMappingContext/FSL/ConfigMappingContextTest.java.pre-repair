package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}.
 * <p>
 * The tests focus on the public behaviour of the context as well as the private
 * utility methods {@code indexOfDashes} and {@code isPropertyInRoot} which are
 * exercised via reflection.
 */
public class ConfigMappingContextTest {

    private ConfigMappingContext context;
    private SmallRyeConfig config;

    @Before
    public void setUp() throws Exception {
        // SmallRyeConfig has a public no‑arg constructor in the current source version.
        config = new SmallRyeConfig();
        // Use an empty mapping – the constructor will still initialise the context.
        context = new ConfigMappingContext(config, Collections.emptyMap());
    }

    // ----------------------------------------------------------------------
    // Simple state‑changing methods
    // ----------------------------------------------------------------------
    @Test
    public void testApplyPrefixUpdatesNameBuilder() {
        context.applyPrefix("my.prefix");
        assertEquals("my.prefix", context.getNameBuilder().toString());

        // applying a second prefix should replace the previous content
        context.applyPrefix("other");
        assertEquals("other", context.getNameBuilder().toString());
    }

    @Test
    public void testApplyNamingStrategyChangesStrategy() {
        // default is KEBAB_CASE
        assertSame(ConfigMapping.NamingStrategy.KEBAB_CASE, context.propertyName().apply("myValue"));

        // switch to VERBATIM and verify the function returns the original string
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.VERBATIM);
        assertSame(ConfigMapping.NamingStrategy.VERBATIM, context.propertyName());
        assertEquals("myValue", context.propertyName().apply("myValue"));
    }

    @Test
    public void testApplyBeanStyleGettersEnablesTransformation() {
        // without bean‑style getters the function is the naming strategy itself
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.VERBATIM);
        assertEquals("getFoo", context.propertyName().apply("getFoo"));

        // enable bean‑style getters
        context.applyBeanStyleGetters(Boolean.TRUE);
        // now "getFoo" should become "foo" (VERBATIM after bean transformation)
        assertEquals("foo", context.propertyName().apply("getFoo"));
        // also test an "is" getter
        assertEquals("active", context.propertyName().apply("isActive"));
    }

    // ----------------------------------------------------------------------
    // Converter handling
    // ----------------------------------------------------------------------
    public static class DummyStringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return "converted:" + value;
        }
    }

    @Test
    public void testGetConverterInstanceCreatesAndCachesConverter() {
        Converter<String> conv1 = context.getConverterInstance(DummyStringConverter.class);
        assertNotNull(conv1);
        assertEquals("converted:hello", conv1.convert("hello"));

        // second call should return the same cached instance
        Converter<String> conv2 = context.getConverterInstance(DummyStringConverter.class);
        assertSame(conv1, conv2);
    }

    // ----------------------------------------------------------------------
    // Problem reporting
    // ----------------------------------------------------------------------
    @Test
    public void testReportProblemAddsProblemToList() {
        RuntimeException ex = new RuntimeException("boom");
        context.reportProblem(ex);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals("java.lang.RuntimeException: boom", problems.get(0).getMessage());
    }

    @Test
    public void testGetMappingsIsEmptyWhenNoMappingsProvided() {
        assertTrue(context.getMappings().isEmpty());
    }

    // ----------------------------------------------------------------------
    // Private static utility methods (accessed via reflection)
    // ----------------------------------------------------------------------
    @Test
    public void testIndexOfDashesMatchesVariousPatterns() throws Exception {
        Method method = ConfigMappingContext.class.getDeclaredMethod("indexOfDashes", String.class, String.class);
        method.setAccessible(true);

        // Simple match – no dashes to replace
        @SuppressWarnings("unchecked")
        List<Integer> result1 = (List<Integer>) method.invoke(null, "foo.bar", "foo.bar");
        assertTrue(result1.isEmpty());

        // One dash expected (mapped property uses '-', env uses '.')
        List<Integer> result2 = (List<Integer>) method.invoke(null, "foo-bar.baz", "foo.bar.baz");
        // dash is at position 3 (zero‑based) in the env string "foo.bar.baz"
        assertEquals(Collections.singletonList(3), result2);

        // Wildcard '*' handling – should ignore the segment and still match
        List<Integer> result3 = (List<Integer>) method.invoke(null, "foo.*.baz", "foo.bar.baz");
        assertTrue(result3.isEmpty());

        // Mismatch – length of mapped > env => null
        Object result4 = method.invoke(null, "very.long.property.name", "short");
        assertNull(result4);
    }

    @Test
    public void testIsPropertyInRootScenarios() throws Exception {
        Method method = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        method.setAccessible(true);

        // Exact match
        assertTrue((Boolean) method.invoke(null, "my.prop", "my.prop"));

        // Property is a child of the root (dot separator)
        assertTrue((Boolean) method.invoke(null, "my.prop.sub", "my.prop"));

        // Property is a child of the root (array index)
        assertTrue((Boolean) method.invoke(null, "my.prop[0]", "my.prop"));

        // Property is shorter than root – cannot be in root
        assertFalse((Boolean) method.invoke(null, "my", "my.prop"));

        // Same length but different character
        assertFalse((Boolean) method.invoke(null, "my.propX", "my.prop"));
    }

    // ----------------------------------------------------------------------
    // Additional sanity checks for propertyName function composition
    // ----------------------------------------------------------------------
    @Test
    public void testPropertyNameFunctionCombinesBeanStyleAndNamingStrategy() {
        // Use SNAKE_CASE naming strategy and enable bean‑style getters
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        context.applyBeanStyleGetters(Boolean.TRUE);

        // "getMyValue" -> bean style => "myValue" -> snake case => "my_value"
        String transformed = context.propertyName().apply("getMyValue");
        assertEquals("my_value", transformed);
    }
}
