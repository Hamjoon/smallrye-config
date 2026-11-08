package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_rawTypeOf_Test {

    // -----------------------------------------------------------------------
    interface Sample<T> {

        List<String> listOfString();

        List<String>[] arrayOfList();

        Map<String, Double> mapStringDouble();

        T[] genericArray();

        List<T> listOfT();

        // Used to obtain a TypeVariable (T)
        T typeVariable();
    }

    // -----------------------------------------------------------------------
    private static class FakeParameterizedType implements ParameterizedType {

        private final Type raw;

        private final Type[] args;

        private final Type owner;

        FakeParameterizedType(Type raw, Type[] args, Type owner) {
            this.raw = raw;
            this.args = args;
            this.owner = owner;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return args;
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return owner;
        }
    }

    static Stream<Arguments> classProvider() {
        return Stream.of(Arguments.of(String.class, String.class), Arguments.of(int.class, int.class), Arguments.of(String[].class, String[].class), Arguments.of(int[].class, int[].class));
    }

    static Stream<Arguments> parameterizedProvider() throws NoSuchMethodException {
        Method listMethod = Sample.class.getMethod("listOfString");
        Method mapMethod = Sample.class.getMethod("mapStringDouble");
        return Stream.of(Arguments.of(listMethod.getGenericReturnType(), List.class), Arguments.of(mapMethod.getGenericReturnType(), Map.class));
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: rawTypeOf({0}) => {1}")
    @MethodSource("classProvider")
    void rawTypeOf_class(Type input, Class<?> expected) {
        assertEquals(expected, ConfigMappingInterface.rawTypeOf(input));
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: rawTypeOf(ParameterizedType of {0}) => {1}")
    @MethodSource("parameterizedProvider")
    void rawTypeOf_parameterizedType(Type input, Class<?> expected) {
        assertEquals(expected, ConfigMappingInterface.rawTypeOf(input));
    }

    // -----------------------------------------------------------------------
    @Test
    void rawTypeOf_genericArrayType_simple() throws NoSuchMethodException {
        // List<String>[]
        Method m = Sample.class.getMethod("arrayOfList");
        // GenericArrayType
        Type genericArray = m.getGenericReturnType();
        Class<?> result = ConfigMappingInterface.rawTypeOf(genericArray);
        assertTrue(result.isArray());
        assertEquals(List.class, result.getComponentType());
    }

    @Test
    void rawTypeOf_genericArrayType_typeVariableComponent() throws NoSuchMethodException {
        // T[] where T is a type variable without bounds -> Object[]
        Method m = Sample.class.getMethod("genericArray");
        // GenericArrayType
        Type genericArray = m.getGenericReturnType();
        Class<?> result = ConfigMappingInterface.rawTypeOf(genericArray);
        assertTrue(result.isArray());
        assertEquals(Object.class, result.getComponentType());
    }

    @Test
    void rawTypeOf_genericArrayType_typeVariableWithBound() {
        // Simulate a GenericArrayType whose component type is a wildcard ? extends Number
        GenericArrayType array = new GenericArrayType() {

            @Override
            public Type getGenericComponentType() {
                return new WildcardType() {

                    @Override
                    public Type[] getUpperBounds() {
                        return new Type[] { Number.class };
                    }

                    @Override
                    public Type[] getLowerBounds() {
                        return new Type[0];
                    }
                };
            }
        };
        Class<?> result = ConfigMappingInterface.rawTypeOf(array);
        assertTrue(result.isArray());
        assertEquals(Number.class, result.getComponentType());
    }

    // -----------------------------------------------------------------------
    @Test
    void rawTypeOf_wildcardWithUpperBound() {
        WildcardType wt = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return new Type[] { Number.class };
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        assertEquals(Number.class, ConfigMappingInterface.rawTypeOf(wt));
    }

    @Test
    void rawTypeOf_wildcardWithoutUpperBound() {
        WildcardType wt = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return new Type[] { Object.class };
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        assertEquals(Object.class, ConfigMappingInterface.rawTypeOf(wt));
    }

    @Test
    void rawTypeOf_wildcardWithMultipleUpperBounds() {
        // ? extends Serializable & Comparable
        WildcardType wt = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return new Type[] { Serializable.class, Comparable.class };
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        assertEquals(Serializable.class, ConfigMappingInterface.rawTypeOf(wt));
    }

    @Test
    void rawTypeOf_wildcardWithNullUpperBoundsArray() {
        WildcardType wt = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return null;
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        assertEquals(Object.class, ConfigMappingInterface.rawTypeOf(wt));
    }

    @Test
    void rawTypeOf_wildcardWithEmptyUpperBoundsArray() {
        WildcardType wt = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return new Type[0];
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        // Accessing upperBounds[0] should throw ArrayIndexOutOfBoundsException
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.rawTypeOf(wt));
    }

    @Test
    void rawTypeOf_wildcardUpperBoundIsNullElement() {
        WildcardType wt = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return new Type[] { null };
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        // The method will try to recurse with a null argument → NPE
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.rawTypeOf(wt));
    }

    @Test
    void rawTypeOf_wildcardUpperBoundIsTypeVariable() throws NoSuchMethodException {
        // ? extends T where T is a type variable from Sample
        TypeVariable<Class<Sample>> tv = Sample.class.getTypeParameters()[0];
        WildcardType wt = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return new Type[] { tv };
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(wt));
        assertTrue(ex.getMessage().contains("no raw type"));
    }

    // -----------------------------------------------------------------------
    @Test
    void rawTypeOf_typeVariable_unsupported() {
        TypeVariable<Class<Sample>> tv = Sample.class.getTypeParameters()[0];
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(tv));
        assertTrue(ex.getMessage().contains("no raw type"));
    }

    @Test
    void rawTypeOf_parameterizedTypeWithNonClassRaw() {
        // Create a ParameterizedType whose raw type is a TypeVariable
        TypeVariable<Class<Sample>> tv = Sample.class.getTypeParameters()[0];
        ParameterizedType pt = new FakeParameterizedType(tv, new Type[] { String.class }, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(pt));
        assertTrue(ex.getMessage().contains("no raw type"));
    }

    @Test
    void rawTypeOf_genericArrayType_componentUnsupported() {
        // GenericArrayType whose component type is a TypeVariable (no bounds)
        GenericArrayType gat = new GenericArrayType() {

            @Override
            public Type getGenericComponentType() {
                // T
                return Sample.class.getTypeParameters()[0];
            }
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(gat));
        assertTrue(ex.getMessage().contains("no raw type"));
    }

    // -----------------------------------------------------------------------
    @Test
    void rawTypeOf_nullInput_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.rawTypeOf(null));
    }

    // -----------------------------------------------------------------------
    @Test
    void rawTypeOf_exceptionMessageSource() {
        TypeVariable<Class<Sample>> tv = Sample.class.getTypeParameters()[0];
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(tv));
        // The message is produced by ConfigMessages.msg.noRawType(Type)
        // We check that it contains the fully‑qualified type name.
        assertTrue(ex.getMessage().contains(tv.getName()));
    }
}