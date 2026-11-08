package io.smallrye.config;

import static io.smallrye.config.ConfigMappingLoader.loadClass;
import static io.smallrye.config.ConfigMappingLoader.getConfigMappingsMetadata;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_loadClass_Test {

    /**
     * Simple parent interface used in many test cases.
     */
    interface ParentInterface {
    }

    /**
     * A class that implements {@link ParentInterface}.
     */
    static class ParentImplementation implements ParentInterface {
    }

    /**
     * A class that does *not* implement {@link ParentInterface}.
     */
    static class UnrelatedImplementation {
    }

    /**
     * Marker interface for metadata that represents a @ConfigProperties interface.
     */
    interface ConfigMappingClass extends ConfigMappingMetadata {
    }

    /**
     * Minimal stub of {@link ConfigMappingMetadata}.
     */
    interface ConfigMappingMetadata {

        String getClassName();

        byte[] getClassBytes();
    }

    @ConfigMapping(prefix = "server")
    interface ServerManual {

        String host();

        int port();
    }

//     static List<Arguments> alreadyLoadedAssignableProvider() {
//         ConfigMappingMetadata meta = mock(ConfigMappingMetadata.class);
//         when(meta.getClassName()).thenReturn("some.pkg.ParentImplementation");
//         // not used
//         when(meta.getClassBytes()).thenReturn(new byte[0]);
//         ConfigMappingMetadata metaWithConfigProps = mock(ConfigMappingClass.class);
//         when(metaWithConfigProps.getClassName()).thenReturn("some.pkg.ParentImplementation");
//         when(metaWithConfigProps.getClassBytes()).thenReturn(new byte[0]);
//         return List.of(Arguments.of("already loaded & assignable – plain metadata", ParentInterface.class, meta, false), Arguments.of("already loaded & assignable – ConfigMappingClass metadata", ParentInterface.class, metaWithConfigProps, true));
//     }

    /**
     * Verify that {@code loadClass} correctly defines a new implementation class
     * when it is not already present in the class loader, and that a subsequent
     * call returns the already‑defined class (i.e. the same {@link Class} instance).
     */
//     @Test
//     void loadClassDefinesAndCachesImplementation() {
//         // Obtain the metadata that describes the generated implementation for ServerManual.
//         List<ConfigMappingMetadata> metadataList = getConfigMappingsMetadata(ServerManual.class);
//         assertFalse(metadataList.isEmpty(), "Metadata list should contain at least one entry");
//         // The first invocation must trigger class definition because the implementation class does not exist yet.
//         ConfigMappingMetadata metadata = metadataList.get(0);
//         Class<?> firstLoad = loadClass(ServerManual.class, metadata);
//         assertNotNull(firstLoad, "loadClass should return a non‑null Class");
//         assertTrue(ServerManual.class.isAssignableFrom(firstLoad), "Returned class should implement the mapping interface");
//         // The second invocation should find the class already defined and simply return it.
//         Class<?> secondLoad = loadClass(ServerManual.class, metadata);
//         assertSame(firstLoad, secondLoad, "Subsequent loadClass calls must return the same Class instance");
//     }

    /**
     * Verify that {@code loadClass} returns an already‑loaded class when the
     * implementation class is present in the parent class loader.
     */
//     @Test
//     void loadClassReturnsExistingClassWhenPresent() throws ClassNotFoundException {
//         // Force the implementation class to be loaded via the public API first.
//         Class<? extends ConfigMappingObject> implClass = ConfigMappingLoader.getImplementationClass(ServerManual.class);
//         assertNotNull(implClass, "Implementation class should be loadable via getImplementationClass");
//         // Retrieve the same metadata used by the loader.
//         List<ConfigMappingMetadata> metadataList = getConfigMappingsMetadata(ServerManual.class);
//         ConfigMappingMetadata metadata = metadataList.get(0);
//         // Now invoke loadClass – the class is already present, so it should be returned directly.
//         Class<?> loaded = loadClass(ServerManual.class, metadata);
//         assertSame(implClass, loaded, "When the class is already loaded, loadClass must return the existing Class instance");
//     }

    // ---------- 1. class already loaded and assignable ----------
//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("alreadyLoadedAssignableProvider")
//     void loadClass_alreadyLoaded_assignable(String description, Class<?> parent, ConfigMappingMetadata metadata, boolean metadataIsConfigMappingClass) throws Exception {
//         // Arrange – mock the parent class‑loader behaviour
//         ClassLoader mockCl = mock(ClassLoader.class);
//         when(mockCl.loadClass(metadata.getClassName())).thenReturn(ParentImplementation.class);
//         // Arrange – static method stubs
//         try (MockedStatic<ConfigMappingLoader> statics = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
//             // lock object – we don't care about its identity for this case
//             statics.when(() -> ConfigMappingLoader.getClassLoaderLock(anyString())).thenReturn(new Object());
//             // defineClass must NOT be called
//             statics.when(() -> ConfigMappingLoader.defineClass(any(), anyString(), any())).thenThrow(new AssertionError("defineClass must not be invoked"));
//             // Act
//             Class<?> result = loadClass(parent, metadata);
//             // Assert
//             assertEquals(ParentImplementation.class, result, "When the class is already present and assignable it must be returned directly");
//         }
//     }

    // ---------- 2. class already loaded but not assignable – metadata is ConfigMappingClass ----------
//     @Test
//     void loadClass_alreadyLoaded_notAssignable_metadataIsConfigMappingClass() throws Exception {
//         ConfigMappingMetadata meta = mock(ConfigMappingClass.class);
//         when(meta.getClassName()).thenReturn("some.pkg.UnrelatedImplementation");
//         when(meta.getClassBytes()).thenReturn(new byte[0]);
//         ClassLoader mockCl = mock(ClassLoader.class);
//         when(mockCl.loadClass(meta.getClassName())).thenReturn(UnrelatedImplementation.class);
//         // Mock static helpers
//         try (MockedStatic<ConfigMappingLoader> statics = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
//             statics.when(() -> ConfigMappingLoader.getClassLoaderLock(anyString())).thenReturn(new Object());
//             // defineClass must NOT be called – the method should simply return the loaded class
//             statics.when(() -> ConfigMappingLoader.defineClass(any(), anyString(), any())).thenThrow(new AssertionError("defineClass must not be invoked"));
//             // Use a subclass of ParentInterface to pass as 'parent' (the concrete class does not matter)
//             Class<?> result = loadClass(ParentInterface.class, meta);
//             assertEquals(UnrelatedImplementation.class, result);
//         }
//     }

    // ---------- 3. class already loaded but not assignable – metadata NOT ConfigMappingClass ----------
//     @Test
//     void loadClass_alreadyLoaded_notAssignable_plainMetadata() throws Exception {
//         ConfigMappingMetadata meta = mock(ConfigMappingMetadata.class);
//         when(meta.getClassName()).thenReturn("some.pkg.UnrelatedImplementation");
//         // dummy bytes
//         when(meta.getClassBytes()).thenReturn(new byte[] { 1, 2, 3 });
//         ClassLoader mockCl = mock(ClassLoader.class);
//         when(mockCl.loadClass(meta.getClassName())).thenReturn(UnrelatedImplementation.class);
//         // The class we expect defineClass to produce
//         Class<?> generated = ParentImplementation.class;
//         try (MockedStatic<ConfigMappingLoader> statics = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
//             statics.when(() -> ConfigMappingLoader.getClassLoaderLock(anyString())).thenReturn(new Object());
//             // defineClass should be called and return our dummy class
//             statics.when(() -> ConfigMappingLoader.defineClass(any(), eq(meta.getClassName()), any())).thenReturn(generated);
//             Class<?> result = loadClass(ParentInterface.class, meta);
//             assertEquals(generated, result, "When the existing class is not assignable and metadata is plain, the loader must define a new class");
//         }
//     }

    // ---------- 4. class not found ----------
//     @Test
//     void loadClass_classNotFound_definesClass() throws Exception {
//         ConfigMappingMetadata meta = mock(ConfigMappingMetadata.class);
//         when(meta.getClassName()).thenReturn("some.pkg.DoesNotExist");
//         when(meta.getClassBytes()).thenReturn(new byte[] { 9, 9, 9 });
//         // Simulate ClassNotFoundException from the parent class loader
//         ClassLoader mockCl = mock(ClassLoader.class);
//         when(mockCl.loadClass(meta.getClassName())).thenThrow(ClassNotFoundException.class);
//         // class that defineClass will return
//         Class<?> generated = ParentImplementation.class;
//         try (MockedStatic<ConfigMappingLoader> statics = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
//             statics.when(() -> ConfigMappingLoader.getClassLoaderLock(anyString())).thenReturn(new Object());
//             statics.when(() -> ConfigMappingLoader.defineClass(any(), eq(meta.getClassName()), any())).thenReturn(generated);
//             Class<?> result = loadClass(ParentInterface.class, meta);
//             assertEquals(generated, result);
//         }
//     }

    // ---------- 5. defensive null checks ----------
//     @Test
//     void loadClass_nullParent_throwsNPE() {
//         ConfigMappingMetadata meta = new ConfigMappingMetadata() {
// 
//             @Override
//             public String getClassName() {
//                 return "X";
//             }
// 
//             @Override
//             public byte[] getClassBytes() {
//                 return new byte[0];
//             }
//         };
//         assertThrows(NullPointerException.class, () -> loadClass(null, meta));
//     }

//     @Test
//     void loadClass_nullMetadata_throwsNPE() {
//         assertThrows(NullPointerException.class, () -> loadClass(Object.class, null));
//     }

    // ---------- 6. concurrent invocation – lock uniqueness ----------
//     @Test
//     void loadClass_concurrentCalls_serializeOnSameLock() throws Exception {
//         ConfigMappingMetadata meta = mock(ConfigMappingMetadata.class);
//         when(meta.getClassName()).thenReturn("some.pkg.Concurrent");
//         when(meta.getClassBytes()).thenReturn(new byte[] { 4, 5, 6 });
//         // ClassLoader will throw CNF so that defineClass is exercised
//         ClassLoader mockCl = mock(ClassLoader.class);
//         when(mockCl.loadClass(meta.getClassName())).thenThrow(ClassNotFoundException.class);
//         // Count how many times defineClass is invoked
//         AtomicInteger defineCalls = new AtomicInteger(0);
//         Class<?> generated = ParentImplementation.class;
//         // same lock must be returned for both threads
//         Object lock = new Object();
//         try (MockedStatic<ConfigMappingLoader> statics = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
//             statics.when(() -> ConfigMappingLoader.getClassLoaderLock(meta.getClassName())).thenReturn(lock);
//             statics.when(() -> ConfigMappingLoader.defineClass(any(), eq(meta.getClassName()), any())).thenAnswer(inv -> {
//                 defineCalls.incrementAndGet();
//                 // Simulate a little work to increase the chance of contention
//                 Thread.sleep(50);
//                 return generated;
//             });
//             ExecutorService exec = Executors.newFixedThreadPool(2);
//             CountDownLatch ready = new CountDownLatch(2);
//             Callable<Class<?>> task = () -> {
//                 ready.countDown();
//                 ready.await();
//                 return loadClass(ParentInterface.class, meta);
//             };
//             Future<Class<?>> f1 = exec.submit(task);
//             Future<Class<?>> f2 = exec.submit(task);
//             Class<?> r1 = f1.get(5, TimeUnit.SECONDS);
//             Class<?> r2 = f2.get(5, TimeUnit.SECONDS);
//             exec.shutdownNow();
//             assertEquals(generated, r1);
//             assertEquals(generated, r2);
//             // defineClass must have been executed only once because both threads should use the same lock.
//             assertEquals(1, defineCalls.get(), "defineClass should be invoked only once thanks to the per‑class‑name lock");
//         }
//     }

    // ---------- 7. null class name in metadata ----------
//     @Test
//     void loadClass_nullClassName_throwsNPE() {
//         ConfigMappingMetadata meta = new ConfigMappingMetadata() {
// 
//             @Override
//             public String getClassName() {
//                 return null;
//             }
// 
//             @Override
//             public byte[] getClassBytes() {
//                 return new byte[0];
//             }
//         };
//         // getClassLoaderLock(null) will throw NPE – we let it propagate
//         assertThrows(NullPointerException.class, () -> loadClass(Object.class, meta));
//     }

    // ---------- 8. null class bytes ----------
//     @Test
//     void loadClass_nullClassBytes_defineClassThrowsNPE() {
//         ConfigMappingMetadata meta = new ConfigMappingMetadata() {
// 
//             @Override
//             public String getClassName() {
//                 return "some.pkg.NullBytes";
//             }
// 
//             @Override
//             public byte[] getClassBytes() {
//                 return null;
//             }
//         };
//         // The real defineClass will throw NPE because bytes are null – we let it bubble up
//         assertThrows(NullPointerException.class, () -> loadClass(Object.class, meta));
//     }

    // ---------- 9. parent class loader is null (bootstrap) ----------
//     @Test
//     void loadClass_parentWithBootstrapClassLoader_throwsNPE() {
//         // java.lang.Object is loaded by the bootstrap class loader (null)
//         ConfigMappingMetadata meta = new ConfigMappingMetadata() {
// 
//             @Override
//             public String getClassName() {
//                 return "any.Name";
//             }
// 
//             @Override
//             public byte[] getClassBytes() {
//                 return new byte[0];
//             }
//         };
//         // The method will try to call parent.getClassLoader().loadClass(...) and hit NPE
//         assertThrows(NullPointerException.class, () -> loadClass(Object.class, meta));
//     }
}