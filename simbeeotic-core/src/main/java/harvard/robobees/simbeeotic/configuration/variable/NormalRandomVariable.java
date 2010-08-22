package harvard.robobees.simbeeotic.configuration.variable;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * A looping variable that uses a random number generator to determine the value at each iteration. The generator can be
 * seeded and configured to return values that are generated using a Gaussian distribution around the given mean. The
 * variable returns random double numbers only, not integers.
 *
 * @author bkate
 */
public class NormalRandomVariable extends RandomVariable {

    private String mean;
    private String stdDev;


    public NormalRandomVariable(String seed, String minValue, String maxValue,
                                String numDraws, String firstDraw, String mean, String stdDev, boolean externalSeed) {

        super(seed, minValue, maxValue, numDraws, firstDraw, externalSeed);

        this.mean = mean;
        addDepIfNeeded(mean);

        this.stdDev = stdDev;
        addDepIfNeeded(stdDev);
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
        if (mean == null) {
            throw new VariableCalculationException("'normal-random' variable needs a mean value.");
        }

        if (stdDev == null) {
            throw new VariableCalculationException("'normal-random' variable needs a mean value.");
        }

        int numDraws;
        try {
            numDraws = intParam(getNumDraws());
        }
        catch (VariableCalculationException e) {
            throw new VariableCalculationException("num-draws parameter is not optional", e);
        }

        int firstDraw = intParam(getFirstDraw(), 1);    //get first draw (how many to skip)

        double meanVal;
        try {
            meanVal = doubleParam(this.mean);
        }
        catch (VariableCalculationException e) {
            throw new VariableCalculationException("mean parameter is not optional", e);
        }

        double stdDevVal;
        try {
            stdDevVal = doubleParam(this.stdDev);
        }
        catch (VariableCalculationException e) {
            throw new VariableCalculationException("std-dev parameter is not optional", e);
        }

        /*
        * Min and max values are optional for normal random looping variables.  They allow outliers to be discarded.
        */
        double minValue = Double.MIN_VALUE;
        boolean useMinValue = false;

        try {
            minValue = doubleParam(getMinValue());
            useMinValue = true;
        }
        catch (VariableCalculationException e) {
            //We can drop this as useMinValue will stay false
        }

        double maxValue = Double.MAX_VALUE;
        boolean useMaxValue = false;

        try {
            maxValue = doubleParam(getMaxValue());
            useMaxValue = true;
        }
        catch (VariableCalculationException e) {
            //We can drop this as useMinValue will stay false
        }


        long seed = longParam(getSeed());
        Random rand = new Random(seed);

        // generate numbers
        for (int i = 0; i < numDraws + firstDraw - 1; i++) {      //we will always want numDraws results, but skip the first (firstDraw-1); results

            double value;
            do {
                value = meanVal + (rand.nextGaussian() * stdDevVal);
            }
            while ((useMaxValue && value > maxValue) || (useMinValue && value < minValue));

            if (i >= firstDraw - 1) {     //firstDraw uses 1 based indexing.  i.e. firstDraw=1 should skip nothing
                results.add(Double.toString(value));
            }
        }

        return results;
    }

}

