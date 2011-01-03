package harvard.robobees.simbeeotic.component;


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.CylinderShapeZ;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import filterd.protocol.Control;
import filterd.protocol.KinematicState;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JFrame;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;


/**
 * A visualization component used with the helicopter testbed. Position
 * and orinetation information is supplied by the Vicon motion tracking
 * cameras.
 *
 * @author bkate
 */
public class ViconVisComponent extends JFrame implements VariationComponent {

    private Java3DWorld world;
    private Lock worldLock = new ReentrantLock();
    private int nextId = 1000000;

    @Inject
    private VariationContext context;

    @Inject
    @Named("object-names")
    private String objectNames;

    @Inject(optional = true)
    @Named("server-host")
    private String serverHost = "localhost";


    @Inject(optional = true)
    @Named("server-port")
    private int serverPort = 6789;

    @Inject(optional = true)
    @Named("use-background")
    private boolean useBackground = true;

    private static Logger logger = Logger.getLogger(ViconVisComponent.class);


    @Override
    public void initialize() {

        world = new Java3DWorld(useBackground);

        // regiater for world objects
        context.getRecorder().addListener(world);

        // setup GUI
        setTitle("Vicon 3D Visualization");
        setSize(900, 600);
        setContentPane(world);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        // hook into vicon data
        String[] objects = objectNames.split(",");

        for (String name : objects) {
            new Thread(new ViconThread(name.trim())).start();
        }
    }


    @Override
    public void shutdown() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }


    @Override
    public void dispose() {

        world.dispose();
        super.dispose();
    }


    /**
     * An instance of this class runs in a thread and updates an individual object.
     */
    private class ViconThread implements Runnable {

        private int objectId;
        private String objectName;
        private DatagramSocket sock;

        
        public ViconThread(String name) {
            objectName = name;
        }


        public boolean connect() {

            // open an incoming socket to receive data
            try {

                sock = new DatagramSocket(null);
                sock.bind(null);

                byte[] buff = Control.Connect.newBuilder().setObjectName(objectName)
                                                          .setPort(sock.getLocalPort()).build().toByteArray();

                // send request
                DatagramPacket packet = new DatagramPacket(buff, buff.length, InetAddress.getByName(serverHost), serverPort);

                sock.send(packet);

                // wait for response
                buff = new byte[1024];
                packet = new DatagramPacket(buff, buff.length);

                sock.receive(packet);

                ByteArrayInputStream in = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                Control.Result result = Control.Result.parseFrom(in);
                in.close();

                if (result.getType() == Control.Result.Type.SUCCESS) {

                    // no need to lock since this is still called sequentially
                    // from the component thread

                    // make an object in the 3D world
                    objectId = nextId++;

                    if (objectName.startsWith("heli")) {

                        CompoundShape cs = new CompoundShape();
                        Transform trans = new Transform();

                        // main body
                        trans.origin.set(new Vector3f(0.0175f, 0, 0));

                        cs.addChildShape(trans, new BoxShape(new Vector3f(0.045f, 0.02f, 0.025f)));

                        // tail
                        trans = new Transform();
                        trans.origin.set(new Vector3f(-0.055f, 0, 0));

                        cs.addChildShape(trans, new BoxShape(new Vector3f(0.055f, 0.0015f, 0.0015f)));

                        trans = new Transform();
                        trans.origin.set(new Vector3f(-0.11f, 0, 0.01f));

                        cs.addChildShape(trans, new BoxShape(new Vector3f(0.01f, 0.0015f, 0.03f)));

                        // rotors
                        trans = new Transform();
                        trans.origin.set(0, 0, 0.025f);

                        cs.addChildShape(trans, new CylinderShapeZ(new Vector3f(0.002f, 0.002f, 0.05f)));

                        trans = new Transform();
                        trans.origin.set(0, 0, 0.03f);

                        cs.addChildShape(trans, new CylinderShapeZ(new Vector3f(0.0925f, 0.0925f, 0.0001f)));

                        trans = new Transform();
                        trans.origin.set(0, 0, 0.045f);

                        cs.addChildShape(trans, new CylinderShapeZ(new Vector3f(0.0925f, 0.0925f, 0.0001f)));

                        // add to world
                        world.shapeUpdate(objectId, cs);
                        world.metaUpdate(objectId, Color.GRAY, null, objectName);
                    }
                    else {

                        world.shapeUpdate(objectId, new SphereShape(0.075f));
                        world.metaUpdate(objectId, Color.RED, null, objectName);
                    }

                    return true;
                }
                else {
                    logger.error("Could not connect to filterd for object '" + objectName + "': " + result.getMessage());
                }
            }
            catch(Exception e) {
                
                e.printStackTrace();
                // fall through
            }

            return false;
        }


        @Override
        public void run() {

            if (!connect()) {
                return;
            }

            int count = 0;
            byte[] buff = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);

            while(true) {

                try {

                    // get another data packet and update the object in the world
                    sock.receive(packet);

                    // drop most packets, don't need to update the screen that fast
                    if ((count % 5) != 0) {
                        continue;
                    }

                    ByteArrayInputStream in = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    KinematicState.State state = KinematicState.State.parseFrom(in);
                    in.close();

                    Vector3f pos = new Vector3f((float)state.getLocX() / 1000.0f,
                                                (float)state.getLocY() / 1000.0f,
                                                (float)state.getLocZ() / 1000.0f);

                    Quat4f orient = new Quat4f((float)state.getOrientQuatX(),
                                               (float)state.getOrientQuatY(),
                                               (float)state.getOrientQuatZ(),
                                               (float)state.getOrientQuatW());

                    worldLock.lock();

                    try {
                        world.stateUpdate(objectId, pos, orient);
                    }
                    finally {
                        worldLock.unlock();
                    }
                }
                catch(IOException ioe) {
                    return;
                }
            }
        }
    }

}
