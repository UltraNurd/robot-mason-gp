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
	 * The robot's current range sensor readings, arranged in 16 radial sectors.
	 */
	private double ranges[] = new double[16];
	
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
	 * The robot's current payload, if any.
	 */
	private Treat carrying = null;

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
		// Define speed and range constants
		final double baseSpeed = 0.1;
		final double minRange = Robot.robotSize/2;
		
		// Check if a carried object has dropped into the goal (based on proximity)
		if (carrying != null) {
			Double2D carriedPosition = field.getObjectLocation(carrying);
			if (carriedPosition.x < 4 && carriedPosition.y > (field.getHeight() - Goal.goalSize + 4)/2 &&
				carriedPosition.y < (field.getHeight() + Goal.goalSize - 4)/2) {
				// Drop it
				field.remove(carrying);
				carrying = null;				
			}
		}
		
		// Look for obstacles near the robot and in front of it
		Boolean obstacle = false;
		updateRanges(field);
		if (ranges[0] < minRange) {
			// Back up
			setSpeed(-2*baseSpeed, -2*baseSpeed);
			obstacle = true;
		}
		for (int r = 4; r > 0; r--)
			if (!obstacle && ranges[r] < minRange) {
				// Bank left proportionally
				setSpeed(-baseSpeed, (1 + 0.25*(16 - r))*baseSpeed);
				obstacle = true;
			}
		for (int r = 12; r < 16; r++)
			if (!obstacle && ranges[r] < minRange) {
				// Bank right proportionally
				setSpeed((1 + 0.25*r)*baseSpeed, -baseSpeed);
				obstacle = true;
			}
		if (obstacle)
			return;
		
		// Look for food/goal in the camera's POV
		updateCamera(field);
		int midpoint = findMidpointOfObjectiveInView();
		
		// Determine left/right offset to the objective
		if ((carrying == null && midpoint < 15) || (carrying != null && midpoint < 8))
			// Turn left
			setSpeed(-baseSpeed, baseSpeed);
		else if ((carrying == null && midpoint > 15) || (carrying != null && midpoint > 22))
			// Turn right
			setSpeed(baseSpeed, -baseSpeed);
		else {
			// Straight ahead, so check if the objective is correctly sized and immediately in front
			if (carrying != null || findWidthOfObjectiveInView() < 13)
				// Move forward at full speed
				setSpeed(2*baseSpeed, 2*baseSpeed);
			else if (!obstacle) {
				// Don't move
				setSpeed(0, 0);
				
				// Pick up or drop off food
				if (carrying == null) {
					for (Object treat: camera)
						if (treat != null) {
							carrying = (Treat) treat;
							carrying.carried = true;
							break;
						}
				}
			}
		}
	}
	
	/**
	 * Update the robot's internal range sensor readings with
	 * the closest obstacles on all sides.
	 */
	private void updateRanges(Continuous2D field) {
		// Get the robot's current location
		Double2D current = field.getObjectLocation(this);
		
		// Reset the sensors
		for (int r = 0; r < 16; r++)
			ranges[r] = Double.MAX_VALUE;
		
		// Find the closest object to each sensor
		for (Object obstacle: field.getAllObjects()) {
			if (obstacle.getClass() == Robot.class && obstacle != this) {
				// Get the relative position vector for this obstacle
				Double2D position = field.getObjectLocation(obstacle).subtract(current).rotate(-orientation);
				
				// Get the angle to the obstacle
				double obstacleAngle = Math.atan2(position.y, position.x);
			
				// Determine which sensor by which it would be seen
				int sensor = (((int) Math.round(obstacleAngle*8/Math.PI)) + 16) % 16;
				
				// Update the closest obstacle
				double distance = position.length() - Robot.robotSize;
				if (distance < ranges[sensor])
					ranges[sensor] = distance;
			}
		}
		
		// Update distance to closest wall for each sensor, if there isn't a closer obstacle
		for (int r = 0; r < 16; r++)
			if (ranges[r] == Double.MAX_VALUE) {
				double sensorAngle = (orientation + r*Math.PI/8) % (2*Math.PI);
				if (sensorAngle > Math.PI)
					sensorAngle -= 2*Math.PI;
				if (sensorAngle >= -Math.PI/4 && sensorAngle < Math.PI/4)
					ranges[r] = (field.getWidth() - current.x)/Math.cos(sensorAngle);
				else if (sensorAngle >= Math.PI/4 && sensorAngle < 3*Math.PI/4)
					ranges[r] = (field.getHeight() - current.y)/Math.cos(sensorAngle - Math.PI/2);
				else if (sensorAngle >= 3*Math.PI/4)
					ranges[r] = current.x/Math.cos(sensorAngle - Math.PI);
				else if (sensorAngle < -3*Math.PI/4)
					ranges[r] = current.x/Math.cos(sensorAngle + Math.PI);
				else if (sensorAngle >= -3*Math.PI/4 && sensorAngle < -Math.PI/4)
					ranges[r] = current.y/Math.cos(sensorAngle + Math.PI/2);
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
		double[] depthBuffer = new double[30];
		for (int pixel = 0; pixel < 30; pixel++) {
			depthBuffer[pixel] = Double.MAX_VALUE;
			camera[pixel] = null;
		}
		for (Object objective: field.getAllObjects()) {
			if ((carrying == null && objective.getClass() == Treat.class) ||
				(carrying != null && objective.getClass() == Goal.class) ||
				objective.getClass() == Robot.class) {
				// Make sure this treat isn't already being carried
				if (objective.getClass() == Treat.class && ((Treat) objective).carried)
					continue;
				
				// Get the relative position vector for this objective
				Double2D position = field.getObjectLocation(objective).subtract(current).rotate(-orientation);
				
				// Make sure the objective is in front
				double objectiveAngle = Math.atan2(position.y, position.x);
				if (objectiveAngle < -Math.PI/2 || objectiveAngle > Math.PI/2)
					continue;
				
				// Determine the span of the object in the image plane
				double imagePlaneLeft = 0, imagePlaneRight = 0;
				if (objective.getClass() == Treat.class) {
					// Treats are round, so edges are based on radius
					imagePlaneLeft = (position.y - Treat.treatSize/2)*(robotSize/2)/position.x;
					imagePlaneRight = (position.y + Treat.treatSize/2)*(robotSize/2)/position.x;				
				} else if (objective.getClass() == Robot.class) {
					// Robots are round, so edges are based on radius
					imagePlaneLeft = (position.y - robotSize/2)*(robotSize/2)/position.x;
					imagePlaneRight = (position.y + robotSize/2)*(robotSize/2)/position.x;				
				} else if (objective.getClass() == Goal.class) {
					// Goal is tall, so reproject each end
					Double2D halfGoal = new Double2D(0, Goal.goalSize/2);
					Double2D leftPost = field.getObjectLocation(objective).add(halfGoal).subtract(current).rotate(-orientation);
					Double2D rightPost = field.getObjectLocation(objective).subtract(halfGoal).subtract(current).rotate(-orientation);
					imagePlaneLeft = leftPost.y*(robotSize/2)/leftPost.x;
					imagePlaneRight = rightPost.y*(robotSize/2)/rightPost.x;				
				}
				
				// Convert into pixels
				int pixelLeft = (int) Math.round(imagePlaneLeft*30/imageWidth) + 14;
				if (pixelLeft < 0)
					pixelLeft = 0;
				int pixelRight = (int) Math.round(imagePlaneRight*30/imageWidth) + 14;
				if (pixelRight > 29)
					pixelRight = 29;
				
				// Update the depth buffer and camera where not obscured
				double distance = position.length();
				for (int pixel = pixelLeft; pixel <= pixelRight; pixel++) {
					if (distance < depthBuffer[pixel]) {
						depthBuffer[pixel] = distance;
						camera[pixel] = objective.getClass() != Robot.class ? objective : null;
					}
				}
			}
		}
		
		// "segment" by removing all but the front-most object
		double minDistance = Double.MAX_VALUE;
		for (int pixel = 0; pixel < 30; pixel++) {
			// Look for the closest unobscured non-robot
			if (depthBuffer[pixel] < minDistance && camera[pixel] != null)
				minDistance = depthBuffer[pixel];
		}
		for (int pixel = 0; pixel < 30; pixel++)
			if (depthBuffer[pixel] != minDistance)
				camera[pixel] = null;
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
			return (pixelLeft + 29)/2;
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
	
	public String getRanges() {
		String rangeString = "";
		for (double range: ranges)
			if (range == Double.MAX_VALUE)
				rangeString += "--";
			else
				rangeString += String.format("%.2f ", range);
		return rangeString;
	}
	
	/**
	 * Uses the current speed to adjust the robot's position and orientation.
	 */
	private void move(Continuous2D field) {
		// Update the orientation based on relative wheel velocity
		orientation -= (rightSpeed - leftSpeed)/robotSize;
		
		// Keep orientation in [-pi,pi] range even though Oriented2D mods correctly
		if (orientation > Math.PI)
			orientation -= 2*Math.PI;
		else if (orientation < -Math.PI)
			orientation += 2*Math.PI;
		
		// Update the position based on midpoint speed and new orientation
		double midpointSpeed = (rightSpeed + leftSpeed)/2;
		Double2D direction = new Double2D(Math.cos(orientation), Math.sin(orientation));
		field.setObjectLocation(this, field.getObjectLocation(this).add(direction.multiply(midpointSpeed)));
		
		// If we're carrying something, update its position too
		if (carrying != null)
			field.setObjectLocation(carrying, field.getObjectLocation(this).add(direction.multiply((Robot.robotSize + Treat.treatSize)/2)));
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
