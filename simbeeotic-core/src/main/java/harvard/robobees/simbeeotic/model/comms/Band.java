package harvard.robobees.simbeeotic.model.comms;


/**
 * A simple container class for frequency bands.
 *
 * @author bkate
 */
public class Band {

    private double centerFreq;
    private double bandwidth;


    public Band(double center, double width) {

        centerFreq = center;
        bandwidth = width;
    }


    /**
     * Gets the center frequency of this band.
     *
     * @return The center frequency (in MHz).
     */
    public double getCenterFrequency() {
        return centerFreq;
    }


    /**
     * Gets the width of this band.
     *
     * @return The bandwidth (in MHz).
     */
    public double getBandwidth() {
        return bandwidth;
    }


    /**
     * Determines if a given frequency is in this band.
     *
     * @param frequency The frequency in question.
     *
     * @return True if the given frequency is in the band, false otherwise.
     */
    public boolean isInBand(double frequency) {

        double halfWidth = bandwidth / 2;

        return (((frequency >= (centerFreq - halfWidth)) && frequency <= (centerFreq + halfWidth)));
    }


    /**
     * Determines if this band contains another band.
     *
     * @param other The otherband to consider.
     *
     * @return True if this band contains the other, false otherwise.
     */
    public boolean contains(Band other) {

        double halfWidth = bandwidth / 2;
        double otherHalfWidth = other.getBandwidth() / 2;

        return (((other.getCenterFrequency() - otherHalfWidth) >= (centerFreq - halfWidth)) &&
                ((other.getCenterFrequency() + otherHalfWidth) <= (centerFreq + halfWidth)));
    }


    /**
     * Determines if two bands overlap.
     *
     * @param other The otherband to consider.
     *
     * @return True if the other band overlaps this band, false otherwise.
     */
    public boolean overlaps(Band other) {

        double halfWidth = bandwidth / 2;
        double otherHalfWidth = other.getBandwidth() / 2;

        if (contains(other) || other.contains(this)) {
            return true;
        }

        return (((other.getCenterFrequency() - otherHalfWidth) <= (centerFreq + halfWidth)) ||
                ((other.getCenterFrequency() + otherHalfWidth) >= (centerFreq - halfWidth)));
    }


    /**
     * {@inheritDoc}
     *
     * auto-generated
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Band band = (Band)o;

        if (Double.compare(band.bandwidth, bandwidth) != 0) {
            return false;
        }

        if (Double.compare(band.centerFreq, centerFreq) != 0) {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     *
     * auto-generated
     */
    @Override
    public int hashCode() {

        int result;
        long temp;

        temp = centerFreq != +0.0d ? Double.doubleToLongBits(centerFreq) : 0L;
        result = (int) (temp ^ (temp >>> 32));

        temp = bandwidth != +0.0d ? Double.doubleToLongBits(bandwidth) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));

        return result;
    }
}
