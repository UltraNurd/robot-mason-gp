/**
 * @file Sexp.java
 * @author nward@fas.harvard.edu
 * @date 2012.04.17
 */

package edu.harvard.seas.cs266.naptime;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ec.util.MersenneTwisterFast;

/**
 * Simple S-expression implementation that makes little distinction
 * between atoms/lists and doesn't handle quoting or comments.
 * 
 * @author nward@fas.harvard.edu
 */
public class Sexp {
	private Sexp parent;
	
	private List<Object> children;
	
	public Sexp(File sexpPath) throws FileNotFoundException, InvalidSexpException {
		this(new Scanner(sexpPath, "UTF-8").useDelimiter("\\A").next());
	}
	
	/**
	 * Constructor for building S-expressions in-memory instead of parsing strings.
	 * 
	 * @param name The string function starting this expression; will become
	 * the first element of children.
	 * @param children Can be null. Will be appended to children.
	 */
	public Sexp(String name, List<Object> children) {
		// Initialize the child atoms/lists
		this.children = new ArrayList<Object>();
		
		// Insert the expression name
		this.children.add(name);
		
		// If there are children, add them
		if (children != null)
			this.children.addAll(children);
		for (Object child: children)
			if (child.getClass() == Sexp.class)
				((Sexp)child).parent = this;
	}
	
	public Sexp(String expression) throws InvalidSexpException {
		this(expression, (Sexp) null);
	}
	
	public Sexp(String expression, Sexp parent) throws InvalidSexpException {
		// Maintain links up the tree
		this.parent = parent;
		
		// Initialize the child atoms/lists
		children = new ArrayList<Object>();
		
		// Track the current nesting depth; we only care about content at the top level
		int depth = 0;
		
		// Accumulate the current token (which could be a child expression)
		String token = "";
		
		// Loop through the string, parsing the expression recursively
		for (int i = 0; i < expression.length(); i++) {
			// Get the current character
			char c = expression.charAt(i);
			
			// Behave differently depending on parse depth
			if (depth == 0) {
				if (c == '(') {
					depth++;
				} else if (!Character.isWhitespace(c)) {
					throw new InvalidSexpException("content outside of expression");
				}
			} else if (depth == 1) {
				if (token == "") {
					if (!Character.isWhitespace(c)) {
						token += c;
					}
					if (c == '(') {
						depth++;
					} else if (c == ')') {
						depth--;
					}
				} else {
					if (Character.isWhitespace(c)) {
						if (token.charAt(0) == '(') {
							children.add(new Sexp(token, this));
						} else {
							children.add(token);
						}
						token = "";
					} else if (c == ')') {
						if (token.charAt(0) == '(') {
							children.add(new Sexp(token, this));
						} else {
							children.add(token);
						}
						token = "";
						depth--;
					} else {
						token += c;
					}
				}
			} else {
				token += c;
				if (c == '(') {
					depth++;
				} else if (c == ')') {
					depth--;
				}
			}
		}
		
		if (depth != 0)
			throw new InvalidSexpException("Expression not terminated");
	}
	
	public List<Sexp> flatten(Boolean booleanOnly, Boolean valueOnly) {
		List<Sexp> flat = new ArrayList<Sexp>();
		if (!firstAtomEquals(Grammar.Step.name))
			if (firstAtomEquals(Grammar.ValueNoOp.name) ||
				firstAtomEquals(Grammar.GetRange.name) ||
				firstAtomEquals(Grammar.GetMidpointInCamera.name) ||
				firstAtomEquals(Grammar.GetWidthInCamera.name) ||
				firstAtomEquals(Grammar.GetDistanceTraveled.name) ||
				firstAtomEquals(Grammar.GetRotations.name)) {
				if (!booleanOnly)
					flat.add(this);
			} else {
				if (!valueOnly)
					flat.add(this);
			}
		for (Object child: children)
			if (child.getClass() == Sexp.class)
				flat.addAll(((Sexp)child).flatten(booleanOnly, valueOnly));
		return flat;
	}
	
	public Sexp selectRandomNode(MersenneTwisterFast generator, Boolean mustBeBoolean, Boolean mustBeValue) {
		// First, flatten this S-expression for convenient random selection, possibly filtering by expression name
		List<Sexp> flat = flatten(mustBeBoolean, mustBeValue);
		if (flat.size() == 0)
			return null;
		return flat.get(generator.nextInt(flat.size()));
	}
	
	public Boolean firstAtomEquals(String label) {
		return children.size() > 0 && children.get(0).getClass() == String.class && ((String)children.get(0)).equals(label);
	}
	
	public String getFirstAtom() {
		if (children.size() > 0 && children.get(0).getClass() == String.class) {
			return (String)children.get(0);
		} else
			return "";
	}
	
	public List<Object> getChildrenAfterFirst() {
		return children.subList(1, children.size());
	}
	
	public String toString() {
		return prettyString(0);
	}

	private String prettyString(int depth) {
		String indent = "";
		for (int i = 0; i < depth; i++)
			indent += "  ";
		String pretty = indent + "(\n";
		for (Object child: children) {
			if (child.getClass() == Sexp.class) {
				pretty += ((Sexp)child).prettyString(depth + 1);
			} else {
				pretty += indent + child.toString() + "\n";
			}
		}
		pretty += indent + ")\n";
		return pretty;
	}
	
	public static void swap(Sexp left, Sexp right) {
		// Make sure these are swappable (i.e. have parents)
		if (left.parent == null || right.parent == null)
			return;
		
		// Cache the current parents
		Sexp leftParent = left.parent;
		Sexp rightParent = right.parent;
		
		// Update children, then exchange parents
		leftParent.children.set(leftParent.children.indexOf(left), right);
		rightParent.children.set(rightParent.children.indexOf(right), left);
		left.parent = rightParent;
		right.parent = leftParent;
	}
}
