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

    private int modelId;
    private MotionRecorder recorder;


    public RecordedMotionState(int modelId, MotionRecorder recorder) {

        super();

        this.modelId = modelId;
        this.recorder = recorder;

        setWorldTransform(startWorldTrans);
    }


    public RecordedMotionState(int modelId, MotionRecorder recorder, Transform startTrans) {

        super(startTrans);

        this.modelId = modelId;
        this.recorder = recorder;

        setWorldTransform(startWorldTrans);
    }


    public RecordedMotionState(int modelId, MotionRecorder recorder, Transform startTrans, Transform centerOfMassOffset) {

        super(startTrans, centerOfMassOffset);

        this.modelId = modelId;
        this.recorder = recorder;

        setWorldTransform(startWorldTrans);
    }


    @Override
    public void setWorldTransform(Transform trans) {

        super.setWorldTransform(trans);

        Vector3f pos = new Vector3f(trans.origin);
        Quat4f orient = new Quat4f();

        trans.getRotation(orient);

        recorder.updateKinematicState(modelId, pos, orient);
    }
}
