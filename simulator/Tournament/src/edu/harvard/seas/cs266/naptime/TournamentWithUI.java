/**
 * @file TournamentWithUI.java
 * @author nward@fas.harvard.edu
 * @date 2012.03.17
 */

package edu.harvard.seas.cs266.naptime;

import java.awt.Color;
import java.io.File;

import javax.swing.JFrame;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.simple.OrientedPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.ShapePortrayal2D;

/**
 * Provides a view and controller for the Tournament model.
 * 
 * @author nward@fas.harvard.edu
 */
public class TournamentWithUI extends GUIState {
	/**
	 * The window that will hold the view.
	 */
	public JFrame displayFrame;
	
	/**
	 * The view that will hold all of the visualizations.
	 */
	public Display2D display;
	
	/**
	 * Visual representation of the 2-D Tournament.field.
	 */
	ContinuousPortrayal2D fieldPortrayal = new ContinuousPortrayal2D();
	
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
	 * Make sure that the visualization is ready when starting the simulation.
	 */
	public void start() {
		super.start();
		setupPortrayals();
	}
	
	/**
	 * Make sure that the visualization is ready when loading a simulation.
	 */
	public void load(SimState state) {
		super.load(state);
		setupPortrayals();
	}
	
	/**
	 * Define how food and robots will be drawn. Convenience method called
	 * by both load and start.
	 */
	public void setupPortrayals() {
		// Get the current simulation
		Tournament tourney = (Tournament) state;
		
		// Set up the foraging field
		fieldPortrayal.setField(tourney.field);
		
		// Define how to draw goals
		fieldPortrayal.setPortrayalForClass(Goal.class, new ShapePortrayal2D(new double[] {-1.0, 1.0, 1.0, -1.0}, new double[] {0.5*Goal.goalSize, 0.5*Goal.goalSize, -0.5*Goal.goalSize, -0.5*Goal.goalSize}, Color.blue, true));
		
		// Define how to draw robots
		fieldPortrayal.setPortrayalForClass(Robot.class, new OrientedPortrayal2D(new OvalPortrayal2D(Color.black, Robot.robotSize, false), 12, 6.0, Color.black));
		
		// Define how to draw food
		fieldPortrayal.setPortrayalForClass(Treat.class, new OvalPortrayal2D(new Color(255, 128, 0), Treat.treatSize));
		
		// Clear the view
		display.reset();
		display.setBackdrop(new Color(0, 192, 0));
		display.repaint();
	}
	
	/**
	 * Initialize the view inside the specified controller.
	 * 
	 * @param c The controller, probably a Console provided by MASON,
	 * that will run the visualized simulation.
	 */
	public void init(Controller c) {
		// Create a view that will display the current field
		super.init(c);
		display = new Display2D(Tournament.fieldLength*4, Tournament.fieldWidth*4, this);
		
		// Attach the view to the controller's window
		displayFrame = display.createFrame();
		displayFrame.setTitle("Tournament Battlefield");
		c.registerFrame(displayFrame);
		displayFrame.setVisible(true);
		
		// Load the portrayal of the field and its contents into the view
		display.attach(fieldPortrayal, "Battlefield");
	}
	
	/**
	 * Handle UI cleanup.
	 */
	public void quit() {
		super.quit();
		if (displayFrame != null)
			displayFrame.dispose();
	}
	
	/**
	 * Creates and displays simulation control console.
	 * 
	 * @param args MASON command-line args which we don't use.
	 */
	public static void main(String[] args) {
		// Check command-line parameters
		if (args.length != 2) {
			System.out.println("Usage: tournamentwithui <strategy> <opposing strategy>");
			System.exit(0);
		}
		
		try {
			// Initialize the simulation state
			Grammar.Step strategy = (Grammar.Step)Grammar.ExpressionFactory.build(new Sexp(new File(args[0])));
			Grammar.Step opposingStrategy = (Grammar.Step)Grammar.ExpressionFactory.build(new Sexp(new File(args[1])));
			Tournament tourney = new Tournament(Population.seed, strategy, opposingStrategy);
		
			TournamentWithUI ui = new TournamentWithUI(tourney);
			Console console = new Console(ui);
			console.setVisible(true);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
