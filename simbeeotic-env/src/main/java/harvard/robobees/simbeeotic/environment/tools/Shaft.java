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
public class Shaft {
	
	private static Element structures = null;
	
	public static void gen(Document out, Vector3f[] tunnelPositions, 
						   float shaftHeight, float diameter, int numberOfSides, float tunnelProb, float tunnelWidthGiven, 
						   Element structuresElement) {
		float sideWidth = diameter * (float) Math.sin(((float) Math.PI)/numberOfSides);
		float tunnelWidth = tunnelWidthGiven;
		float tunnelHeight = tunnelWidth;

		float baseWidth = numberOfSides * sideWidth / (float) Math.PI;
		
		Vector3f sideDimensions = new Vector3f(sideWidth, 0.1f, shaftHeight);
		
		structures = structuresElement;
		
		// construct 'base' of shaft
		/* This can just be a flat surface at the bottom which covers at least the bottom of the shaft.
		   Upper bound on diameter is numberOfSides * sideWidth / PI. */
		outputSurfaceXML(new Vector3f(0, 0, 0), new Vector3f(baseWidth, baseWidth, 0.1f), 0, out);
		
		// theta of tunnel chord
		float theta = 2 * (float) Math.asin(tunnelWidth / diameter);
		System.out.println(theta);
		
		int numRegSides = (int) (numberOfSides * ((2 * (float) Math.PI - theta)/(2 * (float) Math.PI)));
		
		int numMissingSides = numberOfSides - numRegSides;
		
		// establish starting position
		Vector3f position = new Vector3f(diameter, diameter/2 + sideWidth/2, 0);
		
		// initial 'rotation' of walls, first one is along y axis
		float rotation = (float) Math.PI / 2 + (2 * (float) Math.PI / numberOfSides);
		
		Vector3f translation = null;
		
		// construct flat, regular sides
		for(int i = 0; i < numberOfSides - numMissingSides/2; i++) {
			// construct side
			if(i >= numMissingSides/2 && i < numberOfSides - numMissingSides/2)
				outputWallXML(position, sideDimensions, rotation, out);
			
			// adjust 'position' correct distance along x and y
			translation = new Vector3f((float) Math.cos(rotation) * sideWidth, (float) Math.sin(rotation) * sideWidth, 0);
			position.add(translation);
			
			// adjust 'rotation' by 2*PI / numberOfSides
			rotation = (rotation + 2 * (float) Math.PI / numberOfSides) % (2 * (float) Math.PI);
		}
		
		Vector3f arcPosition = null;
		float arcRotation;
		
		// level at which to construct wall
		for(float z = 0; z < shaftHeight; z += tunnelHeight) {
			if(Math.random() > tunnelProb) {
				// establish starting position
				arcPosition = new Vector3f(position.x, position.y, position.z);
				
				// initial 'rotation' of walls, first one is along y axis
				arcRotation = rotation;
				
				
				for(int i = 0; i < numMissingSides; i++) {
					outputWallXML(new Vector3f(arcPosition.x, arcPosition.y, z), 
								  new Vector3f(sideDimensions.x, sideDimensions.y, tunnelHeight), arcRotation, out);
					
					// adjust 'position' correct distance along x and y
					translation = new Vector3f((float) Math.cos(arcRotation) * sideWidth, (float) Math.sin(arcRotation) * sideWidth, 0);
					arcPosition.add(translation);
					
					// adjust 'rotation' by 2*PI / numberOfSides
					arcRotation = (arcRotation + 2 * (float) Math.PI / numberOfSides) % (2 * (float) Math.PI);
				}
			}
			else
				tunnelPositions[(int) (z/tunnelHeight)] = new Vector3f(position.x - sideDimensions.x, position.y, z);
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("The Game.");
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
}
