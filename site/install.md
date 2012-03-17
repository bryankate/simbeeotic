# Installing Simbeeotic

## Installing Java3D
These instructions will allow you to install the Java3D native libraries on your system. Java3D is not a pure Java project. It requires a number of native libraries (.dll, .so, etc) to be installed on the target system in order to work. This is annoying, but it should be a one-time inconvenience.
To start, you need to download this file containing the Java3D native libraries and unzip it to a temporary location:

**[Java3D binaries](../../../raw/master/site/java3d-1.5.2-binaries.zip)**

### Mac OSX

Unfortunately, Apple decided to shop OSX with an ancient version of Java3D pre-installed. The first step is to remove the old files:

    cd /System/Library/Java/Extensions
    sudo rm j3d* libJ3D* vecmath*

Now, copy the unzipped files in the `mac-osx-universal` directory into the `Extensions` directory:

    cd /path/to/unzipped/files/mac-osx-universal
    sudo cp *.jnilib /System/Library/Java/Extensions

### Ubuntu 10.04

There are two directories that contain binaries for Linux - one for 32 bit Intel architectures (x86) and one for 64 bit AMD/Intel architectures (x86_64). Be sure to use the appropriate files for your system, replacing `ARCH` in the instructions below.

Copy the unzipped files in the `linux-ARCH` directory into the `/usr/lib/jni` directory:

    cd /path/to/unzipped/files/linux-ARCH
    sudo cp *.so /usr/lib/jni

You now need to ensure that those libraries can be located by the JVM at runtime. There are multiple ways to do this, but they all involve setting the `LD_LIBRARY_PATH` environment variable. This can be set into the user's environment permanently (by editing `~/.profile`) or as needed (assuming you are using an IDE) in your IDE's run configuration by setting an local environment variable or passing a VM argument when launching. Depending on how the environment is constructed in your IDE it maybe necessary to employ more than one strategy to guarantee the libraries are located.

#### Option 1
Modify the `~/.profile` file to export `LD_LIBRARY_PATH` by adding the line:

    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib/jni
 
#### Option 2
In your IDE's run configuration for the application that requires Java3d, add an entry for `LD_LIBRARY_PATH` into the environment settings. It should be set to the value `/usr/lib/jni`.

### Windows

TBD

## IDE Integration

IDE integration is optional. We prefer to develop complex projects in an IDE, but Simbeeotic can be built using the command line Maven tools and executed from the command line or a script.

This section is intentionally sparse since there are a number of popular IDEs that change quite a bit. Below are some steps to getting this working, assuming the user is comfortable enough with their preferred IDE to work out the details.

### Eclipse
1. Install the Maven plugins, [m2eclipse](http://m2eclipse.sonatype.org/sites/m2e) and [m2eclipse extras](http://m2eclipse.sonatype.org/sites/m2e-extras)
2. Import Simbeeotic with `File->Import->Maven->Check out Maven Projects from SCM`. Eclipse will download the project and configure a number of projects in your workspace from the information in the POM files.
3. Eclipse has now executed the Maven build cycle and generated some additional Java source files. Unfortunately these new sources are not always recognized by Eclipse, so it may be necessary to trigger an update manually. Right click on `simbeeotic-core` in your workspace and select `Maven->Update Project Configuration`. You may need to restart Eclipse after this step.
4. See below for generic instructions on executing a scenario.

### IntelliJ
1. Enable the Maven Integration plugin.
2. Checkout the Simbeeotic project using your favorite git tool.
3. Create a new project from scratch. *Do not create a module.* Just name the project, select a project files location, and finish.
4. After you create the empty project IntelliJ might ask you to create a new module (again). Just close the project settings.
5. Open the `Maven Projects` sidebar. Select the "plus" button to add a Maven project. Browse to the location where you checked out Simbeeotic and select the `pom.xml` file.
6. IntelliJ should read the POM file and create a number of modules based on the information in the POMs. This might take a few minutes.
7. See below for generic instructions on executing a scenario.

### Running a Scenario
To run a scenario from your IDE you need to create a run configuration that invokes the Simbeeotic app and passes the correct inputs. In addition, you may need to set environment variables to help the JVM locate the Java3D binaries.

1. Create a new run configuration, name it `Drop`.
2. Select `simbeeotic-app` as the project/module classpath to use. This module's classpath contains the main class and all the other dependencies needed to run a scenario.
3. Use `harvard.robobees.simbeeotic.Simbeeotic` as the Main class.
4. Set the working directory to (the absolute path of) `simbeeotic-examples/src/main/resources/scenarios`
5. Add the arguments `-s DropScenario.xml -w EmptyWorld.xml -l log4j.properties`
6. You should be able to run *this* scenario. If you enable the 3D visualization you may need to modify the environment variable section of your run configuration to ensure that the Java3D binaries are on your library path (LD_LIBRARY_PATH on Linux).

You should be able to compile the project and run the scenario, producing output similar to:

	INFO  - simbeeotic.SimController - 
	INFO  - simbeeotic.SimController - --------------------------------------------
	INFO  - simbeeotic.SimController - Executing scenario variation 1
	INFO  - simbeeotic.SimController - --------------------------------------------
	INFO  - simbeeotic.SimController - 
	INFO  - example.InertBee - ID: 0  time: 0.0  pos: (0.0, 0.0, 20.05)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 0.1  pos: (0.0, 0.0, 19.976425)  vel: (0.0, 0.0, -0.98100007) 
	INFO  - example.InertBee - ID: 0  time: 0.2  pos: (0.0, 0.0, 19.80475)  vel: (0.0, 0.0, -1.9620004) 
	INFO  - example.InertBee - ID: 0  time: 0.3  pos: (0.0, 0.0, 19.534975)  vel: (0.0, 0.0, -2.9430008) 
	INFO  - example.InertBee - ID: 0  time: 0.4  pos: (0.0, 0.0, 19.1671)  vel: (0.0, 0.0, -3.9240012) 
	INFO  - example.InertBee - ID: 0  time: 0.5  pos: (0.0, 0.0, 18.701126)  vel: (0.0, 0.0, -4.905) 
	INFO  - example.InertBee - ID: 0  time: 0.6  pos: (0.0, 0.0, 18.137049)  vel: (0.0, 0.0, -5.885999) 
	INFO  - example.InertBee - ID: 0  time: 0.7  pos: (0.0, 0.0, 17.474874)  vel: (0.0, 0.0, -6.866998) 
	INFO  - example.InertBee - ID: 0  time: 0.8  pos: (0.0, 0.0, 16.714602)  vel: (0.0, 0.0, -7.847997) 
	INFO  - example.InertBee - ID: 0  time: 0.9  pos: (0.0, 0.0, 15.856227)  vel: (0.0, 0.0, -8.828997) 
	INFO  - example.InertBee - ID: 0  time: 1.0  pos: (0.0, 0.0, 14.899753)  vel: (0.0, 0.0, -9.809996) 
	INFO  - example.InertBee - ID: 0  time: 1.1  pos: (0.0, 0.0, 13.845179)  vel: (0.0, 0.0, -10.790995) 
	INFO  - example.InertBee - ID: 0  time: 1.2  pos: (0.0, 0.0, 12.692504)  vel: (0.0, 0.0, -11.771994) 
	INFO  - example.InertBee - ID: 0  time: 1.3  pos: (0.0, 0.0, 11.44173)  vel: (0.0, 0.0, -12.752993) 
	INFO  - example.InertBee - ID: 0  time: 1.4  pos: (0.0, 0.0, 10.092855)  vel: (0.0, 0.0, -13.733992) 
	INFO  - example.InertBee - ID: 0  time: 1.5  pos: (0.0, 0.0, 8.645882)  vel: (0.0, 0.0, -14.714991) 
	INFO  - example.InertBee - ID: 0  time: 1.6  pos: (0.0, 0.0, 7.1008077)  vel: (0.0, 0.0, -15.69599) 
	INFO  - example.InertBee - ID: 0  time: 1.7  pos: (0.0, 0.0, 5.457634)  vel: (0.0, 0.0, -16.676989) 
	INFO  - example.InertBee - ID: 0  time: 1.8  pos: (0.0, 0.0, 3.7163603)  vel: (0.0, 0.0, -17.657988) 
	INFO  - example.InertBee - ID: 0  time: 1.9  pos: (0.0, 0.0, 1.8769869)  vel: (0.0, 0.0, -18.638987) 
	INFO  - example.InertBee - ID: 0  time: 2.0  pos: (0.0, 0.0, -0.060486436)  vel: (0.0, 0.0, -19.619986) 
	INFO  - example.InertBee - ID: 0  time: 2.1  pos: (0.0, 0.0, 0.03449287)  vel: (0.0, 0.0, 0.704574) 
	INFO  - example.InertBee - ID: 0  time: 2.2  pos: (0.0, 0.0, 0.04678544)  vel: (0.0, 0.0, 0.064290985) 
	INFO  - example.InertBee - ID: 0  time: 2.3  pos: (0.0, 0.0, 0.04915732)  vel: (0.0, 0.0, 0.016853489) 
	INFO  - example.InertBee - ID: 0  time: 2.4  pos: (0.0, 0.0, 0.0497791)  vel: (0.0, 0.0, 0.0044180523) 
	INFO  - example.InertBee - ID: 0  time: 2.5  pos: (0.0, 0.0, 0.049942095)  vel: (0.0, 0.0, 0.001158185) 
	INFO  - example.InertBee - ID: 0  time: 2.6  pos: (0.0, 0.0, 0.049984816)  vel: (0.0, 0.0, 3.0362606E-4) 
	INFO  - example.InertBee - ID: 0  time: 2.7  pos: (0.0, 0.0, 0.04999602)  vel: (0.0, 0.0, 7.9611316E-5) 
	INFO  - example.InertBee - ID: 0  time: 2.8  pos: (0.0, 0.0, 0.049998954)  vel: (0.0, 0.0, 2.087839E-5) 
	INFO  - example.InertBee - ID: 0  time: 2.9  pos: (0.0, 0.0, 0.049999725)  vel: (0.0, 0.0, 5.4609263E-6) 
	INFO  - example.InertBee - ID: 0  time: 3.0  pos: (0.0, 0.0, 0.049999926)  vel: (0.0, 0.0, 1.4249235E-6) 
	INFO  - example.InertBee - ID: 0  time: 3.1  pos: (0.0, 0.0, 0.049999982)  vel: (0.0, 0.0, 3.9674342E-7) 
	INFO  - example.InertBee - ID: 0  time: 3.2  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 3.3  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 3.4  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 3.5  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 3.6  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 3.7  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 3.8  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 3.9  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 4.0  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 4.1  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 4.2  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 8.381903E-8) 
	INFO  - example.InertBee - ID: 0  time: 4.3  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 4.4  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 4.5  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 4.6  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 4.7  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 4.8  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 4.9  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.0  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.1  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.2  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.3  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.4  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.5  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.6  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.7  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.8  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 5.9  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.0  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.1  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.2  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.3  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.4  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.5  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.6  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.7  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.8  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 6.9  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.0  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.1  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.2  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.3  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.4  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.5  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.6  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.7  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.8  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 7.9  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.0  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.1  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.2  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.3  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.4  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.5  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.6  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.7  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.8  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 8.9  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.0  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.1  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.2  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.3  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.4  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.5  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.6  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.7  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.8  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 9.9  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - example.InertBee - ID: 0  time: 10.0  pos: (0.0, 0.0, 0.049999993)  vel: (0.0, 0.0, 0.0) 
	INFO  - simbeeotic.SimController - 
	INFO  - simbeeotic.SimController - --------------------------------------------
	INFO  - simbeeotic.SimController - Scenario variation 1 executed in 0.078472 seconds.
	INFO  - simbeeotic.SimController - 
	INFO  - simbeeotic.SimController -      init time: 775387000 nanos
	INFO  - simbeeotic.SimController -     event time: 7783000 nanos
	INFO  - simbeeotic.SimController -    event count: 101
	INFO  - simbeeotic.SimController -   physics time: 69739000 nanos
	INFO  - simbeeotic.SimController -       run time: 78472000 nanos
	INFO  - simbeeotic.SimController -     total time: 853859000 nanos
	INFO  - simbeeotic.SimController - --------------------------------------------


### Troubleshooting
If the IDE is having trouble integrating the Maven project, first see if Simbeeotic will build from the command line. Find the checkout out code and execute the following command from the top level directory:

    mvn clean compile
    
If you are able to build successfully from the command line, there is likely a problem with the project definition in your IDE. The most common problem is the IDE not detecting the auto-generated sources correctly. This is usually solved by updating the project configuration or source folders through your IDE's Maven menus.