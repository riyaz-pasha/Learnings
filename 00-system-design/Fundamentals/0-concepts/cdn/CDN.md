A CDN, or **Content Delivery Network**, is a geographically distributed network of servers designed to deliver content, like images, videos, and stylesheets, to users quickly and efficiently. Instead of every user fetching content directly from a single origin server (your main server), the CDN stores copies of that content on its servers, called **edge servers** or **Points of Presence (PoPs)**, located around the world.

-----

## How CDNs Work

The fundamental principle behind a CDN is to reduce **latency** (the delay in communication) by minimizing the physical distance between the user and the content.

1.  **Request Initiation**: When a user's browser requests a web page, the initial request for the main HTML file goes to your origin server.
2.  **Referencing CDN**: The HTML page and other files (like CSS and JavaScript) contain URLs that point to the static assets. For assets you want to serve via the CDN, the URLs are configured to point to your CDN's domain (e.g., `https://cdn.example.com/images/logo.png`).
3.  **DNS Resolution**: The user's browser sends a DNS query to find the IP address for the CDN's domain. The CDN's DNS server uses a method called **Anycast** or other routing algorithms to determine the optimal CDN edge server to serve the request. It typically chooses the server that is geographically closest to the user.
4.  **Content Delivery**:
      * **Cache Hit**: If the requested content is already stored (cached) on the closest edge server, the CDN immediately serves the content to the user. This is a "cache hit." ⚡
      * **Cache Miss**: If the content isn't on the edge server, it's a "cache miss." The edge server then fetches the content from your origin server, caches a copy, and then delivers it to the user. Subsequent requests from users in the same region will get the content from the cached copy, resulting in a fast "cache hit."

-----

## How a User's Browser Connects to a CDN

A user's browser connects to a CDN because your website explicitly tells it to. When you set up a CDN, you change the URLs of your static assets to point to the CDN's domain instead of your own.

For example, without a CDN, your HTML might look like this:

```html
<img src="https://www.yourserver.com/images/photo.jpg">
```

With a CDN, you update the URL to point to the CDN:

```html
<img src="https://cdn.yourserver.com/images/photo.jpg">
```

When the browser parses the HTML and sees the new URL, it knows to make a request to `cdn.yourserver.com`, which the CDN's DNS system then routes to the nearest edge server.

-----

## Using a CDN with S3

To store an image in Amazon S3 and serve it via a CDN like Amazon CloudFront, you follow a specific process:

1.  **Store the Image in S3**: First, upload your image (e.g., `image.jpg`) to an S3 bucket. This bucket will act as your **origin server**—the single source of truth for your content.

2.  **Create a CloudFront Distribution**: In the AWS console, you'll create a new CloudFront distribution.

      * You'll specify your S3 bucket's URL as the **Origin Domain Name**.
      * It's a best practice to configure **Origin Access Control (OAC)** to ensure that users cannot access your S3 bucket directly and must go through the CloudFront distribution. This keeps your S3 bucket private while still allowing the CDN to fetch content from it.

3.  **Configure DNS and URLs**: Once the CloudFront distribution is deployed, it will provide a domain name (e.g., `d12345.cloudfront.net`). You would then update the URLs on your website to use this CloudFront domain.

    ```html
    <img src="https://d12345.cloudfront.net/images/image.jpg">
    ```

    Now, when a user's browser requests `image.jpg`, the request goes to CloudFront. CloudFront checks its cache, and if the image is not there, it pulls it from your private S3 bucket, caches it, and serves it to the user. All future requests for that image in that region will be served directly from the CloudFront edge server, bypassing S3 entirely.

---
---
---

## **1. How CDNs Work (High-Level)**

A **Content Delivery Network (CDN)** is a network of distributed servers (edge servers/PoPs) placed across the globe. The main goal is to deliver static (and sometimes dynamic) content **closer to the user** to reduce latency, improve performance, and offload your origin server.

* Your origin: Where your actual content lives (e.g., S3 bucket, your web server).
* CDN edge servers: Cache copies of your content and serve them to users from the nearest location.

When a user requests content:

1. The request first hits the **CDN DNS/edge network** instead of your server.
2. The CDN checks if the requested content is already cached at the nearest edge server.

   * If cached → return immediately from the edge (super fast).
   * If not cached → fetch from your **origin** (e.g., S3), store it at the edge, then serve to the user.

---

## **2. How does the user know which CDN server to connect to?**

This is done through **DNS resolution** + **Anycast routing**:

* You give the user a **CDN URL** (e.g., `https://cdn.example.com/image.jpg`).
* `cdn.example.com` points to the CDN provider’s DNS.
* The CDN provider uses **GeoDNS + Anycast BGP routing** to direct the user to the nearest/fastest edge server.
* To the user, it feels like a normal DNS resolution → but the CDN ensures they’re routed to the closest node.

So the user never decides directly. DNS + CDN routing decides.

---

## **3. How does the user know to fetch data from CDN instead of my server?**

This is up to **you as the system designer**. Two main options:

1. **Use CDN URLs in your app/website**
   Example:

   * Instead of embedding

     ```html
     <img src="https://myserver.com/images/cat.jpg">
     ```

     You embed:

     ```html
     <img src="https://cdn.example.com/images/cat.jpg">
     ```

   Now all requests go to the CDN domain.

2. **Transparent proxy mode (reverse proxy CDN)**

   * You keep URLs as `myserver.com`.
   * But your domain’s DNS is pointed to the CDN (Cloudflare, Akamai, Fastly, CloudFront, etc.).
   * CDN intercepts all requests, serves cached content when possible, and fetches from origin otherwise.

---

## **4. Example: S3 + CDN (CloudFront)**

Let’s say your image is in S3:

```
https://mybucket.s3.amazonaws.com/cat.jpg
```

### Step 1: Create a CloudFront distribution

* Origin: `mybucket.s3.amazonaws.com`
* Configure caching rules, TTL, headers, etc.

### Step 2: CloudFront gives you a CDN domain

* Something like: `https://d123.cloudfront.net`

### Step 3: Use CDN URL in your app

* Instead of:

  ```html
  <img src="https://mybucket.s3.amazonaws.com/cat.jpg">
  ```

  Use:

  ```html
  <img src="https://d123.cloudfront.net/cat.jpg">
  ```

### Step 4 (Optional): CNAME for prettier URLs

* Add DNS record: `cdn.example.com → d123.cloudfront.net`
* Now use:

  ```html
  <img src="https://cdn.example.com/cat.jpg">
  ```

### Step 5: User request flow

1. User requests `cdn.example.com/cat.jpg`.
2. CDN DNS routes them to nearest edge.
3. Edge server checks cache:

   * If present → return immediately.
   * If not → fetch from S3 origin, cache it, then serve.

---

## **5. Summary**

* User doesn’t decide where to fetch — **you control it** via DNS and URLs.
* CDN decides **which edge** to serve from, using DNS + Anycast routing.
* To make users fetch from CDN instead of your server:

  * Change URLs to point to CDN, **or**
  * Put CDN in front of your server with DNS pointing to CDN.
* For S3 → use CloudFront (or any CDN) in front of your bucket, then give users the CDN domain.

---

Let’s go step by step — **Anycast routing** is one of the magic ingredients that makes CDNs (and DNS resolvers like Google DNS `8.8.8.8` or Cloudflare `1.1.1.1`) so fast.

---
---
---

## **1. What is Anycast?**

**Anycast** is a **network routing technique** where a **single IP address** is assigned to **multiple servers/locations** around the world.

* All those servers announce the **same IP** using BGP (Border Gateway Protocol).
* When a user sends a request to that IP, **the Internet automatically routes the request to the "nearest" or "best" location** (based on routing metrics like network distance, AS hops, latency, etc.).

👉 Think of it like having **one phone number shared by multiple call centers worldwide**.
When you dial, your call goes to the nearest call center, not always to the one where the number was created.

---

## **2. How CDNs use Anycast**

* A CDN edge node in New York, London, and Tokyo might all announce the same IP (e.g., `203.0.113.1`) to the global Internet.
* When a user in Paris requests content:

  * Their ISP’s routers see that the London edge advertising that IP is the closest/cheapest route.
  * Traffic flows to London, **even though Paris used the same IP**.

So, the same CDN hostname (like `cdn.example.com`) resolves to an IP, and **Anycast ensures traffic goes to the nearest edge**.

---

## **3. Anycast vs. Unicast**

* **Unicast**: One IP → one server location.
  Example: Your personal VPS in Mumbai with IP `52.95.123.1`. Only that server responds.
* **Anycast**: One IP → many servers in many locations.
  Example: Cloudflare DNS `1.1.1.1`. Dozens of servers worldwide respond, but users always hit the nearest.

---

## **4. Why Anycast is awesome for CDNs**

✅ **Low latency** – Users automatically connect to the closest edge.
✅ **Load balancing** – Requests spread across edges globally.
✅ **Redundancy** – If one data center goes offline, BGP reroutes traffic to the next closest.
✅ **Simple DNS** – You don’t need to manage per-region IPs. Same IP works everywhere.

---

## **5. Example Flow**

Let’s say your CDN provider gives you:

```
cdn.example.com → 203.0.113.1 (Anycast IP)
```

* A user in **Tokyo** connects → BGP routes them to Tokyo edge.
* A user in **New York** connects → BGP routes them to New York edge.
* Both users used the **same IP**, but ended up at totally different physical servers.

---

## **6. Where else is Anycast used?**

* **DNS resolvers** – Google DNS `8.8.8.8`, Cloudflare `1.1.1.1`, OpenDNS `208.67.222.222`.
* **DDoS mitigation** – Spreads attack traffic across many sites.
* **CDNs** – Cloudflare, Akamai, Fastly, AWS Route 53, CloudFront, etc.
* **IPv6 root DNS servers** – Most are Anycasted worldwide.

---

✅ In short:
**Anycast = one IP, many servers, always route user to the nearest/fastest one.**

