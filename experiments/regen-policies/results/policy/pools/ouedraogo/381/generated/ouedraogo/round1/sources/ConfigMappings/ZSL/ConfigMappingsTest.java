package io.smallrye.config;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappingInterface.Property;


import static org.junit.Assert.*;

import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappings}.
 *
 * The tests focus on the public static utility methods and the nested {@link ConfigMappings.ConfigClass}
 * implementation. They verify correct handling of prefix concatenation, equality, hash‑code contracts,
 * factory methods, and the behaviour of the matcher and property extraction utilities.
 *
 * No external mocking frameworks are used – all objects are created via their public constructors
 * or factory methods as required by the assignment constraints.
 */
public class ConfigMappingsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper interfaces / classes used as mapping definitions                */
    /* ---------------------------------------------------------------------- */

    @ConfigMapping(prefix = "my")
    public interface SimpleMapping {
        // A single property – the method name becomes the property name.
        String foo();
    }

    @ConfigProperties(prefix = "props")
    public static class SimpleProperties {
        // No fields are required – the class is only used to test ConfigClass handling.
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for the private static prefix utility methods                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPrefixBothEmpty() {
        assertEquals("", ConfigMappings.prefix("", ""));
    }

    @Test
    public void testPrefixOnlyPrefix() {
        assertEquals("my.prefix", ConfigMappings.prefix("my.prefix", ""));
    }

    @Test
    public void testPrefixOnlyPath() {
        assertEquals("value", ConfigMappings.prefix("", "value"));
    }

    @Test
    public void testPrefixNormalConcatenation() {
        assertEquals("my.prefix.value", ConfigMappings.prefix("my.prefix", "value"));
    }

    @Test
    public void testPrefixArrayStylePath() {
        assertEquals("my.prefix[0]", ConfigMappings.prefix("my.prefix", "[0]"));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for ConfigClass construction and its public contract             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConfigClassFromInterface() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(SimpleMapping.class);
        assertNotNull(cfg);
        assertEquals(SimpleMapping.class, cfg.getType());
        assertEquals("my", cfg.getPrefix());

        // The generated property name should be the prefix plus the method name.
        Map<String, String> props = cfg.getProperties();
        assertTrue("Properties should contain the prefixed method name", props.containsKey("my.foo"));
        assertEquals("foo", props.get("my.foo"));
    }

    @Test
    public void testConfigClassFromClass() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(SimpleProperties.class);
        assertNotNull(cfg);
        assertEquals(SimpleProperties.class, cfg.getType());
        assertEquals("props", cfg.getPrefix());

        // No properties are declared in the class, therefore the map should be empty.
        assertTrue(cfg.getProperties().isEmpty());
    }

    @Test
    public void testConfigClassEqualityAndHashCode() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(SimpleMapping.class, "my");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(SimpleMapping.class, "my");
        ConfigMappings.ConfigClass c = ConfigMappings.ConfigClass.configClass(SimpleMapping.class, "other");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a.hashCode(), c.hashCode());
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClassRejectsInvalidAnnotationCombination_InterfaceWithConfigProperties() {
        @ConfigProperties(prefix = "invalid")
        interface BadInterface {
        }
        ConfigMappings.ConfigClass.configClass(BadInterface.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClassRejectsInvalidAnnotationCombination_ClassWithConfigMapping() {
        @ConfigMapping(prefix = "invalid")
        class BadClass {
        }
        ConfigMappings.ConfigClass.configClass(BadClass.class);
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for getProperties – ensures the prefix is correctly applied      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetPropertiesProducesPrefixedKeys() {
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(SimpleMapping.class);
        Map<String, Property> result = ConfigMappings.getProperties(cfgClass);

        // The only property defined by SimpleMapping is "foo", so the full key must be "my.foo".
        assertEquals(1, result.size());
        assertTrue(result.containsKey("my.foo"));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for propertyNamesMatcher – matcher must recognise the generated  */
    /* property names from the supplied ConfigClass list.                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPropertyNamesMatcherMatchesPrefixedProperty() {
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(SimpleMapping.class);
        List<ConfigMappings.ConfigClass> list = Collections.singletonList(cfgClass);
        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(list);

        assertTrue("Matcher should accept the full prefixed property name", matcher.matches("my.foo"));
        assertFalse("Matcher should reject an unknown property", matcher.matches("my.bar"));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for the registration convenience methods – they should be safe   */
    /* when called with an empty set (no mappings to register).               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNotThrow() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigClass> empty = Collections.emptySet();
        ConfigMappings.registerConfigMappings(config, empty);
        // No exception means success.
    }

    @Test
    public void testRegisterConfigPropertiesWithEmptySetDoesNotThrow() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigClass> empty = Collections.emptySet();
        ConfigMappings.registerConfigProperties(config, empty);
        // No exception means success.
    }

    /* ---------------------------------------------------------------------- */
    /* Additional sanity checks for the private mapConfiguration method via   */
    /* the public registration methods with a single mapping.                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsAppliesSingleMapping() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(SimpleMapping.class);
        Set<ConfigClass> set = Collections.singleton(cfgClass);

        // The call should succeed; after registration the config's mapping map must contain the class.
        ConfigMappings.registerConfigMappings(config, set);
        assertTrue("Config should contain the mapping after registration",
                config.getMappings().containsKey(SimpleMapping.class));
    }

    @Test
    public void testRegisterConfigPropertiesAppliesSingleMapping() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(SimpleProperties.class);
        Set<ConfigClass> set = Collections.singleton(cfgClass);

        ConfigMappings.registerConfigProperties(config, set);
        assertTrue("Config should contain the properties mapping after registration",
                config.getMappings().containsKey(SimpleProperties.class));
    }
}
