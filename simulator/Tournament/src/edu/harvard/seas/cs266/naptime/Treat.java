/**
 * @file Treat.java
 * @author nward@fas.harvard.edu
 * @date 2012.03.17
 */

package edu.harvard.seas.cs266.naptime;

/**
 * Represents a single item of food on the tournament field.
 * 
 * @author nward@fas.harvard.edu
 */
public class Treat {
	/**
	 * The radius of the food-representing balls.
	 */
	public static final double treatSize = 4.0;
	
	/**
	 * Whether or not this treat is currently in the possession of a robot.
	 */
	public Boolean carried = false;
}
