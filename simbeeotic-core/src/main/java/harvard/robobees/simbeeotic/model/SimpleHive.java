package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.vecmath.Vector3f;
import java.awt.*;


/**
 * A hive implementation represented by a static box in the
 * world. Users can add sensors, a radio, and custom logic to
 * give it more interesting behavior.
 *
 * @author bkate
 */
public class SimpleHive extends GenericModel {

    // parameters
    private float size = 1.0f;  // m


    /** {@inheritDoc} */
    @Override
    protected RigidBody initializeBody(DiscreteDynamicsWorld world) {

        // establish the static hive geometry
        float halfSize = size / 2;

        CollisionShape colShape = new BoxShape(new Vector3f(halfSize, halfSize, halfSize));

        Transform startTransform = new Transform();
        startTransform.setIdentity();

        Vector3f start = getStartPosition();
        start.z += halfSize;

        startTransform.origin.set(start);

        getMotionRecorder().initializeObject(getObjectId(), colShape);
        getMotionRecorder().updateMetadata(getObjectId(), new Color(166, 128, 100), getName());

        int id = getObjectId();
        MotionState myMotionState = new RecordedMotionState(id, getMotionRecorder(), startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0, myMotionState,
                                                                         colShape, new Vector3f(0, 0, 0));

        RigidBody body = new RigidBody(rbInfo);
        body.setUserPointer(new EntityInfo(id));

        world.addRigidBody(body, COLLISION_HIVE, COLLISION_NONE);

        return body;
    }


    /** {@inheritDoc} */
    @Override
    public void finish() {
    }


    public final float getSize() {
        return size;
    }


    @Override
    public String toString() {
        return "SimpleHive " + getModelId() + " " + getObjectId();
    }


    @Inject(optional = true)
    public final void setSize(@Named("size") final float size) {

        if (!isInitialized()) {
            this.size = size;
        }
    }
}
