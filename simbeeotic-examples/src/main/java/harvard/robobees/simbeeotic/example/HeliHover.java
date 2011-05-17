package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.HeliBehavior;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.Platform;
import harvard.robobees.simbeeotic.model.RuntimeModelingException;
import harvard.robobees.simbeeotic.model.Timer;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.sensor.PoseSensor;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.util.PIDController;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;


/**
 * An example of using the HWIL capabilities to control a helicopter
 * in the testbed.
 *
 * @author bkate
 * @author dicai
 */
public class HeliHover implements HeliBehavior {

    private Timer pidTimer;

    private static Logger logger = Logger.getLogger(HeliHover.class);


    @Override
    public void start(final Platform platform, final HeliControl control) {

        final PositionSensor posSensor = platform.getSensor("position-sensor", PositionSensor.class);
        final PoseSensor orientSensor = platform.getSensor("pose-sensor", PoseSensor.class);

        if (posSensor == null) {
            throw new RuntimeModelingException("A position sensor is needed for the HeliHover behavior.");
        }

         if (orientSensor == null) {
            throw new RuntimeModelingException("A pose sensor is needed for the HeliHover behavior.");
        }

        // set heli to default state
        control.setThrust(0);
        control.setYaw(0.5);
        control.setPitch(0.5);
        control.setRoll(0.5);

        // PID loop for hovering at a given altitude
        pidTimer = platform.createTimer(new TimerCallback() {

            // we do not use a PID controller for yaw, so it has a standalone set point
            private double yawSetPoint = 0;  // radians

            private double throttle = 0.5;
            private double pitch = 0.6;
            private double roll = 0.5;

            // initialize PID controllers
            private PIDController throttlePID = new PIDController(0.8, 0.004, 1.5e-9, 0.01);
            private PIDController pitchPID = new PIDController(0.125, 0.003, 1.0e-9, 0.01);
            private PIDController rollPID = new PIDController(0, 0.00005, 1.0e-9, 0.01);


            @Override
            public void fire(SimTime time) {

                // takeoff
                if (time.getImpreciseTime() < 1.5) {

                    control.setThrust(throttle);
                    return;
                }

                Vector3f pos = posSensor.getPosition();
                Vector3f euler = MathUtil.quaternionToEulerZYX(orientSensor.getPose());

                Double throttleDiff = throttlePID.update(time.getTime(), pos.z);
                Double pitchDiff = pitchPID.update(time.getTime(), euler.x);
                Double rollDiff = rollPID.update(time.getTime(), euler.y);


                logger.info(time + " " + pos);

                if (throttleDiff != null) {

                    // do not move too much in one timestep
                    if (throttleDiff > 0.01) {
                        throttleDiff = 0.01;
                    }

                    if (throttleDiff < -0.01) {
                        throttleDiff = -0.01;
                    }

                    // gravity compensation
                    if (throttleDiff < 0) {
                        throttleDiff /= 2;
                    }

                    // set new throttle
                    throttle += throttleDiff;
                }

                if (pitchDiff != null) {
                    pitch += pitchDiff;
                }

                if (rollDiff != null) {
                    roll += rollDiff;
                }

                // set yaw using a proportional response to the error
                double yaw = 0.5 + (-0.3*Math.sin(euler.z-yawSetPoint));

                control.setThrust(throttle);
                control.setYaw(yaw);
                control.setPitch(pitch);
                control.setRoll(roll);
            }

        }, 0, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
    }


    @Override
    public void stop() {
        pidTimer.cancel();
    }
}
