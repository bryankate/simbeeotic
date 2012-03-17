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
 * A hacker solution to solve the case of parallel vectors in the Z-direction -- we make a special class
 * for the sensor pointing to the bottom.
 *
 * @author Lucia Mocz
 */

public class BottomOpticalMotionSensor extends DefaultOpticalMotionSensor {

    private static final int AREA = 3; //number of points in one direction of viewing angle
    private static final double FREQUENCY = (Math.PI/3)/(AREA-1);

    private Vector2d[][] vis = new Vector2d[AREA][AREA];
    private Vector2d[][] pos = new Vector2d[AREA][AREA];
    private Vector3d[][] pointing = new Vector3d[AREA][AREA]; //used for debugging
    private double[][] depth = new double[AREA][AREA]; //individual depths
    private double ave; //average depth across image
    private double range, xpos, ypos, xrot, xtras, yrot, ytras, xopt, yopt;
    private Vector3d vrot, vtras, point;
    
    @Override
    public boolean isLeft() {return false;}


    @Override
    public boolean isRight() {return false;}


    @Override
    public boolean isMid() {return false;}


    @Override
    public void getMotion(Vector3d currVel) {

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
    	Quat4d forw = new Quat4d(1,0,0,0);
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
    			pointing[i][j] = verPoint;
    			
    			//transform back to world coordinates--use conjugate of existing quats
    			//and reverse order of their multiplication
    			//use this to get the range to the nearest object
    			
    			Vector3d worldPoint = rotate(back,verPoint);
    			
    			//scalar quantity by which to multiply verPoint to get range vector
    			range = getRange(worldPoint);
    			range = check(range);
    			
    			point = new Vector3d();
    			point.scale(range,verPoint); //correct pointing/distance vector in opt coords
    			
    			if (point.z<0) { point.z = point.z * -1;}
    			
    			//scale into the image plane, use vector in camera coordinates
    			xpos = point.x/point.z;
    			ypos = point.y/point.z;
    			
    			//calculate rotational component of optical flow
    			xrot = vrot.x*(xpos*ypos)+vrot.y*(-1*(xpos*xpos+1))+vrot.z*ypos;
    			yrot = vrot.x*(ypos*ypos+1)+vrot.y*(-xpos*ypos)+vrot.z*(-xpos);
    			
    			//calculate translational component of optical flow
    			//System.out.println("vtras vel: "+vtras);
    			xtras = vtras.x*(-1/point.z)+vtras.z*(xpos/point.z);
    			ytras = vtras.y*(-1/point.z)+vtras.z*(ypos/point.z);
    			
    			//System.out.println("vtras: "+vtras);
    			//System.out.println("currVel: "+vel);
    			
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
    			
    			//System.out.println("point.z: "+point.z);
    		}
    	}
    }


    @Override
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
    	
    	return min;
    }
}