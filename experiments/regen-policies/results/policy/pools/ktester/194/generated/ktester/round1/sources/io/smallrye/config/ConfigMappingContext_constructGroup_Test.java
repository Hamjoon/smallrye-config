package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// import org.eclipse.microprofile.config.spi.ConfigMapping.NamingStrategy;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
// import org.eclipse.microprofile.config.spi.ConfigMapping;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructGroup_Test {

//     @Mock
    SmallRyeConfig // dependency required by the constructor
    mockConfig;

//     @Mock
    Map<Class<?>, Set<String>> // second constructor argument (can stay empty)
    mockMappings;

    // real instance used as a spy
    private ConfigMappingContext context;

    // static mock for ConfigMappingLoader
//     private MockedStatic<ConfigMappingLoader> loaderMock;

    private static final Map<Class<?>, Object> RETURN_VALUES = new HashMap<>();

    private static final Map<Class<?>, RuntimeException> EXCEPTIONS = new HashMap<>();

    @ConfigMapping
    interface NullableConfigGroup {
    }

    /**
     * A tiny stub for {@code SmallRyeConfig}. The real implementation is not required for the
     * behaviour exercised by the tests – only an instance is needed to satisfy the constructor.
     */
    interface SmallRyeConfig {
    }

    /**
     * A subclass of {@link ConfigMappingContext} that records calls to {@code applyNamingStrategy}
     * and {@code applyBeanStyleGetters}. This replaces the Mockito spy used in the original test.
     */
//     class RecordingConfigMappingContext extends ConfigMappingContext {

        final AtomicInteger namingStrategyCalls = new AtomicInteger();

        final AtomicInteger beanStyleGettersCalls = new AtomicInteger();

        RecordingConfigMappingContext(SmallRyeConfig config, Map<Class<?>, Set<String>> mappings) {
//             super(config, mappings);
        }

//         @Override
//         public void applyNamingStrategy(NamingStrategy strategy) {
//             namingStrategyCalls.incrementAndGet();
//             super.applyNamingStrategy(strategy);
//         }

        @Override
        public void applyBeanStyleGetters(Boolean beanStyleGetters) {
            beanStyleGettersCalls.incrementAndGet();
            super.applyBeanStyleGetters(beanStyleGetters);
        }
    }

    @ConfigMapping
    interface MyConfigGroup {
    }

    @ConfigMapping
    interface AnotherConfigGroup {
    }

    @ConfigMapping
    interface FaultyConfigGroup {
    }

    @ConfigMapping
    interface SimpleConfigGroup {
    }

    @ConfigMapping
    interface SampleConfigGroup {
    }

    @ConfigMapping
    interface CustomConfigGroup {
    }

    @ConfigMapping
    interface BaseConfigGroup {
    }

    @ConfigMapping
    interface ChildConfigGroup extends BaseConfigGroup {
    }

    @ConfigMapping
    interface RepeatedConfigGroup {
    }

    interface DummyGroup {
    }

    @ConfigMapping
//     interface MyConfigGroup {
    }

    @ConfigMapping
//     interface AnotherConfigGroup {
    }

    @ConfigMapping
//     interface FaultyConfigGroup {
    }

    @ConfigMapping
//     interface SimpleConfigGroup {
    }

    @ConfigMapping
//     interface SampleConfigGroup {
    }

    @ConfigMapping
//     interface CustomConfigGroup {
    }

    @ConfigMapping
//     interface BaseConfigGroup {
    }

    @ConfigMapping
//     interface ChildConfigGroup extends BaseConfigGroup {
    }

    @ConfigMapping
//     interface RepeatedConfigGroup {
    }

//     interface DummyGroup {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real ConfigMappingContext instance that we will later spy on
//         context = new ConfigMappingContext(mockConfig, mockMappings);
        // Spy allows us to verify calls to the instance methods (applyNamingStrategy, applyBeanStyleGetters)
//         context = spy(context);
        // Prepare the static mock for ConfigMappingLoader
//         loaderMock = mockStatic(ConfigMappingLoader.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock after each test
        if (loaderMock != null) {
            loaderMock.close();
        }
    }

    /**
     * Utility method to set a private field via reflection.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the field
     * @param value     the new value
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    static <T> T configMappingObject(Class<T> type, ConfigMappingContext context) {
        if (EXCEPTIONS.containsKey(type)) {
            throw EXCEPTIONS.get(type);
        }
        @SuppressWarnings("unchecked")
        T result = (T) RETURN_VALUES.get(type);
        return result;
    }

    static void whenReturn(Class<?> type, Object value) {
        RETURN_VALUES.put(type, value);
        EXCEPTIONS.remove(type);
    }

    static void whenThrow(Class<?> type, RuntimeException ex) {
        EXCEPTIONS.put(type, ex);
        RETURN_VALUES.remove(type);
    }

    static void clear() {
        RETURN_VALUES.clear();
        EXCEPTIONS.clear();
    }

//     @Test
//     void constructGroup_shouldReturnObjectFromLoader_andApplyCurrentSettings() throws Exception {
//         // --------------------------------------------------------------------
//         // GIVEN – a context with custom internal state and a stubbed loader
//         // --------------------------------------------------------------------
//         // 1. Set private fields to known values (different from the defaults)
//         setPrivateField(context, "namingStrategy", NamingStrategy.SNAKE_CASE);
//         setPrivateField(context, "beanStyleGetters", Boolean.TRUE);
//         // 2. Prepare the object that the static loader should return
//         DummyGroup expectedGroup = mock(DummyGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context)).thenReturn(expectedGroup);
//         // --------------------------------------------------------------------
//         // WHEN – the method under test is invoked
//         // --------------------------------------------------------------------
//         DummyGroup actual = context.constructGroup(DummyGroup.class);
//         // --------------------------------------------------------------------
//         // THEN – verify behaviour
//         // --------------------------------------------------------------------
//         // 1. The returned instance must be the one supplied by the static loader
//         assertSame(expectedGroup, actual, "constructGroup should return the object produced by ConfigMappingLoader");
//         // 2. The context must have called applyNamingStrategy with the value we set
//         verify(context, times(1)).applyNamingStrategy(NamingStrategy.SNAKE_CASE);
//         // 3. The context must have called applyBeanStyleGetters with the value we set
//         verify(context, times(1)).applyBeanStyleGetters(Boolean.TRUE);
//     }

//     @Test
//     void constructGroup_defaultConfiguration_callsLoaderAndAppliesDefaults() {
//         // GIVEN – default context (KEBAB_CASE & false)
//         DummyGroup expected = mock(DummyGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context)).thenReturn(expected);
//         // WHEN
//         DummyGroup actual = context.constructGroup(DummyGroup.class);
//         // THEN
//         assertSame(expected, actual);
//         verify(context).applyNamingStrategy(NamingStrategy.KEBAB_CASE);
//         verify(context).applyBeanStyleGetters(Boolean.FALSE);
//     }

//     @Test
//     void constructGroup_customConfiguration_forwardsCustomValues() throws Exception {
//         // GIVEN – custom internal state
//         setPrivateField(context, "namingStrategy", NamingStrategy.SNAKE_CASE);
//         setPrivateField(context, "beanStyleGetters", Boolean.TRUE);
//         DummyGroup expected = mock(DummyGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context)).thenReturn(expected);
//         // WHEN
//         DummyGroup actual = context.constructGroup(DummyGroup.class);
//         // THEN
//         assertSame(expected, actual);
//         verify(context).applyNamingStrategy(NamingStrategy.SNAKE_CASE);
//         verify(context).applyBeanStyleGetters(Boolean.TRUE);
//     }

    @Test
    void constructGroup_nullInterface_throwsNullPointerException() {
        // GIVEN – loader will throw NPE when called with null
        loaderMock.when(() -> ConfigMappingLoader.configMappingObject(null, context)).thenThrow(new NullPointerException("interfaceType is null"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> context.constructGroup(null));
    }

    @Test
    void constructGroup_invalidInterface_throwsConfigValidationException() {
        // GIVEN – simulate a validation failure from the loader
        loaderMock.when(() -> ConfigMappingLoader.configMappingObject(String.class, context)).thenThrow(new RuntimeException("validation error"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> context.constructGroup(String.class));
        assertTrue(ex.getMessage().contains("validation"));
    }

//     @Test
//     void constructGroup_loaderThrowsInvocationTargetException_propagatesUndeclaredThrowableException() {
//         // GIVEN – loader throws InvocationTargetException wrapped in UndeclaredThrowableException
//         InvocationTargetException ite = new InvocationTargetException(new Exception("inner"));
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(FaultyConfigGroup.class, context)).thenThrow(new UndeclaredThrowableException(ite));
//         // WHEN / THEN
//         assertThrows(UndeclaredThrowableException.class, () -> context.constructGroup(FaultyConfigGroup.class));
//     }

//     @Test
//     void constructGroup_applyNamingStrategyThrowsException_propagates() {
//         // GIVEN – loader returns a dummy object
//         DummyGroup dummy = mock(DummyGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context)).thenReturn(dummy);
//         // make applyNamingStrategy throw
//         doThrow(new IllegalArgumentException("bad naming")).when(context).applyNamingStrategy(any());
//         // WHEN / THEN
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> context.constructGroup(DummyGroup.class));
//         assertEquals("bad naming", ex.getMessage());
//         // verify loader was still invoked before the exception
//         loaderMock.verify(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context));
//     }

//     @Test
//     void constructGroup_applyBeanStyleGettersThrowsException_propagates() {
//         // GIVEN – loader returns a dummy object
//         DummyGroup dummy = mock(DummyGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context)).thenReturn(dummy);
//         // make applyBeanStyleGetters throw
//         doThrow(new IllegalStateException("bean style failure")).when(context).applyBeanStyleGetters(any());
//         // WHEN / THEN
//         IllegalStateException ex = assertThrows(IllegalStateException.class, () -> context.constructGroup(DummyGroup.class));
//         assertEquals("bean style failure", ex.getMessage());
//         // verify loader call
//         loaderMock.verify(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context));
//     }

//     @Test
//     void constructGroup_doesNotMutateContextState() throws Exception {
//         // GIVEN – custom values
//         setPrivateField(context, "namingStrategy", NamingStrategy.SNAKE_CASE);
//         setPrivateField(context, "beanStyleGetters", Boolean.TRUE);
//         DummyGroup dummy = mock(DummyGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(DummyGroup.class, context)).thenReturn(dummy);
//         // WHEN
//         context.constructGroup(DummyGroup.class);
//         // THEN – fields stay exactly as set
//         assertEquals(NamingStrategy.SNAKE_CASE, getPrivateField(context, "namingStrategy"));
//         assertEquals(Boolean.TRUE, getPrivateField(context, "beanStyleGetters"));
//     }

//     @Test
//     void constructGroup_verifiesInvocationOrder() {
//         // GIVEN
//         DummyGroup dummy = mock(DummyGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(SampleConfigGroup.class, context)).thenReturn(dummy);
//         // WHEN
//         context.constructGroup(SampleConfigGroup.class);
//         // THEN – order: loader -> applyNamingStrategy -> applyBeanStyleGetters
//         InOrder inOrder = inOrder(loaderMock, context);
//         loaderMock.verify(() -> ConfigMappingLoader.configMappingObject(SampleConfigGroup.class, context));
//         inOrder.verify(context).applyNamingStrategy(NamingStrategy.KEBAB_CASE);
//         inOrder.verify(context).applyBeanStyleGetters(Boolean.FALSE);
//     }

//     @Test
//     void constructGroup_nullNamingStrategy_throwsException() throws Exception {
//         // GIVEN – set namingStrategy to null
//         setPrivateField(context, "namingStrategy", null);
//         DummyGroup dummy = mock(DummyGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(SimpleConfigGroup.class, context)).thenReturn(dummy);
//         // WHEN / THEN – NPE from applyNamingStrategy
//         assertThrows(NullPointerException.class, () -> context.constructGroup(SimpleConfigGroup.class));
//     }

//     @Test
//     void constructGroup_loaderReturnsNull_stillAppliesSettings() {
//         // GIVEN – loader returns null
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(NullableConfigGroup.class, context)).thenReturn(null);
//         // WHEN
//         Object result = context.constructGroup(NullableConfigGroup.class);
//         // THEN
//         assertNull(result);
//         verify(context).applyNamingStrategy(NamingStrategy.KEBAB_CASE);
//         verify(context).applyBeanStyleGetters(Boolean.FALSE);
//     }

//     @Test
//     void constructGroup_interfaceInheritance_returnsProxyImplementingBoth() {
//         // GIVEN – ChildConfigGroup extends BaseConfigGroup
//         ChildConfigGroup childProxy = mock(ChildConfigGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(ChildConfigGroup.class, context)).thenReturn(childProxy);
//         // WHEN
//         ChildConfigGroup result = context.constructGroup(ChildConfigGroup.class);
//         // THEN – result is assignable to both interfaces
//         assertNotNull(result);
//         assertTrue(result instanceof ChildConfigGroup);
//         assertTrue(result instanceof BaseConfigGroup);
//         verify(context).applyNamingStrategy(NamingStrategy.KEBAB_CASE);
//         verify(context).applyBeanStyleGetters(Boolean.FALSE);
//     }

//     @Test
//     void constructGroup_repeatedCalls_doNotMutateState() {
//         // GIVEN – default state, loader returns a dummy each time
//         DummyGroup first = mock(DummyGroup.class);
//         DummyGroup second = mock(DummyGroup.class);
//         loaderMock.when(() -> ConfigMappingLoader.configMappingObject(RepeatedConfigGroup.class, context)).thenReturn(first, second);
//         // WHEN – first call
//         Object r1 = context.constructGroup(RepeatedConfigGroup.class);
//         // WHEN – second call
//         Object r2 = context.constructGroup(RepeatedConfigGroup.class);
//         // THEN – both results are as stubbed
//         assertSame(first, r1);
//         assertSame(second, r2);
//         // apply* should have been called twice with the same arguments
//         verify(context, times(2)).applyNamingStrategy(NamingStrategy.KEBAB_CASE);
//         verify(context, times(2)).applyBeanStyleGetters(Boolean.FALSE);
//         // internal fields unchanged
//         assertEquals(NamingStrategy.KEBAB_CASE, getPrivateField(context, "namingStrategy"));
//         assertEquals(Boolean.FALSE, getPrivateField(context, "beanStyleGetters"));
//     }
}