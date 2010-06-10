package harvard.robobees.simbeeotic.component;


/**
 * An interface to be implemented by components that are associated with a single variation
 * of a scenario. 
 *
 * @author bkate
 */
public interface VariationComponent {

    /**
     * Initializes the component.
     */
    public void initialize();


    /**
     * Called when the component is to be shutdown (when the scenario variation has completed).
     */
    public void shutdown();
}
