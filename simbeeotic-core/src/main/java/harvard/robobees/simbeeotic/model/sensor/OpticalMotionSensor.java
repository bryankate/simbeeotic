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
