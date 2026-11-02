# The Saga Pattern

In microservices architecture, a single transaction often spans multiple services. However, microservices usually don't share a database (each has its own database to ensure loose coupling). This makes traditional ACID (Atomicity, Consistency, Isolation, Durability) transactions impossible because you can't wrap operations across different databases into one single transaction block.

This is where the **Saga Pattern** comes in.

## What is a Saga?

A Saga is a sequence of local transactions. Each local transaction updates the database and publishes a message or event to trigger the next local transaction in the saga.

If a local transaction fails (for example, due to a business rule violation like insufficient funds), the saga executes a series of **compensating transactions** to undo the changes made by the preceding local transactions.

## Real-Life Example: Booking a Vacation

Imagine you are booking a complete vacation package online that includes:
1. Reserving a flight.
2. Reserving a hotel.
3. Renting a car.

In a monolithic application, this might all happen in one database transaction. If any part fails, the entire transaction rolls back. 

In a microservices architecture, these are handled by three separate services:
1. `Flight Service`
2. `Hotel Service`
3. `Car Rental Service`

### 1. The "Happy Path" (Everything goes right)

1. You click "Book Vacation".
2. **Flight Service** reserves a seat and publishes `FlightReservedEvent`.
3. **Hotel Service** listens to the event, reserves a room, and publishes `HotelReservedEvent`.
4. **Car Rental Service** listens to the event, reserves a car, and publishes `CarReservedEvent`.
5. Your vacation is completely booked!

### 2. The "Failure Path" (Using Compensating Transactions)

What happens if the flight and hotel are reserved successfully, but there are no rental cars left?

1. You click "Book Vacation".
2. **Flight Service** reserves a seat and publishes `FlightReservedEvent`.
3. **Hotel Service** listens, reserves a room, and publishes `HotelReservedEvent`.
4. **Car Rental Service** tries to reserve a car but fails (no cars available). It publishes a `CarReservationFailedEvent`.
5. Now, the Saga must undo what was already done. It triggers **compensating transactions**:
    * **Hotel Service** receives the failure event and *cancels* the hotel room reservation.
    * **Flight Service** receives the failure event and *cancels* the flight seat reservation.
6. You receive a message: "Sorry, your booking could not be completed."

## Saga Implementation Approaches

There are two primary ways to coordinate a saga:

### 1. Choreography (Event-Driven)
There is no central coordinator. Each service publishes an event after it completes its local transaction, and other services listen to these events to trigger their own transactions.
* **Pros:** Simple for a few services, low coupling.
* **Cons:** Hard to track the overall flow as the saga gets complex (it can become a "spaghetti" of events).

### 2. Orchestration (Command-Driven)
A central "Orchestrator" service tells the participating services what local transactions to execute. The Orchestrator manages the entire state of the saga.
* **Pros:** Great for complex sagas, easy to see the workflow in one place.
* **Cons:** Adds a new component (the Orchestrator), which can become a central point of failure or overly complex logic.

## Relation to Your Banking System

You added a comment `/* saga step 1 - deduct balance */` in the `AccountController`.

If a user is transferring money between two accounts that live in different services (or processing a payment), a Saga would look like this:
1. **Account Service**: Deduct the balance. Publish `BalanceDeductedEvent`.
2. **Payment/Transfer Service**: Try to deposit the money to the receiver.
3. If the deposit fails (e.g., receiver account doesn't exist), publish a `DepositFailedEvent`.
4. **Account Service**: Listen to `DepositFailedEvent` and execute a **compensating transaction** to refund the money back to the sender's account.
