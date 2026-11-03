Got it — let’s walk through **setting up a multi-node ZooKeeper ensemble on your Mac** using **Homebrew** so you can simulate a real cluster locally.

We’ll do a **3-node ensemble**, which is the smallest production-like setup.

---

## **1. Install ZooKeeper via Homebrew**

```bash
brew install zookeeper
```

This installs ZooKeeper binaries (the same as Apache's official distribution) into `/opt/homebrew/Cellar/zookeeper/...` (Apple Silicon) or `/usr/local/Cellar/zookeeper/...` (Intel).

---

## **2. Create separate data directories for each node**

We’ll have 3 ZooKeeper servers:

* Node 1 → `data1`
* Node 2 → `data2`
* Node 3 → `data3`

```bash
mkdir -p ./{data1,data2,data3}
```

---

## **3. Create config files for each node**

The default Homebrew config is at:

```
/opt/homebrew/etc/zookeeper/zoo.cfg
```

(but we’ll make **separate configs** so each node has its own `clientPort` and `dataDir`).

Create `zoo1.cfg`:

```bash
nano ./zoo1.cfg
```

```properties
tickTime=2000
dataDir=/Users/<your-username>/zookeeper-cluster/data1
clientPort=2181
initLimit=5
syncLimit=2
server.1=localhost:2888:3888
server.2=localhost:2889:3889
server.3=localhost:2890:3890
```

Create `zoo2.cfg`:

```bash
nano ./zoo2.cfg
```

```properties
tickTime=2000
dataDir=/Users/<your-username>/zookeeper-cluster/data2
clientPort=2182
initLimit=5
syncLimit=2
server.1=localhost:2888:3888
server.2=localhost:2889:3889
server.3=localhost:2890:3890
```

Create `zoo3.cfg`:

```bash
nano ./zoo3.cfg
```

```properties
tickTime=2000
dataDir=/Users/<your-username>/zookeeper-cluster/data3
clientPort=2183
initLimit=5
syncLimit=2
server.1=localhost:2888:3888
server.2=localhost:2889:3889
server.3=localhost:2890:3890
```

---

## **4. Add `myid` files for each node**

Each ZooKeeper node needs a `myid` file in its `dataDir` with the **server ID** (matching `server.X` in the config).

```bash
echo 1 > ./data1/myid
echo 2 > ./data2/myid
echo 3 > ./data3/myid
```

---

## **5. Start each ZooKeeper server**

Open 3 terminal tabs (or use `&` in one terminal):

```bash
zkServer start ./zoo1.cfg
zkServer start ./zoo2.cfg
zkServer start ./zoo3.cfg
```

---

## **6. Verify the cluster**

Connect to one node’s CLI:

```bash
zkCli -server localhost:2181
```

Run:

```bash
stat /
```

You should see one node as **leader** and others as **followers**.

---

## **7. Stop the cluster**

```bash
zkServer stop ./zoo1.cfg
zkServer stop ./zoo2.cfg
zkServer stop ./zoo3.cfg
```
