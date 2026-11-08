package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getNested_Test {

    /**
     * Simple {@link ParameterizedType} implementation for tests.
     */
    private static class SimpleParameterizedType implements ParameterizedType {

        private final Type raw;

        private final Type[] args;

        private final Type owner;

        SimpleParameterizedType(Type raw, Type[] args, Type owner) {
            this.raw = raw;
            this.args = args.clone();
            this.owner = owner;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return args.clone();
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

    /**
     * Simple {@link WildcardType} implementation for tests.
     */
    private static class SimpleWildcardType implements WildcardType {

        private final Type[] upper;

        private final Type[] lower;

        SimpleWildcardType(Type[] upper, Type[] lower) {
            this.upper = upper.clone();
            this.lower = lower.clone();
        }

        @Override
        public Type[] getUpperBounds() {
            return upper.clone();
        }

        @Override
        public Type[] getLowerBounds() {
            return lower.clone();
        }
    }

    /**
     * Minimal implementation of {@link ConfigMappingInterface.Property} used for testing.
     */
    private static class SimpleProperty extends ConfigMappingInterface.Property {

        private final ConfigMappingInterface iface;

        private final Class<?> returnType;

        private final Type genericType;

        SimpleProperty(ConfigMappingInterface iface) {
            this(iface, null, null);
        }

        SimpleProperty(ConfigMappingInterface iface, Class<?> returnType, Type genericType) {
            // The superclass constructor expects (ConfigMappingInterface, Method) – we pass nulls because
            // we override the accessor methods below.
            super(null, null);
            this.iface = iface;
            this.returnType = returnType;
            this.genericType = genericType;
        }

        @Override
        public ConfigMappingInterface getInterface() {
            return iface;
        }

        @Override
        public Class<?> getReturnType() {
            return returnType;
        }

        @Override
        public Type getGenericType() {
            return genericType;
        }
    }

    /**
     * Creates a {@link ConfigMappingInterface} with the supplied mocked properties.
     */
    private ConfigMappingInterface createConfigMapping(ConfigMappingInterface.Property... props) {
        // The constructor used in the original test framework (package‑private)
        return new ConfigMappingInterface(/* interfaceType = */
        Object.class, /* superTypes    = */
        ConfigMappingInterface.NO_TYPES, /* properties    = */
        props);
    }

    /**
     * Verifies that {@code getNested()} returns an empty list when the
     * {@link ConfigMappingInterface} instance has no properties.
     */
    @Test
    void getNestedReturnsEmptyWhenNoProperties() {
        ConfigMappingInterface cfg = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        List<ConfigMappingInterface> nested = cfg.getNested();
        assertTrue(nested.isEmpty(), "Expected no nested configuration interfaces");
    }

    /**
     * Verifies that {@code getNested()} returns a list containing a single
     * nested configuration interface when a property refers to another
     * {@link ConfigMappingInterface}. The {@code Property} objects are mocked
     * because the internal logic of {@code getNested(Property[], Set)} only
     * inspects the {@code Property#getInterface()} method (as defined in the
     * SmallRye Config code base). By stubbing this method we can control the
     * behaviour without touching private fields.
     */
    @Test
    void getNestedCollectsNestedInterfaces() {
        ConfigMappingInterface nestedCfg = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface.Property mockProperty = new SimpleProperty(nestedCfg);
        ConfigMappingInterface cfg = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { mockProperty });
        List<ConfigMappingInterface> nested = cfg.getNested();
        assertEquals(1, nested.size(), "Expected exactly one nested configuration interface");
        assertSame(nestedCfg, nested.get(0), "The nested interface should be the one supplied by the mock");
    }

    @Test
    void getNestedEliminatesDuplicates() {
        ConfigMappingInterface nested = createConfigMapping();
        ConfigMappingInterface.Property p1 = new SimpleProperty(nested);
        // same instance
        ConfigMappingInterface.Property p2 = new SimpleProperty(nested);
        ConfigMappingInterface cfg = createConfigMapping(p1, p2);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertEquals(1, result.size(), "Duplicates must be removed");
        assertSame(nested, result.get(0), "The single nested interface should be present");
    }

    @Test
    void getNestedCollectsMultipleDistinctInterfaces() {
        ConfigMappingInterface n1 = createConfigMapping();
        ConfigMappingInterface n2 = createConfigMapping();
        ConfigMappingInterface.Property p1 = new SimpleProperty(n1);
        ConfigMappingInterface.Property p2 = new SimpleProperty(n2);
        ConfigMappingInterface cfg = createConfigMapping(p1, p2);
        List<ConfigMappingInterface> nested = cfg.getNested();
        assertEquals(2, nested.size(), "Both distinct nested interfaces must be present");
        assertTrue(nested.containsAll(List.of(n1, n2)), "Result must contain both nested interfaces");
    }

    @Test
    void getNestedHandlesOptionalWrapper() {
        ConfigMappingInterface nested = createConfigMapping();
        ConfigMappingInterface.Property prop = new SimpleProperty(null, Optional.class, new SimpleParameterizedType(Optional.class, new Type[] { nested.getInterfaceType() }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertEquals(1, result.size());
        assertSame(nested, result.get(0));
    }

    @Test
    void getNestedIgnoresOptionalNonConfig() {
        ConfigMappingInterface.Property prop = new SimpleProperty(null, Optional.class, new SimpleParameterizedType(Optional.class, new Type[] { String.class }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertTrue(result.isEmpty(), "Optional of non‑config type must be ignored");
    }

    @Test
    void getNestedHandlesCollectionWrapper() {
        ConfigMappingInterface nested = createConfigMapping();
        ConfigMappingInterface.Property prop = new SimpleProperty(null, List.class, new SimpleParameterizedType(List.class, new Type[] { nested.getInterfaceType() }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertEquals(1, result.size());
        assertSame(nested, result.get(0));
    }

    @Test
    void getNestedHandlesArrayWrapper() {
        ConfigMappingInterface nested = createConfigMapping();
        ConfigMappingInterface.Property prop = new SimpleProperty(null, nested.getInterfaceType().arrayType(), nested.getInterfaceType().arrayType());
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertEquals(1, result.size());
        assertSame(nested, result.get(0));
    }

    @Test
    void getNestedHandlesMapValue() {
        ConfigMappingInterface nested = createConfigMapping();
        ConfigMappingInterface.Property prop = new SimpleProperty(null, Map.class, new SimpleParameterizedType(Map.class, new Type[] { String.class, nested.getInterfaceType() }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertEquals(1, result.size());
        assertSame(nested, result.get(0));
    }

    @Test
    void getNestedIgnoresMapWithNonConfigValue() {
        ConfigMappingInterface.Property prop = new SimpleProperty(null, Map.class, new SimpleParameterizedType(Map.class, new Type[] { String.class, Integer.class }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertTrue(result.isEmpty(), "Map with non‑config value must be ignored");
    }

    @Test
    void getNestedHandlesWildcardUpperBound() {
        ConfigMappingInterface nested = createConfigMapping();
        WildcardType wildcard = new SimpleWildcardType(new Type[] { nested.getInterfaceType() }, new Type[0]);
        ConfigMappingInterface.Property prop = new SimpleProperty(null, List.class, new SimpleParameterizedType(List.class, new Type[] { wildcard }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertEquals(1, result.size());
        assertSame(nested, result.get(0));
    }

    @Test
    void getNestedIgnoresWildcardLowerBound() {
        WildcardType wildcard = new SimpleWildcardType(new Type[] { Object.class }, new Type[] { String.class });
        ConfigMappingInterface.Property prop = new SimpleProperty(null, List.class, new SimpleParameterizedType(List.class, new Type[] { wildcard }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertTrue(result.isEmpty(), "Wildcard with lower bound must be ignored");
    }

    @Test
    void getNestedHandlesSupplierWrapper() {
        ConfigMappingInterface nested = createConfigMapping();
        ConfigMappingInterface.Property prop = new SimpleProperty(null, Supplier.class, new SimpleParameterizedType(Supplier.class, new Type[] { nested.getInterfaceType() }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertEquals(1, result.size());
        assertSame(nested, result.get(0));
    }

    @Test
    void getNestedIgnoresSupplierNonConfig() {
        ConfigMappingInterface.Property prop = new SimpleProperty(null, Supplier.class, new SimpleParameterizedType(Supplier.class, new Type[] { String.class }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertTrue(result.isEmpty(), "Supplier of non‑config type must be ignored");
    }

    @Test
    void getNestedCollectsTransitiveNestedInterfaces() {
        ConfigMappingInterface levelThree = createConfigMapping();
        ConfigMappingInterface.Property pLevelTwo = new SimpleProperty(levelThree);
        ConfigMappingInterface levelTwo = createConfigMapping(pLevelTwo);
        ConfigMappingInterface.Property pLevelOne = new SimpleProperty(levelTwo);
        ConfigMappingInterface levelOne = createConfigMapping(pLevelOne);
        List<ConfigMappingInterface> result = levelOne.getNested();
        assertEquals(2, result.size(), "Both direct and transitive nested interfaces must be present");
        assertEquals(levelTwo, result.get(0), "First element must be the direct nested interface");
        assertEquals(levelThree, result.get(1), "Second element must be the transitive nested interface");
    }

    @Test
    void getNestedPreservesDeclarationOrder() {
        ConfigMappingInterface n1 = createConfigMapping();
        ConfigMappingInterface n2 = createConfigMapping();
        ConfigMappingInterface n3 = createConfigMapping();
        ConfigMappingInterface.Property p1 = new SimpleProperty(n1);
        ConfigMappingInterface.Property p2 = new SimpleProperty(n2);
        ConfigMappingInterface.Property p3 = new SimpleProperty(n3);
        ConfigMappingInterface cfg = createConfigMapping(p1, p2, p3);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertEquals(List.of(n1, n2, n3), result, "Insertion order must be preserved");
    }

    @Test
    void getNestedIgnoresRawCollection() {
        ConfigMappingInterface.Property prop = new // raw type
        SimpleProperty(// raw type
        null, List.class, List.class);
        ConfigMappingInterface cfg = createConfigMapping(prop);
        List<ConfigMappingInterface> result = cfg.getNested();
        assertTrue(result.isEmpty(), "Raw collection should be ignored");
    }

    @Test
    void privateHelperRejectsNullPropertiesArray() throws Exception {
        Method helper = ConfigMappingInterface.class.getDeclaredMethod("getNested", ConfigMappingInterface.Property[].class, Set.class);
        helper.setAccessible(true);
        Set<ConfigMappingInterface> set = new LinkedHashSet<>();
        assertThrows(NullPointerException.class, () -> helper.invoke(null, (Object) null, set), "Null Property[] must cause NPE");
    }

    @Test
    void privateHelperRejectsNullResultSet() throws Exception {
        Method helper = ConfigMappingInterface.class.getDeclaredMethod("getNested", ConfigMappingInterface.Property[].class, Set.class);
        helper.setAccessible(true);
        ConfigMappingInterface.Property[] empty = new ConfigMappingInterface.Property[0];
        assertThrows(NullPointerException.class, () -> helper.invoke(null, empty, null), "Null Set must cause NPE");
    }

    @Test
    void getNestedThrowsWhenPropertiesFieldIsNull() throws Exception {
        ConfigMappingInterface cfg = createConfigMapping();
        Field f = ConfigMappingInterface.class.getDeclaredField("properties");
        f.setAccessible(true);
        f.set(cfg, null);
        assertThrows(NullPointerException.class, cfg::getNested, "Null internal properties array must cause NPE");
    }

    @Test
    void getNestedThrowsWhenArrayContainsNullElement() throws Exception {
        ConfigMappingInterface.Property nullProp = null;
        ConfigMappingInterface cfg = createConfigMapping(nullProp);
        assertThrows(NullPointerException.class, cfg::getNested, "Null element inside properties array must cause NPE");
    }

    @Test
    void getNestedRejectsRawCollectionWithIllegalArgumentException() {
        ConfigMappingInterface.Property prop = new // raw collection
        SimpleProperty(// raw collection
        null, List.class, List.class);
        ConfigMappingInterface cfg = createConfigMapping(prop);
        assertThrows(IllegalArgumentException.class, cfg::getNested, "Raw collection should trigger IllegalArgumentException");
    }

    @Test
    void getNestedRejectsWildcardMapBothSides() {
        WildcardType any = new SimpleWildcardType(new Type[] { Object.class }, new Type[0]);
        ConfigMappingInterface.Property prop = new SimpleProperty(null, Map.class, new SimpleParameterizedType(Map.class, new Type[] { any, any }, null));
        ConfigMappingInterface cfg = createConfigMapping(prop);
        assertThrows(IllegalArgumentException.class, cfg::getNested, "Map with both wildcard sides must trigger IllegalArgumentException");
    }
}
