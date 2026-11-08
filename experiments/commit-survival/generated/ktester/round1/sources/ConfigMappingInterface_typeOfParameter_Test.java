package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_typeOfParameter_Test {

    interface DummyGeneric {

        Map<String, Integer> map();

        String plain();
    }

    interface DummyListString {

        List<String> list();
    }

    interface DummyMapStringInteger {

        Map<String, Integer> map();
    }

    interface DummyOptionalString {

        Optional<String> optional();
    }

    interface DummyNestedList {

        List<List<Integer>> listOfList();
    }

    interface DummyWildcardList {

        List<? extends Number> list();
    }

    private static Stream<Arguments> conditionalBranchCases() throws NoSuchMethodException {
        Method listString = DummyListString.class.getMethod("list");
        Method mapStringInteger = DummyMapStringInteger.class.getMethod("map");
        Method plain = DummyGeneric.class.getMethod("plain");
        return Stream.of(// List<String>, index 0 -> String
        Arguments.of(listString.getAnnotatedReturnType(), 0, String.class, "List<String> index 0"), // Map<String,Integer>, index 1 -> Integer
        Arguments.of(mapStringInteger.getAnnotatedReturnType(), 1, Integer.class, "Map<String,Integer> index 1"), // Non‑parameterized String, any index -> original
        Arguments.of(plain.getAnnotatedReturnType(), 0, plain.getAnnotatedReturnType().getType(), "Non‑parameterized type should be returned unchanged"));
    }

    @Test
    void typeOfParameter_returnsActualArgumentWhenAnnotatedParameterizedType() throws Exception {
        Method mapMethod = DummyGeneric.class.getMethod("map");
        AnnotatedType returnType = mapMethod.getAnnotatedReturnType();
        assertTrue(returnType instanceof AnnotatedParameterizedType, "Return type should be an AnnotatedParameterizedType");
        AnnotatedType keyType = ConfigMappingInterface.typeOfParameter(returnType, 0);
        assertEquals(String.class, keyType.getType(), "First type argument should be java.lang.String");
        AnnotatedType valueType = ConfigMappingInterface.typeOfParameter(returnType, 1);
        assertEquals(Integer.class, valueType.getType(), "Second type argument should be java.lang.Integer");
    }

    @Test
    void typeOfParameter_returnsOriginalWhenNotParameterized() throws Exception {
        Method plainMethod = DummyGeneric.class.getMethod("plain");
        AnnotatedType returnType = plainMethod.getAnnotatedReturnType();
        assertFalse(returnType instanceof AnnotatedParameterizedType, "Return type should NOT be an AnnotatedParameterizedType");
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(returnType, 0);
        assertSame(returnType, result, "When not parameterized the original AnnotatedType must be returned");
    }

    @Test
    void typeOfParameter_handlesOutOfBoundsIndexGracefully() throws Exception {
        Method mapMethod = DummyGeneric.class.getMethod("map");
        AnnotatedType returnType = mapMethod.getAnnotatedReturnType();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(returnType, 2), "Requesting an invalid index should result in ArrayIndexOutOfBoundsException");
    }

    @Test
    void typeOfParameter_ListString_indexZero_returnsString() throws Exception {
        Method m = DummyListString.class.getMethod("list");
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 0);
        assertEquals(String.class, result.getType(), "List<String> index 0 should yield java.lang.String");
    }

    @Test
    void typeOfParameter_MapStringInteger_indexOne_returnsInteger() throws Exception {
        Method m = DummyMapStringInteger.class.getMethod("map");
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 1);
        assertEquals(Integer.class, result.getType(), "Map<String,Integer> index 1 should yield java.lang.Integer");
    }

    @Test
    void typeOfParameter_nonParameterized_returnsOriginal() throws Exception {
        Method m = DummyGeneric.class.getMethod("plain");
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 0);
        assertSame(type, result, "Non‑parameterized type should be returned unchanged");
    }

    @Test
    void typeOfParameter_ListString_outOfBoundsIndex_throwsArrayIndexOutOfBoundsException() throws Exception {
        Method m = DummyListString.class.getMethod("list");
        AnnotatedType type = m.getAnnotatedReturnType();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(type, 5), "Index 5 on List<String> should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void typeOfParameter_nullInput_returnsNull() {
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(null, 0);
        assertNull(result, "When the supplied AnnotatedType is null the method must return null");
    }

    @Test
    void typeOfParameter_OptionalString_returnsString() throws Exception {
        Method m = DummyOptionalString.class.getMethod("optional");
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 0);
        assertEquals(String.class, result.getType(), "Optional<String> index 0 should yield java.lang.String");
    }

    @Test
    void typeOfParameter_NestedList_returnsInnerParameterizedType() throws Exception {
        Method m = DummyNestedList.class.getMethod("listOfList");
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 0);
        assertTrue(result instanceof AnnotatedParameterizedType, "The returned type should still be an AnnotatedParameterizedType (List<Integer>)");
        AnnotatedType inner = ConfigMappingInterface.typeOfParameter(result, 0);
        assertEquals(Integer.class, inner.getType(), "Inner List<Integer> argument should be java.lang.Integer");
    }

    @Test
    void typeOfParameter_WildcardList_returnsWildcard() throws Exception {
        Method m = DummyWildcardList.class.getMethod("list");
        AnnotatedType type = m.getAnnotatedReturnType();
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, 0);
        assertTrue(result.getType() instanceof java.lang.reflect.WildcardType, "The extracted type argument should be a WildcardType");
    }

    @Test
    void typeOfParameter_ListString_negativeIndex_throwsArrayIndexOutOfBoundsException() throws Exception {
        Method m = DummyListString.class.getMethod("list");
        AnnotatedType type = m.getAnnotatedReturnType();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(type, -1), "Negative index should result in ArrayIndexOutOfBoundsException");
    }

    @Test
    void typeOfParameter_ListString_indexEqualsArgumentCount_throwsArrayIndexOutOfBoundsException() throws Exception {
        Method m = DummyListString.class.getMethod("list");
        AnnotatedType type = m.getAnnotatedReturnType();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.typeOfParameter(type, 1), "Index equal to argument count should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void typeOfParameter_MockedAnnotatedParameterizedType_returnsNullArray_throwsNullPointerException() {
        AnnotatedParameterizedType mock = new AnnotatedParameterizedType() {

            @Override
            public AnnotatedType[] getAnnotatedActualTypeArguments() {
                // simulate null array
                return null;
            }

            @Override
            public Type getType() {
                return List.class;
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
        };
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.typeOfParameter(mock, 0), "Null array from getAnnotatedActualTypeArguments() should cause NPE");
    }

    @Test
    void typeOfParameter_MockedAnnotatedParameterizedType_throwsIllegalStateException() {
        AnnotatedParameterizedType mock = new AnnotatedParameterizedType() {

            @Override
            public AnnotatedType[] getAnnotatedActualTypeArguments() {
                throw new IllegalStateException("boom");
            }

            @Override
            public Type getType() {
                return List.class;
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
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ConfigMappingInterface.typeOfParameter(mock, 0), "Exception from the mock should propagate unchanged");
        assertEquals("boom", ex.getMessage());
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("conditionalBranchCases")
    void typeOfParameter_conditionalBranchCoverage(AnnotatedType type, int index, Class<?> expectedClass, String displayName) {
        AnnotatedType result = ConfigMappingInterface.typeOfParameter(type, index);
        assertEquals(expectedClass, result.getType(), "Unexpected type for case: " + displayName);
    }
}
