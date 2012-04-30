package edu.harvard.seas.cs266.naptime;

import java.io.File;
import java.util.List;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;

@SuppressWarnings("serial")
public class Team implements Steppable {
	/**
	 * Which team are we?
	 */
	public Boolean opposing;
	
	/**
	 * Our robots.
	 */
	public Robot[] members = new Robot[3];
	
	/**
	 * Our goal.
	 */
	public Goal goal;
	
	public Team(Continuous2D field, Boolean opposing, List<Grammar.Step> strategy) {
		this.opposing = opposing;
		
		if (strategy.size() == 0) {
			// Load the default strategy if none was specified
			try {
				Sexp sexp = new Sexp(new File("/Users/nward/Documents/Harvard/2012.01-05/CS266/project/steps/baseline.sexp"));
				strategy.add((Grammar.Step)Grammar.ExpressionFactory.build(sexp));
			} catch (Exception e) {
				System.err.println(e.getMessage());
				try {
					strategy.add((Grammar.Step)Grammar.ExpressionFactory.build(new Sexp("(step)")));
				} catch (InvalidSexpException ise) {
					// This will never happen, but Java makes me do it
					System.err.println(ise.getMessage());
				}
			}
		}
		
		// Create a goal at our end of the field
		goal = new Goal(this);
		if (opposing)
			field.setObjectLocation(goal, new Double2D(0.0, field.getHeight()*0.5));
		else
			field.setObjectLocation(goal, new Double2D(field.getWidth(), field.getHeight()*0.5));
		
		// Position our robots in the field after creating them
		for (int r = 0; r < members.length; r++)
			if (opposing)
				members[r] = new Robot(strategy.get(r % strategy.size()), this, Math.PI);
			else
				members[r] = new Robot(strategy.get(r % strategy.size()), this, 0.0);				
		if (opposing) {
			field.setObjectLocation(members[0], new Double2D(field.getWidth() - Robot.robotSize, field.getHeight()*0.2));
			field.setObjectLocation(members[1], new Double2D(field.getWidth() - Robot.robotSize, field.getHeight()*0.5));
			field.setObjectLocation(members[2], new Double2D(field.getWidth() - Robot.robotSize, field.getHeight()*0.8));
		} else {
			field.setObjectLocation(members[0], new Double2D(Robot.robotSize, field.getHeight()*0.2));
			field.setObjectLocation(members[1], new Double2D(Robot.robotSize, field.getHeight()*0.5));
			field.setObjectLocation(members[2], new Double2D(Robot.robotSize, field.getHeight()*0.8));			
		}
	}

	@Override
	public void step(SimState state) {
		// For now just have each robot behave individually
		//   Track travel distance
		double minDistance = Double.MAX_VALUE, maxDistance = 0.0;
		for (Robot member : members) {
			member.step(state);
			double distance = member.getTotalDistanceTraveled();
			if (distance < minDistance)
				minDistance = distance;
			if (distance > maxDistance)
				maxDistance = distance;
		}
		
		// Update the non-movement penalty flag if necessary
		if (!opposing && minDistance*100 < maxDistance)
			((Tournament)state).setPenalty(10);
		
		// Have goals score any nearby treats
		goal.step(state);
	}
}
