package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 test suite for {@link ConfigMappings}.
 *
 * <p>All public methods of {@code ConfigMappings} are exercised:
 *
 * <ul>
 *   <li>{@link ConfigMappings#registerConfigMappings(SmallRyeConfig, Set)}</li>
 *   <li>{@link ConfigMappings#registerConfigProperties(SmallRyeConfig, Set)}</li>
 *   <li>{@link ConfigMappings#getProperties(ConfigClassWithPrefix)}</li>
 *   <li>{@link ConfigMappings#getNames(ConfigClassWithPrefix)}</li>
 *   <li>{@link ConfigMappings#getKeys(ConfigClassWithPrefix)}</li>
 *   <li>{@link ConfigMappings#getDefaults(ConfigClassWithPrefix)}</li>
 *   <li>{@link ConfigMappings#mappedProperties(ConfigClassWithPrefix, Set)}</li>
 *   <li>package‑private {@link ConfigMappings#getPrefix(Class)}</li>
 *   <li>All public members of the static inner class {@code ConfigClassWithPrefix}</li>
 * </ul>
 *
 * <p>Both normal use cases and identified edge / error scenarios are covered.
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------------- */
    /* Helper interfaces used only for @ConfigMapping annotation checks    */
    /* --------------------------------------------------------------------- */

    @io.smallrye.config.ConfigMapping(prefix = "my.prefix")
    interface AnnotatedConfig {
        // No methods required for the tests – we only need the annotation.
    }

    interface NoAnnotationConfig {
        // Plain interface, no @ConfigMapping.
    }

    /* --------------------------------------------------------------------- */
    /* 1. Tests for ConfigClassWithPrefix (constructor, getters, equals,   */
    /*    hashCode and static factories)                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void configClassWithPrefix_constructorAndGetters() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, "my.prefix");

        assertEquals(AnnotatedConfig.class, wrapper.getKlass());
        assertEquals("my.prefix", wrapper.getPrefix());
    }

    @Test
    public void configClassWithPrefix_equalsAndHashCode() {
        ConfigMappings.ConfigClassWithPrefix a =
                new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, "p1");
        ConfigMappings.ConfigClassWithPrefix b =
                new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, "p1");
        ConfigMappings.ConfigClassWithPrefix c =
                new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, "p2");

        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        assertFalse(a.equals(c));
        assertNotEquals(a.hashCode(), c.hashCode());

        // comparison with a different type must be false
        assertFalse(a.equals("some string"));
    }

    @Test
    public void configClassWithPrefix_staticFactoryMethods() {
        ConfigMappings.ConfigClassWithPrefix viaCtor =
                new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, "my.prefix");
        ConfigMappings.ConfigClassWithPrefix viaFactory1 =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class, "my.prefix");
        ConfigMappings.ConfigClassWithPrefix viaFactory2 =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class); // uses annotation

        assertEquals(viaCtor, viaFactory1);
        assertEquals(viaCtor, viaFactory2);
    }

    @Test(expected = NullPointerException.class)
    public void configClassWithPrefix_nullKlass_throwsNPE() {
        new ConfigMappings.ConfigClassWithPrefix(null, "prefix");
    }

    @Test(expected = NullPointerException.class)
    public void configClassWithPrefix_nullPrefix_throwsNPE() {
        new ConfigMappings.ConfigClassWithPrefix(AnnotatedConfig.class, null);
    }

    /* --------------------------------------------------------------------- */
    /* 2. Tests for the static package‑private getPrefix(Class<?>) method   */
    /* --------------------------------------------------------------------- */

    @Test
    public void getPrefix_annotatedClass_returnsAnnotationValue() {
        String prefix = ConfigMappings.getPrefix(AnnotatedConfig.class);
        assertEquals("my.prefix", prefix);
    }

    @Test
    public void getPrefix_nonAnnotatedClass_returnsEmptyString() {
        String prefix = ConfigMappings.getPrefix(NoAnnotationConfig.class);
        assertEquals("", prefix);
    }

    @Test(expected = NullPointerException.class)
    public void getPrefix_nullArgument_throwsNPE() {
        ConfigMappings.getPrefix(null);
    }

    /* --------------------------------------------------------------------- */
    /* 3. registerConfigMappings / registerConfigProperties – happy path    */
    /* --------------------------------------------------------------------- */

    @Test
    public void registerConfigMappings_emptySet_doesNotThrow() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.registerConfigMappings(config, Collections.emptySet());
        // No exception → pass
    }

    @Test
    public void registerConfigProperties_emptySet_doesNotThrow() throws Exception {
        SmallRyeConfig config = new SmallRyeConfig();
        ConfigMappings.registerConfigProperties(config, Collections.emptySet());
        // No exception → pass
    }

    /* --------------------------------------------------------------------- */
    /* 4. registerConfigMappings / registerConfigProperties – null checks   */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void registerConfigMappings_nullConfig_throwsNPE() throws Exception {
        ConfigMappings.registerConfigMappings(null, Collections.emptySet());
    }

    @Test(expected = NullPointerException.class)
    public void registerConfigMappings_nullSet_throwsNPE() throws Exception {
        ConfigMappings.registerConfigMappings(new SmallRyeConfig(), null);
    }

    @Test(expected = NullPointerException.class)
    public void registerConfigProperties_nullConfig_throwsNPE() throws Exception {
        ConfigMappings.registerConfigProperties(null, Collections.emptySet());
    }

    @Test(expected = NullPointerException.class)
    public void registerConfigProperties_nullSet_throwsNPE() throws Exception {
        ConfigMappings.registerConfigProperties(new SmallRyeConfig(), null);
    }

    /* --------------------------------------------------------------------- */
    /* 5. getProperties – basic (empty) behaviour                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getProperties_nonAnnotatedClass_returnsEmptyMap() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NoAnnotationConfig.class);
        Map<Class<?>, Map<String, Map<String, ConfigMappingInterface.Property>>> result =
                ConfigMappings.getProperties(wrapper);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void getProperties_nullArgument_throwsNPE() {
        ConfigMappings.getProperties(null);
    }

    /* --------------------------------------------------------------------- */
    /* 6. getNames – derived from getProperties (empty case)               */
    /* --------------------------------------------------------------------- */

    @Test
    public void getNames_nonAnnotatedClass_returnsEmptyMap() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NoAnnotationConfig.class);
        Map<String, Map<String, Set<String>>> result = ConfigMappings.getNames(wrapper);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 7. getKeys – empty case                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void getKeys_nonAnnotatedClass_returnsEmptySet() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NoAnnotationConfig.class);
        Set<String> keys = ConfigMappings.getKeys(wrapper);
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 8. getDefaults – empty case                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void getDefaults_nonAnnotatedClass_returnsEmptyMap() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NoAnnotationConfig.class);
        Map<String, String> defaults = ConfigMappings.getDefaults(wrapper);
        assertNotNull(defaults);
        assertTrue(defaults.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 9. mappedProperties – empty filter set                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void mappedProperties_emptyInputSet_returnsEmptySet() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NoAnnotationConfig.class);
        Set<String> result = ConfigMappings.mappedProperties(wrapper, Collections.emptySet());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void mappedProperties_nullPropertiesSet_throwsNPE() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(NoAnnotationConfig.class);
        ConfigMappings.mappedProperties(wrapper, null);
    }

    @Test(expected = NullPointerException.class)
    public void mappedProperties_nullConfigClass_throwsNPE() {
        ConfigMappings.mappedProperties(null, Collections.emptySet());
    }

    /* --------------------------------------------------------------------- */
    /* 10. Additional sanity checks – ensure that the public API never      */
    /*     returns null (except when the contract explicitly allows it).   */
    /* --------------------------------------------------------------------- */

    @Test
    public void getProperties_alwaysReturnsNonNullMap() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class);
        Map<?, ?> map = ConfigMappings.getProperties(wrapper);
        assertNotNull(map);
    }

    @Test
    public void getNames_alwaysReturnsNonNullMap() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class);
        Map<?, ?> map = ConfigMappings.getNames(wrapper);
        assertNotNull(map);
    }

    @Test
    public void getKeys_alwaysReturnsNonNullSet() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class);
        Set<?> set = ConfigMappings.getKeys(wrapper);
        assertNotNull(set);
    }

    @Test
    public void getDefaults_alwaysReturnsNonNullMap() {
        ConfigMappings.ConfigClassWithPrefix wrapper =
                ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(AnnotatedConfig.class);
        Map<?, ?> map = ConfigMappings.getDefaults(wrapper);
        assertNotNull(map);
    }
}
