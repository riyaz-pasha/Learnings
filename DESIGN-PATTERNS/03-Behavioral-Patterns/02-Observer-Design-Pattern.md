# 7. Observer Design Pattern

We've reached the final pattern in your list:

```text
Behavioral
├── Strategy
└── Observer
```

**Observer** is one of the most important behavioral patterns because it appears in:

* event-driven systems
* GUI applications
* messaging
* Spring events
* reactive programming
* notification systems
* domain events
* pub/sub architectures

Its central idea is:

> **When one object's state or event changes, automatically notify all interested objects without tightly coupling the sender to those receivers.**

The easiest mental model:

```text
One thing changes
      ↓
Notify everyone interested
```

---

# 1. Start with the problem

Suppose we have an e-commerce application.

When an order is placed:

```text
Order Placed
    ↓
What should happen?
```

Maybe we need to:

```text
Send email
Send SMS
Update inventory
Create invoice
Notify analytics
Notify shipping system
```

A naive implementation could be:

```java
public class OrderService {

    public void placeOrder(Order order) {

        // save order

        emailService.sendEmail(order);

        smsService.sendSms(order);

        inventoryService.update(order);

        invoiceService.generate(order);

        analyticsService.track(order);
    }
}
```

This works.

But now `OrderService` knows about every interested component.

```text
                   OrderService
                /      |      |      \
               ↓       ↓      ↓       ↓
           Email     SMS  Inventory  Invoice
```

What happens when we add:

```text
PushNotification
FraudDetection
LoyaltyPoints
RecommendationSystem
```

The service keeps growing.

This creates tight coupling.

---

# 2. What is the underlying problem?

The deeper problem is:

> **One object needs to notify multiple other objects when something happens.**

And we don't want the sender to know all the details of the receivers.

We want:

```text
Publisher
    ↓
"Something happened"
    ↓
Interested listeners react
```

That's Observer.

---

# 3. The basic Observer structure

There are two major roles:

### Subject / Publisher

The object whose state changes or which produces an event.

### Observer / Subscriber

An object interested in being notified.

Classic structure:

```text
             SUBJECT
                │
       maintains observers
                │
      ┌─────────┼─────────┐
      ▼         ▼         ▼
 Observer A  Observer B  Observer C
```

When something changes:

```text
Subject
   │
   ├── notify(A)
   ├── notify(B)
   └── notify(C)
```

---

# 4. Basic example: YouTube channel

This is one of the easiest examples.

Suppose:

```text
YouTube Channel
```

has subscribers:

```text
Alice
Bob
Charlie
```

When a new video is published:

```text
Channel
   ↓
New video
   ↓
Notify subscribers
```

The channel doesn't need to know what each subscriber does with the notification.

That's the key.

---

# 5. Observer interface

Let's define:

```java
public interface Observer {

    void update(String message);
}
```

Concrete observers:

```java
public class EmailSubscriber implements Observer {

    @Override
    public void update(String message) {
        System.out.println(
                "Email received: " + message
        );
    }
}
```

```java
public class SmsSubscriber implements Observer {

    @Override
    public void update(String message) {
        System.out.println(
                "SMS received: " + message
        );
    }
}
```

```java
public class PushSubscriber implements Observer {

    @Override
    public void update(String message) {
        System.out.println(
                "Push notification: " + message
        );
    }
}
```

Now all observers share:

```text
update()
```

but each reacts differently.

---

# 6. Subject

Now create the Subject:

```java
public interface Subject {

    void subscribe(Observer observer);

    void unsubscribe(Observer observer);

    void notifyObservers();
}
```

Concrete Subject:

```java
public class YouTubeChannel
        implements Subject {

    private final List<Observer> observers =
            new ArrayList<>();

    private String latestVideo;

    @Override
    public void subscribe(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void unsubscribe(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {

        for (Observer observer : observers) {
            observer.update(latestVideo);
        }
    }

    public void uploadVideo(String title) {

        this.latestVideo = title;

        notifyObservers();
    }
}
```

---

# 7. Using it

Create the channel:

```java
YouTubeChannel channel =
        new YouTubeChannel();
```

Create subscribers:

```java
Observer alice =
        new EmailSubscriber();

Observer bob =
        new SmsSubscriber();

Observer charlie =
        new PushSubscriber();
```

Subscribe:

```java
channel.subscribe(alice);
channel.subscribe(bob);
channel.subscribe(charlie);
```

Upload a video:

```java
channel.uploadVideo("Observer Pattern Explained");
```

Now:

```text
              YouTubeChannel
                    │
                    │ notify
          ┌─────────┼─────────┐
          ▼         ▼         ▼
        Alice      Bob      Charlie
        Email      SMS       Push
```

Each observer receives:

```text
Observer Pattern Explained
```

---

# 8. The crucial benefit

Notice what the channel does **not** know.

It doesn't know:

```text
EmailSubscriber
SmsSubscriber
PushSubscriber
```

It only knows:

```java
Observer
```

This is the important decoupling:

```text
Subject
   ↓
Observer interface
   ↑
   ├── EmailSubscriber
   ├── SmsSubscriber
   └── PushSubscriber
```

The Subject depends on the abstraction.

---

# 9. Registration is dynamic

An Observer system usually allows observers to subscribe and unsubscribe dynamically.

For example:

```java
channel.subscribe(alice);
channel.subscribe(bob);
```

Later:

```java
channel.unsubscribe(bob);
```

Now:

```text
Channel
 ├── Alice ✅
 └── Bob ❌
```

When a new video is uploaded, Bob won't receive the notification.

This dynamic relationship is a defining feature of Observer.

---

# 10. Observer terminology

You should know these terms for interviews.

### Subject

The object being observed.

Also often called:

```text
Publisher
Observable
Event Source
```

### Observer

The object interested in changes.

Also called:

```text
Subscriber
Listener
Consumer
```

### Concrete Subject

Actual implementation maintaining:

```java
List<Observer>
```

### Concrete Observer

Actual subscriber implementation.

---

# 11. The dependency direction

The dependency structure is:

```text
              Subject
                 │
                 │ depends on
                 ▼
              Observer
              interface
                 ▲
         ┌───────┼───────┐
         │       │       │
         ▼       ▼       ▼
      Email     SMS     Push
```

The Subject doesn't directly depend on:

```text
EmailSubscriber
```

That makes the system extensible.

---

# 12. Why is Observer behavioral?

Observer isn't mainly about:

```text
object creation
```

or:

```text
object structure
```

It's about:

```text
communication between objects
```

Specifically:

> One object communicates changes/events to many dependent objects.

So:

```text
Creational → creation
Structural → composition
Behavioral → communication/behavior
```

Observer fits squarely into behavioral patterns.

---

# 13. Push vs Pull model

There are two common ways to notify observers.

## Push model

The Subject sends the data directly:

```java
observer.update(latestVideo);
```

The observer receives everything it needs.

Conceptually:

```text
Subject
   ↓
"Here is the new data."
   ↓
Observer
```

---

## Pull model

The Subject only says:

```java
observer.update();
```

Then the Observer asks the Subject for the latest information.

For example:

```java
public interface Observer {

    void update();
}
```

Then:

```java
public class PriceObserver
        implements Observer {

    private final StockMarket market;

    public PriceObserver(StockMarket market) {
        this.market = market;
    }

    @Override
    public void update() {

        double price =
                market.getCurrentPrice();

        System.out.println(price);
    }
}
```

Conceptually:

```text
Subject
   ↓
"Something changed."
   ↓
Observer
   ↓
"What changed?"
   ↓
Subject
```

---

# 14. Push vs Pull

| Push                               | Pull                                 |
| ---------------------------------- | ------------------------------------ |
| Subject sends data                 | Subject sends notification           |
| Observer receives data immediately | Observer fetches what it needs       |
| Simple for small systems           | Can reduce unnecessary data transfer |
| More coupling to event payload     | More control for observers           |

Neither is universally better.

The right choice depends on the event model.

---

# 15. Real-world example: Stock price

Imagine:

```text
Stock Market
```

changes price.

Observers:

```text
Trading Dashboard
Mobile App
Alert System
Analytics Engine
```

When price changes:

```text
Stock Market
      ↓
   notify()
      ↓
 ┌────┼────────┬────────┐
 ▼    ▼        ▼        ▼
UI   Mobile   Alerts  Analytics
```

Each reacts differently.

The stock market shouldn't know the implementation details of each observer.

---

# 16. Example with stock prices

Subject:

```java
public class StockMarket {

    private final List<Observer> observers =
            new ArrayList<>();

    private double price;

    public void subscribe(Observer observer) {
        observers.add(observer);
    }

    public void unsubscribe(Observer observer) {
        observers.remove(observer);
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {

        this.price = price;

        notifyObservers();
    }

    private void notifyObservers() {

        for (Observer observer : observers) {
            observer.update();
        }
    }
}
```

Observer:

```java
public interface Observer {

    void update();
}
```

One observer:

```java
public class TradingDashboard
        implements Observer {

    private final StockMarket market;

    public TradingDashboard(
            StockMarket market) {
        this.market = market;
    }

    @Override
    public void update() {

        System.out.println(
                "Current price: " +
                market.getPrice()
        );
    }
}
```

Now:

```java
StockMarket market =
        new StockMarket();

TradingDashboard dashboard =
        new TradingDashboard(market);

market.subscribe(dashboard);

market.setPrice(101.50);
```

The dashboard automatically reacts.

---

# 17. Observer solves the "one-to-many" relationship

This is probably the single most important structural concept.

```text
ONE Subject
     │
     ├──> Observer 1
     ├──> Observer 2
     ├──> Observer 3
     └──> Observer N
```

When the Subject changes:

```text
ONE → MANY
```

all registered observers can be notified.

So remember:

> **Observer models a one-to-many dependency.**

---

# 18. Observer vs Pub/Sub

These concepts are closely related but shouldn't be treated as identical.

### Traditional Observer

Usually:

```text
Subject directly maintains Observer references
```

Example:

```java
List<Observer> observers;
```

Then:

```java
observer.update();
```

The Subject knows its observers directly.

---

### Publish/Subscribe

Usually introduces a broker/event bus:

```text
Publisher
    ↓
 Event Bus
    ↓
Subscribers
```

For example:

```text
             Event Bus
           /    |     \
          /     |      \
         ▼      ▼       ▼
      Email    SMS    Analytics
```

The Publisher doesn't usually hold direct references to every subscriber.

So:

```text
Observer
→ direct subject-observer relationship

Pub/Sub
→ intermediary broker/event bus
```

This distinction becomes very important in distributed systems.

---

# 19. Why Pub/Sub is more decoupled

Observer:

```text
Subject
   ↓
Observer references
```

The Subject knows:

```text
"These objects are listening to me."
```

Pub/Sub:

```text
Publisher
   ↓
Message Broker
   ↓
Subscribers
```

Publisher doesn't need to know who ultimately consumes the message.

This is one reason messaging systems can scale across processes and services.

---

# 20. Observer vs Mediator

Another pattern you may encounter later.

### Observer

One-to-many notification:

```text
Subject
 ├── Observer
 ├── Observer
 └── Observer
```

### Mediator

Coordinates communication between multiple components:

```text
Component A ─┐
Component B ─┼──> Mediator
Component C ─┘
```

The Mediator centralizes interaction logic.

So:

```text
Observer → notification
Mediator → coordination
```

---

# 21. Observer vs Chain of Responsibility

Another useful distinction.

### Observer

One event is sent to many observers:

```text
Event
 ├── Observer A
 ├── Observer B
 └── Observer C
```

### Chain of Responsibility

A request moves through a chain:

```text
Request
  ↓
Handler A
  ↓
Handler B
  ↓
Handler C
```

Usually handlers can:

* handle it
* reject it
* pass it onward

So:

```text
Observer
→ broadcast

Chain of Responsibility
→ sequential processing
```

---

# 22. Observer vs Strategy

Since you just learned Strategy:

### Strategy

Choose **one behavior**:

```text
Context
   ↓
Strategy
```

### Observer

Notify **many interested parties**:

```text
Subject
 ├── Observer
 ├── Observer
 └── Observer
```

So:

```text
Strategy
→ one selected algorithm

Observer
→ many subscribers react to an event
```

---

# 23. Observer vs Decorator

Again:

### Decorator

Wrap:

```text
A
 ↓
Decorator
 ↓
A
```

to add behavior.

### Observer

Register:

```text
Subject
 ↓
many observers
```

to receive notifications.

So:

```text
Decorator
→ layered behavior

Observer
→ event notification
```

---

# 24. Observer and the Java standard library

Java has historically provided the classic Observer-related types:

```text
java.util.Observable
java.util.Observer
```

However, these were deprecated and are not the modern approach.

Modern Java applications more commonly use:

```text
custom listener interfaces
events
reactive APIs
framework event mechanisms
```

So in an interview, don't build new applications around `java.util.Observable`.

The **pattern itself** remains highly relevant.

---

# 25. Observer in GUI systems

Observer became famous partly because GUI components naturally produce events.

For example:

```text
Button
   ↓
click
   ↓
listeners
```

You might have:

```text
Button
 ├── LoggingListener
 ├── UIListener
 └── AnalyticsListener
```

When clicked:

```text
button.click()
     ↓
notify listeners
```

Each listener reacts.

That's Observer.

---

# 26. Event listeners are basically Observer-style design

For example:

```java
button.addActionListener(
        event -> System.out.println("Clicked")
);
```

Conceptually:

```text
Button
  ↓
ActionListener
```

The button generates an event.

The listener reacts.

That's the core Observer idea.

---

# 27. Spring's Application Events

This is especially relevant to your Spring Boot preparation.

Spring supports an event mechanism.

Suppose:

```java
public class OrderPlacedEvent {

    private final Long orderId;

    public OrderPlacedEvent(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
```

Publisher:

```java
@Service
public class OrderService {

    private final ApplicationEventPublisher publisher;

    public OrderService(
            ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void placeOrder(Long orderId) {

        // save order

        publisher.publishEvent(
                new OrderPlacedEvent(orderId)
        );
    }
}
```

Listener:

```java
@Component
public class EmailNotificationListener {

    @EventListener
    public void handle(
            OrderPlacedEvent event) {

        System.out.println(
                "Sending email for order " +
                event.getOrderId()
        );
    }
}
```

Another listener:

```java
@Component
public class InventoryListener {

    @EventListener
    public void handle(
            OrderPlacedEvent event) {

        System.out.println(
                "Updating inventory for order " +
                event.getOrderId()
        );
    }
}
```

Now:

```text
                  OrderService
                       │
                       │ publish
                       ▼
              OrderPlacedEvent
                       │
             ┌─────────┼─────────┐
             ▼         ▼         ▼
          Email     Inventory  Analytics
         Listener    Listener    Listener
```

This is **Observer-style event-driven communication**.

---

# 28. Why is this useful in Spring?

Without events:

```java
public void placeOrder() {

    saveOrder();

    sendEmail();

    updateInventory();

    updateAnalytics();
}
```

The service knows all downstream actions.

With events:

```java
public void placeOrder() {

    saveOrder();

    publisher.publishEvent(
            new OrderPlacedEvent(...)
    );
}
```

Now separate components can subscribe.

This can reduce coupling.

---

# 29. Important Spring event nuance

Spring events are not identical to a textbook Observer implementation.

The framework provides:

```text
event publisher
event listeners
application context infrastructure
```

and manages the relationships.

But conceptually:

```text
Publisher
   ↓
Event
   ↓
Many listeners
```

is Observer-like.

For interview purposes:

> "Spring application events implement an Observer-style publish/subscribe mechanism within the application context."

That's a reasonable explanation.

---

# 30. Synchronous vs asynchronous observers

This becomes important in real applications.

Suppose:

```text
Order placed
   ↓
notify observers
```

If the publisher calls each listener synchronously:

```text
OrderService
   ↓
EmailListener
   ↓
InventoryListener
   ↓
AnalyticsListener
```

the publisher waits.

If one listener is slow:

```text
AnalyticsListener → 5 seconds
```

the whole operation may become slow.

---

# 31. Asynchronous notification

We can conceptually have:

```text
OrderService
     ↓
publish event
     ↓
return
```

while listeners process asynchronously.

In Spring, for example, asynchronous event handling can be configured using framework support such as `@Async` together with appropriate application configuration.

Conceptually:

```text
                    Event
                      │
       ┌──────────────┼───────────────┐
       ▼              ▼               ▼
    Email          Inventory       Analytics
   async            async            async
```

This changes the execution semantics.

You now need to think about:

```text
error handling
ordering
threading
transactions
retries
eventual consistency
```

So asynchronous Observer/event systems are more powerful but more complex.

---

# 32. Observer and memory leaks

A classic Observer problem is that the Subject maintains references to observers.

Suppose:

```java
subject.subscribe(observer);
```

but never:

```java
subject.unsubscribe(observer);
```

The Subject may continue holding:

```text
Observer reference
```

even after the rest of the application no longer needs that observer.

This can contribute to memory-retention problems.

This is particularly important with:

```text
long-lived subjects
short-lived observers
```

So subscription lifecycle matters.

---

# 33. Another problem: notification ordering

Suppose:

```text
Observer A
Observer B
Observer C
```

The Subject notifies all three.

What order?

```text
A → B → C
```

or:

```text
C → A → B
```

Does order matter?

If yes, your design should define it explicitly.

A good Observer design generally avoids hidden dependencies like:

```text
B assumes A has already run
```

unless ordering is part of the contract.

---

# 34. Another problem: failure of one observer

Suppose:

```text
Subject
 ├── Observer A ✅
 ├── Observer B ❌ throws exception
 └── Observer C
```

Should C still receive the notification?

A naive implementation:

```java
for (Observer observer : observers) {
    observer.update();
}
```

may stop when B throws.

You could isolate failures:

```java
for (Observer observer : observers) {

    try {
        observer.update();
    } catch (Exception ex) {
        // log failure
    }
}
```

But now you have a design decision:

> Should one observer's failure affect other observers?

Again, this matters in real event-driven systems.

---

# 35. Observer and transactions

This is especially important in Spring backend applications.

Suppose:

```text
Database transaction
    ↓
Save Order
    ↓
Publish OrderPlaced event
```

What if:

```text
Order saved
Event published
Transaction later rolls back
```

You could have listeners reacting to an event representing something that ultimately wasn't committed.

That's a subtle real-world issue.

Spring provides mechanisms such as transaction-bound event listeners to coordinate event handling with transaction phases.

For interview purposes, the important concept is:

> **Publishing an event from transactional code raises questions about when observers should react relative to transaction commit/rollback.**

---

# 36. Domain Events

Observer-style architecture is widely used with **domain events**.

For example:

```text
OrderPlaced
PaymentCompleted
OrderShipped
UserRegistered
InvoiceGenerated
```

A domain event represents:

> Something meaningful happened in the domain.

Then:

```text
OrderPlaced
   ├── Send confirmation
   ├── Update analytics
   ├── Award loyalty points
   └── Start fulfillment
```

The order logic doesn't need to know every consumer.

This can make systems more modular.

---

# 37. In-process events vs distributed events

This distinction is very important in backend interviews.

### In-process

```text
Service
 ↓
Application Event
 ↓
Listeners
```

Everything runs inside the same application.

Examples:

```text
Spring ApplicationEventPublisher
custom Observer
```

### Distributed

```text
Service A
   ↓
Kafka / RabbitMQ / etc.
   ↓
Service B
Service C
Service D
```

Now communication crosses process boundaries.

This is more accurately a **message broker / event-driven architecture**, not merely the classic Observer pattern.

So:

```text
Observer
→ object-level / in-process notification

Message broker
→ distributed event communication
```

The concepts are related but not interchangeable.

---

# 38. Observer and loose coupling

This is the biggest architectural benefit.

Without Observer:

```text
OrderService
 ├── EmailService
 ├── SmsService
 ├── InventoryService
 ├── AnalyticsService
 └── ...
```

With Observer/event model:

```text
OrderService
     ↓
 OrderPlaced
     ↓
 ┌───┼────┬──────┐
 ▼   ▼    ▼      ▼
Email SMS Inventory Analytics
```

The producer says:

> "Order was placed."

It doesn't necessarily need to say:

> "Now send an email, update inventory, calculate analytics, and do X."

That responsibility can be distributed.

---

# 39. Observer and Open/Closed Principle

Suppose you have:

```text
OrderPlaced
```

Initially:

```text
EmailListener
```

Later you add:

```text
AnalyticsListener
```

The `OrderService` doesn't necessarily need modification.

You added a new subscriber.

This can support OCP:

> Existing publisher logic can remain unchanged while new reactions are added through new observers.

---

# 40. Observer and Single Responsibility Principle

Without Observer:

```text
OrderService
├── create order
├── send email
├── update inventory
├── analytics
└── ...
```

With events:

```text
OrderService
→ create order + publish event

EmailListener
→ email

InventoryListener
→ inventory

AnalyticsListener
→ analytics
```

Responsibilities are separated.

---

# 41. Observer doesn't always mean "state change"

This is an important nuance.

The textbook description often says:

> "When the Subject's state changes, notify observers."

But in modern systems, Observer is frequently used for **events**:

```text
OrderPlaced
PaymentCompleted
FileUploaded
UserRegistered
```

The object may not expose a mutable "state" at all.

So a more modern interpretation is:

> **Notify interested parties when something they care about occurs or changes.**

---

# 42. Observer and reactive programming

Reactive systems frequently use concepts related to:

```text
Publisher
Subscriber
Subscription
```

For example, Java's Reactive Streams model includes:

```text
Publisher
Subscriber
Subscription
```

Conceptually:

```text
Publisher
   ↓
stream of events
   ↓
Subscriber
```

This resembles Observer but adds richer semantics around:

```text
backpressure
asynchronous processing
streams
subscription lifecycle
```

So:

```text
Observer
→ basic notification model

Reactive Streams
→ richer asynchronous stream-processing model
```

---

# 43. Backpressure

In classic Observer:

```text
Subject
   ↓
notify
notify
notify
notify
```

What if events arrive faster than observers can process them?

Classic Observer doesn't inherently solve this.

Reactive systems add mechanisms for handling producer/consumer speed differences.

This is called:

> **Backpressure**

You don't need to implement it for classic Observer, but recognizing this distinction is useful in senior interviews.

---

# 44. A better modern event model

Instead of:

```java
observer.update(String message);
```

we can define a typed event:

```java
public record OrderPlacedEvent(
        Long orderId,
        Long customerId
) {
}
```

Observer:

```java
public interface EventListener<T> {

    void onEvent(T event);
}
```

Then:

```java
public class EmailOrderListener
        implements EventListener<OrderPlacedEvent> {

    @Override
    public void onEvent(
            OrderPlacedEvent event) {

        System.out.println(
                "Email order " + event.orderId()
        );
    }
}
```

Typed events make the contract clearer.

---

# 45. Observer with multiple event types

A system may have:

```text
OrderPlaced
PaymentCompleted
OrderShipped
```

Different observers subscribe to different events.

Conceptually:

```text
OrderPlaced
 ├── Email
 ├── Inventory
 └── Analytics

PaymentCompleted
 ├── Receipt
 └── Analytics

OrderShipped
 ├── Notification
 └── Tracking
```

This begins to look much more like a real event-driven application.

---

# 46. Common mistake: Observer becoming a god mechanism

It's tempting to publish events for everything:

```text
UserCreated
NameChanged
PhoneChanged
AddressChanged
EmailChanged
...
```

But too many events can make the system difficult to understand.

Now a developer changes something and has to search the entire application to figure out:

```text
Who listens to this event?
What side effects happen?
In what order?
Are they synchronous?
Are they transactional?
```

So events improve decoupling but can reduce discoverability.

This is a classic trade-off.

---

# 47. Observer can hide control flow

This is perhaps the biggest downside.

Normal code:

```java
placeOrder();

sendEmail();
updateInventory();
```

makes the flow visible.

Event-driven code:

```java
placeOrder();
```

might trigger:

```text
Event
 ↓
Email
Inventory
Analytics
Loyalty
Fraud
...
```

The behavior is distributed.

That's powerful, but debugging and understanding the complete flow can become harder.

---

# 48. Another downside: cascading events

One observer might publish another event:

```text
OrderPlaced
   ↓
PaymentListener
   ↓
PaymentCompleted
   ↓
InvoiceListener
   ↓
InvoiceGenerated
   ↓
NotificationListener
```

Now you have an event chain.

This can be perfectly valid, but it needs careful design.

Otherwise you can create:

```text
hard-to-trace event cascades
```

---

# 49. Observer vs synchronous direct calls

Direct dependency:

```java
orderService.placeOrder();
emailService.sendEmail();
```

is explicit.

Observer/event:

```java
orderService.placeOrder();
```

followed by event listeners.

Event-based design gives:

```text
lower coupling
```

but can reduce:

```text
explicitness
```

So Observer isn't automatically "better."

It's appropriate when multiple components independently care about something happening.

---

# 50. When should you use Observer?

Think Observer when you hear:

```text
"notify all interested components"
"one-to-many relationship"
"listeners"
"subscriptions"
"event occurred"
"state changed"
"automatically notify"
```

Typical examples:

```text
GUI events
Stock price updates
Order events
Notifications
Domain events
Application events
Monitoring systems
Event listeners
```

---

# 51. When should you NOT use Observer?

Don't use Observer just to avoid every method call.

If:

```text
A must directly call B
```

and the relationship is:

```text
mandatory
simple
synchronous
strongly ordered
```

a normal method call may be clearer.

For example:

```java
paymentService.validatePayment();
```

doesn't need to become:

```text
PaymentValidationRequested event
```

unless there is a real architectural reason.

---

# 52. Common interview question: "What problem does Observer solve?"

Strong answer:

> "Observer establishes a one-to-many relationship where multiple objects can subscribe to a subject and be notified when a relevant state change or event occurs. It reduces direct coupling between the publisher and its consumers."

---

# 53. "What are the participants?"

Answer:

> "The main participants are the Subject, which maintains and notifies observers, the Observer abstraction, and the concrete observers that react to notifications."

Diagram:

```text
Subject
   │
   ├── Observer A
   ├── Observer B
   └── Observer C
```

---

# 54. "What is push vs pull?"

Strong answer:

> "In a push model, the subject sends event data to observers as part of the notification. In a pull model, the subject only indicates that something changed and observers query the subject for the information they need."

---

# 55. "Observer vs Pub/Sub?"

Strong answer:

> "Classic Observer typically has a direct subject-to-observer relationship, while publish/subscribe generally introduces an intermediary such as an event bus or message broker. Pub/sub therefore tends to provide stronger decoupling and can extend across process boundaries."

---

# 56. "Observer vs Strategy?"

Answer:

> "Strategy encapsulates interchangeable algorithms and usually selects one behavior. Observer is a one-to-many notification mechanism where multiple subscribers react to an event or state change."

---

# 57. "Observer vs Mediator?"

Answer:

> "Observer is primarily about notifying interested observers, while Mediator centralizes and coordinates communication between multiple components."

---

# 58. "Can Observer be asynchronous?"

Yes.

The classic pattern doesn't inherently require synchronous execution.

You can build:

```text
Subject
   ↓
Event queue
   ↓
Observers
```

or use framework/event infrastructure.

But once you introduce asynchronous processing, you must consider:

```text
threading
ordering
retries
failures
transaction boundaries
eventual consistency
```

---

# 59. "What are the disadvantages?"

A strong answer:

> "Observer reduces coupling but can make control flow less explicit. Notification ordering, observer failures, subscription lifecycle, memory retention, and debugging can become concerns, especially when many observers or asynchronous event handling are involved."

---

# 60. Practical Spring Boot example

Here's a complete picture.

### Event

```java
public record OrderPlacedEvent(
        Long orderId) {
}
```

### Publisher

```java
@Service
public class OrderService {

    private final ApplicationEventPublisher publisher;

    public OrderService(
            ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void placeOrder(Long orderId) {

        // persist order

        publisher.publishEvent(
                new OrderPlacedEvent(orderId)
        );
    }
}
```

### Email observer

```java
@Component
public class EmailNotificationListener {

    @EventListener
    public void handle(OrderPlacedEvent event) {

        System.out.println(
                "Sending email for order " +
                event.orderId()
        );
    }
}
```

### Analytics observer

```java
@Component
public class AnalyticsListener {

    @EventListener
    public void handle(OrderPlacedEvent event) {

        System.out.println(
                "Tracking order " +
                event.orderId()
        );
    }
}
```

The result:

```text
                       OrderService
                            │
                            │ publish
                            ▼
                     OrderPlacedEvent
                            │
                 ┌──────────┴──────────┐
                 ▼                     ▼
        EmailNotification         Analytics
            Listener               Listener
```

The publisher doesn't need to explicitly invoke either listener.

That's the Observer/event-driven idea.

---

# 61. Observer + Factory

These can work together.

For example:

```text
Event
 ↓
Factory
 ↓
create appropriate handler
 ↓
execute handler
```

Factory handles creation/selection.

Observer handles notification.

Different responsibilities.

---

# 62. Observer + Strategy

An event handler itself could use Strategy.

For example:

```text
OrderPlacedEvent
       ↓
NotificationListener
       ↓
NotificationStrategy
    ├── Email
    ├── SMS
    └── Push
```

Now:

```text
Observer
→ tells us that something happened

Strategy
→ tells us how to react
```

This is a very natural combination.

---

# 63. Observer + Decorator

An Observer itself can be decorated.

For example:

```text
OrderListener
     ↓
LoggingDecorator
     ↓
MetricsDecorator
```

Again, patterns are not mutually exclusive.

Real systems often compose several patterns.

---

# 64. Observer + Singleton

An event bus might be implemented as a single application-wide component.

But don't automatically make every Subject a Singleton.

Singleton:

```text
one instance
```

Observer:

```text
one-to-many notification
```

Different concerns.

---

# 65. A complete Observer implementation

Let's create a clean generic example.

### Observer

```java
public interface Observer<T> {

    void update(T event);
}
```

### Subject

```java
public interface Subject<T> {

    void subscribe(Observer<T> observer);

    void unsubscribe(Observer<T> observer);

    void notifyObservers(T event);
}
```

### Concrete Subject

```java
public class EventPublisher<T>
        implements Subject<T> {

    private final List<Observer<T>> observers =
            new ArrayList<>();

    @Override
    public void subscribe(Observer<T> observer) {
        observers.add(observer);
    }

    @Override
    public void unsubscribe(Observer<T> observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(T event) {

        for (Observer<T> observer : observers) {
            observer.update(event);
        }
    }
}
```

Usage:

```java
EventPublisher<String> publisher =
        new EventPublisher<>();

publisher.subscribe(
        message -> System.out.println(
                "Email: " + message
        )
);

publisher.subscribe(
        message -> System.out.println(
                "SMS: " + message
        )
);

publisher.notifyObservers(
        "Order placed"
);
```

This is a compact modern Java version.

---

# 66. Java lambdas make Observer very lightweight

Because:

```java
Observer<T>
```

can be a functional interface:

```java
@FunctionalInterface
public interface Observer<T> {

    void update(T event);
}
```

you can write:

```java
publisher.subscribe(
        event -> System.out.println(event)
);
```

No separate class is required for a simple listener.

Again, modern Java often implements classic patterns using language features rather than large class hierarchies.

---

# 67. Observer and functional programming

Java Streams aren't exactly Observer, but the broader programming model increasingly favors:

```text
events
functions
consumers
publishers
subscribers
```

For example:

```java
Consumer<OrderPlacedEvent> listener =
        event -> sendEmail(event);
```

The `Consumer` itself can act like a simple observer callback.

This is another reason understanding the **concept** is more important than memorizing the classic class diagram.

---

# 68. One-to-many vs one-to-one

Compare:

### Normal dependency

```text
A → B
```

### Observer

```text
A → B
  → C
  → D
  → E
```

The important point isn't merely that there are several calls.

It's that:

> **The subscribers can register independently and the publisher notifies all interested parties through a common mechanism.**

---

# 69. The biggest advantage

The publisher can say:

```text
"OrderPlaced happened."
```

instead of:

```text
"Send email."
"Update inventory."
"Track analytics."
"Generate invoice."
"Send SMS."
```

The difference is:

```text
COMMAND-STYLE COUPLING
        ↓
"Do these exact things."

vs.

EVENT-STYLE COUPLING
        ↓
"This happened."
```

Observers decide what that event means to them.

This is a powerful architectural distinction.

---

# 70. The biggest trade-off

The same thing that makes Observer powerful can make it difficult:

```text
Publisher
   ↓
Event
   ↓
Unknown number of listeners
```

The publisher doesn't know all the consequences anymore.

So you gain:

```text
decoupling
extensibility
modularity
```

but potentially lose:

```text
explicit control flow
discoverability
simple debugging
```

That's the central trade-off to discuss in senior interviews.

---

# 71. Observer mental model

Whenever you hear:

> **"When X happens, many independent components need to react."**

think:

```text
                    EVENT / SUBJECT
                          │
             ┌────────────┼────────────┐
             ▼            ▼            ▼
          Observer      Observer     Observer
             │            │            │
             ▼            ▼            ▼
           Action       Action       Action
```

For example:

```text
OrderPlaced
    │
    ├── SendEmail
    ├── UpdateInventory
    ├── Analytics
    └── LoyaltyPoints
```

---

# 72. Final pattern comparison

You've now completed all seven patterns.

| Category   | Pattern   | Main idea                                  |
| ---------- | --------- | ------------------------------------------ |
| Creational | Singleton | Control instance count                     |
| Creational | Factory   | Encapsulate/select object creation         |
| Creational | Builder   | Construct complex/configurable objects     |
| Structural | Adapter   | Make incompatible interfaces work together |
| Structural | Decorator | Add behavior while preserving interface    |
| Behavioral | Strategy  | Encapsulate interchangeable algorithms     |
| Behavioral | Observer  | Notify multiple interested objects         |

A strong memory map:

```text
                         DESIGN PATTERNS
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
      CREATIONAL           STRUCTURAL           BEHAVIORAL
          │                    │                    │
     ┌────┼────┐          ┌────┴────┐          ┌────┴────┐
     ▼    ▼    ▼          ▼         ▼          ▼         ▼
  Singleton Factory Builder      Adapter     Decorator Strategy Observer
     │      │      │               │            │       │       │
  HOW     WHICH   HOW TO         COMPAT-      ADD      CHOOSE   NOTIFY
  MANY?   OBJECT? BUILD?         IBILITY     BEHAVIOR  ALGORITHM MANY
```

And an even simpler interview memory aid:

```text
Singleton
→ HOW MANY?

Factory
→ WHICH OBJECT?

Builder
→ HOW TO BUILD?

Adapter
→ HOW TO CONNECT?

Decorator
→ HOW TO EXTEND?

Strategy
→ WHICH BEHAVIOR?

Observer
→ WHO SHOULD KNOW?
```

---

# 73. The seven patterns as real backend problems

This is where I'd recommend you connect the patterns to actual Java/Spring development:

```text
Singleton
    ↓
Application-scoped shared component

Factory
    ↓
Choose payment/shipping/provider implementation

Builder
    ↓
Build complex immutable request/domain object

Adapter
    ↓
Integrate third-party/legacy API

Decorator
    ↓
Add logging/caching/metrics/retry around a service

Strategy
    ↓
Choose pricing/payment/tax/shipping algorithm

Observer
    ↓
React to OrderPlaced/PaymentCompleted/etc.
```

For a Spring Boot interview, this practical mapping is much more useful than just memorizing GoF diagrams.

---

# 74. What you should now be able to explain

For **Observer**, make sure you can answer:

```text
1. What problem does Observer solve?
2. What is the one-to-many relationship?
3. Subject vs Observer
4. Push vs Pull model
5. How subscribe/unsubscribe works
6. Observer vs Pub/Sub
7. Observer vs Mediator
8. Observer vs Strategy
9. Observer vs Decorator
10. Synchronous vs asynchronous notification
11. Memory-leak/subscription concerns
12. Failure handling
13. Event ordering
14. Transaction/event interaction
15. Domain events
16. Spring Application Events
17. In-process events vs distributed messaging
18. Advantages and disadvantages
```

---

# Final takeaway

The most important sentence to remember is:

> **Observer defines a one-to-many relationship where a subject/publisher notifies registered observers/subscribers when something relevant happens or changes.**

And the key distinction from Strategy is:

```text
Strategy
→ "Which behavior should I use?"

Observer
→ "Who needs to know that something happened?"
```

You've now completed the **entire seven-pattern course** from your original list.

---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---
---

# Observer Design Pattern in Java

## 1. Start with a Real-Life Story

You subscribe to a YouTube channel and press the bell icon. When the creator uploads a video, you get a notification.

- The creator does **not** know who you are personally.
- The creator does **not** call each fan one by one.
- You can subscribe or unsubscribe anytime.

That is the Observer pattern:

```
   YouTube Channel  ──(new video!)──►  Fan 1
    (Subject)       ──(new video!)──►  Fan 2
                    ──(new video!)──►  Fan 3
```

**One-line definition:** *Observer defines a one-to-many relationship. When one object (the Subject) changes state, all objects that depend on it (the Observers) are notified automatically.*

---

## 2. Why Do We Need It? (The Problem)

Imagine a weather station. When the temperature changes, three screens must update: a phone app, a website, and a TV display.

### The naive (bad) way

```java
class WeatherStation {
    private PhoneApp phone = new PhoneApp();
    private Website website = new Website();
    private TvDisplay tv = new TvDisplay();

    void setTemperature(int temp) {
        phone.show(temp);
        website.show(temp);
        tv.show(temp);
    }
}
```

```
        ┌──────────────────┐
        │  WeatherStation  │
        └────────┬─────────┘
   knows ┌───────┼────────┐ knows
         ▼       ▼        ▼
     PhoneApp  Website  TvDisplay
```

### What is wrong here?

| Problem | Why it hurts |
|---|---|
| **Tight coupling** | WeatherStation knows every concrete display class |
| **Breaks Open/Closed Principle** | To add a `SmartWatch`, you must **edit** WeatherStation |
| **No runtime flexibility** | You cannot add or remove a display while the app is running |
| **Hard to test** | You cannot test WeatherStation without the real displays |
| **Hard to reuse** | WeatherStation is stuck with those three displays |

### Another bad way: polling

Each display keeps asking "Has the temperature changed yet?" every second. This wastes CPU and gives delayed updates.

```
Display: "Changed?"  Station: "No"
Display: "Changed?"  Station: "No"
Display: "Changed?"  Station: "No"
Display: "Changed?"  Station: "Yes, 30°C"   ← too late, wasted calls
```

---

## 3. When Do We Need It?

Use Observer when:

1. One object changes, and **many others must react**.
2. You **don't know in advance** how many objects need the update.
3. The set of interested objects **changes at runtime**.
4. You want the two sides to be **loosely coupled**.

**Real-world uses:**
- GUI events (button click → many listeners)
- Notifications (email, SMS, push)
- Stock price alerts
- Event-driven systems (Kafka, Spring Events)
- MVC (Model changes → Views refresh)
- Chat apps (new message → all group members)

---

## 4. How It Solves the Problem

**The trick:** the Subject doesn't know *concrete* observers. It only knows an **interface**.

```
┌─────────────────────┐            ┌───────────────────────┐
│   <<interface>>     │            │    <<interface>>      │
│      Subject        │            │      Observer         │
│─────────────────────│            │───────────────────────│
│ + subscribe(o)      │  keeps a   │ + update(data)        │
│ + unsubscribe(o)    │ list of ──►│                       │
│ + notifyObservers() │            └───────────▲───────────┘
└──────────▲──────────┘                        │ implements
           │ implements                ┌───────┴────────┐
┌──────────┴──────────┐         ┌──────┴─────┐   ┌──────┴─────┐
│   ConcreteSubject   │         │ObserverA   │   │ObserverB   │
│  (has the state)    │         └────────────┘   └────────────┘
└─────────────────────┘
```

### The flow (step by step)

```
1. Observer ──subscribe()──────────────► Subject   (registers itself)
2. Subject's state changes
3. Subject ──notifyObservers()
                 ├──update()──► Observer A
                 ├──update()──► Observer B
                 └──update()──► Observer C
4. Observer ──unsubscribe()────────────► Subject   (leaves anytime)
```

**Result:**
- Add a new observer → **no change** in the Subject.
- The Subject depends only on the `Observer` interface (loose coupling).
- Updates are **pushed** instantly. No polling.

---

## 5. Example 1: YouTube Channel (Basic Version)

**Step 1: Observer interface**
```java
public interface Subscriber {
    void update(String videoTitle);
}
```

**Step 2: Subject**
```java
import java.util.ArrayList;
import java.util.List;

public class YouTubeChannel {
    private final String name;
    private final List<Subscriber> subscribers = new ArrayList<>();

    public YouTubeChannel(String name) { this.name = name; }

    public void subscribe(Subscriber s)   { subscribers.add(s); }
    public void unsubscribe(Subscriber s) { subscribers.remove(s); }

    public void uploadVideo(String title) {
        System.out.println("\n[" + name + "] uploaded: " + title);
        notifySubscribers(title);
    }

    private void notifySubscribers(String title) {
        for (Subscriber s : subscribers) {
            s.update(title);
        }
    }
}
```

**Step 3: Concrete observers**
```java
public class Fan implements Subscriber {
    private final String fanName;
    public Fan(String fanName) { this.fanName = fanName; }

    @Override
    public void update(String videoTitle) {
        System.out.println(fanName + " got notification: " + videoTitle);
    }
}
```

**Step 4: Run it**
```java
public class Main {
    public static void main(String[] args) {
        YouTubeChannel channel = new YouTubeChannel("CodeWithAmit");

        Fan ravi = new Fan("Ravi");
        Fan sita = new Fan("Sita");

        channel.subscribe(ravi);
        channel.subscribe(sita);
        channel.uploadVideo("Observer Pattern Explained");

        channel.unsubscribe(ravi);              // Ravi leaves
        channel.uploadVideo("Strategy Pattern Explained");
    }
}
```

**Output**
```
[CodeWithAmit] uploaded: Observer Pattern Explained
Ravi got notification: Observer Pattern Explained
Sita got notification: Observer Pattern Explained

[CodeWithAmit] uploaded: Strategy Pattern Explained
Sita got notification: Strategy Pattern Explained
```

Ravi stopped getting updates after unsubscribing, and the channel code never changed.

---

## 6. Example 2: Weather Station (Push vs Pull Model)

There are two ways to send data to observers.

```
PUSH MODEL                          PULL MODEL
Subject sends the data              Subject only says "I changed"
                                    Observer asks for what it needs

Subject ──update(temp, hum)──► O    Subject ──update()──► O
                                    O ──getTemp()──► Subject
```

| | Push | Pull |
|---|---|---|
| Simple to write | Yes | Slightly more work |
| Observer gets unneeded data | Possible | No, it takes only what it needs |
| Good when | Data is small and same for all | Data is large or observers need different parts |

### Pull-model code

```java
import java.util.*;

interface WeatherObserver {
    void update(WeatherStation station);   // pull: observer gets the subject
}

class WeatherStation {
    private final List<WeatherObserver> observers = new ArrayList<>();
    private double temperature;
    private double humidity;

    public void addObserver(WeatherObserver o)    { observers.add(o); }
    public void removeObserver(WeatherObserver o) { observers.remove(o); }

    public void setMeasurements(double temp, double hum) {
        this.temperature = temp;
        this.humidity = hum;
        for (WeatherObserver o : observers) o.update(this);
    }

    public double getTemperature() { return temperature; }
    public double getHumidity()    { return humidity; }
}

class PhoneApp implements WeatherObserver {
    public void update(WeatherStation s) {
        System.out.println("Phone: Temp is " + s.getTemperature() + "°C");
    }
}

class RainAlert implements WeatherObserver {
    public void update(WeatherStation s) {
        if (s.getHumidity() > 80)
            System.out.println("RainAlert: High humidity! Carry an umbrella.");
    }
}

public class WeatherDemo {
    public static void main(String[] args) {
        WeatherStation station = new WeatherStation();
        station.addObserver(new PhoneApp());
        station.addObserver(new RainAlert());

        station.setMeasurements(28, 60);
        station.setMeasurements(24, 90);
    }
}
```

**Output**
```
Phone: Temp is 28.0°C
Phone: Temp is 24.0°C
RainAlert: High humidity! Carry an umbrella.
```

`PhoneApp` only cares about temperature, and `RainAlert` only cares about humidity. Each pulls what it needs.

---

## 7. Example 3: Stock Price Alerts (Modern Java with Lambdas)

`Observer` has just one method, so it works as a **functional interface**. We can skip writing many small classes.

```java
import java.util.*;

@FunctionalInterface
interface PriceListener {
    void onPriceChange(String symbol, double oldPrice, double newPrice);
}

class Stock {
    private final String symbol;
    private double price;
    private final List<PriceListener> listeners = new ArrayList<>();

    Stock(String symbol, double price) {
        this.symbol = symbol;
        this.price = price;
    }

    void addListener(PriceListener l)    { listeners.add(l); }
    void removeListener(PriceListener l) { listeners.remove(l); }

    void setPrice(double newPrice) {
        if (newPrice == price) return;          // notify only on real change
        double old = price;
        price = newPrice;
        listeners.forEach(l -> l.onPriceChange(symbol, old, newPrice));
    }
}

public class StockDemo {
    public static void main(String[] args) {
        Stock infy = new Stock("INFY", 1500);

        // Observer 1: logger
        infy.addListener((sym, oldP, newP) ->
            System.out.println("LOG: " + sym + " " + oldP + " -> " + newP));

        // Observer 2: alert if price drops more than 5%
        infy.addListener((sym, oldP, newP) -> {
            double dropPercent = (oldP - newP) / oldP * 100;
            if (dropPercent > 5)
                System.out.println("ALERT: " + sym + " fell by " + Math.round(dropPercent) + "%!");
        });

        infy.setPrice(1520);
        infy.setPrice(1400);
    }
}
```

**Output**
```
LOG: INFY 1500.0 -> 1520.0
LOG: INFY 1520.0 -> 1400.0
ALERT: INFY fell by 8%!
```

---

## 8. Example 4: E-Commerce Order Events (Real-World Style)

When an order is placed, the system must send an email, reduce stock, and update analytics.

### Without Observer (tightly coupled)
```java
class OrderService {
    EmailService email; InventoryService inventory; AnalyticsService analytics;

    void placeOrder(Order o) {
        // save order...
        email.sendConfirmation(o);
        inventory.reduceStock(o);
        analytics.track(o);
        // Adding a LoyaltyPointsService? Edit this class again!
    }
}
```

### With Observer (loosely coupled)

```
                          ┌──► EmailService
OrderService ──event──►   ├──► InventoryService
 (publisher)              ├──► AnalyticsService
                          └──► LoyaltyService  (added later, zero change in OrderService)
```

```java
import java.util.*;

interface OrderListener {
    void onOrderPlaced(String orderId, double amount);
}

class OrderService {
    private final List<OrderListener> listeners = new ArrayList<>();

    public void register(OrderListener l) { listeners.add(l); }

    public void placeOrder(String orderId, double amount) {
        System.out.println("Order saved: " + orderId);
        for (OrderListener l : listeners) {
            l.onOrderPlaced(orderId, amount);
        }
    }
}

class EmailService implements OrderListener {
    public void onOrderPlaced(String id, double amt) {
        System.out.println("Email: confirmation sent for " + id);
    }
}

class InventoryService implements OrderListener {
    public void onOrderPlaced(String id, double amt) {
        System.out.println("Inventory: stock reduced for " + id);
    }
}

class LoyaltyService implements OrderListener {
    public void onOrderPlaced(String id, double amt) {
        System.out.println("Loyalty: added " + (int) (amt / 10) + " points");
    }
}

public class ShopDemo {
    public static void main(String[] args) {
        OrderService service = new OrderService();
        service.register(new EmailService());
        service.register(new InventoryService());
        service.register(new LoyaltyService());   // plug in new feature easily

        service.placeOrder("ORD-101", 500);
    }
}
```

**Output**
```
Order saved: ORD-101
Email: confirmation sent for ORD-101
Inventory: stock reduced for ORD-101
Loyalty: added 50 points
```

This is exactly the idea behind **Spring's `ApplicationEventPublisher`**, **Kafka**, and **event-driven microservices**, just at a larger scale.

---

## 9. Observer in Java's Standard Library

Java already uses Observer in many places:

| Where | Subject | Observer |
|---|---|---|
| Swing / AWT | `JButton` | `ActionListener` |
| JavaBeans | `PropertyChangeSupport` | `PropertyChangeListener` |
| Spring | `ApplicationEventPublisher` | `@EventListener` |
| Java 9+ | `Flow.Publisher` | `Flow.Subscriber` (reactive streams) |

### Swing example (you have already used Observer!)
```java
button.addActionListener(e -> System.out.println("Clicked!"));
```
The button is the Subject and the lambda is the Observer.

### Built-in `PropertyChangeListener`
```java
import java.beans.*;

class Person {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String name;

    void addListener(PropertyChangeListener l) { support.addPropertyChangeListener(l); }

    void setName(String newName) {
        String old = this.name;
        this.name = newName;
        support.firePropertyChange("name", old, newName);
    }
}

// usage
Person p = new Person();
p.addListener(evt -> System.out.println(
    evt.getPropertyName() + ": " + evt.getOldValue() + " -> " + evt.getNewValue()));
p.setName("Ravi");   // prints: name: null -> Ravi
```

> ⚠️ **Note:** `java.util.Observable` and `java.util.Observer` are **deprecated since Java 9**. Avoid them in new code. Reasons: `Observable` is a class (not an interface, so you lose inheritance), it is not type-safe (the argument is `Object`), and it is not fully thread-safe. Write your own interface or use `PropertyChangeListener`.

---

## 10. Common Pitfalls (Good Interview Material)

### 1. Memory leak (the "Lapsed Listener" problem)
If an observer never unsubscribes, the Subject holds a reference forever. The garbage collector can't clean it up.

```
Subject ──list──► Observer (you forgot to unsubscribe)
                  Observer is "dead" for you but alive in memory
```
**Fix:** always unsubscribe, or use `WeakReference`.

### 2. `ConcurrentModificationException`
If an observer unsubscribes *during* notification, the loop breaks.
```java
for (Observer o : observers) { o.update(); }  // o.update() calls remove() → 💥
```
**Fix:** iterate over a copy, or use `CopyOnWriteArrayList`.
```java
private final List<Observer> observers = new CopyOnWriteArrayList<>();
```

### 3. One bad observer breaks all
If one observer throws an exception, the rest never get notified.
```java
for (Observer o : observers) {
    try { o.update(data); }
    catch (Exception e) { log(e); }   // isolate failures
}
```

### 4. Slow observers block the Subject
Notification is **synchronous** by default. If one observer takes 5 seconds, everyone waits.
**Fix:** notify using an `ExecutorService` (async).

### 5. Notification order is not guaranteed
Never write code that depends on which observer runs first.

### 6. Update loops
If Observer A changes the Subject inside `update()`, that triggers another notification, which can loop forever.

---

## 11. Pros and Cons

| ✅ Pros | ❌ Cons |
|---|---|
| Loose coupling | Memory leaks if not unsubscribed |
| Follows Open/Closed Principle | Unexpected update chains and hard-to-debug flow |
| Add and remove observers at runtime | Random notification order |
| Supports broadcast communication | Can be slow with many observers |

**When NOT to use it:**
- Only one receiver is needed. A simple method call or callback is enough.
- Strict order or guaranteed delivery is needed. Use a message queue.
- Flow must be easy to trace step by step.

---

## 12. How to Explain This in an Interview

### The 30-second answer
> "Observer is a behavioral design pattern that defines a one-to-many dependency. When the Subject's state changes, all registered Observers are notified automatically. It gives loose coupling, because the Subject only knows the Observer *interface*, not the concrete classes. A real example is a YouTube channel and its subscribers, or a button and its click listeners in Swing."

### The 2-minute structured answer (use this order)

```
1. PROBLEM   →  "Many objects need to react to one object's change,
                 but hard-coding them creates tight coupling."
2. SOLUTION  →  "Subject keeps a list of Observers behind an interface.
                 On change, it loops and calls update()."
3. STRUCTURE →  "Subject: subscribe / unsubscribe / notify.
                 Observer: update()."
4. EXAMPLE   →  "Order placed → email, inventory, loyalty services react."
5. TRADE-OFF →  "Watch for memory leaks, thread safety, and slow observers."
```

### Follow-up questions and short answers

**Q: Which design principle does it follow?**
Open/Closed Principle (add observers without changing the Subject) and Loose Coupling. It also supports Dependency Inversion, since the Subject depends on an abstraction.

**Q: Observer vs Pub-Sub?**
```
Observer:  Subject ─────directly────► Observer
           (Subject knows its observers)

Pub-Sub:   Publisher ──► [Message Broker / Event Bus] ──► Subscriber
           (They don't know each other at all)
```
Pub-Sub adds a middle layer (broker). It is more decoupled and can work across processes or machines, like Kafka or RabbitMQ.

**Q: Push vs Pull model?**
Push sends the data in `update(data)`. Pull sends only a signal and the observer calls getters on the Subject. Pull is more flexible, and push is simpler.

**Q: Is Observer thread-safe?**
Not by default. Use `CopyOnWriteArrayList`, synchronize registration, and consider async notification.

**Q: Why is `java.util.Observable` deprecated?**
It is a class (not an interface), it is not type-safe, it has limited thread safety, and better alternatives exist (`PropertyChangeListener`, `Flow`, custom interfaces).

**Q: Observer vs Mediator?**
Observer is one-to-many broadcast. Mediator centralizes many-to-many communication in one object.

**Q: Where have you used it?**
Answer with a real story: "In my project, when an order was placed, we published an `OrderPlacedEvent` using Spring's `ApplicationEventPublisher`, and separate `@EventListener` classes handled email, inventory, and analytics."

---

## 13. Quick Cheat Sheet

```
┌───────────────────────────────────────────────────────┐
│ OBSERVER PATTERN                                      │
├───────────────────────────────────────────────────────┤
│ Type      : Behavioral                                │
│ Intent    : One-to-many automatic notification        │
│ Actors    : Subject (Publisher), Observer (Subscriber)│
│ Key idea  : Subject depends on an INTERFACE only      │
│ Methods   : subscribe(), unsubscribe(), notify()      │
│ Java use  : Swing listeners, Spring events, Flow API  │
│ Watch out : memory leaks, thread safety, slow observers│
│ Remember  : "Don't call us, we'll call you"           │
└───────────────────────────────────────────────────────┘
```

---

## 14. Practice Tasks

1. **Easy:** Build a `NewsAgency` that notifies `Newspaper` and `TvChannel` observers.
2. **Medium:** Add an `unsubscribe` feature to the stock example, and make it thread-safe with `CopyOnWriteArrayList`.
3. **Hard:** Build a generic `EventBus` where observers subscribe to specific event types (`OrderPlaced`, `PaymentFailed`).

If you'd like, I can turn this into a downloadable Markdown or Word file, or walk through the solution to any of the practice tasks.
