package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
// import org.eclipse.microprofile.config.inject.ConfigMapping;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_getConfigMappingsMetadata_Test {

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        String level();
    }

    interface Ssl {

        boolean enabled();
    }

    interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        String level();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

    interface ServerParentNested {

        String value();
    }

//     interface ServerParentNested {

        String value();
    }

    interface ServerChildNested {

        String childValue();
    }

//     interface ServerChildNested {

        String childValue();
    }

    // Simple helper interfaces used in the tests
//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        String level();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface ServerParentNested {

        String value();
    }

//     interface ServerChildNested {

        String childValue();
    }

    static class NoMappingClass {
    }

    @ConfigMapping(prefix = "parent")
    interface ParentConfigInterface$Nested {
    }

    @ConfigMapping(prefix = "child")
    interface ChildConfigInterface$Nested {
    }

    @ConfigMapping(prefix = "grand")
    interface GrandParentConfigInterface {
    }

    @ConfigMapping(prefix = "complex")
    interface ComplexConfigInterface {
    }

    @ConfigProperties(prefix = "complex")
    static class ComplexConfigClass {
    }

    abstract static class AbstractConfigClass {
    }

    @ConfigMapping(prefix = "sub")
    interface SubClassConfigInterface {
    }

    @ConfigMapping(prefix = "super")
    interface SuperClassConfigInterface {
    }

    @ConfigProperties(prefix = "sub")
    static class SubClassConfig {
    }

    @ConfigProperties(prefix = "orphan")
    static class OrphanConfigClass {
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        String level();
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

        String level();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface ServerParentNested {

        String value();
    }

//     interface ServerParentNested {

        String value();
    }

//     interface ServerChildNested {

        String childValue();
    }

//     interface ServerChildNested {

        String childValue();
    }

    /* Dummy classes used only for testing purposes */
    class EnumConfigClass {
    }

    @ConfigMapping(prefix = "simple")
    interface SimpleConfigInterface {
    }

    @ConfigMapping(prefix = "parent")
    interface ParentConfigInterface {
    }

    @ConfigMapping(prefix = "child")
    interface ChildConfigInterface extends ParentConfigInterface {
    }

    @ConfigProperties(prefix = "class")
    static class ConcreteConfigClass {
    }

    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    @ConfigMapping(prefix = "server")
    public interface ServerNested {

        Map<String, Environment> environments();

        Log log();

        Optional<Ssl> ssl();

        List<App> apps();

        interface Environment {

            String host();

            int port();
        }

        interface Log {

            String level();
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

        String level();
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

        String level();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

    @ConfigMapping(prefix = "parent")
    interface ServerParent {

        ServerParentNested parentNested();

        interface ServerParentNested {

            String value();
        }
    }

//     interface ServerParentNested {

        String value();
    }

//     interface ServerParentNested {

        String value();
    }

    @ConfigMapping(prefix = "child")
    interface ServerChild extends ServerParent {

        ServerChildNested childNested();

        interface ServerChildNested {

            String childValue();
        }
    }

//     interface ServerChildNested {

        String childValue();
    }

//     interface ServerChildNested {

        String childValue();
    }

    @ConfigProperties(prefix = "server")
    public static class ServerProperties {

        String host;

        int port;
    }

    @ConfigMapping(prefix = "server")
    public interface ServerMixed {

        String host();

        int port();
    }

    public static class ServerMixedImpl implements ServerMixed {

        private final String host = "localhost";

        private final int port = 8080;

        @Override
        public String host() {
            return host;
        }

        @Override
        public int port() {
            return port;
        }
    }

    @ConfigMapping(prefix = "generic")
    interface GenericConfigInterface<T> {
    }

    @Test
    void simpleConfigMappingInterface() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(Server.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertEquals(1, types.size(), "Only the Server interface should be present");
        assertTrue(types.contains(Server.class), "Metadata must contain Server.class");
    }

    @Test
    void interfaceWithNestedTypes() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerNested.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertTrue(types.contains(ServerNested.class), "Parent interface should be present");
        assertTrue(types.contains(ServerNested.Environment.class), "Nested Environment should be present");
        assertTrue(types.contains(ServerNested.Log.class), "Nested Log should be present");
        assertTrue(types.contains(ServerNested.Ssl.class), "Nested Ssl should be present");
        assertTrue(types.contains(ServerNested.App.class), "Nested App should be present");
        assertEquals(5, types.stream().distinct().count(), "Exactly five metadata entries expected");
    }

    @Test
    void interfaceExtendingParentWithNested() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerChild.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertTrue(types.contains(ServerChild.class), "Child interface should be present");
        assertTrue(types.contains(ServerChild.ServerChildNested.class), "Child's nested type should be present");
        assertTrue(types.contains(ServerParent.class), "Parent interface should be present");
        assertTrue(types.contains(ServerParent.ServerParentNested.class), "Parent's nested type should be present");
        assertEquals(4, types.stream().distinct().count(), "Exactly four metadata entries expected");
    }

    @Test
    void classAnnotatedWithConfigProperties() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerProperties.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertEquals(1, types.size(), "Only the ServerProperties class should be present");
        assertTrue(types.contains(ServerProperties.class), "Metadata must contain ServerProperties.class");
    }

    @Test
    void mixedInterfaceAndClass() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerMixed.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        // The loader should expose both the interface and the concrete implementation
        assertTrue(types.contains(ServerMixed.class), "Interface type should be present");
        assertTrue(types.contains(ServerMixedImpl.class), "Implementation class should be present");
        assertEquals(2, types.size(), "Exactly two metadata entries expected");
    }

//     @Test
//     void noConfigMappingInterfaceAndNoConfigMappingClass() {
//         // Both helpers return null → empty result
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingClass> classMock = mockStatic(ConfigMappingClass.class)) {
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(NoMappingClass.class)).thenReturn(null);
//             classMock.when(() -> ConfigMappingClass.getConfigurationClass(NoMappingClass.class)).thenReturn(null);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(NoMappingClass.class);
//             assertTrue(result.isEmpty(), "Result must be empty when no interface nor class metadata is found");
//         }
//     }

//     @Test
//     void onlyConfigMappingInterfacePresent_noSuperNoNested() {
//         // Interface metadata only, without super‑types or nested types
//         ConfigMappingInterface iface = mock(ConfigMappingInterface.class);
//         when(iface.getNested()).thenReturn(Collections.emptyList());
//         when(iface.getSuperTypes()).thenReturn(Collections.emptyList());
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingClass> classMock = mockStatic(ConfigMappingClass.class)) {
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(SimpleConfigInterface.class)).thenReturn(iface);
//             classMock.when(() -> ConfigMappingClass.getConfigurationClass(SimpleConfigInterface.class)).thenReturn(null);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(SimpleConfigInterface.class);
//             assertEquals(1, result.size(), "Only the interface metadata should be present");
//             assertSame(iface, result.get(0), "Returned metadata must be the mocked interface");
//         }
//     }

//     @Test
//     void configMappingInterfaceWithNestedMappings() {
//         // Parent interface + a single nested interface
//         ConfigMappingInterface parent = mock(ConfigMappingInterface.class);
//         ConfigMappingInterface nested = mock(ConfigMappingInterface.class);
//         when(parent.getNested()).thenReturn(List.of(nested));
//         when(parent.getSuperTypes()).thenReturn(Collections.emptyList());
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingClass> classMock = mockStatic(ConfigMappingClass.class)) {
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ParentConfigInterface.class)).thenReturn(parent);
//             classMock.when(() -> ConfigMappingClass.getConfigurationClass(ParentConfigInterface.class)).thenReturn(null);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(ParentConfigInterface.class);
//             assertEquals(2, result.size(), "Parent + its nested interface should be returned");
//             assertEquals(parent, result.get(0), "First element must be the parent");
//             assertEquals(nested, result.get(1), "Second element must be the nested interface");
//         }
//     }

//     @Test
//     void configMappingInterfaceWithSuperTypesAndNestedMappings() {
//         // Child → nested, Super → nested, GrandParent (no nested)
//         ConfigMappingInterface child = mock(ConfigMappingInterface.class);
//         ConfigMappingInterface childNested = mock(ConfigMappingInterface.class);
//         ConfigMappingInterface parent = mock(ConfigMappingInterface.class);
//         ConfigMappingInterface parentNested = mock(ConfigMappingInterface.class);
//         ConfigMappingInterface grandParent = mock(ConfigMappingInterface.class);
//         when(child.getNested()).thenReturn(List.of(childNested));
//         when(child.getSuperTypes()).thenReturn(List.of(parent));
//         when(parent.getNested()).thenReturn(List.of(parentNested));
//         when(parent.getSuperTypes()).thenReturn(List.of(grandParent));
//         when(grandParent.getNested()).thenReturn(Collections.emptyList());
//         when(grandParent.getSuperTypes()).thenReturn(Collections.emptyList());
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingClass> classMock = mockStatic(ConfigMappingClass.class)) {
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ChildConfigInterface.class)).thenReturn(child);
//             classMock.when(() -> ConfigMappingClass.getConfigurationClass(ChildConfigInterface.class)).thenReturn(null);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(ChildConfigInterface.class);
//             assertEquals(5, result.size(), "Expected 5 entries: child, childNested, parent, parentNested, grandParent");
//             assertEquals(child, result.get(0));
//             assertEquals(childNested, result.get(1));
//             assertEquals(parent, result.get(2));
//             assertEquals(parentNested, result.get(3));
//             assertEquals(grandParent, result.get(4));
//         }
//     }

//     @Test
//     void onlyConfigMappingClassPresent_noInterface() {
//         // Class metadata only; the recursive call should resolve to an empty list
//         ConfigMappingClass classMeta = mock(ConfigMappingClass.class);
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingClass> classMock = mockStatic(ConfigMappingClass.class);
//             MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ConcreteConfigClass.class)).thenReturn(null);
//             classMock.when(() -> ConfigMappingClass.getConfigurationClass(ConcreteConfigClass.class)).thenReturn(classMeta);
//             // getConfigMapping(ConcreteConfigClass) → a mock that reports an interface type
//             ConfigMappingInterface mappingForRecursion = mock(ConfigMappingInterface.class);
//             when(mappingForRecursion.getInterfaceType()).thenReturn(Object.class);
//             loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ConcreteConfigClass.class)).thenReturn(mappingForRecursion);
//             // For the recursive call (Object.class) both helpers return null
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(Object.class)).thenReturn(null);
//             classMock.when(() -> ConfigMappingClass.getConfigurationClass(Object.class)).thenReturn(null);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(ConcreteConfigClass.class);
//             assertEquals(1, result.size(), "Only the class metadata should be present");
//             assertSame(classMeta, result.get(0));
//         }
//     }

//     @Test
//     void bothInterfaceAndClassPresent() {
//         // Interface metadata (with nested) + class metadata + recursive interface metadata
//         ConfigMappingInterface iface = mock(ConfigMappingInterface.class);
//         ConfigMappingInterface nested = mock(ConfigMappingInterface.class);
//         when(iface.getNested()).thenReturn(List.of(nested));
//         when(iface.getSuperTypes()).thenReturn(Collections.emptyList());
//         ConfigMappingClass classMeta = mock(ConfigMappingClass.class);
//         // The recursive call will return a second interface metadata (no nested)
//         ConfigMappingInterface recursiveIface = mock(ConfigMappingInterface.class);
//         when(recursiveIface.getNested()).thenReturn(Collections.emptyList());
//         when(recursiveIface.getSuperTypes()).thenReturn(Collections.emptyList());
//         try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingClass> classMock = mockStatic(ConfigMappingClass.class);
//             MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
//             // First level
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ComplexConfigInterface.class)).thenReturn(iface);
//             classMock.when(() -> ConfigMappingClass.getConfigurationClass(ComplexConfigInterface.class)).thenReturn(null);
//             // Class metadata for the concrete class
//             classMock.when(() -> ConfigMappingClass.getConfigurationClass(ComplexConfigClass.class)).thenReturn(classMeta);
//             // getConfigMapping for the concrete class returns the interface we already mocked
//             loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ComplexConfigClass.class)).thenReturn(iface);
//             // Recursive call – the interface type of the class is ComplexConfigInterface.class
//             // (already mocked above). To avoid infinite recursion we make the second call
//             // return null for both helpers.
//             ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ComplexConfigInterface.class)).thenReturn(null);
//             classMock.when(() -> ConfigMappingClass.getConfigurationClass(ComplexConfigInterface.class)).thenReturn(null);
//             List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(ComplexConfigClass.class);
//             assertEquals(3, result.size(), "Interface, its nested, and the class metadata should be present");
//             assertEquals(iface, result.get(0));
//             assertEquals(nested, result.get(1));
//             assertEquals(classMeta, result.get(2));
//         }
//     }

    @Test
    void abstractClassConfigMappingClassIsNull() {
        // Abstract class → ConfigMappingClass returns null, but we still have an interface
        ConfigMappingInterface iface = mock(ConfigMappingInterface.class);
        when(iface.getNested()).thenReturn(Collections.emptyList());
        when(iface.getSuperTypes()).thenReturn(Collections.emptyList());
        try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(AbstractConfigClass.class)).thenReturn(iface);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(AbstractConfigClass.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(AbstractConfigClass.class);
            assertEquals(1, result.size(), "Only the interface metadata should be present");
            assertSame(iface, result.get(0));
        }
    }

    @Test
    void nullArgumentThrowsException() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingsMetadata(null), "Passing null must result in NPE");
    }

    @Test
    void enumTypeYieldsEmptyResult() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(EnumConfigClass.class);
        assertTrue(result.isEmpty(), "Enum types must produce an empty metadata list");
    }

    @Test
    void genericConfigInterfaceHandledLikeNormalInterface() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(GenericConfigInterface.class);
        assertEquals(1, result.size(), "Generic interfaces should still produce a single metadata entry");
        assertEquals(GenericConfigInterface.class, result.get(0).getInterfaceType(), "The returned metadata should refer to the generic interface");
    }

    @Test
    void concreteClassImplementsConfigPropertiesAndExtendsAnotherConfigPropertiesClass() {
        // Build the hierarchy:
        // SubClassConfig (class) implements SubClassConfigInterface
        // SuperClassConfig (superclass) implements SuperClassConfigInterface
        // Both interfaces have a nested type.
        ConfigMappingInterface subIface = mock(ConfigMappingInterface.class);
        ConfigMappingInterface subNested = mock(ConfigMappingInterface.class);
        ConfigMappingInterface superIface = mock(ConfigMappingInterface.class);
        ConfigMappingInterface superNested = mock(ConfigMappingInterface.class);
        when(subIface.getNested()).thenReturn(List.of(subNested));
        when(subIface.getSuperTypes()).thenReturn(List.of(superIface));
        when(superIface.getNested()).thenReturn(List.of(superNested));
        when(superIface.getSuperTypes()).thenReturn(Collections.emptyList());
        ConfigMappingClass classMeta = mock(ConfigMappingClass.class);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
            // The concrete class itself has no direct interface mapping (null)
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(SubClassConfig.class)).thenReturn(null);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(SubClassConfig.class)).thenReturn(classMeta);
            // Recursive call: getConfigMapping(SubClassConfig) → subIface
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(SubClassConfig.class)).thenReturn(subIface);
            // For the recursive call on subIface.getInterfaceType() we need to stop recursion:
            // Assume subIface.getInterfaceType() returns SubClassConfigInterface.class
            // and that both helpers return null for that type (to keep the test focused).
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(SubClassConfigInterface.class)).thenReturn(null);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(SubClassConfigInterface.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(SubClassConfig.class);
            // Expected order:
            // 1) subIface
            // 2) subNested
            // 3) superIface
            // 4) superNested
            // 5) classMeta
            assertEquals(5, result.size(), "Five entries should be produced according to the spec");
            assertEquals(subIface, result.get(0));
            assertEquals(subNested, result.get(1));
            assertEquals(superIface, result.get(2));
            assertEquals(superNested, result.get(3));
            assertEquals(classMeta, result.get(4));
        }
    }

    @Test
    void concreteClassImplementsConfigPropertiesButInterfaceLookupReturnsNull() {
        // Class metadata exists, interface lookup returns null → only class metadata
        ConfigMappingClass classMeta = mock(ConfigMappingClass.class);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(OrphanConfigClass.class)).thenReturn(null);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(OrphanConfigClass.class)).thenReturn(classMeta);
            // Recursive call – make it return null to stop recursion
            ConfigMappingInterface dummy = mock(ConfigMappingInterface.class);
            when(dummy.getInterfaceType()).thenReturn(Object.class);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(OrphanConfigClass.class)).thenReturn(dummy);
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(Object.class)).thenReturn(null);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(Object.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(OrphanConfigClass.class);
            assertEquals(1, result.size(), "Only the class metadata should be present");
            assertSame(classMeta, result.get(0));
        }
    }
}