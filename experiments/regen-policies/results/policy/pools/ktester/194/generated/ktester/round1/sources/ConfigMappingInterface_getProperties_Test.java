package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getProperties_Test {

    /**
     * Minimal concrete {@link ConfigMappingInterface.Property} implementation
     * that lets us control the {@link #getMemberName()} and {@link #isToStringMethod()}
     * behaviour required for the tests.
     */
    private static final class TestProperty extends ConfigMappingInterface.Property {

        private final String memberName;

        private final boolean toStringMethod;

        TestProperty(final Method method, final String propertyName, final String memberName, final boolean toStringMethod) {
            super(method, propertyName);
            this.memberName = memberName;
            this.toStringMethod = toStringMethod;
        }

        @Override
        public String getMemberName() {
            return memberName;
        }

        @Override
        public boolean isToStringMethod() {
            return toStringMethod;
        }
    }

    /**
     * Dummy interface used only to obtain {@link Method} objects.
     */
    private interface DummyMethods {

        void propA();

        void propB();

        void propC();

        void propD();

        void propE();
    }

    @Test
    void getProperties_withoutSuperTypes_returnsOwnProperties() throws Exception {
        // arrange – a single property belonging to a stand‑alone mapping
        Method mA = DummyMethods.class.getMethod("propA");
        TestProperty ownProp = new TestProperty(mA, null, "propA", false);
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { ownProp });
        // act
        ConfigMappingInterface.Property[] result = mapping.getProperties();
        // assert – exactly one property and it is the one we supplied
        assertEquals(1, result.length);
        assertSame(ownProp, result[0]);
    }

    @Test
    void getProperties_withSuperButNoOverlap_combinesBoth() throws Exception {
        // arrange – super mapping with one property
        Method mA = DummyMethods.class.getMethod("propA");
        TestProperty superProp = new TestProperty(mA, null, "propA", false);
        ConfigMappingInterface superMapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superProp });
        // sub mapping with a different property
        Method mB = DummyMethods.class.getMethod("propB");
        TestProperty subProp = new TestProperty(mB, null, "propB", false);
        ConfigMappingInterface subMapping = new ConfigMappingInterface(DummyMethods.class, new ConfigMappingInterface[] { superMapping }, new ConfigMappingInterface.Property[] { subProp });
        // act
        ConfigMappingInterface.Property[] result = subMapping.getProperties();
        // assert – both properties are present (order is not defined)
        assertEquals(2, result.length);
        Map<String, ConfigMappingInterface.Property> byName = new HashMap<>();
        for (ConfigMappingInterface.Property p : result) {
            byName.put(p.getMemberName(), p);
        }
        assertSame(superProp, byName.get("propA"));
        assertSame(subProp, byName.get("propB"));
    }

    @Test
    void getProperties_withSuperAndOverride_subPropertyOverridesSuper() throws Exception {
        // arrange – super mapping defines "propC"
        Method mC = DummyMethods.class.getMethod("propC");
        TestProperty superProp = new TestProperty(mC, null, "propC", false);
        ConfigMappingInterface superMapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superProp });
        // sub mapping defines another "propC" (same member name) – this should win
        TestProperty subProp = new TestProperty(mC, null, "propC", false);
        ConfigMappingInterface subMapping = new ConfigMappingInterface(DummyMethods.class, new ConfigMappingInterface[] { superMapping }, new ConfigMappingInterface.Property[] { subProp });
        // act
        ConfigMappingInterface.Property[] result = subMapping.getProperties();
        // assert – only one property (the overridden one) is present
        assertEquals(1, result.length);
        assertSame(subProp, result[0]);
    }

    @Test
    void getProperties_ignoresToStringMethodProperty() throws Exception {
        // arrange – a property that is flagged as a toString method should be filtered out
        Method mA = DummyMethods.class.getMethod("propA");
        // isToStringMethod = true
        TestProperty toStringProp = new TestProperty(mA, null, "toString", true);
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { toStringProp });
        // act
        ConfigMappingInterface.Property[] result = mapping.getProperties();
        // assert – result should be empty because the constructor filtered the toString method
        assertEquals(0, result.length);
    }

    @Test
    void getProperties_noSuperAndNoOwn_returnsEmptyArray() throws Exception {
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface.Property[] result = mapping.getProperties();
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void getProperties_onlySuperProperties_returnsSuperOnly() throws Exception {
        // super mapping with two properties
        Method mA = DummyMethods.class.getMethod("propA");
        Method mB = DummyMethods.class.getMethod("propB");
        TestProperty superPropA = new TestProperty(mA, null, "host", false);
        TestProperty superPropB = new TestProperty(mB, null, "port", false);
        ConfigMappingInterface superMapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superPropA, superPropB });
        // sub mapping has no own properties
        ConfigMappingInterface subMapping = new ConfigMappingInterface(DummyMethods.class, new ConfigMappingInterface[] { superMapping }, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface.Property[] result = subMapping.getProperties();
        assertEquals(2, result.length);
        // order is the insertion order of the map (LinkedHashMap is used internally)
        Map<String, ConfigMappingInterface.Property> byMember = new HashMap<>();
        for (ConfigMappingInterface.Property p : result) {
            byMember.put(p.getMemberName(), p);
        }
        assertSame(superPropA, byMember.get("host"));
        assertSame(superPropB, byMember.get("port"));
    }

    @Test
    void getProperties_onlyOwnProperties_returnsOwnOnly() throws Exception {
        // own mapping with three properties
        Method mA = DummyMethods.class.getMethod("propA");
        Method mB = DummyMethods.class.getMethod("propB");
        Method mC = DummyMethods.class.getMethod("propC");
        TestProperty p1 = new TestProperty(mA, null, "username", false);
        TestProperty p2 = new TestProperty(mB, null, "password", false);
        TestProperty p3 = new TestProperty(mC, null, "timeout", false);
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { p1, p2, p3 });
        ConfigMappingInterface.Property[] result = mapping.getProperties();
        assertEquals(3, result.length);
        assertArrayEquals(new ConfigMappingInterface.Property[] { p1, p2, p3 }, result);
    }

    @Test
    void getProperties_superAndOwn_noNameClash_combinesAll() throws Exception {
        // super properties
        Method mA = DummyMethods.class.getMethod("propA");
        Method mB = DummyMethods.class.getMethod("propB");
        TestProperty superA = new TestProperty(mA, null, "host", false);
        TestProperty superB = new TestProperty(mB, null, "port", false);
        ConfigMappingInterface superMapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superA, superB });
        // own properties with distinct names
        Method mC = DummyMethods.class.getMethod("propC");
        Method mD = DummyMethods.class.getMethod("propD");
        Method mE = DummyMethods.class.getMethod("propE");
        TestProperty own1 = new TestProperty(mC, null, "username", false);
        TestProperty own2 = new TestProperty(mD, null, "password", false);
        TestProperty own3 = new TestProperty(mE, null, "timeout", false);
        ConfigMappingInterface subMapping = new ConfigMappingInterface(DummyMethods.class, new ConfigMappingInterface[] { superMapping }, new ConfigMappingInterface.Property[] { own1, own2, own3 });
        ConfigMappingInterface.Property[] result = subMapping.getProperties();
        assertEquals(5, result.length);
        Map<String, ConfigMappingInterface.Property> byMember = new HashMap<>();
        for (ConfigMappingInterface.Property p : result) {
            byMember.put(p.getMemberName(), p);
        }
        assertSame(superA, byMember.get("host"));
        assertSame(superB, byMember.get("port"));
        assertSame(own1, byMember.get("username"));
        assertSame(own2, byMember.get("password"));
        assertSame(own3, byMember.get("timeout"));
    }

    @Test
    void getProperties_ownOverridesSuper_propertyFromOwnWins() throws Exception {
        // super provides "host"
        Method mA = DummyMethods.class.getMethod("propA");
        TestProperty superHost = new TestProperty(mA, null, "host", false);
        ConfigMappingInterface superMapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superHost });
        // own also provides "host" (different instance)
        Method mB = DummyMethods.class.getMethod("propB");
        TestProperty ownHost = new TestProperty(mB, null, "host", false);
        ConfigMappingInterface subMapping = new ConfigMappingInterface(DummyMethods.class, new ConfigMappingInterface[] { superMapping }, new ConfigMappingInterface.Property[] { ownHost });
        ConfigMappingInterface.Property[] result = subMapping.getProperties();
        assertEquals(1, result.length);
        assertSame(ownHost, result[0]);
    }

    @Test
    void getProperties_multipleHierarchyOverrides_mostDerivedWins() throws Exception {
        // level 1 (root) – "mode" = A, "debug" = true
        Method mA = DummyMethods.class.getMethod("propA");
        TestProperty rootMode = new TestProperty(mA, null, "mode", false);
        TestProperty rootDebug = new TestProperty(mA, null, "debug", false);
        ConfigMappingInterface root = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { rootMode, rootDebug });
        // level 2 (intermediate) – overrides "mode" = B
        Method mB = DummyMethods.class.getMethod("propB");
        TestProperty intermediateMode = new TestProperty(mB, null, "mode", false);
        ConfigMappingInterface intermediate = new ConfigMappingInterface(DummyMethods.class, new ConfigMappingInterface[] { root }, new ConfigMappingInterface.Property[] { intermediateMode });
        // level 3 (own) – overrides "mode" = C
        Method mC = DummyMethods.class.getMethod("propC");
        TestProperty ownMode = new TestProperty(mC, null, "mode", false);
        ConfigMappingInterface leaf = new ConfigMappingInterface(DummyMethods.class, new ConfigMappingInterface[] { intermediate }, new ConfigMappingInterface.Property[] { ownMode });
        ConfigMappingInterface.Property[] result = leaf.getProperties();
        // Expected: "debug" from root and "mode" from leaf (own)
        assertEquals(2, result.length);
        Map<String, ConfigMappingInterface.Property> byMember = new HashMap<>();
        for (ConfigMappingInterface.Property p : result) {
            byMember.put(p.getMemberName(), p);
        }
        assertSame(rootDebug, byMember.get("debug"));
        assertSame(ownMode, byMember.get("mode"));
    }

    @Test
    void getProperties_parentPropertyNameIsEmpty_stillReturned() throws Exception {
        // property with empty propertyName (parent property)
        Method mA = DummyMethods.class.getMethod("propA");
        TestProperty parentProp = new TestProperty(mA, "", "parent", false);
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { parentProp });
        ConfigMappingInterface.Property[] result = mapping.getProperties();
        assertEquals(1, result.length);
        assertSame(parentProp, result[0]);
        // verify that getMemberName() (the map key) is still "parent"
        assertEquals("parent", result[0].getMemberName());
    }

    @Test
    void getProperties_defensiveCopy_twoCallsReturnDifferentArrays() throws Exception {
        Method mA = DummyMethods.class.getMethod("propA");
        TestProperty p = new TestProperty(mA, null, "propA", false);
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { p });
        ConfigMappingInterface.Property[] first = mapping.getProperties();
        ConfigMappingInterface.Property[] second = mapping.getProperties();
        assertNotSame(first, second);
        assertArrayEquals(first, second);
        // mutate first array – should not affect second
        first[0] = null;
        assertNotNull(second[0]);
    }

    @Test
    void getProperties_nullElementInOwnArray_throwsNPE() throws Exception {
        // create a mapping with a null entry in the private `properties` array
        Method mA = DummyMethods.class.getMethod("propA");
        TestProperty valid = new TestProperty(mA, null, "valid", false);
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { valid });
        // inject null into the internal array via reflection
        Field propsField = ConfigMappingInterface.class.getDeclaredField("properties");
        propsField.setAccessible(true);
        ConfigMappingInterface.Property[] corrupted = new ConfigMappingInterface.Property[] { valid, null };
        propsField.set(mapping, corrupted);
        assertThrows(NullPointerException.class, mapping::getProperties);
    }

    @Test
    void getProperties_ownOverridesSuper_orderVerification() throws Exception {
        // super map with deterministic insertion order (LinkedHashMap is used internally)
        Method mA = DummyMethods.class.getMethod("propA");
        Method mB = DummyMethods.class.getMethod("propB");
        TestProperty superHost = new TestProperty(mA, null, "host", false);
        TestProperty superPort = new TestProperty(mB, null, "port", false);
        ConfigMappingInterface superMapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superHost, superPort });
        // own overrides "host"
        Method mC = DummyMethods.class.getMethod("propC");
        TestProperty ownHost = new TestProperty(mC, null, "host", false);
        ConfigMappingInterface subMapping = new ConfigMappingInterface(DummyMethods.class, new ConfigMappingInterface[] { superMapping }, new ConfigMappingInterface.Property[] { ownHost });
        ConfigMappingInterface.Property[] result = subMapping.getProperties();
        // Expected order: "port" (still from super) then "host" (own)
        assertEquals(2, result.length);
        assertSame(superPort, result[0]);
        assertSame(ownHost, result[1]);
    }

    @Test
    void getProperties_superMapEmptyOwnPropertiesNull_throwsNPE() throws Exception {
        // create a mapping with null `properties` field
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        // set internal array to null via reflection
        Field propsField = ConfigMappingInterface.class.getDeclaredField("properties");
        propsField.setAccessible(true);
        propsField.set(mapping, null);
        // super properties are empty (no super types)
        assertThrows(NullPointerException.class, mapping::getProperties);
    }

    @Test
    void getProperties_propertyWithNullMethod_throwsNPE() throws Exception {
        // anonymous subclass that does not override getMemberName()
        ConfigMappingInterface.Property broken = new ConfigMappingInterface.Property(null, null) {
        };
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyMethods.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { broken });
        assertThrows(NullPointerException.class, mapping::getProperties);
    }
}