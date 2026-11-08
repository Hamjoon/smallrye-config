package io.smallrye.config;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_getConfigMappingsMetadata_Test {

    /**
     * Stand‑alone child interface used by other tests.
     */
    interface Child {

        String id();
    }

    /**
     * Another child‑nested interface used in tests.
     */
    interface ChildNested {
    }

    /**
     * Separate Global that extends the top‑level Common.
     */
    interface Global extends Common {

        String globalOnly();
    }

    interface Common {

        String commonOnly();
    }

    /**
     * Separate SuperNested to test name clashes.
     */
    interface SuperNested {
    }

    /**
     * Separate SubNested to test name clashes.
     */
    interface SubNested {
    }

    @ConfigProperties
    class GeneratedConfig implements SimpleConfig {

        @Override
        public String name() {
            return "generated";
        }
    }

    class NoInterfaceConfig {
    }

    @ConfigMapping(prefix = "super")
    interface SuperConfig {

        String superProp();

        interface SuperNested {
        }
    }

    interface SuperNested {
    }

    @ConfigMapping(prefix = "sub")
    interface SubConfig extends SuperConfig {

        String subProp();

        interface SubNested {
        }
    }

    interface SubNested {
    }

    @ConfigMapping(prefix = "simple")
    interface SimpleConfig {

        String name();
    }

    @ConfigMapping(prefix = "nested")
    interface NestedConfig {

        Map<String, Child> children();

        interface Child {

            String id();
        }
    }

    interface Child {

        String id();
    }

    interface Child {

        String id();
    }

    /**
     * Stand‑alone child interface used by other tests.
     */
    interface Child {

        String id();
    }

    interface ParentConfig {

        String parentProp();
    }

    interface ChildConfig extends ParentConfig {

        String childProp();

        interface ChildNested {
        }
    }

    interface ChildNested {
    }

    interface ChildNested {
    }

    /**
     * Another child‑nested interface used in tests.
     */
    interface ChildNested {
    }

    interface ComplexConfig {

        Global global();

        interface Global extends Common {

            String globalOnly();
        }

        interface Common {

            String commonOnly();
        }
    }

    interface Global extends Common {

        String globalOnly();
    }

    interface Common {

        String commonOnly();
    }

    interface Global extends Common {

        String globalOnly();
    }

    interface Common {

        String commonOnly();
    }

    /**
     * Separate Global that extends the top‑level Common.
     */
    interface Global extends Common {

        String globalOnly();
    }

    interface Common {

        String commonOnly();
    }

    /**
     * Plain class without any config‑mapping annotation.
     */
    class PlainClass {
    }

    /**
     * Generic interface – the presence of a type parameter should make it
     * ineligible for {@code ConfigMappingClass} creation.
     */
    @ConfigProperties
    interface GenericConfig<T> {

        T value();
    }

    @ConfigProperties
    class GeneratedConfig implements SimpleConfig {

        @Override
        public String name() {
            return "generated";
        }
    }

    /**
     * No annotations, no implemented config‑mapping interface.
     */
    class NoInterfaceConfig {
    }

    @ConfigMapping(prefix = "super")
    interface SuperConfig {

        String superProp();

        interface SuperNested {
        }
    }

    interface SuperNested {
    }

    interface SuperNested {
    }

    /**
     * Separate SuperNested to test name clashes.
     */
    interface SuperNested {
    }

    @ConfigMapping(prefix = "sub")
    interface SubConfig extends SuperConfig {

        String subProp();

        interface SubNested {
        }
    }

    interface SubNested {
    }

    interface SubNested {
    }

    /**
     * Separate SubNested to test name clashes.
     */
    interface SubNested {
    }

    @ConfigProperties
    class GeneratedConfig implements SimpleConfig {

        @Override
        public String name() {
            return "generated";
        }
    }

    class NoInterfaceConfig {
    }

    @ConfigMapping(prefix = "super")
    interface SuperConfig {

        String superProp();

        interface SuperNested {
        }
    }

    interface SuperNested {
    }

    interface SuperNested {
    }

    @ConfigMapping(prefix = "sub")
    interface SubConfig extends SuperConfig {

        String subProp();

        interface SubNested {
        }
    }

    interface SubNested {
    }

    interface SubNested {
    }

    @Test
    void simpleInterfaceReturnsOnlyItself() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(SimpleConfig.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertEquals(1, metadata.size(), "Simple interface should yield exactly one mapping");
        assertTrue(types.contains(SimpleConfig.class), "Returned mapping must contain SimpleConfig");
    }

    @Test
    void discoverNestedInterfaces() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(NestedConfig.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        // Expect the top‑level interface and its nested interface
        assertTrue(types.contains(NestedConfig.class), "Top level interface must be present");
        assertTrue(types.contains(NestedConfig.Child.class), "Nested interface must be present");
        // No extra types should be added
        assertEquals(2, types.size(), "Exactly two mappings expected for a single level of nesting");
    }

    @Test
    void parentInterfaceAndNestedAreBothReturned() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ChildConfig.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertTrue(types.contains(ChildConfig.class), "ChildConfig itself must be present");
        assertTrue(types.contains(ParentConfig.class), "Super‑type ParentConfig must be present");
        assertTrue(types.contains(ChildConfig.ChildNested.class), "Nested interface of ChildConfig must be present");
        // The total number of distinct interface types should be three
        assertEquals(3, types.size(), "Three distinct interface types expected");
    }

    @Test
    void nestedParentsBothReturned() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ComplexConfig.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertTrue(types.contains(ComplexConfig.class), "Root interface must be present");
        assertTrue(types.contains(ComplexConfig.Global.class), "Global (sub‑interface) must be present");
        assertTrue(types.contains(ComplexConfig.Common.class), "Common (shared parent) must be present");
        // Expect exactly three interface types – no duplicates
        assertEquals(3, types.size(), "Exactly three distinct interface types should be discovered");
    }

    @Test
    void classWithoutMappingAnnotationReturnsEmptyList() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(PlainClass.class);
        assertTrue(metadata.isEmpty(), "A class lacking @ConfigMapping should produce no metadata");
    }

    @Test
    void nonConfigClassReturnsEmptyList() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull(metadata, "Result must never be null");
        assertTrue(metadata.isEmpty(), "Non‑config class should produce an empty list");
    }

    @Test
    void nullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingsMetadata(null), "Passing null should result in NullPointerException");
    }

    @Test
    void primitiveTypeTriggersNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingsMetadata(int.class), "Calling with a primitive type should throw NPE");
    }

    @Test
    void enumTypeReturnsEmptyList() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(SampleEnum.class);
        assertNotNull(metadata);
        assertTrue(metadata.isEmpty(), "Enum types must yield an empty list");
    }

    @Test
    void genericInterfaceReturnsEmptyList() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(GenericConfig.class);
        assertNotNull(metadata);
        assertTrue(metadata.isEmpty(), "Generic config‑mapping interface should produce no metadata");
    }

    @Test
    void returnedListIsImmutable() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(SimpleConfig.class);
        assertThrows(UnsupportedOperationException.class, () -> metadata.add(null), "Modifying the returned list must throw UnsupportedOperationException");
        assertThrows(UnsupportedOperationException.class, () -> metadata.remove(0), "Removing from the returned list must also throw UnsupportedOperationException");
    }

    @Test
    void configMappingClassWithAssociatedInterface() {
        @ConfigProperties
        class GeneratedConfig implements SimpleConfig {

            @Override
            public String name() {
                return "generated";
            }
        }
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(GeneratedConfig.class);
        // We expect two entries: one for the class and one for the interface
        assertEquals(2, metadata.size(), "Generated class should yield class + interface metadata");
        // Verify that both expected interface types appear (order is class first)
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertEquals(GeneratedConfig.class, types.get(0), "First element must be the concrete class");
        assertEquals(SimpleConfig.class, types.get(1), "Second element must be the associated config interface");
    }

    @Test
    void concreteClassWithoutInterfaceCausesNPE() {
        class NoInterfaceConfig {
        }
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingsMetadata(NoInterfaceConfig.class), "A plain concrete class should cause NPE during recursion");
    }

    @Test
    void interfaceWithSuperTypeAndNestedMappings() {
        @ConfigMapping(prefix = "super")
        interface SuperConfig {

            String superProp();

            interface SuperNested {
            }
        }
        @ConfigMapping(prefix = "sub")
        interface SubConfig extends SuperConfig {

            String subProp();

            interface SubNested {
            }
        }
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(SubConfig.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        // Expected order: SubConfig, SubNested, SuperConfig, SuperNested
        assertEquals(4, types.size(), "Exactly four mappings expected");
        assertEquals(SubConfig.class, types.get(0), "First should be SubConfig");
        assertEquals(SubConfig.SubNested.class, types.get(1), "Second should be SubConfig's nested interface");
        assertEquals(SuperConfig.class, types.get(2), "Third should be SuperConfig");
        assertEquals(SuperConfig.SuperNested.class, types.get(3), "Fourth should be SuperConfig's nested interface");
    }
}
