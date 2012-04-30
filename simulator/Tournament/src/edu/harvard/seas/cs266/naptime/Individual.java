/**
 * @file Individual.java
 * @author nward@fas.harvard.edu
 * @date 2012.04.29
 */

package edu.harvard.seas.cs266.naptime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ec.util.MersenneTwisterFast;

/**
 * Encapsulates some functionality for evolving strategies in
 * a genetic programming population.
 * 
 * @author nward@fas.harvard.edu
 */
public class Individual {
	/**
	 * The set of strategies associated with this individual, could be
	 * up to one per robot. Minimum length 1.
	 */
	private List<Grammar.Step> strategies = new ArrayList<Grammar.Step>();
	
	/**
	 * The fitness of this individual as of its last run.
	 */
	private double fitness = 0.0;

	public Individual(File strategyPath) throws FileNotFoundException, InvalidSexpException {
		// Read the strategy file(s)
		if (strategyPath.isFile())
			strategies.add((Grammar.Step)Grammar.ExpressionFactory.build(new Sexp(strategyPath)));
		else
			for (File strategyFile: strategyPath.listFiles())
				strategies.add((Grammar.Step)Grammar.ExpressionFactory.build(new Sexp(strategyFile)));
	}
	
	public Individual(List<Grammar.Step> strategies) {
		this.strategies = strategies;
	}

	public Individual(Individual other) {
		this.strategies = other.strategies;
	}

	/**
	 * The body of this method kinda reimplements SimState.doLoop().
	 * Updates fitness by running this individual's strategies against
	 * those of the baseline.
	 * 
	 * @see MASON Manual pp. 83-84
	 */
	public void run(Individual baseline, int iterations) {
		// Set up the simulation with this strategy and give it a unique ID
		Tournament tourney = new Tournament(Population.seed, strategies, baseline.strategies);
		tourney.nameThread();
		tourney.setJob(hashCode());
		
		// Run the simulation multiple times to avoid initial conditions bias
		double totalFitness = 0.0;
		for (int i = 0; i < iterations; i++) {
			// Run the simulation for some large number of steps (baseline can complete in ~6000)
			tourney.start();
			do
				if (!tourney.schedule.step(tourney))
					// Stop if the end condition has been reached
					break;
			while (tourney.schedule.getSteps() < 20000);
			totalFitness += tourney.getFitness();
			tourney.finish();
		}
		
		// Measure average fitness
		fitness = totalFitness/iterations;
	}
	
	public void mutate(double mutationRate, MersenneTwisterFast generator) {
		// Mutate all constituent strategies
		List<Grammar.Step> mutantStrategies = new ArrayList<Grammar.Step>(strategies.size());
		for (Grammar.Step strategy: strategies) {
			try {
				mutantStrategies.add((Grammar.Step)Grammar.ExpressionFactory.build(strategy.mutate(mutationRate, generator)));
			} catch (InvalidSexpException e) {
				// This shouldn't happen
				System.err.println(e.getMessage());
			}
		}
		
		// Replace
		strategies = mutantStrategies;
	}

	public List<Individual> crossoverAndMutate(Individual mate, double mutationRate, MersenneTwisterFast generator) {
		// Select one of the constituent strategy pairs for crossover
		List<Individual> children = new ArrayList<Individual>(2);
		List<Grammar.Step> leftStrategies = new ArrayList<Grammar.Step>(strategies.size());
		List<Grammar.Step> rightStrategies = new ArrayList<Grammar.Step>(strategies.size());
		int crossoverIndex = generator.nextInt(strategies.size());
		for (int i = 0; i < strategies.size(); i++) {
			try {
				if (i == crossoverIndex) {
					List<Grammar.Step> crossedStrategies = strategies.get(i).crossover(mate.strategies.get(i), generator);
					leftStrategies.add((Grammar.Step)Grammar.ExpressionFactory.build(crossedStrategies.get(0).mutate(mutationRate, generator)));
					rightStrategies.add((Grammar.Step)Grammar.ExpressionFactory.build(crossedStrategies.get(1).mutate(mutationRate, generator)));				
				} else {
					leftStrategies.add((Grammar.Step)Grammar.ExpressionFactory.build(strategies.get(i).mutate(mutationRate, generator)));
					rightStrategies.add((Grammar.Step)Grammar.ExpressionFactory.build(mate.strategies.get(i).mutate(mutationRate, generator)));
				}
			} catch (InvalidSexpException e) {
				// This shouldn't happen
				System.err.println(e.getMessage());
			}
		}
		children.add(new Individual(leftStrategies));
		children.add(new Individual(rightStrategies));
		return children;
	}

	public void write(File outputDir) throws FileNotFoundException {
		outputDir.mkdirs();
		int index = 1;
		for (Grammar.Step strategy: strategies) {
			File outputFile = new File(outputDir, String.format("%d.sexp", index));
			PrintWriter writer = new PrintWriter(outputFile.getPath());
			writer.print(strategy.toString());
			writer.flush();
			writer.close();
			index++;
		}
	}
	
	/**
	 * Accessor for the current fitness of this individual.
	 */
	public double getFitness() {
		return fitness;
	}
}
