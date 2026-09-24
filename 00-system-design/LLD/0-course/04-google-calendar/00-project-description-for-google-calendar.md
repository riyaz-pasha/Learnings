# Project Description for Google Calendar

## Introduction

Google Calendar helps people manage events and reminders, and it's especially valuable for companies coordinating meetings across many people's schedules and shared rooms. This chapter is about the algorithms behind that coordination: fitting meetings into rooms, finding free/busy overlaps, and merging schedules.

## Statement

Imagine you're a developer on the Google Calendar team, building features that help individuals, pairs, and larger groups meet and collaborate more efficiently.

## Features

1. **Minimum meeting rooms** — given a schedule of meetings, determine the minimum number of rooms needed so no two overlapping meetings share a room.
2. **Busy-block display** — show the blocks of time a team lead is busy in meetings, so others can find a good time to approach them.
3. **Availability check** — before scheduling, check whether a person is free during a specific proposed time slot.
4. **Add and merge meetings** — for "open agenda" users where overlapping meetings at the same venue are allowed, add a new meeting to an already-busy schedule, merging it with existing meetings where they touch or overlap.
5. **Mutual busy intervals** — given two users' schedules, find every time interval when both are busy.
6. **Two blocks of free consecutive days** — using board members' mutual free hours per day, find two separate stretches of consecutive days suitable for a series of board meetings.
7. **Longest consecutive busy period** — given one person's schedule, find their longest unbroken busy stretch.

This chapter leans heavily on **interval** thinking — merging, intersecting, and sweeping over start/end times is the core skill, and it covers a huge share of "scheduling-flavored" interview questions.
