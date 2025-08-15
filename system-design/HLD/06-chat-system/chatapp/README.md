```shell
mvn clean package
```

```shell
mvn spring-boot:run
```

---

- Open terminal A (Alice):

```shell
wscat -c ws://localhost:8080/chat
```

```json
{
  "type": "join",
  "group": "room1",
  "from": "Alice"
}
```

```json
{
  "type": "msg",
  "group": "room1",
  "content": "Hello everyone"
}
```

---

- Open terminal B (Bob):

```shell
wscat -c ws://localhost:8080/chat
```

```json
{
  "type": "join",
  "group": "room1",
  "from": "Bob"
}
```
