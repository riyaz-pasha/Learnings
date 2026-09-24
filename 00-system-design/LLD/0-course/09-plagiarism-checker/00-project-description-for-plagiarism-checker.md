# Project Description for Plagiarism Checker

## Introduction

Plagiarism means presenting someone else's work as your own. With the internet making copying trivially easy, plagiarism checkers have become essential — especially in academia, where they catch students who lift each other's code or writing and try to disguise it.

This chapter is about the "identify similar content" piece of a plagiarism checker: given someone's submission, how do we tell it apart from an honest one, even when the cheater has tried to hide the copy?

## Statement

Assume you're a developer hired by an educational institute to improve their plagiarism checker. The institute wants the ability to catch plagiarism between multiple students' code submissions.

The strategy: convert each code snippet into a string of alphabetic tokens. Two submissions can then be compared by matching their token strings against each other, to see how much content lines up. Students trying to cheat will often insert dummy statements or comments between the copied parts specifically to throw off a naive checker — so our matching has to be robust to that: it should still recognize a copy even with extra "noise" sprinkled in between the copied tokens.

Two things the institute needs:

1. Given one student's tokens, find out how many other students' submissions it could have been copied from (or copied from it).
2. Given two specific students' tokens, pin down exactly which portion was copied and altered, so a reviewer can see the evidence directly.

## Features

We'll build two features to cover these requirements:

1. **Feature #1: Possible Matches** — match a student's code tokens against the rest of the class's submissions to count how many are *possible* sources of a copy.
2. **Feature #2: Return Match** — given two students' tokens, find the exact (smallest) copied portion, even if the cheater padded it with extra characters to disguise it.

Understanding these two features and their solutions will carry over directly to some very well-known coding interview questions — the "insert junk to hide a subsequence match" trick shows up again and again once you know to look for it.
