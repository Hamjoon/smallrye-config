import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getConfigurationInterface_Test {

    class NotAnInterface {
    }

//     class NotAnInterface {
    }

//     @ConfigMapping(prefix = "test")
    interface TestConfig {

        String value();
    }

//     @ConfigMapping(prefix = "other")
    interface OtherConfig {

        String other();
    }

    // deliberately not annotated with @ConfigMapping (annotation removed)
    interface NoConfigMapping {
    }

//     @ConfigMapping(prefix = "bean")
    interface BeanStyleConfig {

        // bean‑style getter
        int getServerPort();

        // bean‑style getter
        String getHostName();
    }

//     @ConfigMapping(prefix = "kebab", namingStrategy = NamingStrategy.KEBAB_CASE)
    interface KebabNamingConfig {

        String myProperty();
    }

//     @ConfigMapping(prefix = "def")
    interface DefaultMethodConfig {

        // abstract method – mapped to a property
        String required();

        default String optional() {
            return "default";
        }
    }

//     @ConfigMapping(prefix = "unsupported")
    interface UnsupportedReturnTypeConfig {

        // List has no built‑in SmallRye converter
        List<String> values();
    }

//     @ConfigMapping(prefix = "parent")
    interface ParentConfig {

        ChildConfig child();
    }

//     @ConfigMapping(prefix = "child")
    interface ChildConfig {

        String name();
    }

    // no additional methods
//     @ConfigMapping(prefix = "multi")
    interface MultiParentConfig extends SuperA, SuperB {
    }

//     @ConfigMapping(prefix = "a")
    interface SuperA {

        String a();
    }

//     @ConfigMapping(prefix = "b")
    interface SuperB {

        String b();
    }

    // no methods
//     @ConfigMapping(prefix = "empty")
    interface EmptyConfig {
    }

//     @ConfigMapping(prefix = "functional")
    interface FunctionalConfig {

        // single abstract method
        String value();
    }

//     @ConfigMapping(prefix = "dup")
    interface DuplicatePropertyConfig {

        String foo();

//         @WithName("foo")
//         String bar();
    }

//     @ConfigMapping(prefix = "param")
    interface MethodWithParametersConfig {

        // illegal: method has a parameter
        String value(String param);
    }

//     @ConfigMapping(prefix = "void")
    interface VoidReturnConfig {

        // illegal: void return type
        void nothing();
    }

//     @ConfigMapping(prefix = "emptyname")
    interface EmptyWithNameConfig {

//         @WithName("")
//         String foo();
    }

//     @ConfigMapping(prefix = "invaliddefault")
    interface InvalidDefaultConfig {

        // cannot convert "not-a-number" to int
//         @WithDefault("not-a-number")
//         int number();
    }

//     @ConfigMapping(prefix = "genericarray")
    interface GenericArrayUnsupportedConfig {

        // component type is generic and unsupported
        <T> T[] values();
    }

//     class NotAnInterface {
    }

    /**
     * The method must reject a {@code null} argument.
     */
//     @Test
//     void nullParameterShouldThrow() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(null));
//     }

    /**
     * The same {@code Class} must always return the same {@link ConfigMappingInterface}
     * instance because the implementation is cached in a {@link ClassValue}.
     */
//     @Test
//     void sameClassReturnsCachedInstance() {
//         ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(TestConfig.class);
//         ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(TestConfig.class);
//         assertSame(first, second, "Repeated calls with the same interface should return the cached instance");
//     }

    /**
     * Different configuration interfaces must produce different {@link ConfigMappingInterface}
     * instances.
     */
//     @Test
//     void differentClassesReturnDifferentInstances() {
//         ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(TestConfig.class);
//         ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(OtherConfig.class);
//         assertNotSame(first, second, "Different interfaces should not share the same ConfigMappingInterface instance");
//     }

    /**
     * Supplying a concrete class (i.e. not an interface) must be rejected.
     */
//     @Test
//     void nonInterfaceClassShouldThrow() {
//         class NotAnInterface {
//         }
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(NotAnInterface.class));
//     }

    /**
     * An interface that lacks the {@code @ConfigMapping} annotation must be rejected.
     */
//     @Test
//     void missingConfigMappingAnnotationShouldThrow() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(NoConfigMapping.class));
//     }

    /**
     * Bean‑style getter methods (prefixed with {@code get}) should be detected.
     */
//     @Test
//     void beanStyleGettersAreDetected() {
//         ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(BeanStyleConfig.class);
//         assertTrue(cmi.isBeanStyleGetters(), "Interface using bean‑style getters must have isBeanStyleGetters() == true");
//     }

    /**
     * The naming strategy defined in {@code @ConfigMapping} must be exposed.
     */
//     @Test
//     void explicitNamingStrategyIsReturned() {
//         ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(KebabNamingConfig.class);
//         assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy(), "Naming strategy defined in @ConfigMapping must be returned");
//     }

    /**
     * Default methods are allowed but must be ignored when building the property model.
     */
//     @Test
//     void defaultMethodsAreIgnored() {
//         ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(DefaultMethodConfig.class);
//         assertNotNull(cmi, "ConfigMappingInterface must be created even when default methods exist");
//         // The interface has exactly one abstract method, therefore exactly one property
//         assertEquals(1, cmi.getProperties().length, "Only abstract methods should be turned into properties");
//     }

    /**
     * Methods returning types without a registered {@link Converter} must cause a failure.
     */
//     @Test
//     void unsupportedReturnTypeShouldThrow() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(UnsupportedReturnTypeConfig.class));
//     }

    /**
     * Nested configuration interfaces must be exposed via {@code getNested()}.
     */
//     @Test
//     void nestedConfigurationIsReturned() {
//         ConfigMappingInterface parent = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
//         ConfigMappingInterface[] nested = parent.getNested();
//         assertNotNull(nested, "Nested array must not be null");
//         assertEquals(1, nested.length, "ParentConfig defines a single nested configuration");
//         assertEquals(ChildConfig.class, nested[0].getInterfaceType(), "Nested ConfigMappingInterface must correspond to ChildConfig");
//     }

    /**
     * Interfaces that extend multiple {@code @ConfigMapping} super‑interfaces should list them.
     */
//     @Test
//     void multipleSuperInterfacesAreReported() {
//         ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(MultiParentConfig.class);
//         ConfigMappingInterface[] supers = cmi.getSuperTypes();
//         assertNotNull(supers);
//         assertEquals(2, supers.length, "MultiParentConfig extends two @ConfigMapping interfaces");
//         Set<Class<?>> superTypes = Set.of(supers[0].getInterfaceType(), supers[1].getInterfaceType());
//         assertTrue(superTypes.contains(SuperA.class) && superTypes.contains(SuperB.class), "Both SuperA and SuperB should be present in the super‑type list");
//     }

    /**
     * An empty {@code @ConfigMapping} interface (no abstract methods) must be accepted.
     */
//     @Test
//     void emptyConfigMappingIsAccepted() {
//         ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(EmptyConfig.class);
//         assertNotNull(cmi);
//         assertEquals(0, cmi.getProperties().length, "EmptyConfig should have no properties");
//         assertTrue(cmi.getNames().isEmpty(), "EmptyConfig should have no configuration names");
//     }

    /**
     * Functional interfaces (single abstract method) are valid configuration interfaces.
     */
//     @Test
//     void functionalInterfaceIsAccepted() {
//         ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(FunctionalConfig.class);
//         assertNotNull(cmi);
//         assertEquals(1, cmi.getProperties().length, "FunctionalConfig should expose exactly one property");
//     }

    /**
     * Duplicate property names (e.g., via {@code @WithName}) must be rejected.
     */
//     @Test
//     void duplicatePropertyNamesShouldThrow() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(DuplicatePropertyConfig.class));
//     }

    /**
     * Methods that declare parameters are illegal.
     */
//     @Test
//     void methodWithParametersShouldThrow() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(MethodWithParametersConfig.class));
//     }

    /**
     * Methods returning {@code void} are illegal.
     */
//     @Test
//     void voidReturnMethodShouldThrow() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(VoidReturnConfig.class));
//     }

    /**
     * {@code @WithName} with an empty string must be rejected.
     */
//     @Test
//     void emptyWithNameShouldThrow() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(EmptyWithNameConfig.class));
//     }

    /**
     * {@code @WithDefault} values that cannot be converted to the target type must cause a failure.
     */
//     @Test
//     void invalidDefaultValueShouldThrow() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(InvalidDefaultConfig.class));
//     }

    /**
     * Generic array return types with unsupported component types must be rejected.
     */
//     @Test
//     void genericArrayUnsupportedShouldThrow() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(GenericArrayUnsupportedConfig.class));
//     }
}