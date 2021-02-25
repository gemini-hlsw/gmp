
1. Requirements
---------------
To build the project you need to install the following:

- Java JDK 1.6 or higher
- Maven 3, version 3.0.3 or higher.

2. Build and deploy locally
---------------------------
At the top level run
   mvn install

This command will go through all the modules and install them in the local
Maven repository (at ~/.m2/repository). The install process will download
any required dependencies, compile the code (under src/main/java), compile
and run the tests (under src/test/java), and package the jar file with
the necessary OSGi headers.

The headers are derived from the pom information and those provided in the
osgi.bnd file

3. Build a single module
------------------------
You can build and deploy locally a single module by issuing a
   mvn install

in a single module

4. How to skip the tests
------------------------

During development we often don't want to run the tests all the time
You can skip them by issuing:
   mvn -Dmaven.test.skip=true install

This can be done at the top level or module level

5. Launching gmp-server
-----------------------
To launch gmp-server you can use the Maven pax plugin issuing:
   mvn pax:provision

This will launch felix with all the required modules
The configuration is stored at src/main/config

The felix launcher will create a local cache of the feilx framework and
installed files in the runner directory

This directory can be freely deleted

The logs are locate under runner/logs/gmp.log

6. Integration tests
------------------------
Most tests in the project are unit test but there are some integration tests are
identified by classes ending with IT unlike unit tests that end in Test.

Integration tests are not run by default to make the builds faster and the
normal mvn test target won't execute them.

To run the integration tests manually you need to issue:
    mvn install failsafe:integration-test failsafe:verify

If you wish to run a single integration test you can add to the command line
the system variable it.test with the classname of the test you want to write like

mvn -Dit.test=edu.gemini.aspen.integrationtests.GDSEndToEndIT install failsafe:integration-test failsafe:verify

For more options check
http://maven.apache.org/plugins/maven-failsafe-plugin/examples/single-test.html

7. Use with IntelliJ idea
------------------------------
Idea works best by just importing the pom.xml as a project file definition

8. Generate application
-----------------------
Applications are just other modules that define a list of bundles to
deploy and configuration. They use the assembly plugin and will produce
a zip file with all the required bundles and configurations.

As an example go to distribution and check the pom file which defines a generic gmp-server

9. Additional Documentation
--------------------------
This project comes with a set of documentation that can be generated via doxygen.

To produce the documentation, go to the gmp-server directory and type:

    mvn -Pdocumentation,production resources:copy-resources doxygen:report scala:doc

and then open the generated documentation at gmp-server/target/site/doxygen/index.html

10. RPM and tar.gz package
--------------------------
To generate the full package you need to activate the production profile using the command
    mvn -Pproduction clean install

This command will compile all the modules and at the end it will generate a tar.gz and rpm
files to be installed

They will end up in the distribution/target dir

It is also possible to include the documentation. To do so you need to also activate the
documentation profile with the command
    mvn -Pdocumentation,production clean install

The produced tarball and rpm will then include the documentation

11. Instance specific distribution files
----------------------------------------
The GMP can be built using configuration specific to different instruments.
This is done using Maven profiles, defined in the distribution module.

profiles have names like gpi, graces, etc which correspond to directories at
instances/<profile-name>/src/main/config

That directory can contain configuration files that override the base configuration files at
src/main/config

It is then possible to build gmp-server distribution files that are specific for a given instrument
using the command

    mvn -Pgpi,production clean install

12. Release
-----------

You can use Maven to do releases by using the Maven release plugin.

Prerequisites:
* You must have set your system for automatic login to Github through SSH.
* You must have a local working copy of gmp. It must have git@github.com:gemini-hlsw/gmp.git as a
  remote repository. Your master branch must be up to date with the master branch on the gemini-hlsw
  repository.
* You also must fork gemini-hlsw/maven-repo.git in Github, and have an updated local working copy.

You can start doing an initial test. In the root of your local working copy of gmp, run the following
command:

mvn release:clean release:prepare -DdryRun=true

If that works fine you can do the actual release preparation as

mvn release:prepare

That command will:
  1. Update all the SNAPSHOT version to final versions.
  2. Commit the changes to the versions.
  3. Tag the repository with the new version for GMP (GMP-R<version>)
  4. Increment all the version numbers and add the SNAPSHOT prefix to them, in preparation for the next
     development cycle.
  5. Commit the changes to the versions.
  6. Push all the changes and the new tag to the gemini-hlsw repository in Github.

Once that is ready you can actually perform the release with the command (you must use the location of your
local maven-repo working copy):

mvn release:stage -DstagingRepository="edu.gemini.releases::default::file:///<local-maven-repo-location>/releases"

That command will deploy all the artifacts to your local copy of maven-repo and update the indexes.

The final steps are:
  1. Commit the changes in yoor local maven-repo copy with a proper commit message (like
  "New release GMP-RX.Y.Z").
  2. Push them to your fork in Github, and create a pull request to gemini-hlsw/maven-repo.
