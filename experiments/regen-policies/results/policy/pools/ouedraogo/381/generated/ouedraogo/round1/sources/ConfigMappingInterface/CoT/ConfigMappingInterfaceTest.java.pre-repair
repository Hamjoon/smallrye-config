package io.smallrye.config;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link ConfigMappingInterface}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods</li>
 *   <li>Typical property types (primitive, leaf, optional, group, map, collection)</li>
 *   <li>Edge‑case handling (methods with parameters, void return, self‑reference, @ToString detection)</li>
 *   <li>Naming‑strategy defaults and {@code @ConfigMapping} handling</li>
 *   <li>Nested configuration detection via {@link ConfigMappingInterface#getNested()}</li>
 * </ul>
 *
 * The test interfaces are defined as static inner types so that they are compiled together
 * with the test class and can be inspected by {@link ConfigMappingInterface#getConfigurationInterface(Class)}.
 */
public class ConfigMappingInterfaceTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    private ConfigMappingInterface findPropertyByMemberName(ConfigMappingInterface cfg, String memberName) {
        for (ConfigMappingInterface.Property p : cfg.getProperties()) {
            if (p.getMemberName().equals(memberName)) {
                return p;
            }
        }
        return null;
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Public API – basic sanity checks                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBasicApiMethods() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("ConfigMappingInterface must not be null", cfg);

        // getInterfaceType()
        assertEquals(SimpleConfig.class, cfg.getInterfaceType());

        // getClassName()
        String expectedClassName = ConfigMappingInterface.getImplementationClassName(SimpleConfig.class);
        assertEquals(expectedClassName, cfg.getClassName());

        // getSuperTypes()
        assertArrayEquals(new ConfigMappingInterface[0], cfg.getSuperTypes());

        // getProperties()
        ConfigMappingInterface.Property[] props = cfg.getProperties();
        assertEquals(2, props.length); // primitive int and String leaf

        // getToStringMethod()
        assertFalse(cfg.getToStringMethod().generate());

        // hasConfigMapping() – SimpleConfig is NOT annotated with @ConfigMapping
        assertFalse(cfg.hasConfigMapping());

        // naming strategy defaults to KEBAB_CASE when no @ConfigMapping is present
        assertEquals(ConfigMapping.NamingStrategy.KEBAB_CASE, cfg.getNamingStrategy());

        // beanStyleGetters defaults to false
        assertFalse(cfg.isBeanStyleGetters());

        // internal name uses '/' as separator
        assertEquals(cfg.getClassName().replace('.', '/'), cfg.getClassInternalName());
    }

    /* ---------------------------------------------------------------------- */
    /*  2. Primitive property (int)                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPrimitiveProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(PrimitiveConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMemberName(cfg, "value");
        assertNotNull(p);
        assertTrue(p.isPrimitive());

        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertFalse(pp.hasConvertWith()); // no @WithConverter defined
        assertTrue(pp.hasDefaultValue());
        assertEquals("42", pp.getDefaultValue());
    }

    /* ---------------------------------------------------------------------- */
    /*  3. Leaf property (String)                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLeafProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(LeafConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMemberName(cfg, "name");
        assertNotNull(p);
        assertTrue(p.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = p.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
        assertFalse(leaf.isSecret());
    }

    /* ---------------------------------------------------------------------- */
    /*  4. Optional property (Optional<String>)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMemberName(cfg, "maybe");
        assertNotNull(p);
        assertTrue(p.isOptional());

        ConfigMappingInterface.OptionalProperty opt = p.asOptional();
        assertTrue(opt.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = opt.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    /* ---------------------------------------------------------------------- */
    /*  5. Group property (nested config interface)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGroupProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMemberName(cfg, "sub");
        assertNotNull(p);
        assertTrue(p.isGroup());

        ConfigMappingInterface.GroupProperty gp = p.asGroup();
        ConfigMappingInterface subCfg = gp.getGroupType();
        assertEquals(SubConfig.class, subCfg.getInterfaceType());

        // verify that the nested property inside the group is present
        ConfigMappingInterface.Property subProp = findPropertyByMemberName(subCfg, "value");
        assertNotNull(subProp);
        assertTrue(subProp.isLeaf());
    }

    /* ---------------------------------------------------------------------- */
    /*  6. Map property (Map<String,Integer>)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMemberName(cfg, "values");
        assertNotNull(p);
        assertTrue(p.isMap());

        ConfigMappingInterface.MapProperty mp = p.asMap();
        assertEquals(String.class, mp.getKeyRawType());
        assertEquals(Integer.class, ((ConfigMappingInterface.LeafProperty) mp.getValueProperty()).getValueRawType());
        assertFalse(mp.hasKeyUnnamed());
        assertFalse(mp.hasKeyProvider());
    }

    /* ---------------------------------------------------------------------- */
    /*  7. Collection property (List<String>)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(CollectionConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMemberName(cfg, "items");
        assertNotNull(p);
        assertTrue(p.isCollection());

        ConfigMappingInterface.CollectionProperty cp = p.asCollection();
        assertEquals(List.class, cp.getCollectionRawType());
        ConfigMappingInterface.LeafProperty element = (ConfigMappingInterface.LeafProperty) cp.getElement();
        assertEquals(String.class, element.getValueRawType());
    }

    /* ---------------------------------------------------------------------- */
    /*  8. @ToString detection – custom toString method                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        // the toString method should be recognized as a special property and not appear in the normal list
        ConfigMappingInterface.Property[] props = cfg.getProperties();
        for (ConfigMappingInterface.Property p : props) {
            assertFalse("toString method must not be in regular properties", p.isToStringMethod());
        }
        // the dedicated getter must return a ToStringMethod with generate() == true
        assertTrue(cfg.getToStringMethod().generate());
    }

    /* ---------------------------------------------------------------------- */
    /*  9. Edge case – method with parameters throws IllegalArgumentException */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testMethodWithParametersNotAllowed() {
        // The exception is thrown while building the metadata for the interface
        ConfigMappingInterface.getConfigurationInterface(BadConfigWithParams.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Edge case – void return type throws IllegalArgumentException      */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testVoidReturnMethodNotAllowed() {
        ConfigMappingInterface.getConfigurationInterface(BadConfigVoidReturn.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 11. Edge case – self‑reference type (interface returns itself)        */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testSelfReferenceNotAllowed() {
        ConfigMappingInterface.getConfigurationInterface(SelfReferenceConfig.class);
    }

    /* ---------------------------------------------------------------------- */
    /* 12. Nested detection via getNested()                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetNested() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(NestedConfig.class);
        List<ConfigMappingInterface> nested = cfg.getNested();
        // nested should contain SubConfig and its super‑type (none in this case)
        assertEquals(1, nested.size());
        assertEquals(SubConfig.class, nested.get(0).getInterfaceType());
    }

    /* ---------------------------------------------------------------------- */
    /* 13. Static method getProperties() – hierarchical map generation        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStaticGetPropertiesHierarchy() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(NestedConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>>
                hierarchy = ConfigMappingInterface.getProperties(cfg);

        // root class entry
        assertTrue(hierarchy.containsKey(NestedConfig.class));
        // ensure leaf property from nested group is reachable via the hierarchy map
        Map<String, Map<String, ConfigMappingInterface.Property>> nestedMap = hierarchy.get(SubConfig.class);
        assertNotNull(nestedMap);
        // key path "" (empty) contains the leaf "value"
        Map<String, ConfigMappingInterface.Property> leafs = nestedMap.get("");
        assertNotNull(leafs);
        assertTrue(leafs.containsKey("value"));
    }

    /* ---------------------------------------------------------------------- */
    /* 14. getClassBytes – ensure the method does not crash (implementation   */
    /*     may be a stub in the test environment)                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassBytesDoesNotThrow() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(PrimitiveConfig.class);
        // The generator may not be present in the test classpath; we only verify that an
        // unchecked exception is not thrown from the wrapper.
        byte[] bytes = cfg.getClassBytes();
        assertNotNull(bytes);
    }

    /* ---------------------------------------------------------------------- */
    /*  Test interfaces used by the test suite                                 */
    /* ---------------------------------------------------------------------- */

    // 1. Simple config – no @ConfigMapping
    public interface SimpleConfig {
        int primitive(); // not used directly, just to have a method
        String text();
    }

    // 2. Primitive with @WithDefault
    @ConfigMapping(prefix = "prim")
    public interface PrimitiveConfig {
        @WithDefault("42")
        int value();
    }

    // 3. Leaf property
    @ConfigMapping(prefix = "leaf")
    public interface LeafConfig {
        String name();
    }

    // 4. Optional leaf
    @ConfigMapping(prefix = "opt")
    public interface OptionalConfig {
        Optional<String> maybe();
    }

    // 5. Group – nested interface
    @ConfigMapping(prefix = "group")
    public interface GroupConfig {
        SubConfig sub();
    }

    @ConfigMapping(prefix = "sub")
    public interface SubConfig {
        int value();
    }

    // 6. Map property
    @ConfigMapping(prefix = "map")
    public interface MapConfig {
        Map<String, Integer> values();
    }

    // 7. Collection property
    @ConfigMapping(prefix = "col")
    public interface CollectionConfig {
        List<String> items();
    }

    // 8. Interface with a custom toString method (non‑default)
    @ConfigMapping(prefix = "tostr")
    public interface ToStringConfig {
        // non‑default toString, must be treated as a ToStringMethod property
        String toString();
    }

    // 9. Bad interface – method with parameters (should fail)
    public interface BadConfigWithParams {
        String hello(String name);
    }

    // 10. Bad interface – void return type (should fail)
    public interface BadConfigVoidReturn {
        void doIt();
    }

    // 11. Self‑reference – returns its own type (should fail)
    @ConfigMapping(prefix = "self")
    public interface SelfReferenceConfig {
        SelfReferenceConfig self();
    }

    // 12. Nested configuration (group inside a group)
    @ConfigMapping(prefix = "nested")
    public interface NestedConfig {
        SubConfig sub();
    }

    /* ---------------------------------------------------------------------- */
    /*  Stub converter – used only to satisfy type‑checking in tests that      */
    /*  need a @WithConverter annotation (not exercised directly here).       */
    /* ---------------------------------------------------------------------- */
    public static class StringToIntConverter implements Converter<Integer> {
        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }
}
