package harvard.robobees.simbeeotic.component;

import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.MotionListener;
import harvard.robobees.simbeeotic.model.MotionRecorder;
import java.awt.*;

import com.bulletphysics.collision.shapes.*;
import com.google.inject.Inject;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.universe.*;

import javax.media.j3d.*;
import javax.vecmath.*;
import java.util.*;

/**
 * SimpleVis draws shapes from the JBullet CollisionShape class, such as spheres, cones, cylinders, planes. 
 * Implements translation and rotation of objects as well, which are sent throught the stateUpdate method.
 *  
 * @author dicai
*/

public class SimpleVisControl extends Frame implements VariationComponent, MotionListener {
	
	private static final long serialVersionUID = 1L;

	@Inject
	@GlobalScope
	private MotionRecorder recorder;
	
	private static final Point3d USERPOSN = new Point3d(30,30,10);
	
	private SimpleUniverse u = null;
	private Canvas3D c = null;
	
	private BranchGroup objRoot = new BranchGroup(); 
	private BoundingSphere bounds = new BoundingSphere(new Point3d(0,0,0), 100.0);
	private TransformGroup tg;
	private Transform3D t3d;
	
	private Map<Integer, TransformGroup> objects = new HashMap<Integer, TransformGroup>();
	
	private final static int FLOOR = 400; 
	private final static Color3f black = new Color3f(0f, 0f, 0f);
	private final static Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
	private final static Color3f yellow = new Color3f(0.9f, 0.9f, 0.1f);
	private final static Color3f specular = new Color3f(0.9f,0.9f,0.9f);
	private final static Color3f brown = new Color3f(0.3f, 0.16f, 0.16f);
	private final static Color3f green = new Color3f(0.3f, 0.9f, 0.4f);
	private final static Color3f red = new Color3f(1.0f, 0.0f, 0.0f);
	
	//userinterface
	private Button start = new Button("Start");
	private Button pause = new Button("Pause");
	
	public BranchGroup createSceneGraph() {
        
        addLights(); // add lights
        addBackground(); // add sky
        //addGround();
        labelAxes();
  
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
	
	private void addGround() {
		
		Material brownMat = new Material(green,black,green,specular,25.0f);
		brownMat.setLightingEnable(true);
		Appearance brownApp = new Appearance();
		brownApp.setMaterial(brownMat);
		
		Box ground = new Box(100,100,0,brownApp);
		objRoot.addChild(ground);
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
	    t3d.lookAt( USERPOSN, new Point3d(0,0,5), new Vector3d(0,0,1));
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
		
		recorder.addListener(this);
		
		setSize(800,500);
        setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        
        Panel p = new Panel();
        p.add(start);
        p.add(pause);
        //p.add();
        add("West",p);

        c = new Canvas3D(config);
        add("Center",c);
        
        setVisible(true);
        c.setFocusable(true); 
        
        
        // attach scene to universe
    	u = new SimpleUniverse(c);
        
        // creates object
        createSceneGraph(); 
        
        initUserPosition();
        
        orbitControls(c);
        
        u.addBranchGraph(objRoot);
		
	}

	@Override
	public void shutdown() {
		// close frame
		// dispose();
		
	}

	@Override
	public void initializeObject(int objectId, CollisionShape shape) {
		System.out.println("Object id: " + objectId);
		System.out.println("CollisionShape: " + shape.getName());
		
		// recognize and draw correct shape
		if (shape instanceof SphereShape) {
			System.out.println("Now drawing sphere!");
			
			SphereShape s = (SphereShape) shape;
			float radius = s.getRadius();
			//System.out.println("Radius: " + radius);
			drawSphere(objectId, radius);
			u.addBranchGraph(objRoot);
			
		}
		else if (shape instanceof CylinderShape) {
			CylinderShape c = (CylinderShape) shape;
			float r = c.getRadius();
			//System.out.println("Radius: " + r);
			
			Vector3f halfExtents = new Vector3f();
			c.getHalfExtentsWithoutMargin(halfExtents);
			System.out.println("Half extents: " + halfExtents);
			
			drawCylinder(objectId, r, Math.abs(2*halfExtents.z));
			u.addBranchGraph(objRoot);
		}	
//		else if (shape instanceof CylinderShapeX) {
//			CylinderShape c = (CylinderShape) shape;
//			float r = c.getRadius();
//			//System.out.println("Radius: " + r);
//			
//			Vector3f halfExtents = new Vector3f();
//			c.getHalfExtentsWithoutMargin(halfExtents);
//			//System.out.println("Half extents: " + halfExtents);
//			
//			drawCylinder(objectId, r, 2*halfExtents.x);
//			u.addBranchGraph(objRoot);
//		}	
		else if (shape instanceof BoxShape) { 
			BoxShape b = (BoxShape) shape;
			Vector3f halfExtents = new Vector3f();
			b.getHalfExtentsWithoutMargin(halfExtents);
			//System.out.println("Half extents: " + halfExtents);
			drawBox(objectId, Math.abs(2*halfExtents.x), Math.abs(2*halfExtents.y), Math.abs(2*halfExtents.z));
			u.addBranchGraph(objRoot);
		}	
		else if (shape instanceof ConeShape) {
			ConeShape c = (ConeShape) shape;
			float r = c.getRadius();
			float h = c.getHeight();
			drawCone(objectId, r, h);
			u.addBranchGraph(objRoot);
		}
		else if (shape instanceof StaticPlaneShape){
			StaticPlaneShape s = (StaticPlaneShape) shape;
			float l = s.getPlaneConstant();
			
			System.out.println("Plane constant: " + l);
			
			Vector3f normal = new Vector3f();
			s.getPlaneNormal(normal);
			
			System.out.println("Normal vector: " + normal);
			
			drawBox(objectId,6000,0,6000);
			
			//objects.put(objectId, tg);
			u.addBranchGraph(objRoot);
		}
		else if (shape instanceof CompoundShape){
		
			drawCompoundShape(objectId, shape);
			
		}
		else 
			System.out.println("Object not recognized!");
		
		// put ID and BranchGroup object into hash table
		//objects.put(objectId,bee);
		
		//System.out.println(objectId + "" + shape);
		
	}

	@Override
	public void stateUpdate(int objectId, Vector3f position, Quat4f orientation) {
		
		//System.out.println("Object id: " + objectId + " Position: " + position);
		
		// get object from the ID
		TransformGroup group = objects.get(objectId);
		Transform3D t3D = new Transform3D();	
		t3D.setRotation(orientation);
		t3D.rotX( Math.PI/2 ); 
		t3D.setTranslation(position);
		group.setTransform(t3D);
			
		// update orientation with setRotation
		
	}
	
	@Override
	public void metaUpdate(int objectId, Color color, String label) {
		// TODO Auto-generated method stub
		
	}
	
	private void drawSphere(int objectId, float radius) {

		//appearance 
		Material yellowMat = new Material(yellow,black,yellow,specular,25.0f);
		yellowMat.setLightingEnable(true);
		Appearance yellowApp = new Appearance();
		yellowApp.setMaterial(yellowMat);
		
		// position 
		objRoot = new BranchGroup();
		t3d = new Transform3D();
		t3d.rotX( -Math.PI/2.0 );
	    tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new Sphere(radius, yellowApp)); 
	    objRoot.addChild(tg);
		
	    // place TransformGroup into hash
	    objects.put(objectId,tg);

	}
	
	private void drawCylinder(int objectId, float radius, float height) {
		
		// change colors later
		Material yellowMat = new Material(red,black,red,specular,25.0f);
		yellowMat.setLightingEnable(true);
		Appearance yellowApp = new Appearance();
		yellowApp.setMaterial(yellowMat);
		
		// position 
		objRoot = new BranchGroup();
		t3d = new Transform3D();
		t3d.rotX( -Math.PI/2 );
	    tg = new TransformGroup(t3d);
	    Transform3D t = new Transform3D();
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new Cylinder(radius, height, yellowApp)); 
	    objRoot.addChild(tg);
	    
	    objects.put(objectId,tg);
		
	}
	
	private void drawCone(int objectId, float r, float h) {
		Material brownMat = new Material(brown,black,brown,specular,25.0f);
		brownMat.setLightingEnable(true);
		Appearance brownApp = new Appearance();
		brownApp.setMaterial(brownMat);
		
		// position 
		objRoot = new BranchGroup();
		t3d = new Transform3D();
		t3d.rotX( -Math.PI/2.0 );
	    tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new Cone (r, h, brownApp)); 
	    objRoot.addChild(tg);
	    
	    objects.put(objectId,tg);
	}
	
	private void drawBox(int objectId, float l, float w, float h) {
		Material brownMat = new Material(green,black,brown,specular,25.0f);
		brownMat.setLightingEnable(true);
		Appearance brownApp = new Appearance();
		brownApp.setMaterial(brownMat);
		
		// position 
		objRoot = new BranchGroup();
		t3d = new Transform3D();
		t3d.rotX( -Math.PI/2.0 );
	    tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new Box(l,w,h,brownApp)); 
	    objRoot.addChild(tg);
	    
	    objects.put(objectId,tg);

	}
	
	private void drawCompoundShape(int objectId, CollisionShape shape){
		CompoundShape s = (CompoundShape) shape;
		int numChild = s.getNumChildShapes();
		
		CollisionShape[] cShapes = new CollisionShape[numChild]; 
		for (int i = 0; i<numChild; i++) {
			cShapes[i] = s.getChildShape(i);
		}
		
		objRoot = new BranchGroup();
		t3d = new Transform3D();
		t3d.rotX( -Math.PI/2.0 );
	    tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    
	    Material brownMat = new Material(brown,black,brown,specular,25.0f);
		brownMat.setLightingEnable(true);
		Appearance brownApp = new Appearance();
		brownApp.setMaterial(brownMat);
		
		for (CollisionShape c: cShapes) {
		    
			if (c instanceof SphereShape){
				SphereShape sphere = (SphereShape)c;
				Float r = sphere.getRadius();
				
				tg.addChild(new Sphere(r,brownApp)); 
			    objRoot.addChild(tg);
			}
			else if (c instanceof CylinderShape){
				CylinderShape cylinder = (CylinderShape)c;
				Float r = cylinder.getRadius();
				Vector3f halfExtents = new Vector3f();
				cylinder.getHalfExtentsWithoutMargin(halfExtents);
				
				tg.addChild(new Cylinder(r,2*Math.abs(halfExtents.x),brownApp));
				objRoot.addChild(tg);
			}
			else if (c instanceof BoxShape){
				BoxShape box = (BoxShape)c;
				Vector3f halfExtents = new Vector3f();
				box.getHalfExtentsWithoutMargin(halfExtents);
				
				tg.addChild(new Box(2*Math.abs(halfExtents.x),
						2*Math.abs(halfExtents.y),2*Math.abs(halfExtents.z),brownApp));
				objRoot.addChild(tg);
				
			}
			else if (c instanceof ConeShape){
				ConeShape cone = (ConeShape)c;
				Float r = cone.getRadius();
				Float h = cone.getHeight();
				
				tg.addChild(new Cone(r,h,brownApp));
				objRoot.addChild(tg);
				
			}
			else 
				System.out.println("Child not recognized!");
			
		}
		objects.put(objectId,tg);
	}

}
