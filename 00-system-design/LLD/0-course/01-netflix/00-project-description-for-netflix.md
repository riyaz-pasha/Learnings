# Project Description for Netflix

## Introduction

Netflix is the biggest video streaming platform in the world — movies, series, documentaries, reality shows, and a catalog that keeps growing every day. With that much content, the hard problem stops being "do we have this movie?" and becomes "how do we help a user find *something worth watching* in the next 30 seconds?"

Everything in this chapter is really about that one question, wearing different hats: better search, better recommendations, better caching, better session handling. And this isn't Netflix-specific — any product that shows a large, growing catalog of content to users (an app store, a music service, a video platform) runs into the exact same set of problems.

## Statement

Imagine you're a developer on the Netflix engineering team, working on improving how users discover content — both through search and through recommendations.

## Features

Here's the list of features the team wants built:

1. **Typo-tolerant search** — users should get relevant results even with minor typos in what they type.
2. **Top movies by region** — show globally top-rated movies, given per-region rankings.
3. **Median viewer age** — track the median age of all viewers, updated efficiently every time a new user signs up.
4. **Popularity trend detection** — figure out which titles are gaining or losing popularity, to drive content distribution and recommendations.
5. **LRU cache for the client app** — evict the *least recently watched* title when the cache is full.
6. **LFU cache** — an alternative eviction strategy: evict the *least frequently watched* title instead.
7. **Browsing history with a "jump to top pick" shortcut** — let a user move back and forth through the titles they've browsed this session (smells like a stack, right?), plus a way to jump straight to their top-ranked pick from that history.
8. **Validate the history navigation** — a beta user reported that "next" and "previous" feel broken; given a recorded session, verify whether the navigation logic is actually correct.
9. **All movie combinations across genres** — given a few genres, generate every possible combination of one movie per genre (for building marathons/playlists).
10. **Median buffering events per session** — track the running median number of buffering/packet-drop events per session, to guide UX improvements.
11. **All possible viewing orders** — generate every permutation in which a fixed set of movies could be presented in a marathon.
12. **"Continue watching" bar v2** — return the most frequently watched show, to power a revamped continue-watching bar.

Take a moment before reading on: how would *you* implement each of these? Sitting with that question is where the real learning happens — by the end of this chapter you'll see the same handful of data-structure choices (tries, heaps, stacks, hash maps, backtracking) reappear again and again, both here and in classic interview questions.
