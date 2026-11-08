import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.Permission;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_configMappingObject_Test {

    /**
     * Shared configuration used by all tests.
     */
    private static SmallRyeConfig CONFIG;

    /**
     * Context created before each test – it is cheap to recreate.
     */
    private ConfigMappingContext context;

    class Dummy {
    }

    class Dummy {
    }

    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    @ConfigMapping(prefix = "valid")
    public interface ValidMapping {

        String greeting();
    }

    public static class ValidMappingImpl extends ConfigMappingObject implements ValidMapping {

        public ValidMappingImpl(ConfigMappingContext ctx) {
            Objects.requireNonNull(ctx, "configMappingContext is null");
        }

        @Override
        public String greeting() {
            return "hello";
        }
    }

    @ConfigMapping(prefix = "noctor")
    public interface NoCtorMapping {

        String value();
    }

    // No constructor that accepts ConfigMappingContext – only a default constructor.
    public static class NoCtorMappingImpl extends ConfigMappingObject implements NoCtorMapping {

        public NoCtorMappingImpl() {
        }

        @Override
        public String value() {
            return "no ctor";
        }
    }

    @ConfigMapping(prefix = "abstract")
    public interface AbstractImplMapping {

        String data();
    }

    // Abstract implementation – cannot be instantiated.
    public abstract static class AbstractImplMappingImpl extends ConfigMappingObject implements AbstractImplMapping {

        public AbstractImplMappingImpl(ConfigMappingContext ctx) {
        }
    }

    @ConfigMapping(prefix = "private")
    public interface PrivateCtorMapping {

        String secret();
    }

    public static class PrivateCtorMappingImpl extends ConfigMappingObject implements PrivateCtorMapping {

        private PrivateCtorMappingImpl(ConfigMappingContext ctx) {
        }

        @Override
        public String secret() {
            return "shhh";
        }
    }

    @ConfigMapping(prefix = "runtime")
    public interface RuntimeExceptionMapping {

        String explode();
    }

    public static class RuntimeExceptionMappingImpl extends ConfigMappingObject implements RuntimeExceptionMapping {

        public RuntimeExceptionMappingImpl(ConfigMappingContext ctx) {
            throw new RuntimeException("boom from ctor");
        }

        @Override
        public String explode() {
            return "won't get here";
        }
    }

    @ConfigMapping(prefix = "checked")
    public interface CheckedExceptionMapping {

        String fail();
    }

    public static class CheckedExceptionMappingImpl extends ConfigMappingObject implements CheckedExceptionMapping {

        public CheckedExceptionMappingImpl(ConfigMappingContext ctx) throws IOException {
            throw new IOException("checked exception from ctor");
        }

        @Override
        public String fail() {
            return "won't get here";
        }
    }

    @ConfigMapping(prefix = "error")
    public interface ErrorThrowingMapping {

        String crash();
    }

    public static class ErrorThrowingMappingImpl extends ConfigMappingObject implements ErrorThrowingMapping {

        public ErrorThrowingMappingImpl(ConfigMappingContext ctx) {
            throw new AssertionError("error from ctor");
        }

        @Override
        public String crash() {
            return "won't get here";
        }
    }

    @ConfigMapping(prefix = "incompatible")
    public interface IncompatibleMapping {

        String something();
    }

    // Does NOT implement IncompatibleMapping – only extends ConfigMappingObject.
    public static class IncompatibleMappingImpl extends ConfigMappingObject {

        public IncompatibleMappingImpl(ConfigMappingContext ctx) {
        }
    }

    @ConfigMapping(prefix = "secure")
    public interface SecureMapping {

        String safe();
    }

    public static class SecureMappingImpl extends ConfigMappingObject implements SecureMapping {

        public SecureMappingImpl(ConfigMappingContext ctx) {
        }

        @Override
        public String safe() {
            return "secure";
        }
    }

    @ConfigMapping(prefix = "nullimpl")
    public interface NullImplMapping {

        String nothing();
    }

    class Dummy {
    }

    /**
     * Build a {@link SmallRyeConfig} with a few properties that match the {@code Server}
     * {@code @ConfigMapping} interface. The configuration is reused for all tests.
     */
    @BeforeAll
    static void setupBeforeAll() {
        CONFIG = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).withMapping(ConfigMappingLoader_configMappingObject_Test.Server.class).build();
        // Force generation of the implementation class so the ClassValue cache is populated.
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoader_configMappingObject_Test.Server.class);
    }

    /**
     * Create a fresh {@link ConfigMappingContext} for every test.  The maps are empty
     * because they are only needed for advanced scenarios (roots, name‑resolution).
     */
    @BeforeEach
    void setupBeforeEach() {
        Map<String, List<Class<?>>> roots = Collections.emptyMap();
        Map<String, Map<String, Set<String>>> names = Collections.emptyMap();
        context = new ConfigMappingContext(CONFIG, roots, names);
    }

    private static void forceCache(Class<?> iface) {
        ConfigMappingLoader.getImplementationClass(iface);
    }

    /**
     * Verify that {@code configMappingObject(..)} returns an instance of the generated
     * implementation class and that the instance behaves like the original mapping
     * interface (i.e. returns the configured values).
     */
    @Test
    void configMappingObjectCreatesCorrectImplementation() {
        Server server = ConfigMappingLoader.configMappingObject(ConfigMappingLoader_configMappingObject_Test.Server.class, context);
        assertNotNull(server, "The returned mapping object must not be null");
        assertTrue(server instanceof Server, "The object must implement the Server interface");
        assertEquals("localhost", server.host(), "Host value should be read from the config source");
        assertEquals(8080, server.port(), "Port value should be read from the config source");
    }

    /**
     * Ensure that a {@link NoSuchMethodError} is thrown when the generated implementation
     * does not contain the expected constructor.  This situation cannot happen with the
     * normal code‑generation path, so we simulate it by using a dummy interface that
     * deliberately lacks the generated class.
     *
     * The test demonstrates that the error handling in {@code configMappingObject(..)}
     * works as specified.
     */
    @Test
    void configMappingObjectThrowsWhenConstructorMissing() {
        class Dummy {
        }
        assertThrows(NoSuchMethodError.class, () -> ConfigMappingLoader.configMappingObject(Dummy.class, context));
    }

    @Test
    void configMappingObjectThrowsWhenImplementationIsAbstract() {
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoader_configMappingObject_Test.AbstractImplMapping.class);
        assertThrows(InstantiationError.class, () -> ConfigMappingLoader.configMappingObject(AbstractImplMapping.class, context));
    }

    @Test
    void configMappingObjectThrowsWhenConstructorIsPrivate() {
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoader_configMappingObject_Test.PrivateCtorMapping.class);
        assertThrows(IllegalAccessError.class, () -> ConfigMappingLoader.configMappingObject(PrivateCtorMapping.class, context));
    }

    @Test
    void configMappingObjectPropagatesRuntimeExceptionFromConstructor() {
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoader_configMappingObject_Test.RuntimeExceptionMapping.class);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.configMappingObject(RuntimeExceptionMapping.class, context));
        assertEquals("boom from ctor", ex.getMessage());
    }

    @Test
    void configMappingObjectWrapsCheckedExceptionInUndeclaredThrowableException() {
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoader_configMappingObject_Test.CheckedExceptionMapping.class);
        UndeclaredThrowableException ex = assertThrows(UndeclaredThrowableException.class, () -> ConfigMappingLoader.configMappingObject(CheckedExceptionMapping.class, context));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("checked exception from ctor", ex.getCause().getMessage());
    }

    @Test
    void configMappingObjectPropagatesErrorFromConstructor() {
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoader_configMappingObject_Test.ErrorThrowingMapping.class);
        AssertionError err = assertThrows(AssertionError.class, () -> ConfigMappingLoader.configMappingObject(ErrorThrowingMapping.class, context));
        assertEquals("error from ctor", err.getMessage());
    }

    @Test
    void configMappingObjectRejectsNullInterfaceType() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingObject(null, context));
    }

    @Test
    void configMappingObjectRejectsNullConfigMappingContext() {
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoader_configMappingObject_Test.ValidMapping.class);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingObject(ValidMapping.class, null));
        assertEquals("configMappingContext is null", npe.getMessage());
    }

    @Test
    void configMappingObjectDetectsIncompatibleImplementation() {
        ConfigMappingLoader.getImplementationClass(ConfigMappingLoader_configMappingObject_Test.IncompatibleMapping.class);
        assertThrows(ClassCastException.class, () -> ConfigMappingLoader.configMappingObject(IncompatibleMapping.class, context));
    }

    @Test
    void configMappingObjectPropagatesSecurityExceptionWhenAccessDenied() {
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            protected void checkMemberAccess(Class<?> clazz, int which) {
                throw new SecurityException("reflective access denied");
            }
        });
        try {
            ConfigMappingLoader.getImplementationClass(ConfigMappingLoader_configMappingObject_Test.SecureMapping.class);
            assertThrows(SecurityException.class, () -> ConfigMappingLoader.configMappingObject(SecureMapping.class, context));
        } finally {
            System.setSecurityManager(original);
        }
    }

    @Test
    void configMappingObjectFailsWhenImplementationClassIsNull() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingObject(NullImplMapping.class, context));
    }
}
