
```sh
docker exec -it pg-master psql -U postgres
```

```sh
docker exec -it pg-replica psql -U postgres
```

---

```SQL
SELECT client_addr, state, sync_state FROM pg_stat_replication;
```

```SQL
\dt
```
```SQL
SELECT * FROM pg_catalog.pg_tables;
```

---

```SQL
CREATE TABLE replicated_table (id serial primary key, message text);
```

```SQL
insert into replicated_table (message) values ('First Message'), ('Second Message');
```

---
### Logical Backup using pg_dump

```SQL
docker exec -t pg-master pg_dump -U postgres -d postgres > postgres_backup.sql
```


## ✅ Step 2: Restore Logical Backup

Let’s test restoring it on the replica or a fresh PostgreSQL instance.

---

### 🛠 To restore on the replica:

```bash
cat postgres_backup.sql | docker exec -i pg-replica psql -U postgres -d postgres
```

---

### 🧪 To test restoring on a fresh container (optional):

You can spin up a new PostgreSQL container (without replication) and restore there:

```bash
docker run --name pg-test -e POSTGRES_PASSWORD=postgres -d -p 5434:5432 postgres:16
```

Then restore:

```bash
cat postgres_backup.sql | docker exec -i pg-test psql -U postgres -d postgres
```

---

## What Does This Backup Include?

* Table schema
* Data rows
* Indexes
* Constraints
* Functions and triggers

---

## Notes

* `pg_dump` is consistent, it uses MVCC snapshots so you don’t have to stop your database.
* Logical backups can be large if your data size is large.

---

## 🧱 Physical Backup Using `pg_basebackup`

### What is `pg_basebackup`?

* It creates a **binary copy** of the entire PostgreSQL data directory.
* Used for setting up replicas and full backups.
* Faster than logical dumps for large datasets.
* Can be combined with WAL archiving for Point-In-Time Recovery.

---

### ✅ Step 1: Taking a physical backup from the master

Run this command **inside your host** (assuming master container is running and accessible):

```bash
docker exec -t pg-master pg_basebackup -U postgres -D /tmp/pg_backup -Fp -Xs -P
```

Explanation:

* `-U postgres`: user
* `-D /tmp/pg_backup`: directory inside container (you can choose any path)
* `-Fp`: plain format (copy files as-is)
* `-Xs`: include WAL files required for consistency
* `-P`: show progress

---

### ✅ Step 2: Copy backup files out of container

To get the backup files on your host:

```bash
docker cp pg-master:/tmp/pg_backup ./pg_backup
```
