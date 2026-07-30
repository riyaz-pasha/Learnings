# Project Description for Facebook

## Introduction

Facebook is the biggest social media company in the world, and it also owns sister platforms like Instagram. The engineering team keeps looking for better ways to connect people **across** these platforms — so content and connections on one platform can be shared and viewed sensibly on another.

This chapter's problems are all about that cross-platform integration: syncing data between sister apps, rate-limiting shared APIs, avoiding duplicate work, and catching bad content — the same class of problems any company with multiple connected products (a suite of apps sharing a user base) eventually runs into.

## Statement

Imagine you're a developer on the Facebook engineering team, working on integration between the sister platforms. The priorities: operationally efficient code, API rate limiting, eliminating duplicate requests from the same user across platforms, and detecting objectionable content.

## Features

1. **Friend circles** — find every person in a user's friend circle on Facebook.
2. **Copy connections to Instagram** — since Instagram is a separate platform with its own database, copy a user's Facebook friend graph over so it can power Instagram's friend suggestions.
3. **Sync stories with Instagram** — keep the Facebook stories list in sync with Instagram's.
4. **Cross-platform request limiter** — the same request shouldn't be allowed again from a *different* platform until enough time has passed since the first platform's request.
5. **Detect morphed profanity** — catch disguised/misspelled versions of abusive words so posts containing them can be flagged.
6. **Group similar gibberish posts** — cluster similar-looking scrambled/obfuscated posts together so a decoding pattern can be spotted.
7. **Optimal post-to-cluster assignment** — mining a user's posts for patterns runs on a high-performance cluster; assign posts to cluster nodes so processing power is used optimally.
8. **Smallest overlapping topic sequence** — find the shortest sequence of topics a user mentioned that still overlaps with the topics their friend mentioned.
9. **Recreate the recommendation tree** — to power Instagram ad recommendations, rebuild Facebook's decision tree from its serialized preorder and inorder traversals.

As before: think through how you'd tackle each of these before reading on. The patterns here (graph traversal, sliding windows, greedy scheduling, tree reconstruction) show up constantly in interviews, independent of the "Facebook" wrapper.
