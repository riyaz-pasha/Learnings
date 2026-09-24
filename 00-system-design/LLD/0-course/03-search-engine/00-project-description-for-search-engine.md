# Project Description for Search Engine

## Introduction

A search engine lets users search the web. It crawls pages with bots, builds an index (a database of web pages), runs the user's query against that index, and returns ranked results. Google, Bing, Baidu, Yahoo!, DuckDuckGo — all of them hide complex algorithms behind that simple search box.

This chapter is about the algorithmic building blocks behind that box: efficient word storage, autocomplete, query correction, ranking, and the distributed-systems concerns (fault tolerance, load balancing) that come with running any of this at scale.

## Statement

Imagine you're building a search engine at a startup, experimenting with new algorithms for searching documents. Priorities: an efficient word index, autocomplete, a "did you mean" fallback when a query gets zero results, better ranking, better result organization, and the operational concerns of running this as a distributed system.

## Features

1. **Efficient word storage and retrieval** — a structure to store and fetch words quickly, e.g. for indexing web pages.
2. **Autocomplete** — suggest completions as the user types, based on a set of popular existing queries.
3. **Word-break check** — when a query gets zero hits, check whether inserting spaces turns it into valid dictionary words.
4. **All possible word-break results** — extend the above: return every valid way to split the query into words.
5. **Search ranking factor** — compute a page's ranking score based on the scores of pages that reference it.
6. **Reorganize search results** — rearrange results so consecutive entries never come from the same website.
7. **Per-service timing** — the search engine is built from chained/recursive services; compute how much time each individual service actually took.
8. **Distributed snapshot** — for fault tolerance, snapshot the current state of nodes in a distributed system.
9. **Optimal workload distribution** — assign a workload across servers with different capacities, optimally.

As always: think through your own approach to each before reading on — the patterns here (tries, DP over strings, backtracking, heaps, and a couple of systems-flavored puzzles) recur constantly in interviews.
