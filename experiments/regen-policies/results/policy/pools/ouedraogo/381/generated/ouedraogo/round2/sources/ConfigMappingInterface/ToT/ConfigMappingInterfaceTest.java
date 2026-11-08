package io.smallrye.config;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.DefaultMethodProperty;
import io.smallrye.config.ConfigMappingInterface.GroupProperty;
import io.smallrye.config.ConfigMappingInterface.LeafProperty;
import io.smallrye.config.ConfigMappingInterface.MapProperty;
import io.smallrye.config.ConfigMappingInterface.OptionalProperty;
import io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappingInterface.ToStringMethod;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Basic factory / caching behaviour of {@code getConfigurationInterface}</li>
 *   <li>Generation of the implementation class name and internal name</li>
 *   <li>Detection of super‑type hierarchy</li>
 *   <li>Detection and classification of all supported {@link Property} sub‑types
 *       (primitive, leaf, group, optional, map, collection, default method, toString method)</li>
 *   <li>Behaviour of {@link ConfigMappingInterface#getProperties()} (the hierarchical map view)</li>
 *   <li>Edge‑cases such as non‑config interfaces, Java platform interfaces and self‑reference checks</li>
 * </ul>
 *
 * <p>All test interfaces are declared as static nested types so that they are compiled together with the test
 * class and are available at runtime for reflection.
 */
public class ConfigMappingInterfaceTest {

    /* -------------------------------------------------------------
     *  Helper utilities used by many test methods
     * ------------------------------------------------------------- */
    private static ConfigMappingInterface cmi(final Class<?> iface) {
        ConfigMappingInterface result = ConfigMappingInterface.getConfigurationInterface(iface);
        assertNotNull("ConfigMappingInterface must not be null for " + iface, result);
        return result;
    }

    private static Property findByMemberName(ConfigMappingInterface cmi, String memberName) {
        for (Property p : cmi.getProperties()) {
            if (p.getMemberName().equals(memberName)) {
                return p;
            }
        }
        fail("No property with member name " + memberName + " in " + cmi.getInterfaceType());
        return null; // unreachable
    }

    /* -------------------------------------------------------------
     *  1. Factory & caching
     * ------------------------------------------------------------- */
    @Test
    public void testFactoryAndCaching() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertSame("Factory must cache the instance", first, second);
    }

    @Test
    public void testFactoryRejectsJavaPlatformInterfaces() {
        // java.lang.Runnable is a java.* interface – the factory must return null
        assertNull("Java platform interfaces must be ignored",
                ConfigMappingInterface.getConfigurationInterface(Runnable.class));
    }

    @Test
    public void testFactoryRejectsGenericInterfaces() {
        // An interface with a type parameter should be rejected (returns null)
        assertNull("Generic interfaces must be ignored",
                ConfigMappingInterface.getConfigurationInterface(GenericInterface.class));
    }

    /* -------------------------------------------------------------
     *  2. Implementation class name & internal name
     * ------------------------------------------------------------- */
    @Test
    public void testImplementationClassName() {
        ConfigMappingInterface cmi = cmi(SimpleConfig.class);
        String expected = SimpleConfig.class.getName() + "$$CMImpl";
        assertEquals("Implementation class name must match the convention", expected, cmi.getClassName());
        assertEquals("Internal name must replace '.' with '/'",
                expected.replace('.', '/'), cmi.getClassInternalName());
    }

    /* -------------------------------------------------------------
     *  3. Super‑type handling
     * ------------------------------------------------------------- */
    @Test
    public void testSuperTypesAreDetected() {
        ConfigMappingInterface child = cmi(ChildConfig.class);
        ConfigMappingInterface[] supers = child.getSuperTypes();
        assertEquals("ChildConfig extends one super type", 1, supers.length);
        assertEquals("Super type must be ParentConfig", ParentConfig.class, supers[0].getInterfaceType());
    }

    /* -------------------------------------------------------------
     *  4. Property detection – primitive
     * ------------------------------------------------------------- */
    @Test
    public void testPrimitiveProperty() {
        Property p = findByMemberName(cmi(SimpleConfig.class), "intValue");
        assertTrue("intValue must be recognised as primitive", p.isPrimitive());
        PrimitiveProperty pp = p.asPrimitive();
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertEquals("intValue", pp.getMemberName());
        assertFalse("No @WithConverter on intValue", pp.hasConvertWith());
        assertFalse("No default value on intValue", pp.hasDefaultValue());
    }

    @Test
    public void testPrimitivePropertyWithDefaultAndConverter() {
        Property p = findByMemberName(cmi(PrimitiveWithConverter.class), "booleanValue");
        assertTrue(p.isPrimitive());
        PrimitiveProperty pp = p.asPrimitive();
        assertTrue("hasConvertWith must be true", pp.hasConvertWith());
        assertEquals(TestBooleanConverter.class, pp.getConvertWith());
        assertTrue("hasDefaultValue must be true", pp.hasDefaultValue());
        assertEquals("true", pp.getDefaultValue());
    }

    /* -------------------------------------------------------------
     *  5. Leaf property (String) with @WithDefault
     * ------------------------------------------------------------- */
    @Test
    public void testLeafPropertyWithDefault() {
        Property p = findByMemberName(cmi(SimpleConfig.class), "stringValue");
        assertTrue("stringValue must be a leaf", p.isLeaf());
        LeafProperty lp = p.asLeaf();
        assertEquals(String.class, lp.getValueRawType());
        assertTrue("Leaf with @WithDefault must report a default", lp.hasDefaultValue());
        assertEquals("hello", lp.getDefaultValue());
        assertFalse("Leaf without @WithConverter must report false", lp.hasConvertWith());
    }

    /* -------------------------------------------------------------
     *  6. Custom name handling (@WithName, @WithParentName)
     * ------------------------------------------------------------- */
    @Test
    public void testCustomNameAndParentName() {
        Property named = findByMemberName(cmi(SimpleConfig.class), "customName");
        assertEquals("custom", named.getPropertyName());
        Property parent = findByMemberName(cmi(SimpleConfig.class), "parentName");
        assertTrue("parentName must be recognised as parent name", parent.isParentPropertyName());
        assertEquals("", parent.getPropertyName()); // empty string for parent name
    }

    /* -------------------------------------------------------------
     *  7. Optional property (Optional<String>)
     * ------------------------------------------------------------- */
    @Test
    public void testOptionalProperty() {
        Property p = findByMemberName(cmi(OptionalConfig.class), "maybe");
        assertTrue("maybe must be optional", p.isOptional());
        OptionalProperty op = p.asOptional();
        assertTrue("Nested property of optional must be leaf", op.getNestedProperty().isLeaf());
        LeafProperty nestedLeaf = op.getNestedProperty().asLeaf();
        assertEquals(String.class, nestedLeaf.getValueRawType());
    }

    /* -------------------------------------------------------------
     *  8. Group property (nested configuration interface)
     * ------------------------------------------------------------- */
    @Test
    public void testGroupProperty() {
        Property p = findByMemberName(cmi(ParentConfig.class), "nested");
        assertTrue("nested must be a group", p.isGroup());
        GroupProperty gp = p.asGroup();
        assertEquals(NestedConfig.class, gp.getGroupType().getInterfaceType());
        // nested configuration must expose its own leaf property
        Property inner = findByMemberName(gp.getGroupType(), "nestedValue");
        assertTrue(inner.isLeaf());
    }

    /* -------------------------------------------------------------
     *  9. Map property (Map<String,Integer>) with default value
     * ------------------------------------------------------------- */
    @Test
    public void testMapProperty() {
        Property p = findByMemberName(cmi(MapConfig.class), "map");
        assertTrue("map must be recognised as a map", p.isMap());
        MapProperty mp = p.asMap();
        assertEquals(String.class, mp.getKeyRawType());
        assertEquals(Integer.class, mp.getValueProperty().asLeaf().getValueRawType());
        assertTrue("Map with @WithDefault must report default", mp.hasDefaultValue());
        assertEquals("a=1,b=2", mp.getDefaultValue());
    }

    /* -------------------------------------------------------------
     * 10. Collection property (List<String>) and collection of groups
     * ------------------------------------------------------------- */
    @Test
    public void testCollectionOfLeafs() {
        Property p = findByMemberName(cmi(CollectionConfig.class), "list");
        assertTrue("list must be a collection", p.isCollection());
        CollectionProperty cp = p.asCollection();
        assertEquals(List.class, cp.getCollectionRawType());
        assertTrue(cp.getElement().isLeaf());
    }

    @Test
    public void testCollectionOfGroups() {
        Property p = findByMemberName(cmi(CollectionConfig.class), "groupList");
        assertTrue("groupList must be a collection", p.isCollection());
        CollectionProperty cp = p.asCollection();
        assertTrue(cp.getElement().isGroup());
        GroupProperty gp = cp.getElement().asGroup();
        assertEquals(NestedConfig.class, gp.getGroupType().getInterfaceType());
    }

    /* -------------------------------------------------------------
     * 11. Default method handling
     * ------------------------------------------------------------- */
    @Test
    public void testDefaultMethodProperty() {
        Property p = findByMemberName(cmi(DefaultMethodConfig.class), "defaulted");
        assertTrue(p.isDefaultMethod());
        DefaultMethodProperty dmp = p.asDefaultMethod();
        assertNotNull(dmp.getDefaultMethod());
        // the default property is the leaf defined by the default method return type
        assertTrue(dmp.getDefaultProperty().isLeaf());
    }

    /* -------------------------------------------------------------
     * 12. ToString method detection
     * ------------------------------------------------------------- */
    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cmi = cmi(ToStringConfig.class);
        // the toString method is stored separately – it must not appear in the normal properties array
        for (Property p : cmi.getProperties()) {
            assertFalse("toString must not be part of normal properties", p.isToStringMethod());
        }
        ToStringMethod tsm = cmi.getToStringMethod();
        assertTrue(tsm.isToStringMethod());
        assertTrue(tsm.generate()); // generated because we declared an abstract toString()
    }

    /* -------------------------------------------------------------
     * 13. Naming strategy fallback (no @ConfigMapping present)
     * ------------------------------------------------------------- */
    @Test
    public void testNamingStrategyFallback() {
        ConfigMappingInterface cmi = cmi(UnannotatedConfig.class);
        // No @ConfigMapping -> default strategy is KEBAB_CASE
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
    }

    /* -------------------------------------------------------------
     * 14. Hierarchical property map produced by getProperties()
     * ------------------------------------------------------------- */
    @Test
    public void testHierarchicalPropertiesMap() {
        ConfigMappingInterface cmi = cmi(ParentConfig.class);
        Map<Class<?>, Map<String, Map<String, Property>>>
                hierarchy = ConfigMappingInterface.getProperties(cmi);

        // root class entry
        assertTrue(hierarchy.containsKey(ParentConfig.class));
        Map<String, Map<String, Property>> root = hierarchy.get(ParentConfig.class);
        // the path for the leaf inside the nested group is "nested.nested-value" (using default kebab case)
        assertTrue(root.containsKey("nested"));
        Map<String, Property> nestedProps = root.get("nested");
        assertTrue(nestedProps.containsKey("nested-value"));
        Property leaf = nestedProps.get("nested-value");
        assertTrue(leaf.isLeaf());
        assertEquals("nestedValue", leaf.getMemberName());
    }

    /* -------------------------------------------------------------
     * 15. getNested() returns all group types reachable from the root
     * ------------------------------------------------------------- */
    @Test
    public void testGetNested() {
        ConfigMappingInterface cmi = cmi(ParentConfig.class);
        List<ConfigMappingInterface> nested = cmi.getNested();
        // only one nested configuration interface: NestedConfig
        assertEquals(1, nested.size());
        assertEquals(NestedConfig.class, nested.get(0).getInterfaceType());
    }

    /* -------------------------------------------------------------
     * 16. hasConfigMapping() true only when @ConfigMapping is present
     * ------------------------------------------------------------- */
    @Test
    public void testHasConfigMappingFlag() {
        assertTrue("SimpleConfig is annotated with @ConfigMapping", cmi(SimpleConfig.class).hasConfigMapping());
        assertFalse("UnannotatedConfig has no @ConfigMapping", cmi(UnannotatedConfig.class).hasConfigMapping());
    }

    /* -------------------------------------------------------------
     * 17. isBeanStyleGetters() respects the annotation attribute
     * ------------------------------------------------------------- */
    @Test
    public void testBeanStyleGettersFlag() {
        assertTrue("BeanConfig has beanStyleGetters=true", cmi(BeanConfig.class).isBeanStyleGetters());
        assertFalse("SimpleConfig defaults to false", cmi(SimpleConfig.class).isBeanStyleGetters());
    }

    /* -------------------------------------------------------------
     * 18. Self‑reference detection – a configuration interface cannot reference itself
     * ------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testSelfReferenceRejected() {
        // The factory will throw when it tries to create a Property for a method returning its own type
        ConfigMappingInterface.getConfigurationInterface(SelfReferenceConfig.class);
    }

    /* -------------------------------------------------------------
     * 19. PrimitiveProperty unboxing helpers
     * ------------------------------------------------------------- */
    @Test
    public void testPrimitiveUnboxingInfo() {
        PrimitiveProperty pp = (PrimitiveProperty) findByMemberName(cmi(PrimitiveWithConverter.class), "booleanValue");
        assertEquals("booleanValue", pp.getMemberName());
        assertEquals("booleanValue", pp.getPropertyName());
        // unbox method name and descriptor are internal details – verify they exist for the primitive type
        assertEquals("booleanValue", pp.getUnboxMethodName());
        assertEquals("()Z", pp.getUnboxMethodDescriptor());
    }

    /* -------------------------------------------------------------
     * 20. Verify that getClassBytes throws a wrapped ConfigMessages exception
     * ------------------------------------------------------------- */
    @Test
    public void testGetClassBytesExceptionWrapping() {
        ConfigMappingInterface cmi = cmi(SimpleConfig.class);
        try {
            cmi.getClassBytes();
            fail("Expected a RuntimeException wrapping the generation failure");
        } catch (RuntimeException e) {
            // The exact type is not important – we only assert that the cause is a ConfigMessages exception
            assertTrue(e.getMessage().contains("couldNotGenerateMapping"));
        }
    }

    /* =============================================================
     *  Test Interfaces used by the suite
     * ============================================================= */

    @ConfigMapping(prefix = "simple")
    public interface SimpleConfig {
        int intValue();

        @WithDefault("hello")
        String stringValue();

        @WithName("custom")
        String customName();

        @WithParentName
        String parentName();

        // abstract toString method – should be identified as ToStringMethod
        String toString();
    }

    @ConfigMapping(prefix = "primitive")
    public interface PrimitiveWithConverter {
        @WithDefault("true")
        @WithConverter(TestBooleanConverter.class)
        boolean booleanValue();
    }

    public static class TestBooleanConverter implements Converter<Boolean> {
        @Override
        public Boolean convert(String value) {
            return Boolean.valueOf(value);
        }
    }

    @ConfigMapping(prefix = "optional")
    public interface OptionalConfig {
        Optional<String> maybe();
    }

    @ConfigMapping(prefix = "parent")
    public interface ParentConfig {
        NestedConfig nested();
    }

    public interface NestedConfig {
        String nestedValue();
    }

    @ConfigMapping(prefix = "child")
    public interface ChildConfig extends ParentConfig {
        // inherits nested() from ParentConfig
    }

    @ConfigMapping(prefix = "map")
    public interface MapConfig {
        @WithDefault("a=1,b=2")
        Map<String, Integer> map();
    }

    @ConfigMapping(prefix = "collection")
    public interface CollectionConfig {
        List<String> list();

        List<NestedConfig> groupList();
    }

    @ConfigMapping(prefix = "default")
    public interface DefaultMethodConfig {
        default String defaulted() {
            return "default";
        }
    }

    @ConfigMapping(prefix = "tostring")
    public interface ToStringConfig {
        // abstract toString – will be treated as a ToStringMethod
        String toString();
        String other();
    }

    public interface UnannotatedConfig {
        String foo();
    }

    @ConfigMapping(prefix = "bean", beanStyleGetters = true)
    public interface BeanConfig {
        String getFoo();
    }

    @ConfigMapping(prefix = "self")
    public interface SelfReferenceConfig {
        SelfReferenceConfig self(); // illegal self‑reference
    }

    // generic interface – should be ignored by the factory
    public interface GenericInterface<T> {
        T get();
    }
}
