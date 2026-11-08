package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}.
 * The tests focus on the public API of {@link ConfigMappingContext}
 * and on the edge‑case handling of {@link ConfigMappingContext#getConverterInstance(Class)}.
 */
public class ConfigMappingContextTest {

    private ConfigMappingContext context;
    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor in the library.
        config = new SmallRyeConfig();
        // No roots and empty names map for the purpose of these tests.
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

    /* ---------------------------------------------------------------------- */
    /*  Basic functionality tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testApplyNamingStrategy_NullWhenUnsetDefaultsToKebabCase() {
        // At construction the namingStrategy is null.
        NamingStrategy result = context.applyNamingStrategy(null);
        assertNotNull(result);
        assertEquals(NamingStrategy.KEBAB_CASE, result);
    }

    @Test
    public void testApplyNamingStrategy_NonNullSetsValue() {
        NamingStrategy custom = NamingStrategy.VERBATIM;
        NamingStrategy result = context.applyNamingStrategy(custom);
        assertEquals(custom, result);
        // Subsequent call with null must keep the previously set strategy.
        NamingStrategy again = context.applyNamingStrategy(null);
        assertEquals(custom, again);
    }

    @Test
    public void testGetStringBuilder_IsMutableAndShared() {
        StringBuilder sb = context.getStringBuilder();
        assertNotNull(sb);
        sb.append("test");
        assertEquals("test", context.getStringBuilder().toString());
    }

    @Test
    public void testReportProblemAndGetProblems() {
        RuntimeException ex = new IllegalArgumentException("boom");
        context.reportProblem(ex);
        List<Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals("java.lang.IllegalArgumentException: boom", problems.get(0).getMessage());
    }

    @Test
    public void testGetRootsMapInitiallyEmpty() {
        Map<Class<?>, Map<String, ConfigMappingObject>> rootsMap = context.getRootsMap();
        assertNotNull(rootsMap);
        assertTrue(rootsMap.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  getConverterInstance success case                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConverterInstance_SuccessfulCreation() {
        Converter<String> converter = context.getConverterInstance(SimpleStringConverter.class);
        assertNotNull(converter);
        assertEquals("hello", converter.convert("hello"));
    }

    /* ---------------------------------------------------------------------- */
    /*  getConverterInstance error handling                                   */
    /* ---------------------------------------------------------------------- */

    @Test(expected = InstantiationError.class)
    public void testGetConverterInstance_InstantiationError() {
        // Converter whose public no‑arg constructor throws InstantiationException
        context.getConverterInstance(InstThrowsInstantiationExceptionConverter.class);
    }

    @Test(expected = IllegalAccessError.class)
    public void testGetConverterInstance_IllegalAccessError() {
        // Converter with a private no‑arg constructor
        context.getConverterInstance(PrivateConstructorConverter.class);
    }

    @Test(expected = NoSuchMethodError.class)
    public void testGetConverterInstance_NoSuchMethodError() {
        // Converter without a no‑arg constructor
        context.getConverterInstance(NoDefaultConstructorConverter.class);
    }

    @Test(expected = UndeclaredThrowableException.class)
    public void testGetConverterInstance_UndeclaredThrowableException() {
        // Converter whose constructor throws a checked exception wrapped by InvocationTargetException
        context.getConverterInstance(CheckedExceptionInConstructorConverter.class);
    }

    @Test(expected = RuntimeException.class)
    public void testGetConverterInstance_RuntimeExceptionFromConstructor() {
        // Converter whose constructor throws a RuntimeException directly
        context.getConverterInstance(RuntimeExceptionInConstructorConverter.class);
    }

    /* ---------------------------------------------------------------------- */
    /*  Private static helper methods (tested via reflection)                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsPropertyInRoot_ExactMatch() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "my.prop", "my.prop"));
    }

    @Test
    public void testIsPropertyInRoot_PrefixWithDot() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "my.prop.sub", "my.prop"));
    }

    @Test
    public void testIsPropertyInRoot_PrefixWithBracket() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "my.prop[0].value", "my.prop"));
    }

    @Test
    public void testIsPropertyInRoot_FalseWhenShorter() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        assertFalse((Boolean) m.invoke(null, "my", "my.prop"));
    }

    @Test
    public void testIsPropertyInRoot_FalseWhenNoSeparator() throws Exception {
        Method m = ConfigMappingContext.class.getDeclaredMethod("isPropertyInRoot", String.class, String.class);
        m.setAccessible(true);
        // property starts with root but the next char is not '.' or '['
        assertFalse((Boolean) m.invoke(null, "my.propX", "my.prop"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Dummy Converter implementations for error‑case testing               */
    /* ---------------------------------------------------------------------- */

    public static class SimpleStringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Constructor throws InstantiationException (simulated via RuntimeException). */
    public static class InstThrowsInstantiationExceptionConverter implements Converter<String> {
        public InstThrowsInstantiationExceptionConverter() throws InstantiationException {
            throw new InstantiationException("cannot instantiate");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Private no‑arg constructor. */
    public static class PrivateConstructorConverter implements Converter<String> {
        private PrivateConstructorConverter() {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** No default (no‑arg) constructor. */
    public static class NoDefaultConstructorConverter implements Converter<String> {
        public NoDefaultConstructorConverter(String dummy) {
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Constructor throws a checked exception wrapped in InvocationTargetException. */
    public static class CheckedExceptionInConstructorConverter implements Converter<String> {
        public CheckedExceptionInConstructorConverter() throws Exception {
            throw new Exception("checked exception");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** Constructor throws a RuntimeException directly. */
    public static class RuntimeExceptionInConstructorConverter implements Converter<String> {
        public RuntimeExceptionInConstructorConverter() {
            throw new RuntimeException("runtime failure");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }
}
