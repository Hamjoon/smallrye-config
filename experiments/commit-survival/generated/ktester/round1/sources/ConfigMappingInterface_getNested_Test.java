package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getNested_Test {

    interface LevelTwo {

        String value();
    }

    interface SubConfig {

        String id();
    }

    interface SubConfig {

        String id();
    }

    interface SubConfig {

        String value();
    }

    interface SubConfig {

        String value();
    }

    interface SubConfig {

        String data();
    }

    interface SubConfig {

        String data();
    }

    interface SubConfig {

        String opt();
    }

    interface SubConfig {

        String opt();
    }

    interface SubConfig {

        String mapVal();
    }

    interface SubConfig {

        String mapVal();
    }

    interface SubConfig {

        String v();
    }

    interface SubConfig {

        String v();
    }

    interface B {

        C c();

        interface C {

            String leaf();
        }
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface B {

        C c();

        interface C {

            String leaf();
        }
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface SubConfig {

        String s();
    }

    interface SubConfig {

        String s();
    }

    interface SubConfig {

        String m();
    }

    interface SubConfig {

        String m();
    }

    interface ChildConfig {

        String name();
    }

    interface LevelTwo {

        String value();
    }

    interface LevelTwo {

        String value();
    }

    interface LevelOne {

        LevelTwo levelTwo();

        interface LevelTwo {

            String value();
        }
    }

    interface LevelTwo {

        String value();
    }

    interface LevelTwo {

        String value();
    }

    interface LevelTwo {

        String value();
    }

    interface LevelTwo {

        String value();
    }

    interface SubConfig {

        String name();
    }

    interface SubConfig {

        String name();
    }

    interface A {

        String v();
    }

    interface B {

        int i();
    }

    interface C {

        boolean flag();
    }

    interface A {

        String v();
    }

    interface B {

        int i();
    }

    interface C {

        boolean flag();
    }

    interface Parent {

        String p();
    }

    interface Child extends Parent {

        String c();
    }

    interface Parent {

        String p();
    }

    interface Child extends Parent {

        String c();
    }

    interface SubConfig {

        String id();
    }

    interface SubConfig {

        String id();
    }

    interface SubConfig {

        String value();
    }

    interface SubConfig {

        String value();
    }

    interface SubConfig {

        String data();
    }

    interface SubConfig {

        String data();
    }

    interface SubConfig {

        String opt();
    }

    interface SubConfig {

        String opt();
    }

    interface SubConfig {

        String mapVal();
    }

    interface SubConfig {

        String mapVal();
    }

    interface SubConfig {

        String v();
    }

    interface SubConfig {

        String v();
    }

    interface B {

        C c();

        interface C {

            String leaf();
        }
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface B {

        C c();

        interface C {

            String leaf();
        }
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface SubConfig {

        String s();
    }

    interface SubConfig {

        String s();
    }

    interface SubConfig {

        String m();
    }

    interface SubConfig {

        String m();
    }

    interface ChildConfig {

        String name();
    }

    interface ChildConfig {

        String name();
    }

    interface LevelOne {

        LevelTwo levelTwo();

        interface LevelTwo {

            String value();
        }
    }

    interface LevelTwo {

        String value();
    }

    interface LevelTwo {

        String value();
    }

    interface LevelOne {

        LevelTwo levelTwo();

        interface LevelTwo {

            String value();
        }
    }

    interface LevelTwo {

        String value();
    }

    interface LevelTwo {

        String value();
    }

    interface LevelTwo {

        String value();
    }

    interface SubConfig {

        String name();
    }

    interface SubConfig {

        String name();
    }

    interface A {

        String v();
    }

    interface B {

        int i();
    }

    interface C {

        boolean flag();
    }

    interface A {

        String v();
    }

    interface B {

        int i();
    }

    interface C {

        boolean flag();
    }

    interface Parent {

        String p();
    }

    interface Child extends Parent {

        String c();
    }

    interface Parent {

        String p();
    }

    interface Child extends Parent {

        String c();
    }

    interface SubConfig {

        String id();
    }

    interface SubConfig {

        String id();
    }

    interface SubConfig {

        String value();
    }

    interface SubConfig {

        String value();
    }

    interface SubConfig {

        String data();
    }

    interface SubConfig {

        String data();
    }

    interface SubConfig {

        String opt();
    }

    interface SubConfig {

        String opt();
    }

    interface SubConfig {

        String mapVal();
    }

    interface SubConfig {

        String mapVal();
    }

    interface SubConfig {

        String v();
    }

    interface SubConfig {

        String v();
    }

    interface B {

        C c();

        interface C {

            String leaf();
        }
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface B {

        C c();

        interface C {

            String leaf();
        }
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    interface SubConfig {

        String s();
    }

    interface SubConfig {

        String s();
    }

    interface SubConfig {

        String m();
    }

    interface SubConfig {

        String m();
    }

    @ConfigMapping(prefix = "parent")
    interface ParentConfig {

        ChildConfig child();

        interface ChildConfig {

            String name();
        }
    }

    interface ChildConfig {

        String name();
    }

    interface ChildConfig {

        String name();
    }

    @ConfigMapping(prefix = "root")
    interface RootConfig {

        LevelOne levelOne();

        interface LevelOne {

            LevelTwo levelTwo();

            interface LevelTwo {

                String value();
            }
        }
    }

    interface LevelOne {

        LevelTwo levelTwo();

        interface LevelTwo {

            String value();
        }
    }

    interface LevelTwo {

        String value();
    }

    interface LevelOne {

        LevelTwo levelTwo();

        interface LevelTwo {

            String value();
        }
    }

    interface LevelTwo {

        String value();
    }

    interface LevelTwo {

        String value();
    }

    @ConfigMapping(prefix = "flat")
    interface FlatConfig {

        String simple();
    }

    @ConfigMapping(prefix = "empty")
    interface EmptyConfig {
        // No methods
    }

    @ConfigMapping(prefix = "single")
    interface SingleParent {

        SubConfig sub();

        interface SubConfig {

            String name();
        }
    }

    interface SubConfig {

        String name();
    }

    interface SubConfig {

        String name();
    }

    @ConfigMapping(prefix = "multi")
    interface MultiParent {

        A a();

        B b();

        C c();

        interface A {

            String v();
        }

        interface B {

            int i();
        }

        interface C {

            boolean flag();
        }
    }

    interface A {

        String v();
    }

    interface B {

        int i();
    }

    interface C {

        boolean flag();
    }

    interface A {

        String v();
    }

    interface B {

        int i();
    }

    interface C {

        boolean flag();
    }

    @ConfigMapping(prefix = "super")
    interface SuperParent {

        Child child();

        interface Parent {

            String p();
        }

        interface Child extends Parent {

            String c();
        }
    }

    interface Parent {

        String p();
    }

    interface Child extends Parent {

        String c();
    }

    interface Parent {

        String p();
    }

    interface Child extends Parent {

        String c();
    }

    @ConfigMapping(prefix = "array")
    interface ArrayParent {

        SubConfig[] subs();

        interface SubConfig {

            String id();
        }
    }

    interface SubConfig {

        String id();
    }

    interface SubConfig {

        String id();
    }

    @ConfigMapping(prefix = "wild")
    interface WildcardParent {

        List<? extends SubConfig> list();

        interface SubConfig {

            String value();
        }
    }

    interface SubConfig {

        String value();
    }

    interface SubConfig {

        String value();
    }

    @ConfigMapping(prefix = "col")
    interface CollectionParent {

        List<SubConfig> subConfigs();

        interface SubConfig {

            String data();
        }
    }

    interface SubConfig {

        String data();
    }

    interface SubConfig {

        String data();
    }

    @ConfigMapping(prefix = "opt")
    interface OptionalParent {

        java.util.Optional<SubConfig> optionalSub();

        interface SubConfig {

            String opt();
        }
    }

    interface SubConfig {

        String opt();
    }

    interface SubConfig {

        String opt();
    }

    @ConfigMapping(prefix = "map")
    interface MapParent {

        java.util.Map<String, SubConfig> subConfigMap();

        interface SubConfig {

            String mapVal();
        }
    }

    interface SubConfig {

        String mapVal();
    }

    interface SubConfig {

        String mapVal();
    }

    @ConfigMapping(prefix = "prim")
    interface PrimitiveParent {

        int count();

        String name();
    }

    @ConfigMapping(prefix = "dup")
    interface DuplicateParent {

        SubConfig a();

        SubConfig b();

        interface SubConfig {

            String v();
        }
    }

    interface SubConfig {

        String v();
    }

    interface SubConfig {

        String v();
    }

    @ConfigMapping(prefix = "self")
    interface SelfParent {

        SelfParent self();
    }

    @ConfigMapping(prefix = "deep")
    interface DeepA {

        B b();

        interface B {

            C c();

            interface C {

                String leaf();
            }
        }
    }

    interface B {

        C c();

        interface C {

            String leaf();
        }
    }

    interface C {

        String leaf();
    }

    interface B {

        C c();

        interface C {

            String leaf();
        }
    }

    interface C {

        String leaf();
    }

    interface C {

        String leaf();
    }

    @ConfigMapping(prefix = "raw")
    interface RawListParent {

        // raw type, no generic information
        java.util.List rawList();
    }

    @ConfigMapping(prefix = "lower")
    interface LowerWildcardParent {

        List<? super SubConfig> superWildcard();

        interface SubConfig {

            String s();
        }
    }

    interface SubConfig {

        String s();
    }

    interface SubConfig {

        String s();
    }

    @ConfigMapping(prefix = "multiarr")
    interface MultiArrayParent {

        SubConfig[][] matrix();

        interface SubConfig {

            String m();
        }
    }

    interface SubConfig {

        String m();
    }

    interface SubConfig {

        String m();
    }

    /**
     * Returns the list of interface types contained in the nested metadata.
     */
    private static List<Class<?>> nestedInterfaceTypes(ConfigMappingInterface meta) {
        return meta.getNested().stream().map(ConfigMappingInterface::getInterfaceType).collect(Collectors.toList());
    }

    @Test
    void getNested_singleLevel() {
        ConfigMappingInterface parentMeta = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        assertNotNull(parentMeta, "Metadata for ParentConfig should not be null");
        List<ConfigMappingInterface> nested = parentMeta.getNested();
        assertEquals(1, nested.size(), "ParentConfig should have exactly one nested configuration interface");
        ConfigMappingInterface childMeta = nested.get(0);
        assertEquals(ParentConfig.ChildConfig.class, childMeta.getInterfaceType(), "Nested interface type should be ParentConfig.ChildConfig");
    }

    @Test
    void getNested_multipleLevels() {
        ConfigMappingInterface rootMeta = ConfigMappingInterface.getConfigurationInterface(RootConfig.class);
        assertNotNull(rootMeta, "Metadata for RootConfig should not be null");
        // First level of nesting
        List<ConfigMappingInterface> firstLevel = rootMeta.getNested();
        assertEquals(1, firstLevel.size(), "RootConfig should have one direct nested interface");
        ConfigMappingInterface levelOneMeta = firstLevel.get(0);
        assertEquals(RootConfig.LevelOne.class, levelOneMeta.getInterfaceType(), "First nested interface should be RootConfig.LevelOne");
        // Second level – retrieve nested from the first level meta
        List<ConfigMappingInterface> secondLevel = levelOneMeta.getNested();
        assertEquals(1, secondLevel.size(), "LevelOne should have one nested interface");
        ConfigMappingInterface levelTwoMeta = secondLevel.get(0);
        assertEquals(RootConfig.LevelOne.LevelTwo.class, levelTwoMeta.getInterfaceType(), "Second nested interface should be RootConfig.LevelOne.LevelTwo");
    }

    @Test
    void getNested_noNestedInterfaces() {
        ConfigMappingInterface flatMeta = ConfigMappingInterface.getConfigurationInterface(FlatConfig.class);
        assertNotNull(flatMeta, "Metadata for FlatConfig should not be null");
        List<ConfigMappingInterface> nested = flatMeta.getNested();
        assertTrue(nested.isEmpty(), "FlatConfig should have no nested configuration interfaces");
    }

    @Test
    @DisplayName("EmptyProperties – getNested() on interface with no methods")
    void EmptyProperties() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(EmptyConfig.class);
        assertNotNull(meta);
        List<ConfigMappingInterface> nested = meta.getNested();
        assertTrue(nested.isEmpty(), "Expected empty list when no properties are defined");
    }

    @Test
    @DisplayName("SingleNestedProperty – one nested configuration interface")
    void SingleNestedProperty() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(SingleParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        assertEquals(1, types.size(), "Exactly one nested interface expected");
        assertEquals(SingleParent.SubConfig.class, types.get(0));
    }

    @Test
    @DisplayName("MultipleNestedProperties – three distinct nested interfaces")
    void MultipleNestedProperties() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(MultiParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        assertEquals(3, types.size(), "Three nested interfaces expected");
        assertTrue(types.containsAll(Set.of(MultiParent.A.class, MultiParent.B.class, MultiParent.C.class)));
    }

    @Test
    @DisplayName("PropertiesWithSuperTypes – nested type with super‑type hierarchy")
    void PropertiesWithSuperTypes() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(SuperParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        // The algorithm should return only the most specific type (Child) – duplicate super‑type is not added.
        assertEquals(1, types.size(), "Only the concrete child interface should be present");
        assertEquals(SuperParent.Child.class, types.get(0));
    }

    @Test
    @DisplayName("NullPropertiesSafety – properties field forced to null")
    void NullPropertiesSafety() throws Exception {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(EmptyConfig.class);
        // Use reflection to set the private 'properties' field to null.
        Field f = ConfigMappingInterface.class.getDeclaredField("properties");
        f.setAccessible(true);
        f.set(meta, null);
        // The method must not throw NPE and should behave as if there were no properties.
        List<ConfigMappingInterface> nested = meta.getNested();
        assertTrue(nested.isEmpty(), "Null properties array should be treated as empty");
    }

    @Test
    @DisplayName("NestedPropertyWithArrayType – array of configuration interface")
    void NestedPropertyWithArrayType() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(ArrayParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        assertEquals(1, types.size());
        assertEquals(ArrayParent.SubConfig.class, types.get(0));
    }

    @Test
    @DisplayName("NestedPropertyWithWildcard – upper‑bounded wildcard")
    void NestedPropertyWithWildcard() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(WildcardParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        assertEquals(1, types.size());
        assertEquals(WildcardParent.SubConfig.class, types.get(0));
    }

    @Test
    @DisplayName("GenericCollectionNested – List<SubConfig>")
    void GenericCollectionNested() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(CollectionParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        assertEquals(1, types.size());
        assertEquals(CollectionParent.SubConfig.class, types.get(0));
    }

    @Test
    @DisplayName("OptionalNested – Optional<SubConfig>")
    void OptionalNested() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(OptionalParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        assertEquals(1, types.size());
        assertEquals(OptionalParent.SubConfig.class, types.get(0));
    }

    @Test
    @DisplayName("MapNested – Map<String, SubConfig>")
    void MapNested() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(MapParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        assertEquals(1, types.size());
        assertEquals(MapParent.SubConfig.class, types.get(0));
    }

    @Test
    @DisplayName("PrimitivePropertyIgnored – only primitive / String properties")
    void PrimitivePropertyIgnored() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(PrimitiveParent.class);
        List<ConfigMappingInterface> nested = meta.getNested();
        assertTrue(nested.isEmpty(), "Primitive and String return types must be ignored");
    }

    @Test
    @DisplayName("DuplicateNestedNoDuplicates – same nested type referenced twice")
    void DuplicateNestedNoDuplicates() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(DuplicateParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        assertEquals(1, types.size(), "Duplicate references should be de‑duplicated");
        assertEquals(DuplicateParent.SubConfig.class, types.get(0));
    }

    @Test
    @DisplayName("CyclicNestedReference – self‑referencing property")
    void CyclicNestedReference() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(SelfParent.class);
        List<ConfigMappingInterface> nested = meta.getNested();
        // The self‑reference must be ignored to avoid infinite recursion.
        assertTrue(nested.isEmpty(), "Self‑reference should not appear in nested list");
    }

    @Test
    @DisplayName("DeepNestedHierarchy – A -> B -> C")
    void DeepNestedHierarchy() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(DeepA.class);
        List<Class<?>> firstLevel = nestedInterfaceTypes(meta);
        assertEquals(1, firstLevel.size());
        assertEquals(DeepA.B.class, firstLevel.get(0));
        // Retrieve nested from B
        ConfigMappingInterface bMeta = ConfigMappingInterface.getConfigurationInterface(DeepA.B.class);
        List<Class<?>> secondLevel = nestedInterfaceTypes(bMeta);
        assertEquals(1, secondLevel.size());
        assertEquals(DeepA.B.C.class, secondLevel.get(0));
    }

    @Test
    @DisplayName("RawTypeNested – raw List (no generic info)")
    void RawTypeNested() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(RawListParent.class);
        List<ConfigMappingInterface> nested = meta.getNested();
        assertTrue(nested.isEmpty(), "Raw collection types must not be considered nested");
    }

    @Test
    @DisplayName("BoundedWildcardLower – lower‑bounded wildcard is ignored")
    void BoundedWildcardLower() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(LowerWildcardParent.class);
        List<ConfigMappingInterface> nested = meta.getNested();
        assertTrue(nested.isEmpty(), "Lower‑bounded wildcard should not yield a nested interface");
    }

    @Test
    @DisplayName("MultiDimensionalArrayNested – SubConfig[][]")
    void MultiDimensionalArrayNested() {
        ConfigMappingInterface meta = ConfigMappingInterface.getConfigurationInterface(MultiArrayParent.class);
        List<Class<?>> types = nestedInterfaceTypes(meta);
        assertEquals(1, types.size());
        assertEquals(MultiArrayParent.SubConfig.class, types.get(0));
    }
}
