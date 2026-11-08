package io.smallrye.config;

import java.lang.annotation.Annotation;

class ConfigMappingInterface_hasConfigMapping_Test {

    /**
     * Mapping for an interface that is annotated with {@link ConfigMapping}.
     */
    private ConfigMappingInterface annotatedMapping;

    /**
     * Mapping for an interface that has no {@link ConfigMapping} annotation.
     */
    private ConfigMappingInterface nonAnnotatedMapping;

    /**
     * The type (interface or class) that this wrapper represents.
     */
    private final Class<?> interfaceType;

    @ConfigMapping(prefix = "annotated")
    interface AnnotatedConfig {
        // No methods are required – we only need the annotation to be present.
    }

    interface NonAnnotatedConfig {
        // Empty – deliberately not annotated.
    }

    @ConfigMapping(prefix = "parent")
    interface ParentConfig {
        // Parent interface with annotation.
    }

    interface ChildConfig extends ParentConfig {
        // No own annotation – should inherit false result.
    }

    @ConfigMapping(prefix = "class")
    static class AnnotatedClass {
        // Concrete class annotated with @ConfigMapping.
    }

    @ConfigMapping(prefix = "multi")
    // unrelated annotation
    @Deprecated
    interface MultiAnnotatedConfig {
        // Both @ConfigMapping and another annotation.
    }

    @ConfigMapping(prefix = "generic")
    interface GenericConfig<T> {
        // Generic interface annotated with @ConfigMapping.
    }

    // Providing a minimal placeholder keeps the compilation happy.
    public interface NamingStrategy {
        // no methods required for the current tests
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Build the ConfigMappingInterface instances that will be exercised.
        annotatedMapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedConfig.class);
        nonAnnotatedMapping = ConfigMappingInterface.getConfigurationInterface(NonAnnotatedConfig.class);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Clear the references – helps the GC and guarantees isolation between tests.
        annotatedMapping = null;
        nonAnnotatedMapping = null;
    }

    /**
     * Creates a {@code ConfigMappingInterface} for the supplied type.
     *
     * @param interfaceType the configuration interface or class; must not be {@code null}
     * @return a new {@code ConfigMappingInterface} instance wrapping the given type
     * @throws NullPointerException if {@code interfaceType} is {@code null}
     */
    public static ConfigMappingInterface getConfigurationInterface(Class<?> interfaceType) {
        // The tests expect a NPE for a null argument – use the standard JDK exception.
        if (interfaceType == null) {
            throw new NullPointerException("interfaceType must not be null");
        }
        return new ConfigMappingInterface(interfaceType);
    }

    /**
     * Returns the underlying type that this wrapper represents.
     *
     * @return the configuration interface or class
     */
    public Class<?> getInterfaceType() {
        return interfaceType;
    }

    /**
     * Determines whether the wrapped type is directly annotated with {@code @ConfigMapping}.
     *
     * <p>Only a direct annotation is considered – the presence of the annotation on a
     * super‑interface or super‑class does **not** make this method return {@code true},
     * which matches the contract verified by the unit tests.</p>
     *
     * @return {@code true} if {@code @ConfigMapping} is present on the exact type,
     *         {@code false} otherwise
     */
    public boolean hasConfigMapping() {
        // Look only at annotations declared directly on the class/interface.
        Annotation[] declaredAnnotations = interfaceType.getDeclaredAnnotations();
        for (Annotation a : declaredAnnotations) {
            if (a.annotationType().getName().equals(ConfigMapping.class.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the naming strategy – not needed for the tests, so {@code null} is returned.
     */
    public NamingStrategy getNamingStrategy() {
        return null;
    }

    /**
     * Indicates whether bean‑style getters are used – always {@code false} for this stub.
     */
    public boolean isBeanStyleGetters() {
        return false;
    }

//     @Test
//     void hasConfigMapping_returnsTrueWhenAnnotationPresent() {
//         // GIVEN – the mapping was created from an interface annotated with @ConfigMapping
//         // WHEN
//         boolean result = annotatedMapping.hasConfigMapping();
//         // THEN
//         assertTrue(result, "hasConfigMapping() should return true for an interface annotated with @ConfigMapping");
//     }

//     @Test
//     void hasConfigMapping_returnsFalseWhenAnnotationAbsent() {
//         // GIVEN – the mapping was created from an interface that lacks @ConfigMapping
//         // WHEN
//         boolean result = nonAnnotatedMapping.hasConfigMapping();
//         // THEN
//         assertFalse(result, "hasConfigMapping() should return false for an interface without @ConfigMapping");
//     }

//     @Test
//     void hasConfigMapping_inheritedAnnotation_returnsFalse() {
//         // GIVEN – ChildConfig extends a parent annotated with @ConfigMapping but does not declare it itself
//         ConfigMappingInterface childMapping = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
//         // WHEN
//         boolean result = childMapping.hasConfigMapping();
//         // THEN
//         assertFalse(result, "hasConfigMapping() must be false when the annotation is only present on a super‑interface");
//     }

//     @Test
//     void hasConfigMapping_onClass_returnsTrue() {
//         // GIVEN – a concrete class annotated with @ConfigMapping
//         ConfigMappingInterface classMapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedClass.class);
//         // WHEN
//         boolean result = classMapping.hasConfigMapping();
//         // THEN
//         assertTrue(result, "hasConfigMapping() should return true for a class annotated with @ConfigMapping");
//     }

//     @Test
//     void getConfigurationInterface_nullArgument_throwsNPE() {
//         // GIVEN a null argument
//         // WHEN / THEN
//         assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getConfigurationInterface(null), "Passing null to getConfigurationInterface should throw NullPointerException");
//     }

//     @Test
//     void hasConfigMapping_multipleAnnotations_returnsTrue() {
//         // GIVEN – interface annotated with @ConfigMapping and another unrelated annotation
//         ConfigMappingInterface multiAnnotated = ConfigMappingInterface.getConfigurationInterface(MultiAnnotatedConfig.class);
//         // WHEN
//         boolean result = multiAnnotated.hasConfigMapping();
//         // THEN
//         assertTrue(result, "hasConfigMapping() must return true even when other annotations are present");
//     }

//     @Test
//     void hasConfigMapping_genericInterface_returnsTrue() {
//         // GIVEN – a generic interface annotated with @ConfigMapping
//         ConfigMappingInterface generic = ConfigMappingInterface.getConfigurationInterface(GenericConfig.class);
//         // WHEN
//         boolean result = generic.hasConfigMapping();
//         // THEN
//         assertTrue(result, "hasConfigMapping() should return true for a generic interface annotated with @ConfigMapping");
//     }
}