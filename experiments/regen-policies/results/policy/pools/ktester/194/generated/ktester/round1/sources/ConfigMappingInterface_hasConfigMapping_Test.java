package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_hasConfigMapping_Test {

    /**
     * Mapping for an interface that is annotated with {@link ConfigMapping}.
     */
    private ConfigMappingInterface annotatedMapping;

    /**
     * Mapping for an interface that has no {@link ConfigMapping} annotation.
     */
    private ConfigMappingInterface nonAnnotatedMapping;

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

    @BeforeEach
    void setupBeforeEach() {
        // Build the ConfigMappingInterface instances that will be exercised.
        annotatedMapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedConfig.class);
        nonAnnotatedMapping = ConfigMappingInterface.getConfigurationInterface(NonAnnotatedConfig.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the references – helps the GC and guarantees isolation between tests.
        annotatedMapping = null;
        nonAnnotatedMapping = null;
    }

    @Test
    void hasConfigMapping_returnsTrueWhenAnnotationPresent() {
        // GIVEN – the mapping was created from an interface annotated with @ConfigMapping
        // WHEN
        boolean result = annotatedMapping.hasConfigMapping();
        // THEN
        assertTrue(result, "hasConfigMapping() should return true for an interface annotated with @ConfigMapping");
    }

    @Test
    void hasConfigMapping_returnsFalseWhenAnnotationAbsent() {
        // GIVEN – the mapping was created from an interface that lacks @ConfigMapping
        // WHEN
        boolean result = nonAnnotatedMapping.hasConfigMapping();
        // THEN
        assertFalse(result, "hasConfigMapping() should return false for an interface without @ConfigMapping");
    }

    @Test
    void hasConfigMapping_inheritedAnnotation_returnsFalse() {
        // GIVEN – ChildConfig extends a parent annotated with @ConfigMapping but does not declare it itself
        ConfigMappingInterface childMapping = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        // WHEN
        boolean result = childMapping.hasConfigMapping();
        // THEN
        assertFalse(result, "hasConfigMapping() must be false when the annotation is only present on a super‑interface");
    }

    @Test
    void hasConfigMapping_onClass_returnsTrue() {
        // GIVEN – a concrete class annotated with @ConfigMapping
        ConfigMappingInterface classMapping = ConfigMappingInterface.getConfigurationInterface(AnnotatedClass.class);
        // WHEN
        boolean result = classMapping.hasConfigMapping();
        // THEN
        assertTrue(result, "hasConfigMapping() should return true for a class annotated with @ConfigMapping");
    }

    @Test
    void getConfigurationInterface_nullArgument_throwsNPE() {
        // GIVEN a null argument
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getConfigurationInterface(null), "Passing null to getConfigurationInterface should throw NullPointerException");
    }

    @Test
    void hasConfigMapping_multipleAnnotations_returnsTrue() {
        // GIVEN – interface annotated with @ConfigMapping and another unrelated annotation
        ConfigMappingInterface multiAnnotated = ConfigMappingInterface.getConfigurationInterface(MultiAnnotatedConfig.class);
        // WHEN
        boolean result = multiAnnotated.hasConfigMapping();
        // THEN
        assertTrue(result, "hasConfigMapping() must return true even when other annotations are present");
    }

    @Test
    void hasConfigMapping_genericInterface_returnsTrue() {
        // GIVEN – a generic interface annotated with @ConfigMapping
        ConfigMappingInterface generic = ConfigMappingInterface.getConfigurationInterface(GenericConfig.class);
        // WHEN
        boolean result = generic.hasConfigMapping();
        // THEN
        assertTrue(result, "hasConfigMapping() should return true for a generic interface annotated with @ConfigMapping");
    }
}