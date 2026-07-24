# capella-sysmlv2-bridge

**capella-sysmlv2-bridge** is a standalone Java toolchain for transforming Capella/Arcadia models into SysML v2 models represented by the SysON EMF metamodel.

The project supports pragmatic, incremental migration from Capella to SysML v2 while preserving semantic intent and reusing concepts from the [Arcadia SysML v2 library](https://github.com/eclipse-capella/arcadia-sysmlv2-lib).

> **Project status:** This project is under active development at Proof of Concept (PoC) level.

## Current Status

The project currently provides:

- a Java 21 Maven multi-module transformation architecture;
- an Equinox headless runtime based on Capella 7.0.1;
- direct loading of Capella `.capella` semantic models without opening a Sirius session or Eclipse workbench;
- a SysON SysML v2 target model using SysON 2026.7.0;
- resolution of the official `Arcadia::Component` library concept;
- a standalone Linux product built with Tycho;
- the production mapping `LogicalComponent -> PartUsage : Arcadia::Component`.

The standalone product has been manually validated without `DISPLAY` against the In-Flight Entertainment System model:

```text
Transformation completed
Source LogicalComponent count: 23
Generated PartUsage count: 23
Errors: 0
```

Textual SysML export, parser/linker validation, automated UI-bundle activation checks, and additional mappings are not implemented yet.

See [Project Structure and Status](doc/project-status.adoc) for the detailed architecture, current limitations, and roadmap.

## Repository Structure

### Maven Modules

- `arcadia-library`: discovers and validates Arcadia SysML v2 library resources.
- `capella-loader`: defines the headless Capella model loading API.
- `capella-runtime`: defines the Capella runtime initialization and lifecycle boundary.
- `capella2sysmlv2-transformer`: contains the transformation engine, traceability, diagnostics, target structure, and semantic mappings.
- `cli`: contains the Picocli command model and runtime-independent service contract.
- `integration-tests`: contains functional and integration-test scenarios.
- `test-support`: provides reusable test resources and golden-file assertions.

### Runtime And Distribution

- `eclipse/`: contains the Tycho reactor, Capella target platform, Equinox bundles, features, p2 repository, standalone product, and official-product integration tests.

### Documentation And Examples

- `doc/`: contains architecture, implementation status, limitations, and roadmap documentation.
- `examples/`: reserved for the future distributable In-Flight Entertainment System example.

## Architecture

```text
Capella model
              |
              v
Equinox headless runtime initialization
              |
              v
Capella semantic model loading
              |
              v
MetaclassId-based mapper dispatch
              |
              v
Shared TransformationContext
              |
              v
SysON SysML v2 model
              |
              v
Official SysON textual export             [not implemented yet]
              |
              v
SysML v2 parser/linker validation          [not implemented yet]
```

The transformation engine is independent from Equinox startup, model loading, textual serialization, and parser implementations. Mappers access Arcadia concepts through `TransformationContext.libraryResolver()` and do not load the library directly.

## Requirements

- Java 21
- Maven 3.9+
- Linux `x86_64` for the currently configured standalone product

SysON dependencies are retrieved from GitHub Packages. Maven credentials may be required depending on the repository access policy.

## Build

Build and test the regular Maven modules:

```bash
mvn clean verify
```

Build the Equinox bundles, p2 repository, and standalone product:

Install the dependencies:

- `arcadia-sysmlv2-lib:arcadia-library:jar:0.0.3` produced from the https://github.com/Obeo/arcadia-sysmlv2-lib repository content
- `org.eclipse.syson:syson-sysml-metamodel:jar:2026.7.0` produced from the https://github.com/eclipse-syson/syson repository content

```bash
mvn -Pcapella-runtime -DskipTests package
```

The Linux product is materialized under:

```text
eclipse/product/org.eclipse.capella.sysmlv2.bridge.product/
  target/products/org.eclipse.capella.sysmlv2.bridge.product/linux/gtk/x86_64/
```

## CLI

The standalone application exposes the `transform` command:

```bash
capella2sysmlv2 transform \
  --project /path/to/capella-project \
  --model model.capella \
  --arcadia-library /path/to/arcadia-library/folder \
  --workspace /path/to/workspace
```

The command currently creates the SysON target model in memory and reports transformation counts and diagnostics. It does not write a `.sysml` file yet.

## Known Limitations

- Only Capella `LogicalComponent` is mapped in production.
- The official SysON textual exporter is not connected.
- Generated SysML text cannot yet be parsed or linked for validation.
- SWT and Eclipse UI bundles are transitively installed because of Capella dependencies, although the validated execution does not create a workbench or require `DISPLAY`.
- UI bundle non-activation is not yet asserted by an automated product test.
- The standalone product still needs qualification with a real JDK 21 runtime; the latest manual product validation used Java 25.
- `--arcadia-library` is validated as a directory, while the production Arcadia library model currently comes from the bundled official artifact.

## Next Steps

1. Automate the standalone IFE acceptance test without `DISPLAY`.
2. Verify that SWT, Eclipse UI, and E4 UI bundles remain inactive.
3. Qualify the complete build and product with Java 21.
4. Integrate the official SysON textual exporter.
5. Add an output option and generate a `.sysml` file.
6. Validate generated text with SysON parser and linker services.
7. Extend mapping coverage incrementally with focused tests.

## Contributing

Contributions are welcome, including new transformation rules, improved semantic alignment, additional scenarios, tests, and performance improvements. Please open an issue or submit a pull request.

## License

Eclipse Public License v2.0.

## Contact

Maintained by Obeo, Thales, and contributors.
