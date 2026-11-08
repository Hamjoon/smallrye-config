import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.MockitoAnnotations;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getSuperTypes_Test {

    // dummy property, not used by the test
//     @Mock
//     Property mockProperty;

    private AutoCloseable mocks;

//     @ConfigMapping
    interface SimpleConfig {
    }

//     @ConfigMapping
    interface BaseConfig {
    }

//     @ConfigMapping
    interface DerivedConfig extends BaseConfig {
    }

//     @ConfigMapping
    interface FirstParent {
    }

//     @ConfigMapping
    interface SecondParent {
    }

//     @ConfigMapping
    interface MultiParentConfig extends FirstParent, SecondParent {
    }

//     @ConfigMapping
    interface RootConfig {
    }

//     @ConfigMapping
    interface IntermediateConfig extends RootConfig {
    }

//     @ConfigMapping
    interface LeafConfig extends IntermediateConfig {
    }

//     @ConfigMapping
    interface OnlyObjectConfig {
    }

//     @ConfigMapping
    interface ParentA {
    }

//     @ConfigMapping
    interface ParentB extends ParentA {
    }

//     @ConfigMapping
    interface AnotherParent {
    }

//     @ConfigMapping
    interface ChildConfig extends ParentB, AnotherParent {
    }

//     @ConfigMapping
    interface GenericConfig<T> {
    }

//     @ConfigMapping
    interface StringConfig extends GenericConfig<String> {
    }

//     @ConfigMapping
    interface ProxyConfig {
    }

//     @ConfigMapping
    interface A {
    }

//     @ConfigMapping
    interface C {
    }

//     @ConfigMapping
    interface B extends C {
    }

//     @ConfigMapping
    interface E {
    }

//     @ConfigMapping
    interface D extends A, B, E {
    }

//     @ConfigMapping
    interface LocalSimpleConfig {
    }

//     @ConfigMapping
    interface LocalBaseConfig {
    }

//     @ConfigMapping
    interface LocalDerivedConfig extends LocalBaseConfig {
    }

//     @ConfigMapping
    interface LocalFirstParent {
    }

//     @ConfigMapping
    interface LocalSecondParent {
    }

//     @ConfigMapping
    interface LocalMultiParentConfig extends LocalFirstParent, LocalSecondParent {
    }

//     @ConfigMapping
    interface LocalRootConfig {
    }

//     @ConfigMapping
    interface LocalIntermediateConfig extends LocalRootConfig {
    }

//     @ConfigMapping
    interface LocalLeafConfig extends LocalIntermediateConfig {
    }

    class SomeConcreteClass {
    }

//     @ConfigMapping
    interface LocalOnlyObjectConfig {
    }

//     @ConfigMapping
    interface LocalParentA {
    }

//     @ConfigMapping
    interface LocalParentB extends LocalParentA {
    }

//     @ConfigMapping
    interface LocalAnotherParent {
    }

//     @ConfigMapping
    interface LocalChildConfig extends LocalParentB, LocalAnotherParent {
    }

//     @ConfigMapping
    interface LocalGenericConfig<T> {
    }

//     @ConfigMapping
    interface LocalStringConfig extends LocalGenericConfig<String> {
    }

//     @ConfigMapping
    interface LocalProxyConfig {
    }

//     @ConfigMapping
    interface LocalA {
    }

//     @ConfigMapping
    interface LocalC {
    }

//     @ConfigMapping
    interface LocalB extends LocalC {
    }

//     @ConfigMapping
    interface LocalE {
    }

//     @ConfigMapping
    interface LocalD extends LocalA, LocalB, LocalE {
    }

    @ConfigMapping
//     interface SimpleConfig {
    }

    @ConfigMapping
//     interface BaseConfig {
    }

    @ConfigMapping
//     interface DerivedConfig extends BaseConfig {
    }

    @ConfigMapping
//     interface FirstParent {
    }

    @ConfigMapping
//     interface SecondParent {
    }

    @ConfigMapping
//     interface MultiParentConfig extends FirstParent, SecondParent {
    }

    @ConfigMapping
//     interface RootConfig {
    }

    @ConfigMapping
//     interface IntermediateConfig extends RootConfig {
    }

    @ConfigMapping
//     interface LeafConfig extends IntermediateConfig {
    }

    @ConfigMapping
//     interface OnlyObjectConfig {
    }

    @ConfigMapping
//     interface ParentA {
    }

    @ConfigMapping
//     interface ParentB extends ParentA {
    }

    @ConfigMapping
//     interface AnotherParent {
    }

    @ConfigMapping
//     interface ChildConfig extends ParentB, AnotherParent {
    }

    @ConfigMapping
//     interface GenericConfig<T> {
    }

    @ConfigMapping
//     interface StringConfig extends GenericConfig<String> {
    }

    @ConfigMapping
//     interface ProxyConfig {
    }

    @ConfigMapping
//     interface A {
    }

    @ConfigMapping
//     interface C {
    }

    @ConfigMapping
//     interface B extends C {
    }

    @ConfigMapping
//     interface E {
    }

    @ConfigMapping
//     interface D extends A, B, E {
    }

    interface SuperA {
    }

    interface SuperB {
    }

    interface Child {
    }

    @ConfigMapping
//     interface LocalSimpleConfig {
    }

    @ConfigMapping
//     interface LocalBaseConfig {
    }

    @ConfigMapping
//     interface LocalDerivedConfig extends LocalBaseConfig {
    }

    @ConfigMapping
//     interface LocalFirstParent {
    }

    @ConfigMapping
//     interface LocalSecondParent {
    }

    @ConfigMapping
//     interface LocalMultiParentConfig extends LocalFirstParent, LocalSecondParent {
    }

    @ConfigMapping
//     interface LocalRootConfig {
    }

    @ConfigMapping
//     interface LocalIntermediateConfig extends LocalRootConfig {
    }

    @ConfigMapping
//     interface LocalLeafConfig extends LocalIntermediateConfig {
    }

//     class SomeConcreteClass {
    }

    @ConfigMapping
//     interface LocalOnlyObjectConfig {
    }

    @ConfigMapping
//     interface LocalParentA {
    }

    @ConfigMapping
//     interface LocalParentB extends LocalParentA {
    }

    @ConfigMapping
//     interface LocalAnotherParent {
    }

    @ConfigMapping
//     interface LocalChildConfig extends LocalParentB, LocalAnotherParent {
    }

    @ConfigMapping
//     interface LocalGenericConfig<T> {
    }

    @ConfigMapping
//     interface LocalStringConfig extends LocalGenericConfig<String> {
    }

    @ConfigMapping
//     interface LocalProxyConfig {
    }

    @ConfigMapping
//     interface LocalA {
    }

    @ConfigMapping
//     interface LocalC {
    }

    @ConfigMapping
//     interface LocalB extends LocalC {
    }

    @ConfigMapping
//     interface LocalE {
    }

    @ConfigMapping
//     interface LocalD extends LocalA, LocalB, LocalE {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (required for the @ExtendWith support).
//         mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release mock resources.
        mocks.close();
    }

    /**
     * Helper method used in other tests to create a simple {@link ConfigMappingInterface}
     * with the supplied super‑type array.
     */
//     private ConfigMappingInterface createMapping(Class<?> iface, ConfigMappingInterface[] supers) {
//         // No properties are required for the super‑type behaviour tests,
//         // so we pass an empty array.
//         return new ConfigMappingInterface(iface, supers, new ConfigMappingInterface.Property[0]);
//     }

    /**
     * Verify that when a ConfigMappingInterface is created with no super‑types,
     * {@code getSuperTypes()} returns an empty (non‑null) array.
     */
    @Test
    void getSuperTypes_emptyArray() {
        ConfigMappingInterface emptySuper = new ConfigMappingInterface(ConfigMappingInterface_getSuperTypes_Test.Child.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface[] result = emptySuper.getSuperTypes();
        assertNotNull(result, "Super‑type array must never be null");
        assertEquals(0, result.length, "Super‑type array should be empty");
        // The implementation returns the exact field reference – changing the result
        // should affect the internal state. This behaviour is intentional.
        assertSame(ConfigMappingInterface.NO_TYPES, result, "When created with NO_TYPES the same constant array instance should be returned");
    }

    /**
     * Verify that when a ConfigMappingInterface is created with a non‑empty super‑type list,
     * {@code getSuperTypes()} returns that exact array.
     */
    @Test
    void getSuperTypes_nonEmptyArray() {
        ConfigMappingInterface superA = new ConfigMappingInterface(ConfigMappingInterface_getSuperTypes_Test.SuperA.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface superB = new ConfigMappingInterface(ConfigMappingInterface_getSuperTypes_Test.SuperB.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface[] superArray = new ConfigMappingInterface[] { superA, superB };
        ConfigMappingInterface child = new ConfigMappingInterface(ConfigMappingInterface_getSuperTypes_Test.Child.class, superArray, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface[] result = child.getSuperTypes();
        assertNotNull(result, "Result must not be null");
        assertEquals(2, result.length, "There should be exactly two super‑types");
        assertSame(superA, result[0], "First super‑type should be the same instance supplied to the constructor");
        assertSame(superB, result[1], "Second super‑type should be the same instance supplied to the constructor");
        // Demonstrate that the returned array is the internal one.
        result[0] = null;
        ConfigMappingInterface[] afterModification = child.getSuperTypes();
        assertNull(afterModification[0], "Modifying the returned array should reflect in subsequent calls");
    }

    /**
     * Verify that {@code getSuperTypes()} can be called repeatedly and always returns the same array instance.
     */
    @Test
    void getSuperTypes_consistentReference() {
        ConfigMappingInterface[] empty = ConfigMappingInterface.NO_TYPES;
        ConfigMappingInterface mapping = new ConfigMappingInterface(ConfigMappingInterface_getSuperTypes_Test.Child.class, empty, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface[] firstCall = mapping.getSuperTypes();
        ConfigMappingInterface[] secondCall = mapping.getSuperTypes();
        assertSame(firstCall, secondCall, "Multiple invocations should return the same array instance");
    }

    /**
     * Demonstrates that the method works correctly when the super‑type array contains
     * duplicated references (the method does not deduplicate or filter them).
     */
    @Test
    void getSuperTypes_arrayWithDuplicates() {
        ConfigMappingInterface superA = createMapping(ConfigMappingInterface_getSuperTypes_Test.SuperA.class, ConfigMappingInterface.NO_TYPES);
        ConfigMappingInterface[] dupArray = new ConfigMappingInterface[] { superA, superA };
        ConfigMappingInterface child = createMapping(ConfigMappingInterface_getSuperTypes_Test.Child.class, dupArray);
        ConfigMappingInterface[] result = child.getSuperTypes();
        assertEquals(2, result.length);
        assertSame(superA, result[0]);
        assertSame(superA, result[1]);
    }

    /**
     * Verify that a configuration interface without any declared super‑interfaces
     * returns an empty array.
     */
    @Test
    void getSuperTypes_noExplicitSuperInterfaces() {
        @ConfigMapping
        interface LocalSimpleConfig {
        }
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(LocalSimpleConfig.class);
        ConfigMappingInterface[] supers = cfg.getSuperTypes();
        assertNotNull(supers);
        assertEquals(0, supers.length, "Expected no super‑types");
    }

    /**
     * Validate that a configuration interface extending a single super‑interface
     * returns that super‑interface.
     */
    @Test
    void getSuperTypes_singleSuperInterface() {
        @ConfigMapping
        interface LocalBaseConfig {
        }
        @ConfigMapping
        interface LocalDerivedConfig extends LocalBaseConfig {
        }
        ConfigMappingInterface derived = ConfigMappingInterface.getConfigurationInterface(LocalDerivedConfig.class);
        ConfigMappingInterface[] supers = derived.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(LocalBaseConfig.class, supers[0].getInterfaceType(), "The sole super‑type should correspond to LocalBaseConfig");
    }

    /**
     * Check handling of multiple inheritance.
     */
    @Test
    void getSuperTypes_multipleSuperInterfaces() {
        @ConfigMapping
        interface LocalFirstParent {
        }
        @ConfigMapping
        interface LocalSecondParent {
        }
        @ConfigMapping
        interface LocalMultiParentConfig extends LocalFirstParent, LocalSecondParent {
        }
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(LocalMultiParentConfig.class);
        ConfigMappingInterface[] supers = cfg.getSuperTypes();
        assertEquals(2, supers.length, "Expected two direct super‑interfaces");
        Set<Class<?>> expected = Set.of(LocalFirstParent.class, LocalSecondParent.class);
        Set<Class<?>> actual = Set.of(supers[0].getInterfaceType(), supers[1].getInterfaceType());
        assertEquals(expected, actual, "Super‑type set does not match expected interfaces");
    }

    /**
     * Ensure that only direct super‑interfaces are reported (transitive interfaces are ignored).
     */
    @Test
    void getSuperTypes_transitiveSuperInterfaceNotReturned() {
        @ConfigMapping
        interface LocalRootConfig {
        }
        @ConfigMapping
        interface LocalIntermediateConfig extends LocalRootConfig {
        }
        @ConfigMapping
        interface LocalLeafConfig extends LocalIntermediateConfig {
        }
        ConfigMappingInterface leaf = ConfigMappingInterface.getConfigurationInterface(LocalLeafConfig.class);
        ConfigMappingInterface[] supers = leaf.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(LocalIntermediateConfig.class, supers[0].getInterfaceType(), "Only the direct super‑interface should be present");
    }

    /**
     * Confirm that the utility method guards against non‑interface types.
     */
    @Test
    void getSuperTypes_nonInterfaceThrows() {
        class SomeConcreteClass {
        }
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(SomeConcreteClass.class), "Expected IllegalArgumentException for non‑interface class");
    }

    /**
     * Even though every interface implicitly extends {@code java.lang.Object},
     * {@code getSuperTypes()} must not include {@code Object}.
     */
    @Test
    void getSuperTypes_implicitObjectNotReturned() {
        @ConfigMapping
        interface LocalOnlyObjectConfig {
        }
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(LocalOnlyObjectConfig.class);
        ConfigMappingInterface[] supers = cfg.getSuperTypes();
        assertEquals(0, supers.length, "Object should never appear in the super‑type array");
    }

    /**
     * Verify that only direct super‑interfaces are returned when a parent itself
     * extends another interface.
     */
    @Test
    void getSuperTypes_directSuperInterfacesOnly() {
        @ConfigMapping
        interface LocalParentA {
        }
        @ConfigMapping
        interface LocalParentB extends LocalParentA {
        }
        @ConfigMapping
        interface LocalAnotherParent {
        }
        @ConfigMapping
        interface LocalChildConfig extends LocalParentB, LocalAnotherParent {
        }
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(LocalChildConfig.class);
        ConfigMappingInterface[] supers = child.getSuperTypes();
        assertEquals(2, supers.length);
        Set<Class<?>> expected = Set.of(LocalParentB.class, LocalAnotherParent.class);
        Set<Class<?>> actual = Set.of(supers[0].getInterfaceType(), supers[1].getInterfaceType());
        assertEquals(expected, actual);
    }

    /**
     * Generic inheritance must not duplicate the same raw super‑interface.
     */
    @Test
    void getSuperTypes_genericSuperInterface() {
        @ConfigMapping
        interface LocalGenericConfig<T> {
        }
        @ConfigMapping
        interface LocalStringConfig extends LocalGenericConfig<String> {
        }
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(LocalStringConfig.class);
        ConfigMappingInterface[] supers = cfg.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(LocalGenericConfig.class, supers[0].getInterfaceType(), "The raw generic super‑interface should be reported once");
    }

    /**
     * Proxy‑generated interfaces (runtime) should behave like normal interfaces.
     * The proxy class itself is a concrete class, so we obtain the mapping via the
     * original interface type.
     */
    @Test
    void getSuperTypes_proxyGeneratedInterface() {
        @ConfigMapping
        interface LocalProxyConfig {
        }
        // Create a dummy proxy instance – the instance itself is not used.
        Proxy.newProxyInstance(LocalProxyConfig.class.getClassLoader(), new Class<?>[] { LocalProxyConfig.class }, (p, m, args) -> null);
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(LocalProxyConfig.class);
        ConfigMappingInterface[] supers = cfg.getSuperTypes();
        assertEquals(0, supers.length, "ProxyConfig has no explicit super‑interfaces");
    }

    /**
     * Interface extending three others, where one of them itself extends another.
     * Only direct super‑interfaces should be present.
     */
    @Test
    void getSuperTypes_complexHierarchy() {
        @ConfigMapping
        interface LocalA {
        }
        @ConfigMapping
        interface LocalC {
        }
        @ConfigMapping
        interface LocalB extends LocalC {
        }
        @ConfigMapping
        interface LocalE {
        }
        @ConfigMapping
        interface LocalD extends LocalA, LocalB, LocalE {
        }
        ConfigMappingInterface d = ConfigMappingInterface.getConfigurationInterface(LocalD.class);
        ConfigMappingInterface[] supers = d.getSuperTypes();
        assertEquals(3, supers.length);
        Set<Class<?>> expected = Set.of(LocalA.class, LocalB.class, LocalE.class);
        Set<Class<?>> actual = Set.of(supers[0].getInterfaceType(), supers[1].getInterfaceType(), supers[2].getInterfaceType());
        assertEquals(expected, actual, "Only direct super‑interfaces should be returned");
    }

    /**
     * Calling {@code getSuperTypes()} on a {@code null} reference must throw a
     * {@link NullPointerException}.
     */
    @Test
    void getSuperTypes_nullReferenceThrowsNPE() {
        ConfigMappingInterface nullRef = null;
        assertThrows(NullPointerException.class, () -> nullRef.getSuperTypes(), "Invoking getSuperTypes() on null should throw NPE");
    }
}