package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getSuperTypes_Test {

    // ----------------------------------------------------------------------
    @Test
    void getSuperTypes_returnsEmptyArrayWhenNoSuperTypesProvided() {
        // GIVEN a ConfigMappingInterface constructed with the constant NO_TYPES
        ConfigMappingInterface configMapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN we retrieve the super types
        ConfigMappingInterface[] result = configMapping.getSuperTypes();
        // THEN the result should be the exact same empty array instance
        assertSame(ConfigMappingInterface.NO_TYPES, result, "The returned array should be the same instance as the one supplied to the constructor");
        assertEquals(0, result.length, "The array length must be 0");
    }

    @Test
    void getSuperTypes_returnsSingleSuperTypePreservingReference() {
        // GIVEN a super‑type instance
        ConfigMappingInterface superType = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // AND a ConfigMappingInterface that declares this single super type
        ConfigMappingInterface[] superArray = new ConfigMappingInterface[] { superType };
        ConfigMappingInterface configMapping = new ConfigMappingInterface(Object.class, superArray, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN we retrieve the super types
        ConfigMappingInterface[] result = configMapping.getSuperTypes();
        // THEN the returned array should be the exact same instance we passed in
        assertSame(superArray, result, "The returned array must be the same instance that was supplied to the constructor");
        assertArrayEquals(superArray, result, "The content of the array must match the supplied super type");
    }

    @Test
    void getSuperTypes_returnsMultipleSuperTypesInGivenOrder() {
        // GIVEN two distinct super‑type instances
        ConfigMappingInterface superType1 = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface superType2 = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // AND a ConfigMappingInterface that declares both super types in a specific order
        ConfigMappingInterface[] superArray = new ConfigMappingInterface[] { superType1, superType2 };
        ConfigMappingInterface configMapping = new ConfigMappingInterface(Object.class, superArray, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN we retrieve the super types
        ConfigMappingInterface[] result = configMapping.getSuperTypes();
        // THEN the returned array must be the same instance and preserve the order
        assertSame(superArray, result, "The returned array must be the same instance that was supplied to the constructor");
        assertArrayEquals(superArray, result, "The order of super types must be preserved");
    }

    @Test
    void getSuperTypes_isMutableBecauseConstructorKeepsReference() {
        // GIVEN a super‑type array that we can mutate after construction
        ConfigMappingInterface superType = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface[] mutableArray = new ConfigMappingInterface[] { superType };
        ConfigMappingInterface configMapping = new ConfigMappingInterface(Object.class, mutableArray, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN we replace the first element in the original array
        ConfigMappingInterface anotherSuper = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        mutableArray[0] = anotherSuper;
        // THEN the change is reflected in the result of getSuperTypes()
        ConfigMappingInterface[] result = configMapping.getSuperTypes();
        assertSame(anotherSuper, result[0], "Because the constructor stores the array reference, external mutation is visible");
    }

    /**
     * Verify that when an interface extends only non‑ConfigMapping super‑interfaces
     * (e.g. {@code java.io.Serializable}), the constructor is expected to receive an
     * empty array, therefore {@code getSuperTypes()} returns an empty array.
     */
    @Test
    void getSuperTypes_nonConfigSuperInterface_returnsEmptyArray() {
        // GIVEN a ConfigMappingInterface built with NO_TYPES because the only
        // super‑interface is not a ConfigMapping interface.
        ConfigMappingInterface configMapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN
        ConfigMappingInterface[] result = configMapping.getSuperTypes();
        // THEN
        assertNotNull(result, "Result must never be null");
        assertEquals(0, result.length, "No ConfigMapping super‑interfaces should be present");
    }

    /**
     * Generic super‑interfaces are treated the same as regular ones – they are
     * supplied by the caller as an array.  This test ensures the order is kept.
     */
    @Test
    void getSuperTypes_genericSuperInterfaces_preserveDeclarationOrder() {
        // GIVEN two generic ConfigMappingInterface instances
        ConfigMappingInterface genericBase = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface simpleBase = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // AND they are declared in the order: GenericBase, SimpleBase
        ConfigMappingInterface[] superArray = new ConfigMappingInterface[] { genericBase, simpleBase };
        ConfigMappingInterface configMapping = new ConfigMappingInterface(Object.class, superArray, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN
        ConfigMappingInterface[] result = configMapping.getSuperTypes();
        // THEN
        assertSame(superArray, result, "Array reference must be preserved");
        assertArrayEquals(superArray, result, "Declaration order must be preserved");
    }

    /**
     * When an interface extends both a ConfigMapping interface and a regular Java
     * interface, the constructor should have filtered out the non‑ConfigMapping
     * one, leaving only the ConfigMapping super‑type.
     */
    @Test
    void getSuperTypes_mixedSuperInterfaces_omitsNonConfig() {
        // GIVEN a ConfigMapping super‑type (ConfigBase)
        ConfigMappingInterface configBase = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // AND the other super‑interface (java.lang.Runnable) is ignored, so we pass
        // only the ConfigMapping one to the constructor.
        ConfigMappingInterface[] superArray = new ConfigMappingInterface[] { configBase };
        ConfigMappingInterface configMapping = new ConfigMappingInterface(Object.class, superArray, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN
        ConfigMappingInterface[] result = configMapping.getSuperTypes();
        // THEN
        assertEquals(1, result.length, "Only the ConfigMapping super‑type should be present");
        assertSame(configBase, result[0], "The retained element must be the ConfigMapping super‑type");
    }

    /**
     * Direct super‑type exposure only – even if the direct super‑type itself has a
     * ConfigMapping super‑type, {@code getSuperTypes()} must expose only the direct
     * one.
     */
    @Test
    void getSuperTypes_multipleLevelsDirectOnly_exposesOnlyImmediate() {
        // GIVEN a direct parent ConfigMappingInterface
        ConfigMappingInterface directParent = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // The indirect grand‑parent is *not* supplied to the constructor because the
        // metadata building logic would have omitted it.
        ConfigMappingInterface[] superArray = new ConfigMappingInterface[] { directParent };
        ConfigMappingInterface configMapping = new ConfigMappingInterface(Object.class, superArray, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN
        ConfigMappingInterface[] result = configMapping.getSuperTypes();
        // THEN
        assertEquals(1, result.length, "Only the immediate ConfigMapping super‑type should be returned");
        assertSame(directParent, result[0], "The returned element must be the direct parent");
    }

    /**
     * Verify that the order of super‑interfaces matches the textual declaration
     * order, even when the source code declares them in reverse order.
     */
    @Test
    void getSuperTypes_orderPreservationReverseDeclaration_respectsDeclarationOrder() {
        // GIVEN two super‑type instances where the declaration order is B then A
        ConfigMappingInterface superB = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface superA = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // The array reflects the order B, A (as declared in the source)
        ConfigMappingInterface[] superArray = new ConfigMappingInterface[] { superB, superA };
        ConfigMappingInterface configMapping = new ConfigMappingInterface(Object.class, superArray, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN
        ConfigMappingInterface[] result = configMapping.getSuperTypes();
        // THEN
        assertArrayEquals(superArray, result, "Returned order must follow the declaration order (B, A)");
    }

    /**
     * Sanity check that {@code getSuperTypes()} never returns {@code null}.
     */
    @Test
    void getSuperTypes_arrayNotNull_alwaysReturnsArray() {
        // GIVEN a ConfigMappingInterface with an empty super‑type array
        ConfigMappingInterface empty = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // AND another instance with a non‑empty array
        ConfigMappingInterface someSuper = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface[] nonEmptyArray = new ConfigMappingInterface[] { someSuper };
        ConfigMappingInterface nonEmpty = new ConfigMappingInterface(Object.class, nonEmptyArray, ConfigMappingInterface.NO_PROPERTIES);
        // WHEN
        ConfigMappingInterface[] resultEmpty = empty.getSuperTypes();
        ConfigMappingInterface[] resultNonEmpty = nonEmpty.getSuperTypes();
        // THEN
        assertNotNull(resultEmpty, "Empty case must not return null");
        assertNotNull(resultNonEmpty, "Non‑empty case must not return null");
    }
}