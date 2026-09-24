### Multiple Producers

```sh 
mvn compile exec:java -Dexec.mainClass="com.example.multi.Producer" -Dexec.args="test-topic-1 Producer1 Hello1"
```

```sh 
mvn compile exec:java -Dexec.mainClass="com.example.multi.Producer" -Dexec.args="test-topic-1 Producer2 Hello1"
```

```sh 
mvn compile exec:java -Dexec.mainClass="com.example.multi.Producer" -Dexec.args="test-topic-1 Producer3 Hello1"
```

---

### Multiple Consumers

#### Same consumer group

```shell
mvn compile exec:java -Dexec.mainClass="com.example.multi.Consumer" -Dexec.args="test-topic-1 group1"
```

```shell
mvn compile exec:java -Dexec.mainClass="com.example.multi.Consumer" -Dexec.args="test-topic-1 group1"
```

### Add another consumer group

```shell
mvn compile exec:java -Dexec.mainClass="com.example.multi.Consumer" -Dexec.args="test-topic-1 group2"
```
