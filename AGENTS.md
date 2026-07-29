# AGENTS.md

## Project Goal

Build `capella2sysmlv2`, a standalone Java model-to-model transformation from Capella models to SysML v2 models using the SysML v2 model provided by SysON.

The tool must:

* run outside the Capella UI;
* use Java 21, Maven and EMF;
* produce SysON SysML v2 models;
* delegate textual SysML export to SysON tooling;
* avoid dependency on the SysML v2 Reference Implementation;
* support TDD with unit, functional and integration tests.

The transformation targets the Arcadia SysML v2 library and must reuse Arcadia concepts instead of redefining them.

---

## Maven Coordinates

* Group ID: `org.obeonetwork.capella.sysmlv2`
* Parent Artifact ID: `capella-sysmlv2-parent`
* Java package root: `org.obeonetwork.capella.sysmlv2`

---

## Architecture

### Transformation Pipeline

```text
Capella EObject
      ↓
Type dispatch
      ↓
ElementMapper
      ↓
TransformationContext
      ↓
SysON SysML v2 model
      ↓
SysON serializer
      ↓
Parser / linker validation
```

Detailed workflow:

1. Resolve installed Capella metamodel bundles through the Equinox application adapter.
2. Register the Capella EMF packages and load the `.capella` resource directly through `capella-loader`.
3. Resolve the Arcadia SysML v2 library through `arcadia-library` and expose semantic lookups through `LibraryResolver`.
4. Dispatch Capella `EObject`s by stable `MetaclassId`.
5. Invoke registered `ElementMapper`s.
6. Store shared execution state in `TransformationContext`.
7. Produce a SysON SysML v2 model.
8. Delegate textual export to SysON tooling.
9. Validate generated files using parser/linker services when available.

---

## Arcadia SysML v2 Library

The transformation targets the Arcadia SysML v2 library provided by:

https://github.com/eclipse-capella/arcadia-sysmlv2-lib

Generated SysML v2 models must:

* reuse Arcadia SysML v2 concepts whenever available;
* import/reference Arcadia libraries instead of redefining equivalent concepts;
* remain compatible with future versions of the Arcadia SysML v2 library.

Access to the Arcadia library must be isolated behind library discovery and resolver APIs.
Mappers must not navigate or load the Arcadia library directly; they must use `TransformationContext.libraryResolver()`.

Never hardcode absolute paths.

---

## Transformation Engine Rules

### Metaclass Identity

Mapper lookup uses `MetaclassId(nsURI, className)`, not `EClass` instances and not class names alone.

### ElementMapper

Each mapper declares its source type and receives the shared context:

```java
interface ElementMapper {
    MetaclassId sourceType();
    void transform(EObject source, TransformationContext context);
}
```

Mappings must never instantiate or locate other mappers directly.

### MapperRegistry

The mapper registry stores `Map<MetaclassId, ElementMapper>` and does not depend on EMF runtime `EClass` instances.

### TransformationDispatcher

The dispatcher only:

* computes `MetaclassId.of(source.eClass())`;
* locates the mapper;
* invokes it;
* reports missing mapper diagnostics.

No transformation logic belongs in the dispatcher.

### TransformationContext

The context owns mutable state for one transformation execution:

* source-to-target traceability;
* generated SysML element registry;
* deferred reference resolution;
* diagnostics;
* Arcadia library resolution;
* transformation options.

Mappers must not maintain mutable transformation state themselves.

---

## Design Principles

### Separation Of Concerns

The project separates:

* model loading;
* transformation dispatch;
* semantic mapping;
* library resolution;
* textual serialization through SysON;
* validation.

Keep responsibilities isolated.

### Runtime Isolation

Only the Equinox application adapter may discover installed Capella metamodel bundles and register their EMF packages.
`capella-loader` must remain independent of Eclipse, Sirius, and Capella runtime initialization.
Loading semantic data must not require an Eclipse workspace, Sirius session, `.aird` file, or editing domain.

---

## Coding Rules

### General

* Use Java 21.
* Use Maven multi-module.
* Use JUnit 5.
* Use AssertJ.
* Prefer immutable records where appropriate.
* Prefer constructor injection.
* Prefer small and testable services.
* Keep APIs minimal.
* Keep modules loosely coupled.

### Forbidden Dependencies

Do not:

* introduce Capella UI dependencies;
* introduce Eclipse UI dependencies;
* depend on the SysML v2 Reference Implementation;
* couple transformers to parser implementations;
* hardcode local machine paths;
* maintain a local SysML v2 serializer in this repository.

### Arcadia Library

Do not:

* redefine Arcadia SysML v2 concepts already provided by the library;
* embed Arcadia concepts directly in generated models when a library reference can be used.

---

## Testing Strategy

The project follows a TDD approach.

For every feature:

1. Add or update a minimal input model or metamodel.
2. Add focused assertions or golden files when textual output is involved.
3. Execute the failing test.
4. Implement the smallest possible change.
5. Validate generated output or model structure.
6. Validate using parser/linker services when available.

Unit tests must not require:

* Capella runtime;
* external repositories.

---

## Modules

### arcadia-library

Responsibilities:

* locate Arcadia SysML v2 libraries;
* validate local Arcadia SysML v2 library checkouts;
* expose discovered `.sysml` files in deterministic order;
* avoid transformation semantics.

### capella-loader

Responsibilities:

* load Capella models;
* expose model loading services;
* remain independent of Eclipse and Sirius runtime APIs.

### capella2sysmlv2-transformer

Responsibilities:

* provide the Java transformation engine;
* provide mapper registry and dispatch infrastructure;
* hold handwritten semantic mappings;
* produce SysON SysML v2 models.

### cli

Responsibilities:

* command-line interface;
* transformation command;
* configuration handling.

### integration-tests

Responsibilities:

* functional scenarios;
* integration scenarios;
* IFE validation scenarios.

### test-support

Responsibilities:

* golden-file assertions;
* directory comparisons;
* test resource helpers;
* reusable testing infrastructure.

---

## Build

The project must always pass:

```bash
mvn clean verify
```

All new functionality must be accompanied by tests.
