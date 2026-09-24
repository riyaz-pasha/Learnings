# Project Description for Computational Biology

## Introduction

Computational biology is the field where software meets genetics — DNA sequences, viruses, and proteins are all, at bottom, just strings over a small alphabet, and a surprising number of real biology questions turn out to be classic string and array algorithms wearing a lab coat.

The scenario in this chapter puts us on a team building diagnostic and research tooling for a genomics lab.

## Statement

Imagine you're a developer working with a team of biologists who study DNA, viruses, and proteins across different species. Every sample they collect — a chromosome, an infected strand, a folded protein — arrives as a string of nucleotides, and the lab needs software to make sense of it: detect mutations, spot embedded viruses, identify proteins by their structure, tell one species' DNA from another's, and measure how similar two samples are.

## Features

To build all of this, we'll implement nine features:

- **Feature #1:** Determine whether one DNA sample can be mutated into another by replacing genes one type at a time.
- **Feature #2:** Detect a virus by finding the longest stretch of a DNA sample that contains at most `k` distinct nucleotides.
- **Feature #3:** Locate a candidate protein by finding the longest palindromic portion of a DNA sequence.
- **Feature #4:** Identify whether an unknown genome sequence is itself a protein by checking if it's a palindrome.
- **Feature #5:** Mutate a virus's nucleotide sequence into its next lexicographically greater arrangement, in place.
- **Feature #6:** Identify a species by finding the longest substring of its DNA where no nucleotide repeats.
- **Feature #7:** Detect a protein by checking whether any permutation of a nucleotide sequence could be a palindrome.
- **Feature #8:** Measure the similarity between two DNA samples as the minimum number of edits needed to turn one into the other.
- **Feature #9:** Given a species' DNA (a sorted list of the genes it has), find the `k`th gene missing from it.

Working through these features — and the equivalent interview questions they map to — will give us a solid toolkit of graph-modeling, sliding-window, palindrome, in-place-array, hashing, dynamic-programming, and binary-search techniques that show up again and again in real interviews.
