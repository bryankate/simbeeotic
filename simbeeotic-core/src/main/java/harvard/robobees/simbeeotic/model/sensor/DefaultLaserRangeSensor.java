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

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.Contact;

import javax.vecmath.Vector3f;

/**
 * @author Mburkardt
 */


public class DefaultLaserRangeSensor extends AbstractSensor implements LaserRangeSensor {

    private DiscreteDynamicsWorld world;

    private float sigma = 0.05f;    // m
    private float minRange = 0.2f;  // m
    private float maxRange = 20.0f;  // m
    private float range[] = new float[181]; //array to keep track of 181 data points from LaserRangeSensor

    private static final float CONTACT_EPSILON = 0.01f;


    /** {@inheritDoc} */
    public float[] getRange() {
      for (int h=0; h<181; h++){  //heading is from -y axis to y axis, both endpoints inclusive.
          int heading = 90 - h;

          Vector3f rotatedOffset = new Vector3f(getOffset());
          Vector3f rotatedPointing = new Vector3f(getPointing());

          double rotation[] = {rotatedPointing.x,rotatedPointing.y, rotatedPointing.z};
          double angle = Math.PI*heading/180.0;
          float rotateZ[][] = {
                                {(float)Math.cos(angle), (float)-Math.sin(angle), 0},
                                {(float)Math.sin(angle), (float)Math.cos(angle), 0},
                                {0, 0, 1}
          };
          float laserHeading[] = {0,0,0};
          for (int i = 0; i<3; i++){
              for (int j = 0; j<3; j++){
                  laserHeading[i] += rotateZ[i][j]*rotation[j];

              }
          }


          Vector3f rotatedLaser = new Vector3f(laserHeading[0], laserHeading[1], laserHeading[2]);

        // check if the range sensor is actually in contact with another object.
        // if there is an object contacting the body in the vicinity of the range sensor,
        // JBullet will actually see through the adjacent object to the next object. for example,
        // consider the body resting on a box that is sitting on the ground. if the range sensor
        // is on the bottom of the body (which is contacting the box), the ray cast downward
        // will see through the box and hit the floor, even though the range to the closest
        // object should be 0. perhaps this is not the best strategy, but it works for most cases.
        for (Contact c : getHost().getContactPoints()) {

            Vector3f diff = new Vector3f();
            diff.sub(getOffset(), c.getBodyContactPoint());

            if (diff.length() <= CONTACT_EPSILON) {
                range[h] =  Float.POSITIVE_INFINITY;
            }
        }


        // we need to find the sensor's position and pointing vector
        // (in world coordinates) given the body's current orientation


        rotatedLaser.scale(maxRange);     //makes maxRange magnitude 15

        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(getHost().getTruthOrientation());

        trans.transform(rotatedOffset);
        trans.transform(rotatedLaser);

        Vector3f from = new Vector3f(getHost().getTruthPosition()); //from = where bee is
        Vector3f to = new Vector3f(getHost().getTruthPosition());   //to = where bee is

        from.add(rotatedOffset); //where bee is +.1x
        to.add(rotatedOffset);   //where bee is +.1x
        to.add(rotatedLaser); //where bee is +.1x +15x


        // collide the ray with the world and see what objects are intersected
        RayCallback callback = new RayCallback(maxRange);

        world.rayTest(from, to, callback);

        // add noise and check bounds
        range[h] = addNoise(callback.getMinDIstance(), sigma);

        if ((range[h] < minRange) || (range[h] > maxRange)) {
            range[h] = Float.POSITIVE_INFINITY;
        }


    }
        return range;
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


    /**
     * A callback that handles ray intersections with objects in the world. It records
     * the minimum distance to any object.
     */
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

        public float getMinDIstance() {
            return minDistance;
        }
    }


}
