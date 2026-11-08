package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.*;
import java.util.function.Supplier;

class ConfigMappingLoader_loadClass_Test {

    /**
     * Simple {@code @ConfigMapping} interface used only for this test class.
     * The {@code ConfigMappingLoader} will generate an implementation class for it at runtime.
     */
    @ConfigMapping(prefix = "test")
    interface SimpleMapping {

        String name();

        int age();
    }

    /**
     * Verify that {@link ConfigMappingLoader#loadClass(Class, ConfigMappingMetadata)} returns the
     * generated implementation class for a mapping interface and that the same class instance is
     * returned on subsequent invocations (i.e. the method does not create duplicate classes).
     */
    @Test
    void loadClassGeneratesImplementationAndCachesIt() {
        // --------------------------------------------------------------------
        // given – obtain the metadata that the loader uses to generate the class
        // --------------------------------------------------------------------
        List<ConfigMappingMetadata> metadataList = ConfigMappingLoader.getConfigMappingsMetadata(SimpleMapping.class);
        assertEquals(1, metadataList.size(), "There should be exactly one metadata entry for the interface");
        ConfigMappingMetadata metadata = metadataList.get(0);
        // --------------------------------------------------------------------
        // when – invoke the focal method for the first time
        // --------------------------------------------------------------------
        Class<?> firstLoad = ConfigMappingLoader.loadClass(SimpleMapping.class, metadata);
        // --------------------------------------------------------------------
        // then – the returned class must be the generated implementation and
        //        must implement the original interface
        // --------------------------------------------------------------------
        assertNotNull(firstLoad, "loadClass must not return null");
        assertTrue(SimpleMapping.class.isAssignableFrom(firstLoad), "The generated class must implement the mapping interface");
        String expectedImplName = ConfigMappingInterface.getImplementationClassName(SimpleMapping.class);
        assertEquals(expectedImplName, firstLoad.getName(), "The generated class name must follow the naming convention");
        // --------------------------------------------------------------------
        // when – invoke the focal method a second time with the same metadata
        // --------------------------------------------------------------------
        Class<?> secondLoad = ConfigMappingLoader.loadClass(SimpleMapping.class, metadata);
        // --------------------------------------------------------------------
        // then – the same Class object should be returned (no duplicate definition)
        // --------------------------------------------------------------------
        assertSame(firstLoad, secondLoad, "Subsequent calls with the same metadata must return the already‑loaded class");
    }

    /**
     * The method dereferences {@code parent} to obtain its class‑loader, therefore
     * a {@code NullPointerException} must be thrown when {@code parent} is {@code null}.
     */
    @Test
    void loadClassThrowsNullPointerExceptionWhenParentIsNull() {
        ConfigMappingMetadata meta = new ConfigMappingMetadata() {

            @Override
            public String getClassName() {
                return "java.lang.String";
            }

            @Override
            public byte[] getClassBytes() {
                return new byte[0];
            }

            @Override
            public Class<?> getInterfaceType() {
                return Supplier.class;
            }
        };
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(null, meta), "Calling loadClass with a null parent should throw NullPointerException");
    }

    /**
     * Passing a {@code null} metadata argument leads to an immediate NPE when
     * {@code getClassName()} is invoked.
     */
    @Test
    void loadClassThrowsNullPointerExceptionWhenMetadataIsNull() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(Supplier.class, null), "Calling loadClass with a null metadata argument should throw NullPointerException");
    }

    /**
     * If the metadata returns {@code null} for {@code getClassName()}, the
     * underlying {@code ClassLoader.loadClass(null)} call throws a
     * {@code NullPointerException}.
     */
    @Test
    void loadClassThrowsNullPointerExceptionWhenClassNameIsNull() {
        ConfigMappingMetadata meta = new ConfigMappingMetadata() {

            @Override
            public String getClassName() {
                return null;
            }

            @Override
            public byte[] getClassBytes() {
                return new byte[0];
            }

            @Override
            public Class<?> getInterfaceType() {
                return Supplier.class;
            }
        };
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(Supplier.class, meta), "A null className must cause NullPointerException");
    }

    /**
     * An empty class name results in {@code ClassLoader.loadClass("")} throwing
     * {@code ClassNotFoundException}. The subsequent call to {@code defineClass}
     * with an empty name is illegal and leads to an {@code IllegalArgumentException}.
     */
    @Test
    void loadClassThrowsIllegalArgumentExceptionWhenClassNameIsEmpty() {
        ConfigMappingMetadata meta = new ConfigMappingMetadata() {

            @Override
            public String getClassName() {
                return "";
            }

            @Override
            public byte[] getClassBytes() {
                // dummy non‑null bytes – the failure occurs before they are used
                return new byte[] { 0x0 };
            }

            @Override
            public Class<?> getInterfaceType() {
                return Supplier.class;
            }
        };
        // Use this test class (or any other application class) as the parent to avoid a null class‑loader.
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.loadClass(ConfigMappingLoader_loadClass_Test.class, meta), "An empty class name should result in IllegalArgumentException from defineClass");
    }

    /**
     * When the parent class is loaded by the bootstrap class‑loader
     * (e.g. {@code java.lang.Runnable}), {@code parent.getClassLoader()} returns
     * {@code null}. Attempting to invoke {@code loadClass} on a {@code null}
     * class‑loader must throw {@code NullPointerException}.
     */
    @Test
    void loadClassThrowsNullPointerExceptionWhenParentClassLoaderIsNull() {
        ConfigMappingMetadata meta = new ConfigMappingMetadata() {

            @Override
            public String getClassName() {
                return "java.lang.String";
            }

            @Override
            public byte[] getClassBytes() {
                return new byte[0];
            }

            @Override
            public Class<?> getInterfaceType() {
                return Supplier.class;
            }
        };
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(Runnable.class, meta), "Using a bootstrap‑class parent (null classloader) must throw NullPointerException");
    }

    /**
     * If the class cannot be found and the metadata provides {@code null}
     * {@code classBytes}, the fallback to {@code defineClass} triggers a
     * {@code NullPointerException} (or {@code IllegalArgumentException} depending
     * on the JVM). The test asserts that an exception is thrown.
     */
    @Test
    void loadClassThrowsWhenClassNotFoundAndClassBytesNull() {
        ConfigMappingMetadata meta = new ConfigMappingMetadata() {

            @Override
            public String getClassName() {
                // a class name that is guaranteed not to exist
                return "com.example.DoesNotExist";
            }

            @Override
            public byte[] getClassBytes() {
                // illegal for defineClass
                return null;
            }

            @Override
            public Class<?> getInterfaceType() {
                return Supplier.class;
            }
        };
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.loadClass(Supplier.class, meta), "When class is missing and classBytes is null, loadClass must propagate NullPointerException");
    }
}
