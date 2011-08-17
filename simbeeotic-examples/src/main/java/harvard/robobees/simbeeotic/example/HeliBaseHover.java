package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.model.BaseHeliBehavior;
import harvard.robobees.simbeeotic.model.Boundary;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.Platform;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;


/**
 * An example of using the base helicopter behavior to script a simple flight pattern.
 *
 * @author bkate
 */
public class HeliBaseHover extends BaseHeliBehavior {
    
    private static Logger logger = Logger.getLogger(HeliBaseHover.class);


    @Override
    public void start(Platform platform, final HeliControl control, final Boundary bounds) {

        super.start(platform, control, bounds);

        moveToPoint(0, 0, 1, 0.2,
        			new MoveCallback() {

						@Override
						public void reachedDestination() {
							hover(new Vector3f(0, 0, 1));
						}
        			});
    }
}
