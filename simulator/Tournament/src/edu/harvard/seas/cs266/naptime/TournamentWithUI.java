/**
 * @file TournamentWithUI.java
 * @author nward@fas.harvard.edu
 * @date 2012.03.17
 */

package edu.harvard.seas.cs266.naptime;

import sim.display.Console;
import sim.display.GUIState;
import sim.engine.SimState;

/**
 * Provides a view and controller for the Tournament model.
 * 
 * @author nward@fas.harvard.edu
 */
public class TournamentWithUI extends GUIState {
	/**
	 * Initialize a UI for a new Tournament simulation.
	 */
	public TournamentWithUI() {
		// Initialize the simulation's random seed using the clock
		super(new Tournament(System.currentTimeMillis()));
	}
	
	/**
	 * Initialize a UI for an existing Tournament simulation (presumably deserialized).
	 * 
	 * @param state The existing simulation.
	 */
	public TournamentWithUI(SimState state) {
		super(state);
	}

	/**
	 * Display name for the UI's window.
	 */
	public static String getName() {
		return "CS266 Robot Tournament";
	}
	
	/**
	 * Creates and displays simulation control console.
	 * 
	 * @param args MASON command-line args which we don't use.
	 */
	public static void main(String[] args) {
		TournamentWithUI ui = new TournamentWithUI();
		Console console = new Console(ui);
		console.setVisible(true);
	}
}
