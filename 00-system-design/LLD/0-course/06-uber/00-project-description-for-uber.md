# Project Description for Uber

## Introduction

Uber is the largest ride-hailing company in the world. Beyond the core "get me from A to B" business, it also handles food and package delivery, and its user base of riders and drivers keeps growing every year. That growth puts constant pressure on one core engineering problem: how do you match riders to drivers quickly, fairly, and cheaply, across a city that's constantly changing?

Everything in this chapter is really about that one question, seen from different angles: finding nearby drivers, costing out a route, handling shared rides, keeping new drivers busy, and being fair about who gets picked. None of this is Uber-specific either — any dispatch, logistics, or matching system (food delivery, courier networks, ride-pooling apps) runs into the same handful of problems.

## Statement

Imagine you're a developer on the Uber engineering team. The team wants to optimize driver allocation, especially during bad weather, and also wants to add accessibility features to the client app.

Whenever a user requests a ride, the system first selects the drivers closest to the user's location. Then it plots a path from each of those drivers to the user and checks whether a driver can actually reach the pickup point. The city map is divided into checkpoints, and travel between checkpoints has a cost — one that depends on how much rainwater has pooled on the road between them. The driver with the lowest-cost path gets selected. Once the ride ends, a fare is calculated, and the accessibility plugin needs to announce it out loud. On top of this, the team wants to steer new drivers toward routes where they're likely to find their first customers, and to make sure that low-ranked drivers occasionally get a shot at rides instead of always losing out to the top-ranked ones.

## Features

Here's the list of features the team wants built:

1. **Select the closest drivers** — given a user's location and a list of driver locations, find at least the **n** nearest drivers, ignoring anyone too far away.
2. **Path cost from accumulated rainwater** — given the elevation profile of a road between two checkpoints, calculate how much water has pooled along the way, since that adds to the travel cost.
3. **Plot and select a path** — given a map of checkpoints and a list of candidate drivers, find whether each driver has a path to the user, and if so, its accumulated cost, so the cheapest one can be chosen.
4. **Fare in words** — convert the numeric ride fare into English words, for text-to-speech accessibility.
5. **Uber Pool routing** — once a driver has picked up a pool passenger, suggest a route (weighted by likelihood, not just the highest-probability one) that maximizes the chance of picking up another pool passenger along the way.
6. **Longest route for new drivers** — recommend the longest possible route through the city so a new driver is more likely to encounter a customer on their first day.
7. **Fair driver ranking** — instead of always handing rides to the single highest-ranked driver, find the *k*th highest rank so lower-ranked drivers get a fair shot too.
8. **Optimal path cost** — given a grid-shaped map with travel costs between adjacent cells, find the cheapest path from a driver's cell to the passenger's cell.

Take a moment before reading on: how would *you* implement each of these? By the end of this chapter you'll see the same handful of techniques — heaps, DFS on graphs, dynamic programming on grids and trees, prefix sums with binary search — show up again and again, both here and in classic interview questions.
