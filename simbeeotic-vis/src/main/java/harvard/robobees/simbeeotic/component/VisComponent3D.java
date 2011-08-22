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
    @Named("testbed-view")
    private boolean testbedView = false;


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

        if (testbedView) {
            world.setMainView(new Point3d(0, 8, 0), new Point3d(0, 0, 0), new Vector3d(1, 0, 0));
        }

        context.getRecorder().addListener(world);

        setTitle("3D Visualization");
        setContentPane(contentPane);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (secondScreen) {

            size = new Dimension(1024, 1040);
            setLocation(1715, 20);
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
