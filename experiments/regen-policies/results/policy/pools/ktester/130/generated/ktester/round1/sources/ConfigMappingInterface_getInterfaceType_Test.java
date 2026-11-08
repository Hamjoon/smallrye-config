package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import io.smallrye.config.ConfigMappingInterface.Property;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getInterfaceType_Test {

    static interface InnerConfig {

        String inner();
    }

    static interface InnerConfig {

        String inner();
    }

    interface InnerConfig {

        String inner();
    }

    interface InnerConfig {

        String inner();
    }

    static interface InnerConfig {

        String inner();
    }

    static interface InnerConfig {

        String inner();
    }

    interface InnerConfig {

        String inner();
    }

    interface InnerConfig {

        String inner();
    }

    static interface InnerConfig {

        String inner();
    }

    static interface InnerConfig {

        String inner();
    }

    // Simple interface – no annotations needed.
    private interface SampleConfig {

        String dummy();
    }

    /**
     * Simple configuration interface – no generics, no inheritance.
     */
    interface SimpleConfig {

        String property();
    }

    /**
     * Generic configuration interface.
     */
    interface GenericConfig<T> {

        T value();
    }

    /**
     * Interface containing a static nested interface.
     */
    interface NestedConfig {

        interface InnerConfig {

            String inner();
        }
    }

    interface InnerConfig {

        String inner();
    }

    interface InnerConfig {

        String inner();
    }

    /**
     * Parent interface for inheritance test.
     */
    interface ParentConfig {

        String parentMethod();
    }

    /**
     * Child interface extending {@link ParentConfig}.
     */
    interface ChildConfig extends ParentConfig {

        String childMethod();
    }

    /**
     * Interface with a default method – should not affect getInterfaceType().
     */
    interface DefaultMethodConfig {

        default String defaultMethod() {
            return "default";
        }

        String foo();
    }

    /**
     * Bounded generic interface.
     */
    interface BoundedGenericConfig<T extends Number> {

        T number();
    }

    /**
     * Generic outer class with a static nested configuration interface.
     */
    static class OuterGenericConfig<T> {

        static interface InnerConfig {

            String inner();
        }
    }

    static interface InnerConfig {

        String inner();
    }

    static interface InnerConfig {

        String inner();
    }

    /**
     * Simple static inner interface used in a few tests.
     */
    static interface StaticInnerConfig {

        String inner();
    }

    /**
     * Verify that the {@code getInterfaceType()} method returns the exact {@code Class}
     * instance that was supplied to the {@link ConfigMappingInterface} constructor.
     */
    @Test
    void getInterfaceType_returnsConstructorArgument(@Mock Property dummyProperty) {
        // given: a target interface class and minimal constructor arguments
        Class<?> expectedInterface = SampleConfig.class;
        ConfigMappingInterface[] superTypes = ConfigMappingInterface.NO_TYPES;
        // No properties are required for this test – an empty array is sufficient.
        Property[] properties = new Property[0];
        // when: creating the metadata instance
        ConfigMappingInterface mapping = new ConfigMappingInterface(expectedInterface, superTypes, properties);
        // then: the getter must return the same class object
        assertSame(expectedInterface, mapping.getInterfaceType(), "getInterfaceType() should return the class supplied to the constructor");
    }

    @Test
    void getInterfaceType_returnsSimpleInterfaceClass() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertSame(SimpleConfig.class, mapping.getInterfaceType(), "Should return the exact class supplied to getConfigurationInterface");
    }

    @Test
    void getInterfaceType_returnsGenericInterfaceClass() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(GenericConfig.class);
        assertSame(GenericConfig.class, mapping.getInterfaceType(), "Generic interface class must be returned unchanged");
    }

    @Test
    void getInterfaceType_returnsNestedInterfaceClass() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(NestedConfig.InnerConfig.class);
        assertSame(NestedConfig.InnerConfig.class, mapping.getInterfaceType(), "Nested interface class must be returned unchanged");
    }

    @Test
    void getInterfaceType_returnsChildInterfaceWhenExtendingParent() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        assertSame(ChildConfig.class, mapping.getInterfaceType(), "Should return the concrete child interface, not its super‑type");
    }

    @Test
    void getInterfaceType_cachingReturnsSameInstance() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertSame(first, second, "ClassValue cache should return the same ConfigMappingInterface instance");
        assertSame(first.getInterfaceType(), second.getInterfaceType(), "Both instances must expose the same Class object");
    }

    @Test
    void getInterfaceType_ignoresDefaultMethods() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(DefaultMethodConfig.class);
        assertSame(DefaultMethodConfig.class, mapping.getInterfaceType(), "Default methods must not affect the returned interface type");
    }

    @Test
    void getInterfaceType_returnsBoundedGenericInterfaceClass() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(BoundedGenericConfig.class);
        assertSame(BoundedGenericConfig.class, mapping.getInterfaceType(), "Bounded generic interface class must be returned unchanged");
    }

    @Test
    void getInterfaceType_returnsStaticNestedInterfaceInsideGenericOuter() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(OuterGenericConfig.InnerConfig.class);
        assertSame(OuterGenericConfig.InnerConfig.class, mapping.getInterfaceType(), "Static nested interface inside a generic outer class must be reported correctly");
    }

    @Test
    void getConfigurationInterface_nonInterface_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(String.class), "Calling getConfigurationInterface on a non‑interface should throw IllegalArgumentException");
    }

    @Test
    void getInterfaceType_nullInternalField_throwsNullPointerException(@Mock Property dummyProperty) throws Exception {
        // Locate the constructor matching (Class, ConfigMappingInterface[], Property[])
        Constructor<ConfigMappingInterface> ctor = null;
        for (Constructor<?> c : ConfigMappingInterface.class.getDeclaredConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length == 3 && params[0] == Class.class && params[1] == ConfigMappingInterface[].class && params[2] == ConfigMappingInterface.Property[].class) {
                //noinspection unchecked
                ctor = (Constructor<ConfigMappingInterface>) c;
                break;
            }
        }
        assertNotNull(ctor, "Expected constructor with (Class, ConfigMappingInterface[], Property[]) signature");
        ctor.setAccessible(true);
        // Create an instance with a null interfaceType
        ConfigMappingInterface instance = ctor.newInstance(null, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        // The getter should now throw NPE because the internal field is null
        assertThrows(NullPointerException.class, instance::getInterfaceType, "Calling getInterfaceType() when internal field is null must throw NPE");
    }
}
