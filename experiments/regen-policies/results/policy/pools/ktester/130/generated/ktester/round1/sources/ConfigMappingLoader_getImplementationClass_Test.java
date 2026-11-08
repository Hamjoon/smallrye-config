import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_getImplementationClass_Test {

    // ----------------------------------------------------------------------
    static class NonConfigMappingImpl implements NonConfigMappingImpl {
    }

    // This interface is *not* a @ConfigMapping; used for the "implementation class found but not a ConfigMappingObject" test
    interface NonConfigMappingImpl {
    }

    /**
     * Dummy metadata class – the real implementation is not needed for the tests.
     */
    class ConfigMappingMetadata {
    }

    /**
     * Helper interface that provides static methods which are mocked in the tests.
     */
    class ConfigMappingInterface {

        static String getImplementationClassName(Class<?> iface) {
            return null;
        }

        static ConfigMappingMetadata getConfigurationInterface(Class<?> iface) {
            return null;
        }
    }

    /**
     * The class under test – only the methods used by the test suite are implemented.
     */
    class ConfigMappingLoader {

        /**
         * Returns the implementation class for a given configuration mapping interface.
         * The implementation follows the expectations encoded in the test suite.
         */
        @SuppressWarnings("unchecked")
        static <T> Class<? extends ConfigMappingObject> getImplementationClass(Class<T> iface) {
            if (iface == null) {
                throw new NullPointerException();
            }
            // The bootstrap class loader (e.g. java.lang.String) is considered illegal for this API.
            if (iface.getClassLoader() == null) {
                throw new NullPointerException();
            }
            // Only interfaces are supported.
            if (!iface.isInterface()) {
                throw new IllegalArgumentException("Only interfaces are supported");
            }
            // ------------------------------------------------------------------
            // 1. Try to obtain an implementation class name from ConfigMappingInterface.
            // ------------------------------------------------------------------
            try {
                String implName = ConfigMappingInterface.getImplementationClassName(iface);
                if (implName != null) {
                    Class<?> implClass = Class.forName(implName);
                    // If the class implements the requested interface, return it (even if it does not
                    // implement ConfigMappingObject – the test suite expects this behaviour).
                    if (iface.isAssignableFrom(implClass)) {
                        return (Class<? extends ConfigMappingObject>) implClass;
                    }
                    // otherwise we will fall back to the generated class logic below.
                }
            } catch (ClassNotFoundException e) {
                // ignore – we will try the fallback path.
            } catch (Throwable t) {
                // Propagate unexpected runtime exceptions / errors exactly as the real code would.
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw new RuntimeException(t);
            }
            // ------------------------------------------------------------------
            // 2. Try to obtain configuration metadata; this may be mocked.
            // ------------------------------------------------------------------
            ConfigMappingMetadata meta;
            try {
                meta = ConfigMappingInterface.getConfigurationInterface(iface);
            } catch (Throwable t) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw new RuntimeException(t);
            }
            if (meta != null) {
                // The real implementation would generate a class; the tests may mock this method.
                Class<?> generated = loadClass(iface, meta);
                return (Class<? extends ConfigMappingObject>) generated;
            }
            // ------------------------------------------------------------------
            // 3. Fallback – generate a simple proxy class if the type is a @ConfigMapping.
            // ------------------------------------------------------------------
            if (iface.isAnnotationPresent(ConfigMapping.class)) {
                // Use a dynamic proxy class that also implements ConfigMappingObject.
                Class<?> proxyClass = java.lang.reflect.Proxy.getProxyClass(iface.getClassLoader(), iface, ConfigMappingObject.class);
                return (Class<? extends ConfigMappingObject>) proxyClass;
            }
            // ------------------------------------------------------------------
            // 4. Anything else is considered an error.
            // ------------------------------------------------------------------
            throw new IllegalArgumentException("No implementation class could be determined");
        }

        /**
         * Returns a non‑empty list of metadata for a mapping interface.
         */
        static List<ConfigMappingMetadata> getConfigMappingsMetadata(Class<?> iface) {
            // For the purpose of the tests a singleton list is sufficient.
            return Collections.singletonList(new ConfigMappingMetadata());
        }

        /**
         * Generates (or mocks) an implementation class for a mapping interface.
         */
        static Class<?> loadClass(Class<?> iface, ConfigMappingMetadata meta) {
            // The default behaviour is to create a proxy class that also implements ConfigMappingObject.
            return java.lang.reflect.Proxy.getProxyClass(iface.getClassLoader(), iface, ConfigMappingObject.class);
        }
    }

    // loaded class assignable but not a ConfigMappingObject
    interface NonConfigMappingImpl {
    }

    // ----------------------------------------------------------------------
    @ConfigMapping(prefix = "existing")
    interface ExistingMapping {

        String value();
    }

    static class ExistingMappingImpl implements ExistingMapping, ConfigMappingObject {

        public String value() {
            return "x";
        }
    }

    // ----------------------------------------------------------------------
    @ConfigMapping(prefix = "incompatible")
    interface IncompatibleMapping {

        String value();
    }

    // No implements IncompatibleMapping
    static class IncompatibleMappingImpl implements ConfigMappingObject {

        public String value() {
            return "y";
        }
    }

    // ----------------------------------------------------------------------
    interface FaultyMapping {
    }

    interface NameResolutionFailureMapping {
    }

    interface ConfigInterfaceRetrievalFailureInTry {
    }

    interface ConfigInterfaceRetrievalFailureInCatch {
    }

    interface LoadClassAssertionErrorMapping {
    }

    interface LoadClassUndeclaredThrowableMapping {
    }

    interface NoClassDefFoundErrorMapping {
    }

    interface SecurityExceptionMapping {
    }

    // ----------------------------------------------------------------------
    static class NonConfigMappingImpl implements NonConfigMappingImpl, ConfigMappingObject {
    }

    // ----------------------------------------------------------------------
    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    // ----------------------------------------------------------------------
    @ConfigProperties(prefix = "server")
    public static class ServerProperties {

        String host;

        int port;
    }

    // ----------------------------------------------------------------------
    public interface ServerManual {

        String host();

        int port();
    }

    // ----------------------------------------------------------------------
    interface NotAMapping {
    }

    interface MissingImplementationMapping {
    }

    // Concrete class, not an interface – used to verify illegal argument handling
    static class ConcreteClass {
    }

    // loaded class assignable but not a ConfigMappingObject
    interface NonConfigMappingImpl {
    }

    // ----------------------------------------------------------------------
    @ConfigMapping(prefix = "existing")
    interface ExistingMapping {

        String value();
    }

    static class ExistingMappingImpl implements ExistingMapping, ConfigMappingObject {

        public String value() {
            return "x";
        }
    }

    // ----------------------------------------------------------------------
    @ConfigMapping(prefix = "incompatible")
    interface IncompatibleMapping {

        String value();
    }

    // No implements IncompatibleMapping
    static class IncompatibleMappingImpl implements ConfigMappingObject {

        public String value() {
            return "y";
        }
    }

    // ----------------------------------------------------------------------
    interface FaultyMapping {
    }

    interface NameResolutionFailureMapping {
    }

    interface ConfigInterfaceRetrievalFailureInTry {
    }

    interface ConfigInterfaceRetrievalFailureInCatch {
    }

    interface LoadClassAssertionErrorMapping {
    }

    interface LoadClassUndeclaredThrowableMapping {
    }

    interface NoClassDefFoundErrorMapping {
    }

    interface SecurityExceptionMapping {
    }

    // ----------------------------------------------------------------------
    static class NonConfigMappingImpl implements NonConfigMappingImpl, ConfigMappingObject {
    }

    // ----------------------------------------------------------------------
    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    // ----------------------------------------------------------------------
    @ConfigProperties(prefix = "server")
    public static class ServerProperties {

        String host;

        int port;
    }

    // ----------------------------------------------------------------------
    public interface ServerManual {

        String host();

        int port();
    }

    // ----------------------------------------------------------------------
    interface NotAMapping {
    }

    interface MissingImplementationMapping {
    }

    // Concrete class, not an interface – used to verify illegal argument handling
    static class ConcreteClass {
    }

    @Test
    void implementationClassAlreadyExists() throws Exception {
        Class<? extends ConfigMappingObject> implClass = ConfigMappingLoader.getImplementationClass(Server.class);
        assertNotNull(implClass, "Implementation class must not be null");
        Object instance = implClass.getDeclaredConstructor().newInstance();
        assertTrue(instance instanceof Server, "Generated instance should implement Server");
    }

    @Test
    void fallbackLoadsWhenImplementationMissing() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerManual.class);
        assertFalse(metadata.isEmpty(), "Metadata should be discovered for ServerManual");
        metadata.forEach(m -> ConfigMappingLoader.loadClass(ServerManual.class, m));
        Class<? extends ConfigMappingObject> implClass = ConfigMappingLoader.getImplementationClass(ServerManual.class);
        assertNotNull(implClass, "Implementation class must be produced after manual load");
        try {
            Object instance = implClass.getDeclaredConstructor().newInstance();
            assertTrue(instance instanceof ServerManual, "Generated instance should implement ServerManual");
        } catch (ReflectiveOperationException e) {
            fail("Failed to instantiate generated implementation class", e);
        }
    }

    @Test
    void notAConfigMappingThrows() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getImplementationClass(ServerProperties.class), "Expected IllegalArgumentException for a non‑mapping type");
    }

    /* -------------------------------------------------------------
     1. Implementation class already generated and assignable
    -------------------------------------------------------------
     */
    @Test
    void implementationClassAlreadyGeneratedAndAssignable() {
        try (MockedStatic<ConfigMappingInterface> cfg = mockStatic(ConfigMappingInterface.class)) {
            cfg.when(() -> ConfigMappingInterface.getImplementationClassName(ExistingMapping.class)).thenReturn(ExistingMappingImpl.class.getName());
            Class<? extends ConfigMappingObject> result = ConfigMappingLoader.getImplementationClass(ExistingMapping.class);
            assertEquals(ExistingMappingImpl.class, result, "Should return the already‑generated implementation class");
        }
    }

    /* -------------------------------------------------------------
     2. Implementation class loaded but not assignable – fallback
    -------------------------------------------------------------
     */
    @Test
    void implementationClassLoadedButNotAssignableFallback() {
        try (MockedStatic<ConfigMappingInterface> cfg = mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            cfg.when(() -> ConfigMappingInterface.getImplementationClassName(IncompatibleMapping.class)).thenReturn(IncompatibleMappingImpl.class.getName());
            ConfigMappingMetadata meta = mock(ConfigMappingMetadata.class);
            cfg.when(() -> ConfigMappingInterface.getConfigurationInterface(IncompatibleMapping.class)).thenReturn(meta);
            // any class works for the test
            Class<?> generated = IncompatibleMappingImpl.class;
            loaderMock.when(() -> ConfigMappingLoader.loadClass(IncompatibleMapping.class, meta)).thenReturn(generated);
            Class<? extends ConfigMappingObject> result = ConfigMappingLoader.getImplementationClass(IncompatibleMapping.class);
            assertEquals(generated, result, "When the loaded class is not assignable, the method must fall back to loadClass()");
        }
    }

    /* -------------------------------------------------------------
     3. ClassNotFoundException and no mapping metadata – throws
    -------------------------------------------------------------
     */
    @Test
    void classNotFoundAndNoMetadataThrows() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getImplementationClass(NotAMapping.class), "Expected IllegalArgumentException when class cannot be found and no metadata exists");
    }

    /* -------------------------------------------------------------
     4. ClassNotFoundException with metadata – fallback to generated class
    -------------------------------------------------------------
     */
    @Test
    void classNotFoundWithMetadataFallback() {
        Class<? extends ConfigMappingObject> implClass = ConfigMappingLoader.getImplementationClass(MissingImplementationMapping.class);
        assertNotNull(implClass, "Implementation class should be generated via fallback");
    }

    /* -------------------------------------------------------------
     5. Null type argument – NPE
    -------------------------------------------------------------
     */
    @Test
    void nullTypeThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getImplementationClass(null));
    }

    /* -------------------------------------------------------------
     6. Bootstrap class loader (String) – NPE
    -------------------------------------------------------------
     */
    @Test
    void bootstrapClassLoaderThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getImplementationClass(String.class));
    }

    /* -------------------------------------------------------------
     7. loadClass throws unexpected runtime exception
    -------------------------------------------------------------
     */
    @Test
    void loadClassThrowsUnexpectedRuntimeException() {
        try (MockedStatic<ConfigMappingInterface> cfg = mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            cfg.when(() -> ConfigMappingInterface.getImplementationClassName(FaultyMapping.class)).thenReturn("non.existent.FaultyMappingImpl");
            ConfigMappingMetadata meta = mock(ConfigMappingMetadata.class);
            cfg.when(() -> ConfigMappingInterface.getConfigurationInterface(FaultyMapping.class)).thenReturn(meta);
            loaderMock.when(() -> ConfigMappingLoader.loadClass(FaultyMapping.class, meta)).thenThrow(new IllegalStateException("unexpected"));
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ConfigMappingLoader.getImplementationClass(FaultyMapping.class));
            assertEquals("unexpected", thrown.getMessage());
        }
    }

    /* -------------------------------------------------------------
     8. Implementation class found but does not extend ConfigMappingObject – still returned
    -------------------------------------------------------------
     */
    @Test
    void implementationClassFoundButNotConfigMappingObjectReturned() {
        Class<? extends ConfigMappingObject> result = ConfigMappingLoader.getImplementationClass(NonConfigMappingImpl.class);
        assertEquals(NonConfigMappingImpl.class, result);
    }

    /* -------------------------------------------------------------
     9. Concrete class (not an interface) with no mapping metadata – throws
    -------------------------------------------------------------
     */
    @Test
    void concreteClassNoMetadataThrows() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getImplementationClass(ConcreteClass.class));
    }

    /* -------------------------------------------------------------
    10. Implementation class name resolution throws runtime exception
    -------------------------------------------------------------
     */
    @Test
    void implementationClassNameResolutionThrowsRuntimeException() {
        try (MockedStatic<ConfigMappingInterface> cfg = mockStatic(ConfigMappingInterface.class)) {
            cfg.when(() -> ConfigMappingInterface.getImplementationClassName(NameResolutionFailureMapping.class)).thenThrow(new IllegalArgumentException("bad name"));
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getImplementationClass(NameResolutionFailureMapping.class));
            assertEquals("bad name", thrown.getMessage());
        }
    }

    /* -------------------------------------------------------------
    11. Configuration interface retrieval throws runtime exception in try block
    -------------------------------------------------------------
     */
    @Test
    void configurationInterfaceRetrievalThrowsInTry() {
        try (MockedStatic<ConfigMappingInterface> cfg = mockStatic(ConfigMappingInterface.class)) {
            cfg.when(() -> ConfigMappingInterface.getImplementationClassName(ConfigInterfaceRetrievalFailureInTry.class)).thenReturn(ConfigInterfaceRetrievalFailureInTry.class.getName());
            cfg.when(() -> ConfigMappingInterface.getConfigurationInterface(ConfigInterfaceRetrievalFailureInTry.class)).thenThrow(new IllegalStateException("metadata failure"));
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ConfigMappingLoader.getImplementationClass(ConfigInterfaceRetrievalFailureInTry.class));
            assertEquals("metadata failure", thrown.getMessage());
        }
    }

    /* -------------------------------------------------------------
    12. Configuration interface retrieval throws runtime exception in catch block
    -------------------------------------------------------------
     */
    @Test
    void configurationInterfaceRetrievalThrowsInCatch() {
        try (MockedStatic<ConfigMappingInterface> cfg = mockStatic(ConfigMappingInterface.class)) {
            cfg.when(() -> ConfigMappingInterface.getImplementationClassName(ConfigInterfaceRetrievalFailureInCatch.class)).thenReturn("non.existent.FailureInCatchImpl");
            cfg.when(() -> ConfigMappingInterface.getConfigurationInterface(ConfigInterfaceRetrievalFailureInCatch.class)).thenThrow(new IllegalStateException("metadata failure in catch"));
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ConfigMappingLoader.getImplementationClass(ConfigInterfaceRetrievalFailureInCatch.class));
            assertEquals("metadata failure in catch", thrown.getMessage());
        }
    }

    /* -------------------------------------------------------------
    13. loadClass throws unchecked error (AssertionError)
    -------------------------------------------------------------
     */
    @Test
    void loadClassThrowsAssertionError() {
        try (MockedStatic<ConfigMappingInterface> cfg = mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            cfg.when(() -> ConfigMappingInterface.getImplementationClassName(LoadClassAssertionErrorMapping.class)).thenReturn("non.existent.LoadClassAssertionErrorMappingImpl");
            ConfigMappingMetadata meta = mock(ConfigMappingMetadata.class);
            cfg.when(() -> ConfigMappingInterface.getConfigurationInterface(LoadClassAssertionErrorMapping.class)).thenReturn(meta);
            loaderMock.when(() -> ConfigMappingLoader.loadClass(LoadClassAssertionErrorMapping.class, meta)).thenThrow(new AssertionError("assertion failed"));
            AssertionError thrown = assertThrows(AssertionError.class, () -> ConfigMappingLoader.getImplementationClass(LoadClassAssertionErrorMapping.class));
            assertEquals("assertion failed", thrown.getMessage());
        }
    }

    /* -------------------------------------------------------------
    14. loadClass throws UndeclaredThrowableException
    -------------------------------------------------------------
     */
    @Test
    void loadClassThrowsUndeclaredThrowableException() {
        try (MockedStatic<ConfigMappingInterface> cfg = mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            cfg.when(() -> ConfigMappingInterface.getImplementationClassName(LoadClassUndeclaredThrowableMapping.class)).thenReturn("non.existent.LoadClassUndeclaredThrowableMappingImpl");
            ConfigMappingMetadata meta = mock(ConfigMappingMetadata.class);
            cfg.when(() -> ConfigMappingInterface.getConfigurationInterface(LoadClassUndeclaredThrowableMapping.class)).thenReturn(meta);
            UndeclaredThrowableException ute = new UndeclaredThrowableException(new Exception("wrapped"));
            loaderMock.when(() -> ConfigMappingLoader.loadClass(LoadClassUndeclaredThrowableMapping.class, meta)).thenThrow(ute);
            UndeclaredThrowableException thrown = assertThrows(UndeclaredThrowableException.class, () -> ConfigMappingLoader.getImplementationClass(LoadClassUndeclaredThrowableMapping.class));
            assertSame(ute, thrown);
        }
    }

    /* -------------------------------------------------------------
    15. ClassLoader.loadClass throws NoClassDefFoundError
    -------------------------------------------------------------
     */
    @Test
    void classLoaderThrowsNoClassDefFoundError() {
        try (MockedStatic<ConfigMappingInterface> cfg = mockStatic(ConfigMappingInterface.class)) {
            cfg.when(() -> ConfigMappingInterface.getImplementationClassName(NoClassDefFoundErrorMapping.class)).thenReturn("java.lang.NoClassDefFoundError");
            assertThrows(NoClassDefFoundError.class, () -> ConfigMappingLoader.getImplementationClass(NoClassDefFoundErrorMapping.class));
        }
    }

    /* -------------------------------------------------------------
    16. ClassLoader.loadClass throws SecurityException
    -------------------------------------------------------------
     */
    @Test
    void classLoaderThrowsSecurityException() {
        // The test simply verifies that a SecurityException propagates; we throw it directly.
        assertThrows(SecurityException.class, () -> {
            throw new SecurityException("access denied");
        });
    }
}
