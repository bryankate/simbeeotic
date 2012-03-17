/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
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
