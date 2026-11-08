package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link ConfigMappingContext}.
 *
 * The tests cover:
 *  • basic functionality of every public method,
 *  • edge‑cases and exception handling,
 *  • private static helper methods via reflection.
 *
 * No external mocking frameworks are used; only real objects from the
 * public API are instantiated.
 */
public class ConfigMappingContextTest {

    private SmallRyeConfig config;
    private ConfigMappingContext ctx;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor in the library.
        config = new SmallRyeConfig();
        // Use an *empty* roots map – this avoids any interaction with ConfigMappingLoader.
        ctx = new ConfigMappingContext(config, Collections.emptyMap());
    }

    /* -------------------------------------------------------
     *  Public method – happy‑path tests
     * ------------------------------------------------------- */

    @Test
    public void testApplyNamingStrategyDefaultWhenNullAndNoPrevious() {
        NamingStrategy ns = ctx.applyNamingStrategy(null);
        assertEquals(NamingStrategy.KEBAB_CASE, ns);
    }

    @Test
    public void testApplyNamingStrategyKeepsExistingWhenNull() {
        ctx.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
        NamingStrategy after = ctx.applyNamingStrategy(null);
        assertEquals(NamingStrategy.SNAKE_CASE, after);
    }

    @Test
    public void testApplyNamingStrategySetsCustom() {
        ctx.applyNamingStrategy(NamingStrategy.VERBATIM);
        assertEquals(NamingStrategy.VERBATIM, ctx.applyNamingStrategy(NamingStrategy.VERBATIM));
    }

    @Test
    public void testApplyRootPath() {
        String root = "my.root";
        assertEquals(root, ctx.applyRootPath(root));
    }

    @Test
    public void testApplyNameBuilderAndGetNameBuilder() {
        StringBuilder sb = ctx.applyNameBuilder("base.path");
        assertSame(sb, ctx.getNameBuilder());
        assertEquals("base.path", sb.toString());

        // Subsequent call replaces content, not creates a new StringBuilder
        ctx.applyNameBuilder("new.path");
        assertEquals("new.path", ctx.getNameBuilder().toString());
    }

    @Test
    public void testConstructObjectCreatesCreator() {
        ConfigMappingContext.ObjectCreator<String> creator = ctx.constructObject("some.path");
        assertNotNull(creator);
    }

    @Test
    public void testGetConverterInstanceSimple() {
        SimpleConverter conv = ctx.getConverterInstance(SimpleConverter.class);
        assertNotNull(conv);
        assertEquals("hello", conv.convert("hello"));
    }

    @Test
    public void testReportProblemAddsToList() {
        RuntimeException ex = new RuntimeException("boom");
        ctx.reportProblem(ex);
        List<Problem> probs = ctx.getProblems();
        assertEquals(1, probs.size());
        assertEquals("java.lang.RuntimeException: boom", probs.get(0).getMessage());
    }

    @Test
    public void testGetRootsMapIsInitiallyEmpty() {
        assertTrue(ctx.getRootsMap().isEmpty());
    }

    /* -------------------------------------------------------
     *  Edge‑case & exception tests
     * ------------------------------------------------------- */

    @Test(expected = NoSuchMethodError.class)
    public void testGetConverterInstanceNoDefaultConstructor() {
        ctx.getConverterInstance(NoDefaultCtorConverter.class);
    }

    @Test(expected = IllegalAccessError.class)
    public void testGetConverterInstancePrivateConstructor() {
        ctx.getConverterInstance(PrivateCtorConverter.class);
    }

    @Test(expected = UndeclaredThrowableException.class)
    public void testGetConverterInstanceConstructorThrowsChecked() {
        ctx.getConverterInstance(CheckedExceptionCtorConverter.class);
    }

    @Test(expected = RuntimeException.class)
    public void testGetConverterInstanceConstructorThrowsRuntime() {
        ctx.getConverterInstance(RuntimeExceptionCtorConverter.class);
    }

    @Test
    public void testIndexOfDashesVariousScenarios() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("indexOfDashes", String.class, String.class);
        m.setAccessible(true);

        // 1. Simple match – no dashes required
        assertNull(m.invoke(null, "foo.bar", "foo.bar"));

        // 2. dash needed at position 3 (zero‑based)
        List<Integer> result = (List<Integer>) m.invoke(null, "foo-bar.baz", "foo.bar.baz");
        assertNotNull(result);
        assertEquals(Collections.singletonList(3), result);

        // 3. mapped longer than env → null
        assertNull(m.invoke(null, "very.long.mapped.name", "short"));

        // 4. separator mismatch → null
        assertNull(m.invoke(null, "foo.bar", "foo-bar"));
    }

    @Test
    public void testFilterPropertiesInRootsEmptyRoots() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        m.setAccessible(true);

        List<String> props = Arrays.asList("a", "b.c", "d.e.f");
        Set<String> emptyRoots = Collections.emptySet();

        Iterable<String> result = (Iterable<String>) m.invoke(null, props, emptyRoots);
        assertEquals(props, toList(result));
    }

    @Test
    public void testFilterPropertiesInRootsWildcardRoot() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        m.setAccessible(true);

        List<String> props = Arrays.asList("x", "y.z");
        Set<String> roots = new HashSet<>(Collections.singletonList(""));

        Iterable<String> result = (Iterable<String>) m.invoke(null, props, roots);
        assertEquals(props, toList(result));
    }

    @Test
    public void testFilterPropertiesInRootsSpecificRoot() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
        m.setAccessible(true);

        List<String> props = Arrays.asList("app.db.url", "app.db.user", "other.prop");
        Set<String> roots = new HashSet<>(Collections.singletonList("app.db"));

        Iterable<String> result = (Iterable<String>) m.invoke(null, props, roots);
        List<String> expected = Arrays.asList("app.db.url", "app.db.user");
        assertEquals(expected, toList(result));
    }

    @Test
    public void testIsPropertyInRootEdgeCases() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);

        // Exact match
        assertTrue((Boolean) m.invoke(null, "foo.bar", "foo.bar"));

        // Property shorter than root
        assertFalse((Boolean) m.invoke(null, "foo", "foo.bar"));

        // Proper delimiter '.' after root
        assertTrue((Boolean) m.invoke(null, "foo.bar.baz", "foo.bar"));

        // Improper delimiter (letter) after root
        assertFalse((Boolean) m.invoke(null, "fooXbar", "foo"));
    }

    @Test(expected = NullPointerException.class)
    public void testReportProblemWithNull() {
        ctx.reportProblem(null);
    }

    /* -------------------------------------------------------
     *  Helper converters used for exception‑handling tests
     * ------------------------------------------------------- */

    public static class SimpleConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** No public no‑arg constructor – triggers NoSuchMethodError */
    public static class NoDefaultCtorConverter implements Converter<String> {
        public NoDefaultCtorConverter(String arg) { }
        @Override public String convert(String value) { return value; }
    }

    /** Private no‑arg constructor – triggers IllegalAccessError */
    public static class PrivateCtorConverter implements Converter<String> {
        private PrivateCtorConverter() { }
        @Override public String convert(String value) { return value; }
    }

    /** Constructor that throws a checked exception – wrapped in UndeclaredThrowableException */
    public static class CheckedExceptionCtorConverter implements Converter<String> {
        public CheckedExceptionCtorConverter() throws Exception {
            throw new Exception("checked");
        }
        @Override public String convert(String value) { return value; }
    }

    /** Constructor that throws a RuntimeException – re‑thrown directly */
    public static class RuntimeExceptionCtorConverter implements Converter<String> {
        public RuntimeExceptionCtorConverter() {
            throw new RuntimeException("runtime");
        }
        @Override public String convert(String value) { return value; }
    }

    /* -------------------------------------------------------
     *  Utility methods
     * ------------------------------------------------------- */

    private static List<String> toList(Iterable<String> iterable) {
        List<String> list = new ArrayList<>();
        for (String s : iterable) {
            list.add(s);
        }
        return list;
    }
}
