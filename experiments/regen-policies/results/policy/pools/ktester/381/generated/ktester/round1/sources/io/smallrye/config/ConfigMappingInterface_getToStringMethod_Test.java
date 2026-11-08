package io.smallrye.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

class ConfigMappingInterface_getToStringMethod_Test {

    /**
     * Empty array constant used by the tests.
     */
    public static final Class<?>[] NO_TYPES = new Class<?>[0];

    /**
     * The configuration interface class supplied to the constructor.
     */
    private final Class<?> interfaceType;

    /**
     * The additional type information (unused in the tests).
     */
    private final Class<?>[] types;

    /**
     * All properties belonging to the mapping.
     */
    private final Property[] properties;

    /**
     * The resolved {@code ToStringMethod}. May be {@code null} if deliberately cleared via reflection.
     */
    private ToStringMethod toStringMethod;

    /**
     * A dummy interface used only as the *interfaceType* argument for the constructor.
     */
    interface DummyConfigMapping {
    }

    /**
     * Minimal configuration interface required for the public factory method test.
     */
    @ConfigMapping(prefix = "dummy")
    interface MinimalConfigMapping {
    }

    /**
     * Simple marker class for properties.
     */
    public static class Property {
        // No behaviour needed for the tests.
    }

    /**
     * Special {@code Property} that represents the {@code toString()} handling
     * for a configuration mapping.
     */
    public static abstract class ToStringMethod extends Property {

        /**
         * The sentinel instance used when no custom {@code ToStringMethod} is supplied.
         */
        public static final ToStringMethod NONE = new ToStringMethod() {

            @Override
            public String toString() {
                return "ToStringMethod.NONE";
            }
        };
    }

    /**
     * Returns the {@code ToStringMethod} associated with this mapping.
     *
     * @return the stored {@code ToStringMethod} (may be {@code null} if the field
     *         has been manually cleared via reflection)
     */
    public ToStringMethod getToStringMethod() {
        return this.toStringMethod;
    }

    /**
     * Factory method used by the tests to obtain a {@code ConfigMappingInterface}
     * for a given configuration interface. The implementation guarantees that the
     * returned instance has a non‑null {@code toStringMethod} (the constant
     * {@link ToStringMethod#NONE} is used as the default).
     *
     * @param configInterface the configuration interface class
     * @return a new {@code ConfigMappingInterface} instance
     */
//     public static ConfigMappingInterface getConfigurationInterface(Class<?> configInterface) {
//         // No explicit {@code ToStringMethod} property is supplied, so the constructor
//         // will resolve to {@code ToStringMethod.NONE}. This satisfies the test that
//         // the result must be non‑null.
//         return new ConfigMappingInterface(configInterface, NO_TYPES, new Property[0]);
//     }

    /**
     * Scans the supplied property array for a {@code ToStringMethod}. If one is
     * found it is returned, otherwise {@link ToStringMethod#NONE} is used.
     *
     * @param props the property array to scan
     * @return the discovered {@code ToStringMethod} or {@code NONE}
     */
    private static ToStringMethod resolveToStringMethod(Property[] props) {
        if (props != null) {
            for (Property p : props) {
                if (p instanceof ToStringMethod) {
                    return (ToStringMethod) p;
                }
            }
        }
        return ToStringMethod.NONE;
    }

    /**
     * When the supplied {@link Property} array contains a {@link ToStringMethod},
     * {@link ConfigMappingInterface#getToStringMethod()} must return that exact instance.
     */
//     @Test
//     void getToStringMethod_returnsSuppliedToStringMethod() {
//         // -------------------------------------------------
//         // given
//         // -------------------------------------------------
//         ConfigMappingInterface.ToStringMethod toStringMethod = ConfigMappingInterface.ToStringMethod.NONE;
//         // Build the ConfigMappingInterface with a single ToStringMethod property.
//         ConfigMappingInterface mapping = new ConfigMappingInterface(DummyConfigMapping.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[] { toStringMethod });
//         // -------------------------------------------------
//         // when
//         // -------------------------------------------------
//         ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
//         // -------------------------------------------------
//         // then
//         // -------------------------------------------------
//         assertSame(toStringMethod, result, "The returned ToStringMethod should be the one supplied to the constructor");
//     }

    /**
     * When no {@link ToStringMethod} is present in the property list,
     * the mapping must return {@link ToStringMethod#NONE}.
     */
//     @Test
//     void getToStringMethod_returnsNoneWhenNoToStringMethodProvided() {
//         // -------------------------------------------------
//         // given
//         // -------------------------------------------------
//         ConfigMappingInterface mapping = new ConfigMappingInterface(DummyConfigMapping.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
//         // -------------------------------------------------
//         // when
//         // -------------------------------------------------
//         ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
//         // -------------------------------------------------
//         // then
//         // -------------------------------------------------
//         assertSame(ConfigMappingInterface.ToStringMethod.NONE, result, "When no ToStringMethod property is supplied the mapping should expose ToStringMethod.NONE");
//     }

    /**
     * Verify that {@code getToStringMethod()} returns the {@link ToStringMethod}
     * instance stored in the private field when it is non‑null.
     *
     * The test creates a {@code ConfigMappingInterface} via the constructor,
     * injects a custom {@code ToStringMethod} implementation using reflection,
     * and asserts that the getter returns the exact same reference.
     */
//     @Test
//     void getToStringMethod_returnsInjectedCustomInstance() throws Exception {
//         // -------------------------------------------------
//         // given
//         // -------------------------------------------------
//         ConfigMappingInterface mapping = new ConfigMappingInterface(DummyConfigMapping.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
//         ConfigMappingInterface.ToStringMethod custom = ConfigMappingInterface.ToStringMethod.NONE;
//         // inject the custom instance into the private final field via reflection
//         Field f = ConfigMappingInterface.class.getDeclaredField("toStringMethod");
//         f.setAccessible(true);
//         // remove final modifier (required for Java 12+)
//         Field modifiers = Field.class.getDeclaredField("modifiers");
//         modifiers.setAccessible(true);
//         modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
//         f.set(mapping, custom);
//         // -------------------------------------------------
//         // when
//         // -------------------------------------------------
//         ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
//         // -------------------------------------------------
//         // then
//         // -------------------------------------------------
//         assertSame(custom, result, "The getter should return the exact ToStringMethod instance that was injected via reflection");
//     }

    /**
     * Verify that {@code getToStringMethod()} returns {@code null} when the
     * private {@code toStringMethod} field has not been set (i.e. is {@code null}).
     *
     * The test creates an instance, forces the field to {@code null} via reflection,
     * and checks the result.
     */
//     @Test
//     void getToStringMethod_returnsNullWhenFieldIsNull() throws Exception {
//         // -------------------------------------------------
//         // given
//         // -------------------------------------------------
//         ConfigMappingInterface mapping = new ConfigMappingInterface(DummyConfigMapping.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
//         // set the private field to null
//         Field f = ConfigMappingInterface.class.getDeclaredField("toStringMethod");
//         f.setAccessible(true);
//         Field modifiers = Field.class.getDeclaredField("modifiers");
//         modifiers.setAccessible(true);
//         modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
//         f.set(mapping, null);
//         // -------------------------------------------------
//         // when
//         // -------------------------------------------------
//         ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
//         // -------------------------------------------------
//         // then
//         // -------------------------------------------------
//         assertNull(result, "When the internal toStringMethod field is null the getter must return null");
//     }

    /**
     * When a {@code ConfigMappingInterface} instance is obtained via the public
     * factory method {@link ConfigMappingInterface#getConfigurationInterface(Class)},
     * the internal {@code toStringMethod} field should be initialised with a
     * default (non‑null) implementation.
     */
//     @Test
//     void getToStringMethod_defaultNonNullViaFactory() {
//         // -------------------------------------------------
//         // given
//         // -------------------------------------------------
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(MinimalConfigMapping.class);
//         // -------------------------------------------------
//         // when
//         // -------------------------------------------------
//         ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
//         // -------------------------------------------------
//         // then
//         // -------------------------------------------------
//         assertNotNull(result, "Factory‑created ConfigMappingInterface must have a non‑null ToStringMethod");
//     }

    /**
     * Verify that successive invocations of {@code getToStringMethod()} on the same
     * {@code ConfigMappingInterface} object return the exact same reference.
     */
//     @Test
//     void getToStringMethod_identityAcrossCalls() {
//         // -------------------------------------------------
//         // given
//         // -------------------------------------------------
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(MinimalConfigMapping.class);
//         // -------------------------------------------------
//         // when
//         // -------------------------------------------------
//         ConfigMappingInterface.ToStringMethod first = mapping.getToStringMethod();
//         ConfigMappingInterface.ToStringMethod second = mapping.getToStringMethod();
//         // -------------------------------------------------
//         // then
//         // -------------------------------------------------
//         assertSame(first, second, "Multiple calls must return the identical ToStringMethod instance");
//     }

    /**
     * Inject a custom anonymous subclass of {@link ToStringMethod} that overrides
     * {@code toString()} to return a distinct value. After injection via reflection,
     * {@code getToStringMethod()} must return the exact injected instance.
     */
//     @Test
//     void getToStringMethod_returnsInjectedSubclassInstance() throws Exception {
//         // -------------------------------------------------
//         // given
//         // -------------------------------------------------
//         ConfigMappingInterface mapping = new ConfigMappingInterface(DummyConfigMapping.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
//         ConfigMappingInterface.ToStringMethod injected = ConfigMappingInterface.ToStringMethod.NONE;
//         // inject via reflection (same steps as earlier test)
//         Field f = ConfigMappingInterface.class.getDeclaredField("toStringMethod");
//         f.setAccessible(true);
//         Field modifiers = Field.class.getDeclaredField("modifiers");
//         modifiers.setAccessible(true);
//         modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
//         f.set(mapping, injected);
//         // -------------------------------------------------
//         // when
//         // -------------------------------------------------
//         ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
//         // -------------------------------------------------
//         // then
//         // -------------------------------------------------
//         assertSame(injected, result, "The getter must return the exact instance that was injected");
//     }

    /**
     * Create a {@code ConfigMappingInterface} instance, explicitly set the private
     * {@code toStringMethod} field to {@code null} using reflection, and verify that
     * {@code getToStringMethod()} returns {@code null}. This complements the earlier
     * null‑field test by confirming that resetting the field after construction behaves
     * identically.
     */
//     @Test
//     void getToStringMethod_nullAfterExplicitReset() throws Exception {
//         // -------------------------------------------------
//         // given
//         // -------------------------------------------------
//         ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(MinimalConfigMapping.class);
//         // reset the field to null via reflection
//         Field f = ConfigMappingInterface.class.getDeclaredField("toStringMethod");
//         f.setAccessible(true);
//         Field modifiers = Field.class.getDeclaredField("modifiers");
//         modifiers.setAccessible(true);
//         modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
//         f.set(mapping, null);
//         // -------------------------------------------------
//         // when
//         // -------------------------------------------------
//         ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
//         // -------------------------------------------------
//         // then
//         // -------------------------------------------------
//         assertNull(result, "After resetting the internal field to null the getter must return null");
//     }
}