Background
==========

This code is my portion of our final project for [CS266](http://www.eecs.harvard.edu/ssr/teaching/cs266/). It contains a set of Java classes that uses the [MASON simulation library](http://cs.gmu.edu/~eclab/projects/mason/ "MASON Multiagent Simulation Toolkit") and a genetic algorithm approach to evolve strategies for robot foraging. See Section 3 (Simulation) of [our final paper](http://www.ultranurd.net/robots/cs266/report.pdf) for more information.

I've shared the code here because I found that, other than the pretty solid [MASON manual](http://cs.gmu.edu/~eclab/projects/mason/manual.pdf), there wasn't a lot of documentation available showing how to do various things. I hope it's useful for others who want to simulate something similar.

Acknowledgements
================

My final project partners were Andrew Reiter and Pierre-Emile Duhamel. Our professor was [Rhadika Nagpal](http://www.eecs.harvard.edu/~rad/).

Files
=====

`README`
  You're looking at it.

`simulator`
  The source, intended to be opened as a workspace in Eclipse. The `Tournament` subdirectory is intended to be an Eclipse project, with the MASON project as its sibling and the various MASON jarfile dependencies available somewhere in your `$JAVAPATH`.

`steps`
  Assorted example strategies, in our domain-specific S-expression-based step program language. Some are manually written, some are evolved by the GA.