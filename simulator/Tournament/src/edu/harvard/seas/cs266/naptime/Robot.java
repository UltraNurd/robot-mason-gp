package edu.harvard.seas.cs266.naptime;

import java.util.ArrayList;
import java.util.List;

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
		// Look for food in the camera's POV
		List<Double> objective;
		try {
			objective = lookForObjective(field, Treat.class);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		// Determine the angle to the objective
		double baseSpeed = 0.1;
		if (objective.get(0) < -baseSpeed/robotSize || objective.get(1) == Double.MAX_VALUE)
			// Turn right
			setSpeed(baseSpeed, -baseSpeed);
		else if (objective.get(0) > baseSpeed/robotSize)
			// Turn left
			setSpeed(-baseSpeed, baseSpeed);
		else if (objective.get(1) > 2*baseSpeed + robotSize/2)
			// Move forward at full speed
			setSpeed(2*baseSpeed, 2*baseSpeed);
		else
			// Move to the objective (last step)
			setSpeed(objective.get(1) - robotSize/2, objective.get(1) - robotSize/2);
	}
	
	private List<Double> lookForObjective(Continuous2D field, Class<Treat> targetClass) throws InstantiationException, IllegalAccessException {
		// Get the robot's current location
		Double2D current = field.getObjectLocation(this);
		
		// Generate the orientation vector
		Double2D direction = new Double2D(Math.cos(orientation), Math.sin(orientation));
		
		// Find the closest object of the specified type within the field of view
		double minDistance = Double.MAX_VALUE;
		double angleToClosestObjective = 0.0;
		for (Object objective: field.getAllObjects()) {
			if (objective.getClass() == targetClass) {
				// Get the normalized relative position vector for this object
				Double2D position = field.getObjectLocation(objective).subtract(current).normalize();
				
				// Find the angle to the object
				double dotLength = position.dot(direction);
				double objectiveAngle = Math.acos(dotLength);
				if (position.subtract(direction.resize(dotLength)).y < 0) {
					objectiveAngle *= -1;
				}
				
				// Check that the angle is within the POV
				if (Math.abs(objectiveAngle) < Math.PI/6) {
					// Update the closest objective
					double distance = field.getObjectLocation(objective).distance(current);
					if (distance < minDistance) {
						minDistance = distance;
						angleToClosestObjective = objectiveAngle;
					}
				}
			}
		}
		
		// Done
		List<Double> toObjective = new ArrayList<Double>();
		toObjective.add(angleToClosestObjective);
		toObjective.add(minDistance);
		return toObjective;
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
