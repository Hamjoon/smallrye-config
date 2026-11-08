package io.smallrye.config;

import static io.smallrye.config.ConfigMapping.NamingStrategy;
import static io.smallrye.config.ConfigMapping.NamingStrategy.KEBAB_CASE;
import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;
import static org.junit.jupiter.api.Assertions.*;
import static io.smallrye.config.ConfigMapping.NamingStrategy.SNAKE_CASE;
// import static io.smallrye.config.ConfigMapping.NamingStrategy.CAMEL_CASE;
// import static io.smallrye.config.ConfigMapping.NamingStrategy.CAMEL_CASE;
import org.junit.jupiter.api.Test;

class ConfigMappingInterface_getNamingStrategy_Test {

    /**
     * Interface annotated with {@code @ConfigMapping} but without an explicit
     * namingStrategy – the default defined by the annotation is {@code KEBAB_CASE}.
     */
    @ConfigMapping(prefix = "test")
    interface WithAnnotationDefault {

        String anotherProperty();
    }

    /**
     * Interface annotated with {@code @ConfigMapping} that explicitly sets
     * the naming strategy to {@link NamingStrategy#VERBATIM}.
     */
    @ConfigMapping(prefix = "verb", namingStrategy = VERBATIM)
    interface WithVerbatiNaming {

        String verbatimProperty();
    }

    /**
     * No @ConfigMapping annotation – default strategy must be KEBAB_CASE.
     */
    interface NoAnnotation {

        String someProperty();
    }

    /**
     * Explicit SNAKE_CASE naming strategy.
     */
    @ConfigMapping(prefix = "snake", namingStrategy = SNAKE_CASE)
    interface SnakeCaseMappingInterface {

        String snakeProp();
    }

    /**
     * Explicit CAMEL_CASE naming strategy.
     */
//     @ConfigMapping(prefix = "camel", namingStrategy = CAMEL_CASE)
    interface CamelCaseMappingInterface {

        String camelProp();
    }

    /**
     * Explicit KEBAB_CASE naming strategy (even though it is the default).
     */
    @ConfigMapping(prefix = "explicit", namingStrategy = KEBAB_CASE)
    interface ExplicitKebabCaseMappingInterface {

        String kebabProp();
    }

    /**
     * Parent interface annotated with SNAKE_CASE.
     */
    @ConfigMapping(prefix = "parent", namingStrategy = SNAKE_CASE)
    interface ParentAnnotatedInterface {

        String parentProp();
    }

    /**
     * Sub‑interface does NOT declare its own @ConfigMapping – must NOT inherit.
     */
    interface SubInterfaceWithoutAnnotation extends ParentAnnotatedInterface {

        String childProp();
    }

    /**
     * Multiple annotations – @ConfigMapping should still be honoured.
     */
    @Deprecated
    @ConfigMapping(prefix = "multi", namingStrategy = SNAKE_CASE)
    interface MultiAnnotationMappingInterface {

        String multiProp();
    }

    /**
     * Concrete class (not an interface) annotated with CAMEL_CASE.
     */
//     @ConfigMapping(prefix = "class", namingStrategy = CAMEL_CASE)
    static class ClassAnnotatedWithConfigMapping {

        public String classProp() {
            return "value";
        }
    }

    @Test
    void getNamingStrategy_withoutAnnotation_returnsDefault() {
        ConfigMappingInterface cmi = new ConfigMappingInterface(NoAnnotation.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertEquals(KEBAB_CASE, cmi.getNamingStrategy(), "When no @ConfigMapping is present the naming strategy must be KEBAB_CASE");
    }

    @Test
    void getNamingStrategy_withAnnotationDefault_returnsDefault() {
        ConfigMappingInterface cmi = new ConfigMappingInterface(WithAnnotationDefault.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // The annotation does not specify a namingStrategy, so the method should fall back to KEBAB_CASE
        assertEquals(KEBAB_CASE, cmi.getNamingStrategy(), "When @ConfigMapping is present without namingStrategy the default must be KEBAB_CASE");
    }

    @Test
    void getNamingStrategy_withExplicitVerbatiNaming_returnsVerbati() {
        ConfigMappingInterface cmi = new ConfigMappingInterface(WithVerbatiNaming.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertEquals(VERBATIM, cmi.getNamingStrategy(), "When @ConfigMapping specifies VERBATIM the method must return VERBATIM");
    }

    @Test
    void getNamingStrategy_withExplicitSnakeCase_returnsSnakeCase() {
        ConfigMappingInterface cmi = new ConfigMappingInterface(SnakeCaseMappingInterface.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertEquals(SNAKE_CASE, cmi.getNamingStrategy(), "Explicit SNAKE_CASE naming strategy must be returned");
    }

//     @Test
//     void getNamingStrategy_withExplicitCamelCase_returnsCamelCase() {
//         ConfigMappingInterface cmi = new ConfigMappingInterface(CamelCaseMappingInterface.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
//         assertEquals(CAMEL_CASE, cmi.getNamingStrategy(), "Explicit CAMEL_CASE naming strategy must be returned");
//     }

    @Test
    void getNamingStrategy_withExplicitKebabCase_returnsKebabCase() {
        ConfigMappingInterface cmi = new ConfigMappingInterface(ExplicitKebabCaseMappingInterface.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertEquals(KEBAB_CASE, cmi.getNamingStrategy(), "Explicit KEBAB_CASE naming strategy must be returned (even though it is the default)");
    }

    @Test
    void getNamingStrategy_subInterfaceWithoutAnnotation_fallsBackToDefault() {
        ConfigMappingInterface cmi = new ConfigMappingInterface(SubInterfaceWithoutAnnotation.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertEquals(KEBAB_CASE, cmi.getNamingStrategy(), "Sub‑interface without its own @ConfigMapping must not inherit the parent strategy");
    }

    @Test
    void getNamingStrategy_multiAnnotationInterface_returnsSnakeCase() {
        ConfigMappingInterface cmi = new ConfigMappingInterface(MultiAnnotationMappingInterface.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertEquals(SNAKE_CASE, cmi.getNamingStrategy(), "Presence of other annotations must not affect retrieval of the naming strategy");
    }

//     @Test
//     void getNamingStrategy_classAnnotatedWithConfigMapping_returnsCamelCase() {
//         ConfigMappingInterface cmi = new ConfigMappingInterface(ClassAnnotatedWithConfigMapping.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
//         assertEquals(CAMEL_CASE, cmi.getNamingStrategy(), "A concrete class annotated with @ConfigMapping should be handled the same way as an interface");
//     }

    @Test
    void getNamingStrategy_nullInterfaceType_throwsNullPointerException() {
        ConfigMappingInterface cmi = new ConfigMappingInterface(null, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        assertThrows(NullPointerException.class, cmi::getNamingStrategy, "Calling getNamingStrategy() with a null interfaceType must throw NPE");
    }
}