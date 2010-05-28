package harvard.robobees.simbeeotic.example;


import org.apache.log4j.Logger;
import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.model.Timer;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.comms.MessageListener;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;


/**
 * A bee that broadcasts a message at every timestep and records the messages
 * that it receives.
 * 
 * @author bkate
 */
public class BroadcastBee extends SimpleBee implements MessageListener {

    private static Logger logger = Logger.getLogger(BroadcastBee.class);


    @Override
    public void initialize() {

        super.initialize();

        // set some initial direction
        setHovering(true);
        setDesiredLinearVelocity(new Vector3f((float)getRandom().nextGaussian() * 0.1f,
                                              (float)getRandom().nextGaussian() * 0.1f,
                                              (float)getRandom().nextGaussian() * 0.1f));

        getRadio().addMessageListener(this);

        // send a message every second
        Timer msgTimer = createTimer(new TimerCallback() {

            public void fire(SimTime time) {
                getRadio().transmit(("" + getModelId()).getBytes());
            }
        }, 0, TimeUnit.MILLISECONDS, 1, TimeUnit.SECONDS);
    }


    @Override
    protected void updateKinematics(SimTime time) {

        Vector3f pos = getTruthPosition();

        logger.info("ID: " + getModelId() + "  " +
                    "time: " + time.getImpreciseTime() + "  " +
                    "pos: " + pos);
    }


    public void messageReceived(SimTime time, byte[] data, double rxPower) {

        logger.info("ID: " + getModelId() + "  " +
                    "time: " + time.getImpreciseTime() + "  " +
                    "power: " + rxPower + "  (dBm) " +
                    "recv from: " + new String(data));
    }


    @Override
    public void finish() {
    }
}
