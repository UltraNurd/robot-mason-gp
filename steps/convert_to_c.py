#!/usr/bin/env python
###
# convert_to_c.py
#
# Reads a possibly machine-generated step program and
# converts it to C code that will run on the e-pucks
# (with some manual editing).
#
# Nicolas Ward
# nward@fas.harvard.edu
# 2012.04.30
###

import sys
import pyparsing

# Define really basic S-expression parser
left_paren, right_paren = map(pyparsing.Suppress, "()")
integer = pyparsing.Regex(r'-?0|[1-9]\d*').setParseAction(lambda tokens: int(tokens[0]))
double = pyparsing.Regex(r'-?\d+.\d+').setParseAction(lambda tokens: float(tokens[0]))
sexpString = pyparsing.Word(pyparsing.alphas)
sexp = pyparsing.Forward()
sexp << pyparsing.Group(pyparsing.And([left_paren, pyparsing.OneOrMore(pyparsing.Or([sexpString, integer, double, sexp])), right_paren]))

def convert(input_file, output_file):
    """
    Reads the specified input step program S-expression
    and writes out the equivalent C code to the
    specified output file.
    """

    # Parse
    parsed = sexp.parseFile(input_file)

    # Dump
    output_f = open(output_file, "w")
    print parsed
    output_f.write(str(parsed.asList()))
    output_f.close()

def main():
    if len(sys.argv) != 3:
        print "usage: convert_to_c.py <input step program> <output C code>"
        exit(0)

    convert(*sys.argv[1:])

if __name__ == "__main__":
    main()
