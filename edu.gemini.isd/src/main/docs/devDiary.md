# Development Diary of Scorpio ISD
This archive is focused to give an overview of the development of Scorpio ISD

> Please ignore the grammatical problems, I'm still working on that

<img src="https://raw.githubusercontent.com/alejandrovillarroel-dev/images-gmp/refs/heads/main/classDiagram4.png" alt="Class Diagram" width="500" height="700">

### Code Workflow

1. Initialization
    + The system starts the module of Scorpio ISD
    + In the activator class the Configuration Reader is called to bring the Status Items related to Scorpio, StatusCacheHandler is instanced and WebsocketController start the Javalin Websocket.
   + ConfigurationReader filters by the variable ActiveInstrument to bring only the Scorpio Status Items, the config file have the names provided by giapi and its translation to frontend
2. Websocket
   + Every established connection is saved in a set called ActiveSessions and its iterated everytime It's wanted to transmit data.
   + Its used an ScheduleExecutorService to send every second the StatusItems contained int the StatusCacheHandler
3. Subscription to Giapi Items
    + The StatusCacheHandler its automatically recognized by giapi and filters all the Status Items received to only work with related ones to Scorpio.
    + When StatusCacheHandler receives an update of StatusItems before saving the Object its mapped to DTO.
4. Mapping process and dictionary
    + When the status method TransformItem from StatusMapper is called, it transfers all the values to its equivalent from a object StatusDTO.
    + The original name of StatusItem its replaced by its translation saved in the dictionary map
5. Delivery of Items
    + The websocket receives the StatusItems and parses it into JSON format
    + When everything else is ready the websocket proceed to send JSON to the designated endpoint (ws://localhost:7000/ws)

### Technical decisions (WIP)
The module consider the following points to maintain consistency, maintainability and clean code:
- SOLID
- DTO: To have good trait to te external objects (Status Item from giapi), it was implemented a DTO object to be independent of external objects and replace the original name with a name used only for the frontend

> TODO: check if the code follows the SOLID principles and check the possibility to refactor to a better architecture
### Build and run the isd
Considering that all the gmp modules are already built, you can build and run specifically this module with the following commands

```bash
mvn install -Dmaven.test.skip=true -rf :isd
mvn -Dmaven.test.skip=true  pax:run
```

### Solution to possible problems
Trying to move ScorpioISD and gmp to others machines, occurred some problems, 2 possible solutions are:

1. Define a JAVA_HOME path if the java installer didn't make it
```bash
echo 'export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home' >> ~/.bash_profile
source ~/.bash_profile
```
You will need to execute the .bash_profile everytime you want to install and build the project
```bash
mvn install -Dmaven.test.skip=true -rf :isd
mvn -Dmaven.test.skip=true  pax:run
```

2. Use IntelliJ IDEA instead Visual Studio Code (optional, but recommended)
### Module tree
```
edu.gemini.isd/
└── src/
    └── main/
        ├── docs/ # Documentations
        │   ├── classDiagram.plantuml
        │   └── devDiary.md
        └── java/
            └── edu.gemini.isd.scorpio/
                ├── controller/
                │   ├── WebsocketController.java # Javalin websocket controller
                │   │   ├── models/
                │   │   │   ├── StatusDTO.java
                │   │   │   ├── StatusMapper.java
                │   │   │   └── StatusNameDictionary.java
                │   │   └── osgi/
                │   │       └── Activator.java
                │   ├── status/
                │   │   └── StatusCacheHandler.java # Receives data from the StatusHandlerAggregate of giapi-status-service
                │   └── ConfigurationReader.java
                ├── osgi.bnd
                └── pom.xml
```
### Module information
This module was developed locally using the following software:
* Amazon Corretto JDK 8 (Due to problems with OpenJDK for AArch64/ARM architecture)
* Maven 3.9.12
* IntelliJ IDEA

> In the case of problems with permissions, you could use the binaries and manually add the paths to the terminal.

## How to build a new Module
This section is a guide of how to implement your own module to the GMP ecosystem in OSGi.
#### Create a module
If you are in IntelliJ IDEA:
1. Right click in the root directory
2. New => Module
3. Complete the fields, an example:

<img src="https://raw.githubusercontent.com/alejandrovillarroel-dev/images-gmp/refs/heads/main/exampleBundle.png" alt="Class Diagram" width="600" height="500">

> Note: You can use < none > as parent, but you will need to add your bundle to < modules > in the root pom.xml, using "giapi-osgi" will make that automatically added

### Activator.java example
```java
public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Hello World - bundle started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("Goodbye - bundle stopped");
    }
}
```
### pom.xml example
if im not wrong bundle.symbolicName must be different from bundle.namespace and groupId
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

    <parent>
        <relativePath>../poms/compiled/</relativePath>
        <groupId>edu.gemini.aspen.giapi-osgi.build</groupId>
        <artifactId>compiled-bundle-settings</artifactId>
        <version>0.2.7-SNAPSHOT</version>
    </parent>
    
    <properties>
        <bundle.symbolicName>edu.gemini.bundle.example</bundle.symbolicName> 
        <bundle.namespace>edu.gemini.bundle</bundle.namespace>
        <javalin.version>4.6.8</javalin.version>
    </properties>

    <modelVersion>4.0.0</modelVersion>
    <groupId>edu.gemini.bundle</groupId>
    <artifactId>artifactExample</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <name>bundle name</name>
    <description>bundle description</description>

    <packaging>bundle</packaging>

    <dependencies>
        <dependency>
            <groupId>io.example</groupId>
            <artifactId>example</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>

</project>

```
### osgi.bnd example
```bnd
# Use the actual package path where the Activator lives
Bundle-SymbolicName: ${bundle.symbolicName}
Bundle-Version: ${project.version}
Bundle-Activator: edu.gemini.isd.example.Activator

Export-Package: edu.gemini.bundle.*
Private-Package: edu.gemini.bundle.*

# Embed all compile/runtime deps so the bundle is self-contained.
Embed-Dependency: *;scope=compile|runtime
Embed-Transitive: true
Bundle-ClassPath: ., {maven-dependencies}

# Keep imports minimal; everything else is embedded in the bundle.
Import-Package: \
 org.osgi.framework,\

# Include any other package exports/privacy rules below.

```
### How to implement a Status Handler in a module
You will need to implement the Interface StatusHandler and implement the minimal methods and the connection will be automatic.
```java
import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.StatusItem;

public class exampleStatusHandler implements StatusHandler {
    private final Set<String> subscribedItems;

    public StatusCacheHandler(Set<String> subscribedItems) {
        this.subscribedItems = new LinkedHashSet<>(subscribedItems);
    }
    
    @Override
    public String getName() {
        return "exampleStatusHandler";
    }

    @Override
    public <T> void update(StatusItem<T> item) {
        // You need a filter to only work with necesary data
        String name = item.getName();
        if (!subscribedItems.contains(name)) {
            return;
        }
        
        // add the logic here and call the necesary methods
    }
}
```