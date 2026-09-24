# Project Description for Zoom

## Introduction

Zoom is a widely popular video-conferencing application. Beyond personal use, businesses around the world rely on it to run remote meetings, host virtual events, and more. Zoom supports video-only, audio-only, or combined meetings, all with live chat running alongside, and it lets you record sessions to watch later.

Everything in this chapter revolves around Zoom's participant management: how names are displayed in a meeting, how that data travels between server and client, and a couple of side features — a mini-game for remote team building and a display tweak for mobile devices.

## Statement

Imagine you're a developer on the Zoom team responsible for participant management.

First, you need to implement pagination for the meeting lobby's "Gallery Mode," which displays participant names in alphabetical order. Next, you need to build a serializer/deserializer that converts participant data into a transmittable form before it goes over the network, and reconstructs it on arrival. As teams navigate the challenges of working from home, they lean on remote team-building activities — Zoom wants to help by adding mini-games to meetings, starting with one pilot game. You'll also need to validate that participant data arrives intact after transmission, and add a display rotation feature so profile pictures look right when a mobile user rotates their phone.

## Features

Here's the list of features the team wants built:

1. **Display Meeting Lobby** — given a binary search tree of participant names, paginate through them ten at a time, always in alphabetical order, even as new pages are requested one after another.
2. **Serialize and Deserialize Participant Data** — convert a binary search tree of participant names into a string for network transmission, and rebuild the identical tree from that string at the other end.
3. **Meeting Activity** — a guessing mini-game: given the numbers written on a staircase's steps, find the minimum number of jumps needed to get from the first step to the last, where equal-valued steps let you teleport between them.
4. **Validate Sorted Participants Data** — given the in-order listing of participant names after they've traveled over the network, confirm the ordering wasn't corrupted along the way.
5. **Auto Rotate in Mobile Devices** — rotate a participant's profile picture 90 degrees clockwise when a phone flips from portrait to landscape or back.

Take a moment before reading on: how would *you* implement each of these? By the end of this chapter you'll see a handful of classic patterns — BST in-order traversal, tree serialization, BFS on an implicit graph, sorted-order validation, and in-place matrix rotation — show up again and again, both here and in classic interview questions.
