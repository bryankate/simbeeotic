package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.BaseHeliBehavior;
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
public class HeliSquare extends BaseHeliBehavior {

    private Timer navTimer;
    private int heliID;
    
    private Vector3f[] waypoints = new Vector3f[] {new Vector3f(1, 1, 1),
                                                   new Vector3f(1, -1, 1),
                                                   new Vector3f(-1, -1, 1),
                                                   new Vector3f(-1, 1, 1),
                                                   new Vector3f(1, 1, 1)};
    
    private PositionSensor posSensor;

    private static Logger logger = Logger.getLogger(HeliSquare.class);


    @Override
    public void start(Platform platform, final HeliControl control) {

        posSensor = platform.getSensor("position-sensor", PositionSensor.class);

        super.start(platform, control);


        Vector3f pos = posSensor.getPosition();
        moveToPoint(pos.x, pos.y, 1, 0.1, null);

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
                        landAtHive();
                    }
                    else
                    {
                        logger.info("Heli: " + heliID + " Moving to waypoint " + currWaypoint + " " + waypoints[currWaypoint]);

                        moveToPoint(waypoints[currWaypoint].x,
                                    waypoints[currWaypoint].y,
                                    waypoints[currWaypoint].z,
                                    0.25,
                                    new MoveCallback()
                        			{
                                        @Override
                                        public void reachedDestination()
                                        {
                                            logger.info("Heli: " + heliID + " Reached waypoint.");
                                            reachedWaypoint = true;
                                        }
                                    });
                    }
                }
            }
        }, 3, TimeUnit.SECONDS, 5, TimeUnit.SECONDS);
    }


    @Override
    public void stop() {
        
        super.stop();
        
        navTimer.cancel();
    }
}
