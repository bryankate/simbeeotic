package harvard.robobees.simbeeotic.component;


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.ConeShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.linearmath.Transform;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;
import harvard.robobees.simbeeotic.model.MotionListener;
import org.apache.log4j.Logger;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Material;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import java.awt.Image;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A panel that uses Java3D to display a 3D world view.
 *
 * @author dcai
 * @author bkate
 */
public class Java3DWorld extends JPanel implements MotionListener {

    private SimpleUniverse universe;
    private TransformGroup rootTransformGroup;

    private Map<Integer, TransformGroup> transformMap = new HashMap<Integer, TransformGroup>();
    private Map<Integer, Appearance> appearanceMap = new HashMap<Integer, Appearance>();
    private Map<Integer, ObjectView> objectViewMap = new ConcurrentHashMap<Integer, ObjectView>();

    private static Logger logger = Logger.getLogger(Java3DWorld.class);

    private static final Color3f BLACK = new Color3f(Color.BLACK);
    private static final Color3f SPECULAR = new Color3f(new Color(0.9f, 0.9f, 0.9f));
    private static final Color3f DEFAULT_COLOR = new Color3f(Color.LIGHT_GRAY);
    private static final float SHININESS = 25;

    private static final double BOUNDS = 1000;

    private static final Point3d ORIGIN = new Point3d(0, 0, 0);
    private static final Point3d DEFAULT_VIEW = new Point3d(-20, 12, 20);
    private static final Vector3d UP = new Vector3d(0, 1, 0);


    public Java3DWorld() {
        initWorld();
    }


    private void initWorld() {

        BranchGroup rootBranchgroup = new BranchGroup();
        BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), BOUNDS);

        Canvas3D canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        canvas.setFocusable(true);

        universe = new SimpleUniverse(canvas);
        universe.getViewer().getView().setBackClipDistance(BOUNDS);

        // lights
        AmbientLight ambientLightNode = new AmbientLight(new Color3f(Color.WHITE));
        ambientLightNode.setInfluencingBounds(bounds);

        Vector3f light1Direction = new Vector3f(-1.0f, -1.0f, -1.0f);
        Vector3f light2Direction  = new Vector3f(1.0f, -1.0f, 1.0f);

        DirectionalLight light1 = new DirectionalLight(new Color3f(Color.WHITE), light1Direction);
        DirectionalLight light2 = new DirectionalLight(new Color3f(Color.WHITE), light2Direction);

        light1.setInfluencingBounds(bounds);
        light2.setInfluencingBounds(bounds);

        rootBranchgroup.addChild(ambientLightNode);
        rootBranchgroup.addChild(light1);
        rootBranchgroup.addChild(light2);

        // background
        Background back = new Background();

        back.setColor(new Color3f(Color.BLUE));
        back.setApplicationBounds(bounds);
        rootBranchgroup.addChild(back);

        // todo: draw axes

        // root transform that makes Z axis up (as in Simbeeotic)
        Transform3D t3d = new Transform3D();
        t3d.rotX(-Math.PI / 2);

        rootTransformGroup = new TransformGroup(t3d);

        rootTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        rootTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        rootTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);

        rootBranchgroup.addChild(rootTransformGroup);
        rootBranchgroup.compile();

        // view and controls
        setMainView(DEFAULT_VIEW, ORIGIN, UP);

        OrbitBehavior orbit = new OrbitBehavior(canvas, OrbitBehavior.REVERSE_ALL);
        orbit.setSchedulingBounds(bounds);

        universe.getViewingPlatform().setViewPlatformBehavior(orbit);
        universe.addBranchGraph(rootBranchgroup);

        // add canvas to the panel
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
    }


    @Override
    public void initializeObject(int objectId, CollisionShape shape) {

        TransformGroup tg = null;

        if (shape instanceof SphereShape) {
            tg = createSphere(objectId, ((SphereShape)shape).getRadius());
        }
        else if (shape instanceof CylinderShape) {

            CylinderShape c = (CylinderShape)shape;

            float r = c.getRadius();
            Vector3f halfExtents = new Vector3f();
            c.getHalfExtentsWithMargin(halfExtents);

            tg = createCylinder(objectId, r, 2 * Math.abs(halfExtents.z));
        }
        else if (shape instanceof BoxShape) {

            BoxShape b = (BoxShape)shape;

            Vector3f halfExtents = new Vector3f();
            b.getHalfExtentsWithMargin(halfExtents);

            tg = createBox(objectId, Math.abs(halfExtents.x), Math.abs(halfExtents.y), Math.abs(halfExtents.z));
        }
        else if (shape instanceof ConeShape) {

            ConeShape c = (ConeShape) shape;

            float r = c.getRadius();
            float h = c.getHeight();

            tg = createCone(objectId, r, h);
        }
        else if (shape instanceof StaticPlaneShape){

            StaticPlaneShape s = (StaticPlaneShape)shape;

            Vector3f normal = new Vector3f();
            s.getPlaneNormal(normal);

            tg = createBox(objectId, (float)BOUNDS, (float)BOUNDS, 0);
        }
        else if (shape instanceof CompoundShape){
            tg = createCompoundShape(objectId, (CompoundShape)shape);
        }
        else {
            logger.warn("Object not recognized!");
        }

        if (tg != null) {

            BranchGroup bg = new BranchGroup();

            bg.addChild(tg);
            rootTransformGroup.addChild(bg);
        }
    }


    @Override
    public void stateUpdate(int objectId, Vector3f position, Quat4f orientation) {

        TransformGroup group = transformMap.get(objectId);

        Transform3D t3D = new Transform3D();
        group.getTransform(t3D);

        t3D.setRotation(orientation);
        t3D.setTranslation(position);

        group.setTransform(t3D);

        // see if this object is the center of a custom view window
        ObjectView view = objectViewMap.get(objectId);

        if (view != null) {
            view.update(t3D);
        }
    }


    @Override
    public void metaUpdate(int objectId, Color color, Image texture, String label) {

        if (color != null) {

            Color3f col = new Color3f(color);
            float alpha = 1 - color.getAlpha() / 255;

            Appearance appear = appearanceMap.get(objectId);
            TransparencyAttributes ta = new TransparencyAttributes();

            if (alpha != 0) {
                ta.setTransparencyMode(TransparencyAttributes.BLENDED);
            }
            else {
                ta.setTransparencyMode(TransparencyAttributes.NONE);
            }

            ta.setTransparency(alpha);

            appear.setMaterial(new Material(col, BLACK, col, SPECULAR, SHININESS));
            appear.setTransparencyAttributes(ta);
        }

        // todo: texture

        // todo: labels
    }


    private TransformGroup createSphere(int objectId, float radius) {

        Appearance appear = createDefaultAppearance();
        TransformGroup tg = new TransformGroup();

        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        tg.addChild(new Sphere(radius, appear));

        transformMap.put(objectId, tg);
        appearanceMap.put(objectId, appear);

        return tg;
    }


    private TransformGroup createBox(int objectId, float l, float w, float h) {

        Appearance appear = createDefaultAppearance();
        TransformGroup tg = new TransformGroup();

        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        tg.addChild(new Box(l, w, h, appear));

        transformMap.put(objectId, tg);
        appearanceMap.put(objectId, appear);

        return tg;
    }


    private TransformGroup createCone(int objectId, float r, float h) {

        Appearance appear = createDefaultAppearance();
        TransformGroup tg = new TransformGroup();

        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        tg.addChild(new Cone(r, h, appear));

        transformMap.put(objectId, tg);
        appearanceMap.put(objectId, appear);

        return tg;
    }


    private TransformGroup createCylinder(int objectId, float r, float h) {

        Appearance appear = createDefaultAppearance();
        TransformGroup tg = new TransformGroup();

        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        // cylinders in Java3D are about a different axis than JBullet,
        // so wrap them in an extra TG so that they can be rotated properly
        Transform3D t3d = new Transform3D();
        t3d.rotX(-Math.PI / 2);

        TransformGroup innerTg = new TransformGroup(t3d);

        innerTg.addChild(new Cylinder(r, h, appear));
        tg.addChild(innerTg);

        transformMap.put(objectId, tg);
        appearanceMap.put(objectId, appear);

        return tg;
    }


    private TransformGroup createCompoundShape(int objectId, CompoundShape shape) {

        Appearance appear = createDefaultAppearance();
        TransformGroup tg = new TransformGroup();

        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        // todo: support recursive compound shapes
        for (int shapeId = 0; shapeId < shape.getNumChildShapes(); shapeId++) {

            CollisionShape childShape = shape.getChildShape(shapeId);
            Transform trans = new Transform();

            shape.getChildTransform(shapeId, trans);

            Quat4f rot = new Quat4f();
            trans.getRotation(rot);

            Transform3D t3D = new Transform3D();
            t3D.setRotation(rot);
            t3D.setTranslation(trans.origin);

            TransformGroup childTg = new TransformGroup(t3D);

            if (childShape instanceof SphereShape){

                SphereShape sphere = (SphereShape)childShape;

                childTg.addChild(new Sphere(sphere.getRadius(), appear));
            }
            else if (childShape instanceof CylinderShape) {

                CylinderShape cylinder = (CylinderShape)childShape;

                // cylinders in Java3D are about a different axis than JBullet,
                // so wrap them in an extra TG so that they can be rotated properly
                Transform3D innerT3D = new Transform3D();
                innerT3D.rotX(-Math.PI / 2);

                TransformGroup innerTg = new TransformGroup(innerT3D);

                Vector3f halfExtents = new Vector3f();
                cylinder.getHalfExtentsWithMargin(halfExtents);

                innerTg.addChild(new Cylinder(cylinder.getRadius(), 2 * Math.abs(halfExtents.z), appear));
                childTg.addChild(innerTg);
            }
            else if (childShape instanceof BoxShape) {

                BoxShape box = (BoxShape)childShape;

                Vector3f halfExtents = new Vector3f();
                box.getHalfExtentsWithMargin(halfExtents);

                childTg.addChild(new Box(Math.abs(halfExtents.x), Math.abs(halfExtents.y), Math.abs(halfExtents.z), appear));
            }
            else if (childShape instanceof ConeShape) {

                ConeShape cone = (ConeShape)childShape;

                childTg.addChild(new Cone(cone.getRadius(), cone.getHeight(), appear));
            }
            else {
                logger.warn("Child not recognized!");
            }

            tg.addChild(childTg);
        }

        transformMap.put(objectId, tg);
        appearanceMap.put(objectId, appear);

        return tg;
    }


    private Appearance createDefaultAppearance() {

        Material mat = new Material(DEFAULT_COLOR, BLACK, DEFAULT_COLOR, SPECULAR, SHININESS);
        Appearance app = new Appearance();

        mat.setLightingEnable(true);
        app.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        app.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
        app.setMaterial(mat);

        return app;
    }


    public void spawnObjectView(int objectId) {

        if (objectViewMap.containsKey(objectId)) {
            return;
        }

        // a new view on the world
        Canvas3D canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());

        canvas.setFocusable(true);

        ViewingPlatform vp = new ViewingPlatform(1);

        vp.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        vp.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        Viewer viewer = new Viewer(canvas);
        viewer.setViewingPlatform(vp);

        universe.getLocale().addBranchGraph(vp);

        objectViewMap.put(objectId, new ObjectView(objectId, canvas, vp));
    }


    public void setMainView(Point3d from, Point3d to, Vector3d up) {

        ViewingPlatform vp = universe.getViewingPlatform();
        TransformGroup tg = vp.getViewPlatformTransform();

        Transform3D t3d = new Transform3D();
        tg.getTransform(t3d);

        t3d.lookAt(from, to, up);
        t3d.invert();

        tg.setTransform(t3d);
    }


    public void setMainViewTransform(Transform3D t3d) {
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(t3d);
    }


    public Transform3D getMainViewTransform() {

        TransformGroup tg = universe.getViewingPlatform().getViewPlatformTransform();
        Transform3D t3d = new Transform3D();

        tg.getTransform(t3d);

        return t3d;
    }


    public void setLabelsVisible(boolean visible) {

        // todo: toggle labels
    }


    public void dispose() {

        for (ObjectView v : objectViewMap.values()) {
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new WindowEvent(v, WindowEvent.WINDOW_CLOSING));
        }
    }


    /**
     * A special frame that shows the view of a single entity.
     */
    private class ObjectView extends JFrame {

        private int objectId;
        private ViewingPlatform vp;


        public ObjectView(int id, Canvas3D canvas, ViewingPlatform plat) {

            objectId = id;
            vp = plat;

            // GUI
            Dimension size = new Dimension(400, 400);
            JPanel panel = new JPanel(new BorderLayout());

            panel.add(canvas, BorderLayout.CENTER);

            setTitle("Object " + objectId + " View");
            setSize(size);
            setContentPane(panel);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setVisible(true);
        }


        public void update(Transform3D t3d) {

            Vector3f pos = new Vector3f();
            t3d.get(pos);

            Point3d camPos = new Point3d(pos);

            t3d.lookAt(camPos, new Point3d(camPos.x, camPos.y, -10), UP);
            t3d.invert();

            vp.getViewPlatformTransform().setTransform(t3d);
        }


        @Override
        public void dispose() {

            // if the user closes the window, no need to waste time updating the view
            objectViewMap.remove(objectId);

            super.dispose();
        }
    }

}
