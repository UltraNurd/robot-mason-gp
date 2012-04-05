/**
 * @file Tournament.java
 * @author nward@fas.harvard.edu
 * @date 2012.03.17
 */

package edu.harvard.seas.cs266.naptime;

import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;

/**
 * Implements a simulation of the robot foraging field.
 * 
 * @author nward@fas.harvard.edu
 */
@SuppressWarnings("serial")
public class Tournament extends SimState {
	/**
	 * The length of the field from goal to goal.
	 */
	public static final int fieldLength = 220;
	
	/**
	 * The width of the field (between the non-goal sides).
	 */
	public static final int fieldWidth = 150;

	/**
	 * Representation of 2-D space where robots will forage for food.
	 */
	public Continuous2D field = new Continuous2D(1.0, fieldLength, fieldWidth);
	
	/**
	 * Initial count of food particles in the field (may get parameterized).
	 */
	public int nTreats = 20;
	
	/**
	 * Creates the simulation.
	 * 
	 * @param seed Seed for the simulation's RNG.
	 */
	public Tournament(long seed) {
		super(seed);
	}
	
	/**
	 * Implements simulation initialization
	 */
	public void start() {
		// Start the simulation
		super.start();
		
		// Clear the field of food and robots
		field.clear();
		
		// Add our team of robots to the field and activate them
		Team team = new Team(field, false);
		schedule.scheduleRepeating(team);
		
		// Add the opposing team of robots to the field and activate them
		Team opposingTeam = new Team(field, true);
		schedule.scheduleRepeating(opposingTeam);
		
		// Add some randomly distributed food to the field
		for (int t = 0; t < nTreats; t++) {
			// Select a random but empty location
			Double2D treatLocation;
			do {
				treatLocation = new Double2D(field.getWidth()*(random.nextDouble()*0.8 + 0.1),
						 					 field.getHeight()*(random.nextDouble()*0.8 + 0.1));
			} while (field.getObjectsWithinDistance(treatLocation, Treat.treatSize).size() > 0);
			Treat treat = new Treat();
			field.setObjectLocation(treat, treatLocation);
		}
	}

	/**
	 * Runs simulation by invoking SimState.doLoop.
	 * 
	 * @param args MASON command-line args which we don't use.
	 */
	public static void main(String[] args) {
		// Run the simulation using parent class convenience method.
		doLoop(Tournament.class, args);
		System.exit(0);
	}

}
