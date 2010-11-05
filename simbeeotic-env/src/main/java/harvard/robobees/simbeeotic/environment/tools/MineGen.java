package harvard.robobees.simbeeotic.environment.tools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;

// xml stuff
import org.w3c.dom.*;

import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import javax.vecmath.Vector3f;


/**
 * @author chartier
 */
public class MineGen {
    
	// 'skew' of the branches of each tunnel in radians. PI/2 has branching tunnels perpendicular to main shaft, less creates acute angle
	private static float tunnelBranchSkew = (float) Math.PI/4;
	
	// probability of a branching tunnel being generated off of the main tunnel, probability decreases for branches of branches
	private static float tunnelBranchProb = 0.3f;
	
	// bounds of tunnel system
	private static int maxTunnelRegionWidth = 8;
	private static int maxTunnelRegionLength = 10;
	
	// size of tunnels
	private static float tunnelWidth = 1;
	
	// number of 'tunnel levels'
	private static int levels = 10;
		
	// physical dimensions of the main shaft
	private static float shaftHeight = tunnelWidth * levels;
	private static float shaftDiameter = 4;
	private static int shaftNumberOfSides = 50;
	
	// probability of a tunnel branching from the main shaft
	private static float tunnelProb = 0.3f;
	
	public static void main(String[] args) throws FileNotFoundException, ParserConfigurationException, TransformerException
	{
        // create the xml Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
		
        // create the root world element and add it to the document
        Element world = doc.createElement("world");
        world.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        world.setAttribute("xmlns", "http://harvard/robobees/simbeeotic/configuration/world");
        world.setAttribute("xsi:schemaLocation", "http://harvard/robobees/simbeeotic/configuration/world http://some/path/to/schema.xsd");
        doc.appendChild(world);
		
        // create radius element as a child of world
        Element radius = doc.createElement("radius");
        world.appendChild(radius);
        
        // insert value for the radius of the world (MAGIC 2000)
        Text radiusValue = doc.createTextNode("2000");
        radius.appendChild(radiusValue);
        
        // add structures child element
        Element structures = doc.createElement("structures");
        world.appendChild(structures);
        
        // add people child element
        Element people = doc.createElement("people");
        world.appendChild(people);
        
		// generate shaft and get tunnel positions
		Vector3f[] tunnelPositions = new Vector3f[levels];
		Shaft.gen(doc, tunnelPositions, shaftHeight, shaftDiameter, shaftNumberOfSides, tunnelProb, tunnelWidth, structures);
		
		// at each tunnel position, generate a tunnel
		for(int i = 0; i < tunnelPositions.length; i++) {
			if(tunnelPositions[i] != null)
				Tunnel.gen(tunnelPositions[i], doc, tunnelBranchSkew,
							  tunnelWidth, maxTunnelRegionWidth, maxTunnelRegionLength, tunnelBranchProb, structures, people);
		}
		
        // Output the XML
        // set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        // create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        String xmlString = sw.toString();

        PrintWriter out = new PrintWriter("/home/chartier/workspace/simbeeotic/simbeeotic-examples/src/main/resources/scenarios/minegenoutput.xml");
        
        // print xml
        out.print(xmlString);
        out.flush();
        out.close();
	}
	
}
