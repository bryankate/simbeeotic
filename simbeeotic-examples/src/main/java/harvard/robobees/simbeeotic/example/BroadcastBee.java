package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.model.GenericBeeLogic;
import harvard.robobees.simbeeotic.model.GenericBee;

import javax.vecmath.Vector3f;

import org.apache.log4j.Logger;


/**
 * A bee that broadcasts a message at every timestep and records the messages
 * that it receives.
 * 
 * @author bkate
 */
public class BroadcastBee implements GenericBeeLogic {

    private GenericBee host;

    private static Logger logger = Logger.getLogger(BroadcastBee.class);


    @Override
    public void intialize(GenericBee bee) {

        host = bee;

        // set some initial direction
        host.setHovering(true);
        host.setDesiredLinearVelocity(new Vector3f((float)host.getRandom().nextGaussian(),
                                                   (float)host.getRandom().nextGaussian(),
                                                   (float)host.getRandom().nextGaussian()));
    }


    @Override
    public void update(double time) {

        // send a message
        host.getRadio().transmit(("" + host.getModelId()).getBytes());

        Vector3f pos = host.getTruthPosition();

        logger.info("ID: " + host.getModelId() + "  " +
                    "time: " + time + "  " +
                    "pos: " + pos.x + " " + pos.y + " " + pos.z);
    }


    @Override
    public void messageReceived(double time, byte[] data, float rxPower) {

        logger.info("ID: " + host.getModelId() + "  " +
                    "time: " + time + "  " +
                    "power: " + rxPower + "  " +
                    "recv from: " + new String(data));
    }
}
