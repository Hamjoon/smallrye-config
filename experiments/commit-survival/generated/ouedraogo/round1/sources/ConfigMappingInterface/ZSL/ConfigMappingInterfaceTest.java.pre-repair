package io.smallrye.config;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

/**
 * Minimal dummy annotations and types required to compile the tests.
 * They mimic the signatures used by {@link ConfigMappingInterface}.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.PARAMETER })
@interface WithDefault {
    String value();
}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.PARAMETER })
@interface WithDefaults {}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.PARAMETER })
@interface WithUnnamedKey {
    String value();
}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.PARAMETER })
@interface WithConverter {
    Class<? extends Converter<?>> value();
}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE })
@interface WithParentName {}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE })
@interface WithName {
    String value();
}

/**
 * Dummy ConfigMapping annotation with a nested NamingStrategy enum.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
@interface ConfigMapping {
    NamingStrategy namingStrategy() default NamingStrategy.KEBAB_CASE;

    enum NamingStrategy {
        VERBATIM, KEBAB_CASE, SNAKE_CASE;

        public String apply(String name) {
            // simple identity implementation – sufficient for the tests
            return name;
        }
    }
}

/**
 * Dummy Converter interface matching the MicroProfile signature.
 */
interface Converter<T> {
    T convert(String value);
}

/**
 * Test suite for {@link ConfigMappingInterface}.
 */
public class ConfigMappingInterfaceTest {

    /* ---------- Helper interfaces used in tests ---------- */

    interface SimpleConfig {
        int primitive();
    }

    interface OptionalConfig {
        Optional<String> optional();
    }

    interface MapConfig {
        Map<String, Integer> map();
    }

    interface ListConfig {
        List<Integer> list();
    }

    @ConfigMapping(namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    interface GroupConfig {
        NestedGroup nested();
    }

    interface NestedGroup {
        long id();
    }

    interface CollectionGroupConfig {
        Set<NestedGroup> groups();
    }

    interface ToStringConfig {
        String toString();
    }

    interface BaseConfig {
        int base();
    }

    interface ChildConfig extends BaseConfig {
        String child();
    }

    /* ---------- Tests ---------- */

    @Test
    public void testGetConfigurationInterfaceCaching() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Assert.assertSame("Configuration interface should be cached and identical", first, second);
        Assert.assertEquals(SimpleConfig.class, first.getInterfaceType());
        Assert.assertEquals(1, first.getProperties().length);
        Assert.assertTrue(first.getProperties()[0] instanceof ConfigMappingInterface.PrimitiveProperty);
    }

    @Test
    public void testPrimitivePropertyDetails() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property prop = cfg.getProperties()[0];
        Assert.assertTrue(prop.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty prim = prop.asPrimitive();
        Assert.assertEquals(int.class, prim.getPrimitiveType());
        Assert.assertEquals(Integer.class, prim.getBoxType());
        Assert.assertFalse(prim.hasConvertWith());
        Assert.assertFalse(prim.hasDefaultValue());
        // getDefaultValue would throw NPE if called when no default is present – we avoid calling it.
        Assert.assertEquals("primitive", prop.getMemberName());
        Assert.assertEquals("primitive", prop.getPropertyName());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        ConfigMappingInterface.Property prop = cfg.getProperties()[0];
        Assert.assertTrue(prop.isOptional());
        ConfigMappingInterface.OptionalProperty opt = prop.asOptional();
        ConfigMappingInterface.MayBeOptionalProperty nested = opt.getNestedProperty();
        Assert.assertTrue(nested.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = nested.asLeaf();
        Assert.assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        ConfigMappingInterface.Property prop = cfg.getProperties()[0];
        Assert.assertTrue(prop.isMap());
        ConfigMappingInterface.MapProperty mapProp = prop.asMap();
        Assert.assertEquals(String.class, mapProp.getKeyRawType());
        Assert.assertFalse(mapProp.hasKeyUnnamed());
        ConfigMappingInterface.Property valueProp = mapProp.getValueProperty();
        Assert.assertTrue(valueProp.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = valueProp.asLeaf();
        Assert.assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testListCollectionProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        ConfigMappingInterface.Property prop = cfg.getProperties()[0];
        Assert.assertTrue(prop.isCollection());
        ConfigMappingInterface.CollectionProperty coll = prop.asCollection();
        ConfigMappingInterface.Property element = coll.getElement();
        Assert.assertTrue(element.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = element.asLeaf();
        Assert.assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testGroupProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        ConfigMappingInterface.Property prop = cfg.getProperties()[0];
        Assert.assertTrue(prop.isGroup());
        ConfigMappingInterface.GroupProperty grp = prop.asGroup();
        ConfigMappingInterface nested = grp.getGroupType();
        Assert.assertEquals(NestedGroup.class, nested.getInterfaceType());
        Assert.assertTrue(grp.hasNamingStrategy());
        Assert.assertEquals(ConfigMapping.NamingStrategy.SNAKE_CASE, grp.getNamingStrategy());
    }

    @Test
    public void testCollectionOfGroupProperty() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(CollectionGroupConfig.class);
        ConfigMappingInterface.Property prop = cfg.getProperties()[0];
        Assert.assertTrue(prop.isCollection());
        ConfigMappingInterface.CollectionProperty coll = prop.asCollection();
        ConfigMappingInterface.Property element = coll.getElement();
        Assert.assertTrue(element.isGroup());
        ConfigMappingInterface.GroupProperty grp = element.asGroup();
        ConfigMappingInterface nested = grp.getGroupType();
        Assert.assertEquals(NestedGroup.class, nested.getInterfaceType());
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        ConfigMappingInterface.Property prop = cfg.getProperties()[0];
        Assert.assertTrue(prop.isToStringMethod());
        ConfigMappingInterface.ToStringMethod toStringMethod = (ConfigMappingInterface.ToStringMethod) prop;
        Assert.assertTrue(toStringMethod.generate());
        Assert.assertEquals("toString", prop.getMemberName());
    }

    @Test
    public void testSuperTypesAndIncludeSuper() {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        // direct properties (without super)
        ConfigMappingInterface.Property[] direct = child.getProperties(false);
        Assert.assertEquals(1, direct.length);
        Assert.assertEquals("child", direct[0].getMemberName());

        // include super – should contain both base and child
        ConfigMappingInterface.Property[] all = child.getProperties(true);
        Assert.assertEquals(2, all.length);
        // order is not guaranteed – check presence by name
        boolean foundBase = false, foundChild = false;
        for (ConfigMappingInterface.Property p : all) {
            if ("base".equals(p.getMemberName())) foundBase = true;
            if ("child".equals(p.getMemberName())) foundChild = true;
        }
        Assert.assertTrue(foundBase);
        Assert.assertTrue(foundChild);
    }

    @Test
    public void testNamingStrategyDetection() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        Assert.assertTrue(cfg.hasNamingStrategy());
        Assert.assertEquals(ConfigMapping.NamingStrategy.SNAKE_CASE, cfg.getNamingStrategy());
    }

    @Test
    public void testClassNameAndInternalName() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        String className = cfg.getClassName();
        Assert.assertTrue(className.startsWith(SimpleConfig.class.getName()));
        Assert.assertEquals(className.replace('.', '/'), cfg.getClassInternalName());
    }

    @Test
    public void testGetNestedInterfaces() {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(CollectionGroupConfig.class);
        List<ConfigMappingInterface> nested = cfg.getNested();
        // Should contain the NestedGroup configuration interface exactly once
        Assert.assertEquals(1, nested.size());
        Assert.assertEquals(NestedGroup.class, nested.get(0).getInterfaceType());
    }

    @Test
    public void testPropertyEqualityAndHashCode() throws NoSuchMethodException {
        ConfigMappingInterface cfg = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p1 = cfg.getProperties()[0];
        Method method = SimpleConfig.class.getMethod("primitive");
        ConfigMappingInterface.PrimitiveProperty p2 = new ConfigMappingInterface.PrimitiveProperty(
                method, null, int.class, null, null);
        // They represent the same method – equality should be true
        Assert.assertEquals(p1, p2);
        Assert.assertEquals(p1.hashCode(), p2.hashCode());
    }
}
