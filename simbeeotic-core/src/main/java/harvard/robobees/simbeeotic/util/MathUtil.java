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
package harvard.robobees.simbeeotic.util;


import com.bulletphysics.linearmath.QuaternionUtil;
import com.bulletphysics.linearmath.ScalarUtil;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class MathUtil {


    /**
     * Returns the direction cosin matrix (DCM), aka rotation matrix, for the ZYX rotation
     * of the given Euler angles. This means that the body is first rotatd about the X axis (roll),
     * then the Y axis (pitch), then the Z axis, (yaw).
     *
     * @param x The rotation about the X axis.
     * @param y The rotation about the Y axis.
     * @param z The rotation about the Z axis.
     *
     * @return The corresponding DCM (using ZYX rotation order) for the given Euler angles.
     */
    public static Matrix3f eulerZYXtoDCM(final float x, final float y, final float z) {

        float c1 = (float)Math.cos(z);
        float c2 = (float)Math.cos(y);
        float c3 = (float)Math.cos(x);
        float s1 = (float)Math.sin(z);
        float s2 = (float)Math.sin(y);
        float s3 = (float)Math.sin(x);
        float c1c3 = c1 * c3;
        float s1s2 = s1 * s2;


        Matrix3f dcm = new Matrix3f();

        dcm.setRow(0, c1 * c2,                  -c2 * s1,             s2);
        dcm.setRow(1, c3 * s1 + c1 * s2 * s3,   c1c3 - s1s2 * s3,     -c2 * s3);
        dcm.setRow(2, s1 * s3 - c1c3 * s2,      c3 * s1s2 + c1 * s3,  c2 * c3);

        return dcm;
    }



    /**
     * Converts a quaternion rotation representation into an Euler angle representation
     * that uses the ZXY rotation sequence.
     *
     * <br/>
     * There are singular behaviors in the conversion resulting in possible failure when this method is set up
     * with Euler angles with theta (pitch) = pi/2 or -pi/2. In the pi/2 case the difference psi - phi is the only thing
     * that is determined and in the -pi/2 the sum psi + phi is the only thing that is determined. Physically when a body is
     * pointing up (theta == pi/2) then a positive yaw (psi) is equivalent to a negative roll (phi) and when the body is
     * pointing down (theta == -pi/2) then a positive yaw (psi) is equivalent to a positive roll (phi). The singularity
     * remains no matter which choice of Euler angles is used, but these specific relationships are specific to the local
     * ZYX sequence.
     *
     * @param quat The quaternion to convert.
     *
     * @return Euler angles (rotX, rotY, rotZ) = (roll, pitch, yaw) representing the ZYX rotation of the body.
     */
    public static Vector3f quaternionToEulerZYX(final Quat4f quat) {

        Quat4f normalized = new Quat4f(quat);
        normalized.normalize();

        double q0 = normalized.w;
        double q1 = normalized.x;
        double q2 = normalized.y;
        double q3 = normalized.z;
        double psi, theta, phi;

        if ((Math.abs(q0 - q2) < 1e-9) && (Math.abs(q3 + q1) < 1e-9)) {

            theta = Math.PI / 2;
            psi = 0;
            phi = 2 * Math.atan2(q1, q0);
        }
        else if ((Math.abs(q0 + q2) < 1e-9) && (Math.abs(q1 - q3) < 1e-9)) {

            theta = -Math.PI / 2;
            psi = 0;
            phi = 2 * Math.atan2(q1, q0);
        }
        else {

            // yaw
            psi = Math.atan2(2.0 * (q1 * q2 + q0 * q3), q0 * q0 + q1 * q1 - (q2 * q2 + q3 * q3));

            // pitch
            double temp = -2.0 * (q1 * q3 - q0 * q2);

            if (Math.abs(temp) < 1.0) {
                theta = Math.asin(temp);
            }
            else {
                theta = temp > 0.0 ? Math.asin(1.0) : Math.asin(-1.0);
            }

            // roll
            phi = Math.atan2(2.0 * (q2 * q3 + q0 * q1), q0 * q0 + q3 * q3 - (q1 * q1 + q2 * q2));
        }

        return new Vector3f((float)phi, (float)theta, (float)psi);
    }


    /**
     * Gets the rotation (expressed as a quaternion) necessary to rotate from one
     * vecgtor to another.
     *
     * @param v1 The first vector.
     * @param v2 The second vector.
     *
     * @return The rotation required to get from v1 to v2.
     */
    public static Quat4f getRotation(final Vector3f v1, final Vector3f v2) {

        // use the axis-angle method to find the rotation between the two

        Vector3f v1Norm = new Vector3f(v1);
        Vector3f v2Norm = new Vector3f(v2);

        v1Norm.normalize();
        v2Norm.normalize();

        // parallel vectors?
        if (ScalarUtil.fuzzyZero(v1Norm.angle(v2Norm))) {
            return new Quat4f();
        }

        Vector3f rotAxis = new Vector3f();

        rotAxis.cross(v1Norm, v2Norm);
        rotAxis.normalize();

        float rotAng = (float)Math.acos(v1Norm.dot(v2Norm));

        Quat4f rot = new Quat4f();
        QuaternionUtil.setRotation(rot, rotAxis, rotAng);

        return rot;
    }


    /**
     * Converts a value in millWatts to dBm (the power ratio referenced to one milliWatt).
     *
     * @param mW The input power.
     *
     * @return The power ratio in dBm.
     */
    public static double mwToDbm(final double mW) {
        return 10 * Math.log10(mW / 1);
    }


    /**
     * Converts a power ratio given in dBm to milliWatts.
     *
     * @param dBm The power ratio to convert.
     *
     * @return The power in milliWatts.
     */
    public static double dbmToMw(final double dBm) {
        return Math.pow(10, dBm / 10);
    }

}
