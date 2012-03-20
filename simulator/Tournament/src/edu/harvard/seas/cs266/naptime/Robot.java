package edu.harvard.seas.cs266.naptime;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.util.Bag;
import sim.util.Double2D;

@SuppressWarnings("serial")
public class Robot implements Steppable {
	@Override
	public void step(SimState state) {
		// Get the current simulation
		Tournament tourney = (Tournament) state;
		
		// Determine where we are and where to go
		Double2D current = tourney.field.getObjectLocation(this);
		Double2D objective = senseNextObjective(current, tourney.field);
		
		// Move in that direction, slowing as we approach
		double distanceToObjective = current.distance(objective);
		if (distanceToObjective < 8.0)
			return;
		double speed = 1.0;
		Double2D destination = objective.subtract(current);
		destination = destination.normalize();
		destination = destination.resize(speed);
		destination = destination.add(current);
		tourney.field.setObjectLocation(this, destination);
	}

	private Double2D senseNextObjective(Double2D current, Continuous2D field) {
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
		if (objective == null)
			return current;
		return objective;
	}
}
