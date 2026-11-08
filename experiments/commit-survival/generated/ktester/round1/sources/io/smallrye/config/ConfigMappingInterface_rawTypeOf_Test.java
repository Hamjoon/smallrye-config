package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.List;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_rawTypeOf_Test {

    /**
     * Helper class that provides a variety of generic method signatures.
     * The methods are never invoked – they are only used to obtain {@link Type}
     * instances via reflection.
     */
    private static class TypeProvider {

        public String plainString() {
            return "";
        }

        public List<String> parameterizedList() {
            return null;
        }

        public List<String>[] genericArray() {
            return null;
        }

        public List<? extends Number> wildcardExtends() {
            return null;
        }

        public List<?> wildcardUnbounded() {
            return null;
        }

        public <T> T typeVariable() {
            return null;
        }
    }

    @Test
    void rawTypeOf_Class() {
        // Direct class instance
        Class<?> result = ConfigMappingInterface.rawTypeOf(String.class);
        assertEquals(String.class, result);
    }

    @Test
    void rawTypeOf_ParameterizedType() throws NoSuchMethodException {
        Method method = TypeProvider.class.getMethod("parameterizedList");
        // should be ParameterizedType List<String>
        Type type = method.getGenericReturnType();
        assertTrue(type instanceof ParameterizedType);
        Class<?> result = ConfigMappingInterface.rawTypeOf(type);
        assertEquals(List.class, result);
    }

    @Test
    void rawTypeOf_GenericArrayType() throws NoSuchMethodException {
        Method method = TypeProvider.class.getMethod("genericArray");
        // should be GenericArrayType List<String>[]
        Type type = method.getGenericReturnType();
        assertTrue(type instanceof GenericArrayType);
        Class<?> result = ConfigMappingInterface.rawTypeOf(type);
        // Expected: array class whose component type is List
        Class<?> expected = java.lang.reflect.Array.newInstance(List.class, 0).getClass();
        assertEquals(expected, result);
    }

    @Test
    void rawTypeOf_WildcardType_WithUpperBound() throws NoSuchMethodException {
        Method method = TypeProvider.class.getMethod("wildcardExtends");
        // WildcardType with upper bound Number
        Type type = method.getGenericReturnType();
        // The raw type of List<? extends Number> is List
        assertTrue(type instanceof ParameterizedType);
        Type arg = ((ParameterizedType) type).getActualTypeArguments()[0];
        assertTrue(arg instanceof WildcardType);
        Class<?> result = ConfigMappingInterface.rawTypeOf(arg);
        assertEquals(Number.class, result);
    }

    @Test
    void rawTypeOf_WildcardType_Unbounded() throws NoSuchMethodException {
        Method method = TypeProvider.class.getMethod("wildcardUnbounded");
        // WildcardType with implicit upper bound Object
        Type type = method.getGenericReturnType();
        assertTrue(type instanceof ParameterizedType);
        Type arg = ((ParameterizedType) type).getActualTypeArguments()[0];
        assertTrue(arg instanceof WildcardType);
        Class<?> result = ConfigMappingInterface.rawTypeOf(arg);
        assertEquals(Object.class, result);
    }

    @Test
    void rawTypeOf_UnsupportedType_throws() throws NoSuchMethodException {
        Method method = TypeProvider.class.getMethod("typeVariable");
        // TypeVariable
        Type type = method.getGenericReturnType();
        assertTrue(type instanceof TypeVariable);
        // The method should throw the ConfigMessages exception (a RuntimeException subclass)
        assertThrows(RuntimeException.class, () -> ConfigMappingInterface.rawTypeOf(type));
    }
}