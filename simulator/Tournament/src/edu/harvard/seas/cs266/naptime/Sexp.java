/**
 * @file Sexp.java
 * @author nward@fas.harvard.edu
 * @date 2012.04.17
 */

package edu.harvard.seas.cs266.naptime;

import java.util.ArrayList;

/**
 * Simple S-expression implementation that makes little distinction
 * between atoms/lists and doesn't handle quoting or comments.
 * 
 * @author nward@fas.harvard.edu
 */
public class Sexp {
	private ArrayList<Object> children;
	
	public Sexp(String expression) throws InvalidSexpException {
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
					}
				} else {
					if (Character.isWhitespace(c)) {
						if (token.charAt(0) == '(') {
							children.add(new Sexp(token));
						} else {
							children.add(token);
						}
						token = "";
					} else if (c == ')') {
						if (token.charAt(0) == '(') {
							children.add(new Sexp(token));
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
}
