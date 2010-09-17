package harvard.robobees.simbeeotic.component;

import harvard.robobees.simbeeotic.ClockListener;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.MotionListener;
import harvard.robobees.simbeeotic.model.SimpleBee;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.ConeShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.behaviors.keyboard.KeyNavigatorBehavior;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;

/**
 * SimpleVis draws shapes from the JBullet CollisionShape class, such as spheres, cones, cylinders, planes.
 * Implements translation and rotation of objects as well, which are sent through the stateUpdate method.
 *
 * @author dicai
 */

public class VisComponent3D extends JPanel implements VariationComponent, MotionListener, ActionListener, KeyListener, ClockListener {

    private static final long serialVersionUID = 1L;

    private final static Color3f black = new Color3f(0f, 0f, 0f);
    private final static Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
    private final static Color3f yellow = new Color3f(1.0f, 0.7f, 0.1f);
    private final static Color3f specular = new Color3f(0.9f,0.9f,0.9f);
    private final static Color3f brown = new Color3f(0.3f, 0.16f, 0.16f);
    private final static Color3f green = new Color3f(0.2f, 0.4f, 0.2f);
    //private final static Color3f lightGreen = new Color3f(0.3f, 0.9f, 0.4f);
    //private final static Color3f red = new Color3f(1.0f, 0.0f, 0.0f);
    private final static Color3f pink = new Color3f(1.0f, 0.0f, 0.0f);
    private final static Color3f gray = new Color3f(0.2f, 0.3f, 0.3f);
    private final static int FLOOR = 400;
    private final static Point3d userPosn = new Point3d(-20,12,20);
    private final static int HEIGHT = 1000;
    private final static int WIDTH = 1400;
    private final static int VIEWHEIGHT = 400;
    private final static int VIEWWIDTH = 400;
    private final static int CLIPDIST = 30;

    @Inject
    private VariationContext context;

    private SimpleUniverse u = null;
    private Canvas3D c = null;
    private Canvas3D c2 = null;
    private GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

    private BranchGroup objRoot = new BranchGroup();
    private BoundingSphere bounds = new BoundingSphere(new Point3d(0,0,0), 100.0);
    private TransformGroup objTrans;
    private Transform3D objTrans3D = new Transform3D();
    private Appearance msgApp = new Appearance();

    private Map<Integer, TransformGroup> objects = new HashMap<Integer, TransformGroup>();
    private Map<Integer, Color3f> colors = new HashMap<Integer, Color3f>();
    private Map<Integer, Appearance> app = new HashMap<Integer, Appearance>();
    private Map<Integer, Text2D> labeltgs = new HashMap<Integer, Text2D>();
    private Map<Integer, Boolean> needRot = new HashMap<Integer, Boolean>();

    // GUI elements
    private JButton pause = new JButton("pause");
    private JButton reset = new JButton("reset");
    private JButton save = new JButton("save");
    private JButton load = new JButton("load");
    private JButton labels = new JButton("off");
    private JTextField simTime = new JTextField(5);
    private JComboBox beeList;
    private JComboBox viewList;
    private JComboBox viewTypeList;

    private JFrame mf = new JFrame("Simbeeotic Visualization");
    private JPanel p0 = new JPanel();
    private JPanel main = new JPanel();
    private JFileChooser fc = new JFileChooser();

    private ViewingPlatform vp2 = new ViewingPlatform(1);

    private boolean isTexture = false;
    private boolean isGround = false;
    private boolean firstTime = true;
    private boolean camFrame = false;
    private boolean clock = false;
    private boolean labeloff = false;
    private int currBee = 0;
    private int viewType = 0;


    @Override
    public void initialize() {

        initGUI();

        context.getRecorder().addListener(this);
        context.getClockControl().addListener(this);

        // attach scene to universe
        u = new SimpleUniverse(c);
        u.getViewer().getView().setBackClipDistance(CLIPDIST);

        // set cap for vp2
        setTgCap(vp2.getViewPlatformTransform());

        createSceneGraph();

        initUserPosition();
        orbitControls(c);
        u.addBranchGraph(objRoot);

    }

    @Override
    public void shutdown() {
        // close frame
        mf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void initializeObject(int objectId, CollisionShape shape) {

        // sphere shape
        if (shape instanceof SphereShape) {

            SphereShape s = (SphereShape) shape;
            float radius = s.getRadius();

            drawSphere(objectId, yellow, radius);
            u.addBranchGraph(objRoot);

        }
        // cylinder shape
        else if (shape instanceof CylinderShape) {
            CylinderShape c = (CylinderShape) shape;
            float r = c.getRadius();

            Vector3f halfExtents = new Vector3f();
            c.getHalfExtentsWithMargin(halfExtents);

            drawCylinder(objectId, pink, r, 2*Math.abs(halfExtents.z));

            u.addBranchGraph(objRoot);
        }
        // box shape
        else if (shape instanceof BoxShape) {
            BoxShape b = (BoxShape) shape;
            Vector3f halfExtents = new Vector3f();
            b.getHalfExtentsWithMargin(halfExtents);

            isGround = false;

            drawBox(objectId, gray, Math.abs(halfExtents.x), Math.abs(halfExtents.y), Math.abs(halfExtents.z));
            u.addBranchGraph(objRoot);
        }
        // cone shape
        else if (shape instanceof ConeShape) {
            ConeShape c = (ConeShape) shape;
            float r = c.getRadius();
            float h = c.getHeight();

            drawCone(objectId, gray, r, h);
            u.addBranchGraph(objRoot);
        }
        // plane shape
        else if (shape instanceof StaticPlaneShape){
            StaticPlaneShape s = (StaticPlaneShape) shape;

            Vector3f normal = new Vector3f();
            s.getPlaneNormal(normal);

            isGround = true;

            drawBox(objectId,green,6000,6000,0);

            u.addBranchGraph(objRoot);
        }
        else if (shape instanceof CompoundShape){

            drawCompoundShape(objectId, brown, shape);

            u.addBranchGraph(objRoot);
        }
        else
            System.out.println("Object not recognized!");

    }

    @Override
    public void stateUpdate(int objectId, Vector3f position, Quat4f orientation) {

        // get object from the ID
        TransformGroup group = objects.get(objectId);

        Transform3D t3D = new Transform3D();
        group.getTransform(t3D);

        t3D.setRotation(orientation);
        if (needRot.get(objectId)==true)
            t3D.rotX(Math.PI/2);
        t3D.setTranslation(position);

        group.setTransform(t3D);

        // store position in map
        //pos.put(objectId,position);

        if (camFrame && currBee==objectId){

            TransformGroup currBeeGroup = objects.get(currBee);

            Transform3D t3d = new Transform3D();
            currBeeGroup.getTransform(t3d);

            Vector3f pos = new Vector3f();
            t3d.get(pos);

            Point3d camPos = new Point3d(pos);

//            // args are: viewer posn, where looking, up direction
            t3d.lookAt(camPos, new Point3d(camPos.x,camPos.y,-10), new Vector3d(0,1,0));
            t3d.invert();

            vp2.getViewPlatformTransform().setTransform(t3d);
        }

    }

    @Override
    public void metaUpdate(int objectId, Color color, Image texture, String label) {
        // updates color

        if (color!=null) {
            Color3f col = new Color3f();
            col.set(color);

            // set transparency
            Appearance appear = app.get(objectId);
            appear.setMaterial(new Material(col,black,col,specular,25.0f));

            float alpha = 1-color.getAlpha()/255; //convert so it works with java3d
            TransparencyAttributes ta = makeTransparent(alpha);
            appear.setTransparencyAttributes(ta);

//            System.out.println(""+alpha);
        }


        // updates label
        if (label!=null){
            Text2D message = labeltgs.get(objectId);

            if (message==null) {
                message = new Text2D(label, black, "SansSerif", 70, Font.BOLD);
                labeltgs.put(objectId,message);
            }
            message = new Text2D(label, black, "SansSerif", 70, Font.BOLD);
        }
    }

    @Override
    public void clockUpdated(SimTime time) {

        simTime.setText("" + time.getImpreciseTime() + " s");
        simTime.setEditable(false);

        if (firstTime) {

            List<SimpleBee> bees = context.getSimEngine().findModelsByType(SimpleBee.class);

            for (SimpleBee b : bees) {
                beeList.addItem(b.getObjectId());
            }

            firstTime = false;
        }
    }


    private BranchGroup createSceneGraph() {

        addLights(); // add lights
        addBackground(); // add sky
        labelAxes();
//        addSky();

        objTrans3D = new Transform3D();
        objTrans3D.rotX(-Math.PI/2);
        objTrans = new TransformGroup(objTrans3D);

        // optimizations
        objRoot.compile();
        return objRoot;
    }

    private void addLights() {
        AmbientLight ambientLightNode = new AmbientLight(white);
        ambientLightNode.setInfluencingBounds(bounds);
        objRoot.addChild(ambientLightNode);

        Vector3f light1Direction = new Vector3f(-1.0f, -1.0f, -1.0f);
        DirectionalLight light1 = new DirectionalLight(white, light1Direction);
        light1.setInfluencingBounds(bounds);
        objRoot.addChild(light1);

        Vector3f light2Direction  = new Vector3f(1.0f, -1.0f, 1.0f);
        DirectionalLight light2 = new DirectionalLight(white, light2Direction);
        light2.setInfluencingBounds(bounds);
        objRoot.addChild(light2);
    }

    private void addBackground() {

//        TextureLoader myLoader = new TextureLoader( "/home/dcai/workspace/simbeeotic/simbeeotic-vis/src/main/resources/textures/sky_3r.png", this );
//        ImageComponent2D myImage = myLoader.getImage( );

        Background back = new Background();
//        back.setApplicationBounds(bounds);
        back.setColor(0.17f, 0.55f, 0.82f); //sky color
//        back.setImage(myImage);
        back.setApplicationBounds(bounds);
//        back.setImageScaleMode(SCALE_FIT_MAX);
        objRoot.addChild(back);
    }

    private void addToMap(int objectId, TransformGroup tg, Color3f c, Appearance a, Text2D msg, Boolean rot) {
        objects.put(objectId,tg);
        colors.put(objectId,c);
        app.put(objectId,a);
        labeltgs.put(objectId,msg);
        needRot.put(objectId,rot);
    }

    private void addSky() {
        Appearance app = new Appearance();
//        TransparencyAttributes ta = new TransparencyAttributes();
//        ta.setTransparencyMode(TransparencyAttributes.BLENDED);
//        ta.setTransparency(0.6f);
//        app.setTransparencyAttributes(ta);

        // Load in the Texture File
//        TextureLoader loader = new TextureLoader
//                ("/home/dcai/workspace/simbeeotic/simbeeotic-vis/src/main/resources/textures/sky_3r.png",this);
//
//        // Create Texture object
//        Texture brick = loader.getTexture();
//
//        TextureAttributes txa = new TextureAttributes();
//        app.setTextureAttributes(txa);
//
//        // Attach Texture object to Appearance object
//        app.setTexture(brick);
//
//        TransformGroup tg = new TransformGroup();
//        tg.addChild(new Sphere(30,Primitive.GENERATE_TEXTURE_COORDS,app));
//        objRoot.addChild(tg);
    }

    private Appearance createAppCap(Color3f color) {
        Material mat = new Material(color,black,color,specular,25.0f);
        Appearance app = new Appearance();
        mat.setLightingEnable(true);
        app.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        app.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
        app.setMaterial(mat);
        return app;
    }


    private ViewingPlatform createViewer(Canvas3D c, Vector3f pos) {

        Viewer viewer = new Viewer(c);
        vp2.setPlatformGeometry(null);

        // initial position for viewer
        Transform3D t3d = new Transform3D();
        t3d.setTranslation(pos);
        vp2.getViewPlatformTransform().setTransform(t3d);

//        setTgCap(vp2.getViewPlatformTransform());
        viewer.setViewingPlatform(vp2);

        return vp2;
    }

    private void drawCylinder(int objectId, Color3f color, float radius, float height) {

        // creates appearance with default colors set
        Appearance appear = createAppCap(color);

        // position
        objRoot = new BranchGroup();
        objTrans = new TransformGroup(objTrans3D);

        Transform3D t3d = new Transform3D();
        TransformGroup tg = new TransformGroup(t3d);
        setTgCap(tg);
        tg.addChild(new Cylinder(radius, height, appear));

        Vector3f pos = new Vector3f();
        t3d.get(pos);


        Text2D message = labeltgs.get(objectId);
        if (message==null)
            message = new Text2D(""+objectId, black, "SansSerif", 70, Font.BOLD );
        Transform3D labelt3d = new Transform3D();
        //labelt3d.rotX(Math.PI/2);


        TransformGroup labeltg = new TransformGroup(labelt3d);
        labeltg.addChild(makeLabel(message,pos));

        tg.addChild(labeltg);

        objTrans.addChild(tg);
        objRoot.addChild(objTrans);

//        objRoot.addChild(tg);

        addToMap(objectId,tg,color,appear,message,true);
    }

    private void drawCone(int objectId, Color3f color, float r, float h) {
        Appearance appear = createAppCap(color);

        // position
        objRoot = new BranchGroup();
        objTrans = new TransformGroup(objTrans3D);
        Transform3D t3d = new Transform3D();


        TransformGroup tg = new TransformGroup(t3d);
//        t3d.rotX(Math.PI/2);
        tg.setTransform(t3d);

        setTgCap(tg);
        tg.addChild(new Cone (r, h, appear));

        Vector3f pos = new Vector3f();
        t3d.get(pos);


        Text2D message = labeltgs.get(objectId);
        if (message==null)
            message = new Text2D(""+objectId, black, "SansSerif", 70, Font.BOLD );
        Transform3D labelt3d = new Transform3D();
        //labelt3d.rotX(Math.PI/2);


        TransformGroup labeltg = new TransformGroup(labelt3d);
        labeltg.addChild(makeLabel(message,pos));

        tg.addChild(labeltg);

        objTrans.addChild(tg);
        objRoot.addChild(objTrans);

        addToMap(objectId,tg,color,appear,message,true);
    }

    private void drawBox(int objectId, Color3f color, float l, float w, float h) {
        Appearance appear = createAppCap(color);

        // position
        objRoot = new BranchGroup();
        objTrans = new TransformGroup(objTrans3D);
        Transform3D t3d = new Transform3D();
        TransformGroup tg = new TransformGroup(t3d);
        setTgCap(tg);


        if (!isGround) {

            // Load in the Texture File
//            TextureLoader loader = new TextureLoader
//                    ("/home/dcai/workspace/simbeeotic/simbeeotic-vis/src/main/resources/textures/brick.jpg",this);

//            // Create Texture object
//            Texture brick = loader.getTexture();
////
//
//            TextureAttributes ta = new TextureAttributes();
//            appear.setTextureAttributes(ta);
//
//            // Attach Texture object to Appearance object
//            appear.setTexture(brick);
//
//
//            tg.addChild(new com.sun.j3d.utils.geometry.Box(l,w,h,Primitive.GENERATE_TEXTURE_COORDS,appear));

            // Create Texture object
//            Texture brick = loader.getTexture();
//
//            TextureAttributes ta = new TextureAttributes();
//            appear.setTextureAttributes(ta);
//
//            // Attach Texture object to Appearance object
//            appear.setTexture(brick);
//
//            tg.addChild(new com.sun.j3d.utils.geometry.Box(l,w,h,Primitive.GENERATE_TEXTURE_COORDS,appear));

        }
        else {
//            tg.addChild(new com.sun.j3d.utils.geometry.Box(l,w,h,appear));
//            TextureLoader loader = new TextureLoader
//            ("/home/dcai/workspace/simbeeotic/simbeeotic-vis/src/main/resources/textures/grass_1r.jpg",this);
//
//            // Create Texture object
//            Texture brick = loader.getTexture();
////
//
//            TextureAttributes ta = new TextureAttributes();
//            appear.setTextureAttributes(ta);
//
//            // Attach Texture object to Appearance object
//            appear.setTexture(brick);

            tg.addChild(new com.sun.j3d.utils.geometry.Box(l,w,h,appear));
        }

        Vector3f pos = new Vector3f();
        t3d.get(pos);


        Text2D message = labeltgs.get(objectId);
        if (message==null)
            message = new Text2D(""+objectId, black, "SansSerif", 70, Font.BOLD );
        Transform3D labelt3d = new Transform3D();
        labelt3d.rotX(Math.PI/2);

        TransformGroup labeltg = new TransformGroup(labelt3d);
        labeltg.addChild(makeLabel(message,pos));

        tg.addChild(labeltg);

        objTrans.addChild(tg);
        objRoot.addChild(objTrans);

        addToMap(objectId,tg,color,appear,message,false);
    }

    private void drawSphere(int objectId, Color3f color, float radius) {

        //appearance
        Appearance appear = createAppCap(color);

        // position
        objRoot = new BranchGroup();
        objTrans = new TransformGroup(objTrans3D);
        Transform3D t3d = new Transform3D();

        TransformGroup tg = new TransformGroup(t3d);
        setTgCap(tg);

        tg.addChild(new Sphere(radius, appear));

        Vector3f pos = new Vector3f();
        t3d.get(pos);

        Text2D message = labeltgs.get(objectId);
        if (message==null)
            message = new Text2D(""+objectId, black, "SansSerif", 70, Font.BOLD );
        message.setCapability(Text2D.ALLOW_APPEARANCE_READ);
        message.setCapability(Text2D.ALLOW_APPEARANCE_WRITE);
        Transform3D labelt3d = new Transform3D();
        labelt3d.rotX(Math.PI/2);
        Appearance labelApp = new Appearance();
        labelApp.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
        labelApp.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);

//        TransparencyAttributes ta = new TransparencyAttributes();
//        ta.setTransparencyMode(TransparencyAttributes.BLENDED);
//        ta.setTransparency(0.6f);
//        labelApp.setTransparencyAttributes(ta);

//        message.setAppearance(labelApp);

        TransformGroup labeltg = new TransformGroup(labelt3d);
        labeltg.addChild(makeLabel(message,pos));

        tg.addChild(labeltg);

        objTrans.addChild(tg);
        objRoot.addChild(objTrans);

//        objRoot.addChild(tg);

        // place TransformGroup into hash
        addToMap(objectId,tg,color,appear,message,false);
    }

    private void drawCompoundShape(int objectId, Color3f color, CollisionShape shape){
        CompoundShape s = (CompoundShape) shape;

        objRoot = new BranchGroup();
        objTrans = new TransformGroup(objTrans3D);

        Transform3D t3d = new Transform3D();
        TransformGroup tg = new TransformGroup(t3d);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Appearance appear = createAppCap(color);

        TransparencyAttributes ta = makeTransparent(0.6f);
        appear.setTransparencyAttributes(ta);

        for (int shapeId = 0; shapeId < s.getNumChildShapes(); shapeId++) {

            CollisionShape c = s.getChildShape(shapeId);
            Transform trans = new Transform();

            s.getChildTransform(shapeId, trans);

            if (c instanceof SphereShape){
                SphereShape sphere = (SphereShape)c;
                Float r = sphere.getRadius();

                Transform3D t3D = new Transform3D();
                Quat4f rot = new Quat4f();

                trans.getRotation(rot);
                t3D.rotX(-Math.PI/2);
                t3D.setRotation(rot);

                t3D.setTranslation(trans.origin);

                TransformGroup childTg = new TransformGroup(t3D);

                childTg.addChild(new Sphere(r,appear));
                tg.addChild(childTg);
            }
            else if (c instanceof CylinderShape){
                CylinderShape cylinder = (CylinderShape)c;
                Float r = cylinder.getRadius();

                Vector3f halfExtents = new Vector3f();
                cylinder.getHalfExtentsWithMargin(halfExtents);


                Quat4f rot = new Quat4f();
                trans.getRotation(rot);

                Transform3D t3D = new Transform3D();
                t3D.setRotation(rot);
                t3D.rotX(-Math.PI/2);
                t3D.setTranslation(trans.origin);

                TransformGroup childTg = new TransformGroup(t3D);

                childTg.addChild(new Cylinder(r,2*Math.abs(halfExtents.z),appear));

                tg.addChild(childTg);
            }
            else if (c instanceof BoxShape){
                BoxShape box = (BoxShape)c;
                Vector3f halfExtents = new Vector3f();
                box.getHalfExtentsWithMargin(halfExtents);

                Transform3D t3D = new Transform3D();
                Quat4f rot = new Quat4f();

                trans.getRotation(rot);

                t3D.setRotation(rot);
//                t3D.rotX(-Math.PI/2);
                t3D.setTranslation(trans.origin);

                TransformGroup childTg = new TransformGroup(t3D);

                childTg.addChild(new com.sun.j3d.utils.geometry.Box(Math.abs(halfExtents.x),
                                                                    Math.abs(halfExtents.y),Math.abs(halfExtents.z),appear));

                tg.addChild(childTg);
            }
            else if (c instanceof ConeShape){
                ConeShape cone = (ConeShape)c;
                Float r = cone.getRadius();
                Float h = cone.getHeight();

                Transform3D t3D = new Transform3D();
                Quat4f rot = new Quat4f();

                trans.getRotation(rot);

                t3D.setRotation(rot);
                t3D.rotX(-Math.PI/2);
                t3D.setTranslation(trans.origin);

                TransformGroup childTg = new TransformGroup(t3D);

                childTg.addChild(new Cone(r,h,appear));
                tg.addChild(childTg);

            }
            else
                System.out.println("Child not recognized!");


        }

        Text2D message = null;

        objTrans.addChild(tg);
        objRoot.addChild(objTrans);

        addToMap(objectId,tg,color,appear,message,false);
    }

    private void initUserPosition()
    // Set the user's initial viewpoint using lookAt()
    {
        setUserPos(userPosn,new Point3d(0,3,0), new Vector3d(0,1,0));
    }

    private void labelAxes()
    {
        Vector3d pt = new Vector3d();
        for (int i=-FLOOR/2; i <= FLOOR/2; i++) {
            pt.x = i;
            objRoot.addChild( makeText(pt,""+i) );   // along x-axis
        }
        pt.x = 0;
        for (int i=-FLOOR/2; i <= FLOOR/2; i++) {
            pt.z = i;
            objRoot.addChild( makeText(pt,""+i) );   // along z-axis
        }
        pt.z=0;
        for (int i=-FLOOR/2; i <= FLOOR/2; i++) {
            pt.y = i;
            objRoot.addChild( makeText(pt,""+i) );   // along y-axis
        }
    }

    private TransformGroup makeText(Vector3d vertex, String text)
    {
        Text2D message = new Text2D(text, white, "SansSerif", 76, Font.BOLD );
//        PolygonAttributes pa = new PolygonAttributes();
//        pa.setCapability(PolygonAttributes.ALLOW_CULL_FACE_WRITE);
//        pa.setCullFace(PolygonAttributes.CULL_NONE);
//        Appearance app = new Appearance();
//        app.setPolygonAttributes(pa);
//        message.setAppearance(app);

        TransformGroup tg = new TransformGroup();
        Transform3D t3d = new Transform3D();
        t3d.setTranslation(vertex);
        tg.setTransform(t3d);
        tg.addChild(message);
        return tg;
    }


    private TransparencyAttributes makeTransparent(float alpha) {

        TransparencyAttributes ta = new TransparencyAttributes();
        if (alpha!=0)
            ta.setTransparencyMode(TransparencyAttributes.BLENDED);
        else
            ta.setTransparencyMode(TransparencyAttributes.NONE);
        ta.setTransparency(alpha);
        return ta;
    }

    private TransformGroup makeLabel(Text2D message, Vector3f pos) {

        TransformGroup labeltg = new TransformGroup();
        Transform3D labelt3d = new Transform3D();
        labelt3d.setTranslation(new Vector3f(pos.x+0.0f,pos.y+0.1f,pos.z+0.0f));
        Quat4f quat = new Quat4f();
        labelt3d.setRotation(quat);
        labeltg.setTransform(labelt3d);
        labeltg.addChild(message);

        return labeltg;

    }

    private void orbitControls(Canvas3D c){
        OrbitBehavior orbit =
                new OrbitBehavior(c, OrbitBehavior.REVERSE_ALL);
        orbit.setSchedulingBounds(bounds);

        ViewingPlatform vp = u.getViewingPlatform();
        vp.setViewPlatformBehavior(orbit);
    }

    private void setLabelVisibility(Appearance a, boolean isVisible){
        RenderingAttributes ra = new RenderingAttributes();
        ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_READ);
        ra.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
        ra.setVisible(isVisible);
        a.setRenderingAttributes(ra);
    }

    // gui stuff starts here

    private void initGUI(){

        createFrame(mf,WIDTH,HEIGHT);

        c = createCanvas(mf);

        //panels
        p0.add(simTime);
        p0.setBorder(BorderFactory.createTitledBorder("Clock Control"));

        pause.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton(p0,pause);

        JPanel beeview = new JPanel();
        beeview.setBorder(BorderFactory.createTitledBorder("Bee View"));

        JPanel plabel = new JPanel();
        plabel.setBorder(BorderFactory.createTitledBorder("Labels"));
//        plabel.add(labels);
        addButton(plabel,labels);

        JPanel p2reset = new JPanel();

        addButton(p2reset,reset);
        addButton(p2reset,save);
        addButton(p2reset,load);

        JPanel mapview = new JPanel();
        mapview.add(p2reset);
        mapview.setBorder(BorderFactory.createTitledBorder("Map View"));

        //Create the combo box
        //Indices start at 0
        String[] viewTypes = {"Behind"};
//        String[] viewTypes = {"Above","Below","Bee View"};
        viewTypeList = new JComboBox(viewTypes);
        addComboBox(beeview,viewTypeList,"Choose view");

        beeList = new JComboBox();
        addComboBox(beeview,beeList,"Choose bee");

        JPanel viewSetter = new JPanel();
        viewSetter.setBorder(BorderFactory.createTitledBorder("Set View"));
        String[] views = {"Upper left", "Upper right", "Top view"};
        viewList = new JComboBox(views);
        addComboBox(viewSetter,viewList,"Select a view");

        JPanel map = new JPanel();
        map.setLayout(new BorderLayout());

        //main panel
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.add(p0);
        main.add(plabel);
        main.add(beeview);
        main.add(mapview);
        main.add(viewSetter);
        main.add(map);

        mf.add("East", main);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getSource()==pause){

            if (!clock) {
                context.getClockControl().pause();
                pause.setText("start");
                clock = true;
            }
            else {
                context.getClockControl().start();
                pause.setText("pause");
                clock = false;
            }
        }

        if(e.getSource()==labels){
            if (!labeloff) {

                Set<Integer> objectIds = new HashSet<Integer>();
                List<SimpleBee> bees = context.getSimEngine().findModelsByType(SimpleBee.class);

                for (SimpleBee b : bees) {
                    objectIds.add(b.getObjectId());
                }
                for (int i : objectIds) {
                    Text2D text = labeltgs.get(i);
                    Appearance app = new Appearance();
                    app.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
                    app.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);
                    setLabelVisibility(app,false);
                    text.setAppearance(app);
                }
                labels.setText("on");
                labeloff = true;
            }
            else {
                Set<Integer> objectIds = new HashSet<Integer>();
                List<SimpleBee> bees = context.getSimEngine().findModelsByType(SimpleBee.class);

                for (SimpleBee b : bees) {
                    objectIds.add(b.getObjectId());
                }
                for (int i : objectIds) {
                    Text2D text = labeltgs.get(i);
                    Appearance app = new Appearance();
                    app.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
                    app.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);
                    setLabelVisibility(app,true);
                    text.setAppearance(app);
                }
                labels.setText("off");
                labeloff = false;
            }
        }

        if (e.getSource()==viewTypeList) {

            System.out.println("view List!");
            viewType = 0;

            String chosenView = (String)viewTypeList.getSelectedItem();

            if (chosenView.equals("Above")) {
                viewType = 1;
                System.out.println(""+viewType);
            }
            if (chosenView.equals("Below")) {
                viewType = 2;
                System.out.println(""+viewType);
            }
            if (chosenView.equals("Bee view")) {
                viewType = 3;
                System.out.println(""+viewType);
            }

            System.out.println(""+viewType);
        }

        if (e.getSource()==beeList) {

            int id = (Integer)beeList.getSelectedItem();
            currBee = id;

            // spawn new window
            JFrame f = new JFrame("Bee View");
            createFrame(f,VIEWWIDTH,VIEWHEIGHT);

            c2 = createCanvas(f);

            TransformGroup tg = objects.get(id);
            Transform3D t3d = new Transform3D();
            tg.getTransform(t3d);
            Vector3f pos = new Vector3f();
            t3d.get(pos);

            switch(viewType) {
                case 0:
                    System.out.println("Please select a view type");
                    break;
                //above bee
                case 1:
                    System.out.println("Curr view: "+viewType+" t3d: "+t3d);
                    t3d.rotX(Math.PI/2);
                    System.out.println("t3d: "+t3d);
                    break;
                //behind bee
                case 2:
//                    t3d.get(pos);
                    break;
                //bee view
                case 3:
                    break;
                default: break;
            }

            u.getLocale().addBranchGraph(createViewer(c2, pos));
            vp2.getViewPlatformTransform().setTransform(t3d);

            camFrame = true;
        }

        if (e.getSource()==reset) {
            setUserPos(userPosn, new Point3d(0,3,0), new Vector3d(0,1,0));
        }

        if (e.getSource()==save) {
            ViewingPlatform vp = u.getViewingPlatform();
            TransformGroup vpTg = vp.getViewPlatformTransform();
            Transform3D vpTg3d = new Transform3D();
            vpTg.getTransform(vpTg3d);

            int returnVal = fc.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();

                String filePath = file.getAbsolutePath();

                System.out.println(filePath);

                try
                {
                    FileWriter f = new FileWriter(filePath+".txt");
                    f.write(""+vpTg3d);
                    f.close();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }

        if (e.getSource()==load) {
            int returnVal = fc.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();

                String filePath = file.getAbsolutePath();
                try {

                    FileInputStream f = new FileInputStream(filePath);
                    DataInputStream in = new DataInputStream(f);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String strLine;

                    String[] tokens = null;
                    double[] values = new double[16];
                    int count = 0;

                    while ((strLine = br.readLine()) != null)   {

                        System.out.println (strLine);

                        tokens = strLine.split(",");

                        for (int i=0; i<tokens.length; i++) {
                            values[count+i] = new Double(tokens[i]).doubleValue();
                        }
                        count += 4;

                    }
                    in.close();

                    Transform3D t3d = new Transform3D();
                    t3d.set(values);

                    System.out.println("Array values: "+t3d);

                    ViewingPlatform vp = u.getViewingPlatform();
                    TransformGroup steerTG = vp.getViewPlatformTransform();
                    steerTG.setTransform(t3d);

                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            } else {
            }
        }

        if (e.getSource()==viewList) {

            String chosenView = (String)viewList.getSelectedItem();

            if (chosenView.equals("Upper left")) {
                setUserPos(new Point3d(-20,8,-20),new Point3d(0,3,0),new Vector3d(0,1,0));
            }
            if (chosenView.equals("Upper right")) {
                setUserPos(new Point3d(20,12,-20),new Point3d(0,3,0),new Vector3d(0,1,0));
            }
            if (chosenView.equals("Top view")) {
                setUserPos(new Point3d(.5,50,0),new Point3d(0,0,0),new Vector3d(0,1,0));
            }

        }

    }

    @Override
    public void keyPressed(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void keyTyped(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    private void addButton(JPanel panel, JButton button) {
        panel.add(button);
        button.addActionListener(this);
        button.addKeyListener(this);
    }

    private void addComboBox(JPanel panel,JComboBox cb, String start) {
        cb.insertItemAt(start,0);
        cb.setSelectedIndex(0);
        cb.addActionListener(this);
        panel.add(cb);
    }

    private Canvas3D createCanvas(JFrame f) {
        Canvas3D c = new Canvas3D(config);
        c.addKeyListener(this);
        f.add(c,"Center");
        c.setFocusable(true);
        return c;
    }

    private void createFrame(JFrame frame, int width, int height) {
        frame.setSize(width,height);
        frame.setLayout(new BorderLayout());
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    private void setTgCap(TransformGroup tg) {
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    }

    private void setUserPos(Point3d userPos, Point3d view, Vector3d upDir) {
        ViewingPlatform vp = u.getViewingPlatform();
        TransformGroup tg = vp.getViewPlatformTransform();
        Transform3D t3d = new Transform3D();
        tg.getTransform(t3d);

        // args are: viewer posn, where looking, up direction
        t3d.lookAt(userPos, view, upDir);
        t3d.invert();
        tg.setTransform(t3d);
    }

    public void changeColor(int objectId, Color color) {
        context.getRecorder().updateMetadata(objectId,color);
    }
}
