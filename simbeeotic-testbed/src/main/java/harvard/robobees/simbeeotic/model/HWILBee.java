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
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.awt.Color;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


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
    private byte[] commands = new byte[8];

    // params
    private String serverHost = "192.168.7.11";
    private int serverPort = 8000;

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

            // start out by zeroing the heli
            sendCommands();
        }
        catch(Exception e) {
            logger.error("Could not establish connection to heli_server.", e);
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

        // try to shutdown the heli gently
        commands = new byte[8];
        sendCommands();

        sock.close();
    }


    @Override
    public final void setThrust(double level) {

        short curr = (short)(CMD_LOW + (CMD_RANGE * cap(level)));

        commands[0] = (byte)(curr & 0x00ff);
        commands[1] = (byte)((curr & 0xff00) >> 8);

        sendCommands();
    }


    @Override
    public final void setRoll(double level) {

        short curr = (short)(CMD_LOW + (CMD_RANGE * cap(level)));

        commands[2] = (byte)(curr & 0x00ff);
        commands[3] = (byte)((curr & 0xff00) >> 8);

        sendCommands();
    }


    @Override
    public final void setPitch(double level) {

        short curr = (short)(CMD_LOW + (CMD_RANGE * cap(level)));

        commands[4] = (byte)(curr & 0x00ff);
        commands[5] = (byte)((curr & 0xff00) >> 8);

        sendCommands();
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
}
