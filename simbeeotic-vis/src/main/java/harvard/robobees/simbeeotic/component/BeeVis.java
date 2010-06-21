package harvard.robobees.simbeeotic.component;

import harvard.robobees.simbeeotic.model.MotionListener;
import harvard.robobees.simbeeotic.model.MotionRecorder;
import java.awt.*;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.google.inject.Inject;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.universe.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import java.util.*;


public class BeeVis extends Frame implements VariationComponent, MotionListener {
	
	@Inject
	private MotionRecorder recorder;
	
	private static final Point3d USERPOSN = new Point3d(0,5,50);
	
	private SimpleUniverse u = null;
	private Canvas3D c = null;
	
	private BranchGroup objRoot;
	private BranchGroup floorBG;
	private BoundingSphere bounds;
	private TransformGroup tg;
	private Transform3D t3d;
	
	private BranchGroup bee = new BranchGroup();
	private BranchGroup flower = new BranchGroup();
	private BranchGroup obstacle = new BranchGroup();
	
	private Map<Integer, BranchGroup> objects = new HashMap<Integer, BranchGroup>();
	
	public BranchGroup createSceneGraph() {

        // create root of branch graph
        objRoot = new BranchGroup(); 
        bounds = new BoundingSphere(new Point3d(0,0,0), 100.0);
        
        addLights(); // add lights
        addBackground(); // add sky
        addGround();
 
        //addObject(i, pos); // add object
        
        floorBG = new BranchGroup();
        //labelAxes();

  
        // optimizations
        objRoot.compile();

        return objRoot;
       
    }
	
	private void addLights() {
		Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
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
		//Color3f brown = new Color3f(0.3f, 0.16f, 0.16f);
		Color3f brown = new Color3f(0.1f, 0.9f, 0.1f); //bright green (ew)
		Color3f black = new Color3f(0,0,0);
		Color3f specular = new Color3f(0.8f,0.9f,0.9f);
		
		Material brownMat = new Material(brown,black,brown,specular,25.0f);
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
	    t3d.lookAt( USERPOSN, new Point3d(0,0,0), new Vector3d(0,1,0));
	    t3d.invert();

	    steerTG.setTransform(t3d);
	  } 
	  
	  
	  
	  

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		recorder.addListener(this);
		
		setSize(500,500);
        setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

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
		dispose();
		
	}

	@Override
	public void initializeObject(int objectId, CollisionShape shape) {
		//objects = new TreeMap();
		
		// recognize what shape it is
		
		// if sphere, draw a bee
		createBee();
		
		// BranchGroup for the shape
		
		// put ID and BranchGroup object into hash table
		//objects.put(objectId,bee);
		
		System.out.println(objectId + "" + shape);
		
	}

	@Override
	public void stateUpdate(int objectId, Vector3f position, Quat4f orientation) {
		// get object from the ID
		
		
		
		// update position with setTranslation
		t3d.setTranslation(position);
    	tg.setTransform(t3d);
		
		
		// update orientation with setRotation
		
	}
	
	@Override
	public void metaUpdate(int objectId, Color color, String label) {
		// TODO Auto-generated method stub
		
	}
	
	private void createBee() {

		//appearance 
		Color3f yellow = new Color3f(0.9f, 0.9f, 0.1f);
		Color3f black = new Color3f(0,0,0);
		Color3f specular = new Color3f(0.9f,0.9f,0.9f);
		
		Material yellowMat = new Material(yellow,black,yellow,specular,25.0f);
		yellowMat.setLightingEnable(true);
		Appearance yellowApp = new Appearance();
		yellowApp.setMaterial(yellowMat);
		
		
		// position 
		t3d = new Transform3D();
	    //t3d.set( pos ); // assume for now that it starts out at 0,0,0
	    tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new Sphere(0.5f, yellowApp)); 
	    objRoot.addChild(tg);
		
	}
	
	private void createFlower() {
		
		// change colors later
		
		Color3f yellow = new Color3f(0.9f, 0.9f, 0.1f);
		Color3f black = new Color3f(0,0,0);
		Color3f specular = new Color3f(0.9f,0.9f,0.9f);
		
		Material yellowMat = new Material(yellow,black,yellow,specular,25.0f);
		yellowMat.setLightingEnable(true);
		Appearance yellowApp = new Appearance();
		yellowApp.setMaterial(yellowMat);
		
		
		// position 
		t3d = new Transform3D();
	    //t3d.set( pos ); // assume for now that it starts out at 0,0,0
	    tg = new TransformGroup(t3d);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	    tg.addChild(new Cylinder(0.3f, 0.2f, yellowApp)); 
	    objRoot.addChild(tg);
		
	}
	
	private void createObstacle() {
		
	}
	

	

}
