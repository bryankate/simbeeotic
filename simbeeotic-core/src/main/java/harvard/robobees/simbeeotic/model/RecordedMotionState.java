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


import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;


/**
 * An implementation of {@link com.bulletphysics.linearmath.MotionState} that pushes motion 
 * updates to the {@link MotionRecorder}.
 *
 * @author bkate
 */
public class RecordedMotionState extends DefaultMotionState {

    private int objectId;
    private MotionRecorder recorder;


    public RecordedMotionState(int objectId, MotionRecorder recorder) {

        super();

        this.objectId = objectId;
        this.recorder = recorder;

        setWorldTransform(startWorldTrans);
    }


    public RecordedMotionState(int objectId, MotionRecorder recorder, Transform startTrans) {

        super(startTrans);

        this.objectId = objectId;
        this.recorder = recorder;

        setWorldTransform(startWorldTrans);
    }


    public RecordedMotionState(int objectId, MotionRecorder recorder, Transform startTrans, Transform centerOfMassOffset) {

        super(startTrans, centerOfMassOffset);

        this.objectId = objectId;
        this.recorder = recorder;

        setWorldTransform(startWorldTrans);
    }


    @Override
    public void setWorldTransform(Transform trans) {

        super.setWorldTransform(trans);

        Vector3f pos = new Vector3f(trans.origin);
        Quat4f orient = new Quat4f();

        trans.getRotation(orient);

        recorder.updateKinematicState(objectId, pos, orient);
    }
}
