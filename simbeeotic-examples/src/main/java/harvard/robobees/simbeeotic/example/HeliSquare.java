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


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.BaseHeliBehavior;
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
    public void start(Platform platform, final HeliControl control, final Boundary bounds) {

        posSensor = platform.getSensor("position-sensor", PositionSensor.class);

        super.start(platform, control, bounds);


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
