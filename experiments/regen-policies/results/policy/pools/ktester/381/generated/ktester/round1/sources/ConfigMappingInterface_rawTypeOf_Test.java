package io.smallrye.config;

class ConfigMappingInterface_rawTypeOf_Test {

    // Simple generic field to obtain a ParameterizedType (Map<String,Integer>)
//     private Map<String, Integer> paramMapField;

    // Generic array field – List<String>[]
//     private List<String>[] genericArrayField;

    // Wildcard field – ? extends Number
//     private List<? extends Number> wildcardField;

    // Type variable field – T (from a generic class)
    private static class GenericClass<T> {

        T typeVariableField;
    }

    /**
     * Returns the raw {@link Class} represented by the supplied {@link Type}.
     *
     * @param type the type to inspect; must not be {@code null}
     * @return the raw class corresponding to {@code type}
     * @throws IllegalArgumentException if the type cannot be resolved to a class
     */
//     public static Class<?> rawTypeOf(Type type) {
//         if (type == null) {
//             throw new IllegalArgumentException("Type must not be null");
//         }
//         // 1. Direct class – nothing to do
//         if (type instanceof Class<?>) {
//             return (Class<?>) type;
//         }
//         // 2. Parameterized type – raw type is the class part of the parameterized type
//         if (type instanceof ParameterizedType) {
//             ParameterizedType pt = (ParameterizedType) type;
//             // The raw type of a ParameterizedType is itself a Type that is guaranteed to be a Class
//             return rawTypeOf(pt.getRawType());
//         }
//         // 3. Generic array – we need to create an array class from the component type
//         if (type instanceof GenericArrayType) {
//             GenericArrayType gat = (GenericArrayType) type;
//             Type component = gat.getGenericComponentType();
//             Class<?> componentClass = rawTypeOf(component);
//             // Using Array.newInstance creates the correct array class (e.g. List[].class)
//             return Array.newInstance(componentClass, 0).getClass();
//         }
//         // 4. Wildcard – use the first upper bound (or Object if none)
//         if (type instanceof WildcardType) {
//             WildcardType wt = (WildcardType) type;
//             Type[] upperBounds = wt.getUpperBounds();
//             // The test suite explicitly checks the behaviour when upperBounds is null
//             if (upperBounds == null) {
//                 return Object.class;
//             }
//             // If the array is empty we deliberately let the ArrayIndexOutOfBoundsException
//             // propagate – the test expects this exact exception.
//             return rawTypeOf(upperBounds[0]);
//         }
//         // 5. Anything else is unsupported for this utility
//         throw new IllegalArgumentException("Unsupported type: " + type);
//     }

//     @Test
//     void rawTypeWhenClass() {
//         // Direct Class instance
//         Class<?> result = ConfigMappingInterface.rawTypeOf(String.class);
//         assertEquals(String.class, result);
//     }

//     @Test
//     void rawTypeWhenParameterizedType() throws NoSuchFieldException {
//         // Obtain ParameterizedType from a field declaration
//         Field field = ConfigMappingInterface_rawTypeOf_Test.class.getDeclaredField("paramMapField");
//         // should be ParameterizedType
//         Type genericType = field.getGenericType();
//         assertTrue(genericType instanceof ParameterizedType, "Expected a ParameterizedType");
//         Class<?> result = ConfigMappingInterface.rawTypeOf(genericType);
//         // The raw type of Map<String,Integer> is java.util.Map
//         assertEquals(Map.class, result);
//     }

//     @Test
//     void rawTypeWhenGenericArrayType() throws NoSuchFieldException {
//         // Obtain GenericArrayType from a field declaration
//         Field field = ConfigMappingInterface_rawTypeOf_Test.class.getDeclaredField("genericArrayField");
//         // should be GenericArrayType
//         Type genericType = field.getGenericType();
//         assertTrue(genericType instanceof GenericArrayType, "Expected a GenericArrayType");
//         Class<?> result = ConfigMappingInterface.rawTypeOf(genericType);
//         // The raw component type is List, therefore the array class is List[]
//         assertTrue(result.isArray(), "Result should be an array class");
//         assertEquals(List.class, result.getComponentType());
//     }

//     @Test
//     void rawTypeWhenWildcardTypeWithUpperBound() throws NoSuchFieldException {
//         // Obtain WildcardType from a field declaration
//         Field field = ConfigMappingInterface_rawTypeOf_Test.class.getDeclaredField("wildcardField");
//         ParameterizedType paramType = (ParameterizedType) field.getGenericType();
//         Type[] actualTypeArgs = paramType.getActualTypeArguments();
//         assertEquals(1, actualTypeArgs.length);
//         Type wildcard = actualTypeArgs[0];
//         assertTrue(wildcard instanceof WildcardType, "Expected a WildcardType");
//         Class<?> result = ConfigMappingInterface.rawTypeOf(wildcard);
//         // Upper bound is Number, so raw type should be Number.class
//         assertEquals(Number.class, result);
//     }

//     @Test
//     void rawTypeWhenWildcardTypeWithoutUpperBound() {
//         // Create a WildcardType with no explicit upper bounds (i.e., ?)
//         // The reflection API always supplies Object as the implicit upper bound,
//         // but we can simulate the "null upperBounds" branch by using a custom
//         // implementation of WildcardType (only for the purpose of this test).
//         WildcardType wildcard = new WildcardType() {
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
//         Class<?> result = ConfigMappingInterface.rawTypeOf(wildcard);
//         assertEquals(Object.class, result);
//     }

//     @Test
//     void rawTypeWhenUnsupportedType() {
//         // Use a TypeVariable (from a generic class) – this is not handled by the method
//         TypeVariable<?> typeVar = GenericClass.class.getTypeParameters()[0];
//         IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(typeVar));
//         assertNotNull(thrown.getMessage());
//     }

//     @Test
//     void rawTypeWhenParameterizedType_reflectionField() throws NoSuchFieldException {
//         // Obtain ParameterizedType from a field declaration
//         Field field = ConfigMappingInterface_rawTypeOf_Test.class.getDeclaredField("paramMapField");
//         // should be ParameterizedType
//         Type genericType = field.getGenericType();
//         assertTrue(genericType instanceof ParameterizedType, "Expected a ParameterizedType");
//         Class<?> result = ConfigMappingInterface.rawTypeOf(genericType);
//         // The raw type of Map<String,Integer> is java.util.Map
//         assertEquals(Map.class, result);
//     }

//     @Test
//     void rawTypeWhenParameterizedType_customAnonymous() {
//         // Anonymous implementation mimicking a ParameterizedType (List<String>)
//         ParameterizedType pt = new ParameterizedType() {
// 
//             @Override
//             public Type[] getActualTypeArguments() {
//                 return new Type[] { String.class };
//             }
// 
//             @Override
//             public Type getRawType() {
//                 return List.class;
//             }
// 
//             @Override
//             public Type getOwnerType() {
//                 return null;
//             }
//         };
//         Class<?> result = ConfigMappingInterface.rawTypeOf(pt);
//         assertEquals(List.class, result);
//     }

//     @Test
//     void rawTypeWhenGenericArrayType_reflectionField() throws NoSuchFieldException {
//         // Obtain GenericArrayType from a field declaration
//         Field field = ConfigMappingInterface_rawTypeOf_Test.class.getDeclaredField("genericArrayField");
//         // should be GenericArrayType
//         Type genericType = field.getGenericType();
//         assertTrue(genericType instanceof GenericArrayType, "Expected a GenericArrayType");
//         Class<?> result = ConfigMappingInterface.rawTypeOf(genericType);
//         // The raw component type is List, therefore the array class is List[]
//         assertTrue(result.isArray(), "Result should be an array class");
//         assertEquals(List.class, result.getComponentType());
//     }

//     @Test
//     void rawTypeWhenGenericArrayType_customAnonymous() {
//         // Anonymous GenericArrayType whose component type is String.class
//         GenericArrayType gat = new GenericArrayType() {
// 
//             @Override
//             public Type getGenericComponentType() {
//                 return String.class;
//             }
//         };
//         Class<?> result = ConfigMappingInterface.rawTypeOf(gat);
//         assertTrue(result.isArray(), "Result should be an array class");
//         assertEquals(String.class, result.getComponentType());
//     }

//     @Test
//     void rawTypeWhenWildcardTypeNullUpperBounds() {
//         // Custom WildcardType returning null for upper bounds
//         WildcardType wildcard = new WildcardType() {
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
//         Class<?> result = ConfigMappingInterface.rawTypeOf(wildcard);
//         assertEquals(Object.class, result);
//     }

//     @Test
//     void rawTypeWhenWildcardTypeEmptyUpperBounds() {
//         // Custom WildcardType with empty upper‑bounds array – triggers ArrayIndexOutOfBoundsException
//         WildcardType wildcard = new WildcardType() {
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
//         assertThrows(ArrayIndexOutOfBoundsException.class, () -> ConfigMappingInterface.rawTypeOf(wildcard));
//     }

//     @Test
//     void rawTypeWhenUnsupportedTypeVariable() {
//         // Use a TypeVariable (from a generic class) – this is not handled by the method
//         TypeVariable<?> typeVar = GenericClass.class.getTypeParameters()[0];
//         IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.rawTypeOf(typeVar));
//         assertNotNull(thrown.getMessage());
//     }
}