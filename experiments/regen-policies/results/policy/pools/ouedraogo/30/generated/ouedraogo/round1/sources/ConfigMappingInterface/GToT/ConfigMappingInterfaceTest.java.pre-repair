/*
 * Comprehensive JUnit‑4 test suite for {@link io.smallrye.config.ConfigMappingInterface}.
 *
 * The tests cover:
 *   • All public API methods (including static helpers)
 *   • Normal usage scenarios
 *   • Edge‑case handling (null arguments, non‑interface types, Java‑package interfaces)
 *   • Validation of property type detection (primitive, leaf, optional, collection, map)
 *   • Exception paths when illegal methods are present in a configuration interface
 *
 * The tests construct real configuration interfaces using the library’s own factory
 * method {@code ConfigMappingInterface.getConfigurationInterface(Class)} – no mocking
 * or subclassing of library classes is performed.
 */

package io.smallrye.config.test;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

import io.smallrye.config.ConfigMappingInterface;
import io.smallrye.config.ConfigMappingInterface.*;
import io.smallrye.config.ConfigMapping.NamingStrategy;

/**
 * Test class for {@link ConfigMappingInterface}.
 */
public class ConfigMappingInterfaceTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper configuration interfaces used in the test suite                */
    /* ---------------------------------------------------------------------- */

    /** Simple valid configuration interface – no annotations. */
    public interface SimpleConfig {
        int size();                                 // primitive
        String name();                              // leaf
        Optional<String> optionalName();            // optional leaf
        List<String> names();                       // collection of leaf
        Map<String, Integer> scores();              // map of leaf values
    }

    /** Interface that contains an illegal method (has parameters). */
    public interface InvalidParamConfig {
        int valid();                                 // ok
        String illegal(String param);                // illegal – has a parameter
    }

    /** Interface that contains an illegal method (void return type). */
    public interface InvalidVoidConfig {
        void voidMethod();                           // illegal – void return
    }

    /** Interface that extends another configuration interface (group test). */
    public interface ParentConfig {
        String parentProp();
    }

    public interface ChildConfig extends ParentConfig {
        int childInt();
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Basic factory method tests                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterface_NullArgument() {
        try {
            ConfigMappingInterface.getConfigurationInterface(null);
            fail("Expected an exception when passing null to getConfigurationInterface");
        } catch (Exception e) {
            // The library uses Assert.checkNotNullParam which throws IllegalArgumentException
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetConfigurationInterface_NonInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(String.class);
        assertNull("Non‑interface types must result in null", cmi);
    }

    @Test
    public void testGetConfigurationInterface_JavaPackageInterface() {
        // java.lang.Runnable is an interface from the java package – should be ignored
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(Runnable.class);
        assertNull("Java‑package interfaces must result in null", cmi);
    }

    @Test
    public void testGetConfigurationInterface_Caching() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(first);
        assertSame("The same instance should be returned from the cache", first, second);
    }

    /* ---------------------------------------------------------------------- */
    /*  2. Public instance methods                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInstanceMetadata() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(cmi);

        // getInterfaceType()
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());

        // getClassName() – must contain the interface name and be a valid identifier
        String className = cmi.getClassName();
        assertNotNull(className);
        assertTrue(className.contains(SimpleConfig.class.getName()));

        // getClassInternalName() – dot replaced by slash
        String internalName = cmi.getClassInternalName();
        assertEquals(className.replace('.', '/'), internalName);
    }

    @Test
    public void testNamingStrategyMethods() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        // No @ConfigMapping annotation → no naming strategy defined
        assertFalse("hasNamingStrategy should be false for un‑annotated interface", cmi.hasNamingStrategy());

        // Default strategy is KEBAB_CASE
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
    }

    @Test
    public void testGetProperties_CountAndTypes() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Property[] props = cmi.getProperties();

        // Expected properties: size, name, optionalName, names, scores
        assertEquals("Exactly five properties are expected", 5, props.length);

        // Helper maps for easier lookup
        Map<String, Property> byName = new HashMap<>();
        for (Property p : props) {
            byName.put(p.getMemberName(), p);
        }

        // Primitive property
        Property sizeProp = byName.get("size");
        assertTrue(sizeProp instanceof PrimitiveProperty);
        PrimitiveProperty pp = (PrimitiveProperty) sizeProp;
        assertEquals(int.class, pp.getPrimitiveType());

        // Leaf property
        Property nameProp = byName.get("name");
        assertTrue(nameProp instanceof LeafProperty);
        LeafProperty lp = (LeafProperty) nameProp;
        assertEquals(String.class, lp.getValueRawType());

        // Optional property (wrapping a leaf)
        Property optProp = byName.get("optionalName");
        assertTrue(optProp instanceof OptionalProperty);
        OptionalProperty op = (OptionalProperty) optProp;
        assertTrue(op.getNestedProperty() instanceof LeafProperty);
        LeafProperty nestedLeaf = op.getNestedProperty().asLeaf();
        assertEquals(String.class, nestedLeaf.getValueRawType());

        // Collection property (List<String>)
        Property namesProp = byName.get("names");
        assertTrue(namesProp instanceof CollectionProperty);
        CollectionProperty cp = (CollectionProperty) namesProp;
        assertEquals(List.class, cp.getCollectionRawType());
        assertTrue(cp.getElement() instanceof LeafProperty);
        LeafProperty elemLeaf = cp.getElement().asLeaf();
        assertEquals(String.class, elemLeaf.getValueRawType());

        // Map property (Map<String,Integer>)
        Property scoresProp = byName.get("scores");
        assertTrue(scoresProp instanceof MapProperty);
        MapProperty mp = (MapProperty) scoresProp;
        assertEquals(String.class, mp.getKeyRawType());
        assertTrue(mp.getValueProperty() instanceof LeafProperty);
        LeafProperty valueLeaf = mp.getValueProperty().asLeaf();
        assertEquals(Integer.class, valueLeaf.getValueRawType());
    }

    @Test
    public void testGetNamesStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();

        // The top‑level key must be the fully qualified interface name
        String topKey = SimpleConfig.class.getName();
        assertTrue(names.containsKey(topKey));

        Map<String, Set<String>> groups = names.get(topKey);
        // The root path is the empty string – all top‑level properties belong here
        assertTrue(groups.containsKey(""));
        Set<String> rootProps = groups.get("");

        // Verify that all property names appear in the root set (using default kebab‑case)
        Set<String> expected = new HashSet<>(Arrays.asList(
                "size", "name", "optional-name", "names", "scores"));
        assertEquals(expected, rootProps);
    }

    @Test
    public void testGetNested_NoGroups() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertTrue("SimpleConfig has no nested groups", nested.isEmpty());
    }

    @Test
    public void testGetClassBytes_NotEmpty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        byte[] bytes = cmi.getClassBytes();
        assertNotNull("Class bytes must not be null", bytes);
        assertTrue("Generated class bytes should have positive length", bytes.length > 0);
    }

    /* ---------------------------------------------------------------------- */
    /*  3. Error handling when configuration interface contains illegal methods */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInvalidMethod_WithParameter() {
        try {
            ConfigMappingInterface.getConfigurationInterface(InvalidParamConfig.class);
            fail("Expected IllegalArgumentException for method with parameters");
        } catch (IllegalArgumentException e) {
            // Expected path
        }
    }

    @Test
    public void testInvalidMethod_VoidReturn() {
        try {
            ConfigMappingInterface.getConfigurationInterface(InvalidVoidConfig.class);
            fail("Expected IllegalArgumentException for void return type");
        } catch (IllegalArgumentException e) {
            // Expected path
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  4. Inheritance (super‑type) handling                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSuperTypes_InheritedProperties() {
        ConfigMappingInterface childCmi = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        assertNotNull(childCmi);
        Property[] props = childCmi.getProperties();

        // ChildConfig inherits parentProp from ParentConfig, plus childInt
        Set<String> memberNames = new HashSet<>();
        for (Property p : props) {
            memberNames.add(p.getMemberName());
        }

        assertTrue(memberNames.contains("parentProp"));
        assertTrue(memberNames.contains("childInt"));
        assertEquals(2, memberNames.size());

        // Verify that super type information is present
        ConfigMappingInterface[] supers = childCmi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(ParentConfig.class, supers[0].getInterfaceType());
    }

    /* ---------------------------------------------------------------------- */
    /*  5. ToStringMethod handling – ensure it is ignored in property list    */
    /* ---------------------------------------------------------------------- */

    public interface ToStringConfig {
        String toString(); // should be treated as a ToStringMethod, not a regular property
        String value();
    }

    @Test
    public void testToStringMethod_IsIgnored() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        Property[] props = cmi.getProperties();

        // Only the 'value' property should be present
        assertEquals(1, props.length);
        assertEquals("value", props[0].getMemberName());

        // The internal ToStringMethod instance should be stored in the ConfigMappingInterface
        assertTrue(cmi.getToStringMethod().isToStringMethod());
        assertFalse(cmi.getToStringMethod().generate()); // default is NONE → generate == false
    }

    /* ---------------------------------------------------------------------- */
    /*  6. Property equality – ensure equals/hashCode contract works          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPropertyEqualsAndHashCode() throws NoSuchMethodException {
        // Obtain two Property instances referring to the same method
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Property[] props = cmi.getProperties();

        Method sizeMethod = SimpleConfig.class.getMethod("size");
        Property sizeProp1 = null, sizeProp2 = null;
        for (Property p : props) {
            if (p.getMethod().equals(sizeMethod)) {
                sizeProp1 = p;
                break;
            }
        }
        assertNotNull(sizeProp1);

        // Re‑create a PrimitiveProperty manually for the same method to test equality
        PrimitiveProperty manual = new PrimitiveProperty(sizeMethod, null, int.class, null, null);
        sizeProp2 = manual;

        // Equality must hold (method hierarchy check also applies)
        assertEquals(sizeProp1, sizeProp2);
        assertEquals(sizeProp1.hashCode(), sizeProp2.hashCode());
    }

    /* ---------------------------------------------------------------------- */
    /*  7. Converter validation – using a simple custom converter            */
    /* ---------------------------------------------------------------------- */

    public static class UpperCaseConverter implements Converter<String> {
        @Override
        public String convert(String value) {
            return value == null ? null : value.toUpperCase(Locale.ROOT);
        }
    }

    public interface ConverterConfig {
        @io.smallrye.config.WithConverter(UpperCaseConverter.class)
        String greeting();
    }

    @Test
    public void testPropertyWithConverter() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ConverterConfig.class);
        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        Property prop = props[0];
        assertTrue(prop instanceof LeafProperty);
        LeafProperty leaf = (LeafProperty) prop;
        assertEquals(UpperCaseConverter.class, leaf.getConvertWith());
        assertTrue(leaf.hasConvertWith());
    }
}
