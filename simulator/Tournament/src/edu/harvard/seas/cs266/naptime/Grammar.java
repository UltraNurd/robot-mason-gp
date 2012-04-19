package edu.harvard.seas.cs266.naptime;

import java.util.ArrayList;
import java.util.List;

public class Grammar {

	public abstract class Expression {
		private Sexp sexp;
		
		public Expression(String value) throws InvalidSexpException {
			this.sexp = new Sexp("(" + value + ")");
		}
		
		public Expression(Sexp sexp, String name) throws InvalidSexpException {
			// Make sure this is the correct S-expression
			if (!sexp.firstAtomEquals(name))
				throw new InvalidSexpException(String.format("Expected '%s', found '%s'", name, sexp.getFirstAtom()));
			
			// Store the original S-expression for reference
			this.sexp = sexp;
		}
		
		public String toString() {
			return sexp.toString();
		}
		
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
			else if (input.getClass() == Sexp.class)
				sexp = (Sexp)input;
			else
				throw new InvalidSexpException("This is not an S-expression or atom");
			
			String name = sexp.getFirstAtom();
			if (name.equals(""))
				throw new InvalidSexpException("Expression did not start with atom");
			else if (name.equals(NoOp.name))
				return grammar.new NoOp(sexp);
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
			else if (name.equals(IsCarrying.name))
				return grammar.new IsCarrying(sexp);
			else if (name.equals(GetMidpointInCamera.name))
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
			super(value);
			this.value = Double.parseDouble(value);
		}
		
		public double getValue() {
			return value;
		}
	}
	
	public class NoOp extends Expression {
		public final static String name = "noop";
		
		public NoOp(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			if (sexp.getChildrenAfterFirst().size() != 0) {
				throw new InvalidSexpException("noop takes no arguments (duh)");
			}
		}
		
		public Boolean eval() {
			return true;
		}
		
		public double getValue() {
			return 0.0;
		}
	}
	
	public class Step extends Expression {
		public final static String name = "step";
		
		private List<Expression> steps = new ArrayList<Expression>();
		
		public Step(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			for (Object child: sexp.getChildrenAfterFirst()) {
				if (child.getClass() == Sexp.class) {
					steps.add(ExpressionFactory.build((Sexp)child));
				} else {
					throw new InvalidSexpException("Expected S-expression in step, got atom");
				}
			}
		}
		
		public Boolean eval(Robot robot) throws InvalidSexpException {
			// Evaluate each expression in turn
			Boolean success = true;
			for (Expression step: steps) {
				success = step.eval(robot) && success;
			}
			return success;
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
	}
	
	public class And extends Expression {
		public final static String name = "and";
		
		private List<Expression> expressions = new ArrayList<Expression>();
		
		public And(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			for (Object child: sexp.getChildrenAfterFirst()) {
				if (child.getClass() == Sexp.class) {
					expressions.add(ExpressionFactory.build((Sexp)child));
				} else {
					throw new InvalidSexpException("Expected S-expression in and, got atom");
				}
			}
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
	
	public class Or extends Expression {
		public final static String name = "or";
		
		private List<Expression> expressions = new ArrayList<Expression>();
		
		public Or(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			for (Object child: sexp.getChildrenAfterFirst()) {
				if (child.getClass() == Sexp.class) {
					expressions.add(ExpressionFactory.build((Sexp)child));
				} else {
					throw new InvalidSexpException("Expected S-expression in or, got atom");
				}
			}
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
	}
	
	public class IsCarrying extends Expression {
		public final static String name = "isCarrying";
		
		public IsCarrying(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			if (sexp.getChildrenAfterFirst().size() != 0) {
				throw new InvalidSexpException("isCarrying takes no arguments");
			}
		}
		
		public Boolean eval(Robot robot) {
			return robot.isCarrying();
		}
	}
	
	public class GetMidpointInCamera extends Expression {
		public final static String name = "getMidpoint";
		
		public GetMidpointInCamera(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			if (sexp.getChildrenAfterFirst().size() != 0) {
				throw new InvalidSexpException("getMidpoint takes no arguments");
			}
		}
		
		public double getValue(Robot robot) {
			return robot.findMidpointOfObjectiveInView();
		}
	}
	
	public class GetWidthInCamera extends Expression {
		public final static String name = "getWidth";
		
		public GetWidthInCamera(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			if (sexp.getChildrenAfterFirst().size() != 0) {
				throw new InvalidSexpException("getWidth takes no arguments");
			}
		}
		
		public double getValue(Robot robot) {
			return robot.findWidthOfObjectiveInView();
		}
	}
	
	public class PickUp extends Expression {
		public final static String name = "pickUp";
		
		public PickUp(Sexp sexp) throws InvalidSexpException {
			super(sexp, name);
			
			if (sexp.getChildrenAfterFirst().size() != 0) {
				throw new InvalidSexpException("pickUp takes no arguments");
			}
		}
		
		public Boolean eval(Robot robot) {
			return robot.pickUpObjective();
		}
	}
}
