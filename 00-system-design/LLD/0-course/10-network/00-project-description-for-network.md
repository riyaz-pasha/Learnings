# Project Description for Network

## Introduction

A network is an interconnection of computers, routers, or switches spanning a geographic area, with the devices talking to each other through various protocols. Corporations use LANs to build private networks for their employees, capable of pushing data and messages to every device on the network at once. Most networks also connect out to the wider Internet, so devices on the same LAN can reach resources beyond it.

Everything in this chapter is about data propagation in a network — how fast a message reaches everyone, how far it's allowed to travel, how routers pick the shortest path, how configuration changes ripple outward, and how a device recovers gracefully when things go wrong.

## Statement

Imagine you work for a company that builds devices for LANs, high-performance compute clusters, and overlay networks. Your team writes the software for these devices — efficient communication protocols, plus the analytics that make sense of the data flowing through them.

You'll need to figure out how long a broadcast message takes to reach every client, cap how far a message can travel using a time-to-live counter, and find the fewest hops needed to relay a packet across a chain of routers. You'll disseminate updates across a grid of routers under forwarding constraints, propagate VLAN ID changes across connected switches, and detect whether a request and its response took the same path. You'll divide a large batch of files across cluster nodes to minimize cross-talk, flag the most out-of-sync routers on the network, and time how long a configuration update takes to spread through a grid. You'll also measure how consistently a customer's traffic behaves, compute how long a device backs off after a collision, and finally, place an advertising board at the sweet spot of signal strength along a stretch of access points.

## Features

Here's the list of features the team wants built:

1. **Total Time** — given the delays along each hop of a spanning tree rooted at a server, find the time at which the last client finishes receiving a broadcast message.
2. **TTL Expiry** — given an n-ary tree network, a server node, and a message's time-to-live, find every device where the message's TTL will run out.
3. **Minimum Hops** — given each router's maximum forward-hop distance in a linear chain, find the fewest transmissions needed to get a packet from the first router to the last.
4. **Maximum Routers** — given a grid of router IDs, find the longest chain of strictly-increasing-ID neighbors a packet can be forwarded through, starting anywhere in the grid.
5. **Update VLAN ID** — given a grid of switch VLAN IDs and a starting switch, propagate a new VLAN ID to every 4-directionally connected switch that shares the old ID.
6. **Transmission Error** — given a request path and its reversed response path packed into one array, decide whether at most one router diverged from the expected round-trip path.
7. **Divide Files Over the Network** — given a string of file operations, split it into the maximum number of contiguous chunks such that no chunk shares a file with another.
8. **Maximum Clock Skew** — given a tree of routers with clock-time values, find the largest time difference between any ancestor-descendant pair.
9. **Update Configuration** — given a grid where some routers already hold the new configuration and others don't, find how many minutes it takes for the update to reach every router.
10. **Minimum Variation** — given a customer's daily traffic numbers and a variation threshold, find the longest stretch of days whose traffic stayed within that threshold of itself.
11. **Weighted Exponential Back-off** — given two linked lists of digits representing time slots waited after successive collisions, add them together to get the total idle time.
12. **Peak Signal Strength** — given signal-strength readings along an expressway, find a position where the signal is a local peak — stronger than both neighbors.

Take a moment before reading on: how would *you* implement each of these? By the end of this chapter you'll see BFS and DFS on trees and grids, greedy interval covering, monotonic deques, and binary search on unsorted-looking arrays show up again and again — both here and in classic interview questions.
