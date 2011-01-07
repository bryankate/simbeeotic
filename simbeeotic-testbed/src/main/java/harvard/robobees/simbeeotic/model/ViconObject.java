package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.ExternalRigidBody;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;

import javax.vecmath.Vector3f;
import java.awt.Color;


/**
 * A representation of an object that is tracked by the Vicon motion capture
 * system. It's position and orientation are given by vicon. The testbed helicopters
 * have a specialized class to represent them in the simulation, and should not use this
 * class.
 *
 * @author bkate
 */
public class ViconObject extends AbstractPhysicalEntity {

    private ExternalRigidBody body;
    private ExternalStateSync externalSync;


    @Override
    protected RigidBody initializeBody(DiscreteDynamicsWorld world) {

        // todo: custom shape given by scenario XML

        float mass = 1;
        int id = getObjectId();
        CollisionShape cs = new SphereShape(0.1f);

        getMotionRecorder().updateShape(id, cs);
        getMotionRecorder().updateMetadata(id, Color.RED, null, getName());

        Transform startTransform = new Transform();
        startTransform.setIdentity();

        Vector3f localInertia = new Vector3f(0, 0, 0);
        cs.calculateLocalInertia(mass, localInertia);

        Vector3f start = getStartPosition();
        startTransform.origin.set(start);

        MotionState myMotionState = new RecordedMotionState(id, getMotionRecorder(), startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, cs, localInertia);

        // modify the thresholds for deactivating the bee
        // because it moves at a much smaller scale
        rbInfo.linearSleepingThreshold = 0.08f;  // m/s
        rbInfo.angularSleepingThreshold = 0.1f;  // rad/s

        // NOTE: EXTERNAL RIGID BODY!
        body = new ExternalRigidBody(rbInfo);
        body.setUserPointer(new EntityInfo(id));

        // bees do not collide with each other or the hive
        world.addRigidBody(body, COLLISION_ALL, COLLISION_ALL);

        // register this object with the external synchronizer
        externalSync.registerObject(getName(), body);

        return body;
    }


    @Override
    public void finish() {
    }


    @Override
    public final Vector3f getTruthAngularAcceleration() {
        return body.getAngularAcceleration(new Vector3f());
    }


    @Override
    public final Vector3f getTruthLinearAcceleration() {
        return body.getLinearAcceleration(new Vector3f());
    }


    @Inject
    public final void setExternalStateSync(@GlobalScope ExternalStateSync sync) {
        this.externalSync = sync;
    }
}
