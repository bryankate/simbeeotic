package harvard.robobees.simbeeotic.model;


/**
 * @author bkate
 */
public interface Model extends PhysicalEntity {

    public void initialize();

    public void update(final double currTime);
}
