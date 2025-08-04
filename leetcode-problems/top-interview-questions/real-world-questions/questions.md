Here’s **Batch 1: 10 open-ended, real-world DSA problems** in **Markdown format**, complete with:

* Vague prompt
* Clarifying questions
* Hints and approach
* Java starter code
* Difficulty & tags

---

### ✅ **Batch 1: Real-World DSA Problems**

````markdown
# Real-World DSA Modeling Problems – Batch 1

---

## 1. Movie Recommendation Engine

**Prompt:**  
Given a movie name, return the top `n` similar movies.

**Difficulty:** Medium  
**Tags:** `graph`, `bfs`, `priority-queue`, `design`

**Clarifying Questions:**
- How is similarity defined? A score or graph connection?
- Are we limited to a certain number of hops?
- Are similarity scores static or dynamic?

**Hints:**
- Graph traversal (BFS/DFS)
- Min-heap to maintain top-N
- Visited set to avoid cycles

**Approach:**
- Use BFS traversal from the given movie.
- Track top-N using a priority queue.
- Avoid revisiting using a visited set.

**Follow-ups:**
- Add a hop limit (`k`).
- Use cosine similarity from vectors.
- Build the graph from a list of movie pairs.

**Java Starter Template:**

```java
class Movie {
    String name;
    double similarityScore;
    List<Movie> similarMovies;
}

public List<Movie> getTopNSimilarMovies(Movie movie, int n) {
    // BFS + MinHeap + visited
    return new ArrayList<>();
}
````

---

## 2. Autocomplete Search System

**Prompt:**
Design a system that takes typed characters and returns the top 3 most frequently searched terms that start with the current prefix.

**Difficulty:** Medium
**Tags:** `trie`, `heap`, `design`

**Clarifying Questions:**

* Should the system learn from new inputs?
* Are terms preloaded?
* Frequency fixed or real-time?

**Hints:**

* Use Trie to store prefixes.
* Use max-heap at each node for top 3 terms.

**Approach:**

* Build a Trie with frequency maps.
* At each prefix node, maintain a heap of top 3.
* Update dynamically with new searches.

**Follow-ups:**

* Support deletion.
* Integrate with typo-tolerance.
* Paginate suggestions.

**Java Starter Template:**

```java
class AutocompleteSystem {
    public AutocompleteSystem(String[] sentences, int[] times) {
        // Build Trie with frequency
    }

    public List<String> input(char c) {
        // Return top 3 suggestions for prefix
        return new ArrayList<>();
    }
}
```

---

## 3. Trending Hashtags from Tweets

**Prompt:**
Given a stream of tweets, return top N hashtags by frequency.

**Difficulty:** Easy-Medium
**Tags:** `hashmap`, `heap`, `streaming`

**Clarifying Questions:**

* How large is the stream?
* Should results be real-time or batched?

**Hints:**

* HashMap for counting.
* MinHeap of size N for top trending.

**Approach:**

* Maintain frequency count of hashtags.
* Use heap to extract top-N.

**Follow-ups:**

* Support sliding window over time.
* Group by topics instead of exact tags.

**Java Starter Template:**

```java
public List<String> getTopNHashtags(List<String> tweets, int n) {
    // Parse hashtags, use heap
    return new ArrayList<>();
}
```

---

## 4. Word Suggestion with Typos

**Prompt:**
Suggest valid dictionary words within edit distance of 1 or 2 from the input string.

**Difficulty:** Medium
**Tags:** `trie`, `recursion`, `levenshtein`

**Clarifying Questions:**

* What's the max edit distance?
* Precomputed or dynamic dictionary?

**Hints:**

* Use Trie for dictionary.
* Recursively match with edit distance logic.

**Approach:**

* DFS in Trie with edit distance calculation.
* Track results within distance limit.

**Follow-ups:**

* Use Damerau-Levenshtein.
* Rank suggestions by frequency.

**Java Starter Template:**

```java
public List<String> getSuggestions(String input, int maxDistance) {
    // Trie + edit distance DFS
    return new ArrayList<>();
}
```

---

## 5. Social Network Friend Recommendations

**Prompt:**
Suggest new friends to a user based on mutual friends.

**Difficulty:** Medium
**Tags:** `graph`, `bfs`, `hashmap`

**Clarifying Questions:**

* How many levels deep to go?
* Do mutual friends have weights?

**Hints:**

* Count mutual friends.
* Sort by highest count.

**Approach:**

* BFS to find friends-of-friends.
* Use a map to count overlaps.

**Follow-ups:**

* Rank by mutual friend influence.
* Time-windowed friend interactions.

**Java Starter Template:**

```java
public List<String> suggestFriends(String user, Map<String, List<String>> network, int k) {
    // BFS + mutual count
    return new ArrayList<>();
}
```

---

## 6. Real-Time News Feed Merger

**Prompt:**
Merge multiple user feeds sorted by recency into one combined sorted feed.

**Difficulty:** Medium
**Tags:** `heap`, `merge-k`, `design`

**Clarifying Questions:**

* Are feeds paginated?
* How many feeds typically?

**Hints:**

* Use min/max-heap for merging.
* Track pointer in each feed.

**Approach:**

* K-way merge using heap of current heads.
* Repeat until N items collected.

**Follow-ups:**

* Support filtering or deduplication.
* Use timestamps instead of integer values.

**Java Starter Template:**

```java
public List<Post> mergeFeeds(List<List<Post>> feeds, int n) {
    // K-way merge using heap
    return new ArrayList<>();
}
```

---

## 7. LRU Cache with Expiry

**Prompt:**
Implement an LRU cache with time-based expiry for each entry.

**Difficulty:** Medium-Hard
**Tags:** `hashmap`, `linkedlist`, `design`

**Clarifying Questions:**

* Can keys expire without access?
* What’s the expected capacity?

**Hints:**

* Use doubly linked list for LRU.
* Store expiry timestamp with value.

**Approach:**

* HashMap + DLL for LRU.
* On access, check if expired.

**Follow-ups:**

* Auto-clean expired entries.
* Support TTL refresh.

**Java Starter Template:**

```java
class LRUCache {
    public LRUCache(int capacity) {
        // initialize
    }

    public int get(int key) {
        // get with expiry check
    }

    public void put(int key, int value, long ttlMillis) {
        // put with expiry
    }
}
```

---

## 8. Top Products by Category

**Prompt:**
Given a product stream, return top N products per category based on sales.

**Difficulty:** Medium
**Tags:** `hashmap`, `heap`, `group-by`, `design`

**Clarifying Questions:**

* What defines "top"? Units sold? Revenue?
* Should updates be real-time?

**Hints:**

* Map\<Category, Heap<Product>> structure.

**Approach:**

* Maintain category-wise heaps.
* Update heaps on new sales.

**Follow-ups:**

* Time-decayed scores.
* Multi-level categorization.

**Java Starter Template:**

```java
public Map<String, List<Product>> getTopProductsByCategory(List<Sale> sales, int n) {
    // Category -> MinHeap of products
    return new HashMap<>();
}
```

---

## 9. Elevator Control System

**Prompt:**
Design an elevator scheduler that efficiently processes user pickup/drop requests.

**Difficulty:** Medium-Hard
**Tags:** `queue`, `priority`, `simulation`, `design`

**Clarifying Questions:**

* Are requests prioritized? (direction, floors)
* How many elevators?

**Hints:**

* Use priority queues for direction.
* Merge up/down requests smartly.

**Approach:**

* Queue by direction.
* Track current state and merge requests accordingly.

**Follow-ups:**

* Multi-elevator coordination.
* Time-based scheduling.

**Java Starter Template:**

```java
class Elevator {
    public void addRequest(int floor) {}
    public void run() {}
}
```

---

## 10. Marketplace Buyer-Seller Match

**Prompt:**
Match buyers and sellers with best price offers in real-time.

**Difficulty:** Medium
**Tags:** `heap`, `priority-queue`, `design`

**Clarifying Questions:**

* Buy/sell types? Multiple products?
* Do orders expire?

**Hints:**

* Use MinHeap for sell, MaxHeap for buy.

**Approach:**

* Match highest buyer with lowest seller.
* Update queues as trades execute.

**Follow-ups:**

* Partial fills.
* Order book with timestamps.

**Java Starter Template:**

```java
public void processOrder(Order order) {
    // Match buy/sell using heaps
}
```

Here’s **Batch 2: Real-World DSA Problems 11–20** in the same **Markdown format**, covering more modeling-based, open-ended problems commonly asked in top-tier interviews like Google, Meta, and Netflix.

---

### ✅ **Batch 2: Problems 11–20**

````markdown
---

## 11. Job Matching System

**Prompt:**  
Match job seekers to job postings based on skill overlap and preference score.

**Difficulty:** Medium  
**Tags:** `heap`, `hashmap`, `set`, `greedy`

**Clarifying Questions:**
- How is "match" scored — skill overlap, location, title?
- Are preferences mutual (job → seeker and seeker → job)?

**Hints:**
- Model job & candidate as sets of skills.
- Use Jaccard index or score function.
- Heap for top matches.

**Approach:**
- For each candidate, compute scores with jobs.
- Use max-heap to track top matches.

**Follow-ups:**
- Add weights to skills.
- Include experience levels.

**Java Starter Template:**

```java
public List<Job> getTopJobMatches(Candidate c, List<Job> jobs, int n) {
    // Compute scores, use max-heap
    return new ArrayList<>();
}
````

---

## 12. Family Tree Ancestor Finder

**Prompt:**
Find the common ancestor between two people in a family tree.

**Difficulty:** Medium
**Tags:** `graph`, `dfs`, `lowest-common-ancestor`

**Clarifying Questions:**

* Is the tree binary or N-ary?
* Are nodes uniquely identified?

**Hints:**

* Use DFS to find paths.
* Compare paths for common ancestor.

**Approach:**

* Store path to root for both.
* Compare from root down to find LCA.

**Follow-ups:**

* Multiple roots (forests).
* Add weights (e.g., time difference between generations).

**Java Starter Template:**

```java
public String findCommonAncestor(String p1, String p2, Map<String, String> parentMap) {
    // Path comparison to find LCA
    return "";
}
```

---

## 13. Fraud Transaction Detection

**Prompt:**
Given a stream of transactions, detect potential fraud patterns (e.g., same IP, high value, fast repeat).

**Difficulty:** Hard
**Tags:** `hashmap`, `sliding-window`, `graph`, `detection`

**Clarifying Questions:**

* What defines fraud — frequency, value, origin?
* Real-time or batch?

**Hints:**

* Use sliding window on timestamp.
* Group by user/IP.

**Approach:**

* Maintain recent activity logs.
* Trigger alerts based on thresholds.

**Follow-ups:**

* Use ML scoring for fraud.
* Cross-account pattern analysis.

**Java Starter Template:**

```java
public boolean isSuspicious(Transaction tx, List<Transaction> history) {
    // Sliding window + rules
    return false;
}
```

---

## 14. Merge Contact Duplicates

**Prompt:**
Merge user contacts where email/phone overlaps — each group is one unique user.

**Difficulty:** Medium
**Tags:** `union-find`, `graph`, `hashmap`

**Clarifying Questions:**

* Can contacts share multiple fields?
* Are input fields reliable?

**Hints:**

* Union-Find to group connected contacts.
* Treat email/phone as graph edges.

**Approach:**

* Group by connected components (email or phone shared).
* Return merged sets.

**Follow-ups:**

* Handle typos.
* Add scoring for confidence in merging.

**Java Starter Template:**

```java
public List<Set<String>> mergeContacts(List<Contact> contacts) {
    // Union-find on email/phone
    return new ArrayList<>();
}
```

---

## 15. Path Cost in Map Grid with Toll Roads

**Prompt:**
Find the minimum cost to travel from (0,0) to (m,n) in a grid, where some roads have tolls.

**Difficulty:** Medium
**Tags:** `graph`, `dijkstra`, `matrix`

**Clarifying Questions:**

* Can we move diagonally?
* Are tolls fixed or dynamic?

**Hints:**

* Model grid as weighted graph.
* Use Dijkstra for shortest cost path.

**Approach:**

* PriorityQueue of cell costs.
* Track visited to avoid cycles.

**Follow-ups:**

* Add time constraints (avoid traffic).
* Dynamic toll updates.

**Java Starter Template:**

```java
public int minCostPath(int[][] grid) {
    // Dijkstra on matrix
    return 0;
}
```

---

## 16. Event Overlap Detector

**Prompt:**
Given a calendar with events, return all overlapping event pairs.

**Difficulty:** Medium
**Tags:** `sweep-line`, `interval`, `sorting`

**Clarifying Questions:**

* Are events sorted?
* Can events be nested?

**Hints:**

* Sort by start time.
* Track active events using TreeSet.

**Approach:**

* Use sweep line algorithm.
* For each event, check overlap with active set.

**Follow-ups:**

* Detect chains of overlapping events.
* Optimize for real-time scheduling.

**Java Starter Template:**

```java
public List<Pair<Event, Event>> findOverlaps(List<Event> events) {
    // Sort + sweep line
    return new ArrayList<>();
}
```

---

## 17. Predict Winner in Voting System

**Prompt:**
Design a voting system that returns the current leading candidate at any time.

**Difficulty:** Medium
**Tags:** `hashmap`, `prefix-sum`, `heap`

**Clarifying Questions:**

* Are votes timestamped?
* Real-time queries?

**Hints:**

* HashMap to count votes.
* Use TreeMap for prefix count by time.

**Approach:**

* Build time-indexed leader mapping.
* Binary search on query time.

**Follow-ups:**

* Handle tie-breakers.
* Support vote retraction.

**Java Starter Template:**

```java
public String getLeadingCandidate(int time) {
    // prefix map + binary search
    return "";
}
```

---

## 18. Playlist Shuffle System

**Prompt:**
Design a system that shuffles songs randomly but avoids repeats until all songs played.

**Difficulty:** Medium
**Tags:** `random`, `set`, `queue`, `design`

**Clarifying Questions:**

* Is order truly random?
* Reset after full loop?

**Hints:**

* Shuffle array, track index.
* Reset after finishing all.

**Approach:**

* Fisher-Yates shuffle.
* Iterator that cycles through shuffled list.

**Follow-ups:**

* Add weighting to songs.
* Add skip history.

**Java Starter Template:**

```java
public class PlaylistShuffler {
    public String nextSong() {
        // Fisher-Yates based
        return "";
    }
}
```

---

## 19. Multi-Level Comment Threading System

**Prompt:**
Given a list of comments with parent-child relationships, return a threaded tree structure.

**Difficulty:** Medium
**Tags:** `tree`, `recursion`, `map`

**Clarifying Questions:**

* Can comments have multiple replies?
* Is depth limited?

**Hints:**

* Build comment tree using ID → list of children.

**Approach:**

* Map parent ID to list of children.
* Recursively build thread.

**Follow-ups:**

* Sort replies by timestamp.
* Flatten tree to display thread.

**Java Starter Template:**

```java
public List<CommentNode> buildThread(List<Comment> comments) {
    // Parent-Child Tree Build
    return new ArrayList<>();
}
```

---

## 20. Rate Limiter for API Calls

**Prompt:**
Implement a rate limiter that allows N requests per user per minute.

**Difficulty:** Medium-Hard
**Tags:** `queue`, `timestamp`, `design`

**Clarifying Questions:**

* Per user? Global?
* Are requests in real-time?

**Hints:**

* Store timestamps in queue.
* Drop if queue size exceeds limit in time window.

**Approach:**

* For each user, queue of timestamps.
* Remove outdated timestamps on each request.

**Follow-ups:**

* Weighted limits.
* Different rules for endpoints.

**Java Starter Template:**

```java
public boolean allowRequest(String userId, long timestamp) {
    // Queue of timestamps
    return true;
}
```

---

```


Here’s **Batch 3: Real-World DSA Problems 21–30** in Markdown format — the final set in your curated simulation set.

---

### ✅ **Batch 3: Problems 21–30**

````markdown
---

## 21. Dynamic Friend Suggestion System

**Prompt:**  
Suggest top N friend recommendations based on mutual friends and profile similarity.

**Difficulty:** Medium-Hard  
**Tags:** `graph`, `priority-queue`, `social-graph`

**Clarifying Questions:**
- Are mutual friends weighted more than profile similarity?
- Real-time or batch?

**Hints:**
- Model users as graph nodes.
- Use BFS with friend-of-friend scoring.

**Approach:**
- For each user, run BFS up to 2 hops.
- Score based on overlap and similarity.
- Use a min-heap to track top N suggestions.

**Follow-ups:**
- Incorporate user activity.
- Time-decay scoring.

**Java Starter Template:**

```java
public List<User> suggestFriends(User user, int n) {
    // BFS + score
    return new ArrayList<>();
}
````

---

## 22. Auto-Correct Typo Fixer

**Prompt:**
Design a system that auto-corrects misspelled words based on edit distance and frequency.

**Difficulty:** Medium
**Tags:** `trie`, `edit-distance`, `hashmap`

**Clarifying Questions:**

* Do we have access to a dictionary corpus?
* Is frequency known?

**Hints:**

* Use Trie to optimize prefix search.
* Levenshtein distance to find similar words.

**Approach:**

* Preprocess dictionary into trie.
* For input, generate candidates and score.

**Follow-ups:**

* Context-aware corrections.
* Multi-word typo handling.

**Java Starter Template:**

```java
public String autoCorrect(String input, Trie dictionary) {
    // Edit distance + frequency
    return "";
}
```

---

## 23. Package Dependency Resolver

**Prompt:**
Given package dependencies, return a valid install order or detect cycles.

**Difficulty:** Medium
**Tags:** `graph`, `topological-sort`, `cycle-detection`

**Clarifying Questions:**

* Are all dependencies listed?
* Can circular dependencies exist?

**Hints:**

* Build graph with edges from package → dependency.
* Use DFS for cycle + topo sort.

**Approach:**

* Topological sort via Kahn’s or DFS.
* Track visited and recursion stack.

**Follow-ups:**

* Partial install if cycle detected.
* Version constraints.

**Java Starter Template:**

```java
public List<String> resolveInstallOrder(List<Package> packages) {
    // Topo sort
    return new ArrayList<>();
}
```

---

## 24. Nearby Ride Finder (Uber Style)

**Prompt:**
Given coordinates, return available drivers within K kilometers.

**Difficulty:** Medium
**Tags:** `geometry`, `grid-bucketing`, `spatial-index`

**Clarifying Questions:**

* Are driver locations dynamic?
* Earth curvature considered?

**Hints:**

* Grid-based bucketing.
* Haversine formula for distance.

**Approach:**

* Use bucketing to reduce search space.
* Calculate distance precisely for candidates.

**Follow-ups:**

* Optimize with QuadTree or KD-Tree.
* Real-time movement updates.

**Java Starter Template:**

```java
public List<Driver> findNearbyDrivers(Location loc, double radiusKm) {
    // Grid bucketing + haversine
    return new ArrayList<>();
}
```

---

## 25. Trending Hashtags System

**Prompt:**
Track and return top K trending hashtags in a live stream.

**Difficulty:** Medium
**Tags:** `heap`, `sliding-window`, `hashmap`

**Clarifying Questions:**

* Over what time window?
* Can hashtags drop from top K?

**Hints:**

* Maintain count per hashtag.
* Use min-heap of size K.

**Approach:**

* HashMap to count.
* Min-heap to track top K.

**Follow-ups:**

* Time decay or window-based popularity.
* Normalize hashtags (case, variants).

**Java Starter Template:**

```java
public List<String> getTopKHashtags(List<String> stream, int k) {
    // HashMap + minHeap
    return new ArrayList<>();
}
```

---

## 26. News Feed Ranking

**Prompt:**
Design a ranking algorithm to sort a user’s news feed by relevance.

**Difficulty:** Hard
**Tags:** `heap`, `ranking`, `multi-source-merge`, `personalization`

**Clarifying Questions:**

* What features define "relevance"?
* Static vs dynamic ranking?

**Hints:**

* PriorityQueue per user.
* Score based on recency + engagement + personalization.

**Approach:**

* Merge k sorted streams with custom comparator.

**Follow-ups:**

* Train relevance score using ML.
* Feedback loop based on clicks.

**Java Starter Template:**

```java
public List<Post> getRankedFeed(User user, List<Stream> sources) {
    // PriorityQueue merge
    return new ArrayList<>();
}
```

---

## 27. Flight Connection Path Finder

**Prompt:**
Find the cheapest flight from city A to city B with at most K stops.

**Difficulty:** Medium
**Tags:** `graph`, `bfs`, `dijkstra`, `k-limited-path`

**Clarifying Questions:**

* Can flights loop back?
* What if no path within K stops?

**Hints:**

* Use BFS with depth tracking.
* PriorityQueue for cost.

**Approach:**

* Modified Dijkstra with stop count tracking.

**Follow-ups:**

* Add layover time.
* Maximize convenience vs cost.

**Java Starter Template:**

```java
public int cheapestFlight(int n, int[][] flights, int src, int dst, int k) {
    // BFS + stops tracking
    return -1;
}
```

---

## 28. Resource Allocation with Constraints

**Prompt:**
Assign limited resources (machines, memory) to jobs with varying demands and priorities.

**Difficulty:** Hard
**Tags:** `greedy`, `interval`, `heap`, `backtracking`

**Clarifying Questions:**

* Can jobs wait or preempt?
* Are resources homogeneous?

**Hints:**

* Sort jobs by start time.
* Greedy assignment + heap of available resources.

**Approach:**

* Track active jobs.
* Allocate based on available capacity.

**Follow-ups:**

* Add fairness or SLA enforcement.
* Handle job cancellation.

**Java Starter Template:**

```java
public boolean allocateResources(List<Job> jobs, int totalResources) {
    // Greedy + heap
    return true;
}
```

---

## 29. Dynamic Tag-Based Document Search

**Prompt:**
Support fast document search by tags with boolean queries (AND, OR, NOT).

**Difficulty:** Hard
**Tags:** `inverted-index`, `set`, `query-parser`

**Clarifying Questions:**

* Support complex queries?
* Dynamic insertions?

**Hints:**

* Maintain tag → doc list.
* Evaluate query tree using sets.

**Approach:**

* Build inverted index.
* Parse and evaluate query using stack or AST.

**Follow-ups:**

* Ranking results.
* Add regex/partial tag support.

**Java Starter Template:**

```java
public Set<String> search(String query) {
    // Parse + inverted index eval
    return new HashSet<>();
}
```

---

## 30. Multi-Currency Wallet Aggregator

**Prompt:**
Given wallets in various currencies, return total net worth in a base currency.

**Difficulty:** Medium
**Tags:** `graph`, `conversion`, `DFS`, `cycles`

**Clarifying Questions:**

* Do conversion rates form a complete graph?
* Are rates symmetric?

**Hints:**

* Build graph of currency → (rate, target).
* DFS for conversion paths.

**Approach:**

* Build currency conversion graph.
* Convert all balances to base using DFS.

**Follow-ups:**

* Add exchange rate volatility.
* Detect arbitrage.

**Java Starter Template:**

```java
public double computeNetWorth(Map<String, Double> wallets, String base, Map<String, List<Rate>> rates) {
    // DFS conversion
    return 0.0;
}
```


---


## 🔁 Level 1: Constraint Variations

### 1. **Cheapest Flight with Exactly K Stops**

**Problem:**
Find the cheapest flight from `src` to `dst` with **exactly K stops** (not at most K).
**Hint:** Track the number of stops and only accept result if `stops == K`.

---

### 2. **Count All Valid Paths with At Most K Stops**

**Problem:**
Return the **number of different paths** from `src` to `dst` with total cost ≤ budget and stops ≤ K.
**Hint:** Use DFS with memoization or modified BFS to track cost and stop constraints.

---

### 3. **Cheapest Flight with Maximum Budget**

**Problem:**
Find the number of paths from `src` to `dst` such that total cost ≤ given `budget` and ≤ K stops.
**Hint:** This adds another pruning condition inside DFS/BFS.

---

## 🔁 Level 2: Graph Model & Algorithm Shifts

### 4. **Flight Prices with Negative Costs (Allow Discounts)**

**Problem:**
If flight costs can be negative (e.g., promo codes), find the cheapest route.
**Hint:** Dijkstra fails. Use **Bellman-Ford**.

---

### 5. **Multi-Source Cheapest Flights**

**Problem:**
Given a list of starting cities, find the minimum cost to reach `dst` from **any of them** within K stops.
**Hint:** Initialize your PQ with all starting cities.

---

### 6. **Minimum Time Instead of Minimum Cost**

**Problem:**
Each flight has a duration and layover time. Find the fastest route from `src` to `dst` with ≤ K stops.
**Hint:** Replace `cost` with `time` in your priority queue logic.

---

## 🔁 Level 3: Advanced Modeling

### 7. **Cheapest Multi-City Trip**

**Problem:**
Find the cheapest way to visit a given set of cities `{A, B, C, D}` starting and ending at `src`, like a traveling salesman problem with constraints.
**Hint:** Variation of **TSP** using DP + bitmasking.

---

### 8. **Cheapest Flight with Layover Constraints**

**Problem:**
You can only take the next flight if the layover between current and next flight is ≥ 1 hour and ≤ 6 hours.
**Hint:** Model each edge with `(toCity, arrivalTime, cost)` and simulate layovers.

---

### 9. **Flight Booking System with Caching**

**Problem:**
Implement a system that caches the cheapest flight from `src → dst` with at most K stops. If queried again with same parameters, return cached result.
**Hint:** Use memoization or a hashmap with composite keys.

---

### 10. **Return Actual Path (Not Just Cost)**

**Problem:**
Modify your solution to return the **full path** (list of cities) of the cheapest route.
**Hint:** Store parent pointers or path arrays in your `State`.

---

## 🧠 Bonus Challenge

### 11. **Cheapest Flight in a Dynamic Graph**

**Problem:**
The list of available flights changes over time (flights get canceled/added). Update your algorithm to handle dynamic changes efficiently.
**Hint:** Use an event-driven or observer-based architecture.

