# Project Description for Stocks

## Introduction

Stocks represent a company's or an individual's claim on shares of a public company. Their prices move constantly, driven by inflation, demand, reputation, and a dozen other factors. Trading firms exist to buy and sell these shares, and they're always looking for new traders to bring on board.

The scenario in this chapter is about that onboarding process — and how a bit of software can make it far less error-prone.

## Statement

Imagine you're a developer at a stock trading company, working on their online trading platform. The company wants you to add features to the onboarding flow for new hires.

Management has noticed that rookies frequently mess up buying and selling prices while making trades, so a validator needs to be in place to flag likely mistakes before they go through. After a trade is made, the platform also needs to enforce a settling period before the same stock can be traded again. On top of that, the company wants to track whether every rookie hits their daily minimum trading goal, and when each broker first reaches certain career trading milestones — all of which feeds into performance reviews and promotion decisions.

## Features

To build all of this, we'll implement eight features:

- **Feature #1:** Validate the price for buying and selling stocks according to the company's defined criteria.
- **Feature #2:** Stock of the same company shouldn't be traded again until its settling period is over.
- **Feature #3:** Store the trades made by every rookie and evaluate whether each of them met their minimum daily quota.
- **Feature #4:** Determine the day and week a broker reached a given trading milestone.
- **Feature #5:** Determine the top brokers based on how frequently they trade.
- **Feature #6:** N stock transactions need to be carried out by K brokers, assigned in same-arrival-order batches.
- **Feature #7:** Several transactions are logged every day in a log file; process one line and pull out the integer at its start.
- **Feature #8:** Given price predictions for a stock over a future time window, find the minimum wait before the price rises again after each interval.

Working through these features — and the equivalent interview questions they map to — will give us a solid toolkit of parsing, greedy, binary-search, heap, linked-list, and monotonic-stack techniques that show up again and again in real interviews.
