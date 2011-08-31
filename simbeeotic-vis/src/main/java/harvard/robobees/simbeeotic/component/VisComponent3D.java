package harvard.robobees.simbeeotic.component;


import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;


/**
 * A top level component that provides a 3D world view and controls.
 *
 * @author bkate
 */
public class VisComponent3D extends JFrame implements VariationComponent {

    private Java3DWorld world;

    @Inject
    private VariationContext context;

    @Inject(optional=true)
    @Named("use-background")
    private boolean useBackground = true;


    @Inject(optional=true)
    @Named("show-controls")
    private boolean showControls = true;


    @Inject(optional=true)
    @Named("testbed-overhead-view")
    private boolean testbedOverheadView = false;


    @Inject(optional=true)
    @Named("testbed-angle-view")
    private boolean testbedAngleView = false;


    @Inject(optional=true)
    @Named("second-screen")
    private boolean secondScreen = false;

    
    @Override
    public void initialize() {

        Dimension size = new Dimension(900, 600);
        JComponent contentPane;

        world = new Java3DWorld(useBackground);

        if (showControls) {

            ControlPanel control = new ControlPanel(world, context.getClockControl(), context.getSimEngine());
            JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, world, control);

            pane.setPreferredSize(size);
            pane.setResizeWeight(1.0);

            pack();

            pane.setDividerLocation(0.7);

            contentPane = pane;
        }
        else {
            contentPane = world;
        }

        if (testbedOverheadView) {
            world.setMainView(new Point3d(0, 9, 0), new Point3d(0, 0, 0), new Vector3d(0, 0, -1));
        }
        else if (testbedAngleView) {
            world.setMainView(new Point3d(-6, 2, 6), new Point3d(0, 1, 0), new Vector3d(0, 1, 0));
        }

        context.getRecorder().addListener(world);

        setTitle("3D Visualization");
        setContentPane(contentPane);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (secondScreen) {

            setLocation(1680, 0);

            if (testbedOverheadView) {

                size = new Dimension(960, 600);
            }
            else if (testbedAngleView) {

                size = new Dimension(960, 480);
                setLocation(1680, 600);
            }
        }

        setSize(size);
        setVisible(true);
    }


    @Override
    public void shutdown() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }


    @Override
    public void dispose() {

        world.dispose();
        super.dispose();
    }
}
