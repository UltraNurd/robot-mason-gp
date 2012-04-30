/**
 * @file Population.java
 * @author nward@fas.harvard.edu
 * @date 2012.04.21
 */

package edu.harvard.seas.cs266.naptime;

import java.io.File;
import java.io.FileNotFoundException;
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
	private int size;
	
	/**
	 * The rate of mutation, as a probability [0.0, 1.0].
	 */
	private double mutationRate;
	
	/**
	 * How many simulation iterations to run for each individual.
	 */
	private int simulations;
	
	/**
	 * The comparison individual, presumably manually written.
	 */
	private Individual baseline;
	
	/**
	 * The progenitor individual, presumably manually written.
	 */
	private Individual progenitor;
	
	/**
	 * The current set of individuals in this population.
	 */
	private List<Individual> individuals;
	
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
	public Population(File baselinePath, File progenitorPath, int size, double mutationRate, int simulations) throws FileNotFoundException, InvalidSexpException {
		// Set parameters
		this.size = size;
		this.mutationRate = mutationRate;
		this.simulations = simulations;
		
		// Read the strategy files
		baseline = new Individual(baselinePath);
		progenitor = new Individual(progenitorPath);
		
		// Start the population with the progenitors plus some mutations
		individuals = new ArrayList<Individual>(size);
		individuals.add(progenitor);
		MersenneTwisterFast generator = new MersenneTwisterFast(seed);
		for (int i = 1; i < size; i++) {
			Individual individual = new Individual(progenitorPath);
			individual.mutate(mutationRate, generator);
			individuals.add(individual);
		}
	}

	/**
	 * The main "tick" of the genetic programming algorithm. Determines the
	 * fitness of each individual, selects individuals for mating, performs
	 * crossover between parent pairs, mutates children as needed.
	 * 
	 * @return The current fittest individual, which may be less fit than
	 * the fittest from a previous generation.
	 */
	public Individual evolve() {
		// Update fitness by running the individual against the baseline
		for (Individual individual: individuals) {
			// Run the simulation for this individual, comparing against the baseline
			individual.run(baseline, simulations);
			//System.out.printf("%d %x %f\n", generations, individual.hashCode(), individual.getFitness());
		}
		
		// Select individuals for reproduction and find the fittest individual in this generation
		MersenneTwisterFast generator = new MersenneTwisterFast(seed);
		double totalFitness = 0.0, maxFitness = 0.0;
		for (Individual individual: individuals)
			totalFitness += individual.getFitness();
		Individual fittest = null;
		List<Individual> parents = new ArrayList<Individual>(size);
		for (Individual individual: individuals) {
			// Check if this individual is the fittest
			if (individual.getFitness() > maxFitness) {
				maxFitness = individual.getFitness();
				fittest = individual;
			}
			
			// Randomly select an individual, weighted by fitness
			double randomFitness = generator.nextDouble()*totalFitness;
			double summedFitness = 0.0;
			for (Individual parent: individuals) {
				if (summedFitness <= randomFitness && randomFitness < summedFitness + parent.getFitness()) {
					parents.add(parent);
					break;
				}
				summedFitness += parent.getFitness();
			}
		}
		
		// Dump some fitness stats for graphing
		System.out.printf("%d\t%f\t%f\n", generations, totalFitness/size, maxFitness);
		
		// Pairwise mate the parents, then mutate their offspring
		individuals.clear();
		for (int i = 0; i < parents.size(); i += 2) {
			individuals.addAll(parents.get(i).crossoverAndMutate(parents.get(i + 1), mutationRate, generator));
		}
		
		// Tick
		generations++;
		
		// Return the current fittest individual
		return fittest;
	}
	

	/**
	 * Repeatedly runs simulation using genetic programming on robot strategies.
	 * 
	 * @param args Path to the baseline strategy S-expression file.
	 */
	public static void main(String[] args) {
		// Check command-line parameters
		if (args.length != 7) {
			System.out.println("Usage: population <baseline strategy> <seed strategy> <population size> <mutation rate> <simulation iterations> <# generations> <fittest individual>");
			System.exit(0);
		}
		
		try {
			// Set up population of individuals representing robot strategies
			Population population = new Population(new File(args[0]), new File(args[1]), Integer.parseInt(args[2]), Double.parseDouble(args[3]), Integer.parseInt(args[4]));
			
			// Evolve several times for testing purposes
			Individual fittest = null;
			for (int i = 0; i < Integer.parseInt(args[5]); i++)
				fittest = population.evolve();
			
			// Dump the best evolved step, so we can see what they learned
			if (fittest != null)
				fittest.write(new File(args[6]));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		// Done
		System.exit(0);
	}
}
