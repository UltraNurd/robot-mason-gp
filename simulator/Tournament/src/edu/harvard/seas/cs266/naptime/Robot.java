package edu.harvard.seas.cs266.naptime;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.portrayal.Oriented2D;
import sim.util.Double2D;

@SuppressWarnings("serial")
public class Robot implements Steppable, Oriented2D {
	/**
	 * The radius of the round robots.
	 */
	public static final double robotSize = 12.0;
	
	/**
	 * The current facing of the robot. Its front is where the camera and kicker are.
	 */
	private double orientation = 0.0;
	
	/**
	 * The robot's current "camera" view, a single line of 30 pixels, storing
	 * references to the "seen" object.
	 */
	private Object camera[] = new Object[30];
	
	/**
	 * The speed (in units per step) of the robot's left motor.
	 */
	private double leftSpeed = 0.0;
	
	/**
	 * The speed (in units per step) of the robot's right motor.
	 */
	private double rightSpeed = 0.0;

	/**
	 * Increments the simulated state of this robot one time step.
	 * 
	 * @param state The current Tournament simulation.
	 */
	@Override
	public void step(SimState state) {
		// Get the current simulation
		Tournament tourney = (Tournament) state;
		
		// Determine where we are and how to move
		sense(tourney.field);
		
		// Run the "motors" at their current speed settings
		move(tourney.field);
	}
	
	/**
	 * View the current Tournament field and determine motor speeds.
	 * 
	 * @param field The current Tournament field containing this robot.
	 */
	private void sense(Continuous2D field) {
		// Look for food in the camera's POV
		updateCamera(field);
		int foodMidpoint = findMidpointOfObjectiveInView();
		
		// Determine left/right offset to the objective
		double baseSpeed = 0.1;
		if (foodMidpoint < 15)
			// Turn right
			setSpeed(baseSpeed, -baseSpeed);
		else if (foodMidpoint > 15)
			// Turn left
			setSpeed(-baseSpeed, baseSpeed);
		else {
			// Straight ahead, so check if the objective is food-sized and immediately in front
			if (findWidthOfObjectiveInView() < 13)
				// Move forward at full speed
				setSpeed(2*baseSpeed, 2*baseSpeed);
			else
				// Don't move
				setSpeed(0, 0);
		}
	}
	
	/**
	 * Update the robot's internal camera buffer with the closest
	 * object currently in view.
	 */
	private void updateCamera(Continuous2D field) {
		// Calculate some camera constants
		final double imageWidth = Math.tan(Math.PI/6)*robotSize;
		
		// Get the robot's current location
		Double2D current = field.getObjectLocation(this);
		
		// Find the closest object of the specified type within the field of view
		double minDistance = Double.MAX_VALUE;
		Object minObjective = null;
		int minPixelLeft = 30;
		int minPixelRight = -1;
		for (Object objective: field.getAllObjects()) {
			if (objective.getClass() == Treat.class) {
				// Get the relative position vector for this objective
				Double2D position = field.getObjectLocation(objective).subtract(current).rotate(-orientation);
				
				// Make sure the objective is in view
				double objectiveAngle = Math.atan2(position.y, position.x);
				if (objectiveAngle < -Math.PI/6 || objectiveAngle > Math.PI/6)
					continue;
				
				// Project the object onto the camera's image plane
				double imagePlaneLeft = (position.y - Treat.treatSize/2)*(robotSize/2)/position.x;
				double imagePlaneRight = (position.y + Treat.treatSize/2)*(robotSize/2)/position.x;
				int pixelLeft = (int) Math.round(imagePlaneLeft*30/imageWidth) + 14;
				int pixelRight = (int) Math.round(imagePlaneRight*30/imageWidth) + 14;
				
				// Update the closest visible objective
				double distance = position.length();
				if (distance < minDistance) {
					minDistance = distance;
					minObjective = objective;
					minPixelLeft = pixelLeft;
					minPixelRight = pixelRight;
				}
			}
		}
		
		// Update the camera view
		for (int pixel = 0; pixel < 30; pixel++)
			if (pixel >= minPixelLeft && pixel <= minPixelRight) {
				camera[pixel] = minObjective;
			} else {
				camera[pixel] = null;
			}
	}
	
	/**
	 * Hacky method for finding the midpoint of the object in view.
	 */
	private int findMidpointOfObjectiveInView() {
		// Loop through the camera buffer, checking for object boundaries
		int pixelLeft = -1;
		int pixelRight = -1;
		for (int pixel = 0; pixel < 30; pixel++) {
			if (pixelLeft == -1 && camera[pixel] != null)
				pixelLeft = pixel;
			if (pixelLeft != -1 && pixelRight == -1 && camera[pixel] == null)
				pixelRight = pixel - 1;
		}
		
		// Check for edge-crossing and calculate the midpoint
		if (pixelLeft == -1)
			return 0;
		else if (pixelRight == -1)
			return 29;
		else
			return (pixelLeft + pixelRight)/2;
	}

	/**
	 * Hacky method for finding the width of the object in view.
	 */
	private int findWidthOfObjectiveInView() {
		// Loop through the camera buffer, checking for object boundaries
		int pixelLeft = -1;
		int pixelRight = -1;
		for (int pixel = 0; pixel < 30; pixel++) {
			if (pixelLeft == -1 && camera[pixel] != null)
				pixelLeft = pixel;
			if (pixelLeft != -1 && pixelRight == -1 && camera[pixel] == null)
				pixelRight = pixel - 1;
		}
		
		// Check for edge-crossing and calculate the width
		if (pixelLeft == -1)
			return 0;
		else if (pixelRight == -1)
			return 30 - pixelLeft;
		else
			return pixelRight - pixelLeft;
	}

	/**
	 * Rudimentary accessor to expose the camera buffer to the inspector.
	 * 
	 * @return A string where '*' represents the object in view.
	 */
	public String getCamera() {
		// Loop through the camera buffer, appending
		String image = "";
		for (Object pixel: camera)
			if (pixel != null)
				image += "*";
			else
				image += "-";
		return image;
	}
	
	/**
	 * Uses the current speed to adjust the robot's position and orientation.
	 */
	private void move(Continuous2D field) {
		// Update the orientation based on relative wheel velocity
		orientation += (rightSpeed - leftSpeed)/robotSize;
		
		// Keep orientation in [-pi,pi] range even though Oriented2D mods correctly
		if (orientation > Math.PI)
			orientation -= 2*Math.PI;
		else if (orientation < -Math.PI)
			orientation += 2*Math.PI;
		
		// Update the position based on midpoint speed and new orientation
		Double2D currentPosition = field.getObjectLocation(this);
		double midpointSpeed = (rightSpeed + leftSpeed)/2;
		Double2D newPosition = new Double2D(currentPosition.x + midpointSpeed*Math.cos(orientation),
											currentPosition.y + midpointSpeed*Math.sin(orientation));
		field.setObjectLocation(this, newPosition);
	}
	
	/**
	 * Mutator to update the current motor speeds of the robot.
	 * 
	 * @param left The desired speed of the left motor.
	 * @param right The desired speed of the right motor.
	 */
	protected void setSpeed(double left, double right) {
		leftSpeed = left;
		rightSpeed = right;
	}

	/**
	 * Accessor for the robot's current facing. Implements the Oriented2D interface.
	 */
	@Override
	public double orientation2D() {
		return orientation;
	}
}
