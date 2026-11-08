package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.getImplementationClass;
import static org.junit.jupiter.api.Assertions.*;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_getImplementationClass_Test {

    static class DummyImpl implements ConfigMappingObject, ConfigInterface {
    }

    static class WrongImpl implements ConfigMappingObject {
    }

    static class GeneratedImpl implements ConfigMappingObject {
    }

    static class PlainImpl implements ConfigMappingObject {
    }

    static class InterfaceWithoutConfigMappingObjectImpl implements InterfaceWithoutConfigMappingObject {
    }

    // Marker interfaces used only for type identification in the tests
    interface ConfigInterface {
    }

    interface InterfaceWithoutConfigMappingObject {
    }

    // the class loader can resolve a name if needed.
    static class ConfigInterfaceImpl implements ConfigInterface, ConfigMappingObject {
    }

    static class WrongImplConfigImpl {
    }

    static class NotAMappingImpl {
    }

    static class MissingImplConfigImpl {
    }

    static class UnknownInterfaceImpl {
    }

    static class FailingLoadClassConfigImpl {
    }

    static class SecurityRestrictedConfigImpl {
    }

    static class BadImplNameConfigImpl {
    }

    static class MetadataExceptionConfigImpl {
    }

    static class MetadataExceptionOnMissingImplConfigImpl {
    }

    static class NoClassDefErrorConfigImpl {
    }

    static class InitializerErrorConfigImpl {
    }

    static class RuntimeExceptionLoaderConfigImpl {
    }

    // ----------------------------------------------------------------------
    interface ConfigMappingObject {
    }

    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    @ConfigProperties(prefix = "server")
    public static class ServerProperties {

        String host;

        int port;
    }

    // Interface without a generated implementation – used to trigger generation.
    interface ServerManual {

        String host();

        int port();
    }

    /**
     * Returns a mock {@link ClassLoader} that behaves according to the supplied
     * {@code loadBehaviour}.  The behaviour can be a {@link Class} to return,
     * a {@link ClassNotFoundException}, a {@link RuntimeException},
     * {@link SecurityException}, {@link NoClassDefFoundError},
     * {@link ExceptionInInitializerError}, etc.
     */
    private static ClassLoader mockClassLoader(Object loadBehaviour) throws Exception {
        ClassLoader loader = Mockito.mock(ClassLoader.class);
        if (loadBehaviour instanceof Class) {
            Mockito.when(loader.loadClass(Mockito.anyString())).thenReturn((Class<?>) loadBehaviour);
        } else if (loadBehaviour instanceof Exception) {
            Exception ex = (Exception) loadBehaviour;
            Mockito.when(loader.loadClass(Mockito.anyString())).thenThrow(ex);
        } else if (loadBehaviour instanceof RuntimeException) {
            RuntimeException ex = (RuntimeException) loadBehaviour;
            Mockito.when(loader.loadClass(Mockito.anyString())).thenThrow(ex);
        } else if (loadBehaviour instanceof Error) {
            Error err = (Error) loadBehaviour;
            Mockito.when(loader.loadClass(Mockito.anyString())).thenThrow(err);
        } else {
            // default – return a dummy class
            Mockito.when(loader.loadClass(Mockito.anyString())).thenReturn(Object.class);
        }
        return loader;
    }

    /**
     * Verify that a mapping interface without a generated implementation class
     * is loaded dynamically by {@code loadClass}.
     */
    @Test
    void loadImplementationWhenClassNotPresent() {
        // The {@code ServerManual} interface does not have a compiled implementation.
        // The first call will trigger the ClassNotFoundException path and cause
        // {@code getImplementationClass} to generate the implementation.
        Class<? extends ConfigMappingObject> implClass = getImplementationClass(ServerManual.class);
        assertNotNull(implClass, "Implementation class should be generated");
        assertTrue(ServerManual.class.isAssignableFrom(implClass), "Generated class must implement the mapping interface");
        // A second call should return the same class (cached) without re‑generating it.
        Class<? extends ConfigMappingObject> secondCall = getImplementationClass(ServerManual.class);
        assertSame(implClass, secondCall, "Subsequent calls must return the cached class");
    }

    /**
     * Ensure that a non‑mapping type (e.g., a regular POJO annotated with {@code @ConfigProperties})
     * results in an {@code IllegalArgumentException}.
     */
    @Test
    void rejectNonMappingType() {
        assertThrows(IllegalArgumentException.class, () -> getImplementationClass(ServerProperties.class), "Non‑mapping types should cause an IllegalArgumentException");
    }

    /**
     * Verify that a mapping interface with a generated no‑args constructor can be instantiated.
     */
    @Test
    void instantiateGeneratedImplementation() throws Exception {
        Class<? extends ConfigMappingObject> impl = getImplementationClass(Server.class);
        Object instance = impl.getDeclaredConstructor().newInstance();
        assertTrue(instance instanceof Server, "Instance should be assignable to the mapping interface");
    }

    /**
     * Creates a mock {@link Class} representing the supplied {@code typeName}.
     * The mock can be configured with a custom {@link ClassLoader} and
     * {@code isAssignableFrom} behaviour.
     */
    @SuppressWarnings("unchecked")
    private static <T> Class<T> mockType(String typeName, ClassLoader loader, java.util.function.Predicate<Class<?>> assignablePredicate) {
        Class<T> type = (Class<T>) Mockito.mock(Class.class, Mockito.withSettings().name(typeName).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        Mockito.when(type.getName()).thenReturn(typeName);
        Mockito.when(type.getClassLoader()).thenReturn(loader);
        Mockito.when(type.isAssignableFrom(Mockito.any())).thenAnswer(invocation -> {
            Class<?> arg = invocation.getArgument(0);
            return assignablePredicate.test(arg);
        });
        return type;
    }

    /**
     * 1. Successful implementation class loading (type is assignable).
     */
    @Test
    void successfulImplementationClassLoading_assignable() throws Exception {
        // Mock implementation class that will be returned by the class loader
        Class<?> implClass = DummyImpl.class;
        // Mock class loader to return the implementation class
        ClassLoader loader = mockClassLoader(implClass);
        // Mock the interface type – assignableFrom returns true
        Class<?> type = mockType("com.example.ConfigInterface", loader, c -> c == implClass);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMessages> msgMock = Mockito.mockStatic(ConfigMessages.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.ConfigInterfaceImpl");
            // No metadata needed – method should return before calling getConfigurationInterface
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenReturn(null);
            // Execute
            Class<? extends ConfigMappingObject> result = getImplementationClass(type);
            // Verify
            assertSame(implClass, result, "Should return the class loaded by the class loader");
        }
    }

    /**
     * 2. Implementation class loaded but not assignable – metadata present – loadClass used.
     */
    @Test
    void implementationClassNotAssignable_metadataPresent_loadClassUsed() throws Exception {
        // class loader returns a wrong impl
        Class<?> implClass = WrongImpl.class;
        // result of loadClass
        Class<?> generatedClass = GeneratedImpl.class;
        ClassLoader loader = mockClassLoader(implClass);
        Class<?> type = mockType("com.example.WrongImplConfig", loader, c -> false);
        ConfigMappingMetadata metadata = Mockito.mock(ConfigMappingMetadata.class);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMessages> msgMock = Mockito.mockStatic(ConfigMessages.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.WrongImplConfigImpl");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenReturn(metadata);
            loaderMock.when(() -> ConfigMappingLoader.loadClass(type, metadata)).thenReturn(generatedClass);
            Class<? extends ConfigMappingObject> result = getImplementationClass(type);
            assertSame(generatedClass, result, "Should fall back to loadClass when not assignable");
        }
    }

    /**
     * 3. Implementation class loaded but not assignable – metadata missing – exception thrown.
     */
    @Test
    void implementationClassNotAssignable_noMetadata_throwsException() throws Exception {
        Class<?> implClass = WrongImpl.class;
        ClassLoader loader = mockClassLoader(implClass);
        Class<?> type = mockType("com.example.NotAMapping", loader, c -> false);
        IllegalArgumentException iae = new IllegalArgumentException("not a mapping");
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMessages> msgMock = Mockito.mockStatic(ConfigMessages.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.NotAMappingImpl");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenReturn(null);
            // ConfigMessages.msg.classIsNotAMapping should produce the IAE
            msgMock.when(() -> ConfigMessages.msg.classIsNotAMapping(type)).thenReturn(iae);
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> getImplementationClass(type));
            assertSame(iae, thrown, "Should throw the exception supplied by ConfigMessages");
        }
    }

    /**
     * 4. ClassNotFoundException path – metadata present – loadClass used.
     */
    @Test
    void classNotFound_metadataPresent_loadClassUsed() throws Exception {
        ClassNotFoundException cnfe = new ClassNotFoundException();
        Class<?> generatedClass = GeneratedImpl.class;
        ConfigMappingMetadata metadata = Mockito.mock(ConfigMappingMetadata.class);
        ClassLoader loader = mockClassLoader(cnfe);
        Class<?> type = mockType("com.example.MissingImplConfig", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMessages> msgMock = Mockito.mockStatic(ConfigMessages.class);
            MockedStatic<ConfigMappingLoader> loadMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.MissingImplConfigImpl");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenReturn(metadata);
            loadMock.when(() -> ConfigMappingLoader.loadClass(type, metadata)).thenReturn(generatedClass);
            Class<? extends ConfigMappingObject> result = getImplementationClass(type);
            assertSame(generatedClass, result, "Should return class generated by loadClass");
        }
    }

    /**
     * 5. ClassNotFoundException path – metadata missing – exception thrown.
     */
    @Test
    void classNotFound_noMetadata_throwsException() throws Exception {
        ClassNotFoundException cnfe = new ClassNotFoundException();
        IllegalArgumentException iae = new IllegalArgumentException("not a mapping");
        ClassLoader loader = mockClassLoader(cnfe);
        Class<?> type = mockType("com.example.UnknownInterface", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMessages> msgMock = Mockito.mockStatic(ConfigMessages.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.UnknownInterfaceImpl");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenReturn(null);
            msgMock.when(() -> ConfigMessages.msg.classIsNotAMapping(type)).thenReturn(iae);
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> getImplementationClass(type));
            assertSame(iae, thrown);
        }
    }

    /**
     * 6. Null type argument – NullPointerException.
     */
    @Test
    void nullType_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> getImplementationClass(null), "Passing null should result in NullPointerException");
    }

    /**
     * 7. Primitive type argument – NullPointerException (class loader is null).
     */
    @Test
    void primitiveType_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> getImplementationClass(int.class), "Primitive types have no ClassLoader – should throw NPE");
    }

    /**
     * 8. Implementation class assignable but does NOT extend ConfigMappingObject – returned unchanged.
     */
    @Test
    void assignableButNotConfigMappingObject_returned() throws Exception {
        // The implementation class does not implement ConfigMappingObject
        Class<?> implClass = PlainImpl.class;
        ClassLoader loader = mockClassLoader(implClass);
        Class<?> type = mockType("com.example.InterfaceWithoutConfigMappingObject", loader, c -> c == implClass);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMessages> msgMock = Mockito.mockStatic(ConfigMessages.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.InterfaceWithoutConfigMappingObjectImpl");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenReturn(null);
            Class<? extends ConfigMappingObject> result = getImplementationClass(type);
            // The method returns the class even though it does not implement ConfigMappingObject
            assertSame(implClass, result);
        }
    }

    /**
     * 9. loadClass throws unchecked RuntimeException – exception propagates.
     */
    @Test
    void loadClassThrowsRuntimeException_propagates() throws Exception {
        ClassNotFoundException cnfe = new ClassNotFoundException();
        RuntimeException rte = new RuntimeException("unexpected error");
        ConfigMappingMetadata metadata = Mockito.mock(ConfigMappingMetadata.class);
        ClassLoader loader = mockClassLoader(cnfe);
        Class<?> type = mockType("com.example.FailingLoadClassConfig", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader> loadMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.FailingLoadClassConfigImpl");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenReturn(metadata);
            loadMock.when(() -> ConfigMappingLoader.loadClass(type, metadata)).thenThrow(rte);
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> getImplementationClass(type));
            assertSame(rte, thrown);
        }
    }

    /**
     * 10. ClassLoader throws SecurityException – exception propagates.
     */
    @Test
    void classLoaderThrowsSecurityException_propagates() throws Exception {
        SecurityException secEx = new SecurityException("access denied");
        ClassLoader loader = mockClassLoader(secEx);
        Class<?> type = mockType("com.example.SecurityRestrictedConfig", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.SecurityRestrictedConfigImpl");
            // The method never reaches getConfigurationInterface because the loader throws
            SecurityException thrown = assertThrows(SecurityException.class, () -> getImplementationClass(type));
            assertSame(secEx, thrown);
        }
    }

    /**
     * 11. Implementation class name resolution throws unchecked exception – exception propagates.
     */
    @Test
    void implNameResolutionThrowsUnchecked_exceptionPropagates() {
        Class<?> type = mockType("com.example.BadImplNameConfig", null, c -> false);
        IllegalStateException ise = new IllegalStateException("failed to compute impl name");
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenThrow(ise);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> getImplementationClass(type));
            assertSame(ise, thrown);
        }
    }

    /**
     * 12. ConfigMappingInterface.getConfigurationInterface throws unchecked exception – non‑assignable path.
     */
    @Test
    void getConfigurationInterfaceThrowsUnchecked_nonAssignablePath() throws Exception {
        Class<?> implClass = WrongImpl.class;
        ClassLoader loader = mockClassLoader(implClass);
        Class<?> type = mockType("com.example.MetadataExceptionConfig", loader, c -> false);
        IllegalStateException ise = new IllegalStateException("metadata error");
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.MetadataExceptionConfigImpl");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenThrow(ise);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> getImplementationClass(type));
            assertSame(ise, thrown);
        }
    }

    /**
     * 13. ConfigMappingInterface.getConfigurationInterface throws unchecked exception – ClassNotFoundException path.
     */
    @Test
    void getConfigurationInterfaceThrowsUnchecked_classNotFoundPath() throws Exception {
        ClassNotFoundException cnfe = new ClassNotFoundException();
        IllegalStateException ise = new IllegalStateException("metadata error");
        ClassLoader loader = mockClassLoader(cnfe);
        Class<?> type = mockType("com.example.MetadataExceptionOnMissingImplConfig", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.MetadataExceptionOnMissingImplConfigImpl");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenThrow(ise);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> getImplementationClass(type));
            assertSame(ise, thrown);
        }
    }

    /**
     * 14. ClassLoader.loadClass throws NoClassDefFoundError – exception propagates.
     */
    @Test
    void classLoaderThrowsNoClassDefFoundError_propagates() throws Exception {
        NoClassDefFoundError error = new NoClassDefFoundError("Missing dependency");
        ClassLoader loader = mockClassLoader(error);
        Class<?> type = mockType("com.example.NoClassDefErrorConfig", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.NoClassDefErrorConfigImpl");
            NoClassDefFoundError thrown = assertThrows(NoClassDefFoundError.class, () -> getImplementationClass(type));
            assertSame(error, thrown);
        }
    }

    /**
     * 15. ClassLoader.loadClass throws ExceptionInInitializerError – exception propagates.
     */
    @Test
    void classLoaderThrowsExceptionInInitializerError_propagates() throws Exception {
        ExceptionInInitializerError err = new ExceptionInInitializerError("init failure");
        ClassLoader loader = mockClassLoader(err);
        Class<?> type = mockType("com.example.InitializerErrorConfig", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.InitializerErrorConfigImpl");
            ExceptionInInitializerError thrown = assertThrows(ExceptionInInitializerError.class, () -> getImplementationClass(type));
            assertSame(err, thrown);
        }
    }

    /**
     * 16. ClassLoader.loadClass throws RuntimeException – exception propagates.
     */
    @Test
    void classLoaderThrowsRuntimeException_propagates() throws Exception {
        RuntimeException rte = new RuntimeException("loader failure");
        ClassLoader loader = mockClassLoader(rte);
        Class<?> type = mockType("com.example.RuntimeExceptionLoaderConfig", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.RuntimeExceptionLoaderConfigImpl");
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> getImplementationClass(type));
            assertSame(rte, thrown);
        }
    }

    /**
     * 15. ClassLoader.loadClass throws ExceptionInInitializerError – exception propagates.
     */
    @Test
    void classLoaderThrowsExceptionInInitializerError_propagate() throws Exception {
        ExceptionInInitializerError err = new ExceptionInInitializerError("init failure");
        ClassLoader loader = mockClassLoader(err);
        Class<?> type = mockType("com.example.InitializerErrorConfig", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.InitializerErrorConfigImpl");
            ExceptionInInitializerError thrown = assertThrows(ExceptionInInitializerError.class, () -> getImplementationClass(type));
            assertSame(err, thrown);
        }
    }

    /**
     * 16. ClassLoader.loadClass throws RuntimeException – exception propagates.
     */
    @Test
    void classLoaderThrowsRuntimeException_propagate() throws Exception {
        RuntimeException rte = new RuntimeException("loader failure");
        ClassLoader loader = mockClassLoader(rte);
        Class<?> type = mockType("com.example.RuntimeExceptionLoaderConfig", loader, c -> false);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(type)).thenReturn("com.example.RuntimeExceptionLoaderConfigImpl");
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> getImplementationClass(type));
            assertSame(rte, thrown);
        }
    }
}
