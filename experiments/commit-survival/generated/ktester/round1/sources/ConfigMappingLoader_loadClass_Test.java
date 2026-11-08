package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.*;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_loadClass_Test {

    /**
     * A dummy interface used as the “parent” type for the first test case.
     */
    interface DummyParent {

        void doSomething();
    }

    /**
     * A dummy class that implements {@link DummyParent}. Used to verify the assignable path.
     */
    static class DummyImpl implements DummyParent {

        public void doSomething() {
        }
    }

    /**
     * Simple parent interface for assignable‑class tests.
     */
    interface ParentInterface {

        void doSomething();
    }

    /**
     * Concrete implementation of {@link ParentInterface}.
     */
    static class ParentImpl implements ParentInterface {

        public void doSomething() {
        }
    }

    /**
     * Concrete parent class (not an interface) used in a few scenarios.
     */
    static class ConcreteParentClass {
    }

    /**
     * A class that does NOT extend {@link ConcreteParentClass}.
     */
    static class UnrelatedClass {
    }

    class SubConcrete extends ConcreteParentClass {
    }

    class SubConcrete extends ConcreteParentClass {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the internal lock map to avoid cross‑test interference.
        // The lock map is a private static field inside ConfigMappingLoader, but we can clear it
        // via reflection because it is package‑private (same package as the test).
        try {
            java.lang.reflect.Field lockField = ConfigMappingLoader.class.getDeclaredField("classLoaderLocks");
            lockField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String, Object> map = (ConcurrentHashMap<String, Object>) lockField.get(null);
            map.clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // If the field does not exist we simply ignore – the test will still be valid.
        }
    }

    /**
     * Verifies that when the class represented by {@code ConfigMappingMetadata} is already
     * available in the parent class‑loader and is assignable to the parent type, the method
     * returns that class without trying to redefine it.
     */
    @Test
    void loadClass_returnsAlreadyLoadedAssignableClass() {
        // Arrange – a mock metadata that points to an existing class (DummyImpl)
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn(DummyImpl.class.getName());
        // The byte‑code is never used in this branch, but we provide a non‑null value to avoid NPE.
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        // Act
        Class<?> result = ConfigMappingLoader.loadClass(DummyParent.class, metadata);
        // Assert
        assertEquals(DummyImpl.class, result, "The loaded class should be the already existing DummyImpl class");
    }

    /**
     * Verifies the branch where the class is present but not assignable and the metadata is an
     * instance of {@link ConfigMappingClass}. In that situation the method should still return the
     * loaded class (the special {@code ConfigMappingClass} marker tells the loader that the class
     * does not need to be re‑defined).
     */
    @Test
    void loadClass_returnsClassWhenMetadataIsConfigMappingClass() {
        // Arrange – a mock metadata that points to a class that does NOT implement DummyParent
        ConfigMappingMetadata metadata = mock(ConfigMappingClass.class);
        when(metadata.getClassName()).thenReturn(String.class.getName());
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        // Act
        Class<?> result = ConfigMappingLoader.loadClass(DummyParent.class, metadata);
        // Assert – String does not implement DummyParent, but because the metadata is a
        // ConfigMappingClass the loader must return the loaded class unchanged.
        assertEquals(String.class, result, "When metadata is a ConfigMappingClass the loader should return the loaded class even if it is not assignable");
    }

    /**
     * Forces the {@code ClassNotFoundException} path. The supplied class name does not exist,
     * therefore {@code loadClass} will attempt to call the private {@code defineClass} method.
     * Because we provide an empty byte‑array, the attempt to define the class fails with a
     * {@link java.lang.IllegalArgumentException}. The test asserts that this exception (wrapped in
     * an {@link UndeclaredThrowableException}) propagates out of the method.
     */
    @Test
    void loadClass_usesDefineClassWhenClassNotFound() {
        // Arrange – a mock metadata with a non‑existent class name
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn("non.existent.FakeClass");
        // Empty byte array – the private defineClass will reject it.
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        // Act & Assert
        UndeclaredThrowableException thrown = assertThrows(UndeclaredThrowableException.class, () -> ConfigMappingLoader.loadClass(DummyParent.class, metadata), "When the class cannot be loaded, defineClass should be invoked and its failure wrapped in UndeclaredThrowableException");
        // The cause should be the IllegalArgumentException thrown by defineClass.
        assertTrue(thrown.getCause() instanceof IllegalArgumentException, "The root cause should be IllegalArgumentException from defineClass");
    }

    @Test
    void loadClass_fallbackToDefineClassWhenNotAssignableAndNotConfigMappingClass() throws Exception {
        // Use a real class name that does NOT implement ParentInterface.
        String className = UnrelatedClass.class.getName();
        // Load the byte‑code of UnrelatedClass from the class‑path – this is a valid class file.
        byte[] classBytes;
        try (java.io.InputStream is = UnrelatedClass.class.getResourceAsStream(UnrelatedClass.class.getSimpleName() + ".class")) {
            assertNotNull(is, "Unable to read bytecode of UnrelatedClass");
            classBytes = is.readAllBytes();
        }
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn(className);
        when(metadata.getClassBytes()).thenReturn(classBytes);
        // The class loader will load UnrelatedClass (which is not assignable). The loader
        // should therefore invoke defineClass and return the newly defined class.
        Class<?> result = ConfigMappingLoader.loadClass(ParentInterface.class, metadata);
        assertNotNull(result, "defineClass should have returned a Class instance");
        assertEquals(className, result.getName(), "The defined class should have the expected name");
    }

    @Test
    void getClassLoaderLock_returnsSameObjectForSameClassName() throws Exception {
        Method lockMethod = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);
        Object lock1 = lockMethod.invoke(null, "com.example.Foo");
        Object lock2 = lockMethod.invoke(null, "com.example.Foo");
        assertSame(lock1, lock2, "Locks for identical class names must be the same instance");
    }

    @Test
    void getClassLoaderLock_returnsDifferentObjectsForDifferentNames() throws Exception {
        Method lockMethod = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);
        Object lockA = lockMethod.invoke(null, "com.example.Foo");
        Object lockB = lockMethod.invoke(null, "com.example.Bar");
        assertNotSame(lockA, lockB, "Locks for different class names must be distinct objects");
    }

    @Test
    void loadClass_nullParent_throwsNullPointerException() {
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn("any.Name");
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(null, metadata), "Passing a null parent should result in NPE");
    }

    @Test
    void loadClass_nullMetadata_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(ParentInterface.class, null), "Passing a null metadata should result in NPE");
    }

    @Test
    void loadClass_nullClassNameInMetadata_throwsNullPointerException() {
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn(null);
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(ParentInterface.class, metadata), "Null class name should cause NPE during lock acquisition or class loading");
    }

    @Test
    void loadClass_nullClassBytesInMetadata_throwsNullPointerException() {
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn("non.existent.FakeClass");
        // will be passed to defineClass
        when(metadata.getClassBytes()).thenReturn(null);
        // ClassNotFoundException forces the defineClass path.
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(ParentInterface.class, metadata), "Null class bytes should cause NPE inside defineClass");
    }

    @Test
    void loadClass_parentBootstrapClassLoaderNull_throwsNullPointerException() {
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn("java.lang.String");
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(Object.class, metadata), "Bootstrap class loader is null; dereferencing it must throw NPE");
    }

    @Test
    void loadClass_classLoaderThrowsSecurityException_propagates() {
        // Mock a ClassLoader that throws SecurityException on any loadClass call.
        ClassLoader badLoader = mock(ClassLoader.class);
        when(badLoader.loadClass(anyString())).thenThrow(new SecurityException("Forbidden"));
        // Mock a parent Class whose getClassLoader returns the bad loader.
        @SuppressWarnings("unchecked")
        Class<Object> parent = mock(Class.class);
        when(parent.getClassLoader()).thenReturn(badLoader);
        // The isAssignableFrom call is irrelevant because loadClass already throws.
        when(parent.isAssignableFrom(any())).thenReturn(false);
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn("any.Name");
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        assertThrows(SecurityException.class, () -> ConfigMappingLoader.loadClass(parent, metadata), "SecurityException from the class loader must propagate");
    }

    @Test
    void loadClass_classLoaderThrowsNoClassDefFoundError_propagates() {
        ClassLoader badLoader = mock(ClassLoader.class);
        when(badLoader.loadClass(anyString())).thenThrow(new NoClassDefFoundError("Missing"));
        @SuppressWarnings("unchecked")
        Class<Object> parent = mock(Class.class);
        when(parent.getClassLoader()).thenReturn(badLoader);
        when(parent.isAssignableFrom(any())).thenReturn(false);
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn("any.Name");
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        assertThrows(NoClassDefFoundError.class, () -> ConfigMappingLoader.loadClass(parent, metadata), "NoClassDefFoundError from the class loader must propagate");
    }

    @Test
    void loadClass_defineClassThrowsIllegalArgumentException_propagates() {
        // Use a valid existing class name but give an illegal name to metadata.
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn("illegal name with spaces");
        // Provide a non‑empty byte array (the content is irrelevant – defineClass will reject the name first)
        when(metadata.getClassBytes()).thenReturn(new byte[] { 0xCA, 0xFE, 0xBA, 0xBE });
        // Force the class loader to successfully load a class (so we reach the defineClass branch)
        ClassLoader loader = mock(ClassLoader.class);
        when(loader.loadClass(anyString())).thenReturn(String.class);
        @SuppressWarnings("unchecked")
        Class<Object> parent = mock(Class.class);
        when(parent.getClassLoader()).thenReturn(loader);
        // not assignable
        when(parent.isAssignableFrom(any())).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.loadClass(parent, metadata), "IllegalArgumentException from defineClass (bad class name) must propagate");
    }

    @Test
    void loadClass_defineClassThrowsClassFormatError_propagates() {
        // Valid class name but malformed bytecode.
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn("io.smallrye.config.Malformed");
        // not a valid class file
        when(metadata.getClassBytes()).thenReturn(new byte[] { 0, 1, 2 });
        // Force the class loader to load a class that is not assignable.
        ClassLoader loader = mock(ClassLoader.class);
        when(loader.loadClass(anyString())).thenReturn(String.class);
        @SuppressWarnings("unchecked")
        Class<Object> parent = mock(Class.class);
        when(parent.getClassLoader()).thenReturn(loader);
        when(parent.isAssignableFrom(any())).thenReturn(false);
        assertThrows(ClassFormatError.class, () -> ConfigMappingLoader.loadClass(parent, metadata), "ClassFormatError from defineClass (malformed bytecode) must propagate");
    }

    @Test
    void getClassLoaderLock_emptyClassName_throwsIllegalArgumentException() throws Exception {
        Method lockMethod = ConfigMappingLoader.class.getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> lockMethod.invoke(null, ""), "Empty class name must cause IllegalArgumentException in getClassLoaderLock");
    }

    @Test
    void loadClass_parentConcreteClassAssignable_returnsLoadedClass() {
        // Create a subclass of ConcreteParentClass at runtime.
        class SubConcrete extends ConcreteParentClass {
        }
        ConfigMappingMetadata metadata = mock(ConfigMappingMetadata.class);
        when(metadata.getClassName()).thenReturn(SubConcrete.class.getName());
        // not used
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        Class<?> result = ConfigMappingLoader.loadClass(ConcreteParentClass.class, metadata);
        assertEquals(SubConcrete.class, result, "When the loaded class extends the concrete parent, it should be returned directly");
    }

    @Test
    void loadClass_parentConcreteClassNonAssignable_metadataIsConfigMappingClass() {
        ConfigMappingMetadata metadata = mock(ConfigMappingClass.class);
        when(metadata.getClassName()).thenReturn(String.class.getName());
        when(metadata.getClassBytes()).thenReturn(new byte[0]);
        Class<?> result = ConfigMappingLoader.loadClass(ConcreteParentClass.class, metadata);
        assertEquals(String.class, result, "When metadata is ConfigMappingClass the loader returns the loaded class even if not assignable");
    }
}
