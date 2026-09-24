# Project Description for Language Compiler

## Introduction

A language compiler is software that converts the source code of a language into machine code, which the computer's processor then executes. A compiler can have several components — a scanner, a lexical analyzer, a semantic analyzer, code generation, and more — often split across different modules of the program. Compilers are both language-specific and specific to the underlying hardware processor.

This chapter's scenarios revolve around handling a language compiler's code comments and expression-computation functionality across a variety of situations.

## Statement

Imagine you're a developer at a company experimenting with a new kind of compiler for C++. This compiler doesn't follow the architecture of a conventional compiler — your team is building it from scratch, with its own modules for each part of the conversion, and pushing several optimizations for both large and small program compilations.

## Features

To build out this compiler, we'll implement the following features:

- **Feature #1:** Detect comments in the source code and remove them.
- **Feature #2:** Compute the result of a mathematical expression given as a string, in the C++ language.
- **Feature #3:** Unroll a loop by replacing it with repeated instructions.
- **Feature #4:** Optimize code by replacing slow function calls with faster ones.
- **Feature #5:** Find the first build step that failed, so we don't have to recompile every file.
- **Feature #6:** Find the most frequently used variable or function call in a piece of code.
- **Feature #7:** Provide exponentiation on devices that don't support it natively.
- **Feature #8:** Make the compiler generate adaptive code that behaves differently under different battery conditions.
- **Feature #9:** Verify the order of brackets in a complex program during compilation.

The coming lessons walk through each of these features in detail. By the end of the chapter, you'll be able to apply the same techniques to a range of interview problems.
