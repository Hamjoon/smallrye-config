package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.security.Permission;

class ConfigMappingInterface_hasNamingStrategy_Test {

    /* ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "sample")
    interface AnnotatedConfig {

        String value();
    }

    interface NonAnnotatedConfig {

        String value();
    }

    // Sub‑interface that inherits from an annotated interface – annotation is NOT @Inherited
    @ConfigMapping(prefix = "parent")
    interface ParentAnnotatedConfig {

        String parentValue();
    }

    interface ChildConfig extends ParentAnnotatedConfig {

        String childValue();
    }

    // Concrete class implementing an annotated interface – the class itself has no annotation
    static class ImplConfig implements AnnotatedConfig {

        @Override
        public String value() {
            return "impl";
        }
    }

    /* ---------------------------------------------------------------------- */
    private static void setInterfaceTypeViaReflection(ConfigMappingInterface instance, Class<?> newType) throws Exception {
        Field field = ConfigMappingInterface.class.getDeclaredField("interfaceType");
        field.setAccessible(true);
        field.set(instance, newType);
    }

    @Test
    void hasNamingStrategy_returnsTrueWhenInterfaceIsAnnotated() {
        // GIVEN a configuration interface annotated with @ConfigMapping
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedConfig.class);
        // WHEN checking for a naming strategy
        boolean result = mapping.hasNamingStrategy();
        // THEN the result must be true
        assertTrue(result, "Expected hasNamingStrategy() to return true for an @ConfigMapping annotated interface");
    }

    @Test
    void hasNamingStrategy_returnsFalseWhenInterfaceIsNotAnnotated() {
        // GIVEN a configuration interface without @ConfigMapping
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NonAnnotatedConfig.class);
        // WHEN checking for a naming strategy
        boolean result = mapping.hasNamingStrategy();
        // THEN the result must be false
        assertFalse(result, "Expected hasNamingStrategy() to return false for a non‑annotated interface");
    }

    @Test
    void hasNamingStrategy_returnsFalseForSubInterface() {
        // ChildConfig extends ParentAnnotatedConfig but does NOT carry the annotation itself
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        assertFalse(mapping.hasNamingStrategy(), "Sub‑interfaces should not inherit @ConfigMapping, therefore false is expected");
    }

//     @Test
//     void hasNamingStrategy_returnsFalseForImplementingClass() {
//         // The concrete class implements an annotated interface, but the class itself is not annotated.
//         // Obtain a mapping for the annotated interface and then replace its internal type with the
//         // concrete class to simulate the scenario.
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedConfig.class);
//         setInterfaceTypeViaReflection(mapping, ImplConfig.class);
//         assertFalse(mapping.hasNamingStrategy(), "Implementing classes should not inherit @ConfigMapping, therefore false is expected");
//     }

    @Test
    void hasNamingStrategy_returnsFalseWhenInterfaceTypeIsPrimitive() throws Exception {
        // Obtain a normal mapping first, then replace the internal field with a primitive class
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NonAnnotatedConfig.class);
        setInterfaceTypeViaReflection(mapping, int.class);
        assertFalse(mapping.hasNamingStrategy(), "Primitive types cannot be annotated, therefore false is expected");
    }

    @Test
    void hasNamingStrategy_returnsFalseWhenInterfaceTypeIsArray() throws Exception {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NonAnnotatedConfig.class);
        setInterfaceTypeViaReflection(mapping, String[].class);
        assertFalse(mapping.hasNamingStrategy(), "Array types do not carry @ConfigMapping, therefore false is expected");
    }

    @Test
    void hasNamingStrategy_returnsFalseWhenInterfaceTypeIsProxy() throws Exception {
        // Create a dynamic proxy for an annotated interface – the proxy class itself has no annotations
        Object proxy = Proxy.newProxyInstance(ConfigMappingInterface_hasNamingStrategy_Test.class.getClassLoader(), new Class<?>[] { AnnotatedConfig.class }, (p, m, args) -> null);
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NonAnnotatedConfig.class);
        setInterfaceTypeViaReflection(mapping, proxy.getClass());
        assertFalse(mapping.hasNamingStrategy(), "Proxy classes do not inherit annotations, therefore false is expected");
    }

    @Test
    void hasNamingStrategy_throwsNullPointerExceptionWhenInterfaceTypeIsNull() throws Exception {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NonAnnotatedConfig.class);
        // Force the private field to null
        setInterfaceTypeViaReflection(mapping, null);
        assertThrows(NullPointerException.class, mapping::hasNamingStrategy, "When interfaceType is null the method should throw NPE");
    }

    @Test
    void hasNamingStrategy_throwsSecurityExceptionWhenAnnotationAccessIsDenied() throws Exception {
        // Install a restrictive SecurityManager that denies reflective access to annotations
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except suppressAccessChecks which is used by getAnnotation()
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Access to annotations denied by test SecurityManager");
                }
            }
        });
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedConfig.class);
        try {
            // The call should now fail with a SecurityException
            assertThrows(SecurityException.class, mapping::hasNamingStrategy, "Expected SecurityException when a SecurityManager blocks annotation access");
        } finally {
            // Restore the original SecurityManager to avoid side‑effects for other tests
            System.setSecurityManager(original);
        }
    }
}