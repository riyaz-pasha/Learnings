# Transactions

- Transaction is a group of queries/tasks where all of them either pass or fail.
- A transaction is a sequence of one or more operations (like read, write, update, delete) that are executed as a single unit of work.


```sh
docker-compose up
```

```sh
psql -h localhost -p 5432 -U myuser -d mydatabase
```

or

```sh
docker exec -it my_postgres psql -U myuser -d mydatabase
```

```sh

---

```sh
docker cp create_accounts.sql my_postgres:/create_accounts.sql
```

```sh
docker exec -it my_postgres bash
```

```sh
psql -U myuser -d mydatabase -f /create_accounts.sql
```

---

- `\l` or `\list`
- `\dt`
- `\q`
  
---

### Atomicity (All or Nothing)

- Atom - smaller unit
- Atomicity guarantees that a transaction is indivisible—it either completes fully or doesn't happen at all.

**Success Scenario**

```SQL
BEGIN;

UPDATE accounts SET balance=balance+99 WHERE ID=1;
UPDATE accounts SET balance=balance-99 WHERE ID=2;

COMMIT;
```

**Failure Scenaio**

```SQL
BEGIN;
UPDATE accounts SET balance=balance+99 WHERE id=1;
UPDATE accounts SET balance=balance-99 WHERE id=999;
COMMIT;
```

- above account 1 balance doesn't get update.

---

### Isolation 

- Concurrently running transactions do not interefere with each other.
- We can decide if two or more transactions reading/modifying the same row at once, then we can decide how the data should look like.

---

### Consistency

- data should be consitent with respect to the schema and satisfy all the relations, rules etc mentioned.
- It should not modify other rows unnecessarily.


---

### Durability

- Once transaction is commited, its changes are permanent - even in case of crashes, failures.
- Changes are first written to a log on disk, and only then applied to the actual data.
- Ensures the log is written to non-volatile storage before confirming the transaction.
- So even if the server crashes, PostgreSQL replays the log during restart to restore committed changes.

| Property    | Meaning                                 |
| ----------- | --------------------------------------- |
| Atomicity   | All or nothing                          |
| Consistency | Rules must be followed                  |
| Isolation   | No interference from other transactions |
| Durability  | Committed = saved forever               |

