package io.smallrye.config;
import io.smallrye.config.ConfigMapping;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigMapping;
import org.junit.Test;

/**
 * JUnit 4 tests for {@link ConfigMappings}.
 *
 * The tests are placed in the same package as {@code ConfigMappings} so that
 * package‑private members (e.g. {@code prefix}) can be accessed directly.
 */
public class ConfigMappingsTest {

    /* -------------------------------------------------------------
     *  Tests for the prefix helper methods
     * ------------------------------------------------------------- */

    @Test
    public void testPrefixSimpleConcatenation() {
        assertEquals("a.b", ConfigMappings.prefix("a", "b"));
        assertEquals("a", ConfigMappings.prefix("a", ""));
        assertEquals("b", ConfigMappings.prefix("", "b"));
    }

    @Test
    public void testPrefixArrayNotation() {
        // When the path starts with '[' the prefix is directly concatenated
        assertEquals("a[0].b", ConfigMappings.prefix("a", "[0].b"));
        assertEquals("[0].b", ConfigMappings.prefix("", "[0].b"));
    }

    @Test
    public void testPrefixWithStringBuilderArgument() {
        StringBuilder sb = new StringBuilder("myPrefix");
        String result = ConfigMappings.prefix("myPrefix", "property", sb);
        assertEquals("myPrefix.property", result);
        // The StringBuilder should now contain the full prefixed name
        assertEquals("myPrefix.property", sb.toString());

        // Reset builder and test the empty‑path case
        sb.setLength(0);
        sb.append("onlyPrefix");
        result = ConfigMappings.prefix("onlyPrefix", "", sb);
        assertEquals("onlyPrefix", result);
        assertEquals("onlyPrefix", sb.toString());

        // Empty prefix – the method should return the path unchanged
        sb.setLength(0);
        sb.append("");
        result = ConfigMappings.prefix("", "standalone", sb);
        assertEquals("standalone", result);
        assertEquals("standalone", sb.toString());
    }

    /* -------------------------------------------------------------
     *  Tests for registerConfigMappings / registerConfigProperties
     * ------------------------------------------------------------- */

    @Test
    public void testRegisterConfigMappingsWithEmptySetDoesNothing() throws ConfigValidationException {
        SmallRyeConfig config = new SmallRyeConfig();
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();

        // Should complete without throwing any exception
        ConfigMappings.registerConfigMappings(config, empty);
        ConfigMappings.registerConfigProperties(config, empty);
    }

    /* -------------------------------------------------------------
     *  Tests for ConfigClass static factory validation
     * ------------------------------------------------------------- */

    @Test(expected = IllegalStateException.class)
    public void testConfigClassRejectsClassAnnotatedWithConfigMapping() {
        // According to the validation logic a concrete class annotated with
        // @ConfigMapping must cause an IllegalStateException.
        ConfigMappings.ConfigClass.configClass(ClassWithConfigMapping.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClassRejectsInterfaceAnnotatedWithConfigProperties() {
        // According to the validation logic an interface annotated with
        // @ConfigProperties must cause an IllegalStateException.
        ConfigMappings.ConfigClass.configClass(InterfaceWithConfigProperties.class);
    }

    @Test
    public void testConfigClassAcceptsPlainInterface() {
        // A plain interface (no mapping annotations) should be accepted and
        // produce a ConfigClass whose prefix is the empty string.
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(PlainInterface.class);
        assertNotNull(cc);
        assertEquals(PlainInterface.class, cc.getType());
        assertEquals("", cc.getPrefix());
        // The properties map should be empty for a mapping without any declared properties.
        assertTrue(cc.getProperties().isEmpty());
        assertTrue(cc.getSecrets().isEmpty());
    }

    @Test
    public void testConfigClassAcceptsClassWithConfigPropertiesAndUnconfiguredPrefix() {
        // When a class is annotated with @ConfigProperties and uses the special
        // UNCONFIGURED_PREFIX constant, the resulting ConfigClass should have an
        // empty prefix (the constant is translated to the empty string inside
        // ConfigClass.configClass).
        ConfigMappings.ConfigClass cc = ConfigMappings.ConfigClass.configClass(ClassWithUnconfiguredPrefix.class);
        assertNotNull(cc);
        assertEquals(ClassWithUnconfiguredPrefix.class, cc.getType());
        assertEquals("", cc.getPrefix());
    }

    /* -------------------------------------------------------------
     *  Helper types used in the test suite
     * ------------------------------------------------------------- */

    // -----------------------------------------------------------------
    // 1. A regular class annotated with @ConfigMapping – this must be rejected
    // -----------------------------------------------------------------
    @ConfigMapping(prefix = "illegal")
    public static class ClassWithConfigMapping {
    }

    // -----------------------------------------------------------------
    // 2. An interface annotated with @ConfigProperties – this must be rejected
    // -----------------------------------------------------------------
    @ConfigProperties(prefix = "illegal")
    public interface InterfaceWithConfigProperties {
    }

    // -----------------------------------------------------------------
    // 3. A plain interface without any mapping annotations – this is the
    //    happy‑path for ConfigClass.configClass(klass)
    // -----------------------------------------------------------------
    public interface PlainInterface {
    }

    // -----------------------------------------------------------------
    // 4. A concrete class annotated with @ConfigProperties that uses the
    //    UNCONFIGURED_PREFIX constant – the prefix should be normalized to ""
    // -----------------------------------------------------------------
    @ConfigProperties(prefix = ConfigProperties.UNCONFIGURED_PREFIX)
    public static class ClassWithUnconfiguredPrefix {
    }
}
