package edu.harvard.seas.cs266.naptime;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;

@SuppressWarnings("serial")
public class Team implements Steppable {
	/**
	 * Our robots.
	 */
	public Robot[] members = new Robot[3];
	
	/**
	 * Our goal.
	 */
	public Goal goal;
	
	public Team(Continuous2D field, Boolean opposing) {
		// Create a goal at our end of the field
		goal = new Goal();
		if (opposing)
			field.setObjectLocation(goal, new Double2D(field.getWidth(), field.getHeight()*0.5));
		else
			field.setObjectLocation(goal, new Double2D(0.0, field.getHeight()*0.5));
		
		// Position our robots in the field after creating them
		for (int r = 0; r < members.length; r++)
			if (opposing)
				members[r] = new Robot(this, Math.PI);
			else
				members[r] = new Robot(this, 0.0);				
		if (opposing) {
			field.setObjectLocation(members[0], new Double2D(field.getWidth()*0.8, field.getHeight()*0.2));
			field.setObjectLocation(members[1], new Double2D(field.getWidth()*0.6, field.getHeight()*0.5));
			field.setObjectLocation(members[2], new Double2D(field.getWidth()*0.8, field.getHeight()*0.8));
		} else {
			field.setObjectLocation(members[0], new Double2D(field.getWidth()*0.2, field.getHeight()*0.2));
			field.setObjectLocation(members[1], new Double2D(field.getWidth()*0.4, field.getHeight()*0.5));
			field.setObjectLocation(members[2], new Double2D(field.getWidth()*0.2, field.getHeight()*0.8));			
		}
	}

	@Override
	public void step(SimState state) {
		// For now just have each robot behave individually
		for (Robot member : members) {
			member.step(state);
		}
	}
}
