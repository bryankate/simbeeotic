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


import harvard.robobees.simbeeotic.model.BaseAutoHeliBehavior;
import harvard.robobees.simbeeotic.model.Boundary;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.Platform;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;


/**
 * An example of using the base helicopter behavior to script a simple flight pattern.
 *
 * @author bkate
 */
public class AutoHeliWaypoint extends BaseAutoHeliBehavior {

    private java.util.Timer navTimer;
    private int heliID;

    private Vector3f[] waypoints = new Vector3f[] { new Vector3f(0.0f, 0.0f, 0.75f),
                                                    new Vector3f(0.0f, 2.25f, 0.75f),
                                                    new Vector3f(0.0f, -2.25f, 0.75f),
                                                    new Vector3f(0.0f, 0.0f, 0.75f)};
    private PositionSensor posSensor;

    private static Logger logger = Logger.getLogger(AutoHeliWaypoint.class);


    @Override
    public void start(Platform platform, final HeliControl control, final Boundary bounds) {

        posSensor = platform.getSensor("position-sensor", PositionSensor.class);

        super.start(platform, control, bounds);


        Vector3f pos = posSensor.getPosition();
        //moveToPoint(pos.x, pos.y, 1, 0.1, null);

        logger.info("Starting program.");

        heliID = control.getHeliId();

       // a loop that checks to see if a segment of the scripted path is complete
       // and assigns new waypoints. once all segments are complete the heli idles.
       navTimer = new java.util.Timer();
       navTimer.scheduleAtFixedRate(new java.util.TimerTask() {

            double tol = 0.2;
            int headingWait = 350;

            private boolean reachedWaypoint = true;
            private int currWaypoint = -1;
            private int headingWaitCtr = headingWait;

            @Override
            public void run()
            {
            	if (reachedWaypoint) {
                    currWaypoint++;
                    reachedWaypoint = false;

                    if (currWaypoint >= waypoints.length) {
                        logger.info("Heli: " + heliID + " Finished scripted path, landing.");
                        land();
                    } else if( headingWaitCtr <= 0 ) {
                            logger.info("Heli: " + heliID + " Moving to waypoint " + currWaypoint + " " + waypoints[currWaypoint]);
                            if(currWaypoint == 2) {
                                runToPoint(waypoints[currWaypoint].x,
                                        waypoints[currWaypoint].y,
                                        waypoints[currWaypoint].z,
                                        tol,
                                        new MoveCallback()
                                        {
                                            @Override
                                            public void reachedDestination()
                                            {
                                                //hover(1.0);
                                                logger.info("Heli: " + heliID + " Reached waypoint.");
                                                reachedWaypoint = true;
                                            }
                                        });
                                headingWaitCtr = headingWait;
                            }
                            else {
                                moveToPoint(waypoints[currWaypoint].x,
                                        waypoints[currWaypoint].y,
                                        waypoints[currWaypoint].z,
                                        tol,
                                        new MoveCallback()
                                        {
                                            @Override
                                            public void reachedDestination()
                                            {
                                                //hover(1.0);
                                                logger.info("Heli: " + heliID + " Reached waypoint.");
                                                reachedWaypoint = true;
                                            }
                                        });
                                headingWaitCtr = headingWait;
                            }
                    } else { // headingWaitCtr > 0
                        if( headingWaitCtr == headingWait ) {
                            if( currWaypoint > 0 ) {
                                hover(waypoints[currWaypoint-1]);
                            } else {
                                hover(waypoints[currWaypoint].getZ());
                            }
                            face(waypoints[currWaypoint]);
                        }
                        headingWaitCtr--;
                        currWaypoint--; // negate default run() operations
                        reachedWaypoint = true; // negate default run() operations
                    }
                }
            }
        }, 100, 10);
    }


    @Override
    public void stop() {
        
        super.stop();
        
        navTimer.cancel();
    }
}
