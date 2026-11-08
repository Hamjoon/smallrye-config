package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructRoot_Test {

//     @Mock
    SmallRyeConfig // mock to avoid heavy initialization
    mockConfig;

    private ConfigMappingContext context;

    private ConfigMappingContext spyContext;

    /**
     * Simple test implementation that allows us to control the behaviour of {@code constructGroup}.
     */
//     static class TestConfigMappingContext extends ConfigMappingContext {

//         private final Map<Class<?>, Object> returnMap = new HashMap<>();

//         private final Map<Class<?>, RuntimeException> exceptionMap = new HashMap<>();

//         TestConfigMappingContext(SmallRyeConfig config, Map<Class<?>, Set<String>> roots) {
            super(config, roots);
        }

        /**
         * Configure a successful return value for the given interface.
         */
        <T> void whenConstructGroupReturns(Class<T> iface, T value) {
            returnMap.put(iface, value);
        }

        /**
         * Configure an exception to be thrown when {@code constructGroup} is called with the given interface.
         */
        void whenConstructGroupThrows(Class<?> iface, RuntimeException ex) {
            exceptionMap.put(iface, ex);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T constructGroup(Class<T> iface) {
            if (exceptionMap.containsKey(iface)) {
                throw exceptionMap.get(iface);
            }
            if (returnMap.containsKey(iface)) {
                return (T) returnMap.get(iface);
            }
            // Default behaviour – delegate to the real implementation (which may itself throw)
            return super.constructGroup(iface);
        }
    }

    interface DummyConfig {
    }

    interface AnotherDummyConfig {
    }

    interface EmptyInterface {
    }

    interface InterfaceWithDefaultMethods {

        default String hello() {
            return "hi";
        }
    }

    interface BaseConfig {
    }

    interface InterfaceExtendingAnother extends BaseConfig {
    }

    interface GenericInterface<T> {
    }

    interface ClassWithStaticMethods {

        static void util() {
        }

        int value();
    }

    interface InterfaceWithAnnotatedMethods {

        String name();
    }

    interface InterfaceWithMethodParameters {

        String greet(String name);
    }

    interface InterfaceWithUnsupportedReturn {

        java.io.File file();
    }

    interface InterfaceWithDuplicateProperties {

        String prop();

        String getProp();
    }

    interface InterfaceExtendingNonConfig extends java.io.Serializable {

        String data();
    }

    interface InterfaceWithGenericReturn {

        java.util.List<String> list();
    }

    interface InterfaceWithArrayReturn {

        String[] array();
    }

    interface DuplicateRootConfig {
    }

    /**
     * Minimal {@code SmallRyeConfig} implementation used only to satisfy the
     * {@code ConfigMappingContext} constructor.  All methods throw
     * {@code UnsupportedOperationException} because the tests never invoke them.
     */
//     private static class DummySmallRyeConfig implements SmallRyeConfig {

//         @Override
//         public ConfigSource getConfigSource(String name) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Iterable<ConfigSource> getConfigSources() {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public <T> T getValue(String name, Class<T> type) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public <T> Optional<T> getOptionalValue(String name, Class<T> type) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public <T> T getValue(String name, Class<T> type, T defaultValue) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public Optional<String> getOptionalValue(String name) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public String getValue(String name) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public <T> T convert(String value, Class<T> type) {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public <T> T convert(String value, Class<T> type, Converter<T> converter) {
//             throw new UnsupportedOperationException();
//         }
    }

    // intentionally invalid (whitespace only)
//     @ConfigMapping(rootPath = "   ")
    interface InterfaceWithInvalidRootPath {
    }

    interface InterfaceWithoutConfigMappingAnnotation {

        String something();
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor only stores the config and the roots map; an empty map is sufficient.
//         SmallRyeConfig dummyConfig = new DummySmallRyeConfig();
//         Map<Class<?>, Set<String>> emptyRoots = Collections.emptyMap();
        context = new ConfigMappingContext(dummyConfig, emptyRoots);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks/spies after each test to avoid cross‑test interference
//         reset(mockConfig);
//         reset(mockConfig);
    }

//     private static Stream<Arguments> edgeCaseProvider() {
//         return Stream.of(Arguments.of(EmptyInterface.class), Arguments.of(InterfaceWithDefaultMethods.class), Arguments.of(InterfaceExtendingAnother.class), Arguments.of(GenericInterface.class), Arguments.of(ClassWithStaticMethods.class), Arguments.of(InterfaceWithAnnotatedMethods.class));
//     }

//     @Test
//     void constructRoot_delegatesToConstructGroup_andReturnsItsResult() {
//         DummyConfig expected = new DummyConfig() {
//         };
//         context.whenConstructGroupReturns(DummyConfig.class, expected);
//         DummyConfig actual = context.constructRoot(DummyConfig.class);
//         Assertions.assertSame(expected, actual, "constructRoot should return the object produced by constructGroup");
//     }

    @Test
    void constructRoot_withNullArgument_throwsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> context.constructRoot(null));
    }

    @Test
    void constructRoot_nonInterface_throwsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> context.constructRoot(String.class));
    }

//     @Test
//     void constructRoot_delegatesExactlyOnce() {
//         AnotherDummyConfig expected = new AnotherDummyConfig() {
//         };
//         context.whenConstructGroupReturns(AnotherDummyConfig.class, expected);
//         AnotherDummyConfig actual = context.constructRoot(AnotherDummyConfig.class);
//         Assertions.assertSame(expected, actual);
//     }

//     @ParameterizedTest(name = "[{index}] constructRoot on {0}")
//     @MethodSource("edgeCaseProvider")
//     void constructRoot_edgeCases_delegatesAndReturnsResult(Class<?> iface) {
//         Object expected = new Object();
//         context.whenConstructGroupReturns(iface, expected);
//         Object actual = context.constructRoot(iface);
//         Assertions.assertSame(expected, actual, "Expected the stubbed proxy instance to be returned");
//     }

//     @Test
//     void constructRoot_interfaceWithMethodParameters_throwsIllegalArgumentException() {
//         context.whenConstructGroupThrows(InterfaceWithMethodParameters.class, new IllegalArgumentException("Methods with parameters not allowed"));
//         Assertions.assertThrows(IllegalArgumentException.class, () -> context.constructRoot(InterfaceWithMethodParameters.class));
//     }

//     @Test
//     void constructRoot_interfaceWithUnsupportedReturn_throwsIllegalArgumentException() {
//         context.whenConstructGroupThrows(InterfaceWithUnsupportedReturn.class, new IllegalArgumentException("Unsupported return type"));
//         Assertions.assertThrows(IllegalArgumentException.class, () -> context.constructRoot(InterfaceWithUnsupportedReturn.class));
//     }

//     @Test
//     void constructRoot_interfaceWithDuplicateProperties_throwsConfigValidationException() {
//         ConfigValidationException expected = new ConfigValidationException(Collections.singletonList(new ConfigValidationException.Problem("duplicate property", "duplicate")));
//         context.whenConstructGroupThrows(InterfaceWithDuplicateProperties.class, expected);
//         ConfigValidationException thrown = Assertions.assertThrows(ConfigValidationException.class, () -> context.constructRoot(InterfaceWithDuplicateProperties.class));
//         Assertions.assertEquals(expected.getProblems(), thrown.getProblems(), "The same validation problems should be propagated");
//     }

    @Test
    void constructRoot_interfaceWithoutConfigMappingAnnotation_throwsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> context.constructRoot(InterfaceWithoutConfigMappingAnnotation.class));
    }

    @Test
    void constructRoot_interfaceWithInvalidRootPath_throwsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> context.constructRoot(InterfaceWithInvalidRootPath.class));
    }

//     @Test
//     void constructRoot_interfaceExtendingNonConfig_throwsIllegalArgumentException() {
//         context.whenConstructGroupThrows(InterfaceExtendingNonConfig.class, new IllegalArgumentException("Extends non‑config interface"));
//         Assertions.assertThrows(IllegalArgumentException.class, () -> context.constructRoot(InterfaceExtendingNonConfig.class));
//     }

//     @Test
//     void constructRoot_interfaceWithGenericReturn_throwsIllegalArgumentException() {
//         context.whenConstructGroupThrows(InterfaceWithGenericReturn.class, new IllegalArgumentException("Generic return type not supported"));
//         Assertions.assertThrows(IllegalArgumentException.class, () -> context.constructRoot(InterfaceWithGenericReturn.class));
//     }

//     @Test
//     void constructRoot_interfaceWithArrayReturn_throwsIllegalArgumentException() {
//         context.whenConstructGroupThrows(InterfaceWithArrayReturn.class, new IllegalArgumentException("Array return type not supported"));
//         Assertions.assertThrows(IllegalArgumentException.class, () -> context.constructRoot(InterfaceWithArrayReturn.class));
//     }

//     @Test
//     void constructRoot_duplicateRootConfig_throwsIllegalStateException() {
//         context.whenConstructGroupThrows(DuplicateRootConfig.class, new IllegalStateException("Root already constructed"));
//         Assertions.assertThrows(IllegalStateException.class, () -> context.constructRoot(DuplicateRootConfig.class));
//     }
}