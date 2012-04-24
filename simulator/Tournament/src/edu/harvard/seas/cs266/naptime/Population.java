/**
 * @file Sexp.java
 * @author nward@fas.harvard.edu
 * @date 2012.04.21
 */

package edu.harvard.seas.cs266.naptime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ec.util.MersenneTwisterFast;

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
	final static long seed = 1333593072282L;
	
	/**
	 * Number of individuals in the population at any given time. Must
	 * be even for mating purposes.
	 */
	private final static int size = 40;
	
	/**
	 * The rate of mutation, as a probability [0.0, 1.0].
	 */
	private final static double mutationRate = 0.05;
	
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
			Grammar.Step strategy = (Grammar.Step)Grammar.ExpressionFactory.build(baseline.mutate(mutationRate, (new Tournament(seed)).random));
			individuals.add(strategy);
		}
	}

	/**
	 * The main "tick" of the genetic programming algorithm. Determines the
	 * fitness of each individual, selects individuals for mating, performs
	 * crossover between parent pairs, mutates children as needed.
	 */
	public Grammar.Step evolve() {
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
		
		// Select individuals for reproduction and find the fittest individual in this generation
		MersenneTwisterFast generator = (new Tournament(seed)).random;
		double totalFitness = 0.0, maxFitness = 0.0;
		int fittestIndex = -1;
		for (double fitness: fitnesses)
			totalFitness += fitness;
		List<Grammar.Step> parents = new ArrayList<Grammar.Step>(individuals.size());
		for (int i = 0; i < individuals.size(); i++) {
			// Check if this individual is the fittest
			if (fitnesses[i] > maxFitness) {
				maxFitness = fitnesses[i];
				fittestIndex = i;
			}
			
			// Randomly select an individual, weighted by fitness
			double randomFitness = generator.nextDouble()*totalFitness;
			double summedFitness = 0.0;
			for (int j = 0; j < fitnesses.length; j++) {
				if (summedFitness <= randomFitness && randomFitness < summedFitness + fitnesses[j]) {
					Grammar.Step parent = individuals.get(j);
					//System.out.printf("  Selected %d (%x)\n", j, parent.hashCode());
					parents.add(parent);
					break;
				}
				summedFitness += fitnesses[j];
			}
		}
		Grammar.Step fittest = null;
		if (fittestIndex != -1)
			fittest = individuals.get(fittestIndex);
		
		// Dump some fitness stats for graphing
		System.out.printf("%d\t%f\t%f\n", generations, totalFitness/individuals.size(), maxFitness);
		
		// Pairwise mate the parents, then mutate their offspring
		List<Grammar.Step> children = new ArrayList<Grammar.Step>(individuals.size());
		individuals.clear();
		for (int i = 0; i < parents.size(); i += 2) {
			children.addAll(parents.get(i).crossover(parents.get(i + 1), generator));
		}
		for (Grammar.Step child: children)
			try {
				individuals.add((Grammar.Step)Grammar.ExpressionFactory.build(child.mutate(mutationRate, generator)));
			} catch (Exception e) {
				// Shouldn't happen
				System.err.println(e.getMessage());
			}
		
		// Tick
		generations++;
		
		// Return the current fittest individual
		if (fittestIndex != -1) {
			//System.out.printf("Fittest: %d %x %f\n", fittestIndex, fittest.hashCode(), fitnesses[fittestIndex]);
			return fittest;
		} else
			return null;
	}
	

	/**
	 * Repeatedly runs simulation using genetic programming on robot strategies.
	 * 
	 * @param args Path to the baseline strategy S-expression file.
	 */
	public static void main(String[] args) {
		// Check command-line parameters
		if (args.length != 2) {
			System.out.println("Usage: population <baseline strategy> <fittest individual>");
			System.exit(0);
		}
		
		try {
			// Set up population of individuals representing robot strategies
			Population population = new Population(args[0]);
			
			// Evolve several times for testing purposes
			Grammar.Step fittest = null;
			for (int i = 0; i < 10; i++)
				fittest = population.evolve();
			
			// Dump the best evolved step, so we can see what they learned
			if (fittest != null) {
				PrintWriter writer = new PrintWriter(args[1]);
				writer.print(fittest.toString());
				writer.flush();
				writer.close();
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		// Done
		System.exit(0);
	}
}
