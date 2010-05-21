#!/bin/sh

# test scenario
SCENARIO=examples/scenarios/RandomWalkScenario.xml
WORLD=examples/scenarios/EmptyWorld.xml

# construct the classpath
CP=

for JAR in jar/*.jar
do
  CP="$CP:$JAR"
done

for JAR in jar/3rd_party/*.jar
do
  CP="$CP:$JAR"
done

# assume Java is on the PATH
java -cp $CP harvard.robobees.simbeeotic.Simbeeotic -s $SCENARIO -w $WORLD
