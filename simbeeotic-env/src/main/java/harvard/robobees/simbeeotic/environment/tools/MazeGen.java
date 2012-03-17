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
package harvard.robobees.simbeeotic.environment.tools;

import harvard.robobees.simbeeotic.environment.tools.graph.Edge;
import harvard.robobees.simbeeotic.environment.tools.graph.DisjointSets;

import java.io.FileNotFoundException;
import java.io.PrintWriter;


/**
 * Generates an XML world file containing a environment composed of stacked two-dimensional mazes.
 * User may modify number of rows, columns, floors, the scale of the maze, and the probability of 
 * removing additional walls to create sparser environments.
 * 
 * @author chartier
 */

public class MazeGen {
	
	public static void main(String[] args) throws FileNotFoundException
	{
		// MAGIC! This stuff should be taken from command-line input later.
		// works up to 150 x 150 confirmed (with 0.3 for random value),
		// higher numbers seem to cause problems (overflow issues)
		int cols = 10;
		int rows = 10;
		int floors = 1;
		
		// include entrance and exit openings in outer walls?
//		int entrance = 1;
//		int exit = floors;
		
		int entrance = -1;
		int exit = -1;
		
		float scale = 1;
		
		// some hard-coded values
		float length = 1 * scale;
		float width = 0.1f * scale;
		float height = 1 * scale;
		float surfaceHeight = 0.1f * scale;
		
		// dimensions of the maze
		float mazeX = cols * scale;
		float mazeY = rows * scale;
		
		float bottomLength, topLength;
		float trapX, trapY, trapWidth, trapLength;
		float rotation;
		float x;
		float y;
		float z = 0f;
		int source;
		
		float sparseness = 0;
		
		// number of cells and interior walls
		int numCells = rows*cols;
		int numWalls = 2*numCells - rows - cols;


		// create an array of edges representing walls
		Edge[] edges = new Edge[numWalls];

		// number of vertical walls (between cells adjacent parallel to x axis)
		// and horizontal walls (adjacent parallel to y axis)
		int numVertWalls = rows * (cols - 1);
		int numHorizWalls = (rows - 1) * cols;
		
		// set up output
		PrintWriter out = new PrintWriter("/home/chartier/workspace/simbeeotic/simbeeotic-examples/src/main/resources/scenarios/mazegenoutput.xml");
		PrintWriter out2 = new PrintWriter("/home/chartier/workspace/simbeeotic/simbeeotic-examples/src/main/resources/scenarios/mazegentracerdata.dat");
		
		// write out initial XML tags
		out.println("<?xml version=\"1.0\"?><world xmlns:xsi=\"http://www.w3.org" + 
					"/2001/XMLSchema-instance\" xmlns=\"http://harvard/robobees" + 
					"/simbeeotic/configuration/world\" xsi:schemaLocation=" + 
					"\"http://harvard/robobees/simbeeotic/configuration/world http://some/path/to/schema.xsd\">");
		out.println("<radius>2000</radius>");
		out.println("<structures>");
		
		// for each 'floor' of the 'building'
		for(int floor = 1; floor <= floors; floor++)
		{
			// create disjoint sets representing cells
			// cells unite sets if wall between them is removed
			DisjointSets sets = new DisjointSets(numCells);
			
			// begin with the first cell (closest to origin)
			source = 0;
			
			// create the 'vertical' walls (between columns)
			for(int i = 0; i < numVertWalls; i++) {
				if((source + 1) % cols == 0)
					source++;
				edges[i] = new Edge(source, source + 1);
				source++;
			}
			
			// create the 'horizontal walls' (between rows)
			for(int j = 0; j < numHorizWalls; j++) {
				edges[numVertWalls + j] = new Edge(j, j + cols);
			}
			
			// generate a random permutation of integers [0, numCells) to
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
				// with some probability remove it anyway
				else if (Math.random() < sparseness)
					edges[permutation[i]] = null;
			}
			
			// convert edges into XML
			for(int i = 0; i < numWalls; i++) {
				if(edges[i] == null)
					continue;		
				
				if(i < numVertWalls) {
					rotation = 1.57079633f;				
					x = (edges[i].getDest() % cols) * scale;
					y = (edges[i].getSource() / cols) * scale;
					outputTracerData(x, y, x, (y + length), out2);
				}
	
				else {
					rotation = 0;
					x = (edges[i].getSource() % cols) * scale;
					y = (edges[i].getDest() / cols) * scale;
					outputTracerData(x, y, (x + length), y, out2);
				}
				if(i < numVertWalls && (i + cols - 1) < numVertWalls && edges[i + cols - 1] == null)
					outputWallXML(x, y, z, length + width, width, height, rotation, out);
				else
					outputWallXML(x, y, z, length, width, height, rotation, out);
	
			}
			
			// add outer walls
			bottomLength = (entrance == floor) ? mazeX - scale : mazeX; 
			topLength = (exit == floor) ? mazeX - scale : mazeX;
			
			// bottom
			if (entrance == floor)
				outputWallXML(scale, 0, z, bottomLength, width, height, 0, out);
			else
				outputWallXML(0, 0, z, bottomLength, width, height, 0, out);
			
			if(entrance == floor)
				outputTracerData(scale, 0, mazeX, 0, out2);
			else
				outputTracerData(0, 0, mazeX, 0, out2);
			
			// top
			outputWallXML(0, rows*scale, z, topLength, width, height, 0, out);
			
			if (exit == floor)
				outputTracerData(0, mazeY, mazeX - scale, mazeY, out2);
			else
				outputTracerData(0, mazeY, mazeX, mazeY, out2);
			
			// left
			outputWallXML(0, 0, z, mazeY, width, height, 1.57079633f, out);
			outputTracerData(0, 0, 0, mazeY, out2);
			
			// right
			outputWallXML(mazeX, 0, z, mazeY, width, height, 1.57079633f, out);
			outputTracerData(mazeX, 0, mazeX, mazeY, out2);
			
			// surface
			trapX = ((int) (Math.random() * cols)) * scale;
			trapY = ((int) (Math.random() * rows)) * scale;
			trapLength = scale;
			trapWidth = scale;
			
			outputSurfaceXML(0, 0, z + height, 
							 mazeX, mazeY, surfaceHeight, 0, 
							 trapX, trapY, trapLength, trapWidth, out);
			
			
			// prepare for next iteration
			z += height + surfaceHeight;
		}
		
		// write out final XML bits
		out.println("</structures></world>");
		out.close();
		out2.close();
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
			   						  float rotation, 
			   						  float trapX, float trapY,
			   						  float trapLength, float trapWidth,
			   						  PrintWriter out) {
		out.println("<structure>");
		out.println("<surface " + "length= \"" + length + "\" width=\"" + width + 
					"\" height=\"" + height + "\" rotation=\"" + rotation +
					"\" trap=\"" + "true" + 
					"\" trapX=\"" + trapX + "\" trapY=\"" + trapY +  
					"\" trapLength=\"" + trapLength + 
					"\" trapWidth=\"" + trapWidth + "\">");
		out.println("<position " + "x= \"" + xPos + "\" y=\"" + yPos + "\" z=\"" + zPos + "\"></position>");
		out.println("</surface></structure>");
}
	
	
	private static void outputTracerData(float xPos1, float yPos1, 
										 float xPos2, float yPos2, 
										 PrintWriter out) {
		out.println(xPos1 + "\n" + yPos1);
		out.println(xPos2 + "\n" + yPos2);
	}
}
