package harvard.robobees.simbeeotic.model.comms;


import static harvard.robobees.simbeeotic.environment.PhysicalConstants.SPEED_OF_LIGHT;


/**
 * Uses the two ray (aka plane Earth) model for determining path loss
 * between two radios. This model can be used when modeling outdoor,
 * open space scenarios. The model determines the received power
 * by combining a direct line-of-sight component with a component
 * derived from a ground reflection.
 *
 * <ul>
 *   <li>http://people.seas.harvard.edu/~jones/es151/prop_models/propagation.html#pel</li>
 *   <li>http://www.google.com/url?sa=t&source=web&ct=res&cd=1&ved=0CAYQFjAA&url=http%3A%2F%2Fwww.winlab.rutgers.edu%2F~narayan%2FCourse%2FWSID%2FLectures02%2Flect1.pdf&ei=MALFS-XNC4P58AaiiKGyDw&usg=AFQjCNG0gLtJMXugkb1hZ31Hj9_hcFn4gA&sig2=tjKdtkY6wyUJv09M8dwc-Q</li>
 * </ul>
 *
 * @author bkate
 */
public class TwoRayPropagationModel extends AbstractPropagationModel {

    private static final double MIN_DISTANCE = 0.01;


    /**
     * {@inheritDoc}
     *
     * Uses a two ray model to calculate the path loss between the two radios.
     */
    @Override
    protected double calculatePathLoss(Radio tx, Radio rx, double txPower, Band band, double distance) {

        double ht = tx.getPosition().z;  // m
        double hr = rx.getPosition().z;  // m
        double lambda = SPEED_OF_LIGHT / (band.getCenterFrequency() * 10e6);  // m

        // crossover distance
        double dc = (4 * Math.PI * ht * hr) / lambda;

        double rxPower = getReceivedPower(tx, rx, txPower);  // Pt * Gt * Gr
        double loss;

        if (distance < MIN_DISTANCE) {
            loss = Math.pow(distance + 1, 2);
        }
        else if (distance <= dc) {
            
            loss = 4 * Math.pow(Math.sin((2 * Math.PI * ht * hr) / (lambda * distance)), 2) *
                   Math.pow(lambda / (4 * Math.PI * distance), 2);
        }
        else {

            // past the crossover distance the signal degrades with fourth inverse power law
            loss = Math.pow(ht * hr, 2) / Math.pow(distance, 4);
        }

        // todo: random degradation of signal?

        return rxPower + 10 * Math.log10(loss);
    }
}
