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

import java.io.FileNotFoundException;

import org.w3c.dom.*;

import javax.vecmath.Vector3f;


/**
 * @author chartier
 */
public class Tunnel {
	
	// true if there is open space here, false otherwise
	private static boolean[][] map;
	// true if this portion of the tunnel is 'skewed', false otherwise
	private static boolean[][] skewMap;
	
	// upper bound on the length/width of the tunnel system in skewed 'coordinate system'
	private static int length;
	private static int width;
	
	// angle between 'axes' in skewed 'coordinate system' - between 0 and PI/2 makes sense.
	private static float skew = (float) Math.PI/4;
	
	// constants which affect the behavior of the tunnel generation algorithm
	// BRANCH_PROB + CONT_PROB < 1 allows tunnel branches to end with some probability
	private static float BRANCH_PROB;
	private static float BRANCH_PROB_RIGHT;
	private static float BRANCH_PROB_LEFT;
	private static float CONT_PROB;
	private static float MAIN_CONT_PROB;
	
	// constants used to scale the coordinate system
	private static float tunnel_width;
	private static float aperture_width;
	
	// defines dimensions of walls spawned into physical world
	private static Vector3f wallDimensions;
	private static Vector3f surfaceDimensions;
	
	// positioning output
	private static Vector3f worldPosition;
	private static Vector3f initialPosition;
	
	private static boolean makeSurfaces = false;
	private static boolean makePeople = true;
	
	private static Element structures = null;
	private static Element people = null;
	
	public static void gen(Vector3f givenPosition, Document out, float tunnelBranchSkew, float tunnelWidth, 
						   int maxTunnelRegionWidth, int maxTunnelRegionLength, float tunnelBranchProb, Element structuresElement,
						   Element peopleElement) {
		
		// set some parameters using MAGIC
		length = maxTunnelRegionLength;
		width = maxTunnelRegionWidth;
		
		skew = tunnelBranchSkew;
		
		tunnel_width = tunnelWidth;
		aperture_width = tunnel_width / (float) Math.sin(skew);
		
		wallDimensions = new Vector3f(aperture_width, 0.1f, tunnel_width);
		surfaceDimensions = new Vector3f(aperture_width, tunnel_width, 0.1f);
		
		BRANCH_PROB = tunnelBranchProb;
		BRANCH_PROB_RIGHT = BRANCH_PROB/2;
		BRANCH_PROB_LEFT = BRANCH_PROB/2;
		CONT_PROB = (1 - BRANCH_PROB - 0.05f);
		MAIN_CONT_PROB = ((1 - BRANCH_PROB) + CONT_PROB)/2;
		
		// I want this global
		worldPosition = givenPosition;
		
		map = new boolean[length][width];
		skewMap = new boolean[length][width];
		initialPosition = new Vector3f(0, width/2, 0);
		
		// set up position vector for use in construction of tunnels
		Vector3f position = new Vector3f(initialPosition.x,
										 initialPosition.y,
										 initialPosition.z);
		
		// direction of tunnel, might be able to deprecate this
		Vector3f direction = new Vector3f(1,0,0);
		
		// vectors used for plotting branching tunnels
		Vector3f branchPosition = new Vector3f();
		Vector3f branchDirection;
		
		// special case of branching for main tunnel
		boolean doneBuilding = false;
		double rand;
		
		structures = structuresElement;
		people = peopleElement;
		
		// set starting position to be part of the tunnel
		map[(int) position.x][(int) position.y] = true; 
		
		// STRUCTURE PLOTTING SECTION
		// plotting out the tunnel system
		while(!doneBuilding) {
			rand = Math.random();
			
			if(rand < BRANCH_PROB_LEFT) {
				// left rotation by pi/2 radians
				branchDirection = new Vector3f(-direction.y, direction.x, direction.z);
				
				// set the appropriate cell to true and move the position
				branchPosition.add(position, branchDirection);
				
				// check bounds
				if(branchPosition.x >= length || branchPosition.y >= width ||
				   branchPosition.x <= 0 || branchPosition.y <= 0)
					continue;
				
				map[(int) branchPosition.x][(int) branchPosition.y] = true;
				skewMap[(int) branchPosition.x][(int) branchPosition.y] = true;
				
				branch(branchPosition, branchDirection, 1, false, true);	
			}
			else if(rand < BRANCH_PROB_LEFT + BRANCH_PROB_RIGHT) {
				// right rotation by pi/2 radians
				branchDirection = new Vector3f(direction.y, -direction.x, direction.z);

				// set the appropriate cell to true and move the position
				branchPosition.add(position, branchDirection);
				
				// check bounds
				if(branchPosition.x >= length || branchPosition.y >= width ||
				   branchPosition.x <= 0 || branchPosition.y <= 0)
					continue;
				
				map[(int) branchPosition.x][(int) branchPosition.y] = true;
				skewMap[(int) branchPosition.x][(int) branchPosition.y] = true;
				
				branch(branchPosition, branchDirection, 1, true, true);	
			}
			else if(rand < BRANCH_PROB + MAIN_CONT_PROB) {
				// continue
				// set appropriate cell to true and move the position
				position.add(direction);
				
				// check bounds (if branch hits edge, abort)
				if(position.x >= length || position.y >= width ||
				   position.x <= 0 || position.y <= 0) {
					doneBuilding = true;
					continue;
				}		
				
				map[(int) position.x][(int) position.y] = true;
			}
			else {
				// halt constructing this branch
				doneBuilding = true;
			}
		}
		
		// XML OUTPUT SECTION

		boolean isStartingPosition;
		Vector3f coordinates = new Vector3f();
		Vector3f outCoordinates = new Vector3f();
		
		float outRotLR, outRotAB;

		// unique ID for each person - TODO: HAX, MAKE THIS BETTER
		int personID = (int) worldPosition.z * 10;
		
		for(int x = 0; x < length; x++) {
			for(int y = 0; y < width; y++) {
				
				// place surfaces - non-skewed
				if((map[x][y] || ((x != 0) && map[x - 1][y]) || ((y != 0) && map[x][y - 1])
							  || ((y + 1) != width && (x != 0) && map[x][y + 1]))) {
					coordinates.x = x;
					coordinates.y = y;
					
					if(y < initialPosition.y)
						coordinates.x = x - 1;
				
					// translate to world coordinate system
					translate(coordinates, outCoordinates);
					
					if(makeSurfaces) {
						outputSurfaceXML(outCoordinates, surfaceDimensions, 0, out);
						outputSurfaceXML(new Vector3f(outCoordinates.x, outCoordinates.y, outCoordinates.z + wallDimensions.z), surfaceDimensions, 0, out);
					}
					
					// this is really hackish - fix
					if(map[x][y] && (x == (length - 1)) && y != initialPosition.y) {
						coordinates.x = x;
						
						// translate to world coordinate system
						translate(coordinates, outCoordinates);
						
						if(makeSurfaces) {
							outputSurfaceXML(outCoordinates, surfaceDimensions, 0, out);
							outputSurfaceXML(new Vector3f(outCoordinates.x, outCoordinates.y, outCoordinates.z + wallDimensions.z), surfaceDimensions, 0, out);
						}
						coordinates.x = x + 1;
						
						// translate to world coordinate system
						translate(coordinates, outCoordinates);
						
						if(makeSurfaces) {
							outputSurfaceXML(outCoordinates, surfaceDimensions, 0, out);
							outputSurfaceXML(new Vector3f(outCoordinates.x, outCoordinates.y, outCoordinates.z + wallDimensions.z), surfaceDimensions, 0, out);
						}
					}
				}	
				
				// not part of tunnel system?
				if(!map[x][y])
					continue;
				
				coordinates.x = x;
				coordinates.y = y;
				
				outCoordinates.x = coordinates.x;
				outCoordinates.y = coordinates.y;
				
				// TODO: What if the starting position is on a corner? One wall must be erected!
				isStartingPosition = (x == initialPosition.x && y == initialPosition.y);
				
				// rot for 'right or left'
				if(y == initialPosition.y)
					outRotLR = (float) Math.PI/2;
				else if(y < initialPosition.y)
					outRotLR = (float) Math.PI - skew;
				else
					outRotLR = skew;
				
				// rot for 'above or below'
				outRotAB = 0;
				
				// construct walls around 'cells' in the tunnel system
				// we consider 'right' to be +x, 'up' to be +y
				
				// to the left
				if((x > 0 && !map[x - 1][y]) || (x == 0 && !isStartingPosition)) {
					coordinates.x = x;
					coordinates.y = y;
					
					// translate to world coordinate system
					translate(coordinates, outCoordinates);
					
					outputWallXML(outCoordinates, wallDimensions, outRotLR, out);
				}	
				
				// to the right
				if((((x + 1) < length && !map[x + 1][y]) || ((x + 1) == length && !isStartingPosition)) 
						&& y != initialPosition.y) {
					coordinates.x = x + 1;
					coordinates.y = y;
					
					// translate to world coordinate system
					translate(coordinates, outCoordinates);
					
					outputWallXML(outCoordinates, wallDimensions, outRotLR, out);
				}
				
				// to the right ending main tunnel
				else if(((x + 1) == length || ((x + 1) < length && !map[x + 1][y])) && y == initialPosition.y) {
					coordinates.x = x + 1;
					coordinates.y = y;
					
					// translate to world coordinate system
					translate(coordinates, outCoordinates);
					
					outputWallXML(outCoordinates, new Vector3f(tunnel_width,0.1f,tunnel_width), outRotLR, out);
				}
				
				// above
				if(((y + 1) < width && !map[x][y + 1]) || ((y + 1) == width && !isStartingPosition)) {
					coordinates.x = x;
					coordinates.y = y + 1;
					
					// translate to world coordinate system
					translate(coordinates, outCoordinates);
					
					outputWallXML(outCoordinates, wallDimensions, outRotAB, out);
				}
				
				// below
				if((y > 0 && !map[x][y - 1]) || (y == 0 && !isStartingPosition)) {
					coordinates.x = x;
					coordinates.y = y;
				
					// translate to world coordinate system
					translate(coordinates, outCoordinates);
					
					outputWallXML(new Vector3f(outCoordinates.x, outCoordinates.y - 0.1f, outCoordinates.z), wallDimensions, outRotAB, out);
				}
				
				// with some probability put a man anywhere
				if(makePeople) {
					if(map[x][y] && Math.random() < 0.1f) {
						coordinates.x = x;
						coordinates.y = y;
						
						// translate to world coordinate system
						translate(coordinates, outCoordinates);
						
						outCoordinates.x += aperture_width/2;
						outCoordinates.y += tunnel_width/2;
						outCoordinates.z += surfaceDimensions.z;
						
						outputManXML(outCoordinates, 0.5f, 0.5f, personID++, out);

					}
				}
			}
		}
	}
	
	
	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("Blarg");
	}
	
	/**
	 * Given a starting position and direction, constructs a branch of the
	 * tunnel system. Branches always head away from the beginning of
	 * the main tunnel. Direction vector is expected to be normalized to
	 * the appropriate scalar.
	 */
	private static void branch(Vector3f position, Vector3f direction, int depth, boolean right, boolean skew) {
		boolean doneBuilding = false;
		double rand;

		// these are for the branches which originate from *this* branch
		Vector3f branchDirection;
		Vector3f branchPosition = new Vector3f();
		
		float moddedBranchProb = BRANCH_PROB / (depth);
		
		while(!doneBuilding) {
			rand = Math.random();
			
			if(rand < moddedBranchProb) {
				// 'bud' the new branch
				// NOTE: this only works in the plane!
				if(right)
					// left rotation by pi/2 radians
					branchDirection = new Vector3f(-direction.y, direction.x, direction.z);										
				else
					// right rotation by pi/2 radians
					branchDirection = new Vector3f(direction.y, -direction.x, direction.z);
										
				// set the appropriate cell to true and move the position
				branchPosition.add(position, branchDirection);
				
				// check bounds
				if(branchPosition.x >= length || branchPosition.y >= width ||
				   branchPosition.x <= 0 || branchPosition.y <= 0)
					continue;
				
				map[(int) branchPosition.x][(int) branchPosition.y] = true;
				skewMap[(int) position.x][(int) position.y] = skew;
				
				branch(branchPosition, branchDirection, depth + 1, !right, !skew);		

			}

			else if(rand < moddedBranchProb + CONT_PROB) {
				// continue
				// set appropriate cell to true and move the position
				position.add(direction);
				
				// check bounds (if branch hits edge, abort)
				if(position.x >= length || position.y >= width ||
				   position.x <= 0 || position.y <= 0) {
					doneBuilding = true;
				}	
				else {
					map[(int) position.x][(int) position.y] = true;
					skewMap[(int) position.x][(int) position.y] = skew;
				}
			}
			else {
				// halt constructing this branch
				doneBuilding = true;
			}
		}
	}
	
	private static void outputWallXML(Vector3f pos, Vector3f dimensions, float rotation, Document out) {

		Element structure = out.createElement("structure");
		structures.appendChild(structure);
		
		Element wall = out.createElement("wall");
		wall.setAttribute("length", Float.toString(dimensions.x));
		wall.setAttribute("width", Float.toString(dimensions.y));
		wall.setAttribute("height", Float.toString(dimensions.z));
		wall.setAttribute("rotation", Float.toString(rotation));
		
		structure.appendChild(wall);
		
		Element position = out.createElement("position");
		
		position.setAttribute("x", Float.toString(pos.x));
		position.setAttribute("y", Float.toString(pos.y));
		position.setAttribute("z", Float.toString(pos.z));
		
		wall.appendChild(position);
	}
	
	private static void outputSurfaceXML(Vector3f pos, Vector3f dimensions, 
										 float rotation, Document out) {
		Element structure = out.createElement("structure");
		structures.appendChild(structure);
		
		Element surface = out.createElement("surface");
		surface.setAttribute("length", Float.toString(dimensions.x));
		surface.setAttribute("width", Float.toString(dimensions.y));
		surface.setAttribute("height", Float.toString(dimensions.z));
		surface.setAttribute("rotation", Float.toString(rotation));
		
		structure.appendChild(surface);
		
		Element position = out.createElement("position");
		
		position.setAttribute("x", Float.toString(pos.x));
		position.setAttribute("y", Float.toString(pos.y));
		position.setAttribute("z", Float.toString(pos.z));
		
		surface.appendChild(position);
	}
	
	private static void outputManXML(Vector3f pos, float height, float width, int personID, Document out) {
		Element person = out.createElement("person");
		people.appendChild(person);

		Element man = out.createElement("man");
		man.setAttribute("height", Float.toString(height));
		man.setAttribute("width", Float.toString(width));
		person.appendChild(man);

		Element position = out.createElement("position");
		position.setAttribute("x", Float.toString(pos.x));
		position.setAttribute("y", Float.toString(pos.y));
		position.setAttribute("z", Float.toString(pos.z));

		man.appendChild(position);
		
		Element meta = out.createElement("meta");
		man.appendChild(meta);
		
		Element prop = out.createElement("prop");
		prop.setAttribute("name", "person-id");
		System.out.println(personID);
		prop.setAttribute("value", Integer.toString(personID));
		meta.appendChild(prop);
	}
	
	private static void translate(Vector3f local, Vector3f output) {
		
		// initially match
		output.x = local.x;
		output.y = local.y;
		output.z = local.z;
		
		// computer distance from nearest 'axis'
		float axisDist = Math.min(Math.abs(local.y - initialPosition.y), 
		                          Math.abs(local.y - (initialPosition.y + 1)));
		
		// skew x
		output.x += (float) Math.cos(skew) * axisDist;
		
		// scale x
		output.x *= aperture_width;
		
		// scale y
		
		if(output.y >= initialPosition.y + 1)
			output.y = initialPosition.y + ((1 + axisDist) * tunnel_width);
		else
			output.y = initialPosition.y - (axisDist * tunnel_width);
			
		// translate to world coordinates
		output.x -= initialPosition.x;
		output.y -= initialPosition.y;
		output.z -= initialPosition.z;
		
		output.x += worldPosition.x;
		output.y += worldPosition.y;
		output.z += worldPosition.z;
	}
}
