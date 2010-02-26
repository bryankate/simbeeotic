package harvard.robobees.simbeeotic.model;


import org.apache.log4j.Logger;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;

import javax.vecmath.Vector3f;


/**
 * A hive implementation that does nothing. It is represented by a static box in the
 * world and has no behavior, sensors, or communication capabilities. Users can extend
 * the class to add functionality.
 *
 * @author bkate
 */
public class SimpleHive extends AbstractPhysicalModel {

    private float size = 1.0f;  // m

    private static Logger logger = Logger.getLogger(SimpleHive.class);


    /** {@inheritDoc} */
    @Override
    protected RigidBody initializeBody(DiscreteDynamicsWorld world) {

        // establish the static hive geometry
        float halfSize = size / 2;

        CollisionShape colShape = new BoxShape(new Vector3f(halfSize, halfSize, halfSize));

        Transform startTransform = new Transform();
        startTransform.setIdentity();

        startTransform.origin.set(new Vector3f(getStartX(), getStartY(), getStartZ() + halfSize));

        MotionState myMotionState = new DefaultMotionState(startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0, myMotionState,
                                                                         colShape, new Vector3f(0, 0, 0));

        RigidBody body = new RigidBody(rbInfo);
        body.setUserPointer(new EntityInfo());

        world.addRigidBody(body, COLLISION_HIVE, COLLISION_NONE);

        return body;
    }


    public final float getSize() {
        return size;
    }


    @Inject(optional = true)
    public final void setSize(@Named(value = "size") final float size) {

        if (!isInitialized()) {
            this.size = size;
        }
    }
}
