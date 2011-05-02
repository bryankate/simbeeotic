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
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;


/**
 * A model that acts as a proxy for a physical helicopter flying in the
 * testbed. The position and orientation are obtained from an outside
 * source (e.g. the Vicon motion capture cameras) and all commands are
 * forwarded to the physical helicopter via radio.
 * <p/>
 * Any attempt to directly apply a force on this model will fail. It can
 * only be controlled via the given command API.
 *
 * @author bkate
 */
public class HWILBee extends AbstractHeli {

    private ExternalRigidBody body;
    private ExternalStateSync externalSync;

    private DatagramSocket sock;
    private InetAddress server;

    // current command - 2 bytes each for thrust, roll, pitch, and yaw
    private byte[] commands = new byte[] {(byte)0xaa, 0x00, (byte)0xff, 0x01, (byte)0xff, 0x01, (byte)0xff, 0x01};

    private Timer boundsTimer;
    private long landingTime = 2;        // seconds, duration of soft landing command
    private double landingThrust = 0.3;  // thrust command for soft landing
    private double landingHeight = 0.75; // m, above which a soft landing should be attempted

    // params
    private String serverHost = "192.168.7.11";
    private int serverPort = 8000;
    private boolean boundsCheckEnabled = true;
    private float xBoundMin = -2f;  // m
    private float xBoundMax = 2f;   // m
    private float yBoundMin = -2f;  // m
    private float yBoundMax = 2f;   // m
    private float zBoundMax = 2;   // m

    private static final short CMD_LOW  = 170;
    private static final short CMD_MID  = 511;
    private static final short CMD_HIGH = 852;
    private static final short CMD_RANGE = CMD_HIGH - CMD_LOW;

    private static Logger logger = Logger.getLogger(HWILBee.class);


    @Override
    public void initialize() {

        super.initialize();

        try {

            sock = new DatagramSocket();
            server = InetAddress.getByName(serverHost);
        }
        catch(Exception e) {
            logger.error("Could not establish connection to heli_server.", e);
        }

        // start out by zeroing the heli (thrust to zero, yaw, pitch and roll to 0.5)
        sendCommands();

        // setup a timer that checks for boundary violations
        if (boundsCheckEnabled) {

            boundsTimer = createTimer(new TimerCallback() {

                @Override
                public void fire(SimTime time) {

                    Vector3f currPos = getTruthPosition();

                    if ((currPos.x < xBoundMin) || (currPos.x > xBoundMax) ||
                        (currPos.y < yBoundMin) || (currPos.y > yBoundMax) ||
                        (currPos.z > zBoundMax)) {

                        // out of bounds, shutdown behaviors and heli
                        logger.warn("Heli (" + getName() + ") is out of bounds, shutting down.");

                        for (HeliBehavior b : getBehaviors().values()) {
                            b.stop();
                        }

                        // if we are too high try a soft landing
                        if (currPos.z >= landingHeight) {
                            
                            // reduce rotor speed for soft landing
                            setThrust(landingThrust);

                            // set a timer a few seconds in the future to shutdown completely
                            createTimer(new TimerCallback() {

                                @Override
                                public void fire(SimTime time) {
                                    setThrust(0);
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
        rbInfo.linearSleepingThreshold = 0.08f;  // m/s
        rbInfo.angularSleepingThreshold = 0.1f;  // rad/s

        // NOTE: EXTERNAL RIGID BODY!
        body = new ExternalRigidBody(rbInfo);
        body.setUserPointer(new EntityInfo(id));

        // bees do not collide with each other or the hive
        world.addRigidBody(body, COLLISION_BEE, (short)(COLLISION_TERRAIN | COLLISION_FLOWER));

        // register this object with the external synchronizer
        externalSync.registerObject(getName(), body);

        return body;
    }


    @Override
    public void finish() {

        super.finish();

        if (boundsTimer != null) {
            boundsTimer.cancel();
        }

        // try to shutdown the heli gently
        commands = new byte[8];
        sendCommands();

        sock.close();
    }


    @Override
    public double getThrust() {

        short val = (short)((0xff & commands[1]) << 8 |
                            (0xff & commands[0]));

        return (val - CMD_LOW) / (double)CMD_RANGE;
    }


    @Override
    public final void setThrust(double level) {

        short curr = (short)(CMD_LOW + (CMD_RANGE * cap(level)));

        commands[0] = (byte)(curr & 0x00ff);
        commands[1] = (byte)((curr & 0xff00) >> 8);

        sendCommands();
    }


    @Override
    public double getRoll() {

        short val = (short)((0xff & commands[3]) << 8 |
                            (0xff & commands[2]));

        return (val - CMD_LOW) / (double)CMD_RANGE;
    }


    @Override
    public final void setRoll(double level) {

        short curr = (short)(CMD_LOW + (CMD_RANGE * cap(level)));

        commands[2] = (byte)(curr & 0x00ff);
        commands[3] = (byte)((curr & 0xff00) >> 8);

        sendCommands();
    }


    @Override
    public double getPitch() {

        short val = (short)((0xff & commands[5]) << 8 |
                            (0xff & commands[4]));

        return (val - CMD_LOW) / (double)CMD_RANGE;
    }


    @Override
    public final void setPitch(double level) {

        short curr = (short)(CMD_LOW + (CMD_RANGE * cap(level)));

        commands[4] = (byte)(curr & 0x00ff);
        commands[5] = (byte)((curr & 0xff00) >> 8);

        sendCommands();
    }


    @Override
    public double getYaw() {

        short val = (short)((0xff & commands[7]) << 8 |
                            (0xff & commands[6]));

        return (val - CMD_LOW) / (double)CMD_RANGE;
    }


    @Override
    public final void setYaw(double level) {

        short curr = (short)(CMD_LOW + (CMD_RANGE * cap(level)));

        commands[6] = (byte)(curr & 0x00ff);
        commands[7] = (byte)((curr & 0xff00) >> 8);

        sendCommands();
    }


    private void sendCommands() {

        DatagramPacket dgram = new DatagramPacket(commands, commands.length, server, serverPort);

        try {
            sock.send(dgram);
        }
        catch(IOException ioe) {
            logger.error("Could not send command packet to heli_server.", ioe);
        }
    }


    private double cap(double in) {

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
    public final void setXBoundMin(@Named("x-bound-min") final float val) {
        this.xBoundMin = val;
    }


    @Inject(optional = true)
    public final void setXBoundMax(@Named("x-bound-max") final float val) {
        this.xBoundMax = val;
    }


    @Inject(optional = true)
    public final void setYBoundMin(@Named("y-bound-min") final float val) {
        this.yBoundMin = val;
    }


    @Inject(optional = true)
    public final void setYBoundMax(@Named("y-bound-max") final float val) {
        this.yBoundMax = val;
    }


    @Inject(optional = true)
    public final void setZBoundMax(@Named("z-bound-max") final float val) {
        this.zBoundMax = val;
    }
}
