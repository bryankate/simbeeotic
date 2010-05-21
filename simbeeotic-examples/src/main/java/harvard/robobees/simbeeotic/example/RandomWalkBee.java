package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.model.sensor.Accelerometer;
import harvard.robobees.simbeeotic.model.sensor.Gyroscope;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.RangeSensor;
import harvard.robobees.simbeeotic.model.sensor.ContactSensor;
import harvard.robobees.simbeeotic.SimTime;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;


/**
 * A bee that makes random adjustments to its movement at every time step.
 *
 * @author bkate
 */
public class RandomWalkBee extends SimpleBee {

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
    public void initialize() {

        super.initialize();

        setHovering(true);

        accelerometer = getSensor("accelerometer", Accelerometer.class);
        gyro = getSensor("gyro", Gyroscope.class);
        compass = getSensor("compass", Compass.class);
        rangeBottom = getSensor("rangeBottom", RangeSensor.class);
        contactBottom = getSensor("contactBottom", ContactSensor.class);
    }


    @Override
    protected void updateKinematics(SimTime time) {

        // randomly vary the heading (rotation about the Z axis)
        turn((float)getRandom().nextGaussian() * headingSigma);

        // randomly vary the velocity in the X and Z directions
        Vector3f newVel = getDesiredLinearVelocity();

        newVel.add(new Vector3f((float)getRandom().nextGaussian() * velocitySigma,
                                0,
                                (float)getRandom().nextGaussian() * velocitySigma));

        // cap the velocity
        if (newVel.length() > maxVelocity) {

            newVel.normalize();
            newVel.scale(maxVelocity);
        }

        setDesiredLinearVelocity(newVel);

        Vector3f pos = getTruthPosition();
        Vector3f vel = getTruthLinearVelocity();

        logger.info("ID: " + getModelId() + "  " +
                    "time: " + time.getImpreciseTime() + "  " +
                    "pos: " + pos.x + " " + pos.y + " " + pos.z + "  " +
                    "vel: " + vel.x + " " + vel.y + " " + vel.z + " ");
    }


    @Override
    public void finish() {
    }


    @Inject(optional = true)
    public final void setMaxVelocity(@Named(value = "max-vel") final float vel) {
        this.maxVelocity = vel;
    }


    @Inject(optional = true)
    public final void setVelocitySigma(@Named(value = "vel-sigma") final float sigma) {
        this.velocitySigma = sigma;
    }


    @Inject(optional = true)
    public final void setHeadingSigma(@Named(value = "heading-sigma") final float sigma) {
        this.headingSigma = sigma;
    }
}
