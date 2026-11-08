package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_typeOfParameter_Test {

    /* --------------------------------------------------------------------- */
    interface DummyInterface {

        // non‑parameterized
        String simple();

        // simple parameterized
        Map<String, Integer> map();

        // nested parameterized
        Map<String, Map<Integer, Double>> nestedMap();

        // List<String>
        List<String> listString();

        // Map<String,Integer>
        Map<String, Integer> mapStringInteger();

        // raw List (no type arguments)
        List rawList();

        // wildcard
        List<? extends Number> wildcardList();

        // array type
        int[] intArray();

        // primitive return
        int primitiveInt();
    }

    /**
     * Simple stub for {@link AnnotatedParameterizedType} used in tests that need to
     * control the result of {@link AnnotatedParameterizedType#getAnnotatedActualTypeArguments()}.
     */
    private static class SimpleAnnotatedParameterizedType implements AnnotatedParameterizedType {

        private final AnnotatedType[] actualTypeArguments;

        SimpleAnnotatedParameterizedType(AnnotatedType[] actualTypeArguments) {
            this.actualTypeArguments = actualTypeArguments;
        }

        @Override
        public AnnotatedType[] getAnnotatedActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getType() {
            // Not needed for the tests that use this stub.
            return null;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }
    }

    @Test
    void typeOfParameter_returnsOriginalWhenNotParameterized() throws Exception {
        Method simpleMethod = DummyInterface.class.getMethod("simple");
        AnnotatedType annotatedReturn = simpleMethod.getAnnotatedReturnType();
        // The method is not parameterized, so the helper must return the same instance.
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(annotatedReturn, 0);
        assertSame(annotatedReturn, result, "When the AnnotatedType is not an AnnotatedParameterizedType the original instance must be returned");
        assertEquals(String.class, result.getType(), "The underlying type of a non‑parameterized method should be String");
    }

    @Test
    void typeOfParameter_returnsActualArgumentForSimpleParameterizedType() throws Exception {
        Method mapMethod = DummyInterface.class.getMethod("map");
        AnnotatedType annotatedReturn = mapMethod.getAnnotatedReturnType();
        assertTrue(annotatedReturn instanceof AnnotatedParameterizedType, "Return type of map() must be an AnnotatedParameterizedType");
        // Index 0 -> key type (String)
        AnnotatedType keyType = ConfigMappingInterface.typeOfParameter(annotatedReturn, 0);
        assertEquals(String.class, keyType.getType(), "First type argument of Map<String,Integer> should be String");
        // Index 1 -> value type (Integer)
        AnnotatedType valueType = ConfigMappingInterface.typeOfParameter(annotatedReturn, 1);
        assertEquals(Integer.class, valueType.getType(), "Second type argument of Map<String,Integer> should be Integer");
    }

    @Test
    void typeOfParameter_handlesNestedParameterizedTypes() throws Exception {
        Method nestedMapMethod = DummyInterface.class.getMethod("nestedMap");
        AnnotatedType annotatedReturn = nestedMapMethod.getAnnotatedReturnType();
        assertTrue(annotatedReturn instanceof AnnotatedParameterizedType, "Return type of nestedMap() must be an AnnotatedParameterizedType");
        // First level: Map<String, Map<Integer, Double>>
        // Index 0 -> outer key type (String)
        AnnotatedType outerKey = ConfigMappingInterface.typeOfParameter(annotatedReturn, 0);
        assertEquals(String.class, outerKey.getType(), "Outer key of nested map should be String");
        // Index 1 -> outer value type (which itself is a parameterized type)
        AnnotatedType outerValue = ConfigMappingInterface.typeOfParameter(annotatedReturn, 1);
        assertTrue(outerValue instanceof AnnotatedParameterizedType, "Outer value should be a parameterized type (Map<Integer,Double>)");
        // Now dig into the inner map
        AnnotatedType innerKey = ConfigMappingInterface.typeOfParameter(outerValue, 0);
        assertEquals(Integer.class, innerKey.getType(), "Inner key of nested map should be Integer");
        AnnotatedType innerValue = ConfigMappingInterface.typeOfParameter(outerValue, 1);
        assertEquals(Double.class, innerValue.getType(), "Inner value of nested map should be Double");
    }

    /**
     * AnnotatedParameterizedType branch – valid index (List<String>, index 0).
     */
    @Test
    void typeOfParameter_annotatedParameterizedType_validIndex() throws Exception {
        Method m = DummyInterface.class.getMethod("listString");
        // List<String>
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 0);
        assertEquals(String.class, result.getType(), "List<String> with index 0 must yield the AnnotatedType for String");
    }

    /**
     * AnnotatedParameterizedType branch – multiple type arguments (Map<String,Integer>, index 1).
     */
    @Test
    void typeOfParameter_annotatedParameterizedType_multipleArgs() throws Exception {
        Method m = DummyInterface.class.getMethod("mapStringInteger");
        // Map<String,Integer>
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 1);
        assertEquals(Integer.class, result.getType(), "Map<String,Integer> with index 1 must yield the AnnotatedType for Integer");
    }

    /**
     * AnnotatedParameterizedType branch – index out of bounds (Map<String,Integer>, index 2).
     */
    @Test
    void typeOfParameter_annotatedParameterizedType_indexOutOfBounds() throws Exception {
        Method m = DummyInterface.class.getMethod("mapStringInteger");
        AnnotatedType type = m.getAnnotatedReturnType();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(type, 2), "Requesting index 2 on a two‑argument parameterized type must throw AIOOBE");
    }

    /**
     * AnnotatedParameterizedType branch – negative index (Map<String,Integer>, index -1).
     */
    @Test
    void typeOfParameter_annotatedParameterizedType_negativeIndex() throws Exception {
        Method m = DummyInterface.class.getMethod("mapStringInteger");
        AnnotatedType type = m.getAnnotatedReturnType();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(type, -1), "Negative index must result in ArrayIndexOutOfBoundsException");
    }

    /**
     * Non‑AnnotatedParameterizedType branch – null type argument (should NPE).
     */
    @Test
    void typeOfParameter_nullAnnotatedType_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.typeOfParameter(null, 0), "Passing a null AnnotatedType must result in NullPointerException");
    }

    /**
     * Non‑AnnotatedParameterizedType branch – primitive int (original instance returned).
     */
    @Test
    void typeOfParameter_nonParameterized_primitiveInt() throws Exception {
        Method m = DummyInterface.class.getMethod("primitiveInt");
        // primitive int
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 0);
        assertSame(type, result, "For a non‑parameterized primitive the original AnnotatedType must be returned");
        assertEquals(int.class, result.getType(), "Underlying type must be int");
    }

    /**
     * Non‑AnnotatedParameterizedType branch – array type (int[]).
     */
    @Test
    void typeOfParameter_nonParameterized_arrayType() throws Exception {
        Method m = DummyInterface.class.getMethod("intArray");
        // int[]
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 3);
        assertSame(type, result, "For an array type the original AnnotatedType must be returned irrespective of the index");
        assertEquals(int[].class, result.getType(), "Underlying type must be int[]");
    }

    /**
     * AnnotatedParameterizedType branch – zero type arguments (raw List).
     */
    @Test
    void typeOfParameter_annotatedParameterizedType_zeroArguments() {
        // Simulate a raw parameterized type with no actual type arguments.
        AnnotatedParameterizedType mockType = new SimpleAnnotatedParameterizedType(new AnnotatedType[0]);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(mockType, 0), "A raw parameterized type (no actual arguments) must cause AIOOBE when accessed");
    }

    /**
     * AnnotatedParameterizedType branch – null actual type arguments array.
     */
    @Test
    void typeOfParameter_annotatedParameterizedType_nullActualArgumentsArray() {
        // Simulate a malformed parameterized type where the actual arguments array is null.
        AnnotatedParameterizedType mockType = new SimpleAnnotatedParameterizedType(null);
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.typeOfParameter(mockType, 0), "When getAnnotatedActualTypeArguments() returns null the method should throw NPE");
    }

    /**
     * AnnotatedParameterizedType branch – wildcard type argument (List<? extends Number>).
     */
    @Test
    void typeOfParameter_annotatedParameterizedType_wildcardArgument() throws Exception {
        Method m = DummyInterface.class.getMethod("wildcardList");
        // List<? extends Number>
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 0);
        assertTrue(result.getType() instanceof WildcardType, "The type argument should be a WildcardType");
        WildcardType wt = (WildcardType) result.getType();
        // Verify that the upper bound is Number (i.e. "? extends Number").
        assertArrayEquals(new Class<?>[] { Number.class }, wt.getUpperBounds(), "Wildcard upper bound must be Number");
    }
}
