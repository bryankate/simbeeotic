package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;
import java.awt.Color;


/**
 * A model that represents the Blade MCX helicopter platform. There
 * is a simplified command API that allows subclass implementations
 * to control the yaw, pitch, roll, and thrust of the vehicle. These
 * commands are transformed to forces on the body according to a model
 * of the helicopter obtained through a system identification process.
 *
 * @author bkate
 */
public class HeliBee extends AbstractHeli {


    @Override
    protected final RigidBody initializeBody(DiscreteDynamicsWorld world) {

        float mass = 0.28f;
        int id = getObjectId();
        CollisionShape cs = HELI_SHAPE;

        getMotionRecorder().updateShape(id, cs);
        getMotionRecorder().updateMetadata(id, new Color(238, 201, 0), null, getName());

        Transform startTransform = new Transform();
        startTransform.setIdentity();

        Vector3f localInertia = new Vector3f(0, 0, 0);
        cs.calculateLocalInertia(mass, localInertia);

        Vector3f start = getStartPosition();
        start.z += 0.0225;
        
        startTransform.origin.set(start);

        MotionState myMotionState = new RecordedMotionState(id, getMotionRecorder(), startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, cs, localInertia);

        // modify the thresholds for deactivating the bee
        // because it moves at a much smaller scale
        rbInfo.linearSleepingThreshold = 0.08f;  // m/s
        rbInfo.angularSleepingThreshold = 0.1f;  // rad/s

        RigidBody body = new RigidBody(rbInfo);
        body.setUserPointer(new EntityInfo(id));

        // bees do not collide with each other or the hive
        world.addRigidBody(body, COLLISION_BEE, (short)(COLLISION_TERRAIN | COLLISION_FLOWER));

        return body;
    }


    @Override
    public double getYaw() {
        return 0;
    }


    @Override
    public void setYaw(double level) {
    }


    @Override
    public double getPitch() {
        return 0;
    }


    @Override
    public void setPitch(double level) {
    }


    @Override
    public double getRoll() {
        return 0;
    }


    @Override
    public void setRoll(double level) {
    }


    @Override
    public double getThrust() {
        return 0;
    }


    @Override
    public void setThrust(double level) {
    }
}
