package harvard.robobees.simbeeotic.component;


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.linearmath.MatrixUtil;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import filterd.protocol.Control;
import filterd.protocol.KinematicState;
import harvard.robobees.simbeeotic.util.MathUtil;
import org.apache.log4j.Logger;

import java.awt.Toolkit;
import java.awt.event.WindowEvent;
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
    private int serverPort = 7777;

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
        setTitle("Vicon 3D Visulaization");
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

                sock = new DatagramSocket();

                byte[] buff = Control.Connect.newBuilder().setObjectName(objectName)
                                                          .setPort(sock.getPort()).build().toByteArray();

                // send request
                DatagramPacket packet = new DatagramPacket(buff, buff.length, InetAddress.getByName(serverHost), serverPort);

                sock.send(packet);

                // wait for response
                buff = new byte[1024];
                packet = new DatagramPacket(buff, buff.length);

                sock.receive(packet);

                Control.Result result = Control.Result.parseFrom(packet.getData());

                if (result.getType() == Control.Result.Type.SUCCESS) {

                    // no need to lock since this is still called sequentially
                    // from the component thread

                    // make an object in the 3D world
                    objectId = nextId++;

                    // todo: size, color, label
                    world.initializeObject(objectId, new BoxShape(new Vector3f(1.0f, 0.5f, 0.1f)));

                    return true;
                }
                else {
                    logger.error("Could not connect to filterd for object '" + objectName + "': " + result.getMessage());
                }
            }
            catch(Exception e) {
                // fall through
            }

            return false;
        }


        @Override
        public void run() {

            if (!connect()) {
                return;
            }

            byte[] buff = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);

            while(true) {

                try {

                    // get another data packet and update the object in the world
                    sock.receive(packet);

                    KinematicState.State state = KinematicState.State.parseFrom(packet.getData());

                    Vector3f pos = new Vector3f((float)state.getLocX(),
                                                (float)state.getLocY(),
                                                (float)state.getLocZ());

                    Quat4f orient = new Quat4f();

                    MatrixUtil.getRotation(MathUtil.eulerZYXtoDCM((float)state.getOrientEulerX(),
                                                                  (float)state.getOrientEulerY(),
                                                                  (float)state.getOrientEulerZ()),
                                           orient);

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
