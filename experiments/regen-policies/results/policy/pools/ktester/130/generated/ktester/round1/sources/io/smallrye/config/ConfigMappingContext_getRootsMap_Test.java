package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class ConfigMappingContext_getRootsMap_Test {

    private static SmallRyeConfig config;

    /**
     * Helper interface used as a dummy root for construction tests.
     */
    interface DummyRoot {
        // marker interface
    }

    /**
     * Helper interface used as a dummy group for construction tests.
     */
    interface DummyGroup {
        // marker interface
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Build a minimal SmallRyeConfig – no sources, no mappings.
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        config = new SmallRyeConfig(builder);
    }

    @AfterAll
    static void teardownAfterAll() {
        config = null;
    }

    /**
     * Utility to create a roots definition map that contains {@link DummyRoot}.
     */
    private static Map<Class<?>, Set<String>> rootsWithDummyRoot() {
        // The value (the Set) is not used by the tests – an empty set is sufficient.
        return Collections.singletonMap(DummyRoot.class, Collections.emptySet());
    }

    @Test
    void getRootsMapShouldReturnEmptyMapWhenNoRootsProvided() {
        // Given: an empty roots definition
        Map<Class<?>, Set<String>> emptyRoots = Collections.emptyMap();
        // When: the context is created
        ConfigMappingContext context = new ConfigMappingContext(config, emptyRoots);
        // Then: the internal roots map is empty and the same instance is returned
        Map<Class<?>, Map<String, ConfigMappingObject>> rootsMap = context.getRootsMap();
        assertNotNull(rootsMap, "The roots map must never be null");
        assertTrue(rootsMap.isEmpty(), "The roots map should be empty when no roots are supplied");
    }

    @Test
    void getRootsMapReturnsSameInstanceOnRepeatedCalls() {
        ConfigMappingContext context = new ConfigMappingContext(config, Collections.emptyMap());
        Map<Class<?>, Map<String, ConfigMappingObject>> first = context.getRootsMap();
        Map<Class<?>, Map<String, ConfigMappingObject>> second = context.getRootsMap();
        assertSame(first, second, "Two calls to getRootsMap() must return the exact same object reference");
    }

    @Test
    void externalModificationOfReturnedMapIsReflectedInternally() {
        ConfigMappingContext context = new ConfigMappingContext(config, Collections.emptyMap());
        Map<Class<?>, Map<String, ConfigMappingObject>> rootsMap = context.getRootsMap();
        // Insert a dummy entry directly
        rootsMap.put(DummyRoot.class, new HashMap<>());
        // Retrieve again and verify the change is visible
        Map<Class<?>, Map<String, ConfigMappingObject>> after = context.getRootsMap();
        assertEquals(1, after.size(), "Map size should be 1 after external insertion");
        assertTrue(after.containsKey(DummyRoot.class), "Map must contain the key that was inserted externally");
    }

    @Test
    void constructRootRegistersEntryInRootsMap() {
        // Provide a roots definition that includes DummyRoot so that constructRoot becomes effective.
        ConfigMappingContext context = new ConfigMappingContext(config, rootsWithDummyRoot());
        // The method under test – it should create a root entry
        context.constructRoot(DummyRoot.class);
        Map<Class<?>, Map<String, ConfigMappingObject>> rootsMap = context.getRootsMap();
        assertEquals(1, rootsMap.size(), "After constructing a root, the roots map should contain exactly one entry");
        assertTrue(rootsMap.containsKey(DummyRoot.class), "The roots map must contain the DummyRoot class as a key");
        // The inner map should be non‑null (it will hold group paths)
        assertNotNull(rootsMap.get(DummyRoot.class), "Inner map for the root must be instantiated");
    }

    @Test
    void constructGroupAddsEntryUnderExistingRoot() {
        // Provide a roots definition that includes DummyRoot.
        ConfigMappingContext context = new ConfigMappingContext(config, rootsWithDummyRoot());
        // First create the root – this should add a top‑level entry
        context.constructRoot(DummyRoot.class);
        // Now construct a group; the implementation should place the group inside the root's inner map
        context.constructGroup(DummyGroup.class);
        Map<Class<?>, Map<String, ConfigMappingObject>> rootsMap = context.getRootsMap();
        // The top‑level map must still contain only the root entry
        assertEquals(1, rootsMap.size(), "Root map size must remain 1 after constructing a group");
        assertTrue(rootsMap.containsKey(DummyRoot.class), "Root entry must still be present");
        // The inner map for the root should now contain an entry for the group path.
        Map<String, ConfigMappingObject> inner = rootsMap.get(DummyRoot.class);
        assertNotNull(inner, "Inner map for the root must not be null");
        // The exact key used for groups is implementation‑specific; we only assert that the inner map is non‑empty.
        assertFalse(inner.isEmpty(), "Inner map should contain at least one entry after constructing a group");
    }
}
