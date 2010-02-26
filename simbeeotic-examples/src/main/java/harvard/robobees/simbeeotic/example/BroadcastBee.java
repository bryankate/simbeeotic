package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.model.SimTime;

import javax.vecmath.Vector3f;

import org.apache.log4j.Logger;


/**
 * A bee that broadcasts a message at every timestep and records the messages
 * that it receives.
 * 
 * @author bkate
 */
public class BroadcastBee extends SimpleBee {

    private boolean started = false;

    private static Logger logger = Logger.getLogger(BroadcastBee.class);


    @Override
    protected void applyLogic(final SimTime currTime) {

        // when we start the sim, move in a fixed direction
        if (!started) {

            setHovering(true);
            setDesiredLinearVelocity(new Vector3f((float)getRandom().nextGaussian(),
                                                  (float)getRandom().nextGaussian(),
                                                  (float)getRandom().nextGaussian()));

            started = true;
        }

        // send a message
        radio.transmit(("" + getModelId()).getBytes());

        Vector3f pos = getTruthPosition();

        logger.info("ID: " + getModelId() + "  " + 
                    "time: " + currTime + "  " +
                    "pos: " + pos.x + " " + pos.y + " " + pos.z);
    }


    @Override
    protected void handleMessage(SimTime currTime, byte[] message) {

        logger.info("ID: " + getModelId() + "  " +
                    "time: " + currTime + "  " +
                    "recv from: " + new String(message));
    }
}
