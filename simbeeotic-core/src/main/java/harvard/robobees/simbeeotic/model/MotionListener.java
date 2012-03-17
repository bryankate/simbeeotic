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
package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.CollisionShape;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;
import java.awt.*;


/**
 * An interface that allows an object to be informed about the kinematic
 * state of objects in the simulation.
 *
 * @author bkate
 */
public interface MotionListener {

    /**
     * Supplies the shape and size information for an object.
     *
     * @param objectId The unique identifier for this object.
     * @param shape The shape of the object. It can be queried to determine its
     *              specific type and then downcast.
     */
    public void shapeUpdate(int objectId, CollisionShape shape);


    /**
     * Supplies an update that scales the size of an object, relative to its original size.
     *
     * @param objectId The unique identifier for this object.
     * @param scale The scale of the object.
     */
    public void scaleUpdate(int objectId, Vector3f scale);


    /**
     * Updates the kinematic state of an object.
     *
     * @param objectId The unique identifier of the object.
     * @param position The new position of the object's center (in the world frame).
     * @param orientation The new orientation (in the world frame).
     */
    public void stateUpdate(int objectId, Vector3f position, Quat4f orientation);


    /**
     * Updates the metadata associated with an object.
     *
     * @param objectId The unique identifier of the object.
     * @param color The color update for the object (may be null if no change is being made from previous value).
     * @param texture The texture update for the object (may be null if no change is being made from previous value).
     * @param label The label update for the object (may be null if no change is being made from previous value).
     */
    public void metaUpdate(int objectId, Color color, Image texture, String label);

}
