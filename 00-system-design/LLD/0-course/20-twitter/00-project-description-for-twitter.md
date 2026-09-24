# Project Description for Twitter

## Introduction

Twitter is a social media platform on which users post and interact with messages known as "Tweets." Twitter is also considered a microblogging service because the text of a Tweet can only be up to 280 characters. Users on Twitter can interact with each other by following each other, which lets them view each other's Tweets. They can also Retweet, comment on, or like other Tweets.

The scenario and the problems discussed in this chapter relate to Twitter's Tweet management and recommendation system.

## Statement

Assume you are a developer on Twitter's engineering team. The team has decided to implement APIs that can be exposed to partnered businesses, and to build internal modules used across the platform.

Some of the functionality relates to displaying feeds merged from multiple sources in chronological order. Other functionality relates to analytics and the recommendation system.

## Features

We need to introduce the following features to implement the functionalities discussed above:

- **Feature #1:** Create an API that calculates the total number of likes on a person's Tweets.
- **Feature #2:** Implement a module that adds a user's Tweets into an already populated Twitter feed in chronological order.
- **Feature #3:** Identify the time periods in which the maximum number of people interact with a user's Tweets.
- **Feature #4:** Check if a group of people can be split into two groups such that no one follows anyone else in the same group, so we can recommend people from the same group to each other.
- **Feature #5:** Keep track of trending hashtags by drawing a global profile of viral tweets on a particular day.
- **Feature #6:** In a given interval, find the moving average of user data to adjust the number of deployed servers.
- **Feature #7:** Given a daily log of hashtags used by people in their tweets, group and return all the people who used the same set of hashtags in one of their tweets, along with the day on which they tweeted with those hashtags.

Understanding these feature requests and designing their solutions will help us implement the requested functionality into Twitter's system.

In the next few lessons, we'll discuss the recommended implementations of these features. Before we start, we suggest that you think about how you would implement these features yourself. You may recognize some of the underlying problems that you'll need to solve. The solutions to these basic problems are also applicable to other common coding interview questions.
