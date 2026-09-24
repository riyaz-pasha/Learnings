Absolutely. We can do this as a **hands-on, story-driven SSL/TLS course**, similar to the HTTP course: start from the problem, introduce a solution, discover its weaknesses, then introduce the next piece.

We'll use your **Mac + terminal + very small local servers**, mostly with `openssl` and Python, so we can see what is actually happening rather than hiding everything behind Spring/Node/Nginx.

# SSL/TLS — Hands-on Course

The mental model we'll build is:

```text
                HTTP
                 │
                 ▼
        "I need to communicate"
                 │
                 ▼
          ┌─────────────┐
          │    TLS      │
          └─────────────┘
            │    │    │
            │    │    └── Authentication
            │    └─────── Integrity
            └──────────── Encryption

                 │
                 ▼
              HTTPS
```

And eventually we'll understand this:

```text
Browser                         Server
   │                               │
   │──── ClientHello ─────────────>│
   │                               │
   │<─── ServerHello ──────────────│
   │<─── Certificate ──────────────│
   │<─── Key Exchange ────────────>│
   │                               │
   │      🔑 shared secret         │
   │                               │
   │<════ encrypted HTTP ═════════>│
```

But **we won't start there**. First we'll understand why all of this exists.

---

# Course roadmap

We'll progress roughly like this:

### Part 1 — The problem

1. What happens with plain HTTP?
2. Network attackers
3. Eavesdropping
4. Tampering
5. Impersonation
6. The three goals of secure communication

### Part 2 — Encryption fundamentals

7. Symmetric encryption
8. Why one shared key is a problem
9. Public/private keys
10. Asymmetric encryption
11. Digital signatures
12. Hashes
13. MAC / HMAC

### Part 3 — Key exchange

14. The key-distribution problem
15. Diffie-Hellman
16. Hands-on Diffie-Hellman
17. Why modern TLS doesn't simply encrypt everything with RSA
18. Ephemeral keys
19. Forward secrecy

### Part 4 — The identity problem

20. "How do I know I'm talking to Google?"
21. Certificates
22. Certificate Authority
23. Root CA
24. Intermediate CA
25. Certificate chains
26. Certificate validation
27. Trust stores

### Part 5 — TLS itself

28. TLS handshake
29. ClientHello
30. ServerHello
31. Certificate
32. Key exchange
33. Finished messages
34. Session keys
35. Encrypted application data

### Part 6 — Hands-on

36. Create your own CA
37. Create a server certificate
38. Run a local HTTPS server
39. Connect with `curl`
40. Inspect TLS with `openssl s_client`
41. Inspect certificates
42. Create a certificate chain
43. Make your browser trust your local CA

### Part 7 — Advanced TLS

44. TLS 1.2 vs TLS 1.3
45. Cipher suites
46. AES-GCM / ChaCha20-Poly1305
47. ECDHE
48. RSA signatures
49. Certificate verification
50. SNI
51. ALPN
52. HTTP/2 + TLS
53. HTTP/3 + QUIC
54. Session resumption
55. 0-RTT
56. mTLS
57. Certificate rotation
58. TLS termination
59. Load balancers / reverse proxies
60. Production TLS architecture

We'll take these **one concept at a time**.

---

# Lesson 1 — Why do we need TLS?

Let's forget TLS for a moment.

Suppose your browser wants to access:

```text
https://bank.example
```

The application wants to send:

```http
POST /transfer HTTP/1.1
Host: bank.example
Content-Type: application/json

{
    "from": "Alice",
    "to": "Bob",
    "amount": 1000
}
```

Imagine that this is sent over the network as plain HTTP.

Conceptually:

```text
Browser
   │
   │  "Transfer ₹1000 to Bob"
   │
   ▼
 Wi-Fi
   │
   ▼
 Router
   │
   ▼
 Internet
   │
   ▼
 Bank
```

The problem is that **the network between Alice and the bank is not necessarily trustworthy**.

There could be:

```text
Browser
   │
   ▼
Attacker
   │
   ▼
Bank
```

The attacker might be sitting on:

* public Wi-Fi
* a compromised router
* a compromised network
* an ISP/network infrastructure
* some other point along the communication path

There are three fundamental problems.

---

# Problem 1 — Confidentiality

Suppose you send:

```http
username=riyaz
password=secret123
```

If HTTP sends it as plaintext:

```text
username=riyaz
password=secret123
```

someone observing the network could potentially see it.

We want:

```text
Browser
   │
   │  🔒 encrypted
   │
   ▼
Network
   │
   │  gibberish
   │
   ▼
Server
```

So the first requirement is:

> **Nobody who observes the network should be able to understand the data.**

That's **confidentiality**.

---

# Problem 2 — Integrity

Suppose you send:

```http
amount=1000
```

An attacker might try to change it:

```text
amount=1000
       ↓
amount=9000
```

So encryption alone isn't enough.

We also need:

> **The receiver must know whether the message was modified.**

That's **integrity**.

---

# Problem 3 — Authentication

Here's the really interesting one.

Suppose you connect to:

```text
bank.example
```

How do you know you're actually talking to the bank?

Imagine:

```text
Browser
   │
   │ "Give me bank.example"
   ▼
Attacker
   │
   │ pretends to be bank
   ▼
Browser
```

The attacker could say:

> "Hi, I'm bank.example."

The browser needs a way to determine:

> **Is this really the server I intended to communicate with?**

That's **authentication**.

---

# So TLS has three major jobs

| Goal               | Problem                                    |
| ------------------ | ------------------------------------------ |
| 🔒 Confidentiality | Others shouldn't read our data             |
| 🛡️ Integrity      | Others shouldn't modify our data unnoticed |
| 🪪 Authentication  | We need to know who we're talking to       |

This is the foundation of everything we're going to learn.

---

# But here's the interesting part

We now have a seemingly simple requirement:

> "Let's encrypt the communication."

Easy, right?

Not quite.

Because now we have another problem.

---

# Problem 4 — Where does the encryption key come from?

Imagine we have an encryption algorithm:

```text
encrypt(message, key)
```

We want:

```text
"Hello Bob"
       +
     🔑 KEY
       ↓
"8fA91$x..."
```

Bob receives:

```text
"8fA91$x..."
```

and does:

```text
decrypt(ciphertext, key)
```

Great.

But...

### How does Bob get the key?

Alice can't simply send:

```text
Here is the encrypted message:

8fA91$x...

And here is the key:

SECRET_KEY_123
```

because an attacker could simply read the key.

We have:

```text
Alice                         Bob

  🔑                           🔑
   \                           /
    \                         /
     \                       /
      ───── Internet ───────

             👀
          Attacker
```

If the key travels across the network in plaintext:

```text
Alice ───── KEY ─────> Bob
              ↑
           Attacker
```

we're back to the original problem.

---

# This is the key-distribution problem

And this is one of the central problems TLS solves.

We need to somehow get to:

```text
Alice                         Bob

     🔑                     🔑
      \                     /
       \                   /
        \                 /
         Internet
            │
            👀
         Attacker

Attacker should NOT learn 🔑
```

How can two parties establish a secret key when an attacker can observe their communication?

This leads us to **asymmetric cryptography** and eventually **Diffie-Hellman key exchange**.

But before we jump there, we need to understand the two major types of cryptography.

---

# Lesson 2 — Symmetric encryption

Let's start with something simple.

Alice and Bob already somehow share:

```text
🔑 SECRET = 12345
```

Alice wants to send:

```text
HELLO BOB
```

She runs:

```text
encrypt("HELLO BOB", 12345)
```

and gets something like:

```text
A8F91C72...
```

She sends:

```text
Alice
  │
  │ A8F91C72...
  ▼
Network
  │
  ▼
Bob
```

Bob has the same key:

```text
12345
```

and does:

```text
decrypt("A8F91C72...", 12345)
```

Result:

```text
HELLO BOB
```

That's **symmetric encryption**.

Same secret:

```text
          🔑
       /     \
      /       \
   Encrypt   Decrypt
      │         │
      ▼         ▼
   Message   Message
```

Examples of symmetric algorithms include:

* AES
* ChaCha20

---

# Why is symmetric encryption useful?

It's extremely fast.

Imagine a connection transferring:

```text
10 GB
```

We don't want expensive public-key operations for every byte.

Instead, TLS eventually establishes symmetric session keys and then uses those keys to protect the actual application data.

Conceptually:

```text
TLS handshake
      │
      │ establish secret
      ▼
  🔑 session key
      │
      ▼
┌───────────────────┐
│ HTTP data         │
│ HTTP data         │
│ HTTP data         │
│ HTTP data         │
└───────────────────┘
       encrypted
```

So symmetric encryption is going to be very important.

But we're still stuck with:

> **How do Alice and Bob securely establish that shared secret?**

---

# Hands-on #1 — See encryption locally

Your Mac already has OpenSSL available.

Run:

```bash
openssl version
```

You should see an OpenSSL version.

Now let's generate a random key:

```bash
openssl rand -hex 32
```

You'll get something like:

```text
a4f8...32 bytes...
```

That's **256 bits of random data**.

Think of it conceptually as:

```text
🔑
```

This is the kind of secret material cryptographic systems use.

You can generate another:

```bash
openssl rand -hex 32
```

and you'll get a completely different value.

---

# Important distinction

Don't confuse these:

```text
Password
```

with:

```text
Cryptographic key
```

A password is usually something humans choose:

```text
myPassword123
```

A cryptographic key should generally be generated using a cryptographically secure random generator:

```text
a91f83c4e...
```

TLS deals heavily with **cryptographic keys**, not human passwords.

---

# Where we're going next

We've now discovered the first chain of problems:

```text
HTTP
 │
 ├── ❌ Anyone can read data
 │       ↓
 │     Encryption
 │
 ├── ❌ Anyone may modify data
 │       ↓
 │     Integrity protection
 │
 ├── ❌ We don't know who we're talking to
 │       ↓
 │     Authentication
 │
 └── ❌ How do we establish encryption keys?
         ↓
       Key exchange
```

And that last problem leads to one of the most beautiful ideas in cryptography:

> **Two people can establish a shared secret over a public channel without directly sending the secret.**

That's **Diffie-Hellman**.

Next, we'll do **Diffie-Hellman by hand with tiny numbers first**, then use OpenSSL to see the real cryptographic version. After that we'll connect it to how TLS actually creates its session keys.

---

# Lesson 3 — The Key Exchange Problem → Diffie-Hellman

We ended with the biggest question:

> **How can two computers create the same secret key when an attacker can see everything they exchange?**

Let's solve that problem.

---

## 1. The naive approach doesn't work

Alice and Bob want to communicate securely.

They need:

```text
Alice 🔑 ───────── 🔑 Bob
```

Suppose Alice generates:

```text
SECRET = 123456
```

and sends it:

```text
Alice ──── 123456 ────> Bob
                 👀
              Attacker
```

The attacker now knows the secret.

So we need something clever.

---

# 2. Diffie-Hellman: the idea

Diffie-Hellman allows:

```text
Alice                         Bob
  │                             │
  │                             │
  │──── public information ────>│
  │<─── public information ─────│
  │                             │
  │                             │
  └────── 🔑 same secret ───────┘
```

The important part:

```text
Attacker sees the communication
            ↓
       👀 👀 👀
            ↓
But cannot practically calculate
the shared secret
```

**The secret itself is never transmitted.**

That's the key idea.

---

# 3. Let's use tiny numbers

Real Diffie-Hellman uses enormous numbers and sophisticated mathematics.

We'll use tiny numbers so we can understand the mechanism.

Alice and Bob publicly agree on two numbers:

```text
p = 23
g = 5
```

These aren't secrets.

Everyone can know them.

```text
             PUBLIC
          p = 23
          g = 5

Alice 👩                 Bob 👨
        \               /
         \             /
          👀 Attacker
```

---

# 4. Alice chooses a private number

Alice secretly chooses:

```text
a = 6
```

She does **not** send `6`.

```text
Alice

private:
a = 6 🔒
```

She calculates:

```text
A = g^a mod p
```

So:

```text
A = 5^6 mod 23
```

That gives:

```text
A = 8
```

Alice sends:

```text
8
```

to Bob.

---

# 5. Bob does the same thing

Bob secretly chooses:

```text
b = 15
```

Again:

```text
b = 15
```

is private.

Bob calculates:

```text
B = g^b mod p
```

So:

```text
B = 5^15 mod 23
```

which gives:

```text
B = 19
```

Bob sends:

```text
19
```

to Alice.

---

# 6. Look at what the attacker sees

The attacker can see:

```text
p = 23
g = 5

Alice → 8

Bob → 19
```

So the attacker knows:

```text
23
5
8
19
```

But does **not** know:

```text
Alice's private a = 6
Bob's private b = 15
```

---

# 7. Now Alice calculates the shared secret

Alice received:

```text
B = 19
```

She has her private value:

```text
a = 6
```

She calculates:

```text
secret = B^a mod p
```

Therefore:

```text
secret = 19^6 mod 23
```

Result:

```text
secret = 2
```

---

# 8. Bob calculates the shared secret

Bob received:

```text
A = 8
```

He has his private value:

```text
b = 15
```

He calculates:

```text
secret = A^b mod p
```

Therefore:

```text
secret = 8^15 mod 23
```

Result:

```text
secret = 2
```

Both have:

```text
🔑 2
```

without ever sending `2`.

---

# 9. The magic

The mathematics gives us:

```text
(B^a) mod p
=
(A^b) mod p
```

because:

```text
B = g^b
A = g^a
```

Therefore:

```text
B^a
= (g^b)^a
= g^(ab)

A^b
= (g^a)^b
= g^(ab)
```

So both sides arrive at the same result.

---

# 10. What does the attacker need to do?

The attacker sees:

```text
g = 5
p = 23
A = 8
B = 19
```

They want:

```text
🔑 shared secret
```

They would need to recover something like:

```text
a from:

A = g^a mod p
```

or:

```text
b from:

B = g^b mod p
```

This is related to the **discrete logarithm problem**.

With our tiny numbers:

```text
5^6 mod 23 = 8
```

an attacker could simply try:

```text
5^1 mod 23
5^2 mod 23
5^3 mod 23
...
```

and discover:

```text
a = 6
```

So our example is obviously insecure.

Real cryptography uses parameters large enough that this computational problem becomes infeasible.

---

# 11. The important distinction

Notice something fascinating.

Diffie-Hellman doesn't actually encrypt our HTTP request.

It does this:

```text
Alice
  │
  │ DH exchange
  ▼
🔑 shared secret
```

Then we can use that secret to derive symmetric encryption keys:

```text
🔑 shared secret
       │
       ▼
   key derivation
       │
       ├── encryption key
       ├── authentication key
       └── other TLS secrets
```

Then:

```text
HTTP
 │
 ▼
Symmetric encryption
 │
 ▼
Encrypted bytes
```

So TLS combines different cryptographic techniques.

---

# 12. This gives us an important architecture

Eventually we'll have:

```text
             TLS HANDSHAKE
                   │
                   ▼
        Establish shared secrets
                   │
                   ▼
              🔑 Session Keys
                   │
                   ▼
       ┌─────────────────────┐
       │   TLS Record Layer  │
       └─────────────────────┘
                   │
                   ▼
            Encrypted HTTP
```

This is why you shouldn't think:

> "HTTPS = RSA encryption."

Modern TLS is much more interesting than that.

---

# 13. But we've discovered another problem 😈

Suppose an attacker sits between Alice and Bob.

Instead of simply observing:

```text
Alice ─────────────── Bob
          👀
       Attacker
```

the attacker actively interferes:

```text
Alice
  │
  ▼
Attacker
  │
  ▼
Bob
```

Alice thinks she's establishing a secret with Bob.

But perhaps she's actually establishing:

```text
Alice 🔑A Attacker 🔑B Bob
```

Let's see how.

Alice sends:

```text
Alice → A → Attacker
```

The attacker intercepts it.

The attacker sends **their own** DH value to Bob:

```text
Attacker → X → Bob
```

Bob responds:

```text
Bob → B → Attacker
```

The attacker sends another value to Alice:

```text
Attacker → Y → Alice
```

Now we have:

```text
Alice
  │
  │ 🔑 secret #1
  ▼
Attacker
  │
  │ 🔑 secret #2
  ▼
Bob
```

Alice thinks:

```text
🔑
```

is shared with Bob.

Bob thinks:

```text
🔑
```

is shared with Alice.

But the attacker sits in the middle and can potentially decrypt and re-encrypt traffic.

This is the **Man-in-the-Middle (MITM) problem**.

---

# 14. Diffie-Hellman solved one problem but created another

This is an important learning pattern.

### Before DH

Problem:

> How do Alice and Bob establish a shared secret?

DH:

> They can establish one without sending the secret.

But now:

> How does Alice know that the DH exchange is actually with Bob?

So:

```text
Key exchange
     │
     ▼
Solved ✅
     │
     ▼
Authentication
     │
     ▼
Still a problem ❌
```

And **this is where certificates and Certificate Authorities enter the story.**

---

# Hands-on #2 — Let's see DH with OpenSSL

First check OpenSSL:

```bash
openssl version
```

Now generate DH parameters:

```bash
openssl genpkey \
  -genparam \
  -algorithm DH \
  -out dhparams.pem
```

You can inspect them:

```bash
openssl pkeyparam \
  -in dhparams.pem \
  -text
```

Depending on your OpenSSL version, the output will show the DH parameters.

Now generate Alice's private/public key pair:

```bash
openssl genpkey \
  -paramfile dhparams.pem \
  -out alice-private.pem
```

Extract Alice's public key:

```bash
openssl pkey \
  -in alice-private.pem \
  -pubout \
  -out alice-public.pem
```

Bob:

```bash
openssl genpkey \
  -paramfile dhparams.pem \
  -out bob-private.pem
```

and:

```bash
openssl pkey \
  -in bob-private.pem \
  -pubout \
  -out bob-public.pem
```

Conceptually:

```text
Alice

alice-private.pem 🔒
alice-public.pem  📢
```

and:

```text
Bob

bob-private.pem 🔒
bob-public.pem  📢
```

The private keys must remain private.

---

# One terminology improvement

You'll see several terms:

```text
Private key
Public key
Secret
Session key
Shared secret
```

They aren't interchangeable.

A simplified mental model for now:

```text
Private key
     │
     │ cryptographic operation
     ▼
Public key

DH private value
     +
Other party's public value
     ↓
Shared secret
     ↓
Key derivation
     ↓
Session keys
```

We'll make these distinctions much sharper when we reach TLS 1.3.

---

# Where we are now

Our story has evolved:

```text
HTTP
 │
 ├── Anyone can read
 │       ↓
 │    Encryption
 │
 ├── Anyone can modify
 │       ↓
 │    Integrity
 │
 ├── Don't know who we're talking to
 │       ↓
 │    Authentication
 │
 └── How establish encryption key?
         │
         ▼
    Diffie-Hellman
         │
         ▼
   Shared secret ✅
         │
         ▼
   BUT...
         │
         ▼
   MITM attack ❌
```

So the next question is the crucial one:

> **How can Alice prove that a public key really belongs to Bob?**

That question leads directly to:

**public/private keys → digital signatures → certificates → Certificate Authorities → certificate chains.**

---

# Lesson 4 — Public/Private Keys & Digital Signatures

We have reached the **authentication problem**.

Diffie-Hellman lets two parties establish a shared secret, but it doesn't automatically tell Alice **who she's talking to**.

So let's step back and understand another cryptographic building block:

> **Public-key cryptography.**

---

# 1. Two different kinds of keys

Imagine Bob has two keys:

```text
                 Bob
                  │
          ┌───────┴────────┐
          │                │
     🔒 Private key     📢 Public key
       secret              share
```

The important rule is:

> **Private key stays private. Public key can be distributed freely.**

For example:

```text
Bob's private key
        🔒
        │
        └── only Bob has it

Bob's public key
        📢
        │
        ├── Alice can have it
        ├── Server can have it
        ├── Browser can have it
        └── Anyone can have it
```

---

# 2. What can we do with these keys?

There are two major concepts we need to distinguish.

### Encryption

Someone uses Bob's **public key** to protect something intended for Bob.

```text
Alice
  │
  │ Bob's public key
  ▼
🔒 Encrypt
  │
  ▼
Ciphertext
  │
  ▼
Bob
  │
  │ Bob's private key
  ▼
Decrypt
```

The private key is required to recover the protected information.

---

### Digital signatures

This works in the opposite conceptual direction.

Bob uses his **private key** to create a signature.

```text
Bob
 │
 │ private key 🔒
 ▼
Sign
 │
 ▼
Digital signature
 │
 ▼
Alice
```

Alice uses Bob's **public key** to verify it:

```text
Signature
    +
Bob's public key
    ↓
Verify
    ↓
✅ Valid
```

This is extremely important for TLS.

---

# 3. Why are digital signatures useful?

Suppose Bob says:

> "I own this public key."

Anyone can create a public/private key pair.

So merely receiving:

```text
Bob's public key
```

doesn't prove that it's actually Bob's key.

But suppose Bob can produce a signature that only someone possessing Bob's private key could produce.

Now Alice can check:

```text
                 Bob
                  │
           private key 🔒
                  │
                Sign
                  │
                  ▼
            Signature
                  │
                  ▼
                Alice
                  │
           Bob public key
                  │
                Verify
                  │
                  ▼
                  ✅
```

The signature provides evidence that the holder of the corresponding private key authorized the signed data.

---

# 4. Don't think of a signature as an encrypted message

This is a common beginner mistake.

You might hear:

> "A digital signature is encrypting with the private key."

That's a useful historical simplification, but **don't use it as your mental model for modern cryptography**.

Think:

```text
Encryption
──────────
Protect secrecy

Digital signature
─────────────────
Prove authenticity/integrity
```

They solve different problems.

---

# 5. What exactly gets signed?

Suppose Bob wants to sign:

```text
Hello Alice
```

A simplified conceptual implementation is:

```text
message
   │
   ▼
Hash
   │
   ▼
digest
   │
   ▼
Sign with private key
   │
   ▼
signature
```

So:

```text
message
   ↓
SHA-256
   ↓
digest
   ↓
private-key signature
```

Alice receives:

```text
message
signature
```

She can calculate the hash herself and verify the signature using Bob's public key.

---

# 6. Why hash the message first?

Imagine signing a 500 MB file directly.

That's unnecessary.

Instead:

```text
500 MB document
       │
       ▼
     SHA-256
       │
       ▼
  32-byte digest
       │
       ▼
    signature
```

The digest is a compact representation of the content.

If the message changes:

```text
Hello Alice
```

to:

```text
Hello Bob
```

the hash changes dramatically.

For example, conceptually:

```text
SHA256("Hello Alice")
       ↓
ABC123...

SHA256("Hello Bob")
       ↓
91F827...
```

Therefore the signature verification fails.

This gives us **integrity** as well as authentication of the signer.

---

# 7. Hands-on: SHA-256

Let's actually see this.

Run:

```bash
echo -n "Hello Alice" | shasum -a 256
```

You'll get a hash.

Now:

```bash
echo -n "Hello Bob" | shasum -a 256
```

Different input:

```text
Hello Alice
     ↓
hash A

Hello Bob
     ↓
hash B
```

Now change only one character:

```bash
echo -n "Hello AlicE" | shasum -a 256
```

You'll get another completely different digest.

That's the **avalanche effect** you'll frequently hear about with cryptographic hashes.

---

# 8. Generate a real key pair

Let's create an RSA key pair for learning purposes.

```bash
openssl genpkey \
  -algorithm RSA \
  -out bob-private.pem \
  -pkeyopt rsa_keygen_bits:2048
```

This creates:

```text
bob-private.pem
```

This file contains Bob's private key.

Treat it as secret.

Now extract the public key:

```bash
openssl pkey \
  -in bob-private.pem \
  -pubout \
  -out bob-public.pem
```

Now:

```text
bob-private.pem 🔒
bob-public.pem  📢
```

---

# 9. Look at the private key

Run:

```bash
openssl pkey \
  -in bob-private.pem \
  -text \
  -noout
```

You'll see a large amount of mathematical information.

Don't worry about understanding every number yet.

The important thing is:

```text
Private key
    │
    ├── secret mathematical parameters
    │
    └── must be protected
```

---

# 10. Look at the public key

Run:

```bash
openssl pkey \
  -pubin \
  -in bob-public.pem \
  -text \
  -noout
```

You'll see the public mathematical parameters.

The distinction is:

```text
Private key
───────────
Secret

Public key
──────────
Not secret
```

---

# 11. Create a message

```bash
echo -n "Hello Alice" > message.txt
```

Now Bob signs it:

```bash
openssl dgst \
  -sha256 \
  -sign bob-private.pem \
  -out signature.bin \
  message.txt
```

We now have:

```text
message.txt
signature.bin
bob-public.pem
```

---

# 12. Verify the signature

Alice receives:

```text
message.txt
signature.bin
bob-public.pem
```

She runs:

```bash
openssl dgst \
  -sha256 \
  -verify bob-public.pem \
  -signature signature.bin \
  message.txt
```

You should see:

```text
Verified OK
```

🎉

You've just performed a real digital signature operation.

---

# 13. Now attack the message

Change the message:

```bash
echo -n "Hello Bob" > message.txt
```

Run verification again:

```bash
openssl dgst \
  -sha256 \
  -verify bob-public.pem \
  -signature signature.bin \
  message.txt
```

It should fail.

Why?

Because Bob signed:

```text
Hello Alice
```

but Alice is now verifying:

```text
Hello Bob
```

The hash changed.

Therefore:

```text
signature
   ↓
doesn't match
   ↓
❌ verification failure
```

This is one of the most important properties we'll use later.

---

# 14. Now we have an authentication building block

We can now say:

```text
Bob
 │
 │ private key 🔒
 ▼
Digital Signature
 │
 ▼
Alice
 │
 │ Bob's public key
 ▼
Verify
```

But...

There's still a gigantic problem.

---

# 15. Where did Alice get Bob's public key?

Suppose Bob sends:

```text
Here is my public key:

bob-public.pem
```

Alice receives it.

But an attacker could intercept it:

```text
Bob
 │
 │ Bob's real public key
 ▼
Attacker
 │
 │ Attacker's public key
 ▼
Alice
```

Alice thinks:

```text
"This is Bob's public key."
```

But it's actually:

```text
Attacker's public key
```

The attacker can now create signatures with **their own private key**.

So we're back to the MITM problem.

---

# 16. We need a trusted third party

We need someone Alice already trusts to say:

> "Yes, this public key belongs to Bob."

Conceptually:

```text
             Trusted Authority
                    │
                    │
              "This key belongs
                 to Bob"
                    │
                    ▼
Bob ──────────────── Certificate
                    │
                    ▼
                   Alice
```

This trusted authority is called a:

# Certificate Authority (CA)

And this is where **certificates** enter the story.

---

# 17. A certificate is more than a public key

A simplified certificate might contain:

```text
┌───────────────────────────────┐
│ Certificate                   │
├───────────────────────────────┤
│ Subject: bank.example         │
│                               │
│ Public Key:                   │
│     ABCDEFG...                │
│                               │
│ Valid From: ...               │
│ Valid Until: ...              │
│                               │
│ Issuer: Example Intermediate  │
│                               │
│ Signature:                    │
│     XYZ123...                 │
└───────────────────────────────┘
```

The important idea is:

```text
Certificate
     │
     ├── identity information
     ├── public key
     ├── validity period
     ├── issuer
     └── CA signature
```

The CA signs the certificate.

---

# 18. The trust chain

Eventually your browser will do something conceptually like:

```text
bank.example
     │
     │ certificate
     ▼
Intermediate CA
     │
     │ certificate
     ▼
Root CA
     │
     ▼
Browser's trust store
```

The browser already has a collection of trusted root certificates.

Therefore:

```text
Browser
   │
   │ "I trust this Root CA."
   ▼
Root CA
   │
   │ signed Intermediate CA
   ▼
Intermediate CA
   │
   │ signed bank.example
   ▼
bank.example
```

This is the **chain of trust**.

We'll spend a full lesson on this because it is one of the most important parts of TLS.

---

# 19. Our story so far

Look at how each problem introduced the next concept:

```text
Plain HTTP
    │
    ├── Anyone can read data
    │       ↓
    │    Encryption
    │
    ├── Data can be modified
    │       ↓
    │    Integrity protection
    │
    ├── How do we establish a secret?
    │       ↓
    │    Diffie-Hellman
    │
    ├── How do we know who we're talking to?
    │       ↓
    │    Public/private keys
    │    Digital signatures
    │
    └── How do we know a public key belongs
        to the claimed server?
            ↓
        Certificates
            ↓
        Certificate Authorities
```

And we're getting very close to TLS.

---

# One important correction to keep in mind

You may now be tempted to imagine HTTPS as:

```text
Certificate
   ↓
Encrypt everything using certificate public key
   ↓
HTTP
```

**That's not how modern TLS works.**

Instead, we're heading toward something closer to:

```text
             TLS Handshake
                  │
       ┌──────────┴──────────┐
       │                     │
Authentication          Key Exchange
       │                     │
Certificate             ECDHE/DH
       │                     │
       └──────────┬──────────┘
                  ▼
            Shared secrets
                  │
                  ▼
            Key derivation
                  │
                  ▼
           Symmetric keys
                  │
                  ▼
        Encrypted HTTP data
```

That's the architecture we'll eventually reproduce ourselves.

---

## Next: Lesson 5 — Certificates & Certificate Authorities

We'll build a **real certificate hierarchy on your Mac**:

```text
                    Root CA
                       │
                       │ signs
                       ▼
                Intermediate CA
                       │
                       │ signs
                       ▼
                localhost cert
                       │
                       ▼
                HTTPS server
                       │
                       ▼
                    curl
```

You'll generate the CA, generate a server certificate, inspect it with OpenSSL, and see exactly **what the browser is trusting and why**.

---

# Lesson 5 — Certificates & Certificate Authorities

We now have a key-exchange mechanism and digital signatures, but there's still one critical question:

> **How does my browser know that a public key actually belongs to `google.com`?**

This is the problem that **certificates + Certificate Authorities (CAs)** solve.

---

# 1. The problem with simply sending a public key

Imagine you visit:

```text
https://bank.example
```

The server sends:

```text
Public Key:
ABC123...
```

Your browser can't simply say:

> "Great, this must be the bank's key."

An attacker could do:

```text
Real Bank                  Attacker
    │                          │
    │ real public key          │
    └──────────┐               │
               ▼               │
          Attacker intercepts
               │
               │ fake public key
               ▼
             Browser
```

The browser now has:

```text
❌ Attacker's public key
```

So we need a trusted mechanism for binding:

```text
Domain name
     +
Public key
     ↓
"This key belongs to this domain"
```

---

# 2. Enter the Certificate

A TLS certificate is essentially a **signed statement about a public key and its identity**.

Conceptually:

```text
┌─────────────────────────────────┐
│         Certificate             │
├─────────────────────────────────┤
│ Subject / SAN: bank.example     │
│                                 │
│ Public Key:                     │
│     ABC123...                   │
│                                 │
│ Valid From:                     │
│     2026-01-01                  │
│                                 │
│ Valid Until:                    │
│     2027-01-01                  │
│                                 │
│ Issuer:                         │
│     Example Intermediate CA     │
│                                 │
│ CA Signature:                   │
│     XYZ987...                   │
└─────────────────────────────────┘
```

The important part is:

```text
Certificate
     │
     ├── "This certificate is for bank.example"
     │
     ├── "Here is the public key"
     │
     ├── "It is valid during this period"
     │
     └── "I, the CA, signed this information"
```

---

# 3. Who signs the certificate?

A **Certificate Authority**.

For example, conceptually:

```text
                 CA
                 │
                 │ signs
                 ▼
       ┌─────────────────┐
       │ bank.example    │
       │                 │
       │ Public Key      │
       │ ABC123...       │
       └─────────────────┘
```

The CA uses its **private key** to sign the certificate.

That means the browser can later use the CA's public key to verify the signature.

---

# 4. But why does the browser trust the CA?

This is the really important question.

Imagine your browser has:

```text
Trusted Root CAs
────────────────────

CA #1
CA #2
CA #3
CA #4
...
```

These trusted root certificates are installed in the operating system/browser trust infrastructure.

So when a server gives you:

```text
bank.example certificate
```

your browser can trace:

```text
bank.example
     │
     │ signed by
     ▼
Intermediate CA
     │
     │ signed by
     ▼
Root CA
     │
     │ trusted by
     ▼
Browser / OS
```

This is called a:

# Certificate Chain

---

# 5. Why do we need intermediate CAs?

You might wonder:

> Why not have every website certificate directly signed by a root CA?

You could conceptually do that, but it would be undesirable operationally.

Instead, the hierarchy usually looks like:

```text
                    Root CA
                       │
              ┌────────┴────────┐
              │                 │
       Intermediate A     Intermediate B
              │                 │
          websites          websites
```

The root CA is kept highly protected, often offline or in tightly controlled infrastructure.

Intermediate CAs handle day-to-day certificate issuance.

---

# 6. Think of it like a chain of authority

Imagine your university gives you a certificate:

```text
University
    │
    │ certifies
    ▼
Professor
    │
    │ certifies
    ▼
Student
```

You don't personally know the student.

But you trust:

```text
University
```

and therefore you can verify the chain.

TLS works with a similar trust model:

```text
Root CA
  ↓
Intermediate CA
  ↓
Website certificate
```

---

# 7. Let's inspect a real certificate

Now let's do something useful.

Run:

```bash
openssl s_client -connect google.com:443 -servername google.com
```

You'll get a lot of output.

Near the beginning, you'll see something resembling:

```text
Certificate chain
 0 s:...
   i:...
 1 s:...
   i:...
```

And eventually:

```text
SSL-Session:
...
```

You can quit with:

```text
Ctrl+C
```

---

# 8. What did we just do?

We manually performed something similar to what a TLS client does.

We said:

```text
openssl
    │
    │ "I want TLS"
    │
    ▼
google.com:443
```

The server responded with its certificate chain.

Conceptually:

```text
OpenSSL                         Google
   │                               │
   │──── TLS ClientHello ─────────>│
   │                               │
   │<──── ServerHello ─────────────│
   │<──── Certificate ─────────────│
   │                               │
   ...
```

We'll dissect the handshake later.

For now we're interested only in the certificate.

---

# 9. Extract the certificate

Try:

```bash
openssl s_client \
  -connect google.com:443 \
  -servername google.com \
  </dev/null 2>/dev/null \
  | openssl x509 -noout -text
```

This extracts the server's leaf certificate and prints its contents.

You'll see sections such as:

```text
Certificate:
    Data:
        Version: 3
        Serial Number: ...
        Signature Algorithm: ...
        Issuer: ...
        Validity:
            Not Before: ...
            Not After : ...
        Subject: ...
        Subject Public Key Info:
        X509v3 extensions:
            X509v3 Subject Alternative Name:
                DNS:...
```

Don't worry about all of it yet.

Let's understand the important fields.

---

# 10. Subject

Historically, certificates have a `Subject` field.

For example:

```text
Subject:
    CN=example.com
```

But don't rely on `CN` as the modern hostname check.

The important field today is:

```text
Subject Alternative Name
```

or:

```text
SAN
```

You might see:

```text
X509v3 Subject Alternative Name:
    DNS:example.com
    DNS:www.example.com
```

This tells the client which DNS names the certificate is valid for.

---

# 11. Why SAN matters

Suppose the certificate says:

```text
SAN:
    DNS:bank.example
```

Then the client can use it for:

```text
https://bank.example
```

But not automatically:

```text
https://evil.example
```

The client checks that the hostname it's connecting to matches an appropriate SAN entry.

So:

```text
Requested hostname
       │
       ▼
bank.example
       │
       │ compare
       ▼
Certificate SAN
       │
       ▼
bank.example
       │
       ▼
✅ hostname matches
```

---

# 12. Issuer

You'll also see:

```text
Issuer:
    ...
```

This tells us who issued/signed the certificate.

Conceptually:

```text
Subject:
    bank.example

Issuer:
    Example Intermediate CA
```

Meaning:

> "This certificate was issued by Example Intermediate CA."

---

# 13. Validity

You'll see:

```text
Validity
    Not Before: ...
    Not After : ...
```

For example:

```text
Not Before: Jan 1 00:00:00 2026
Not After : Apr 1 00:00:00 2026
```

The client checks that the current time falls within the certificate's validity period.

So:

```text
Current time
     │
     ▼
┌──────────────────────────┐
│ Not Before → Not After   │
└──────────────────────────┘
             │
             ▼
            ✅
```

If expired:

```text
❌ Certificate expired
```

---

# 14. Public Key

The certificate contains the server's public key.

You might see:

```text
Subject Public Key Info:
    Public Key Algorithm: id-ecPublicKey
    ...
```

That gives the client the public key associated with the certificate.

Remember our earlier discussion:

```text
Private key 🔒
     │
     │ belongs to
     ▼
Public key 📢
```

The certificate contains the **public** key.

The server keeps the corresponding private key secret.

---

# 15. The CA signature

The certificate is signed.

Conceptually:

```text
Certificate contents
        │
        ▼
       Hash
        │
        ▼
   CA private key
        │
        ▼
   CA signature
```

The browser can verify:

```text
Certificate
    +
CA public key
    ↓
Verify signature
    ↓
✅
```

If somebody modifies the certificate:

```text
bank.example
     ↓
evil.example
```

the signature no longer matches.

```text
❌ Invalid signature
```

---

# 16. But there's one more important detail

Suppose I create my own CA:

```text
Riyaz's CA
```

I generate:

```text
Riyaz's CA private key
Riyaz's CA public key
```

Then I create:

```text
evil.example
```

certificate and sign it with my CA.

The certificate signature could be perfectly valid.

So why shouldn't your browser trust it?

Because:

```text
Your browser
     │
     │ trusts
     ▼
Known Root CAs
```

It does **not** automatically trust:

```text
Riyaz's CA
```

This leads to the key distinction:

> **A valid signature is not the same thing as a trusted signature.**

The client needs both.

---

# 17. Valid vs trusted

Imagine:

```text
Certificate
    │
    ├── Signature mathematically valid? ──> YES
    │
    └── Issuer trusted by client? ────────> YES
```

Then we're getting somewhere.

But TLS hostname verification also needs:

```text
Certificate
    │
    ├── Signature valid
    ├── Chain trusted
    ├── Not expired
    ├── Hostname matches
    └── Other constraints valid
             │
             ▼
             ✅ Trusted
```

We'll examine each of these checks later.

---

# 18. Where are trusted certificates stored?

This is an important practical question you asked earlier.

Your computer has a **trust store** containing trusted CA certificates.

On macOS, certificates are managed through the **Keychain** system.

You can inspect certificates through:

```text
Keychain Access
```

You can also use the command line:

```bash
security find-certificate -a
```

There are also browser/application-specific trust configurations in some environments.

So the mental model is:

```text
                    macOS
                     │
               ┌─────┴─────┐
               │ Keychain  │
               └─────┬─────┘
                     │
              Trusted roots
                     │
                     ▼
                   curl
                   apps
                TLS clients
```

The exact trust behavior can vary by application, so don't assume every application uses precisely the same store.

---

# 19. Where does a website's private key live?

This is another important operational concept.

A server might have:

```text
/etc/.../server.key
```

or some equivalent location.

Conceptually:

```text
Server
 │
 ├── certificate.pem
 │      📢 public
 │
 └── private-key.pem
        🔒 secret
```

The private key must be protected.

If an attacker steals the private key, the consequences can be severe because they may be able to impersonate the server in situations where the certificate is still accepted.

Modern TLS deployments therefore put significant effort into:

* filesystem permissions
* secret managers
* HSMs
* key rotation
* restricted access
* avoiding unnecessary private-key exposure

We'll cover this in the production section.

---

# 20. Certificate vs private key

Keep these mentally separate:

```text
Certificate
───────────
Public
Contains public key
Contains identity information
Contains CA signature
Can be sent to clients
```

versus:

```text
Private key
───────────
Secret
Held by server
Used for cryptographic operations
Must be protected
```

A common server deployment might therefore have:

```text
server.crt   ← certificate
server.key   ← private key
```

The `.crt`, `.cer`, `.pem`, `.key` extensions are file-format conventions, though; the extension alone doesn't tell you exactly what's inside.

---

# 21. A complete certificate chain

Now put everything together.

Imagine:

```text
                    Root CA
                       │
                 private key 🔒
                       │
                       │ signs
                       ▼
              Intermediate CA
                       │
                 private key 🔒
                       │
                       │ signs
                       ▼
              example.com cert
                       │
                       │ contains
                       ▼
                 Public key 📢
```

The server generally sends its leaf certificate and the necessary intermediate certificates.

The client already has the trusted root, so it can construct:

```text
example.com
     ↓
Intermediate CA
     ↓
Trusted Root CA
```

The client verifies the signatures up the chain.

---

# 22. Why doesn't the server normally send the root?

Usually, the server sends:

```text
Leaf certificate
Intermediate certificate(s)
```

but not necessarily the root.

Why?

Because the client is expected to already have the trusted root.

Think:

```text
SERVER SENDS

example.com
    ↓
Intermediate CA


CLIENT ALREADY HAS

Root CA
```

Then:

```text
example.com
     ↓
Intermediate
     ↓
Root
     ↓
Trust store
```

---

# 23. Our TLS story is almost complete

Look at how every piece now connects:

```text
                     HTTPS
                       │
                       ▼
                      TLS
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   Encryption      Integrity     Authentication
        │              │              │
     AES-GCM        AEAD         Certificates
        │                             │
        │                        CA signatures
        │                             │
        │                        Trust store
        │
        └────────── Key Exchange
                       │
                    ECDHE/DH
                       │
                       ▼
                 Shared secrets
```

We're now ready to look at the actual **TLS handshake**.

---

# Hands-on checkpoint

Before moving on, run these three commands.

### 1. See the certificate chain

```bash
openssl s_client \
  -connect google.com:443 \
  -servername google.com
```

Look for:

```text
Certificate chain
```

---

### 2. Inspect the leaf certificate

```bash
openssl s_client \
  -connect google.com:443 \
  -servername google.com \
  </dev/null 2>/dev/null \
  | openssl x509 -noout -text
```

Find these sections:

```text
Issuer
Subject
Validity
Subject Public Key Info
Subject Alternative Name
```

---

### 3. See the certificate in a compact form

```bash
openssl s_client \
  -connect google.com:443 \
  -servername google.com \
  </dev/null 2>/dev/null \
  | openssl x509 -noout \
    -subject \
    -issuer \
    -dates \
    -ext subjectAltName
```

You should get something much easier to read.

---

## The big picture so far

We started with:

```text
HTTP
 │
 └── ❌ insecure
```

and progressively solved the problems:

```text
Confidentiality
      ↓
Encryption

Integrity
      ↓
Hashes + authentication / AEAD

Key establishment
      ↓
Diffie-Hellman / ECDHE

Server authentication
      ↓
Digital signatures

"Who says this key belongs to this server?"
      ↓
Certificate

"Who do I trust?"
      ↓
Certificate Authority

"How do I know the CA is trusted?"
      ↓
Root CA + Trust Store
```

### Next lesson: **The TLS Handshake**

We'll take an actual connection like:

```text
curl https://example.com
```

and go **packet-by-packet**:

```text
Client                         Server

ClientHello ──────────────────>
            <────────────────── ServerHello
            <────────────────── Certificate
            <────────────────── ...
...
HTTP request ─────────────────>
            <────────────────── HTTP response
```

We'll explain **what each message contains, why it exists, where the keys come from, and how TLS 1.3 gets from "hello" to encrypted HTTP**.

---

# Lesson 6 — The TLS 1.3 Handshake

Now we get to the most important part of the course:

> **What actually happens when you type `https://example.com` and press Enter?**

We'll focus on **TLS 1.3**, because that's the modern version you should understand first.

Don't worry if some details feel new. We'll keep connecting them back to the pieces we've already learned.

---

# 1. Start with the big picture

Suppose:

```text
Browser
   │
   │ HTTPS
   ▼
example.com
```

Before HTTP data can be safely exchanged, TLS has to establish:

1. Which cryptographic algorithms to use
2. A shared secret
3. The server's identity
4. Keys for encrypting the connection

Conceptually:

```text
Browser                         Server

   │                              │
   │────── ClientHello ──────────>│
   │                              │
   │<────── ServerHello ──────────│
   │<────── Certificate ──────────│
   │<────── CertificateVerify ────│
   │<────── Finished ─────────────│
   │                              │
   │────── Finished ─────────────>│
   │                              │
   │══════ Encrypted HTTP ═══════>│
```

That's the simplified TLS 1.3 handshake.

But there's a subtle and beautiful thing happening here:

**The key exchange and authentication happen together.**

---

# 2. First: ClientHello

The client starts.

Conceptually:

```text
Browser
   │
   │ ClientHello
   ▼
Server
```

The ClientHello contains information such as:

```text
ClientHello
├── TLS version / supported versions
├── Random value
├── Supported cipher suites
├── Key share
├── Extensions
└── SNI
```

Let's unpack these.

---

# 3. TLS version

The client tells the server what TLS versions it supports.

Modern clients generally advertise TLS 1.3 support.

Conceptually:

```text
Supported versions:

TLS 1.3
TLS 1.2
...
```

The server selects a compatible version.

For our course:

```text
Client → TLS 1.3 supported
Server → TLS 1.3 selected
```

---

# 4. Cipher suites

Remember that TLS needs symmetric encryption.

The client therefore tells the server which cryptographic combinations it supports.

For TLS 1.3, you'll encounter names such as:

```text
TLS_AES_128_GCM_SHA256
TLS_AES_256_GCM_SHA384
TLS_CHACHA20_POLY1305_SHA256
```

Don't try to memorize them yet.

We'll decode this later.

The important idea:

```text
Client:
"I support these cryptographic algorithms."

Server:
"I'll use this one."
```

---

# 5. SNI — Server Name Indication

This is extremely important in real-world HTTPS.

Imagine one server has:

```text
Server
 ├── google.example
 ├── shop.example
 ├── api.example
 └── blog.example
```

The server needs to know which hostname the client wants.

The client therefore sends an extension called:

**SNI — Server Name Indication**

Conceptually:

```text
ClientHello

SNI:
    example.com
```

So the server knows:

> "The client is asking for `example.com`."

This allows infrastructure hosting many HTTPS sites on the same IP address to select the appropriate certificate/configuration.

---

# 6. The most important part: Key Share

Here's where our Diffie-Hellman lesson comes back.

The client generates an ephemeral key pair.

Conceptually:

```text
Client

private key 🔒
      +
public key 📢
```

The public portion is included in the ClientHello:

```text
ClientHello
    │
    └── Key Share
            │
            ▼
       Client public value
```

The private portion **never leaves the client**.

---

# 7. Server does the same

The server generates its own ephemeral key pair:

```text
Server

private key 🔒
      +
public key 📢
```

The server sends its public value back.

So:

```text
Client                              Server

private 🔒                           private 🔒
public  📢                           public  📢
    │                                  │
    │────── public ──────────────────>│
    │<────── public ──────────────────│
```

Both can now derive the same shared secret.

This is essentially the modern TLS form of **Diffie-Hellman**, usually using elliptic-curve Diffie-Hellman.

You'll frequently see:

**ECDHE**

which means:

> Elliptic Curve Diffie-Hellman Ephemeral.

---

# 8. What does "ephemeral" mean?

This is important.

The server doesn't necessarily use one permanent Diffie-Hellman private key forever.

Instead:

```text
Connection 1
    ↓
temporary key pair

Connection 2
    ↓
different temporary key pair

Connection 3
    ↓
different temporary key pair
```

So:

```text
Connection #1 → 🔑 temporary secret #1
Connection #2 → 🔑 temporary secret #2
Connection #3 → 🔑 temporary secret #3
```

This provides an important property called:

# Forward secrecy

We'll dedicate a full lesson to it later.

---

# 9. ServerHello

The server responds:

```text
Client                          Server

ClientHello ──────────────────>

            <────────────────── ServerHello
```

The ServerHello tells the client:

```text
"I've selected these parameters."
```

Among other things:

```text
ServerHello
├── Selected TLS version
├── Selected cipher suite
└── Server key share
```

Now both sides have exchanged the information needed for the key agreement.

---

# 10. Shared secret

Conceptually:

```text
Client                           Server

Client private 🔒                Server private 🔒
       │                                │
       │                                │
       ▼                                ▼
Client public 📢  ──────────────>  Server
Server public 📢  <──────────────  Client
       │                                │
       └──────────┬─────────────────────┘
                  ▼
             Shared secret
```

The network observer sees the public values.

But doesn't know the private values.

Therefore the attacker cannot practically derive the same secret.

---

# 11. But we still haven't authenticated the server

This is the problem we solved with certificates.

The server now sends:

```text
Certificate
```

Conceptually:

```text
Server
   │
   │ Certificate
   ▼
Client
```

The certificate says roughly:

```text
"This public key belongs to example.com."

Signed by:
Intermediate CA
```

The client then validates the certificate chain.

---

# 12. Certificate validation

The client checks things such as:

```text
Certificate
    │
    ├── Is the signature valid?
    │
    ├── Is the CA trusted?
    │
    ├── Does the chain lead to a trusted root?
    │
    ├── Is the certificate currently valid?
    │
    ├── Does hostname match SAN?
    │
    └── Are other certificate constraints satisfied?
```

If validation fails:

```text
❌ TLS connection rejected
```

If validation succeeds:

```text
✅ Server identity accepted
```

---

# 13. But the certificate alone isn't enough

This is a subtle but extremely important point.

Suppose the certificate says:

```text
example.com
public key = ABC...
```

The client needs to know:

> "Does the server actually possess the private key corresponding to ABC...?"

Otherwise someone could simply steal/copy the public certificate.

That's where:

# CertificateVerify

comes in.

---

# 14. CertificateVerify

The server uses its **private key corresponding to the certificate's public key** to create a digital signature over the relevant handshake transcript.

Conceptually:

```text
Handshake messages
      │
      ▼
    Hash
      │
      ▼
Server private key 🔒
      │
      ▼
Digital signature
```

The server sends:

```text
CertificateVerify
    │
    └── signature
```

The client uses the public key from the certificate:

```text
Certificate public key
          +
CertificateVerify signature
          ↓
        Verify
          ↓
          ✅
```

This proves something very important:

> The server participating in this handshake possesses the private key corresponding to the certified public key.

---

# 15. We now have two different kinds of keys

This is where beginners often get confused.

During TLS, we have:

### Certificate key

Used for authentication:

```text
Server private key
       ↓
CertificateVerify
```

and:

```text
Certificate public key
       ↓
Verify signature
```

---

### Ephemeral ECDHE key

Used for key agreement:

```text
Client ephemeral private
       +
Server ephemeral public
       ↓
Shared secret
```

and:

```text
Server ephemeral private
       +
Client ephemeral public
       ↓
Same shared secret
```

So:

```text
Certificate keys
        │
        └── Authentication

ECDHE keys
        │
        └── Key agreement
```

**They're solving different problems.**

---

# 16. Why not use the certificate key for everything?

Excellent question.

Historically, TLS could use RSA for key transport.

Modern TLS 1.3 intentionally uses ephemeral Diffie-Hellman-style key exchange.

Why?

One major reason is:

# Forward secrecy

Suppose an attacker records your encrypted traffic today:

```text
2026

Attacker
   │
   └── records encrypted traffic
```

Then imagine they steal your server's long-term private key next year:

```text
2027

Server private key stolen 🔓
```

With ephemeral key exchange, the attacker generally **cannot use that long-term private key alone to reconstruct past session secrets**.

That's a huge security property.

We'll explore exactly why later.

---

# 17. Finished

After the handshake messages have been authenticated, both sides send:

```text
Finished
```

Conceptually:

```text
Client                         Server

ClientHello ──────────────────>
             <───────────────── ServerHello
             <───────────────── Certificate
             <───────────────── CertificateVerify
             <───────────────── Finished

Finished ─────────────────────>
```

The Finished message proves that both parties derived the expected handshake secrets and that the handshake wasn't tampered with.

At this point:

```text
             TLS established
                    │
                    ▼
              🔑 session keys
                    │
                    ▼
             Secure channel
```

---

# 18. Now HTTP can finally happen

The browser can now send:

```http
GET / HTTP/1.1
Host: example.com
```

But this isn't sent as plaintext over the network.

Instead:

```text
HTTP request
     │
     ▼
TLS record layer
     │
     ▼
Encryption
     │
     ▼
Encrypted bytes
     │
     ▼
Network
```

The server decrypts it:

```text
Encrypted bytes
      │
      ▼
   decrypt
      │
      ▼
HTTP request
```

So:

```text
Browser                              Server

GET / HTTP/1.1
      │
      │ 🔒 encrypted
      ├─────────────────────────────>
      │
      │                     decrypt
      │                         ↓
      │                    GET / HTTP/1.1
```

---

# 19. The complete TLS 1.3 story

Let's put everything together.

```text
CLIENT                                      SERVER
  │                                            │
  │ ClientHello                                │
  │ - TLS versions                             │
  │ - cipher suites                            │
  │ - SNI                                      │
  │ - ephemeral key share                      │
  │───────────────────────────────────────────>│
  │                                            │
  │                          ServerHello        │
  │                          - selected TLS     │
  │                          - selected cipher  │
  │                          - key share        │
  │<───────────────────────────────────────────│
  │                                            │
  │                          Certificate       │
  │<───────────────────────────────────────────│
  │                                            │
  │                          CertificateVerify │
  │<───────────────────────────────────────────│
  │                                            │
  │                          Finished         │
  │<───────────────────────────────────────────│
  │                                            │
  │ Finished                                   │
  │───────────────────────────────────────────>│
  │                                            │
  │                                            │
  │ 🔒 encrypted HTTP                         │
  │───────────────────────────────────────────>│
  │                                            │
  │ 🔒 encrypted HTTP response                 │
  │<───────────────────────────────────────────│
```

This is the core of HTTPS.

---

# 20. Let's see this on your Mac

Now let's stop talking abstractly.

Run:

```bash
openssl s_client \
  -connect example.com:443 \
  -servername example.com \
  -tls1_3
```

You'll see information about the TLS connection.

Look for things such as:

```text
Protocol  : TLSv1.3
Cipher    : ...
Server certificate
```

The exact output varies by OpenSSL version and server configuration.

---

# 21. Get a much cleaner summary

Try:

```bash
openssl s_client \
  -connect example.com:443 \
  -servername example.com \
  -tls1_3 \
  </dev/null 2>/dev/null
```

Look for:

```text
Protocol  : TLSv1.3
Cipher    : TLS_AES_256_GCM_SHA384
```

You may get a different cipher suite. That's perfectly normal.

---

# 22. What does this cipher suite mean?

Suppose you see:

```text
TLS_AES_256_GCM_SHA384
```

Break it apart:

```text
TLS
 │
 └── TLS protocol

AES_256
 │
 └── AES with 256-bit key

GCM
 │
 └── authenticated encryption mode

SHA384
 │
 └── hash used by the TLS cryptographic machinery
```

We'll dive deeply into this later.

Notice what's **not** in the name:

```text
RSA
Diffie-Hellman
ECDHE
```

That's because TLS 1.3 separates key exchange/authentication mechanisms from the symmetric cipher suite naming in a way that differs from TLS 1.2.

---

# 23. Try forcing TLS 1.2

Run:

```bash
openssl s_client \
  -connect example.com:443 \
  -servername example.com \
  -tls1_2 \
  </dev/null 2>/dev/null
```

Then compare:

```text
TLS 1.2
```

with:

```text
TLS 1.3
```

You'll notice differences.

TLS 1.3 significantly simplified and shortened the handshake and removed several older cryptographic mechanisms.

We'll do a dedicated **TLS 1.2 vs TLS 1.3** lesson later.

---

# 24. One subtle correction

You may now think:

```text
ClientHello
      ↓
ServerHello
      ↓
Certificate
      ↓
Finished
      ↓
HTTP
```

and that's approximately correct for learning.

But don't think of TLS as simply a sequence of independent messages.

The handshake messages are cryptographically tied together.

The protocol derives keys from the handshake state and authenticates the transcript.

That's important because an attacker shouldn't be able to:

```text
modify message 1
remove message 2
replace message 3
```

without the other side detecting it.

This is why **Finished** is so important.

---

# 25. The complete mental model

At this point you should be able to think about HTTPS like this:

```text
                    HTTPS
                      │
                      ▼
                    TLS
                      │
          ┌───────────┴───────────┐
          │                       │
    Authentication          Key Agreement
          │                       │
    Certificate              ECDHE
          │                       │
    CertificateVerify             │
          │                       │
          └───────────┬───────────┘
                      ▼
                Shared secrets
                      │
                      ▼
                Key derivation
                      │
                      ▼
                Session keys
                      │
                      ▼
              AES-GCM / ChaCha20
                      │
                      ▼
              Encrypted HTTP
```

And the three original problems:

```text
Can someone read it?
        ↓
Encryption

Can someone modify it?
        ↓
Authenticated encryption + handshake authentication

Am I talking to the real server?
        ↓
Certificate + CA + signature
```

---

# Next lesson — Let's actually build HTTPS ourselves

This is where the course becomes much more hands-on.

We'll create:

```text
                    Our local machine

                 ┌──────────────┐
                 │    Root CA   │
                 └──────┬───────┘
                        │
                     signs
                        │
                        ▼
                 ┌──────────────┐
                 │ localhost    │
                 │ certificate  │
                 └──────┬───────┘
                        │
                        ▼
              Python HTTPS server
                        │
                        │ TLS
                        ▼
                     curl
```

We'll generate the CA and server certificate ourselves, start a tiny Python HTTPS server, connect to it with `curl`, and intentionally make certificate validation fail first.

Then we'll make our client trust our CA.

That will make **CA → certificate → trust store → TLS handshake → HTTPS** concrete rather than just theoretical.

---

# Lesson 7 — Build Your Own HTTPS Server

Now we're going to stop using `example.com`.

We'll create our own tiny HTTPS setup:

```text
                  Our Mac

        ┌──────────────────────┐
        │      Root CA         │
        │   ca.crt / ca.key    │
        └──────────┬───────────┘
                   │ signs
                   ▼
        ┌──────────────────────┐
        │ localhost certificate│
        │ server.crt           │
        │ server.key           │
        └──────────┬───────────┘
                   │
                   ▼
             Python server
                   │
              HTTPS :8443
                   │
                   ▼
                 curl
```

The goal is to understand **exactly where each file comes from and why it exists**.

---

# 1. Create a workspace

Open Terminal:

```bash
mkdir -p ~/tls-course
cd ~/tls-course
```

We'll keep everything here.

```text
tls-course/
```

---

# 2. First create our Certificate Authority

Remember our certificate chain:

```text
Root CA
   │
   │ signs
   ▼
Server certificate
```

We are going to become our own tiny CA.

This is **only for learning/local development**.

---

## Generate the CA private key

Run:

```bash
openssl genrsa -out ca.key 4096
```

Now:

```text
tls-course/
└── ca.key
```

This is extremely important.

```text
ca.key 🔒
```

It is the CA's private key.

Anyone who gets this key could potentially create certificates that clients trusting this CA would accept.

So in real life:

> **CA private keys are extremely sensitive.**

---

# 3. Create the CA certificate

Now run:

```bash
openssl req \
  -x509 \
  -new \
  -key ca.key \
  -sha256 \
  -days 3650 \
  -out ca.crt \
  -subj "/C=IN/O=TLS Course/CN=TLS Course Root CA"
```

We now have:

```text
tls-course/
├── ca.key    🔒
└── ca.crt    📜
```

What's `ca.crt`?

It's the **public certificate of our CA**.

It contains information about the CA and its public key, and it is self-signed because this is our root.

Conceptually:

```text
ca.key
  │
  │ signs
  ▼
ca.crt
```

---

# 4. Why is the root certificate self-signed?

Our root CA is at the top of the trust hierarchy:

```text
Root CA
   │
   └── nobody above it
```

So who signs the root?

The root signs itself.

Hence:

```text
Root CA
  │
  └── signs its own certificate
```

This is called a **self-signed certificate**.

But remember:

> Self-signed does not automatically mean trusted.

Our Mac doesn't inherently trust `TLS Course Root CA`.

We haven't installed it into a trust store yet.

---

# 5. Inspect our CA certificate

Run:

```bash
openssl x509 \
  -in ca.crt \
  -noout \
  -subject \
  -issuer \
  -dates
```

You'll see something like:

```text
subject=C=IN, O=TLS Course, CN=TLS Course Root CA
issuer=C=IN, O=TLS Course, CN=TLS Course Root CA
notBefore=...
notAfter=...
```

Notice:

```text
subject = TLS Course Root CA
issuer  = TLS Course Root CA
```

That's because it's self-signed.

---

# 6. Now create the server private key

Our HTTPS server needs its own key pair.

Run:

```bash
openssl genrsa -out server.key 2048
```

Now:

```text
tls-course/
├── ca.key       🔒
├── ca.crt       📜
└── server.key   🔒
```

The server's private key is different from the CA's private key.

That's important.

```text
CA private key
      │
      └── signs certificates

Server private key
      │
      └── proves server identity during TLS
```

---

# 7. Create a certificate signing request

Now the server needs to ask our CA:

> "Please issue me a certificate for `localhost`."

That's what a **CSR** is.

CSR = **Certificate Signing Request**

Run:

```bash
openssl req \
  -new \
  -key server.key \
  -out server.csr \
  -subj "/C=IN/O=TLS Course/CN=localhost"
```

Now:

```text
tls-course/
├── ca.key
├── ca.crt
├── server.key
└── server.csr
```

---

# 8. What is inside a CSR?

Conceptually:

```text
CSR
├── Requested identity
│     localhost
│
├── Server public key
│
└── Proof that requester owns
    corresponding private key
```

The CSR is essentially:

> "Here is the public key I want certified, and here is the identity I'm requesting."

The CA then decides whether to issue a certificate.

---

# 9. Important distinction: CSR ≠ Certificate

Don't mix these up.

```text
CSR
 │
 │ submitted to CA
 ▼
CA
 │
 │ validates / approves / signs
 ▼
Certificate
```

So:

```text
server.csr
```

is a request.

Whereas:

```text
server.crt
```

will be the actual certificate.

---

# 10. Why do we need SAN?

Earlier we discussed:

```text
Subject Alternative Name
```

Modern TLS hostname validation relies on SAN.

So we're going to explicitly create:

```text
localhost
127.0.0.1
```

as valid identities.

Create:

```bash
cat > server.ext <<'EOF'
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=@alt_names

[alt_names]
DNS.1=localhost
IP.1=127.0.0.1
EOF
```

Check it:

```bash
cat server.ext
```

You should see:

```text
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=@alt_names

[alt_names]
DNS.1=localhost
IP.1=127.0.0.1
```

---

# 11. Sign the server certificate

Now our CA signs the CSR:

```bash
openssl x509 \
  -req \
  -in server.csr \
  -CA ca.crt \
  -CAkey ca.key \
  -CAcreateserial \
  -out server.crt \
  -days 365 \
  -sha256 \
  -extfile server.ext
```

Now:

```text
tls-course/
├── ca.key
├── ca.crt
├── ca.srl
├── server.key
├── server.csr
├── server.crt
└── server.ext
```

The important files are:

```text
ca.key       🔒 Root CA private key
ca.crt       📜 Root CA certificate

server.key   🔒 Server private key
server.crt   📜 Server certificate
```

---

# 12. Look at the server certificate

Run:

```bash
openssl x509 \
  -in server.crt \
  -noout \
  -subject \
  -issuer \
  -dates \
  -ext subjectAltName
```

You should see something like:

```text
subject=C=IN, O=TLS Course, CN=localhost
issuer=C=IN, O=TLS Course, CN=TLS Course Root CA

X509v3 Subject Alternative Name:
    DNS:localhost, IP Address:127.0.0.1
```

This is our chain:

```text
TLS Course Root CA
       │
       │ signed
       ▼
   localhost
 certificate
```

---

# 13. Verify the certificate manually

This is a great command:

```bash
openssl verify \
  -CAfile ca.crt \
  server.crt
```

You should get:

```text
server.crt: OK
```

Think about what we just told OpenSSL:

```text
"Trust ca.crt as a CA,
and verify server.crt against it."
```

It found:

```text
server.crt
    │
    │ signed by
    ▼
ca.crt
    │
    ▼
trusted
```

---

# 14. But your browser doesn't trust our CA

This is intentional.

Our CA is:

```text
TLS Course Root CA
```

Your Mac has no reason to trust it.

So if we start an HTTPS server and connect normally, we'll get a trust error.

That's exactly what we want to see.

---

# 15. Create a tiny HTTPS server

Create:

```bash
cat > server.py <<'EOF'
from http.server import HTTPServer, SimpleHTTPRequestHandler
import ssl

server = HTTPServer(("127.0.0.1", 8443), SimpleHTTPRequestHandler)

context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
context.load_cert_chain(
    certfile="server.crt",
    keyfile="server.key",
)

server.socket = context.wrap_socket(
    server.socket,
    server_side=True,
)

print("HTTPS server running on https://localhost:8443")

server.serve_forever()
EOF
```

Run:

```bash
python3 server.py
```

You should see:

```text
HTTPS server running on https://localhost:8443
```

Your server is now listening on:

```text
https://localhost:8443
```

---

# 16. Connect using curl

Open another terminal.

Run:

```bash
curl https://localhost:8443
```

You will likely get something similar to:

```text
curl: (60) SSL certificate problem:
unable to get local issuer certificate
```

This is **good**.

Our TLS connection is being rejected because:

```text
localhost certificate
       │
       │ signed by
       ▼
TLS Course Root CA
       │
       │
       ▼
❌ curl doesn't trust this CA
```

---

# 17. This is the most important lesson here

The certificate itself can be perfectly valid.

The signature can be perfectly valid.

The certificate can contain:

```text
localhost
```

And still:

```text
❌ connection rejected
```

because the client doesn't trust the CA.

This is why we said earlier:

> **Valid signature ≠ trusted certificate.**

Trust is ultimately anchored in the client's configured trust store.

---

# 18. Let's explicitly tell curl to trust our CA

We can do this without changing your Mac's global trust store.

Run:

```bash
curl \
  --cacert ca.crt \
  https://localhost:8443
```

Now:

```text
curl
 │
 │ trust ca.crt
 ▼
TLS Course Root CA
 │
 │ signed
 ▼
localhost certificate
 │
 ▼
TLS handshake
 │
 ▼
HTTP
```

You should receive an HTTP response from your Python server.

🎉

You just built a working private HTTPS PKI.

---

# 19. Notice what `--cacert` means

This:

```bash
curl --cacert ca.crt ...
```

does **not** mean:

> "Ignore certificate validation."

It's the opposite.

It means:

> "Use this certificate as a trusted CA."

That's very different from:

```bash
curl -k https://localhost:8443
```

which disables normal certificate verification.

Avoid thinking of `-k` as a solution to certificate problems.

For learning, it's useful to see the difference:

```text
--cacert ca.crt
     ↓
"I trust this CA."
```

versus:

```text
-k
     ↓
"Don't verify the certificate properly."
```

---

# 20. Let's inspect the actual TLS connection

Run:

```bash
openssl s_client \
  -connect localhost:8443 \
  -servername localhost
```

You'll see your certificate:

```text
Certificate chain
...
```

and information such as:

```text
Protocol
Cipher
Server certificate
```

You can also explicitly give OpenSSL your CA:

```bash
openssl s_client \
  -connect localhost:8443 \
  -servername localhost \
  -CAfile ca.crt
```

Near the end you should see:

```text
Verify return code: 0 (ok)
```

That is extremely useful.

---

# 21. What just happened?

Let's reconstruct the entire process.

You ran:

```text
curl https://localhost:8443
```

The client contacted:

```text
127.0.0.1:8443
```

The server said:

```text
"I support TLS."
```

Then:

```text
Server
  │
  ├── server.crt
  │
  └── server.key
```

The certificate says:

```text
localhost
   │
   │ public key
   │
   └── signed by TLS Course Root CA
```

Curl initially said:

```text
"I don't trust TLS Course Root CA."
```

Therefore:

```text
❌ TLS verification failed
```

Then we gave curl:

```text
--cacert ca.crt
```

Now:

```text
curl
  │
  │ trusts
  ▼
TLS Course Root CA
  │
  │ signed
  ▼
localhost certificate
  │
  ▼
✅ trusted
```

Then the TLS handshake could complete.

---

# 22. This is a miniature version of real HTTPS

Our setup:

```text
              Our Root CA
                   │
                   │ signs
                   ▼
            localhost.crt
                   │
                   ▼
              Python server
                   │
                   ▼
                 curl
```

The real internet:

```text
                 Root CA
                    │
                    │
                    ▼
              Intermediate CA
                    │
                    │
                    ▼
              example.com
                    │
                    ▼
               Web server
                    │
                    ▼
                 Browser
```

Same fundamental trust model.

The scale and operational complexity are different.

---

# 23. One thing we haven't done yet

Our setup currently has:

```text
Root CA
   │
   └── localhost
```

Real production PKI normally looks more like:

```text
Root CA
   │
   ▼
Intermediate CA
   │
   ▼
Server certificate
```

We'll build that ourselves too.

But before doing that, there's another very important concept.

---

# 24. Where are the actual encryption keys?

Look at our files:

```text
ca.key
server.key
```

Those are **private keys**.

But when you connect with TLS, the actual traffic encryption uses **session keys derived during the handshake**.

These are not simply:

```text
server.key
```

used to encrypt every HTTP request.

Instead:

```text
server.key
      │
      │ authentication
      ▼
CertificateVerify

ECDHE ephemeral keys
      │
      ▼
shared secret
      │
      ▼
TLS key schedule
      │
      ▼
session traffic keys
      │
      ▼
AES-GCM / ChaCha20-Poly1305
      │
      ▼
HTTP
```

This distinction is fundamental.

---

# 25. Your current directory

At this point you should have:

```text
tls-course/
│
├── ca.key          🔒 Root private key
├── ca.crt          📜 Root certificate
│
├── server.key      🔒 Server private key
├── server.csr      📄 Certificate request
├── server.crt      📜 Server certificate
├── server.ext      ⚙️ Certificate extensions
│
├── ca.srl
└── server.py
```

You can inspect everything with:

```bash
ls -la
```

---

# 26. A very important security habit

Don't accidentally commit these into Git:

```text
❌ ca.key
❌ server.key
```

For this learning directory, you could create:

```bash
cat > .gitignore <<'EOF'
*.key
*.srl
EOF
```

The certificates are public information in principle:

```text
ca.crt
server.crt
```

The private keys are not.

---

# 27. What we have now learned

We started with:

```text
"I want HTTPS."
```

and actually built:

```text
                 Root CA
                   │
                signs
                   │
                   ▼
          localhost certificate
                   │
                   ▼
              HTTPS server
                   │
                   ▼
                  curl
```

We saw a real trust failure:

```text
❌ unable to get local issuer certificate
```

and fixed it properly by giving the client the CA:

```bash
curl --cacert ca.crt https://localhost:8443
```

That's a much better learning experience than simply saying "certificates provide trust."

---

## Next: Lesson 8 — What exactly is inside a TLS certificate?

We'll dissect `server.crt` field-by-field and then build the **full chain**:

```text
Root CA
   ↓
Intermediate CA
   ↓
localhost certificate
```

We'll also answer a subtle question:

> **If the CA signs a certificate, why doesn't the CA's private key have to participate in every HTTPS connection?**

That leads directly into the distinction between **certificate authentication, TLS key exchange, and session encryption**.

---

# Lesson 8 — What exactly is inside a TLS certificate?

We now have a working HTTPS server:

```text
curl
  │
  │ HTTPS
  ▼
localhost:8443
  │
  ├── server.crt
  └── server.key
```

But we have mostly treated `server.crt` as a black box.

Let's open it up.

---

## 1. First, inspect your certificate

From your `~/tls-course` directory:

```bash
cd ~/tls-course

openssl x509 \
  -in server.crt \
  -noout \
  -text
```

You'll see quite a lot of output.

Don't worry about understanding everything yet. The important sections are:

```text
Certificate:
    Data:
        Version: 3
        Serial Number: ...
        Signature Algorithm: ...
        Issuer: ...
        Validity:
            Not Before: ...
            Not After : ...
        Subject: ...
        Subject Public Key Info:
            ...
        X509v3 extensions:
            ...
    Signature Algorithm: ...
    Signature Value:
        ...
```

Let's understand these one by one.

---

# 2. Who issued this certificate?

Look at:

```text
Issuer: C=IN, O=TLS Course, CN=TLS Course Root CA
```

This means:

> "The CA that issued/signed this certificate is TLS Course Root CA."

And:

```text
Subject: C=IN, O=TLS Course, CN=localhost
```

means:

> "This certificate represents localhost."

So:

```text
Issuer  → who signed it
Subject → who the certificate represents
```

For a real website, you might see something like:

```text
Issuer:
    Google Trust Services ...

Subject:
    CN=www.google.com
```

---

# 3. The public key is inside the certificate

Look for:

```text
Subject Public Key Info:
    Public Key Algorithm: ...
    Public-Key: ...
```

This is extremely important.

Remember our earlier discussion:

```text
server.key
    │
    │ contains
    ▼
PRIVATE KEY 🔒
```

while:

```text
server.crt
    │
    │ contains
    ▼
PUBLIC KEY 🔓
```

The certificate essentially says:

```text
"This public key belongs to localhost."
```

and the CA signs that statement.

---

# 4. The certificate does NOT contain the private key

This distinction is worth making very clear.

You have:

```text
server.key
```

and:

```text
server.crt
```

The certificate contains:

```text
identity
+
public key
+
metadata
+
CA signature
```

It does **not** contain:

```text
server.key
```

That's why exposing a certificate is normally fine:

```bash
cat server.crt
```

But you should **never** casually expose:

```bash
cat server.key
```

The private key must remain secret.

---

# 5. Why does the certificate contain a public key?

Suppose your server sends:

```text
server.crt
```

to the browser.

The browser extracts:

```text
Server public key
```

Then during TLS authentication, the server proves:

> "I actually possess the private key corresponding to this public key."

It does this through `CertificateVerify`.

Conceptually:

```text
Certificate
    │
    └── public key
          │
          │ verify
          ▼
      CertificateVerify
          ▲
          │
      server.key
```

So the certificate doesn't authenticate the server by itself.

It provides the **public key and identity binding** needed for the authentication process.

---

# 6. What is SAN?

In your certificate, run:

```bash
openssl x509 \
  -in server.crt \
  -noout \
  -ext subjectAltName
```

You should see:

```text
X509v3 Subject Alternative Name:
    DNS:localhost, IP Address:127.0.0.1
```

This is the **Subject Alternative Name (SAN)** extension.

It tells the client:

```text
This certificate is valid for:

localhost
127.0.0.1
```

This matters because when you run:

```bash
curl https://localhost:8443
```

curl needs to verify:

```text
Requested hostname
        │
        ▼
     localhost
        │
        │ matches
        ▼
Certificate SAN
```

If the certificate were only valid for:

```text
example.com
```

then:

```bash
curl https://localhost:8443
```

should fail hostname verification.

---

# 7. Let's actually create that failure

We can demonstrate this.

Our current certificate has:

```text
DNS:localhost
IP:127.0.0.1
```

So this works:

```bash
curl --cacert ca.crt https://localhost:8443
```

But imagine we connect using a hostname that isn't listed.

For example:

```bash
curl --cacert ca.crt https://127.0.0.1:8443
```

Our certificate actually contains `127.0.0.1`, so this should also work.

But if we used:

```bash
curl --cacert ca.crt https://127.0.0.2:8443
```

we'd have a different problem.

The TCP connection itself isn't the issue.

The certificate identity is.

Conceptually:

```text
Client asks for:
127.0.0.2

Certificate says:
localhost
127.0.0.1

            ❌

Hostname/IP doesn't match
```

This is why certificates aren't simply:

> "The server has a valid certificate."

They are:

> "The server has a certificate that is trusted **and valid for the identity I'm connecting to**."

---

# 8. The certificate's validity period

You'll see:

```text
Validity
    Not Before: ...
    Not After : ...
```

For our certificate:

```bash
openssl x509 \
  -in server.crt \
  -noout \
  -dates
```

Example:

```text
notBefore=...
notAfter=...
```

The client checks whether the current time falls within that period.

Conceptually:

```text
         valid period
    ├───────────────────┤
    ▲                   ▲
 Not Before          Not After

          ▲
       current time
```

Outside the validity period:

```text
             ❌ certificate invalid
```

This is one reason certificates eventually need to be renewed/replaced.

---

# 9. What is the CA signature?

This is the really important part.

Our certificate contains something like:

```text
Signature Algorithm: sha256WithRSAEncryption

Signature Value:
    4a:9f:...
    ...
```

Remember our earlier digital-signature lesson.

The CA effectively signs the certificate's contents using:

```text
CA private key 🔒
```

The client can verify that signature using:

```text
CA public key 🔓
```

So conceptually:

```text
                CA
                │
         CA private key
                │
                ▼
       signs certificate
                │
                ▼
        server.crt
                │
                ▼
             Client
                │
       CA public key
                │
                ▼
       verify signature
```

---

# 10. The subtle question

You might now wonder:

> If the CA's private key signed my certificate, does the CA's private key participate whenever I make an HTTPS request?

**No.**

This is a crucial TLS concept.

The CA was involved when the certificate was issued:

```text
        Certificate issuance
       
Server ──CSR──► CA
                 │
                 │ CA private key
                 ▼
            server.crt
```

After that, the CA isn't sitting in the middle of every HTTPS connection.

Actual HTTPS connection:

```text
Browser                    Server
   │                          │
   │──── TLS handshake ──────►│
   │                          │
   │     certificate          │
   │◄─────────────────────────│
   │                          │
   │   verify CA signature    │
   │                          │
   │──── key exchange ───────►│
   │                          │
   │◄──── encrypted data ─────│
```

The CA's private key is **not** used to encrypt your HTTP requests.

---

# 11. Three different keys — three different jobs

This is one of the most important mental models in TLS.

### ① CA private key

```text
ca.key
```

Used by:

```text
CA
 │
 └── signs certificates
```

It is mainly involved during **certificate issuance**.

---

### ② Server private key

```text
server.key
```

Used by the server to prove:

```text
"I possess the private key corresponding
to the public key in this certificate."
```

This is part of TLS authentication.

---

### ③ Ephemeral ECDHE private key

Created for the TLS connection itself.

Used for:

```text
key agreement
```

It helps both sides derive a shared secret.

So:

```text
CA private key
     │
     └── certificate issuance


Server private key
     │
     └── server authentication


ECDHE private key
     │
     └── session key establishment
```

And eventually:

```text
session keys
     │
     └── encrypt HTTP data
```

---

# 12. Now let's build the complete picture

At certificate issuance time:

```text
                    Certificate Authority

                     ca.key 🔒
                        │
                        │ signs
                        ▼
                 ┌──────────────┐
                 │ server.crt   │
                 │              │
                 │ identity     │
                 │ public key   │
                 │ validity     │
                 │ SAN          │
                 │ CA signature │
                 └──────────────┘
```

At connection time:

```text
Client                                      Server
  │                                           │
  │────────── ClientHello ──────────────────►│
  │                                           │
  │◄──────── ServerHello ────────────────────│
  │                                           │
  │◄──────── server.crt ─────────────────────│
  │                                           │
  │   verify certificate                      │
  │                                           │
  │   verify CA signature                     │
  │   check validity                          │
  │   check SAN                               │
  │                                           │
  │◄──── CertificateVerify ──────────────────│
  │                                           │
  │   verify using certificate's             │
  │   public key                              │
  │                                           │
  │─────── ECDHE key exchange ──────────────►│
  │                                           │
  │      derive shared secrets                │
  │                                           │
  │════════ encrypted HTTP ══════════════════│
```

Notice the separation:

```text
Certificate
     ↓
"Who is this server?"

ECDHE
     ↓
"How do we establish shared secrets?"

Symmetric encryption
     ↓
"How do we efficiently protect the actual data?"
```

That's the architecture we're trying to understand.

---

# 13. One more important certificate extension

Run:

```bash
openssl x509 \
  -in server.crt \
  -noout \
  -ext basicConstraints \
  -ext keyUsage \
  -ext extendedKeyUsage
```

You should see things corresponding to what we specified:

```text
CA:FALSE
```

This says:

> This certificate is not a CA certificate.

And:

```text
Extended Key Usage:
    TLS Web Server Authentication
```

This says the certificate is intended for server authentication.

So certificates don't merely contain:

```text
name + public key
```

They can contain constraints describing **how that certificate may be used**.

---

## Your certificate mental model

Think of `server.crt` as a signed ID card:

```text
┌────────────────────────────────────┐
│          SERVER CERTIFICATE        │
│                                    │
│ Subject: localhost                 │
│                                    │
│ SAN: localhost, 127.0.0.1          │
│                                    │
│ Public Key: 🔓                     │
│                                    │
│ Valid: date → date                 │
│                                    │
│ Usage: Server Authentication       │
│                                    │
│ Issuer: TLS Course Root CA         │
│                                    │
│ CA Signature: ✍️                   │
└────────────────────────────────────┘
```

The CA is essentially saying:

> "I, a trusted CA, signed this certificate stating that this public key is associated with this identity, subject to these constraints."

---

## Next: Lesson 9 — Let's build a real certificate chain

Right now we have:

```text
Root CA
   │
   └── localhost certificate
```

Real production PKI usually looks more like:

```text
Root CA
   │
   ▼
Intermediate CA
   │
   ▼
Server Certificate
```

We'll create that ourselves with OpenSSL.

You'll see **why intermediate CAs exist**, why servers normally send the intermediate certificate, why the root is normally kept offline, and what happens when the chain is incomplete.

Then we'll deliberately break the chain and watch TLS fail.

Say **next** when you're ready.

---
