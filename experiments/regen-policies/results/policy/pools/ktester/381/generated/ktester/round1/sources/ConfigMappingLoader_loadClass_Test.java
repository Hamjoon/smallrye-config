package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.loadClass;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_loadClass_Test {

    @Mock
    ConfigMappingMetadata // mock supplied by MockitoExtension
    mockMetadata;

    /**
     * Simple parent interface used for the tests.
     */
    interface DummyParent {

        String greet();
    }

    /**
     * Concrete implementation that will be loaded by the class‑loader.
     */
    static class DummyParentImpl implements DummyParent {

        @Override
        public String greet() {
            return "hello";
        }
    }

    class Unrelated {
    }

    /**
     * Simple parent interface used for the tests.
     */
    interface ParentInterface {

        String hello();
    }

    /**
     * A class that implements {@link ParentInterface} and is already loadable.
     */
    static class ParentImpl implements ParentInterface {

        public String hello() {
            return "hi";
        }
    }

    /**
     * A class that does NOT implement {@link ParentInterface}.
     */
    static class UnrelatedClass {
    }

    /**
     * Custom class loader that throws {@link SecurityException} on any loadClass call.
     */
    static class ThrowingClassLoader extends ClassLoader {

        ThrowingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new SecurityException("Forbidden");
        }
    }

    /**
     * Interface that will be loaded by the throwing class loader.
     */
    interface RestrictedInterface {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to avoid cross‑test interference.
        reset(mockMetadata);
        reset(mockMetadata);
    }

    /**
     * Helper that creates a simple subclass of the given interface using the supplied class loader.
     * The generated class does not need any byte‑code generation – we simply define a dynamic
     * subclass via reflection (a lambda proxy works because it implements the interface).
     */
    private static Class<?> defineProxyClass(ClassLoader loader, Class<?> iface) {
        // Use a lambda to create an instance that implements the interface, then retrieve its class.
        Object proxyInstance = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[] { iface }, (proxy, method, args) -> null);
        return proxyInstance.getClass();
    }

    /**
     * When the implementation class is already loadable and is assignable to the parent interface,
     * {@code loadClass} must return that class without attempting to define a new one.
     */
    @Test
    void loadClass_returnsAlreadyLoadedClass() {
        // Arrange – the metadata points to the already compiled implementation.
        when(mockMetadata.getClassName()).thenReturn(DummyParentImpl.class.getName());
        // The byte‑code is irrelevant because the class is found before it would be needed.
        when(mockMetadata.getClassBytes()).thenReturn(new byte[0]);
        // Act
        Class<?> result = loadClass(DummyParent.class, mockMetadata);
        // Assert
        assertNotNull(result, "Result should never be null");
        assertEquals(DummyParentImpl.class, result, "Should return the already loaded implementation class");
        // Verify that we never tried to fetch class bytes (the fallback path) – this is an
        // indirect way to ensure the lock‑free path was taken.
        verify(mockMetadata, never()).getClassBytes();
    }

    /**
     * When the target class cannot be found, {@code loadClass} must fall back to the private
     * {@code defineClass} method. Because {@code defineClass} is private we cannot observe its
     * direct result, but we can confirm that the fallback is executed by providing a class name
     * that does not exist and a {@code null} byte array – the latter will cause a
     * {@link NullPointerException} inside {@code defineClass}. The exact exception is not part
     * of the public contract; we only care that the method does not return the parent class.
     */
    @Test
    void loadClass_fallbackToDefineClassWhenClassNotFound() {
        // Arrange – a class name that is guaranteed not to exist.
        String nonExistentName = "io.smallrye.config.DoesNotExist";
        when(mockMetadata.getClassName()).thenReturn(nonExistentName);
        // Return null bytes to force a failure inside defineClass.
        when(mockMetadata.getClassBytes()).thenReturn(null);
        // Act & Assert – we expect a NullPointerException because defineClass will try to use the
        // null byte array. The important part is that the exception originates *after* the
        // ClassNotFoundException path has been taken.
        assertThrows(NullPointerException.class, () -> loadClass(DummyParent.class, mockMetadata), "Fallback path should be taken and eventually fail due to null class bytes");
    }

    /**
     * When the loaded class is *not* assignable to the parent but the metadata represents a
     * {@link ConfigMappingClass}, the method should still return the loaded class (the special
     * handling for ConfigMappingClass). We simulate this by creating a tiny subclass of
     * {@link ConfigMappingMetadata} that also implements the marker interface {@code ConfigMappingClass}.
     */
    @Test
    void loadClass_returnsClassEvenIfNotAssignableWhenMetadataIsConfigMappingClass() {
        // Arrange – a dummy class that does NOT implement DummyParent.
        class Unrelated {
        }
        // Create a minimal ConfigMappingClass implementation. The real library uses an internal
        // subclass, but for the purpose of this test we only need it to be an instance of
        // ConfigMappingClass (a marker interface) so that the loader takes the special branch.
        ConfigMappingClass dummyMapping = mock(ConfigMappingClass.class);
        when(dummyMapping.getClassName()).thenReturn(Unrelated.class.getName());
        // The byte array is never used because the class is found.
        when(dummyMapping.getClassBytes()).thenReturn(new byte[0]);
        // Act
        Class<?> result = loadClass(DummyParent.class, dummyMapping);
        // Assert – the loader should return the found class even though it is not assignable.
        assertEquals(Unrelated.class, result);
    }

    /**
     * Utility method to check that a given class is truly public, non‑abstract and can be
     * instantiated via the default constructor – this mirrors what {@code ensureLoaded}
     * expects from the implementation class.
     */
    @Test
    void implementationClassIsInstantiable() throws Exception {
        // This is not a direct test of loadClass, but it validates the contract of the class
        // returned by the first test.
        Class<?> impl = loadClass(DummyParent.class, // reuse the mock from the first test – it returns DummyParentImpl
        new ConfigMappingMetadata() {

            public String getClassName() {
                return DummyParentImpl.class.getName();
            }

            public byte[] getClassBytes() {
                return new byte[0];
            }

            // other methods of the interface are not used in the current test
            public Class<?> getInterfaceType() {
                return DummyParent.class;
            }

            public String getPrefix() {
                return "";
            }

            public boolean isConfigProperties() {
                return false;
            }
        });
        // Verify that a no‑arg constructor exists and can be invoked.
        assertTrue(Modifier.isPublic(impl.getModifiers()));
        Object instance = impl.getDeclaredConstructor().newInstance();
        assertTrue(instance instanceof DummyParent);
    }

    @Test
    void loadClass_nullParent_throwsNPE() {
        when(mockMetadata.getClassName()).thenReturn("any.Name");
        when(mockMetadata.getClassBytes()).thenReturn(new byte[0]);
        assertThrows(NullPointerException.class, () -> loadClass(null, mockMetadata), "Passing a null parent must result in NPE because the method dereferences parent");
    }

    @Test
    void loadClass_nullMetadata_throwsNPE() {
        assertThrows(NullPointerException.class, () -> loadClass(ParentInterface.class, null), "Passing a null metadata must result in NPE when getClassName() is invoked");
    }

    @Test
    void loadClass_nullClassNameInMetadata_throwsNPE() {
        when(mockMetadata.getClassName()).thenReturn(null);
        when(mockMetadata.getClassBytes()).thenReturn(new byte[0]);
        // The NPE is thrown when getClassLoaderLock receives a null key.
        assertThrows(NullPointerException.class, () -> loadClass(ParentInterface.class, mockMetadata));
    }

    @Test
    void loadClass_notAssignableAndNotConfigMappingClass_usesDefineClassFallback() {
        // The class loader already knows UnrelatedClass, but it does not implement ParentInterface.
        when(mockMetadata.getClassName()).thenReturn(UnrelatedClass.class.getName());
        // Return null bytes so that defineClass will fail with NPE – this proves the fallback path.
        when(mockMetadata.getClassBytes()).thenReturn(null);
        // The mock does NOT implement ConfigMappingClass, therefore the special branch is skipped.
        assertThrows(NullPointerException.class, () -> loadClass(ParentInterface.class, mockMetadata), "Fallback to defineClass should be taken and fail due to null byte array");
    }

    @Test
    void loadClass_securityExceptionPropagates() {
        // Create a dummy metadata pointing to a class name that would normally exist.
        when(mockMetadata.getClassName()).thenReturn(ParentImpl.class.getName());
        when(mockMetadata.getClassBytes()).thenReturn(new byte[0]);
        // Use a proxy class that uses the ThrowingClassLoader as its defining loader.
        ClassLoader original = RestrictedInterface.class.getClassLoader();
        ClassLoader throwing = new ThrowingClassLoader(original);
        Class<?> proxy = defineProxyClass(throwing, RestrictedInterface.class);
        // The proxy class implements the same interface but its class loader will throw.
        assertThrows(SecurityException.class, () -> loadClass(proxy, mockMetadata), "SecurityException from parent class loader must propagate");
    }

    @Test
    void loadClass_configMappingClassEvenIfNotAssignable_returnsLoadedClass() {
        // Metadata is a mock that also implements ConfigMappingClass (marker interface)
        ConfigMappingClass cfgClassMock = mock(ConfigMappingClass.class);
        when(cfgClassMock.getClassName()).thenReturn(UnrelatedClass.class.getName());
        // not used
        when(cfgClassMock.getClassBytes()).thenReturn(new byte[0]);
        Class<?> result = loadClass(ParentInterface.class, cfgClassMock);
        assertEquals(UnrelatedClass.class, result, "When metadata is ConfigMappingClass the loader must return the already loaded class even if not assignable");
    }

    @Test
    void loadClass_successfulLoad_returnsPublicClass() {
        when(mockMetadata.getClassName()).thenReturn(ParentImpl.class.getName());
        when(mockMetadata.getClassBytes()).thenReturn(new byte[0]);
        Class<?> loaded = loadClass(ParentInterface.class, mockMetadata);
        assertTrue(Modifier.isPublic(loaded.getModifiers()), "Returned class should be public");
        assertTrue(ParentInterface.class.isAssignableFrom(loaded), "Returned class must implement the parent interface");
    }
}
