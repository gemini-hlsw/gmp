# Foobar

This is the gmp backend service for all the ISD new generations.

## Installation

### Requirements

- Java version 1.8.0_472
- Maven version 3.9.12

If you have multiple java versions, please make sure you are using the right one. The desired java version can be set using the environment variable `JAVA_HOME`, in ArchLinux it can be done using the command `sudo archlinux-java set java-8-openjdk`.

Using bash shell it can be done using:

```bash
export JAVA_HOME=/path/to/java/8
```

Maven can be installed using your OS package manager, brew, apt, dnf, pacman.

To have a more detailed explanation about the installation process, please visit the [SCORPIO ISD architecture](https://docs.google.com/document/d/1D8LW2w2Vb0aqxKV8kh5_Co97drfRYjenX4v1eWFAW2M/edit?tab=t.0) document.

Once you have cloned the gmp repository and you have the right java and maven versions the entire repository must be compiled at least one time to create the needed jars to run the backend, this is done using the following command:

```bash
mvn install -Dmaven.test.skip=true
```

The previous command will also compile the isd backend project. If you want to compile any further change made only to the isd backed project, located in gmp/edu.gemini.isd directory, the following command can be used:

```bash
mvn install -Dmaven.test.skip=true -rf :isd
```

## Usage

After compiling the project, the backend can be started using the following command:

```bash
mvn -Dmaven.test.skip=true pax:run
```

The previous command will start the websocket server using the default url `ws://localhost:7000/ws/`.

After the webscocket service is started the server will:

1. Check every 1 second the current devices state.
2. Compare every device default value and will store all changed states.
3. Send the entire set of changed states on every new client connection received.
4. Send only one time to every active connection any new change discovered in the device statuses.

To update the status of a device the `giapi-tester` compiled jar can be used. To do it you should go to its corresponding directory `gmp/giapi-tester/target/` where the `giapi-tester.jar` should be located. If the jar is not found check the installation section to compile the entire repository.

For example, to update the cover state the following command can be used:

```bash
java -jar giapi-tester -set SCO:CC:cover.cover -type integer -value 1
```

To have a full list of possible devices check the [Data Dictionary](https://docs.google.com/spreadsheets/d/1OMu5tEHHBd4GNI9MWErGx2CvYIffGhWO/edit?rtpof=true&gid=71060901#gid=71060901) spreadsheet.

### Scripts

In the directory `gmp/edu.gemini.isd/src/main/resources/` a set of scripts can be found, every script have a pre-defined list of instructions to update the states and emulate a possible scenario. For example, the `temperatures.sh` script will start updating the bench and detectors temperatures using random values emulating the sensors reading them.

## Contributing
