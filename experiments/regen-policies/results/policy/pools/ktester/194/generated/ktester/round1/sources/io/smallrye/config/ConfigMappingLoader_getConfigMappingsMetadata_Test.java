package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;
// import static org.mockito.Mockito.*;
// import org.mockito.MockedStatic;
// import org.mockito.ArgumentMatchers;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_getConfigMappingsMetadata_Test {

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        boolean enabled();
    }

    interface Ssl {

        boolean enabled();
    }

    interface App {

        String name();
    }

    interface ServerParentNested {
    }

    interface ServerChildNested {
    }

    interface CommonConfig {

        java.util.Optional<String> commonTest();

        UserConfig user();
    }

    interface GlobalConfig extends CommonConfig {

        java.util.Optional<String> globalTest();
    }

    interface UserConfig {

        java.util.Optional<String> name();
    }

    interface SuperNested {
    }

//     interface SuperNested {
    }

    interface ParentNested {
    }

//     interface ParentNested {
    }

    interface BaseNested {
    }

//     interface BaseNested {
    }

    interface MidNested {
    }

//     interface MidNested {
    }

    interface DeepNested {
    }

//     interface DeepNested {
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface ServerParentNested {
    }

//     interface ServerParentNested {
    }

//     interface ServerChildNested {
    }

//     interface ServerChildNested {
    }

//     interface CommonConfig {

        java.util.Optional<String> commonTest();

        UserConfig user();
    }

//     interface GlobalConfig extends CommonConfig {

        java.util.Optional<String> globalTest();
    }

//     interface UserConfig {

        java.util.Optional<String> name();
    }

//     interface CommonConfig {

        java.util.Optional<String> commonTest();

        UserConfig user();
    }

//     interface GlobalConfig extends CommonConfig {

        java.util.Optional<String> globalTest();
    }

//     interface UserConfig {

        java.util.Optional<String> name();
    }

//     interface SuperNested {
    }

//     interface SuperNested {
    }

//     interface ParentNested {
    }

//     interface ParentNested {
    }

//     interface BaseNested {
    }

//     interface BaseNested {
    }

//     interface MidNested {
    }

//     interface MidNested {
    }

//     interface DeepNested {
    }

//     interface DeepNested {
    }

    /* -------------------------------------------------------------
     *  Simple @ConfigMapping interface – only the interface itself
     * ------------------------------------------------------------- */
    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    /* -------------------------------------------------------------
     *  Interface with nested types – all nested interfaces must appear
     * ------------------------------------------------------------- */
    public interface ServerNested {

        java.util.Map<String, Environment> environments();

        Log log();

        java.util.Optional<Ssl> ssl();

        java.util.List<App> apps();

        interface Environment {

            String host();

            int port();
        }

        interface Log {

            boolean enabled();
        }

        interface Ssl {

            boolean enabled();
        }

        interface App {

            String name();
        }
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

    /* -------------------------------------------------------------
     *  Interface that extends another interface – both parent and child
     *  (and their nested types) must be discovered
     * ------------------------------------------------------------- */
    interface ServerParent {

        ServerParentNested parentNested();

        interface ServerParentNested {
        }
    }

//     interface ServerParentNested {
    }

//     interface ServerParentNested {
    }

    interface ServerChild extends ServerParent {

        ServerChildNested childNested();

        interface ServerChildNested {
        }
    }

//     interface ServerChildNested {
    }

//     interface ServerChildNested {
    }

    /* -------------------------------------------------------------
     *  Deeply nested hierarchy – all interfaces in the tree must be
     *  present in the result
     * ------------------------------------------------------------- */
    interface MyConfig {

        GlobalConfig global();

        interface CommonConfig {

            java.util.Optional<String> commonTest();

            UserConfig user();
        }

        interface GlobalConfig extends CommonConfig {

            java.util.Optional<String> globalTest();
        }

        interface UserConfig {

            java.util.Optional<String> name();
        }
    }

//     interface CommonConfig {

        java.util.Optional<String> commonTest();

        UserConfig user();
    }

//     interface GlobalConfig extends CommonConfig {

        java.util.Optional<String> globalTest();
    }

//     interface UserConfig {

        java.util.Optional<String> name();
    }

//     interface CommonConfig {

        java.util.Optional<String> commonTest();

        UserConfig user();
    }

//     interface GlobalConfig extends CommonConfig {

        java.util.Optional<String> globalTest();
    }

//     interface UserConfig {

        java.util.Optional<String> name();
    }

    /* -------------------------------------------------------------
     *  Simple concrete class – no annotations required for ConfigMappingClass
     * ------------------------------------------------------------- */
    public static class SimpleConfigClass {

        public String foo = "bar";
    }

    /**
     * Minimal implementation of {@link ConfigMappingMetadata}.
     */
    private static class SimpleMetadata implements ConfigMappingMetadata {

        private final Class<?> iface;

        SimpleMetadata(Class<?> iface) {
            this.iface = iface;
        }

        @Override
        public Class<?> getInterfaceType() {
            return iface;
        }

        @Override
        public String getClassName() {
            return null;
        }

        @Override
        public byte[] getClassBytes() {
            return new byte[0];
        }
    }

    /**
     * Plain class – no annotations, not a configuration class.
     */
    static class PlainClass {
    }

    /**
     * Interface annotated with @ConfigProperties (no super‑type, no nested).
     */
    @ConfigProperties(prefix = "simple")
    interface SimpleConfigInterface {

        String value();
    }

    /**
     * Super‑type used in the “interface with nested and super types” case.
     */
    interface SuperConfigInterface {

        interface SuperNested {
        }
    }

//     interface SuperNested {
    }

//     interface SuperNested {
    }

    /**
     * Primary interface that extends {@link SuperConfigInterface}.
     */
    interface ParentConfigInterface extends SuperConfigInterface {

        interface ParentNested {
        }
    }

//     interface ParentNested {
    }

//     interface ParentNested {
    }

    /**
     * Concrete class that should generate a {@link ConfigMappingClass}.
     */
    static class ConcreteConfigClass {
    }

    /**
     * Abstract class – should never generate a {@link ConfigMappingClass}.
     */
    abstract static class AbstractConfigClass implements ConfigInterface {
    }

    /**
     * Simple configuration interface used by {@link ConfigInterface}.
     */
    @ConfigProperties(prefix = "cfg")
    interface ConfigInterface {
    }

    /**
     * Deep hierarchy – three levels of inheritance with nested types.
     */
    interface BaseConfigInterface {

        interface BaseNested {
        }
    }

//     interface BaseNested {
    }

//     interface BaseNested {
    }

    interface MidLevelConfigInterface extends BaseConfigInterface {

        interface MidNested {
        }
    }

//     interface MidNested {
    }

//     interface MidNested {
    }

    interface DeepHierarchyConfigInterface extends MidLevelConfigInterface {

        interface DeepNested {
        }
    }

//     interface DeepNested {
    }

//     interface DeepNested {
    }

    /**
     * Sample interface – no class mapping should be produced.
     */
    interface SampleInterface {
    }

    /* -------------------------------------------------------------
     *  Simple @ConfigMapping interface – only the interface itself
     * ------------------------------------------------------------- */
    @Test
    void simpleInterface() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(Server.class);
        List<ConfigMappingInterface> interfaces = metadata.stream().filter(ConfigMappingInterface.class::isInstance).map(ConfigMappingInterface.class::cast).collect(Collectors.toList());
        assertEquals(1, interfaces.size(), "Only the Server interface should be present");
        assertEquals(Server.class, interfaces.get(0).getInterfaceType());
    }

    /* -------------------------------------------------------------
     *  Interface with nested types – all nested interfaces must appear
     * ------------------------------------------------------------- */
    @Test
    void interfaceWithNested() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerNested.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).filter(Objects::nonNull).collect(Collectors.toList());
        assertTrue(types.contains(ServerNested.class));
        assertTrue(types.contains(ServerNested.Environment.class));
        assertTrue(types.contains(ServerNested.Log.class));
        assertTrue(types.contains(ServerNested.Ssl.class));
        assertTrue(types.contains(ServerNested.App.class));
        assertEquals(5, types.size(), "Root + 4 nested interfaces should be present");
    }

    /* -------------------------------------------------------------
     *  Interface that extends another interface – both parent and child
     *  (and their nested types) must be discovered
     * ------------------------------------------------------------- */
    @Test
    void interfaceWithParentAndNested() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerChild.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).filter(Objects::nonNull).collect(Collectors.toList());
        assertTrue(types.contains(ServerChild.class));
        assertTrue(types.contains(ServerChild.ServerChildNested.class));
        assertTrue(types.contains(ServerParent.class));
        assertTrue(types.contains(ServerParent.ServerParentNested.class));
        assertEquals(4, types.size(), "Child, child‑nested, parent and parent‑nested interfaces should be present");
    }

    /* -------------------------------------------------------------
     *  Deeply nested hierarchy – all interfaces in the tree must be
     *  present in the result
     * ------------------------------------------------------------- */
    @Test
    void deepNestedHierarchy() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(MyConfig.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).filter(Objects::nonNull).collect(Collectors.toList());
        assertTrue(types.contains(MyConfig.class));
        assertTrue(types.contains(MyConfig.GlobalConfig.class));
        assertTrue(types.contains(MyConfig.CommonConfig.class));
        assertTrue(types.contains(MyConfig.UserConfig.class));
        assertEquals(4, types.size(), "All four interfaces of MyConfig hierarchy should be present");
    }

    /* -------------------------------------------------------------
     *  Concrete class – should yield a ConfigMappingClass entry
     * ------------------------------------------------------------- */
    @Test
    void concreteClassProducesConfigMappingClass() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(SimpleConfigClass.class);
        assertEquals(1, metadata.size(), "A concrete class should produce a single ConfigMappingClass");
        assertTrue(metadata.get(0) instanceof ConfigMappingLoader.ConfigMappingClass, "The metadata element should be an instance of ConfigMappingClass");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void noMappingsBothNull() {
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingLoader.ConfigMappingClass> classMock = mockStatic(ConfigMappingLoader.ConfigMappingClass.class)) {
//             // Both look‑ups return null
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(PlainClass.class)).thenReturn(null);
//             classMock.when(() -> ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(PlainClass.class)).thenReturn(null);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(PlainClass.class);
//             assertTrue(result.isEmpty(), "Result must be an empty immutable list");
//         }
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void onlyConfigMappingInterfacePresent() {
//         // a mock metadata representing the interface
//         ConfigMappingInterface mockInterface = mock(ConfigMappingInterface.class);
//         when(mockInterface.getInterfaceType()).thenReturn(SimpleConfigInterface.class);
//         when(mockInterface.getNested()).thenReturn(Collections.emptyList());
//         when(mockInterface.getSuperTypes()).thenReturn(Collections.emptyList());
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingLoader.ConfigMappingClass> classMock = mockStatic(ConfigMappingLoader.ConfigMappingClass.class)) {
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(SimpleConfigInterface.class)).thenReturn(mockInterface);
//             classMock.when(() -> ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(SimpleConfigInterface.class)).thenReturn(null);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(SimpleConfigInterface.class);
//             assertEquals(1, result.size(), "Only one metadata element expected");
//             assertSame(mockInterface, result.get(0), "The element must be the mocked ConfigMappingInterface");
//         }
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void interfaceWithNestedAndSuperTypes() {
//         // Mock for the primary interface
//         ConfigMappingInterface mockParent = mock(ConfigMappingInterface.class);
//         ConfigMappingInterface mockParentNested = mock(ConfigMappingInterface.class);
//         ConfigMappingInterface mockSuper = mock(ConfigMappingInterface.class);
//         ConfigMappingInterface mockSuperNested = mock(ConfigMappingInterface.class);
//         when(mockParent.getInterfaceType()).thenReturn(ParentConfigInterface.class);
//         when(mockParent.getNested()).thenReturn(List.of(mockParentNested));
//         when(mockParent.getSuperTypes()).thenReturn(List.of(mockSuper));
//         when(mockParentNested.getInterfaceType()).thenReturn(ParentConfigInterface.ParentNested.class);
//         when(mockParentNested.getNested()).thenReturn(Collections.emptyList());
//         when(mockParentNested.getSuperTypes()).thenReturn(Collections.emptyList());
//         when(mockSuper.getInterfaceType()).thenReturn(SuperConfigInterface.class);
//         when(mockSuper.getNested()).thenReturn(List.of(mockSuperNested));
//         when(mockSuper.getSuperTypes()).thenReturn(Collections.emptyList());
//         when(mockSuperNested.getInterfaceType()).thenReturn(SuperConfigInterface.SuperNested.class);
//         when(mockSuperNested.getNested()).thenReturn(Collections.emptyList());
//         when(mockSuperNested.getSuperTypes()).thenReturn(Collections.emptyList());
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingLoader.ConfigMappingClass> classMock = mockStatic(ConfigMappingLoader.ConfigMappingClass.class)) {
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ParentConfigInterface.class)).thenReturn(mockParent);
//             classMock.when(() -> ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(ArgumentMatchers.any())).thenReturn(null);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(ParentConfigInterface.class);
//             // Expected order: parent, parent‑nested, super, super‑nested
//             assertEquals(4, result.size(), "Four metadata objects expected");
//             assertEquals(mockParent, result.get(0));
//             assertEquals(mockParentNested, result.get(1));
//             assertEquals(mockSuper, result.get(2));
//             assertEquals(mockSuperNested, result.get(3));
//         }
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void classMappingWithRecursiveInterfaceLookup() {
//         // Mock for the ConfigMappingClass generated from the concrete class
//         ConfigMappingLoader.ConfigMappingClass mockClassMeta = mock(ConfigMappingLoader.ConfigMappingClass.class);
//         when(mockClassMeta.getInterfaceType()).thenReturn(SimpleConfigInterface.class);
//         // Mock for the interface metadata that will be discovered recursively
//         ConfigMappingInterface mockInterface = mock(ConfigMappingInterface.class);
//         when(mockInterface.getInterfaceType()).thenReturn(SimpleConfigInterface.class);
//         when(mockInterface.getNested()).thenReturn(Collections.emptyList());
//         when(mockInterface.getSuperTypes()).thenReturn(Collections.emptyList());
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingLoader.ConfigMappingClass> classMock = mockStatic(ConfigMappingLoader.ConfigMappingClass.class);
//             MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
//             // No ConfigMappingInterface for the concrete class itself
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ConcreteConfigClass.class)).thenReturn(null);
//             // Class mapping is present
//             classMock.when(() -> ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(ConcreteConfigClass.class)).thenReturn(mockClassMeta);
//             // Recursive call – ConfigMappingLoader.getConfigMapping(...) should return the interface mock
//             loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(SimpleConfigInterface.class)).thenReturn(mockInterface);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(ConcreteConfigClass.class);
//             // Expected order: class metadata, then the interface metadata from the recursive call
//             assertEquals(2, result.size(), "Two metadata objects expected");
//             assertSame(mockClassMeta, result.get(0), "First element must be the ConfigMappingClass");
//             assertSame(mockInterface, result.get(1), "Second element must be the ConfigMappingInterface from recursion");
//         }
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void abstractClassIgnoresClassMapping() {
        // Mock for the interface that the abstract class implements
        ConfigMappingInterface mockInterface = mock(ConfigMappingInterface.class);
        when(mockInterface.getInterfaceType()).thenReturn(ConfigInterface.class);
        when(mockInterface.getNested()).thenReturn(Collections.emptyList());
        when(mockInterface.getSuperTypes()).thenReturn(Collections.emptyList());
        try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader.ConfigMappingClass> classMock = mockStatic(ConfigMappingLoader.ConfigMappingClass.class)) {
            // Abstract class does not produce a ConfigMappingClass
            classMock.when(() -> ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(AbstractConfigClass.class)).thenReturn(null);
            // Interface lookup succeeds
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(AbstractConfigClass.class)).thenReturn(mockInterface);
            List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(AbstractConfigClass.class);
            assertEquals(1, result.size(), "Only the interface metadata should be present");
            assertSame(mockInterface, result.get(0), "Result must contain the ConfigMappingInterface mock");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void deepHierarchyMultipleSuperTypes() {
        // Deep level
        ConfigMappingInterface deep = mock(ConfigMappingInterface.class);
        ConfigMappingInterface deepNested = mock(ConfigMappingInterface.class);
        // Mid level
        ConfigMappingInterface mid = mock(ConfigMappingInterface.class);
        ConfigMappingInterface midNested = mock(ConfigMappingInterface.class);
        // Base level
        ConfigMappingInterface base = mock(ConfigMappingInterface.class);
        ConfigMappingInterface baseNested = mock(ConfigMappingInterface.class);
        when(deep.getInterfaceType()).thenReturn(DeepHierarchyConfigInterface.class);
        when(deep.getNested()).thenReturn(List.of(deepNested));
        when(deep.getSuperTypes()).thenReturn(List.of(mid));
        when(deepNested.getInterfaceType()).thenReturn(DeepHierarchyConfigInterface.DeepNested.class);
        when(deepNested.getNested()).thenReturn(Collections.emptyList());
        when(deepNested.getSuperTypes()).thenReturn(Collections.emptyList());
        when(mid.getInterfaceType()).thenReturn(MidLevelConfigInterface.class);
        when(mid.getNested()).thenReturn(List.of(midNested));
        when(mid.getSuperTypes()).thenReturn(List.of(base));
        when(midNested.getInterfaceType()).thenReturn(MidLevelConfigInterface.MidNested.class);
        when(midNested.getNested()).thenReturn(Collections.emptyList());
        when(midNested.getSuperTypes()).thenReturn(Collections.emptyList());
        when(base.getInterfaceType()).thenReturn(BaseConfigInterface.class);
        when(base.getNested()).thenReturn(List.of(baseNested));
        when(base.getSuperTypes()).thenReturn(Collections.emptyList());
        when(baseNested.getInterfaceType()).thenReturn(BaseConfigInterface.BaseNested.class);
        when(baseNested.getNested()).thenReturn(Collections.emptyList());
        when(baseNested.getSuperTypes()).thenReturn(Collections.emptyList());
        try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingLoader.ConfigMappingClass> classMock = mockStatic(ConfigMappingLoader.ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(DeepHierarchyConfigInterface.class)).thenReturn(deep);
            classMock.when(() -> ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(ArgumentMatchers.any())).thenReturn(null);
            List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(DeepHierarchyConfigInterface.class);
            // Expected order: deep, deep‑nested, mid, mid‑nested, base, base‑nested
            assertEquals(6, result.size(), "Six metadata objects expected");
            assertEquals(deep, result.get(0));
            assertEquals(deepNested, result.get(1));
            assertEquals(mid, result.get(2));
            assertEquals(midNested, result.get(3));
            assertEquals(base, result.get(4));
            assertEquals(baseNested, result.get(5));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingsMetadata(null), "Passing null must result in NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void primitiveTypeThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingsMetadata(int.class), "int.class should cause NullPointerException from generateInterfaceName");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void arrayTypeThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingsMetadata(String[].class), "Array class should cause NullPointerException from generateInterfaceName");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void enumTypeReturnsEmptyList() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(SampleEnum.class);
        assertTrue(result.isEmpty(), "Enum types must yield an empty immutable list");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void interfaceTypeOnlyInterfaceProcessing() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(SampleInterface.class);
        assertTrue(result.isEmpty(), "Interface without @ConfigProperties should result in an empty list");
    }
}