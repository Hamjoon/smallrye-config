package io.smallrye.config;
import io.smallrye.config._private.ConfigMessages;


import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigMappings}.
 * These tests focus on the pure‑Java utility methods that can be exercised
 * without needing a full SmallRye Config environment.
 */
public class ConfigMappingsTest {

    /* --------------------------------------------------------------------- */
    /*  Tests for the private static prefix helpers                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPrefixEmptyPrefixReturnsPath() {
        String result = ConfigMappings.prefix("", "my.prop");
        assertEquals("my.prop", result);
    }

    @Test
    public void testPrefixEmptyPathReturnsPrefix() {
        String result = ConfigMappings.prefix("my.prefix", "");
        assertEquals("my.prefix", result);
    }

    @Test
    public void testPrefixSimpleConcatenation() {
        String result = ConfigMappings.prefix("my.prefix", "prop");
        assertEquals("my.prefix.prop", result);
    }

    @Test
    public void testPrefixArrayNotationKeepsBracket() {
        String result = ConfigMappings.prefix("my.prefix", "[0]");
        assertEquals("my.prefix[0]", result);
    }

    @Test
    public void testPrefixWithStringBuilderParameter() {
        StringBuilder sb = new StringBuilder("base");
        String result = ConfigMappings.prefix("base", "name", sb);
        assertEquals("base.name", result);
        // ensure the original StringBuilder was used (its content is now the result)
        assertSame(sb, new StringBuilder(result)); // just a sanity check – sb is mutable
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for ConfigMappings.ConfigClass utility class                     */
    /* --------------------------------------------------------------------- */

    /**
     * A minimal {@code @ConfigMapping} interface used only to exercise the
     * {@link ConfigMappings.ConfigClass} constructor. The mapping has no
     * properties, which means the internal loops iterate over an empty map
     * and never touch {@link ConfigMappingLoader}.
     */
    @ConfigMapping(prefix = "test")
    interface EmptyMapping {
    }

    /**
     * A minimal {@code @ConfigProperties} class used to exercise the
     * {@link ConfigMappings.ConfigClass#configClass(Class)} factory method.
     */
    @ConfigProperties(prefix = "props")
    static class EmptyProperties {
    }

    @Test
    public void testConfigClassEqualsAndHashCode() {
        ConfigMappings.ConfigClass a = new ConfigMappings.ConfigClass(EmptyMapping.class, "test");
        ConfigMappings.ConfigClass b = new ConfigMappings.ConfigClass(EmptyMapping.class, "test");
        ConfigMappings.ConfigClass c = new ConfigMappings.ConfigClass(EmptyMapping.class, "other");

        // equality based on type + prefix only
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, c);
        assertNotEquals(a.hashCode(), c.hashCode());

        // also test against null and different class
        assertNotEquals(a, null);
        assertNotEquals(a, "some string");
    }

    @Test
    public void testConfigClassFactoryFromInterface() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(EmptyMapping.class);
        assertEquals(EmptyMapping.class, cfg.getType());
        assertEquals("test", cfg.getPrefix());
        // No properties → empty map
        assertTrue(cfg.getProperties().isEmpty());
    }

    @Test
    public void testConfigClassFactoryFromClass() {
        ConfigMappings.ConfigClass cfg = ConfigMappings.ConfigClass.configClass(EmptyProperties.class);
        assertEquals(EmptyProperties.class, cfg.getType());
        assertEquals("props", cfg.getPrefix());
        assertTrue(cfg.getProperties().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for illegal annotation usage – we inject a stub ConfigMessages   */
    /* --------------------------------------------------------------------- */

    @ConfigMapping(prefix = "illegal")
    static class IllegalMappingClass {
    }

    @ConfigProperties(prefix = "illegal")
    interface IllegalPropertiesInterface {
    }

    @BeforeClass
    public static void injectConfigMessagesStub() throws Exception {
        // ConfigMessages.msg is a static final field; we replace it via reflection.
        Field msgField = ConfigMessages.class.getDeclaredField("msg");
        msgField.setAccessible(true);

        // Remove the 'final' modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(msgField, msgField.getModifiers() & ~Modifier.FINAL);

        // Provide a minimal implementation that throws the expected exceptions
        ConfigMessages stub = new ConfigMessages() {
            @Override public ConfigMessages msg() { return this; }

            @Override public IllegalStateException mappingAnnotationNotSupportedInClass(Class<?> type) {
                return new IllegalStateException("Mapping annotation not allowed on class: " + type.getName());
            }

            @Override public IllegalStateException propertiesAnnotationNotSupportedInInterface(Class<?> type) {
                return new IllegalStateException("Properties annotation not allowed on interface: " + type.getName());
            }

            // All other methods are irrelevant for this test suite – return null / dummy values
            @Override public IllegalStateException fileNotFound(String fileName) { return null; }
            @Override public IllegalStateException failedToLoadConfig(Throwable throwable) { return null; }
            @Override public IllegalArgumentException failedCharacterConversion(String value) { return null; }
            @Override public IllegalStateException singleTypeConverter(String className) { return null; }
            @Override public IllegalArgumentException notArrayType(String arrayType) { return null; }
            @Override public IllegalArgumentException valueNotMatchPattern(java.util.regex.Pattern pattern, String value) { return null; }
            @Override public IllegalArgumentException lessThanMinimumValue(Object minimum, String value) { return null; }
            @Override public IllegalArgumentException lessThanEqualToMinimumValue(Object minimum, String value) { return null; }
            @Override public IllegalArgumentException greaterThanMaximumValue(Object maximum, String value) { return null; }
            @Override public IllegalArgumentException greaterThanEqualToMaximumValue(Object maximum, String value) { return null; }
            @Override public java.io.InvalidObjectException unknownConverterId(int id) { return null; }
            @Override public java.util.NoSuchElementException expandingElementNotFound(String key, String valueName) { return null; }
            @Override public IllegalStateException unableToAddConverter(Converter<?> converter) { return null; }
            @Override public IllegalArgumentException noRegisteredConverter(Class<?> type) { return null; }
            @Override public String propertyNotFound(String name) { return null; }
            @Override public IllegalArgumentException noConfigForClassloader() { return null; }
            @Override public IllegalArgumentException configIsNull() { return null; }
            @Override public IllegalStateException configAlreadyRegistered() { return null; }
            @Override public IllegalArgumentException malformedEncoding() { return null; }
            @Override public IllegalArgumentException constructorConverterFailure(Throwable cause) { return null; }
            @Override public IllegalArgumentException staticMethodConverterFailure(Throwable cause) { return null; }
            @Override public IllegalArgumentException classConverterNotFound(Throwable cause, String className) { return null; }
            @Override public IllegalArgumentException unknownHost(Throwable cause, String host) { return null; }
            @Override public IllegalArgumentException unknownArrayType() { return null; }
            @Override public SecurityException notAllowed(String name) { return null; }
            @Override public IllegalArgumentException expressionExpansionTooDepth(String name) { return null; }
            @Override public IllegalArgumentException malformedUUID(Throwable cause, String malformedUUID) { return null; }
            @Override public java.util.NoSuchElementException mappingNotFound(String className) { return null; }
            @Override public java.util.NoSuchElementException mappingPrefixNotFound(String className, String prefix) { return null; }
            @Override public NumberFormatException integerExpected(String value) { return null; }
            @Override public NumberFormatException longExpected(String value) { return null; }
            @Override public NumberFormatException doubleExpected(String value) { return null; }
            @Override public NumberFormatException floatExpected(String value) { return null; }
            @Override public IllegalArgumentException schemeNotSupported(String scheme) { return null; }
            @Override public IllegalArgumentException uriSyntaxInvalid(Throwable cause, String uri) { return null; }
            @Override public IllegalArgumentException failedToLoadResource(Throwable cause, String location) { return null; }
            @Override public IllegalArgumentException getTypeNotSupportedForUnwrapping(Class<?> type) { return null; }
            @Override public NullPointerException converterNullValue() { return null; }
            @Override public IllegalArgumentException noRawType(java.lang.reflect.Type type) { return null; }
            @Override public IllegalArgumentException converterException(Throwable converterException, String configProperty, String configValue, String causeMessage) { return null; }
            @Override public java.util.NoSuchElementException propertyEmptyString(String configPropertyName, String converter) { return null; }
            @Override public java.util.NoSuchElementException converterReturnedNull(String configPropertyName, String configValue, String converter) { return null; }
            @Override public java.util.NoSuchElementException valueNotMatchMapFormat(String value) { return null; }
            @Override public IllegalStateException propertiesAnnotationNotSupportedInInterface(Class<?> type) { return null; }
            @Override public IllegalArgumentException classIsNotAMapping(Class<?> type) { return null; }
            @Override public java.util.NoSuchElementException secretKeyHandlerNotFound(String handler) { return null; }
            @Override public IllegalStateException ambiguousMapping(String path, String amb1, String amb2) { return null; }
            @Override public IllegalArgumentException explicitNameInUnnamed(String name, String key) { return null; }
            @Override public IllegalArgumentException cannotConvertEnum(String value, Class<?> enumType, String values) { return null; }
            @Override public IllegalStateException propertyDoesNotMapToAnyRoot(String name, String location) { return null; }
            @Override public IllegalStateException couldNotGenerateMapping(Throwable throwable, String mapping) { return null; }
            @Override public IllegalArgumentException mappingMethodsCannotAcceptParameters(String method) { return null; }
            @Override public IllegalArgumentException mappingMethodsCannotBeVoid(String method) { return null; }
            @Override public IllegalArgumentException mappingCannotUseSelfReferenceTypes(String type) { return null; }
            @Override public IllegalArgumentException defaultValuesConfigSourceNameReserved(String name) { return null; }
            @Override public IllegalStateException incompatibleConfigProvider(String expected, String actual) { return null; }
        };

        msgField.set(null, stub);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClassFactoryRejectsClassWithConfigMapping() {
        // The stub ConfigMessages will produce the exception we expect.
        ConfigMappings.ConfigClass.configClass(IllegalMappingClass.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigClassFactoryRejectsInterfaceWithConfigProperties() {
        ConfigMappings.ConfigClass.configClass(IllegalPropertiesInterface.class);
    }
}
