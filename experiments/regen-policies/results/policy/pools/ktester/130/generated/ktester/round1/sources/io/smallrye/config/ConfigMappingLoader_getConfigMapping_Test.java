package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;
import java.lang.reflect.UndeclaredThrowableException;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
@ConfigMapping(prefix = "server")
class ConfigMappingLoader_getConfigMapping_Test {

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        boolean enabled();
    }

    interface Ssl {

        boolean enabled();
    }

    interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

    interface ServerParentNested {
    }

//     interface ServerParentNested {
    }

    interface ServerChildNested {
    }

//     interface ServerChildNested {
    }

    /* --------------------------------------------------------------------- */
//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface ServerParentNested {
    }

//     interface ServerParentNested {
    }

//     interface ServerParentNested {
    }

//     interface ServerChildNested {
    }

//     interface ServerChildNested {
    }

//     interface ServerChildNested {
    }

    @ConfigMapping(prefix = "server")
    public interface ServerNested {

        Map<String, Environment> environments();

        Log log();

        java.util.Optional<Ssl> ssl();

        List<App> apps();

        interface Environment {

            String host();

            int port();
        }

        interface Log {

            boolean enabled();
        }

        interface Ssl {

            boolean enabled();
        }

        interface App {

            String name();
        }
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

    interface ServerParent {

        ServerParentNested parentNested();

        interface ServerParentNested {
        }
    }

//     interface ServerParentNested {
    }

//     interface ServerParentNested {
    }

    interface ServerChild extends ServerParent {

        ServerChildNested childNested();

        interface ServerChildNested {
        }
    }

//     interface ServerChildNested {
    }

//     interface ServerChildNested {
    }

    /* Plain class without any config‑mapping annotation */
    static class PlainClass {

        String foo;
    }

    /* Simple mapping interfaces */
    @ConfigMapping(prefix = "simple")
    interface NoConfigProperties {

        String foo();
    }

    @ConfigMapping(prefix = "default")
    interface SimpleConfig {

        String name();

        int age();
    }

    @ConfigMapping(prefix = "default")
    interface DefaultMethodConfig {

        String value();

        default String greet() {
            return "hello " + value();
        }
    }

    /* Broken mapping – used to test class‑generation failure */
    @ConfigMapping(prefix = "broken")
    interface BrokenConfig {

        String foo();
    }

    /* Cyclic mapping interfaces */
    @ConfigMapping(prefix = "a")
    interface CyclicConfigA {

        CyclicConfigB b();
    }

    @ConfigMapping(prefix = "b")
    interface CyclicConfigB {

        CyclicConfigA a();
    }

    /* Inheritance of @ConfigMapping */
    @ConfigMapping(prefix = "parent")
    interface ParentConfig {

        String parentValue();
    }

    interface SubConfig extends ParentConfig {
        // No additional annotation
    }

    /* Optional return type */
    @ConfigMapping(prefix = "opt")
    interface OptionalConfig {

        Optional<String> maybe();
    }

    /* Custom key */
    @ConfigMapping(prefix = "custom")
    interface CustomKeyConfig {

//         @ConfigKey("my.custom.key")
//         String custom();
    }

    /* Default value */
    @ConfigMapping(prefix = "def")
    interface DefaultValueConfig {

        @WithDefault("42")
        int answer();
    }

    /* Parent with child mapping */
    @ConfigMapping(prefix = "parent")
    interface ParentWithChildConfig {

        ChildConfig child();
    }

    @ConfigMapping(prefix = "child")
    interface ChildConfig {

        String name();
    }

    /* Unsupported generic collection */
    @ConfigMapping(prefix = "gen")
    interface GenericCollectionConfig {

        // not supported – test expects failure
        List<String> values();
    }

    /* Static method */
    @ConfigMapping(prefix = "static")
    interface StaticMethodConfig {

        String foo();

        static String bar() {
            return "static";
        }
    }

    /* Default method that throws */
    @ConfigMapping(prefix = "throws")
    interface DefaultThrowsConfig {

        String value();

        default String explode() {
            throw new RuntimeException("boom");
        }
    }

    /* Multiple inheritance */
    @ConfigMapping(prefix = "a")
    interface MultiA {

        String a();
    }

    @ConfigMapping(prefix = "b")
    interface MultiB {

        String b();
    }

    interface MultiParentConfig extends MultiA, MultiB {
        // No extra methods
    }

    /* Invalid prefix */
    @ConfigMapping(prefix = "my*prefix")
    interface InvalidPrefixConfig {

        String foo();
    }

    /* Cached implementation */
    @ConfigMapping(prefix = "cached")
    interface CachedConfig {

        String value();
    }

    /* Primitive array */
    @ConfigMapping(prefix = "array")
    interface PrimitiveArrayConfig {

        int[] numbers();
    }

    /* Unsupported map */
    @ConfigMapping(prefix = "map")
    interface MapReturnConfig {

        // unsupported – test expects failure
        Map<String, String> map();
    }

    /* Method with parameters */
    @ConfigMapping(prefix = "params")
    interface MethodWithParamsConfig {

        String getValue(String arg);
    }

    /* Void return */
    @ConfigMapping(prefix = "void")
    interface VoidReturnConfig {

        void reload();
    }

    /* Duplicate keys */
    @ConfigMapping(prefix = "dup")
    interface DuplicateKeyConfig {

//         @ConfigKey("app.name")
//         String name();

//         @ConfigKey("app.name")
//         String anotherName();
    }

    /* Object array – unsupported */
    @ConfigMapping(prefix = "objarr")
    interface ObjectArrayConfig {

        // non‑primitive array
        String[] names();
    }

    /* Optional with unsupported generic */
    @ConfigMapping(prefix = "optgen")
    interface OptionalUnsupportedConfig {

        // not supported
        Optional<List<String>> values();
    }

    /* Empty config key */
    @ConfigMapping(prefix = "emptykey")
    interface EmptyConfigKeyConfig {

//         @ConfigKey("")
//         String empty();
    }

    /* Whitespace in prefix */
    @ConfigMapping(prefix = "my config")
    interface WhitespacePrefixConfig {

        String foo();
    }

    /* Child without @ConfigMapping */
    @ConfigMapping(prefix = "parent")
    interface ParentWithoutChildAnnotationConfig {

        ChildWithoutAnnotation child();
    }

    interface ChildWithoutAnnotation {

        String value();
    }

    /* Duplicate key via inheritance */
    @ConfigMapping(prefix = "parentdup")
    interface ParentDup {

//         @ConfigKey("dup.key")
//         String foo();
    }

    @ConfigMapping(prefix = "childdup")
    interface InheritedDuplicateKeyConfig extends ParentDup {

//         @ConfigKey("dup.key")
//         String bar();
    }

//     String host();

//     int port();

    /* --------------------------------------------------------------------- */
    @Test
    void simpleMapping() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(Server.class);
        assertNotNull(mapping, "Mapping must not be null for a valid @ConfigMapping interface");
        assertEquals(Server.class, mapping.getInterfaceType(), "Returned mapping should reference the original interface");
        // The interface has no nested mappings.
        assertTrue(mapping.getNested().isEmpty(), "Simple mapping must not contain nested mappings");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nestedMapping() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(ServerNested.class);
        assertNotNull(mapping, "Mapping must not be null for a nested @ConfigMapping interface");
        List<ConfigMappingInterface> nested = mapping.getNested();
        // Expected nested interfaces: Environment, Log, Ssl, App
        assertEquals(4, nested.size(), "Exactly four nested mappings are expected");
        Set<Class<?>> nestedTypes = nested.stream().map(ConfigMappingInterface::getInterfaceType).collect(Collectors.toSet());
        assertTrue(nestedTypes.contains(ServerNested.Environment.class), "Missing Environment nested type");
        assertTrue(nestedTypes.contains(ServerNested.Log.class), "Missing Log nested type");
        assertTrue(nestedTypes.contains(ServerNested.Ssl.class), "Missing Ssl nested type");
        assertTrue(nestedTypes.contains(ServerNested.App.class), "Missing App nested type");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void parentChildMapping() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerChild.class);
        List<Class<?>> types = metadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(Collectors.toList());
        // Verify that both child and parent interfaces are present
        assertTrue(types.contains(ServerChild.class), "Child interface missing");
        assertTrue(types.contains(ServerParent.class), "Parent interface missing");
        // Verify that nested types from both hierarchies are present
        assertTrue(types.contains(ServerChild.ServerChildNested.class), "Nested type from child missing");
        assertTrue(types.contains(ServerParent.ServerParentNested.class), "Nested type from parent missing");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nonMappingReturnsNull() {
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(PlainClass.class);
        assertNull(result, "Non‑mapping class should yield a null ConfigMappingInterface");
    }

    @Test
    void nullInputThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMapping(null), "Calling getConfigMapping with null must throw NullPointerException");
    }

    @Test
    void notAnInterfaceThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(String.class), "Passing a concrete class should cause IllegalArgumentException");
    }

    @Test
    void missingConfigMappingAnnotationThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(NoConfigProperties.class), "Interface without @ConfigMapping must be rejected");
    }

    @Test
    void validSimpleMappingReturnsInstance() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(SimpleConfig.class);
        assertNotNull(mapping, "Mapping must not be null for a valid @ConfigMapping interface");
        assertTrue(mapping instanceof ConfigMappingInterface, "Result should be an instance of ConfigMappingInterface");
    }

    @Test
    void defaultMethodDoesNotBreakLoading() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(DefaultMethodConfig.class);
        assertNotNull(mapping, "Mapping must be created even if the interface contains default methods");
    }

//     @Test
//     void implementationClassLoadingFailureIsWrapped() {
//         // Mock the static loadClass method to throw a runtime exception.
//         try (MockedStatic<ConfigMappingLoader> mocked = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
//             mocked.when(() -> ConfigMappingLoader.loadClass(any(), any())).thenThrow(new UndeclaredThrowableException(new RuntimeException("generation failure")));
//             assertThrows(UndeclaredThrowableException.class, () -> ConfigMappingLoader.getConfigMapping(BrokenConfig.class), "Failure to define the implementation class must be wrapped in UndeclaredThrowableException");
//         }
//     }

    @Test
    void recursiveInterfaceCycleDetected() {
        assertThrows(IllegalStateException.class, () -> ConfigMappingLoader.getConfigMapping(CyclicConfigA.class), "Cyclic configuration interfaces must cause IllegalStateException");
    }

    @Test
    void inheritedConfigMappingAnnotationIsAccepted() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(SubConfig.class);
        assertNotNull(mapping, "Sub‑interface inheriting @ConfigMapping must be accepted");
    }

    @Test
    void optionalReturnTypeIsSupported() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(OptionalConfig.class);
        assertNotNull(mapping, "Interface with Optional return type must be processed");
    }

    @Test
    void configKeyOverrideIsHandled() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(CustomKeyConfig.class);
        assertNotNull(mapping, "Interface using @ConfigKey must be processed");
    }

    @Test
    void withDefaultValueIsHandled() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(DefaultValueConfig.class);
        assertNotNull(mapping, "Interface with @WithDefault must be processed");
    }

    @Test
    void nestedConfigMappingInterfaceIsLoaded() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(ParentWithChildConfig.class);
        assertNotNull(mapping, "Parent interface returning a child @ConfigMapping must be loaded");
    }

    @Test
    void unsupportedGenericReturnTypeThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(GenericCollectionConfig.class), "Unsupported generic collection return types must cause IllegalArgumentException");
    }

    @Test
    void staticMethodIsIgnored() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(StaticMethodConfig.class);
        assertNotNull(mapping, "Static methods should be ignored and not prevent mapping creation");
    }

    @Test
    void defaultMethodThrowingExceptionDoesNotAffectLoading() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(DefaultThrowsConfig.class);
        assertNotNull(mapping, "Default methods that throw must not affect mapping generation");
    }

    @Test
    void multipleInheritanceIsSupported() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(MultiParentConfig.class);
        assertNotNull(mapping, "Interface extending multiple @ConfigMapping interfaces must be supported");
    }

    @Test
    void invalidConfigMappingPrefixThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(InvalidPrefixConfig.class), "Invalid characters in @ConfigMapping prefix must cause IllegalArgumentException");
    }

    @Test
    void cachedImplementationClassIsReused() {
        ConfigMappingInterface first = ConfigMappingLoader.getConfigMapping(CachedConfig.class);
        ConfigMappingInterface second = ConfigMappingLoader.getConfigMapping(CachedConfig.class);
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getClass(), second.getClass(), "Two calls for the same interface must return objects of the same implementation class");
    }

    @Test
    void primitiveArrayReturnTypeIsSupported() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(PrimitiveArrayConfig.class);
        assertNotNull(mapping, "Primitive array return types must be supported");
    }

    @Test
    void unsupportedMapReturnTypeThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(MapReturnConfig.class), "Map return types are unsupported and must cause IllegalArgumentException");
    }

    @Test
    void methodWithParametersThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(MethodWithParamsConfig.class), "Methods with parameters must be rejected");
    }

    @Test
    void voidReturnMethodThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(VoidReturnConfig.class), "Methods returning void are illegal");
    }

    @Test
    void duplicatePropertyKeysThrowIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(DuplicateKeyConfig.class), "Duplicate configuration keys must cause IllegalArgumentException");
    }

    @Test
    void unsupportedObjectArrayThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(ObjectArrayConfig.class), "Object array return types are unsupported");
    }

    @Test
    void optionalWithUnsupportedGenericThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(OptionalUnsupportedConfig.class), "Optional of unsupported generic type must be rejected");
    }

    @Test
    void emptyConfigKeyThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(EmptyConfigKeyConfig.class), "Empty @ConfigKey values are illegal");
    }

    @Test
    void whitespaceInPrefixThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(WhitespacePrefixConfig.class), "Whitespace in @ConfigMapping prefix must be rejected");
    }

    @Test
    void nestedInterfaceWithoutConfigMappingThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(ParentWithoutChildAnnotationConfig.class), "Nested interface lacking @ConfigMapping must cause IllegalArgumentException");
    }

    @Test
    void duplicateKeyViaInheritanceThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(InheritedDuplicateKeyConfig.class), "Duplicate keys across inheritance hierarchy must be detected");
    }
}