package io.smallrye.config;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;

class ConfigMappingInterface_typeOfParameter_Test {

    /**
     * Helper interface that defines a method with generic parameter types.
     * The reflective information from this interface is used to obtain
     * {@link AnnotatedParameterizedType} instances.
     */
    interface GenericMethodHolder {

//         void genericMethod(List<String> list, Map<Integer, String> map);

        void nonGenericMethod(String value);
    }

    /**
     * Helper class that defines a variety of generic and non‑generic fields.
     * The reflective information from these fields is used to obtain
     * {@link AnnotatedType} instances for the tests.
     */
    static class GenericFields {

//         List<String> listString;

//         Map<String, Integer> mapStringInteger;

//         List<List<String>> listOfListString;

        // raw type – no generic information
//         List rawList;

        String plainString;
    }

    /**
     * Returns the {@link AnnotatedType} representing the type argument at the given index
     * if the supplied {@code annotatedType} is an {@link AnnotatedParameterizedType}.
     * Otherwise returns the original {@code annotatedType}.
     *
     * @param annotatedType the type to inspect; may be {@code null}
     * @param index the index of the type argument to retrieve
     * @return the requested {@link AnnotatedType}, or {@code null} if {@code annotatedType} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if {@code annotatedType} is a parameterized type
     *         and the index is out of range
     */
    public static AnnotatedType typeOfParameter(AnnotatedType annotatedType, int index) {
        if (annotatedType == null) {
            return null;
        }
        if (annotatedType instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType paramType = (AnnotatedParameterizedType) annotatedType;
            AnnotatedType[] args = paramType.getAnnotatedActualTypeArguments();
            // This will naturally throw ArrayIndexOutOfBoundsException for illegal indexes
            return args[index];
        }
        // Non‑parameterized type – ignore the index and return the original instance
        return annotatedType;
    }

//     @Test
//     void typeOfParameter_returnsActualArgumentWhenAnnotatedParameterizedType() throws Exception {
//         // Obtain the method and its annotated parameter types
//         Method method = GenericMethodHolder.class.getMethod("genericMethod", List.class, Map.class);
//         AnnotatedType[] params = method.getAnnotatedParameterTypes();
//         // First parameter is List<String>
//         AnnotatedType firstParam = params[0];
//         assertTrue(firstParam instanceof AnnotatedParameterizedType, "First parameter should be an AnnotatedParameterizedType");
//         // Ask ConfigMappingInterface to return the type argument at index 0 (i.e., String)
//         AnnotatedType result = ConfigMappingInterface.typeOfParameter(firstParam, 0);
//         assertNotNull(result, "Result should not be null");
//         // The underlying Java Type of the result must be String.class
//         assertEquals(String.class, result.getType(), "The extracted type argument should be java.lang.String");
//     }

//     @Test
//     void typeOfParameter_returnsOriginalWhenNotAnnotatedParameterizedType() throws Exception {
//         // Obtain a method with a non‑parameterized argument
//         Method method = GenericMethodHolder.class.getMethod("nonGenericMethod", String.class);
//         AnnotatedType[] params = method.getAnnotatedParameterTypes();
//         // The only parameter is a plain String, not an AnnotatedParameterizedType
//         AnnotatedType param = params[0];
//         assertFalse(param instanceof AnnotatedParameterizedType, "Parameter should NOT be an AnnotatedParameterizedType");
//         // The method should return the same instance when called
//         AnnotatedType result = ConfigMappingInterface.typeOfParameter(param, 0);
//         assertSame(param, result, "When not parameterized, the original AnnotatedType must be returned");
//     }

    /**
     * Group: AnnotatedParameterizedType handling – multiple arguments.
     */
//     @Test
//     void typeOfParameter_returnsSecondArgumentForMap() throws Exception {
//         // Map<String, Integer> field
//         Field mapField = GenericFields.class.getDeclaredField("mapStringInteger");
//         AnnotatedType mapType = mapField.getAnnotatedType();
//         assertTrue(mapType instanceof AnnotatedParameterizedType, "Field type should be AnnotatedParameterizedType");
//         // Index 1 should give Integer
//         AnnotatedType result = ConfigMappingInterface.typeOfParameter(mapType, 1);
//         assertEquals(Integer.class, result.getType(), "Index 1 of Map<String,Integer> should be java.lang.Integer");
//     }

    /**
     * Group: Non‑parameterized type handling – raw type.
     */
//     @Test
//     void typeOfParameter_returnsOriginalForRawList() throws Exception {
//         Field rawField = GenericFields.class.getDeclaredField("rawList");
//         AnnotatedType rawType = rawField.getAnnotatedType();
//         // Raw List is NOT an AnnotatedParameterizedType
//         assertFalse(rawType instanceof AnnotatedParameterizedType, "Raw List field should not be an AnnotatedParameterizedType");
//         AnnotatedType result = ConfigMappingInterface.typeOfParameter(rawType, 0);
//         assertSame(rawType, result, "Raw type should be returned unchanged");
//     }

    /**
     * Group: Error conditions – index out of bounds (positive).
     */
//     @Test
//     void typeOfParameter_throwsArrayIndexOutOfBoundsWhenIndexTooLarge() throws Exception {
//         Field listField = GenericFields.class.getDeclaredField("listString");
//         AnnotatedType listType = listField.getAnnotatedType();
//         assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(listType, 2), "Index 2 on List<String> should throw ArrayIndexOutOfBoundsException");
//     }

    /**
     * Group: Error conditions – index out of bounds (negative).
     */
//     @Test
//     void typeOfParameter_throwsArrayIndexOutOfBoundsWhenIndexNegative() throws Exception {
//         Field listField = GenericFields.class.getDeclaredField("listString");
//         AnnotatedType listType = listField.getAnnotatedType();
//         assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(listType, -1), "Negative index should throw ArrayIndexOutOfBoundsException");
//     }

    /**
     * Group: Error conditions – extremely large index.
     */
//     @Test
//     void typeOfParameter_throwsArrayIndexOutOfBoundsWhenIndexHuge() throws Exception {
//         Field mapField = GenericFields.class.getDeclaredField("mapStringInteger");
//         AnnotatedType mapType = mapField.getAnnotatedType();
//         assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(mapType, Integer.MAX_VALUE), "Huge index should throw ArrayIndexOutOfBoundsException");
//     }

    /**
     * Group: Error conditions – null AnnotatedType.
     */
//     @Test
//     void typeOfParameter_returnsNullWhenAnnotatedTypeIsNull() {
//         AnnotatedType result = ConfigMappingInterface.typeOfParameter(null, 0);
//         assertNull(result, "When the supplied AnnotatedType is null the method must return null");
//     }

    /**
     * Group: Nested parameterized type handling.
     */
//     @Test
//     void typeOfParameter_returnsInnerParameterizedTypeForNestedGenerics() throws Exception {
//         Field nestedField = GenericFields.class.getDeclaredField("listOfListString");
//         AnnotatedType outerType = nestedField.getAnnotatedType();
//         assertTrue(outerType instanceof AnnotatedParameterizedType, "Outer field must be an AnnotatedParameterizedType");
//         // Index 0 should give the inner List<String> as an AnnotatedParameterizedType
//         AnnotatedType inner = ConfigMappingInterface.typeOfParameter(outerType, 0);
//         assertTrue(inner instanceof AnnotatedParameterizedType, "Inner type should also be an AnnotatedParameterizedType");
//         // Verify the raw type of the inner parameterized type is java.util.List
//         assertEquals(List.class, inner.getType(), "Inner raw type should be java.util.List");
//         // Verify its own type argument is String
//         AnnotatedParameterizedType innerParam = (AnnotatedParameterizedType) inner;
//         assertEquals(String.class, innerParam.getAnnotatedActualTypeArguments()[0].getType(), "Inner generic argument should be java.lang.String");
//     }

    /**
     * Group: Identity verification for valid index.
     */
//     @Test
//     void typeOfParameter_returnsExactInstanceFromArray() throws Exception {
//         Field mapField = GenericFields.class.getDeclaredField("mapStringInteger");
//         AnnotatedParameterizedType mapType = (AnnotatedParameterizedType) mapField.getAnnotatedType();
//         // Grab the first actual type argument directly from the array – should be String
//         AnnotatedType expected = mapType.getAnnotatedActualTypeArguments()[0];
//         // Invoke the method with the same index
//         AnnotatedType result = ConfigMappingInterface.typeOfParameter(mapType, 0);
//         assertSame(expected, result, "The method must return the exact same AnnotatedType instance stored in the array");
//     }

    /**
     * Group: Raw type treated as non‑parameterized (duplicate of earlier raw test).
     */
//     @Test
//     void typeOfParameter_rawTypeIsHandledAsNonParameterized() throws Exception {
//         Field rawField = GenericFields.class.getDeclaredField("rawList");
//         AnnotatedType rawType = rawField.getAnnotatedType();
//         // Ensure it's not an AnnotatedParameterizedType
//         assertFalse(rawType instanceof AnnotatedParameterizedType, "Raw List should not be an AnnotatedParameterizedType");
//         // The method should simply return the original instance
//         AnnotatedType result = ConfigMappingInterface.typeOfParameter(rawType, 0);
//         assertSame(rawType, result, "For raw types the original AnnotatedType must be returned");
//     }

    /**
     * Group: Null AnnotatedType handling – negative index.
     */
//     @Test
//     void typeOfParameter_nullAnnotatedTypeIgnoresIndex() {
//         AnnotatedType result = ConfigMappingInterface.typeOfParameter(null, -5);
//         assertNull(result, "Null input should always result in null, regardless of index");
//     }
}