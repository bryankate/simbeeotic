package harvard.robobees.simbeeotic.model;


import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.MatrixUtil;

import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;

import harvard.robobees.simbeeotic.util.LinearMathUtil;
import harvard.robobees.simbeeotic.environment.PhysicalConstants;


/**
 * @author bkate
 */
public class TestingBee extends SimpleBee {

    @Override
    protected RigidBody initializeBody(DiscreteDynamicsWorld world) {

        RigidBody body = super.initializeBody(world);

//        float piOver2 = (float)Math.PI / 2;
//
//        Matrix3f rot = LinearMathUtil.eulerZYXtoDCM(piOver2, 0, 0);
//        Quat4f quat= new Quat4f();
//
//        MatrixUtil.getRotation(rot, quat);
//
//        Transform trans = new Transform();
//
//        body.getWorldTransform(trans);
//        trans.setRotation(quat);
//
//        body.setWorldTransform(trans);
//        body.setAngularVelocity(new Vector3f(0, 0, 0));

        return body;
    }


    @Override
    public void applyLogic(double currTime) {

        Vector3f pos = getTruthPosition();
        Vector3f linVel = getTruthLinearVelocity();
        Vector3f angVel = getTruthAngularVelocity();
        Vector3f linAccel = getTruthLinearAcceleration();
        Vector3f angAccel = getTruthAngularAcceleration();
        Vector3f orient = LinearMathUtil.quaternionToEulerZYX(getTruthOrientation());

        float heading = this.compass.getHeading();
        Vector3f accelSens = this.accelerometer.getLinearAcceleration();
        Vector3f gyroSens = this.gyro.getAngularVelocity();

        System.out.println("time: " + currTime + "  " +
                           "pos: " + pos.x + " " + pos.y + " " + pos.z + "  " +
//                           "linVel: " + linVel.x + " " + linVel.y + " " + linVel.z + "  " +
//                           "angVel: " + angVel.x + " " + angVel.y + " " + angVel.z + "  " +
//                           "linAccel: " + linAccel.x + " " + linAccel.y + " " + linAccel.z + "  " +
//                           "angAccel: " + angAccel.x + " " + angAccel.y + " " + angAccel.z + "  " +
//                           "orient: " + orient.x + " " + orient.y + " " + orient.z + "  " +
//                           "accelerometer: " + accelSens.x + " " + accelSens.y + " " + accelSens.z + "  " +
//                           "gyro: " + gyroSens.x + " " + gyroSens.y + " " + gyroSens.z + "  " +
//                           "heading: " + heading + "  " +
//                           "range: " + this.rangeSensorBottom.getRange() + "  " +
//                           "contact: " + this.contactSensorBottom.isTripped() + "  " +
                           "active: " + this.isActive());
    }
}
