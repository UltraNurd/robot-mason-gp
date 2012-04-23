package edu.harvard.seas.cs266.naptime;

import java.io.File;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;

@SuppressWarnings("serial")
public class Team implements Steppable {
	/**
	 * Our strategy step program.
	 */
	public Grammar.Step strategy;
	
	/**
	 * Our robots.
	 */
	public Robot[] members = new Robot[3];
	
	/**
	 * Our goal.
	 */
	public Goal goal;
	
	public Team(Continuous2D field, Boolean opposing, Grammar.Step strategy) {
		if (strategy != null) {
			this.strategy = strategy;
		} else {
			// Load the default strategy if none was specified
			try {
				Sexp sexp = new Sexp(new File("/Users/nward/Documents/Harvard/2012.01-05/CS266/project/steps/baseline.sexp"));
				this.strategy = (Grammar.Step)Grammar.ExpressionFactory.build(sexp);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				try {
					this.strategy = (Grammar.Step)Grammar.ExpressionFactory.build(new Sexp("(step)"));
				} catch (InvalidSexpException ise) {
					// This will never happen, but Java makes me do it
					System.err.println(ise.getMessage());
				}
			}
		}
		
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
