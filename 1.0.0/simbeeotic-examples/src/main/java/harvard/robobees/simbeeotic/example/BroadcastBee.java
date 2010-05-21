package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.comms.MessageListener;

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
    public void initialize() {

        super.initialize();

        // add a message listener to print when a messageis received. normally a listener
        // would do a heck of a lot more, but this is just an example. it also does not need
        // to be added on initialization.
        radio.addMessageListener(new MessageListener() {

            @Override
            public void messageReceived(float time, byte[] data) {
                logger.info("ID: " + getModelId() + "  " +
                            "time: " + time + "  " +
                            "recv from: " + new String(data));
            }
        });
    }


    @Override
    protected void applyLogic(float currTime) {

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
}
