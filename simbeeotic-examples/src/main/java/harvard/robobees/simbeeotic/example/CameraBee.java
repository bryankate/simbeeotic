package harvard.robobees.simbeeotic.example;

import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.model.sensor.CameraSensor;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.vecmath.Vector3f;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Joseph
 * Date: 4/29/11
 * Time: 9:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class CameraBee extends SimpleBee{
    private CameraSensor camera;
    private static Logger logger = Logger.getLogger(CameraBee.class);
    @Override
    public void initialize() {

        super.initialize();

        //Have bee sit in place (can be overridden later)
        setHovering(true);

        camera = getSensor("camera", CameraSensor.class);

    }
    @Override
    protected void updateKinematics(SimTime time) {

        Vector3f pos = getTruthPosition();
        Vector3f vel = getTruthLinearVelocity();
        setDesiredLinearVelocity(new Vector3f(0f,0.3f,0f));

        //Log information
        logger.info("ID: " + getModelId() + "  " +
                    "time: " + time.getImpreciseTime() + "  " +
                    "pos: " + pos + "  " +
                    "vel: " + vel + " ");

        double cur_time = time.getImpreciseTime();

        //Write image to file every second (for testing purposes)
        BufferedImage img = camera.getView().getImg();
        if(Math.abs(cur_time-(int)cur_time)<0.01){
            try{
                File output = new File("Image"+Double.toString(cur_time)+".png");
                ImageIO.write(img, "png", output);
            }
            catch (IOException e) {
            }
            /* For edge/circle detection, but slows down sim
            camera.getView().writeEdges("edges"+Double.toString(cur_time));
            if(cur_time == 2)
                camera.getView().writeCircles("circles", 10, 50);
            */
        }

    }
    @Override
    public void finish() {
    }
}
