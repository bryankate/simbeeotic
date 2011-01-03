package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.HeliBehavior;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.Platform;
import harvard.robobees.simbeeotic.model.RuntimeModelingException;
import harvard.robobees.simbeeotic.model.Timer;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.util.PIDController;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;


/**
 * An example of using the HWIL capabilities to control a helicopter
 * in the testbed.
 *
 * @author bkate
 */
public class HeliHover implements HeliBehavior {

    private Timer pidTimer;

    private static Logger logger = Logger.getLogger(HeliHover.class);


    @Override
    public void start(final Platform platform, final HeliControl control) {

        final PositionSensor posSensor = platform.getSensor("position-sensor", PositionSensor.class);

        if (posSensor == null) {
            throw new RuntimeModelingException("A position sensor is needed for the HeliHover behavior.");
        }
        
        // set heli to default state
        control.setThrust(0);
        control.setYaw(0.5);
        control.setPitch(0.5);
        control.setRoll(0.5);

        // PID loop for hovering at a given altitude
        pidTimer = platform.createTimer(new TimerCallback() {

            private double throttle = 0.4;
            private PIDController throttlePID = new PIDController(0.8, 0.004, 1.5e-9, 0.01);


            @Override
            public void fire(SimTime time) {

                Vector3f pos = posSensor.getPosition();
                Double diff = throttlePID.update(time.getTime(), pos.z);

                logger.info(pos + " " + throttle + " " + diff);

                if (diff != null) {

                    // do not move too much in one timestep
                    if (diff > 0.01) {
                        diff = 0.01;
                    }

                    if (diff < -0.01) {
                        diff = -0.01;
                    }

                    // gravity compensation
                    if (diff < 0) {
                        diff /= 2;
                    }

                    // set new throttle
                    throttle += diff;
                }

                control.setThrust(throttle);
            }

        }, 0, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
    }


    @Override
    public void stop() {
        pidTimer.cancel();
    }
}
