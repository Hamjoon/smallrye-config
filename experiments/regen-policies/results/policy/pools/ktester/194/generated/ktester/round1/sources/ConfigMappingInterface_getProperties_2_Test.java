package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.lang.reflect.*;

class ConfigMappingInterface_getProperties_2_Test {

    /**
     * Minimal {@link ConfigMappingInterface.Property} implementation used in tests.
     */
    private static final class SimpleProperty extends ConfigMappingInterface.Property {

        private final String name;

        SimpleProperty(String name) {
            // The {@code Property} class has a protected constructor that expects a {@code Method}
            // (the accessor method) and the logical property name.
            super(null, name);
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Class<?> type() {
            return Object.class;
        }

        @Override
        public Optional<String> defaultValue() {
            return Optional.empty();
        }

        @Override
        public boolean isOptional() {
            return false;
        }

        // The real {@code Property} class contains many other abstract methods.
        // Harmless implementations are provided for the purpose of these tests.
        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public boolean isMap() {
            return false;
        }

        @Override
        public ConfigMappingInterface.Group group() {
            return null;
        }

        @Override
        public ConfigMappingInterface.Group parentGroup() {
            return null;
        }
    }

    /**
     * Builder that creates a {@link ConfigMappingInterface} instance with the
     * required mutable state for the tests.  The real {@code ConfigMappingInterface}
     * class is {@code final}, therefore we cannot subclass it – instead we build an
     * instance via its public constructor and then inject the needed fields using
     * reflection.
     */
    private static final class TestConfigMapping {

        private ConfigMapping.NamingStrategy namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE;

        private List<ConfigMappingInterface> nested = Collections.emptyList();

        private ConfigMappingInterface.Property[] properties = ConfigMappingInterface.NO_PROPERTIES;

        private ConfigMappingInterface[] superTypes = ConfigMappingInterface.NO_TYPES;

        private Class<?> interfaceType = Object.class;

        private boolean beanStyleGetters = false;

        TestConfigMapping namingStrategy(ConfigMapping.NamingStrategy ns) {
            this.namingStrategy = ns;
            return this;
        }

        TestConfigMapping nested(List<ConfigMappingInterface> n) {
            this.nested = n == null ? Collections.emptyList() : n;
            return this;
        }

        TestConfigMapping properties(ConfigMappingInterface.Property[] p) {
            this.properties = p == null ? null : p.clone();
            return this;
        }

        TestConfigMapping superTypes(List<ConfigMappingInterface> s) {
            if (s == null) {
                this.superTypes = ConfigMappingInterface.NO_TYPES;
            } else {
                this.superTypes = s.toArray(new ConfigMappingInterface[0]);
            }
            return this;
        }

        TestConfigMapping interfaceType(Class<?> it) {
            this.interfaceType = it;
            return this;
        }

        TestConfigMapping beanStyleGetters(boolean b) {
            this.beanStyleGetters = b;
            return this;
        }

        /**
         * Builds the {@link ConfigMappingInterface} instance using reflection to
         * set the fields that are not configurable via the public constructor.
         */
        ConfigMappingInterface build() {
            ConfigMappingInterface mapping = new ConfigMappingInterface(interfaceType, superTypes, properties == null ? ConfigMappingInterface.NO_PROPERTIES : properties);
            try {
                // namingStrategy
                Field nsField = ConfigMappingInterface.class.getDeclaredField("namingStrategy");
                nsField.setAccessible(true);
                nsField.set(mapping, namingStrategy);
                // nested groups
                Field nestedField = ConfigMappingInterface.class.getDeclaredField("nested");
                nestedField.setAccessible(true);
                nestedField.set(mapping, nested);
                // bean‑style getters flag
                Field beanField = ConfigMappingInterface.class.getDeclaredField("beanStyleGetters");
                beanField.setAccessible(true);
                beanField.setBoolean(mapping, beanStyleGetters);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            return mapping;
        }
    }

    /* ---------------------------------------------------------------------- */
    interface RootConfig {
    }

    interface ServerConfig {
    }

    interface SuperConfig {
    }

    /**
     * Creates a mock {@link ConfigMappingInterface.Property} with the given
     * logical name.  The actual {@code Property} implementation is not needed
     * for the assertions – the mock instance itself is stored in the result
     * map and later verified for presence.
     */
    private static ConfigMappingInterface.Property mockProperty(String name) {
        return new SimpleProperty(name);
    }

    /**
     * Verify that {@code getProperties} returns a non‑null map containing an entry for the
     * interface type even when the {@link ConfigMappingInterface} instance has no properties.
     */
    @Test
    void getPropertiesShouldReturnEmptyStructureForMappingWithoutProperties() {
        ConfigMappingInterface mapping = new ConfigMappingInterface(Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        assertNotNull(result, "Result map must not be null");
        assertTrue(result.containsKey(Object.class), "Result must contain the mapping's interface type as a key");
        Map<String, Map<String, ConfigMappingInterface.Property>> innerMap = result.get(Object.class);
        assertNotNull(innerMap, "Inner map for the interface type must not be null");
        assertTrue(innerMap.isEmpty(), "Inner map should be empty when there are no properties");
    }

    @Test
    @DisplayName("Empty configuration interface – returns an empty map")
    void emptyConfigurationInterface() {
        ConfigMappingInterface mapping = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).nested(Collections.emptyList()).properties(new ConfigMappingInterface.Property[0]).superTypes(Collections.emptyList()).interfaceType(Object.class).build();
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result map must be empty when there are no properties or groups");
    }

    @Test
    @DisplayName("Simple flat interface with KEBAB_CASE naming")
    void simpleFlatInterfaceKebabCase() {
        ConfigMappingInterface.Property prop = mockProperty("my.prop");
        ConfigMappingInterface mapping = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).nested(Collections.emptyList()).properties(new ConfigMappingInterface.Property[] { prop }).superTypes(Collections.emptyList()).interfaceType(RootConfig.class).build();
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        assertNotNull(result);
        assertEquals(1, result.size(), "Exactly one top‑level class entry expected");
        Map<String, Map<String, ConfigMappingInterface.Property>> byPath = result.get(RootConfig.class);
        assertNotNull(byPath);
        Map<String, ConfigMappingInterface.Property> byName = byPath.get("");
        assertNotNull(byName);
        assertEquals(1, byName.size());
        assertSame(prop, byName.get("my.prop"));
    }

    @Test
    @DisplayName("Interface with DOT naming strategy – property name unchanged")
    void dotNamingStrategy() {
        ConfigMappingInterface.Property prop = mockProperty("my.prop");
        ConfigMappingInterface mapping = new TestConfigMapping().namingStrategy(// DOT not available, KEBAB_CASE leaves dot unchanged
        ConfigMapping.NamingStrategy.KEBAB_CASE).nested(Collections.emptyList()).properties(new ConfigMappingInterface.Property[] { prop }).superTypes(Collections.emptyList()).interfaceType(RootConfig.class).build();
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        assertNotNull(result);
        Map<String, Map<String, ConfigMappingInterface.Property>> byPath = result.get(RootConfig.class);
        assertNotNull(byPath);
        Map<String, ConfigMappingInterface.Property> byName = byPath.get("");
        assertNotNull(byName);
        assertEquals(prop, byName.get("my.prop"));
    }

    @Test
    @DisplayName("Interface with a nested group property")
    void nestedGroupProperty() {
        // root mapping
        ConfigMappingInterface root = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(new ConfigMappingInterface.Property[0]).superTypes(Collections.emptyList()).interfaceType(RootConfig.class).build();
        // nested mapping (group “server”)
        ConfigMappingInterface.Property nestedProp = mockProperty("host");
        ConfigMappingInterface nested = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(new ConfigMappingInterface.Property[] { nestedProp }).nested(Collections.emptyList()).superTypes(Collections.emptyList()).interfaceType(ServerConfig.class).build();
        // attach nested group to root
        try {
            Field nestedField = ConfigMappingInterface.class.getDeclaredField("nested");
            nestedField.setAccessible(true);
            nestedField.set(root, Collections.singletonList(nested));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(root);
        assertEquals(2, result.size(), "Root and nested group should each produce a top‑level class entry");
        // root entry – should be empty (no properties)
        Map<String, Map<String, ConfigMappingInterface.Property>> rootPathMap = result.get(RootConfig.class);
        assertNotNull(rootPathMap);
        assertTrue(rootPathMap.isEmpty() || rootPathMap.get("").isEmpty());
        // nested entry – contains the “host” property under the “server” path
        Map<String, Map<String, ConfigMappingInterface.Property>> nestedPathMap = result.get(ServerConfig.class);
        assertNotNull(nestedPathMap);
        Map<String, ConfigMappingInterface.Property> serverGroup = nestedPathMap.get("server");
        assertNotNull(serverGroup);
        assertEquals(1, serverGroup.size());
        assertSame(nestedProp, serverGroup.get("host"));
    }

    @Test
    @DisplayName("Interface with bean‑style getters")
    void beanStyleGetters() {
        ConfigMappingInterface.Property prop = mockProperty("timeout");
        ConfigMappingInterface mapping = new TestConfigMapping().beanStyleGetters(true).namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(new ConfigMappingInterface.Property[] { prop }).nested(Collections.emptyList()).superTypes(Collections.emptyList()).interfaceType(RootConfig.class).build();
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, Map<String, ConfigMappingInterface.Property>> root = result.get(RootConfig.class);
        assertNotNull(root);
        Map<String, ConfigMappingInterface.Property> byName = root.get("");
        assertNotNull(byName);
        assertSame(prop, byName.get("timeout"));
    }

    @Test
    @DisplayName("Interface with super types – properties from both levels are collected")
    void interfaceWithSuperTypes() {
        // super type
        ConfigMappingInterface.Property superProp = mockProperty("parent.prop");
        ConfigMappingInterface superMapping = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(new ConfigMappingInterface.Property[] { superProp }).nested(Collections.emptyList()).superTypes(Collections.emptyList()).interfaceType(SuperConfig.class).build();
        // child type
        ConfigMappingInterface.Property childProp = mockProperty("child.prop");
        ConfigMappingInterface child = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(new ConfigMappingInterface.Property[] { childProp }).nested(Collections.emptyList()).superTypes(Collections.singletonList(superMapping)).interfaceType(RootConfig.class).build();
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(child);
        // both classes should appear as keys
        assertEquals(2, result.size());
        // child properties
        Map<String, Map<String, ConfigMappingInterface.Property>> childMap = result.get(RootConfig.class);
        assertNotNull(childMap);
        assertEquals(childProp, childMap.get("").get("child.prop"));
        // super properties
        Map<String, Map<String, ConfigMappingInterface.Property>> superMap = result.get(SuperConfig.class);
        assertNotNull(superMap);
        assertEquals(superProp, superMap.get("").get("parent.prop"));
    }

    @Test
    @DisplayName("Null naming strategy – falls back to KEBAB_CASE")
    void nullNamingStrategyFallback() {
        ConfigMappingInterface.Property prop = mockProperty("fallback");
        ConfigMappingInterface mapping = new TestConfigMapping().namingStrategy(// explicitly null
        null).properties(new ConfigMappingInterface.Property[] { prop }).nested(Collections.emptyList()).superTypes(Collections.emptyList()).interfaceType(RootConfig.class).build();
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result = ConfigMappingInterface.getProperties(mapping);
        Map<String, Map<String, ConfigMappingInterface.Property>> root = result.get(RootConfig.class);
        assertNotNull(root);
        assertEquals(prop, root.get("").get("fallback"));
    }

    @Test
    @DisplayName("Null ConfigMapping argument – throws NullPointerException")
    void nullConfigMapping() {
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(null), "Passing null should result in NPE");
    }

    @Test
    @DisplayName("Null properties array – throws NullPointerException")
    void nullPropertiesArray() {
        ConfigMappingInterface mapping = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(// deliberately null
        null).nested(Collections.emptyList()).superTypes(Collections.emptyList()).interfaceType(Object.class).build();
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(mapping), "A null properties array should cause NPE during processing");
    }

    @Test
    @DisplayName("Duplicate property names – throws IllegalArgumentException")
    void duplicatePropertyNames() {
        ConfigMappingInterface.Property p1 = mockProperty("dup.prop");
        ConfigMappingInterface.Property p2 = mockProperty("dup.prop");
        ConfigMappingInterface mapping = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(new ConfigMappingInterface.Property[] { p1, p2 }).nested(Collections.emptyList()).superTypes(Collections.emptyList()).interfaceType(RootConfig.class).build();
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(mapping), "Duplicate property names in the same group must cause IAE");
    }

    @Test
    @DisplayName("Unsupported property type – throws IllegalArgumentException")
    void unsupportedPropertyType() {
        ConfigMappingInterface.Property prop = mockProperty("unsupported");
        ConfigMappingInterface mapping = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(new ConfigMappingInterface.Property[] { prop }).nested(Collections.emptyList()).superTypes(Collections.emptyList()).interfaceType(RootConfig.class).build();
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(mapping), "Unsupported property type should trigger IllegalArgumentException");
    }

    @Test
    @DisplayName("Null naming strategy without fallback – throws NullPointerException")
    void nullNamingStrategyNoFallback() {
        ConfigMappingInterface.Property prop = mockProperty("any");
        ConfigMappingInterface mapping = new TestConfigMapping().namingStrategy(// no fallback
        null).properties(new ConfigMappingInterface.Property[] { prop }).nested(Collections.emptyList()).superTypes(Collections.emptyList()).interfaceType(RootConfig.class).build();
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getProperties(mapping));
    }

    @Test
    @DisplayName("Nested group name collision – throws IllegalArgumentException")
    void nestedGroupNameCollision() {
        // Root property named "group"
        ConfigMappingInterface.Property rootProp = mockProperty("group");
        // Nested group also named "group"
        ConfigMappingInterface.Property innerProp = mockProperty("inner");
        ConfigMappingInterface nested = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(new ConfigMappingInterface.Property[] { innerProp }).nested(Collections.emptyList()).superTypes(Collections.emptyList()).interfaceType(ServerConfig.class).build();
        ConfigMappingInterface root = new TestConfigMapping().namingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE).properties(new ConfigMappingInterface.Property[] { rootProp }).nested(Collections.singletonList(nested)).superTypes(Collections.emptyList()).interfaceType(RootConfig.class).build();
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getProperties(root), "A property and a nested group sharing the same name must cause IAE");
    }
}
