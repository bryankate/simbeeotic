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

    private static Logger logger = Logger.getLogger(BroadcastBee.class);


    @Override
    public void initialize() {

        // be sure to call the super class method, otherwise you will not have a body, sensors, or radio!
        super.initialize();

        // add a message listener to print when a message is received. normally a listener
        // would do a heck of a lot more, but this is just an example. it also does not need
        // to be added on initialization.
        radio.addMessageListener(new MessageListener() {

            @Override
            public void messageReceived(double time, byte[] data, float rxPower) {
                logger.info("ID: " + getModelId() + "  " +
                            "time: " + time + "  " +
                            "power: " + rxPower + "  " +
                            "recv from: " + new String(data));
            }
        });

        // set some initial direction
        setHovering(true);
        setDesiredLinearVelocity(new Vector3f((float)getRandom().nextGaussian(),
                                              (float)getRandom().nextGaussian(),
                                              (float)getRandom().nextGaussian()));
    }


    @Override
    protected void applyLogic(double currTime) {

        // send a message
        radio.transmit(("" + getModelId()).getBytes());

        Vector3f pos = getTruthPosition();

        logger.info("ID: " + getModelId() + "  " + 
                    "time: " + currTime + "  " +
                    "pos: " + pos.x + " " + pos.y + " " + pos.z);
    }
}
