package harvard.robobees.simbeeotic.model;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations;
import org.apache.log4j.Logger;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeUnit;


/**
 * A debugging model that outputs its position and orientation (as tracked in Vicon)
 * to a file for debugging purposes.
 *
 * @author bkate
 */
public class TrackedObject extends ViconObject {

    private Writer out;

    // params
    private long updateRate = 33;  // ms


    private static Logger logger = Logger.getLogger(TrackedObject.class);


    @Override
    public void initialize() {

        super.initialize();

        try {
            out = new FileWriter(getName() + ".log");
        }
        catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // data recording timer
        createTimer(new TimerCallback() {

            @Override
            public void fire(SimTime time) {

                Vector3f pos = getTruthPosition();
                Quat4f orient = getTruthOrientation();

                logger.debug("time: " + time.getTime() + "  pos: " + pos + "  orient: " + orient);

                try {

                    out.write(time.getTime() + " " + pos.x + " " + pos.y + " " + pos.z + " " +
                              orient.x + " " + orient.y + " " + orient.z + " " + orient.w + "\n");
                }
                catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

        }, 0, TimeUnit.MILLISECONDS, updateRate, TimeUnit.MILLISECONDS);
    }


    @Override
    public void finish() {

        try {

            out.flush();
            out.close();
        }
        catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }

        super.finish();
    }


    @Inject(optional=true)
    public final void setUpdateRate(@Named("update-rate") long rate) {
        this.updateRate = rate;
    }
}
