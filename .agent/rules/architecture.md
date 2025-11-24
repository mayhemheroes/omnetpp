---
trigger: always_on
glob:
description: OMNeT++ Project Overview and Architecture
---
# OMNeT++ Project Overview

This document provides a high-level overview of the OMNeT++ project structure, architecture, and development workflows. It is designed to help agents and developers quickly understand the codebase.

## Project Summary
OMNeT++ is a public-source, component-based, modular, and open-architecture discrete event simulation environment. It is primarily used for simulating communication networks but is applicable to other domains like IT systems, queueing networks, and hardware architectures.

## Directory Structure & Component Mapping

### Key Files
-   `configure.user`: User-configurable build options.
-   `setenv`: Script to set up environment variables (`PATH`, `LD_LIBRARY_PATH`, etc.). Source this before running OMNeT++.

### Directory Structure
| Directory | Component | Description |
| :--- | :--- | :--- |
| `/` | **Root** | Contains build scripts (`configure`, `Makefile`), `install.sh`, and documentation. |
| `src/` | **Core Source** | The main C++ source code for the simulation system. |
| `src/sim/` | **Simulation Kernel** | The core discrete event simulation engine (`cModule`, `cMessage`, etc.). |
| `src/envir/` | **Envir** | Common code for runtime user interfaces. |
| `src/cmdenv/` | **Cmdenv** | Command-line runtime user interface. |
| `src/qtenv/` | **Qtenv** | Qt-based graphical runtime user interface. |
| `src/nedxml/` | **NEDXML** | NED language parser, generator, and validator (used by `nedtool`). |
| `src/scave/` | **Scave** | Library for processing result files (vector and scalar data). |
| `src/common/` | **Common** | Common utility classes used across the project. |
| `ui/` | **IDE** | Source code for the OMNeT++ Integrated Development Environment (Eclipse-based). |
| `python/` | **Python Bindings** | Python API for OMNeT++ (`omnetpp` package). |
| `samples/` | **Samples** | Sample simulations (e.g., `aloha`, `tictoc`). |
| `doc/src/` | **Documentation** | Contains manuals (User Guide, Simulation Manual), API references (`api/`), and guides (Install Guide, IDE Developers Guide). |
| `include/` | **Public API** | The public C++ header files for the simulation kernel. `omnetpp.h` is the main entry point. `omnetpp/` contains the headers. |
| `test/` | **Tests** | Regression test suite. Includes `core/` (kernel tests), `anim/` (animation tests), `scave/` (analysis tool tests), and `ide/` (IDE tests). |

## Architecture Highlights

### Simulation Kernel (`src/sim`)
The heart of OMNeT++. It provides the infrastructure for:
-   **Component Model**: Modules, gates, connections.
-   **Event Scheduling**: FES (Future Event Set) management.
-   **Random Number Generation**: RNG interfaces and distributions.
-   **Statistics**: Recording scalars, vectors, and histograms.

### User Interfaces (`src/envir`, `src/cmdenv`, `src/qtenv`)
OMNeT++ simulations can run under different user interfaces. `Envir` provides the common interface, while `Cmdenv` (CLI) and `Qtenv` (GUI) implement specific runtime behaviors.

### IDE (`ui/`)
The OMNeT++ IDE is built on Eclipse. The plugins are located in the `ui/` directory.

| Plugin | Role & Responsibility |
| :--- | :--- |
| **Core & Infrastructure** | |
| `org.omnetpp.main` | Main entry point, branding, and initialization. |
| `org.omnetpp.main.omnetpp` | OMNeT++ specific product branding. |
| `org.omnetpp.common` | Shared UI utilities, widgets, and helper classes used by other plugins. |
| `org.omnetpp.common.core` | Core (non-UI) shared utilities. |
| `org.omnetpp.ide.nativelibs` | Loads native libraries required by the IDE. |
| `org.omnetpp.ide.nativelibs.*` | Platform-specific native libraries (Linux, macOS, Windows). |
| **NED Language** | |
| `org.omnetpp.ned.core` | NED language parsing, validation, and model (headless/core). |
| `org.omnetpp.ned.model` | Interfaces for the NED model. |
| `org.omnetpp.ned.editor` | Graphical and text-based NED editor. |
| `org.omnetpp.neddoc` | Generator for NED documentation. |
| **Simulation & Launch** | |
| `org.omnetpp.launch` | Launch configurations for running and debugging simulations. |
| `org.omnetpp.cdt` | Integration with Eclipse CDT (C/C++ Development Tooling) for project management. |
| `org.omnetpp.dsp` | Debug Server Protocol support for non-interactive debugging/launching. |
| **Analysis (Scave)** | |
| `org.omnetpp.scave` | The Analysis Tool (Scave) UI for plotting and analyzing results. |
| `org.omnetpp.scave.model` | Data model for Scave (datasets, charts). |
| `org.omnetpp.scave.builder` | Builder for indexing result files (`.vec`, `.sca`). |
| `org.omnetpp.scave.pychart` | Support for Python-based charts in Scave. |
| `org.omnetpp.scave.templates` | Templates for Scave charts. |
| **Editors & Views** | |
| `org.omnetpp.inifile.editor` | Editor for `omnetpp.ini` configuration files (form and text modes). |
| `org.omnetpp.msg.editor` | Text editor for `.msg` (message definition) files. |
| `org.omnetpp.eventlogtable` | Viewer for simulation event logs. |
| `org.omnetpp.sequencechart` | Sequence chart visualization for event logs. |
| `org.omnetpp.figures` | Draw2d figures used in graphical editors. |
| **Integration & Misc** | |
| `org.omnetpp.python` | Python integration support. |
| `org.omnetpp.doc` | Help content and documentation integration. |
| `org.swtworkbench.xswt` | Support for XSWT (XML-based SWT UI definition). |
| `org.visigoths.freemarker` | Wrapper for the FreeMarker template engine. |
| `org.visigoths.freemarker.ide` | IDE support for FreeMarker templates. |

## Development Workflow

### Build System
The project uses a custom build system based on `make`.
-   **Configuration**: `./configure` detects system environment and generates `Makefile.inc`.
-   **Build**: `make` (or `make -j<cores>`) builds the project. MODE=release or MODE=debug can be specified.
-   **Build Environment**: ghcr.io/omnetpp/distrobuild can be used to build the project.
-   **Quick Install**: `./install.sh` (Linux/macOS) automates the build process and installs dependencies.
-   **IDE**: The native library for the IDE is built with `make ui`.

### Running Tests
The `test/` directory contains the regression test suite.
-   **Run all tests**: `make tests` (from root) or `make` (from `test/`).
-   **Run specific suite**: `make test_core`, `make test_envir`, etc. (from `test/`).
-   **Run individual tests**: Go to a subdirectory (e.g., `test/core`) and run `./runtest <testfile>`.
    -   Example: `./runtest cModule_creation_1.test`
    -   Without arguments, `./runtest` runs all tests in the directory.
-   **Mechanism**: Tests are typically `.test` files processed by the `opp_test` tool, which generates C++ code, builds it, runs it, and compares output against expected results.

## Tips for Agents
-   **Navigation**: Use `find_by_name` to locate specific files as the source tree is large.
-   **Context**: When working on the kernel, focus on `src/sim` and `include`. When working on tools, check `src/nedxml` or `src/scave` or `src/utils`. Common code is in `src/common`.
-   **IDE**: The IDE code is in `ui/` and follows Eclipse plugin structure (`plugin.xml`, `MANIFEST.MF`). It can be built with maven.
-   **Python**: Python bindings are in `python/`.

