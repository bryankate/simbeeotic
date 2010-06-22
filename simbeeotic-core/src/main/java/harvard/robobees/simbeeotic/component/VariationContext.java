package harvard.robobees.simbeeotic.component;


import harvard.robobees.simbeeotic.model.MotionRecorder;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.ClockControl;

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * The context in which {@link VariationComponent}s are executed.
 *
 * @author bkate
 */
public class VariationContext {

    @Inject
    @Named("variation-number")
    private int variationNum;

    @Inject
    @Named("variables")
    private Map<String, String> variables;

    @Inject
    @GlobalScope
    private MotionRecorder recorder;

    @Inject
    @GlobalScope
    private ClockControl clockControl;


    /**
     * Gets the scenario variation number.
     *
     * @return The number of the variation of this scenario.
     */
    public final int getVariationNum() {
        return variationNum;
    }


    /**
     * Gets the set of variables that are used in this scenario variation.
     *
     * @return The variable map for this variation.
     */
    public final Map<String, String> getVariables() {
        return variables;
    }


    /**
     * Gets the motion recorder, which can be used to obtain motion updates for objects in the simulation.
     *
     * @return The motion recorder.
     */
    public final MotionRecorder getRecorder() {
        return recorder;
    }


    /**
     * Gets the class that controls the state of the simulation clock.
     *
     * @return The clock control class for this variation.
     */
    public final ClockControl getClockControl() {
        return clockControl;
    }
}
