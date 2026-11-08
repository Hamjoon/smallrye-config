# Table 3. Changed-line coverage

Coverage uses methods alive entering each index. Changed-line coordinates are the post-image added/modified lines. Build failures and missing coverage remain unavailable.

## All files

| Index | Subject | Executable lines | Dev (scope) covered | KTester covered | Ouédraogo covered | Dev (scope) lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
Unavailable: batch execution measures scope files only.
## Scope files

| Index | Subject | Executable lines | Dev (scope) covered | KTester covered | Ouédraogo covered | Dev (scope) lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 2 | Include mappings nested super types metadata (#1123) | 10 | 10 (100.0%) | 0 (0.0%) | 5 (50.0%) | 0 | 0 | 0 |
| 17 | Do not fail with inline map value in mapping (#1135) | 8 | 4 (50.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 27 | Always include super properties of mappings (#1148) | 9 | 9 (100.0%) | 5 (55.6%) | 5 (55.6%) | 0 | 6 | 36 |
| 30 | Generate mapping names and defaults (#1146) | 281 | 244 (86.8%) | 0 (0.0%) | 1 (0.4%) | 26 | 10 | 10 |
| 35 | Change getSuperTypes to public (#1154) | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 36 | Override super members defaults (#1156) | 8 | 8 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 38 | Keep all mapping names (#1158) | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 46 | add method to exception (#1168) | 2 | 0 (0.0%) | 2 (100.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 58 | Improve message on mapping generation failure (#1175) | 3 | 1 (33.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 94 | Avoid using string concatenation to forge impl name (#1211) | 7 | 7 (100.0%) | 5 (71.4%) | 5 (71.4%) | 0 | 2 | 0 |
| 111 | Support a fixed list of Map keys statically @WithKeys (#1220) | 74 | 59 (79.7%) | 1 (1.4%) | 1 (1.4%) | 0 | 0 | 0 |
| 114 | Move local classes to inner to reduce the number of classes in the main package (#1226) | 23 | 21 (91.3%) | 0 (0.0%) | 6 (26.1%) | 0 | 0 | 0 |
| 123 | Rename ConfigClassWithPrefix to ConfigClass and use it in SmallRyeConfigBuilder (#1236) | 14 | 10 (71.4%) | 0 (0.0%) | 0 (0.0%) | 1 | 5 | 18 |
| 124 | Fix NPE on ConfigValue#getSourceName | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 130 | Add @ConfigMapping beanStyleGetter to enable / disable bean style getter names matching with configuration names (#1239) | 22 | 17 (77.3%) | 1 (4.5%) | 0 (0.0%) | 0 | 7 | 0 |
| 158 | Improve reportUnknown allocations (#1263) | 24 | 24 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 160 | Match most specific mapping prefix when matching environment variables (#1267) | 19 | 19 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 163 | Generate properties names with defaults in mapping class (#1273) | 71 | 61 (85.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 164 | Validate mappings on creation instead of on lookup (#1264) | 22 | 20 (90.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 168 | Use `invokeExact` when creating a ConfigMapping object (#1275) | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 178 | Use static method to create object for leaf properties (#1286) | 97 | 85 (87.6%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 199 | Ignore default methods inherited from super classes in mappings (#1304) | 15 | 14 (93.3%) | 1 (6.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 200 | Add a way for ConfigClass to also carry and load defaults from a config mapping class (#1306) | 42 | 28 (66.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 202 | Cache MethodHandles to construct and get defaults from the mapping implementation (#1309) | 23 | 15 (65.2%) | 0 (0.0%) | 0 (0.0%) | 12 | 0 | 0 |
| 203 | Add API in the builder to register a mapping instance (#1310) | 18 | 12 (66.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 206 | Keep properties from ConfigMapping implementation (#1313) | 18 | 16 (88.9%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 224 | Correctly match Env names when mapping contains empty names (#1331) | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 237 | Move mapping Env matching to ConfigSource initialization (#1346) | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 276 | Make order of Config elements more deterministic | 9 | 7 (77.8%) | 3 (33.3%) | 0 (0.0%) | 0 | 0 | 0 |
| 277 | Reduce allocations when collecting properties from superTypes | 8 | 8 (100.0%) | 5 (62.5%) | 0 (0.0%) | 0 | 0 | 0 |
| 281 | Keep only a single array of properties for ConfigMappingInterface | 2 | 2 (100.0%) | 1 (50.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 312 | Secret type to represent secrets and integrate with the Secret Keys handling (#1232) | 55 | 23 (41.8%) | 1 (1.8%) | 0 (0.0%) | 0 | 0 | 0 |
| 316 | Improve ConfigMapping generation error (#1411) | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 318 | Keep all the @ConfigMapping path and names operations to resolve the final property name in ConfigMappingContext (#1413) | 88 | 54 (61.4%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 326 | Introduce exact methods to get values from String, int and boolean (#1419) | 6 | 6 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 334 | Throw IllegalArgumentException when using self-reference types (#1428) | 16 | 12 (75.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 11 | 0 |
| 346 | Support `Map<String, Optional<String>>` in `@ConfigMapping` (#1438) | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 351 | Ensure modular access when reflecting (#1444) | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 353 | Matcher for property names (#1446) | 15 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 399 | Optimize reportUnknown() to avoid PropertyName HashSet collisions (#1490) | 14 | 11 (78.6%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 400 | Add withMappingIgnorePrefix() API for optimized prefix filtering (#1491) | 5 | 2 (40.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 402 | Improve PropertyNamesMatcher and use it instead of structures that use PropertyName as a key (#1496) | 5 | 1 (20.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 403 | Generate direct ctor call for ConfigMapping nested elements (#1497) | 56 | 42 (75.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 409 | Use PropertyNamesMatcher to report unknowns (#1502) | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 417 | Apply BeanStyleGetters to defaults (#1508) | 12 | 12 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 435 | Stabilize hashCode of ConfigClass (#1527) | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 446 | Add SPI ConfigMappingHandler to split handling of @ConfigMapping and @ConfigProperties (#1532) | 3 | 2 (66.7%) | 0 (0.0%) | 0 (0.0%) | 2 | 0 | 0 |
| 447 | Support nested config classes in class-based mappings (#1538) | 24 | 20 (83.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 456 | Reproduce issue with `@WithUnnamedKey` config group considered present even if unconfigured (#1534) | 25 | 25 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 462 | @WithUnnamedKey#eager to control if default keys are present in the mapping Map (#1546) | 32 | 14 (43.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 467 | Ignore @WithDefault on Map and Collection with nested group values (#1550) | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 468 | Collect nested ConfigMappingClass entries in getConfigMappingsMetadata (#1551) | 20 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 478 | Refactor ConfigMappings to unify metadata model and clearly separate introspection, generation and loading (#1556) | 219 | 152 (69.4%) | 3 (1.4%) | 0 (0.0%) | 0 | 11 | 0 |
| 485 | ConfigInstanceBuilder implementation: - Builder class generation - Simple leaf types and primitives - Simple optionals - @WithDefault | 19 | 14 (73.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 496 | ConfigInstanceBuilder use Converters to load | 7 | 4 (57.1%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 498 | ConfigInstanceBuilder Converters per interface Classloader | 3 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 514 | Warn on mapping ambiguous single segment quoted / unquoted Map keys (#1580) | 40 | 31 (77.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 525 | Use ClassValue to cache ConfigMappingInterface and ConfigMappingClass (#1589) | 44 | 30 (68.2%) | 1 (2.3%) | 0 (0.0%) | 0 | 0 | 0 |
| 529 | Prefer AtomicReference instead of a Holder object (#1592) | 6 | 6 (100.0%) | 1 (16.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 552 | Support ConfigMapping template instances (#1613) | 8 | 5 (62.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 556 | Fix Bean Validation property names with beanStyleGetters (#1616) | 1 | 1 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 559 | Fix lazy groups with inherited beanStyleGetters (#1618) | 2 | 2 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 560 | Avoid scanning linearly all the property names (#1621) | 21 | 20 (95.2%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 564 | Apply the naming strategy without composing a Function per property (#1628) | 3 | 3 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 566 | Use invokeExact instead of invokeWithArguments to call mapping handles (#1625) | 18 | 3 (16.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 571 | Discover generated config classes for transitive super interfaces (#1631) | 6 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 574 | Fix lazy group creation for nested groups with their own naming strategy (#1634) | 4 | 4 (100.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 581 | Discover transitive super interfaces of nested groups (#1638) | 6 | 4 (66.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| Total | Common measured commits | 1632 | 1223 (74.9%) | 30 (1.8%) | 23 (1.4%) | 41 | 52 | 64 |

## Other files

| Index | Subject | Executable lines | Dev (scope) covered | KTester covered | Ouédraogo covered | Dev (scope) lost | KTester lost | Ouédraogo lost |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
Unavailable: batch execution measures scope files only.
Production-change commits: 218. Commits with zero executable changed lines: 141. Commits without an executable-line measurement: 9.
