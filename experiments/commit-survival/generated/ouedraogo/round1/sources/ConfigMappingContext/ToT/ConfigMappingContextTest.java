/**
 * Comprehensive JUnit‑4 test suite for {@link io.smallrye.config.ConfigMappingContext}.
 *
 * <p>The tests are written without any external mocking framework – only concrete classes
 * and minimal stubs that are defined in this file are used.  All stubs are placed in the
 * same package {@code io.smallrye.config} so that the production class can access them
 * (e.g. {@code ConfigMappingLoader}, {@code ConfigMessages}, {@code ConfigValue},
 * {@code SimpleConverter} etc.).  The goal is to exercise every public method,
 * covering normal operation, edge cases and error handling.</p>
 *
 * <p>Because the original library uses many other classes that are not part of the
 * supplied source, lightweight replacements are provided.  They implement only the
 * members that the class under test touches.</p>
 */
package io.smallrye.config;
import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigValidationException.Problem;


import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.*;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Before;
import org.junit.Test;

/* -------------------------------------------------------------------------- */
/* ---- Minimal stubs required for the production class to compile/run ----- */
/* -------------------------------------------------------------------------- */

/**
 * Very small {@link ConfigValue} implementation – only the methods used by
 * {@code ConfigMappingContext.reportUnknown} are required.
 */
final class SimpleConfigValue implements ConfigValue {
    private final String sourceName;
    private final String location;

    SimpleConfigValue(String sourceName, String location) {
        this.sourceName = sourceName;
        this.location = location;
    }

    @Override
    public String getName() { return null; }

    @Override
    public String getValue() { return null; }

    @Override
    public String getRawValue() { return null; }

    @Override
    public String getSourceName() { return sourceName; }

    @Override
    public ConfigSource getConfigSource() { return null; }

    @Override
    public String getLocation() { return location; }

    @Override
    public boolean isSecret() { return false; }

    @Override
    public String toString() { return sourceName + ":" + location; }
}

/**
 * Simple {@link Converter} that either returns the string unchanged or parses an {@code Integer}.
 */
final class SimpleStringConverter implements Converter<String> {
    @Override
    public String convert(String value) {
        return value;
    }
}
final class SimpleIntegerConverter implements Converter<Integer> {
    @Override
    public Integer convert(String value) {
        return Integer.valueOf(value);
    }
}

/**
 * Stub of {@code ConfigMappingLoader}.  The real implementation creates a proxy that
 * implements the mapping interface.  For the purpose of the tests we simply return a
 * dummy object that also implements {@link ConfigMappingObject}.
 */
final class ConfigMappingLoader {
    static <T> T configMappingObject(Class<T> iface, ConfigMappingContext ctx) {
        // create a dummy proxy that implements both the requested interface and ConfigMappingObject
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface, ConfigMappingObject.class},
                (proxyObj, method, args) -> {
                    // default behaviour – return null or default primitive
                    Class<?> ret = method.getReturnType();
                    if (ret.isPrimitive()) {
                        if (ret == boolean.class) return false;
                        if (ret == byte.class) return (byte) 0;
                        if (ret == short.class) return (short) 0;
                        if (ret == int.class) return 0;
                        if (ret == long.class) return 0L;
                        if (ret == float.class) return 0f;
                        if (ret == double.class) return 0d;
                        if (ret == char.class) return '\0';
                    }
                    return null;
                });
        @SuppressWarnings("unchecked")
        T casted = (T) proxy;
        return casted;
    }
}

/**
 * Minimal implementation of {@code ConfigMessages}.  Only the {@code propertyDoesNotMapToAnyRoot}
 * method is required for the test.
 */
enum SimpleConfigMessages implements ConfigMessages {
    INSTANCE;

    @Override
    public ConfigMessages msg() { return this; }

    @Override
    public IllegalStateException fileNotFound(String fileName) { return null; }

    @Override
    public IllegalStateException failedToLoadConfig(Throwable throwable) { return null; }

    @Override
    public IllegalArgumentException failedCharacterConversion(String value) { return null; }

    @Override
    public IllegalStateException singleTypeConverter(String className) { return null; }

    @Override
    public IllegalArgumentException notArrayType(String arrayType) { return null; }

    @Override
    public IllegalArgumentException valueNotMatchPattern(java.util.regex.Pattern pattern, String value) { return null; }

    @Override
    public IllegalArgumentException lessThanMinimumValue(Object minimum, String value) { return null; }

    @Override
    public IllegalArgumentException lessThanEqualToMinimumValue(Object minimum, String value) { return null; }

    @Override
    public IllegalArgumentException greaterThanMaximumValue(Object maximum, String value) { return null; }

    @Override
    public IllegalArgumentException greaterThanEqualToMaximumValue(Object maximum, String value) { return null; }

    @Override
    public java.io.InvalidObjectException unknownConverterId(int id) { return null; }

    @Override
    public java.util.NoSuchElementException expandingElementNotFound(String key, String valueName) { return null; }

    @Override
    public IllegalStateException unableToAddConverter(Converter<?> converter) { return null; }

    @Override
    public IllegalArgumentException noRegisteredConverter(Class<?> type) { return null; }

    @Override
    public String propertyNotFound(String name) { return null; }

    @Override
    public IllegalStateException noConfigForClassloader() { return null; }

    @Override
    public IllegalArgumentException configIsNull() { return null; }

    @Override
    public IllegalStateException configAlreadyRegistered() { return null; }

    @Override
    public IllegalArgumentException malformedEncoding() { return null; }

    @Override
    public IllegalArgumentException constructorConverterFailure(Throwable cause) { return null; }

    @Override
    public IllegalArgumentException staticMethodConverterFailure(Throwable cause) { return null; }

    @Override
    public IllegalArgumentException classConverterNotFound(Throwable cause, String className) { return null; }

    @Override
    public IllegalArgumentException unknownHost(Throwable cause, String host) { return null; }

    @Override
    public IllegalArgumentException unknownArrayType() { return null; }

    @Override
    public SecurityException notAllowed(String name) { return null; }

    @Override
    public IllegalArgumentException expressionExpansionTooDepth(String name) { return null; }

    @Override
    public IllegalArgumentException malformedUUID(Throwable cause, String malformedUUID) { return null; }

    @Override
    public java.util.NoSuchElementException mappingNotFound(String className) { return null; }

    @Override
    public java.util.NoSuchElementException mappingPrefixNotFound(String className, String prefix) { return null; }

    @Override
    public NumberFormatException integerExpected(String value) { return null; }

    @Override
    public NumberFormatException longExpected(String value) { return null; }

    @Override
    public NumberFormatException doubleExpected(String value) { return null; }

    @Override
    public NumberFormatException floatExpected(String value) { return null; }

    @Override
    public IllegalArgumentException schemeNotSupported(String scheme) { return null; }

    @Override
    public IllegalArgumentException uriSyntaxInvalid(Throwable cause, String uri) { return null; }

    @Override
    public IllegalArgumentException failedToLoadResource(Throwable cause, String location) { return null; }

    @Override
    public IllegalArgumentException getTypeNotSupportedForUnwrapping(Class<?> type) { return null; }

    @Override
    public NullPointerException converterNullValue() { return null; }

    @Override
    public IllegalArgumentException noRawType(java.lang.reflect.Type type) { return null; }

    @Override
    public IllegalArgumentException converterException(Throwable converterException, String configProperty, String configValue, String causeMessage) { return null; }

    @Override
    public java.util.NoSuchElementException propertyEmptyString(String configPropertyName, String converter) { return null; }

    @Override
    public java.util.NoSuchElementException converterReturnedNull(String configPropertyName, String configValue, String converter) { return null; }

    @Override
    public java.util.NoSuchElementException valueNotMatchMapFormat(String value) { return null; }

    @Override
    public IllegalStateException mappingAnnotationNotSupportedInClass(Class<?> type) { return null; }

    @Override
    public IllegalStateException propertiesAnnotationNotSupportedInInterface(Class<?> type) { return null; }

    @Override
    public IllegalArgumentException classIsNotAMapping(Class<?> type) { return null; }

    @Override
    public java.util.NoSuchElementException secretKeyHandlerNotFound(String handler) { return null; }

    @Override
    public IllegalStateException ambiguousMapping(String path, String amb1, String amb2) { return null; }

    @Override
    public IllegalArgumentException explicitNameInUnnamed(String name, String key) { return null; }

    @Override
    public IllegalArgumentException cannotConvertEnum(String value, Class<?> enumType, String values) { return null; }

    @Override
    public IllegalStateException propertyDoesNotMapToAnyRoot(String name, String location) {
        return new IllegalStateException("No root for property '" + name + "' at " + location);
    }
}

/* -------------------------------------------------------------------------- */
/* --------------------------- The Test Class -------------------------------- */
/* -------------------------------------------------------------------------- */

public class ConfigMappingContextTest {

    private ConfigMappingContext context;
    private TestConfig config;

    /**
     * Very small concrete {@code SmallRyeConfig} that allows us to control
     * property names, values and converters.
     */
    private static class TestConfig extends SmallRyeConfig {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, List<String>> indexed = new HashMap<>();
        private final Map<String, List<Integer>> indexedIndexes = new HashMap<>();
        private final Map<String, ConfigValue> configValues = new HashMap<>();

        TestConfig() { }

        void setValue(String name, String value) {
            values.put(name, value);
            configValues.put(name, new SimpleConfigValue("source", "location:" + name));
        }

        void setIndexed(String base, List<String> entries) {
            indexed.put(base, entries);
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                idx.add(i);
            }
            indexedIndexes.put(base, idx);
        }

        @Override
        public Iterable<String> getPropertyNames() {
            return values.keySet();
        }

        @Override
        public List<String> getIndexedProperties(String property) {
            return indexed.getOrDefault(property, Collections.emptyList());
        }

        @Override
        public List<Integer> getIndexedPropertiesIndexes(String property) {
            return indexedIndexes.getOrDefault(property, Collections.emptyList());
        }

        @Override
        public <T> T getValue(String name, Class<T> aClass) {
            String val = values.get(name);
            if (val == null) return null;
            if (aClass == String.class) {
                @SuppressWarnings("unchecked")
                T r = (T) val;
                return r;
            }
            if (aClass == Integer.class) {
                @SuppressWarnings("unchecked")
                T r = (T) Integer.valueOf(val);
                return r;
            }
            throw new IllegalArgumentException("Unsupported type " + aClass);
        }

        @Override
        public <T> T getValue(String name, Converter<T> converter) {
            String val = values.get(name);
            if (val == null) return null;
            return converter.convert(val);
        }

        @Override
        public ConfigValue getConfigValue(String name) {
            return configValues.get(name);
        }

        @Override
        public <T> Converter<T> requireConverter(Class<T> asType) {
            if (asType == String.class) {
                @SuppressWarnings("unchecked")
                Converter<T> c = (Converter<T>) new SimpleStringConverter();
                return c;
            }
            if (asType == Integer.class) {
                @SuppressWarnings("unchecked")
                Converter<T> c = (Converter<T>) new SimpleIntegerConverter();
                return c;
            }
            throw new IllegalArgumentException("No converter for " + asType);
        }

        // The remaining methods of SmallRyeConfig are not needed for the tests
    }

    /** Simple mapping interface used for {@code constructRoot/Group} tests. */
    public interface DummyMapping extends ConfigMappingObject {
        // no methods – we only need the proxy object to be created
    }

    @Before
    public void setUp() {
        // No roots, no names – the context is ready for the tests
        config = new TestConfig();
        Map<String, List<Class<?>>> roots = new HashMap<>();
        Map<String, Map<String, Set<String>>> names = new HashMap<>();
        context = new ConfigMappingContext(config, roots, names);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- constructRoot ----------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void constructRootCreatesProxyImplementingConfigMappingObject() {
        DummyMapping root = context.constructRoot(DummyMapping.class);
        assertNotNull(root);
        assertTrue(root instanceof ConfigMappingObject);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- constructGroup ---------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void constructGroupAppliesNamingStrategyAndReturnsProxy() {
        // set a naming strategy first
        context.applyNamingStrategy(NamingStrategy.VERBATIM);
        DummyMapping group = context.constructGroup(DummyMapping.class);
        assertNotNull(group);
        // after the call the naming strategy should be reset to the one returned by applyNamingStrategy
        assertEquals(NamingStrategy.VERBATIM, context.applyNamingStrategy(null));
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- getConverterInstance ---------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getConverterInstanceCreatesNewInstance() {
        Converter<String> conv = context.getConverterInstance(SimpleStringConverter.class);
        assertNotNull(conv);
        assertEquals("hello", conv.convert("hello"));
    }

    @Test(expected = InstantiationError.class)
    public void getConverterInstanceHandlesInstantiationException() {
        // a converter without a no‑arg constructor
        class NoDefaultCtor implements Converter<String> {
            NoDefaultCtor(String dummy) {}
            @Override public String convert(String value) { return null; }
        }
        context.getConverterInstance(NoDefaultCtor.class);
    }

    @Test(expected = IllegalAccessError.class)
    public void getConverterInstanceHandlesIllegalAccessException() {
        class PrivateCtor implements Converter<String> {
            private PrivateCtor() {}
            @Override public String convert(String value) { return null; }
        }
        context.getConverterInstance(PrivateCtor.class);
    }

    @Test(expected = NoSuchMethodError.class)
    public void getConverterInstanceHandlesNoSuchMethodException() {
        class NoConstructor implements Converter<String> {
            // explicit constructor with arguments, no no‑arg constructor
            NoConstructor(String s) {}
            @Override public String convert(String value) { return null; }
        }
        context.getConverterInstance(NoConstructor.class);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- applyNamingStrategy ---------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void applyNamingStrategyUsesProvidedStrategy() {
        NamingStrategy result = context.applyNamingStrategy(NamingStrategy.SNAKE_CASE);
        assertEquals(NamingStrategy.SNAKE_CASE, result);
    }

    @Test
    public void applyNamingStrategyDefaultsToKebabWhenNullAndUnset() {
        // ensure the internal field is null
        context.applyNamingStrategy(null);
        NamingStrategy result = context.applyNamingStrategy(null);
        assertEquals(NamingStrategy.KEBAB_CASE, result);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- getStringBuilder -------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getStringBuilderIsReusableAndInitiallyEmpty() {
        StringBuilder sb = context.getStringBuilder();
        assertNotNull(sb);
        assertEquals(0, sb.length());
        sb.append("test");
        assertEquals("test", context.getStringBuilder().toString());
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- reportProblem / getProblems -------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void reportProblemAddsProblemToList() {
        RuntimeException ex = new RuntimeException("oops");
        context.reportProblem(ex);
        List<Problem> probs = context.getProblems();
        assertEquals(1, probs.size());
        assertEquals("java.lang.RuntimeException: oops", probs.get(0).getMessage());
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- getRootsMap ------------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void getRootsMapInitiallyEmptyAndPopulatedAfterRegisterRoot() {
        // create a new context with a root mapping to trigger registration
        Map<String, List<Class<?>>> roots = new HashMap<>();
        roots.put("my.root", Collections.singletonList(DummyMapping.class));
        ConfigMappingContext ctx = new ConfigMappingContext(config, roots, new HashMap<>());
        Map<Class<?>, Map<String, ConfigMappingObject>> map = ctx.getRootsMap();
        assertTrue(map.containsKey(DummyMapping.class));
        assertTrue(map.get(DummyMapping.class).containsKey("my.root"));
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- reportUnknown ---------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void reportUnknownDetectsUnusedPropertiesAndAddsProblems() {
        // set up configuration with two properties – one used, one unused
        config.setValue("app.port", "8080");
        config.setValue("app.unused", "value");

        // register a root that matches "app"
        Map<String, List<Class<?>>> roots = new HashMap<>();
        roots.put("app", Collections.singletonList(DummyMapping.class));
        ConfigMappingContext ctx = new ConfigMappingContext(config, roots, new HashMap<>());

        // mark "app.port" as used via a dummy ObjectCreator value call
        ctx.constructObject("app").value(String.class, null).get();

        // ignore list is empty – we expect a problem for the unused property
        ctx.reportUnknown(Collections.emptyList());

        List<Problem> probs = ctx.getProblems();
        assertEquals(1, probs.size());
        assertTrue(probs.get(0).getMessage().contains("app.unused"));
    }

    @Test
    public void reportUnknownRespectsIgnoredPathsAndWildcard() {
        config.setValue("service.timeout", "30");
        config.setValue("service.debug", "true");
        config.setValue("service.extra", "x");

        // root "service"
        Map<String, List<Class<?>>> roots = new HashMap<>();
        roots.put("service", Collections.singletonList(DummyMapping.class));
        ConfigMappingContext ctx = new ConfigMappingContext(config, roots, new HashMap<>());

        // ignore "service.debug" and all under "service.extra.**"
        ctx.reportUnknown(Arrays.asList("service.debug", "service.extra.**"));

        List<Problem> probs = ctx.getProblems();
        // only "service.timeout" should be reported
        assertEquals(1, probs.size());
        assertTrue(probs.get(0).getMessage().contains("service.timeout"));
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- filterPropertiesInRoots ------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void filterPropertiesInRootsMatchesRootExactlyAndPrefix() {
        List<String> props = Arrays.asList("a", "a.b", "a.b.c", "x", "x.y");
        Set<String> roots = new HashSet<>(Arrays.asList("a", "x.y"));
        Iterable<String> filtered = invokeFilterPropertiesInRoots(props, roots);
        List<String> list = toList(filtered);
        assertEquals(4, list.size());
        assertTrue(list.containsAll(Arrays.asList("a", "a.b", "a.b.c", "x.y")));
    }

    @Test
    public void filterPropertiesInRootsReturnsAllWhenRootIsEmptyString() {
        List<String> props = Arrays.asList("any.prop", "another");
        Set<String> roots = new HashSet<>(Collections.singletonList(""));
        Iterable<String> filtered = invokeFilterPropertiesInRoots(props, roots);
        assertEquals(props, toList(filtered));
    }

    @Test
    public void filterPropertiesInRootsReturnsAllWhenRootsEmpty() {
        List<String> props = Arrays.asList("p1", "p2");
        Set<String> roots = Collections.emptySet();
        Iterable<String> filtered = invokeFilterPropertiesInRoots(props, roots);
        assertEquals(props, toList(filtered));
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- isPropertyInRoot -------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void isPropertyInRootExactMatch() {
        assertTrue(invokeIsPropertyInRoot("foo", "foo"));
    }

    @Test
    public void isPropertyInRootPrefixWithDot() {
        assertTrue(invokeIsPropertyInRoot("foo.bar", "foo"));
    }

    @Test
    public void isPropertyInRootPrefixWithBracket() {
        assertTrue(invokeIsPropertyInRoot("foo[0].baz", "foo"));
    }

    @Test
    public void isPropertyInRootRejectsShorterProperty() {
        assertFalse(invokeIsPropertyInRoot("fo", "foo"));
    }

    @Test
    public void isPropertyInRootRejectsUnrelatedProperty() {
        assertFalse(invokeIsPropertyInRoot("bar.foo", "foo"));
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- ObjectCreator.value ----------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void objectCreatorValueRetrievesAndMarksUsedProperty() {
        config.setValue("my.prop", "hello");
        ConfigMappingContext.ObjectCreator<String> creator = context.constructObject("my.prop")
                .value(String.class, null);
        String result = creator.get();
        assertEquals("hello", result);
        // usedProperties is private – we verify indirectly via reportUnknown
        context.reportUnknown(Collections.emptyList());
        assertTrue(context.getProblems().isEmpty()); // no unknowns because property was used
    }

    @Test
    public void objectCreatorOptionalValueReturnsEmptyWhenMissing() {
        ConfigMappingContext.ObjectCreator<Optional<Integer>> creator = context.constructObject("missing")
                .optionalValue(Integer.class, null);
        Optional<Integer> opt = creator.get();
        assertFalse(opt.isPresent());
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- ObjectCreator.collection ------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void objectCreatorCollectionBuildsListFromIndexedProperties() {
        config.setIndexed("list", Arrays.asList("a", "b", "c"));
        ConfigMappingContext.ObjectCreator<List<String>> creator = context.constructObject("list")
                .collection(List.class);
        List<String> result = creator.get();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void objectCreatorOptionalCollectionReturnsEmptyOptionalWhenNoIndexes() {
        ConfigMappingContext.ObjectCreator<Optional<Set<String>>> creator = context.constructObject("empty")
                .optionalCollection(Set.class);
        Optional<Set<String>> opt = creator.get();
        assertFalse(opt.isPresent());
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- ObjectCreator.map ------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void objectCreatorMapCreatesMapWithConvertedKeys() {
        // simulate properties:
        // map.foo = "v1"
        // map.bar = "v2"
        config.setValue("map.foo", "v1");
        config.setValue("map.bar", "v2");
        ConfigMappingContext.ObjectCreator<Map<String, String>> creator = context.constructObject("map")
                .map(String.class, null, null);
        Map<String, String> map = creator.get();
        assertEquals(2, map.size());
        assertEquals("v1", map.get("foo"));
        assertEquals("v2", map.get("bar"));
    }

    @Test
    public void objectCreatorMapWithDefaultValueSuppliesDefaultWhenKeyMissing() {
        // only one entry, the other will use default
        config.setValue("defMap.present", "value");
        ConfigMappingContext.ObjectCreator<Map<String, DummyMapping>> creator = context.constructObject("defMap")
                .map(String.class, null, null, DummyMapping.class);
        Map<String, DummyMapping> map = creator.get();
        // present key should have a dummy mapping instance
        assertTrue(map.containsKey("present"));
        // missing key should have a dummy mapping instance supplied by defaultClass
        assertTrue(map.containsKey("missing"));
        assertNotNull(map.get("missing"));
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- Helper Reflection Calls ------------------- */
    /* ---------------------------------------------------------------------- */

    /**
     * Uses reflection to access the private static method {@code filterPropertiesInRoots}
     * because it is not visible otherwise.
     */
    private static Iterable<String> invokeFilterPropertiesInRoots(Iterable<String> props, Set<String> roots) {
        try {
            java.lang.reflect.Method m = ConfigMappingContext.class
                    .getDeclaredMethod("filterPropertiesInRoots", Iterable.class, Set.class);
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            Iterable<String> result = (Iterable<String>) m.invoke(null, props, roots);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean invokeIsPropertyInRoot(String property, String root) {
        try {
            java.lang.reflect.Method m = ConfigMappingContext.class
                    .getDeclaredMethod("isPropertyInRoot", String.class, String.class);
            m.setAccessible(true);
            return (boolean) m.invoke(null, property, root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> toList(Iterable<String> iterable) {
        List<String> list = new ArrayList<>();
        for (String s : iterable) list.add(s);
        return list;
    }
}
