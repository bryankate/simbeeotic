package harvard.robobees.simbeeotic.example;

import java.io.*;

import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.sensor.OpticalMotionSensor;
import harvard.robobees.simbeeotic.environment.PhysicalConstants;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector3d;

import org.apache.log4j.Logger;



/**
 * AutoBee uses the CenteyeMotionSensor and has 3 DOF motion: thrust from back, rotation about
 * z-axis (left/right rotation), and thrust from bottom.  The idea is to model motion similarly to
 * a helicopter's motion, and also to make the bee navigate through a maze using optical flow.
 * Note that this bee is very computationally EXPENSIVE: we will be observing OF at each point in
 * the image(!!) and find the distance of every individual point(!!) in the image(!!).
 * 
 * @author Lucia Mocz, the master of flying bees XD
 *
 */

public class OpticalMotionBee extends SimpleBee {

	//values to be changed/updated by user:
	private static int AREA;
	private double desiredHeight = 8; //used for take-off
	private double desiredDist = 3; //used for left-hand rule
	
	//matlab files
	private static final String VIS_FILE = "cor";
	private static final String MAP_FILE = "m"+VIS_FILE;
	/* STATISTICS
	 * the files will be named by the following:
	 * -t/p for time or position
	 * -specificity of OF (AREA)
	 * -distance of walls (d pos)
	 * -initial position of bee (p pos)
	 * -maxVel of bee (v vel)
	 * 
	 * example of a file: t3d10p5v2
	 * 
	 * the plain series is the test of OF sensitivity: manipulated variable
	 * is position away and we will find the most optimum OF sensor in terms of
	 * --time to stabilization
	 * --distance until stabilization
	 * --error once stabilization is reached
	 * (note that max-speed here is 2 m/s)
	 * 
	 * the A series is the test of walls: we set the OF sensor to be the optimum one
	 * found in the test above, then set the displacement to be 2, and change the distance
	 * of the walls :)
	 * 
	 * --NOTE: naming scheme is same as above except with A after it it!!
	 * 
	 * in the B series, we find how the speed of the bee affects the centering: 
	 * get the most optimum wall/OF sensor output, and then change the max-speed of the
	 * bee and find how sensitive it is (note that we made epsilons to be affected by the
	 * speed, so manually altering them is not an issue: we can also keep track of how 
	 * high the mathematical error is for these epsilons!)
	 * 
	 * in the C series we set the bee to the best of the above three tests, and add noise
	 * to the reading and determine at what point the noise will ruin the bee's navigation
	 */
	
	private static double maxVel = 2; //only forward direction
	private static final String NAME = 5+"d"+12+"p"+0+"v"+1+"B";
	private static final String TIME_FILE = "t"+NAME;
	private static final String POS_FILE = "p"+NAME;
	
	//OF sensor input values
	private OpticalMotionSensor s0, s1, s2, s3, s4, s5, s6, s7;
	private Vector3d odoDist = new Vector3d();
	private Vector3d odoDisp = new Vector3d();
	//odometry readings, distance and displacement
	
	//necessary values to interpret OF
	private static double EPSILON = 0;
	private static double SLOW = maxVel/2; //another epsilon, used for slowing the vehicle
	private static final float TIME = 0.1f;
	private static final double RATIO = 0.5; //OF Ratio--can alter based on desires of env/speed
	private static double maxAccel = 1;
	private double mass;
	private static boolean slowTurn = false;
	private static boolean slowObst = false;
	private static boolean slowLeft = false;
	private static boolean slowRight = false;
	private static boolean saccade = false;
	private static boolean cornerLeft = false;
	private static boolean cornerRight = false;
	
	//updated intermediate outputs
	private Vector3d currVel = new Vector3d();
	private Vector3d prevVel = new Vector3d();
	private Vector3d prevAcc = new Vector3d();
	private Vector3f prevPos = new Vector3f();
	private Vector3d displacement = new Vector3d();
	private OpticalMotionSensor sLeft, sRight; //used for wall-following, gets altered based on rotate
	private double rotate = 0; //current angle rotation of bee from having 2/6 be the real l/r sensors
	
	
    private static Logger logger = Logger.getLogger(OpticalMotionBee.class);

    @Override
    public void initialize() {
        super.initialize();
        s0 = getSensor("motion0", OpticalMotionSensor.class);
        s1 = getSensor("motion1", OpticalMotionSensor.class);
        s2 = getSensor("motion2", OpticalMotionSensor.class);
        s3 = getSensor("motion3", OpticalMotionSensor.class);
        s4 = getSensor("motion4", OpticalMotionSensor.class);
        s5 = getSensor("motion5", OpticalMotionSensor.class);
        s6 = getSensor("motion6", OpticalMotionSensor.class);
        s7 = getSensor("motion7", OpticalMotionSensor.class);
        
        sLeft = s2;
        sRight = s6;

        AREA = s0.getAREA();
        mass = getMass();
        //displacement = new Vector3d(getStartPosition());
        //setHovering(true);
    }

    @Override
    protected void updateKinematics(SimTime time) {
    	
    	clearForces();
    	
        Vector3f pos = getTruthPosition();
        Vector3f vel = getTruthLinearVelocity();
        Quat4d orient = new Quat4d(getTruthOrientation());
        
        //use accelerometer to find current velocity:
        if (time.getImpreciseTime() == 0) {return;}
        else {currVel = findCurrVel();}
        
        updateEPSILON();
        updateSLOW();
        
        //find the distances of each pixel in the image
        s0.getMotion(currVel);
        s1.getMotion(currVel);
        s2.getMotion(currVel);
        s3.getMotion(currVel);
        s4.getMotion(currVel);
        s5.getMotion(currVel);
        s6.getMotion(currVel);
        s7.getMotion(currVel);
        
        sLeft.getMotion(currVel);
        sRight.getMotion(currVel);
        
        //make the bee turn around when it is too near to an object in the front
        obstacleAvoid();
        
        //make the bee turn away from high concentrations of flow vectors--essentially
        //we make it be able to follow walls
        wallCenter();
        
        hover2(desiredHeight);
        
    	//make the bee hover
        if (time.getImpreciseTime() == 0) {return;}
        else {
        	hover();
        }
        
        //give the bee a constant forward speed, adjust based on distances of objects
    	//which are on the side--essentially slow down when road is thin, speed up when
    	//road is fast :)
    	double desiredVel = getDesiredVel();
    	Vector3d forward = new Vector3d(desiredVel,0,0);
    	cruiseControl(forward);
        
        //odometry--track the movement of the bee using OF
        //odometry();
    	
    	//visualize flow:
        /*Vector2d lPos[][] = sLeft.getPos();
        Vector2d lVis[][] = sLeft.getVis();
        Vector2d rPos[][] = sRight.getPos();
        Vector2d rVis[][] = sRight.getVis();
        Vector2d fPos[][] = s0.getPos();
        Vector2d fVis[][] = s0.getVis();
        
        int count = (int)(time.getImpreciseTime()*10);
        
        visFlow(lPos,lVis,"L",count);
        visFlow(rPos,rVis,"R",count);
        visFlow(fPos,fVis,"F",count);*/
    	
  
    	//timeDat("C:\\Users\\Lucia\\Documents\\MATLAB\\"+TIME_FILE+".dat",time.getImpreciseTime());
        //posDat("C:\\Users\\Lucia\\Documents\\MATLAB\\"+POS_FILE+".dat");
    	
        //update previous readings
    	prevVel = currVel;
    	prevPos = pos;
        
        logger.info("ID: " + getModelId() + "  " +
                "time: " + time.getImpreciseTime() + "  " +
                "pos: " + pos + "  " +
                "vel: " + vel);
    }

    @Override
    public void finish() {
    	//errorMat("C:\\Users\\Lucia\\Documents\\MATLAB\\"+ERROR_FILE+"Prog.m",ERROR_FILE);
    }
    
    //simple integration formula to find current velocity of the object
    //NOTE that this may possibly be expanded once we get more familar with OF and
    //air disturbances, etc.
    private Vector3d findCurrVel() {
    	Vector3d currVel = new Vector3d();
    	
    	//determine controlled acceleration
    	Vector3d acc = new Vector3d(0,0,PhysicalConstants.EARTH_GRAVITY);
    	acc.add(prevAcc);
    	
    	//eq. of motion formula
    	acc.scale(TIME);
    	currVel.add(prevVel,acc);
    	
    	return currVel;
    }
    
    //updates epsilon based on the speed of the bee, i.e., if the bee goes faster, we need
    //a greater leeway in error--this is mathematically accurate within the greatest bounds
    private void updateEPSILON() {
    	Vector3d latVel = new Vector3d(currVel.x,currVel.y,0);
    	double eps = latVel.length() / 10;
    	EPSILON = eps;
    	if (EPSILON==0) {EPSILON = 0.0001;}
    	else return;
    }
    
    private void updateSLOW() {
    	Vector3d imp = new Vector3d();
    	Vector3d init = new Vector3d(currVel);
    	double dist = 0;
    	double fin = Double.POSITIVE_INFINITY;
    	
    	while (fin > 0.0000000001) {
    		imp.negate(init);
        	
        	if (imp.length()>maxAccel) {
        		imp.normalize();
        		imp.scale(maxAccel);
        	}
        	
        	fin = init.x + imp.x;
        	double move = TIME/2 * (init.x + fin);
        	dist+=move;
        	
        	init = new Vector3d(fin,0,-init.z); //best hack i could think of, not sure
        	//how else to account for vertical motion--might fix this wrt the actual thing, 
        	//normalizing only the forward distance (although last time this failed)
    	}
    	
    	SLOW = dist;
    }
    
    //this hover function is based on the distance the bee is from the ground based on a downward-
    //pointing OF sensor
    private void hover2(double desiredHeight) {
    	//TODO: MAKE THIS!!
    	//IDEA: measure distance, adjust based on that, always add on counteractive force of
    	//gravity
    	
    	//actual height as determined by OF
    	double actualHeight = s4.aveDist();
    	System.out.println("actualHeight: "+actualHeight);
    	double foo = getTruthPosition().z;
    	System.out.println("foo: "+foo);
    	
    	System.out.println("currVel: "+currVel);
    	
    	//displacement from desired height:
    	double disp = desiredHeight-actualHeight; //error of where we should be at....
    	
    	double acc = 2/TIME * (disp/TIME - currVel.z); //determine accel based on disp, time, and vel.
    	     //- PhysicalConstants.EARTH_GRAVITY; //add on earth's grav.
    	
    	System.out.println("acc: "+acc);
    	
    	Vector3d upAcc = new Vector3d(0,0,acc);
        
    	//update the prev values
    	//prevAcc = upAcc;
    	
    	Vector3f force = new Vector3f(0,0,(float)(upAcc.z*mass));
    }
    
    //cheating hover function just in case the above one does not work :(
    private void hover() {
    	//determine the position displacement wrt starting position:
		Vector3d pos = new Vector3d();
		pos.add(prevVel,currVel);
		pos.scale(TIME/2);
		pos.negate(); //pos to fix!
		
		displacement.add(pos); //total displacement from original position
		//measured only based on invocation of hover (like an internal clock)

    	//calculate the new acceleration of the object in the next 0.1 sec
    	Vector3d startVel = new Vector3d(currVel);
    	Vector3d newAccel = new Vector3d(displacement);
    	startVel.scale(TIME);
    	newAccel.sub(startVel);
    	newAccel.scale(2/(TIME*TIME));
    		
    	//determine force to apply to bee to make it hover
    	Vector3f force = new Vector3f(0,0,(float)(newAccel.z*mass));
    	applyForce(force);
    		
    	//update all the old values with new ones!
    	Vector3d upAcc = new Vector3d(0,0,newAccel.z);
        prevAcc = upAcc;
    }
    
    //adjust for constant velocity: input is the desired forward vel
    private void cruiseControl(Vector3d desiredVel) {
    	Vector3d impulse = new Vector3d(desiredVel);
    	
    	impulse.sub(currVel);
    	
    	if (impulse.length() > maxAccel) {
    		impulse.normalize();
    		impulse.scale(maxAccel);
    	}
    	
    	Vector3d forwardAcc = new Vector3d(impulse.x,0,0);
    	forwardAcc.scale(1/TIME);
    	
    	impulse.scale(mass);
    	
    	Vector3d forImp = new Vector3d((float)impulse.x,0,0);
    	
    	//rotate impulse the direction the bee is traveling in
    	Vector3d linImp = rotate(rotate,forImp);
    	linImp.scale(forImp.length());
    	
    	Vector3f cruiseImp = new Vector3f(linImp);
    	applyImpulse(cruiseImp);

    	//update the acceleration constant
    	prevAcc.add(forwardAcc);
    }
    
    //finds desired forward vel based on distance: keep a constant ratio of
    //vel to distance
    private double getDesiredVel() {
    	double desiredVel;

    	if (slowTurn || slowObst) {
    		desiredVel = 0;
    		//System.out.println("desiredVel: "+desiredVel);
        	return desiredVel;
    	}
    	
    	double left = sLeft.minDist();
    	double right = sRight.minDist();
    	double min;
    	
    	//find minimum to one side--we want to make sure it won't crash in
    	//to anything on either side
    	if (left<right) {min=left;}
    	else {min=right;}
    	
    	desiredVel = RATIO*min; //keep OF ratio constant
    	if (desiredVel>maxVel) {desiredVel = maxVel;} //bee can only go a max vel
    	
    	return desiredVel;
    }
    
    //follows the contour of the wall and centers bee
    //some things to note: as it approaches a turn, it slows down to practically a stop--when it
    //stops, it makes a turn :)
    private void wallCenter() {
    	//NOTE: rotate used in this tracks the total rotation according to the initial
    	//orientation of the bee--in no way will it 
    	double error = sLeft.aveDist()-sRight.aveDist();
    	double len = currVel.length();
    	
    	//System.out.println("left: "+sLeft.aveDist());
    	//System.out.println("right: "+sRight.aveDist());
    	//System.out.println("error: "+error);
    	//System.out.println("EPSILON: "+EPSILON);
    	//System.out.println("SLOW: "+SLOW);
    	
    	//additional two states
    	if (slowLeft) {
    		if (currVel.x<0.0000000001) {
    			double turn = Math.PI/4;
        		turn((float)turn);
        		rotate = rotate + turn;
        		sLeft = s1;
        		sRight = s5;
        		currVel = rotate(turn,currVel);
        		currVel.scale(len);
        		//System.out.println("turn left");
        		slowTurn = false;
        		slowLeft = false;
    		}
    		return;
    	}
    	
    	if (slowRight) {
    		if (currVel.x<0.0000000001) {
    			double turn = -Math.PI/4;
        		turn((float)turn);
        		rotate = rotate + turn;
        		sLeft = s3;
        		sRight = s7;
        		currVel = rotate(Math.PI/4,currVel);
        		currVel.scale(len);
        		//System.out.println("turn right");
        		slowTurn = false;
        		slowRight = false;
    		}
    		return;
    	}
    	
    	if (sLeft.equals(s2) && sRight.equals(s6)) {
    		if (error>EPSILON) {
        		//turn 45 degrees left
        		slowTurn = true;
        		slowLeft = true;
        		//System.out.println("slow for left");
        	}
        	else if (-error>EPSILON) {
        		//turn 45 degrees right
        		slowTurn = true;
        		slowRight = true;
        		//System.out.println("slow for right");
        	}
        	else {
        		//System.out.println("center");
        		return; //otherwise just go on yo' course!
        	}
    	}
    	else if (sLeft.equals(s1) && sRight.equals(s5)) {
    		if (currVel.x<0.0000000001) {
    			//turn back to normal--find rotation offset from current
    			double turn = -Math.PI/4;
    			turn((float)turn);
    			rotate = rotate + turn;
    			sLeft = s2;
    			sRight = s6;
    			currVel = rotate(Math.PI/4,currVel);
    			currVel.scale(len);
    			//System.out.println("turn right back to normal");
    			if (error < 0) {EPSILON = -error;}
    			else {EPSILON = error;}
    			slowTurn = false;
    		}
    		else {
    	    	if (error < SLOW) {
    	    		slowTurn = true;
    	    	}
    			//System.out.println("moving left");
    			return;
    		}
    	}
    	else if (sLeft.equals(s3) && sRight.equals(s7)) {
    		if (currVel.x<0.0000000001) {
    			//turn back to normal--find rotation offset from current    			
    			double turn = Math.PI/4;
    			turn((float)turn);
    			rotate = rotate + turn;
    			sLeft = s2;
    			sRight = s6;
    			currVel = rotate(-Math.PI/4,currVel);
    			currVel.scale(len);
    			//System.out.println("turn left back to normal");
    			if (error < 0) {EPSILON = -error;}
    			else {EPSILON = error;}
    			slowTurn = false;
    		}
    		else {
    			if (-error < SLOW) {
    	    		slowTurn = true;
    	    	}
    			//System.out.println("moving right");
    			return;
    		}
    	}
    	else {
    		//System.out.println("IT SHOULD NEVER PRINT THIS!!!");
    	}
    }
 
    //use only when traveling forward, i.e., when s2/s6 are left/right (?? maybe not ??)
    private void obstacleAvoid() {
    	//System.out.println("s0.minDist: "+s0.minDist());
    	//System.out.println("s0.aveDist: "+s0.aveDist());

    	double diff = s0.aveDist() - s0.minDist();
    	
    	if (saccade) {
    		
    		if (currVel.x<0.000001) {
    			double turn;
    			//System.out.println("sLeft.aveDist(): "+sLeft.aveDist());
    			//System.out.println("sRight.aveDist(): "+sRight.aveDist());
    			//System.out.println("isLeft: "+s0.isLeft());
    			//System.out.println("isRight: "+s0.isRight());
    			
    			if (slowRight) {
    				turn = -Math.PI/2; //turn 180 degrees around....
    				//System.out.println("~~S2");
    				slowRight = false;
    			}
    			else {
    				turn = Math.PI/2; //turn 180 degrees around....
    				//System.out.println("~~S1");
    				slowLeft = false;
    			}
    			
        		turn((float)turn);
        		rotate = rotate + turn;
        		double len = currVel.length();
        		currVel = rotate(turn,currVel);
        		currVel.scale(len);
        		slowObst = false;
        		saccade = false;
        		//System.out.println("!!SACCADE");
        		//System.out.println("min: "+s0.minDist());
        		//System.out.println("ave: "+s0.aveDist());
        		
        		//TODO: END IT AT THE END OF THE SIMS!!!
        		//timeMat("C:\\Users\\Lucia\\Documents\\MATLAB\\"+TIME_FILE+"Prog.m",TIME_FILE);
            	//posMat("C:\\Users\\Lucia\\Documents\\MATLAB\\"+POS_FILE+"Prog.m",POS_FILE);
        		//getSimEngine().requestScenarioTermination();
    		}
    		else {
    			//System.out.println("slowSaccade");
    		}
    		//System.out.println("slow: "+slowObst);
    		return;
    	}
    	
    	if (cornerLeft) {
    		
    		if (diff < 0.1) {
        		saccade = true;
        		return;
        	}
    		if (currVel.x<0.000001) {
    			double turn = Math.PI/4; //turn 45 degrees away, parallel to object
    			turn((float)turn);
    		    rotate = rotate + turn;
    		    //reset walls
    		    sLeft = s2;
    			sRight = s6;
    		    double len = currVel.length();
    		    currVel = rotate(turn,currVel);
    		    currVel.scale(len);
    		    slowObst = false;
    		    cornerLeft = false;
    		    //System.out.println("!!CORNER: left");
    		}
    		else {
    			//System.out.println("slowCornerLeft");
    		}
    		//System.out.println("slow: "+slowObst);
    		return;
    	}
    	
        if (cornerRight) {
        	
        	if (diff < 0.1) {
        		saccade = true;
        		return;
        	}
			if (currVel.x<0.000001) {
				double turn = -Math.PI/4; //turn 45 degrees away, parallel to object
    		    turn((float)turn);
    		    rotate = rotate + turn;
    		    //reset walls
    		    sLeft = s2;
    			sRight = s6;
    		    double len = currVel.length();
    		    currVel = rotate(turn,currVel);
    		    currVel.scale(len);
    		    slowObst = false;
    		    cornerRight = false;
    		    //System.out.println("!!CORNER: right");
			}
			else {
				//System.out.println("slowCornerRight");
			}
			//System.out.println("slow: "+slowObst);
			return;
        }
        
        //main point of making the decision
        if (s0.aveDist() < 2) { //if we are within 2 m of the wall
    		slowObst = true;
    		
    		//make decisions now....
    		if (diff < 0.1) {saccade = true;} 
    		else if (s0.isLeft() || (sLeft.equals(s1)&&sRight.equals(s5))) {cornerLeft = true;}
    		else if (s0.isRight() || (sLeft.equals(s3)&&sRight.equals(s7))) {cornerRight = true;}
    		else if (s0.isMid()) {slowObst = false;}
    		//System.out.println("slow: "+slowObst);
    	}
    }
    
    //code to land--keep optical flow constant, i.e., angle of declination is 45 degrees
    //and the ratio of speed/distance must be the same!
    
    //odometry: gives total distance traveled by bee
    private void odometry() {
    	Vector3d distance = new Vector3d();
    	distance.add(prevVel,currVel);
    	distance.scale(TIME/2);
    	
    	//rotate the vector by rotate angle:
    	distance = rotate(rotate,distance);
    	
    	odoDisp.add(distance); //total displacement from start pos
    	distance.absolute();
    	odoDist.add(distance); //total distance from start pos
    	
    	mapRoute(); //maps route in a matlab file (optional)
    }
    
    //rotates the forward velocity vectors to the direction that the bee
    //is currently moving in:
    private Vector3d rotate(double rotate, Vector3d in) {
    	//hackish solution, but essentially if the in vector is
    	//the zero vector, then don't do anything....
    	if (in.x==0) { return in; }
    	
    	double z = Math.sin(rotate/2);
    	double w = Math.cos(rotate/2);

    	Quat4d left = new Quat4d(0,0,z,w);
    	Quat4d right = new Quat4d(left);
    	right.conjugate();
    	
    	Quat4d point = new Quat4d(in.x,in.y,in.z,0);
    	left.mul(point);
    	left.mul(right);
    	
    	Vector3d out = new Vector3d(left.x,left.y,left.z);
    	
    	return out;
    }
    
    /*MATLAB visualization code below*/
    
    //takes in odometry reading and uses it to create a map of its travels
    //essentially this just returns a matlab map of where the bee went
    private void mapRoute() {
    	mapDat("C:\\Users\\Lucia\\Documents\\MATLAB\\"+MAP_FILE+".dat");
    	mapMat("C:\\Users\\Lucia\\Documents\\MATLAB\\"+MAP_FILE+"Prog.m",MAP_FILE);
    }
    
    //data file for map of displacement
    private void mapDat(String filename) {
    	Vector3d actDisp = new Vector3d(getTruthPosition());
    	Vector3d start = new Vector3d(getStartPosition());
    	actDisp.sub(start);
    	
    	try {
    		BufferedWriter out = new BufferedWriter(new FileWriter(filename,true));
    		out.write(odoDisp.x+" "+odoDisp.y+" "+odoDisp.z
    				+" "+actDisp.x+" "+actDisp.y+" "+actDisp.z);
    		out.newLine();
    		out.close();
    	}
    	catch (IOException e) {
    	}
    }
    
    //mat file for map of displacement
    private void mapMat(String filename,String dat) {
    	try {
    		PrintWriter out = new PrintWriter(new File(filename));
    		out.println("load "+dat+".dat;");
    		out.println("x = "+dat+"(:,1);");
    		out.println("y = "+dat+"(:,2);");
    		out.println("z = "+dat+"(:,3);");
    		out.println("a = "+dat+"(:,4);");
    		out.println("b = "+dat+"(:,5);");
    		out.println("c = "+dat+"(:,6);");
    		out.println("plot3(x,y,z,'o',a,b,c,'o');");
    		out.println("xlabel('x');");
    		out.println("ylabel('y');");
    		out.println("zlabel('z');");
    		out.close();
    	}
    	catch (IOException e) {
    	}
    }
    
    private void timeDat(String filename,double time) {
    	Vector3d actDisp = new Vector3d(getTruthPosition());
    	
    	try {
    		BufferedWriter out = new BufferedWriter(new FileWriter(filename,true));
    		out.write(time+" "+actDisp.y);
    		out.newLine();
    		out.close();
    	}
    	catch (IOException e) {
    	}
    }
    
    private void timeMat(String filename,String dat) {
    	try {
    		PrintWriter out = new PrintWriter(new File(filename));
    		out.println("load "+dat+".dat;");
    		out.println("x = "+dat+"(:,1);");
    		out.println("y = "+dat+"(:,2);");
    		out.println("plot(x,y);");
    		out.println("xlabel('TIME');");
    		out.println("ylabel('POSITION');");
    		out.close();
    	}
    	catch (IOException e) {
    	}
    }
    
    private void posDat(String filename) {
    	Vector3d actDisp = new Vector3d(getTruthPosition());
    	Vector3d start = new Vector3d(getStartPosition().x,0,0);
    	actDisp.sub(start);
    	
    	try {
    		BufferedWriter out = new BufferedWriter(new FileWriter(filename,true));
    		out.write(actDisp.x+" "+actDisp.y);
    		out.newLine();
    		out.close();
    	}
    	catch (IOException e) {
    	}
    }
    
    private void posMat(String filename,String dat) {
    	try {
    		PrintWriter out = new PrintWriter(new File(filename));
    		out.println("load "+dat+".dat;");
    		out.println("x = "+dat+"(:,1);");
    		out.println("y = "+dat+"(:,2);");
    		out.println("plot(x,y);");
    		out.println("xlabel('DISTANCE');");
    		out.println("ylabel('POSITION');");
    		out.close();
    	}
    	catch (IOException e) {
    	}
    }
    
    //creates matlab code for visualization
    private void visFlow(Vector2d[][] pos, Vector2d[][] vis, String orient, int count) {
    	visDat(pos,vis,"C:\\Users\\Lucia\\Documents\\MATLAB\\"+VIS_FILE+orient+count+".dat");
    	visMat("C:\\Users\\Lucia\\Documents\\MATLAB\\"+VIS_FILE+orient+count+"Prog.m",VIS_FILE+orient+count);
    }
    
    //creates dat file
    public void visDat(Vector2d[][] pos, Vector2d[][] vis, String filename) {
    	try {
    		PrintWriter out = new PrintWriter(new File(filename));
    		for (int i=0;i<AREA;i++) {
        		for (int j=0;j<AREA;j++) {
        			out.println(pos[i][j].x+" "+pos[i][j].y+" "+vis[i][j].x+" "+vis[i][j].y+" ");
        		}
        	}
    		out.close();
    	}
    	catch (IOException e) {
    	}
    }
    
    //creates mat file
    public void visMat(String filename,String dat) {
    	try {
    		PrintWriter out = new PrintWriter(new File(filename));
    		out.println("load "+dat+".dat");
    		out.println("x = "+dat+"(:,1);");
    		out.println("y = "+dat+"(:,2);");
    		out.println("u = "+dat+"(:,3);");
    		out.println("v = "+dat+"(:,4);");
    		out.println("xmin = min(x);");
    		out.println("xmax = max(x);");
    		out.println("ymin = min(y);");
    		out.println("ymax = max(y);");
    		//out.println("daspect([1,1])");
    		out.println("xrange = linspace(xmin,xmax,8);");
    		out.println("yrange = linspace(ymin,ymax,8);");
    		out.println("q=quiver(x,y,u,v);");
    		//optional things to improve viewability
    		//out.println("grid off");
    		//out.println("axis([ xmin xmax ymin ymax ])");
    		//out.println("axis off");
    		out.close();
    	}
    	catch (IOException e) {
    	}
    }
}