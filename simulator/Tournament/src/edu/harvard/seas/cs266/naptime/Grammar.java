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
		public Boolean eval() throws InvalidSexpException {
			throw new InvalidSexpException("Value expression used in logical context");
		}
		
		/**
		 * Expected to be overridden by real-valued expressions (literals, sensors, etc.)
		 * @throws InvalidSexpException 
		 */
		public double getValue() throws InvalidSexpException {
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
		
		public Boolean eval() throws InvalidSexpException {
			// Evaluate each expression in turn
			Boolean success = true;
			for (Expression step: steps) {
				success = step.eval() && success;
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
		
		public Boolean eval() throws InvalidSexpException {
			if (predicate.eval()) {
				return consequent.eval();
			} else if (alternative != null) {
				return alternative.eval();
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
		
		public Boolean eval() throws InvalidSexpException {
			// Evaluate each expression in turn, but short-circuit if one is false
			for (Expression expression: expressions) {
				if (!expression.eval()) {
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
		
		public Boolean eval() throws InvalidSexpException {
			// Evaluate each expression in turn, but short-circuit once one is true
			for (Expression expression: expressions) {
				if (expression.eval()) {
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
		
		public Boolean eval() throws InvalidSexpException {
			return !expression.eval();
		}
	}
}
