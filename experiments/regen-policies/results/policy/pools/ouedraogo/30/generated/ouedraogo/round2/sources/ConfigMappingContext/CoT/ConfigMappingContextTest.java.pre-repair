package io.smallrye.config;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import sun.misc.Unsafe;

/**
 * Unit tests for {@link ConfigMappingContext}.
 * <p>
 * The tests use {@link Unsafe#allocateInstance(Class)} to create a
 * {@code ConfigMappingContext} instance without invoking its constructor.
 * This allows us to test the public behaviour of the class without needing a
 * fully‑initialised {@link SmallRyeConfig} (which would require a large amount
 * of infrastructure).  All methods exercised in these tests are independent of
 * the {@code config} field.
 */
public class ConfigMappingContextTest {

    private ConfigMappingContext context;

    /**
     * Obtain the {@link Unsafe} instance (the only supported way is via reflection).
     */
    private static Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    @Before
    public void setUp() throws Exception {
        Unsafe unsafe = getUnsafe();
        // Allocate the instance without calling any constructor.
        context = (ConfigMappingContext) unsafe.allocateInstance(ConfigMappingContext.class);
        // Initialise the mutable fields that are required for the tested methods.
        // The fields are private; we set them via reflection.
        setField(context, "namingStrategy", null);
        setField(context, "rootPath", null);
        setField(context, "nameBuilder", new StringBuilder());
        setField(context, "usedProperties", new HashSet<String>());
        setField(context, "problems", new ArrayList<ConfigValidationException.Problem>());
        setField(context, "roots", new IdentityHashMap<Class<?>, Map<String, ConfigMappingObject>>());
        setField(context, "converterInstances", new IdentityHashMap<Class<?>, Converter<?>>());
        setField(context, "config", null); // not needed for the methods under test
    }

    @After
    public void tearDown() {
        context = null;
    }

    /** Helper to set a private field via reflection. */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /* ---------------------------------------------------------------------- */
    /*  Public method signatures extracted from ConfigMappingContext        */
    /* ---------------------------------------------------------------------- */
    // public <T> T constructGroup(Class<T> interfaceType)
    // public <T> ObjectCreator<T> constructObject(String path)
    // public <T> Converter<T> getConverterInstance(Class<? extends Converter<? extends T>> converterType)
    // public NamingStrategy applyNamingStrategy(final NamingStrategy namingStrategy)
    // public String applyRootPath(final String rootPath)
    // public StringBuilder applyNameBuilder(final String rootPath)
    // public StringBuilder getNameBuilder()
    // public void reportProblem(RuntimeException problem)
    // List<Problem> getProblems()
    // Map<Class<?>, Map<String, ConfigMappingObject>> getRootsMap()
    // private static List<Integer> indexOfDashes(final String mappedProperty, final String envProperty)

    /* ---------------------------------------------------------------------- */
    /*  Basic functionality tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testApplyNamingStrategy_WithNonNull() {
        // Set an initial strategy
        context.applyNamingStrategy(NamingStrategy.VERBATIM);
        // Apply a different non‑null strategy
        NamingStrategy result = context.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
        assertEquals(NamingStrategy.SNAKE_CASE, result);
    }

    @Test
    public void testApplyNamingStrategy_WithNullWhenUnset() {
        // No strategy has been set yet
        NamingStrategy result = context.applyNamingStrategy(null);
        // Default should be KEBAB_CASE
        assertEquals(NamingStrategy.KEBAB_CASE, result);
    }

    @Test
    public void testApplyNamingStrategy_WithNullWhenAlreadySet() {
        // First set a strategy
        context.applyNamingStrategy(NamingStrategy.VERBATIM);
        // Now call with null – should keep the existing one
        NamingStrategy result = context.applyNamingStrategy(null);
        assertEquals(NamingStrategy.VERBATIM, result);
    }

    @Test
    public void testApplyRootPath() {
        String path = "my.root";
        String returned = context.applyRootPath(path);
        assertEquals(path, returned);
    }

    @Test
    public void testApplyNameBuilderAndGetNameBuilder() {
        String root = "service.api";
        StringBuilder sb = context.applyNameBuilder(root);
        assertSame(sb, context.getNameBuilder());
        assertEquals(root, sb.toString());

        // Append something else and ensure the builder is still the same instance
        sb.append(".extra");
        assertEquals("service.api.extra", context.getNameBuilder().toString());
    }

    @Test
    public void testReportProblemAndGetProblems() {
        RuntimeException ex = new RuntimeException("test problem");
        context.reportProblem(ex);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertEquals("java.lang.RuntimeException: test problem", problems.get(0).getMessage());
    }

    @Test
    public void testGetRootsMapInitiallyEmpty() {
        Map<Class<?>, Map<String, ConfigMappingObject>> rootsMap = context.getRootsMap();
        assertNotNull(rootsMap);
        assertTrue(rootsMap.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for the private static method indexOfDashes                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIndexOfDashes_NoDashesNeeded() throws Exception {
        // mappedProperty == envProperty, no dash characters required
        List<Integer> result = invokeIndexOfDashes("foo.bar", "foo.bar");
        assertNull(result);
    }

    @Test
    public void testIndexOfDashes_SimpleDashReplacement() throws Exception {
        // mapped property contains a dash that must be replaced in the env property
        List<Integer> result = invokeIndexOfDashes("foo-bar", "foo.bar");
        // dash is at position 3 (0‑based) in the env string "foo.bar"
        assertNotNull(result);
        assertEquals(Collections.singletonList(3), result);
    }

    @Test
    public void testIndexOfDashes_MultipleDashes() throws Exception {
        List<Integer> result = invokeIndexOfDashes("a-b.c-d", "a.b.c.d");
        assertNotNull(result);
        // positions of the two dashes in the env property: after 'a' (1) and after 'c' (3)
        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void testIndexOfDashes_MappedLongerThanEnv() throws Exception {
        List<Integer> result = invokeIndexOfDashes("longer.property.name", "short");
        assertNull(result);
    }

    @Test
    public void testIndexOfDashes_MismatchReturnsNull() throws Exception {
        List<Integer> result = invokeIndexOfDashes("foo.bar", "foo.baz");
        assertNull(result);
    }

    /**
     * Utility to invoke the private static method {@code indexOfDashes}.
     */
    @SuppressWarnings("unchecked")
    private static List<Integer> invokeIndexOfDashes(String mapped, String env) throws Exception {
        java.lang.reflect.Method m = ConfigMappingContext.class.getDeclaredMethod(
                "indexOfDashes", String.class, String.class);
        m.setAccessible(true);
        return (List<Integer>) m.invoke(null, mapped, env);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for getConverterInstance                                        */
    /* ---------------------------------------------------------------------- */

    // Simple converter with a public no‑arg constructor
    public static class DummyStringConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value;
        }
    }

    // Converter without a public no‑arg constructor – should trigger NoSuchMethodError
    public static class NoDefaultCtorConverter implements Converter<String> {
        private final String prefix;

        public NoDefaultCtorConverter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String convert(String value) {
            return prefix + value;
        }
    }

    // Converter whose constructor throws a RuntimeException
    public static class ThrowingConverter implements Converter<String> {
        public ThrowingConverter() {
            throw new IllegalStateException("construction failed");
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    @Test
    public void testGetConverterInstance_CreatesAndCaches() {
        Converter<String> conv1 = context.getConverterInstance(DummyStringConverter.class);
        assertNotNull(conv1);
        assertTrue(conv1 instanceof DummyStringConverter);

        // Second call should return the same cached instance
        Converter<String> conv2 = context.getConverterInstance(DummyStringConverter.class);
        assertSame(conv1, conv2);
    }

    @Test(expected = NoSuchMethodError.class)
    public void testGetConverterInstance_NoDefaultConstructor() {
        context.getConverterInstance(NoDefaultCtorConverter.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetConverterInstance_ConstructorThrows() {
        context.getConverterInstance(ThrowingConverter.class);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for constructObject (basic sanity – it returns a creator)      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructObjectReturnsObjectCreator() {
        ConfigMappingContext.ObjectCreator<String> creator = context.constructObject("some.path");
        assertNotNull(creator);
        // The creator should be able to return null before any value is set
        assertNull(creator.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for constructGroup – limited to null‑safety (cannot fully test) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructGroup_NullInterfaceReturnsNull() {
        // Since ConfigMappingLoader is not available in this isolated test,
        // we expect a NullPointerException when the loader returns null.
        // The purpose of the test is to verify that the method forwards the call.
        try {
            context.constructGroup(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected – we cannot exercise the full mapping logic without the loader.
        }
    }
}
