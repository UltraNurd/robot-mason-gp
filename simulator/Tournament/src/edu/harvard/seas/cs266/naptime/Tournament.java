/**
 * @file Tournament.java
 * @author nward@fas.harvard.edu
 * @date 2012.03.17
 */

package edu.harvard.seas.cs266.naptime;

import sim.engine.SimState;

/**
 * Implements a simulation of the robot foraging field.
 * 
 * @author nward@fas.harvard.edu
 */
@SuppressWarnings("serial")
public class Tournament extends SimState {
	/**
	 * Initializes the foraging field.
	 * 
	 * @param seed Seed for the simulation's RNG.
	 */
	public Tournament(long seed) {
		super(seed);
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
