package harvard.robobees.simbeeotic.example;


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.BaseHeliBehavior;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.Platform;
import harvard.robobees.simbeeotic.model.Timer;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;


/**
 * An example of using the base helicopter behavior to script a simple flight pattern.
 *
 * @author bkate
 */
public class HeliBaseHover extends BaseHeliBehavior
{
    private static Logger logger = Logger.getLogger(HeliBaseHover.class);


    @Override
    public void start(Platform platform, final HeliControl control)
    {
        super.start(platform, control);

        moveToPoint(0, 0, 1, 0.2,
        			new MoveCallback()
        			{
						@Override
						public void reachedDestination()
						{
							hover(new Vector3f(0, 0, 1));
						}
        			});
    }
}
