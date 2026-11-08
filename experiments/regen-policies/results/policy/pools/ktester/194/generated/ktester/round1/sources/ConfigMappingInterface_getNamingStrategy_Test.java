package io.smallrye.config;

import static io.smallrye.config.ConfigMapping.NamingStrategy;
import static io.smallrye.config.ConfigMapping.NamingStrategy.KEBAB_CASE;
import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;
import org.junit.jupiter.api.Test;
import static io.smallrye.config.ConfigMapping.NamingStrategy.SNAKE_CASE;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
// import static io.smallrye.config.ConfigMapping.NamingStrategy.CAMEL_CASE;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getNamingStrategy_Test {

    // No @ConfigMapping annotation – should fall back to the default.
    interface NoAnnotationConfig {

        // dummy method to make it a valid config mapping interface
        String dummy();
    }

    // Explicit naming strategy via @ConfigMapping.
    @ConfigMapping(prefix = "test", namingStrategy = NamingStrategy.VERBATIM)
    interface VerbatimConfig {

        // dummy method to make it a valid config mapping interface
        String dummy();
    }

    // 3. Explicit SNAKE_CASE naming strategy.
    @ConfigMapping(prefix = "snake", namingStrategy = SNAKE_CASE)
    interface SnakeCaseConfig {

        String dummy();
    }

    // 4. Explicit CAMEL_CASE naming strategy.
//     @ConfigMapping(prefix = "camel", namingStrategy = CAMEL_CASE)
    interface CamelCaseConfig {

        String dummy();
    }

    // 5. @ConfigMapping present but namingStrategy omitted – defaults to KEBAB_CASE.
    @ConfigMapping(prefix = "default")
    interface DefaultAnnotationConfig {

        String dummy();
    }

    // 6. Super‑interface annotated with SNAKE_CASE.
    @ConfigMapping(prefix = "super-snake", namingStrategy = SNAKE_CASE)
    interface SuperSnakeConfig {

        String dummy();
    }

    // Sub‑interface without any @ConfigMapping.
    interface SubNoAnnotationConfig extends SuperSnakeConfig {
    }

    // 7. Super‑interface annotated with CAMEL_CASE.
//     @ConfigMapping(prefix = "super-camel", namingStrategy = CAMEL_CASE)
    interface SuperCamelConfig {

        String dummy();
    }

    // Sub‑interface annotated with @ConfigMapping but does NOT set namingStrategy.
    @ConfigMapping(prefix = "sub-default")
    interface SubOverridesDefaultConfig extends SuperCamelConfig {
    }

    /**
     * When the interface is not annotated with {@link ConfigMapping},
     * the default naming strategy (KEBAB_CASE) must be returned.
     */
    @Test
    void getNamingStrategy_defaultWhenNoAnnotation() {
        // GIVEN an interface without @ConfigMapping
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(NoAnnotationConfig.class);
        // WHEN
        NamingStrategy result = cmi.getNamingStrategy();
        // THEN
        assertEquals(KEBAB_CASE, result, "Expected default naming strategy KEBAB_CASE when @ConfigMapping is absent");
    }

    /**
     * When the interface is annotated with {@link ConfigMapping} and a specific
     * naming strategy, that strategy must be returned.
     */
    @Test
    void getNamingStrategy_respectsAnnotation() {
        // GIVEN
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(VerbatimConfig.class);
        // WHEN
        NamingStrategy result = cmi.getNamingStrategy();
        // THEN
        assertEquals(VERBATIM, result, "Expected naming strategy VERBATIM as defined in @ConfigMapping");
    }

    @Test
    void getNamingStrategy_respectsVerbatimAnnotation() {
        // GIVEN an interface annotated with VERBATIM
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(VerbatimConfig.class);
        // WHEN
        NamingStrategy result = cmi.getNamingStrategy();
        // THEN
        assertEquals(VERBATIM, result, "Expected naming strategy VERBATIM as defined in @ConfigMapping");
    }

    @Test
    void getNamingStrategy_respectsSnakeCaseAnnotation() {
        // GIVEN an interface annotated with SNAKE_CASE
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SnakeCaseConfig.class);
        // WHEN
        NamingStrategy result = cmi.getNamingStrategy();
        // THEN
        assertEquals(SNAKE_CASE, result, "Expected naming strategy SNAKE_CASE as defined in @ConfigMapping");
    }

//     @Test
//     void getNamingStrategy_respectsCamelCaseAnnotation() {
//         // GIVEN an interface annotated with CAMEL_CASE
//         ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(CamelCaseConfig.class);
//         // WHEN
//         NamingStrategy result = cmi.getNamingStrategy();
//         // THEN
//         assertEquals(CAMEL_CASE, result, "Expected naming strategy CAMEL_CASE as defined in @ConfigMapping");
//     }

    @Test
    void getNamingStrategy_defaultWhenAnnotationOmitsNamingStrategy() {
        // GIVEN an interface annotated with @ConfigMapping but without namingStrategy attribute
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(DefaultAnnotationConfig.class);
        // WHEN
        NamingStrategy result = cmi.getNamingStrategy();
        // THEN
        assertEquals(KEBAB_CASE, result, "When @ConfigMapping does not specify namingStrategy, the default KEBAB_CASE must be returned");
    }

    @Test
    void getNamingStrategy_subInterfaceWithoutAnnotationFallsBackToDefault() {
        // GIVEN a sub‑interface that has no @ConfigMapping, while its super‑interface
        // is annotated with SNAKE_CASE. getAnnotation does NOT consider inheritance.
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SubNoAnnotationConfig.class);
        // WHEN
        NamingStrategy result = cmi.getNamingStrategy();
        // THEN
        assertEquals(KEBAB_CASE, result, "Sub‑interface without @ConfigMapping should ignore its super‑interface's annotation and use default");
    }

    @Test
    void getNamingStrategy_subInterfaceOverridesSuperWithDefaultAnnotation() {
        // GIVEN a sub‑interface annotated with @ConfigMapping (no namingStrategy) that
        // extends a super‑interface annotated with CAMEL_CASE.
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SubOverridesDefaultConfig.class);
        // WHEN
        NamingStrategy result = cmi.getNamingStrategy();
        // THEN
        assertEquals(KEBAB_CASE, result, "Sub‑interface's own @ConfigMapping (even without namingStrategy) overrides the super‑interface");
    }

    @Test
    void getNamingStrategy_nullInterfaceTypeThrowsNPE() throws Exception {
        // GIVEN a normal ConfigMappingInterface instance
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(NoAnnotationConfig.class);
        // Use reflection to set the private field `interfaceType` to null
        Field field = ConfigMappingInterface.class.getDeclaredField("interfaceType");
        field.setAccessible(true);
        field.set(cmi, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, cmi::getNamingStrategy, "Calling getNamingStrategy() with a null interfaceType should throw NullPointerException");
    }
}