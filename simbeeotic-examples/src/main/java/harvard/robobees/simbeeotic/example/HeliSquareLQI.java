package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.BaseHeliBehaviorLQI;
import harvard.robobees.simbeeotic.model.Boundary;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.Platform;
import harvard.robobees.simbeeotic.model.Timer;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;


/**
 * An example of using the base helicopter behavior to script a simple flight pattern.
 *
 * @author bkate
 */
public class HeliSquareLQI extends BaseHeliBehaviorLQI {

    private Timer navTimer;
    private int heliID;
    
    private Vector3f[] waypoints =
    	new Vector3f[] {
    		new Vector3f(1, 1, 1),
            new Vector3f(1, -1, 1),
            new Vector3f(-1, -1, 1),
            new Vector3f(-1, 1, 1),
    		new Vector3f(1, 1, 1)
    	};
    
    private PositionSensor posSensor;
    private static Logger logger = Logger.getLogger(HeliSquareLQI.class);


    @Override
    public void start(final Platform platform, final HeliControl control, final Boundary bounds) {

        posSensor = platform.getSensor("position-sensor", PositionSensor.class);

        super.start(platform, control, bounds);


        Vector3f pos = posSensor.getPosition();
        moveToPoint(pos.x, pos.y, 1.0f, 0.1f, null);

        logger.info("Starting program.");
        heliID = control.getHeliId();
        

        // a loop that checks to see if a segment of the scripted path is complete
        // and assigns new waypoints. once all segments are complete the heli idles.
        navTimer = platform.createTimer(new TimerCallback() {
        	
            private boolean reachedWaypoint = true;
            private int currWaypoint = -1;

            @Override
            public void fire(SimTime time)
            {
            	if (reachedWaypoint)
            	{
                    currWaypoint++;
                    reachedWaypoint = false;

                    if (currWaypoint >= waypoints.length) {

                        logger.info("Heli: " + heliID + " Finished scripted path, idling.");

                        // done the script, land the heli at the hive
                        landHeli();
                    }
                    else
                    {
                        logger.info("Heli: " + heliID + " Moving to waypoint " + currWaypoint + " " + waypoints[currWaypoint]);

                        moveToPoint(waypoints[currWaypoint].x,
                                    waypoints[currWaypoint].y,
                                    waypoints[currWaypoint].z,
                                    0.1f,
                                    new MoveCallback()
                        			{
                                        @Override
                                        public void reachedDestination()
                                        {
                                            logger.info("Heli: " + heliID + " Reached waypoint.");
                                            reachedWaypoint = true;
                                            hover();
                                        }
                                    });
                    }
                }
            }
        }, 2, TimeUnit.SECONDS, 20, TimeUnit.MILLISECONDS);
    }


    @Override
    public void stop()
    {
        super.stop();
        
        navTimer.cancel();
    }
}
