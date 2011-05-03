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
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;
import harvard.robobees.simbeeotic.model.MotionListener;
import harvard.robobees.simbeeotic.model.sensor.camera.CameraView;
import harvard.robobees.simbeeotic.util.ImageLoader;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.log4j.Logger;

import javax.media.j3d.*;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A panel that uses Java3D to display a 3D world view.
 *
 * @author dcai
 * @author bkate
 */
public class Java3DWorld extends JPanel implements ViewPanel, MotionListener {

    private SimpleUniverse universe;
    private TransformGroup rootTransformGroup;

    private Map<Integer, BranchGroup> branchMap = new HashMap<Integer, BranchGroup>();
    private Map<Integer, TransformGroup> transformMap = new HashMap<Integer, TransformGroup>();
    private Map<Integer, Appearance> appearanceMap = new HashMap<Integer, Appearance>();
    private Map<Integer, ObjectView> objectViewMap = new ConcurrentHashMap<Integer, ObjectView>();
    private Map<Integer, CameraView> cameraViewMap = new ConcurrentHashMap<Integer, CameraView>();

    private static Logger logger = Logger.getLogger(Java3DWorld.class);

    private static final Color3f SKY = new Color3f(new Color(65, 105, 225));
    private static final Color3f GROUND = new Color3f(new Color(85, 107, 47));
    private static final Color3f BLACK = new Color3f(Color.BLACK);
    private static final Color3f SPECULAR = new Color3f(new Color(0.9f, 0.9f, 0.9f));
    private static final Color3f DEFAULT_COLOR = new Color3f(Color.LIGHT_GRAY);
    private static final float SHININESS = 25;
    private static final int PRIM_FLAGS = Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS;

    private static final double BOUNDS = 3000;

    private static final Point3d ORIGIN = new Point3d(0, 0, 0);
    private static final Point3d DEFAULT_VIEW = new Point3d(-20, 12, 20);
    private static final Vector3d UP = new Vector3d(0, 1, 0);
//      private static final Vector3d UP = new Vector3d(0, 0, 1);


    public Java3DWorld(boolean useSkyBackground) {
        initWorld(useSkyBackground);
    }


    private void initWorld(boolean useSkyBackground) {

        BranchGroup rootBranchgroup = new BranchGroup();
        BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), BOUNDS);

        Canvas3D canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        canvas.setFocusable(true);

        universe = new SimpleUniverse(canvas);
        universe.getViewer().getView().setBackClipDistance(BOUNDS);

        // lights
        AmbientLight ambientLightNode = new AmbientLight(new Color3f(Color.WHITE));
        ambientLightNode.setInfluencingBounds(bounds);

        DirectionalLight light = new DirectionalLight(new Color3f(Color.WHITE), new Vector3f(-1.0f, -1.0f, -1.0f));
        light.setInfluencingBounds(bounds);

        rootBranchgroup.addChild(ambientLightNode);
        rootBranchgroup.addChild(light);

        // background
        Background back = new Background();

        if (useSkyBackground) {

            Material mat = new Material(GROUND, GROUND, GROUND, SPECULAR, SHININESS);
            mat.setLightingEnable(true);

            Appearance appear = new Appearance();
            appear.setMaterial(mat);

            Texture tex = new TextureLoader(ImageLoader.loadImageFromClasspath("/textures/sky_1.png"), this).getTexture();
            TextureAttributes ta = new TextureAttributes();

            tex.setBoundaryModeT(Texture.WRAP);
            tex.setBoundaryModeS(Texture.WRAP);
            ta.setTextureMode(TextureAttributes.DECAL);

            appear.setTexture(tex);
            appear.setTextureAttributes(ta);

            Sphere backSphere = new Sphere(1.0f,
                                           PRIM_FLAGS | Primitive.GENERATE_NORMALS_INWARD,
                                           45,
                                           appear);

            // rotate the sphere so the coords are the right way
            Transform3D backTrans = new Transform3D();
            backTrans.rotX(-Math.PI);

            TransformGroup tg = new TransformGroup(backTrans);
            BranchGroup backGeoBranch = new BranchGroup();

            tg.addChild(backSphere);
            backGeoBranch.addChild(tg);

            back.setGeometry(backGeoBranch);
        }

        back.setColor(SKY);
        back.setApplicationBounds(bounds);

        rootBranchgroup.addChild(back);

        // root transform that makes Z axis up (as in Simbeeotic)
        Transform3D t3d = new Transform3D();
        t3d.rotX(-Math.PI / 2);

        rootTransformGroup = new TransformGroup(t3d);

        rootTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        rootTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        rootTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);

        // draw axes
//        rootTransformGroup.addChild(createAxes());

        // compile scene
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
    public void shapeUpdate(int objectId, CollisionShape shape) {

        boolean exists = transformMap.containsKey(objectId);
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

            // approximate a static plane with a cylinder
            // that is large enough to cover the world bounds 
            tg = createCylinder(objectId, (float)BOUNDS, 0);
        }
        else if (shape instanceof CompoundShape){
            tg = createCompoundShape(objectId, (CompoundShape)shape);
        }
        else {
            logger.warn("Object not recognized!");
        }

        if (tg != null) {

            if (exists) {
                rootTransformGroup.removeChild(branchMap.get(objectId));
            }

            BranchGroup bg = new BranchGroup();

            bg.setCapability(BranchGroup.ALLOW_DETACH);
            bg.addChild(tg);

            branchMap.put(objectId, bg);
            rootTransformGroup.addChild(bg);
        }
    }


    @Override
    public void scaleUpdate(int objectId, Vector3f scale) {

        TransformGroup tg = transformMap.get(objectId);
        Transform3D t3d = new Transform3D();

        tg.getTransform(t3d);
        t3d.setScale(new Vector3d(scale));
        tg.setTransform(t3d);
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

        CameraView cview = cameraViewMap.get(objectId);

        if(cview != null) {
            cview.update(t3D);
        }

    }


    @Override
    public void metaUpdate(int objectId, Color color, Image texture, String label) {

        if (color != null) {

            Color3f col = new Color3f(color);

            // the alpha values coming in are [0,255] where
            // 0 is fully transparent and 255 is opaque.
            // java3d uses a range of [0.0,1.0] where 0.0 is
            // fully opaque and 1.0 is fully transparent
            float alpha = 1 - (color.getAlpha() / 255.0f);

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

        if (texture != null) {

            Texture tex = new TextureLoader(texture, this).getTexture();
            TextureAttributes ta = new TextureAttributes();
            Appearance appear = appearanceMap.get(objectId);

            tex.setBoundaryModeT(Texture.WRAP);
            tex.setBoundaryModeS(Texture.WRAP);
            ta.setTextureMode(TextureAttributes.DECAL);
            
            appear.setTexture(tex);
            appear.setTextureAttributes(ta);
        }

        // todo: labels
    }


    private TransformGroup createSphere(int objectId, float radius) {

        Appearance appear = createDefaultAppearance();

        if (appearanceMap.containsKey(objectId)) {
            appear = appearanceMap.get(objectId);
        }

        TransformGroup tg = new TransformGroup();

        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        tg.addChild(new Sphere(radius, PRIM_FLAGS, appear));

        transformMap.put(objectId, tg);
        appearanceMap.put(objectId, appear);

        return tg;
    }


    private TransformGroup createBox(int objectId, float l, float w, float h) {

        Appearance appear = createDefaultAppearance();

        if (appearanceMap.containsKey(objectId)) {
            appear = appearanceMap.get(objectId);
        }

        TransformGroup tg = new TransformGroup();

        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        tg.addChild(new Box(l, w, h, PRIM_FLAGS, appear));

        transformMap.put(objectId, tg);
        appearanceMap.put(objectId, appear);

        return tg;
    }


    private TransformGroup createCone(int objectId, float r, float h) {

        Appearance appear = createDefaultAppearance();

        if (appearanceMap.containsKey(objectId)) {
            appear = appearanceMap.get(objectId);
        }

        TransformGroup tg = new TransformGroup();

        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        // cones in Java3D are about a different axis than JBullet,
        // so wrap them in an extra TG so that they can be rotated properly
        Transform3D t3d = new Transform3D();
        t3d.rotX(Math.PI / 2);

        TransformGroup innerTg = new TransformGroup(t3d);

        innerTg.addChild(new Cone(r, h, PRIM_FLAGS, appear));
        tg.addChild(innerTg);

        transformMap.put(objectId, tg);
        appearanceMap.put(objectId, appear);

        return tg;
    }


    private TransformGroup createCylinder(int objectId, float r, float h) {

        Appearance appear = createDefaultAppearance();

        if (appearanceMap.containsKey(objectId)) {
            appear = appearanceMap.get(objectId);
        }

        TransformGroup tg = new TransformGroup();

        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        // cylinders in Java3D are about a different axis than JBullet,
        // so wrap them in an extra TG so that they can be rotated properly
        Transform3D t3d = new Transform3D();
        t3d.rotX(-Math.PI / 2);

        TransformGroup innerTg = new TransformGroup(t3d);

        innerTg.addChild(new Cylinder(r, h, PRIM_FLAGS, appear));
        tg.addChild(innerTg);

        transformMap.put(objectId, tg);
        appearanceMap.put(objectId, appear);

        return tg;
    }


    private TransformGroup createCompoundShape(int objectId, CompoundShape shape) {

        Appearance appear = createDefaultAppearance();

        if (appearanceMap.containsKey(objectId)) {
            appear = appearanceMap.get(objectId);
        }
        
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

                childTg.addChild(new Sphere(sphere.getRadius(), PRIM_FLAGS, appear));
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

                innerTg.addChild(new Cylinder(cylinder.getRadius(), 2 * Math.abs(halfExtents.z), PRIM_FLAGS, appear));
                childTg.addChild(innerTg);
            }
            else if (childShape instanceof BoxShape) {

                BoxShape box = (BoxShape)childShape;

                Vector3f halfExtents = new Vector3f();
                box.getHalfExtentsWithMargin(halfExtents);

                childTg.addChild(new Box(Math.abs(halfExtents.x), Math.abs(halfExtents.y), Math.abs(halfExtents.z), PRIM_FLAGS, appear));
            }
            else if (childShape instanceof ConeShape) {

                ConeShape cone = (ConeShape)childShape;

                // cones in Java3D are about a different axis than JBullet,
                // so wrap them in an extra TG so that they can be rotated properly
                Transform3D t3d = new Transform3D();
                t3d.rotX(Math.PI / 2);

                TransformGroup innerTg = new TransformGroup(t3d);

                innerTg.addChild(new Cone(cone.getRadius(), cone.getHeight(), PRIM_FLAGS, appear));
                childTg.addChild(innerTg);
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


    private TransformGroup createLabel(Text2D message, Vector3f pos) {

        // todo: fix this - taken from old code
//        TransformGroup labeltg = new TransformGroup();
//        Transform3D labelt3d = new Transform3D();
//        labelt3d.setTranslation(new Vector3f(pos.x+0.0f,pos.y+0.1f,pos.z+0.0f));
//        Quat4f quat = new Quat4f();
//        labelt3d.setRotation(quat);
//        labeltg.setTransform(labelt3d);
//        labeltg.addChild(message);

        return null;
    }


    private TransformGroup createAxes() {

        float axisLength = 5.0f;
        float axisWidth = 0.05f;
        float coneHeight = 0.5f;
        float coneWidth = 0.2f;

        Appearance appear = createDefaultAppearance();
        TransformGroup axesTg = new TransformGroup();
        TransformGroup tg;
        Transform3D t3d;

        // origin
        axesTg.addChild(new Sphere(coneWidth, PRIM_FLAGS, appear));

        // X axis
        t3d = new Transform3D();
        t3d.rotZ(-Math.PI / 2);
        t3d.setTranslation(new Vector3f(axisLength / 2, 0, 0));

        tg = new TransformGroup(t3d);
        tg.addChild(new Cylinder(axisWidth, axisLength, PRIM_FLAGS, appear));

        axesTg.addChild(tg);

        t3d = new Transform3D();
        t3d.rotZ(-Math.PI / 2);
        t3d.setTranslation(new Vector3f(axisLength, 0, 0));

        tg = new TransformGroup(t3d);
        tg.addChild(new Cone(coneWidth, coneHeight, PRIM_FLAGS, appear));

        axesTg.addChild(tg);

        // Y axis
        t3d = new Transform3D();
        t3d.setTranslation(new Vector3f(0, axisLength / 2, 0));

        tg = new TransformGroup(t3d);
        tg.addChild(new Cylinder(axisWidth, axisLength, PRIM_FLAGS, appear));

        axesTg.addChild(tg);

        t3d = new Transform3D();
        t3d.setTranslation(new Vector3f(0, axisLength, 0));

        tg = new TransformGroup(t3d);
        tg.addChild(new Cone(coneWidth, coneHeight, PRIM_FLAGS, appear));

        axesTg.addChild(tg);

        // Z axis
        t3d = new Transform3D();
        t3d.rotX(-Math.PI / 2);
        t3d.setTranslation(new Vector3f(0, 0, axisLength / 2));

        tg = new TransformGroup(t3d);
        tg.addChild(new Cylinder(axisWidth, axisLength, PRIM_FLAGS, appear));

        axesTg.addChild(tg);

        t3d = new Transform3D();
        t3d.rotX(Math.PI / 2);
        t3d.setTranslation(new Vector3f(0, 0, axisLength));

        tg = new TransformGroup(t3d);
        tg.addChild(new Cone(coneWidth, coneHeight, PRIM_FLAGS, appear));

        axesTg.addChild(tg);

        return axesTg;
    }


    private Appearance createDefaultAppearance() {

        Material mat = new Material(DEFAULT_COLOR, BLACK, DEFAULT_COLOR, SPECULAR, SHININESS);
        Appearance app = new Appearance();

        mat.setLightingEnable(true);

        app.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        app.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
        app.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_WRITE);
        app.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
        app.setMaterial(mat);

        return app;
    }


    @Override
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

    //Create a CameraView within the 3D world
    public void spawnCameraView(int cameraId, ImageComponent2D buf, Transform3D trans, int w, int h, float focalLength) {
 //       spawnObjectView(cameraId);         //for demonstration purposes
        if (cameraViewMap.containsKey(cameraId)) {
            return;
        }

        // a new off-screen view on the world
        Canvas3D canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration(), true);
        canvas.setSize(w,h);
        canvas.setFocusable(true);

        //Set canvas properties
        canvas.getScreen3D().setPhysicalScreenHeight(h);
        canvas.getScreen3D().setPhysicalScreenWidth(w);
        canvas.getScreen3D().setSize(w, h);
        canvas.setOffScreenBuffer(buf);

        //Set ViewingPlatform properties
        ViewingPlatform vp = new ViewingPlatform(1);
        vp.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        vp.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        //Set ViewPlatform properties
        ViewPlatform viewPlatform = new ViewPlatform();
        viewPlatform.setViewAttachPolicy(View.NOMINAL_HEAD);
        viewPlatform.setActivationRadius(focalLength);
        vp.setViewPlatform(viewPlatform);

        //Set Viewer Properties
        Viewer viewer = new Viewer(canvas);
        viewer.setViewingPlatform(vp);
        viewer.getView().setBackClipDistance(BOUNDS);//Make smaller
        //Add CameraView to map
        universe.getLocale().addBranchGraph(vp);
        CameraView view = new CameraView(canvas, vp, trans);
        cameraViewMap.put(cameraId, view);

    }
    public void renderCameraView(int objectId){
        CameraView view = cameraViewMap.get(objectId);
        view.render();
    }

    @Override
    public void setMainView(Point3d from, Point3d to, Vector3d up) {

        ViewingPlatform vp = universe.getViewingPlatform();
        TransformGroup tg = vp.getViewPlatformTransform();

        Transform3D t3d = new Transform3D();
        tg.getTransform(t3d);

        t3d.lookAt(from, to, up);
        t3d.invert();

        tg.setTransform(t3d);
    }


    @Override
    public void setMainViewTransform(Transform3D t3d) {
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(t3d);
    }


    @Override
    public Transform3D getMainViewTransform() {

        TransformGroup tg = universe.getViewingPlatform().getViewPlatformTransform();
        Transform3D t3d = new Transform3D();

        tg.getTransform(t3d);

        return t3d;
    }


    @Override
    public void setLabelsVisible(boolean visible) {

        // todo: toggle labels
    }


    @Override
    public void dispose() {

        for (ObjectView v : objectViewMap.values()) {
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new WindowEvent(v, WindowEvent.WINDOW_CLOSING));
        }
    }
    //Class which represents CameraView within 3DWorld
    private class CameraView {
        Transform3D trans;
        Canvas3D canvas;
        ViewingPlatform vp;

        public CameraView(Canvas3D can, ViewingPlatform plat, Transform3D trans){
            this.canvas = can;
            this.vp = plat;
            this.trans = trans;
        }
        public void update(Transform3D t3d) {
            // Apply camera's transformation to bee's transformation
            t3d.mul(trans);
//          //Geometry needs adjusting to fix coordinate system issue
            vp.getViewPlatformTransform().setTransform(t3d);

        }
        public void render(){
            canvas.renderOffScreenBuffer();
            canvas.waitForOffScreenRendering();
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
            //Geometry needs adjusting to fix coordinate system issue
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
