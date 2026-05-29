# capella-sysmlv2-bridge

**capella-sysmlv2-bridge** provides model transformation and interoperability mechanisms between Capella/Arcadia models and SysML v2 environments.

The project aims to enable smooth data exchange and semantic alignment between the Arcadia ecosystem and next-generation SysML v2–based MBSE toolchains.

## 🎯 Objectives, Scope and Positioning

**capella-sysmlv2-bridge** is designed to support **migration paths from Capella/Arcadia to SysML v2 environments** while enabling practical interoperability between the two ecosystems.

> ⚠️ **Project status:** This project is currently under active development at **Proof of Concept (PoC)** level.
> Its intent is to mature through experimentation and, in the future, to contribute relevant capabilities back to the Capella project.


The project aims to:

- enable interoperability between Capella models and SysML v2 models
- support transformation workflows from Arcadia/Capella toward SysML v2
- preserve semantic intent across modeling environments
- facilitate migration experiments with controlled risk
- help organizations progressively introduce SysML v2 into existing MBSE toolchains
- remain extensible to accommodate different transformation strategies

This project complements **arcadia-sysmlv2-lib**:

- **arcadia-sysmlv2-lib** provides the SysML v2 semantic foundation aligned with Arcadia
- **capella-sysmlv2-bridge** enables transformation and migration from existing Capella models

The bridge focuses on **pragmatic, incremental migration scenarios** rather than large-scale one-shot conversions or defining a normative standard mapping.

## 📁 Repository Structure

### `/doc`

Documentation describing:

- transformation principles
- mapping assumptions
- supported scenarios
- known limitations
-
### `/transformations`

Contains the implementation of model transformations between Capella and SysML v2.

Depending on the implementation, this may include:

- model-to-model transformations
- helper utilities
- mapping definitions

### `/examples`

Provides sample models and transformation scenarios demonstrating how to use the bridge.


## 🚀 Getting Started

1. Clone the repository:
```bash
git clone https://github.com/<your-org>/capella-sysmlv2-bridge.git
```
2. Import the project into your modeling environment.
3. Review the /doc folder to understand the supported scenarios.
4. Run the example transformations from the /examples folder.

## 🤝 Contributing

Contributions are welcome, including:

- new transformation rules
- improved semantic alignment
- additional scenarios
- performance improvements

Please open an issue or submit a pull request.

## 📜 License
EPL v2

## ✉️ Contact

Maintained by Obeo, Thales and contributors.