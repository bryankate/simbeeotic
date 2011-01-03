package harvard.robobees.simbeeotic.component;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import filterd.protocol.Control;
import filterd.protocol.KinematicState;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.ExternalStateSync;
import org.apache.log4j.Logger;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A component that interfaces with the Vicon data server
 * and updates the state of objects in the simulation.
 *
 * @author bkate
 */
public class ViconInputComponent implements VariationComponent {

    @Inject
    @GlobalScope
    private ExternalStateSync externalSync;

    @Inject
    @Named("object-names")
    private String objectNames;

    @Inject(optional = true)
    @Named("server-host")
    private String serverHost = "localhost";

    @Inject(optional = true)
    @Named("server-port")
    private int serverPort = 6789;

    private AtomicBoolean running = new AtomicBoolean(true);

    private static Logger logger = Logger.getLogger(ViconInputComponent.class);

    
    @Override
    public void initialize() {

        String[] objects = objectNames.split(",");

        for (String name : objects) {
            new Thread(new ViconThread(name.trim(), running)).start();
        }
    }


    @Override
    public void shutdown() {
        running.set(false);
    }


    /**
     * An instance of this class runs in a thread and updates an individual object.
     */
    private class ViconThread implements Runnable {

        private String objectName;
        private DatagramSocket sock;
        private AtomicBoolean running;


        public ViconThread(String name, AtomicBoolean run) {

            objectName = name;
            running = run;
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

            byte[] buff = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);

            while(running.get()) {

                try {

                    // get another data packet and update the object in the world
                    sock.receive(packet);

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

                    externalSync.setState(objectName, pos, orient);

//                    logger.debug(objectName + " " + pos + " " + orient);
                }
                catch(IOException ioe) {
                    return;
                }
            }
        }
    }
}
