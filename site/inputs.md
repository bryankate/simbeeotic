# Simbeeotic Inputs

Each invocation of the simulation controller takes two inputs, the scenario being simulated and a description of the world.

## Scenario
The scenario is an XML file that specifies the models that are to participate in the simulation (e.g. MAVs, weather). In addition to defining the models, the scenario file defines variables that are processed by the framework to create scenario variations. This is useful for performing batch analysis, such as Monte-Carlo studies. 

> An XML schema is provided in the distribution package for validating scenario files.

The following is the scenario XML for one of the supplied examples, `DropScenario.xml`:

    <scenario xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://harvard/robobees/simbeeotic/configuration/scenario">

    	<master-seed>
        	<constant value="111982"/>
    	</master-seed>

    	<simulation>
        	<end-time>10.0</end-time>
    	</simulation>

    	<models>
       		<model>
            	<java-class>harvard.robobees.simbeeotic.example.InertBee</java-class>
            	<start-position x="0" y="0" z="20"/>
        	</model>
    	</models>
    </scenario>

The scenario contains some bits of configuration, including the master random seed (so that deterministic random number streams are provided to the models) and a cutoff time for the scenario (otherwise it might run indefinitely). 

The more interesting section id the model definitions. Users can specify any number of model configurations that are loaded by the simulation at runtime. Note that the Java class of the model is given, making it possible to include models that are not compiled with Simbeeotic but are in the classpath at runtime.

For more examples of how to configure a scenario (including parameterizing models and adding equipment), see the provided examples in:

    simbeeotic-examples/src/main/resources/scenarios/

### Variation
The scenario schema defines a number of "looping variables" that can be used to generate variations of the base scenario. In fact, the looping variables are more like variable *generators* that can produce one or more values, which will result in one or more scenario variations (each of which will be executed). For example, consider the following variable definition:

    <variable name="foo">
      <for from="0" to="9" step="2"/>
    </variable>

The variable, named `foo` will produce 5 values, which will result in the simulation being executed 5 times. In each variation, the appropriate value is substituted into the model properties where the corresponding placeholder is present.

Placeholders follow the pattern `${name}:default` where the name begins with an alpha character, and has optional alternating sections of alphanumeric characters separated by a '-', '_', or '.', and ends with an alphanumeric section. In addition, a default value can be specified by placing a colon after the variable, followed by the default value, which is unrestricted. Whitespace before and after the variable is ignored (unless the variable has a default, in which case the whitespace at the end is considered part of the default value). For example, `${a}`, `${foo}`, `${foo.bar}`, `${f.o.0.bar-baz_9}`, and `${foo}:800` are valid placeholders, while `${0}`, `${foo..bar}`, and `${foo}:` are invalid. An example of how a placeholder can be used in the model definition is given in the snippet below (from the `LoopingScenario.xml` example):

    <models>
        <model>
            <java-class>harvard.robobees.simbeeotic.model.InertHive</java-class>
            <start-position x="0" y="0" z="0" />
        </model>
        <model>
            <java-class>harvard.robobees.simbeeotic.example.RandomWalkBee</java-class>

            <!-- variable placeholders can be used in property values -->
            <properties>
                <prop name="length" value="${bee.len}"/>

                <!-- the mass variable has a default value -->
                <prop name="mass" value="${bee.mass}:0.1"/>
            </properties>
        </model>
    </models>

The value for the variable `bee.len` will be substituted for the placeholder prior to the model being handed its configuration properties.

When multiple variable definitions are present in a scenario, they are combined to produce a number of scenario variations that is the product of the number of values produced by each variable. For example, the following variable definitions will produce 100 variations, essentially a "gridded" analysis:

    <variable name="x">
        <for from="0" to="9" step="1"/>
    </variable>
    <variable name="y">
        <for from="0" to="9" step="1"/>
    </variable>

The types of variables and their exact inputs are given in the XML schema, but are listed here:

* **Constant** - Produces a single, constant value.
* **Uniform Random** - Produces an arbitrary number of values drawn from a uniform distribution.
* **Normal Random** - Produces an arbitrary number of values drawn from a normal distribution.
* **For** - Produces a series of evenly-spaced values between an upper and lower bound.
* **Each** - Produces values from a given list.

The **master seed** is a special variable that defines how the master random number generator is seeded for each variation of the scenario. It is combined with the other looping variables in the normal way with the exception that it is evaluated first and can be used to seed other random variables.

For more information, see the `LoopingScenario.xml` and the scenario XML schema file.

## World
The second input is an XML file that describes the simulated world in which your models operate. The world can contain the following:

> An XML schema is provided in the distribution package for validating world files.

* **Bounds** - Currently this is a radius that defines the half-sphere centered around the world origin. The physics engine uses this bound to limit the interaction space in which collisions can occur.
* **Obstacles** - Non-kinematic (static) objects that are placed in the world at a specific location. The obstacles can be boxes, cylinders, or spheres of arbitrary size and color. Objects can be given arbitrary properties that are accessible by mobile agents upon collision.
* **Structures** - Non-kinematic (static) objects that define vertical and horizontal surfaces (walls and floors).
* **People** - Non-kinematic (static) geometric objects that sort-of resemble poeple.
* **Flowers** - Non-kinematic (static) objects that are randomly placed in the world within a "patch" that is defined by a center, radius, and density. Flowers are constructed from a tall, cylindical "stem" and a flat, cylindrical "platform". Flowers are fixed size, but can vary in color. The platform can be given arbitrary properties that are accessible by mobile agents upon collision.

Some care must be taken to avoid placing the hive inside an obstacle at runtime. In this case bees will be able to leave the hive, but will collide with the obstacle when trying to re-enter.

An example of a simple world is given below (from the `SimpleWorld.xml` example):

    <world xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns="http://harvard/robobees/simbeeotic/configuration/world">

	    <radius>2000</radius>
	
	    <obstacles>
	        <obstacle>
	            <box>
	                <position x="10" y="10"/>
	            </box>
	            <meta>
	                <prop name="foo" value="bar"/>
	            </meta>
	        </obstacle>
	        <obstacle>
	            <box length="4" width="4" height="4">
	                <position x="-5" y="8"/>
	            </box>
	            <color red="192" green="65" blue="23" alpha="128"/>
	        </obstacle>
	        <obstacle>
	            <box height="4">
	                <position x="3" y="-20"/>
	            </box>
	            <texture>
	                <classpath path="/textures/brick_2.jpg"/>
	            </texture>
	        </obstacle>
	        <obstacle>
	            <sphere radius="1">
	                <position x="-4" y="-10"/>
	            </sphere>
	            <texture>
	                <classpath path="/textures/metal_2.jpg"/>
	            </texture>
	        </obstacle>
	    </obstacles>
	
	    <flowers>
	        <patch>
	            <center x="3" y="-2"/>
	            <radius>12</radius>
	            <density>0.5</density>
	            <meta>
	                <prop name="pollen" value="2"/>
	            </meta>
	        </patch>
	    </flowers>
    </world>

For more examples of how to define objects in the virtual world, refer to the world XML schema and the examples in:

    simbeeotic-examples/src/main/resources/scenarios/