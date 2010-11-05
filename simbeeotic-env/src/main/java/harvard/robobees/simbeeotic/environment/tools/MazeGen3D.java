package harvard.robobees.simbeeotic.environment.tools;

import harvard.robobees.simbeeotic.environment.tools.graph.Edge;
import harvard.robobees.simbeeotic.environment.tools.graph.DisjointSets;

import java.io.FileNotFoundException;
import java.io.PrintWriter;


/**
 * Generates an XML world file containing a environment composed of a three-dimensional maze.
 * User may modify number of rows, columns, floors, the scale of the maze, and the probability of 
 * removing additional walls to create sparser environments.
 *
 * @author chartier
 */

// TODO: Modify this so that it is actually different!

public class MazeGen3D {
	
	public static void main(String[] args) throws FileNotFoundException
	{
		// MAGIC! This stuff should be taken from command-line input later.
		// works up to 150 x 150 confirmed (with 0.3 for random value),
		// higher numbers seem to cause problems (overflow issues)
		int cols = 5;
		int rows = 5;
		int levels = 5;
		
		// include entrance and exit openings in outer walls?
		int entrance = 1;
		int exit = levels;
		float scale = 1;
		
		// some hard-coded values
		float length = 1 * scale;
		float width = 0.1f * scale;
		float height = 1 * scale;
		float surfaceHeight = 0.1f * scale;
		
		// dimensions of the maze
		float mazeX = cols * scale;
		float mazeY = rows * scale;
		float mazeZ = levels * scale;
		float bottomLength, topLength;
		
		float x;
		float y;
		float z;
		float rotation;
		int source;
		
		// number of cells and interior walls
		int numCells = rows * cols * levels;
		
		// calculating numWalls (may need to review this)
//		(i * (j - 1) + j * (i - 1)) * k + (k - 1) * i * j
//		(i*j - i + i * j - j) * k + k*i + k*j - i - j
//		2*i*j*k -i*k - j*k + k*i + k*j - i - j
//		2*i*j*k - i - j

		// create disjoint sets representing cells
		// cells unite sets if wall between them is removed
		DisjointSets sets = new DisjointSets(numCells);

		// number of vertical walls (between cells adjacent parallel to x axis)
		// and horizontal walls (adjacent parallel to y axis)
		int numVertWalls = (rows * levels) * (cols - 1);
		int numHorizWalls = (cols * levels) * (rows - 1);
		int numSurfaces = (rows * cols) * (levels - 1);
		int numWalls = numVertWalls + numHorizWalls + numSurfaces;
		
		// create an array of edges representing walls
		Edge[] edges = new Edge[numWalls];
		
		// set up output
		PrintWriter out = new PrintWriter("/home/chartier/workspace/simbeeotic/simbeeotic-examples/src/main/resources/scenarios/mazegen3Doutput.xml");
		
		// write out initial XML tags
		out.println("<?xml version=\"1.0\"?><world xmlns:xsi=\"http://www.w3.org" + 
					"/2001/XMLSchema-instance\" xmlns=\"http://harvard/robobees" + 
					"/simbeeotic/configuration/world\" xsi:schemaLocation=" + 
					"\"http://harvard/robobees/simbeeotic/configuration/world http://some/path/to/schema.xsd\">");
		out.println("<radius>2000</radius>");
		out.println("<structures>");
		
		// begin with the first cell (closest to origin)
		source = 0;
		// create the 'vertical' walls (between columns)
		for(int i = 0; i < numVertWalls; i++) {
			if((source + 1) % cols == 0)
				source++;
			edges[i] = new Edge(source, source + 1);
			source++;
		}
		
		source = 0;
		// create the 'horizontal walls' (between rows)
		for(int j = 0; j < numHorizWalls; j++) {
			// top row? jump to the first row of next level
			if(((source % (rows*cols)) / cols) == (rows - 1))
				source += cols;
			edges[numVertWalls + j] = new Edge(j, j + cols);
			source++;
		}
		
		// create the 'flat surfaces' (between levels)
		for(int k = 0; k < numSurfaces; k++)
		{
			edges[numVertWalls + numHorizWalls + k] = new Edge(k, k + rows*cols);
		}
		
		// generate a random permutation of integers [0, numWalls) to
		// determine the order in which we shall consider the walls
		int[] permutation = new int[numWalls];
		
		for(int i = 0; i < numWalls; i++) {
			permutation[i] = i;
		}	
		
		for(int i = 0; i < numWalls; i++) {
			int r = (int) (Math.random() * (i + 1));
			int swap = permutation[r];
			permutation[r] = permutation[i];
			permutation[i] = swap;
		}
		
		// for each wall, if the 'source' and 'dest' cells are from disjoint sets,
		// unite them and remove the wall.
		Edge wall;
		int sourceRoot;
		int destRoot;
		for(int i = 0; i < numWalls; i++) {
			// get the wall
			wall = edges[permutation[i]];
			
			// find roots of the sets of cells separated by wall
			sourceRoot = sets.find(wall.getSource());
			destRoot = sets.find(wall.getDest());
			
			// if the wall separates disjoint sets of cells, remove it
			if(sourceRoot != destRoot) {
				// remove the wall from the array of edges
				edges[permutation[i]] = null;
				
				// these sets are no longer disjoint
				sets.union(sourceRoot, destRoot);			
			}
//			// with some probability remove it anyway
//			else if (Math.random() < 0.3f)
//				edges[permutation[i]] = null;
		}
		
		// TODO: Finish
		
		// convert edges into XML
		for(int i = 0; i < numWalls; i++) {
			if(edges[i] == null)
				continue;		
			
			if(i < numVertWalls) {
				rotation = 1.57079633f;				
				x = (edges[i].getDest() % cols) * scale;
				y = (edges[i].getSource() / cols) * scale;
				z = (i / (numVertWalls / levels)) * (height * surfaceHeight);
				outputWallXML(x, y, z, length, width, height, rotation, out);
				// outputTracerData(x, y, x, (y + length), out2);
			}

			else if (i < (numVertWalls + numHorizWalls)){
				rotation = 0;
				x = (edges[i].getSource() % cols) * scale;
				y = (edges[i].getDest() / cols) * scale;
				z = ((i - numVertWalls) / (numHorizWalls / levels)) * (height * surfaceHeight);
				outputWallXML(x, y, z, length, width, height, rotation, out);
				// outputTracerData(x, y, (x + length), y, out2);
			}
			
			else {
				rotation = 0;
				x = (edges[i].getSource() % cols) * scale;
				y = ((edges[i].getSource() / cols) % rows) * scale;
				z = ((i - numVertWalls - numHorizWalls) / (numSurfaces / (levels - 1))) * (height + (surfaceHeight));
				outputSurfaceXML(x, y, z, length, width, height, rotation, out);
				// outputTracerData(x, y, (x + length), y, out2);
			}
		}
		
		z = 0;
		
		for(int level = 1; level <= levels; level++)
		{
			// add outer walls
			bottomLength = (entrance == level) ? mazeX - scale : mazeX; 
			topLength = (exit == level) ? mazeX - scale : mazeX;
			
			// bottom
			outputWallXML(1, 0, z, bottomLength, width, height + surfaceHeight, 0, out);
			
//			if(entrance == floor)
//				//outputTracerData(scale, 0, mazeX, 0, out2);
//			else
//				//outputTracerData(0, 0, mazeX, 0, out2);
			
			// top
			outputWallXML(0, rows, z, topLength, width, height + surfaceHeight, 0, out);
			
//			if (exit == level)
//				//outputTracerData(0, mazeY, mazeX - scale, mazeY, out2);
//			else
//				//outputTracerData(0, mazeY, mazeX, mazeY, out2);
			
			// left
			outputWallXML(0, 0, z, mazeY, width, height + surfaceHeight, 1.57079633f, out);
			// outputTracerData(0, 0, 0, mazeY, out2);
			
			// right
			outputWallXML(mazeX, 0, z, mazeY, width, height + surfaceHeight, 1.57079633f, out);
			//outputTracerData(mazeX, 0, mazeX, mazeY, out2);
			
			// prepare for next iteration
			z += height + surfaceHeight;
		}
		
		
		
		// write out final XML bits
		out.println("</structures></world>");
		out.close();
	}
	
	private static void outputWallXML(float xPos, float yPos, float zPos, 
							   float length, float width, float height, 
							   float rotation, PrintWriter out) {
		out.println("<structure>");
		out.println("<wall " + "length= \"" + length + "\" width=\"" + width + 
					"\" height=\"" + height + "\" rotation=\"" + rotation + "\">");
		out.println("<position " + "x= \"" + xPos + "\" y=\"" + yPos + "\" z=\"" + zPos + "\"></position>");
		out.println("</wall></structure>");
	}
	private static void outputSurfaceXML(float xPos, float yPos, float zPos, 
			   						  float length, float width, float height, 
			   						  float rotation, PrintWriter out) {
		out.println("<structure>");
		out.println("<surface " + "length= \"" + length + "\" width=\"" + width + 
					"\" height=\"" + height + "\" rotation=\"" + rotation + "\">");
		out.println("<position " + "x= \"" + xPos + "\" y=\"" + yPos + "\" z=\"" + zPos + "\"></position>");
		out.println("</surface></structure>");
	}
}
