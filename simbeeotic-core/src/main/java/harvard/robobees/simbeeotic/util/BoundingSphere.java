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
package harvard.robobees.simbeeotic.util;


import javax.vecmath.Vector3f;


/**
 * A class that represents a bounding sphere - a sphere that
 * completely encompasses an object's physical extent.
 *
 * @author bkate
 */
public class BoundingSphere {

    private Vector3f center;
    private float radius;


    public BoundingSphere(final Vector3f center, final float radius) {

        this.center = new Vector3f(center);
        this.radius = radius;
    }


    /**
     * Gets the center of the boundign sphere.
     *
     * @return The center of the sphere, in the world frame.
     */
    public final Vector3f getCenter() {
        return center;
    }


    /**
     * Gets the radius of the bounding sphere.
     *
     * @return The radius of the sphere (in meters).
     */
    public final float getRadius() {
        return radius;
    }


    /**
     * Determines if a bounding sphere intersects this sphere.
     *
     * @param other The sphere to check against.
     *
     * @return True if the other sphere intersects this sphere, false otherwise.
     */
    public final boolean intersects(final BoundingSphere other) {
        return intersects(this, other);
    }


    /**
     * Determines if two bounding spheres intersect.
     *
     * @param sphere1 The first sphere to check.
     * @param sphere2 The second shpere to check.
     *
     * @return True if the two spheres intersect, false otherwise.
     */
    public static boolean intersects(final BoundingSphere sphere1, final BoundingSphere sphere2) {

        Vector3f diff = new Vector3f();

        diff.sub(sphere1.getCenter(), sphere2.getCenter());

        return (diff.length() <= (sphere1.getRadius() + sphere2.getRadius()));
    }
}
