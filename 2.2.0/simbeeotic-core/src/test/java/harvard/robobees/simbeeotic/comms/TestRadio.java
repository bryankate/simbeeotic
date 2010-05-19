package harvard.robobees.simbeeotic.comms;


import harvard.robobees.simbeeotic.util.MathUtil;

import javax.vecmath.Vector3f;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.LinkedList;


/**
 * @author bkate
 */
public class TestRadio implements Radio {

    private Vector3f position;
    private Vector3f pointing = new Vector3f(0, 0, 1);
    private AntennaPattern pattern = new IsotropicAntenna();
    private PropagationModel propModel;

    private List<Double> points = new LinkedList<Double>();

    private static Logger logger = Logger.getLogger(PropagationModelTest.class);


    public TestRadio(Vector3f pos, PropagationModel prop) {

        position = pos;
        propModel = prop;
    }


    @Override
    public Vector3f getPointing() {
        return pointing;
    }


    @Override
    public Vector3f getPosition() {
        return position;
    }


    public void setPosition(Vector3f pos) {
        position = pos;
    }


    @Override
    public AntennaPattern getAntennaPattern() {
        return pattern;
    }


    @Override
    public void receive(double time, byte[] data, double rxPower, double frequency) {

        points.add(rxPower);

        logger.debug("received message wih power: " + rxPower + " dBm ( " + MathUtil.dbmToMw(rxPower) + " mW )");
    }


    @Override
    public void transmit(byte[] data) {
        propModel.transmit(this, data, -25, new Band(2405, 5));
    }


    @Override
    public Band getOperatingBand() {
        return new Band(2442.5, 85);
    }

    public List<Double> getReceivedData() {
        return points;
    }
}
