package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.SimTime;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class InertBee extends SimpleBee {

    private static Logger logger = Logger.getLogger(InertBee.class);


    @Override
    public void initialize() {
        super.initialize();
    }


    @Override
    protected void updateKinematics(SimTime time) {

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
}
