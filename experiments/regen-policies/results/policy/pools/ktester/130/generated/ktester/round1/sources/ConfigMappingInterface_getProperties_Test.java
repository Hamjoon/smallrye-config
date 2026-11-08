package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Map;
import java.lang.reflect.Field;

class ConfigMappingInterface_getProperties_Test {

    /* -------------------------------------------------------------
     * Dummy configuration interfaces used only for the tests
     * ------------------------------------------------------------- */
    interface SuperConfig {

        String superValue();

        String overridden();
    }

    interface ChildConfig extends SuperConfig {

        String childValue();

        @Override
        String overridden();
    }

    /* -------------------------------------------------------------
     * Helper to create a Property instance from a Method and a name
     * ------------------------------------------------------------- */
    private static ConfigMappingInterface.Property createProperty(final Method method, final String name) {
        // Property is abstract, but all its behaviour is already implemented.
        // An anonymous subclass is sufficient.
        return new ConfigMappingInterface.Property(method, name) {
        };
    }

    /* -------------------------------------------------------------
     * Helper to instantiate ConfigMappingInterface via its package‑private
     * constructor (used in the original test file). The constructor signature
     * is (Class<?> interfaceType, ConfigMappingInterface[] superTypes,
     * Property[] ownProperties).
     * ------------------------------------------------------------- */
    private static ConfigMappingInterface newConfig(final Class<?> iface, final ConfigMappingInterface[] supers, final ConfigMappingInterface.Property[] own) throws Exception {
        // Use an empty array to satisfy the constructor's validation.
        ConfigMappingInterface.Property[] safeOwn = new ConfigMappingInterface.Property[0];
        var ctor = ConfigMappingInterface.class.getDeclaredConstructor(Class.class, ConfigMappingInterface[].class, ConfigMappingInterface.Property[].class);
        ctor.setAccessible(true);
        ConfigMappingInterface cfg = (ConfigMappingInterface) ctor.newInstance(iface, supers, safeOwn);
        // If the caller supplied a different array (including null), overwrite the private field.
        if (own != safeOwn) {
            Field propsField = ConfigMappingInterface.class.getDeclaredField("properties");
            propsField.setAccessible(true);
            propsField.set(cfg, own);
        }
        return cfg;
    }

    @Test
    void getProperties_withoutSuperTypes_returnsOwnProperties() throws NoSuchMethodException {
        Method superMethod = SuperConfig.class.getMethod("superValue");
        Method overriddenMethod = SuperConfig.class.getMethod("overridden");
        ConfigMappingInterface.Property p1 = createProperty(superMethod, "p1");
        ConfigMappingInterface.Property p2 = createProperty(overriddenMethod, "p2");
        ConfigMappingInterface config = new ConfigMappingInterface(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { p1, p2 });
        ConfigMappingInterface.Property[] result = config.getProperties();
        Map<String, ConfigMappingInterface.Property> byName = Arrays.stream(result).collect(Collectors.toMap(ConfigMappingInterface.Property::getMemberName, p -> p));
        assertEquals(2, byName.size(), "Should contain exactly the two declared properties");
        assertSame(p1, byName.get("superValue"));
        assertSame(p2, byName.get("overridden"));
    }

    @Test
    void getProperties_withSuperTypes_overridesSuperProperties() throws NoSuchMethodException {
        Method superMethod = SuperConfig.class.getMethod("superValue");
        ConfigMappingInterface.Property superProp = createProperty(superMethod, "superProp");
        Method superOverridden = SuperConfig.class.getMethod("overridden");
        ConfigMappingInterface.Property superOverriddenProp = createProperty(superOverridden, "superOverridden");
        ConfigMappingInterface superConfig = new ConfigMappingInterface(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superProp, superOverriddenProp });
        Method childMethod = ChildConfig.class.getMethod("childValue");
        ConfigMappingInterface.Property childProp = createProperty(childMethod, "childProp");
        Method childOverridden = ChildConfig.class.getMethod("overridden");
        ConfigMappingInterface.Property overridingProp = createProperty(childOverridden, "overridingProp");
        ConfigMappingInterface childConfig = new ConfigMappingInterface(ChildConfig.class, new ConfigMappingInterface[] { superConfig }, new ConfigMappingInterface.Property[] { childProp, overridingProp });
        ConfigMappingInterface.Property[] result = childConfig.getProperties();
        Map<String, ConfigMappingInterface.Property> byMember = Arrays.stream(result).collect(Collectors.toMap(ConfigMappingInterface.Property::getMemberName, p -> p));
        assertEquals(3, byMember.size(), "Should contain three properties after merging with super type");
        assertSame(superProp, byMember.get("superValue"));
        assertSame(childProp, byMember.get("childValue"));
        assertSame(overridingProp, byMember.get("overridden"));
        assertNotSame(superOverriddenProp, byMember.get("overridden"));
    }

    /* -------------------------------------------------------------
     * 1. No super properties, no own properties
     * ------------------------------------------------------------- */
    @Test
    void getProperties_noSuper_noOwn_returnsEmptyArray() throws Exception {
        ConfigMappingInterface cfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface.Property[] result = cfg.getProperties();
        assertNotNull(result);
        assertEquals(0, result.length, "Expected empty array when no super and no own properties");
    }

    /* -------------------------------------------------------------
     * 2. No super properties, own properties present
     * ------------------------------------------------------------- */
    @Test
    void getProperties_noSuper_withOwn_returnsOwnProperties() throws Exception {
        Method m1 = SuperConfig.class.getMethod("superValue");
        Method m2 = SuperConfig.class.getMethod("overridden");
        ConfigMappingInterface.Property p1 = createProperty(m1, "p1");
        ConfigMappingInterface.Property p2 = createProperty(m2, "p2");
        ConfigMappingInterface cfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { p1, p2 });
        ConfigMappingInterface.Property[] result = cfg.getProperties();
        assertEquals(2, result.length);
        Map<String, ConfigMappingInterface.Property> byMember = Arrays.stream(result).collect(Collectors.toMap(ConfigMappingInterface.Property::getMemberName, p -> p));
        assertSame(p1, byMember.get("superValue"));
        assertSame(p2, byMember.get("overridden"));
    }

    /* -------------------------------------------------------------
     * 3. Super properties present, own properties empty
     * ------------------------------------------------------------- */
    @Test
    void getProperties_withSuper_noOwn_returnsSuperProperties() throws Exception {
        Method sm1 = SuperConfig.class.getMethod("superValue");
        Method sm2 = SuperConfig.class.getMethod("overridden");
        ConfigMappingInterface.Property sp1 = createProperty(sm1, "sp1");
        ConfigMappingInterface.Property sp2 = createProperty(sm2, "sp2");
        ConfigMappingInterface superCfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { sp1, sp2 });
        ConfigMappingInterface child = newConfig(ChildConfig.class, new ConfigMappingInterface[] { superCfg }, new ConfigMappingInterface.Property[0]);
        ConfigMappingInterface.Property[] result = child.getProperties();
        assertEquals(2, result.length);
        Map<String, ConfigMappingInterface.Property> byMember = Arrays.stream(result).collect(Collectors.toMap(ConfigMappingInterface.Property::getMemberName, p -> p));
        assertSame(sp1, byMember.get("superValue"));
        assertSame(sp2, byMember.get("overridden"));
    }

    /* -------------------------------------------------------------
     * 4. Super and own properties with distinct member names
     * ------------------------------------------------------------- */
    @Test
    void getProperties_superAndOwnDistinct_mergeAll() throws Exception {
        Method sm1 = SuperConfig.class.getMethod("superValue");
        ConfigMappingInterface.Property superProp = createProperty(sm1, "superProp");
        ConfigMappingInterface superCfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superProp });
        Method cm = ChildConfig.class.getMethod("childValue");
        ConfigMappingInterface.Property childProp = createProperty(cm, "childProp");
        ConfigMappingInterface child = newConfig(ChildConfig.class, new ConfigMappingInterface[] { superCfg }, new ConfigMappingInterface.Property[] { childProp });
        ConfigMappingInterface.Property[] result = child.getProperties();
        assertEquals(2, result.length);
        Map<String, ConfigMappingInterface.Property> byMember = Arrays.stream(result).collect(Collectors.toMap(ConfigMappingInterface.Property::getMemberName, p -> p));
        assertSame(superProp, byMember.get("superValue"));
        assertSame(childProp, byMember.get("childValue"));
    }

    /* -------------------------------------------------------------
     * 5. Own property overrides a super property with the same member name
     * ------------------------------------------------------------- */
    @Test
    void getProperties_ownOverridesSuper_propertyIsOwn() throws Exception {
        Method superMethod = SuperConfig.class.getMethod("overridden");
        ConfigMappingInterface.Property superProp = createProperty(superMethod, "superProp");
        ConfigMappingInterface superCfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { superProp });
        Method childMethod = ChildConfig.class.getMethod("overridden");
        ConfigMappingInterface.Property ownProp = createProperty(childMethod, "ownProp");
        ConfigMappingInterface child = newConfig(ChildConfig.class, new ConfigMappingInterface[] { superCfg }, new ConfigMappingInterface.Property[] { ownProp });
        ConfigMappingInterface.Property[] result = child.getProperties();
        assertEquals(1, result.length);
        assertSame(ownProp, result[0]);
    }

    /* -------------------------------------------------------------
     * 6. Own properties array contains duplicate member names
     * ------------------------------------------------------------- */
    @Test
    void getProperties_ownDuplicates_lastOneWins() throws Exception {
        Method m = SuperConfig.class.getMethod("superValue");
        ConfigMappingInterface.Property first = createProperty(m, "dup");
        ConfigMappingInterface.Property second = createProperty(m, "dup");
        ConfigMappingInterface cfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { first, second });
        ConfigMappingInterface.Property[] result = cfg.getProperties();
        assertEquals(1, result.length);
        assertSame(second, result[0], "The later property should overwrite the earlier one");
    }

    /* -------------------------------------------------------------
     * 7. Own properties array contains a null element
     * ------------------------------------------------------------- */
    @Test
    void getProperties_nullElementInOwnArray_throwsNPE() throws Exception {
        Method m = SuperConfig.class.getMethod("superValue");
        ConfigMappingInterface.Property valid = createProperty(m, "valid");
        ConfigMappingInterface.Property[] arrayWithNull = new ConfigMappingInterface.Property[] { valid, null };
        ConfigMappingInterface cfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, arrayWithNull);
        assertThrows(NullPointerException.class, cfg::getProperties, "Iterating over a null element should cause NPE");
    }

    /* -------------------------------------------------------------
     * 8. Own property throws exception in getMemberName()
     * ------------------------------------------------------------- */
    @Test
    void getProperties_propertyThrowsInGetMemberName_propagatesException() throws Exception {
        Method m = SuperConfig.class.getMethod("superValue");
        ConfigMappingInterface.Property bad = new ConfigMappingInterface.Property(m, "bad") {

            @Override
            public String getMemberName() {
                throw new IllegalStateException("boom");
            }
        };
        ConfigMappingInterface cfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { bad });
        IllegalStateException ex = assertThrows(IllegalStateException.class, cfg::getProperties);
        assertEquals("boom", ex.getMessage());
    }

    /* -------------------------------------------------------------
     * 9. Own properties array is null
     * ------------------------------------------------------------- */
    @Test
    void getProperties_ownArrayIsNull_throwsNPE() throws Exception {
        ConfigMappingInterface cfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, // own properties null
        null);
        assertThrows(NullPointerException.class, cfg::getProperties);
    }

    /* -------------------------------------------------------------
     * 10. Returned array is a defensive copy
     * ------------------------------------------------------------- */
    @Test
    void getProperties_returnsDefensiveCopy() throws Exception {
        Method m = SuperConfig.class.getMethod("superValue");
        ConfigMappingInterface.Property p = createProperty(m, "p");
        ConfigMappingInterface cfg = newConfig(SuperConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { p });
        ConfigMappingInterface.Property[] first = cfg.getProperties();
        assertEquals(1, first.length);
        // mutate the returned array
        first[0] = null;
        ConfigMappingInterface.Property[] second = cfg.getProperties();
        assertEquals(1, second.length);
        assertSame(p, second[0], "Subsequent call must return original property, not the mutated array");
    }
}
