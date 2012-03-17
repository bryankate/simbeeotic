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
import java.util.HashSet;
import java.util.Set;
import java.awt.Color;
import java.awt.Image;

import org.apache.log4j.Logger;


/**
 * A class that coordinates the dissemination of motion information for the
 * objects in the simulation. Interested classes can register as a listener
 * of motion state updates via the {@link MotionListener} interface.
 *
 * @author bkate
 */
public class MotionRecorder {

    private Set<MotionListener> listeners = new HashSet<MotionListener>();

    private static Logger logger = Logger.getLogger(MotionRecorder.class);


    /**
     * Set the details of the object shape and size. For the sake of any
     * {@link MotionListener}s, this must be called at least once prior to 
     * any call to {@link #updateKinematicState} or {@link #updateSize}.
     *
     * @param objectId The unique identifier of the object.
     * @param shape The shape and size of the object.
     */
    public void updateShape(int objectId, CollisionShape shape) {

        for (MotionListener listener : listeners) {

            try {
                listener.shapeUpdate(objectId, shape);
            }
            catch(Exception e) {
                logger.warn("Caught an exception when updating MotionListener.", e);
            }
        }
    }


    /**
     * Set the details of the object's scale.
     *
     * @param objectId The unique identifier of the object.
     * @param scale The new scale of the object.
     */
    public void updateScale(int objectId, Vector3f scale) {

        for (MotionListener listener : listeners) {

            try {
                listener.scaleUpdate(objectId, scale);
            }
            catch(Exception e) {
                logger.warn("Caught an exception when updating MotionListener.", e);
            }
        }
    }


    /**
     * Update thie kinematic state of an object (its position and orientation).
     *
     * @param objectId The unique identifier of the object.
     * @param position The position of the object (in the world frame).
     * @param orientation The orientation of the object (in the world frame).
     */
    public void updateKinematicState(int objectId, Vector3f position, Quat4f orientation) {

        for (MotionListener listener : listeners) {

            try {
                listener.stateUpdate(objectId, position, orientation);
            }
            catch(Exception e) {
                logger.warn("Caught an exception when updating MotionListener.", e);
            }
        }
    }


    /**
     * Updates the color of an object.
     *
     * @param objectId The unique identifier of the object.
     * @param color The new color information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, Color color) {
        updateMetadata(objectId, color, null, null);
    }


    /**
     * Updates the color of an object.
     *
     * @param objectId The unique identifier of the object.
     * @param texture The new texture information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, Image texture) {
        updateMetadata(objectId, null, texture, null);
    }


    /**
     * Updates the label associated with an object.
     *
     * @param objectId The unique identifier of the object.
     * @param label The new label information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, String label) {
        updateMetadata(objectId, null, null, label);
    }


    /**
     * Updates the metadata associated with an object.
     *
     * @param objectId The unique identifier of the object.
     * @param color The new color information (may be null if no change is being made).
     * @param texture The new texture information (may be null if no change is being made).
     * @param label The new label information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, Color color, Image texture, String label) {

        for (MotionListener listener : listeners) {

            try {
                listener.metaUpdate(objectId, color, texture, label);
            }
            catch(Exception e) {
                logger.warn("Caught an exception when updating MotionListener.", e);
            }
        }
    }


    /**
     * Adds a listener to the recorder.
     *
     * @param listener The motion listener to be added.
     */
    public void addListener(MotionListener listener) {
        listeners.add(listener);
    }


    /**
     * Removes a listener from the recorder.
     *
     * @param listener The motion listener to be removed.
     */
    public void removeListener(MotionListener listener) {
        listeners.remove(listener);
    }


    /**
     * Shuts down the recorder. This includes stopping any processing threads.
     */
    public void shutdown() {
        listeners.clear();
    }
}
