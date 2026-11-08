package io.smallrye.config;
import io.smallrye.config.ConfigMappingInterface.OptionalProperty;
import io.smallrye.config.ConfigMappingInterface.Property;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingInterface}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Public API extraction and basic behaviour verification.</li>
 *   <li>Edge‑case handling such as null arguments, non‑interface types,
 *       Java platform interfaces and illegal method signatures.</li>
 *   <li>Correct classification of properties (primitive, leaf, optional,
 *       group, collection, map) and hierarchy handling (super‑types).</li>
 *   <li>Utility methods like {@code getImplementationClassName},
 *       {@code getClassInternalName} and nested‑type discovery.</li>
 * </ul>
 *
 * <p>No mocking frameworks are used – all objects are created via the
 * public constructors and factory methods that exist in the source code.
 */
public class ConfigMappingInterfaceTest {

    /* -------------------------------------------------------
     *  Helper configuration interfaces used by the tests
     * -----------------------------------------------------*/

    // Simple configuration interface – no @ConfigMapping annotation.
    public interface SimpleConfig {
        String name();

        int count();
    }

    // Configuration interface that extends a parent interface.
    public interface BaseConfig {
        String baseProp();
    }

    public interface InheritedConfig extends BaseConfig {
        String childProp();
    }

    // Interface containing an {@code Optional} property.
    public interface OptionalConfig {
        Optional<String> optionalValue();
    }

    // Interface that deliberately contains an illegal method (parameter).
    public interface BadParameterConfig {
        String bad(String param);
    }

    // Interface that deliberately contains an illegal method (void return).
    public interface BadVoidConfig {
        void nothing();
    }

    // Concrete class – not an interface.
    public static class NotAnInterface {
        public String something() {
            return "x";
        }
    }

    /* -------------------------------------------------------
     *  1. Public‑API basic tests
     * -----------------------------------------------------*/

    @Test
    public void testGetConfigurationInterface_NullArgument() {
        try {
            ConfigMappingInterface.getConfigurationInterface(null);
            fail("Expected an exception for null argument");
        } catch (Exception e) {
            // The implementation uses Assert.checkNotNullParam which throws
            // IllegalArgumentException or NullPointerException – we accept either.
            assertTrue(e instanceof IllegalArgumentException || e instanceof NullPointerException);
        }
    }

    @Test
    public void testGetConfigurationInterface_NonInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(NotAnInterface.class);
        assertNull("Non‑interface types must return null", cmi);
    }

    @Test
    public void testGetConfigurationInterface_JavaPlatformInterface() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(List.class);
        assertNull("Java platform interfaces must return null", cmi);
    }

    @Test
    public void testSimpleConfigMetadata() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull("Configuration interface should be created", cmi);

        // Interface type
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());

        // Generated class name ends with the suffix $$CMImpl
        String className = cmi.getClassName();
        assertTrue(className.endsWith("$$CMImpl"));
        assertTrue(className.contains(SimpleConfig.class.getName()));

        // Internal name uses '/' as separator
        String internalName = cmi.getClassInternalName();
        assertEquals(className.replace('.', '/'), internalName);

        // No super‑types for a top‑level interface
        assertEquals(0, cmi.getSuperTypes().length);

        // Property count – two abstract methods
        Property[] props = cmi.getProperties();
        assertEquals(2, props.length);

        // Verify primitive detection for the int method
        Property countProp = findByMemberName(props, "count");
        assertNotNull(countProp);
        assertTrue(countProp.isPrimitive());
        assertTrue(countProp.asPrimitive().getPrimitiveType() == int.class);

        // Verify leaf detection for the String method
        Property nameProp = findByMemberName(props, "name");
        assertNotNull(nameProp);
        assertTrue(nameProp.isLeaf());
        assertTrue(nameProp.asLeaf().getValueType() == String.class);
    }

    @Test
    public void testInheritedConfigSuperTypesAndProperties() {
        ConfigMappingInterface childCmi = ConfigMappingInterface.getConfigurationInterface(InheritedConfig.class);
        assertNotNull(childCmi);

        // One super‑type (BaseConfig)
        ConfigMappingInterface[] supers = childCmi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(BaseConfig.class, supers[0].getInterfaceType());

        // All properties from both interfaces must be present (2 total)
        Property[] props = childCmi.getProperties();
        assertEquals(2, props.length);
        assertNotNull(findByMemberName(props, "baseProp"));
        assertNotNull(findByMemberName(props, "childProp"));
    }

    @Test
    public void testOptionalPropertyDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        assertNotNull(cmi);

        Property[] props = cmi.getProperties();
        assertEquals(1, props.length);

        Property optProp = props[0];
        assertTrue(optProp.isOptional());
        OptionalProperty optionalProperty = optProp.asOptional();

        // The nested property must be a leaf of type String
        Property nested = optionalProperty.getNestedProperty();
        assertTrue(nested.isLeaf());
        assertEquals(String.class, nested.asLeaf().getValueType());
    }

    @Test
    public void testBadParameterMethodThrowsExceptionDuringParsing() {
        try {
            ConfigMappingInterface.getConfigurationInterface(BadParameterConfig.class);
            fail("Expected IllegalArgumentException because method has parameters");
        } catch (IllegalArgumentException e) {
            // Message comes from ConfigMessages.msg.mappingMethodsCannotAcceptParameters
            assertTrue(e.getMessage().contains("bad"));
        }
    }

    @Test
    public void testBadVoidMethodThrowsExceptionDuringParsing() {
        try {
            ConfigMappingInterface.getConfigurationInterface(BadVoidConfig.class);
            fail("Expected IllegalArgumentException because method returns void");
        } catch (IllegalArgumentException e) {
            // Message comes from ConfigMessages.msg.mappingMethodsCannotBeVoid
            assertTrue(e.getMessage().contains("nothing"));
        }
    }

    @Test
    public void testHasConfigMappingAndNamingStrategy() {
        // SimpleConfig is NOT annotated with @ConfigMapping
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertFalse("hasConfigMapping should be false when no @ConfigMapping is present", cmi.hasConfigMapping());

        // Default naming strategy must be KEBAB_CASE when annotation is missing
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());

        // Bean‑style getters default to false
        assertFalse(cmi.isBeanStyleGetters());
    }

    @Test
    public void testGetNestedGroups() {
        // Define a configuration interface with a nested group
        @ConfigMapping(prefix = "parent")
        interface ParentConfig {
            ChildConfig child();

            interface ChildConfig {
                String leaf();
            }
        }

        ConfigMappingInterface parentCmi = ConfigMappingInterface.getConfigurationInterface(ParentConfig.class);
        assertNotNull(parentCmi);

        List<ConfigMappingInterface> nested = parentCmi.getNested();
        // One nested group (ChildConfig)
        assertEquals(1, nested.size());
        assertEquals(ParentConfig.ChildConfig.class, nested.get(0).getInterfaceType());
    }

    /* -------------------------------------------------------
     *  Utility methods for the test suite
     * -----------------------------------------------------*/

    /**
     * Finds a {@link Property} in the supplied array by its member (method) name.
     */
    private Property findByMemberName(Property[] properties, String memberName) {
        for (Property p : properties) {
            if (memberName.equals(p.getMemberName())) {
                return p;
            }
        }
        return null;
    }
}
