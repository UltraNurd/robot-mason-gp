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
	
	public Team(Continuous2D field) {
		// Position our robots in the field after creating them
		for (int r = 0; r < members.length; r++)
			members[r] = new Robot();
		field.setObjectLocation(members[0], new Double2D(field.getWidth()*0.2, field.getHeight()*0.2));
		field.setObjectLocation(members[1], new Double2D(field.getWidth()*0.4, field.getHeight()*0.5));
		field.setObjectLocation(members[2], new Double2D(field.getWidth()*0.2, field.getHeight()*0.8));
	}

	@Override
	public void step(SimState state) {
		// For now just have each robot behave individually
		for (Robot member : members) {
			member.step(state);
		}
	}
}
