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
integer = pyparsing.Regex(r'-?(0|[1-9]\d*)').setParseAction(lambda tokens: int(tokens[0]))
double = pyparsing.Regex(r'-?\d+.\d+').setParseAction(lambda tokens: float(tokens[0]))
sexpString = pyparsing.Word(pyparsing.alphas)
sexp = pyparsing.Forward()
sexp << pyparsing.Group(pyparsing.And([left_paren, pyparsing.OneOrMore(pyparsing.Or([sexpString, integer, double, sexp])), right_paren]))

operator_map = {
    "lt":"<",
    "lte":"<=",
    "gt":">",
    "gte":">=",
    "eq":"==",
    "and":"&&",
    "or":"||",
    "not":"!",
    }

leaf_operators = [
    "getMidpoint",
    "getWidth",
    "getTravel",
    "getRotations",
    "drop",
    "pickUp",
    ]

states = {
    "search":0,
    "carry":1,
    "backup":2,
    "uturn":3,
    }

def c_lines(sexp_list, indent = ""):
    """
    Generator to produce lines of C from a step program.
    """

    # Check if this is an atom
    if type(sexp_list) != list:
        yield sexp_list
        return

    # Get and check operator name
    op = sexp_list.pop(0)
    if op == "step":
        yield indent + "int current_state = 0;"
        for child in sexp_list:
            for line in c_lines(child, indent):
                yield line
    elif op == "if":
        yield indent + "if (%s) {" % "".join(c_lines(sexp_list[0], indent + "  "))
        lines = list(c_lines(sexp_list[1], indent + "  "))
        if len(lines) == 1:
            yield indent + "  " + lines[0] + ";"
        else:
            for line in lines:
                yield line
        if len(sexp_list) == 3:
            yield indent + "} else {"
            lines = list(c_lines(sexp_list[2], indent + "  "))
            if len(lines) == 1:
                yield indent + "  " + lines[0] + ";"
            else:
                for line in lines:
                    yield line
        yield indent + "}"
    elif op in ("and", "or"):
        if sexp_list[0][0] == "if" or (sexp_list[0][0] in ("and", "or") and sexp_list[0][1][0] == "if"):
            for child in sexp_list:
                for line in c_lines(child, indent):
                    yield line
        else:
            children = []
            for child in sexp_list:
                children.extend(c_lines(child, indent + "  "))
            yield (" %s " % operator_map[op]).join(children)
    elif op == "not":
        yield operator_map[op] + "".join(c_lines(sexp_list[0], indent + "  "))
    elif op in ("lt", "lte", "gt", "gte", "eq"):
        yield "%s %s %s" % ("".join(map(str, c_lines(sexp_list[0], indent + "  "))), operator_map[op], "".join(map(str, c_lines(sexp_list[1], indent + "  "))))
    elif op == "inState":
        yield "current_state == %d" % states[sexp_list[0]]
    elif op == "setState":
        yield "current_state = %d" % states[sexp_list[0]]
    elif op == "setSpeed":
        yield "setSpeed(%f, %f)" % tuple(sexp_list)
    elif op == "getRange":
        yield "getRange(%d)" % sexp_list[0]
    elif op in ("drop", "pickUp"):
        yield "current_state = %d" % int(op != "drop")
    elif op in leaf_operators:
        yield "%s()" % op
    else:
        yield str(sexp_list)

def convert(input_file, output_file):
    """
    Reads the specified input step program S-expression
    and writes out the equivalent C code to the
    specified output file.
    """

    # Parse
    parsed = sexp.parseFile(input_file, parseAll = True)

    # Dump
    output_f = open(output_file, "w")
    for line in c_lines(parsed.asList()[0]):
        output_f.write(line + "\n")
    output_f.close()

def main():
    if len(sys.argv) != 3:
        print "usage: convert_to_c.py <input step program> <output C code>"
        exit(0)

    convert(*sys.argv[1:])

if __name__ == "__main__":
    main()
