/*
 * Comprehensive JUnit‑4 test suite for {@code io.smallrye.config.ConfigMappingLoader}.
 *
 * The tests cover:
 *   1. Normal behaviour of the public API.
 *   2. Edge cases such as null arguments.
 *   3. Error handling when the supplied type is not a configuration mapping.
 *
 * No external mocking frameworks are used – all objects are created through the
 * real constructors/factory methods that are part of the library.
 */

package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.Test;

/**
 * Test class for {@link ConfigMappingLoader}.
 */
public class ConfigMappingLoaderTest {

    /**
     * A very small configuration mapping used for the "positive" test cases.
     * The {@code prefix} value is irrelevant – the presence of the annotation
     * is enough for {@code ConfigMappingInterface} to recognise the type as a
     * mapping.
     */
    @ConfigProperties(prefix = "test")
    public interface SimpleMapping {
        String name();
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for ConfigMappingLoader.getConfigMappingsMetadata(Class)      */
    /* --------------------------------------------------------------------- */

    @Test
    public void getConfigMappingsMetadata_NonMapping_ReturnsEmptyList() {
        List<ConfigMappingMetadata> result = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull("Result must never be null", result);
        assertTrue("Non‑mapping type should yield an empty list", result.isEmpty());
    }

    @Test
    public void getConfigMappingsMetadata_NullArgument_ThrowsNPE() {
        try {
            ConfigMappingLoader.getConfigMappingsMetadata(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void getConfigMappingsMetadata_MappingInterface_ReturnsMetadata() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(SimpleMapping.class);
        assertNotNull(metadata);
        // The list must contain at least the mapping itself
        assertFalse("Metadata list should contain at least one element", metadata.isEmpty());

        // Verify that the first element corresponds to the interface we passed
        ConfigMappingMetadata first = metadata.get(0);
        assertEquals("Interface type must match the supplied class",
                SimpleMapping.class, first.getInterfaceType());

        // No nested mappings for this simple example – the list size should be 1
        assertEquals("Expected only the top‑level mapping", 1, metadata.size());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for ConfigMappingLoader.getConfigMapping(Class)                */
    /* --------------------------------------------------------------------- */

    @Test
    public void getConfigMapping_NonMapping_ReturnsNull() {
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(String.class);
        assertNull("Non‑mapping type should return null", result);
    }

    @Test
    public void getConfigMapping_NullArgument_ThrowsNPE() {
        try {
            ConfigMappingLoader.getConfigMapping(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void getConfigMapping_MappingInterface_ReturnsConfigMappingInterface() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(SimpleMapping.class);
        assertNotNull("Mapping interface should be non‑null for a valid mapping type", mapping);
        assertEquals("Returned mapping must refer to the supplied interface type",
                SimpleMapping.class, mapping.getInterfaceType());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for ConfigMappingLoader.getImplementationClass(Class)           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getImplementationClass_NonMapping_ThrowsIllegalArgumentException() {
        try {
            ConfigMappingLoader.getImplementationClass(String.class);
            fail("Expected IllegalArgumentException for non‑mapping type");
        } catch (IllegalArgumentException e) {
            // The library uses ConfigMessages.msg.classIsNotAMapping(...)
            // We only verify that an IllegalArgumentException is thrown.
            assertTrue(e.getMessage().contains("not a mapping"));
        }
    }

    @Test
    public void getImplementationClass_NullArgument_ThrowsNPE() {
        try {
            ConfigMappingLoader.getImplementationClass(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void getImplementationClass_ValidMapping_ReturnsImplementationClass() {
        Class<? extends ConfigMappingObject> implClass =
                ConfigMappingLoader.getImplementationClass(SimpleMapping.class);

        assertNotNull("Implementation class must not be null for a valid mapping", implClass);
        assertTrue("Implementation class must implement ConfigMappingObject",
                ConfigMappingObject.class.isAssignableFrom(implClass));

        // The generated implementation class should have a constructor that accepts
        // ConfigMappingContext.  We verify its existence via reflection.
        try {
            implClass.getConstructor(ConfigMappingContext.class);
        } catch (NoSuchMethodException e) {
            fail("Generated implementation class does not have the expected constructor");
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Additional edge‑case validation for the public API                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void getConfigMappingsMetadata_WithSuperInterface_MergesSuperMetadata() {
        // Define a second mapping interface that extends SimpleMapping.
        @ConfigProperties(prefix = "child")
        interface ChildMapping extends SimpleMapping {
            int age();
        }

        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ChildMapping.class);
        assertNotNull(metadata);
        // Expect at least the child mapping and its super‑type (SimpleMapping)
        assertTrue("Metadata should contain the child mapping", metadata.stream()
                .anyMatch(m -> m.getInterfaceType() == ChildMapping.class));
        assertTrue("Metadata should contain the super‑type mapping", metadata.stream()
                .anyMatch(m -> m.getInterfaceType() == SimpleMapping.class));
    }

    @Test
    public void getConfigMapping_WithSuperInterface_ContainsSuperTypes() {
        @ConfigProperties(prefix = "parent")
        interface ParentMapping {
            String parentProp();
        }

        @ConfigProperties(prefix = "child")
        interface ChildExtendsParent extends ParentMapping {
            String childProp();
        }

        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(ChildExtendsParent.class);
        assertNotNull(mapping);
        ConfigMappingInterface[] superTypes = mapping.getSuperTypes();
        assertNotNull(superTypes);
        // The super‑type array must contain at least the direct parent interface
        assertTrue("Super‑type list must contain ParentMapping",
                java.util.Arrays.stream(superTypes)
                        .anyMatch(s -> s.getInterfaceType() == ParentMapping.class));
    }

}
