package io.smallrye.config;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link ConfigMappingContext}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods signatures</li>
 *   <li>Normal behaviour</li>
 *   <li>Edge cases and exception handling</li>
 * </ul>
 *
 * No external mocking frameworks are used – only real objects that can be instantiated
 * with the public API provided in the prompt.
 */
public class ConfigMappingContextTest {

    private ConfigMappingContext context;
    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor (as per the API description).
        config = new SmallRyeConfig();

        // No roots and no name mappings – we only need the context for the methods we test.
        Map<String, List<Class<?>>> roots = Collections.emptyMap();
        Map<String, Map<String, Set<String>>> names = Collections.emptyMap();

        context = new ConfigMappingContext(config, roots, names);
    }

    /* ---------------------------------------------------------------------- */
    /*  Public method signatures (extracted for reference)                    */
    /* ---------------------------------------------------------------------- */
    // <T> T constructRoot(Class<T> interfaceType)
    // <T> T constructGroup(Class<T> interfaceType)
    // <T> ObjectCreator<T> constructObject(String path)
    // <T> Converter<T> getConverterInstance(Class<? extends Converter<? extends T>> converterType)
    // NamingStrategy applyNamingStrategy(NamingStrategy namingStrategy)
    // StringBuilder getStringBuilder()
    // void reportProblem(RuntimeException problem)
    // List<Problem> getProblems()
    // Map<Class<?>, Map<String, ConfigMappingObject>> getRootsMap()
    // void reportUnknown(List<String> ignoredPaths)
    // static Iterable<String> filterPropertiesInRoots(Iterable<String> properties, Set<String> roots)
    // static boolean isPropertyInRoot(String property, String root)

    /* ---------------------------------------------------------------------- */
    /*  Tests for applyNamingStrategy                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testApplyNamingStrategyWhenNullAndNoPriorStrategy() {
        // No prior strategy, passing null should set the default (KEBAB_CASE)
        NamingStrategy result = context.applyNamingStrategy(null);
        assertEquals(NamingStrategy.KEBAB_CASE, result);
    }

    @Test
    public void testApplyNamingStrategyWhenNullAndAlreadySet() {
        // Set a strategy first
        context.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
        // Passing null now must keep the already set strategy
        NamingStrategy result = context.applyNamingStrategy(null);
        assertEquals(NamingStrategy.SNAKE_CASE, result);
    }

    @Test
    public void testApplyNamingStrategyWhenNonNull() {
        NamingStrategy result = context.applyNamingStrategy(NamingStrategy.VERBATIM);
        assertEquals(NamingStrategy.VERBATIM, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for getStringBuilder                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetStringBuilderIsReusable() {
        StringBuilder sb1 = context.getStringBuilder();
        sb1.append("hello");
        StringBuilder sb2 = context.getStringBuilder();
        assertSame(sb1, sb2);
        assertEquals("hello", sb2.toString());

        // Clear and reuse
        sb2.setLength(0);
        sb2.append("world");
        assertEquals("world", sb1.toString());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for reportProblem / getProblems                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testReportProblemAddsProblem() {
        RuntimeException ex = new IllegalArgumentException("bad value");
        context.reportProblem(ex);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals("java.lang.IllegalArgumentException: bad value", problems.get(0).getMessage());
    }

    @Test
    public void testGetProblemsReturnsLiveList() {
        List<Problem> problems = context.getProblems();
        assertTrue(problems.isEmpty());

        context.reportProblem(new RuntimeException("oops"));
        assertEquals(1, problems.size()); // same list instance
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for static filterPropertiesInRoots                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFilterPropertiesInRootsWithEmptyRootsReturnsAll() {
        List<String> props = Arrays.asList("a.b", "c.d", "e");
        Set<String> roots = Collections.emptySet();
        Iterable<String> result = ConfigMappingContext.filterPropertiesInRoots(props, roots);
        assertEquals(props, toList(result));
    }

    @Test
    public void testFilterPropertiesInRootsWithRootWildcardReturnsAll() {
        List<String> props = Arrays.asList("x", "y.z");
        Set<String> roots = new HashSet<>(Arrays.asList(""));
        Iterable<String> result = ConfigMappingContext.filterPropertiesInRoots(props, roots);
        assertEquals(props, toList(result));
    }

    @Test
    public void testFilterPropertiesInRootsMatchesCorrectly() {
        List<String> props = Arrays.asList(
                "my.root.prop",
                "my.root.prop.sub",
                "my.root[0]",
                "other.prop",
                "my.root2");
        Set<String> roots = new HashSet<>(Arrays.asList("my.root", "my.root2"));
        Iterable<String> result = ConfigMappingContext.filterPropertiesInRoots(props, roots);
        List<String> expected = Arrays.asList(
                "my.root.prop",
                "my.root.prop.sub",
                "my.root[0]",
                "my.root2");
        assertEquals(expected, toList(result));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for static isPropertyInRoot                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsPropertyInRootExactMatch() {
        assertTrue(ConfigMappingContext.isPropertyInRoot("foo.bar", "foo.bar"));
    }

    @Test
    public void testIsPropertyInRootWithDotSeparator() {
        assertTrue(ConfigMappingContext.isPropertyInRoot("foo.bar.baz", "foo.bar"));
    }

    @Test
    public void testIsPropertyInRootWithBracketSeparator() {
        assertTrue(ConfigMappingContext.isPropertyInRoot("foo.bar[0].baz", "foo.bar"));
    }

    @Test
    public void testIsPropertyInRootShorterProperty() {
        assertFalse(ConfigMappingContext.isPropertyInRoot("foo", "foo.bar"));
    }

    @Test
    public void testIsPropertyInRootInvalidSeparator() {
        assertFalse(ConfigMappingContext.isPropertyInRoot("foo-bar.baz", "foo-bar"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for getConverterInstance (caching and error handling)           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConverterInstanceCachesInstance() {
        Converter<String> c1 = context.getConverterInstance(SimpleStringConverter.class);
        Converter<String> c2 = context.getConverterInstance(SimpleStringConverter.class);
        assertSame(c1, c2);
    }

    @Test(expected = NoSuchMethodError.class)
    public void testGetConverterInstanceNoNoArgConstructor() {
        context.getConverterInstance(NoDefaultConstructorConverter.class);
    }

    @Test(expected = IllegalAccessError.class)
    public void testGetConverterInstancePrivateConstructor() {
        context.getConverterInstance(PrivateConstructorConverter.class);
    }

    @Test(expected = RuntimeException.class)
    public void testGetConverterInstanceConstructorThrowsRuntimeException() {
        context.getConverterInstance(ThrowingConstructorConverter.class);
    }

    @Test(expected = InstantiationError.class)
    public void testGetConverterInstanceAbstractClass() {
        // Abstract classes cause InstantiationException which is wrapped as InstantiationError
        context.getConverterInstance(AbstractConverter.class);
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    private static List<String> toList(Iterable<String> iterable) {
        List<String> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }

    /* ---------------------------------------------------------------------- */
    /*  Dummy Converter implementations for the reflection tests              */
    /* ---------------------------------------------------------------------- */

    public static class SimpleStringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    // No zero‑argument constructor – triggers NoSuchMethodError
    public static class NoDefaultConstructorConverter implements Converter<String> {
        public NoDefaultConstructorConverter(String dummy) { }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    // Private zero‑arg constructor – triggers IllegalAccessError
    public static class PrivateConstructorConverter implements Converter<String> {
        private PrivateConstructorConverter() { }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    // Constructor throws a RuntimeException – triggers the original RuntimeException
    public static class ThrowingConstructorConverter implements Converter<String> {
        public ThrowingConstructorConverter() {
            throw new RuntimeException("boom");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    // Abstract class – instantiation leads to InstantiationException → InstantiationError
    public static abstract class AbstractConverter implements Converter<String> {
        // No implementation; just to be abstract.
    }
}
