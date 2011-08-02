package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.sensor.PoseSensor;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.util.PIDController;

import com.bulletphysics.linearmath.Transform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.vecmath.GMatrix;
import javax.vecmath.GVector;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Behavior for testbed helicopters implementing
 * Linear-Quadratic-Integral control of six variables:
 *     u -- body-frame x velocity
 *     v -- body-frame y velocity
 *     w -- body-frame z velocity
 *     p -- angular velocity of roll
 *     q -- angular velocity of pitch
 *     r -- angular velocity of yaw
 * 
 * @author davidpalmer
 */
public abstract class LQIHeliBehavior implements HeliBehavior
{
	// sensors
	private PositionSensor s1;
	private PoseSensor s2;
	
	// trim values
    protected int throttleTrim = 400;
    protected int rollTrim = 400;
    protected int pitchTrim = 400;
    protected int yawTrim = 400;
	
	// world-frame state
	private Vector3f lastPos;
	private Quat4f lastPose;
	private long lastTime;
	
	// control
	private String controlMatrixDirectory;
	private Timer controlTimer;
	private GVector reference;
	private LQIController lqi;
	
	// logging
	private static Logger logger = Logger.getLogger(LQIHeliBehavior.class);
	private boolean logData = false;
	private String logPath = "heli_data.txt";
	private FileWriter logFW;
    private BufferedWriter logBW;
    private String more = ""; // extra data to be logged
	
	private GMatrix loadCSVMatrix(File file) throws IOException
	{
		double[] matrix = new double[324];
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
	    		logBW.write(dt + " 0 " + u.getElement(0) + " " + u.getElement(1) +
	    				  " " + u.getElement(2) + " " + u.getElement(3) + " " +
	    				  reference.getElement(0) + " " + reference.getElement(1) + " " + reference.getElement(2) + " " +
	    				  reference.getElement(3) + " " + reference.getElement(4) + " " + reference.getElement(5) + " " +
	    				  y.getElement(0) + " " + y.getElement(1) + " " + y.getElement(2) + " " +
	    				  y.getElement(3) + " " + y.getElement(4) + " " + y.getElement(5) + " " +
	    				  lastPos.x + " " + lastPos.y + " " + lastPos.z + " " + more + "\n");
	    	}
	    	catch (IOException e)
	    	{
	    		// do nothing
	    	}
    	}
	}
	
	@Override
	public void start(final Platform platform, final HeliControl control)
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
			GMatrix lqiA = loadCSVMatrix(new File(controlMatrixDirectory, "LQI_A"));
			GMatrix lqiB = loadCSVMatrix(new File(controlMatrixDirectory, "LQI_B"));
			GMatrix lqiC = loadCSVMatrix(new File(controlMatrixDirectory, "LQI_C"));
			GMatrix lqiD = loadCSVMatrix(new File(controlMatrixDirectory, "LQI_D"));
			GMatrix kfC = loadCSVMatrix(new File(controlMatrixDirectory, "KF_C"));
			GMatrix kfD = loadCSVMatrix(new File(controlMatrixDirectory, "KF_D"));
			
			// check matrix dimensions
			assert lqiB.getNumCol() == 12;
			assert lqiC.getNumRow() == 4;
			assert lqiD.getNumRow() == 4;
			assert lqiD.getNumCol() == 12;
			assert kfC.getNumRow() == 6;
			assert kfD.getNumRow() == 6;
			assert kfD.getNumCol() == 6;
			
			// initialize controller and reference vector
			lqi = new LQIController(lqiA, lqiB, lqiC, lqiD, kfC, kfD);
			reference = new GVector(lqiB.getNumCol() / 2);
		}
		
		catch (Exception e)
		{
			logger.info("Failed to load control matrices. Ending simulation...");
			System.exit(1);
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
            	
            	// update saved values
            	lastTime = System.currentTimeMillis();
            	lastPos = s1.getPosition();
        		lastPose = s2.getPose();
        		
        		// calculate controls
        		double[] y_array = {uvw.x, uvw.y, uvw.z, pqr.x, pqr.y, pqr.z};
        		GVector y = new GVector(y_array);
        		GVector u = lqi.calculateControls(time.getTime(), reference, y);
        		
        		// log reference, measurements, controls
            	logger.info(reference + "\n" + y + "\n" + u);
        		
        		// record data
        		record(dt, reference, y, u);
        		
        		// implement controls
        		control.setThrust(HWILBee.normCommand(Math.round(u.getElement(0) + throttleTrim)));
        		control.setRoll(HWILBee.normCommand(Math.round(u.getElement(1) + rollTrim)));
        		control.setPitch(HWILBee.normCommand(Math.round(u.getElement(2) + pitchTrim)));
        		control.setYaw(HWILBee.normCommand(Math.round(u.getElement(3) + yawTrim)));
            }
        }, 2, TimeUnit.SECONDS, 20, TimeUnit.MILLISECONDS);
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
    	// correct for division by zero
		if (dt == 0)
			dt = 0.020f;
		
		Vector3f vel = new Vector3f();
    	vel.sub(s1.getPosition(), lastPos);
    	vel.scale(1/dt);
    	float z = vel.z;
    	
    	Quat4f pose = s2.getPose();
    	pose.conjugate();
    	
    	Transform orient = new Transform();
    	orient.setIdentity();
    	orient.setRotation(pose);
    	orient.transform(vel);
    	
    	vel.z = z;
    	
    	return vel;
	}
	
	protected Vector3f getPQR(float dt)
	{
		// correct for division by zero
		if (dt == 0)
			dt = 0.020f;
		
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
	
	private class LQIController
	{
		// gain matrices
		private GMatrix a;
		private GMatrix b;
		private GMatrix c;
		private GMatrix d;
		private GMatrix kfC;
		private GMatrix kfD;
		
		// secret PID yaw controller
		private PIDController rPID;
		
		// state space model + integrator
		private GVector x;
		
		// observation estimate
		private GVector y_hat;
		
		public LQIController(GMatrix a, GMatrix b, GMatrix c, GMatrix d, GMatrix kfC, GMatrix kfD)
		{
			this.a = a;
			this.b = b;
			this.c = c;
			this.d = d;
			this.kfC = kfC;
			this.kfD = kfD;
			this.x = new GVector(a.getNumRow());
			this.y_hat = new GVector(kfC.getNumRow());
			this.rPID = new PIDController(0, 5, 0, 0);
		}
		
		/*
		 * calculate control inputs given setpoint r and observed state y
		 */
		public GVector calculateControls(long t, GVector r, GVector y)
		{
			// compose error vector
			GVector ry = new GVector(r);
			ry.setSize(r.getSize() + y.getSize());
			for (int i = 0; i < y.getSize(); i++)
				ry.setElement(r.getSize() + i, y.getElement(i));
			
			
			// calculate new state estimate
			GVector ax = new GVector(x.getSize());
			ax.mul(a, x);
			GVector by = new GVector(x.getSize());
			by.mul(b, ry);
			x.add(ax, by);
			
			// record state estimate
			appendLoggingData(x.toString());
			
			// estimate observation vector
			GVector x_hat = new GVector(x);
			x_hat.setSize(kfC.getNumCol()); // excise integrator from state estimate
			GVector kfCx = new GVector(y_hat.getSize());
			kfCx.mul(kfC, x_hat);
			GVector kfDy = new GVector(y_hat.getSize());
			kfDy.mul(kfD, y);
			y_hat.add(kfCx, kfDy);
			
			// run PID on yaw Kalman estimate
			Double u_yaw = rPID.update(t, y_hat.getElement(y_hat.getSize() - 1) - r.getElement(r.getSize() - 1));
			
			if (u_yaw == null)
				u_yaw = 0.0;
			
			// generate control values
			GVector cx = new GVector(c.getNumRow());
			cx.mul(c, x);
			GVector dy = new GVector(d.getNumRow());
			dy.mul(d, ry);
			GVector u = new GVector(c.getNumRow());
			u.add(cx, dy);
			
			// replace calculated yaw control with PID yaw control
			//u.setElement(u.getSize() - 1, u_yaw);
			
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
