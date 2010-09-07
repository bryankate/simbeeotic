package harvard.robobees.simbeeotic.model.sensor;


import javax.vecmath.Vector3d;
import javax.vecmath.Vector2d;


/**
 * An interface for an optical flow ring.
 *
 * @author Lucia Mocz
 */
public interface OpticalMotionSensor {

    /**
     * Finds the scaled motion in world coordinates of the bee through
     * determining the relative motion of objects around it in given
     * directions per instantaneous units of time.
     * <p/>
     * Most important function: does all the optical flow calculations, need to
     * invoke it first before invoking the other functions :)
     */
	public void getMotion(Vector3d currVel);
	
	//returns position of each point on the image
	public Vector2d[][] getPos();
	
	//returns OF vectors' length on the image (use with prev to create a map
	//and/or visualize optical flow)
	public Vector2d[][] getVis();
	
	//returns pointing view of the camera
	public Vector3d[][] getPoint();
	
	//returns size of OF sensor
	public int getAREA();
	
	//returns minimum distance in image
	public double minDist();
	
	//returns the depth of image
	public double[][] getDepth();
	
	//returns the average distance of objects in image
	public double aveDist();
	
	//returns whether the minDist is left of the midpoint
	public boolean isLeft();
	
	//returns whether the minDist is right of the midpoint
    public boolean isRight();
    
    //returns whether the minDist is at the midpoint--possibly a rare case, and 
    //should be tested against when using this for things like saccading motion
    public boolean isMid();
}
