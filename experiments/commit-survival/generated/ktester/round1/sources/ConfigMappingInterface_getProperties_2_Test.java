package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

class ConfigMappingInterface_getProperties_2_Test {

    interface ChildConfig {

        String foo();
    }

    interface SuperConfig {

        String superProp();
    }

    interface OverlapConfig {

        String overrideProp();
    }

    interface GrandSuper {

        String grandSuperProp();
    }

    interface MidSuper extends GrandSuper {

        // redeclare with same name to test overriding
        String grandSuperProp();

        String subProp();
    }

    interface Bottom extends MidSuper {
        // no own properties – we only want the inherited ones
    }

    interface ConflictSuper1 {

        String conflictProp();
    }

    interface ConflictSuper2 {

        String conflictProp();
    }

    interface ConflictChild extends ConflictSuper2 {
        // does not declare its own conflictProp, should inherit from ConflictSuper2
    }

    @Test
    void getProperties_withoutSuper_returnsOwnPropertiesOnly() throws Exception {
        // Arrange – create a single property belonging to the child interface
        Method fooMethod = ChildConfig.class.getMethod("foo");
        ConfigMappingInterface.Property childProp = new ConfigMappingInterface.Property(fooMethod, "foo") {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { childProp });
        // Act
        ConfigMappingInterface.Property[] result = mapping.getProperties(false);
        // Assert – only the child property is present
        assertEquals(1, result.length);
        assertSame(childProp, result[0]);
    }

    @Test
    void getProperties_withSuper_mergesSuperAndOwnProperties() throws Exception {
        // Arrange – super property
        Method barMethod = SuperConfig.class.getMethod("bar");
        ConfigMappingInterface.Property superProp = new ConfigMappingInterface.Property(barMethod, "bar") {
        };
        ConfigMappingInterface superMapping = new ConfigMappingInterface(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superProp });
        // Arrange – child property
        Method fooMethod = ChildConfig.class.getMethod("foo");
        ConfigMappingInterface.Property childProp = new ConfigMappingInterface.Property(fooMethod, "foo") {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, new ConfigMappingInterface[] { superMapping }, new ConfigMappingInterface.Property[] { childProp });
        // Act
        ConfigMappingInterface.Property[] result = mapping.getProperties(true);
        // Assert – both super and child properties are present (order is not guaranteed)
        assertEquals(2, result.length);
        assertTrue(Arrays.stream(result).anyMatch(p -> p == superProp), "Super property missing");
        assertTrue(Arrays.stream(result).anyMatch(p -> p == childProp), "Child property missing");
    }

    @Test
    void getProperties_withSuper_overriddenMemberUsesChildProperty() throws Exception {
        // Arrange – super property with method name "same"
        Method superMethod = OverlapConfig.class.getMethod("same");
        ConfigMappingInterface.Property superProp = new ConfigMappingInterface.Property(superMethod, "same") {
        };
        ConfigMappingInterface superMapping = new ConfigMappingInterface(OverlapConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superProp });
        // Arrange – child property that has the same member name ("same")
        // We reuse the same Method instance to keep the signature identical.
        ConfigMappingInterface.Property childProp = new ConfigMappingInterface.Property(superMethod, "same") {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(OverlapConfig.class, new ConfigMappingInterface[] { superMapping }, new ConfigMappingInterface.Property[] { childProp });
        // Act
        ConfigMappingInterface.Property[] result = mapping.getProperties(true);
        // Assert – the child property overrides the super one, so only one entry exists
        assertEquals(1, result.length);
        assertSame(childProp, result[0], "Child property should override the super property");
    }

    @Test
    void includeSuper_true_basicMerging_noSuperProperties() throws Exception {
        // Current interface defines a single property, no super‑type properties.
        Method propAMethod = ChildConfig.class.getMethod("propA");
        ConfigMappingInterface.Property ownProp = new ConfigMappingInterface.Property(propAMethod, "propA") {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { ownProp });
        ConfigMappingInterface.Property[] result = mapping.getProperties(true);
        // Expect exactly the own property
        assertEquals(1, result.length);
        assertSame(ownProp, result[0]);
    }

    @Test
    void includeSuper_true_basicMerging_withOverride() throws Exception {
        // Super interface defines "overrideProp"
        Method superMethod = SuperConfig.class.getMethod("superProp");
        ConfigMappingInterface.Property superProp = new ConfigMappingInterface.Property(superMethod, "overrideProp") {
        };
        ConfigMappingInterface superMapping = new ConfigMappingInterface(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superProp });
        // Child overrides the same member name
        Method childMethod = OverlapConfig.class.getMethod("overrideProp");
        ConfigMappingInterface.Property childProp = new ConfigMappingInterface.Property(childMethod, "overrideProp") {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(OverlapConfig.class, new ConfigMappingInterface[] { superMapping }, new ConfigMappingInterface.Property[] { childProp });
        ConfigMappingInterface.Property[] result = mapping.getProperties(true);
        // Child property must win, resulting array size = 1
        assertEquals(1, result.length);
        assertSame(childProp, result[0]);
    }

    @Test
    void includeSuper_true_currentHasNoProperties_onlySuperReturned() throws Exception {
        // Super interface with two properties
        Method superMethod1 = SuperConfig.class.getMethod("superProp");
        ConfigMappingInterface.Property superProp = new ConfigMappingInterface.Property(superMethod1, "superProp") {
        };
        ConfigMappingInterface superMapping = new ConfigMappingInterface(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superProp });
        // Current interface defines no properties
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, new ConfigMappingInterface[] { superMapping }, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface.Property[] result = mapping.getProperties(true);
        // Expect only the super property
        assertEquals(1, result.length);
        assertSame(superProp, result[0]);
    }

    @Test
    void includeSuper_false_delegatesTo_getProperties() throws Exception {
        // Mapping with a single own property, no supers
        Method fooMethod = ChildConfig.class.getMethod("propA");
        ConfigMappingInterface.Property ownProp = new ConfigMappingInterface.Property(fooMethod, "propA") {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { ownProp });
        ConfigMappingInterface.Property[] result = mapping.getProperties(false);
        // Should be identical to the array returned by the no‑arg method
        assertArrayEquals(new ConfigMappingInterface.Property[] { ownProp }, result);
    }

    @Test
    void includeSuper_true_emptyHierarchy_returnsEmptyArray() {
        // No own properties and no super types
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface.Property[] result = mapping.getProperties(true);
        assertEquals(0, result.length);
    }

    @Test
    void includeSuper_true_multipleSuperLevels_withOverriding() throws Exception {
        // Grand‑super defines "grandSuperProp"
        Method gsMethod = GrandSuper.class.getMethod("grandSuperProp");
        ConfigMappingInterface.Property gsProp = new ConfigMappingInterface.Property(gsMethod, "grandSuperProp") {
        };
        ConfigMappingInterface grandSuperMapping = new ConfigMappingInterface(GrandSuper.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { gsProp });
        // Mid‑super redeclares "grandSuperProp" (overrides) and adds "subProp"
        Method msMethod = MidSuper.class.getMethod("grandSuperProp");
        ConfigMappingInterface.Property msOverrideProp = new ConfigMappingInterface.Property(msMethod, "grandSuperProp") {
        };
        Method subMethod = MidSuper.class.getMethod("subProp");
        ConfigMappingInterface.Property subProp = new ConfigMappingInterface.Property(subMethod, "subProp") {
        };
        ConfigMappingInterface midSuperMapping = new ConfigMappingInterface(MidSuper.class, new ConfigMappingInterface[] { grandSuperMapping }, new ConfigMappingInterface.Property[] { msOverrideProp, subProp });
        // Bottom interface has no own properties
        ConfigMappingInterface bottomMapping = new ConfigMappingInterface(Bottom.class, new ConfigMappingInterface[] { midSuperMapping }, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface.Property[] result = bottomMapping.getProperties(true);
        // Expected: "grandSuperProp" from MidSuper (overriding) and "subProp"
        assertEquals(2, result.length);
        List<String> names = Arrays.asList(result[0].getMemberName(), result[1].getMemberName());
        assertTrue(names.contains("grandSuperProp"));
        assertTrue(names.contains("subProp"));
        // Ensure the overriding instance is the one from MidSuper
        assertTrue(Arrays.stream(result).anyMatch(p -> p == msOverrideProp));
    }

    @Test
    void includeSuper_true_duplicateMemberNamesAcrossSuperTypes_keepsNearest() throws Exception {
        // First super (farther) defines conflictProp
        Method cs1Method = ConflictSuper1.class.getMethod("conflictProp");
        ConfigMappingInterface.Property cs1Prop = new ConfigMappingInterface.Property(cs1Method, "conflictProp") {
        };
        ConfigMappingInterface cs1Mapping = new ConfigMappingInterface(ConflictSuper1.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { cs1Prop });
        // Second super (nearer) also defines conflictProp
        Method cs2Method = ConflictSuper2.class.getMethod("conflictProp");
        ConfigMappingInterface.Property cs2Prop = new ConfigMappingInterface.Property(cs2Method, "conflictProp") {
        };
        ConfigMappingInterface cs2Mapping = new ConfigMappingInterface(ConflictSuper2.class, new ConfigMappingInterface[] { cs1Mapping }, new ConfigMappingInterface.Property[] { cs2Prop });
        // Child extends only the nearer super (ConflictSuper2) – no own properties
        ConfigMappingInterface childMapping = new ConfigMappingInterface(ConflictChild.class, new ConfigMappingInterface[] { cs2Mapping }, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface.Property[] result = childMapping.getProperties(true);
        // Only one property should be present and it must be the nearer one (cs2Prop)
        assertEquals(1, result.length);
        assertSame(cs2Prop, result[0]);
    }

    @Test
    void includeSuper_false_emptyOwnProperties_returnsEmptyArray() {
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        ConfigMappingInterface.Property[] result = mapping.getProperties(false);
        assertEquals(0, result.length);
    }

    @Test
    void includeSuper_true_nullInternalPropertiesArray_throwsNPE() throws Exception {
        // Build a normal mapping first
        Method fooMethod = ChildConfig.class.getMethod("propA");
        ConfigMappingInterface.Property prop = new ConfigMappingInterface.Property(fooMethod, "propA") {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { prop });
        // Use reflection to set the private final 'properties' field to null
        Field f = ConfigMappingInterface.class.getDeclaredField("properties");
        f.setAccessible(true);
        f.set(mapping, null);
        // Expect NPE when includeSuper = true (the method iterates over this.properties)
        assertThrows(NullPointerException.class, () -> mapping.getProperties(true));
    }

    @Test
    void includeSuper_true_internalArrayContainsNullElement_throwsNPE() throws Exception {
        // Build a mapping whose properties array contains a null entry
        Method fooMethod = ChildConfig.class.getMethod("propA");
        ConfigMappingInterface.Property prop = new ConfigMappingInterface.Property(fooMethod, "propA") {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { null, prop });
        // The loop inside getProperties(true) will hit the null element first
        assertThrows(NullPointerException.class, () -> mapping.getProperties(true));
    }

    @Test
    void includeSuper_false_nullInternalPropertiesArray_throwsNPE() throws Exception {
        // Build a mapping with a non‑null properties array first
        Method fooMethod = ChildConfig.class.getMethod("propA");
        ConfigMappingInterface.Property prop = new ConfigMappingInterface.Property(fooMethod, "propA") {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(ChildConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { prop });
        // Set the internal array to null via reflection
        Field f = ConfigMappingInterface.class.getDeclaredField("properties");
        f.setAccessible(true);
        f.set(mapping, null);
        // Delegation to getProperties() should now throw NPE
        assertThrows(NullPointerException.class, () -> mapping.getProperties(false));
    }
}
