package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.getConfigMappingsMetadata;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
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

    interface ServerParentNested {
    }

    interface ServerChildNested {
    }

    interface ServerChildNested {
    }

    interface Nested {

        String nestedValue();
    }

    @ConfigProperties(prefix = "server")
    public static class ServerProperties {

        String host;

        int port;
    }

    interface ServerManual {

        String host();

        int port();
    }

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

    interface ServerParent {

        String parent();

        ServerParentNested parentNested();

        interface ServerParentNested {
        }
    }

    interface ServerParentNested {
    }

    interface ServerParentNested {
    }

    interface ServerParentNested {
    }

    interface ServerParentNested {
    }

    interface ServerChild extends ServerParent {

        ServerChildNested childNested();

        interface ServerChildNested {
        }
    }

    interface ServerChildNested {
    }

    interface ServerChildNested {
    }

    interface ServerChildNested {
    }

    interface ServerChildNested {
    }

    // 2. Only ConfigMappingInterface present (no super / nested)
    interface SimpleConfigInterface {
    }

    // 3. ConfigMappingInterface with nested mappings
    interface ParentConfigInterface {
    }

    // 4. ConfigMappingInterface with super types and nested mappings
    interface ChildConfigInterface {
    }

    // 5. Only ConfigMappingClass present (no ConfigMappingInterface)
    static class ConfigClassOnly {
    }

    // 6. Both ConfigMappingInterface and ConfigMappingClass present
    static class CombinedConfig {
    }

    // 7. Recursive termination (class -> interface -> null)
    static class SelfReferencingConfig {
    }

    // 8. Interface with null nested list
    interface InterfaceWithNullNested {
    }

    // 9. Interface with null super types list
    interface InterfaceWithNullSuperTypes {
    }

    // 10. Child with super whose nested list is null
    interface ChildWithSuperHavingNullNested {
    }

    // 11. Class with missing mapping (getConfigMapping returns null)
    static class ClassWithMissingMapping {
    }

    // 12. Interface with multiple direct super types and nested mappings
    interface MultiSuperInterface {
    }

    // 13. Class whose interface has super types & nested mappings
    static class ClassWithComplexInterface {
    }

    // 14. Grand child interface (direct super only)
    interface GrandChildInterface {
    }

    // 15. Class with null interface type from mapping
    static class ClassWithNullInterfaceType {
    }

    // 16. Cyclic config classes
    static class CyclicConfigClassA {
    }

    static class CyclicConfigClassB {
    }

    // 17. Interface with null element inside super types list
    interface InterfaceWithNullSuperTypeEntry {
    }

    // Dummy nested types for the previous test
    interface NestedA {
    }

    interface NestedB {
    }

    interface SuperInterface {
    }

    interface SuperWithNullNested {
    }

    interface SuperA {
    }

    interface SuperB {
    }

    interface GrandParentInterface {
    }

    interface ParentInterface {
    }

    interface ValidSuper {
    }

    interface Nested {

        String nestedValue();
    }

    interface Nested {

        String nestedValue();
    }

    interface Nested {

        String nestedValue();
    }

    @ConfigMapping(prefix = "server")
    interface Server {

        String host();

        int port();
    }

    interface PlainClass {
    }

    interface SimpleInterface {

        String value();
    }

    interface ParentWithNested {

        interface Nested {

            String nestedValue();
        }

        String parentValue();
    }

    interface Nested {

        String nestedValue();
    }

    interface Nested {

        String nestedValue();
    }

    /**
     * Verify that the metadata list contains the interface itself, its nested types,
     * and the metadata of its super‑interfaces (including their nested types).
     */
    @Test
    void interfaceWithNestedAndSuperTypes() {
        List<ConfigMappingMetadata> metadata = getConfigMappingsMetadata(ServerChild.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertTrue(types.contains(ServerChild.class), "Should contain ServerChild");
        assertTrue(types.contains(ServerChild.ServerChildNested.class), "Should contain ServerChildNested");
        assertTrue(types.contains(ServerParent.class), "Should contain ServerParent");
        assertTrue(types.contains(ServerParent.ServerParentNested.class), "Should contain ServerParentNested");
    }

    /**
     * Verify that for a plain interface without any configuration annotations
     * the method still returns a list containing the interface metadata.
     */
    @Test
    void plainInterfaceOnly() {
        List<ConfigMappingMetadata> metadata = getConfigMappingsMetadata(ServerManual.class);
        assertEquals(1, metadata.size(), "Plain interface should yield a single metadata entry");
        assertEquals(ServerManual.class, metadata.get(0).getInterfaceType(), "The returned metadata should correspond to ServerManual");
    }

    /**
     * The method must be idempotent – calling it several times with the same argument
     * must return equal results (same size and same set of interface types).
     */
    @Test
    void idempotentCalls() {
        List<ConfigMappingMetadata> firstCall = getConfigMappingsMetadata(ServerNested.class);
        List<ConfigMappingMetadata> secondCall = getConfigMappingsMetadata(ServerNested.class);
        assertEquals(firstCall.size(), secondCall.size(), "Both calls should return the same number of metadata entries");
        List<Class<?>> firstTypes = firstCall.stream().map(ConfigMappingMetadata::getInterfaceType).sorted().collect(Collectors.toList());
        List<Class<?>> secondTypes = secondCall.stream().map(ConfigMappingMetadata::getInterfaceType).sorted().collect(Collectors.toList());
        assertEquals(firstTypes, secondTypes, "Both calls should return the same set of interface types");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void noInterfaceNoClassReturnsEmptyList() {
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(PlainClass.class)).thenReturn(null);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(PlainClass.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(PlainClass.class);
            assertTrue(result.isEmpty(), "When neither interface nor class metadata is present the list must be empty");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void onlyInterfaceNoSuperNoNested() {
        ConfigMappingMetadata ifaceMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(ifaceMeta.getInterfaceType()).thenReturn(SimpleConfigInterface.class);
        Mockito.when(ifaceMeta.getNested()).thenReturn(Collections.emptyList());
        Mockito.when(ifaceMeta.getSuperTypes()).thenReturn(Collections.emptyList());
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(SimpleConfigInterface.class)).thenReturn((ConfigMappingInterface) ifaceMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(SimpleConfigInterface.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(SimpleConfigInterface.class);
            assertEquals(1, result.size(), "Only the interface metadata should be present");
            assertSame(ifaceMeta, result.get(0), "Returned metadata must be the mocked interface metadata");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void interfaceWithNestedMappings() {
        ConfigMappingMetadata nested1 = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata nested2 = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(nested1.getInterfaceType()).thenReturn(NestedA.class);
        Mockito.when(nested2.getInterfaceType()).thenReturn(NestedB.class);
        ConfigMappingMetadata ifaceMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(ifaceMeta.getInterfaceType()).thenReturn(ParentConfigInterface.class);
        Mockito.when(ifaceMeta.getNested()).thenReturn(List.of(nested1, nested2));
        Mockito.when(ifaceMeta.getSuperTypes()).thenReturn(Collections.emptyList());
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ParentConfigInterface.class)).thenReturn((ConfigMappingInterface) ifaceMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(ParentConfigInterface.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(ParentConfigInterface.class);
            assertEquals(3, result.size(), "Interface + its two nested entries");
            assertEquals(ParentConfigInterface.class, result.get(0).getInterfaceType());
            assertTrue(result.subList(1, 3).stream().map(ConfigMappingMetadata::getInterfaceType).toList().containsAll(List.of(NestedA.class, NestedB.class)));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void interfaceWithSuperTypesAndNested() {
        // Super type metadata
        ConfigMappingMetadata superMeta = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata superNested = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(superMeta.getInterfaceType()).thenReturn(SuperInterface.class);
        Mockito.when(superMeta.getNested()).thenReturn(List.of(superNested));
        Mockito.when(superMeta.getSuperTypes()).thenReturn(Collections.emptyList());
        // Child interface metadata
        ConfigMappingMetadata childMeta = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata childNested = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(childMeta.getInterfaceType()).thenReturn(ChildConfigInterface.class);
        Mockito.when(childMeta.getNested()).thenReturn(List.of(childNested));
        Mockito.when(childMeta.getSuperTypes()).thenReturn(List.of(superMeta));
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ChildConfigInterface.class)).thenReturn((ConfigMappingInterface) childMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(ChildConfigInterface.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(ChildConfigInterface.class);
            // Expected order: child, childNested, super, superNested
            assertEquals(4, result.size(), "Child + its nested + super + super nested");
            assertEquals(ChildConfigInterface.class, result.get(0).getInterfaceType());
            assertEquals(childNested, result.get(1));
            assertEquals(SuperInterface.class, result.get(2).getInterfaceType());
            assertEquals(superNested, result.get(3));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void onlyClassPresent() {
        ConfigMappingMetadata classMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(classMeta.getInterfaceType()).thenReturn(ConfigClassOnly.class);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ConfigClassOnly.class)).thenReturn(null);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(ConfigClassOnly.class)).thenReturn((ConfigMappingClass) classMeta);
            // The recursive call should receive a null interface type → empty list
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ConfigClassOnly.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(ConfigClassOnly.class);
            assertEquals(1, result.size(), "Only the class metadata should be present");
            assertSame(classMeta, result.get(0));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void bothInterfaceAndClassPresent() {
        // Interface side
        ConfigMappingMetadata ifaceMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(ifaceMeta.getInterfaceType()).thenReturn(CombinedConfig.class);
        Mockito.when(ifaceMeta.getNested()).thenReturn(Collections.emptyList());
        Mockito.when(ifaceMeta.getSuperTypes()).thenReturn(Collections.emptyList());
        // Class side
        ConfigMappingMetadata classMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(classMeta.getInterfaceType()).thenReturn(CombinedConfig.class);
        // Mapping from class → interface (used for recursion)
        ConfigMappingInterface mappingFromClass = Mockito.mock(ConfigMappingInterface.class);
        Mockito.when(mappingFromClass.getInterfaceType()).thenReturn(CombinedConfig.class);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(CombinedConfig.class)).thenReturn((ConfigMappingInterface) ifaceMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(CombinedConfig.class)).thenReturn((ConfigMappingClass) classMeta);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(CombinedConfig.class)).thenReturn(mappingFromClass);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(CombinedConfig.class);
            // Expected order: interface, class, interface (from recursion)
            assertEquals(3, result.size());
            assertEquals(ifaceMeta, result.get(0));
            assertEquals(classMeta, result.get(1));
            assertEquals(ifaceMeta, result.get(2));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void recursionTerminatesWhenInterfaceMappingIsNull() {
        ConfigMappingMetadata classMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(classMeta.getInterfaceType()).thenReturn(SelfReferencingConfig.class);
        ConfigMappingInterface classToInterface = Mockito.mock(ConfigMappingInterface.class);
        // recursion ends
        Mockito.when(classToInterface.getInterfaceType()).thenReturn(null);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(SelfReferencingConfig.class)).thenReturn(null);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(SelfReferencingConfig.class)).thenReturn((ConfigMappingClass) classMeta);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(SelfReferencingConfig.class)).thenReturn(classToInterface);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(SelfReferencingConfig.class);
            assertEquals(1, result.size(), "Only the class metadata should be present; recursion stops");
            assertSame(classMeta, result.get(0));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> getConfigMappingsMetadata(null), "Calling getConfigMappingsMetadata with null must throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void interfaceNullNestedThrowsNPE() {
        ConfigMappingMetadata ifaceMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(ifaceMeta.getInterfaceType()).thenReturn(InterfaceWithNullNested.class);
        // <-- problem
        Mockito.when(ifaceMeta.getNested()).thenReturn(null);
        Mockito.when(ifaceMeta.getSuperTypes()).thenReturn(Collections.emptyList());
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(InterfaceWithNullNested.class)).thenReturn((ConfigMappingInterface) ifaceMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(InterfaceWithNullNested.class)).thenReturn(null);
            assertThrows(NullPointerException.class, () -> getConfigMappingsMetadata(InterfaceWithNullNested.class), "addAll(null) on nested list must throw NPE");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void interfaceNullSuperTypesThrowsNPE() {
        ConfigMappingMetadata ifaceMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(ifaceMeta.getInterfaceType()).thenReturn(InterfaceWithNullSuperTypes.class);
        Mockito.when(ifaceMeta.getNested()).thenReturn(Collections.emptyList());
        // <-- problem
        Mockito.when(ifaceMeta.getSuperTypes()).thenReturn(null);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(InterfaceWithNullSuperTypes.class)).thenReturn((ConfigMappingInterface) ifaceMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(InterfaceWithNullSuperTypes.class)).thenReturn(null);
            assertThrows(NullPointerException.class, () -> getConfigMappingsMetadata(InterfaceWithNullSuperTypes.class), "Iterating over null super types must throw NPE");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void superTypeNullNestedThrowsNPE() {
        // Super type metadata with null nested
        ConfigMappingMetadata superMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(superMeta.getInterfaceType()).thenReturn(SuperWithNullNested.class);
        // <-- problem
        Mockito.when(superMeta.getNested()).thenReturn(null);
        Mockito.when(superMeta.getSuperTypes()).thenReturn(Collections.emptyList());
        // Child interface metadata
        ConfigMappingMetadata childMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(childMeta.getInterfaceType()).thenReturn(ChildWithSuperHavingNullNested.class);
        Mockito.when(childMeta.getNested()).thenReturn(Collections.emptyList());
        Mockito.when(childMeta.getSuperTypes()).thenReturn(List.of(superMeta));
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ChildWithSuperHavingNullNested.class)).thenReturn((ConfigMappingInterface) childMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(ChildWithSuperHavingNullNested.class)).thenReturn(null);
            assertThrows(NullPointerException.class, () -> getConfigMappingsMetadata(ChildWithSuperHavingNullNested.class), "addAll(null) on super type nested list must throw NPE");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void classPresentButMappingNullThrowsNPE() {
        ConfigMappingMetadata classMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(classMeta.getInterfaceType()).thenReturn(ClassWithMissingMapping.class);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ClassWithMissingMapping.class)).thenReturn(null);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(ClassWithMissingMapping.class)).thenReturn((ConfigMappingClass) classMeta);
            // <-- problem
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ClassWithMissingMapping.class)).thenReturn(null);
            assertThrows(NullPointerException.class, () -> getConfigMappingsMetadata(ClassWithMissingMapping.class), "Calling getInterfaceType() on null mapping must throw NPE");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void interfaceMultipleSuperTypesAndNested() {
        // Super A
        ConfigMappingMetadata superA = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata superANested = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(superA.getInterfaceType()).thenReturn(SuperA.class);
        Mockito.when(superA.getNested()).thenReturn(List.of(superANested));
        Mockito.when(superA.getSuperTypes()).thenReturn(Collections.emptyList());
        // Super B
        ConfigMappingMetadata superB = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata superBNested = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(superB.getInterfaceType()).thenReturn(SuperB.class);
        Mockito.when(superB.getNested()).thenReturn(List.of(superBNested));
        Mockito.when(superB.getSuperTypes()).thenReturn(Collections.emptyList());
        // Main interface
        ConfigMappingMetadata mainMeta = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata mainNested = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(mainMeta.getInterfaceType()).thenReturn(MultiSuperInterface.class);
        Mockito.when(mainMeta.getNested()).thenReturn(List.of(mainNested));
        Mockito.when(mainMeta.getSuperTypes()).thenReturn(List.of(superA, superB));
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(MultiSuperInterface.class)).thenReturn((ConfigMappingInterface) mainMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(MultiSuperInterface.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(MultiSuperInterface.class);
            // Expected order: main, mainNested, superA, superANested, superB, superBNested
            assertEquals(6, result.size());
            assertEquals(MultiSuperInterface.class, result.get(0).getInterfaceType());
            assertEquals(mainNested, result.get(1));
            assertEquals(SuperA.class, result.get(2).getInterfaceType());
            assertEquals(superANested, result.get(3));
            assertEquals(SuperB.class, result.get(4).getInterfaceType());
            assertEquals(superBNested, result.get(5));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void classWithComplexInterfaceHierarchy() {
        // Interface hierarchy (same as previous test but reused)
        ConfigMappingMetadata superA = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata superANested = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(superA.getInterfaceType()).thenReturn(SuperA.class);
        Mockito.when(superA.getNested()).thenReturn(List.of(superANested));
        Mockito.when(superA.getSuperTypes()).thenReturn(Collections.emptyList());
        ConfigMappingMetadata superB = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata superBNested = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(superB.getInterfaceType()).thenReturn(SuperB.class);
        Mockito.when(superB.getNested()).thenReturn(List.of(superBNested));
        Mockito.when(superB.getSuperTypes()).thenReturn(Collections.emptyList());
        ConfigMappingMetadata ifaceMeta = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata ifaceNested = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(ifaceMeta.getInterfaceType()).thenReturn(ClassWithComplexInterface.class);
        Mockito.when(ifaceMeta.getNested()).thenReturn(List.of(ifaceNested));
        Mockito.when(ifaceMeta.getSuperTypes()).thenReturn(List.of(superA, superB));
        // Class metadata
        ConfigMappingMetadata classMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(classMeta.getInterfaceType()).thenReturn(ClassWithComplexInterface.class);
        // Mapping from class → interface
        ConfigMappingInterface classToIface = Mockito.mock(ConfigMappingInterface.class);
        Mockito.when(classToIface.getInterfaceType()).thenReturn(ClassWithComplexInterface.class);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            // Interface side
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ClassWithComplexInterface.class)).thenReturn((ConfigMappingInterface) ifaceMeta);
            // Class side
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(ClassWithComplexInterface.class)).thenReturn((ConfigMappingClass) classMeta);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ClassWithComplexInterface.class)).thenReturn(classToIface);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(ClassWithComplexInterface.class);
            // Expected order:
            // 1 classMeta
            // 2 ifaceMeta
            // 3 ifaceNested
            // 4 superA
            // 5 superANested
            // 6 superB
            // 7 superBNested
            assertEquals(7, result.size());
            assertSame(classMeta, result.get(0));
            assertSame(ifaceMeta, result.get(1));
            assertSame(ifaceNested, result.get(2));
            assertEquals(SuperA.class, result.get(3).getInterfaceType());
            assertSame(superANested, result.get(4));
            assertEquals(SuperB.class, result.get(5).getInterfaceType());
            assertSame(superBNested, result.get(6));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void grandChildDirectSuperOnly() {
        // GrandParent (won't be visited)
        ConfigMappingMetadata grandParent = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(grandParent.getInterfaceType()).thenReturn(GrandParentInterface.class);
        Mockito.when(grandParent.getNested()).thenReturn(Collections.emptyList());
        Mockito.when(grandParent.getSuperTypes()).thenReturn(Collections.emptyList());
        // Parent (direct super of GrandChild)
        ConfigMappingMetadata parent = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata parentNested = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(parent.getInterfaceType()).thenReturn(ParentInterface.class);
        Mockito.when(parent.getNested()).thenReturn(List.of(parentNested));
        // The parent reports a super type, but the loader only iterates over direct super types,
        // so this list should be ignored by the method under test.
        Mockito.when(parent.getSuperTypes()).thenReturn(List.of(grandParent));
        // GrandChild
        ConfigMappingMetadata child = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(child.getInterfaceType()).thenReturn(GrandChildInterface.class);
        Mockito.when(child.getNested()).thenReturn(Collections.emptyList());
        Mockito.when(child.getSuperTypes()).thenReturn(List.of(parent));
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(GrandChildInterface.class)).thenReturn((ConfigMappingInterface) child);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(GrandChildInterface.class)).thenReturn(null);
            List<ConfigMappingMetadata> result = getConfigMappingsMetadata(GrandChildInterface.class);
            // Expected: child, parent, parentNested (grandParent never appears)
            assertEquals(3, result.size());
            assertEquals(GrandChildInterface.class, result.get(0).getInterfaceType());
            assertEquals(ParentInterface.class, result.get(1).getInterfaceType());
            assertSame(parentNested, result.get(2));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void classWithNullInterfaceTypeThrowsNPE() {
        ConfigMappingMetadata classMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(classMeta.getInterfaceType()).thenReturn(ClassWithNullInterfaceType.class);
        ConfigMappingInterface mapping = Mockito.mock(ConfigMappingInterface.class);
        // <-- null interface type
        Mockito.when(mapping.getInterfaceType()).thenReturn(null);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(ClassWithNullInterfaceType.class)).thenReturn(null);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(ClassWithNullInterfaceType.class)).thenReturn((ConfigMappingClass) classMeta);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(ClassWithNullInterfaceType.class)).thenReturn(mapping);
            assertThrows(NullPointerException.class, () -> getConfigMappingsMetadata(ClassWithNullInterfaceType.class), "Recursive call with null interface type must throw NPE");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cyclicClassMappingLeadsToStackOverflow() {
        // Class A maps to interface B, Class B maps back to interface A
        ConfigMappingMetadata classAMeta = Mockito.mock(ConfigMappingMetadata.class);
        ConfigMappingMetadata classBMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(classAMeta.getInterfaceType()).thenReturn(CyclicConfigClassA.class);
        Mockito.when(classBMeta.getInterfaceType()).thenReturn(CyclicConfigClassB.class);
        ConfigMappingInterface mappingAtoB = Mockito.mock(ConfigMappingInterface.class);
        ConfigMappingInterface mappingBtoA = Mockito.mock(ConfigMappingInterface.class);
        Mockito.when(mappingAtoB.getInterfaceType()).thenReturn(CyclicConfigClassB.class);
        Mockito.when(mappingBtoA.getInterfaceType()).thenReturn(CyclicConfigClassA.class);
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class)) {
            // Both classes are recognized as ConfigMappingClass
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(CyclicConfigClassA.class)).thenReturn((ConfigMappingClass) classAMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(CyclicConfigClassB.class)).thenReturn((ConfigMappingClass) classBMeta);
            // No interface metadata for either class (return null)
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(Mockito.any())).thenReturn(null);
            // Mapping from class to interface creates the cycle
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(CyclicConfigClassA.class)).thenReturn(mappingAtoB);
            loaderMock.when(() -> ConfigMappingLoader.getConfigMapping(CyclicConfigClassB.class)).thenReturn(mappingBtoA);
            // The recursion should eventually overflow the stack.
            assertThrows(StackOverflowError.class, () -> getConfigMappingsMetadata(CyclicConfigClassA.class), "Infinite recursion between two classes must cause StackOverflowError");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void superTypesListContainsNullElementThrowsNPE() {
        ConfigMappingMetadata validSuper = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(validSuper.getInterfaceType()).thenReturn(ValidSuper.class);
        Mockito.when(validSuper.getNested()).thenReturn(Collections.emptyList());
        Mockito.when(validSuper.getSuperTypes()).thenReturn(Collections.emptyList());
        ConfigMappingMetadata childMeta = Mockito.mock(ConfigMappingMetadata.class);
        Mockito.when(childMeta.getInterfaceType()).thenReturn(InterfaceWithNullSuperTypeEntry.class);
        Mockito.when(childMeta.getNested()).thenReturn(Collections.emptyList());
        // List contains a valid super type followed by null
        Mockito.when(childMeta.getSuperTypes()).thenReturn(List.of(validSuper, null));
        try (MockedStatic<ConfigMappingInterface> ifaceMock = Mockito.mockStatic(ConfigMappingInterface.class);
            MockedStatic<ConfigMappingClass> classMock = Mockito.mockStatic(ConfigMappingClass.class)) {
            ifaceMock.when(() -> ConfigMappingInterface.getConfigurationInterface(InterfaceWithNullSuperTypeEntry.class)).thenReturn((ConfigMappingInterface) childMeta);
            classMock.when(() -> ConfigMappingClass.getConfigurationClass(InterfaceWithNullSuperTypeEntry.class)).thenReturn(null);
            assertThrows(NullPointerException.class, () -> getConfigMappingsMetadata(InterfaceWithNullSuperTypeEntry.class), "Iterating over a super types list containing null must throw NPE");
        }
    }

    /**
     * Verify that a simple {@code @ConfigMapping} interface returns a non‑empty
     * metadata list containing the interface itself.
     */
    @Test
    void simpleConfigMappingReturnsMetadata() {
        List<ConfigMappingMetadata> metadata = getConfigMappingsMetadata(Server.class);
        assertFalse(metadata.isEmpty(), "Metadata list should not be empty for a @ConfigMapping interface");
        assertEquals(Server.class, metadata.get(0).getInterfaceType(), "First metadata entry should correspond to the Server interface");
    }

    @Test
    void plainClassReturnsEmptyList() {
        List<ConfigMappingMetadata> result = getConfigMappingsMetadata(PlainClass.class);
        assertTrue(result.isEmpty(), "Plain class without mapping should return an empty list");
    }

    @Test
    void simpleInterfaceReturnsSingleMetadata() {
        List<ConfigMappingMetadata> metadata = getConfigMappingsMetadata(SimpleInterface.class);
        assertEquals(1, metadata.size(), "Non‑annotated interface should yield exactly one metadata entry");
        assertEquals(SimpleInterface.class, metadata.get(0).getInterfaceType(), "Metadata should correspond to SimpleInterface");
    }

    @Test
    void interfaceWithNestedTypes() {
        List<ConfigMappingMetadata> metadata = getConfigMappingsMetadata(ParentWithNested.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        assertTrue(types.contains(ParentWithNested.class), "Metadata must contain the parent interface");
        assertTrue(types.contains(ParentWithNested.Nested.class), "Metadata must contain the nested interface");
    }
}
