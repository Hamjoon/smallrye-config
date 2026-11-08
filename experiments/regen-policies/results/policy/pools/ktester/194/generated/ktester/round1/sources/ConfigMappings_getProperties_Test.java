package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigMapping;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_getProperties_Test {

    @ConfigMapping(prefix = "sub")
    interface Sub {

        String subProp();
    }

    @ConfigMapping(prefix = "sub")
    interface Sub {

        String subProp();
    }

    @ConfigMapping(prefix = "sub")
    interface Sub {

        String subProp();
    }

    interface Nested {

        String value();
    }

    // Simple nested interface used by several tests
    interface Nested {

        String value();
    }

    // Simple interface used for many tests – no annotations
    interface DummyMapping {

        String prop1();

        String prop2();
    }

    // Interface without @ConfigMapping – used in test 7
    interface PlainInterface {

        String propA();

        String propB();
    }

    // Interface annotated with @ConfigProperties UNCONFIGURED_PREFIX
    @ConfigProperties(prefix = ConfigProperties.UNCONFIGURED_PREFIX)
    interface UnconfiguredDummy {

        String prop1();

        String prop2();
    }

    // Empty mapping used in test 4
    static class EmptyMapping {
    }

    // Mapping with nested sub‑path – used in test 10
    @ConfigMapping(prefix = "root")
    interface NestedMapping {

        String base();

        @ConfigMapping(prefix = "sub")
        interface Sub {

            String subProp();
        }
    }

    @ConfigMapping(prefix = "sub")
    interface Sub {

        String subProp();
    }

    @ConfigMapping(prefix = "sub")
    interface Sub {

        String subProp();
    }

    @ConfigMapping(prefix = "sub")
    interface Sub {

        String subProp();
    }

    // Simple sub‑path interface used in a few tests
    @ConfigMapping(prefix = "sub")
    interface Sub {

        String subProp();
    }

    // Class without any config annotations – used in test 12
    static class PlainClassWithoutAnnotations {
    }

    // Class where ConfigMappingInterface.getProperties returns null – test 13
    static class ClassReturningNullProperties {
    }

    // Class missing the root path entry – test 14
    @ConfigMapping(prefix = "missing")
    static class ClassMissingRootPath {
    }

    // Class with a null property key – test 15
    @ConfigMapping(prefix = "nullkey")
    static class ClassWithNullKey {
    }

    interface Nested {

        String value();
    }

    interface Nested {

        String value();
    }

    // Class annotated with @ConfigProperties (non‑UNCONFIGURED)
    @ConfigProperties(prefix = "service")
    static class ConfigPropertiesClass {

        public int timeout() {
            return 0;
        }

        public int retries() {
            return 0;
        }
    }

    @ConfigMapping(prefix = "my.prefix")
    interface ExplicitPrefixMapping {

        String value();

        Nested nested();

        interface Nested {

            String value();
        }
    }

    interface Nested {

        String value();
    }

    interface Nested {

        String value();
    }

    @ConfigMapping
    interface EmptyPrefixMapping {

        String value();
    }

    /* Helper methods to build the nested map returned by
     * ConfigMappingInterface.getProperties().
     * --------------------------------------------------------------------- */
    private static Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> buildOuterMap(Class<?> klass, Map<String, ConfigMappingInterface.Property> rootProperties, Map<String, Map<String, ConfigMappingInterface.Property>> otherPaths) {
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> outer = new HashMap<>();
        Map<String, Map<String, ConfigMappingInterface.Property>> inner = new HashMap<>();
        // root path (empty string) – always present unless the test explicitly wants it missing
        if (rootProperties != null) {
            inner.put("", rootProperties);
        }
        // additional (non‑root) paths, e.g. "sub"
        if (otherPaths != null) {
            inner.putAll(otherPaths);
        }
        outer.put(klass, inner);
        return outer;
    }

    private static ConfigMappingInterface.Property mockProperty(String value) {
        ConfigMappingInterface.Property p = Mockito.mock(ConfigMappingInterface.Property.class);
        Mockito.when(p.toString()).thenReturn("Property[value=" + value + "]");
        return p;
    }

    /**
     * Verify that the properties map contains the expected keys when a prefix is defined
     * via {@link ConfigMapping}.
     */
    @Test
    void getPropertiesWithExplicitPrefix() {
        // given
        ConfigMappings.ConfigClass cfgClass = configClass(ExplicitPrefixMapping.class);
        // when
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
        // then
        assertEquals(2, props.size(), "Expected two properties for ExplicitPrefixMapping");
        assertTrue(props.containsKey("my.prefix.value"), "Property key should be prefixed with the mapping prefix");
        assertTrue(props.containsKey("my.prefix.nested.value"), "Nested property key should also be prefixed");
    }

    /**
     * Verify that the properties map contains the expected keys when the mapping has no
     * explicit prefix (empty prefix).
     */
    @Test
    void getPropertiesWithEmptyPrefix() {
        // given
        ConfigMappings.ConfigClass cfgClass = configClass(EmptyPrefixMapping.class);
        // when
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
        // then
        assertEquals(1, props.size(), "Expected a single property for EmptyPrefixMapping");
        assertTrue(props.containsKey("value"), "When no prefix is defined the key should be the bare property name");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptyPrefixSimpleMapping() {
        // given
        ConfigMappings.ConfigClass cfgClass = configClass(DummyMapping.class, "");
        ConfigMappingInterface.Property p1 = mockProperty("val1");
        ConfigMappingInterface.Property p2 = mockProperty("val2");
        Map<String, ConfigMappingInterface.Property> root = Map.of("prop1", p1, "prop2", p2);
        // mock static collaborators
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(DummyMapping.class, root, null));
            // when
            Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
            // then
            assertEquals(Set.of("prop1", "prop2"), props.keySet(), "When the prefix is empty the raw property names must be returned");
            assertSame(p1, props.get("prop1"));
            assertSame(p2, props.get("prop2"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nonEmptyPrefixKeyConcatenation() {
        ConfigMappings.ConfigClass cfgClass = configClass(DummyMapping.class, "my.prefix");
        ConfigMappingInterface.Property p1 = mockProperty("val1");
        ConfigMappingInterface.Property p2 = mockProperty("val2");
        Map<String, ConfigMappingInterface.Property> root = Map.of("prop1", p1, "prop2", p2);
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(DummyMapping.class, root, null));
            Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
            assertEquals(Set.of("my.prefix.prop1", "my.prefix.prop2"), props.keySet(), "Prefix must be prepended (with a dot) to each property name");
            assertSame(p1, props.get("my.prefix.prop1"));
            assertSame(p2, props.get("my.prefix.prop2"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void unconfiguredPrefixIsTreatedAsEmpty() {
        ConfigMappings.ConfigClass cfgClass = configClass(UnconfiguredDummy.class, ConfigProperties.UNCONFIGURED_PREFIX);
        ConfigMappingInterface.Property p1 = mockProperty("val1");
        ConfigMappingInterface.Property p2 = mockProperty("val2");
        Map<String, ConfigMappingInterface.Property> root = Map.of("prop1", p1, "prop2", p2);
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(UnconfiguredDummy.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(UnconfiguredDummy.class, root, null));
            Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
            // after conversion the prefix becomes empty, therefore raw keys are expected
            assertEquals(Set.of("prop1", "prop2"), props.keySet());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptyMapWhenNoPropertiesReturned() {
        ConfigMappings.ConfigClass cfgClass = configClass(EmptyMapping.class, "any");
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(EmptyMapping.class)).thenReturn(mapping);
            // return an empty outer map – i.e. no entry for the class at all
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(Collections.emptyMap());
            Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
            assertTrue(props.isEmpty(), "When the underlying getProperties returns an empty map the result must be empty");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullPrefixThrowsNPE() {
        ConfigMappings.ConfigClass cfgClass = configClass(DummyMapping.class, null);
        ConfigMappingInterface.Property p1 = mockProperty("val1");
        Map<String, ConfigMappingInterface.Property> root = Map.of("prop1", p1);
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(DummyMapping.class, root, null));
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass), "A null prefix should cause a NullPointerException in the private prefix method");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullConfigClassArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(null), "Passing a null ConfigClass must result in a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void interfaceWithoutConfigMappingUsesEmptyPrefix() {
        // no annotation
        ConfigMappings.ConfigClass cfgClass = configClass(PlainInterface.class);
        ConfigMappingInterface.Property pA = mockProperty("valA");
        ConfigMappingInterface.Property pB = mockProperty("valB");
        Map<String, ConfigMappingInterface.Property> root = Map.of("propA", pA, "propB", pB);
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(PlainInterface.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(PlainInterface.class, root, null));
            Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
            assertEquals(Set.of("propA", "propB"), props.keySet());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void configPropertiesClassPrefixConcatenation() {
        ConfigMappings.ConfigClass cfgClass = configClass(ConfigPropertiesClass.class);
        ConfigMappingInterface.Property timeout = mockProperty("30");
        ConfigMappingInterface.Property retries = mockProperty("5");
        Map<String, ConfigMappingInterface.Property> root = Map.of("timeout", timeout, "retries", retries);
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ConfigPropertiesClass.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(ConfigPropertiesClass.class, root, null));
            Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
            assertEquals(Set.of("service.timeout", "service.retries"), props.keySet(), "Prefix from @ConfigProperties must be applied");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void prefixEndingWithDotAvoidsDoubleDot() {
        ConfigMappings.ConfigClass cfgClass = configClass(DummyMapping.class, "my.prefix.");
        ConfigMappingInterface.Property p1 = mockProperty("val1");
        ConfigMappingInterface.Property p2 = mockProperty("val2");
        Map<String, ConfigMappingInterface.Property> root = Map.of("prop1", p1, "prop2", p2);
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(DummyMapping.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(DummyMapping.class, root, null));
            Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
            // Expected: only one dot between prefix and key
            assertEquals(Set.of("my.prefix.prop1", "my.prefix.prop2"), props.keySet());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void onlyRootEntriesReturnedWhenNestedPathsExist() {
        ConfigMappings.ConfigClass cfgClass = configClass(NestedMapping.class, "root");
        ConfigMappingInterface.Property base = mockProperty("baseVal");
        Map<String, ConfigMappingInterface.Property> root = Map.of("base", base);
        // simulate a sub‑path "sub" that should be ignored by getProperties
        ConfigMappingInterface.Property subProp = mockProperty("subVal");
        Map<String, ConfigMappingInterface.Property> subPath = Map.of("subProp", subProp);
        Map<String, Map<String, ConfigMappingInterface.Property>> otherPaths = Map.of("sub", subPath);
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(NestedMapping.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(NestedMapping.class, root, otherPaths));
            Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);
            assertEquals(Set.of("root.base"), props.keySet(), "Only entries under the empty path ('') should be exposed");
            assertSame(base, props.get("root.base"));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullKlassInConfigClassThrowsNPE() {
        ConfigMappings.ConfigClass cfgClass = configClass(null, "anyPrefix");
        assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass), "A null klass inside ConfigClass must cause a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nonConfigurableClassPropagatesLoaderException() {
        ConfigMappings.ConfigClass cfgClass = configClass(PlainClassWithoutAnnotations.class, "");
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(PlainClassWithoutAnnotations.class)).thenThrow(new IllegalArgumentException("Not a config mapping"));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappings.getProperties(cfgClass));
            assertEquals("Not a config mapping", ex.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void configMappingInterfaceReturnsNullThrowsNPE() {
        ConfigMappings.ConfigClass cfgClass = configClass(ClassReturningNullProperties.class, "");
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ClassReturningNullProperties.class)).thenReturn(mapping);
            // simulate null return
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(null);
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void missingRootPathEntryThrowsNPE() {
        ConfigMappings.ConfigClass cfgClass = configClass(ClassMissingRootPath.class, "");
        // Build a map that contains only a non‑empty path entry
        Map<String, ConfigMappingInterface.Property> nonRoot = Map.of("some", mockProperty("x"));
        Map<String, Map<String, ConfigMappingInterface.Property>> otherPaths = Map.of("sub", nonRoot);
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ClassMissingRootPath.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(ClassMissingRootPath.class, null, otherPaths));
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullPropertyKeyThrowsNPEInPrefix() {
        ConfigMappings.ConfigClass cfgClass = configClass(ClassWithNullKey.class, "my.prefix");
        // Map contains a null key
        Map<String, ConfigMappingInterface.Property> root = new HashMap<>();
        root.put(null, mockProperty("val"));
        try (MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class);
            MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class)) {
            ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ClassWithNullKey.class)).thenReturn(mapping);
            ifaceMock.when(() -> ConfigMappingInterface.getProperties(mapping)).thenReturn(buildOuterMap(ClassWithNullKey.class, root, null));
            assertThrows(NullPointerException.class, () -> ConfigMappings.getProperties(cfgClass));
        }
    }
}
