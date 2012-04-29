package edu.harvard.seas.cs266.naptime;

import java.util.ArrayList;
import java.util.List;

import ec.util.MersenneTwisterFast;

public class Grammar {

	public abstract class Expression {
		protected String name = "";
		
		public Expression() { }
		
		public Expression(Sexp sexp, String name) throws InvalidSexpException {
			// Make sure this is the correct S-expression
			if (!sexp.firstAtomEquals(name))
				throw new InvalidSexpException(String.format("Expected '%s', found '%s'", name, sexp.getFirstAtom()));
			
			// Store the expression name (used for deserialization)
			this.name = name;
		}
		
		public String toString() {
			return toSexp().toString();
		}
		
		/**
		 * Expected to be overridden by subclasses to produce unparsed
		 * equivalent of their contents.
		 */
		public abstract Object toSexp();
		
		/**
		 * Expected to be overridden by subclasses to produce unparsed
		 * mutated version of their contents.
		 * 
		 * @param rate The probability that a given node mutates.
		 * @param generator The seeded PRNG from the Tournament state.
		 */
		public abstract Object mutate(double rate, MersenneTwisterFast generator);

		/**
		 * Expected to be overridden by logical expressions (boolean operators, comparisons, etc.)
		 * @throws InvalidSexpException 
		 */
		public Boolean eval(Robot robot) throws InvalidSexpException {
			throw new InvalidSexpException("Value expression used in logical context");
		}
		
		/**
		 * Expected to be overridden by real-valued expressions (literals, sensors, etc.)
		 * @throws InvalidSexpException 
		 */
		public double getValue(Robot robot) throws InvalidSexpException {
			throw new InvalidSexpException("Logical expression used in value context");
		}
	}
	
	public static class ExpressionFactory {
		public final static Grammar grammar = new Grammar();
		
		public static Expression build(Object input) throws InvalidSexpException {
			Sexp sexp = null;
			if (input.getClass() == String.class)
				return grammar.new Literal((String)input);
			else if (input instanceof Expression)
				// Lazy man's copy by serializing to S-expression and reparsing
				return build(((Expression) input).toSexp());
			else if (input.getClass() == Sexp.class)
				sexp = (Sexp)input;
			else
				throw new InvalidSexpException("This is not an S-expression or atom");
			
			String name = sexp.getFirstAtom();
			if (name.equals(""))
				throw new InvalidSexpException("Expression did not start with atom");
			else if (name.equals(NoOp.name))
				return grammar.new NoOp(sexp);
			else if (name.equals(ValueNoOp.name))
				return grammar.new ValueNoOp(sexp);
			else if (name.equals(Step.name))
				return grammar.new Step(sexp);
			else if (name.equals(If.name))
				return grammar.new If(sexp);
			else if (name.equals(And.name))
				return grammar.new And(sexp);
			else if (name.equals(Or.name))
				return grammar.new Or(sexp);
			else if (name.equals(Not.name))
				return grammar.new Not(sexp);
			else if (name.equals(Equals.name))
				return grammar.new Equals(sexp);
			else if (name.equals(LessThan.name))
				return grammar.new LessThan(sexp);
			else if (name.equals(LessThanOrEquals.name))
				return grammar.new LessThanOrEquals(sexp);
			else if (name.equals(GreaterThan.name))
				return grammar.new GreaterThan(sexp);
			else if (name.equals(GreaterThanOrEquals.name))
				return grammar.new GreaterThanOrEquals(sexp);
			else if (name.equals(GetRange.name))
				return grammar.new GetRange(sexp);
			else if (name.equals(SetSpeed.name))
				return grammar.new SetSpeed(sexp);
			else if (name.equals(InState.name))
				return grammar.new InState(sexp);
			else if (name.equals(IsCarrying.name)) {
				System.err.printf("%s has been deprecated, use %s\n", IsCarrying.name, "inState");
				return grammar.new IsCarrying(sexp);
			} else if (name.equals(GetMidpointInCamera.name))
				return grammar.new GetMidpointInCamera(sexp);
			else if (name.equals(GetWidthInCamera.name))
				return grammar.new GetWidthInCamera(sexp);
			else if (name.equals(PickUp.name))
				return grammar.new PickUp(sexp);
			else
				throw new InvalidSexpException(String.format("Unexpected expression name '%s'", name));
		}
	}
	
	public class Literal extends Expression {
		private double value;
		
		public Literal(String value) throws InvalidSexpException {
			this.value = Double.parseDouble(value);
		}
		
		public double getValue(Robot robot) {
			return value;
		}
		
		public String toString() {
			return Double.toString(value);
		}

		@Override
		public Object toSexp() {
			return toString();
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			if (generator.nextDouble() < rate)
				if (value == 0.0)
					return Double.toString(generator.nextDouble()*0.2 - 0.1);
				else
					return Double.toString(value*(generator.nextDouble() + 0.5));
			else
				return toSexp();
		}
	}
	
	public abstract class LeafExpression extends Expression {
		public LeafExpression(Sexp sexp, String name) throws InvalidSexpException {
			super(sexp, name);
			
			if (sexp.getChildrenAfterFirst().size() != 0) {
				throw new InvalidSexpException(String.format("%s takes no arguments", name));
			}
		}

		@Override
		public Object toSexp() {
			return new Sexp(name, new ArrayList<Object>());
		}
	}
	
	public class NoOp extends LeafExpression {
		public final static String name = "noop";
		
		public NoOp(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) {
			return true;
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			// Much higher mutation rate, so we "grow" from no-ops
			if (generator.nextDouble() < 0.5) {
				final String[] names = {If.name, And.name, Or.name, Not.name,
										Equals.name, LessThan.name, LessThanOrEquals.name,
										GreaterThan.name, GreaterThanOrEquals.name,
										SetSpeed.name, IsCarrying.name, PickUp.name};
				String mutantName = names[generator.nextInt(names.length)];
				List<Object> children = new ArrayList<Object>();
				if (mutantName.equals(If.name) ||
					mutantName.equals(And.name) ||
					mutantName.equals(Or.name)) {
					children.add(new Sexp(NoOp.name, new ArrayList<Object>()));
					children.add(new Sexp(NoOp.name, new ArrayList<Object>()));
				} else if (mutantName.equals(Not.name))
					children.add(new Sexp(NoOp.name, new ArrayList<Object>()));
				else if (mutantName.equals(Equals.name) ||
						 mutantName.equals(LessThan.name) ||
						 mutantName.equals(LessThanOrEquals.name) ||
						 mutantName.equals(GreaterThan.name) ||
						 mutantName.equals(GreaterThanOrEquals.name)) {
					children.add(new Sexp(ValueNoOp.name, new ArrayList<Object>()));
					children.add("0.0");
				} else if (mutantName.equals(SetSpeed.name)) {
					children.add("0.0");
					children.add("0.0");
				}
				return new Sexp(mutantName, children);
			} else
				return toSexp();
		}
	}
	
	public class ValueNoOp extends LeafExpression {
		public final static String name = "vnoop";
		
		public ValueNoOp(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public double getValue(Robot robot) {
			return 0.0;
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			// Much higher mutation rate, so we "grow" from no-ops
			if (generator.nextDouble() < 0.5) {
				final String[] names = {GetRange.name, GetMidpointInCamera.name, GetWidthInCamera.name};
				String mutantName = names[generator.nextInt(names.length)];
				List<Object> children = new ArrayList<Object>();
				if (mutantName.equals(GetRange.name))
					children.add("0");
				return new Sexp(mutantName, children);
			} else
				return toSexp();
		}
	}
	
	public abstract class ListExpression extends Expression {
		protected List<Expression> expressions = new ArrayList<Expression>();
		
		public ListExpression(Sexp sexp, String name) throws InvalidSexpException {
			super(sexp, name);
			
			for (Object child: sexp.getChildrenAfterFirst()) {
				if (child.getClass() == Sexp.class) {
					expressions.add(ExpressionFactory.build((Sexp)child));
				} else {
					throw new InvalidSexpException(String.format("Expected S-expression in %s, got atom", name));
				}
			}
		}

		@Override
		public Object toSexp() {
			// Convert and add each child expression
			List<Object> children = new ArrayList<Object>();
			for (Expression expression: this.expressions)
				children.add(expression.toSexp());
			return new Sexp(this.name, children);
		}
		
		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			List<Object> children = new ArrayList<Object>();
			int deleteIndex = -1;
			if (generator.nextDouble() < rate && expressions.size() > 0)
				// Delete one child
				deleteIndex = generator.nextInt(expressions.size());
			for (Expression expression: expressions)
				if (deleteIndex == -1 || expressions.indexOf(expression) != deleteIndex)
					children.add(expression.mutate(rate, generator));
			return new Sexp(this.name, children);
		}
	}
	
	public class Step extends ListExpression {
		public final static String name = "step";
		
		public Step(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			// Evaluate each expression in turn
			Boolean success = true;
			for (Expression step: expressions) {
				success = step.eval(robot) && success;
			}
			return success;
		}
		
		public List<Step> crossover(Step mate, MersenneTwisterFast generator) {
			// Crossover at the S-expression level (easier)
			Sexp sexp = (Sexp) toSexp();
			Sexp mateSexp = (Sexp) mate.toSexp();
			
			// Find crossover points
			Sexp crossover = sexp.selectRandomNode(generator, false, false);
			Sexp mateCrossover = null;
			String crossoverName = crossover.getFirstAtom();
			if (crossoverName.equals(ValueNoOp.name) ||
				crossoverName.equals(GetRange.name) ||
				crossoverName.equals(GetMidpointInCamera.name) ||
				crossoverName.equals(GetWidthInCamera.name))
				mateCrossover = mateSexp.selectRandomNode(generator, false, true);
			else
				mateCrossover = mateSexp.selectRandomNode(generator, true, false);
			
			// Perform the crossover
			Sexp.swap(crossover, mateCrossover);
			
			// Convert the offspring back to Steps
			List<Step> children = new ArrayList<Step>(2);
			try {
				children.add(new Step(sexp));
				children.add(new Step(mateSexp));
			} catch (InvalidSexpException e) {
				// This shouldn't happen
				System.err.println(e.getMessage());
			}
			return children;
		}
	}
	
	public class If extends Expression {
		public final static String name = "if";
		
		private Expression predicate;
		
		private Expression consequent;
		
		private Expression alternative;
		
		public If(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		
			List<Object> contents = sexp.getChildrenAfterFirst();
			
			if (contents.size() >= 2) {
				predicate = ExpressionFactory.build((Sexp)contents.get(0));
				consequent = ExpressionFactory.build((Sexp)contents.get(1));
			} else {
				throw new InvalidSexpException("if requires at least 2 args");
			}
			if (contents.size() == 3) {
				alternative = ExpressionFactory.build((Sexp)contents.get(2));
			} else if (contents.size() > 3) {
				throw new InvalidSexpException("if cannot have more than 3 args");
			} else {
				alternative = null;
			}
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			if (predicate.eval(robot)) {
				return consequent.eval(robot);
			} else if (alternative != null) {
				return alternative.eval(robot);
			} else {
				return false;
			}
		}

		@Override
		public Object toSexp() {
			// Convert and add each child expression
			List<Object> children = new ArrayList<Object>();
			children.add(predicate.toSexp());
			children.add(consequent.toSexp());
			if (alternative != null)
				children.add(alternative.toSexp());
			return new Sexp(name, children);
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			List<Object> children = new ArrayList<Object>();
			children.add(predicate.mutate(rate, generator));
			if (generator.nextDouble() < rate) {
				if (generator.nextDouble() < 0.5) {
					// Reverse condition
					if (alternative != null) {
						children.add(alternative.mutate(rate, generator));
						children.add(consequent.mutate(rate, generator));
					} else {
						children.add(new Sexp(NoOp.name, new ArrayList<Object>()));
						children.add(consequent.mutate(rate, generator));
					}
				} else {
					// Add/delete alternative
					if (alternative != null)
						children.add(consequent.mutate(rate, generator));
					else {
						children.add(consequent.mutate(rate, generator));
						children.add(new Sexp(NoOp.name, new ArrayList<Object>()));
					}
				}
			} else {
				children.add(consequent.mutate(rate, generator));
				if (alternative != null)
					children.add(alternative.mutate(rate, generator));
			}
			return new Sexp(name, children);
		}
	}
	
	public class And extends ListExpression {
		public final static String name = "and";
		
		public And(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			// Evaluate each expression in turn, but short-circuit if one is false
			for (Expression expression: expressions) {
				if (!expression.eval(robot)) {
					return false;
				}
			}
			return true;
		}
	}
	
	public class Or extends ListExpression {
		public final static String name = "or";
		
		public Or(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);

		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			// Evaluate each expression in turn, but short-circuit once one is true
			for (Expression expression: expressions) {
				if (expression.eval(robot)) {
					return true;
				}
			}
			return false;
		}
	}
	
	public class Not extends Expression {
		public final static String name = "not";
		
		private Expression expression;
		
		public Not(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			List<Object> contents = sexp.getChildrenAfterFirst();
			
			if (contents.size() != 1) {
				throw new InvalidSexpException("not can only take a single expression");
			}
			
			expression = ExpressionFactory.build((Sexp)contents.get(0));
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			return !expression.eval(robot);
		}

		@Override
		public Object toSexp() {
			List<Object> children = new ArrayList<Object>();
			children.add(expression.toSexp());
			return new Sexp(name, children);
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			// Possibly negate
			if (generator.nextDouble() < rate)
				return expression.mutate(rate, generator);
			else {
				List<Object> children = new ArrayList<Object>();
				children.add(expression.mutate(rate, generator));
				return new Sexp(name, children);
			}
		}
	}
	
	public abstract class BinaryOperator extends Expression {
		protected Expression left;
		
		protected Expression right;
		
		public BinaryOperator(Sexp sexp, String name) throws InvalidSexpException {
			super(sexp, name);
			
			List<Object> contents = sexp.getChildrenAfterFirst();
			
			if (contents.size() != 2) {
				throw new InvalidSexpException(String.format("%s is a binary operator", name));
			}
			
			this.left = ExpressionFactory.build(contents.get(0));

			this.right = ExpressionFactory.build(contents.get(1));
		}

		@Override
		public Object toSexp() {
			// Convert and add each child expression
			List<Object> children = new ArrayList<Object>();
			children.add(left.toSexp());
			children.add(right.toSexp());
			return new Sexp(this.name, children);
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			// Possibly become a different binop
			String mutantName = this.name;
			if (generator.nextDouble() < rate) {
				final String[] names = {Equals.name, LessThan.name, LessThanOrEquals.name,
										GreaterThan.name, GreaterThanOrEquals.name};
				mutantName = names[generator.nextInt(names.length)];
			}
			List<Object> children = new ArrayList<Object>();
			children.add(left.mutate(rate, generator));
			children.add(right.mutate(rate, generator));
			return new Sexp(mutantName, children);
		}
	}
	
	public class Equals extends BinaryOperator {
		public final static String name = "eq";
		
		public Equals(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			return left.getValue(robot) == right.getValue(robot);
		}
	}
	
	public class LessThan extends BinaryOperator {
		public final static String name = "lt";
		
		public LessThan(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			return left.getValue(robot) < right.getValue(robot);
		}
	}

	public class LessThanOrEquals extends BinaryOperator {
		public final static String name = "lte";
		
		public LessThanOrEquals(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			return left.getValue(robot) <= right.getValue(robot);
		}
	}

	public class GreaterThan extends BinaryOperator {
		public final static String name = "gt";
		
		public GreaterThan(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			return left.getValue(robot) > right.getValue(robot);
		}
	}

	public class GreaterThanOrEquals extends BinaryOperator {
		public final static String name = "gte";
		
		public GreaterThanOrEquals(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			return left.getValue(robot) >= right.getValue(robot);
		}
	}
	
	public class GetRange extends Expression {
		public final static String name = "getRange";
		
		private int sensor;
		
		public GetRange(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			List<Object> contents = sexp.getChildrenAfterFirst();
			
			if (contents.size() != 1) {
				throw new InvalidSexpException("getRange takes only one argument");
			}
			
			sensor = Integer.parseInt((String)contents.get(0));
		}
		
		public double getValue(Robot robot) {
			return robot.getRange(sensor);
		}

		@Override
		public Object toSexp() {
			List<Object> children = new ArrayList<Object>();
			children.add(Integer.toString(sensor));
			return new Sexp(name, children);
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			if (generator.nextDouble() < rate) {
				List<Object> children = new ArrayList<Object>();
				children.add(Integer.toString(generator.nextInt(16)));
				return new Sexp(name, children);
			} else
				return toSexp();
		}
	}
	
	public class SetSpeed extends Expression {
		public final static String name = "setSpeed";
		
		private double left;
		
		private double right;
		
		public SetSpeed(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			List<Object> contents = sexp.getChildrenAfterFirst();
			
			if (contents.size() != 2) {
				throw new InvalidSexpException("setSpeed takes two arguments");
			}
			
			this.left = Double.parseDouble((String)contents.get(0));

			this.right = Double.parseDouble((String)contents.get(1));
		}

		public Boolean eval(Robot robot) {
			robot.setSpeed(left, right);
			return true;
		}

		@Override
		public Object toSexp() {
			List<Object> children = new ArrayList<Object>();
			children.add(Double.toString(left));
			children.add(Double.toString(right));
			return new Sexp(name, children);
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			if (generator.nextDouble() < rate) {
				List<Object> children = new ArrayList<Object>();
				if (left == 0.0)
					children.add(Double.toString(generator.nextDouble()*0.2 - 0.1));
				else
					children.add(Double.toString(left*(generator.nextDouble() + 0.5)));
				if (right == 0.0)
					children.add(Double.toString(generator.nextDouble()*0.2 - 0.1));
				else
					children.add(Double.toString(right*(generator.nextDouble() + 0.5)));
				return new Sexp(name, children);
			} else
				return toSexp();
		}
	}
	
	public class InState extends Expression {
		public final static String name = "inState";
		
		private Robot.State state;
		
		public InState(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			List<Object> contents = sexp.getChildrenAfterFirst();
			
			if (contents.size() != 1) {
				throw new InvalidSexpException("inState takes only one argument");
			}
			
			String stateString = (String)contents.get(0);
			try {
				state = Robot.State.valueOf(stateString.toUpperCase());
			} catch (Exception e) {
				throw new InvalidSexpException(String.format("invalid state %s", stateString));
			}
		}
		
		public Boolean eval(Robot robot) {
			return robot.inState(state);
		}

		@Override
		public Object toSexp() {
			List<Object> children = new ArrayList<Object>();
			children.add(state.toString().toLowerCase());
			return new Sexp(name, children);
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			if (generator.nextDouble() < rate) {
				List<Object> children = new ArrayList<Object>();
				Robot.State[] states = Robot.State.values();
				children.add(states[generator.nextInt(states.length)].toString().toLowerCase());
				return new Sexp(name, children);
			} else
				return toSexp();
		}
	}
	
	/**
	 * Kept around so old S-expressions can still be run.
	 * 
	 * @deprecated
	 */
	public class IsCarrying extends LeafExpression {
		public final static String name = "isCarrying";
		
		public IsCarrying(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) {
			return robot.inState(Robot.State.CARRY);
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			// For now, don't mutate this
			return toSexp();
		}
	}
	
	public class GetMidpointInCamera extends LeafExpression {
		public final static String name = "getMidpoint";
		
		public GetMidpointInCamera(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public double getValue(Robot robot) {
			return robot.findMidpointOfObjectiveInView();
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			// For now, don't mutate this
			return toSexp();
		}
	}
	
	public class GetWidthInCamera extends LeafExpression {
		public final static String name = "getWidth";
		
		public GetWidthInCamera(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public double getValue(Robot robot) {
			return robot.findWidthOfObjectiveInView();
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			// For now, don't mutate this
			return toSexp();
		}
	}
	
	public class PickUp extends LeafExpression {
		public final static String name = "pickUp";
		
		public PickUp(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
		}
		
		public Boolean eval(Robot robot) {
			return robot.pickUpObjective();
		}

		@Override
		public Object mutate(double rate, MersenneTwisterFast generator) {
			// For now, don't mutate this
			return toSexp();
		}
	}
}
