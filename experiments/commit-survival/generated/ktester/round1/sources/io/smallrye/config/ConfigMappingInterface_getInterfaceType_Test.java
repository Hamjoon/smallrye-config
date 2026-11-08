package io.smallrye.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getInterfaceType_Test {

    /**
     * The instance under test.
     */
    private ConfigMappingInterface mapping;

    /**
     * The class that is supplied to the constructor.
     */
    private Class<?> suppliedInterfaceClass;

    interface NestedConfig {

        String nested();
    }

//     interface NestedConfig {

        String nested();
    }

//     interface NestedConfig {

        String nested();
    }

//     interface NestedConfig {

        String nested();
    }

    /**
     * Dummy configuration interface used as the target type.
     */
    interface DummyConfig {

        String value();
    }

    /**
     * Simple configuration interface – no super‑interfaces, no methods.
     */
    interface SampleConfig {

        String value();
    }

    /**
     * Parent interface for inheritance tests.
     */
    interface ParentA {

        String a();
    }

    /**
     * Another parent interface for multi‑inheritance tests.
     */
    interface ParentB {

        String b();
    }

    /**
     * Child interface that extends a single parent.
     */
    interface ChildConfig extends ParentA {

        String child();
    }

    /**
     * Interface that extends two parents (multiple inheritance).
     */
    interface MultiParentConfig extends ParentA, ParentB {

        String child();
    }

    /**
     * Generic configuration interface.
     */
    interface GenericConfig<T> {

        T get();
    }

    /**
     * Outer class containing a static nested configuration interface.
     */
    static class OuterClass {

        interface NestedConfig {

            String nested();
        }
    }

//     interface NestedConfig {

        String nested();
    }

//     interface NestedConfig {

        String nested();
    }

    /**
     * Interface declaring default and static methods – they must not affect the stored type.
     */
    interface DefaultMethodConfig {

        default String defaultMethod() {
            return "default";
        }

        static String staticMethod() {
            return "static";
        }
    }

    /**
     * Interface annotated with {@code @ConfigMapping} to test annotation‑driven naming strategies.
     */
    @ConfigMapping
    interface AnnotatedNamingConfig {

        String annotated();
    }

    @BeforeEach
//     void setupBeforeEach(@Mock ConfigMappingInterface.Property mockProperty) {
        // The constructor expects an array of super‑types (can be empty) and an
        // array of Property objects (can be empty as well).  We use an empty
        // super‑type array and no Property objects – the actual content is
        // irrelevant for the getInterfaceType() test.
        this.suppliedInterfaceClass = DummyConfig.class;
        ConfigMappingInterface.Property[] properties = new ConfigMappingInterface.Property[0];
        this.mapping = new ConfigMappingInterface(suppliedInterfaceClass, ConfigMappingInterface.NO_TYPES, properties);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the reference to help the GC; not strictly necessary but keeps
        // the test tidy.
        mapping = null;
    }

    @Test
    void getInterfaceType_returnsTheClassPassedToConstructor() {
        // When
        Class<?> result = mapping.getInterfaceType();
        // Then
        assertSame(suppliedInterfaceClass, result, "The interface type returned by getInterfaceType() must be the exact class supplied to the constructor.");
    }

    @Test
    void getInterfaceType_isConsistentAcrossMultipleCalls() {
        // When
        Class<?> firstCall = mapping.getInterfaceType();
        Class<?> secondCall = mapping.getInterfaceType();
        // Then
        assertSame(firstCall, secondCall, "Multiple invocations of getInterfaceType() should return the same Class instance.");
        assertEquals(DummyConfig.class.getName(), firstCall.getName(), "The name of the returned class must match the supplied interface's name.");
    }

    @Test
    @DisplayName("Simple interface – returns the same Class object")
    void getInterfaceType_simpleInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        assertSame(SampleConfig.class, cmi.getInterfaceType(), "The returned Class must be exactly the one supplied to the factory.");
    }

    @Test
    @DisplayName("Child interface – returns the concrete child type, not its super‑interface")
    void getInterfaceType_childInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        assertSame(ChildConfig.class, cmi.getInterfaceType(), "For an interface that extends another, the concrete interface type must be returned.");
    }

    @Test
    @DisplayName("Multi‑inheritance interface – returns the concrete child type")
    void getInterfaceType_multiInheritance() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(MultiParentConfig.class);
        assertSame(MultiParentConfig.class, cmi.getInterfaceType(), "Even with several super‑interfaces, the concrete interface type must be returned.");
    }

    @Test
    @DisplayName("Generic interface – returns the raw interface Class")
    void getInterfaceType_genericInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(GenericConfig.class);
        assertSame(GenericConfig.class, cmi.getInterfaceType(), "The raw interface class (without type arguments) must be returned.");
    }

    @Test
    @DisplayName("Static nested interface – returns the nested Class object")
    void getInterfaceType_nestedInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(OuterClass.NestedConfig.class);
        assertSame(OuterClass.NestedConfig.class, cmi.getInterfaceType(), "The nested interface class must be returned unchanged.");
    }

    @Test
    @DisplayName("Interface with default and static methods – returns the interface Class")
    void getInterfaceType_defaultAndStaticMethods() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(DefaultMethodConfig.class);
        assertSame(DefaultMethodConfig.class, cmi.getInterfaceType(), "Default/static methods must not affect the stored interface type.");
    }

    @Test
    @DisplayName("Annotation‑driven naming strategy – returns the interface Class")
    void getInterfaceType_annotationDrivenNaming() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(AnnotatedNamingConfig.class);
        assertSame(AnnotatedNamingConfig.class, cmi.getInterfaceType(), "Annotations should not change the returned interface Class.");
    }

    @Test
    @DisplayName("Non‑interface type (String) – IllegalArgumentException")
    void getInterfaceType_nonInterfaceArgument() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(String.class), "Providing a non‑interface class must result in IllegalArgumentException.");
    }

    @Test
    @DisplayName("Null argument – NullPointerException")
    void getInterfaceType_nullArgument() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getConfigurationInterface(null), "Passing null must result in NullPointerException.");
    }

//     @Test
//     @DisplayName("Annotation type – IllegalArgumentException")
//     void getInterfaceType_annotationInterfaceArgument() {
//         assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(SampleAnnotation.class), "Annotation interfaces are not valid configuration mappings and must be rejected.");
//     }

    @Test
    @DisplayName("Caching – two calls for the same interface return the same ConfigMappingInterface instance")
    void getInterfaceType_cachingConsistency() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        assertSame(first, second, "The ClassValue cache must return the identical ConfigMappingInterface instance.");
        // The interface type obtained from both instances must also be identical.
        assertSame(first.getInterfaceType(), second.getInterfaceType(), "Both cached instances must expose the same interface Class.");
    }

    @Test
    @DisplayName("Repeated getInterfaceType() calls on the same instance return the same Class")
    void getInterfaceType_multipleCallsSameInstance() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        Class<?> first = cmi.getInterfaceType();
        Class<?> second = cmi.getInterfaceType();
        assertSame(first, second, "Multiple invocations must return the same Class object.");
    }
}