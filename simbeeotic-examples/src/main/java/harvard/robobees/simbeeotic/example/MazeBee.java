package harvard.robobees.simbeeotic.example;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.util.TracePlotter2D;
import harvard.robobees.simbeeotic.SimTime;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class MazeBee extends SimpleBee {

    private static Logger logger = Logger.getLogger(MyBee.class);
    //private Vector3f goal = new Vector3f(2.5f,0.5f,0.5f);
    
    //private TracePlotter2D tracePlotter = new TracePlotter2D("Bee", "x", "y");

    @Override
    public void initialize() {
        super.initialize();
        setHovering(true);
        setDesiredLinearVelocity(new Vector3f(1,0,0));
        //turnToward(goal);
        
        //tracePlotter.setTitle("Bee " + getModelId());
        //tracePlotter.setCap("myBeePosition", 20);
        
        // read in and plot tracer data
        /*Scanner data = new Scanner("null");
		try {
			data = new Scanner(new File("/home/chartier/workspace/simbeeotic/simbeeotic-examples/src/main/resources/scenarios/mazegentracerdata.dat"));
		} catch (FileNotFoundException e) {
			System.out.println("Tracer Data File Not Found");
		}*/
        
      /*  double x, y;
        System.out.println(data.hasNextFloat());
        int i = 0;
        while (data.hasNextFloat() != false)
        {
        	x = data.nextFloat();
        	y = data.nextFloat();
        	tracePlotter.addData("structures" + (i / 2), x, y);
        	System.out.println("x: " + x + " y: " + y);
        	i++;
        }*/
    }


    @Override
    protected void updateKinematics(SimTime time) {

    	//turnToward(goal);
    	Vector3f pos = getTruthPosition();
        Vector3f vel = getTruthLinearVelocity();

        /*logger.info("ID: " + getModelId() + "  " +
                    "time: " + time.getImpreciseTime() + "  " +
                    "pos: " + pos + "  " +
                    "vel: " + vel + " "); */
        
        //tracePlotter.addData("myBeePosition", pos.x, pos.y);
    }
    
    @Override
    public void finish() {
    	//tracePlotter.dispose();
    }
}
