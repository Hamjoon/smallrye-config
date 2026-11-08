package io.smallrye.config;
import io.smallrye.config.ConfigMapping.NamingStrategy;


import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;

public class ConfigMappingInterfaceTest {

    /* ---------- Dummy Annotations & Converter ---------- */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ConfigMapping {
        ConfigMappingInterface.NamingStrategy namingStrategy() default ConfigMappingInterface.NamingStrategy.KEBAB_CASE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface WithDefault {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface WithDefaults {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
    public @interface WithUnnamedKey {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
    public @interface WithConverter {
        Class<? extends Converter<?>> value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface WithParentName {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface WithName {
        String value();
    }

    public static class StringToIntConverter implements Converter<Integer> {
        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }
    }

    /* ---------- Test Interfaces ---------- */

    public interface SimpleConfig {
        int getInt();

        @WithDefault("100")
        int getIntWithDefault();

        Optional<String> getOptionalString();

        @WithConverter(StringToIntConverter.class)
        int getConvertedInt();

        @WithUnnamedKey("defaultKey")
        Map<String, Integer> getMap();

        @WithParentName
        String getParentNamed();

        // toString method – will be detected as ToStringMethod
        String toString();
    }

    @ConfigMapping(namingStrategy = ConfigMappingInterface.NamingStrategy.VERBATIM)
    public interface GroupConfig {
        String getName();
    }

    public interface SuperConfig {
        String getSuperValue();
    }

    public interface ChildConfig extends SuperConfig, GroupConfig {
        int getChildValue();
    }

    @ConfigMapping
    public interface WithNameConfig {
        @WithName("custom-name")
        int getCustom();
    }

    public interface ListConfig {
        List<Integer> getIntList();

        List<GroupConfig> getGroupList();
    }

    /* ---------- Tests ---------- */

    @Test
    public void testGetConfigurationInterfaceCaching() {
        ConfigMappingInterface cmi1 = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface cmi2 = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Assert.assertNotNull(cmi1);
        Assert.assertSame("ConfigurationInterface should be cached and identical", cmi1, cmi2);
    }

    @Test
    public void testBasicMetadata() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Assert.assertEquals(SimpleConfig.class, cmi.getInterfaceType());
        Assert.assertTrue(cmi.getClassName().contains("SimpleConfig"));
        Assert.assertEquals(cmi.getClassName().replace('.', '/'), cmi.getClassInternalName());
    }

    @Test
    public void testSuperTypesDiscovery() {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface[] supers = child.getSuperTypes();
        // ChildConfig extends SuperConfig and GroupConfig – both should be present
        Set<Class<?>> superNames = new HashSet<>();
        for (ConfigMappingInterface ci : supers) {
            superNames.add(ci.getInterfaceType());
        }
        Assert.assertTrue(superNames.contains(SuperConfig.class));
        Assert.assertTrue(superNames.contains(GroupConfig.class));
    }

    @Test
    public void testPropertiesExcludingToString() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        // toString() should NOT be among regular properties
        for (ConfigMappingInterface.Property p : props) {
            Assert.assertNotEquals("toString", p.getMemberName());
        }
        // The ToStringMethod should be accessible via getToStringMethod()
        ConfigMappingInterface.ToStringMethod tsm = cmi.getToStringMethod();
        Assert.assertNotNull(tsm);
        Assert.assertTrue(tsm.isToStringMethod());
        Assert.assertFalse(tsm.generate());
    }

    @Test
    public void testPropertiesIncludeSuperWhenRequested() {
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface.Property[] direct = child.getProperties();
        ConfigMappingInterface.Property[] all = child.getProperties(true);
        // All should contain at least the number of direct + super properties
        Assert.assertTrue(all.length >= direct.length + 2); // SuperConfig + GroupConfig each have one method
        // Verify that a super property is present
        boolean foundSuper = false;
        for (ConfigMappingInterface.Property p : all) {
            if (p.getMemberName().equals("getSuperValue")) {
                foundSuper = true;
                break;
            }
        }
        Assert.assertTrue("Super property should be present when includeSuper=true", foundSuper);
    }

    @Test
    public void testNamingStrategyDetection() {
        ConfigMappingInterface simple = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Assert.assertFalse(simple.hasNamingStrategy());
        Assert.assertEquals(ConfigMappingInterface.NamingStrategy.KEBAB_CASE, simple.getNamingStrategy());

        ConfigMappingInterface group = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        Assert.assertTrue(group.hasNamingStrategy());
        Assert.assertEquals(ConfigMappingInterface.NamingStrategy.VERBATIM, group.getNamingStrategy());
    }

    @Test
    public void testPrimitivePropertyDetails() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMember(cmi, "getIntWithDefault");
        Assert.assertTrue(p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
        Assert.assertEquals(int.class, pp.getPrimitiveType());
        Assert.assertEquals(Integer.class, pp.getBoxType());
        Assert.assertTrue(pp.hasDefaultValue());
        Assert.assertEquals("100", pp.getDefaultValue());
        // No converter attached
        Assert.assertFalse(pp.hasConvertWith());
    }

    @Test
    public void testConvertedPrimitiveProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMember(cmi, "getConvertedInt");
        Assert.assertTrue(p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
        Assert.assertTrue(pp.hasConvertWith());
        Assert.assertEquals(StringToIntConverter.class, pp.getConvertWith());
    }

    @Test
    public void testOptionalPropertyWrappingLeaf() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMember(cmi, "getOptionalString");
        Assert.assertTrue(p.isOptional());
        ConfigMappingInterface.OptionalProperty op = p.asOptional();
        Assert.assertTrue(op.isLeaf());
        ConfigMappingInterface.LeafProperty leaf = op.asLeaf();
        Assert.assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testMapPropertyKeyUnnamed() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMember(cmi, "getMap");
        Assert.assertTrue(p.isMap());
        ConfigMappingInterface.MapProperty mp = p.asMap();
        Assert.assertTrue(mp.hasKeyUnnamed());
        Assert.assertEquals("defaultKey", mp.getKeyUnnamed());
        Assert.assertEquals(String.class, mp.getKeyRawType());
        Assert.assertEquals(Integer.class, mp.getValueProperty().asLeaf().getValueRawType());
    }

    @Test
    public void testParentPropertyNameFlag() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMember(cmi, "getParentNamed");
        Assert.assertTrue(p.isParentPropertyName());
        Assert.assertEquals("", p.getPropertyName()); // parent name yields empty string
    }

    @Test
    public void testWithNameAnnotation() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(WithNameConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMember(cmi, "getCustom");
        Assert.assertEquals("custom-name", p.getPropertyName());
    }

    @Test
    public void testCollectionOfPrimitives() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMember(cmi, "getIntList");
        Assert.assertTrue(p.isCollection());
        ConfigMappingInterface.CollectionProperty cp = p.asCollection();
        ConfigMappingInterface.LeafProperty leaf = cp.getElement().asLeaf();
        Assert.assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testCollectionOfGroup() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        ConfigMappingInterface.Property p = findPropertyByMember(cmi, "getGroupList");
        Assert.assertTrue(p.isCollection());
        ConfigMappingInterface.CollectionProperty cp = p.asCollection();
        ConfigMappingInterface.GroupProperty gp = cp.getElement().asGroup();
        Assert.assertEquals(GroupConfig.class, gp.getGroupType().getInterfaceType());
    }

    @Test
    public void testNestedGroupsDiscovery() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        List<ConfigMappingInterface> nested = cmi.getNested();
        // ChildConfig has GroupConfig as a super interface, which is a group type
        boolean foundGroup = false;
        for (ConfigMappingInterface nestedCi : nested) {
            if (nestedCi.getInterfaceType().equals(GroupConfig.class)) {
                foundGroup = true;
                break;
            }
        }
        Assert.assertTrue("Nested should contain GroupConfig", foundGroup);
    }

    @Test
    public void testPropertyEqualityAndHashCode() throws NoSuchMethodException {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Method m = SimpleConfig.class.getMethod("getInt");
        ConfigMappingInterface.Property p1 = findPropertyByMember(cmi, "getInt");
        ConfigMappingInterface.Property p2 = findPropertyByMember(cmi, "getInt");
        Assert.assertEquals(p1, p2);
        Assert.assertEquals(p1.hashCode(), p2.hashCode());

        // Different method should not be equal
        ConfigMappingInterface.Property other = findPropertyByMember(cmi, "getConvertedInt");
        Assert.assertNotEquals(p1, other);
    }

    /* ---------- Helper ---------- */
    private ConfigMappingInterface.Property findPropertyByMember(ConfigMappingInterface cmi, String memberName) {
        for (ConfigMappingInterface.Property p : cmi.getProperties(true)) {
            if (p.getMemberName().equals(memberName)) {
                return p;
            }
        }
        Assert.fail("Property with member name '" + memberName + "' not found");
        return null; // unreachable
    }
}
