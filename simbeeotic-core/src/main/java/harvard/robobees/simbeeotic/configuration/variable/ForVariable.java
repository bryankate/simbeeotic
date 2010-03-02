package harvard.robobees.simbeeotic.configuration.variable;


import harvard.robobees.simbeeotic.util.DocUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * A looping variable that uses a loop to generate values between a lower bound (inclusive) and an upper bound
 * (inclusive) with a given step size.
 *
 * @author bkate
 */
public class ForVariable extends AbstractLoopingVariable {

    private String lower = null;
    private String upper = null;
    private String step = null;


    /**
     * Default constructor that takes the necessary looping arguments.
     *
     * @param lower The lower bound value (may be a reference).
     * @param upper The upper bound value (may be a reference).
     * @param step The step size value (may be a reference).
     */
    public ForVariable(String lower, String upper, String step) {

        this.lower = lower;
        this.upper = upper;
        this.step = step;

        // check for references
        if ((lower != null) && DocUtils.isPlaceholder(lower)) {
            addDependency(DocUtils.extractPlaceholderName(lower));
        }

        if ((upper != null) && DocUtils.isPlaceholder(upper)) {
            addDependency(DocUtils.extractPlaceholderName(upper));
        }

        if ((step != null) && DocUtils.isPlaceholder(step)) {
            addDependency(DocUtils.extractPlaceholderName(step));
        }
    }


    /**
     * Calculates a list of values using a loop from the lower value (inclusive) to the upper value (inclusive)
     * separated by the amount of the step size parameter.
     *
     * @return A list of values representing the output for the current parameter values.
     */
    protected List<String> calculateValues() throws VariableCalculationException {

        List<String> results = new ArrayList<String>();

        // check for unset params
        if ((lower == null) || (upper == null) || (step == null)) {
            throw new VariableCalculationException("The lower, upper, and step params must be set on " +
                                                   "the 'for' looping variable.");
        }

        double lowerVal = 0;
        double upperVal = 0;
        double stepVal = 0;

        try {

            // get the lower bound
            if (DocUtils.isPlaceholder(lower)) {
                lowerVal = Double.parseDouble(getDependencyValue(DocUtils.extractPlaceholderName(lower)));
            }
            else {
                lowerVal = Double.parseDouble(lower);
            }

            // get the upper bound
            if (DocUtils.isPlaceholder(upper)) {
                upperVal = Double.parseDouble(getDependencyValue(DocUtils.extractPlaceholderName(upper)));
            }
            else {
                upperVal = Double.parseDouble(upper);
            }

            // get the step size
            if (DocUtils.isPlaceholder(step)) {
                stepVal = Double.parseDouble(getDependencyValue(DocUtils.extractPlaceholderName(step)));
            }
            else {
                stepVal = Double.parseDouble(step);
            }
        }
        catch (NumberFormatException nfe) {
            throw new VariableCalculationException("The lower, upper, and step size must be double values in " +
                                                   "the 'for' looping variable. (" +
                                                   lower + "," + upper + "," + step + ")", nfe);
        }

        if (stepVal == 0.0) {
            throw new VariableCalculationException("Step value cannot be zero in 'for' looping variable.");
        }

        // check ordering of arguments
        if (((lowerVal > upperVal) && (stepVal > 0.0)) ||
            ((lowerVal < upperVal) && (stepVal < 0.0))) {

            throw new VariableCalculationException("Imporperly ordered bounds in 'for' looping variable.");
        }

        // generate values
        for (double i = lowerVal; i <= upperVal; i += stepVal) {
            results.add(Double.toString(i));
        }

        return results;
    }
}

