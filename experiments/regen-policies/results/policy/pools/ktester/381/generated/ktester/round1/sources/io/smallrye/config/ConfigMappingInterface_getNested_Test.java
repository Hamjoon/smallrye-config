package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getNested_Test {

    @ConfigMapping(prefix = "simple")
    interface SimpleConfig {

        @WithDefault("localhost")
        String host();
    }

    @ConfigMapping(prefix = "empty")
    interface EmptyConfig {
        // No methods – no properties.
    }

    @ConfigMapping(prefix = "single")
    interface SingleNestedRoot {

        NestedA nested();
    }

    @ConfigMapping(prefix = "duplicate")
    interface DuplicateNestedRoot {

        NestedA a1();

        // same nested type, should be deduped.
        NestedA a2();
    }

    @ConfigMapping(prefix = "multi")
    interface MultiNestedRoot {

        NestedA a();

        NestedB b();

        NestedC c();
    }

    @ConfigMapping(prefix = "recursive")
    interface RecursiveRoot {

        NestedB b();
    }

    @ConfigMapping(prefix = "opt")
    interface OptionalNestedRoot {

        Optional<NestedA> maybeA();
    }

    @ConfigMapping(prefix = "list")
    interface ListNestedRoot {

        List<NestedA> listA();
    }

    @ConfigMapping(prefix = "set")
    interface SetNestedRoot {

        Set<NestedA> setA();
    }

    @ConfigMapping(prefix = "array")
    interface ArrayNestedRoot {

        NestedA[] arrayA();
    }

    @ConfigMapping(prefix = "wild")
    interface WildcardNestedRoot {

        List<? extends NestedA> listExtendsA();
    }

    @ConfigMapping(prefix = "hierarchy")
    interface HierarchyRoot {

        NestedA a();

        NestedB b();
    }

    @ConfigMapping(prefix = "concrete")
    interface ConcreteClassRoot {

        // concrete class – should trigger IllegalArgumentException
        String name();
    }

    @ConfigMapping(prefix = "unannotated")
    interface UnannotatedInterfaceRoot {

        UnannotatedConfig config();
    }

    @ConfigMapping(prefix = "generic")
    interface GenericVariableRoot<T> {

        // generic type variable cannot be resolved to a config interface
        List<T> list();
    }

    @ConfigMapping(prefix = "primitiveArray")
    interface PrimitiveArrayRoot {

        // primitive array – should trigger IllegalArgumentException
        int[] numbers();
    }

    @ConfigMapping(prefix = "optNonConfig")
    interface OptionalNonConfigRoot {

        // Optional of non‑config type
        Optional<String> maybeString();
    }

    @ConfigMapping(prefix = "wildNoBound")
    interface WildcardNoBoundRoot {

        // wildcard without upper bound
        List<?> rawList();
    }

    @ConfigMapping(prefix = "nestedA")
    interface NestedA {

        String value();
    }

    @ConfigMapping(prefix = "nestedB")
    interface NestedB {

        // further nesting
        NestedC c();
    }

    @ConfigMapping(prefix = "nestedC")
    interface NestedC {

        String leaf();
    }

    @ConfigMapping(prefix = "unannotated")
    interface UnannotatedConfig {
        // No @ConfigMapping annotation on purpose.
    }

    @ConfigMapping(prefix = "simple")
//     interface SimpleConfig {

        @WithDefault("localhost")
        String host();
    }

    @ConfigMapping(prefix = "empty")
//     interface EmptyConfig {
        // No methods – no properties.
    }

    @ConfigMapping(prefix = "single")
//     interface SingleNestedRoot {

        NestedA nested();
    }

    @ConfigMapping(prefix = "duplicate")
//     interface DuplicateNestedRoot {

        NestedA a1();

        // same nested type, should be deduped.
        NestedA a2();
    }

    @ConfigMapping(prefix = "multi")
//     interface MultiNestedRoot {

        NestedA a();

        NestedB b();

        NestedC c();
    }

    @ConfigMapping(prefix = "recursive")
//     interface RecursiveRoot {

        NestedB b();
    }

    @ConfigMapping(prefix = "opt")
//     interface OptionalNestedRoot {

        Optional<NestedA> maybeA();
    }

    @ConfigMapping(prefix = "list")
//     interface ListNestedRoot {

        List<NestedA> listA();
    }

    @ConfigMapping(prefix = "set")
//     interface SetNestedRoot {

        Set<NestedA> setA();
    }

    @ConfigMapping(prefix = "array")
//     interface ArrayNestedRoot {

        NestedA[] arrayA();
    }

    @ConfigMapping(prefix = "wild")
//     interface WildcardNestedRoot {

        List<? extends NestedA> listExtendsA();
    }

    @ConfigMapping(prefix = "hierarchy")
//     interface HierarchyRoot {

        NestedA a();

        NestedB b();
    }

    @ConfigMapping(prefix = "concrete")
//     interface ConcreteClassRoot {

        // concrete class – should trigger IllegalArgumentException
        String name();
    }

    @ConfigMapping(prefix = "unannotated")
//     interface UnannotatedInterfaceRoot {

        UnannotatedConfig config();
    }

    @ConfigMapping(prefix = "generic")
//     interface GenericVariableRoot<T> {

        // generic type variable cannot be resolved to a config interface
        List<T> list();
    }

    @ConfigMapping(prefix = "primitiveArray")
//     interface PrimitiveArrayRoot {

        // primitive array – should trigger IllegalArgumentException
        int[] numbers();
    }

    @ConfigMapping(prefix = "optNonConfig")
//     interface OptionalNonConfigRoot {

        // Optional of non‑config type
        Optional<String> maybeString();
    }

    @ConfigMapping(prefix = "wildNoBound")
//     interface WildcardNoBoundRoot {

        // wildcard without upper bound
        List<?> rawList();
    }

    @ConfigMapping(prefix = "nestedA")
//     interface NestedA {

        String value();
    }

    @ConfigMapping(prefix = "nestedB")
//     interface NestedB {

        // further nesting
        NestedC c();
    }

    @ConfigMapping(prefix = "nestedC")
//     interface NestedC {

        String leaf();
    }

    @ConfigMapping(prefix = "unannotated")
//     interface UnannotatedConfig {
        // No @ConfigMapping annotation on purpose.
    }

    @ConfigMapping(prefix = "server")
    interface Server {

        Map<String, Group> group();
    }

    @ConfigMapping(prefix = "group")
    interface Group {

        String name();
    }

    /**
     * Verifies that {@code getNested()} returns the metadata for a nested configuration interface.
     *
     * The {@code Server} interface (declared in {@link ConfigMappingInterfaceTest}) contains a method
     * {@code Map<String, Group> group()} where {@code Group} is a nested configuration interface.
     * The test asserts that the nested list contains exactly one element and that this element
     * corresponds to {@code Group.class}.
     */
    @Test
    void getNested_shouldReturnNestedConfigMappingInterfaces() {
        ConfigMappingInterface serverMapping = ConfigMappingInterface.getConfigurationInterface(Server.class);
        List<ConfigMappingInterface> nested = serverMapping.getNested();
        assertEquals(1, nested.size(), "Expected exactly one nested ConfigMappingInterface for Server");
        ConfigMappingInterface groupMapping = nested.get(0);
        assertEquals(Group.class, groupMapping.getInterfaceType(), "The nested ConfigMappingInterface should correspond to Group.class");
    }

    /**
     * Verifies that {@code getNested()} returns an empty list when the configuration interface
     * does not declare any nested configuration interfaces.
     *
     * The {@code SimpleConfig} interface defined below contains only a primitive property,
     * therefore no nested metadata should be produced.
     */
    @Test
    void getNested_shouldReturnEmptyListWhenNoNestedInterfaces() {
        ConfigMappingInterface simpleMapping = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        List<ConfigMappingInterface> nested = simpleMapping.getNested();
        assertTrue(nested.isEmpty(), "Expected no nested ConfigMappingInterface for a simple configuration interface");
    }

    @Test
    void getNested_shouldReturnEmptyListWhenNoProperties() {
        ConfigMappingInterface empty = ConfigMappingInterface.getConfigurationInterface(EmptyConfig.class);
        List<ConfigMappingInterface> nested = empty.getNested();
        assertTrue(nested.isEmpty(), "Expected empty list for configuration without properties");
    }

    @Test
    void getNested_shouldReturnSingleNestedInterface() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(SingleNestedRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        assertEquals(1, nested.size(), "Exactly one nested interface expected");
        assertEquals(NestedA.class, nested.get(0).getInterfaceType(), "Nested interface should be NestedA");
    }

    @Test
    void getNested_shouldDeduplicateNestedInterfaces() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(DuplicateNestedRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        // Two properties refer to the same nested interface; list must contain it once.
        assertEquals(1, nested.size(), "Duplicates must be removed");
        assertEquals(NestedA.class, nested.get(0).getInterfaceType(), "The only nested interface should be NestedA");
    }

    @Test
    void getNested_shouldPreserveDiscoveryOrder() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(MultiNestedRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        assertEquals(3, nested.size(), "Three distinct nested interfaces expected");
        assertEquals(NestedA.class, nested.get(0).getInterfaceType(), "First discovered should be NestedA");
        assertEquals(NestedB.class, nested.get(1).getInterfaceType(), "Second discovered should be NestedB");
        assertEquals(NestedC.class, nested.get(2).getInterfaceType(), "Third discovered should be NestedC");
    }

    @Test
    void getNested_shouldDiscoverRecursiveNestedInterfaces() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(RecursiveRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        // Expected order: B (direct child) then C (child of B)
        assertEquals(2, nested.size(), "Two nested interfaces expected");
        assertEquals(NestedB.class, nested.get(0).getInterfaceType(), "First nested should be B");
        assertEquals(NestedC.class, nested.get(1).getInterfaceType(), "Second nested should be C");
    }

    @Test
    void getNested_shouldUnwrapOptionalAndDetectNested() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(OptionalNestedRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        assertEquals(1, nested.size(), "Optional should be unwrapped");
        assertEquals(NestedA.class, nested.get(0).getInterfaceType(), "Nested interface should be NestedA");
    }

    @Test
    void getNested_shouldDetectNestedInList() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(ListNestedRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        assertEquals(1, nested.size(), "List generic should be resolved");
        assertEquals(NestedA.class, nested.get(0).getInterfaceType(), "Nested interface should be NestedA");
    }

    @Test
    void getNested_shouldDetectNestedInSet() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(SetNestedRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        assertEquals(1, nested.size(), "Set generic should be resolved");
        assertEquals(NestedA.class, nested.get(0).getInterfaceType(), "Nested interface should be NestedA");
    }

    @Test
    void getNested_shouldDetectNestedInArray() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(ArrayNestedRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        assertEquals(1, nested.size(), "Array component type should be resolved");
        assertEquals(NestedA.class, nested.get(0).getInterfaceType(), "Nested interface should be NestedA");
    }

    @Test
    void getNested_shouldResolveWildcardUpperBound() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(WildcardNestedRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        assertEquals(1, nested.size(), "Wildcard with upper bound should be resolved");
        assertEquals(NestedA.class, nested.get(0).getInterfaceType(), "Nested interface should be NestedA");
    }

    @Test
    void getNested_shouldDeduplicateAcrossHierarchy() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(HierarchyRoot.class);
        List<ConfigMappingInterface> nested = root.getNested();
        assertEquals(2, nested.size(), "Two distinct nested interfaces expected");
        assertEquals(NestedA.class, nested.get(0).getInterfaceType(), "First nested should be A");
        assertEquals(NestedB.class, nested.get(1).getInterfaceType(), "Second nested should be B (appears twice but deduped)");
    }

    @Test
    void getNested_shouldThrowWhenPropertyIsConcreteClass() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(ConcreteClassRoot.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, root::getNested, "Expected IllegalArgumentException for concrete class property");
        assertTrue(ex.getMessage().contains("java.lang.String"), "Exception message should mention the offending type");
    }

    @Test
    void getNested_shouldThrowWhenPropertyIsUnannotatedInterface() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(UnannotatedInterfaceRoot.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, root::getNested, "Expected IllegalArgumentException for un‑annotated interface property");
        assertTrue(ex.getMessage().contains("UnannotatedConfig"), "Exception message should mention the un‑annotated interface");
    }

    @Test
    void getNested_shouldThrowWhenGenericTypeVariableCannotBeResolved() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(GenericVariableRoot.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, root::getNested, "Expected IllegalArgumentException for unresolved generic type variable");
        assertTrue(ex.getMessage().contains("T"), "Exception message should mention the type variable");
    }

    @Test
    void getNested_shouldThrowWhenArrayComponentIsPrimitive() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(PrimitiveArrayRoot.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, root::getNested, "Expected IllegalArgumentException for primitive array component");
        assertTrue(ex.getMessage().contains("int"), "Exception message should mention the primitive type");
    }

    @Test
    void getNested_shouldThrowWhenOptionalElementIsNotConfigInterface() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(OptionalNonConfigRoot.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, root::getNested, "Expected IllegalArgumentException for Optional of non‑config type");
        assertTrue(ex.getMessage().contains("java.lang.String"), "Exception message should mention the non‑config type");
    }

    @Test
    void getNested_shouldThrowWhenWildcardHasNoUpperBound() {
        ConfigMappingInterface root = ConfigMappingInterface.getConfigurationInterface(WildcardNoBoundRoot.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, root::getNested, "Expected IllegalArgumentException for raw wildcard");
        assertTrue(ex.getMessage().contains("wildcard"), "Exception message should mention wildcard handling");
    }

    @Test
    void getNested_privateHelperShouldThrowWhenPropertyEntryIsNull() throws Exception {
        // Create a dummy ConfigMappingInterface instance (any valid interface works)
        ConfigMappingInterface dummy = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        // Use reflection to obtain the private static helper method
        Method helper = ConfigMappingInterface.class.getDeclaredMethod("getNested", ConfigMappingInterface.Property[].class, Set.class);
        helper.setAccessible(true);
        // Prepare an array with a single null entry
        ConfigMappingInterface.Property[] props = new ConfigMappingInterface.Property[1];
        props[0] = null;
        Set<ConfigMappingInterface> set = new LinkedHashSet<>();
        // Invoke and expect NPE
        NullPointerException npe = assertThrows(NullPointerException.class, () -> helper.invoke(null, props, set), "Null property entry should cause NullPointerException");
        assertTrue(npe.getMessage() == null || npe.getMessage().isEmpty(), "NPE message is not relevant");
    }
}