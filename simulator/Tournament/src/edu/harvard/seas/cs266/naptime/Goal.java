/**
 * @file Goal.java
 * @author nward@fas.harvard.edu
 * @date 2012.03.28
 */

package edu.harvard.seas.cs266.naptime;

import sim.engine.SimState;
import sim.util.Double2D;

/**
 * Represents a team's goal at one end of the field.
 * 
 * @author nward@fas.harvard.edu
 */
public class Goal {
	/**
	 * The spacing of the goalposts.
	 */
	public static final double goalSize = Tournament.fieldWidth*0.5;

	/**
	 * The parent team of this goal.
	 */
	private Team parent = null;
	
	/**
	 * Sets up the goal.
	 * 
	 * @param team The team targeting this goal.
	 */
	public Goal(Team team) {
		this.parent = team;
	}
	
	public void step(SimState state) {
		// Get the current simulation
		Tournament tourney = (Tournament) state;
		
		// Determine the bounding box of this goal's scoring area
		Double2D goalPosition = tourney.field.getObjectLocation(this);
		double maxX;
		if (goalPosition.x == 0.0)
			maxX = 4.0;
		else
			maxX = tourney.field.getWidth();
		double minX = maxX - 4.0;
		double minY = goalPosition.y - goalSize/2 - 4.0;
		double maxY = goalPosition.y + goalSize/2 + 4.0;
		
		// Check if any treats are within bounds
		Boolean done = true;
		for (Object treat: tourney.field.getAllObjects()) {
			if (treat.getClass() == Treat.class) {
				done = false;
				Double2D treatPosition = tourney.field.getObjectLocation(treat);
				if (treatPosition.x >= minX && treatPosition.x <= maxX && treatPosition.y >= minY && treatPosition.y <= maxY) {
					// Clear it from the field
					tourney.field.remove(treat);
					
					// Update the score
					tourney.score[parent.opposing ? 1 : 0]++;
				}
			}
		}
		if (done)
			// If there's no food left, end the simulation immediately
			state.kill();
	}
}
