package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import java.lang.reflect.UndeclaredThrowableException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.microprofile.config.inject.ConfigMapping;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_configMappingObject_Test {

    /**
     * A {@link SmallRyeConfig} instance shared by all tests.
     */
    private static SmallRyeConfig CONFIG;

    /**
     * The {@link ConfigMappingContext} used for each test execution.
     */
    private ConfigMappingContext context;

    /**
     * A class annotated with {@code @ConfigProperties}. It is deliberately *not* a
     * {@code @ConfigMapping} so that the generated implementation does not contain
     * the {@code ConfigMappingContext} constructor required by {@code configMappingObject}.
     */
    @ConfigProperties(prefix = "server")
    public static class ServerProperties {

        public String host;

        public int port;
    }

    @Nested
    @DisplayName("NoSuchMethodException path")
    class NoSuchMethodExceptionPath {

        @Test
        void missingConstructorLeadsToNoSuchMethodError() {
            assertThrows(NoSuchMethodError.class, () -> ConfigMappingLoader.configMappingObject(MissingConstructorMapping.class, context));
        }
    }

    @Nested
    @DisplayName("InstantiationException path")
    class InstantiationExceptionPath {

        @Test
        void abstractImplementationLeadsToInstantiationError() {
            assertThrows(InstantiationError.class, () -> ConfigMappingLoader.configMappingObject(AbstractImplementationMapping.class, context));
        }
    }

    @Nested
    @DisplayName("IllegalAccessException path")
    class IllegalAccessExceptionPath {

        @Test
        void privateConstructorLeadsToIllegalAccessError() {
            assertThrows(IllegalAccessError.class, () -> ConfigMappingLoader.configMappingObject(PrivateConstructorMapping.class, context));
        }
    }

    @Nested
    @DisplayName("InvocationTargetException – cause is RuntimeException")
    class InvocationTargetRuntimeExceptionPath {

        @Test
        void runtimeExceptionIsUnwrappedAndPropagated() {
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.configMappingObject(RuntimeExceptionDuringConstruction.class, context));
            assertEquals("boom", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("InvocationTargetException – cause is Error")
    class InvocationTargetErrorPath {

        @Test
        void errorIsUnwrappedAndPropagated() {
            Error thrown = assertThrows(Error.class, () -> ConfigMappingLoader.configMappingObject(ErrorDuringConstruction.class, context));
            assertEquals("fatal", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("InvocationTargetException – cause is checked Throwable")
    class InvocationTargetCheckedExceptionPath {

        @Test
        void checkedExceptionIsWrappedInUndeclaredThrowableException() {
            UndeclaredThrowableException ex = assertThrows(UndeclaredThrowableException.class, () -> ConfigMappingLoader.configMappingObject(CheckedExceptionDuringConstruction.class, context));
            assertTrue(ex.getUndeclaredThrowable() instanceof IOException);
        }
    }

    @Nested
    @DisplayName("Cache reuse across multiple calls")
    class CacheReusePath {

        @Test
        void sameImplementationClassIsUsedOnSubsequentCalls() {
            // First call – forces the ClassValue to compute the holder.
            ValidConfigMapping first = ConfigMappingLoader.configMappingObject(ValidConfigMapping.class, context);
            // Second call – should retrieve the same implementation class from the cache.
            ValidConfigMapping second = ConfigMappingLoader.configMappingObject(ValidConfigMapping.class, context);
            assertSame(first.getClass(), second.getClass(), "Implementation class should be retrieved from CACHE on subsequent calls");
        }
    }

    @Nested
    @DisplayName("Null arguments")
    class NullArgumentsPath {

        @Test
        void nullInterfaceTypeThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingObject(null, context));
        }

        @Test
        void nullContextIsAcceptedIfConstructorAllowsIt() {
            // The constructor of NullContextMapping does not dereference the argument,
            // therefore instantiation succeeds.
            NullContextMapping instance = ConfigMappingLoader.configMappingObject(NullContextMapping.class, null);
            assertNotNull(instance);
        }
    }

    @Nested
    @DisplayName("Implementation class does not extend ConfigMappingObject")
    class WrongSuperClassPath {

        @Test
        void nonConfigMappingObjectImplementationLeadsToClassCastException() {
            assertThrows(ClassCastException.class, () -> ConfigMappingLoader.configMappingObject(NonConfigMappingObject.class, context));
        }
    }

    @Nested
    @DisplayName("Checked exception wrapped in RuntimeException")
    class CheckedExceptionWrappedInRuntimePath {

        @Test
        void runtimeWrapperIsPropagatedUnchanged() {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.configMappingObject(CheckedExceptionWrappedInRuntime.class, context));
            assertEquals("wrapped", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Package‑private constructor")
    class PackagePrivateConstructorPath {

        @Test
        void packagePrivateConstructorLeadsToIllegalAccessError() {
            assertThrows(IllegalAccessError.class, () -> ConfigMappingLoader.configMappingObject(PackagePrivateConstructorMapping.class, context));
        }
    }

    @Nested
    @DisplayName("Constructor throws AssertionError")
    class AssertionErrorPath {

        @Test
        void assertionErrorIsPropagated() {
            AssertionError err = assertThrows(AssertionError.class, () -> ConfigMappingLoader.configMappingObject(AssertionErrorDuringConstruction.class, context));
            assertEquals("assertion failed", err.getMessage());
        }
    }

    @Nested
    @DisplayName("Missing implementation class for interface")
    class MissingImplementationPath {

        @Test
        void missingImplementationLeadsToClassCastException() {
            assertThrows(ClassCastException.class, () -> ConfigMappingLoader.configMappingObject(InterfaceWithoutImplementation.class, context));
        }
    }

    // -----------------------------------------------------------------
    @ConfigMapping(prefix = "server")
    public interface ServerMapping {

        String host();

        int port();
    }

    /**
     * Valid mapping – has a public constructor taking {@link ConfigMappingContext}.
     */
    public static class ValidConfigMapping implements ConfigMappingObject {

        public ValidConfigMapping(ConfigMappingContext ctx) {
            // No further initialization required for the test
        }
    }

    /**
     * No constructor that accepts {@link ConfigMappingContext}.
     */
    public static class MissingConstructorMapping implements ConfigMappingObject {

        public MissingConstructorMapping() {
        }
    }

    /**
     * Abstract class – cannot be instantiated via reflection.
     */
    public abstract static class AbstractImplementationMapping implements ConfigMappingObject {

        public AbstractImplementationMapping(ConfigMappingContext ctx) {
        }
    }

    /**
     * Private constructor – illegal‑access when instantiated via reflection.
     */
    public static class PrivateConstructorMapping implements ConfigMappingObject {

        private PrivateConstructorMapping(ConfigMappingContext ctx) {
        }
    }

    /**
     * Constructor throws a {@link RuntimeException}.
     */
    public static class RuntimeExceptionDuringConstruction implements ConfigMappingObject {

        public RuntimeExceptionDuringConstruction(ConfigMappingContext ctx) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * Constructor throws an {@link Error}.
     */
    public static class ErrorDuringConstruction implements ConfigMappingObject {

        public ErrorDuringConstruction(ConfigMappingContext ctx) {
            throw new AssertionError("fatal");
        }
    }

    /**
     * Constructor throws a checked {@link IOException}.
     */
    public static class CheckedExceptionDuringConstruction implements ConfigMappingObject {

        public CheckedExceptionDuringConstruction(ConfigMappingContext ctx) throws IOException {
            throw new IOException("io problem");
        }
    }

    /**
     * Constructor catches a checked exception and re‑throws it as a {@link RuntimeException}.
     */
    public static class CheckedExceptionWrappedInRuntime implements ConfigMappingObject {

        public CheckedExceptionWrappedInRuntime(ConfigMappingContext ctx) {
            try {
                throw new IOException("io");
            } catch (IOException e) {
                throw new RuntimeException("wrapped", e);
            }
        }
    }

    /**
     * Constructor throws an {@link AssertionError}.
     */
    public static class AssertionErrorDuringConstruction implements ConfigMappingObject {

        public AssertionErrorDuringConstruction(ConfigMappingContext ctx) {
            throw new AssertionError("assertion failed");
        }
    }

    /**
     * Class that does **not** implement {@link ConfigMappingObject}.
     */
    public static class NonConfigMappingObject {

        public NonConfigMappingObject(ConfigMappingContext ctx) {
        }
    }

    /**
     * Mapping whose generated implementation is missing – we use the interface
     * directly, which means {@code getImplementationClass} will return the interface
     * itself (which does not implement {@link ConfigMappingObject}) and the final cast
     * fails.
     */
    public interface InterfaceWithoutImplementation {
        // empty
    }

    /**
     * Constructor that accepts {@code null} for the context.
     */
    public static class NullContextMapping implements ConfigMappingObject {

        public NullContextMapping(ConfigMappingContext ctx) {
            // deliberately do nothing with the context
        }
    }

    /**
     * Package‑private constructor – not accessible from a different package.
     * (The test resides in the same package, but we still assert the expected
     * exception type to illustrate the scenario.)
     */
    static class PackagePrivateConstructorMapping implements ConfigMappingObject {

        PackagePrivateConstructorMapping(ConfigMappingContext ctx) {
        }
    }

    /**
     * Build a minimal configuration that contains the keys required by {@link ServerMapping}.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Register the mapping so that the implementation class is generated
        CONFIG = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).withMapping(ServerMapping.class).build();
        // An empty configuration is sufficient for the error‑path tests.
        CONFIG = new SmallRyeConfigBuilder().build();
        // Successful‑creation test (forces generation again, harmless)
        new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).withMapping(ServerMapping.class).build();
    }

    /**
     * Create a fresh {@link ConfigMappingContext} before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The “roots” map tells the context which root paths belong to which mapping interface.
        // For a simple mapping we just use an empty string as the root path.
        Map<Class<?>, Set<String>> roots = Collections.singletonMap(ServerMapping.class, Collections.singleton(""));
        context = new ConfigMappingContext(CONFIG, roots);
    }

    /**
     * Verify that {@code configMappingObject} returns a correctly constructed instance of the generated
     * mapping implementation and that the values are populated from the underlying {@link SmallRyeConfig}.
     */
    @Test
    void configMappingObjectCreatesImplementation() {
        // Act – invoke the method under test
        ServerMapping mapping = ConfigMappingLoader.configMappingObject(ServerMapping.class, context);
        // Assert – the returned object implements the interface and returns the expected values
        assertNotNull(mapping, "The mapping object must not be null");
        assertEquals("localhost", mapping.host(), "Host value should be read from the config source");
        assertEquals(8080, mapping.port(), "Port value should be read from the config source");
    }

    /**
     * When the target type does not have a constructor that accepts {@link ConfigMappingContext},
     * {@code configMappingObject} is expected to throw {@link NoSuchMethodError}.
     *
     * <p>{@code ServerProperties} is annotated with {@code @ConfigProperties} and therefore is **not**
     * a {@code @ConfigMapping}. Its generated implementation (if any) does not contain the required
     * constructor, which triggers the error path.
     */
    @Test
    void configMappingObjectFailsWhenNoContextConstructor() {
        // Arrange – a class that is not a ConfigMapping
        Map<Class<?>, Set<String>> roots = Collections.singletonMap(ServerProperties.class, Collections.singleton(""));
        ConfigMappingContext wrongContext = new ConfigMappingContext(CONFIG, roots);
        // Act & Assert
        assertThrows(NoSuchMethodError.class, () -> ConfigMappingLoader.configMappingObject(ServerProperties.class, wrongContext), "Expected NoSuchMethodError because ServerProperties does not have the required constructor");
    }
}
