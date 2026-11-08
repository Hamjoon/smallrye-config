package io.smallrye.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static io.smallrye.config.ConfigMapping.NamingStrategy.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getNamingStrategy_Test {

    /**
     * Interface without an explicit {@code namingStrategy} element.
     * The default strategy (KEBAB_CASE) should be returned.
     */
    @ConfigMapping(prefix = "default")
    interface DefaultNamingStrategyConfig {

        String someProperty();
    }

    /**
     * Interface with an explicit {@code namingStrategy = VERBATIM}.
     */
    @ConfigMapping(prefix = "verbatim", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    interface VerbatimNamingStrategyConfig {

        String someProperty();
    }

    /**
     * No @ConfigMapping annotation – should fall back to KEBAB_CASE.
     */
    interface NoConfigMappingInterface {

        String someProperty();
    }

    /**
     * Annotation present but namingStrategy not set – default KEBAB_CASE.
     */
    @ConfigMapping(prefix = "default")
    interface AnnotatedWithDefaultNamingStrategy {

        String someProperty();
    }

    /**
     * Explicit SNAKE_CASE strategy.
     */
    @ConfigMapping(prefix = "snake", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    interface AnnotatedWithSnakeCase {

        String someProperty();
    }

    /**
     * Explicit VERBATIM strategy.
     */
    @ConfigMapping(prefix = "verbatim", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
    interface AnnotatedWithVerbatim {

        String someProperty();
    }

    /**
     * Explicit KEBAB_CASE strategy (same as default but explicitly set).
     */
    @ConfigMapping(prefix = "kebab", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface AnnotatedWithExplicitKebabCase {

        String someProperty();
    }

    /**
     * Parent interface defines a strategy; child does not – annotation is not inherited.
     */
    @ConfigMapping(prefix = "parent", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    interface ParentAnnotated {

        String parentProp();
    }

    interface ChildWithoutAnnotationExtendsParentAnnotated extends ParentAnnotated {

        String childProp();
    }

    /**
     * Annotation with several attributes – only namingStrategy matters for the test.
     */
    @ConfigMapping(prefix = "all", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    interface AnnotatedWithAllAttributes {

        String someProperty();
    }

    private static Stream<Arguments> namingStrategyProvider() {
        return Stream.of(Arguments.of(NoConfigMappingInterface.class, KEBAB_CASE, "When the interface is not annotated with @ConfigMapping, the default KEBAB_CASE should be used."), Arguments.of(AnnotatedWithDefaultNamingStrategy.class, KEBAB_CASE, "Annotation present but namingStrategy not specified – default KEBAB_CASE should be returned."), Arguments.of(AnnotatedWithSnakeCase.class, ConfigMapping.NamingStrategy.SNAKE_CASE, "Explicit SNAKE_CASE strategy should be returned."), Arguments.of(AnnotatedWithVerbatim.class, VERBATIM, "Explicit VERBATIM strategy should be returned."), Arguments.of(AnnotatedWithExplicitKebabCase.class, KEBAB_CASE, "Explicit KEBAB_CASE strategy should be returned even though it matches the default."), Arguments.of(ChildWithoutAnnotationExtendsParentAnnotated.class, KEBAB_CASE, "Annotation is not inherited – child without annotation should fall back to default KEBAB_CASE."), Arguments.of(AnnotatedWithAllAttributes.class, ConfigMapping.NamingStrategy.SNAKE_CASE, "Annotation with additional attributes should still return the specified namingStrategy."));
    }

    @Test
    void getNamingStrategy_defaultsToKebabCaseWhenNotSpecified() {
        // Obtain the metadata representation for the interface.
        ConfigMappingInterface metadata = ConfigMappingInterface.getConfigurationInterface(DefaultNamingStrategyConfig.class);
        // The method under test.
        ConfigMapping.NamingStrategy strategy = metadata.getNamingStrategy();
        assertEquals(KEBAB_CASE, strategy, "When the ConfigMapping annotation does not define a namingStrategy, the default KEBAB_CASE should be used.");
    }

    @Test
    void getNamingStrategy_returnsExplicitValueWhenConfigured() {
        ConfigMappingInterface metadata = ConfigMappingInterface.getConfigurationInterface(VerbatimNamingStrategyConfig.class);
        ConfigMapping.NamingStrategy strategy = metadata.getNamingStrategy();
        assertEquals(VERBATIM, strategy, "When the ConfigMapping annotation defines namingStrategy = VERBATIM, the method should return VERBATIM.");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("namingStrategyProvider")
    void getNamingStrategy_variousScenarios(Class<?> interfaceClass, ConfigMapping.NamingStrategy expectedStrategy, String description) {
        // Obtain the metadata representation for the supplied interface.
        ConfigMappingInterface metadata = ConfigMappingInterface.getConfigurationInterface(interfaceClass);
        // Invoke the method under test.
        ConfigMapping.NamingStrategy actual = metadata.getNamingStrategy();
        assertEquals(expectedStrategy, actual, () -> "Failed for interface " + interfaceClass.getSimpleName() + ": " + description);
    }

    @Test
    void getNamingStrategy_throwsNPEWhenInterfaceTypeIsNull() throws Exception {
        // Create a normal metadata instance first.
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(NoConfigMappingInterface.class);
        // Use reflection to nullify the private final field `interfaceType`.
        Field field = ConfigMappingInterface.class.getDeclaredField("interfaceType");
        field.setAccessible(true);
        field.set(meta, null);
        // The call should now throw a NullPointerException because getAnnotation is invoked on a null reference.
        assertThrows(NullPointerException.class, meta::getNamingStrategy, "Expected NullPointerException when internal interfaceType field is null.");
    }

    /**
     * Simulating a {@link SecurityException} caused by a restrictive {@link SecurityManager}
     * is not feasible on modern JDKs (the SecurityManager API is deprecated and often disabled).
     * The test is therefore disabled but kept as documentation of the intended behaviour.
     */
    @Test
    @Disabled("SecurityManager based test is not reliable on current JDKs")
    void getNamingStrategy_propagatesSecurityException() {
        // Placeholder for a potential SecurityManager test.
        // In a real environment one could install a custom SecurityManager that denies
        // reflective access to annotations and then assert that the exception propagates.
        // Example (pseudo‑code):
        //   System.setSecurityManager(new SecurityManager() {
        //       @Override public void checkPermission(Permission perm) {
        //           if (perm instanceof ReflectPermission && perm.getName().equals("suppressAccessChecks")) {
        //               throw new SecurityException("Access denied");
        //           }
        //       }
        //   });
        //   ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(AnnotatedWithVerbatim.class);
        //   assertThrows(SecurityException.class, meta::getNamingStrategy);
    }
}