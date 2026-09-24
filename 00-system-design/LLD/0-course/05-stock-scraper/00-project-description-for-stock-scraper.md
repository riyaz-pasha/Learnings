# Project Description for Stock Scraper

## Introduction

Extracting data from a website is known as scraping. The main piece of scraping is fetching data from the HTML DOM structure once a website responds to a request. Think of web scraping as a spider crawling over every corner of an HTML structure, looking for information of interest.

This chapter's scenario and problems are all about scraping stock data from websites.

## Statement

Imagine you're a developer on a trading company's engineering team. The company wants a program that dynamically scrapes stock data from different websites, then runs a profit analysis on what it extracts.

Since a website is really just a DOM tree of HTML tags, the first challenge is traversing that tree. Finding stock price data inside an arbitrary HTML page is harder still, because every website has a different structure, and the data you want could be anywhere on the page.

## Features

1. **Traverse the DOM tree** — develop a way to parse the DOM tree structure of different stock websites.
2. **Locate stock data** — assign a confidence score to each HTML tag, representing how likely it is to contain stock price data, then filter down to the minimal subtree that actually holds it.
3. **Traverse the DOM tree, more efficiently** — stock data sitting at the same level of the tree tends to be related, so introduce a *next* pointer on each node, linking it to the next node at the same level.
4. **Maximum profit** — pull the stock's daily percentage changes out of the DOM tree and calculate the maximum profit achievable from them.

Understanding these feature requests and designing solutions for them will help build out the scraper's real functionality. Before reading ahead, think about how you'd implement each feature yourself — the underlying problems you'll run into are also some of the most common patterns in coding interviews.
