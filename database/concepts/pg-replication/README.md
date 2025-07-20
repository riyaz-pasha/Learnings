
```sh
docker exec -it pg-master psql -U postgres
```

```sh
docker exec -it pg-replica psql -U postgres
```


```SQL
SELECT client_addr, state, sync_state FROM pg_stat_replication;
```