package edu.harvard.seas.cs266.naptime;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.portrayal.Oriented2D;
import sim.util.Bag;
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
		// Get the robot's current location
		Double2D current = field.getObjectLocation(this);
		
		// Find the nearest food
		Bag neighbors = field.getNearestNeighbors(current, 3, false, false, true, null);
		double minDistance = 2.0*Math.sqrt(Math.pow(field.getWidth(), 2.0) + Math.pow(field.getHeight(), 2.0));
		Double2D objective = null;
		for (Object neighbor : neighbors) {
			if (neighbor.getClass() == Treat.class) {
				Double2D neighborLocation = field.getObjectLocation(neighbor);
				double distance = current.distance(neighborLocation);
				if (distance < minDistance) {
					minDistance = distance;
					objective = neighborLocation;
				}
			}
		}
		if (objective == null) {
			setSpeed(0.0, 0.0);
			return;
		}
		
		// Determine the distance to the objective
		double distance = current.distance(objective);
		
		// Check if we're within epsilon of the objective
		if (distance < 0.001) {
			setSpeed(0.0, 0.0);
			return;
		}
		
		// Determine the angle to the objective
		double baseSpeed = 0.1;
		double objectiveAngle = objective.subtract(current).angle() - orientation;
		if (objectiveAngle < -baseSpeed/robotSize)
			// Turn right
			setSpeed(baseSpeed, -baseSpeed);
		else if (objectiveAngle > baseSpeed/robotSize)
			// Turn left
			setSpeed(-baseSpeed, baseSpeed);
		else if (distance > 2*baseSpeed)
			// Move forward at full speed
			setSpeed(2*baseSpeed, 2*baseSpeed);
		else
			// Move to the objective (last step)
			setSpeed(distance, distance);
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
