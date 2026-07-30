# Project Description for Operating System

## Introduction

A modern Operating System (OS) is a complex piece of software. It manages finite, complex hardware resources while efficiently providing services to user software — handling processing, storage, and input/output resources, plus the user shell that handles user interactions.

This chapter's scenarios come from the memory and process manager modules of an OS.

## Statement

Suppose you're a developer on a famous OS engineering team. Your team is working on several features related to scheduling processes and efficiently allocating memory to them. Another module you're working on deals with efficient encoding of files on disk.

## Features

To implement the above functionality, we need the following features:

- **Feature #1:** Determine the number of possible ways one or more running processes with contiguous memory allocation can be preempted to free up memory for a new process.
- **Feature #2:** Locate the `n`th process to resume from a list of process IDs currently in memory.
- **Feature #3:** Order the processes so that whenever a process is scheduled, all of its dependencies are already met.
- **Feature #4:** Deploy a compression strategy to identify and isolate all concatenated words.
- **Feature #5:** Recover corrupted files by removing the minimum number of unmatched start or end delimiters.
- **Feature #6:** Build a file management system to add files and perform wildcard searches.
- **Feature #7:** Serialize and deserialize the file system rooted at a specific directory to a remote machine.
- **Feature #8:** Compress a file by replacing consecutive repeating characters with their count.
- **Feature #9:** Search files in a file directory system using regular expression matching.
- **Feature #10:** Reverse engineer a message digest function to validate if a message digest is possible with a given set of operators.
- **Feature #11:** Create a directory tree iterator to iterate through every file and directory in the file system.
- **Feature #12:** Validate a potential sequence of priority updates in a multi-processing system.
- **Feature #13:** Correct the order of commands by reversing them in a log file.
- **Feature #14:** Release the process lock by finding the process that did not release it.
- **Feature #15:** Reconstruct a process queue, using a process's priority and the number of processes with a higher priority.

Understanding these feature requests and designing their solutions will help us implement the requested functionality into the operating system. Before we start, think about how you would implement these features yourself — you'll likely find some of the same underlying patterns that come up in other common coding interview questions.
