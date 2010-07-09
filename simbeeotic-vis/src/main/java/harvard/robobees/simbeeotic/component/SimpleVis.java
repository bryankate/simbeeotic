package harvard.robobees.simbeeotic.component;

import harvard.robobees.simbeeotic.ClockListener;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.MotionListener;
import harvard.robobees.simbeeotic.model.SimpleBee;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Cylinder;
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

public class SimpleVis extends JPanel implements VariationComponent, MotionListener, ActionListener, KeyListener, ClockListener {
	
	private static final long serialVersionUID = 1L;

	@Inject
	private VariationContext context;
	//ClockControl clock;
	
	private static Point3d userPosn = new Point3d(-20,12,20);
	
	private SimpleUniverse u = null;
	private Canvas3D c = null;
	private Canvas3D c2 = null;
	GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
	
	private BranchGroup objRoot = new BranchGroup(); 
	private BoundingSphere bounds = new BoundingSphere(new Point3d(0,0,0), 100.0);
	//private TransformGroup tg;
	private TransformGroup objTrans;
	Transform3D objTrans3D = new Transform3D();
	//private Transform3D t3d;
	
	private Map<Integer, TransformGroup> objects = new HashMap<Integer, TransformGroup>();
	private Map<Integer, Color3f> colors = new HashMap<Integer, Color3f>();
	private Map<Integer, Appearance> app = new HashMap<Integer, Appearance>();
	//private Map<Integer, Vector3f> pos = new HashMap<Integer, Vector3f>();
	
	private final static int FLOOR = 400; 
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
	
	private final static int HEIGHT = 700;
	private final static int WIDTH = 1000;
	private final static int CLIPDIST = 30;
	
	// GUI elements
	private JButton pause = new JButton("pause");
	private JButton start = new JButton("start");
	private JButton reset = new JButton("reset");
	private JButton labels = new JButton("labels");
	//private Button camera = new Button("camera view");
	private JTextField simTime = new JTextField(5);
	private JComboBox beeList;
	private boolean firstTime = true;
	JFrame mf = new JFrame("Simbeeotic Visualization");
	JPanel p0 = new JPanel();
	JPanel main = new JPanel();
	
	private ViewingPlatform vp2 = new ViewingPlatform(1);
	
	private boolean camFrame = false;
	private int currBee = 0;
	
	private BranchGroup createSceneGraph() {
        
        addLights(); // add lights
        addBackground(); // add sky
        labelAxes();
        
        //objRoot.addChild(new Sphere(100));
        
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
		Background back = new Background();
        back.setApplicationBounds(bounds);
        back.setColor(0.17f, 0.55f, 0.82f); //sky color
        objRoot.addChild(back);
	}
	
	private void orbitControls(Canvas3D c){
	    OrbitBehavior orbit = 
	        new OrbitBehavior(c, OrbitBehavior.REVERSE_ALL);
	    orbit.setSchedulingBounds(bounds);

	    ViewingPlatform vp = u.getViewingPlatform();
	    vp.setViewPlatformBehavior(orbit);     
	  } 
	
	  private void initUserPosition()
	  // Set the user's initial viewpoint using lookAt()
	  {
	    ViewingPlatform vp = u.getViewingPlatform();
	    TransformGroup steerTG = vp.getViewPlatformTransform();

	    Transform3D t3d = new Transform3D();
	    steerTG.getTransform(t3d);

	    // args are: viewer posn, where looking, up direction
	    t3d.lookAt(userPosn, new Point3d(0,3,0), new Vector3d(0,1,0));
	    t3d.invert();

	    steerTG.setTransform(t3d);
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
	    Text2D message = new Text2D(text, black, "SansSerif", 76, Font.BOLD );

	    TransformGroup tg = new TransformGroup();
	    Transform3D t3d = new Transform3D();
	    t3d.setTranslation(vertex);
	    tg.setTransform(t3d);
	    tg.addChild(message);
	    return tg; 
	  } 
  
	@Override
	public void initialize() {
	
		initGUI();
		
		context.getRecorder().addListener(this);
	
        context.getClockControl().addListener(this);
        
        // attach scene to universe
    	u = new SimpleUniverse(c);
    	
    	u.getViewer().getView().setBackClipDistance(CLIPDIST);
        
        // creates object
        createSceneGraph(); 
        
        initUserPosition();
        
        orbitControls(c);
     
        u.addBranchGraph(objRoot);
        
        //u.getLocale().addBranchGraph(createViewer(c2, new Vector3f(10,10,-10)));
	}

	@Override
	public void shutdown() {
		// close frame
		// dispose();	
		mf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	@Override
	public void initializeObject(int objectId, CollisionShape shape) {
//		System.out.println("Object id: " + objectId);
//		System.out.println("CollisionShape: " + shape.getName());
		
		// sphere shape
		if (shape instanceof SphereShape) {
//			System.out.println("Now drawing sphere!");
			
			SphereShape s = (SphereShape) shape;
			float radius = s.getRadius();
			//System.out.println("Radius: " + radius);
			
			drawSphere(objectId, yellow, radius);
			u.addBranchGraph(objRoot);
			
		}
		// cylinder shape
		else if (shape instanceof CylinderShape) {
			CylinderShape c = (CylinderShape) shape;
			float r = c.getRadius();
			//System.out.println("Radius: " + r);
			
			Vector3f halfExtents = new Vector3f();
			c.getHalfExtentsWithMargin(halfExtents);
//			System.out.println("Half extents: " + halfExtents);
			
			drawCylinder(objectId, pink, r, Math.abs(halfExtents.z));
			
			u.addBranchGraph(objRoot);
		}	
		// box shape
		else if (shape instanceof BoxShape) { 
			BoxShape b = (BoxShape) shape;
			Vector3f halfExtents = new Vector3f();
			b.getHalfExtentsWithMargin(halfExtents);
			//System.out.println("Half extents: " + halfExtents);
			
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
			float l = s.getPlaneConstant();
			
//			System.out.println("Plane constant: " + l);
			
			Vector3f normal = new Vector3f();
			s.getPlaneNormal(normal);
			
//			System.out.println("Normal vector: " + normal);
			
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
		t3D.setRotation(orientation);

		t3D.setTranslation(position);
		
		group.setTransform(t3D);
		
		// store position in map
		//pos.put(objectId,position);
		
		if (camFrame && currBee==objectId){
			
			TransformGroup currBeeGroup = objects.get(currBee);
			
			Transform3D t3d = new Transform3D();
			currBeeGroup.getTransform(t3d);
			//t3d.rotY(-Math.PI/2);
			//t3d.setTranslation(position);
			
			vp2.getViewPlatformTransform().setTransform(t3d);
		}
		
		makeLabel(objectId, "aa"+objectId, new Vector3f(position.x+0.1f,position.y+0.1f,position.z+0.1f));
	}
	
	@Override
	public void metaUpdate(int objectId, Color color, String label) {
		// updates color
		
		Color3f col = new Color3f();
		col.set(color);
		
		Appearance appear = app.get(objectId);
		appear.setMaterial(new Material(col,black,col,specular,25.0f));
		
		// updates label
		//makeLabel(objectId,label);
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
	
	private void makeLabel(int objectId, String objectLabel, Vector3f position) {
		//Vector3f position = pos.get(objectId);
		Vector3d vertex = new Vector3d(position);
		
		makeText(vertex,objectLabel);
	}

	private void drawSphere(int objectId, Color3f color, float radius) {

		//appearance 
		Material mat = new Material(color,black,color,specular,25.0f);
		mat.setLightingEnable(true);
		Appearance appear = new Appearance();
		appear.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
		appear.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
		appear.setMaterial(mat);
		
		// position 
		objRoot = new BranchGroup();
		objTrans = new TransformGroup(objTrans3D);
		Transform3D t3d = new Transform3D();
		
	    TransformGroup tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new Sphere(radius, appear)); 
	    objTrans.addChild(tg);
	    objRoot.addChild(objTrans);
		
	    // place TransformGroup into hash
	    objects.put(objectId,tg);
	    colors.put(objectId,color);
	    app.put(objectId, appear);
	}
	
	private void drawCylinder(int objectId, Color3f color, float radius, float height) {
		
		// change colors later
		Material mat = new Material(color,black,color,specular,25.0f);
		mat.setLightingEnable(true);
		Appearance appear = new Appearance();
		appear.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
		appear.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
		appear.setMaterial(mat);
		
		// position 
		objRoot = new BranchGroup();
		objTrans = new TransformGroup(objTrans3D);
		Transform3D t3d = new Transform3D();

	    TransformGroup tg = new TransformGroup(t3d);

	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new Cylinder(radius, height, appear)); 
	    
	    objTrans.addChild(tg);
	    objRoot.addChild(objTrans);
	    
	    objects.put(objectId,tg);
	    colors.put(objectId,color);
	    app.put(objectId,appear);
	}
	
	private void drawCone(int objectId, Color3f color, float r, float h) {
		Material mat = new Material(color,black,color,specular,25.0f);
		mat.setLightingEnable(true);
		Appearance appear = new Appearance();
		appear.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
		appear.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
		appear.setMaterial(mat);
		
		// position 
		objRoot = new BranchGroup();
		objTrans = new TransformGroup(objTrans3D);
		Transform3D t3d = new Transform3D();

	    TransformGroup tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new Cone (r, h, appear)); 
	    objTrans.addChild(tg);
	    objRoot.addChild(objTrans);
	    
	    objects.put(objectId,tg);
	    colors.put(objectId,color);
	    app.put(objectId,appear);
	}
	
	private void drawBox(int objectId, Color3f color, float l, float w, float h) {
		Material mat = new Material(color,black,color,specular,25.0f);
		mat.setLightingEnable(true);
		Appearance appear = new Appearance();
		appear.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
		appear.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
		appear.setMaterial(mat);
		
		// position 
		objRoot = new BranchGroup();
		objTrans = new TransformGroup(objTrans3D);
		Transform3D t3d = new Transform3D();

	    TransformGroup tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new com.sun.j3d.utils.geometry.Box(l,w,h,appear)); 
	    objTrans.addChild(tg);
	    objRoot.addChild(objTrans);
	    
	    objects.put(objectId,tg);
	    colors.put(objectId,color);
	    app.put(objectId,appear);
	}
	
	private void drawCompoundShape(int objectId, Color3f color, CollisionShape shape){
		CompoundShape s = (CompoundShape) shape;
		
		objRoot = new BranchGroup();
		objTrans = new TransformGroup(objTrans3D);
		Transform3D t3d = new Transform3D();

	    TransformGroup tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    
	    Material mat = new Material(color,black,color,specular,25.0f);
		mat.setLightingEnable(true);
		Appearance appear = new Appearance();
		appear.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
		appear.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
		appear.setMaterial(mat);
		
		TransparencyAttributes ta = new TransparencyAttributes();
		ta.setTransparencyMode(TransparencyAttributes.BLENDED);
		ta.setTransparency(0.6f);
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
			
				t3D.setRotation(rot);

				t3D.setTranslation(trans.origin);
				
				TransformGroup childTg = new TransformGroup(t3D);

				childTg.addChild(new Sphere(r,appear));
				tg.addChild(childTg);
			}
			else if (c instanceof CylinderShape){
				CylinderShape cylinder = (CylinderShape)c;
				Float r = cylinder.getRadius();
//				System.out.println("Radius: " + r);
				
				Vector3f halfExtents = new Vector3f();
				cylinder.getHalfExtentsWithMargin(halfExtents);
				
				Transform3D t3D = new Transform3D();
				Quat4f rot = new Quat4f();
				
				trans.getRotation(rot);
				
				t3D.setRotation(rot);
				t3D.rotX(-Math.PI/2);
				t3D.setTranslation(trans.origin);
							
				TransformGroup childTg = new TransformGroup(t3D);
				
				childTg.addChild(new Cylinder(r,Math.abs(halfExtents.z),appear));
				
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

				t3D.setTranslation(trans.origin);

				TransformGroup childTg = new TransformGroup(t3D);

				childTg.addChild(new Cone(r,h,appear));
				tg.addChild(childTg);
				
			}
			else 
				System.out.println("Child not recognized!");
		    
			
		}
	
		objTrans.addChild(tg);
		objRoot.addChild(objTrans);
		
		objects.put(objectId,tg);
		colors.put(objectId,color);
		app.put(objectId,appear);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getSource()==pause){
			context.getClockControl().pause();
		}
		if(e.getSource()==start){
			context.getClockControl().start();
		}
		if(e.getSource()==reset){
			//userPosn = new Point3d(-20,12,20);
		}
		else if (e.getSource() == beeList) {

			int id = (Integer)beeList.getSelectedItem();
			
			currBee = id;

			System.out.println(""+id);

			// spawn new window
			JFrame f = new JFrame("Bee View");
			f.setSize(400,400);
			f.setLayout(new BorderLayout());

			c2 = new Canvas3D(config);
			c2.setSize(400,400);
			f.add(c2,"Center");
			f.setVisible(true);
			c2.setFocusable(true);
			
			TransformGroup tg = objects.get(id);
			
			Transform3D t3d = new Transform3D();
			tg.getTransform(t3d);
			//t3d.setTranslation(position);
			
			Vector3f pos = new Vector3f();
			t3d.get(pos);
			
			u.getLocale().addBranchGraph(createViewer(c2, pos));
			
			vp2.getViewPlatformTransform().setTransform(t3d);
			
			camFrame = true;
			
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

	private void initGUI(){
		
		//canvas and size of mainframe
		mf.setSize(WIDTH,HEIGHT);
        mf.setLayout(new BorderLayout());
        
        mf.setContentPane(new JPanel(new BorderLayout()));

        c = new Canvas3D(config);
        c.addKeyListener(this);
        mf.add(c,"Center");
        c.setFocusable(true);
        mf.pack();
        mf.setVisible(true);
    
		//panels
        JLabel sTime = new JLabel("Simulation Time: ");
        //p0.add(sTime);
        p0.add(simTime);
        LineBorder ln = new LineBorder(java.awt.Color.black);
        p0.setBorder(BorderFactory.createTitledBorder("Clock Control"));
		
		JPanel p1 = new JPanel();
//		p0.add(new JSeparator(SwingConstants.HORIZONTAL));

		JLabel simControl = new JLabel("Control: ");
		p1.add(simControl); 
		pause.setAlignmentX(Component.CENTER_ALIGNMENT);
		p1.add(pause);
		pause.addActionListener(this);
		pause.addKeyListener(this);
		start.setAlignmentX(Component.CENTER_ALIGNMENT);
		p1.add(start);
		start.addActionListener(this);
		start.addKeyListener(this);
		p0.add(javax.swing.Box.createVerticalGlue());
		p0.add(start);
		p0.add(javax.swing.Box.createVerticalGlue());
		p0.add(pause);
		
		JPanel p2 = new JPanel();
//		JLabel label = new JLabel("View: ");
//		p2.add(label); 
		p2.setBorder(BorderFactory.createTitledBorder("View"));
		p2.add(labels);
		p2.add(reset);
		reset.addActionListener(this);
		reset.addKeyListener(this);
		
		JPanel p3 = new JPanel();
		JLabel beeView = new JLabel("Bee view ID: ");
		//p3.add(beeView);

        //Create the combo box
        //Indices start at 0
        beeList = new JComboBox();
        beeList.insertItemAt("Select a bee",0);
        beeList.setSelectedIndex(0);
        beeList.addActionListener(this);
        p2.add(beeList);
        
        p3.setBorder(BorderFactory.createTitledBorder("2D Map"));

		//main panel
		
		main.setLayout(new BoxLayout(main, BoxLayout.PAGE_AXIS));
//        main.setLayout(new FlowLayout(FlowLayout.LEADING, FlowLayout.CENTER, FlowLayout.TRAILING));
		main.add(p0);
//		main.add(p1);
		main.add(p2);
		main.add(p3);
		
		mf.add("East", main);

		
	}
	
	private ViewingPlatform createViewer(Canvas3D c, Vector3f pos) {
		
		Viewer viewer = new Viewer(c);
			
		vp2.setPlatformGeometry(null);
		
		// initial position for viewer
		Transform3D t3d = new Transform3D();
		t3d.setTranslation(pos);
		vp2.getViewPlatformTransform().setTransform(t3d);
		
		vp2.getViewPlatformTransform().setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		vp2.getViewPlatformTransform().setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		
		viewer.setViewingPlatform(vp2);
		
		//orbitControls(c);
		
		return vp2;
	}
	
	private void getBeeID(){

		
		//return beeId;
	}


	
//	private void setUserPosn(Point3d userPos) {
//		userPosn = userPos;
//	}
}
