package io.smallrye.config;
import io.smallrye.config.ConfigMapping.NamingStrategy;


import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Minimal stub definitions required for compilation of {@link ConfigMappingInterface}.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
@interface ConfigMapping {
    NamingStrategy namingStrategy() default NamingStrategy.KEBAB_CASE;
    boolean beanStyleGetters() default false;
    String prefix() default "";
}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@interface WithDefault {
    String value();
}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@interface WithDefaults {}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@interface WithConverter {
    Class<? extends Converter<?>> value();
}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@interface WithName {
    String value();
}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@interface WithParentName {}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@interface WithUnnamedKey {
    String value();
}

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@interface WithKeys {
    Class<? extends Supplier<Iterable<String>>> value();
}

/**
 * Minimal {@link ConfigMessages} implementation needed for {@code getClassBytes()} error handling.
 */
interface ConfigMessages {
    ConfigMessages msg = new ConfigMessages() {
        @Override public IllegalStateException couldNotGenerateMapping(Throwable throwable) {
            return new IllegalStateException("could not generate mapping", throwable);
        }
        // All other methods throw UnsupportedOperationException – they are never used in the tests.
        @Override public IllegalStateException fileNotFound(String fileName) { throw new UnsupportedOperationException();}
        @Override public IllegalStateException failedToLoadConfig(Throwable throwable) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException failedCharacterConversion(String value) { throw new UnsupportedOperationException();}
        @Override public IllegalStateException singleTypeConverter(String className) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException notArrayType(String arrayType) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException valueNotMatchPattern(java.util.regex.Pattern pattern, String value) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException lessThanMinimumValue(Object minimum, String value) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException lessThanEqualToMinimumValue(Object minimum, String value) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException greaterThanMaximumValue(Object maximum, String value) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException greaterThanEqualToMaximumValue(Object maximum, String value) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException unknownConverterId(int id) { throw new UnsupportedOperationException();}
        @Override public java.util.NoSuchElementException expandingElementNotFound(String key, String valueName) { throw new UnsupportedOperationException();}
        @Override public IllegalStateException unableToAddConverter(Converter<?> converter) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException noRegisteredConverter(Class<?> type) { throw new UnsupportedOperationException();}
        @Override public String propertyNotFound(String name) { throw new UnsupportedOperationException();}
        @Override public IllegalStateException noConfigForClassloader() { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException configIsNull() { throw new UnsupportedOperationException();}
        @Override public IllegalStateException configAlreadyRegistered() { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException malformedEncoding() { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException constructorConverterFailure(Throwable cause) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException staticMethodConverterFailure(Throwable cause) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException classConverterNotFound(Throwable cause, String className) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException unknownHost(Throwable cause, String host) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException unknownArrayType() { throw new UnsupportedOperationException();}
        @Override public SecurityException notAllowed(String name) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException expressionExpansionTooDepth(String name) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException malformedUUID(Throwable cause, String malformedUUID) { throw new UnsupportedOperationException();}
        @Override public java.util.NoSuchElementException mappingNotFound(String className) { throw new UnsupportedOperationException();}
        @Override public java.util.NoSuchElementException mappingPrefixNotFound(String className, String prefix) { throw new UnsupportedOperationException();}
        @Override public NumberFormatException integerExpected(String value) { throw new UnsupportedOperationException();}
        @Override public NumberFormatException longExpected(String value) { throw new UnsupportedOperationException();}
        @Override public NumberFormatException doubleExpected(String value) { throw new UnsupportedOperationException();}
        @Override public NumberFormatException floatExpected(String value) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException schemeNotSupported(String scheme) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException uriSyntaxInvalid(Throwable cause, String uri) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException failedToLoadResource(Throwable cause, String location) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException getTypeNotSupportedForUnwrapping(Class<?> type) { throw new UnsupportedOperationException();}
        @Override public NullPointerException converterNullValue() { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException noRawType(Type type) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException converterException(Throwable converterException, String configProperty, String configValue, String causeMessage) { throw new UnsupportedOperationException();}
        @Override public java.util.NoSuchElementException propertyEmptyString(String configPropertyName, String converter) { throw new UnsupportedOperationException();}
        @Override public java.util.NoSuchElementException converterReturnedNull(String configPropertyName, String configValue, String converter) { throw new UnsupportedOperationException();}
        @Override public java.util.NoSuchElementException valueNotMatchMapFormat(String value) { throw new UnsupportedOperationException();}
        @Override public IllegalStateException mappingAnnotationNotSupportedInClass(Class<?> type) { throw new UnsupportedOperationException();}
        @Override public IllegalStateException propertiesAnnotationNotSupportedInInterface(Class<?> type) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException classIsNotAMapping(Class<?> type) { throw new UnsupportedOperationException();}
        @Override public java.util.NoSuchElementException secretKeyHandlerNotFound(String handler) { throw new UnsupportedOperationException();}
        @Override public IllegalStateException ambiguousMapping(String path, String amb1, String amb2) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException explicitNameInUnnamed(String name, String key) { throw new UnsupportedOperationException();}
        @Override public IllegalArgumentException cannotConvertEnum(String value, Class<?> enumType, String values) { throw new UnsupportedOperationException();}
        @Override public IllegalStateException propertyDoesNotMapToAnyRoot(String name, String location) { throw new UnsupportedOperationException();}
    };
}

/**
 * Stub generator used only to allow {@code getClassBytes()} to be exercised.
 */
class ConfigMappingGenerator {
    static byte[] generate(ConfigMappingInterface cmi) {
        // In a real environment this would generate bytecode; for the test we simply return an empty array.
        return new byte[0];
    }
}

/**
 * Test suite for {@link ConfigMappingInterface}.
 */
public class ConfigMappingInterfaceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ----------------------------------------------------------------------
     * Helper configuration interfaces used in the tests
     * ---------------------------------------------------------------------- */

    public interface SimpleConfig {
        String name();
    }

    public interface PrimitiveConfig {
        int count();
    }

    public interface OptionalConfig {
        Optional<String> optional();
    }

    public interface NestedConfig {
        String inner();
    }

    public interface GroupConfig {
        NestedConfig nested();
    }

    public interface MapConfig {
        Map<String, Integer> map();
    }

    public interface ListConfig {
        List<String> list();
    }

    public interface ToStringConfig {
        String toString();
    }

    public interface SuperConfig {
        String superProp();
    }

    public interface ChildConfig extends SuperConfig {
        String childProp();
    }

    public interface BadParamConfig {
        String bad(String arg);
    }

    public interface BadVoidConfig {
        void nothing();
    }

    /* ----------------------------------------------------------------------
     * 1. Public method signatures (extracted for reference)
     * ---------------------------------------------------------------------- */
    // public static ConfigMappingInterface getConfigurationInterface(Class<?> interfaceType)
    // public Class<?> getInterfaceType()
    // public String getClassName()
    // public ConfigMappingInterface[] getSuperTypes()
    // public Property[] getProperties()
    // public Map<String, Map<String, Set<String>>> getNames()
    // public boolean hasConfigMapping()
    // public NamingStrategy getNamingStrategy()
    // public boolean isBeanStyleGetters()
    // public String getClassInternalName()
    // public List<ConfigMappingInterface> getNested()
    // public byte[] getClassBytes()

    /* ----------------------------------------------------------------------
     * 2. Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigurationInterface_NullThrows() {
        thrown.expect(NullPointerException.class);
        ConfigMappingInterface.getConfigurationInterface(null);
    }

    @Test
    public void testSimpleInterfaceProperties() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        assertNotNull(cmi);
        assertEquals(SimpleConfig.class, cmi.getInterfaceType());

        // class name should be the fully‑qualified name plus the $$CMImpl suffix
        assertTrue(cmi.getClassName().endsWith("$$CMImpl"));
        assertTrue(cmi.getClassName().startsWith(SimpleConfig.class.getName()));

        // no super types
        assertArrayEquals(new ConfigMappingInterface[0], cmi.getSuperTypes());

        // single property
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertEquals("name", p.getMemberName());
        assertTrue(p.isLeaf());
        assertFalse(p.isPrimitive());
        assertFalse(p.isGroup());
        assertFalse(p.isMap());
        assertFalse(p.isCollection());
        assertFalse(p.isOptional());
    }

    @Test
    public void testPrimitiveProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(PrimitiveConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p.isPrimitive());
        ConfigMappingInterface.PrimitiveProperty pp = p.asPrimitive();
        assertEquals(int.class, pp.getPrimitiveType());
        assertEquals(Integer.class, pp.getBoxType());
        assertFalse(pp.hasConvertWith());
    }

    @Test
    public void testOptionalProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(OptionalConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p.isOptional());
        ConfigMappingInterface.OptionalProperty op = p.asOptional();
        assertTrue(op.isLeaf()); // nested leaf
        ConfigMappingInterface.LeafProperty leaf = op.asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testGroupProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(GroupConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p.isGroup());
        ConfigMappingInterface.GroupProperty gp = p.asGroup();
        ConfigMappingInterface nestedCmi = gp.getGroupType();
        assertEquals(NestedConfig.class, nestedCmi.getInterfaceType());

        // nested interface should appear in getNested()
        List<ConfigMappingInterface> nested = cmi.getNested();
        assertEquals(1, nested.size());
        assertEquals(NestedConfig.class, nested.get(0).getInterfaceType());
    }

    @Test
    public void testMapProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(MapConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p.isMap());
        ConfigMappingInterface.MapProperty mp = p.asMap();
        assertEquals(String.class, mp.getKeyRawType());
        assertTrue(mp.getValueProperty().isLeaf());
        ConfigMappingInterface.LeafProperty leaf = mp.getValueProperty().asLeaf();
        assertEquals(Integer.class, leaf.getValueRawType());
    }

    @Test
    public void testCollectionProperty() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ListConfig.class);
        ConfigMappingInterface.Property[] props = cmi.getProperties();
        assertEquals(1, props.length);
        ConfigMappingInterface.Property p = props[0];
        assertTrue(p.isCollection());
        ConfigMappingInterface.CollectionProperty cp = p.asCollection();
        assertEquals(List.class, cp.getCollectionRawType());
        ConfigMappingInterface.LeafProperty leaf = cp.getElement().asLeaf();
        assertEquals(String.class, leaf.getValueRawType());
    }

    @Test
    public void testToStringMethodDetection() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(ToStringConfig.class);
        // The toString method should be recognised as a ToStringMethod and not appear in normal properties
        assertEquals(0, cmi.getProperties().length);
        ConfigMappingInterface.ToStringMethod tsm = cmi.getToStringMethod();
        assertTrue(tsm.isToStringMethod());
        assertTrue(tsm.generate());
    }

    @Test
    public void testSuperTypesAndInheritance() {
        ConfigMappingInterface childCmi = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        ConfigMappingInterface[] supers = childCmi.getSuperTypes();
        assertEquals(1, supers.length);
        assertEquals(SuperConfig.class, supers[0].getInterfaceType());

        // properties from super and child should be merged
        ConfigMappingInterface.Property[] props = childCmi.getProperties();
        assertEquals(2, props.length);
        Set<String> names = new java.util.HashSet<>();
        for (ConfigMappingInterface.Property p : props) {
            names.add(p.getMemberName());
        }
        assertTrue(names.contains("superProp"));
        assertTrue(names.contains("childProp"));
    }

    @Test
    public void testGetNamesStructure() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        Map<String, Map<String, Set<String>>> names = cmi.getNames();
        assertTrue(names.containsKey(SimpleConfig.class.getName()));
        Map<String, Set<String>> inner = names.get(SimpleConfig.class.getName());
        // The root path is the empty string; the property name set should contain "name"
        assertTrue(inner.containsKey(""));
        assertTrue(inner.get("").contains("name"));
    }

    @Test
    public void testClassInternalName() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        String internal = cmi.getClassInternalName();
        assertEquals(cmi.getClassName().replace('.', '/'), internal);
    }

    @Test
    public void testGetClassBytesHandlesGeneratorException() {
        // The stub generator returns an empty byte array, so this call should succeed.
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        byte[] bytes = cmi.getClassBytes();
        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testHasConfigMappingAndNamingStrategyDefaults() {
        ConfigMappingInterface cmi = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        // No @ConfigMapping annotation – defaults
        assertFalse(cmi.hasConfigMapping());
        assertEquals(NamingStrategy.KEBAB_CASE, cmi.getNamingStrategy());
        assertFalse(cmi.isBeanStyleGetters());
    }

    /* ----------------------------------------------------------------------
     * 3. Edge‑case and exception handling tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testMethodWithParametersThrows() {
        thrown.expect(IllegalArgumentException.class);
        ConfigMappingInterface.getConfigurationInterface(BadParamConfig.class);
    }

    @Test
    public void testVoidReturnMethodThrows() {
        thrown.expect(IllegalArgumentException.class);
        ConfigMappingInterface.getConfigurationInterface(BadVoidConfig.class);
    }

    @Test
    public void testInvalidPropertyNameAnnotations() {
        // Define an interface with contradictory annotations to trigger the validation in getPropertyName().
        @SuppressWarnings("unused")
        @ConfigMapping
        interface BadNameConfig {
            @WithParentName
            @WithName("foo")
            String foo();
        }
        thrown.expect(IllegalArgumentException.class);
        ConfigMappingInterface.getConfigurationInterface(BadNameConfig.class);
    }

    @Test
    public void testEmptyWithNameThrows() {
        @SuppressWarnings("unused")
        interface EmptyNameConfig {
            @WithName("")
            String empty();
        }
        thrown.expect(IllegalArgumentException.class);
        ConfigMappingInterface.getConfigurationInterface(EmptyNameConfig.class);
    }
}
