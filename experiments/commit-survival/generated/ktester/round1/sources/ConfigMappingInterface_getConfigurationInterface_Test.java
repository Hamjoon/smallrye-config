package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.smallrye.config.ConfigMapping;
// import org.smallrye.config.WithName;
// import org.smallrye.config.NamingStrategy;
// import org.smallrye.config.source.keyvalues.KeyValuesConfigSource;

/**
 * Tests for {@link ConfigMappingInterface#getConfigurationInterface(Class)}.
 */
// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getConfigurationInterface_Test {

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "private")
    private static interface PrivateNestedConfig {
        String foo();
    }

    interface InnerConfig {
        String value();
    }

    class NotAnInterface {}

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "test")
    interface MinimalConfig {
        String host();
    }

    /* ---------------------------------------------------------------------- */
//     @ConfigMapping(prefix = "ns", namingStrategy = NamingStrategy.KEBAB_CASE)
    interface NamingStrategyConfig {
        String someValue();
    }

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "parent")
    interface ParentConfig {
        String parentProp();
    }

    @ConfigMapping(prefix = "child")
    interface ChildConfig extends ParentConfig {
        String childProp();
    }

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "generic")
    interface GenericConfig<T> {
        @WithName("value")
        T getValue();
    }

    /* ---------------------------------------------------------------------- */
    static class Outer {
        @ConfigMapping(prefix = "private")
        private static interface PrivateNestedConfig {
            String foo();
        }
    }

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "proxy")
    interface ProxyGeneratedConfig {
        String foo();
    }

    /* ---------------------------------------------------------------------- */
    interface UnannotatedConfig {
        String value();
    }

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "param")
    interface ParamMethodConfig {
        String getValue(String key);
    }

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "unsupported")
    interface UnsupportedReturnConfig {
        Map<String, Integer> getMap();
    }

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "dup")
    interface DuplicateNameConfig {
        @WithName("my.prop")
        String foo();

        @WithName("my.prop")
        String bar();
    }

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "default")
    interface DefaultMethodConfig {
        String foo();

        default String defaultFoo() {
            return "default";
        }
    }

    /* ---------------------------------------------------------------------- */
    // non‑static outer class containing a static inner interface
    class OuterNonStatic {
        interface InnerConfig {
            String value();
        }
    }

    /* ---------------------------------------------------------------------- */
    interface ParentNoAnnotation {
        String parentValue();
    }

    @ConfigMapping(prefix = "child")
    interface ChildWithoutParentAnnotationConfig extends ParentNoAnnotation {
        String childValue();
    }

    /* ---------------------------------------------------------------------- */
    /**
     * Verify that a {@code null} argument is rejected with an {@link IllegalArgumentException}.
     */
    @Test
    void getConfigurationInterface_nullArgument_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(null));
    }

    /**
     * Verify that a valid configuration interface yields a non‑null {@link ConfigMappingInterface}
     * and that the result is cached (the same instance is returned on subsequent calls).
     */
//     @Test
//     void getConfigurationInterface_validInterface_returnsCachedInstance() {
//         // Build a SmallRyeConfig that knows about the {@code MinimalConfig} interface.
//         new SmallRyeConfigBuilder()
//                 .withMapping(MinimalConfig.class)
//                 .withSources(KeyValuesConfigSource.config("test.host", "localhost"))
//                 .build();
// 
//         // First call – should create a new ConfigMappingInterface instance.
//         ConfigMappingInterface first = ConfigMappingInterface
//                 .getConfigurationInterface(MinimalConfig.class);
//         assertNotNull(first, "The first call should return a non‑null ConfigMappingInterface");
// 
//         // Second call – should return the exact same instance (cached via ClassValue).
//         ConfigMappingInterface second = ConfigMappingInterface
//                 .getConfigurationInterface(MinimalConfig.class);
//         assertSame(first, second, "Subsequent calls must return the cached instance");
//     }

    /**
     * Verify that calling the method for a class that is not a configuration interface
     * results in a {@link NoSuchElementException}.
     */
    @Test
    void getConfigurationInterface_nonConfigInterface_throws() {
        class NotAnInterface {}
        assertThrows(NoSuchElementException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(NotAnInterface.class));
    }

//     @Test
//     void getConfigurationInterface_namingStrategy_isExtractedCorrectly() {
//         new SmallRyeConfigBuilder()
//                 .withMapping(NamingStrategyConfig.class)
//                 .build();
// 
//         ConfigMappingInterface cmi = ConfigMappingInterface
//                 .getConfigurationInterface(NamingStrategyConfig.class);
//         assertNotNull(cmi);
//         assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy(),
//                 "Naming strategy should be KEBAB_CASE as declared on the interface");
//     }

    @Test
    void getConfigurationInterface_inheritance_detectsSuperTypes() {
        new SmallRyeConfigBuilder()
                .withMapping(ChildConfig.class)
                .build();

        ConfigMappingInterface childCmi = ConfigMappingInterface
                .getConfigurationInterface(ChildConfig.class);
        assertNotNull(childCmi);
        ConfigMappingInterface[] supers = childCmi.getSuperTypes();
        assertNotNull(supers);
        assertEquals(1, supers.length, "ChildConfig should have exactly one super‑type");
        assertEquals(ParentConfig.class, supers[0].getInterfaceType(),
                "The super‑type must be ParentConfig");
    }

    @Test
    void getConfigurationInterface_genericInterface_isAccepted() {
        new SmallRyeConfigBuilder()
                .withMapping(GenericConfig.class)
                .build();

        ConfigMappingInterface cmi = ConfigMappingInterface
                .getConfigurationInterface(GenericConfig.class);
        assertNotNull(cmi, "GenericConfig should produce a non‑null ConfigMappingInterface");
    }

    @Test
    void getConfigurationInterface_privateNestedInterface_isSupported() throws Exception {
        // Obtain the Class object via reflection because the type is private.
        Class<?> nestedCls = Class.forName(
                ConfigMappingInterface_getConfigurationInterface_Test.class.getName()
                        + "$Outer$PrivateNestedConfig");

        new SmallRyeConfigBuilder()
                .withMapping(nestedCls)
                .build();

        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(nestedCls);
        assertNotNull(cmi, "Private nested interface should be processed successfully");
    }

    @Test
    void getConfigurationInterface_proxyClass_isRejected() {
        InvocationHandler handler = (proxy, method, args) -> null;
        Class<?> proxyCls = Proxy.newProxyInstance(
                ProxyGeneratedConfig.class.getClassLoader(),
                new Class<?>[] { ProxyGeneratedConfig.class },
                handler).getClass();

        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(proxyCls),
                "A runtime generated proxy class must be rejected");
    }

    @Test
    void getConfigurationInterface_unannotatedInterface_isRejected() {
        new SmallRyeConfigBuilder()
                .withMapping(UnannotatedConfig.class)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(UnannotatedConfig.class),
                "Missing @ConfigMapping annotation should cause IllegalArgumentException");
    }

    @Test
    void getConfigurationInterface_methodWithParameters_isRejected() {
        new SmallRyeConfigBuilder()
                .withMapping(ParamMethodConfig.class)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(ParamMethodConfig.class),
                "Methods that declare parameters must cause IllegalArgumentException");
    }

    @Test
    void getConfigurationInterface_unsupportedReturnType_isRejected() {
        new SmallRyeConfigBuilder()
                .withMapping(UnsupportedReturnConfig.class)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(UnsupportedReturnConfig.class),
                "Unsupported return types (e.g., Map) must trigger IllegalArgumentException");
    }

    @Test
    void getConfigurationInterface_duplicatePropertyNames_isRejected() {
        new SmallRyeConfigBuilder()
                .withMapping(DuplicateNameConfig.class)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(DuplicateNameConfig.class),
                "Duplicate property names must cause IllegalArgumentException");
    }

    @Test
    void getConfigurationInterface_defaultMethod_isRejected() {
        new SmallRyeConfigBuilder()
                .withMapping(DefaultMethodConfig.class)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(DefaultMethodConfig.class),
                "Default methods must be rejected with IllegalArgumentException");
    }

    @Test
    void getConfigurationInterface_nonStaticInnerInterface_isRejected() {
        // The inner interface is static, but the enclosing class is non‑static.
        // SmallRye treats such a type as unsupported for configuration mapping.
        Class<?> innerCls = OuterNonStatic.InnerConfig.class;

        new SmallRyeConfigBuilder()
                .withMapping(innerCls)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(innerCls),
                "Non‑static inner configuration interfaces must be rejected");
    }

    @Test
    void getConfigurationInterface_superInterfaceMissingAnnotation_isRejected() {
        new SmallRyeConfigBuilder()
                .withMapping(ChildWithoutParentAnnotationConfig.class)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ConfigMappingInterface.getConfigurationInterface(ChildWithoutParentAnnotationConfig.class),
                "A super‑interface lacking @ConfigMapping must cause IllegalArgumentException");
    }
}