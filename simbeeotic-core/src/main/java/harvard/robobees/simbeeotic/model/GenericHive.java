package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;


/**
 * A hive implementation represented by a static box in the
 * world. Users can add sensors, a radio, and custom logic to
 * give it more interesting behavior.
 *
 * @author bkate
 */
public class GenericHive extends GenericModel {

    private GenericHiveLogic logic;

    // parameters
    private float size = 1.0f;  // m

    private static Logger logger = Logger.getLogger(GenericHive.class);


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


    /** {@inheritDoc} */
    @Override
    protected void initializeBehavior() {
        logic.initialize(this);
    }


    /** {@inheritDoc} */
    @Override
    public void update(double currTime) {
        logic.update(currTime);
    }


    public final float getSize() {
        return size;
    }


    public final void setLogic(final GenericHiveLogic logic) {

        if (!isInitialized()) {

            this.logic = logic;

            if (getRadio() != null) {
                getRadio().addMessageListener(logic);
            }
        }
    }


    @Inject(optional = true)
    public final void setSize(@Named(value = "size") final float size) {

        if (!isInitialized()) {
            this.size = size;
        }
    }
}
