# Project Description for Cyber Security

## Introduction

Cyber security is the application of technologies and controls to defend computers, networks, servers, and critical data from cyber-attacks. It's a large subject area that spans information security, network security, and more. Cyber-attacks have existed for decades and continue to be an evolving danger to companies, customers, and employees — they typically involve unauthorized access to information that can cause massive, irreparable damage to businesses and threaten the identities of customers and users.

The scenario and problems discussed in this chapter relate to implementing encryption-based defenses to prevent cyber-attacks.

## Statement

You are a developer on a company's engineering team that researches robots and extraterrestrial lifeforms. The company wants you to develop new and improved encryption schemes to understand alien languages and securely communicate with them. You're also tasked with building features to optimize tasks for robotic machines.

## Features

We need to introduce the following features:

- **Feature #1:** Verify the packet structure to ensure packet integrity — i.e., that packets have not been tampered with.
- **Feature #2:** Verify message integrity — i.e., that messages have not been tampered with.
- **Feature #3:** We have a set of encrypted training messages from a sender to a receiver. The receiver must reverse-engineer a dictionary describing the encryption scheme, so it can decrypt future messages from the sender.
- **Feature #4:** To uniquely decipher encrypted messages, we need to enumerate all the possible plain texts a ciphertext could decode to.
- **Feature #5:** Optimize the traffic and time taken to elect a leader from a cluster of machines.

Understanding these feature requests and designing their solutions will help us implement the requested functionality into cyber security systems. In the next few lessons, we'll walk through recommended implementations of each feature — the underlying problems they reduce to are also among the most common patterns asked in coding interviews at top-tier companies.
