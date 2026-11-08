package io.smallrye.config;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigMappingInterface.Property;


import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 *
 * The test suite is written entirely with the public API of {@code ConfigMappingInterface}
 * and the minimal set of annotations that the class expects.  All annotations are
 * defined locally in this test file so that the compilation does not depend on the
 * external SmallRye Config library.
 */
public class ConfigMappingInterfaceTest {

    /* --------------------------------------------------------------------- */
    /*  Minimal annotation definitions required by ConfigMappingInterface   */
    /* --------------------------------------------------------------------- */

    // Simulate the ConfigMapping annotation used to mark configuration interfaces
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
    public @interface ConfigMapping {
        NamingStrategy namingStrategy() default NamingStrategy.KEBAB_CASE;
    }

    // Simulate @WithDefault used to provide a default value for a property
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    public @interface WithDefault {
        String value();
    }

    // Simulate @WithDefaults (presence only matters)
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    public @interface WithDefaults {
    }

    // Simulate @WithName used to explicitly name a property
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.PARAMETER,
            java.lang.annotation.ElementType.FIELD})
    public @interface WithName {
        String value();
    }

    // Simulate @WithParentName (used together with @WithName)
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    public @interface WithParentName {
    }

    // Simulate @WithConverter – not used directly in the tests but required for compilation
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.TYPE_USE})
    public @interface WithConverter {
        Class<? extends Converter<?>> value();
    }

    // Simulate @WithUnnamedKey – not used directly in the tests
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.TYPE_USE})
    public @interface WithUnnamedKey {
        String value();
    }

    // --------------------------------------------------------------------- //
    //  Helper interfaces used as configuration interfaces in the tests
    // --------------------------------------------------------------------- //

    @ConfigMapping(namingStrategy = NamingStrategy.SNAKE_CASE)
    public interface BaseConfig {
        String host();
    }

    @ConfigMapping
    public interface SimpleConfig extends BaseConfig {
        int port();

        @WithDefault("42")
        int timeout();

        // toString() is treated specially – it should become a ToStringMethod
        String toString();
    }

    // Nested configuration group
    public interface NestedConfig {
        String nestedValue();
    }

    @ConfigMapping
    public interface ComplexConfig {
        NestedConfig nested();

        Optional<String> optionalString();

        List<Integer> intList();

        Set<String> stringSet();

        Map<String, Long> stringLongMap();

        // method with a parameter – should cause IllegalArgumentException
        // (the test expects the exception when the interface is processed)
        // The method is deliberately *not* used in a successful test.
        // It is only present to provoke the error case.
        // Note: we keep the method *abstract* (no default implementation).
        // The method is never called directly.
        // The exception is thrown during the static processing of the interface.
        // The name of the method is irrelevant.
        // The test will attempt to load this interface via getConfigurationInterface()
        // and verify that the IllegalArgumentException is thrown.
        void illegalMethod(String param);
    }

    // Interface with an empty @WithName – should trigger IllegalArgumentException
    public interface InvalidNameConfig {
        @WithName("")
        int emptyName();
    }

    // --------------------------------------------------------------------- //
    //  JUnit rules
    // --------------------------------------------------------------------- //

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // --------------------------------------------------------------------- //
    //  Tests for static factory method and basic metadata
    // --------------------------------------------------------------------- //

    @Test
    public void testGetConfigurationInterface_basicMetadata() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("ConfigMappingInterface should not be null", cmi);
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());
        assertTrue("Class name should contain the interface name", cmi.getClassName().contains("SimpleConfig"));
        assertEquals(cmi.getClassName().replace('.', '/'), cmi.getClassInternalName());
    }

    @Test
    public void testHasNamingStrategy_and_getNamingStrategy() {
        ConfigMappingInterface baseCmi = ConfigMappingInterface.getConfigurationInterface(BaseConfig.class);
        assertTrue("BaseConfig is annotated with @ConfigMapping", baseCmi.hasNamingStrategy());
        // The annotation on BaseConfig requests SNAKE_CASE
        assertEquals(NamingStrategy.SNAKE_CASE, baseCmi.getNamingStrategy());

        ConfigMappingInterface simpleCmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        // SimpleConfig has @ConfigMapping without explicit strategy – defaults to KEBAB_CASE
        assertTrue(simpleCmi.hasNamingStrategy());
        assertEquals(NamingStrategy.KEBAB_CASE, simpleCmi.getNamingStrategy());
    }

    @Test
    public void testSuperTypes_and_propertiesMerging() {
        ConfigMappingInterface simpleCmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        // SimpleConfig extends BaseConfig, therefore one super type is expected
        ConfigMappingInterface[] supers = simpleCmi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(BaseConfig.class, supers[0].getInterfaceType());

        // Only the non‑toString abstract methods should be returned by getProperties()
        Property[] ownProps = simpleCmi.getProperties();
        // Expected: port(), timeout()
        assertEquals(2, ownProps.length);
        assertTrue(containsMemberName(ownProps, "port"));
        assertTrue(containsMemberName(ownProps, "timeout"));

        // With includeSuper = true we should see host() from BaseConfig as well
        Property[] allProps = simpleCmi.getProperties(true);
        assertEquals(3, allProps.length);
        assertTrue(containsMemberName(allProps, "host"));
        assertTrue(containsMemberName(allProps, "port"));
        assertTrue(containsMemberName(allProps, "timeout"));
    }

    // --------------------------------------------------------------------- //
    //  Tests for specific Property sub‑types
    // --------------------------------------------------------------------- //

    @Test
    public void testPrimitiveProperty_defaultValue() {
        ConfigMappingInterface simpleCmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Property timeoutProp = findPropertyByMember(simpleCmi.getProperties(), "timeout");
        assertNotNull(timeoutProp);
        assertTrue(timeoutProp.isPrimitive());
        PrimitiveProperty pp = timeoutProp.asPrimitive();
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertTrue(pp.hasDefaultValue());
        assertEquals("42", pp.getDefaultValue());
    }

    @Test
    public void testOptionalProperty_andLeafProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ComplexConfig.class);
        Property optionalProp = findPropertyByMember(cmi.getProperties(), "optionalString");
        assertNotNull(optionalProp);
        assertTrue(optionalProp.isOptional());
        OptionalProperty op = optionalProp.asOptional();
        assertTrue(op.isLeaf());
        LeafProperty leaf = op.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ComplexConfig.class);
        Property listProp = findPropertyByMember(cmi.getProperties(), "intList");
        assertNotNull(listProp);
        assertTrue(listProp.isCollection());
        CollectionProperty cp = listProp.asCollection();
        assertEquals(List.class, cp.getCollectionRawType());
        Property element = cp.getElement();
        assertTrue(element.isLeaf());
        LeafProperty leaf = element.asLeaf();
        assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ComplexConfig.class);
        Property mapProp = findPropertyByMember(cmi.getProperties(), "stringLongMap");
        assertNotNull(mapProp);
        assertTrue(mapProp.isMap());
        MapProperty mp = mapProp.asMap();
        assertEquals(String.class, mp.getKeyRawType());
        assertTrue(mp.getValueProperty().isLeaf());
        LeafProperty leaf = mp.getValueProperty().asLeaf();
        assertEquals(Long.class, leaf.getValueRawType());
    }

    @Test
    public void testGroupProperty_andNested() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ComplexConfig.class);
        Property groupProp = findPropertyByMember(cmi.getProperties(), "nested");
        assertNotNull(groupProp);
        assertTrue(groupProp.isGroup());
        GroupProperty gp = groupProp.asGroup();
        ConfigMappingInterface nestedCmi = gp.getGroupType();
        assertEquals(NestedConfig.class, nestedCmi.getInterfaceType());

        // Verify that getNested() returns the nested configuration interface
        List<ConfigMappingInterface> nestedList = cmi.getNested();
        assertEquals(1, nestedList.size());
        assertEquals(NestedConfig.class, nestedList.get(0).getInterfaceType());
    }

    @Test
    public void testToStringMethod_detection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        // The toString() method should be recognised as a ToStringMethod and therefore
        // not appear in the ordinary properties list.
        Property[] props = cmi.getProperties();
        assertFalse("toString() must not be among the regular properties",
                containsMemberName(props, "toString"));

        // The ToStringMethod instance should be present via the getter
        ToStringMethod tsm = cmi.getToStringMethod();
        assertNotNull(tsm);
        assertTrue(tsm.isToStringMethod());
        assertFalse("No default implementation – generate should be false", tsm.generate());
    }

    // --------------------------------------------------------------------- //
    //  Edge‑case and exception handling tests
    // --------------------------------------------------------------------- //

    @Test
    public void testIllegalArgumentException_forMethodWithParameters() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Configuration methods cannot accept parameters");
        ConfigMappingInterface.getConfigurationInterface(ComplexConfig.class);
    }

    @Test
    public void testIllegalArgumentException_forVoidReturnType() {
        // Create a synthetic interface with a void method – we cannot reuse ComplexConfig
        // because it already fails on the parameter case.  Therefore we define a local
        // interface inside the test method.
        @ConfigMapping
        interface VoidMethodConfig {
            void voidMethod();
        }
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Void config methods are not allowed");
        ConfigMappingInterface.getConfigurationInterface(VoidMethodConfig.class);
    }

    @Test
    public void testIllegalArgumentException_forEmptyWithName() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Property name is empty");
        ConfigMappingInterface.getConfigurationInterface(InvalidNameConfig.class);
    }

    // --------------------------------------------------------------------- //
    //  Utility methods for the test suite
    // --------------------------------------------------------------------- //

    private static boolean containsMemberName(Property[] props, String name) {
        for (Property p : props) {
            if (p.getMemberName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static Property findPropertyByMember(Property[] props, String memberName) {
        for (Property p : props) {
            if (p.getMemberName().equals(memberName)) {
                return p;
            }
        }
        return null;
    }
}
