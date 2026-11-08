package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getSuperTypes_Test {

    /**
     * A reusable empty {@code Property[]} array – the constructor does not inspect the contents for these tests.
     */
    private static ConfigMappingInterface.Property[] EMPTY_PROPERTIES;

    /**
     * Dummy interface used as the {@code interfaceType} argument for the constructor.
     */
    interface Dummy {
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Create an empty Property array once for the whole test suite.
        EMPTY_PROPERTIES = new ConfigMappingInterface.Property[0];
    }

    /**
     * Verify that when the constructor is given an empty super‑type array,
     * {@code getSuperTypes()} returns an empty array.
     */
    @Test
    void getSuperTypes_emptyArray() {
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        ConfigMappingInterface[] result = cfg.getSuperTypes();
        assertNotNull(result, "Result must not be null");
        assertEquals(0, result.length, "Super‑type array should be empty");
    }

    /**
     * Verify that when the constructor is given a non‑empty super‑type array,
     * {@code getSuperTypes()} returns an array containing the same elements in the same order.
     */
    @Test
    void getSuperTypes_multipleSuperTypes() {
        // Create two dummy super‑type ConfigMappingInterface instances.
        ConfigMappingInterface super1 = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        ConfigMappingInterface super2 = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        ConfigMappingInterface[] supers = new ConfigMappingInterface[] { super1, super2 };
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, supers, EMPTY_PROPERTIES);
        ConfigMappingInterface[] result = cfg.getSuperTypes();
        assertNotNull(result, "Result must not be null");
        assertEquals(2, result.length, "Super‑type array should contain two elements");
        assertSame(super1, result[0], "First element should be the first super‑type supplied");
        assertSame(super2, result[1], "Second element should be the second super‑type supplied");
    }

    /**
     * Ensure that the returned array is the exact same instance that was passed to the constructor.
     * This behaviour is part of the current implementation and is useful for callers that rely on
     * reference equality.
     */
    @Test
    void getSuperTypes_returnsSameReference() {
        ConfigMappingInterface[] supers = new ConfigMappingInterface[] { new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES) };
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, supers, EMPTY_PROPERTIES);
        assertSame(supers, cfg.getSuperTypes(), "The returned array should be the same reference that was supplied to the constructor");
    }

    /**
     * Group: Empty super types (using the constant {@code NO_TYPES}).
     *
     * The constructor receives {@code ConfigMappingInterface.NO_TYPES}.  The method must return
     * the same empty constant array.
     */
    @Test
    void emptySuperTypes_usingConstantNoTypes() {
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, ConfigMappingInterface.NO_TYPES, EMPTY_PROPERTIES);
        ConfigMappingInterface[] result = cfg.getSuperTypes();
        assertNotNull(result, "Result must not be null");
        assertSame(ConfigMappingInterface.NO_TYPES, result, "Result should be the exact same constant array (reference equality)");
        assertEquals(0, result.length, "Length must be zero for NO_TYPES");
    }

    /**
     * Group: Multiple super types (injected via reflection).
     *
     * This test demonstrates that we can set the private {@code superTypes} field after construction
     * and that {@code getSuperTypes()} returns the same array with the exact order.
     */
    @Test
    void multipleSuperTypes_injectedViaReflection() throws Exception {
        // Create a ConfigMappingInterface with a dummy superTypes array (will be overwritten).
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        ConfigMappingInterface parent1 = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        ConfigMappingInterface parent2 = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        ConfigMappingInterface[] injected = new ConfigMappingInterface[] { parent1, parent2 };
        // Inject the custom array into the private final field.
        Field superTypesField = ConfigMappingInterface.class.getDeclaredField("superTypes");
        superTypesField.setAccessible(true);
        superTypesField.set(cfg, injected);
        ConfigMappingInterface[] result = cfg.getSuperTypes();
        assertNotNull(result, "Result must not be null");
        assertSame(injected, result, "Returned array must be the same reference injected via reflection");
        assertEquals(2, result.length, "Array length should be 2");
        assertSame(parent1, result[0], "First element must be parent1");
        assertSame(parent2, result[1], "Second element must be parent2");
    }

    /**
     * Group: Reference equality with custom superTypes array.
     *
     * Verifies that the method returns the exact same array instance that was stored internally.
     */
    @Test
    void referenceEquality_customArray() {
        ConfigMappingInterface parent = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        ConfigMappingInterface[] customArray = new ConfigMappingInterface[] { parent };
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, customArray, EMPTY_PROPERTIES);
        assertSame(customArray, cfg.getSuperTypes(), "Returned array must be the same reference as the custom array supplied to the constructor");
    }

    /**
     * Group: Reference equality with constant {@code NO_TYPES}.
     *
     * The method should return the same constant reference when the internal field holds {@code NO_TYPES}.
     */
    @Test
    void referenceEquality_constantNoTypes() {
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, ConfigMappingInterface.NO_TYPES, EMPTY_PROPERTIES);
        assertSame(ConfigMappingInterface.NO_TYPES, cfg.getSuperTypes(), "Returned array must be the same reference as ConfigMappingInterface.NO_TYPES");
    }

    /**
     * Group: Modification of returned array reflects internal state.
     *
     * The implementation returns the internal array directly (no defensive copy).  Mutating the
     * returned array must affect subsequent calls.
     */
    @Test
    void modificationOfReturnedArrayIsVisible() {
        ConfigMappingInterface parent = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        ConfigMappingInterface[] mutable = new ConfigMappingInterface[] { parent };
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, mutable, EMPTY_PROPERTIES);
        // First call – obtain the internal array reference.
        ConfigMappingInterface[] first = cfg.getSuperTypes();
        assertSame(mutable, first, "First call should return the mutable array");
        // Mutate the array.
        first[0] = null;
        // Second call – the change must be reflected.
        ConfigMappingInterface[] second = cfg.getSuperTypes();
        assertSame(mutable, second, "Second call should still return the same mutable array");
        assertNull(second[0], "The element should be null after mutation");
    }

    /**
     * Group: Super types array containing {@code null} entries.
     *
     * The method must return the array unchanged, preserving {@code null} entries.
     */
    @Test
    void superTypesArrayContainingNullEntries() {
        ConfigMappingInterface parent = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        ConfigMappingInterface[] withNull = new ConfigMappingInterface[] { null, parent };
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, withNull, EMPTY_PROPERTIES);
        ConfigMappingInterface[] result = cfg.getSuperTypes();
        assertNotNull(result, "Result must not be null");
        assertSame(withNull, result, "Returned array should be the same reference");
        assertEquals(2, result.length, "Array length must be 2");
        assertNull(result[0], "First element should be null");
        assertSame(parent, result[1], "Second element should be the parent instance");
    }

    /**
     * Group: Null internal {@code superTypes} field.
     *
     * Using reflection we set the private {@code superTypes} field to {@code null}
     * and verify that invoking {@code getSuperTypes()} throws a {@link NullPointerException}.
     */
    @Test
    void nullInternalSuperTypesField_throwsNPE() throws Exception {
        ConfigMappingInterface cfg = new ConfigMappingInterface(Dummy.class, new ConfigMappingInterface[0], EMPTY_PROPERTIES);
        // Force the private field to null.
        Field superTypesField = ConfigMappingInterface.class.getDeclaredField("superTypes");
        superTypesField.setAccessible(true);
        superTypesField.set(cfg, null);
        assertThrows(NullPointerException.class, cfg::getSuperTypes, "Calling getSuperTypes() when the internal array is null should throw NPE");
    }
}