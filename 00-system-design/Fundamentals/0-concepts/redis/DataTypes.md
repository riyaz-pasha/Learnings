### Strings

```sh
SET color red
```

```sh
get color
```

```sh
unlink color
```
- same as `del color`
- async memory cleanup


### KeySpace
```sh
SET product:1234:color red
SET product:1234:price 12.30
```

---

### Lists

- An ordered group of elements.

```sh
LPUSH mylist lpush1
LPUSH mylist lpush2 lpush3
```

```sh
LLEN mylist
```

- Removes from left (index 0)
```sh
LPOP mylist
```

- Removes from right (index n-1)
```sh
RPOP mylist

```
```sh
RPUSH mylist rpush1
RPUSH mylist rpush2 rpush3
```

# Redis Stack & Queue Implementation

Redis **lists** can be used to implement both **stacks** (LIFO) and **queues** (FIFO) by choosing which end to push and pop from.


## 1. Stack (LIFO)
> Last In, First Out

**Approach:** Push and pop from the same end.

| Operation | Command           | Description                           |
| --------- | ----------------- | ------------------------------------- |
| Push      | `LPUSH key value` | Add element to the **left/head**      |
| Pop       | `LPOP key`        | Remove element from the **left/head** |

**Alternative (right-end stack):**
```redis
RPUSH key value  # Push to right
RPOP key         # Pop from right
```

## 2. Queue (FIFO)

> First In, First Out

**Approach:** Push on one end, pop from the other.

| Operation | Command           | Description                        |
| --------- | ----------------- | ---------------------------------- |
| Push      | `LPUSH key value` | Add element to **left/head**       |
| Pop       | `RPOP key`        | Remove element from **right/tail** |

**Alternative (push right, pop left):**

```redis
RPUSH key value  # Add to right/tail
LPOP key         # Remove from left/head
```

## 3. Blocking Queue (optional)

> Consumers wait for elements if the queue is empty.

| Command               | Description                                       |
| --------------------- | ------------------------------------------------- |
| `BLPOP key [timeout]` | Blocks until an element is available on the left  |
| `BRPOP key [timeout]` | Blocks until an element is available on the right |

## Notes

* Use **LPUSH + LPOP** for a left-side stack.
* Use **LPUSH + RPOP** (or **RPUSH + LPOP**) for a queue.
* **Blocking commands** are useful in producer-consumer scenarios.


`LRANGE mylist start<inclusive> end<inclusive>`

- returns first element
```sh
LRANGE mylist 0 0
```

- returns last element
```sh
LRANGE mylist -1 -1
```

```sh
LRANGE mylist 0 2
```

- returns entire list
```sh
LRANGE mylist 0 -1
```

`LINDEX`

```sh
LINDEX mylist 0
LINDEX mylist -1
```

---

### Sets

- Unordered collection of unique elements.

```sh
SADD myset 101
SADD myset 102 103 104
SADD myset 105 101
```

- Returns all members of a set.
```sh
SMEMBERS myset
```

- Returns the number of members in a set
```sh
SCARD myset
```

- Removes one or more members from a set. Deletes the set if the last member was removed.
```sh
SREM myset 103 104
```

---

### Hashes

- collection of key value pairs.

```sh
HSET myhash name "Riyaz"
HSET myhash name "Riyaz" age 28 city Hyderabad pincode 500085
```

```sh
HGET myhash name
HGET myhash age
HGET myhash city
HGET myhash pincode
```
-Returns the values of all fields in a hash.

```sh
HMGET myhash name age city pincode
```

```sh
HGETALL myhash
```

- Deletes one or more fields and their values from a hash. Deletes the hash if no fields remain.

```sh
HDEL myhash pincode random
```

- Increments the integer value of a field in a hash by a number. Uses 0 as initial value if the field doesn't exist.

```sh
HINCRBY myhash age 1
```

- HSETNX - Sets the value of a field in a hash only when the field doesn't exist.

- does not set this because it already exists
```sh
HSETNX myhash age 28
```

- does set this because it doesn't exists
```sh
HSETNX myhash gender male
```

---

### sorted sets

- A set where each member is associated with a score

```sh
ZADD mysortedset 98 student1
ZADD mysortedset 92 student2
ZADD mysortedset 93 student3 99 student4
```

```sh
ZSCORE mysortedset student1
```

```sh
ZRANK mysortedset student1
```

- Returns members in a sorted set within a range of indexes.
```sh
ZRANGE mysortedset 0 -1 WITHSCORES
```
- Returns members in a sorted set within a range of indexes in reverse order.
```sh
ZREVRANGE mysortedset 0 -1 WITHSCORES
```

- Returns members in a sorted set within a range of scores.
- ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]
```sh
ZRANGEBYSCORE mysortedset 92 93
ZRANGEBYSCORE mysortedset 92 93 WITHSCORES
```

- ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count]
```sh
ZREVRANGEBYSCORE mysortedset 93 92
ZREVRANGEBYSCORE mysortedset 93 92 WITHSCORES
```

---

### JSON


```sh
JSON.SET myjson $ '{"name":"Riyaz","colors":["red","green","blue"],"age":28,"loggedIn":false}'
```

```sh
JSON.GET myjson
JSON.GET myjson $
JSON.GET myjson $.name
JSON.GET myjson $.colors
JSON.GET myjson $.name $.age
JSON.GET myjson $.colors[0]
JSON.GET myjson $.colors[1,2]
```

```sh
JSON.SET myjson $.loggedIn true
```

```sh
JSON.NUMINCRBY myjson $.age 1  # age = 29 + 1 → 30
```

```sh
JSON.TOGGLE myjson $.loggedIn
```

| Command          | Description                           |
| ---------------- | ------------------------------------- |
| `JSON.ARRAPPEND` | Add element at end                    |
| `JSON.ARRINSERT` | Insert element at index               |
| `JSON.ARRPOP`    | Pop element from index (default last) |
| `JSON.ARRLEN`    | Length of array                       |
| `JSON.ARRINDEX`  | Index of value                        |

```sh
JSON.ARRAPPEND myjson $.colors '"yellow"'
JSON.ARRINSERT myjson $.colors 1 '"orange"'  # insert at index 1
JSON.ARRPOP myjson $.colors 0              # remove first element
JSON.ARRLEN myjson $.colors               # 4
JSON.ARRINDEX myjson $.colors '"green"'     # 2
```

```sh
JSON.DEL myjson $.loggedIn   # remove loggedIn field
JSON.DEL myjson .age $.colors[0]  # remove first color
```