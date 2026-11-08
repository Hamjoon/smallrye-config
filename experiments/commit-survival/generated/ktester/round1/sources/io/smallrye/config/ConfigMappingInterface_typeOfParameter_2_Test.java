package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_typeOfParameter_2_Test {

    /* ---------------------------------------------------------------------- */
    private static class DummyWithGenericField {

        List<String> list;
    }

    private static class DummyWithoutGenericField {

        String value;
    }

    private static class DummyStringField {

        String value;
    }

    private static class DummyPrimitiveField {

        int number;
    }

    private static class DummyListStringField {

        List<String> list;
    }

    private static class DummyMapStringIntegerField {

        Map<String, Integer> map;
    }

    private static class DummyListListStringField {

        List<List<String>> list;
    }

    private static class DummyWildcardField {

        List<? extends Number> list;
    }

    private static class DummyTriple<A, B, C> {
        // placeholder generic type
    }

    private static class DummyTripleField {

        DummyTriple<String, Integer, Boolean> triple;
    }

    private static Stream<Arguments> annotatedSuccessProvider() throws NoSuchFieldException {
        // List<String> – index 0 → String
        Field listStringField = DummyListStringField.class.getDeclaredField("list");
        AnnotatedParameterizedType listStringParam = (AnnotatedParameterizedType) listStringField.getAnnotatedType();
        AnnotatedType listStringArg0 = listStringParam.getAnnotatedActualTypeArguments()[0];
        // Map<String,Integer> – index 1 → Integer
        Field mapStringIntField = DummyMapStringIntegerField.class.getDeclaredField("map");
        AnnotatedParameterizedType mapStringIntParam = (AnnotatedParameterizedType) mapStringIntField.getAnnotatedType();
        AnnotatedType mapStringIntArg1 = mapStringIntParam.getAnnotatedActualTypeArguments()[1];
        // Map<String,Integer> – index 0 → String
        AnnotatedType mapStringIntArg0 = mapStringIntParam.getAnnotatedActualTypeArguments()[0];
        // List<List<String>> – index 0 → List<String>
        Field listListStringField = DummyListListStringField.class.getDeclaredField("list");
        AnnotatedParameterizedType listListStringParam = (AnnotatedParameterizedType) listListStringField.getAnnotatedType();
        AnnotatedType listListStringArg0 = listListStringParam.getAnnotatedActualTypeArguments()[0];
        // List<? extends Number> – index 0 → wildcard
        Field wildcardField = DummyWildcardField.class.getDeclaredField("list");
        AnnotatedParameterizedType wildcardParam = (AnnotatedParameterizedType) wildcardField.getAnnotatedType();
        AnnotatedType wildcardArg0 = wildcardParam.getAnnotatedActualTypeArguments()[0];
        // Triple<String,Integer,Boolean> – index 2 → Boolean
        Field tripleField = DummyTripleField.class.getDeclaredField("triple");
        AnnotatedParameterizedType tripleParam = (AnnotatedParameterizedType) tripleField.getAnnotatedType();
        AnnotatedType tripleArg2 = tripleParam.getAnnotatedActualTypeArguments()[2];
        // Non‑parameterized String – any index returns the original type
        Field stringField = DummyStringField.class.getDeclaredField("value");
        AnnotatedType stringAnnotated = stringField.getAnnotatedType();
        // Primitive int – any index returns the original type
        Field intField = DummyPrimitiveField.class.getDeclaredField("number");
        AnnotatedType intAnnotated = intField.getAnnotatedType();
        return Stream.of(Arguments.of("AnnotatedParameterizedType List<String> index 0 → String", DummyListStringField.class, "list", 0, listStringArg0), Arguments.of("AnnotatedParameterizedType Map<String,Integer> index 1 → Integer", DummyMapStringIntegerField.class, "map", 1, mapStringIntArg1), Arguments.of("AnnotatedParameterizedType Map<String,Integer> index 0 → String", DummyMapStringIntegerField.class, "map", 0, mapStringIntArg0), Arguments.of("AnnotatedParameterizedType List<List<String>> index 0 → inner List<String>", DummyListListStringField.class, "list", 0, listListStringArg0), Arguments.of("AnnotatedParameterizedType List<? extends Number> index 0 → wildcard", DummyWildcardField.class, "list", 0, wildcardArg0), Arguments.of("AnnotatedParameterizedType Triple<String,Integer,Boolean> index 2 → Boolean", DummyTripleField.class, "triple", 2, tripleArg2), Arguments.of("Non‑parameterized String returns original (index 0)", DummyStringField.class, "value", 0, stringAnnotated), Arguments.of("Non‑parameterized String returns original (negative index)", DummyStringField.class, "value", -1, stringAnnotated), Arguments.of("Non‑parameterized primitive int returns original (any index)", DummyPrimitiveField.class, "number", 5, intAnnotated));
    }

    private static Stream<Arguments> annotatedExceptionProvider() throws NoSuchFieldException {
        return Stream.of(Arguments.of("AnnotatedParameterizedType List<String> index 1 → ArrayIndexOutOfBoundsException", DummyListStringField.class, "list", 1, ArrayIndexOutOfBoundsException.class), Arguments.of("AnnotatedParameterizedType List<String> index -1 → ArrayIndexOutOfBoundsException", DummyListStringField.class, "list", -1, ArrayIndexOutOfBoundsException.class));
    }

    private static Stream<Arguments> typeSuccessProvider() throws NoSuchFieldException {
        // Helper to extract generic Types from fields
        Field listStringField = DummyListStringField.class.getDeclaredField("list");
        ParameterizedType listStringParam = (ParameterizedType) listStringField.getGenericType();
        Field mapStringIntField = DummyMapStringIntegerField.class.getDeclaredField("map");
        ParameterizedType mapStringIntParam = (ParameterizedType) mapStringIntField.getGenericType();
        Field listListStringField = DummyListListStringField.class.getDeclaredField("list");
        ParameterizedType listListStringParam = (ParameterizedType) listListStringField.getGenericType();
        Field wildcardField = DummyWildcardField.class.getDeclaredField("list");
        ParameterizedType wildcardParam = (ParameterizedType) wildcardField.getGenericType();
        Type wildcardArg0 = wildcardParam.getActualTypeArguments()[0];
        Field tripleField = DummyTripleField.class.getDeclaredField("triple");
        ParameterizedType tripleParam = (ParameterizedType) tripleField.getGenericType();
        // Non‑parameterized types
        Type stringType = String.class;
        Type intType = int.class;
        return Stream.of(Arguments.of("Parameterized Type List<String> index 0 → java.lang.String", listStringParam, 0, String.class), Arguments.of("Parameterized Type Map<String,Integer> index 1 → java.lang.Integer", mapStringIntParam, 1, Integer.class), Arguments.of("Parameterized Type Map<String,Integer> index 0 → java.lang.String", mapStringIntParam, 0, String.class), Arguments.of("Parameterized Type List<List<String>> index 0 → java.util.List", listListStringParam, 0, List.class), Arguments.of("Parameterized Type List<? extends Number> index 0 → WildcardType", wildcardParam, 0, wildcardArg0), Arguments.of("Parameterized Type Triple<String,Integer,Boolean> index 2 → java.lang.Boolean", tripleParam, 2, Boolean.class), Arguments.of("Non‑parameterized String (any index) → java.lang.String", stringType, 5, String.class), Arguments.of("Non‑parameterized primitive int (any index) → int", intType, 0, int.class), Arguments.of("Non‑parameterized String with negative index → java.lang.String", stringType, -3, String.class));
    }

    private static Stream<Arguments> typeExceptionProvider() throws NoSuchFieldException {
        Field listStringField = DummyListStringField.class.getDeclaredField("list");
        ParameterizedType listStringParam = (ParameterizedType) listStringField.getGenericType();
        Field mapStringIntField = DummyMapStringIntegerField.class.getDeclaredField("map");
        ParameterizedType mapStringIntParam = (ParameterizedType) mapStringIntField.getGenericType();
        return Stream.of(Arguments.of("List<String> index 1 → ArrayIndexOutOfBoundsException", listStringParam, 1, ArrayIndexOutOfBoundsException.class), Arguments.of("List<String> index -1 → ArrayIndexOutOfBoundsException", listStringParam, -1, ArrayIndexOutOfBoundsException.class), Arguments.of("Map<String,Integer> index 2 → ArrayIndexOutOfBoundsException", mapStringIntParam, 2, ArrayIndexOutOfBoundsException.class));
    }

    /**
     * When the supplied {@link AnnotatedType} is an {@link AnnotatedParameterizedType},
     * the method should return the annotated actual type argument at the given index.
     */
    @Test
    void typeOfParameter_returnsActualArgumentForParameterizedType() throws NoSuchFieldException {
        // Arrange: a field with a generic type List<String>
        AnnotatedType annotatedType = DummyWithGenericField.class.getDeclaredField("list").getAnnotatedType();
        assertTrue(annotatedType instanceof AnnotatedParameterizedType, "The field's annotated type must be an AnnotatedParameterizedType");
        AnnotatedParameterizedType paramType = (AnnotatedParameterizedType) annotatedType;
        AnnotatedType expectedArgument = paramType.getAnnotatedActualTypeArguments()[0];
        // Act
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(annotatedType, 0);
        // Assert
        assertSame(expectedArgument, result, "typeOfParameter should return the annotated actual type argument at the requested index");
    }

    /**
     * When the supplied {@link AnnotatedType} is *not* an {@link AnnotatedParameterizedType},
     * the method should simply return the original instance.
     */
    @Test
    void typeOfParameter_returnsOriginalWhenNotParameterized() throws NoSuchFieldException {
        // Arrange: a field with a non‑generic type String
        AnnotatedType annotatedType = DummyWithoutGenericField.class.getDeclaredField("value").getAnnotatedType();
        // Act
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(annotatedType, 0);
        // Assert
        assertSame(annotatedType, result, "typeOfParameter should return the original AnnotatedType when it is not parameterized");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("annotatedSuccessProvider")
    void typeOfParameter_annotated_success(String description, Class<?> holder, String fieldName, int index, AnnotatedType expected) throws NoSuchFieldException {
        // Arrange
        AnnotatedType type = holder.getDeclaredField(fieldName).getAnnotatedType();
        // Act
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, index);
        // Assert
        assertSame(expected, result, () -> "Expected the annotated type for index " + index + " of field " + fieldName);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("annotatedExceptionProvider")
    void typeOfParameter_annotated_exceptions(String description, Class<?> holder, String fieldName, int index, Class<? extends Throwable> expectedException) throws NoSuchFieldException {
        // Arrange
        AnnotatedType type = holder.getDeclaredField(fieldName).getAnnotatedType();
        // Act & Assert
        assertThrows(expectedException, () -> ConfigMappingInterface.typeOfParameter(type, index));
    }

    @Test
    void typeOfParameter_annotated_nullArgument() {
        // Directly pass null to verify NPE
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.typeOfParameter(null, 0));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("typeSuccessProvider")
    void typeOfParameter_type_success(String description, Type sourceType, int index, Type expected) {
        // Act
        Type result = ConfigMappingInterface.typeOfParameter(sourceType, index);
        // Assert
        assertEquals(expected, result, () -> "Expected type " + expected + " for index " + index);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("typeExceptionProvider")
    void typeOfParameter_type_exceptions(String description, Type sourceType, int index, Class<? extends Throwable> expectedException) {
        // Act & Assert
        assertThrows(expectedException, () -> ConfigMappingInterface.typeOfParameter(sourceType, index));
    }

    @Test
    void typeOfParameter_type_nullArgument() {
        // According to the source, a null Type returns null (no NPE)
        assertNull(ConfigMappingInterface.typeOfParameter(null, 0), "When a null Type is supplied the method should return null");
    }

    @Test
    void typeOfParameter_type_wildcardUpperBound() {
        // Verify that a wildcard type is returned and its upper bound is Number
        Field wildcardField;
        try {
            wildcardField = DummyWildcardField.class.getDeclaredField("list");
        } catch (NoSuchFieldException e) {
            fail(e);
            return;
        }
        ParameterizedType wildcardParam = (ParameterizedType) wildcardField.getGenericType();
        Type result = ConfigMappingInterface.typeOfParameter(wildcardParam, 0);
        assertTrue(result instanceof WildcardType, "Result should be a WildcardType");
        WildcardType wt = (WildcardType) result;
        // The wildcard is "? extends Number", so there should be a single upper bound Number
        assertArrayEquals(new Type[] { Number.class }, wt.getUpperBounds(), "Wildcard upper bound should be Number");
    }
}