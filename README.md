# Introduction

Simbeeotic is a simulator geared toward modeling swarms of micro-aerial vehicles (MAVs). It was originally developed as part of the [RoboBees](http://robobees.seas.harvard.edu) project at Harvard University. Simbeeotic provides a 3-dimensional virtual world in which swarms operate. The simulation is backed by a physics engine that handles the kinematics of the objects in the world, including collisions between them.

# Installation
Simbeeotic is written in Java and uses Maven as its build system. Maven will automatically fetch the required dependencies and compile the project. However, there are some runtime dependencies that must be installed for 3D visualization.

See the **[installation instructions](simbeeotic/wiki/Installing-Simbeeotic)** for details on installing the runtime dependencies and integrating the project with an IDE.


# Working with Simbeeotic
We expect that Simbeeotic will be used as a base framework for your work. It is likely that your contribution will be models classes that represent different aspects of your problem domain. There are multiple ways of integrating Simbeeotic into your project. 

## Extend
Though not our preferred method, the easiest approach to adding models is to implement them directly in the Simbeeotic source tree and recompile. The least invasive place to add your models is in the submodule *simbeeotic-examples*. Your Java code code and scenario files can go into:

    simbeeotic-examples/src/main/java/
    simbeeotic-examples/src/main/resources/scenarios/

You can then recompile the project, it will now include your models.

## Use as a Dependency
This is the preferred method (one that we use in the RoboBees project). Since Maven is very good at managing dependencies, you can just make a new Maven project and list Simbeeotic as a dependency. All of the Simbeeotic modules and transitive dependencies will be added to your new project's classpath by Maven. To do this, you must install the Simbeeotic artifacts into a local repository. To install them on a single machine, execute the following from the top level Simbeeotic directory:

    mvn -Dmaven.test.skip clean compile source:jar package install

Now Simbeeotic artifacts will be installed in your local repository, and can be referenced by your new project. In your new project's POM, add something like:

    <dependency>
        <groupId>harvard.robobees.simbeeotic</groupId>
        <artifactId>simbeeotic-app</artifactId>
        <version>VERSION</version>
    </dependency>

Be sure to use the version of Simbeeotic that you installed.

## Build a Distribution
The last option is to build a Simbeeotic distribution. This process takes the required dependencies, documentation, and example source code and bundles it into a neat package for distribution. To make a distribution, run the following command from the top level Simbeeotic directory.

    mvn -Dmaven.test.skip clean compile package

The distribution directory will be

    simbeeotic-dist/target/simbeeotic-dist-VERSION-dist/

# Running a Simulation
The application that is provided in the `simbeeotic-app` module is accessed via the harvard.robobees.simbeeotic.Simbeeotic class. In order to run properly, it requires `simbeeotic-core` (and with the 3rd party libraries) to be in the classpath. The arguments to the command line application are given below:

    Option               			Description                            
    ------                       	-----------
    -s, --scenario <File>			Scenario XML file.                     
    -w, --world <File>				World XML file.                              
    -l, --log <File>				Log4j properties file (optional).      
    -r, --real-time-scale <Scale>	Constrained real time scaling factor (optional, default unconstrained).
    -p, --paused					Start in a paused state (optional).
    -h, --help						Show help.

Thus, with the Java classpath properly constructed, it should be possible to kick off a simulation with the following command:

    java -cp $CLASSPATH harvard.robobees.simbeeotic.Simbeeotic -s myscenario.xml -w myworld.xml

Of course, if you integrate with an IDE or create a script to kick off a simulation, you will not need to execute the above command directly from the command line.

## Inputs

The two required inputs, the scenario and world XML files are described in more detail in the **[inputs guide](simbeeotic/wiki/Simbeeotic-Inputs)**.

# Modeling
See the **[modeling guide](simbeeotic/wiki/Modeling-Guide)** for more information on how to get started modeling your problem domain.

# Documentation

Simbeeotic is described in [Simbeeotic: A Simulator and Testbed for Micro-Aerial Vehicle Swarm Experiments](http://www.eecs.harvard.edu/~bkate/pubs/simbeeotic-ipsn12.pdf).

## API Documentation
Users can generate Javadoc for Simbeeotic using Maven. The easiest way to accomplish this is to run the following command from the top level directory:

    mvn javadoc:aggregate

This should produce the API documentation in the following directoy:

    target/site/apidocs/

# License
See the [LICENSE.txt](simbeeotic/blob/master/LICENSE.txt) and [LICENSE.contrib.txt](simbeeotic/blob/master/LICENSE.contrib.txt) files for information regarding the open source licenses of Simbeeotic and its dependencies. 
 
# Support
You are welcome to email the developers, but since we are busy researchers you may not respond immediately to requests for help.
    
