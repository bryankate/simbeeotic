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


/**
 * A bee that hangs out in underground mines.
 *
 * @author chartier
 */
public class MineBee2 extends SimpleBee {

    private float velocity = 0.2f;  // m/s
    private PersonSensor personSensor;
    private WallSensor wallSensor;
    private Set<Integer> peopleSeen = new HashSet<Integer>();

    private static Logger logger = Logger.getLogger(MineBee.class);

    private Vector3f dest = new Vector3f();
    private Vector3f relPos = new Vector3f();
    private Vector3f currPos = new Vector3f();
    
    private final float delta = 0.01f;
    
    @Override
    public void initialize() {

        // always call the super
        super.initialize();

        // hover
        setHovering(true);

        // initialize sensors
        personSensor = getSensor("person-sensor", PersonSensor.class);
        wallSensor = getSensor("wall-sensor", WallSensor.class);
        
        dest = getTruthPosition();
        
        // movement speed
        setDesiredLinearVelocity(new Vector3f(velocity, 0, 0));
        
        // direction
        turnToward(new Vector3f(dest));
    }


    @Override
    protected void updateKinematics(SimTime time) {
    	
    	currPos = getTruthPosition();
        relPos.sub(dest, currPos);
        
        if(relPos.length() < delta)
        {
        	// stop moving
        	setDesiredLinearVelocity(new Vector3f(0, 0, 0));
        	
        	// search for people
        	checkPeople();
        	
        	// prepare to check paths by looking in y direction
        	Vector3f point = new Vector3f(); 
        	point.add(currPos, new Vector3f(0, 1, 0));
        	turnToward(point);
        	
        	// check available paths
        	boolean[] paths = new boolean[4];
        	// 0 - pos_y, 1 - neg_x, 2 - neg_y, 3 - pos_y
        	for(int i = 0; i < 4; i++) {
        		if(wallSensor.isWallDetected())
        			paths[i] = true;
        		turn((float) (Math.PI/2));
        	}
        	
        	// choose one path randomly of the good options
        	int options = 0;
        	for(int i = 0; i < 4; i++)
        		if(paths[i]) 
        			options++;
        	int choice = (int) (Math.random() * options + 1);
        	int path = -1;
        	for(int i = 0; i < 4; i++) {
        		if(paths[i])
        			choice--;
        		if(choice == 0)
        			path = i;
        	}
        	
        	// DEBUG
        	System.out.println("Old dest: " + dest.toString());
        	
        	// plot trajectory to next cell along chosen path 	
        	switch(path) {
        		case 0:
                	dest.add(new Vector3f(0,1,0));
                	System.out.println("0");
        			break;
        		case 1:
                	dest.add(new Vector3f(-1,0,0));
                	System.out.println("1");
        			break;
        		case 2:
                	dest.add(new Vector3f(0,-1,0));
                	System.out.println("2");
        			break;
        		case 3:
                	dest.add(new Vector3f(1,0,0));
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