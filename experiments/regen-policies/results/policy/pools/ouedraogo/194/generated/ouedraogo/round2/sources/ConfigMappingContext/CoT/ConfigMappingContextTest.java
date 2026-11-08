package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.util.*;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappingContext}.
 * <p>
 * The tests focus on the public API of the class.  They verify the basic behaviour of each method,
 * and also exercise edge‑case and exceptional paths such as passing {@code null} arguments or
 * using a {@link Converter} implementation without a public no‑arg constructor.
 * </p>
 *
 * <p>
 * A minimal {@link SmallRyeConfig} instance is created using its public no‑arg constructor.
 * The instance is deliberately left with its default (empty) configuration because the
 * tested methods do not depend on any real configuration values.
 * </p>
 */
public class ConfigMappingContextTest {

    private SmallRyeConfig emptyConfig;
    private ConfigMappingContext context;

    @Before
    public void setUp() {
        // SmallRyeConfig has a public no‑arg constructor (see the public API above)
        emptyConfig = new SmallRyeConfig();

        // Initialise the mapping context with an empty mapping map – the constructor will run
        // but will not populate any mappings.
        context = new ConfigMappingContext(emptyConfig, Collections.emptyMap());
    }

    /* ---------------------------------------------------------------------- */
    /*  1.  Public method list (extracted from the source)                     */
    /* ---------------------------------------------------------------------- */
    // Constructor
    // <T> T constructMapping(Class<T> interfaceType, String prefix)
    // public <T> T constructGroup(Class<T> interfaceType)
    // public <T> ObjectCreator<T> constructObject(String path)
    // public <T> Converter<T> getConverterInstance(Class<? extends Converter<? extends T>> converterType)
    // public void applyPrefix(String prefix)
    // public void applyNamingStrategy(NamingStrategy namingStrategy)
    // public void applyBeanStyleGetters(Boolean beanStyleGetters)
    // public Function<String, String> propertyName()
    // public StringBuilder getNameBuilder()
    // public void reportProblem(RuntimeException problem)
    // List<Problem> getProblems()
    // Map<Class<?>, Map<String, Object>> getMappings()
    // void reportUnknown(Set<String> ignoredPaths)

    /* ---------------------------------------------------------------------- */
    /*  2.  Basic functionality tests                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testApplyPrefixUpdatesNameBuilder() {
        context.applyPrefix("my.prefix");
        assertEquals("my.prefix", context.getNameBuilder().toString());

        // applying a second prefix should replace the previous content
        context.applyPrefix("other");
        assertEquals("other", context.getNameBuilder().toString());
    }

    @Test
    public void testApplyNamingStrategyAndBeanStyleGetters() {
        // default strategy is KEBAB_CASE and beanStyleGetters is false
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, getPrivateField("namingStrategy"));
        assertFalse(getPrivateField("beanStyleGetters"));

        // change both
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.SNAKE_CASE);
        context.applyBeanStyleGetters(Boolean.TRUE);

        assertEquals(ConfigMapping.NamingStrategy.SNAKE_CASE, getPrivateField("namingStrategy"));
        assertTrue(getPrivateField("beanStyleGetters"));
    }

    @Test
    public void testApplyNamingStrategyIgnoresNull() {
        ConfigMapping.NamingStrategy original = getPrivateField("namingStrategy");
        context.applyNamingStrategy(null);
        assertEquals(original, getPrivateField("namingStrategy"));
    }

    @Test
    public void testApplyBeanStyleGettersIgnoresNull() {
        Boolean original = getPrivateField("beanStyleGetters");
        context.applyBeanStyleGetters(null);
        assertEquals(original, getPrivateField("beanStyleGetters"));
    }

    @Test
    public void testPropertyNameFunctionRespectsBeanStyleFlag() {
        // beanStyleGetters = false → naming strategy only
        context.applyBeanStyleGetters(Boolean.FALSE);
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.VERBATIM);
        Function<String, String> fn = context.propertyName();
        assertEquals("my-value", fn.apply("my-value"));

        // beanStyleGetters = true → bean‑style conversion first, then naming strategy
        context.applyBeanStyleGetters(Boolean.TRUE);
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.VERBATIM);
        fn = context.propertyName();
        assertEquals("value", fn.apply("getValue"));
        assertEquals("active", fn.apply("isActive"));
        // non‑bean style names are left unchanged
        assertEquals("plain", fn.apply("plain"));
    }

    @Test
    public void testReportProblemAddsProblem() {
        RuntimeException ex = new RuntimeException("boom");
        context.reportProblem(ex);
        List<ConfigValidationException.Problem> problems = context.getProblems();
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().contains("boom"));
    }

    @Test
    public void testConstructObjectReturnsObjectCreator() {
        ConfigMappingContext.ObjectCreator<String> creator = context.constructObject("some.path");
        assertNotNull(creator);
        // the creator should produce a non‑null root after get()
        assertNull(creator.get()); // because no mapping logic was executed – just a sanity check
    }

    @Test
    public void testGetMappingsIsEmptyWhenNoMappingsProvided() {
        assertTrue(context.getMappings().isEmpty());
    }

    @Test
    public void testReportUnknownWithEmptySetDoesNotThrow() {
        // Should simply complete without NPE
        context.reportUnknown(Collections.emptySet());
    }

    /* ---------------------------------------------------------------------- */
    /*  3.  Edge‑case & exceptional path tests                               */
    /* ---------------------------------------------------------------------- */

    /**
     * A simple {@link Converter} implementation with a public no‑arg constructor.
     */
    public static class DummyConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value == null ? null : value.toUpperCase();
        }
    }

    /**
     * Converter without a public no‑arg constructor – used to trigger the
     * {@code NoSuchMethodError} path in {@link ConfigMappingContext#getConverterInstance(Class)}.
     */
    public static class NoDefaultCtorConverter implements Converter<String> {
        private final String arg;

        public NoDefaultCtorConverter(String arg) {
            this.arg = arg;
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    @Test
    public void testGetConverterInstanceCreatesInstance() {
        Converter<String> conv = context.getConverterInstance(DummyConverter.class);
        assertNotNull(conv);
        assertEquals("HELLO", conv.convert("hello"));
    }

    @Test(expected = NoSuchMethodError.class)
    public void testGetConverterInstanceNoDefaultConstructorThrows() {
        // The context should attempt to instantiate the class via reflection and fail,
        // resulting in a NoSuchMethodError as per the implementation.
        context.getConverterInstance(NoDefaultCtorConverter.class);
    }

    @Test(expected = InstantiationError.class)
    public void testGetConverterInstanceInstantiationExceptionWrapped() throws Exception {
        // Use a dynamically generated class that throws InstantiationException from its
        // no‑arg constructor.  This is achieved via reflection on a private inner class.
        class BadConverter implements Converter<String> {
            public BadConverter() throws InstantiationException {
                throw new InstantiationException("boom");
            }

            @Override
            public String convert(String value) {
                return value;
            }
        }
        // The compiler creates a distinct class, we just invoke the method.
        context.getConverterInstance(BadConverter.class);
    }

    @Test(expected = IllegalAccessError.class)
    public void testGetConverterInstanceIllegalAccessExceptionWrapped() throws Exception {
        // Define a converter with a private no‑arg constructor.
        class PrivateCtorConverter implements Converter<String> {
            private PrivateCtorConverter() {
            }

            @Override
            public String convert(String value) {
                return value;
            }
        }
        context.getConverterInstance(PrivateCtorConverter.class);
    }

    @Test(expected = UndeclaredThrowableException.class)
    public void testGetConverterInstanceInvocationTargetExceptionWrapped() throws Exception {
        // Converter whose constructor throws a checked exception wrapped in InvocationTargetException.
        class ThrowsCheckedException implements Converter<String> {
            public ThrowsCheckedException() throws Exception {
                throw new Exception("checked");
            }

            @Override
            public String convert(String value) {
                return value;
            }
        }
        context.getConverterInstance(ThrowsCheckedException.class);
    }

    /* ---------------------------------------------------------------------- */
    /*  4.  Helper to access private fields for verification                 */
    /* ---------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(String name) {
        try {
            java.lang.reflect.Field f = ConfigMappingContext.class.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
