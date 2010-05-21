package harvard.robobees.simbeeotic.configuration.variable;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * A looping variable that uses a random number generator to determine the value at each iteration. The generator can be
 * seeded and configured to return values between a certain range. The variable returns random double numbers only, not
 * integers. Numbers are uniformally distributed over the input range.
 *
 * @author bkate
 */
public class UniformRandomVariable extends RandomVariable {


    public UniformRandomVariable(String seed, String minValue, String maxValue,
                                 String numDraws, String firstDraw, boolean externalSeed) {

        super(seed, minValue, maxValue, numDraws, firstDraw, externalSeed);
    }


    /**
     * Gets a list of randomly generated values, using a generator configured to the input parameters.
     *
     * @return A list of output values (that can be parsed into a double value).
     */
    @Override
    protected List<String> calculateValues() throws VariableCalculationException {


        List<String> results = new ArrayList<String>();

        // required values
        if ((getMinValue() == null) || (getMaxValue() == null)) {
            throw new VariableCalculationException("'uniform-random' variable needs an upper and lower bound.");
        }

        int numDraws;

        try {
            numDraws = intParam(getNumDraws());
        }
        catch (VariableCalculationException e) {
            throw new VariableCalculationException("num-draws parameter is not optional", e);
        }

        int firstDraw = intParam(getFirstDraw(), 0);    //get first draw (how many to skip)

        /*
        * Min and max values are optional for normal random looping variables.  They allow outliers to be discarded.
        */
        double minValue = doubleParam(getMinValue());
        double maxValue = doubleParam(getMaxValue());

        long seed = longParam(getSeed());
        Random rand = new Random(seed);

        // swap upper and lower if need be
        if (minValue > maxValue) {
            double temp = minValue;
            minValue = maxValue;
            maxValue = temp;
        }

        double diff = maxValue - minValue;

        // generate numbers
        double value;

        for (int i = 0; i < numDraws + firstDraw - 1; i++) {

            value = minValue + rand.nextDouble() * diff;

            if (i >= firstDraw - 1) {     //firstDraw uses 1 based indexing.  i.e. firstDraw=1 should skip nothing
                results.add(Double.toString(value));
            }
        }

        return results;
    }

}

