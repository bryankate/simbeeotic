package harvard.robobees.simbeeotic.comms;


import junit.framework.TestCase;
import harvard.robobees.simbeeotic.SimClock;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class PropagationModelTest extends TestCase {

    public void testDefaultPropagationModel() {

        // todo: actual tests

        FreeSpacePropagationModel model = new FreeSpacePropagationModel();
        TestRadio tx = new TestRadio(new Vector3f(0, 0, 1), model);
        TestRadio rx = new TestRadio(new Vector3f(0, 0, 1), model);

        model.setSimClock(new SimClockImpl());
        model.addRadio(tx);
        model.addRadio(rx);

        byte[] data = new byte[] {0x01, 0x02, 0x03, 0x04};

        // move the receiver along a line away from the transmitter
        for (float i = 0; i < 20; i += 0.1) {

            rx.setPosition(new Vector3f(i, 0, 1));

            tx.transmit(data);
        }
    }


    public void testTwoRayPropagationModel() {

        // todo: actual tests

        TwoRayPropagationModel model = new TwoRayPropagationModel();
        TestRadio tx = new TestRadio(new Vector3f(0, 0, 1), model);
        TestRadio rx = new TestRadio(new Vector3f(0, 0, 1), model);

        model.setSimClock(new SimClockImpl());
        model.addRadio(tx);
        model.addRadio(rx);

        byte[] data = new byte[] {0x01, 0x02, 0x03, 0x04};

        // move the receiver along a line away from the transmitter
        for (float i = 0; i < 20; i += 0.1) {

            rx.setPosition(new Vector3f(i, 0, 1));

            tx.transmit(data);
        }
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
