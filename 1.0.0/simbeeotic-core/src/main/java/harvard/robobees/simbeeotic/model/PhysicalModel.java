package harvard.robobees.simbeeotic.model;


/**
 * A marker interface that combines the concept of a {@link PhysicalEntity} with
 * a {@link Model}. This makes it convenient to refer to models that have a physical
 * world presence (a very common thing) without casting.
 *
 * @author bkate
 */
public interface PhysicalModel extends PhysicalEntity, Model {

}
