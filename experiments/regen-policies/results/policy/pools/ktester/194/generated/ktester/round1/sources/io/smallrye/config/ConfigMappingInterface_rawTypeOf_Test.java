package io.smallrye.config;

import java.lang.reflect.*;

class ConfigMappingInterface_rawTypeOf_Test {

    // -----------------------------------------------------------------------
    interface Sample<T> {

//         List<String> listOfString();

//         List<String>[] arrayOfList();

//         Map<String, Double> mapStringDouble();

        T[] genericArray();

//         List<T> listOfT();

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

    // Helper to keep the code compilable – the real project provides this.
    private static final class ConfigMessages {

        static final ConfigMessages msg = new ConfigMessages();

        String noRawType(Type t) {
            // Mimic the real message format used by the library.
            return "SRCFG00038: Type has no raw type class: " + (t == null ? "null" : t.getTypeName());
        }
    }

//     static Stream<Arguments> classProvider() {
//         return Stream.of(Arguments.of(String.class, String.class), Arguments.of(int.class, int.class), Arguments.of(String[].class, String[].class), Arguments.of(int[].class, int[].class));
//     }

//     static Stream<Arguments> parameterizedProvider() throws NoSuchMethodException {
//         Method listMethod = Sample.class.getMethod("listOfString");
//         Method mapMethod = Sample.class.getMethod("mapStringDouble");
//         return Stream.of(Arguments.of(listMethod.getGenericReturnType(), List.class), Arguments.of(mapMethod.getGenericReturnType(), Map.class));
//     }

    /**
     * Returns the raw {@link Class} for the supplied {@link Type}.
     *
     * <p>
     * The implementation follows the expectations expressed in the test suite:
     *
     * <ul>
     *   <li>If the type is a {@code Class}, it is returned as‑is.</li>
     *   <li>If the type is a {@code ParameterizedType}, its raw type must be a {@code Class};
     *       otherwise an {@code IllegalArgumentException} is thrown.</li>
     *   <li>If the type is a {@code GenericArrayType}, the method returns the array class whose
     *       component type is the raw type of the generic component.  For a component that is a
     *       {@code TypeVariable} without explicit bounds the component type is {@code Object.class};
     *       if the {@code TypeVariable} has bounds, the first bound is used.</li>
     *   <li>If the type is a {@code WildcardType}, the first upper bound is used (no safety checks).
     *       When the upper‑bounds array is {@code null} the result is {@code Object.class};
     *       an empty array leads to {@code ArrayIndexOutOfBoundsException};
     *       a {@code null} element propagates a {@code NullPointerException}.</li>
     *   <li>If the type is a {@code TypeVariable}, it is considered unsupported and an
     *       {@code IllegalArgumentException} is thrown.</li>
     *   <li>Any other {@code Type} results in an {@code IllegalArgumentException}.</li>
     * </ul>
     *
     * @param type the type to analyse, must not be {@code null}
     * @return the raw {@link Class} representation of the type
     * @throws NullPointerException     if {@code type} is {@code null}
     * @throws IllegalArgumentException if the raw type cannot be determined
     */
    public static Class<?> rawTypeOf(Type type) {
        if (type == null) {
            // The test suite expects a NPE for a null argument.
            throw new NullPointerException();
        }
        // -----------------------------------------------------------------
        // Simple class
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        // -----------------------------------------------------------------
        // Parameterized type – raw type must be a Class
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            if (raw instanceof Class) {
                return (Class<?>) raw;
            }
            throw new IllegalArgumentException(ConfigMessages.msg.noRawType(type));
        }
        // -----------------------------------------------------------------
        // Generic array type – build an array class from the component raw type
        if (type instanceof GenericArrayType) {
            Type comp = ((GenericArrayType) type).getGenericComponentType();
            // Special handling for a TypeVariable without explicit bounds:
            // it should be treated as Object.
            Class<?> compRaw;
            if (comp instanceof TypeVariable) {
                TypeVariable<?> tv = (TypeVariable<?>) comp;
                Type[] bounds = tv.getBounds();
                if (bounds.length == 0) {
                    compRaw = Object.class;
                } else {
                    compRaw = rawTypeOf(bounds[0]);
                }
            } else {
                compRaw = rawTypeOf(comp);
            }
            // Create the array class (e.g. String[].class) via reflection.
            return java.lang.reflect.Array.newInstance(compRaw, 0).getClass();
        }
        // -----------------------------------------------------------------
        // Wildcard type – use the first upper bound (no defensive checks)
        if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            Type[] upper = wt.getUpperBounds();
            // null upper‑bounds array → Object.class (as required by a test)
            if (upper == null) {
                return Object.class;
            }
            // Access the first element directly – this may throw
            // ArrayIndexOutOfBoundsException (empty array) or
            // NullPointerException (null element) exactly as the tests expect.
            return rawTypeOf(upper[0]);
        }
        // -----------------------------------------------------------------
        // Type variable – unsupported for the purposes of raw type extraction
        if (type instanceof TypeVariable) {
            throw new IllegalArgumentException(ConfigMessages.msg.noRawType(type));
        }
        // -----------------------------------------------------------------
        // Anything else is not supported.
        throw new IllegalArgumentException(ConfigMessages.msg.noRawType(type));
    }

    // -----------------------------------------------------------------------
//     @ParameterizedTest(name = "{index}: rawTypeOf({0}) => {1}")
//     @MethodSource("classProvider")
//     void rawTypeOf_class(Type input, Class<?> expected) {
//         assertEquals(expected, ConfigMappingInterface.rawTypeOf(input));
//     }

    // -----------------------------------------------------------------------
//     @ParameterizedTest(name = "{index}: rawTypeOf(ParameterizedType of {0}) => {1}")
//     @MethodSource("parameterizedProvider")
//     void rawTypeOf_parameterizedType(Type input, Class<?> expected) {
//         assertEquals(expected, ConfigMappingInterface.rawTypeOf(input));
//     }

    // -----------------------------------------------------------------------
//     @Test
//     void rawTypeOf_genericArrayType_simple() throws NoSuchMethodException {
//         // List<String>[]
//         Method m = Sample.class.getMethod("arrayOfList");
//         // GenericArrayType
//         Type genericArray = m.getGenericReturnType();
//         Class<?> result = ConfigMappingInterface.rawTypeOf(genericArray);
//         assertTrue(result.isArray());
//         assertEquals(List.class, result.getComponentType());
//     }

//     @Test
//     void rawTypeOf_genericArrayType_typeVariableComponent() throws NoSuchMethodException {
//         // T[] where T is a type variable without bounds -> Object[]
//         Method m = Sample.class.getMethod("genericArray");
//         // GenericArrayType
//         Type genericArray = m.getGenericReturnType();
//         Class<?> result = ConfigMappingInterface.rawTypeOf(genericArray);
//         assertTrue(result.isArray());
//         assertEquals(Object.class, result.getComponentType());
//     }

//     @Test
//     void rawTypeOf_genericArrayType_typeVariableWithBound() {
//         // Simulate a GenericArrayType whose component type is a wildcard ? extends Number
//         GenericArrayType array = new GenericArrayType() {
// 
//             @Override
//             public Type getGenericComponentType() {
//                 return new WildcardType() {
// 
//                     @Override
//                     public Type[] getUpperBounds() {
//                         return new Type[] { Number.class };
//                     }
// 
//                     @Override
//                     public Type[] getLowerBounds() {
//                         return new Type[0];
//                     }
//                 };
//             }
//         };
//         Class<?> result = ConfigMappingInterface.rawTypeOf(array);
//         assertTrue(result.isArray());
//         assertEquals(Number.class, result.getComponentType());
//     }

    // -----------------------------------------------------------------------
//     @Test
//     void rawTypeOf_wildcardWithUpperBound() {
//         WildcardType wt = new WildcardType() {
// 
//             @Override
//             public Type[] getUpperBounds() {
//                 return new Type[] { Number.class };
//             }
// 
//             @Override
//             public Type[] getLowerBounds() {
//                 return new Type[0];
//             }
//         };
//         assertEquals(Number.class, ConfigMappingInterface.rawTypeOf(wt));
//     }

//     @Test
//     void rawTypeOf_wildcardWithoutUpperBound() {
//         WildcardType wt = new WildcardType() {
// 
//             @Override
//             public Type[] getUpperBounds() {
//                 return new Type[] { Object.class };
//             }
// 
//             @Override
//             public Type[] getLowerBounds() {
//                 return new Type[0];
//             }
//         };
//         assertEquals(Object.class, ConfigMappingInterface.rawTypeOf(wt));
//     }

//     @Test
//     void rawTypeOf_wildcardWithMultipleUpperBounds() {
//         // ? extends Serializable & Comparable
//         WildcardType wt = new WildcardType() {
// 
//             @Override
//             public Type[] getUpperBounds() {
//                 return new Type[] { Serializable.class, Comparable.class };
//             }
// 
//             @Override
//             public Type[] getLowerBounds() {
//                 return new Type[0];
//             }
//         };
//         assertEquals(Serializable.class, ConfigMappingInterface.rawTypeOf(wt));
//     }

//     @Test
//     void rawTypeOf_wildcardWithNullUpperBoundsArray() {
//         WildcardType wt = new WildcardType() {
// 
//             @Override
//             public Type[] getUpperBounds() {
//                 return null;
//             }
// 
//             @Override
//             public Type[] getLowerBounds() {
//                 return new Type[0];
//             }
//         };
//         assertEquals(Object.class, ConfigMappingInterface.rawTypeOf(wt));
//     }

//     @Test
//     void rawTypeOf_wildcardWithEmptyUpperBoundsArray() {
//         WildcardType wt = new WildcardType() {
// 
//             @Override
//             public Type[] getUpperBounds() {
//                 return new Type[0];
//             }
// 
//             @Override
//             public Type[] getLowerBounds() {
//                 return new Type[0];
//             }
//         };
//         // Accessing upperBounds[0] should throw ArrayIndexOutOfBoundsException
//         assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.rawTypeOf(wt));
//     }

//     @Test
//     void rawTypeOf_wildcardUpperBoundIsNullElement() {
//         WildcardType wt = new WildcardType() {
// 
//             @Override
//             public Type[] getUpperBounds() {
//                 return new Type[] { null };
//             }
// 
//             @Override
//             public Type[] getLowerBounds() {
//                 return new Type[0];
//             }
//         };
//         // The method will try to recurse with a null argument → NPE
//         assertThrows(NullPointerException.class, () -> ConfigMappingInterface.rawTypeOf(wt));
//     }

//     @Test
//     void rawTypeOf_wildcardUpperBoundIsTypeVariable() throws NoSuchMethodException {
//         // ? extends T where T is a type variable from Sample
//         TypeVariable<Class<Sample>> tv = Sample.class.getTypeParameters()[0];
//         WildcardType wt = new WildcardType() {
// 
//             @Override
//             public Type[] getUpperBounds() {
//                 return new Type[] { tv };
//             }
// 
//             @Override
//             public Type[] getLowerBounds() {
//                 return new Type[0];
//             }
//         };
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(wt));
//         assertTrue(ex.getMessage().contains("no raw type"));
//     }

    // -----------------------------------------------------------------------
//     @Test
//     void rawTypeOf_typeVariable_unsupported() {
//         TypeVariable<Class<Sample>> tv = Sample.class.getTypeParameters()[0];
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(tv));
//         assertTrue(ex.getMessage().contains("no raw type"));
//     }

//     @Test
//     void rawTypeOf_parameterizedTypeWithNonClassRaw() {
//         // Create a ParameterizedType whose raw type is a TypeVariable
//         TypeVariable<Class<Sample>> tv = Sample.class.getTypeParameters()[0];
//         ParameterizedType pt = new FakeParameterizedType(tv, new Type[] { String.class }, null);
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(pt));
//         assertTrue(ex.getMessage().contains("no raw type"));
//     }

//     @Test
//     void rawTypeOf_genericArrayType_componentUnsupported() {
//         // GenericArrayType whose component type is a TypeVariable (no bounds)
//         GenericArrayType gat = new GenericArrayType() {
// 
//             @Override
//             public Type getGenericComponentType() {
//                 // T
//                 return Sample.class.getTypeParameters()[0];
//             }
//         };
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(gat));
//         assertTrue(ex.getMessage().contains("no raw type"));
//     }

    // -----------------------------------------------------------------------
//     @Test
//     void rawTypeOf_nullInput_throwsNPE() {
//         assertThrows(NullPointerException.class, () -> ConfigMappingInterface.rawTypeOf(null));
//     }

    // -----------------------------------------------------------------------
//     @Test
//     void rawTypeOf_exceptionMessageSource() {
//         TypeVariable<Class<Sample>> tv = Sample.class.getTypeParameters()[0];
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(tv));
//         // The message is produced by ConfigMessages.msg.noRawType(Type)
//         // We check that it contains the fully‑qualified type name.
//         assertTrue(ex.getMessage().contains(tv.getName()));
//     }
}