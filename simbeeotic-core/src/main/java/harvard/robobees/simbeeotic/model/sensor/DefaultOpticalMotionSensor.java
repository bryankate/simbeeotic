/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package harvard.robobees.simbeeotic.model.sensor;

import com.google.inject.Inject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.Contact;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector2d;
import javax.vecmath.AxisAngle4d;

import com.bulletphysics.linearmath.Transform;


/**
 * This is an implementation of the {@link OpticalMotionSensor}
 * interprets every single pixel in the images, instead of essentially a weighted
 * average -- this will require a little more computation, but may be worth it in the long run :)
 *
 * @author Lucia Mocz
 *
 */

public class DefaultOpticalMotionSensor extends AbstractSensor implements OpticalMotionSensor {

	protected static final Vector3d UNIT_X = new Vector3d(1,0,0);
    protected static final Vector3d UNIT_Y = new Vector3d(0,1,0);
    protected static final Vector3d UNIT_Z = new Vector3d(0,0,1);
    
    private static final int AREA = 5; //number of points in one direction of viewing angle
    private static final double FREQUENCY = (Math.PI/3)/(AREA-1);
    private Vector2d[][] vis = new Vector2d[AREA][AREA];
    private Vector2d[][] pos = new Vector2d[AREA][AREA];
    private Vector3d[][] pointing = new Vector3d[AREA][AREA]; //used for debugging
    private double[][] depth = new double[AREA][AREA]; //individual depths
    private double range, xpos, ypos, xrot, xtras, yrot, ytras, xopt, yopt;
    private Vector3d vrot, vtras, point;
    private boolean isLeft;
    private boolean isMid;
    private boolean isRight;
    
    //USE GETMOTION FIRST TO GET THESE VALUES!!
    public Vector2d[][] getVis() {return vis;} //returns length of OF vector
    public Vector2d[][] getPos() {return pos;} //returns position of OF vector
    public Vector3d[][] getPoint() {return pointing;} //returns camera's viewfield
    public int getAREA() {return AREA;} //returns "pixels" in row of camera
    public double[][] getDepth() {return depth;} //returns depth of image
    public boolean isLeft() {return isLeft;}//returns whether the minDist is left of midpoint
    public boolean isRight() {return isRight;}//right of midpoint
    public boolean isMid() {return isMid;}//at midpoint
    
    //calculates the motion vectors for linear/rotational vels--need to invoke it before
    //determining the linear/rotational velocities we need to get!
    
    public void getMotion(Vector3d currVel) {
        
    	//reset these each time motion is called....
    	isLeft = false;
    	isRight = false;
    	isMid = false;
    	
    	//get velocity in world coords.
    	Vector3d vrotWorld = new Vector3d(getHost().getTruthAngularVelocity()); //world coords
    	Vector3d vtrasWorld = new Vector3d(getHost().getTruthLinearVelocity()); //world coords
    	Vector3d pointBee = new Vector3d(getPointing()); //bee coords
    	Quat4d orient = new Quat4d(getHost().getTruthOrientation());
    	Quat4d unorient = new Quat4d();
    	unorient.conjugate(orient);
    	
    	//put vectors into bee coordinates
    	Vector3d vrotBee = rotate(unorient,vrotWorld);
    	Vector3d vtrasBee = rotate(unorient,vtrasWorld);
    	vrotBee.scale(vrotWorld.length());
    	vtrasBee.scale(vtrasWorld.length());
    	
    	//set up the coordinate system of the camera: the center point is the z-direction
    	//we will then be able to rotate more easily along the x/y axes :D
    	Quat4d forw = setZ(pointBee);
    	Quat4d back = new Quat4d();
    	back.conjugate(forw);
    	Vector3d pointRot = rotate(forw,pointBee);
    	
    	//put everything into camera's coordinates:
    	vrot = rotate(forw,vrotBee);
    	vtras = rotate(forw,vtrasBee);
    	vrot.scale(vrotBee.length());
    	vtras.scale(vtrasBee.length());
    	
    	Vector3d vel = rotate(forw,currVel);
    	vel.scale(currVel.length());
    	
    	//get ranges, pointing, and optical flow on sphere in world coords.
    	for (int i=0; i<AREA; i++) { 
    		//first get horizontal line of rayTracers
    		//rotate Math.PI/6-FREQUENCY*i in the x-direction :)
    		Quat4d horiz = axisToQuat(UNIT_Y,Math.PI/6-FREQUENCY*i);
    		Vector3d horPoint = rotate(horiz,pointRot);

    		//now we want to rotate each of these points so we get the square:
    		
    		for (int j=0; j<AREA; j++) {
    			//rotate Math.PI/6-FREQUENCY*i in the y-direction :)
    			Quat4d verti = axisToQuat(UNIT_X,Math.PI/6-FREQUENCY*j);
    			Vector3d verPoint = rotate(verti,horPoint); //pointing in the optical flow frame
    			
    			//transform back to world coordinates--use conjugate of existing quats
    			//and reverse order of their multiplication
    			//use this to get the range to the nearest object
    			
    			Vector3d worldPoint = rotate(back,verPoint);
    			
    			//scalar quantity by which to multiply verPoint to get range vector
    			range = getRange(worldPoint);
    			range = check(range);
    			
    			point = new Vector3d();
    			point.scale(range,verPoint); //correct pointing/distance vector in opt coords
    			
    			pointing[i][j] = point;
    			
    			if (point.z<0) { point.z = point.z * -1;}
    			
    			//scale into the image plane, use vector in camera coordinates
    			xpos = point.x/point.z;
    			ypos = point.y/point.z;
    			
    			//calculate rotational component of optical flow
    			xrot = vrot.x*(xpos*ypos)+vrot.y*(-1*(xpos*xpos+1))+vrot.z*ypos;
    			yrot = vrot.x*(ypos*ypos+1)+vrot.y*(-xpos*ypos)+vrot.z*(-xpos);
    			
    			//calculate translational component of optical flow
    			xtras = vtras.x*(-1/point.z)+vtras.z*(xpos/point.z);
    			ytras = vtras.y*(-1/point.z)+vtras.z*(ypos/point.z);
    			
    			//check for NaN:
    			xrot = check(xrot);
    			yrot = check(yrot);
    			xtras = check(xtras);
    			ytras = check(ytras);
    			xpos = check(xpos);
    			ypos = check(ypos);
    			
    			//optical flow
    			xopt = xtras+xrot;
    			yopt = ytras+yrot;
    			
    			//create the array for containing optical flow info
    			//temporary in case if it is later useful :)
    			pos[i][j] = new Vector2d(xpos,ypos);
    			vis[i][j] = new Vector2d(xopt,yopt);
    			
    			//determine distance based on current velocity of bee:
    			
    			//measures the individual distance of every vector in the image based on the
    		    //current velocity of the bee--currently only translational vel is used!
    			if (xopt==0) {
    				depth[i][j] = 0;
    			}
    			else {
    				depth[i][j] = (-vel.x+vel.z*xpos)/(xopt);
    			}
    			
    			//System.out.println("depth: "+depth[i][j]);
    			//System.out.println("point.z: "+point.z);
    		}
    	}
    }
    
    //finds the minimum depth in the image
    public double minDist() {
    	int pos = -1;
    	double min = Double.POSITIVE_INFINITY;
    	for (int i=0; i<AREA; i++) {
    		for (int j=0; j<AREA; j++) {
    			if (depth[i][j]<min && depth[i][j]>0) {
    				min = depth[i][j];
    				pos = j;
    			}
    		}
    	}
    	
    	//used mainly just for the forward sensor--simple hack, may make better
    	if (pos < (AREA - 1) / 2) {isRight = true;}
    	else if (pos > (AREA - 1) / 2) {isLeft = true;}
    	else {isMid = true;}
    	
    	return min;
    }
    
    public double aveDist() {
    	double ave = 0;
    	double countZero = 0; //used to get rid of useless values
    	for (int i=0; i<AREA; i++) {
    		for (int j=0; j<AREA; j++) {
    			if (depth[i][j]<=0) {
    				countZero++;
    			}
    			else {
    				ave = ave + depth[i][j];
    			}
    		}
    	}
    	if (ave==0) {return ave;}
    	else {return ave/(AREA*AREA-countZero);}
    }
    
    //function takes a vector and sets up a coordinate system for it such that
    //the vector points in the z-direction--it returns the quaternion by which
    //one must rotate the vector to get it in that frame
    private Quat4d setZ(Vector3d in) {
    	double ang = in.dot(UNIT_Z);
    	double angle = Math.acos(ang);
    	
    	Vector3d axis = new Vector3d();
    	axis.cross(in,UNIT_Z);
    	
    	Quat4d out = new Quat4d();
    	out = axisToQuat(axis,angle);
    	
    	return out;
    }
    
    //convert axisAngle coordinates to quaternion coords efficiently
    protected Quat4d axisToQuat(Vector3d axis,double angle) {
    	AxisAngle4d axisAngle = new AxisAngle4d(axis,angle);
    	Quat4d quat = new Quat4d();
    	quat.set(axisAngle);
    	return quat;
    }
    
    //rotates points--needed to keep things as double instead of float
    protected Vector3d rotate(Quat4d rot,Vector3d in) {
    	Quat4d point = new Quat4d(in.x,in.y,in.z,0);
    	Quat4d int1 = new Quat4d();
    	int1.mul(rot,point);
    	Quat4d int2 = new Quat4d();
    	int2.conjugate(rot);
    	int1.mul(int2);
    	Vector3d out = new Vector3d(int1.x,int1.y,int1.z);
    	return out;
    }
    
    //checks for NaN/Infinity values, makes 0 if NaN/Infinity
    protected double check(double d) {
    	if (Double.isNaN(d)) {return 0;}
    	else if (Double.isInfinite(d)) {return 0;}
    	else return d;
    }
    
    /* Code below here is taken from DefaultRangeSensor with some adjustments*/
    
    private DiscreteDynamicsWorld world;
    
    //these values don't even matter DX
    private float sigma = 0.05f;    // m
    private float minRange = 0.1f;  // m
    private float maxRange = 20.0f;  // m

    private static final float CONTACT_EPSILON = 0.01f;
    

    /** {@inheritDoc} */
    public double getRange(Vector3d in) {
    	//check for contact
        for (Contact c : getHost().getContactPoints()) {

            Vector3f diff = new Vector3f();
            diff.sub(getOffset(), c.getBodyContactPoint());

            if (diff.length() <= CONTACT_EPSILON) {
                return Float.POSITIVE_INFINITY; 
            }
        }
        
        Vector3f rotatedOffset = new Vector3f(getOffset());
        Vector3f rotatedPointing = new Vector3f(in);
        
        rotatedPointing.scale(maxRange);

        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(getHost().getTruthOrientation());

        trans.transform(rotatedOffset);
        trans.transform(rotatedPointing);

        Vector3f from = new Vector3f(getHost().getTruthPosition());
        Vector3f to = new Vector3f(getHost().getTruthPosition());
        
        from.add(rotatedOffset);
        to.add(rotatedOffset);
        to.add(rotatedPointing);

        // collide the ray with the world and see what objects are intersected
        RayCallback callback = new RayCallback(maxRange);

        world.rayTest(from, to, callback);
        
        double range = addNoise(callback.getMinDistance(), sigma);
        
        if ((range < minRange) || (range > maxRange)) {
            range = Float.POSITIVE_INFINITY;
        }
        
        return range;
    }
    
    private static class RayCallback extends CollisionWorld.RayResultCallback {

        private float rayLength;
        private float minDistance = Float.POSITIVE_INFINITY;

        public RayCallback(float length) {
            rayLength = length;
        }

        public float addSingleResult(CollisionWorld.LocalRayResult rayResult, boolean normalInWorldSpace) {

            float dist = rayResult.hitFraction * rayLength;

            if (dist < minDistance) {
                minDistance = dist;
            }

            return rayResult.hitFraction;
        }

        public float getMinDistance() {
            return minDistance;
        }
    }
    
    @Inject
    public final void setDynamicsWorld(@GlobalScope DiscreteDynamicsWorld world) {
        this.world = world;
    }

    @Inject(optional = true)
    public final void setSigma(@Named("sigma") final float sigma) {
        this.sigma = sigma;
    }

    @Inject(optional = true)
    public final void setMinRange(@Named("min-range") final float minRange) {
        this.minRange = minRange;
    }

    @Inject(optional = true)
    public final void setMaxRange(@Named("max-range") final float maxRange) {
        this.maxRange = maxRange;
    }
}