package io.smallrye.config;

class ConfigMappingContext_getMappings_Test {

    //     @Mock
    SmallRyeConfig // mocked – its behaviour is not needed for these tests
    mockConfig;

    ConfigMappingContext context;

    /**
     * The configuration instance – may be {@code null} in test environments.
     */
    private final SmallRyeConfig config;

    /**
     * The map that stores the mappings created during configuration mapping.
     * It must be an {@link IdentityHashMap} so that keys are compared by reference,
     * not by {@code equals()}.
     */
//     private final Map<Class<?>, Map<String, Object>> mappings;

//     @BeforeEach
    void setupBeforeEach() {
        // An empty mappings definition – the constructor will not call applyPrefix/constructMapping.
        Map<Class<?>, Set<String>> emptyMappings = new HashMap<>();
        // The ConfigMappingContext constructor only needs a SmallRyeConfig reference; null is acceptable here.
        context = new ConfigMappingContext(null, emptyMappings);
    }

//     @AfterEach
    void teardownAfterEach() {
        // The context does not hold external resources, but we clear the reference for safety.
        context = null;
    }

    /**
     * Returns the mutable map that holds the configuration mappings.
     *
     * @return the internal {@link IdentityHashMap} instance; the same instance is
     *         returned on every call.
     */
//     public Map<Class<?>, Map<String, Object>> getMappings() {
//         return mappings;
//     }

    /**
     * In the real library this method matches configuration properties with environment
     * variables.  For the purposes of the unit‑tests we only need a placeholder that does
     * nothing when {@code config} is {@code null}.  When a real {@code SmallRyeConfig}
     * instance is supplied we keep the original behaviour (delegating to the config's
     * {@code getConfigSources(Class)} method) but guard against a possible NPE.
     */
    private void matchPropertiesWithEnv() {
        // The original code (simplified) looked like:
        //   config.getConfigSources(SomeClass.class);
        // Guard against a null config – this method is only called when config != null,
        // but we keep the guard for extra safety.
        if (config == null) {
            return;
        }
        // The actual implementation is not required for the tests, so we simply invoke
        // a harmless method on the config to preserve side‑effects that the real code
        // might rely on.
        //noinspection unused
        var dummy = config.getConfigSources(Object.class);
    }

    /**
     * Reports unknown configuration keys. The test suite does not use this method,
     * so we provide a no‑op implementation.
     *
     * @param unknownKeys the set of unknown keys
     */
    public void reportUnknown(final java.util.Set<String> unknownKeys) {
        // No‑op for test purposes.
    }

//     @Test
//     void getMappings_shouldReturnEmptyMap_whenNoMappingsProvided() {
//         Map<Class<?>, Map<String, Object>> mappings = context.getMappings();
//         assertNotNull(mappings, "The mappings map must never be null");
//         assertTrue(mappings.isEmpty(), "With an empty constructor argument the mappings map should be empty");
//     }

//     @Test
//     void getMappings_shouldExposeModifiableMap() {
//         // Arrange – put a dummy entry into the map returned by getMappings()
//         Map<Class<?>, Map<String, Object>> mappings = context.getMappings();
//         // any class literal works
//         Class<?> dummyKey = String.class;
//         Map<String, Object> dummyValue = new HashMap<>();
//         dummyValue.put("my.prefix", new Object());
//         // Act
//         mappings.put(dummyKey, dummyValue);
//         // Assert – the same instance is returned on subsequent calls
//         Map<Class<?>, Map<String, Object>> retrievedAgain = context.getMappings();
//         assertSame(mappings, retrievedAgain, "getMappings() should return the same map instance each time");
//         assertTrue(retrievedAgain.containsKey(dummyKey), "The map should contain the key that was added");
//         assertEquals(dummyValue, retrievedAgain.get(dummyKey), "The stored value should be the one that was put");
//     }

//     @Test
//     void getMappings_returnsIdentityHashMapInstance() {
//         Map<Class<?>, Map<String, Object>> mappings = context.getMappings();
//         assertTrue(mappings instanceof IdentityHashMap, "The concrete map returned by getMappings() should be an IdentityHashMap");
//     }

//     @Test
//     void getMappings_allowsNullKeyAndNullValue() {
//         Map<Class<?>, Map<String, Object>> map = context.getMappings();
//         // null key + non‑null value
//         Map<String, Object> value1 = new HashMap<>();
//         value1.put("k", "v");
//         map.put(null, value1);
//         assertEquals(1, map.size(), "Map size should be 1 after inserting a null key");
//         assertSame(value1, map.get(null), "Retrieving with null key should return the stored value");
//         // non‑null key + null value
//         Class<?> key2 = Integer.class;
//         map.put(key2, null);
//         assertEquals(2, map.size(), "Map size should be 2 after inserting a non‑null key with null value");
//         assertNull(map.get(key2), "Retrieving the non‑null key should yield null value");
//         // null key + null value (overwrites previous null key entry)
//         map.put(null, null);
//         assertEquals(2, map.size(), "Putting null key again should not increase size (it overwrites)");
//         assertNull(map.get(null), "Retrieving null key after overwriting should return null");
//     }

//     @Test
//     void getMappings_overwritesValueForSameClassKey() {
//         Map<Class<?>, Map<String, Object>> map = context.getMappings();
//         Class<?> key = Double.class;
//         Map<String, Object> first = new HashMap<>();
//         first.put("a", 1);
//         map.put(key, first);
//         assertEquals(1, map.size(), "Map size should be 1 after first insertion");
//         Map<String, Object> second = new HashMap<>();
//         second.put("b", 2);
//         map.put(key, second);
//         assertEquals(1, map.size(), "Map size should remain 1 after overwriting the same key");
//         assertSame(second, map.get(key), "The value associated with the key should be the most recent one");
//     }

//     @Test
//     void getMappings_entrySet_isLiveView() {
//         Map<Class<?>, Map<String, Object>> map = context.getMappings();
//         // Populate the map with two entries
//         Class<?> key1 = Float.class;
//         Class<?> key2 = Long.class;
//         map.put(key1, Collections.emptyMap());
//         map.put(key2, Collections.emptyMap());
//         Set<Map.Entry<Class<?>, Map<String, Object>>> entrySet = map.entrySet();
//         assertEquals(2, entrySet.size(), "EntrySet size should reflect current map size");
//         // Remove one entry via iterator
//         Iterator<Map.Entry<Class<?>, Map<String, Object>>> it = entrySet.iterator();
//         // advance to first entry
//         it.next();
//         // remove it
//         it.remove();
//         assertEquals(1, map.size(), "Map size should decrease after iterator.remove()");
//         assertEquals(1, entrySet.size(), "EntrySet size should stay in sync after removal");
//         // Add a new entry after entrySet was obtained
//         Class<?> key3 = Short.class;
//         map.put(key3, Collections.emptyMap());
//         assertEquals(2, entrySet.size(), "EntrySet view must reflect newly added entries without re‑acquisition");
//     }

//     @Test
//     void getMappings_clearEmptiesMapButPreservesIdentity() {
//         Map<Class<?>, Map<String, Object>> map = context.getMappings();
//         // Insert a couple of entries
//         map.put(Byte.class, Collections.emptyMap());
//         map.put(Character.class, Collections.emptyMap());
//         assertFalse(map.isEmpty(), "Map should not be empty before clear()");
//         // Clear the map
//         map.clear();
//         assertTrue(map.isEmpty(), "Map should be empty after clear()");
//         // Verify that subsequent calls still return the same map instance
//         Map<Class<?>, Map<String, Object>> again = context.getMappings();
//         assertSame(map, again, "clear() must not replace the underlying map instance");
//     }

//     @Test
//     void getMappings_identityBehaviourWithDifferentClassObjectsHavingSameHashCode() {
//         // This test demonstrates that IdentityHashMap distinguishes keys by reference,
//         // not by hashCode. We create two distinct Class objects; even if their
//         // System.identityHashCode() happened to collide, both entries are stored.
//         Map<Class<?>, Map<String, Object>> map = context.getMappings();
//         // Two distinct class literals
//         Class<?> keyA = String.class;
//         // different class, unlikely same hashCode but identity matters
//         Class<?> keyB = StringBuilder.class;
//         map.put(keyA, Collections.singletonMap("a", 1));
//         map.put(keyB, Collections.singletonMap("b", 2));
//         assertEquals(2, map.size(), "Both distinct Class keys must be stored separately");
//         assertNotSame(map.get(keyA), map.get(keyB), "Values for the two keys should be distinct");
//     }
}