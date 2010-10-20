package harvard.robobees.simbeeotic.component;


import harvard.robobees.simbeeotic.model.MotionRecorder;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.configuration.Variation;
import harvard.robobees.simbeeotic.ClockControl;
import harvard.robobees.simbeeotic.SimEngine;

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
    @Named("variation")
    private Variation variation;

    @Inject
    @GlobalScope
    private MotionRecorder recorder;

    @Inject(optional = true)
    @GlobalScope
    private ClockControl clockControl;

    @Inject(optional = true)
    @GlobalScope
    private SimEngine simEngine;


    /**
     * Gets the scenario variation number.
     *
     * @return The number of the variation of this scenario.
     */
    public final int getVariationNum() {
        return variationNum;
    }


    /**
     * Gets the scenario variation details.
     *
     * @return The details of this scenario variation.
     */
    public final Variation getVariation() {
        return variation;
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


    /**
     * Gets the simulation engine, which can be used to find models.
     *
     * @return The simulation engine in use for this variation.
     */
    public SimEngine getSimEngine() {
        return simEngine;
    }
}
