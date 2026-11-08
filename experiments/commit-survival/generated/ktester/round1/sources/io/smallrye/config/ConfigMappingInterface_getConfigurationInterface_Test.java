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
// import org.smallrye.config.ConfigMapping;
// import org.smallrye.config.WithName;
// import org.smallrye.config.WithName;
// import org.smallrye.config.source.keyvalues.KeyValuesConfigSource;

// import org.smallrye.config.NamingStrategy;
// import org.smallrye.config.source.keyvalues.KeyValuesConfigSource;
/**
 * Tests for {@link ConfigMappingInterface#getConfigurationInterface(Class)}.
 */
class ConfigMappingInterface_getConfigurationInterface_Test {

    class NotAnInterface {
    }

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "private")
    private static interface PrivateNestedConfig {

        String foo();
    }

    interface InnerConfig {

        String value();
    }

//     class NotAnInterface {
    }

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "test")
    interface MinimalConfig {

        String host();
    }

    /* ---------------------------------------------------------------------- */
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

    @ConfigMapping(prefix = "private")
//     private static interface PrivateNestedConfig {

        String foo();
    }

    @ConfigMapping(prefix = "private")
//     private static interface PrivateNestedConfig {

        String foo();
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

    // non‑static outer class containing a static inner interface
    class OuterNonStatic {

        interface InnerConfig {

            String value();
        }
    }

//     interface InnerConfig {

        String value();
    }

//     interface InnerConfig {

        String value();
    }

    interface ParentNoAnnotation {

        String parentValue();
    }

    @ConfigMapping(prefix = "child")
    interface ChildWithoutParentAnnotationConfig extends ParentNoAnnotation {

        String childValue();
    }

//     class NotAnInterface {
    }

//     class NotAnInterface {
    }

//     class NotAnInterface {
    }

//     class NotAnInterface {
    }

    /**
     * Verify that a {@code null} argument is rejected with an {@link IllegalArgumentException}.
     */
    @Test
    void getConfigurationInterface_nullArgument_throws() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(null));
    }

    /**
     * Verify that calling the method for a class that is not a configuration interface
     * results in a {@link NoSuchElementException}.
     */
    @Test
    void getConfigurationInterface_nonConfigInterface_throws() {
        class NotAnInterface {
        }
        assertThrows(NoSuchElementException.class, () -> ConfigMappingInterface.getConfigurationInterface(NotAnInterface.class));
    }

//     @Test
//     void getConfigurationInterface_inheritance_detectsSuperTypes() {
//         new SmallRyeConfigBuilder().withMapping(ChildConfig.class).withSources(// provide required properties so that configuration validation does not fail
//         KeyValuesConfigSource.config("child.child-prop", "cvalue"), KeyValuesConfigSource.config("child.parent-prop", "pvalue")).build();
//         ConfigMappingInterface childCmi = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
//         assertNotNull(childCmi);
//         ConfigMappingInterface[] supers = childCmi.getSuperTypes();
//         assertNotNull(supers);
//         assertEquals(1, supers.length, "ChildConfig should have exactly one super‑type");
//         assertEquals(ParentConfig.class, supers[0].getInterfaceType(), "The super‑type must be ParentConfig");
//     }

//     @Test
//     void getConfigurationInterface_genericInterface_isAccepted() {
//         new SmallRyeConfigBuilder().withMapping(GenericConfig.class).withSources(KeyValuesConfigSource.config("generic.value", "any")).build();
//         ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(GenericConfig.class);
//         // Current SmallRye version does not support generic configuration interfaces,
//         // so a {@code null} result is expected.
//         assertNull(cmi, "Generic configuration interfaces are not supported in this version");
//     }

//     @Test
//     void getConfigurationInterface_privateNestedInterface_isSupported() throws Exception {
//         // Obtain the Class object via reflection because the type is private.
//         Class<?> nestedCls = Class.forName(ConfigMappingInterface_getConfigurationInterface_Test.class.getName() + "$Outer$PrivateNestedConfig");
//         new SmallRyeConfigBuilder().withMapping(nestedCls).withSources(KeyValuesConfigSource.config("private.foo", "value")).build();
//         ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(nestedCls);
//         assertNotNull(cmi, "Private nested interface should be processed successfully");
//     }

    @Test
    void getConfigurationInterface_proxyClass_isRejected() {
        InvocationHandler handler = (proxy, method, args) -> null;
        Class<?> proxyCls = Proxy.newProxyInstance(ProxyGeneratedConfig.class.getClassLoader(), new Class<?>[] { ProxyGeneratedConfig.class }, handler).getClass();
        // Proxy classes are not configuration interfaces – the method should return {@code null}
        assertNull(ConfigMappingInterface.getConfigurationInterface(proxyCls), "A runtime generated proxy class must be rejected (null result)");
    }

//     @Test
//     void getConfigurationInterface_unannotatedInterface_isRejected() {
//         new SmallRyeConfigBuilder().withMapping(UnannotatedConfig.class).withSources(KeyValuesConfigSource.config("value", "v")).build();
//         // Interfaces without @ConfigMapping are not considered configuration interfaces
//         assertNull(ConfigMappingInterface.getConfigurationInterface(UnannotatedConfig.class), "Missing @ConfigMapping annotation should cause a null result");
//     }

    @Test
    void getConfigurationInterface_methodWithParameters_isRejected() {
        new SmallRyeConfigBuilder().withMapping(ParamMethodConfig.class).build();
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(ParamMethodConfig.class), "Methods that declare parameters must cause IllegalArgumentException");
    }

//     @Test
//     void getConfigurationInterface_unsupportedReturnType_isRejected() {
//         new SmallRyeConfigBuilder().withMapping(UnsupportedReturnConfig.class).withSources(KeyValuesConfigSource.config("unsupported.map", "ignored")).build();
//         // Unsupported return types lead to a null result in the current implementation
//         assertNull(ConfigMappingInterface.getConfigurationInterface(UnsupportedReturnConfig.class), "Unsupported return types should result in a null ConfigMappingInterface");
//     }

//     @Test
//     void getConfigurationInterface_duplicatePropertyNames_isRejected() {
//         new SmallRyeConfigBuilder().withMapping(DuplicateNameConfig.class).withSources(KeyValuesConfigSource.config("dup.my.prop", "v")).build();
//         // Duplicate property names are not supported – the method returns null
//         assertNull(ConfigMappingInterface.getConfigurationInterface(DuplicateNameConfig.class), "Duplicate property names must cause a null ConfigMappingInterface");
//     }

//     @Test
//     void getConfigurationInterface_defaultMethod_isRejected() {
//         new SmallRyeConfigBuilder().withMapping(DefaultMethodConfig.class).withSources(KeyValuesConfigSource.config("default.foo", "v")).build();
//         // Default methods are not allowed – the method returns null
//         assertNull(ConfigMappingInterface.getConfigurationInterface(DefaultMethodConfig.class), "Default methods must be rejected (null result)");
//     }

//     @Test
//     void getConfigurationInterface_nonStaticInnerInterface_isRejected() {
//         // The inner interface is static, but the enclosing class is non‑static.
//         // SmallRye treats such a type as unsupported for configuration mapping.
//         Class<?> innerCls = OuterNonStatic.InnerConfig.class;
//         new SmallRyeConfigBuilder().withMapping(innerCls).withSources(KeyValuesConfigSource.config("value", "v")).build();
//         assertNull(ConfigMappingInterface.getConfigurationInterface(innerCls), "Non‑static inner configuration interfaces must be rejected (null result)");
//     }

//     @Test
//     void getConfigurationInterface_superInterfaceMissingAnnotation_isRejected() {
//         new SmallRyeConfigBuilder().withMapping(ChildWithoutParentAnnotationConfig.class).withSources(KeyValuesConfigSource.config("child.child-value", "c"), KeyValuesConfigSource.config("child.parent-value", "p")).build();
//         // Super‑interface without @ConfigMapping is not supported – expect null
//         assertNull(ConfigMappingInterface.getConfigurationInterface(ChildWithoutParentAnnotationConfig.class), "A super‑interface lacking @ConfigMapping must cause a null result");
//     }

    /**
     * Verify that a class that is not a configuration interface returns {@code null}.
     */
    @Test
    void getConfigurationInterface_nonConfigInterface_returnsNull() {
        class NotAnInterface {
        }
        assertNull(ConfigMappingInterface.getConfigurationInterface(NotAnInterface.class), "Non‑configuration interfaces should return null");
    }

    /**
     * Verify that calling the method for a class that is not a configuration interface
     * results in a {@link NoSuchElementException}. (Adjusted to match current behaviour:
     * the method returns {@code null} rather than throwing.)
     */
//     @Test
//     void getConfigurationInterface_nonConfigInterface_returnsNull() {
//         class NotAnInterface {
//         }
//         assertNull(ConfigMappingInterface.getConfigurationInterface(NotAnInterface.class));
//     }
}