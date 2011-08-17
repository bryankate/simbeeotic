package harvard.robobees.simbeeotic.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.sensor.PoseSensor;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.util.MathUtil;

import javax.vecmath.GMatrix;
import javax.vecmath.GVector;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import org.apache.log4j.Logger;

import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Behavior for testbed helicopters implementing
 * Linear-Quadratic-Gaussian control of six variables:
 *     u -- body-frame x velocity
 *     v -- body-frame y velocity
 *     w -- body-frame z velocity
 *     p -- angular velocity of roll
 *     q -- angular velocity of pitch
 *     r -- angular velocity of yaw
 * 
 * @author davidpalmer
 */
public abstract class LQGHeliBehavior implements HeliBehavior
{
	// sensors
	private PositionSensor s1;
	private PoseSensor s2;
	
	// trim values
    private double throttleTrim = 400;
    private double rollTrim = 400;
    private double pitchTrim = 400;
    private double yawTrim = 400;
	
	// world-frame state
	private Vector3f lastPos;
	private Quat4f lastPose;
	private long lastTime;
	
	// control
	private String controlMatrixDirectory;
	private Timer controlTimer;
	private GVector reference;
	private LQGController lqg;
	
	// logging
	private static Logger logger = Logger.getLogger(LQGHeliBehavior.class);
	private boolean logData = false;
	private String logPath = "heli_data.txt";
	private FileWriter logFW;
    private BufferedWriter logBW;
    private String more; // extra data to be logged
	
	private GMatrix loadCSVMatrix(File file) throws IOException
	{
		double[] matrix = new double[144];
		int index = 0;
		int numRows = 0;
		
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		
		// read the file line by line, parsing the rows
		while(br.ready())
		{
			String line = br.readLine();
			String[] row = line.split(",");
			for(String elt : row)
			{
				matrix[index] = Double.parseDouble(elt);
				index++;
			}
			numRows++;
		}
		br.close();
		
		return new GMatrix(numRows, index / numRows, matrix);
	}
	
	protected void appendLoggingData(final String more)
	{
		this.more = more;
	}
	
	protected void record(final float dt, final GVector r, final GVector y, final GVector u)
	{
		if (logData)
    	{
	    	try
	    	{
	    		logBW.write(dt + ", 0, " + u.getElement(0) + ", " + u.getElement(1) +
	    				  ", " + u.getElement(2) + ", " + u.getElement(3) + ", " +
	    				  y.getElement(0) + ", " + y.getElement(1) + ", " + y.getElement(2) + ", " +
	    				  y.getElement(3) + ", " + y.getElement(4) + ", " + y.getElement(5) + ", " + more + "\n");
	    	}
	    	catch (IOException e)
	    	{
	    		// do nothing
	    	}
    	}
	}
	
	@Override
	public void start(final Platform platform, final HeliControl control, final Boundary bounds)
	{
		// set up data logging
		try
		{
			logFW = new FileWriter(logPath);
			logBW = new BufferedWriter(logFW);
		}
		catch (IOException e)
		{
			logger.info("Failed to start data logging. Ending simulation...");
			System.exit(1);
		}
		
		// connect to sensors
		s1 = platform.getSensor("position-sensor", PositionSensor.class);
		s2 = platform.getSensor("pose-sensor", PoseSensor.class);
		
		// get initial global state
		lastPos = s1.getPosition();
		lastPose = s2.getPose();
		
		try
		{
			// load control matrices
			GMatrix lqgA = loadCSVMatrix(new File(controlMatrixDirectory, "LQG_A"));
			GMatrix lqgB = loadCSVMatrix(new File(controlMatrixDirectory, "LQG_B"));
			GMatrix lqgC = loadCSVMatrix(new File(controlMatrixDirectory, "LQG_C"));
			GMatrix lqgD = loadCSVMatrix(new File(controlMatrixDirectory, "LQG_D"));
			
			// check matrix dimensions
			assert lqgB.getNumCol() == 6;
			assert lqgC.getNumRow() == 4;
			assert lqgD.getNumRow() == 4;
			assert lqgD.getNumCol() == 6;
			
			// initialize controller and reference vector
			lqg = new LQGController(lqgA, lqgB, lqgC, lqgD);
			reference = new GVector(lqgB.getNumCol());
		}
		
		catch(Exception e)
		{
			logger.info("Failed to load control matrices. Ending simulation...");
			stop();
		}
		
		// take off
		control.setThrust(0.5);
		
		// start control timer
		controlTimer = platform.createTimer(new TimerCallback()
        {
            public void fire(SimTime time)
            {
            	// get sensor measurements
        		float dt = (System.currentTimeMillis() - lastTime) / 1000.0f;
            	Vector3f uvw = getUVW(dt);
            	Vector3f pqr = getPQR(dt);
            	
            	// log measurements
            	logger.info(uvw + " " + pqr);
            	
            	// update saved values
            	lastTime = System.currentTimeMillis();
            	lastPos = s1.getPosition();
        		lastPose = s2.getPose();
            	
        		// calculate controls
        		double[] y_array = {uvw.x, uvw.y, uvw.z, pqr.x, pqr.y, pqr.z};
        		GVector y = new GVector(y_array);
        		y.sub(reference);
        		GVector u = lqg.calculateControls(y);
        		
        		// log reference, measurements, controls
            	logger.info(reference + "\n" + y + "\n" + u);
        		
        		// record data
        		record(dt, reference, y, u);
        		
        		// implement controls
        		control.setThrust(HWILBee.normCommand((int) (u.getElement(0) + throttleTrim)));
        		control.setRoll(HWILBee.normCommand((int) (u.getElement(1) + rollTrim)));
        		control.setPitch(HWILBee.normCommand((int) (u.getElement(2) + pitchTrim)));
        		control.setYaw(HWILBee.normCommand((int) (u.getElement(3) + yawTrim)));
            }
        }, 1, TimeUnit.SECONDS, 20, TimeUnit.MILLISECONDS);
	}

	@Override
	public void stop()
	{
		// shut down data logging
		try
		{
			logBW.close();
		}
		
		catch (IOException e)
		{
			// do nothing (logBW likely already closed)
		}
		
		if (controlTimer != null)
			controlTimer.cancel();
	}
	
	protected Vector3f getUVW(float dt)
	{
		Vector3f vel = new Vector3f();
    	vel.sub(s1.getPosition(), lastPos);
    	vel.scale(1/dt);
    	float z = vel.z;
    	
    	Quat4f pose = s2.getPose();
    	pose.inverse();
    	
    	Transform orient = new Transform();
    	orient.setIdentity();
    	orient.setRotation(pose);
    	orient.transform(vel);
    	
    	vel.z = z;
    	return vel;
	}
	
	protected Vector3f getPQR(float dt)
	{
		Quat4f dQ = new Quat4f();
    	dQ.mulInverse(s2.getPose(), lastPose);
    	
    	Vector3f dEuler = MathUtil.quaternionToEulerZYX(dQ);
    	dEuler.scale(1/dt);
    	
    	return dEuler;
	}
	
	protected void setU(double u)
	{
		reference.setElement(0, u);
	}
	
	protected void setV(double v)
	{
		reference.setElement(1, v);
	}
	
	protected void setW(double w)
	{
		reference.setElement(2, w);
	}
	
	protected void setP(double p)
	{
		reference.setElement(3, p);
	}
	
	protected void setQ(double q)
	{
		reference.setElement(4, q);
	}
	
	protected void setR(double r)
	{
		reference.setElement(5, r);
	}
	
	private class LQGController
	{
		// gain matrices
		private GMatrix a;
		private GMatrix b;
		private GMatrix c;
		private GMatrix d;
		
		// state space model
		private GVector x;
		
		public LQGController(GMatrix a, GMatrix b, GMatrix c, GMatrix d)
		{
			this.a = a;
			this.b = b;
			this.c = c;
			this.d = d;
			this.x = new GVector(a.getNumRow());
		}
		
		public GVector calculateControls(GVector y)
		{
			// calculate new state estimate
			GVector ax = new GVector(x.getSize());
			ax.mul(a, x);
			GVector by = new GVector(x.getSize());
			by.mul(b, y);
			x.add(ax, by);
			
			// generate control values
			GVector cx = new GVector(c.getNumRow());
			cx.mul(c, x);
			GVector dy = new GVector(d.getNumRow());
			dy.mul(d, y);
			GVector u = new GVector(c.getNumRow());
			u.add(cx, dy);
			
			return u;
		}
	}
	
	@Inject(optional = true)
	public final void setControlMatrixDirectory(@Named("control-matrix-directory") final String dir)
	{
		this.controlMatrixDirectory = dir;
	}
	
	@Inject(optional = true)
    public final void setThrottleTrim(@Named("trim-throttle") final int trim) {
        this.throttleTrim = trim;
    }


    @Inject(optional = true)
    public final void setYawTrim(@Named("trim-yaw") final int trim) {
        this.yawTrim = trim;
    }


    @Inject(optional = true)
    public final void setRollTrim(@Named("trim-roll") final int trim) {
        this.rollTrim = trim;
    }

    @Inject(optional = true)
    public final void setPitchTrim(@Named("trim-pitch") final int trim) {
        this.pitchTrim = trim;
    }
    
    @Inject(optional = true)
    public final void setLogging(@Named("logging") final boolean logData) {
        this.logData = logData;
    }
    
    @Inject(optional = true)
    public final void setLogPath(@Named("log-path") final String logPath) {
        this.logPath = logPath;
    }
}
