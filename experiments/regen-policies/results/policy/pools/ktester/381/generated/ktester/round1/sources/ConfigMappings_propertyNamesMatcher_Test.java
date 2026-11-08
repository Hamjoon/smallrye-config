package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static org.junit.jupiter.api.Assertions.*;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigMappings_propertyNamesMatcher_Test {

    /* ----------------------------------------------------------------------
     *  Mocked static collaborators
     * ---------------------------------------------------------------------- */
    private MockedStatic<ConfigMappingLoader> loaderMock;

    private MockedStatic<ConfigMappingInterface> interfaceMock;

    @ConfigMapping(prefix = "app")
    interface AppConfig {

        String host();

        int port();
    }

    @ConfigMapping(prefix = "app")
    interface AppConfigExtra {

        String mode();
    }

    /* ----------------------------------------------------------------------
     *  Dummy @ConfigMapping interfaces used only as type tokens.
     *  No method bodies are required because the property information is
     *  supplied via mocks.
     * ---------------------------------------------------------------------- */
    @ConfigMapping(prefix = "")
    interface DummyConfig1 {
    }

    @ConfigMapping(prefix = "my.prefix")
    interface DummyConfig2 {
    }

    @ConfigMapping(prefix = "shared")
    interface DummyConfig3 {
    }

    @ConfigMapping(prefix = "shared")
    interface DummyConfig4 {
    }

    @ConfigMapping(prefix = "p1")
    interface DummyConfig5 {
    }

    @ConfigMapping(prefix = "p2")
    interface DummyConfig6 {
    }

    @ConfigMapping(prefix = "emptyProps")
    interface DummyConfig7 {
    }

    @ConfigMapping(prefix = "dup")
    interface DummyConfig8 {
    }

    @ConfigMapping(prefix = "dup")
    interface DummyConfig9 {
    }

    @ConfigMapping(prefix = "a")
    interface DummyConfig10 {
    }

    @ConfigMapping(prefix = "a.b")
    interface DummyConfig11 {
    }

    @ConfigMapping(prefix = "pA")
    interface DummyConfig12 {
    }

    @ConfigMapping(prefix = "pB")
    interface DummyConfig13 {
    }

    @ConfigMapping(prefix = "pA")
    interface DummyConfig14 {
    }

    @ConfigMapping(prefix = "valid")
    interface DummyConfig15 {
    }

    @ConfigMapping(prefix = "spec")
    interface DummyConfig16 {
    }

    @BeforeEach
    void setUp() {
        loaderMock = mockStatic(ConfigMappingLoader.class);
        interfaceMock = mockStatic(ConfigMappingInterface.class);
        // The constructor of ConfigClass calls a few static helpers – we make them
        // return empty data because the real values are irrelevant for the matcher.
        loaderMock.when(() -> ConfigMappingLoader.getConfigMappingClass(any())).thenAnswer(invocation -> invocation.getArgument(0));
        loaderMock.when(() -> ConfigMappingLoader.configMappingProperties(any())).thenReturn(Collections.emptyMap());
        loaderMock.when(() -> ConfigMappingLoader.configMappingSecrets(any())).thenReturn(Collections.emptySet());
    }

    @AfterEach
    void tearDown() {
        loaderMock.close();
        interfaceMock.close();
    }

    /* ----------------------------------------------------------------------
     *  Helper to prepare mocks for a given dummy config class and its property names
     * ---------------------------------------------------------------------- */
    private void mockConfigMapping(Class<?> configClass, String... propertyNames) {
        // Mock the ConfigMapping instance returned by ConfigMappingLoader
        Object configMapping = mock(Object.class, configClass.getSimpleName() + "Mapping");
        loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(configClass)).thenReturn(configMapping);
        // Build the nested map expected by the real method:
        //   Map<Class<?>, Map<String, Map<String, Property>>>
        Map<String, Property> leaf = new HashMap<>();
        for (String name : propertyNames) {
            leaf.put(name, mock(Property.class));
        }
        Map<String, Map<String, Property>> inner = new HashMap<>();
        // the empty string key is used by the implementation
        inner.put("", leaf);
        Map<Class<?>, Map<String, Map<String, Property>>> outer = new HashMap<>();
        outer.put(configClass, inner);
        interfaceMock.when(() -> ConfigMappingInterface.getProperties(configMapping)).thenReturn(outer);
    }

    @Test
    void propertyNamesMatcherMergesPropertiesFromMultipleConfigClasses() {
        // ----------------------------------------------------------------------
        // given – two ConfigClass instances that share the same prefix
        // ----------------------------------------------------------------------
        ConfigMappings.ConfigClass class1 = configClass(AppConfig.class, "app");
        ConfigMappings.ConfigClass class2 = configClass(AppConfigExtra.class, "app");
        // ----------------------------------------------------------------------
        // when – the matcher is built from the list of ConfigClass objects
        // ----------------------------------------------------------------------
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(class1, class2));
        // ----------------------------------------------------------------------
        // then – the matcher knows about all property names from both mappings
        // ----------------------------------------------------------------------
        assertNotNull(matcher, "The matcher must not be null");
        // The matcher should recognise the three properties defined above
        assertTrue(matcher.matches("app.host"), "Matcher should accept app.host");
        assertTrue(matcher.matches("app.port"), "Matcher should accept app.port");
        assertTrue(matcher.matches("app.mode"), "Matcher should accept app.mode");
        // And it should reject a property that does not exist in either mapping
        assertFalse(matcher.matches("app.unused"), "Matcher should reject unknown property app.unused");
    }

    @Test
    void propertyNamesMatcher_emptyList_returnsMatcherWithNoMatches() {
        // given
        List<ConfigMappings.ConfigClass> empty = List.of();
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(empty);
        // then – any arbitrary name must be rejected
        assertNotNull(matcher);
        assertFalse(matcher.matches("any.prefix"), "Matcher should reject any name when no classes are supplied");
    }

    @Test
    void propertyNamesMatcher_singleClassEmptyPrefix_oneProperty() {
        // given
        mockConfigMapping(DummyConfig1.class, "propA");
        ConfigMappings.ConfigClass cfg = configClass(DummyConfig1.class, "");
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(cfg));
        // then
        assertTrue(matcher.matches("propA"));
        assertFalse(matcher.matches("anyOther"));
    }

    @Test
    void propertyNamesMatcher_singleClassNonEmptyPrefix_multipleProperties() {
        // given
        mockConfigMapping(DummyConfig2.class, "alpha", "beta");
        ConfigMappings.ConfigClass cfg = configClass(DummyConfig2.class, "my.prefix");
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(cfg));
        // then
        assertTrue(matcher.matches("my.prefix.alpha"));
        assertTrue(matcher.matches("my.prefix.beta"));
        assertFalse(matcher.matches("my.prefix.gamma"));
        assertFalse(matcher.matches("other.prefix.alpha"));
    }

    @Test
    void propertyNamesMatcher_twoClassesSamePrefix_concatenatedOrder() {
        // given
        mockConfigMapping(DummyConfig3.class, "first", "second");
        mockConfigMapping(DummyConfig4.class, "third", "fourth");
        ConfigMappings.ConfigClass c1 = configClass(DummyConfig3.class, "shared");
        ConfigMappings.ConfigClass c2 = configClass(DummyConfig4.class, "shared");
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(c1, c2));
        // then – all four names are recognised
        assertTrue(matcher.matches("shared.first"));
        assertTrue(matcher.matches("shared.second"));
        assertTrue(matcher.matches("shared.third"));
        assertTrue(matcher.matches("shared.fourth"));
        // a name that does not exist must be rejected
        assertFalse(matcher.matches("shared.fifth"));
    }

    @Test
    void propertyNamesMatcher_twoClassesDifferentPrefixes_areIndependent() {
        // given
        mockConfigMapping(DummyConfig5.class, "one");
        mockConfigMapping(DummyConfig6.class, "two");
        ConfigMappings.ConfigClass c1 = configClass(DummyConfig5.class, "p1");
        ConfigMappings.ConfigClass c2 = configClass(DummyConfig6.class, "p2");
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(c1, c2));
        // then
        assertTrue(matcher.matches("p1.one"));
        assertTrue(matcher.matches("p2.two"));
        assertFalse(matcher.matches("p1.two"));
        assertFalse(matcher.matches("p2.one"));
    }

    @Test
    void propertyNamesMatcher_classWithEmptyPropertySet_createsEmptyEntry() {
        // given – no property names supplied for the dummy class
        // var‑args empty
        mockConfigMapping(DummyConfig7.class);
        ConfigMappings.ConfigClass cfg = configClass(DummyConfig7.class, "emptyProps");
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(cfg));
        // then – matcher must reject any property under that prefix
        assertFalse(matcher.matches("emptyProps.any"));
        assertFalse(matcher.matches("emptyProps."));
    }

    @Test
    void propertyNamesMatcher_duplicatePropertyNames_areNotDeduplicated() {
        // given
        mockConfigMapping(DummyConfig8.class, "x", "y");
        mockConfigMapping(DummyConfig9.class, "x", "z");
        ConfigMappings.ConfigClass c1 = configClass(DummyConfig8.class, "dup");
        ConfigMappings.ConfigClass c2 = configClass(DummyConfig9.class, "dup");
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(c1, c2));
        // then – all names appear (duplicates are harmless for matcher.matches)
        assertTrue(matcher.matches("dup.x"));
        assertTrue(matcher.matches("dup.y"));
        assertTrue(matcher.matches("dup.z"));
        // a non‑existent name must be rejected
        assertFalse(matcher.matches("dup.w"));
    }

    @Test
    void propertyNamesMatcher_subPrefixCollision_treatedIndependently() {
        // given
        mockConfigMapping(DummyConfig10.class, "p1");
        mockConfigMapping(DummyConfig11.class, "p2");
        ConfigMappings.ConfigClass c1 = configClass(DummyConfig10.class, "a");
        ConfigMappings.ConfigClass c2 = configClass(DummyConfig11.class, "a.b");
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(c1, c2));
        // then
        assertTrue(matcher.matches("a.p1"));
        assertTrue(matcher.matches("a.b.p2"));
        assertFalse(matcher.matches("a.p2"));
        assertFalse(matcher.matches("a.b.p1"));
    }

    @Test
    void propertyNamesMatcher_threeClassesTwoSharePrefix_cumulativeOrderRespected() {
        // given
        mockConfigMapping(DummyConfig12.class, "a1", "a2");
        mockConfigMapping(DummyConfig13.class, "b1");
        mockConfigMapping(DummyConfig14.class, "a3");
        ConfigMappings.ConfigClass c1 = configClass(DummyConfig12.class, "pA");
        ConfigMappings.ConfigClass c2 = configClass(DummyConfig13.class, "pB");
        ConfigMappings.ConfigClass c3 = configClass(DummyConfig14.class, "pA");
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(c1, c2, c3));
        // then – order does not affect matching, but all names must be present
        assertTrue(matcher.matches("pA.a1"));
        assertTrue(matcher.matches("pA.a2"));
        assertTrue(matcher.matches("pA.a3"));
        assertTrue(matcher.matches("pB.b1"));
        assertFalse(matcher.matches("pA.b1"));
        assertFalse(matcher.matches("pB.a1"));
    }

    @Test
    void propertyNamesMatcher_nullList_throwsNPE() {
        // given
        List<ConfigMappings.ConfigClass> nullList = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ConfigMappings.propertyNamesMatcher(nullList), "A null argument should cause a NullPointerException");
    }

    @Test
    void propertyNamesMatcher_containsNullElement_throwsNPE() {
        // given
        mockConfigMapping(DummyConfig15.class, "valid");
        ConfigMappings.ConfigClass valid = configClass(DummyConfig15.class, "valid");
        List<ConfigMappings.ConfigClass> list = Arrays.asList(valid, null);
        // when / then
        assertThrows(NullPointerException.class, () -> ConfigMappings.propertyNamesMatcher(list), "A list containing a null ConfigClass must cause a NullPointerException");
    }

    @Test
    void propertyNamesMatcher_specialCharactersInPropertyNames_areHandledVerbally() {
        // given
        mockConfigMapping(DummyConfig16.class, "simple", "complex.name", "list[0]", "list[1].sub");
        ConfigMappings.ConfigClass cfg = configClass(DummyConfig16.class, "spec");
        // when
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(List.of(cfg));
        // then – each exact string must match
        assertTrue(matcher.matches("spec.simple"));
        assertTrue(matcher.matches("spec.complex.name"));
        assertTrue(matcher.matches("spec.list[0]"));
        assertTrue(matcher.matches("spec.list[1].sub"));
        // something else must not match
        assertFalse(matcher.matches("spec.complex"));
        assertFalse(matcher.matches("spec.list[2]"));
    }
}
