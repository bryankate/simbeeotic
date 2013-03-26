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


import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.ExternalRigidBody;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.round;


/**
 * A model that acts as a proxy for a physical helicopter with heliboard flying
 * in the testbed. The position and orientation are obtained from an outside
 * source (e.g. the Vicon motion capture cameras) and all commands are
 * forwarded to the physical helicopter via the base board.
 * This forwarding requires bbserver to be running on 'serverHost' on 'serverPort'
 *
 * <p/>
 * Any attempt to directly apply a force on this model will fail. It can
 * only be controlled via the given command API.
 *
 * @author kar
 */
public class AutoHeliBee extends AbstractHeli {

    private ExternalRigidBody body;
    private ExternalStateSync externalSync;

    private DatagramSocket sock;
    private InetAddress server;

    private byte cmd, thrust, roll, pitch, yaw;

    private Timer boundsTimer;
    private long landingTime = 1;        // seconds, duration of soft landing command
    private double landingHeight = 0.75; // m, above which a soft landing should be attempted
    private static final short CMD_LOW  = 0;
    private static final short CMD_HIGH = 255;
    private static final short CMD_RANGE = CMD_HIGH - CMD_LOW;


    // params
    private String serverHost = "192.168.7.11";
    private int serverPort = 1234;
    private double throttleTrim;
    private double rollTrim;
    private double pitchTrim;
    private double yawTrim;
    private boolean boundsCheckEnabled = true;

    protected int THROTTLE_HIGH, THROTTLE_LOW;
    private static Logger logger = Logger.getLogger(AutoHeliBee.class);

    //
    private byte[] commands = new byte[] {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};

    @Override
    public void initialize() {

        THROTTLE_HIGH = 240;
        THROTTLE_LOW = 80;
        super.initialize();

        throttleTrim = normCommand(185);
        rollTrim = normCommand(127);
        pitchTrim = normCommand(127);
        yawTrim = normCommand(127);
        try {

            sock = new DatagramSocket();
            server = InetAddress.getByName(serverHost);
        }
        catch(Exception e) {
            logger.error("Could not establish connection to bbserver.", e);
        }

        logger.debug("Connected to " + serverHost + " on port " + serverPort);

        setCmd(((byte)142));

        // start out by zeroing the heli (thrust to zero, yaw, pitch and roll to 0.5)
        sendCommands();

        // setup a timer that checks for boundary violations
        if (boundsCheckEnabled) {

            boundsTimer = createTimer(new TimerCallback() {

                @Override
                public void fire(SimTime time) {
                    Vector3f currPos = getTruthPosition();

                   //logger.info(externalSync.getOccluded(getName()));
                   if(externalSync.getOccluded(getName()) > 7500) { // runs every 20 ms
                        // out of bounds, shutdown behaviors and heli
                        logger.warn("Heli (" + getName() + ") is occluded for more than half a second, shutting down.");

                        for (HeliBehavior b : getBehaviors().values()) {
                            b.stop();
                        }

                        // if we are too high try a soft landing
                        if (currPos.z >= landingHeight) {

                            // reduce rotor speed for soft landing
                            setThrust(getThrust() - 0.3);

                            // set a timer a few seconds in the future to shutdown completely
                            createTimer(new TimerCallback() {

                                @Override
                                public void fire(SimTime time) {
                                    logger.info("Out of bounds timer fired");
                                    setThrust(0);
                                    setPitch(getPitchTrim());
                                    setRoll(getRollTrim());
                                    getSimEngine().requestScenarioTermination();
                                    finish();
                                }
                            }, landingTime, TimeUnit.SECONDS);
                        }
                        else {
                            setThrust(0);
                        }

                        // no need to check anymore
                        boundsTimer.cancel();

                    }
                }
            }, 0, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
        }
    }


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
        rbInfo.linearSleepingThreshold = 0;  // m/s
        rbInfo.angularSleepingThreshold = 0;  // rad/s

        // NOTE: EXTERNAL RIGID BODY!
        body = new ExternalRigidBody(rbInfo);
        body.setUserPointer(new EntityInfo(id));

        // bees do not collide with each other or the hive
        world.addRigidBody(body, COLLISION_BEE, (short)(COLLISION_TERRAIN | COLLISION_FLOWER));

        // register this object with the external synchronizer
        externalSync.registerOccludedObject(getName(), body);

        return body;
    }


    @Override
    public void finish() {

        super.finish();

        if (boundsTimer != null) {
            boundsTimer.cancel();
        }

        // try to shutdown the heli gently
        while(thrust > 0) {
            setThrust(getThrust() - 1);
            sendCommands();
            try {
                Thread.currentThread().sleep(500);
            }
            catch(Exception e) {
                System.out.println(" Exception " + e.toString());
            }
        }

        setCmd(((byte)42));
        setThrust(0.0);

        logger.debug("Finishing up in AutoHeliBee ..");
        sock.close();
    }

    public byte getCmd() {
       return cmd;
    }

    public final void setCmd(byte level) {
        thrust = (byte) (level & 0xFF);

        logger.debug("thrust: " + thrust);
    }

    @Override
    public double getThrust() {
        double thrustValue;
        double THROTTLE_RANGE = THROTTLE_HIGH - THROTTLE_LOW;

        if(thrust < 0.0)
            thrustValue = thrust + 256;
        else
            thrustValue = thrust;

        double val = (thrustValue - THROTTLE_LOW) / (double)THROTTLE_RANGE;

        return val;
    }


    @Override
    public final void setThrust(double level) {
        int THROTTLE_RANGE = THROTTLE_HIGH - THROTTLE_LOW;
        short val = (short) (THROTTLE_LOW + cap(level) * THROTTLE_RANGE);

        if(val > THROTTLE_HIGH)
            val = (short) THROTTLE_HIGH;
        else if(val < THROTTLE_LOW)
            val = (short) THROTTLE_LOW;

        thrust = (byte) (val & 0xFF);

        sendCommands();

        logger.debug("thrust: " + thrust);
    }


    @Override
    public double getRoll() {
        return (roll - CMD_LOW) / (double)CMD_RANGE;
    }


    @Override
    public final void setRoll(double level) {
        short val = (short) (CMD_LOW + cap(level) * CMD_RANGE);

        roll = (byte) (val & 0xFF);
        //sendCommands();
        logger.debug("roll: " + roll);
    }


    @Override
    public double getPitch() {
        return (pitch - CMD_LOW) / (double)CMD_RANGE;
    }


    @Override
    public final void setPitch(double level) {
        short val = (short) (CMD_LOW + cap(level) * CMD_RANGE);

        pitch = (byte) (val & 0xFF);
        //sendCommands();
        logger.debug("pitch: " + pitch);
    }


    @Override
    public double getYaw() {
        double val = (yaw - CMD_LOW) / (double)CMD_RANGE;

        if(val < 0.0)
            val += 1.0;

        if(val > 1.0)
            val = 1.0;

        return val;
    }


    @Override
    public final void setYaw(double level) {
        short val = (short) (CMD_LOW + cap(level) * CMD_RANGE);

        yaw = (byte) (val & 0xFF);
        sendCommands();
        logger.debug("yaw: " + yaw);
    }


    public final double getPitchTrim() {
        return pitchTrim;
    }


    public final double getRollTrim() {
        return rollTrim;
    }


    public final double getThrustTrim() {
        return throttleTrim;
    }


    public final double getYawTrim() {
        return yawTrim;
    }


    private void sendCommands() {


        commands[0] = (byte) cmd;
        commands[1] = thrust;
        commands[2] = yaw;
        commands[3] = pitch;
        commands[4] = roll;
//
        DatagramPacket dgram = new DatagramPacket(commands, commands.length, server, serverPort);

        try {
            sock.send(dgram);
            //logger.info("Sent command of size " + commands.length + " t: " + commands[1] + " y: " + commands[2] + " p: " + commands[3] + " r: " + commands[4]);
        }
        catch(IOException ioe) {
            logger.error("Could not send command packet to heli_server.", ioe);
        }
    }


    protected static double cap(double in) {

        if (in < 0) {
            return 0;
        }

        if (in > 1) {
            return 1;
        }

        return in;
    }


    @Override
    public final Vector3f getTruthAngularAcceleration() {
        return body.getAngularAcceleration(new Vector3f());
    }


    @Override
    public final Vector3f getTruthLinearAcceleration() {
        return body.getLinearAcceleration(new Vector3f());
    }


    /**
     * Gets the normalized command that corresponds to the raw heli
     * command value.
     *
     * @param cmd The heli command value (in the range of CMD_LOW to CMD_HIGH).
     *
     * @return A normalized command in the range (0,1).
     */
    public static double normCommand(long cmd) {
        return cap((cmd - CMD_LOW) / (double)CMD_RANGE);
    }


    /**
     * Gets the raw helic ommand that corresponds to a normalized command value.
     *
     * @param cmd The normalize command value, in the range of (0,1).
     *
     * @return The heli command value (in the range of CMD_LOW to CMD_HIGH).
     */
    public static short rawCommand(double cmd) {
        return (short)(CMD_LOW + (int)round(cmd * CMD_RANGE));
    }


    @Inject
    public final void setExternalStateSync(@GlobalScope ExternalStateSync sync) {
        this.externalSync = sync;
    }


    @Inject(optional = true)
    public final void setServerHost(@Named("server-host") final String host) {
        this.serverHost = host;
    }


    @Inject(optional = true)
    public final void setServerPort(@Named("server-port") final int port) {
        this.serverPort = port;
    }


    @Inject(optional = true)
    public final void setBoundsCheckEnabled(@Named("enable-bounds-check") final boolean check) {
        this.boundsCheckEnabled = check;
    }


    @Inject(optional = true)
    public final void setThrottleTrim(@Named("trim-throttle") final int trim) {
        this.throttleTrim = normCommand(trim);
    }


    @Inject(optional = true)
    public final void setYawTrim(@Named("trim-yaw") final int trim) {
        this.yawTrim = normCommand(trim);
    }


    @Inject(optional = true)
    public final void setRollTrim(@Named("trim-roll") final int trim) {
        this.rollTrim = normCommand(trim);
    }


    @Inject(optional = true)
    public final void setPitchTrim(@Named("trim-pitch") final int trim) {
        this.pitchTrim = normCommand(trim);
    }
}
