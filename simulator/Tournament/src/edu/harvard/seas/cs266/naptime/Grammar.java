package edu.harvard.seas.cs266.naptime;

import java.util.ArrayList;
import java.util.List;

public class Grammar {

	public abstract class Expression {
		private Sexp sexp;
		
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
		
		public abstract Boolean eval();
	}
	
	public static class ExpressionFactory {
		public final static Grammar grammar = new Grammar();
		
		public static Expression build(Sexp sexp) throws InvalidSexpException {
			String name = sexp.getFirstAtom();
			if (name.equals(""))
				throw new InvalidSexpException("Expression did not start with atom");
			else if (name.equals(Step.name))
				return grammar.new Step(sexp);
			else
				throw new InvalidSexpException(String.format("Unexpected expression name '%s'", name));
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
				}
			}
		}
		
		public Boolean eval() {
			// Evaluate each expression in turn
			Boolean success = true;
			for (Expression step: steps) {
				success = step.eval() && success;
			}
			return success;
		}
	}
}
