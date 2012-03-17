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


import harvard.robobees.simbeeotic.ClockControl;
import harvard.robobees.simbeeotic.ClockListener;
import harvard.robobees.simbeeotic.SimEngine;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.PhysicalEntity;

import javax.media.j3d.Transform3D;
import javax.swing.*;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


/**
 * A panel that provides some controls for the 3D world view.
 *
 * @author bkate
 */
public class ControlPanel extends JPanel {

    private ClockControl clock;
    private SimEngine simEngine;
    private ViewPanel view3D;

    private static final Point3d DEFAULT_VIEW = new Point3d(-20, 12, 20);
    private static final Point3d UPPER_LEFT_VIEW = new Point3d(-20, 8, -20);
    private static final Point3d UPPER_RIGHT_VIEW = new Point3d(20, 12, -20);
    private static final Point3d TOP_VIEW = new Point3d(0, 50, 0.01);
    private static final Point3d LOOK_AT = new Point3d(0, 0, 0);
    private static final Vector3d UP = new Vector3d(0, 1, 0);


    public ControlPanel(ViewPanel world, ClockControl clockControl, SimEngine engine) {

        view3D = world;
        clock = clockControl;
        simEngine = engine;

        init();
    }


    private void init() {

        setLayout(new GridBagLayout());

        final JComboBox objectList = new JComboBox();

        // clock control
        JPanel timePanel = new JPanel();
        final JTextField simTime = new JTextField(5);
        final JButton pause = new JButton("pause");

        timePanel.setBorder(BorderFactory.createTitledBorder("Clock Control"));

        // a little structure to make the object list prettier without string parsing
        class ObjectInfo {

            public int id;
            public String type;

            public ObjectInfo(int id, String type) {

                this.id = id;
                this.type = type;
            }

            @Override
            public String toString() {
                return id + " (" + type + ")";
            }
        }

        clock.addListener(new ClockListener() {

            private boolean firstTime = true;

            @Override
            public void clockUpdated(SimTime time) {

                simTime.setText(time.getImpreciseTime() + " s");

                if (firstTime) {

                    List<PhysicalEntity> entities = simEngine.findModelsByType(PhysicalEntity.class);

                    for (PhysicalEntity e : entities) {
                        objectList.addItem(new ObjectInfo(e.getObjectId(), e.getClass().getSimpleName()));
                    }

                    firstTime = false;
                }
            }
        });

        pause.setAlignmentX(Component.CENTER_ALIGNMENT);
        pause.addActionListener(new ActionListener() {

            private boolean paused = false;

            @Override
            public void actionPerformed(ActionEvent e) {

                if (!paused) {

                    clock.pause();
                    pause.setText("start");
                }
                else {

                    clock.start();
                    pause.setText("pause");
                }

                paused = !paused;
            }
        });

        timePanel.add(simTime);
        timePanel.add(pause);

        // object view
        JPanel objectViewPanel = new JPanel();

        objectViewPanel.setBorder(BorderFactory.createTitledBorder("Object View"));

        objectList.insertItemAt("Choose Object", 0);
        objectList.setSelectedIndex(0);
        objectList.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                view3D.spawnObjectView(((ObjectInfo)objectList.getSelectedItem()).id);
            }
        });

        objectViewPanel.add(objectList);

        // label controls
        JPanel labelPanel = new JPanel();
        final JButton labels = new JButton("off");

        labels.addActionListener(new ActionListener() {

            private boolean labelsOn = true;
            
            @Override
            public void actionPerformed(ActionEvent e) {

                if (labelsOn) {
                    labels.setText("on");
                }
                else {
                    labels.setText("off");
                }

                labelsOn = !labelsOn;
                view3D.setLabelsVisible(labelsOn);
            }
        });

        labelPanel.setBorder(BorderFactory.createTitledBorder("Labels"));
        labelPanel.add(labels);

        // main window view controls
        JPanel viewPanel = new JPanel(new BorderLayout());
        JButton save = new JButton("save");
        JButton load = new JButton("load");

        save.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                JFileChooser fc = new JFileChooser();
                int returnVal = fc.showSaveDialog(ControlPanel.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {

                    File file = fc.getSelectedFile();
                    String filePath = file.getAbsolutePath();

                    try {

                        FileWriter f = new FileWriter(filePath+".txt");

                        f.write("" + view3D.getMainViewTransform());
                        f.close();
                    }
                    catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        load.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                JFileChooser fc = new JFileChooser();
                int returnVal = fc.showOpenDialog(ControlPanel.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {

                    File file = fc.getSelectedFile();
                    String filePath = file.getAbsolutePath();

                    try {

                        BufferedReader br = new BufferedReader(new FileReader(filePath));
                        String strLine;

                        String[] tokens = null;
                        double[] values = new double[16];
                        int count = 0;

                        while((strLine = br.readLine()) != null)   {

                            tokens = strLine.split(",");

                            for (int i = 0; i < tokens.length; i++) {
                                values[count+i] = new Double(tokens[i]).doubleValue();
                            }

                            count += 4;
                        }

                        br.close();

                        Transform3D t3d = new Transform3D();
                        t3d.set(values);

                        view3D.setMainViewTransform(t3d);
                    }
                    catch (FileNotFoundException fnf) {
                        fnf.printStackTrace();
                    }
                    catch (IOException ioe) {
                       ioe.printStackTrace();
                    }
                }
            }
        });

        final String[] views = {"Upper Left", "Upper Right", "Top View", "Default View"};
        final JComboBox viewList = new JComboBox(views);

        viewList.insertItemAt("Select a View", 0);
        viewList.setSelectedIndex(0);
        viewList.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (viewList.getSelectedItem().equals(views[0])) {
                    view3D.setMainView(UPPER_LEFT_VIEW, LOOK_AT, UP);
                }
                else if (viewList.getSelectedItem().equals(views[1])) {
                    view3D.setMainView(UPPER_RIGHT_VIEW, LOOK_AT, UP);
                }
                else if (viewList.getSelectedItem().equals(views[2])) {
                    view3D.setMainView(TOP_VIEW, LOOK_AT, UP);
                }
                else if (viewList.getSelectedItem().equals(views[3])) {
                    view3D.setMainView(DEFAULT_VIEW, LOOK_AT, UP);
                }
                else {
                    view3D.setMainView(DEFAULT_VIEW, LOOK_AT, UP);
                }
            }
        });

        JPanel subPanel = new JPanel();

        subPanel.add(save);
        subPanel.add(load);

        viewPanel.setBorder(BorderFactory.createTitledBorder("Custom View"));
        viewPanel.add(viewList, BorderLayout.NORTH);
        viewPanel.add(subPanel, BorderLayout.SOUTH);


        // populate
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;

        c.gridx = 0;
        c.gridy = 0;
        add(timePanel, c);

        c.gridx = 0;
        c.gridy = 1;
        add(objectViewPanel,c);

//        c.gridx = 0;
//        c.gridy = 0;
//        add(labelPanel, c);

        c.gridx = 0;
        c.gridy = 2;
        add(viewPanel, c);

        // fill up the rest of the space
        c.gridx = 0;
        c.gridy = 3;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        add(new JPanel(), c);
    }
}
