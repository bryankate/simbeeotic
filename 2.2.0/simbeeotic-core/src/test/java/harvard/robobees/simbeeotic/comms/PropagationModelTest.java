package harvard.robobees.simbeeotic.comms;


import junit.framework.TestCase;
import harvard.robobees.simbeeotic.SimClock;
import harvard.robobees.simbeeotic.util.Gnuplotter;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class PropagationModelTest extends TestCase {

    public void testDefaultPropagationModel() {

        // todo: actual tests

        FreeSpacePropagationModel model = new FreeSpacePropagationModel();
        TestRadio tx = new TestRadio(new Vector3f(0, 0, 1.3f), model);
        TestRadio rx = new TestRadio(new Vector3f(0, 0, 1.3f), model);

        model.setReceiveRadiusThreshold(400);
        model.setSimClock(new SimClockImpl());
        model.addRadio(tx);
        model.addRadio(rx);

        byte[] data = new byte[] {0x01, 0x02, 0x03, 0x04};

        // move the receiver along a line away from the transmitter
        for (float i = 0; i < 300; i += 0.1) {

            rx.setPosition(new Vector3f(i, 0, 1.3f));

            tx.transmit(data);
        }
    }


    public void testTwoRayPropagationModel() {

        // todo: actual tests

        Gnuplotter plotter = new Gnuplotter();

        plotter.setPlotParams("u 1:2 w l");
//        plotter.setProperty("log", "x");

        TwoRayPropagationModel model = new TwoRayPropagationModel();
        TestRadio tx = new TestRadio(new Vector3f(0, 0, 1.3f), model);
        TestRadio rx = new TestRadio(new Vector3f(0, 0, 1.3f), model);

        model.setReceiveRadiusThreshold(1000);
        model.setSimClock(new SimClockImpl());
        model.addRadio(tx);
        model.addRadio(rx);

        byte[] data = new byte[] {0x01, 0x02, 0x03, 0x04};

        // move the receiver along a line away from the transmitter
        for (float i = 0; i < 1000; i += 0.01) {

            rx.setPosition(new Vector3f(i, 0, 1.3f));

            tx.transmit(data);
        }

        double i = 0;

        for (double rxPower : rx.getReceivedData()) {

            plotter.addDataPoint(i + " " + rxPower);
            i += 0.01;
        }

        plotter.plot();
    }


    private static class SimClockImpl implements SimClock {

        @Override
        public double getCurrentTime() {
            return 0;
        }


        @Override
        public double getTimeStep() {
            return 0.1;
        }
    }
}
