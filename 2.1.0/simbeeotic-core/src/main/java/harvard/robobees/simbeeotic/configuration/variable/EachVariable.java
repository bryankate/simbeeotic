package harvard.robobees.simbeeotic.configuration.variable;


import java.util.ArrayList;
import java.util.List;


/**
 * A looping variable that allows a list of items to be specified. Each item in the list can be a variable reference.
 *
 * @author bkate
 */
public class EachVariable extends AbstractLoopingVariable {

    private List<String> picks = null;
    private String firstDraw = null;
    private String numDraws = null;


    /**
     * Default constructor - takes the list of pick values.
     *
     * @param picks The list of values for this variable to pick from - cannot be null, but may include references.
     */
    public EachVariable(List<String> picks, String firstDraw, String numDraws) {

        this.picks = picks;
        this.firstDraw = firstDraw;
        this.numDraws = numDraws;


        if (picks != null) {

            for (String pick : picks) {

                // look for dependencies
                addDepIfNeeded(pick);
            }
        }

        addDepIfNeeded(firstDraw);
        addDepIfNeeded(numDraws);
    }


    /**
     * Calculate the current list of values. This list may be constant if there are no dependencies.
     *
     * @return The list of values that this variable represents.
     */
    @Override
    protected List<String> calculateValues() throws VariableCalculationException {

        if ((picks == null) || picks.isEmpty()) {
            throw new VariableCalculationException("There must be at least one value in the 'each' looping variable.");
        }

        List<String> results = new ArrayList<String>();

        int firstDraw = intParam(this.firstDraw, 1);
        int numDraws = intParam(this.numDraws, -1);

        int i = 0;
        int numDrawn = 0;
        for (String pick : picks) {
            if (i < firstDraw - 1) {
                continue;
            }


            // resolve any dependencies in the list
            results.add(stringParam(pick));


            if (numDraws != -1 && numDrawn >= numDraws) {
                break;
            }
        }

        return results;
    }
}

