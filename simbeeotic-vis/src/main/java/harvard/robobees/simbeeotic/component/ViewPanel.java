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
package harvard.robobees.simbeeotic.component;


import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;


/**
 * An interface that defines the behavior of a viewing panel that is used to
 * display a 3D world. Eac implementation of this class must support the minimal
 * features defined in this interface.
 * <p/>
 * Of course, this interface is geared
 * toward a Java3D implementation at this point in time, but it could change
 * to be more generic in the future.
 * 
 * @author bkate
 */
public interface ViewPanel {

    /**
     * Creates a new window that shows the world from the perspective of
     * a simulated object. A camera is set at the obejct's center, pointing
     * in the direction of it's positive body X axis.
     *
     * @param objectId The ID of the object for which the view is to be spawned.
     */
    public void spawnObjectView(int objectId);


    /**
     * Sets the position and orientation of the camera used in the main 3D panel.
     *
     * @param from The position of the camera.
     * @param to A point in the world upon which the camera is focused.
     * @param up The unit vector indicating the direction that is "up".
     */
    public void setMainView(Point3d from, Point3d to, Vector3d up);


    /**
     * Sets the transform (position and orientation) of the camera used in the
     * main 3D panel.
     *
     * @param t3d The new camera transform.
     */
    public void setMainViewTransform(Transform3D t3d);


    /**
     * Gets the transform (position and orientation) of the camera that
     * is used in the main 3D panel.
     *
     * @return The main camera's transform.
     */
    public Transform3D getMainViewTransform();


    /**
     * Toggles the visibility of object labels.
     *
     * @param visible True if visible, false otherwise.
     */
    public void setLabelsVisible(boolean visible);


    /**
     * Called by the parent frame when it is disposed. The intent is to
     * close any child frames that were spawned.
     */
    public void dispose();
}
