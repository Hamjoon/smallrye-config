package io.smallrye.config;

import static io.smallrye.config.ConfigMappings.registerConfigMappings;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit5.PowerMockExtension;

@ExtendWith(MockitoExtension.class)
@PrepareForTest(ConfigMappings.class)
class ConfigMappings_registerConfigMappings_Test {

    /**
     * Shared resources that can be reused across all tests.
     */
    private static SmallRyeConfigBuilder builder;

    /**
     * Instance created for each test method.
     */
    private SmallRyeConfig config;

    /**
     * A dummy {@link SmallRyeConfig} – its behaviour is irrelevant because the private method is mocked.
     */
    @Mock
    SmallRyeConfig dummyConfig;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a builder that can be reused – it will be cloned for each test.
        builder = new SmallRyeConfigBuilder().withConverter(Version.class, 100, new VersionConverter());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh config built from the shared builder.
        config = builder.clone().withSources(config(// Minimal set of properties required for the Server mapping used below
        "server.host", "localhost", "server.port", "8080")).build();
        // Spy on the whole ConfigMappings class so that we can intercept the private static method.
        PowerMockito.spy(ConfigMappings.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up; method present for symmetry and future extensions.
        config = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared builder reference.
        builder = null;
    }

    /**
     * Helper to create a mock {@link ConfigClass}.
     */
    private ConfigMappings.ConfigClass mockConfigClass() {
        // The static factory method {@code configClass} creates a real instance; we only need a mock.
        return mock(ConfigMappings.ConfigClass.class);
    }

    /**
     * Verify that the private {@code mapConfiguration} method was never called.
     */
    private void verifyMapConfigurationNeverCalled() throws Exception {
        PowerMockito.verifyPrivate(ConfigMappings.class, never()).invoke("mapConfiguration", any(), any(), any());
    }

    /**
     * Verify that the private {@code mapConfiguration} method was called exactly once with the
     *  supplied arguments (the arguments themselves are not asserted – only the call count).
     */
    private void verifyMapConfigurationCalledOnce() throws Exception {
        PowerMockito.verifyPrivate(ConfigMappings.class, times(1)).invoke("mapConfiguration", any(), any(), any());
    }

    @Test
    void shouldRegisterConfigMappingsWhenConfigClassesNotEmpty() throws ConfigValidationException {
        // GIVEN a non‑empty set of ConfigClass objects (Server mapping)
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(Server.class, "server"));
        // WHEN registerConfigMappings is invoked
        registerConfigMappings(config, classes);
        // THEN the mapping is available via SmallRyeConfig#getConfigMapping
        Server server = config.getConfigMapping(Server.class);
        assertNotNull(server, "Server mapping should be registered");
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
    }

    @Test
    void shouldDoNothingWhenConfigClassesIsEmpty() throws ConfigValidationException {
        // GIVEN an empty set
        Set<ConfigMappings.ConfigClass> empty = Set.of();
        // WHEN registerConfigMappings is called – it should simply return without exception
        registerConfigMappings(config, empty);
        // THEN no mapping for Server exists
        assertThrows(IllegalArgumentException.class, () -> config.getConfigMapping(Server.class), "No mapping should have been registered");
    }

    @Test
    void shouldThrowValidationExceptionForUnmappedProperties() {
        // GIVEN a config that contains a property not covered by the mapping
        SmallRyeConfig invalidConfig = builder.clone().withSources(config("server.host", "localhost", "server.port", "8080", "server.unmapped", "value")).build();
        Set<ConfigMappings.ConfigClass> classes = singleton(configClass(Server.class, "server"));
        // WHEN registerConfigMappings is invoked
        // THEN a ConfigValidationException is thrown because of the unmapped property
        assertThrows(ConfigValidationException.class, () -> registerConfigMappings(invalidConfig, classes));
    }

    @Test
    void registerConfigMappings_emptyConfigClasses_doesNothing() throws Exception {
        // GIVEN
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
        // WHEN
        registerConfigMappings(dummyConfig, empty);
        // THEN – method returns normally and private mapConfiguration is never invoked
        verifyMapConfigurationNeverCalled();
    }

    @Test
    void registerConfigMappings_nonEmptyConfigClasses_invokesMapConfigurationOnce() throws Exception {
        // GIVEN
        ConfigMappings.ConfigClass cc = mockConfigClass();
        Set<ConfigMappings.ConfigClass> classes = new HashSet<>(Collections.singletonList(cc));
        // Stub the private method to do nothing (real implementation is not required for this test)
        PowerMockito.doNothing().when(ConfigMappings.class, "mapConfiguration", any(SmallRyeConfig.class), any(SmallRyeConfigBuilder.class), anySet());
        // WHEN
        registerConfigMappings(dummyConfig, classes);
        // THEN – mapConfiguration must be called exactly once
        verifyMapConfigurationCalledOnce();
    }

    @Test
    void registerConfigMappings_mapConfigurationThrows_propagatesException() throws Exception {
        // GIVEN
        ConfigMappings.ConfigClass cc = mockConfigClass();
        Set<ConfigMappings.ConfigClass> classes = new HashSet<>(Collections.singletonList(cc));
        // Make the private method throw a ConfigValidationException
        PowerMockito.doThrow(new ConfigValidationException("validation failed")).when(ConfigMappings.class, "mapConfiguration", any(SmallRyeConfig.class), any(SmallRyeConfigBuilder.class), anySet());
        // WHEN / THEN – the exception must bubble up unchanged
        ConfigValidationException thrown = assertThrows(ConfigValidationException.class, () -> registerConfigMappings(dummyConfig, classes));
        assertEquals("validation failed", thrown.getMessage());
        // Verify that the private method was indeed invoked (once)
        verifyMapConfigurationCalledOnce();
    }

    @Test
    void registerConfigMappings_nullConfig_throwsNullPointerException() {
        // GIVEN
        ConfigMappings.ConfigClass cc = mockConfigClass();
        Set<ConfigMappings.ConfigClass> classes = new HashSet<>(Collections.singletonList(cc));
        // WHEN / THEN – a NullPointerException is expected because the method will forward the null
        // to the private mapConfiguration (or attempt to call isEmpty on the set, which is safe).
        assertThrows(NullPointerException.class, () -> registerConfigMappings(null, classes));
    }

    @Test
    void registerConfigMappings_multipleConfigClasses_invokesMapConfigurationOnce() throws Exception {
        // GIVEN
        Set<ConfigMappings.ConfigClass> classes = new HashSet<>(Arrays.asList(mockConfigClass(), mockConfigClass(), mockConfigClass()));
        // Stub private method – we only care about the invocation count
        PowerMockito.doNothing().when(ConfigMappings.class, "mapConfiguration", any(SmallRyeConfig.class), any(SmallRyeConfigBuilder.class), anySet());
        // WHEN
        registerConfigMappings(dummyConfig, classes);
        // THEN – mapConfiguration is called once with the whole set (size 3)
        verifyMapConfigurationCalledOnce();
        // Additional sanity check: ensure the set passed to the private method has the expected size.
        // Capture the argument using a Mockito ArgumentCaptor.
        var captor = ArgumentCaptor.forClass(Set.class);
        PowerMockito.verifyPrivate(ConfigMappings.class, times(1)).invoke(eq("mapConfiguration"), any(SmallRyeConfig.class), any(SmallRyeConfigBuilder.class), captor.capture());
        @SuppressWarnings("unchecked")
        Set<ConfigMappings.ConfigClass> capturedSet = (Set<ConfigMappings.ConfigClass>) captor.getValue();
        assertEquals(3, capturedSet.size(), "The set passed to mapConfiguration must contain all three ConfigClass instances");
    }

    @Test
    void registerConfigMappings_configClassesContainsNullElement_throwsNullPointerException() {
        // GIVEN a set that contains a null element
        Set<ConfigMappings.ConfigClass> classes = new HashSet<>(Arrays.asList(mockConfigClass(), null));
        // WHEN / THEN – NullPointerException should be thrown (either by this method or by the
        // delegated private method). The private method must not be invoked.
        assertThrows(NullPointerException.class, () -> registerConfigMappings(dummyConfig, classes));
        // Verify that mapConfiguration was never called
        try {
            verifyMapConfigurationNeverCalled();
        } catch (Exception ignored) {
            // If reflection fails, the test has already failed via the assertion above.
        }
    }

    @Test
    void registerConfigMappings_nullConfigClasses_throwsNullPointerException() {
        // GIVEN a null Set
        Set<ConfigMappings.ConfigClass> nullSet = null;
        // WHEN / THEN – NullPointerException should be thrown before any internal call
        assertThrows(NullPointerException.class, () -> registerConfigMappings(dummyConfig, nullSet));
        // Ensure the private method was never invoked
        try {
            verifyMapConfigurationNeverCalled();
        } catch (Exception ignored) {
        }
    }

    @Test
    void registerConfigMappings_nullConfigWithEmptyConfigClasses_completesNormally() throws Exception {
        // GIVEN an empty set and a null config
        Set<ConfigMappings.ConfigClass> empty = Collections.emptySet();
        // WHEN – should complete without throwing and without invoking mapConfiguration
        registerConfigMappings(null, empty);
        // THEN – verify private method never called
        verifyMapConfigurationNeverCalled();
    }
}
