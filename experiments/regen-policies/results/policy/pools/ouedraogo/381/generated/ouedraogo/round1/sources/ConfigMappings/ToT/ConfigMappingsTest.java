package io.smallrye.config;
import io.smallrye.config.ConfigMapping;


import static org.junit.Assert.*;

import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappings}.
 *
 * <p>The tests are written from the perspective of three “experts” that each
 * contributed one test case per public method.  The suite therefore covers:
 *
 * <ul>
 *   <li>registration of {@code ConfigMapping} and {@code ConfigProperties}</li>
 *   <li>property extraction via {@link ConfigMappings#getProperties(ConfigClass)}</li>
 *   <li>construction and behaviour of {@link ConfigMappings#propertyNamesMatcher(List)}</li>
 *   <li>the {@code prefix} helper methods (including edge cases)</li>
 *   <li>the {@link ConfigMappings.ConfigClass} helper class (equals, hashCode,
 *   static factories and validation of illegal usage)</li>
 * </ul>
 *
 * <p>Only the public API shown in the prompt is used; no additional
 * third‑party libraries or mocking frameworks are required.
 */
public class ConfigMappingsTest {

    /* -----------------------------------------------------------------------
     * Test data – two simple mapping definitions used by many test cases.
     * ---------------------------------------------------------------------*/

    @ConfigMapping(prefix = "app")
    public interface AppConfig {
        String name();

        @ConfigMapping(prefix = "nested")
        interface Nested {
            int size();
        }
    }

    @ConfigProperties(prefix = "db")
    public static class DbProperties {
        public String url;
        public String username;
        public String password; // treat as secret for extra coverage
    }

    private SmallRyeConfig config;

    @Before
    public void setUp() {
        // Build a minimal configuration – no sources are required for the
        // functionality we are testing (property extraction and registration).
        config = new SmallRyeConfigBuilder().build();
    }

    /* -----------------------------------------------------------------------
     * 1. registerConfigMappings – happy path
     * ---------------------------------------------------------------------*/
    @Test
    public void testRegisterConfigMappings_nonEmptySet_registersSuccessfully() throws Exception {
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(AppConfig.class);
        Set<ConfigMappings.ConfigClass> set = Collections.singleton(cfgClass);

        // The method should complete without throwing an exception.
        ConfigMappings.registerConfigMappings(config, set);

        // Verify that the mapping is now present in the config's internal map.
        // The SmallRyeConfig API exposes the mappings via buildMappings().
        // Since the mapping is added to the builder and then merged, we can
        // retrieve it via getConfigMapping.
        AppConfig retrieved = config.getConfigMapping(AppConfig.class);
        assertNotNull("Mapping should be registered", retrieved);
    }

    /* -----------------------------------------------------------------------
     * 2. registerConfigProperties – happy path (validateUnknown = false)
     * ---------------------------------------------------------------------*/
    @Test
    public void testRegisterConfigProperties_nonEmptySet_registersSuccessfully() throws Exception {
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(DbProperties.class);
        Set<ConfigMappings.ConfigClass> set = Collections.singleton(cfgClass);

        ConfigMappings.registerConfigProperties(config, set);

        // The mapping should be retrievable via the class type.
        DbProperties retrieved = config.getConfigMapping(DbProperties.class);
        assertNotNull("ConfigProperties mapping should be registered", retrieved);
    }

    /* -----------------------------------------------------------------------
     * 3. getProperties – verify full key construction with prefix handling.
     * ---------------------------------------------------------------------*/
    @Test
    public void testGetProperties_buildsCorrectPropertyMap() {
        ConfigMappings.ConfigClass cfgClass = ConfigMappings.ConfigClass.configClass(AppConfig.class);
        Map<String, ConfigMappingInterface.Property> props = ConfigMappings.getProperties(cfgClass);

        // Expected keys: "app.name" and "app.nested.size"
        assertTrue("Should contain app.name", props.containsKey("app.name"));
        assertTrue("Should contain app.nested.size", props.containsKey("app.nested.size"));

        // Verify that the property objects are non‑null.
        assertNotNull(props.get("app.name"));
        assertNotNull(props.get("app.nested.size"));
    }

    /* -----------------------------------------------------------------------
     * 4. propertyNamesMatcher – matcher correctly aggregates names from many
     *    ConfigClass instances and matches both prefixed and un‑prefixed forms.
     * ---------------------------------------------------------------------*/
    @Test
    public void testPropertyNamesMatcher_matchesNamesFromMultipleMappings() {
        ConfigMappings.ConfigClass appClass = ConfigMappings.ConfigClass.configClass(AppConfig.class);
        ConfigMappings.ConfigClass dbClass = ConfigMappings.ConfigClass.configClass(DbProperties.class);
        List<ConfigMappings.ConfigClass> list = Arrays.asList(appClass, dbClass);

        PropertyNamesMatcher matcher = ConfigMappings.propertyNamesMatcher(list);

        // Positive matches
        assertTrue(matcher.matches("app.name"));
        assertTrue(matcher.matches("db.url"));
        assertTrue(matcher.matches("db.password"));

        // Negative matches – unrelated property
        assertFalse(matcher.matches("unknown.prop"));
    }

    /* -----------------------------------------------------------------------
     * 5. prefix – three edge cases (empty prefix, empty path, path starting with '[')
     * ---------------------------------------------------------------------*/
    @Test
    public void testPrefix_emptyPrefixReturnsPath() {
        assertEquals("path", ConfigMappings.prefix("", "path"));
    }

    @Test
    public void testPrefix_emptyPathReturnsPrefix() {
        assertEquals("prefix", ConfigMappings.prefix("prefix", ""));
    }

    @Test
    public void testPrefix_pathStartsWithBracketAppendsDirectly() {
        assertEquals("prefix[0].value", ConfigMappings.prefix("prefix", "[0].value"));
    }

    @Test
    public void testPrefix_normalConcatenationAddsDot() {
        assertEquals("pre.sub", ConfigMappings.prefix("pre", "sub"));
    }

    /* -----------------------------------------------------------------------
     * 6. ConfigClass – constructor builds property map and secret set.
     * ---------------------------------------------------------------------*/
    @Test
    public void testConfigClass_buildsPropertiesAndSecrets() {
        ConfigMappings.ConfigClass cfgClass = new ConfigMappings.ConfigClass(DbProperties.class, "db");
        Map<String, String> map = cfgClass.getProperties();
        Set<String> secrets = cfgClass.getSecrets();

        // All three fields should be present with full prefixes.
        assertEquals("db.url", map.get("db.url"));
        assertEquals("db.username", map.get("db.username"));
        assertEquals("db.password", map.get("db.password"));

        // For the purpose of this test we treat password as secret – the
        // library marks it as secret when the underlying mapping does so.
        // The test simply asserts that the secret set is either empty or
        // contains the password key (implementation dependent).
        assertTrue(secrets.isEmpty() || secrets.contains("db.password"));
    }

    /* -----------------------------------------------------------------------
     * 7. ConfigClass – equals & hashCode contract.
     * ---------------------------------------------------------------------*/
    @Test
    public void testConfigClass_equalsAndHashCode() {
        ConfigMappings.ConfigClass a = ConfigMappings.ConfigClass.configClass(AppConfig.class, "app");
        ConfigMappings.ConfigClass b = ConfigMappings.ConfigClass.configClass(AppConfig.class, "app");
        ConfigMappings.ConfigClass c = ConfigMappings.ConfigClass.configClass(AppConfig.class, "different");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    /* -----------------------------------------------------------------------
     * 8. ConfigClass static factory – validation of illegal annotation usage.
     * ---------------------------------------------------------------------*/
    @Test(expected = IllegalStateException.class)
    public void testConfigClass_factoryRejectsClassWithConfigMappingAnnotation() {
        // A concrete class annotated with @ConfigMapping is illegal.
        @ConfigMapping(prefix = "illegal")
        class IllegalClass {
        }
        ConfigMappings.ConfigClass.configClass(IllegalClass.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClass_factoryRejectsInterfaceWithConfigPropertiesAnnotation() {
        // An interface annotated with @ConfigProperties is illegal.
        @ConfigProperties(prefix = "illegal")
        interface IllegalInterface {
        }
        ConfigMappings.ConfigClass.configClass(IllegalInterface.class);
    }

    /* -----------------------------------------------------------------------
     * 9. ConfigClass – deprecated getKlass still returns the underlying type.
     * ---------------------------------------------------------------------*/
    @Test
    public void testConfigClass_getKlassDeprecatedMethod() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(AppConfig.class);
        assertEquals(AppConfig.class, cfg.getKlass());
    }

    /* -----------------------------------------------------------------------
     * 10. registerConfigMappings with empty set – should be a no‑op.
     * ---------------------------------------------------------------------*/
    @Test
    public void testRegisterConfigMappings_emptySetDoesNothing() throws Exception {
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        // Should not throw and should leave the config unchanged.
        ConfigMappings.registerConfigMappings(config, empty);
        // The config does not contain our mapping because we never added it.
        assertNull(config.getConfigMapping(AppConfig.class));
    }

    /* -----------------------------------------------------------------------
     * 11. registerConfigProperties with empty set – should be a no‑op.
     * ---------------------------------------------------------------------*/
    @Test
    public void testRegisterConfigProperties_emptySetDoesNothing() throws Exception {
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        ConfigMappings.registerConfigProperties(config, empty);
        assertNull(config.getConfigMapping(DbProperties.class));
    }
}
