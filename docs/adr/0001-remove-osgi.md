# Remove OSGi and run GMP as a plain Java application

Status: accepted (2026-08-23)

GMP runs on Apache Felix with a dead tooling stack (pax-runner 1.9.0, Felix
5.4.0, pax-logging, pax-web) that pins the project to Java 8. Nothing uses
OSGi's dynamic capabilities in a meaningful way: bundles are never
hot-swapped, and the only runtime-dynamic behavior in use is FileInstall's
live config reload. We decided to remove OSGi entirely rather than upgrade
the OSGi stack in place (Felix 7.x would run on modern JVMs, but keeps the
complexity tax and the rotting pax ecosystem forever).

## The shape of the replacement

- **Wiring.** The 33 hand-written `BundleActivator`s and `ServiceTracker`s
  are replaced by a single hand-wired composition root (`Main`). No DI
  framework. Whiteboard-style patterns (collecting all registered
  implementations, e.g. `KeywordActorsFactory`) become explicit lists in the
  composition root.
- **Configuration.** The `conf/services/*.cfg` files and per-instrument
  overrides under `instances/` are kept verbatim. A small loader parses them
  once at startup and feeds the composition root. Hot reload is dropped:
  config changes require a restart. A component with no config present is
  not constructed, mirroring today's ManagedServiceFactory behavior.
- **One binary.** A single `Main` serves all instruments (gpi, ghost,
  igrins2); config decides what gets constructed. The distribution keeps its
  tar.gz/RPM shape: `lib/*.jar` + `conf/` + a launcher script replacing
  pax-runner.
- **Logging.** The 148 files using `java.util.logging` are untouched;
  pax-logging is replaced by the `jul-to-slf4j` bridge plus logback, with a
  `logback.xml` that preserves the `gmp.log` location, daily rollover and
  line format.
- **Tests.** End-to-end Pax Exam integration tests are rewritten as plain
  JUnit against the composition root; OSGi-mechanical tests are dropped.
- **Module layout.** The 45-module Maven structure stays; only the
  maven-bundle-plugin machinery and `osgi/` packages are removed.

## Staging

1. **Stage 1 — delete dead directories.** ~17 stale `gds-*` directories
   (GDS was long ago consolidated into the single `gds` module), `gmp-tui`,
   `gmp-commands-records`, `integrationtests/`, and any other directory not
   referenced by the root pom or `instances/`. They are not in the build and
   mislead any scan of the codebase. Top-level `gpi/` (gpi-observation-status)
   was initially kept because the `gpi` distribution profile depended on it,
   but was deleted during Stage 2: it was still OSGi, outside the reactor, and
   its GDS-event hook (an EventAdmin subscription) had been dead code since the
   GDS rewrite — nothing publishes those events anymore. The `gpi` profile now
   only selects the `instances/gpi` config overlay.
2. **Stage 2 — de-OSGi on Java 8.** Build the config loader, composition
   root, and launcher; delete Felix/pax/OSGi APIs; prove parity against
   today's behavior on the same JVM (app boots, rewritten integration tests
   pass, all instrument distributions produce working artifacts).
3. **Stage 3 — JDK 25 (done 2026-10-02).** Bumped to JDK 25 and Scala
   2.13.18. Kept separate from Stage 2 so JVM breakage (removed JavaEE APIs,
   encapsulated JDK internals) is never confused with de-OSGi breakage. What
   the JVM actually forced:
   - JAXB left the JDK in 11: the four XML-configured modules now depend on
     `jakarta.xml.bind-api` + `jaxb-runtime` and generate with
     `org.jvnet.jaxb:jaxb-maven-plugin`. Imports moved from `javax.xml.bind`
     to `jakarta.xml.bind`; the generated classes previously committed under
     `gmp-status-simulator/src/main/java` are now generated into `target/`.
   - The `gds-fp` OSGi fat bundle (fs2/http4s/circe plus its own copy of the
     Scala library) is rejected by the stricter zip parser in modern JDKs
     and would clash with Scala 2.13.18 anyway. `gds` now depends directly on
     `fs2-io`, `http4s-ember-server`, `http4s-dsl`, `http4s-circe` and
     `log4s` at the versions the bundle pinned.
   - Build plugins that predate the module system were bumped (compiler
     3.14, surefire/failsafe 3.5, scala-maven-plugin 4.10, source plugin
     3.3); `-Xverify:none` and the `-Xbootclasspath/p:` surefire argLine are
     gone. Mockito moved to 5.x (ByteBuddy support for recent class files).
   - ActiveMQ 5.6.0 did **not** need to change: the broker, the vm://
     transport and failover all work on JDK 25 and the end-to-end test
     passes. Upgrading it remains optional follow-up work.

## Considered and rejected

- **Upgrading Felix/pax in place** — cheaper, but keeps OSGi complexity and
  dead tooling permanently.
- **A DI framework (Guice etc.)** — would touch every class with
  annotations, violating the constraint that changes stay OSGi-related.
- **Per-instrument `Main` classes** — would split component selection
  between code and config; config already owns it.
- **Hot config reload via a directory watcher** — rebuilds a chunk of
  ConfigAdmin by hand for a capability operators can get with a restart.
