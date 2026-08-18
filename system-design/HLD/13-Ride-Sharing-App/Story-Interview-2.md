# Uber System Design — the story, told simply

I'm going to tell this as one continuous story. Same rider, same drivers, all the way through. Every time something breaks, I'll explain why it broke in plain terms, and every time I fix it, I'll explain why that specific fix is the right one, not just what the fix is.

The characters: Rider R500 is standing in Koramangala, Bangalore. She wants to go to Indiranagar. There are two drivers near her. Driver D200 is close by, just 800 metres away, but there's a lake between them with no direct road. Driver D205 is a bit further, 1.8 kilometres away, but on a clear main road.

I'll tell this in two parts. First, how do we even know where D200 and D205 are, right now, at any moment. Second, once we know where they are, how do we actually pick one of them and get R500 into a car. These are genuinely two separate problems, so I want to solve the first one completely before touching the second.

---

## Part One: Knowing Where The Drivers Are

### The very first attempt

The simplest thing anyone would try is a normal database table. Every driver has a row, with their id, their status, and their current latitude and longitude. When R500 asks for a ride, we run a query that says, give me everyone whose status is online.

Here's the problem with that, and it's a simple one. We have a million drivers in that table. Most of them are nowhere near Bangalore. The database has no idea that "online" has anything to do with "close to Koramangala," so it has to look through a huge number of rows just to find the handful that are actually useful, D200 and D205 among them. Then, after fetching all of that, our own code has to calculate the distance from R500 to every single one of those rows just to figure out who's close. On a table that size, this takes seconds, not milliseconds. And remember, this is happening for every single ride request coming in, roughly a hundred times a second at minimum. So this approach just falls over immediately.

### Adding an index

The next thing I'd try is telling the database, hey, at least narrow it down by a rough box around R500's location. Only give me drivers whose latitude and longitude fall inside this small rectangle.

This does help, fewer rows come back. But two things are still wrong with it. First, a rectangle is not the same shape as a circle, which is what I actually want, everyone within 3 kilometres of R500. So either I make the rectangle big enough to be safe, in which case I'm still fetching more than I need, or I make it tight, in which case I might miss someone who's genuinely close but happens to sit slightly outside the box. Second, and this is a more technical point, a database index really works best on one column at a time. Latitude and longitude are two separate columns, so the database is doing two separate narrowing steps and then combining them, which still isn't as efficient as it could be.

### Turning two numbers into one

The real fix for this is to stop thinking of location as two separate numbers, latitude and longitude, and instead turn it into a single value. This is what a geohash does. You take the bits of the latitude and the bits of the longitude and you weave them together into one string. The nice property this gives you is, if two locations are physically close to each other, their geohash strings usually share the same starting letters, the same prefix.

So now Koramangala might come out to something like the prefix "tdr1w". D200 might be "tdr1wj" and D205 might be "tdr1wm". To find nearby drivers, I just ask for everyone whose geohash starts with "tdr1w". That's a much simpler, much faster kind of lookup than the rectangle approach, because now it's really just one string comparison instead of two separate range checks.

But there's a real gap here, and it's worth being honest about it. Imagine a third driver, D210, who is standing just 100 metres from D200, genuinely very close, but happens to be positioned just barely on the other side of an invisible cell boundary. Because of exactly where that line falls, D210's geohash comes out completely different, starting with "tdr1x" instead of "tdr1w". My search for "tdr1w" completely misses D210, even though D210 might be closer to R500 than half the drivers I did find. This happens more often than you'd think, right at the edges of these cells. It's a known weakness of geohashing, and it's actually the reason Uber's real system doesn't use plain geohash at all, it uses something called H3, which divides the map into hexagons instead of rectangles. Hexagons don't have this corner problem, because every neighbouring hexagon is exactly the same distance away, there's no sharp edge where a nearby point suddenly looks far away. I wouldn't try to build H3 by hand in an interview, but I'd want to say this out loud, because it shows I understand the limitation, not just the basic idea of geohashing.

### Choosing where this data actually lives

So far I've only talked about how to structure the location data, not where to store it. And this matters a lot, because a normal SQL database is genuinely a bad fit here, even with a good geohash key. The reason is the sheer number of writes. Every driver sends an updated location roughly every five seconds. With a million drivers, that adds up to somewhere around two hundred thousand location updates every single second, system-wide. A traditional SQL database writes every change to a log first, then to disk, then updates its indexes, and all of that work per write is simply too much at this volume. On top of that, rows get locked while being updated, which slows down anyone else trying to read or write at the same time.

So I'd move this specific piece of data into Redis. Redis keeps everything in memory, so reads and writes are extremely fast. It also has built in support for exactly this kind of location data, commands like GEOADD to store a point and GEORADIUS to search nearby points. And there's one more reason this fits well here, it's genuinely fine if we occasionally lose a bit of this data. A driver's location from three seconds ago doesn't matter much once a new update arrives. So I don't need the same durability guarantees here that I'd want for, say, someone's payment history. That's an important distinction to make out loud, not all data in this system needs to be treated the same way.

### Why even Redis needs to be split up

One Redis server sounds great until you think about the actual load. Say a decent chunk of that two hundred thousand updates per second is happening in Bangalore alone, tens of thousands of writes hitting one machine every second, plus every ride request in the city also querying that same machine for nearby drivers. Redis processes commands one at a time on a single thread. So all of that traffic queues up behind each other. Now imagine something like a concert just ending near Koramangala, suddenly there's a burst of both drivers and riders all hitting this one server at once. The queue backs up, and R500's ride request, which needs an answer in under a second, ends up waiting behind a pile of driver location writes. That's exactly the wrong moment for things to slow down.

So we need more than one Redis server. We need a cluster.

### The mistake that's easy to make when clustering

Redis Cluster works by splitting all possible keys into slots, roughly sixteen thousand of them, and spreading those slots across however many machines you have. The very natural first instinct is to decide which slot a driver's data goes into based on their driver id. That's how you'd normally split up data, hash the id, pick a slot.

Let's actually walk through what goes wrong if I do that here. Say D200's id happens to hash into a slot that lives on Machine 1. D205's id happens to hash into a slot that lives on Machine 3. These two drivers are standing less than two kilometres apart in real life, but their data is now sitting on two completely unrelated machines, for a reason that has nothing to do with where they actually are.

Now R500 asks for nearby drivers. The system has no way of knowing in advance which machine holds nearby drivers, because "nearby" and "which machine" have no relationship to each other anymore. So it's forced to ask every single machine in the cluster and combine the answers. If we have eight machines, that's eight separate queries just to answer one ride request. And this happens for every single request. We've technically split the data across machines, but we haven't actually made any single query faster, we've just made every query touch everything.

### The fix, and why it actually works

The fix is simple once you see the problem clearly: don't shard by driver id, shard by location itself. Use the region, or the geohash prefix, to decide which machine a driver's data lives on. So now, every driver whose geohash starts with "tdr1w," meaning they're physically in Koramangala, lands on the same machine, say Machine 2. It doesn't matter what their driver id is.

Now when R500 asks for nearby drivers, the system computes her geohash prefix, looks up which single machine owns that prefix, and sends exactly one query there. D200 and D205 both come back from that one machine, one round trip, no combining results from eight different places. This is the version of sharding that actually delivers on the promise, each machine handles its own slice of the map, and a query only ever needs to touch the one machine responsible for the area it cares about.

The one thing worth keeping in mind with this approach is that when a driver physically moves from one region into another, their record has to move to a different machine too. That's a manageable cost though, because it happens rarely compared to how often location updates happen within a region.

### What happens when a machine dies

Now think about what happens if Machine 2, the one holding every driver in Koramangala including D200 and D205, simply crashes. Without any backup, Koramangala just has no driver data at all until that machine comes back online. Every ride request in that entire area fails to find anyone. That's a real outage, not just a slow response.

So each machine gets one or two replicas, copies of its data sitting on other machines, kept in sync automatically. The way this syncing works matters. The main machine doesn't wait for its replicas to confirm they've received a write before telling the driver's app "update received." If it did wait, every single location update would slow down, and at this volume that would be a real cost. So the copying happens in the background, slightly behind the main machine.

This does mean there's a small window, maybe a couple hundred milliseconds, where if the main machine crashes at exactly the wrong moment, the very latest update or two could be lost before the replica caught up. In this specific case, that's genuinely fine to accept. D200's app is going to send another update within a few seconds regardless, so nobody's decision is really riding on that last fraction of a second of data. The machines also constantly check on each other's health in the background, and if enough of them agree that Machine 2 has actually gone down, one of its replicas is automatically promoted to take over, usually within a few seconds.

I'd also add one more small safety net, completely separate from the replication story. Every driver's location entry should automatically expire after fifteen or thirty seconds if nothing refreshes it. That way, if a driver's app crashes entirely and stops sending updates altogether, their entry just quietly disappears on its own instead of us continuing to send riders toward someone who isn't actually there anymore.

One last point worth making clearly, I would never copy this location data across different parts of the world. There is no reason for Koramangala's driver locations to also exist on a machine sitting in a different country. That data would just sit there unused, wasting space and bandwidth for no benefit.

That closes out the first half of the story. We now know, cheaply and reliably, exactly where D200 and D205 are at any given moment, and a lookup for "who's near R500" touches exactly one healthy machine and comes back fast.

---

## Part Two: Picking A Driver And Getting R500 A Ride

### The simple version, and where it starts to seem fine

R500's request comes in. The system looks up nearby drivers the way we just described, gets back D200 and D205 among others, and simply sorts them by straight line distance. D200 is 800 metres away, D205 is 1.8 kilometres away. D200 wins, gets notified first, and we wait to hear back before trying anyone else.

For this exact moment in the story, this looks completely reasonable. D200 really is closer. But it's about to reveal a real problem.

### Two riders wanting the same driver at once

Now let's bring in a second rider, R501, who's also somewhere in Koramangala, close enough that D200 also looks like her best match, and she happens to request a ride at almost the exact same moment as R500.

Behind the scenes, the system isn't handling one request at a time, it's processing many requests in parallel using multiple workers, because that's the only way to keep up with the request volume. So one worker is handling R500's request, and completely independently, another worker is handling R501's request. Both of them do the same lookup. Both of them arrive at the same conclusion, D200 is the best candidate. Nothing has told either worker that the other one exists or that they've both picked the same driver.

So both workers send D200 a notification. D200's phone now shows two separate ride requests at almost the same time. Say D200 taps accept on R501's request first, and then a second later also taps accept on R500's, because nothing on his screen told him the first one had already claimed him. Now, if nothing prevents this, both rides could end up marked as accepted, and D200 is somehow supposed to be in two places at once. That's the double booking problem, and it's a real consequence of running things in parallel without any coordination between the workers.

### Making sure only one worker can win

The fix has to happen before either worker even sends the notification, not after. Here's how I'd do it. Right before notifying D200, the worker tries to write a very specific kind of entry into Redis, one that only succeeds if that entry doesn't already exist. It says, essentially, claim D200 for ride R500, but only if nobody has already claimed him, and let this claim automatically disappear after fifteen seconds if nothing else happens.

When R500's worker tries this first, it succeeds, because nobody had claimed D200 yet. So it goes ahead and notifies D200 about R500's ride.

A few milliseconds later, R501's worker tries the exact same thing for D200. But this time it fails, because the claim already exists. R501's worker doesn't need D200 to respond to anything at all, it immediately knows D200 is unavailable, and it just moves on to its next candidate, maybe D205, and tries to claim that driver instead.

So D200's phone only ever shows one ride request the whole time, R500's. If D200 never responds within those fifteen seconds, the claim quietly expires on its own and he becomes available again, no extra cleanup work needed. If he explicitly rejects the ride, the claim is removed immediately instead of waiting out the full fifteen seconds, so the next driver in line isn't kept waiting unnecessarily.

This is a really useful pattern to recognise, by the way, it's basically the same idea as reserving the last item in stock during an online checkout. Only one buyer's reservation can succeed, everyone else has to know immediately that it's gone.

### The lake problem

Now let's go back to distance for a second, because straight line distance alone is still going to cause a real problem in this exact story. Remember, D200 is 800 metres away but across a lake with no direct road. D205 is 1.8 kilometres away but on a clear road. If we only ever rank by straight line distance, D200 still wins every time.

D200 accepts the ride, but now has to actually drive there, and because of the lake, the real route is four kilometres long and takes twelve minutes. Meanwhile D205, who looked farther away on paper, could genuinely have reached R500 in about six minutes. R500 ends up waiting twice as long as she needed to, purely because we optimised for the wrong number.

The fix here is to not rely on straight line distance as the final answer, only as a first rough filter. Take the closest ten or so candidates by straight line distance, since that's still a cheap and quick way to narrow things down, and only for those ten, actually ask a real routing service what the driving time would be. Then rank by that instead. In this story, that step would correctly show D205 arriving faster despite being farther away on the map, and he'd get notified first. I'd deliberately keep this to a small number of candidates, not every nearby driver, because calling an outside routing service is usually the slowest and most expensive step in the whole flow, and there's no need to call it more times than necessary.

While I'm ranking, I'd also factor in things like how reliably a driver accepts rides and their rating, not just how fast they can arrive. A driver with a great ETA who reliably rejects offers doesn't actually get R500 into a car any sooner than a slightly slower driver who says yes right away.

### A different way to notify drivers

There's an alternative worth mentioning here, even though I wouldn't lead with it. Instead of notifying one driver and waiting to hear back before trying the next, some systems notify several top candidates all at once, and whichever one accepts first gets the ride, using the exact same claim mechanism from before to make sure only one of them can actually win. This can genuinely reduce how long R500 waits, especially if the top candidate happens to be slow to respond. The trade off is that other drivers occasionally see a ride offer appear and then vanish because someone else grabbed it first, which isn't a great experience if it happens too often, and it does mean sending out more notifications than actually get used. I'd mention this as an option if asked how to reduce waiting time further, but I'd present the one-at-a-time approach as the simpler starting point.

### Where the fare and the time estimate come from

Before any of this matching even happens, R500 saw a fare and an estimated time on her screen before she confirmed the ride. That number comes from a separate service that calls an outside routing provider to get the distance and route, and then works out the fare from that. If a lot of people are asking for roughly the same route around the same time, say a common commute between two busy areas, it's worth briefly caching that routing answer for a few seconds. That way the hundredth person asking for nearly the same trip in the same few seconds doesn't trigger a hundred separate calls to what is usually the slowest external service in the whole system.

### What happens if we make R500's phone just wait

Here's a subtle but important failure to think through. Matching can genuinely take a few seconds sometimes, waiting on the routing service, waiting on a driver to respond. If R500's app made one direct request and simply sat there holding the connection open the entire time, think about what that does to the server handling it. That connection is now tied up for the whole wait. If a burst of a hundred requests all come in around the same time and each one holds a connection open for several seconds, the server can genuinely run out of capacity to accept new requests, and everything slows down for everyone, even requests that have nothing to do with the ones taking a long time. And if that particular server happens to restart or crash while R500's request is still open, her app just gets disconnected with no idea whether a driver was ever found.

### Making the request non-blocking

The fix is to stop treating this as one long request and response. Instead, when R500 submits her request, we immediately create a ride record marked as searching, drop an event describing this request onto a queue, and instantly tell her app, request received, here's your ride id. That's it, that part is done.

Separately, a pool of workers is constantly pulling events off that queue and doing all the matching work described above. If one of those workers happens to crash partway through, the event isn't lost, it's still sitting safely on the queue, and either that worker restarts and picks it back up, or another worker does. And if R500's phone briefly loses signal right after she taps confirm, that's fine too, her request had already made it safely onto the queue before the connection dropped.

One more small but important detail. R500's app should generate a unique id for this request before sending it. If her app happens to retry the request because of a flaky connection, the backend can see it already has a ride under that same id and simply return the existing one instead of accidentally creating a second ride request for the same tap.

### How does R500 actually find out a driver was found

Now that everything's happening in the background, R500's app needs some way to learn when D205 accepts. The simplest approach is to just have her app ask every couple of seconds, are we there yet, is a driver assigned yet. This works, and it's a reasonable starting point, but it's wasteful, most of those checks come back with nothing new, and there's an unavoidable small delay between D205 actually accepting and R500's app happening to ask again.

### Pushing the answer instead of asking for it

A better approach is to keep a connection open between R500's app and the server, and the moment a driver is confirmed, push that information straight down the connection instantly, rather than waiting for her app to ask. On a single server this is simple to picture, R500 connects, the server holds onto that connection, and the moment matching finishes, it writes the answer straight down that same open connection.

But here's where it gets complicated. To handle millions of riders and drivers at once, we obviously can't have just one server holding every connection. So R500 might be connected to one particular server, call it Server A, while the worker that just finished matching her ride is running on a completely different machine that has no direct link to Server A at all. That worker has no way to reach into Server A and push a message down R500's specific connection.

### A dedicated layer just for holding connections

So I'd pull connection handling out into its own separate group of servers, whose only job is to hold open connections and pass along whatever they're told to pass along. No matching logic lives here at all. R500 connects to Server A. D205, completely separately, is connected to Server B. Somewhere in Redis, we keep a simple lookup, this rider is on this server, this driver is on that server, and it gets updated every time someone connects, disconnects, or reconnects.

Now when the matching worker needs to tell D205 about a ride offer, it looks up where D205 is currently connected, finds Server B, and needs a way to get the message specifically to that one server.

### The mistake of telling everyone

The easy but wrong way to do this is to have every single one of these connection-holding servers listen to one shared stream of messages, and have each server check whether any message happening to pass by is meant for one of its own connections. Think about what that means at real scale. If there are two hundred of these servers running, then every single message, every driver notification, every rider update, across the entire platform, gets delivered to all two hundred servers, and one hundred and ninety nine of them look at it, realise it's not relevant to them, and throw it away. That's two hundred times more delivery work than actually necessary, and it only gets worse as we add more servers, which is exactly backwards, adding more servers should make the system handle more load, not create more wasted work.

The right way is to send each message directly and only to the one server that actually needs it. When the worker looks up that D205 is on Server B, it sends the notification specifically to a channel that only Server B is listening to. Server B receives it, and only Server B, and pushes it straight down to D205's phone. One message, one delivery, no matter how many servers exist in total.

### Should the connection server also handle location updates directly

D205 is also sending his location updates every five seconds over this same open connection to Server B. The question is, should Server B just write that straight into the Redis location cluster from Part One, or should it publish it as an event first, the same way we handled R500's ride request.

Early on, when things are still simple, writing directly is completely fine, there's no need to add more moving parts than the problem calls for yet. But once this whole connection-holding layer exists, I'd route location updates through a queue too, for a few good reasons. If Server B writes directly, it now needs to know exactly how the Redis location cluster is organised, which machine handles which region, and it has to maintain connections into that whole cluster. That ties this server, whose entire job was supposed to be just holding connections, into the internal details of a completely different system. If that Redis machine ever has a brief slowdown, Server B either has to wait around or build its own retry logic to handle it. And beyond that, location data isn't only useful for finding nearby drivers, it's also useful for training better time estimates, understanding demand patterns, and catching suspicious activity like GPS spoofing. If Server B writes directly only into the location cluster, none of those other uses get the data unless we bolt on separate direct writes for each one. Routing it through a queue instead means any of those other systems can simply subscribe to the same stream without touching Server B at all. The cost of doing this is one small extra step, adding maybe ten to fifty milliseconds of delay, and against an update that only happens every five seconds anyway, that's not something anyone would notice.

### When one small area gets overwhelmed

Now picture a large concert nearby just finishing. Within about a minute, a huge number of people in that one small area open the app at once, and a cluster of nearby drivers are all updating their location constantly as they circle around looking for pickups. All of that traffic lands on whichever single machine happens to be responsible for that one small area. Compared to every other, calmer part of the city, this one machine is suddenly handling a wildly disproportionate share of the load, while everything else sits comfortably idle. Requests in that area start slowing down noticeably.

The fix has two parts. For places we can reasonably predict will spike, like known stadiums or big venues, we can prepare ahead of time by splitting that specific area into smaller pieces spread across different machines, based on past patterns of demand. For spikes we can't predict in advance, we can spread the load dynamically, by writing the same area's data under a few different variations of the key and having reads check all of them and combine the results, spreading the load across several machines instead of hammering just one. On the matching side specifically, it also helps to widen the search radius gradually instead of using one fixed distance, because a busy area like this usually has plenty of both riders and drivers nearby, so searching a little further out still resolves quickly.

### Keeping regions of the world separate

One more scenario worth walking through briefly. Picture a rider in Delhi requesting a ride. Delhi has its own complete version of everything we've described, its own connection servers, its own matching workers, its own location cluster, entirely separate from Bangalore's or from anything running in another country. That request should never need to travel across the ocean to get answered, there's no reason a Delhi rider being matched to a Delhi driver needs to involve a server on the other side of the world, and doing so would only add delay for no benefit. Each region runs its own complete, self-contained version of this whole system. The only thing that genuinely needs to be shared across regions is account information and past trip history, and even that doesn't need to be perfectly up to date everywhere at every instant, it's fine for it to catch up gradually.

---

## The Parts That Don't Need A Story, Just A Clear Explanation

**The life of a trip.** Once D205 accepts, the ride moves through a clear sequence of states, requested, driver assigned, driver arriving, trip in progress, completed, with cancelled reachable from most of these points along the way. I'd want this modelled explicitly as a defined set of stages, because things like cancellation fees, handling a driver who never shows up, and refunds all depend on knowing exactly which stage the trip was in when something went wrong.

**Where the permanent records live.** Trip history, user profiles, driver profiles, and payment records don't belong in Redis at all, they need a proper durable database, split up by region or by the rider or driver involved, since almost every question we'd ask this database is naturally scoped to one specific person. For this, I'd lean toward a distributed database that supports proper transactions across machines, because payments genuinely need strong correctness guarantees, and this kind of database gives that to me without having to build it myself. A different kind of database, built more for extremely high write volume, is a reasonable alternative for just the trip records specifically, but then the responsibility for getting payment transactions right shifts onto our own application code, which is more work. Either way, this whole layer stays completely separate from the Redis location layer discussed earlier. One is built to accept a little bit of data loss in exchange for speed, the other is built to never lose anything even if it's a bit slower. Trying to use one database to do both of these jobs is usually a sign that something's been forced to fit where it doesn't belong.

**Money, cancellations, pricing, and reviews.** Payment happens after the trip finishes, and it happens in the background, since R500 already received her ride and shouldn't have to wait on a payment provider before she's free to move on with her day. I'd attach a unique identifier to each trip's payment request so that if the payment attempt has to be retried due to a network hiccup, it can never accidentally charge her twice. If R500 cancels early, there might be a small fee involved depending on how much time has passed or how far D205 had already travelled toward her. If D205 simply never shows up, the trip cancels automatically and R500 is placed at the front of the line for the next match attempt. Pricing during busy periods is worked out by comparing how many riders want a ride against how many drivers are available nearby, over a short rolling window of time, and applying a multiplier before R500 confirms, this doesn't need to be perfectly precise right at the edge of one area versus another. Ratings are written once the trip is finished, and the overall average doesn't need to be recalculated instantly, it can happen a little while later in the background since nobody is waiting on it.

**When things go wrong.** If D205 never responds in time, the claim on him automatically expires and the system simply moves to the next candidate, we've already covered this. If a payment fails after the trip is done, the trip still stays marked as completed, and the payment itself is retried separately, with increasing delays between attempts. If one of the connection-holding servers crashes, the riders and drivers who were connected to it simply reconnect to a different, healthy server, and the lookup table updates itself once they do. If some portion of the matching workers go down, since every request is already sitting safely on a queue, whatever workers remain simply keep working through the backlog once more capacity comes back online.

**Updating the system without anyone noticing.** For most of the services here, matching, fare estimation, updating them is straightforward, roll out the new version gradually behind a load balancer. The connection-holding layer needs a bit more care, because it's holding onto live, open connections. Instead of just shutting a server down abruptly, I'd stop sending it any new connections and let its existing ones finish naturally or reconnect elsewhere on their own, so nobody experiences a sudden, hard drop.

---

## A Few Things Worth Having Ready, Even If Not Asked

I'd want to be watching how long matching actually takes in practice, not just assuming it's fast, since that's really the core promise of this whole system. I'd also watch how often drivers successfully receive and respond to notifications, how often connections to the gateway layer are dropping and reconnecting, and how large the backlog on the request queue is getting, since a growing backlog is usually the earliest sign that the matching workers are starting to fall behind.

On the subject of abuse, a driver could try to fake their GPS location to appear closer than they really are, or a rider and driver could work together to fake trips just to collect a referral bonus. A reasonable first check here is simply asking whether someone's reported movement is even physically plausible for a vehicle, that's a fair answer to give if this comes up, without trying to design a full fraud detection system on the spot.

And if asked to sketch out the actual API shape, I'd keep it simple: one endpoint to get a fare and time estimate, one to actually submit and queue the ride request, one to check on a ride's status as a fallback for before the connection is established, the location stream itself coming either through the open connection or a dedicated endpoint, and one for the driver to accept or reject an offer.

---

## How I'd Actually Tell This Story In A Real Interview

I obviously wouldn't walk through every single step out loud the way I just did here, that's for my own preparation. In the actual room, I'd spend the first five minutes on requirements and rough numbers, five minutes sketching the overall shape of the system, then spend the bulk of the time, maybe twenty five minutes, going deep on the two things that matter most, how we track driver locations without falling into the driver-id sharding trap, and how we prevent double booking with the claim mechanism. I'd spend the remaining time on the connection layer and whatever trade-offs the interviewer wants to push on, and I'd leave a small buffer at the end, because interviewers usually have one specific area they want to dig into, and I'd rather follow their lead there than force my own outline through to the end.

If I only had time to get one part of this really solid before walking into an interview, it would be the double booking story, two riders, one driver, both workers racing to claim him, and exactly why the claim mechanism prevents it. That's the single most common follow-up question in almost any version of this problem.
