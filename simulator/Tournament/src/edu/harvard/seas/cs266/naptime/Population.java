/**
 * @file Sexp.java
 * @author nward@fas.harvard.edu
 * @date 2012.04.21
 */

package edu.harvard.seas.cs266.naptime;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents our current genetic programming state, storing
 * some number of Step program trees.
 * 
 * @author nward@fas.harvard.edu
 */
public class Population {
	/**
	 * The random seed we've been using, which governs consistent food
	 * initial conditions.
	 */
	private final static long seed = 1333593072282L;
	
	/**
	 * Number of individuals in the population at any given time. Must
	 * be even for mating purposes.
	 */
	private final static int size = 20;
	
	/**
	 * The progenitor individual, presumably manually written.
	 */
	private Grammar.Step baseline;
	
	/**
	 * The current set of individuals in this population.
	 */
	private List<Grammar.Step> individuals = new ArrayList<Grammar.Step>();
	
	/**
	 * The number of generations for which this population has evolved.
	 */
	private int generations = 0;
	
	/**
	 * Initialize the population with a known working individual and
	 * create a number of mutations for initial diversity.
	 * 
	 * @throws InvalidSexpException 
	 * @throws FileNotFoundException 
	 */
	public Population(String strategyFile) throws FileNotFoundException, InvalidSexpException {
		// For now, just hardcode the location of the manual strategy
		Sexp sexp = new Sexp(new File(strategyFile));
		baseline = (Grammar.Step)Grammar.ExpressionFactory.build(sexp);
		
		// Start the population with the baseline plus some mutations
		individuals.add(baseline);
		for (int i = 1; i < size; i++) {
			Grammar.Step strategy = (Grammar.Step)Grammar.ExpressionFactory.build(baseline);
			individuals.add(strategy);
		}
	}

	/**
	 * The main "tick" of the genetic programming algorithm. Determines the
	 * fitness of each individual, selects individuals for mating, performs
	 * crossover between parent pairs, mutates children as needed.
	 */
	public void evolve() {
		// Determine fitness by running the individual against the baseline
		//   The body of this loop kinda reimplements SimState.doLoop()
		//   @see MASON Manual pp. 83-84
		double[] fitnesses = new double[individuals.size()];
		for (Grammar.Step individual: individuals) {
			// Set up the simulation with this strategy and give it a unique ID
			Tournament tourney = new Tournament(seed, individual, baseline);
			tourney.nameThread();
			tourney.setJob(generations ^ individual.hashCode());
			
			// Run the simulation for some large number of steps (baseline can complete in ~6000)
			tourney.start();
			do
				if (!tourney.schedule.step(tourney))
					// Stop if the end condition has been reached
					break;
			while (tourney.schedule.getSteps() < 20000);
			
			// Measure fitness and cleanup
			double fitness = tourney.getFitness();
			fitnesses[individuals.indexOf(individual)] = fitness;
			//System.out.printf("%d %x %f\n", generations, individual.hashCode(), fitness);
			tourney.finish();
		}
		
		// Tick
		generations++;
	}
	

	/**
	 * Repeatedly runs simulation using genetic programming on robot strategies.
	 * 
	 * @param args Path to the baseline strategy S-expression file.
	 */
	public static void main(String[] args) {
		// Check command-line parameters
		if (args.length != 1) {
			System.out.println("Usage: population <baseline strategy>");
			System.exit(0);
		}
		
		try {
			// Set up population of individuals representing robot strategies
			Population population = new Population(args[0]);
			
			// Evolve once for testing purposes
			population.evolve();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		// Done
		System.exit(0);
	}
}
