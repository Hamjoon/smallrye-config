package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.*;
import java.lang.annotation.Annotation;

class ConfigMappingInterface_getNested_Test {

    @ConfigMapping(prefix = "dummy")
    interface DummyConfig {
    }

    /* --------------------------------------------------------------------- */
    @ConfigMapping(prefix = "dummy")
    interface DummyConfig {
    }

    @ConfigMapping(prefix = "dummy")
    interface Dummy {
    }

    /* --------------------------------------------------------------------- */
    @ConfigMapping(prefix = "dummy")
    interface Dummy {
    }

    @ConfigMapping(prefix = "child")
    interface ChildConfig {

        String value();
    }

    @ConfigMapping(prefix = "parent")
    interface ParentConfig {

        ChildConfig child();
    }

    @ConfigMapping(prefix = "solo")
    interface SoloConfig {

        String name();
    }

    /* --------------------------------------------------------------------- */
    private static final class SimpleProperty implements ConfigMappingInterface.Property {

        private final Type returnType;

        private final Type genericReturnType;

        SimpleProperty(Type returnType) {
            this.returnType = returnType;
            this.genericReturnType = returnType;
        }

        @Override
        public Type getReturnType() {
            return returnType;
        }

        @Override
        public Type getGenericReturnType() {
            return genericReturnType;
        }

        @Override
        public Method getMethod() {
            return null;
        }

        @Override
        public ConfigMappingInterface.Path getPath() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getConfigKey() {
            return null;
        }

        @Override
        public Optional<String> getDefaultValue() {
            return Optional.empty();
        }

        @Override
        public boolean isOptional() {
            return false;
        }

        @Override
        public boolean isIndexed() {
            return false;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public boolean isMap() {
            return false;
        }

        @Override
        public boolean isCollection() {
            return false;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean isOptionalCollection() {
            return false;
        }

        @Override
        public boolean isOptionalArray() {
            return false;
        }

        @Override
        public boolean isOptionalMap() {
            return false;
        }

        @Override
        public boolean isOptionalLeaf() {
            return false;
        }

        @Override
        public boolean isOptionalValue() {
            return false;
        }
    }

    /* --------------------------------------------------------------------- */
    private static final class DummyGenericDeclaration implements GenericDeclaration {

        @Override
        public TypeVariable<?>[] getTypeParameters() {
            return new TypeVariable[0];
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
    }

    private static final class EmptyBoundsTypeVariable implements TypeVariable<GenericDeclaration> {

        private final String name = "T";

        @Override
        public Type[] getBounds() {
            return new Type[0];
        }

        @Override
        public GenericDeclaration getGenericDeclaration() {
            return new DummyGenericDeclaration();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public AnnotatedType[] getAnnotatedBounds() {
            return new AnnotatedType[0];
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
    }

    /**
     * Uses reflection to set the private {@code properties} field of a
     * {@link ConfigMappingInterface} instance.
     */
    private static void injectProperties(ConfigMappingInterface cmi, ConfigMappingInterface.Property[] props) {
        try {
            Field f = ConfigMappingInterface.class.getDeclaredField("properties");
            f.setAccessible(true);
            f.set(cmi, props);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link ConfigMappingInterface} instance for a dummy interface
     * (the interface itself is irrelevant because we will replace its properties
     * array with a mocked one).
     */
    private static ConfigMappingInterface createDummyCmi() {
        return ConfigMappingInterface.getConfigurationInterface(Dummy.class);
    }

    /**
     * Mocks a {@link ConfigMappingInterface.Property} that pretends to return the
     * supplied {@code returnType}.
     */
    private static ConfigMappingInterface.Property mockProperty(Type returnType) {
        return new SimpleProperty(returnType);
    }

    /**
     * Helper to build a {@link ParameterizedType} like {@code List<ChildConfig>}.
     */
    private static ParameterizedType parameterized(Type raw, Type... args) {
        return new ParameterizedType() {

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
                return null;
            }
        };
    }

    @Test
    void getNestedReturnsEmptyWhenNoNestedMappings() {
        ConfigMappingInterface solo = ConfigMappingInterface.getConfigurationInterface(SoloConfig.class);
        assertNotNull(solo, "ConfigMappingInterface for SoloConfig should not be null");
        List<ConfigMappingInterface> nested = solo.getNested();
        assertNotNull(nested, "Resulting list must not be null");
        assertTrue(nested.isEmpty(), "Expected no nested ConfigMappingInterface instances");
    }

    @Test
    void getNestedDetectsDirectNestedConfigurationInterface() {
        ConfigMappingInterface parent = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        assertNotNull(parent, "ConfigMappingInterface for ParentConfig should not be null");
        List<ConfigMappingInterface> nested = parent.getNested();
        assertNotNull(nested, "Resulting list must not be null");
        assertEquals(1, nested.size(), "Expected exactly one nested ConfigMappingInterface");
        ConfigMappingInterface child = nested.get(0);
        assertNotNull(child, "Nested ConfigMappingInterface instance must not be null");
        assertEquals(ChildConfig.class, child.getInterfaceType(), "The nested ConfigMappingInterface should correspond to ChildConfig");
    }

    @Test
    void branch_noPropertiesArray_returnsEmptyList() throws Exception {
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, null);
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertNotNull(nested);
        assertTrue(nested.isEmpty(), "Expected empty list when properties array is null");
    }

    @Test
    void branch_singleNestedProperty_returnsOneElement() {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface.Property prop = mockProperty(child.getInterfaceType());
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals(1, nested.size());
        assertEquals(ChildConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    void branch_multiplePropertiesWithDuplicates_deduplicatedAndOrdered() {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface.Property p1 = mockProperty(child.getInterfaceType());
        // duplicate
        ConfigMappingInterface.Property p2 = mockProperty(child.getInterfaceType());
        // not a config mapping
        ConfigMappingInterface.Property p3 = mockProperty(SoloConfig.class);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { p1, p2, p3 });
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals(1, nested.size(), "Duplicates must be removed");
        assertEquals(ChildConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    void branch_nullPropertyEntry_ignored() {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface.Property valid = mockProperty(child.getInterfaceType());
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { valid, null });
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals(1, nested.size());
        assertEquals(ChildConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    void advanced_directAndTransitiveNested() {
        ConfigMappingInterface c = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface b = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        ConfigMappingInterface a = createDummyCmi();
        ConfigMappingInterface.Property pA = mockProperty(b.getInterfaceType());
        ConfigMappingInterface.Property pB = mockProperty(c.getInterfaceType());
        injectProperties(b, new ConfigMappingInterface.Property[] { pB });
        injectProperties(a, new ConfigMappingInterface.Property[] { pA });
        List<ConfigMappingInterface> nested = a.getNested();
        assertEquals(2, nested.size());
        assertEquals(b.getInterfaceType(), nested.get(0).getInterfaceType());
        assertEquals(c.getInterfaceType(), nested.get(1).getInterfaceType());
    }

    @Test
    void advanced_circularReference_stopsAtFirstLevel() {
        ConfigMappingInterface a = createDummyCmi();
        ConfigMappingInterface b = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        ConfigMappingInterface.Property pA = mockProperty(b.getInterfaceType());
        ConfigMappingInterface.Property pB = mockProperty(a.getInterfaceType());
        injectProperties(a, new ConfigMappingInterface.Property[] { pA });
        injectProperties(b, new ConfigMappingInterface.Property[] { pB });
        List<ConfigMappingInterface> nested = a.getNested();
        assertEquals(1, nested.size());
        assertEquals(b.getInterfaceType(), nested.get(0).getInterfaceType());
    }

    @Test
    void advanced_collectionProperty_genericArgumentDetected() throws Exception {
        Type listType = parameterized(List.class, ChildConfig.class);
        ConfigMappingInterface.Property prop = mockProperty(listType);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals(1, nested.size());
        assertEquals(ChildConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    void advanced_optionalProperty_genericArgumentDetected() throws Exception {
        Type optType = parameterized(Optional.class, ChildConfig.class);
        ConfigMappingInterface.Property prop = mockProperty(optType);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals(1, nested.size());
        assertEquals(ChildConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    void advanced_arrayProperty_componentTypeDetected() {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        Class<?> arrayType = java.lang.reflect.Array.newInstance(child.getInterfaceType(), 0).getClass();
        ConfigMappingInterface.Property prop = mockProperty(arrayType);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals(1, nested.size());
        assertEquals(ChildConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    void advanced_wildcardUpperBoundDetected() throws Exception {
        WildcardType wildcard = new WildcardType() {

            @Override
            public Type[] getUpperBounds() {
                return new Type[] { ChildConfig.class };
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[0];
            }
        };
        ConfigMappingInterface.Property prop = mockProperty(wildcard);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals(1, nested.size());
        assertEquals(ChildConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    void advanced_nonConfigTypesIgnored() {
        ConfigMappingInterface.Property p1 = mockProperty(String.class);
        ConfigMappingInterface.Property p2 = mockProperty(Integer.class);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { p1, p2 });
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertTrue(nested.isEmpty(), "Non‑config types must be ignored");
    }

    @Test
    void advanced_preservesInsertionOrder() {
        ConfigMappingInterface b = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        ConfigMappingInterface c = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface d = ConfigMappingInterface.getConfigurationInterface(SoloConfig.class);
        ConfigMappingInterface.Property pB = mockProperty(b.getInterfaceType());
        ConfigMappingInterface.Property pC = mockProperty(c.getInterfaceType());
        ConfigMappingInterface.Property pD = mockProperty(d.getInterfaceType());
        ConfigMappingInterface a = createDummyCmi();
        injectProperties(a, new ConfigMappingInterface.Property[] { pB, pC, pD });
        List<ConfigMappingInterface> nested = a.getNested();
        assertEquals(3, nested.size());
        assertEquals(b.getInterfaceType(), nested.get(0).getInterfaceType());
        assertEquals(c.getInterfaceType(), nested.get(1).getInterfaceType());
        assertEquals(d.getInterfaceType(), nested.get(2).getInterfaceType());
    }

    @Test
    void exception_nullReturnType_throwsNullPointerException() {
        ConfigMappingInterface.Property prop = mockProperty(null);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        assertThrows(NullPointerException.class, cmi::getNested, "Null return type should cause NPE");
    }

    @Test
    void exception_nonInterfaceType_throwsIllegalArgumentException() {
        ConfigMappingInterface.Property prop = mockProperty(Date.class);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        assertThrows(IllegalArgumentException.class, cmi::getNested, "Non‑interface type should cause IllegalArgumentException");
    }

    @Test
    void exception_genericArrayRawTypeNotClass_throwsIllegalArgumentException() {
        GenericArrayType gar = new GenericArrayType() {

            @Override
            public Type getGenericComponentType() {
                return parameterized(List.class, String.class);
            }
        };
        ConfigMappingInterface.Property prop = mockProperty(gar);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        assertThrows(IllegalArgumentException.class, cmi::getNested);
    }

    @Test
    void exception_unboundedWildcard_throwsIllegalArgumentException() {
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
        ConfigMappingInterface.Property prop = mockProperty(unbounded);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        assertThrows(IllegalArgumentException.class, cmi::getNested);
    }

    @Test
    void exception_typeVariableWithoutBounds_throwsIllegalArgumentException() {
        TypeVariable<?> tv = new EmptyBoundsTypeVariable();
        ConfigMappingInterface.Property prop = mockProperty(tv);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        assertThrows(IllegalArgumentException.class, cmi::getNested);
    }

    @Test
    @Disabled("Running this test would cause a real StackOverflowError and halt the test suite.")
    void exception_circularReference_noCycleDetection_throwsStackOverflowError() {
        ConfigMappingInterface a = createDummyCmi();
        ConfigMappingInterface b = createDummyCmi();
        ConfigMappingInterface.Property pA = mockProperty(b.getInterfaceType());
        ConfigMappingInterface.Property pB = mockProperty(a.getInterfaceType());
        injectProperties(a, new ConfigMappingInterface.Property[] { pA });
        injectProperties(b, new ConfigMappingInterface.Property[] { pB });
        assertThrows(StackOverflowError.class, a::getNested);
    }

    @Test
    void exception_unsupportedGenericArrayComponent_throwsUnsupportedOperationException() {
        GenericArrayType gar = new GenericArrayType() {

            @Override
            public Type getGenericComponentType() {
                return parameterized(Optional.class, String.class);
            }
        };
        ConfigMappingInterface.Property prop = mockProperty(gar);
        ConfigMappingInterface cmi = createDummyCmi();
        injectProperties(cmi, new ConfigMappingInterface.Property[] { prop });
        assertThrows(UnsupportedOperationException.class, cmi::getNested);
    }
}
