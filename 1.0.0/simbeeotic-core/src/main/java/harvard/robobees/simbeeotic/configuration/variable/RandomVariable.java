package harvard.robobees.simbeeotic.configuration.variable;


public abstract class RandomVariable extends AbstractLoopingVariable {

    private String minValue = null;
    private String maxValue = null;
    private String seed = null;
    private String numDraws = null;
    private String firstDraw = null;
    private boolean externallySeeded = false;


    /**
     * Default constructor - takes all the parameters needed to customize the generator.
     */
    public RandomVariable(String seed, String minValue, String maxValue, String numDraws, String firstDraw, boolean extSeed) {

        this.externallySeeded = extSeed;

        this.minValue = minValue;
        addDepIfNeeded(minValue);

        this.maxValue = maxValue;
        addDepIfNeeded(maxValue);

        this.seed = seed;

        if (!externallySeeded) {
            addDepIfNeeded(seed);
        }

        if (numDraws == null || "".equals(numDraws)) {      //guard
            throw new VariableCalculationException("num draws must be specified for all random looping variables, was null");
        }
        this.numDraws = numDraws;
        addDepIfNeeded(numDraws);

        this.firstDraw = firstDraw;
        addDepIfNeeded(firstDraw);

    }


    public void setSeed(String seed) {

        if (!externallySeeded) {

            throw new RuntimeException("The random variable '" + getName() + "' does not " +
                                       "have a RANDOM_STREAM seed source and cannot be re-seeded.");
        }

        // no need to check for dependencies here because
        // the new seed is drawn from the master RNG and is
        // not a placeholder
        this.seed = seed;

        // make sure that values are re-calculated after
        // the variable is re-seeded
        setDirty(true);
    }


    protected String getSeed() {
        return seed;
    }


    protected String getNumDraws() {
        return numDraws;
    }


    protected String getFirstDraw() {
        return firstDraw;
    }


    protected String getMinValue() {
        return minValue;
    }


    protected String getMaxValue() {
        return maxValue;
    }


    public boolean isExternallySeeded() {
        return externallySeeded;
    }
}

