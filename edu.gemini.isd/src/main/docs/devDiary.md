# Development Diary of SCORPIO ISD
This archive is focused to give an overview of the development of SCORPIO ISD. SCORPIO is an Instrument at Gemini South Observatory that has an 8-channel imager and spectrograph.

The SCORPIO ISD was developed to transmit the status of the components of SCORPIO to a web view, implementing it as a bundle of GMP that works like a bridge between the web view and the instrument.

<img src="https://raw.githubusercontent.com/alejandrovillarroel-dev/images-gmp/refs/heads/main/classDiagram7.png" alt="Class Diagram" >

### Code Workflow

1. **Initialization**
    + The system starts the module of SCORPIO ISD.
    + In the activator class the Configuration Reader is called to bring the names of Status Items related to SCORPIO, StatusCacheHandler is instanced and WebsocketController starts the Javalin Websocket.
   + ConfigurationReader filters by the variable ActiveInstrument to bring only the Status Items of SCORPIO, the config file has the names provided by GIAPI and their translation to the frontend.
2. **Websocket Controller**
   + Every established connection is saved in a set called ActiveSessions, and it's iterated everytime It's wanted to transmit data (1 sec).
   + Its used an ScheduleExecutorService to send every second the StatusItems contained in the StatusRepository, called with the method _snapshot()_ of StatusCacheHandler.
3. **Subscription to GIAPI Items**
    + The StatusCacheHandler service is automatically recognized by GIAPI and filters all the Status Items received to only work with related ones to SCORPIO.
    + When StatusCacheHandler receives an update of StatusItems before sending the Object to StatusRepository is mapped to StatusDTO.
    + Once the DTO is ready, it is sent to StatusRepository to be stored in the repository Map.
4. **Mapping process and use of dictionary**
    + When the status method _TransformItem()_ from StatusMapper is called, it transfers all the values to its equivalent from the object StatusDTO.
    + The original name of StatusItem is replaced by its translation saved in the dictionary map.
5. **Delivery of Status Items**
    + The websocket receives the StatusItems from the method _getAllItems()_ from StatusRepository and parses it into JSON format.
    + When everything else is ready the websocket proceed to send JSON to the designated endpoint (ws://localhost:7000/ws).

### Technical decisions
The module consider the following points to maintain consistency, maintainability and better code:
- Single Responsibility Principle: All the classes are focused to only have one responsibility.
- DTO: A DTO was implemented to decouple the system from external objects (StatusItem). It maps the external object to the DTO replacing the original name to a specific id for the frontend.
- Repository Pattern: A repository class has been implemented to store in memory the transformed StatusItems and send them when they are needed.

### Build and run the isd
Considering that all the GMP modules are already built, you can build and run specifically this module with the following commands:

```bash
mvn install -Dmaven.test.skip=true -rf :isd
mvn -Dmaven.test.skip=true  pax:run
```

### Troubleshooting
After cloning ScorpioISD and GMP to a different machine, some build errors may occur. The following 2 solutions can help to resolve them:

1. Define a JAVA_HOME path if the java installer didn't make it (At least this solution works for MAC, windows should be different)
```bash
# Use your own java installation path
echo 'export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home' >> ~/.bash_profile
source ~/.bash_profile
```
You will need to execute the .bash_profile everytime you want to install and build the project
```bash
mvn install -Dmaven.test.skip=true -rf :isd
mvn -Dmaven.test.skip=true  pax:run
```

2. Use IntelliJ IDEA instead Visual Studio Code (recommended for some Maven problems with VS)

### Module tree
```
edu.gemini.isd/
└── src/
    └── main/
        ├── docs/ 
        │   ├── classDiagram.plantuml
        │   └── devDiary.md
        └── java/
            └── edu.gemini.isd.scorpio/
                ├── config/
                │   └── ConfigurationReader.java
                ├── controller/
                │   └── WebsocketController.java
                ├── dto/
                │   ├── StatusDTO.java
                │   └── StatusMapper.java
                ├── handler/
                │   └── StatusCacheHandler.java
                ├── osgi/
                │   └── Activator.java
                ├── repository/
                │   └── StatusRepository.java
                ├── util/
                │   └── StatusNameDictionary.java
                ├── osgi.bnd # OSGi configuration
                └── pom.xml # Maven Configuration
```
### Module information
This module was developed locally using the following software:
* Amazon Corretto JDK 8 (Due to problems with OpenJDK for AArch64/ARM architecture)
* Maven 3.9.12
* IntelliJ IDEA

> In the case of problems with permissions, you could use the binaries and manually add the paths to the terminal (See the first solution of troubleshooting).

### Add StatusItems to the configuration file
The config file structure is based in the giapi Prefix and contains the received name by the StatusHandler and its translation to the frontend item.

For example to add `SCO:CC:ADC.diffPrism`, the following structure should be added to the cfg:
```json
"SCO": {
  "CC": {
    "ADC": {
      "diffPrism": {
        "giapi": "SCO:CC:ADC.diffPrism",
        "frontend": "diff_prism"
      },
      "jointPrism": {
        "giapi": "SCO:CC:ADC.jointPrism",
        "frontend": "joint_prism"
      }
    } 
  }
}
```

> In short terms you should take the first 3 sections of the prefix and the Giapi Status Item SCO > CC > ADC > diffPrism. Follow this if the prefix have more sections.

## How to build a new Module
This section is a guide of how to implement your own module to the GMP ecosystem in OSGi.
#### Create a module
If you are in IntelliJ IDEA:
1. Right click in the root directory
2. New => Module
3. Complete the fields, an example:

<img src="https://raw.githubusercontent.com/alejandrovillarroel-dev/images-gmp/refs/heads/main/exampleBundle.png" alt="Class Diagram" width="600" height="500">

> Note: You can use < none > as parent, but you will need to add your bundle to < modules > in the root pom.xml, using "giapi-osgi" as parent will add that automatically

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
 edu.gemini.aspen.giapi.status
# Include any other package exports/privacy rules below.

```
### How to implement a Status Handler in a module
1. You will need to implement the Interface StatusHandler and implement the minimal methods.
```java
import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.StatusItem;

public class MyStatusHandler implements StatusHandler {
    private final Set<String> subscribedItems;

    public MyStatusHandler(Set<String> subscribedItems) {
        this.subscribedItems = new LinkedHashSet<>(subscribedItems);
    }
    
    @Override
    public String getName() {
        return "MyStatusHandler";
    }

    @Override
    public <T> void update(StatusItem<T> item) {
        // You need a filter to only work with necessary data
        String name = item.getName();
        if (!subscribedItems.contains(name)) {
            return;
        }
        
        // add the logic here and call the necessary methods
    }
}
```

2. After that in your activator you need to register your handler, and it should be detected automatically by gipapi-stats-service module
```java
public class Activator implements BundleActivator {
   private ServiceRegistration<StatusHandler> statusHandlerRegistration;
   @Override
    public void start(BundleContext context) throws Exception {
       MyStatusHandler statusHandler = new MyStatusHandler();
       statusHandlerRegistration = context.registerService(StatusHandler.class, statusHandler, null);
        System.out.println("Hello World - bundle started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
       // Remember to stop the service
       if (statusHandlerRegistration != null) {
          statusHandlerRegistration.unregister();
          statusHandlerRegistration = null;
       }

       System.out.println("Goodbye - bundle stopped");
    }
}
```