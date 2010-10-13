package harvard.robobees.simbeeotic.component;


import com.google.inject.Inject;

import javax.swing.*;
import java.awt.*;
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

    
    @Override
    public void initialize() {

        Dimension size = new Dimension(900, 600);

        world = new Java3DWorld();

        ControlPanel control = new ControlPanel(world, context.getClockControl(), context.getSimEngine());
        JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, world, control);

        context.getRecorder().addListener(world);

        pane.setPreferredSize(size);
        pane.setResizeWeight(1.0);

        setTitle("3D Visulaization");
        setSize(size);
        setContentPane(pane);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);

        pane.setDividerLocation(0.7);
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
