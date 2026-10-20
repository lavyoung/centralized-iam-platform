# Iteration 0 Baseline

## Scope

Iteration 0 introduces the target module boundaries while retaining the three active `centralized-authorize-*` modules
as a migration area. No business use case has moved yet.

## Completed

- Renamed the parent Maven coordinate to `com.lavyoung:centralized-iam-platform`.
- Added the nine target architecture modules under the `com.lavyoung.iam` root package.
- Defined dependencies for shared kernel, API contract, domain, application, inbound/outbound adapters, bootstrap, and
  both Java SDK modules.
- Enabled Maven tests by default instead of globally setting `skipTests=true`.
- Added five ArchUnit rules covering framework-independent domain code and inward dependency direction.
- Aligned Spring Authorization Server with the version managed by Spring Boot 3.2.4.
- Fixed three Java 17 compilation incompatibilities in the legacy server.
- Made the legacy RocketMQ listener switchable so protocol tests can disable the broker dependency.
- Replaced the legacy protocol test's database repositories with Spring Authorization Server's official in-memory
  stores.
- Removed the unused `centralized-authorize-client` placeholder; Java integration is now owned by the two target SDK
  modules.

## Verification

The following command succeeds across all 13 reactor projects:

```bash
mvn -DskipTests compile
```

The architecture suite succeeds with five tests:

```bash
mvn -pl centralized-iam-bootstrap -am test
```

The legacy protocol suite succeeds without MySQL and covers token issuance, invalid client credentials, and token
introspection:

```bash
mvn -pl centralized-authorize-server -am test
```

## Next Work

- Add stable baseline assertions for refresh and revocation behavior when those flows are migrated.
- Migrate the first vertical use case, client registration and lookup, through the new hexagonal modules.

The next implementation step is client registration and lookup. It will establish the first complete path through
contract, inbound adapter, application use case, domain port, and outbound adapter.
