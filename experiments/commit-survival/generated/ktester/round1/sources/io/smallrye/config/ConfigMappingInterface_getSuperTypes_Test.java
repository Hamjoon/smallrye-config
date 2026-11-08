package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import org.junit.jupiter.api.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getSuperTypes_Test {

    /**
     * Holds the instance that will be the subject under test.
     */
    private ConfigMappingInterface subject;

    /**
     * Holds a super‑type instance that will be referenced from {@code subject}.
     */
    private ConfigMappingInterface superType1;

    /**
     * Holds a second super‑type instance for multi‑super‑type scenarios.
     */
    private ConfigMappingInterface superType2;

    /**
     * A simple marker interface used as the {@code interfaceType} argument.
     */
    interface DummyConfig {
    }

    /**
     * Another marker interface used for a second dummy type.
     */
    interface AnotherDummyConfig {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create two independent super‑type ConfigMappingInterface objects.
        superType1 = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, new Property[0]);
        superType2 = new ConfigMappingInterface(AnotherDummyConfig.class, ConfigMappingInterface.NO_TYPES, new Property[0]);
        // The subject under test initially has no super‑types; individual tests
        // will replace it with the desired configuration.
        subject = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, new Property[0]);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help the garbage collector; not strictly required.
        subject = null;
        superType1 = null;
        superType2 = null;
    }

    @Test
    void getSuperTypes_returnsEmptyArrayWhenNoSuperTypesDefined() {
        // The subject was constructed with ConfigMappingInterface.NO_TYPES.
        ConfigMappingInterface[] result = subject.getSuperTypes();
        // The returned array should be the exact same reference as the one passed
        // to the constructor (i.e., the constant NO_TYPES).
        assertSame(ConfigMappingInterface.NO_TYPES, result, "Expected the empty NO_TYPES constant to be returned");
        // It should also report a length of zero.
        assertEquals(0, result.length, "Empty super‑type array should have length 0");
    }

    @Test
    void getSuperTypes_returnsSingleSuperType() {
        // Re‑create the subject with a single super‑type.
        subject = new ConfigMappingInterface(DummyConfig.class, new ConfigMappingInterface[] { superType1 }, new Property[0]);
        ConfigMappingInterface[] result = subject.getSuperTypes();
        // The array should contain exactly one element and that element must be
        // the same instance we supplied.
        assertEquals(1, result.length, "Expected exactly one super‑type");
        assertSame(superType1, result[0], "The returned super‑type should be the same instance supplied");
    }

    @Test
    void getSuperTypes_returnsMultipleSuperTypesInOrder() {
        // Build a subject with two super‑types.
        ConfigMappingInterface[] superArray = new ConfigMappingInterface[] { superType1, superType2 };
        subject = new ConfigMappingInterface(DummyConfig.class, superArray, new Property[0]);
        ConfigMappingInterface[] result = subject.getSuperTypes();
        // Verify the returned array is the same reference (no defensive copy).
        assertSame(superArray, result, "The returned array should be the exact reference supplied to the constructor");
        // Verify both elements are present and in the correct order.
        assertEquals(2, result.length, "Expected two super‑types");
        assertSame(superType1, result[0], "First super‑type should match the first supplied instance");
        assertSame(superType2, result[1], "Second super‑type should match the second supplied instance");
    }

    @Test
    void modifyingReturnedArrayAffectsInternalState() {
        // This test demonstrates that {@code getSuperTypes()} returns the
        // internal array directly (no defensive copy). Modifying the returned
        // array therefore changes the state observed via subsequent calls.
        ConfigMappingInterface[] superArray = new ConfigMappingInterface[] { superType1 };
        subject = new ConfigMappingInterface(DummyConfig.class, superArray, new Property[0]);
        // Retrieve the array and modify it.
        ConfigMappingInterface[] retrieved = subject.getSuperTypes();
        // replace the only element
        retrieved[0] = superType2;
        // A second call should see the mutated content.
        ConfigMappingInterface[] afterMutation = subject.getSuperTypes();
        assertSame(superType2, afterMutation[0], "Because the internal array is exposed, a mutation should be visible on subsequent calls");
    }

    @Test
    void getSuperTypes_returnsArrayWithMockitoMocks() {
        // Create two Mockito mocks of ConfigMappingInterface.
        ConfigMappingInterface mock1 = mock(ConfigMappingInterface.class);
        ConfigMappingInterface mock2 = mock(ConfigMappingInterface.class);
        ConfigMappingInterface[] mockArray = new ConfigMappingInterface[] { mock1, mock2 };
        subject = new ConfigMappingInterface(DummyConfig.class, mockArray, new Property[0]);
        ConfigMappingInterface[] result = subject.getSuperTypes();
        // The exact same array reference should be returned.
        assertSame(mockArray, result, "The returned array should be the same reference supplied");
        // Verify the mocks are present and keep their order.
        assertSame(mock1, result[0], "First mock should be returned as first element");
        assertSame(mock2, result[1], "Second mock should be returned as second element");
    }

    @Test
    void getSuperTypes_returnsNullWhenFieldIsNull() throws Exception {
        // Force the private superTypes field to null via reflection.
        java.lang.reflect.Field superTypesField = ConfigMappingInterface.class.getDeclaredField("superTypes");
        superTypesField.setAccessible(true);
        superTypesField.set(subject, null);
        // The getter should simply return null without throwing.
        ConfigMappingInterface[] result = subject.getSuperTypes();
        assertNull(result, "When the internal superTypes field is null, the getter should return null");
    }

    @Test
    void getSuperTypes_returnsArrayWithPlainInstances() {
        // Use plain ConfigMappingInterface instances (no Mockito).
        ConfigMappingInterface instance1 = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, new Property[0]);
        ConfigMappingInterface instance2 = new ConfigMappingInterface(AnotherDummyConfig.class, ConfigMappingInterface.NO_TYPES, new Property[0]);
        ConfigMappingInterface[] array = new ConfigMappingInterface[] { instance1, instance2 };
        subject = new ConfigMappingInterface(DummyConfig.class, array, new Property[0]);
        ConfigMappingInterface[] result = subject.getSuperTypes();
        // The exact same array reference should be returned.
        assertSame(array, result, "The returned array should be the same reference supplied");
        // Verify the instances are present and keep their order.
        assertSame(instance1, result[0], "First instance should be returned as first element");
        assertSame(instance2, result[1], "Second instance should be returned as second element");
    }
}