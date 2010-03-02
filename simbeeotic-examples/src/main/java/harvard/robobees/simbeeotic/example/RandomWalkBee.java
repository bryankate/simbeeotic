package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.model.GenericBee;
import harvard.robobees.simbeeotic.model.GenericBeeLogic;
import harvard.robobees.simbeeotic.model.sensor.Accelerometer;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.ContactSensor;
import harvard.robobees.simbeeotic.model.sensor.Gyroscope;
import harvard.robobees.simbeeotic.model.sensor.RangeSensor;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;


/**
 * A bee that makes random adjustments to its movement at every time step.
 *
 * @author bkate
 */
public class RandomWalkBee implements GenericBeeLogic {

    private GenericBee host;

    private Accelerometer accelerometer;
    private Gyroscope gyro;
    private Compass compass;
    private RangeSensor rangeBottom;
    private ContactSensor contactBottom;

    private float maxVelocity = 2.0f;                  // m/s
    private float velocitySigma = 0.2f;                // m/s
    private float headingSigma = (float)Math.PI / 16;  // rad

    private static Logger logger = Logger.getLogger(RandomWalkBee.class);


    @Override
    public void intialize(GenericBee bee) {

        host = bee;

        host.setHovering(true);

        accelerometer = host.getSensor("accelerometer", Accelerometer.class);
        gyro = host.getSensor("gyro", Gyroscope.class);
        compass = host.getSensor("compass", Compass.class);
        rangeBottom = host.getSensor("rangeBottom", RangeSensor.class);
        contactBottom = host.getSensor("contactBottom", ContactSensor.class);
    }


    @Override
    public void update(double time) {

        // randomly vary the heading (rotation about the Z axis)
        host.turn((float)host.getRandom().nextGaussian() * headingSigma);

        // randomly vary the velocity in the X and Z directions
        Vector3f newVel = host.getDesiredLinearVelocity();

        newVel.add(new Vector3f((float)host.getRandom().nextGaussian() * velocitySigma,
                                0,
                                (float)host.getRandom().nextGaussian() * velocitySigma));

        // cap the velocity
        if (newVel.length() > maxVelocity) {

            newVel.normalize();
            newVel.scale(maxVelocity);
        }

        host.setDesiredLinearVelocity(newVel);

        Vector3f pos = host.getTruthPosition();
        Vector3f vel = host.getTruthLinearVelocity();

        logger.info("ID: " + host.getModelId() + "  " +
                    "time: " + time + "  " +
                    "pos: " + pos.x + " " + pos.y + " " + pos.z + "  " +
                    "vel: " + vel.x + " " + vel.y + " " + vel.z + " ");
    }


    @Override
    public void messageReceived(double time, byte[] data, float rxPower) {
    }
}
