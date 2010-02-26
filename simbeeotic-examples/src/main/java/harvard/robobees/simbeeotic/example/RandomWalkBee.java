package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.model.SimpleBee;

import javax.vecmath.Vector3f;

import org.apache.log4j.Logger;


/**
 * A bee that makes random adjustments to its movement at every time step.
 *
 * @author bkate
 */
public class RandomWalkBee extends SimpleBee {

    private float maxVelocity = 2.0f;                  // m/s
    private float velocitySigma = 0.2f;                // m/s
    private float headingSigma = (float)Math.PI / 16;  // rad

    private static Logger logger = Logger.getLogger(RandomWalkBee.class);


    @Override
    protected void applyLogic(double currTime) {

        if (!isHovering()) {
            setHovering(true);
        }

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
                    "time: " + currTime + "  " +
                    "pos: " + pos.x + " " + pos.y + " " + pos.z + "  " +
                    "vel: " + vel.x + " " + vel.y + " " + vel.z + " ");
    }
}
