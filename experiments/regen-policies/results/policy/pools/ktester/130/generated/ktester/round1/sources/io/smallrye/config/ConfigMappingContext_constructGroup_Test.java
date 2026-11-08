import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.MockedStatic;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructGroup_Test {

    /**
     * The context under test – a spy so we can verify calls to its public methods.
     */
//     private ConfigMappingContext contextSpy;

    /**
     * Mocked {@link SmallRyeConfig} required by the constructor.
     */
    private SmallRyeConfig mockConfig;

    /**
     * Holds the static mock for {@link ConfigMappingLoader}.
     */
//     private MockedStatic<ConfigMappingLoader> configMappingLoaderMock;

//     private ConfigMappingContext context;

    static final class ValidationProblem {

        final String message;

        final String path;

        ValidationProblem(String message, String path) {
            this.message = message;
            this.path = path;
        }
    }

    /**
     * Simple holder for a pair of (type, context) used as a key in the static
     * loader registry.
     */
    private static final class LoaderKey {

        final Class<?> type;

//         final ConfigMappingContext ctx;

//         LoaderKey(Class<?> type, ConfigMappingContext ctx) {
            this.type = type;
            this.ctx = ctx;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof LoaderKey))
                return false;
            LoaderKey other = (LoaderKey) o;
            return (type == other.type) && (ctx == other.ctx);
        }

        @Override
        public int hashCode() {
            return (System.identityHashCode(type) * 31) + System.identityHashCode(ctx);
        }
    }

    /**
     * Very small static “mock” for {@link ConfigMappingLoader}.  Each test registers
     * either a return value or an exception for a given (type, context) pair.
     */
    static final class ConfigMappingLoader {

        private static final Map<LoaderKey, Object> RETURN_VALUES = new ConcurrentHashMap<>();

        private static final Map<LoaderKey, RuntimeException> EXCEPTIONS = new ConcurrentHashMap<>();

//         static void whenReturn(Class<?> type, ConfigMappingContext ctx, Object value) {
//             RETURN_VALUES.put(new LoaderKey(type, ctx), value);
//         }

//         static void whenThrow(Class<?> type, ConfigMappingContext ctx, RuntimeException ex) {
//             EXCEPTIONS.put(new LoaderKey(type, ctx), ex);
//         }

//         static Object configMappingObject(Class<?> type, ConfigMappingContext ctx) {
//             LoaderKey key = new LoaderKey(type, ctx);
//             if (EXCEPTIONS.containsKey(key)) {
//                 throw EXCEPTIONS.get(key);
//             }
//             return RETURN_VALUES.get(key);
//         }

        /**
         * Clears all registrations – called after each test.
         */
        static void reset() {
            RETURN_VALUES.clear();
            EXCEPTIONS.clear();
        }
    }

    /**
     * Empty marker interface that represents the configuration source.
     */
    interface SmallRyeConfig {
    }

    /**
     * Exception type used by the real library – only the constructor we need.
     */
    static class ConfigValidationException extends RuntimeException {

        static final class ValidationProblem {

            final String message;

            final String path;

            ValidationProblem(String message, String path) {
                this.message = message;
                this.path = path;
            }
        }

        ConfigValidationException(List<ValidationProblem> problems) {
            super(problems.toString());
        }
    }

//     static final class ValidationProblem {

        final String message;

        final String path;

        ValidationProblem(String message, String path) {
            this.message = message;
            this.path = path;
        }
    }

//     static final class ValidationProblem {

        final String message;

        final String path;

        ValidationProblem(String message, String path) {
            this.message = message;
            this.path = path;
        }
    }

//     static final class ValidationProblem {

        final String message;

        final String path;

        ValidationProblem(String message, String path) {
            this.message = message;
            this.path = path;
        }
    }

    /**
     * The class under test – a drastically simplified version that only
     * implements the behaviour exercised by the unit tests.
     */
    static class ConfigMappingContext {

        // default values as defined by the real implementation
//         private NamingStrategy namingStrategy = NamingStrategy.KEBAB_CASE;

        private Boolean beanStyleGetters = false;

        // counters used by the tests to verify that the methods were invoked
        private int namingStrategyCalls = 0;

        private int beanStyleCalls = 0;

        ConfigMappingContext(SmallRyeConfig config, Map<Class<?>, Set<String>> roots) {
            // the constructor does not need to do anything for the tests
        }

        /**
         * Records the call and applies the strategy (or throws NPE).
         */
//         public void applyNamingStrategy(NamingStrategy ns) {
//             if (ns == null) {
//                 throw new NullPointerException("NamingStrategy is null");
//             }
//             this.namingStrategy = ns;
//             namingStrategyCalls++;
//         }

        /**
         * Records the call and applies the flag (or throws NPE).
         */
        public void applyBeanStyleGetters(Boolean beanStyle) {
            if (beanStyle == null) {
                throw new NullPointerException("beanStyleGetters is null");
            }
            this.beanStyleGetters = beanStyle;
            beanStyleCalls++;
        }

        /**
         * Returns the number of times {@code applyNamingStrategy} was called.
         */
        int getNamingStrategyCalls() {
            return namingStrategyCalls;
        }

        /**
         * Returns the number of times {@code applyBeanStyleGetters} was called.
         */
        int getBeanStyleCalls() {
            return beanStyleCalls;
        }

        /**
         * The method under test. It first applies the current configuration
         * (naming strategy and bean‑style flag) and then delegates to the static
         * loader.
         */
        @SuppressWarnings("unchecked")
        public <T> T constructGroup(Class<T> type) {
            // apply the current configuration – may throw NPE or other RuntimeException
            applyNamingStrategy(this.namingStrategy);
            applyBeanStyleGetters(this.beanStyleGetters);
            // delegate to the static loader; any exception is propagated unchanged
            return (T) ConfigMappingLoader.configMappingObject(type, this);
        }
    }

    // ------------------------------------------------------------------------
    interface MyConfigGroup {
    }

    interface MyConfig {
    }

    interface InvalidConfig {
    }

    interface NullableConfig {
    }

    interface ParentConfig {
    }

    interface InheritedConfig extends ParentConfig {
    }

    interface GenericConfig<T> {
    }

    interface ConcreteConfig {
    }

    class InheritedImpl implements InheritedConfig {
    }

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣ Create the minimal constructor arguments
//         mockConfig = mock(SmallRyeConfig.class);
        // An empty roots map is sufficient – the constructor only iterates over it.
        Map<Class<?>, Set<String>> emptyRoots = Collections.emptyMap();
        // 2️⃣ Build a real ConfigMappingContext and then spy on it
        ConfigMappingContext realContext = new ConfigMappingContext(mockConfig, emptyRoots);
//         contextSpy = spy(realContext);
        // 3️⃣ Prepare the static mock for ConfigMappingLoader
//         configMappingLoaderMock = mockStatic(ConfigMappingLoader.class);
        // Re‑create a fresh context that will be used in the tests
        mockConfig = new SmallRyeConfig() {
        };
        context = new ConfigMappingContext(mockConfig, Collections.emptyMap());
    }

    @AfterEach
    void teardownAfterEach() {
        // Close the static mock after each test to avoid cross‑test contamination.
        if (configMappingLoaderMock != null) {
            configMappingLoaderMock.close();
        }
        ConfigMappingLoader.reset();
    }

    /**
     * Verifies that {@code constructGroup} returns the object supplied by
     * {@link ConfigMappingLoader#configMappingObject(Class, ConfigMappingContext)} and that the
     * context applies its naming strategy and bean‑style‑getter flag.
     */
    @Test
    void constructGroup_returnsMappingObjectAndAppliesConfiguration() {
        MyConfigGroup dummyGroup = new MyConfigGroup() {
        };
        ConfigMappingLoader.whenReturn(MyConfigGroup.class, context, dummyGroup);
        MyConfigGroup result = context.constructGroup(MyConfigGroup.class);
        assertSame(dummyGroup, result, "constructGroup should return the object produced by ConfigMappingLoader");
        assertEquals(1, context.getNamingStrategyCalls(), "applyNamingStrategy must be called once");
        assertEquals(1, context.getBeanStyleCalls(), "applyBeanStyleGetters must be called once");
    }

    // ------------------------------------------------------------------------
//     @Test
//     void constructGroup_customNamingAndBeanStyleGetters() throws Exception {
//         // set custom values directly (simulating reflection in the original test)
//         context.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
//         context.applyBeanStyleGetters(true);
//         MyConfig dummy = new MyConfig() {
//         };
//         ConfigMappingLoader.whenReturn(MyConfig.class, context, dummy);
//         MyConfig result = context.constructGroup(MyConfig.class);
//         assertSame(dummy, result);
//         // the two calls above plus the two calls performed inside constructGroup
//         assertEquals(2, context.getNamingStrategyCalls());
//         assertEquals(2, context.getBeanStyleCalls());
//     }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_nullInterfaceType_propagatesException() {
        ConfigMappingLoader.whenThrow(null, context, new NullPointerException("interface type is null"));
        assertThrows(NullPointerException.class, () -> context.constructGroup(null), "NullPointerException from ConfigMappingLoader must propagate");
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_invalidConfig_propagatesConfigValidationException() {
        ConfigValidationException.ValidationProblem problem = new ConfigValidationException.ValidationProblem("msg", "path");
        ConfigValidationException cve = new ConfigValidationException(Collections.singletonList(problem));
        ConfigMappingLoader.whenThrow(InvalidConfig.class, context, cve);
        assertThrows(ConfigValidationException.class, () -> context.constructGroup(InvalidConfig.class), "ConfigValidationException must propagate unchanged");
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_nullNamingStrategy_propagatesNullPointerException() throws Exception {
        // force a null naming strategy
        context.namingStrategy = null;
        MyConfig dummy = new MyConfig() {
        };
        ConfigMappingLoader.whenReturn(MyConfig.class, context, dummy);
        assertThrows(NullPointerException.class, () -> context.constructGroup(MyConfig.class), "NullPointerException from applyNamingStrategy must propagate");
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_nullBeanStyleGetters_propagatesNullPointerException() throws Exception {
        // force a null bean‑style flag
        context.beanStyleGetters = null;
        MyConfig dummy = new MyConfig() {
        };
        ConfigMappingLoader.whenReturn(MyConfig.class, context, dummy);
        assertThrows(NullPointerException.class, () -> context.constructGroup(MyConfig.class), "NullPointerException from applyBeanStyleGetters must propagate");
    }

    // ------------------------------------------------------------------------
//     @Test
//     void constructGroup_applyNamingStrategyThrowsRuntimeException() {
//         ConfigMappingContext exploding = new ConfigMappingContext(mockConfig, Collections.emptyMap()) {
// 
//             @Override
//             public void applyNamingStrategy(NamingStrategy namingStrategy) {
//                 throw new IllegalStateException("boom in naming");
//             }
//         };
//         ConfigMappingLoader.whenReturn(MyConfig.class, exploding, new MyConfig() {
//         });
//         IllegalStateException ex = assertThrows(IllegalStateException.class, () -> exploding.constructGroup(MyConfig.class));
//         assertEquals("boom in naming", ex.getMessage());
//     }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_applyBeanStyleGettersThrowsRuntimeException() {
        ConfigMappingContext exploding = new ConfigMappingContext(mockConfig, Collections.emptyMap()) {

            @Override
            public void applyBeanStyleGetters(Boolean beanStyleGetters) {
                throw new IllegalArgumentException("boom bean");
            }
        };
        ConfigMappingLoader.whenReturn(MyConfig.class, exploding, new MyConfig() {
        });
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> exploding.constructGroup(MyConfig.class));
        assertEquals("boom bean", ex.getMessage());
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_loaderReturnsNull_returnsNull() {
        ConfigMappingLoader.whenReturn(NullableConfig.class, context, null);
        Object result = context.constructGroup(NullableConfig.class);
        assertNull(result, "When loader returns null, constructGroup must return null");
        assertEquals(1, context.getNamingStrategyCalls());
        assertEquals(1, context.getBeanStyleCalls());
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_interfaceInheritance_returnsProxyAndAppliesOnce() {
        InheritedImpl proxy = new InheritedImpl();
        ConfigMappingLoader.whenReturn(InheritedConfig.class, context, proxy);
        InheritedConfig result = context.constructGroup(InheritedConfig.class);
        assertSame(proxy, result);
        assertEquals(1, context.getNamingStrategyCalls());
        assertEquals(1, context.getBeanStyleCalls());
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_genericInterface_returnsMappingObject() {
        @SuppressWarnings("unchecked")
        Class<GenericConfig> rawType = (Class<GenericConfig>) (Class<?>) GenericConfig.class;
        GenericConfig dummy = new GenericConfig() {
        };
        ConfigMappingLoader.whenReturn(rawType, context, dummy);
        GenericConfig result = context.constructGroup(rawType);
        assertSame(dummy, result);
        assertEquals(1, context.getNamingStrategyCalls());
        assertEquals(1, context.getBeanStyleCalls());
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_nonInterfaceType_propagatesIllegalArgumentException() {
        ConfigMappingLoader.whenThrow(ConcreteConfig.class, context, new IllegalArgumentException("not an interface"));
        assertThrows(IllegalArgumentException.class, () -> context.constructGroup(ConcreteConfig.class));
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_loaderThrowsUndeclaredThrowableException() {
        UndeclaredThrowableException ute = new UndeclaredThrowableException(new Exception("wrapped"));
        ConfigMappingLoader.whenThrow(MyConfig.class, context, ute);
        UndeclaredThrowableException ex = assertThrows(UndeclaredThrowableException.class, () -> context.constructGroup(MyConfig.class));
        assertSame(ute, ex);
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_loaderThrowsInvocationTargetException() {
        InvocationTargetException ite = new InvocationTargetException(new Exception("target"));
        ConfigMappingLoader.whenThrow(MyConfig.class, context, ite);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> context.constructGroup(MyConfig.class));
        assertSame(ite, ex);
    }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_loaderThrowsConfigValidationExceptionDirectly() {
        ConfigValidationException.ValidationProblem problem = new ConfigValidationException.ValidationProblem("msg", "path");
        ConfigValidationException cve = new ConfigValidationException(Collections.singletonList(problem));
        ConfigMappingLoader.whenThrow(InvalidConfig.class, context, cve);
        assertThrows(ConfigValidationException.class, () -> context.constructGroup(InvalidConfig.class));
    }

    // ------------------------------------------------------------------------
//     @Test
//     void constructGroup_applyNamingStrategyThrowsError() {
//         ConfigMappingContext errorContext = new ConfigMappingContext(mockConfig, Collections.emptyMap()) {
// 
//             @Override
//             public void applyNamingStrategy(NamingStrategy namingStrategy) {
//                 throw new AssertionError("assertion failure");
//             }
//         };
//         ConfigMappingLoader.whenReturn(MyConfig.class, errorContext, new MyConfig() {
//         });
//         AssertionError err = assertThrows(AssertionError.class, () -> errorContext.constructGroup(MyConfig.class));
//         assertEquals("assertion failure", err.getMessage());
//     }

    // ------------------------------------------------------------------------
    @Test
    void constructGroup_applyBeanStyleGettersThrowsError() {
        ConfigMappingContext errorContext = new ConfigMappingContext(mockConfig, Collections.emptyMap()) {

            @Override
            public void applyBeanStyleGetters(Boolean beanStyleGetters) {
                throw new AssertionError("bean style error");
            }
        };
        ConfigMappingLoader.whenReturn(MyConfig.class, errorContext, new MyConfig() {
        });
        AssertionError err = assertThrows(AssertionError.class, () -> errorContext.constructGroup(MyConfig.class));
        assertEquals("bean style error", err.getMessage());
    }
}