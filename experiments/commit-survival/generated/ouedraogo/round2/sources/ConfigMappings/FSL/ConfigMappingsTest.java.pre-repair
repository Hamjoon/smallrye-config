package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ConfigMappingsTest {

    /* ----------------------------------------------------------------------
     * Tests for ConfigMappings.getPrefix(...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetPrefixReturnsEmptyWhenNoAnnotation() {
        // No @ConfigMapping annotation is present on this class.
        class NoAnnotationClass {}
        String prefix = ConfigMappings.getPrefix(NoAnnotationClass.class);
        assertEquals("Expected empty prefix when no @ConfigMapping annotation is present", "", prefix);
    }

    /* ----------------------------------------------------------------------
     * Tests for ConfigMappings.ConfigClassWithPrefix
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorAndGetters() {
        Class<?> klass = String.class;
        String prefix = "my.prefix";

        ConfigMappings.ConfigClassWithPrefix ccp = new ConfigMappings.ConfigClassWithPrefix(klass, prefix);

        assertSame("Getter should return the same class instance passed to constructor", klass, ccp.getKlass());
        assertEquals("Getter should return the same prefix passed to constructor", prefix, ccp.getPrefix());
    }

    @Test
    public void testFactoryMethodWithPrefix() {
        Class<?> klass = Integer.class;
        String prefix = "int.prefix";

        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(klass, prefix);

        assertNotNull("Factory method should not return null", ccp);
        assertSame("Factory method should store the correct class", klass, ccp.getKlass());
        assertEquals("Factory method should store the correct prefix", prefix, ccp.getPrefix());
    }

    @Test
    public void testFactoryMethodWithoutPrefixUsesAnnotationOrEmpty() {
        // The class does not have a @ConfigMapping annotation, therefore getPrefix() will return "".
        class DummyClass {}

        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(DummyClass.class);
        assertNotNull(ccp);
        assertSame(DummyClass.class, ccp.getKlass());
        assertEquals("", ccp.getPrefix());
    }

    @Test
    public void testEqualsReflexive() {
        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");
        assertTrue("Object should be equal to itself (reflexive)", ccp.equals(ccp));
    }

    @Test
    public void testEqualsSymmetricAndTransitive() {
        ConfigMappings.ConfigClassWithPrefix a = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");
        ConfigMappings.ConfigClassWithPrefix b = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");
        ConfigMappings.ConfigClassWithPrefix c = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");

        // Symmetric
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));

        // Transitive
        assertTrue(a.equals(b));
        assertTrue(b.equals(c));
        assertTrue(a.equals(c));
    }

    @Test
    public void testEqualsDifferentClassOrPrefix() {
        ConfigMappings.ConfigClassWithPrefix base = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");

        ConfigMappings.ConfigClassWithPrefix differentKlass = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(Integer.class, "p");
        ConfigMappings.ConfigClassWithPrefix differentPrefix = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "different");

        assertFalse("Objects with different klass should not be equal", base.equals(differentKlass));
        assertFalse("Objects with different prefix should not be equal", base.equals(differentPrefix));
    }

    @Test
    public void testEqualsNullAndOtherType() {
        ConfigMappings.ConfigClassWithPrefix ccp = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");
        assertFalse("Object should not be equal to null", ccp.equals(null));
        assertFalse("Object should not be equal to an instance of a different type", ccp.equals("some string"));
    }

    @Test
    public void testHashCodeConsistencyWithEquals() {
        ConfigMappings.ConfigClassWithPrefix a = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");
        ConfigMappings.ConfigClassWithPrefix b = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");

        assertEquals("Equal objects must have same hashCode", a.hashCode(), b.hashCode());

        ConfigMappings.ConfigClassWithPrefix c = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(Integer.class, "p");
        // Not guaranteed to be different, but very likely; we just check that unequal objects may have different hash codes.
        if (!a.equals(c)) {
            assertNotEquals("Unequal objects should preferably have different hashCodes", a.hashCode(), c.hashCode());
        }
    }

    @Test
    public void testHashSetBehavior() {
        Set<ConfigMappings.ConfigClassWithPrefix> set = new HashSet<>();

        ConfigMappings.ConfigClassWithPrefix a = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");
        ConfigMappings.ConfigClassWithPrefix b = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(String.class, "p");
        ConfigMappings.ConfigClassWithPrefix c = ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix(Integer.class, "p");

        assertTrue(set.add(a));
        // Adding an equal instance should not increase set size
        assertFalse(set.add(b));
        assertTrue(set.add(c));

        assertEquals("Set should contain exactly two distinct elements", 2, set.size());
        assertTrue(set.contains(a));
        assertTrue(set.contains(c));
    }
}
