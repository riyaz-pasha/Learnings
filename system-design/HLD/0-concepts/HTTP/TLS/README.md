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

