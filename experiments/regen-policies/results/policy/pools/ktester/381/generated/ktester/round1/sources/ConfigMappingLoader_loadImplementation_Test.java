package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_loadImplementation_Test {

    /* -------------------------------------------------------------
     *  Dummy types used for the tests
     * ------------------------------------------------------------- */
    interface DummyInterface {

        String hello();
    }

    static class DummyImplementation implements DummyInterface {

        @Override
        public String hello() {
            return "world";
        }
    }

    interface NonAssignableMappingInterface {
    }

    static class NonAssignableImplementation {
        // does NOT implement NonAssignableMappingInterface
    }

    interface MappingInterfaceWithMetadata {
    }

    interface NonMappingInterface {
    }

    interface ImplementationExistsButNotMapping {
    }

    interface MappingWithImplAndMetadata {
    }

    static class MappingWithImplAndMetadataImpl implements MappingWithImplAndMetadata {
    }

    interface MappingWithFaultyLoadClass {
    }

    interface SecurityExceptionMappingInterface {
    }

    interface LinkageErrorMappingInterface {
    }

    interface NoClassDefFoundErrorMappingInterface {
    }

    interface ImplNameRuntimeExceptionInterface {
    }

    interface ConfigMetadataRuntimeExceptionInterface {
    }

    interface FaultyLoadClassInterface {
    }

    /**
     * When the implementation class can be loaded and is assignable to the
     * supplied type, {@code loadImplementation} must return that class.
     */
    @Test
    void loadImplementation_returnsExistingImplementation() {
        // Mock static helpers from ConfigMappingInterface
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            // The loader will ask for the implementation class name – we give it the real class name
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(DummyInterface.class)).thenReturn(DummyImplementation.class.getName());
            // The mapping metadata lookup is not used in this path, but we return null to be safe
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(DummyInterface.class)).thenReturn(null);
            // Invoke the method under test
            Class<?> result = ConfigMappingLoader.loadImplementation(DummyInterface.class);
            assertEquals(DummyImplementation.class, result, "The returned class should be the existing implementation");
        }
    }

    /**
     * When the implementation class cannot be found, the loader should fall back
     * to {@code loadClass} using the configuration metadata.
     */
    @Test
    void loadImplementation_fallsBackToLoadClassWhenImplementationMissing() {
        // Prepare a dummy metadata object
        ConfigMappingMetadata dummyMetadata = Mockito.mock(ConfigMappingMetadata.class);
        // Mock static helpers from ConfigMappingInterface and ConfigMappingLoader
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            // 1️⃣ getImplementationClassName → a non‑existent class name
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(DummyInterface.class)).thenReturn("non.existent.ClassName");
            // 2️⃣ getConfigurationInterface → returns dummy metadata (non‑null)
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(DummyInterface.class)).thenReturn(dummyMetadata);
            // 3️⃣ loadClass → should be called and return DummyImplementation.class
            loaderMock.when(() -> ConfigMappingLoader.loadClass(DummyInterface.class, dummyMetadata)).thenReturn(DummyImplementation.class);
            // Execute
            Class<?> result = ConfigMappingLoader.loadImplementation(DummyInterface.class);
            // Verify
            assertEquals(DummyImplementation.class, result, "When the implementation class is missing, loadClass should supply the result");
            // Ensure loadClass was indeed invoked
            loaderMock.verify(() -> ConfigMappingLoader.loadClass(DummyInterface.class, dummyMetadata));
        }
    }

    /**
     * When the supplied type is not a configuration mapping (no metadata) the
     * method must throw the exception produced by {@code ConfigMessages.msg.classIsNotAMapping}.
     */
    @Test
    void loadImplementation_throwsWhenNotMapping() {
        // Mock the message bundle to throw an IllegalArgumentException
        ConfigMessages mockMessages = Mockito.mock(ConfigMessages.class);
        IllegalArgumentException expected = new IllegalArgumentException("Not a mapping");
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMessages> msgMock = Mockito.mockStatic(ConfigMessages.class)) {
            // 1️⃣ implementation class name – any value, it will lead to ClassNotFoundException
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(DummyInterface.class)).thenReturn("non.existent.ClassName");
            // 2️⃣ configuration interface lookup → null (not a mapping)
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(DummyInterface.class)).thenReturn(null);
            // 3️⃣ ConfigMessages.msg() returns our mock that throws the exception
            msgMock.when(ConfigMessages::msg).thenReturn(mockMessages);
            Mockito.when(mockMessages.classIsNotAMapping(DummyInterface.class)).thenThrow(expected);
            // Execute and assert the exception
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.loadImplementation(DummyInterface.class));
            assertSame(expected, thrown, "The exception from ConfigMessages should be propagated");
        }
    }

    /**
     * Implementation class is found but not assignable – loader must delegate to {@code loadClass}.
     */
    @Test
    void loadImplementation_implementationFoundButNotAssignable() {
        ConfigMappingMetadata dummyMetadata = Mockito.mock(ConfigMappingMetadata.class);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(NonAssignableMappingInterface.class)).thenReturn(NonAssignableImplementation.class.getName());
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(NonAssignableMappingInterface.class)).thenReturn(dummyMetadata);
            loaderMock.when(() -> ConfigMappingLoader.loadClass(NonAssignableMappingInterface.class, dummyMetadata)).thenReturn(NonAssignableImplementation.class);
            Class<?> result = ConfigMappingLoader.loadImplementation(NonAssignableMappingInterface.class);
            assertEquals(NonAssignableImplementation.class, result, "When not assignable, loadClass should provide the result");
            loaderMock.verify(() -> ConfigMappingLoader.loadClass(NonAssignableMappingInterface.class, dummyMetadata));
        }
    }

    /**
     * Implementation class is found but metadata lookup returns {@code null}
     * while the class is not assignable – an exception from {@code ConfigMessages}
     * must be thrown.
     */
    @Test
    void loadImplementation_implementationFoundNotAssignableAndNoMetadata() {
        ConfigMessages mockMessages = Mockito.mock(ConfigMessages.class);
        IllegalArgumentException expected = new IllegalArgumentException("Not a mapping");
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMessages> msgMock = Mockito.mockStatic(ConfigMessages.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(ImplementationExistsButNotMapping.class)).thenReturn(// any existing class
            DummyImplementation.class.getName());
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ImplementationExistsButNotMapping.class)).thenReturn(null);
            msgMock.when(ConfigMessages::msg).thenReturn(mockMessages);
            Mockito.when(mockMessages.classIsNotAMapping(ImplementationExistsButNotMapping.class)).thenThrow(expected);
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.loadImplementation(ImplementationExistsButNotMapping.class));
            assertSame(expected, thrown);
        }
    }

    /**
     * Null argument – a {@link NullPointerException} is expected because the
     * method tries to call {@code type.getClassLoader()}.
     */
    @Test
    void loadImplementation_nullArgument() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadImplementation(null), "Passing null should result in NPE");
    }

    /**
     * Primitive type argument – {@code int.class} has no class loader, leading
     * to a {@link NullPointerException}.
     */
    @Test
    void loadImplementation_primitiveArgument() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadImplementation(int.class), "Primitive type should cause NPE when class loader is accessed");
    }

    /**
     * Bootstrap class argument (e.g. {@code java.lang.String}) – class loader
     * is {@code null}, therefore a {@link NullPointerException} is thrown.
     */
    @Test
    void loadImplementation_bootstrapClassArgument() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadImplementation(String.class), "Bootstrap class should cause NPE due to null class loader");
    }

    /**
     * Both an assignable implementation class and metadata are present – the
     * implementation class must be returned directly without delegating to {@code loadClass}.
     */
    @Test
    void loadImplementation_assignableImplementationAndMetadataPresent() {
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(MappingWithImplAndMetadata.class)).thenReturn(MappingWithImplAndMetadataImpl.class.getName());
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(MappingWithImplAndMetadata.class)).thenReturn(// metadata present but ignored
            Mockito.mock(ConfigMappingMetadata.class));
            Class<?> result = ConfigMappingLoader.loadImplementation(MappingWithImplAndMetadata.class);
            assertEquals(MappingWithImplAndMetadataImpl.class, result, "Implementation class should be returned even when metadata exists");
        }
    }

    /**
     * When {@code loadClass} throws an unchecked exception (here {@link IllegalStateException}),
     * the exception must propagate unchanged.
     */
    @Test
    void loadImplementation_loadClassThrowsUncheckedException() {
        ConfigMappingMetadata dummyMetadata = Mockito.mock(ConfigMappingMetadata.class);
        IllegalStateException expected = new IllegalStateException("faulty loadClass");
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(MappingWithFaultyLoadClass.class)).thenReturn("non.existent.Class");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(MappingWithFaultyLoadClass.class)).thenReturn(dummyMetadata);
            loaderMock.when(() -> ConfigMappingLoader.loadClass(MappingWithFaultyLoadClass.class, dummyMetadata)).thenThrow(expected);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ConfigMappingLoader.loadImplementation(MappingWithFaultyLoadClass.class));
            assertSame(expected, thrown);
        }
    }

    /**
     * Simulate a {@link SecurityException} thrown by the class loader while loading
     * the implementation class. The exception should propagate.
     */
    @Test
    void loadImplementation_classLoaderThrowsSecurityException() {
        // Mock a Class object whose ClassLoader throws SecurityException on loadClass
        Class<?> mockedType = Mockito.mock(Class.class);
        ClassLoader throwingLoader = Mockito.mock(ClassLoader.class);
        SecurityException expected = new SecurityException("security denied");
        Mockito.when(mockedType.getClassLoader()).thenReturn(throwingLoader);
        Mockito.when(throwingLoader.loadClass("some.Name")).thenThrow(expected);
        // getImplementationClassName returns the name that triggers the exception
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(mockedType)).thenReturn("some.Name");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(mockedType)).thenReturn(// metadata present (won't be used)
            Mockito.mock(ConfigMappingMetadata.class));
            SecurityException thrown = assertThrows(SecurityException.class, () -> ConfigMappingLoader.loadImplementation(mockedType));
            assertSame(expected, thrown);
        }
    }

    /**
     * Simulate a {@link LinkageError} thrown by the class loader.
     */
    @Test
    void loadImplementation_classLoaderThrowsLinkageError() {
        Class<?> mockedType = Mockito.mock(Class.class);
        ClassLoader throwingLoader = Mockito.mock(ClassLoader.class);
        LinkageError expected = new LinkageError("linkage problem");
        Mockito.when(mockedType.getClassLoader()).thenReturn(throwingLoader);
        Mockito.when(throwingLoader.loadClass("some.Name")).thenThrow(expected);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(mockedType)).thenReturn("some.Name");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(mockedType)).thenReturn(Mockito.mock(ConfigMappingMetadata.class));
            LinkageError thrown = assertThrows(LinkageError.class, () -> ConfigMappingLoader.loadImplementation(mockedType));
            assertSame(expected, thrown);
        }
    }

    /**
     * Simulate a {@link NoClassDefFoundError} thrown by the class loader.
     */
    @Test
    void loadImplementation_classLoaderThrowsNoClassDefFoundError() {
        Class<?> mockedType = Mockito.mock(Class.class);
        ClassLoader throwingLoader = Mockito.mock(ClassLoader.class);
        NoClassDefFoundError expected = new NoClassDefFoundError("missing class");
        Mockito.when(mockedType.getClassLoader()).thenReturn(throwingLoader);
        Mockito.when(throwingLoader.loadClass("some.Name")).thenThrow(expected);
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(mockedType)).thenReturn("some.Name");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(mockedType)).thenReturn(Mockito.mock(ConfigMappingMetadata.class));
            NoClassDefFoundError thrown = assertThrows(NoClassDefFoundError.class, () -> ConfigMappingLoader.loadImplementation(mockedType));
            assertSame(expected, thrown);
        }
    }

    /**
     * {@code ConfigMappingInterface.getImplementationClassName} throws a runtime exception.
     * The exception must propagate unchanged.
     */
    @Test
    void loadImplementation_implNameMethodThrowsRuntimeException() {
        RuntimeException expected = new IllegalStateException("bad impl name");
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(ImplNameRuntimeExceptionInterface.class)).thenThrow(expected);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ConfigMappingLoader.loadImplementation(ImplNameRuntimeExceptionInterface.class));
            assertSame(expected, thrown);
        }
    }

    /**
     * {@code ConfigMappingInterface.getConfigurationInterface} throws a runtime exception
     * after a {@link ClassNotFoundException} from the class loader.
     */
    @Test
    void loadImplementation_configMetadataMethodThrowsRuntimeException() {
        RuntimeException expected = new IllegalStateException("metadata failure");
        // Mock a Class whose loader throws ClassNotFoundException
        Class<?> mockedType = Mockito.mock(Class.class);
        ClassLoader loader = Mockito.mock(ClassLoader.class);
        Mockito.when(mockedType.getClassLoader()).thenReturn(loader);
        Mockito.when(loader.loadClass("non.existent.Name")).thenThrow(new ClassNotFoundException("not found"));
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(mockedType)).thenReturn("non.existent.Name");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(mockedType)).thenThrow(expected);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ConfigMappingLoader.loadImplementation(mockedType));
            assertSame(expected, thrown);
        }
    }

    /**
     * {@code ConfigMappingLoader.loadClass} throws a {@link NullPointerException}
     * which must propagate.
     */
    @Test
    void loadImplementation_loadClassThrowsNullPointerException() {
        ConfigMappingMetadata dummyMetadata = Mockito.mock(ConfigMappingMetadata.class);
        NullPointerException expected = new NullPointerException("null in loadClass");
        try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            cfgMock.when(() -> ConfigMappingInterface.getImplementationClassName(FaultyLoadClassInterface.class)).thenReturn("non.existent.Name");
            cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(FaultyLoadClassInterface.class)).thenReturn(dummyMetadata);
            loaderMock.when(() -> ConfigMappingLoader.loadClass(FaultyLoadClassInterface.class, dummyMetadata)).thenThrow(expected);
            NullPointerException thrown = assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadImplementation(FaultyLoadClassInterface.class));
            assertSame(expected, thrown);
        }
    }
}
