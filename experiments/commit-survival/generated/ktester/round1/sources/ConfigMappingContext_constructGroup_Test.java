package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import org.eclipse.microprofile.config.spi.Converter;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructGroup_Test {

    @Mock
    SmallRyeConfig // dependency required by the constructor
    mockConfig;

    /**
     * Instance created in {@link #setupBeforeEach()} and used in the tests.
     */
    ConfigMappingContext context;

    /**
     * Simple configuration interface used as the {@code interfaceType} argument.
     */
    interface TestConfig {

        String getValue();
    }

    /**
     * Simple configuration interface used as a generic placeholder.
     */
    interface DummyConfig {

        String getValue();
    }

    @BeforeEach
    void setupBeforeEach() {
        // The real constructor expects three arguments – we can safely pass empty maps for roots & names.
        Map<String, List<Class<?>>> emptyRoots = Collections.emptyMap();
        Map<String, Map<String, Set<String>>> emptyNames = Collections.emptyMap();
        context = new ConfigMappingContext(mockConfig, emptyRoots, emptyNames);
    }

    /**
     * Helper to set the private {@code namingStrategy} field via reflection.
     */
    private static void setNamingStrategy(ConfigMappingContext ctx, ConfigMapping.NamingStrategy strategy) throws Exception {
        Field f = ConfigMappingContext.class.getDeclaredField("namingStrategy");
        f.setAccessible(true);
        f.set(ctx, strategy);
    }

    /**
     * Helper to read the private {@code namingStrategy} field via reflection.
     */
    private static ConfigMapping.NamingStrategy getNamingStrategy(ConfigMappingContext ctx) throws Exception {
        Field f = ConfigMappingContext.class.getDeclaredField("namingStrategy");
        f.setAccessible(true);
        return (ConfigMapping.NamingStrategy) f.get(ctx);
    }

    @Test
    void constructGroup_returnsObjectFromLoader_andUpdatesNamingStrategy() {
        // ----------------------------------------------------------------------
        // GIVEN
        // ----------------------------------------------------------------------
        // 1. A mock NamingStrategy that will be returned by applyNamingStrategy(...)
        ConfigMapping.NamingStrategy mockNamingStrategy = mock(ConfigMapping.NamingStrategy.class);
        // 2. Spy the context so we can stub applyNamingStrategy while still executing the real
        //    constructGroup method.
        ConfigMappingContext spyContext = spy(context);
        doReturn(mockNamingStrategy).when(spyContext).applyNamingStrategy(any());
        // 3. The object that the static loader should return.
        TestConfig expectedMappingObject = mock(TestConfig.class);
        // 4. Mock the static method ConfigMappingLoader.configMappingObject(...)
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(TestConfig.class, spyContext)).thenReturn(expectedMappingObject);
            // ----------------------------------------------------------------------
            // WHEN – first invocation
            // ----------------------------------------------------------------------
            TestConfig actual = spyContext.constructGroup(TestConfig.class);
            // ----------------------------------------------------------------------
            // THEN – first invocation
            // ----------------------------------------------------------------------
            assertSame(expectedMappingObject, actual, "constructGroup must return the object produced by ConfigMappingLoader");
            // applyNamingStrategy should have been called with the original namingStrategy (null)
            verify(spyContext, times(1)).applyNamingStrategy(isNull());
            // The static loader should have been called exactly once with the supplied arguments.
            loaderMock.verify(() -> ConfigMappingLoader.configMappingObject(TestConfig.class, spyContext), times(1));
            // ----------------------------------------------------------------------
            // WHEN – second invocation (to verify that the namingStrategy field was updated)
            // ----------------------------------------------------------------------
            // Reset interactions on the spy for a clean verification of the second call.
            clearInvocations(spyContext);
            // reset static mock interactions
            loaderMock.reset();
            // Stub the loader to return a different instance for clarity.
            TestConfig secondMappingObject = mock(TestConfig.class);
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(TestConfig.class, spyContext)).thenReturn(secondMappingObject);
            TestConfig secondActual = spyContext.constructGroup(TestConfig.class);
            // ----------------------------------------------------------------------
            // THEN – second invocation
            // ----------------------------------------------------------------------
            assertSame(secondMappingObject, secondActual, "Second call should also return the object supplied by the static loader");
            // Now applyNamingStrategy must have been called with the namingStrategy that was stored
            // after the first call (i.e., mockNamingStrategy).
            verify(spyContext, times(1)).applyNamingStrategy(eq(mockNamingStrategy));
            loaderMock.verify(() -> ConfigMappingLoader.configMappingObject(TestConfig.class, spyContext), times(1));
        }
    }

    /* --------------------------------------------------------------
     *  Normal flow – namingStrategy is null
     * -------------------------------------------------------------- */
    @Test
    void constructGroup_nullNamingStrategy_updatesToSnakeCase() throws Exception {
        // GIVEN
        ConfigMapping.NamingStrategy snakeCase = ConfigMapping.NamingStrategy.SNAKE_CASE;
        ConfigMappingContext spy = spy(context);
        doReturn(snakeCase).when(spy).applyNamingStrategy(isNull());
        DummyConfig expected = mock(DummyConfig.class);
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyConfig.class, spy)).thenReturn(expected);
            // WHEN
            DummyConfig actual = spy.constructGroup(DummyConfig.class);
            // THEN
            assertSame(expected, actual, "Returned object must be the one from ConfigMappingLoader");
            verify(spy, times(1)).applyNamingStrategy(isNull());
            assertEquals(snakeCase, getNamingStrategy(spy), "namingStrategy field must be updated to the value returned by applyNamingStrategy");
        }
    }

    /* --------------------------------------------------------------
     *  NamingStrategy already set – non‑null
     * -------------------------------------------------------------- */
    @Test
    void constructGroup_nonNullNamingStrategy_isReplacedByLowerCase() throws Exception {
        // GIVEN
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        ConfigMapping.NamingStrategy lowerCase = ConfigMapping.NamingStrategy.LOWER_CASE;
        setNamingStrategy(context, initial);
        ConfigMappingContext spy = spy(context);
        doReturn(lowerCase).when(spy).applyNamingStrategy(eq(initial));
        DummyConfig expected = mock(DummyConfig.class);
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyConfig.class, spy)).thenReturn(expected);
            // WHEN
            DummyConfig actual = spy.constructGroup(DummyConfig.class);
            // THEN
            assertSame(expected, actual);
            verify(spy, times(1)).applyNamingStrategy(eq(initial));
            assertEquals(lowerCase, getNamingStrategy(spy), "namingStrategy must be replaced by the value returned from applyNamingStrategy");
        }
    }

    /* --------------------------------------------------------------
     *  applyNamingStrategy returns a different instance (CUSTOM)
     * -------------------------------------------------------------- */
    @Test
    void constructGroup_applyNamingStrategyReturnsCustom_updatesField() throws Exception {
        // GIVEN
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        ConfigMapping.NamingStrategy custom = ConfigMapping.NamingStrategy.CUSTOM;
        setNamingStrategy(context, initial);
        ConfigMappingContext spy = spy(context);
        doReturn(custom).when(spy).applyNamingStrategy(eq(initial));
        DummyConfig expected = mock(DummyConfig.class);
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyConfig.class, spy)).thenReturn(expected);
            // WHEN
            DummyConfig actual = spy.constructGroup(DummyConfig.class);
            // THEN
            assertSame(expected, actual);
            verify(spy, times(1)).applyNamingStrategy(eq(initial));
            assertEquals(custom, getNamingStrategy(spy), "namingStrategy field must be set to the custom strategy returned");
        }
    }

    /* --------------------------------------------------------------
     *  applyNamingStrategy returns null
     * -------------------------------------------------------------- */
    @Test
    void constructGroup_applyNamingStrategyReturnsNull_clearsField() throws Exception {
        // GIVEN
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        setNamingStrategy(context, initial);
        ConfigMappingContext spy = spy(context);
        doReturn(null).when(spy).applyNamingStrategy(eq(initial));
        DummyConfig expected = mock(DummyConfig.class);
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyConfig.class, spy)).thenReturn(expected);
            // WHEN
            DummyConfig actual = spy.constructGroup(DummyConfig.class);
            // THEN
            assertSame(expected, actual);
            verify(spy, times(1)).applyNamingStrategy(eq(initial));
            assertNull(getNamingStrategy(spy), "namingStrategy field must be cleared (null) when applyNamingStrategy returns null");
        }
    }

    /* --------------------------------------------------------------
     *  ConfigMappingLoader throws RuntimeException
     * -------------------------------------------------------------- */
    @Test
    void constructGroup_loaderThrowsRuntimeException_propagatesAndKeepsNamingStrategy() throws Exception {
        // GIVEN
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        setNamingStrategy(context, initial);
        ConfigMappingContext spy = spy(context);
        // not reached, but safe
        doReturn(initial).when(spy).applyNamingStrategy(any());
        RuntimeException simulated = new RuntimeException("simulated loader failure");
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyConfig.class, spy)).thenThrow(simulated);
            // WHEN / THEN
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> spy.constructGroup(DummyConfig.class), "Exception from ConfigMappingLoader must propagate");
            assertSame(simulated, thrown);
            // namingStrategy must stay unchanged
            assertEquals(initial, getNamingStrategy(spy));
        }
    }

    /* --------------------------------------------------------------
     *  interfaceType is null – NPE from loader
     * -------------------------------------------------------------- */
    @Test
    void constructGroup_nullInterfaceType_propagatesNullPointerException() throws Exception {
        // GIVEN
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        setNamingStrategy(context, initial);
        ConfigMappingContext spy = spy(context);
        doReturn(initial).when(spy).applyNamingStrategy(any());
        NullPointerException simulated = new NullPointerException("interfaceType is null");
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(null, spy)).thenThrow(simulated);
            // WHEN / THEN
            NullPointerException thrown = assertThrows(NullPointerException.class, () -> spy.constructGroup(null), "Null interfaceType should result in NPE");
            assertSame(simulated, thrown);
            // namingStrategy unchanged
            assertEquals(initial, getNamingStrategy(spy));
        }
    }

    /* --------------------------------------------------------------
     *  applyNamingStrategy throws RuntimeException
     * -------------------------------------------------------------- */
    @Test
    void constructGroup_applyNamingStrategyThrowsRuntimeException_propagates() throws Exception {
        // GIVEN
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        setNamingStrategy(context, initial);
        ConfigMappingContext spy = spy(context);
        RuntimeException simulated = new RuntimeException("simulated applyNamingStrategy failure");
        doThrow(simulated).when(spy).applyNamingStrategy(eq(initial));
        DummyConfig expected = mock(DummyConfig.class);
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyConfig.class, spy)).thenReturn(expected);
            // WHEN / THEN
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> spy.constructGroup(DummyConfig.class), "RuntimeException from applyNamingStrategy must propagate");
            assertSame(simulated, thrown);
            // namingStrategy must stay as it was before the call
            assertEquals(initial, getNamingStrategy(spy));
        }
    }

    /* --------------------------------------------------------------
     *  applyNamingStrategy throws IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void constructGroup_applyNamingStrategyThrowsIllegalArgumentException_propagates() throws Exception {
        // GIVEN
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        setNamingStrategy(context, initial);
        ConfigMappingContext spy = spy(context);
        IllegalArgumentException simulated = new IllegalArgumentException("unsupported naming strategy");
        doThrow(simulated).when(spy).applyNamingStrategy(eq(initial));
        DummyConfig expected = mock(DummyConfig.class);
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyConfig.class, spy)).thenReturn(expected);
            // WHEN / THEN
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> spy.constructGroup(DummyConfig.class), "IllegalArgumentException from applyNamingStrategy must propagate");
            assertSame(simulated, thrown);
            assertEquals(initial, getNamingStrategy(spy));
        }
    }

    /* --------------------------------------------------------------
     *  applyNamingStrategy throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void constructGroup_applyNamingStrategyThrowsNullPointerException_propagates() throws Exception {
        // GIVEN
        ConfigMapping.NamingStrategy initial = ConfigMapping.NamingStrategy.SNAKE_CASE;
        setNamingStrategy(context, initial);
        ConfigMappingContext spy = spy(context);
        NullPointerException simulated = new NullPointerException("namingStrategy is null inside applyNamingStrategy");
        doThrow(simulated).when(spy).applyNamingStrategy(eq(initial));
        DummyConfig expected = mock(DummyConfig.class);
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class)) {
            loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyConfig.class, spy)).thenReturn(expected);
            // WHEN / THEN
            NullPointerException thrown = assertThrows(NullPointerException.class, () -> spy.constructGroup(DummyConfig.class), "NullPointerException from applyNamingStrategy must propagate");
            assertSame(simulated, thrown);
            assertEquals(initial, getNamingStrategy(spy));
        }
    }
}
