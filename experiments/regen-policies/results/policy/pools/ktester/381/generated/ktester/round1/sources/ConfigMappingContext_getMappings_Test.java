package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getMappings_Test {

    @Mock
    SmallRyeConfig // not used directly by getMappings()
    mockConfig;

    @Mock
    SmallRyeConfigBuilder.MappingBuilder mockBuilder;

    ConfigMappingContext context;

    class Wrapper {

        final String name;

        Wrapper(String n) {
            this.name = n;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Wrapper && ((Wrapper) o).name.equals(name);
        }
    }

    class Wrapper {

        final String name;

        Wrapper(String n) {
            this.name = n;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Wrapper && ((Wrapper) o).name.equals(name);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default the builder returns empty collections – this yields an empty
        // internal mappings map after construction.
        when(mockBuilder.getMappingsInstances()).thenReturn(Collections.emptyMap());
        when(mockBuilder.getMappings()).thenReturn(Collections.emptyList());
        // Construct the context under test.
        context = new ConfigMappingContext(mockConfig, mockBuilder);
    }

    @Test
    void getMappings_returnsNonNullEmptyMap_whenBuilderHasNoMappings() {
        Map<Class<?>, Map<String, Object>> mappings = context.getMappings();
        assertNotNull(mappings, "The mappings map should never be null");
        assertTrue(mappings.isEmpty(), "With an empty builder the mappings map must be empty");
    }

    @Test
    void getMappings_returnsSameInstance_onMultipleInvocations() {
        Map<Class<?>, Map<String, Object>> firstCall = context.getMappings();
        Map<Class<?>, Map<String, Object>> secondCall = context.getMappings();
        assertSame(firstCall, secondCall, "Subsequent calls must return the same map instance");
    }

    @Test
    void getMappings_containsEntry_whenBuilderProvidesMappingInstance() {
        // Arrange – a non‑empty mapping instance.
        ConfigClass dummyConfigClass = mock(ConfigClass.class);
        // Simulate a concrete type and a prefix that the constructor will use.
        // any class is fine
        Class<?> dummyType = String.class;
        String dummyPrefix = "dummy.prefix";
        when(dummyConfigClass.getType()).thenReturn(dummyType);
        when(dummyConfigClass.getPrefix()).thenReturn(dummyPrefix);
        // The instance that should be stored in the internal map.
        Object dummyInstance = new Object();
        // Builder returns a single mapping instance.
        Map<ConfigClass, Object> instances = new HashMap<>();
        instances.put(dummyConfigClass, dummyInstance);
        when(mockBuilder.getMappingsInstances()).thenReturn(instances);
        when(mockBuilder.getMappings()).thenReturn(Collections.emptyList());
        // Re‑create the context so the constructor processes the new builder data.
        ConfigMappingContext ctxWithInstance = new ConfigMappingContext(mockConfig, mockBuilder);
        // Act
        Map<Class<?>, Map<String, Object>> mappings = ctxWithInstance.getMappings();
        // Assert
        assertNotNull(mappings);
        assertTrue(mappings.containsKey(dummyType), "The outer map must contain the dummy type key");
        assertEquals(dummyInstance, mappings.get(dummyType).get(dummyPrefix), "The inner map must contain the dummy instance under its prefix");
    }

    @Test
    void getMappings_isIdentityHashMapImplementation() {
        Map<Class<?>, Map<String, Object>> mappings = context.getMappings();
        assertTrue(mappings instanceof IdentityHashMap, "The concrete implementation should be IdentityHashMap");
    }

    @Test
    void getMappings_modifyingReturnedMapAffectsInternalState() {
        Map<Class<?>, Map<String, Object>> map = context.getMappings();
        // add an entry
        map.put(String.class, new HashMap<>());
        assertFalse(map.isEmpty(), "Map should contain the added entry");
        // clear via the returned reference
        map.clear();
        assertTrue(context.getMappings().isEmpty(), "After clearing, the internal map should be empty");
    }

    @Test
    void getMappings_nullKeyThrowsNPE() {
        Map<Class<?>, Map<String, Object>> map = context.getMappings();
        assertThrows(NullPointerException.class, () -> map.put(null, new HashMap<>()), "IdentityHashMap does not allow null keys");
    }

    @Test
    void getMappings_identityKeyBehavior() {
        Map<Class<?>, Map<String, Object>> outer = context.getMappings();
        // Use distinct Class objects that are equal by content (Class objects are unique,
        // so we simulate identity behaviour with custom wrapper keys).
        // actually java.lang.String.class
        Class<?> key1 = new String("key").getClass();
        // same reference, but we need distinct objects
        Class<?> key2 = new String("key").getClass();
        // To truly have distinct objects with same content we use a wrapper:
        class Wrapper {

            final String name;

            Wrapper(String n) {
                this.name = n;
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof Wrapper && ((Wrapper) o).name.equals(name);
            }
        }
        Wrapper w1 = new Wrapper("same");
        Wrapper w2 = new Wrapper("same");
        // Store wrappers as keys using a raw map to demonstrate identity semantics
        @SuppressWarnings("unchecked")
        Map<Object, Object> identityMap = (Map<Object, Object>) context.getMappings();
        // Cast is safe because the underlying map is IdentityHashMap
        identityMap.put(w1, "first");
        identityMap.put(w2, "second");
        assertEquals(2, identityMap.size(), "IdentityHashMap should treat distinct key objects as different entries");
        assertEquals("first", identityMap.get(w1));
        assertEquals("second", identityMap.get(w2));
    }

    @Test
    void getMappings_allowsNullValue() {
        Map<Class<?>, Map<String, Object>> map = context.getMappings();
        map.put(Integer.class, null);
        assertTrue(map.containsKey(Integer.class), "Key should be present even if value is null");
        assertNull(map.get(Integer.class), "Value retrieved should be null");
    }

    @Test
    void getMappings_persistsEntriesAcrossOtherMethodCalls() {
        Map<Class<?>, Map<String, Object>> map = context.getMappings();
        map.put(String.class, new HashMap<>());
        // invoke a variety of public methods
        context.applyPrefix("test.prefix");
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.KEBAB_CASE);
        context.applyBeanStyleGetters(Boolean.TRUE);
        context.problem(new RuntimeException("dummy"));
        context.reportUnknown(Set.of("ignored.path"));
        // after each call the entry must still be present
        assertTrue(context.getMappings().containsKey(String.class), "Entry must survive after other method invocations");
        assertSame(map, context.getMappings(), "Map instance must remain the same after other method calls");
    }

    @Test
    void getMappings_instanceStabilityWithInterleavedCalls() {
        Map<Class<?>, Map<String, Object>> first = context.getMappings();
        // Interleave calls
        context.applyPrefix("a");
        context.applyBeanStyleGetters(Boolean.FALSE);
        context.applyNamingStrategy(ConfigMapping.NamingStrategy.CAMEL_CASE);
        context.problem(new IllegalArgumentException());
        context.reportUnknown(Set.of());
        Map<Class<?>, Map<String, Object>> second = context.getMappings();
        assertSame(first, second, "Map reference must stay identical across interleaved method calls");
    }

    /* -----------------------------------------------------------
     *  Parameterized test for null‑value acceptance with different keys
     * ----------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(classes = { String.class, Integer.class, List.class })
    void getMappings_nullValueAcceptedForVariousKeys(Class<?> key) {
        Map<Class<?>, Map<String, Object>> map = context.getMappings();
        map.put(key, null);
        assertTrue(map.containsKey(key));
        assertNull(map.get(key));
    }
}
