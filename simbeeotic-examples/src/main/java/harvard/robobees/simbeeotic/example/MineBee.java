/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package harvard.robobees.simbeeotic.example;

import harvard.robobees.simbeeotic.environment.WorldObject;
import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.model.sensor.PersonSensor;
import harvard.robobees.simbeeotic.model.sensor.WallSensor;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;

import org.apache.log4j.Logger;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.Hashtable;


/**
 * A bee that hangs out in underground mines.
 *
 * @author chartier
 */
public class MineBee extends SimpleBee {

    private float velocity = 0.1f;  // m/s
    private PersonSensor personSensor;
    private WallSensor wallSensor;
    private Set<Integer> peopleSeen = new HashSet<Integer>();

    private static Logger logger = Logger.getLogger(MineBee.class);

    private Vector3f dest = new Vector3f();
    private Vector3f relPos = new Vector3f();
    private Vector3f currPos = new Vector3f();
    
    private Hashtable<String, Integer> hash = new Hashtable();
    
    // relative position
    int x;
    int y;
    
    private final float delta = 0.1f;
    
    @Override
    public void initialize() {

        // always call the super
        super.initialize();

        // hover
        setHovering(true);

        // initialize sensors
        personSensor = getSensor("person-sensor", PersonSensor.class);
        wallSensor = getSensor("wall-sensor", WallSensor.class);
        
        wallSensor.setMaxRange(1);
        
        dest = getTruthPosition();
        
        // movement speed
        setDesiredLinearVelocity(new Vector3f(velocity, 0, 0));
        
        // direction
        turnToward(new Vector3f(dest));
        
        x = 0;
        y = 0;
    }


    @Override
    protected void updateKinematics(SimTime time) {
    	
    	currPos = getTruthPosition();
        relPos.sub(dest, currPos);
        
        if(relPos.length() < delta)
        {
        	// stop moving
        	setDesiredLinearVelocity(new Vector3f(0, 0, 0));
        	
        	// prepare to check paths by looking in x direction
        	Vector3f point = new Vector3f(); 
        	point.add(currPos, new Vector3f(1, 0, 0));
        	turnToward(point);
        	
        	// check open paths
        	boolean[] paths = new boolean[4];
        	// 0 - pos_x, 1 - pos_y, 2 - neg_x, 3 - neg_y
        	for(int i = 0; i < 4; i++) {
        		if(wallSensor.isWallDetected())
        			paths[i] = false;
        		else
        			paths[i] = true;
        		turn((float) (Math.PI/2));
        	}
        	
        	
        	// DEBUG
        	System.out.println("Right: " + paths[0] + " Up: " + paths[1] + 
        					   " Left: " + paths[2] + " Down: " + paths[3]);
        	
        	// used for naming paths given current position
        	int[][] relPositions = {{0,0}, {0,0}, {-1, 0}, {0, -1}};
        	String[] pathKeys = new String[4];
        	
        	// track 'weights' of paths
        	int lowestWeight = -1;
        	int[] weights = new int[4];
        	
        	// scope out the 'edges', put in hash table if need be
        	for(int i = 0; i < 4; i++) {
        		if(paths[i]) {
	        		pathKeys[i] = (x + relPositions[i][0]) + "-" + (y + relPositions[i][1]);
	        		if(i == 0 || i == 2) 
	        			pathKeys[i] += "-r";
	        		else 
	        			pathKeys[i] += "-u";
	        		
	        		if(!hash.containsKey(pathKeys[i])) {
	        			hash.put(pathKeys[i], new Integer(0));
	        			lowestWeight = 0;
	        		}
	        		else if (hash.get(pathKeys[i]).intValue() < lowestWeight || lowestWeight == -1)
	        			lowestWeight = hash.get(pathKeys[i]).intValue();
	        		
	        		weights[i] = hash.get(pathKeys[i]).intValue();
        		}
        		else
        			weights[i] = -1;
        	 }
        	
        	
        	int options = 0;
        	for(int i = 0; i < 4; i++) {
        		if(paths[i] && weights[i] == lowestWeight)
        			options++;
        	}
        	
        	System.out.println("Options: " + options);
        	
        	int choice = (int) (Math.random() * options + 1);
        	
        	System.out.println("Choice: " + choice);
        	
        	int path = -1;
        	for(int i = 0; i < 4; i++) {
        		if(paths[i] && weights[i] == lowestWeight) {
        			choice--;
        			if(choice == 0)
        				path = i;
        		}
        	}
        	System.out.println("Path: " + path);
        	
        	
        	// DEBUG
        	System.out.println("Old dest: " + dest.toString());
        	
        	// plot trajectory to next cell along chosen path 	
        	switch(path) {
        		case 0:
                	dest.add(new Vector3f(1,0,0));
                	hash.put(pathKeys[0], new Integer(lowestWeight + 1));
                	System.out.println("0");
        			break;
        		case 1:
                	dest.add(new Vector3f(0,1,0));
                	hash.put(pathKeys[1], new Integer(lowestWeight + 1));
                	System.out.println("1");
        			break;
        		case 2:
                	dest.add(new Vector3f(-1,0,0));
                	hash.put(pathKeys[2], new Integer(lowestWeight + 1));
                	System.out.println("2");
        			break;
        		case 3:
                	dest.add(new Vector3f(0,-1,0));
                	hash.put(pathKeys[3], new Integer(lowestWeight + 1));
                	System.out.println("3");
        			break;
        		default:
        			System.out.println("What?");
        			break;
        	}

        	// DEBUG
        	System.out.println("New dest: " + dest.toString());
        	
        	// start moving again
        	setDesiredLinearVelocity(new Vector3f(velocity, 0, 0));
        	
        	turnToward(new Vector3f(dest));
        }
        
        // stay on target!
        turnToward(new Vector3f(dest));
    }
    
    private void checkPeople() {
        // check for people at current location
        Set<WorldObject> personObjects = personSensor.detectPeople();

        if (!personObjects.isEmpty()) {
            
            // check if it is a person we have seen before
        	for (WorldObject object : personObjects) {

            	Map<String, Object> meta = object.getMetadata();
            	
                // this is horrible code that assumes the map entry exists and that it is a String representing an int
                int personNum = Integer.parseInt((String)meta.get("person-id"));

                if (!peopleSeen.contains(personNum)) {

                    // record the new person
                    peopleSeen.add(personNum);
                    
                    logger.info("Found person " + personNum);
                }
            }
        }
    }
    
    @Override
    public void finish() {

        super.finish();
    }
}