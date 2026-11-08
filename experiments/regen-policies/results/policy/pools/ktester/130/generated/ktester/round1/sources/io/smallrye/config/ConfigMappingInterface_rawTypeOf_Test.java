package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_rawTypeOf_Test {

    /* --------------------------------------------------------------------- */
    private static class SampleTypes {

        List<String> listField;

        Map<String, Integer> mapField;

        List<String>[] genericArrayField;

        String[] stringArrayField;

        List<? extends Number> wildcardNumber;

        List<? extends Collection<String>> wildcardCollection;
    }

    static Stream<Arguments> parameterizedTypeProvider() throws NoSuchFieldException {
        Field listField = SampleTypes.class.getDeclaredField("listField");
        Field mapField = SampleTypes.class.getDeclaredField("mapField");
        return Stream.of(Arguments.of(listField.getGenericType(), List.class), Arguments.of(mapField.getGenericType(), Map.class));
    }

    static Stream<Arguments> genericArrayTypeProvider() throws NoSuchFieldException {
        Field genericArray = SampleTypes.class.getDeclaredField("genericArrayField");
        Field stringArray = SampleTypes.class.getDeclaredField("stringArrayField");
        return Stream.of(// List<String>[]
        Arguments.of(genericArray.getGenericType(), List[].class), // String[]
        Arguments.of(stringArray.getGenericType(), String[].class));
    }

    static Stream<Arguments> wildcardUpperBoundProvider() throws NoSuchFieldException {
        Field wildcardNumber = SampleTypes.class.getDeclaredField("wildcardNumber");
        Field wildcardCollection = SampleTypes.class.getDeclaredField("wildcardCollection");
        // Extract the actual WildcardType arguments
        ParameterizedType ptNumber = (ParameterizedType) wildcardNumber.getGenericType();
        WildcardType wcNumber = (WildcardType) ptNumber.getActualTypeArguments()[0];
        ParameterizedType ptCollection = (ParameterizedType) wildcardCollection.getGenericType();
        WildcardType wcCollection = (WildcardType) ptCollection.getActualTypeArguments()[0];
        return Stream.of(// ? extends Number  -> Number.class
        Arguments.of(wcNumber, Number.class), // ? extends Collection<String> -> Collection.class
        Arguments.of(wcCollection, Collection.class));
    }

    /**
     * Helper that creates a synthetic {@link TypeVariable} implementation.
     * This is only needed when the test class itself does not declare a type
     * parameter (which is the case here).
     */
    private static TypeVariable<Class<SampleTypes>> createArtificialTypeVariable() {
        return new TypeVariable<Class<SampleTypes>>() {

            @Override
            public Type[] getBounds() {
                return new Type[] { Object.class };
            }

            @Override
            public Class<SampleTypes> getGenericDeclaration() {
                return SampleTypes.class;
            }

            @Override
            public String getName() {
                return "T";
            }

            @Override
            public AnnotatedType[] getAnnotatedBounds() {
                return new AnnotatedType[0];
            }
        };
    }

    @Test
    void rawTypeOf_class() {
        Class<?> result = ConfigMappingInterface.rawTypeOf(String.class);
        assertEquals(String.class, result, "rawTypeOf should return the same Class instance for a plain Class");
    }

    @Test
    void rawTypeOf_parameterizedType() throws NoSuchFieldException {
        // Obtain a ParameterizedType from a field declaration
        Field field = SampleTypes.class.getDeclaredField("listField");
        // should be ParameterizedType List<String>
        Type type = field.getGenericType();
        Class<?> result = ConfigMappingInterface.rawTypeOf(type);
        assertEquals(List.class, result);
    }

    @Test
    void rawTypeOf_genericArrayType() throws NoSuchFieldException {
        // Obtain a GenericArrayType from a field declaration
        Field field = SampleTypes.class.getDeclaredField("genericArrayField");
        // should be GenericArrayType List<String>[]
        Type type = field.getGenericType();
        Class<?> result = ConfigMappingInterface.rawTypeOf(type);
        // The raw component type is List, so the resulting class should be List[].class
        assertEquals(List[].class, result);
    }

    @Test
    void rawTypeOf_wildcardType() throws NoSuchFieldException {
        // Obtain a WildcardType from a field declaration
        Field field = SampleTypes.class.getDeclaredField("wildcardNumber");
        // should be ParameterizedType List<? extends Number>
        Type type = field.getGenericType();
        // Extract the wildcard
        ParameterizedType pType = (ParameterizedType) type;
        Type wildcard = pType.getActualTypeArguments()[0];
        Class<?> result = ConfigMappingInterface.rawTypeOf(wildcard);
        assertEquals(Number.class, result);
    }

    @Test
    void rawTypeOf_wildcardWithNoUpperBounds() {
        // Construct a WildcardType with no explicit upper bounds (should default to Object)
        WildcardType wildcard = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return new Type[0];
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        Class<?> result = ConfigMappingInterface.rawTypeOf(wildcard);
        assertEquals(Object.class, result);
    }

    @Test
    void rawTypeOf_unsupportedType_throwsException() {
        // Use a method reference only to have a non‑null Method object; the method itself is irrelevant.
        Method genericMethod = SampleTypes.class.getMethod("toString");
        // SampleTypes has no type parameters, so we use an artificial one
        TypeVariable<?> typeVariable = SampleTypes.class.getTypeParameters().length > 0 ? SampleTypes.class.getTypeParameters()[0] : createArtificialTypeVariable();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ConfigMappingInterface.rawTypeOf(typeVariable));
        // The ConfigMessages.msg.noRawType message contains the type's toString()
        assertTrue(thrown.getMessage().contains(typeVariable.getTypeName()), "Exception message should contain the unsupported type name");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => wildcard={0}, expected={1}")
    @MethodSource("wildcardUpperBoundProvider")
    void rawTypeOf_wildcardWithUpperBound(Type wildcard, Class<?> expected) {
        Class<?> result = ConfigMappingInterface.rawTypeOf(wildcard);
        assertEquals(expected, result, () -> "Expected raw type of wildcard " + wildcard + " to be " + expected);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void rawTypeOf_unboundedWildcard() {
        WildcardType unbounded = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return new Type[0];
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        Class<?> result = ConfigMappingInterface.rawTypeOf(unbounded);
        assertEquals(Object.class, result, "An unbounded wildcard should resolve to Object.class");
    }
}