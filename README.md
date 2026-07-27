# capella-sysmlv2-bridge

**capella-sysmlv2-bridge** is a standalone Java toolchain for transforming Capella/Arcadia models into SysML v2 models represented by the SysON EMF metamodel.

The project supports pragmatic, incremental migration from Capella to SysML v2 while preserving semantic intent and reusing concepts from the open-source [Arcadia SysML v2 library](https://github.com/eclipse-capella/arcadia-sysmlv2-lib).

> **Project status:** This project is under active development at Proof of Concept (PoC) level.

## Current Status

The project currently provides:

- a Java 21 Maven multi-module transformation architecture;
- an Equinox headless runtime based on Capella 7.0.1;
- direct loading of Capella `.capella` semantic models without opening a Sirius session or Eclipse workbench;
- a SysON SysML v2 target model using SysON 2026.7.0;
- resolution of the official `Arcadia::Component` library concept;
- standalone Linux and Windows products built with Tycho;
- intermediate SysML v2 XMI persistence with Arcadia library references;
- the production mapping `LogicalComponent -> PartUsage : Arcadia::Component`.

The standalone product has been manually validated without `DISPLAY` against the In-Flight Entertainment System model:

```text
Transformation completed
Source LogicalComponent count: 23
Generated PartUsage count: 23
Errors: 0
XMI model: /path/to/output/In-Flight Entertainment System.sysml.xmi
```

Official textual SysML export, parser/linker validation, automated UI-bundle activation checks, and additional mappings are not implemented yet.

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
Intermediate XMI persistence              [implemented]
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
- Linux or Windows `x86_64` for the standalone product

SysON dependencies are retrieved from GitHub Packages. Maven credentials may be required depending on the repository access policy.

## Build

Build and test the regular Maven modules:

```bash
mvn clean verify
```

Build the Equinox bundles, p2 repository, and standalone product:

Install the dependencies:

- `org.eclipse.arcadia:arcadia-library:jar:0.0.3` produced from the https://github.com/eclipse-capella/arcadia-sysmlv2-lib repository content
- `org.eclipse.syson:syson-sysml-metamodel:jar:2026.7.0` produced from the https://github.com/eclipse-syson/syson repository content

```bash
mvn -Pcapella-runtime -DskipTests package
```

The products are materialized under:

```text
eclipse/product/org.eclipse.capella.sysmlv2.bridge.product/
  target/products/org.eclipse.capella.sysmlv2.bridge.product/linux/gtk/x86_64/
  target/products/org.eclipse.capella.sysmlv2.bridge.product/win32/win32/x86_64/
```

The corresponding archives are generated as a Linux `.tar.gz` and a Windows `.zip` in the module's `target/products/` directory.

## Getting Started

Build the standalone products as described above, then download the official Capella 7.0.1 In-Flight Entertainment System sample and run the product.

### Linux

```bash
curl -L "https://www.eclipse.org/downloads/download.php?file=/capella/samples/7.0.1/InFlightEntertainmentSystem.zip&r=1" \
  -o InFlightEntertainmentSystem.zip

./capella2sysmlv2 InFlightEntertainmentSystem.zip output
```

### Windows PowerShell

```powershell
Invoke-WebRequest `
  -Uri "https://www.eclipse.org/downloads/download.php?file=/capella/samples/7.0.1/InFlightEntertainmentSystem.zip&r=1" `
  -OutFile "InFlightEntertainmentSystem.zip"

.\capella2sysmlv2c.exe InFlightEntertainmentSystem.zip output
```

The Windows distribution also contains `capella2sysmlv2.exe`; `capella2sysmlv2c.exe` is the console launcher and displays command output in the current terminal.

Both commands create:

```text
output/In-Flight Entertainment System.sysml.xmi
```

This XMI file is an intermediate serialization of the SysON model. It is reloaded and checked by the application after writing, including its `Arcadia::Component` references. It is not the final textual `.sysml` format.

## CLI

The standalone application takes a zipped Capella project and an output directory:

```bash
capella2sysmlv2 INPUT_ZIP OUTPUT_DIRECTORY
```

The application extracts the archive, discovers its single Eclipse project and `.capella` model, and initializes an internal workspace under `OUTPUT_DIRECTORY/.capella2sysmlv2/workspace`. The Arcadia library is bundled in the product. The command writes `<CAPELLA_MODEL_NAME>.sysml.xmi` to the output directory and reports its path, transformation counts, and diagnostics. It does not write a textual `.sysml` file yet.

## Known Limitations

- Only Capella `LogicalComponent` is mapped in production.
- The official SysON textual exporter is not connected.
- Generated SysML text cannot yet be parsed or linked for validation.
- The Windows product is materialized by Tycho but has not been executed in a Windows environment yet.
- SWT and Eclipse UI bundles are transitively installed because of Capella dependencies, although the validated execution does not create a workbench or require `DISPLAY`.
- UI bundle non-activation is not yet asserted by an automated product test.
- The standalone product still needs qualification with a real JDK 21 runtime; the latest manual product validation used Java 25.

## Next Steps

1. Automate the standalone IFE acceptance test without `DISPLAY`.
2. Verify that SWT, Eclipse UI, and E4 UI bundles remain inactive.
3. Qualify the complete build and product with Java 21.
4. Integrate the official SysON textual exporter.
5. Replace the intermediate XMI output with the generated textual `.sysml` file.
6. Validate generated text with SysON parser and linker services.
7. Execute and qualify the materialized product on Windows.
8. Extend mapping coverage incrementally with focused tests.

## Contributing

Contributions are welcome, including new transformation rules, improved semantic alignment, additional scenarios, tests, and performance improvements. Please open an issue or submit a pull request.

## License

Eclipse Public License v2.0.

## Contact

Maintained by Obeo, Thales, and contributors.
