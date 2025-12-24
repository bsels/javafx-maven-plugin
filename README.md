# javafx-maven-plugin

[![Latest version](https://img.shields.io/github/v/release/bsels/javafx-maven-plugin?color=blue&label=GitHub+Tag)](https://github.com/bsels/javafx-maven-plugin/releases)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.bsels/javafx-maven-plugin?color=blue&label=Maven+Central)](https://search.maven.org/artifact/io.github.bsels/javafx-maven-plugin)

[![Push create release](https://github.com/bsels/javafx-maven-plugin/actions/workflows/push-release.yaml/badge.svg)](https://github.com/bsels/javafx-maven-plugin/actions/workflows/push-release.yaml)
[![Release Build](https://github.com/bsels/javafx-maven-plugin/actions/workflows/release-build.yaml/badge.svg?event=release)](https://github.com/bsels/javafx-maven-plugin/actions/workflows/release-build.yaml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A concise Maven plugin that has been inspired from the JavaFX Maven Plugin
([openjfx/javafx‑maven‑plugin](https://github.com/openjfx/javafx-maven-plugin)).

## Features

| Goal              | What it does                                                                                                                                                                                                                                                                       | Typical use‑case                                                                                                                                                                  |
|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`fxml-source`** | Converts each **FXML** file in the project into a corresponding **Java source file**. The generated classes expose the UI structure as plain Java code, which can be compiled together with the rest of the project.                                                               | When you want compile‑time safety for FXML bindings, avoid runtime loading of FXML resources, or need to ship a pure‑Java UI without external `.fxml` files.                      |
| **`jlink`**       | Invokes the **`jlink`** tool to create a **stand‑alone, custom runtime image** that contains only the modules required by the application. The result is a self‑contained directory (or installer) that can be distributed and run on target machines without a pre‑installed JDK. | Packaging a production‑ready JavaFX application that should run out‑of‑the‑box on end‑users’ computers, minimizing size and eliminating the need for a separate JRE installation. |
| **`run`**         | Executes the project in the development environment using the same launch logic provided by the upstream **JavaFX Maven Plugin**. It assembles the module path, sets up the JavaFX runtime, and starts the main class so you can iterate quickly while coding.                     | During development, to test UI changes, verify module configurations, or debug the application without building a full distribution.                                              |

These three goals together give you a smooth workflow: generate type‑safe UI code (`fxml-source`), test and develop
rapidly (`run`), and finally produce a lean, portable executable bundle (`jlink`).

## Requirements—What does the plugin need?

- **Java version**: The codebase must be compiled and run on **Java 25 or later**.
  This ensures access to the latest language features, module system improvements, and the `jlink` tool enhancements
  introduced in recent JDK releases.
- **Modular architecture**: The application is built as a **Java module** (using `module-info.java`).
  All source files, libraries, and resources must be declared in explicit modules, allowing the JDK to enforce strong
  encapsulation and reliable configuration at compile‑time and run‑time.
- **FXML handling**:
    - All **FXML files** reference **classes that are part of the compiled classpath** (i.e., they must already be
      compiled into the module), or that can be compiled without the source code that needs to be generated
      (optimistic compilation: compiles everything that can be compiled without errors).
    - The **generic‑type references** used in those FXML files may reside in source code that has **not yet been
      compiled**; the plugin will generate the necessary Java source files that will require those classes to be
      part of the compilation classpath.

## fxml-source Maven Goal

The **`fxml-source`** goal converts JavaFX FXML files into plain‑Java source code during the **`generate‑sources`**
phase.
It is used to reduce runtime dependencies because the JavaFX FXML loader is not needed at runtime.

### What It Does

1. **Scans** a user‑specified directory for `*.fxml` files.
2. **Parses** each file (`FXMLReader`) and builds an internal model (`FXMLProcesor`).
3. **Generates** a Java class that mirrors the FXML hierarchy – fields, methods, imports, and optional resource‑bundle
   handling.
4. **Writes** the generated `.java` files into a configurable output folder and adds that folder to the project’s
   compile source roots so the classes are compiled together with the rest of the codebase.

### Key Configuration Parameters

| Parameter                            | Property                               | Required?      | Default                                             | Description                                                                                                                                                                                       |
|--------------------------------------|----------------------------------------|----------------|-----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `project`                            | `${project}`                           | Yes (injected) | –                                                   | Maven project reference – used to add the generated source directory to the compile classpath.                                                                                                    |
| `fxmlDirectory`                      | `javafx.fxml.directory`                | Yes            | –                                                   | Directory that contains the FXML files to process.                                                                                                                                                |
| `packageName`                        | `javafx.fxml.package`                  | No             | (no package)                                        | Base Java package for the generated classes. If omitted, classes are placed directly under the generated‑source folder.                                                                           |
| `resourceBundleObject`               | `javafx.fxml.resourceBundleObject`     | No             | –                                                   | Fully‑qualified name of a resource‑bundle class used for i18n look‑ups inside the generated code.                                                                                                 |
| `generatedSourceDirectory`           | `javafx.fxml.generatedSourceDirectory` | Yes            | `${project.build.directory}/generated-sources/fxml` | Destination folder for the generated Java files.                                                                                                                                                  |
| `debugInternalModel`                 | `javafx.fxml.debug.internal.model`     | No             | `false`                                             | When `true`, the plugin logs the intermediate JSON representation of the parsed and processed FXML models for troubleshooting.                                                                    |
| `fxmlParameterizations`              | `javafx.fxml.parameterization`         | No             | –                                                   | A list of `FXMLParameterized` objects that allow custom root parameters, interface mappings, and other fine‑grained tweaks to the generated code.                                                 |
| `includeSourceFilesInClassDiscovery` | `javafx.fxml.include.source.discovery` | No             | `false`                                             | When `true`, the plugin tries to add as much of the uncompiled source code to the discovery classpath. It will only include classes that can be compiled without errors (optimistic compilation). |

### Typical Usage in `pom.xml`

```xml

<build>
    <plugins>
        <plugin>
            <groupId>io.github.bsels</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>1.1.0</version>

            <executions>
                <execution>
                    <id>generate-fxml-sources</id>
                    <goals>
                        <goal>fxml-source</goal>
                    </goals>
                    <phase>generate-sources</phase>
                    <configuration>
                        <!-- Where your .fxml files live -->
                        <fxmlDirectory>${project.basedir}/src/main/resources/fxml</fxmlDirectory>

                        <!-- Package for the generated classes -->
                        <packageName>com.myapp.ui.generated</packageName>

                        <!-- Optional: custom resource bundle -->
                        <resourceBundleObject>com.myapp.i18n.Messages</resourceBundleObject>

                        <!-- Enable debugging of the internal model (optional) -->
                        <debugInternalModel>true</debugInternalModel>

                        <!-- Example of parameterization (optional) -->
                        <fxmlParameterizations>
                            <FXMLParameterized>
                                <className>MyView</className>
                                <interfaces>
                                    <InterfacesWithMethod>
                                        <interfaceName>com.myapp.api.Viewable</interfaceName>
                                        <methodNames>
                                            <method>initialize</method>
                                        </methodNames>
                                    </InterfacesWithMethod>
                                </interfaces>
                            </FXMLParameterized>
                        </fxmlParameterizations>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### When to Use It

- You want **type‑safe Java** access to UI components defined in FXML without a manual controller boilerplate.
- Your project relies heavily on **code generation** for UI scaffolding or wants to keep UI definitions declarative
  while still benefiting from IDE navigation and refactoring.
- You need automatic i18n support via a resource bundle that is wired into the generated code.

### Tips & Gotchas

- **Package name:** If omitted, generated classes end up in the root of the generated‑source folder, which can cause
  naming collisions.
- **Debug mode:** Turn on debugInternalModel only when troubleshooting; the JSON dumps can be large.
- **Classpath:** Ensure any custom JavaFX controls or third‑party libraries used in the FXML are declared as Maven
  dependencies; otherwise class loading will fail.
- **Incremental builds:** The plugin does not currently check timestamps, so it regenerates all files on each run.
  Consider cleaning the generated folder only when necessary.
- **Use generics:** Generics need to be explicitly declared in the FXML file, otherwise the plugin will fail.
  This is done by XML comment: `<!-- generic <index>: <full.qualified.type> -->`, e.g.
  `<!-- generic 0: java.lang.Integer -->`.

### Example FXML File

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.Spinner?>
<?import javafx.scene.layout.ColumnConstraints?>
<?import javafx.scene.layout.GridPane?>
<?import javafx.scene.layout.RowConstraints?>
<GridPane hgap="4.0" maxHeight="-Infinity" maxWidth="-Infinity" minHeight="-Infinity" minWidth="-Infinity"
          prefWidth="600.0" vgap="2.0" xmlns:fx="http://javafx.com/fxml/1" xmlns="http://javafx.com/javafx/17.0.12">
    <columnConstraints>
        <ColumnConstraints hgrow="NEVER"/>
        <ColumnConstraints hgrow="SOMETIMES" minWidth="10.0" prefWidth="100.0"/>
    </columnConstraints>
    <rowConstraints>
        <RowConstraints minHeight="10.0" prefHeight="30.0" vgrow="SOMETIMES"/>
        <RowConstraints minHeight="10.0" prefHeight="30.0" vgrow="SOMETIMES"/>
        <RowConstraints minHeight="10.0" prefHeight="30.0" vgrow="SOMETIMES"/>
    </rowConstraints>
    <children>
        <Label text="Integer spinner:"/>
        <Label text="Double spinner:" GridPane.rowIndex="1"/>
        <Label text="Enum spinner:" GridPane.rowIndex="2"/>
        <Spinner fx:id="spinnerInteger" maxWidth="1.7976931348623157E308" GridPane.columnIndex="1">
            <!-- generic 0: java.lang.Integer -->
        </Spinner>
        <Spinner fx:id="spinnerDouble" maxWidth="1.7976931348623157E308" GridPane.columnIndex="1" GridPane.rowIndex="1">
            <!-- generic 0: java.lang.Double -->
        </Spinner>
        <Spinner fx:id="spinnerEnum" maxWidth="1.7976931348623157E308" GridPane.columnIndex="1" GridPane.rowIndex="2">
            <!-- generic 0: java.time.DayOfWeek -->
        </Spinner>
    </children>
    <padding>
        <Insets bottom="8.0" left="8.0" right="8.0" top="8.0"/>
    </padding>
</GridPane>
```

---

With this configuration, running `mvn generate-sources` (or any later lifecycle phase) will automatically produce
ready‑to‑compile Java classes that mirror your FXML UI definitions, letting you treat the UI as ordinary Java code while
preserving the declarative benefits of FXML.

## jlink Maven Goal

The **`jlink`** goal creates a custom, minimal Java runtime image that contains only the modules required by your JavaFX
application.
It leverages the JDK’s `jlink` tool and aads JavaFX‑specific handling (native access, launcher scripts, additional
binaries,logging configuration, optional ZIP packaging, etc.).

### When to Use It

- You want a **stand‑alone executable** that ships only the Java runtime and the JavaFX modules your app needs—no
  separate JRE installation required.
- You need to **reduce the size** of the distribution by stripping debug info, removing header files/man pages, and
  applying compression.
- You require **custom launcher scripts** (extra JVM options, additional binaries, command‑line arguments) or a *
  *pre‑configured logging format**.
- You want the final image optionally **zipped** for easy distribution.

### Key Configuration Parameters

| Parameter                  | Property                          | Default      | Description                                                                        |
|----------------------------|-----------------------------------|--------------|------------------------------------------------------------------------------------|
| `mainClass`                | `javafx.mainClass`                | **required** | Fully‑qualified main class (or `module/name` if it contains a slash).              |
| `jlinkExecutable`          | `javafx.jlinkExecutable`          | `jlink`      | Path or name of the `jlink` binary.                                                |
| `jmodsPath`                | `javafx.jmodsPath`                | –            | Directory containing JavaFX `jmod` files.                                          |
| `jlinkImageName`           | `javafx.jlinkImageName`           | `image`      | Folder name of the generated runtime image.                                        |
| `jlinkZipName`             | `javafx.jlinkZipName`             | –            | Base name for the optional ZIP archive (`${name}.zip`).                            |
| `launcher`                 | `javafx.launcher`                 | –            | Name of the launcher script to create (`bin/<launcher>`).                          |
| `options`                  | –                                 | –            | List of extra JVM options added to the launcher script.                            |
| `commandlineArgs`          | `javafx.args`                     | –            | Arguments appended to the launcher’s `$@` placeholder.                             |
| `additionalBinaries`       | `javafx.additionalBinaries`       | –            | Files copied into `bin/` and exposed as `-D<property>=./<file>` in the launcher.   |
| `stripDebug`               | `javafx.stripDebug`               | `false`      | Adds `--strip-debug` to `jlink`.                                                   |
| `stripJavaDebugAttributes` | `javafx.stripJavaDebugAttributes` | `false`      | Adds `--strip-java-debug-attributes`.                                              |
| `compress`                 | `javafx.compress`                 | `0`          | Compression level (`0‑2` or `zip‑0`-`zip‑9`). Invalid values abort the build.      |
| `noHeaderFiles`            | `javafx.noHeaderFiles`            | `false`      | Adds `--no-header-files`.                                                          |
| `noManPages`               | `javafx.noManPages`               | `false`      | Adds `--no-man-pages`.                                                             |
| `bindServices`             | `javafx.bindServices`             | `false`      | Adds `--bind-services`.                                                            |
| `ignoreSigningInformation` | `javafx.ignoreSigningInformation` | `false`      | Adds `--ignore-signing-information`.                                               |
| `jlinkVerbose`             | `javafx.jlinkVerbose`             | `false`      | Adds `--verbose`.                                                                  |
| `loggingFormat`            | `javafx.loggingFormat`            | –            | Overrides `java.util.logging.SimpleFormatter.format` in `conf/logging.properties`. |

### Typical Usage in `pom.xml`

```xml

<plugin>
    <groupId>io.github.bsels</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>1.1.0</version>

    <executions>
        <execution>
            <id>jlink-image</id>
            <goals>
                <goal>jlink</goal>
            </goals>
            <phase>process-classes</phase>
            <configuration>
                <!-- Mandatory -->
                <mainClass>com.example.MainApp</mainClass>

                <!-- Optional – customise the image -->
                <jlinkExecutable>jlink</jlinkExecutable><!-- path to jlink -->
                <jmodsPath>${java.home}/jmods</jmodsPath><!-- custom JavaFX jmods -->
                <jlinkImageName>my-ap-image</jlinkImageName><!-- output folder -->
                <jlinkZipName>my-app-runtime</jlinkZipName> <!-- ZIP name (omit to skip) -->

                <!-- Launcher configuration -->
                <launcher>myapp</launcher><!-- name of the launcher -->
                <options>
                    <option>-Xmx512m</option>
                    <option>-Dmy.prop=value</option>
                </options>
                <commandlineArgs>--theme dark</commandlineArgs>

                <!-- Binary files to ship alongside the launcher -->
                <additionalBinaries>
                    <additionalBinary>
                        <name>ffmpeg</name>
                        <location>${project.basedir}/native/ffmpeg.exe</location>
                        <mappedJavaProperty>ffmpeg.path</mappedJavaProperty>
                    </additionalBinary>
                </additionalBinaries>

                <!-- Image optimisation flags -->
                <stripDebug>true</stripDebug>
                <stripJavaDebugAttributes>true</stripJavaDebugAttributes>
                <compress>zip-9</compress><!-- 0‑9 or 1/2 (deprecated) -->
                <noHeaderFiles>true</noHeaderFiles>
                <noManPages>true</noManPages>
                <bindServices>true</bindServices>
                <ignoreSigningInformation>false</ignoreSigningInformation>
                <jlinkVerbose>false</jlinkVerbose>

                <!-- Logging format (applied to conf/logging.properties) -->
                <loggingFormat>%1$tF %1$tT %4$s %2$s - %5$s%6$s%n</loggingFormat>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Example Build Output

```
target/
 ├─ my-app-image/
 │   ├─ bin/
 │   │   ├─ myapp              ← launcher script (Unix)
 │   │   └─ myapp.bat          ← launcher script (Windows)
 │   ├─ conf/
 │   │   └─ logging.properties ← patched format
 │   ├─ lib/…                  ← JavaFX modules + ap modules
 │   └─ …
 └─ my-app-runtime.zip         ← optional distribution archive
```

### Tips & Gotchas

- **Module Descriptor Required** – `jlink` will fail if the project does not produce a `module-info.class`.
- **Compression Values** – Only values present in `COMPRES_OPTIONS` (`zip‑0‑zip‑9` or deprecated `0-2`) are accepted; an
  invalid value stops the build.
- **Custom `jmodsPath`** – Point this to the directory containing the JavaFX `jmod` files that match the JDK version you
  are using.
- **Launcher Naming** – The `<launcher>` name becomes the executable file name under `bin/`.
- **Additional Binaries** – Each binary is exposed to the application via a system property (`-D<prop>=./<file>`). Use
  this to load native libraries at runtime.
- **ZIP Packaging** – If you omit `<jlinkZipName>`, the image remains unpacked; useful for Docker layers or when you
  want to ship the directory as‑is.

### Further Reading

Official JDK jlink
documentation: [https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html).

---

Add the snippet above to your project's pom.xml and run:

```shell
mvn clean package
```

The plugin will generate a minimal Java runtime image containing only the modules your JavaFX application needs,
together with a ready‑to‑use launcher and optional ZIP distribution.
Enjoy smaller, faster start‑uptimes and a truly self‑contained delivery artifact!

## run Maven Goal

The **`run`** goal launches a JavaFX application directly from the Maven build lifecycle.

### When to Use It

- Quick local testing of a JavaFX UI without creating a packaged image.
- Running the application with custom JVM options, debugger attachment, or additional native binaries.
- Integrating UI tests into a CI pipeline that can start the app in head‑less mode (if the test harness supports it).

The goal executes during the `proces-classes` phase and requires **runtime** dependency resolution.

### Key Configuration Parameters

| Parameter            | Property                    | Default      | Description                                                                      |
|----------------------|-----------------------------|--------------|----------------------------------------------------------------------------------|
| `executable`         | `javafx.executable`         | `java`       | Path or name of the Java executable used to launch the app.                      |
| `attachDebugger`     | `javafx.attachDebugger`     | `false`      | When `true`, starts the JVM with JDWP remote debugging enabled.                  |
| `debuggerPort`       | `javafx.debuggerPort`       | `5005`       | Port on which the debugger listens (used only if `attachDebugger` is `true`).    |
| `mainClass`          | `javafx.mainClass`          | **required** | Fully‑qualified main class (or `module/name` if a module descriptor is present). |
| `loggingFormat`      | `javafx.loggingFormat`      | –            | Overrides `java.util.logging.SimpleFormatter.format` via `-D` system property.   |
| `additionalBinaries` | `javafx.additionalBinaries` | –            | List of native files to copy and expose as `-D<prop>=<path>` system properties.  |
| `options`            | –                           | –            | Extra JVM options (e.g., `-Xmx1g`).                                              |
| `modulePathElements` | –                           | –            | Resolved module‑path entries (populated by the base mojo).                       |
| `classPathElements`  | –                           | –            | Resolved class‑path entries (populated by the base mojo).                        |
| `commandlineArgs`    | `javafx.args`               | –            | Arguments passed to the application’s `main` method.                             |
| `skip`               | `javafx.skip`               | `false`      | Skip execution of the goal entirely.                                             |
| `workingDirectory`   | `javafx.workingDirectory`   | –            | Directory where the process is started; defaults to the project base directory.  |

### Typical Usage in `pom.xml`

```xml

<plugin>
    <groupId>io.github.bsels</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>1.1.0</version>

    <executions>
        <execution>
            <id>run-javafx</id>
            <goals>
                <goal>run</goal>
            </goals>
            <phase>process-classes</phase>
            <configuration>
                <!-- Main entry point -->
                <mainClass>com.example.app.MainApp</mainClass>

                <!-- Use a custom Java executable (optional) -->
                <executable>${java.home}/bin/java</executable>

                <!-- Enable remote debugging -->
                <attachDebugger>true</attachDebugger>
                <debuggerPort>6006</debuggerPort>

                <!-- Custom JVM options -->
                <options>
                    <option>-Xmx1024m</option>
                    <option>-Dmy.property=42</option>
                </options>

                <!-- Pass arguments to the application -->
                <commandlineArgs>--mode demo --verbose</commandlineArgs>

                <!-- Include native binaries (e.g., ffmpeg) -->
                <additionalBinaries>
                    <additionalBinary>
                        <name>ffmpeg</name>
                        <location>${project.basedir}/native/ffmpeg.exe</location>
                        <mappedJavaProperty>ffmpeg.path</mappedJavaProperty>
                    </additionalBinary>
                </additionalBinaries>

                <!-- Override logging format (optional) -->
                <loggingFormat>%1$tF %1$tT %4$s %2$s - %5$s%6$s%n</loggingFormat>

                <!-- Working directory (defaults to project base) -->
                <workingDirectory>${project.basedir}/target/run-dir</workingDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Tips & Gotchas

- **Running on Windows** – Ensure the `executable` points to the correct `java.exe` (or use the default `java`).
- **Debugging** – With `attachDebugger=true`, the JVM will suspend until a debugger attaches. Use `mvnDebug` or connect
  a remote debugger to the specified `debuggerPort`.
- **Native Binaries** – The plugin copies each binary into the `bin/` folder of the working directory and adds a
  `-D<prop>=./<file>` flag, so the application can locate it via `System.getProperty("<prop>")`.
- **Large Classpaths** – The generated command may exceed OS limits; consider using the `jlink` goal to create a custom
  runtime image for repeated runs.
- **Headless CI** – If the CI environment lacks a display, add `-Djava.awt.headless=true` to options or configure a
  virtual frame buffer (e.g., Xvfb on Linux).
- _\*Skipping Execution_ – Set `-Djavafx.skip=true` on the command line to bypass the run step (useful for multi‑module
  builds where only some modules need to be executed).

---


Add the snippet above to your project's pom.xml and run:

```shell
mvn javafx:run
```

The console will display the exact command that is executed by the plugin, for example:

```shell
java -Djava.util.logging.SimpleFormatter.format=%1$tF %1$tT %4$s %2$s - %5$s%6$s%n \
     -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:*6006 \
     -Dffmpeg.path=./ffmpeg.exe \
     --enable-native-access=javafx.graphics \
     -Xmx1024m -Dmy.property=42 \
     -cp target/classes:... \
     com.example.app.MainApp --mode demo --verbose
```
