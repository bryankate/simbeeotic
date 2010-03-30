package harvard.robobees.simbeeotic.configuration.variable;


import harvard.robobees.simbeeotic.util.DocUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * A looping variable that holds a single constant value. This is bit of a misnomer in that the value may not be
 * constant if it is in fact a reference to another variable that is changing, but it will always hold a single value at
 * any given time.
 *
 * @author bkate
 */
public class ConstantVariable extends AbstractLoopingVariable {

    protected String value = null;


    /**
     * Default constructor - takes the initial value string.
     *
     * @param value The value of the variable - may be a placeholder reference to another variable (but not null).
     */
    public ConstantVariable(String value) {

        this.value = value;

        addDepIfNeeded(value);
    }


    /**
     * Gets the value of the variable.
     *
     * @return A list containing one item - the current value of this variable.
     */
    protected List<String> calculateValues() throws VariableCalculationException {

        if (value == null) {
            throw new VariableCalculationException("The value of a 'constant' looping variable must be non-null.");
        }

        List<String> results = new ArrayList<String>();

        String val = value;

        // see if it needs to be resolved
        if (DocUtil.isPlaceholder(value)) {
            val = getDependencyValue(DocUtil.extractPlaceholderName(value));
        }

        results.add(val);

        return results;
    }
}

