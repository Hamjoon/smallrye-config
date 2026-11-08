package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.List;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getProperties_Test {

    // -----------------------------------------------------------------------
    @Mock
    Property // a normal property (not a toString method)
    regularProperty;

    @Mock
    Property // a property that represents a toString method
    toStringProperty;

    /**
     * Simple concrete implementation of {@link ConfigMappingInterface.Property}
     * used for the tests without requiring a mocking framework.
     */
    static class TestProperty extends ConfigMappingInterface.Property {

        private final boolean toStringMethod;

        TestProperty(boolean toStringMethod) throws NoSuchMethodException {
            // Use a dummy method; the actual method is irrelevant because we override
            // {@link #isToStringMethod()}.
            super(Object.class.getMethod("hashCode"), "dummy");
            this.toStringMethod = toStringMethod;
        }

        @Override
        public boolean isToStringMethod() {
            return toStringMethod;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        regularProperty = createProperty(false);
        toStringProperty = createProperty(true);
    }

    /**
     * Helper that creates a {@link ConfigMappingInterface.Property} instance
     * with the desired {@code isToStringMethod()} behaviour.
     */
    private static Property createProperty(final boolean toStringMethod) {
        return new TestProperty(toStringMethod);
    }

    @Test
    void getProperties_returnsEmptyArrayWhenNoPropertiesProvided() {
        // Arrange: an empty property list.
        ConfigMappingInterface mapping = new ConfigMappingInterface(// interface type (dummy)
        Object.class, // no super types
        ConfigMappingInterface.NO_TYPES, // empty property array
        new Property[0]);
        // Act
        Property[] result = mapping.getProperties();
        // Assert
        assertNotNull(result, "Result must never be null");
        assertEquals(0, result.length, "Expected an empty array when no properties are supplied");
    }

    @Test
    void getProperties_filtersOutToStringMethodProperty() {
        // Arrange: one regular property and one toString method property.
        Property[] supplied = new Property[] { regularProperty, toStringProperty };
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, supplied);
        // Act
        Property[] result = mapping.getProperties();
        // Assert
        assertNotNull(result, "Result must never be null");
        assertEquals(1, result.length, "Only the non‑toString property should be kept");
        assertSame(regularProperty, result[0], "The retained property should be the regular one");
    }

    @Test
    void getProperties_preservesOrderOfNonToStringProperties() {
        // Arrange: three regular properties and one toString method property interleaved.
        Property regular1 = createProperty(false);
        Property regular2 = createProperty(false);
        Property regular3 = createProperty(false);
        Property[] supplied = new Property[] { regular1, toStringProperty, regular2, regular3 };
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, supplied);
        // Act
        Property[] result = mapping.getProperties();
        // Assert
        assertEquals(3, result.length, "Only the three regular properties should remain");
        assertArrayEquals(new Property[] { regular1, regular2, regular3 }, result, "The order of the retained properties must be the same as the original order");
    }

    @Test
    void getProperties_multipleCallsReturnSameInstanceArray() {
        // Arrange
        Property[] supplied = new Property[] { regularProperty };
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, supplied);
        // Act
        Property[] firstCall = mapping.getProperties();
        Property[] secondCall = mapping.getProperties();
        // Assert
        assertArrayEquals(firstCall, secondCall, "Subsequent calls must return equal arrays");
        // Not required to be the same reference, but they should be equal in content.
    }

    @Test
    void standardBehavior_nonEmptyProperties_returnsExactSameArrayReference() throws Exception {
        // Arrange: two distinct properties.
        Property p1 = createProperty(false);
        Property p2 = createProperty(false);
        Property[] supplied = new Property[] { p1, p2 };
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, supplied);
        // Act
        Property[] result = mapping.getProperties();
        // Assert: same length, same elements, same reference as the internal field.
        assertEquals(2, result.length);
        assertSame(p1, result[0]);
        assertSame(p2, result[1]);
        // Verify that the returned reference is the exact same object stored internally.
        Field f = ConfigMappingInterface.class.getDeclaredField("properties");
        f.setAccessible(true);
        Property[] internal = (Property[]) f.get(mapping);
        assertSame(internal, result, "Returned array must be the internal field reference");
    }

    @Test
    void emptyProperties_returnsConstantNO_PROPERTIESReference() throws Exception {
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        Property[] result = mapping.getProperties();
        assertNotNull(result);
        assertEquals(0, result.length);
        assertSame(ConfigMappingInterface.NO_PROPERTIES, result, "Empty result should be the shared NO_PROPERTIES constant");
    }

    @Test
    void nullPropertiesViaReflection_returnsNull() throws Exception {
        // Construct with a non‑null array first.
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, new Property[] { regularProperty });
        // Use reflection to set the private final field to null.
        Field f = ConfigMappingInterface.class.getDeclaredField("properties");
        f.setAccessible(true);
        f.set(mapping, null);
        // The method does not guard against null, so we expect null.
        assertNull(mapping.getProperties(), "When internal field is null, getProperties() should return null");
    }

    @Test
    void arrayMutability_returnedArrayIsInternalReference() throws Exception {
        Property original = createProperty(false);
        Property replacement = createProperty(false);
        Property[] supplied = new Property[] { original };
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, supplied);
        // First call – obtain the internal array.
        Property[] first = mapping.getProperties();
        assertSame(original, first[0]);
        // Mutate the array.
        first[0] = replacement;
        // Second call – should see the mutation.
        Property[] second = mapping.getProperties();
        assertSame(replacement, second[0], "Mutation of returned array must be reflected on subsequent calls");
    }

    @Test
    void instanceIsolation_distinctInternalArrays() {
        Property pA = createProperty(false);
        Property pB = createProperty(false);
        Property pC = createProperty(false);
        ConfigMappingInterface first = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, new Property[] { pA });
        ConfigMappingInterface second = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, new Property[] { pB, pC });
        Property[] firstProps = first.getProperties();
        Property[] secondProps = second.getProperties();
        // The arrays must not be the same object reference.
        assertNotSame(firstProps, secondProps);
        // Verify each contains its own elements.
        assertArrayEquals(new Property[] { pA }, firstProps);
        assertArrayEquals(new Property[] { pB, pC }, secondProps);
    }

    @Test
    void propertiesArrayContainingNullElements_preservesNullPositions() throws Exception {
        Property mockProp = createProperty(false);
        Property[] supplied = new Property[] { null, mockProp, null };
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, supplied);
        Property[] result = mapping.getProperties();
        assertEquals(3, result.length);
        assertNull(result[0]);
        assertSame(mockProp, result[1]);
        assertNull(result[2]);
        // Verify reference equality with internal field.
        Field f = ConfigMappingInterface.class.getDeclaredField("properties");
        f.setAccessible(true);
        assertSame(f.get(mapping), result);
    }

    @Test
    void largePropertiesArray_sizeHandling() throws Exception {
        final int SIZE = 1_000;
        Property[] supplied = new Property[SIZE];
        for (int i = 0; i < SIZE; i++) {
            supplied[i] = createProperty(false);
        }
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, supplied);
        Property[] result = mapping.getProperties();
        assertEquals(SIZE, result.length);
        assertSame(supplied, result, "The large array must be returned unchanged");
        // Spot‑check a few positions.
        assertSame(supplied[0], result[0]);
        assertSame(supplied[SIZE / 2], result[SIZE / 2]);
        assertSame(supplied[SIZE - 1], result[SIZE - 1]);
    }

    @Test
    void callingOtherMetadataMethodsDoesNotAffectGetPropertiesReference() throws Exception {
        Property[] supplied = new Property[] { regularProperty };
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, supplied);
        // Capture the original reference.
        Property[] original = mapping.getProperties();
        // Invoke a selection of other public methods.
        // They may return null or empty values – we only care that they do not modify the array.
        mapping.getNamingStrategy();
        mapping.getSuperTypes();
        mapping.getToStringMethod();
        // After each call, the reference must stay the same.
        assertSame(original, mapping.getProperties(), "Reference must stay unchanged after getNamingStrategy()");
        assertSame(original, mapping.getProperties(), "Reference must stay unchanged after getSuperTypes()");
        assertSame(original, mapping.getProperties(), "Reference must stay unchanged after getToStringMethod()");
    }

    @Test
    void getPropertiesBooleanDoesNotAlterInternalArrayReference() throws Exception {
        Property[] supplied = new Property[] { regularProperty };
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, supplied);
        // Capture internal reference via reflection for absolute certainty.
        Field f = ConfigMappingInterface.class.getDeclaredField("properties");
        f.setAccessible(true);
        Property[] internal = (Property[]) f.get(mapping);
        // Call both overloads.
        Property[] withSuper = mapping.getProperties(true);
        Property[] withoutSuper = mapping.getProperties(false);
        // Both must be the exact same array as the internal field.
        assertSame(internal, withSuper, "getProperties(true) must return the internal array");
        assertSame(internal, withoutSuper, "getProperties(false) must return the internal array");
    }
}
