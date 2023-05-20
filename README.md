# GIAPI OVERVIEW
The GIAPI concept was developed by GEMINI to facilitate the interaction between the Telescope and the instrument software. Thanks to GIAPI, instrument developers do not need to know in detail the GEMINI Telescope Control System. They only need to deploy the GMP (Gemini Master Process) and incorporate GIAPI-GLUE in their source code to be able to communicate with the Telescope. 

The GMP (Gemini Master Process) is a component provided by Gemini that provides most of the Gemini integration features and functionalities. It basically transforms messages sent through the GIAPI-GLUE to the Telescope system components. The language-specific glue is the API layer used by the builder to implement the integration functionality.  For more details of the GMP and GIAPI-GLUE it is recommended to read the documents "GIAPI Design and Use" and "GIAPI C++ Language Glue API". The below image depicts a Simple Instrument architecture to communicate with the Telescope.

<p align="center">
<img  align="center" src="https://raw.githubusercontent.com/gemini-hlsw/gmp/igrins2/images/SimpleGMPArhictecture.jpg">
</p>

# Desployment from binaries. 
It is possible deploying the GMP and GIAPI-GLUEc++ from their binaries. Gemini recommend uses Rocky 8 or Centos 7 because these are the Operating Systems supported by GEMINI and for which the GIAPI-GLUEc++ binaries have been compiled. 

If you want to deploy the system in other platform, you should compile the external libraries of the [GIAPI-GLUEc++](https://github.com/gemini-hlsw/giapi-glue-cc/tree/develop/external) for you platform. In the previous link is decribed the instrucction executed by GEMINI to CENTOS7 and ROCKY8.

## Dependencies 
The only dependencies we need for GMP and GIAPI-glue are as follows.
* Java JDK 1.8
* GCC greater than 4.8.1.

If you are on Rocky 8, you could use the following commands:
* dnf -y install java-1.8.0-openjdk-devel
* dnf -y group install "Development Tools"

## Deployment instructions. 
This section describes the instructions for deploying the GMP and [any-command](https://github.com/gemini-hlsw/giapi-glue-cc/tree/develop/src/examples/any-command.cpp) example implemented in GIAPI-GLUE. The any-command is an example created by GEMINI where developers can see how to implement a command subscriber using the GIAPI-GLUEcc library. Then, we are going to use the giapi-tester to send a command to the any-command example, emulating how any client of the system could send a command to a subscriber. There are more examples in the [src/examples](https://github.com/gemini-hlsw/giapi-glue-cc/tree/develop/src/examples) folder of the GIAPI-GLUEcc repository. 

For this example, the GMP and the any-command example will be deployed on the same machine. If you need deploy them on different machines, you have to modify the gmp.properties which are located in the src/examples folder. 


### Deployment GMP
Open a terminal or console to execute the following commands. 
```
> tar -xzvf gmp-server-igrins2-v0.2.0.tar.gz
> cd gmp-server-0.2.0
> ./bin/gmp-server-ctl.sh start
```

### Deployment any-command (GIAPI-GLUE example)
Open a terminal or console to execute the following commands. 
```
> tar -xzvf giapi-gluecc_rocky8.tar.gz
> cd giapi-gluecc
> source defineGiapiglueEnv.sh
> cd src/examples
> If you deployed the GMP on other server or machine, you have to edit the gmp.properties file and specifying the IP or hostname of the server where the GMP was deployed (modify the gmp.hostname field)
> ./any-command
```

### GIAPI-TESTER
Open a terminal or console to execute the following commands. 
```
> cd gmp-server-0.2.0/bin
> java -jar giapi-tester.jar -sc OBSERVE -activity START -config DATA_LABEL=S11172022S001.fits
```
When you execute the last command you will see in the any-command standard output the following traces:
<pre><code>
      {DATA_LABEL : S11172022S001.fits}
      Starting worker thread for 3
      Worker Thread started!
      Messages processed = 0
</code></pre>


# Build GMP from source code and deploy locally
## Requirements
* Java JDK 1.8
* Maven 3, version 3.0.3 or higher.

## Build 
At the top level run
```
   mvn install
```
This command will go through all the modules and install them in the local
Maven repository (at ~/.m2/repository). The install process will download
any required dependencies, compile the code (under src/main/java), compile
and run the tests (under src/test/java), and package the jar file with
the necessary OSGi headers.

The headers are derived from the pom information and those provided in the
osgi.bnd file

### Build a single module
You can build and deploy locally a single module by issuing a
   mvn install

in a single module

### How to skip the tests

During development we often don't want to run the tests all the time. 
You can skip them by issuing:
```
   mvn -Dmaven.test.skip=true install
```
This can be done at the top level or module level

##  Launching gmp-server
To launch gmp-server you can use the Maven pax plugin issuing:
```
   mvn pax:provision
```
This will launch felix with all the required modules. 

The configuration is stored at src/main/config folder.

The felix launcher will create a local cache of the feilx framework and installed files in the runner directory

This directory can be freely deleted

The logs are locate under runner/logs/gmp.log

##  Integration tests
Most tests in the project are unit test but there are some integration tests are identified by classes ending with IT unlike unit tests that end in Test.

Integration tests are not run by default to make the builds faster and the normal mvn test target won't execute them.

To run the integration tests manually you need to issue:
```
mvn install failsafe:integration-test failsafe:verify
```
If you wish to run a single integration test you can add to the command line the system variable it.test with the classname of the test you want to write like
```
mvn -Dit.test=edu.gemini.aspen.integrationtests.GDSEndToEndIT install failsafe:integration-test failsafe:verify
```
For more options check (link) [http://maven.apache.org/plugins/maven-failsafe-plugin/examples/single-test.html]

##  Use with IntelliJ idea
Idea works best by just importing the pom.xml as a project file definition

##  Generate application
Applications are just other modules that define a list of bundles to deploy and configuration. They use the assembly plugin and will produce a zip file with all the required bundles and configurations.

As an example go to distribution and check the pom file which defines a generic gmp-server

## Additional Documentation
This project comes with a set of documentation that can be generated via doxygen.

To produce the documentation, go to the gmp-server directory and type:
```
mvn -Pdocumentation,production resources:copy-resources doxygen:report scala:doc
```
and then open the generated documentation at gmp-server/target/site/doxygen/index.html

##  RPM and tar.gz package
To generate the full package you need to activate the production profile using the command
```
 mvn -Pproduction clean install
```

This command will compile all the modules and at the end it will generate a tar.gz and rpm files to be installed

They will end up in the distribution/target directory.

It is also possible to include the documentation. To do so you need to also activate the documentation profile with the command
```
mvn -Pdocumentation,production clean install
```

The produced tarball and rpm will then include the documentation

## Instance specific distribution files
The GMP can be built using configuration specific to different instruments.
This is done using Maven profiles, defined in the distribution module.

profiles have names like gpi, graces, etc which correspond to directories at
instances/<profile-name>/src/main/config

That directory can contain configuration files that override the base configuration files at
src/main/config

It is then possible to build gmp-server distribution files that are specific for a given instrument
using the command
```
mvn -Pgpi,production clean install
```

## Create Release

You can use Maven to do releases by using the Maven release plugin.

Prerequisites:
* You must have set your system for automatic login to Github through SSH.
* You must have a local working copy of gmp. It must have git@github.com:gemini-hlsw/gmp.git as a  remote repository. Your master branch must be up to date with the master branch on the gemini-hlsw  repository.
* You also must fork gemini-hlsw/maven-repo.git in Github, and have an updated local working copy.

You can start doing an initial test. In the root of your local working copy of gmp, run the following command:
```
mvn release:clean release:prepare -DdryRun=true
```
If that works fine you can do the actual release preparation as
```
mvn release:prepare
```

That command will:
* Update all the SNAPSHOT version to final versions.
* Commit the changes to the versions.
* Tag the repository with the new version for GMP (GMP-R<version>)
* Increment all the version numbers and add the SNAPSHOT prefix to them, in preparation for the next development cycle.
* Commit the changes to the versions.
* Push all the changes and the new tag to the gemini-hlsw repository in Github.

Once that is ready you can actually perform the release with the command (you must use the location of your
local maven-repo working copy):

mvn release:stage -DstagingRepository="edu.gemini.releases::default::file:///<local-maven-repo-location>/releases"

That command will deploy all the artifacts to your local copy of maven-repo and update the indexes.

The final steps are:
* Commit the changes in yoor local maven-repo copy with a proper commit message (like "New release GMP-RX.Y.Z").
* Push them to your fork in Github, and create a pull request to gemini-hlsw/maven-repo.
