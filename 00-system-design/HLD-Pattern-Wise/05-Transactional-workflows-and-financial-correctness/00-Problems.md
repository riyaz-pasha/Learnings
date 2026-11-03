# Transactional workflows and financial correctness — HLD interview roadmap

This is an important system design pattern because it tests much more than designing APIs, databases, queues, and microservices. It tests whether you can design a system that remains correct when operations are retried, requests arrive concurrently, services fail halfway through a workflow, and external systems return ambiguous results.

For example:

- A customer must not be charged twice because they clicked Pay twice.
- Two customers must not purchase the last available seat.
- A wallet transfer must not debit the sender without crediting the receiver.
- A refund must not exceed the amount originally captured.
- An order must not be marked as paid when the payment actually failed.
- A subscription must not receive duplicate invoices for the same billing period.
- A bank transfer must eventually reach a clearly defined final state, even when the bank's response is delayed.

The central design question is:

> How do we preserve business and financial invariants across concurrent requests, retries, asynchronous processing, and partial failures?

This is best treated as a family of related HLD problems, not one individual design pattern. The problems below are grouped by the concepts they test, followed by a recommended preparation order.

# 1. Core transactional workflow problems

These are the most important problems to prepare first. They form the foundation for nearly every other problem in this category.

## 1.1 Payment Processing System / Payment Gateway

Problem statement

Design a payment platform that allows customers and businesses to initiate payments using different payment methods, such as credit cards, debit cards, UPI, wallets, and bank transfers.

The platform should integrate with multiple external payment service providers (PSPs), process payment requests, handle asynchronous notifications, and support refunds.

Typical requirements

- Initiate a payment.
- Authorize and capture payments.
- Support multiple payment providers.
- Handle payment success, failure, and pending states.
- Prevent duplicate charges.
- Support refunds and partial refunds.
- Receive and process provider webhooks.
- Route payments to different providers.
- Track payment history and status.

The central correctness challenges

- What happens when the provider charges the customer but your API times out?
- How do you prevent a retry from creating another charge?
- What happens when the payment succeeds but updating your database fails?
- What happens when a webhook is delivered multiple times?
- How do you handle callbacks arriving out of order?
- How do you distinguish a failed payment from a payment with an unknown outcome?

Important concepts to demonstrate

- Idempotency keys.
- Payment state machine.
- Payment authorization versus capture.
- PSP integration and provider abstraction.
- Webhook deduplication.
- Transactional outbox.
- Reconciliation.
- Retry strategies and ambiguous outcomes.

Interview priority: Very high. This is one of the best problems for testing transactional correctness, retries, idempotency, and external-system integration.

## 1.2 E-commerce Checkout and Order Management

Problem statement

Design an e-commerce checkout system where customers can purchase multiple products, apply coupons, pay using different payment methods, and receive order confirmation.

A successful checkout needs to coordinate multiple operations:

1. Validate the cart.
2. Calculate the final price.
3. Validate and reserve inventory.
4. Apply coupons or discounts.
5. Initiate payment.
6. Confirm the order.
7. Trigger fulfillment.

The central correctness challenges

- Payment succeeds, but inventory reservation fails.
- Inventory is reserved, but payment fails.
- Customer submits checkout multiple times.
- Product price changes while checkout is in progress.
- Two customers attempt to purchase the last item.
- An order is created but payment confirmation is delayed.
- Customer cancels an order while payment is processing.

Important concepts to demonstrate

- Order lifecycle and payment lifecycle as separate state machines.
- Inventory reservation.
- Saga orchestration.
- Compensating transactions.
- Checkout idempotency.
- Price snapshots.
- Outbox-driven order events.
- Cancellation and refund workflows.

Why this problem is important

This is an excellent end-to-end example of a distributed transaction. You need to coordinate several services without assuming they can all participate in one database transaction.

## 1.3 Inventory Reservation and Stock Management

Problem statement

Design an inventory system for an e-commerce platform that manages product availability and reserves stock when customers place orders.

The system must prevent overselling, even when thousands of customers attempt to buy the same product simultaneously.

Typical requirements

- Track available and reserved stock.
- Reserve inventory during checkout.
- Confirm reservations after payment.
- Release reservations after payment failure or timeout.
- Support reservation expiration.
- Handle cancellations and returns.
- Support multiple warehouses.

The central correctness challenges

- Two concurrent requests attempt to reserve the last unit.
- A reservation expires while payment is processing.
- A delayed payment-success callback arrives after stock has been released.
- Inventory is reserved twice due to duplicate requests.
- Inventory counts become inconsistent after cancellations or returns.

Important concepts to demonstrate

- Atomic conditional updates.
- Optimistic and pessimistic locking.
- Reservation records.
- TTL-based holds.
- Reservation expiration workers.
- Idempotent reserve, confirm, and release operations.
- Warehouse-level inventory allocation.

Key distinction

Inventory reservation is not the same as order creation. A reservation represents a temporary claim on stock; the order may still fail or be cancelled.

## 1.4 Digital Wallet System

Problem statement

Design a wallet platform where users can maintain balances, add money, send money to other users, and pay merchants.

Typical requirements

- Create user wallets.
- Add money through external payment providers.
- Transfer money between users.
- Pay merchants.
- View current and historical balances.
- Support refunds.
- Support transaction limits.
- Maintain an auditable transaction history.

The central correctness challenges

- A customer spends the same balance through two concurrent requests.
- A transfer debits one wallet but fails before crediting another.
- A top-up succeeds at the PSP but wallet crediting fails.
- A refund is processed multiple times.
- A wallet balance does not match the underlying transactions.

Important concepts to demonstrate

- Double-entry ledger.
- Atomic debit and credit.
- Available versus ledger balance.
- Account-level locking or concurrency control.
- Idempotent transactions.
- Immutable transaction records.
- Balance projections.
- Reconciliation.

Why it is important

A wallet forces you to think about money as a financial record rather than simply a mutable `balance` column.

## 1.5 Peer-to-Peer Money Transfer / Bank Transfer

Problem statement

Design a system that allows one user to transfer money to another user or bank account.

For example, Alice transfers ₹5,000 to Bob.

Typical requirements

- Validate sender and recipient.
- Check account status and transfer limits.
- Verify available balance.
- Debit sender.
- Credit receiver.
- Support internal and external transfers.
- Track pending and completed transfers.
- Support transfer history and notifications.

The central correctness challenges

- Sender is debited, but receiver is not credited.
- Both debit and credit happen twice because of retries.
- The same transfer is submitted concurrently.
- An external bank accepts the transfer but your system times out.
- A transfer is pending for a long time.
- A failed transfer needs to be reversed.

Important concepts to demonstrate

- Atomicity for internal transfers.
- Saga or state-machine workflow for external transfers.
- Double-entry accounting.
- Transaction identifiers.
- Idempotent transfer requests.
- Pending, posted, failed, and reversed states.
- Reconciliation with external banking systems.

Important distinction

An internal wallet-to-wallet transfer may be achievable in one database transaction. An external bank transfer usually crosses a system boundary and needs an asynchronous workflow with explicit handling of uncertain outcomes.

## 1.6 Double-Entry Ledger System

Problem statement

Design a financial ledger that records all money movements across users, merchants, wallets, and the platform itself.

The ledger must ensure that every financial transaction is auditable and that money is neither accidentally created nor lost.

Typical requirements

- Maintain financial accounts.
- Record debits and credits.
- Support deposits, withdrawals, transfers, refunds, and fees.
- Maintain account balances.
- Generate account statements.
- Support transaction reversals.
- Preserve a full audit trail.

The central correctness challenges

- Preventing unbalanced financial transactions.
- Avoiding duplicate postings.
- Preventing modifications to already-posted entries.
- Keeping account balances consistent with ledger entries.
- Handling concurrent postings to the same account.
- Supporting corrections without destroying history.

Important concepts to demonstrate

- Double-entry accounting.
- Immutable journal entries.
- Debit and credit postings.
- Atomic journal posting.
- Balance invariants.
- Ledger entries versus balance projections.
- Reversals instead of destructive updates.
- Account-level concurrency control.

Essential invariant

For each journal transaction in a given currency:

\\[ \text{Total Debits} = \text{Total Credits} \\]

For example, Alice sends ₹1,000 to Bob:

| Account        | Debit  | Credit |
| -------------- | ------ | ------ |
| Alice's wallet | ₹1,000 | —      |
| Bob's wallet   | —      | ₹1,000 |
| Total          | ₹1,000 | ₹1,000 |

The ledger should be the authoritative financial record. A stored balance can be maintained as a performance optimization, but it must remain consistent with the posted ledger activity.

## 1.7 Refund Management System

Problem statement

Design a refund system that allows customers to receive full or partial refunds for previously completed payments.

Typical requirements

- Refund a completed payment.
- Support partial refunds.
- Support multiple partial refunds.
- Process refunds asynchronously through payment providers.
- Track refund status.
- Handle failed refunds.
- Maintain refund history.

The central correctness challenges

- Customer submits the same refund request multiple times.
- Total refunds exceed the original captured amount.
- PSP processes the refund but your system times out.
- Refund succeeds externally but ledger posting fails.
- Two customer-support agents initiate refunds concurrently.
- Order cancellation and refund processing happen simultaneously.

Important concepts to demonstrate

- Refund state machine.
- Refund idempotency.
- Refund amount validation.
- Atomic refund-limit checks.
- Provider-side idempotency.
- Ledger reversal or refund postings.
- Asynchronous processing.
- Reconciliation.

Important business invariant

\\[ \text{Total Successful Refunds} \leq \text{Original Captured Amount} \\]

## 1.8 Subscription Billing System

Problem statement

Design a subscription platform where users subscribe to recurring services and are charged periodically.

Examples include streaming platforms, SaaS applications, and membership services.

Typical requirements

- Create and manage subscriptions.
- Support monthly and annual plans.
- Generate invoices.
- Charge customers automatically.
- Support upgrades and downgrades.
- Handle prorated charges.
- Retry failed payments.
- Suspend or cancel subscriptions.
- Manage trial periods.

The central correctness challenges

- The same subscription receives two invoices for one billing period.
- A billing worker runs twice.
- Payment succeeds, but subscription status is not updated.
- Payment fails repeatedly.
- A user changes plans in the middle of a billing period.
- A cancellation races with a scheduled renewal.
- A payment retry accidentally charges an already-paid invoice.

Important concepts to demonstrate

- Billing cycle management.
- Invoice generation idempotency.
- Subscription state machine.
- Scheduled jobs.
- Payment retries and dunning.
- Proration.
- Entitlement management.
- Invoice and payment separation.

Key distinction

A subscription is a commercial agreement, an invoice is a claim for payment, a payment is a money movement, and an entitlement determines what the customer can access. These should not be represented by one status field.

# 2. Booking and reservation problems

These problems are closely related to checkout and inventory reservation. They are especially useful for understanding limited resources, temporary holds, and distributed workflows.

## 2.1 Flight Ticket Booking System

Problem statement

Design a flight booking system where users search for flights, select seats, reserve tickets, make payments, and receive confirmed bookings.

Typical requirements

- Search available flights.
- Display fares and seat availability.
- Temporarily reserve seats.
- Process payment.
- Confirm a booking.
- Generate tickets.
- Support cancellation and refunds.
- Handle schedule changes.

The central correctness challenges

- Two customers try to book the same seat.
- A seat is held but payment fails.
- Payment succeeds but ticket generation fails.
- A seat hold expires during payment processing.
- A customer is charged but the booking remains pending.
- Flight inventory changes while a customer is checking out.

Important concepts to demonstrate

- Seat inventory.
- Temporary reservation and expiration.
- Booking state machine.
- Fare and seat snapshots.
- Payment orchestration.
- Compensation and cancellation.
- Idempotent ticket generation.

## 2.2 Hotel Reservation System

Problem statement

Design a hotel booking platform where users search for properties, select rooms, reserve them for a date range, and make payments.

Typical requirements

- Search by location and date.
- Check room availability.
- Reserve rooms.
- Process payment.
- Confirm bookings.
- Support cancellations and refunds.
- Handle check-in and check-out.

The central correctness challenges

- Two customers reserve the last room for overlapping dates.
- A room is available when searched but unavailable during checkout.
- A reservation expires while payment is being processed.
- A hotel confirms a booking asynchronously.
- Cancellation policies affect refund amounts.

Important concepts to demonstrate

- Date-range inventory.
- Room allocation.
- Temporary holds.
- Availability consistency.
- Reservation expiration.
- Cancellation policy engine.
- Payment and booking state separation.

## 2.3 Event Ticket Booking System

Problem statement

Design a ticketing platform where users can book tickets for concerts, sports events, movies, or other events.

Typical requirements

- Browse events.
- Select seats or ticket categories.
- Hold seats temporarily.
- Process payments.
- Issue tickets.
- Support cancellations and refunds.
- Prevent duplicate ticket issuance.

The central correctness challenges

- Multiple customers compete for the same seat.
- Flash crowds create extreme contention.
- A seat hold expires during payment processing.
- Duplicate tickets are issued.
- Payment completes after the booking hold expires.

Important concepts to demonstrate

- Seat-level inventory.
- Reservation tokens.
- Hold expiration.
- Queue or waiting-room mechanisms.
- Strong consistency for scarce inventory.
- Idempotent ticket issuance.

What makes this different from ordinary e-commerce

The resource is often unique: a seat can be sold only once. The system must prioritize correctness under intense contention.

## 2.4 Multi-Service Travel Booking System

Problem statement

Design a travel platform where a customer can book a complete trip involving a flight, hotel, and cab.

For example:

- Book a flight.
- Reserve a hotel room.
- Book airport transportation.
- Charge the customer for the complete package.

The central correctness challenges

- Flight booking succeeds but hotel booking fails.
- Flight and hotel succeed but cab booking fails.
- Customer payment succeeds but one provider does not confirm.
- One booking needs to be cancelled because another failed.
- Refund amounts differ across suppliers.
- Suppliers return delayed or ambiguous confirmations.

Important concepts to demonstrate

- Saga orchestration.
- Distributed state machine.
- Compensation actions.
- External supplier integration.
- Booking timeout handling.
- Partial-success management.
- Customer-visible workflow status.

Why it is particularly valuable

This is one of the clearest examples of a distributed business transaction that cannot realistically be implemented as one ACID database transaction.

## 2.5 Car Rental / Vehicle Reservation System

Problem statement

Design a car rental platform where customers can select a vehicle, reserve it for a date range, pay, and collect the vehicle.

The central correctness challenges

- The same vehicle is reserved for overlapping periods.
- Vehicle availability changes due to maintenance.
- Payment succeeds but vehicle allocation fails.
- Reservation expiry and payment callbacks race.
- Cancellation and deposit refunds need to be handled correctly.

Important concepts to demonstrate

- Time-range inventory.
- Vehicle allocation.
- Reservation holds.
- Deposits and additional charges.
- Cancellation and refund workflows.
- State-machine design.

## 2.6 Appointment / Resource Booking System

Problem statement

Design a platform where customers book appointments with doctors, consultants, service providers, or shared resources.

The central correctness challenges

- Two users book the same appointment slot.
- A slot becomes unavailable while payment is happening.
- Appointment cancellation should release the slot.
- Rescheduling needs to reserve a new slot before releasing the old one.
- Payment or deposit status must match the appointment state.

Important concepts to demonstrate

- Slot reservation.
- Atomic slot allocation.
- Hold expiration.
- Rescheduling workflows.
- Payment integration.
- Cancellation and compensation.

# 3. Advanced payments and financial infrastructure problems

These are deeper variations of the core payment and ledger problems. They help you prepare for interviews involving fintech, marketplaces, banking, and large-scale payment platforms.

## 3.1 Card Authorization, Capture, Clearing, and Settlement

Problem statement

Design a card payment processing platform that handles card authorization, capture, clearing, and settlement.

Typical requirements

- Authorize card transactions.
- Place and release authorization holds.
- Capture authorized amounts.
- Support partial capture.
- Support authorization reversals.
- Process clearing records.
- Settle funds with merchants.
- Handle disputes and chargebacks.

The central correctness challenges

- Authorization succeeds, but capture never happens.
- Capture happens more than once.
- Captured amount differs from authorized amount.
- Authorization expires.
- Settlement records do not match internal records.
- Chargebacks arrive long after the original payment.

Important concepts to demonstrate

- Payment lifecycle.
- Authorization holds.
- Capture idempotency.
- Clearing and settlement.
- Reconciliation.
- Financial adjustments.
- Dispute management.

## 3.2 Marketplace Payments and Seller Payouts

Problem statement

Design a marketplace payment platform where customers pay for products or services, and the platform distributes money to multiple sellers.

For example, a customer buys three products from three different sellers in a single checkout.

Typical requirements

- Collect customer payment.
- Calculate platform fees and commissions.
- Split payment across sellers.
- Maintain seller balances.
- Hold funds until fulfillment or a settlement condition.
- Support refunds and disputes.
- Pay sellers periodically.

The central correctness challenges

- Customer payment succeeds but one seller allocation fails.
- Seller is paid before an eligible refund or chargeback.
- Commission calculation is incorrect.
- Refund needs to be distributed across multiple sellers.
- Seller payout is sent twice.
- Seller's available balance differs from their pending balance.

Important concepts to demonstrate

- Split payments.
- Double-entry ledger.
- Escrow-like balances.
- Pending versus available funds.
- Settlement schedules.
- Payout idempotency.
- Refund allocation.
- Chargeback handling.

## 3.3 Payment Reconciliation System

Problem statement

Design a reconciliation service that compares your internal payment and ledger records against external payment provider or bank statements.

Typical requirements

- Import external settlement files or APIs.
- Match internal and external transactions.
- Detect missing records.
- Detect duplicate records.
- Detect amount and fee mismatches.
- Identify delayed settlements.
- Generate reconciliation reports.
- Trigger investigation or correction workflows.

The central correctness challenges

- Internal payment is marked successful, but the provider has no corresponding transaction.
- Provider reports a successful payment that is missing internally.
- Amounts differ due to fees, taxes, or currency conversion.
- Transactions arrive in different orders.
- Settlement files arrive late or more than once.
- A mismatch is resolved but its correction is applied twice.

Important concepts to demonstrate

- Reconciliation batches.
- Matching keys.
- Idempotent ingestion.
- Exception queues.
- Tolerance rules.
- Manual investigation.
- Audit trails.
- Automated correction workflows.

Why this problem matters

Reconciliation is not merely a reporting feature. It is a critical safety mechanism for detecting financial inconsistencies that escaped the primary transaction workflow.

## 3.4 Merchant Payout / Bulk Disbursement System

Problem statement

Design a platform that sends money to thousands or millions of recipients, such as marketplace sellers, contractors, affiliates, or business customers.

Typical requirements

- Create payout batches.
- Validate recipient accounts.
- Schedule payouts.
- Process transfers through banking partners.
- Track payout status.
- Retry failures.
- Support cancellations where possible.
- Generate payout reports.

The central correctness challenges

- A payout batch is processed twice.
- Some payments succeed while others fail.
- A bank accepts a payout but the response is lost.
- Recipient account information changes during processing.
- The payout system retries a transfer that already succeeded.

Important concepts to demonstrate

- Bulk workflow orchestration.
- Per-recipient idempotency.
- Batch and item-level state.
- Rate limiting.
- Provider reconciliation.
- Failure isolation.
- Partial batch completion.

## 3.5 Cross-Border Money Transfer / Remittance System

Problem statement

Design a platform that allows users to send money internationally across currencies and banking networks.

For example, a customer sends ₹50,000 in India and a recipient receives an equivalent amount in another currency.

Typical requirements

- Accept money from the sender.
- Calculate the FX rate and fees.
- Lock or guarantee an exchange rate for a defined period.
- Execute the transfer.
- Credit the recipient.
- Track the transfer across banking partners.
- Handle compliance checks and failed transfers.

The central correctness challenges

- FX rate changes between quote and execution.
- Sender is debited but the receiving bank delays or rejects the transfer.
- Fees and exchange-rate rounding create discrepancies.
- A transfer is retried after an ambiguous bank response.
- Partial refunds are required after an unsuccessful transfer.

Important concepts to demonstrate

- FX quote lifecycle.
- Rate locking and expiry.
- Currency-specific accounting.
- Correspondent banking workflows.
- Settlement and reconciliation.
- Compliance checkpoints.
- Transfer state machine.

## 3.6 Gift Card and Store Credit System

Problem statement

Design a system that allows users to purchase, store, redeem, and transfer gift card value or store credits.

Typical requirements

- Issue gift cards.
- Load value.
- Redeem part or all of the balance.
- Support refunds into store credit.
- Support expiration rules where applicable.
- Track gift card activity.

The central correctness challenges

- The same gift card is redeemed concurrently.
- A gift card is redeemed twice due to retry.
- Refunds incorrectly increase the balance multiple times.
- Gift card balance differs from transaction history.
- Expiration races with redemption.

Important concepts to demonstrate

- Stored-value ledger.
- Atomic redemption.
- Idempotency.
- Balance projection.
- Expiration processing.
- Immutable transactions.

## 3.7 Loyalty Points and Rewards System

Problem statement

Design a loyalty platform that awards customers points for purchases and allows them to redeem points for discounts, products, or other benefits.

Typical requirements

- Earn points from qualifying transactions.
- Redeem points.
- Support promotional multipliers.
- Expire points.
- Reverse points after returns or refunds.
- Display point balances and history.

The central correctness challenges

- Points are awarded multiple times for the same order.
- Customer redeems points concurrently.
- An order is refunded after points were awarded.
- Expiration and redemption happen simultaneously.
- Point balances differ between the loyalty service and the order system.

Important concepts to demonstrate

- Rewards ledger.
- Earn and redeem transactions.
- Point expiration.
- Reversals and adjustments.
- Idempotent event consumption.
- Rules and campaign versioning.

Key distinction

Points are not necessarily equivalent to cash, but they still represent a stored-value liability or contractual entitlement that must be accounted for consistently.

## 3.8 Coupon and Promotion Redemption System

Problem statement

Design a promotion platform that issues coupons and discounts and allows users to redeem them during checkout.

Typical requirements

- Create promotional campaigns.
- Generate coupon codes.
- Enforce eligibility rules.
- Limit redemptions per user.
- Enforce campaign-wide redemption limits.
- Apply discounts during checkout.
- Reverse redemptions when orders are cancelled.

The central correctness challenges

- A coupon with one remaining redemption is redeemed concurrently.
- A user redeems the same coupon multiple times.
- A coupon is reserved but checkout fails.
- A campaign expires during checkout.
- Discount rules change between cart calculation and order confirmation.

Important concepts to demonstrate

- Atomic redemption counters.
- Per-user limits.
- Reservation and confirmation.
- Rule evaluation.
- Pricing snapshots.
- Idempotency.
- Compensation on cancellation.

Why include this?

This is a useful transactional correctness problem even though it does not directly move money. It tests concurrency, scarce-resource allocation, and business-rule enforcement.

## 3.9 Buy Now, Pay Later (BNPL) / Installment Payment System

Problem statement

Design a platform that allows customers to purchase products immediately and repay the amount in installments.

Typical requirements

- Assess customer eligibility.
- Approve or reject financing.
- Create installment schedules.
- Pay merchants.
- Collect recurring installments.
- Handle missed payments.
- Support early repayment.
- Process refunds and cancellations.

The central correctness challenges

- Loan approval succeeds but merchant payment fails.
- Installments are collected twice.
- A refund occurs after some installments have already been paid.
- Payment schedules change after a partial repayment.
- Outstanding balance differs from the accounting records.

Important concepts to demonstrate

- Credit decision workflow.
- Repayment schedules.
- Loan or receivable ledger.
- Scheduled collection.
- Late-payment handling.
- Refund allocation.
- Financial reconciliation.

## 3.10 Loan Management and Repayment System

Problem statement

Design a loan servicing platform that manages loan accounts, disbursements, interest accrual, repayment schedules, and outstanding balances.

Typical requirements

- Create loan accounts.
- Disburse loan principal.
- Calculate interest.
- Generate repayment schedules.
- Collect repayments.
- Support partial and early repayments.
- Calculate outstanding principal and interest.
- Handle missed payments and write-offs.

The central correctness challenges

- Concurrent repayments are applied to the same loan.
- Interest calculations produce rounding differences.
- A repayment is received but not allocated correctly.
- A repayment is processed twice.
- Loan balance differs from the ledger.
- A schedule is changed after a payment has already been processed.

Important concepts to demonstrate

- Financial ledger.
- Principal versus interest allocation.
- Accrual calculations.
- Payment allocation rules.
- Idempotency.
- Reconciliation.
- Auditability.

## 3.11 Invoice and Accounts Receivable / Payable System

Problem statement

Design a billing and payment system for businesses that creates invoices, tracks what customers owe, processes incoming payments, and tracks outgoing payments to suppliers.

Typical requirements

- Generate invoices.
- Support partial payments.
- Track due dates.
- Manage credit notes.
- Track outstanding balances.
- Process supplier payments.
- Handle overdue invoices.
- Generate statements.

The central correctness challenges

- A payment is allocated to the wrong invoice.
- One payment is applied twice.
- An invoice is modified after being issued.
- A credit note and payment are processed concurrently.
- Partial payment allocations do not add up to the amount received.

Important concepts to demonstrate

- Invoice lifecycle.
- Receivables and payables.
- Payment allocation.
- Credit notes.
- Immutable financial documents.
- Aging and reconciliation.

## 3.12 Payroll and Employee Disbursement System

Problem statement

Design a payroll platform that calculates employee salaries and distributes payments to employees on a scheduled pay date.

Typical requirements

- Import employee compensation data.
- Calculate gross and net salary.
- Account for deductions, taxes, and benefits.
- Generate payslips.
- Initiate bulk payouts.
- Handle failed payments.
- Maintain payroll history.

The central correctness challenges

- Payroll is run twice for the same pay period.
- Employee details change while payroll is being calculated.
- Some employee payouts succeed while others fail.
- A bank transfer times out after accepting the transaction.
- A payroll correction needs to be applied without rewriting historical records.

Important concepts to demonstrate

- Payroll batch lifecycle.
- Calculation snapshots.
- Approval workflows.
- Bulk disbursement.
- Idempotent payments.
- Audit trails.
- Reconciliation.

# 4. Additional transactional workflow problems

These problems are also relevant, although they are sometimes classified under payments, order management, or distributed workflows rather than financial systems specifically.

## 4.1 Order Cancellation, Returns, and Refund Workflow

Problem statement

Design an order management system that supports cancellations, returns, exchanges, refunds, and reverse fulfillment.

For example, a customer places an order containing three items, receives two items, and requests a return for one of them.

The central correctness challenges

- Cancellation arrives while payment is processing.
- One item is cancelled from a multi-item order.
- Refund succeeds but returned inventory cannot be restocked.
- Inventory is restocked twice after duplicate return events.
- A refund is initiated before eligibility is verified.
- A return is accepted but the refund amount is calculated incorrectly.

Important concepts to demonstrate

- Order and item-level state machines.
- Partial cancellation and refund.
- Return authorization.
- Reverse inventory movements.
- Payment and fulfillment compensation.
- Idempotent refund and restocking operations.

## 4.2 Banking Core / Bank Account Management System

Problem statement

Design a banking platform that maintains customer accounts and handles deposits, withdrawals, transfers, interest, fees, statements, and account balances.

Typical requirements

- Create and manage bank accounts.
- Deposit and withdraw money.
- Transfer between accounts.
- Maintain account balances.
- Apply fees and interest.
- Generate account statements.
- Support account holds and restrictions.
- Maintain audit history.

The central correctness challenges

- Concurrent withdrawals exceed the available balance.
- Interest is applied more than once for the same period.
- A transfer has only one side recorded.
- A balance does not match the posted transactions.
- A transaction must be corrected without removing its historical record.

Important concepts to demonstrate

- Double-entry accounting.
- Account-level consistency.
- Available versus ledger balance.
- Transaction posting.
- Holds and restrictions.
- Interest and fee accrual.
- Statements and audit trails.

This is a broader financial system design problem that can incorporate wallets, transfers, and ledgers.

## 4.3 ATM Withdrawal System

Problem statement

Design a system that allows customers to withdraw cash from an ATM using their bank cards.

Typical workflow

1. Customer authenticates using their card and PIN.
2. ATM requests transaction authorization.
3. Bank validates the account and available balance.
4. Bank authorizes the withdrawal and places an appropriate hold or debit.
5. ATM dispenses cash.
6. Transaction is finalized or reversed based on the outcome.

The central correctness challenges

- Bank approves the withdrawal, but the ATM fails to dispense cash.
- ATM dispenses cash but the confirmation message is lost.
- ATM loses network connectivity during the transaction.
- The same withdrawal request is submitted again.
- Cash is partially dispensed.
- The ATM's physical cash records do not match banking records.

Important concepts to demonstrate

- Authorization and final posting.
- Idempotent transaction identifiers.
- Hardware and network failure handling.
- Reversal workflows.
- ATM cash reconciliation.
- Financial audit trail.

This is a particularly interesting example because the system must coordinate software transactions with a physical-world action that cannot be rolled back in the same way as a database update.

## 4.4 Chargeback and Payment Dispute Management System

Problem statement

Design a platform that manages disputes raised by customers or cardholders after a payment has been processed.

For example, a customer disputes a card transaction, claiming that the transaction was unauthorized or that the purchased product was not delivered.

Typical requirements

- Receive chargeback notifications.
- Identify the original payment.
- Freeze or adjust relevant funds.
- Collect merchant evidence.
- Submit responses to payment networks.
- Track dispute outcomes.
- Apply financial adjustments.
- Notify affected parties.

The central correctness challenges

- A chargeback event is delivered multiple times.
- A refund and chargeback are processed simultaneously.
- A dispute arrives after a merchant payout.
- The dispute is won after funds have already been deducted.
- Evidence submission or response deadlines are missed.
- Multiple dispute events refer to the same original payment.

Important concepts to demonstrate

- Dispute state machine.
- Event deduplication.
- Financial adjustments.
- Merchant reserve or negative balance.
- Evidence workflow.
- Reconciliation.
- Deadline processing.

Important distinction: A refund is generally initiated by the merchant or payment platform. A chargeback is a dispute process initiated through the cardholder's financial institution or payment network. They should not be treated as identical workflows.

## 4.5 Card Issuing and Cardholder Account Management

Problem statement

Design a platform that issues virtual or physical payment cards to users and manages spending against their accounts.

Typical requirements

- Issue and activate cards.
- Set transaction limits.
- Authorize purchases.
- Block or unblock cards.
- Manage cardholder balances.
- Process reversals.
- Support refunds and disputes.
- Track card transactions.

The central correctness challenges

- Concurrent card authorizations exceed the permitted spending limit.
- A card is blocked while an authorization is in progress.
- Authorization holds are not released correctly.
- A reversal arrives before the original authorization is fully processed.
- A transaction is posted twice.

Important concepts to demonstrate

- Low-latency authorization.
- Spending controls.
- Holds and available balance.
- Authorization reversal.
- Ledger posting.
- Event ordering and deduplication.

## 4.6 Food Delivery / On-Demand Order Workflow

Problem statement

Design a food delivery system where a customer places an order, the restaurant accepts it, a delivery partner is assigned, payment is collected, and the order is completed.

The central correctness challenges

- Payment succeeds, but the restaurant rejects the order.
- Restaurant accepts the order, but no delivery partner is available.
- Customer cancels after preparation has started.
- Delivery partner is reassigned.
- Restaurant and delivery partner report conflicting statuses.
- Refunds, restaurant payments, and delivery partner earnings need to be calculated correctly.

Important concepts to demonstrate

- Order state machine.
- Saga orchestration.
- Payment and payout workflows.
- Cancellation rules.
- Compensation.
- Event-driven status updates.

This is a useful distributed workflow problem, though it is less focused on accounting than wallets, transfers, and payment infrastructure.

## 4.7 Flash Sale / Limited-Stock Purchase System

Problem statement

Design a platform that sells a limited number of products during a short promotional event, potentially receiving millions of requests in a short period.

For example, 10,000 units are available, but millions of users attempt to purchase them.

The central correctness challenges

- Prevent inventory from going negative.
- Prevent overselling.
- Avoid duplicate purchases.
- Handle huge contention on popular products.
- Prevent inventory holds from getting stuck.
- Handle users who receive inventory reservations but never pay.

Important concepts to demonstrate

- Atomic inventory allocation.
- Queues and waiting rooms.
- Admission control and rate limiting.
- Reservation tokens.
- Idempotency.
- Hold expiration.
- Asynchronous order processing.

Why include it?

It is a high-scale variation of inventory reservation. It tests whether you can preserve strict correctness while handling extreme traffic.

# 5. Problem classification: what each design primarily tests

Many of these problems reuse the same components, but their primary interview focus differs.

| Problem                | Primary concept being tested                      | Secondary concepts                 |
| ---------------------- | ------------------------------------------------- | ---------------------------------- |
| Payment gateway        | Idempotency and external payment integration      | Webhooks, retries, reconciliation  |
| E-commerce checkout    | Distributed transaction orchestration             | Inventory, payment, order state    |
| Inventory reservation  | Concurrency and overselling prevention            | Holds, expiration, locking         |
| Digital wallet         | Financial correctness                             | Ledger, balance consistency        |
| P2P transfer           | Atomic debit-credit                               | Saga, reversals                    |
| Double-entry ledger    | Accounting invariants                             | Immutability, auditability         |
| Refund system          | Safe reversal of financial transactions           | Idempotency, concurrency           |
| Subscription billing   | Scheduled recurring transactions                  | Invoicing, retries, proration      |
| Flight booking         | Scarce-resource reservation                       | Holds, payment coordination        |
| Hotel booking          | Time-range inventory                              | Reservation expiration             |
| Event ticketing        | High-contention allocation                        | Queues, seat locking               |
| Travel booking         | Distributed saga                                  | Compensation, partial success      |
| Card authorization     | Low-latency financial decisions                   | Holds, limits, reversals           |
| Marketplace payments   | Money distribution                                | Escrow, commissions, payouts       |
| Reconciliation         | Detecting and resolving financial inconsistencies | Matching, audit                    |
| Bulk payouts           | Reliable large-scale disbursement                 | Batch processing, idempotency      |
| Remittance             | Cross-currency settlement                         | FX, banking partners               |
| Gift cards             | Stored-value accounting                           | Redemption, expiration             |
| Loyalty points         | Transactional rewards accounting                  | Reversals, expiration              |
| Coupons and promotions | Concurrent redemption limits                      | Reservation, validation            |
| BNPL                   | Installment-based financial workflows             | Credit, repayment, ledger          |
| Loan management        | Financial balance and interest correctness        | Accrual, repayment allocation      |
| Invoicing              | Receivables and payment allocation                | Partial payment, credit notes      |
| Payroll                | Correct bulk financial execution                  | Approval, batching, reconciliation |
| ATM withdrawal         | Software and physical-world coordination          | Reversal, reconciliation           |
| Chargebacks            | Long-running dispute workflows                    | Financial adjustments              |
| Card issuing           | Authorization and spending controls               | Holds, limits, posting             |
| Food delivery          | Distributed order workflow                        | Saga, payout, compensation         |
| Flash sale             | High-contention inventory correctness             | Admission control, idempotency     |

# 6. The fundamental concepts you must master

The important thing is not to memorize 25 different architectures. Most of these systems are combinations of a relatively small number of fundamental concepts.

## 6.1 Idempotency

Problem: A client submits a payment request. Your service processes it, but the response is lost. The client retries.

Without idempotency, the customer could be charged twice.

The same issue appears in transfers, refunds, payouts, reservations, subscription invoices, and reward credits.

A common design is:

- Client supplies an idempotency key.
- Service stores the key with the operation and its result.
- A retry using the same key returns the existing operation/result rather than creating another operation.
- The key is scoped to the appropriate customer or operation and retained for a defined period.

Be prepared to explain how idempotency works across both your own database and an external payment provider. A local idempotency record alone cannot prove whether an external charge succeeded.

## 6.2 State Machines

A financial or booking operation should have explicit, validated state transitions.

For example, a simplified payment lifecycle:

```
CREATED
   |
   v
PROCESSING
   |
   +------> SUCCEEDED
   |
   +------> FAILED
   |
   +------> UNKNOWN
                 |
                 v
             RECONCILING
                 |
           +-----+-----+
           v           v
       SUCCEEDED     FAILED
```

The `UNKNOWN` state is important. A timeout does not necessarily mean the operation failed. The external provider might have processed the payment successfully but failed to return its response.

For each state machine, define:

- Valid states.
- Allowed transitions.
- Which actor or service can make each transition.
- Whether the transition is reversible.
- What happens when the same event arrives twice.
- What happens when events arrive out of order.

## 6.3 Distributed Transactions: Saga and Compensation

Suppose checkout involves inventory, payment, and order services:

```
Create Order
    |
Reserve Inventory
    |
Process Payment
    |
Confirm Order
    |
Start Fulfillment
```

A single database transaction cannot normally cover all those independently managed services.

Instead, the workflow uses a saga:

| Forward operation     | Possible compensation                     |
| --------------------- | ----------------------------------------- |
| Reserve inventory     | Release inventory                         |
| Charge payment        | Refund payment                            |
| Confirm booking       | Cancel booking, subject to supplier rules |
| Allocate seller funds | Reverse or adjust allocation              |

A compensation is not necessarily a perfect rollback. A refund may take time, a supplier cancellation may incur a fee, and a physical shipment may already have started.

You should understand both:

- Orchestration: A coordinator manages workflow progression.
- Choreography: Services react to published events and trigger subsequent actions.

Be able to explain when you prefer one approach, how you persist workflow progress, and how you recover from worker or service crashes.

## 6.4 Transactional Outbox and Inbox

Consider this operation:

1. Your payment service updates a payment record.
2. It publishes `PaymentSucceeded` to Kafka.

What happens if the database commit succeeds but Kafka publication fails?

The database and message broker can disagree.

A transactional outbox solves this by writing the business change and an outbox event in the same database transaction. A separate publisher delivers the event to the broker and marks it as published.

An inbox or consumer-deduplication mechanism prevents a consumer from applying the same event more than once.

Remember: messaging systems commonly provide at-least-once delivery. You must design consumers to tolerate duplicate delivery.

## 6.5 Concurrency Control

Typical interview scenarios include:

- Two users buying the last product.
- Two withdrawals attempting to consume the same wallet balance.
- Two agents refunding the same payment.
- Two customers booking the same seat.

Common techniques include:

| Technique                        | Useful when                                                                         |
| -------------------------------- | ----------------------------------------------------------------------------------- |
| Atomic conditional update        | Simple inventory or balance conditions                                              |
| Optimistic locking               | Conflicts are relatively infrequent                                                 |
| Pessimistic locking              | Contention is significant and critical sections are short                           |
| Unique constraints               | Duplicate logical operations must be rejected                                       |
| Serializable transactions        | Strong transaction-level consistency is needed and the database can handle the cost |
| Partitioning by account/resource | You want to localize contention and ownership                                       |

You should be able to explain the actual database operation that prevents the race condition, not just say "we will use a lock."

## 6.6 Double-Entry Ledger

This is fundamental for wallet, banking, card, and marketplace payment systems.

Important distinctions:

- A journal entry represents a financial transaction.
- A posting represents a debit or credit against an account.
- An account balance is derived from financial postings or maintained as a validated projection.
- A reversal adds correcting financial entries instead of deleting historical postings.

For example, when a user adds ₹1,000 to a wallet, the system should represent both sides of the accounting movement, such as the relevant cash/clearing account and the user's wallet liability account.

For a well-designed ledger, explain:

- How a journal is kept balanced.
- How duplicate postings are prevented.
- How postings are committed atomically.
- How balances are maintained.
- How posted entries are corrected.
- How audit history is preserved.

## 6.7 Available Balance versus Ledger Balance

This distinction matters in wallets, banking, and card processing.

For example:

```
Ledger balance:      ₹10,000
Active authorization: ₹2,000
Available balance:    ₹8,000
```

The ledger balance represents posted financial activity. The active authorization reduces what the customer can spend without necessarily being a final posted debit.

Be ready to explain how holds are created, captured, released, expired, and reconciled.

## 6.8 Reconciliation

Retries and idempotency reduce errors, but they do not eliminate every failure.

Your internal system may disagree with the external bank or PSP because of:

- Missing webhooks.
- Lost responses.
- Failed database updates.
- Delayed settlement files.
- Duplicate external events.
- Incorrect fees or amounts.

A reconciliation process periodically compares internal and external records and moves discrepancies into an investigation or correction workflow.

This is particularly important for payments, refunds, payouts, card settlement, remittance, and banking systems.

# 7. A reusable architecture for transactional systems

You will notice that several problems have the same architecture. The business entities change, but the underlying transaction-processing pattern is remarkably similar.

A typical architecture might look like this:

\#chatgpt-mermaid-\_r_122\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_122\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_122\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_122\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_122\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_122\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_122\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_122\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_122\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_122\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_122\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_122\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_122\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_122\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_122\_ p{margin:0;}#chatgpt-mermaid-\_r_122\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_122\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_122\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_122\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_122\_ .label text,#chatgpt-mermaid-\_r_122\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_122\_ .node rect,#chatgpt-mermaid-\_r_122\_ .node circle,#chatgpt-mermaid-\_r_122\_ .node ellipse,#chatgpt-mermaid-\_r_122\_ .node polygon,#chatgpt-mermaid-\_r_122\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_122\_ .rough-node .label text,#chatgpt-mermaid-\_r_122\_ .node .label text,#chatgpt-mermaid-\_r_122\_ .image-shape .label,#chatgpt-mermaid-\_r_122\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_122\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_122\_ .rough-node .label,#chatgpt-mermaid-\_r_122\_ .node .label,#chatgpt-mermaid-\_r_122\_ .image-shape .label,#chatgpt-mermaid-\_r_122\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_122\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_122\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_122\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_122\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_122\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_122\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_122\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_122\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_122\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_122\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_122\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_122\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_122\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_122\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_122\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_122\_ .icon-shape,#chatgpt-mermaid-\_r_122\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_122\_ .icon-shape p,#chatgpt-mermaid-\_r_122\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_122\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_122\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_122\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_122\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_122\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_122\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_122\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_122\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_122\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_122\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_122\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_122\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_122\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_122\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_122\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_122\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_122\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_122\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_122\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_122\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_122\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_122\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_122\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_122\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_122\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_122\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_122\_ .node rect,#chatgpt-mermaid-\_r_122\_ .node circle,#chatgpt-mermaid-\_r_122\_ .node ellipse,#chatgpt-mermaid-\_r_122\_ .node polygon,#chatgpt-mermaid-\_r_122\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_122\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_122\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2,2;}#chatgpt-mermaid-\_r_122\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_122\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_122\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Client / API ConsumerAPI GatewayTransactional ServicePrimary DatabaseTransactional OutboxEvent PublisherMessage BrokerBackground WorkersExternal Systems: PSP / Bank/ SupplierWebhook / Callback HandlerReconciliation Service

The exact number of services varies. You do not need to introduce microservices into every design. A modular monolith with one transactional database can be a better starting point when the business workflow and scaling requirements are relatively simple.

## 7.1 Typical request-processing flow

Consider a payment request.

Step 1: Accept the command

The client submits:

```
{
  "amount": 1000,
  "currency": "INR",
  "idempotencyKey": "payment-12345"
}
```

The service authenticates the request, validates it, and checks whether the idempotency key already exists.

Step 2: Persist the initial operation

In a database transaction:

- Create the payment record.
- Create an idempotency record.
- Create an outbox event requesting payment processing.

The transaction must commit all these changes together.

Step 3: Process the external operation

A background worker consumes the event and communicates with the payment provider.

The worker uses a stable provider-side idempotency key where supported.

Step 4: Record the outcome

The provider may return success, failure, or an ambiguous result.

Update the payment's state accordingly. When payment is confirmed, post the necessary ledger entries and write further outbox events in the same database transaction.

Step 5: Handle asynchronous notifications

A webhook may provide the definitive outcome. Validate its authenticity, deduplicate it, and apply only valid state transitions.

Step 6: Reconcile

A separate process compares internal payments with provider records and settlement information. It detects operations that remain unresolved or whose financial records do not match.

## 7.2 What happens when things fail?

This is where the real interview discussion begins.

| Failure scenario                       | Expected design response                                                         |
| -------------------------------------- | -------------------------------------------------------------------------------- |
| Client retries the same request        | Return or resume the existing operation using idempotency                        |
| Service crashes after DB commit        | Outbox event remains available for processing                                    |
| Broker delivers an event twice         | Consumer deduplicates it                                                         |
| PSP times out                          | Maintain an unknown/pending outcome and investigate before retrying a new charge |
| Webhook arrives twice                  | Deduplicate by provider event or transaction identity                            |
| Webhook arrives before API response    | Apply a valid transition without assuming arrival order                          |
| Ledger update fails                    | Do not report the financial operation as fully recorded; retry or reconcile      |
| Worker crashes halfway through         | Resume from persisted workflow state                                             |
| Refund and cancellation race           | Enforce business invariants using atomic database operations                     |
| External provider is unavailable       | Retry safely with backoff and retain the operation's state                       |
| Internal and external records disagree | Reconciliation identifies and resolves the discrepancy                           |

### An important interview distinction: database transactions versus distributed transactions

You should not use a distributed transaction or two-phase commit as the default solution for every workflow.

Use a local ACID transaction when all critical writes belong to one database boundary. For example, recording a debit and credit in the same ledger database can be atomic.

Use an asynchronous saga when a workflow crosses services or external organizations, such as a checkout involving inventory, a payment provider, and a shipping provider.

The latter usually provides eventual workflow completion with explicit compensation and reconciliation, not a global rollback of every participant.

## 7.3 Financial data correctness

There are a few implementation decisions you should consistently mention:

- Represent monetary values using integer minor units where appropriate, or carefully controlled decimal types. Do not use binary floating-point types for financial calculations.
- Store the currency explicitly alongside every monetary amount.
- Define rounding rules and apply them consistently.
- Do not assume all currencies have the same number of minor-unit decimal places.
- Record the original amount and relevant calculation snapshots for payments, invoices, and refunds.
- Preserve historical financial records. Correct mistakes with adjustment or reversal entries instead of silently rewriting posted transactions.
- Use explicit transaction identifiers and business-level idempotency keys.
- Separate a financial source of truth from cached or materialized balance views.

These details are often what distinguish a convincing financial HLD from a generic microservices diagram.

# 8. Recommended interview preparation order

I recommend not solving the problems in the order they appeared in the list. The best learning sequence is based on the concepts you need to understand first and then combine.

## Phase 1 — Must-master fundamentals

Highest priority

| Order | Problem                      | What you should master                                        |
| ----- | ---------------------------- | ------------------------------------------------------------- |
| 1     | Payment Processing System    | Idempotency, payment state machine, PSP integration, webhooks |
| 2     | Inventory Reservation System | Concurrency, atomic allocation, holds, expiration             |
| 3     | E-commerce Checkout          | Saga, compensation, order-payment-inventory coordination      |
| 4     | Digital Wallet               | Ledger, balances, atomic financial updates                    |
| 5     | P2P / Bank Transfer          | Debit-credit consistency, transfer lifecycle, reversals       |
| 6     | Double-Entry Ledger          | Journal entries, postings, accounting invariants              |
| 7     | Refund Management            | Partial refunds, duplicate prevention, refund limits          |
| 8     | Subscription Billing         | Recurring jobs, invoices, payment retries, proration          |

These eight give you the core building blocks for most transactional system design questions.

## Phase 2 — Apply the same principles to different domains

| Order | Problem                      | New concepts to learn                           |
| ----- | ---------------------------- | ----------------------------------------------- |
| 9     | Event Ticket Booking         | Seat contention and scarce-resource holds       |
| 10    | Flight Booking               | Booking confirmation and reservation expiration |
| 11    | Hotel Reservation            | Date-range inventory and availability           |
| 12    | Multi-Service Travel Booking | Multi-step saga and partial success             |
| 13    | Marketplace Payments         | Split payments, commissions, seller balances    |
| 14    | Payment Reconciliation       | Internal versus external financial truth        |
| 15    | Bulk Payouts                 | High-volume disbursements and batch recovery    |
| 16    | Chargeback Management        | Long-running disputes and financial adjustments |

## Phase 3 — Advanced financial systems

| Order | Problem                           | New concepts to learn                                     |
| ----- | --------------------------------- | --------------------------------------------------------- |
| 17    | Card Authorization and Settlement | Authorization holds, clearing, settlement                 |
| 18    | Cross-Border Remittance           | FX, currency conversion, banking partners                 |
| 19    | Gift Card System                  | Stored-value accounting and redemption                    |
| 20    | Loyalty Points System             | Rewards ledger, expiry, reversal                          |
| 21    | Coupon Redemption                 | Concurrent usage limits and campaign rules                |
| 22    | BNPL                              | Credit and installment workflows                          |
| 23    | Loan Management                   | Interest, repayment allocation, outstanding principal     |
| 24    | Invoicing / Accounts Receivable   | Partial payments, credit notes, payment allocation        |
| 25    | Payroll                           | Bulk financial execution and approvals                    |
| 26    | Banking Core / ATM                | Account-level correctness and physical-world coordination |

## Phase 4 — Additional variations

Once you master the above, cover:

- Order cancellation, returns, and exchanges.
- Car rental reservation.
- Appointment booking.
- Food delivery workflow.
- Flash sale / limited-stock purchase.
- Card issuing and cardholder management.

These are useful variations, but I would not prioritize all of them ahead of payment processing, checkout, wallet, ledger, and transfer systems.

# 9. How to approach any problem in this pattern during an HLD interview

Regardless of whether the interviewer asks you to design a wallet, booking system, payment gateway, or subscription platform, follow the same reasoning process.

## Step 1: Identify the business entities

Start by identifying the main entities.

For a payment system:

- Customer
- Payment
- Payment Method
- Payment Provider
- Refund
- Ledger Account
- Ledger Entry

For a booking system:

- Customer
- Resource / Seat / Room
- Reservation
- Booking
- Payment
- Cancellation

For a subscription system:

- Customer
- Subscription
- Plan
- Invoice
- Payment
- Entitlement

This helps you avoid designing the system around APIs alone.

## Step 2: Identify the critical invariants

An invariant is a condition that must always remain true, regardless of retries, concurrency, or failures.

Examples:

| System         | Critical invariant                                                                                       |
| -------------- | -------------------------------------------------------------------------------------------------------- |
| Payment        | One logical payment must not result in duplicate charges                                                 |
| Wallet         | A user must not spend more than their available balance                                                  |
| Money transfer | Every completed transfer must have corresponding debit and credit postings                               |
| Ledger         | Total debits must equal total credits for each journal transaction and currency                          |
| Refund         | Total successful refunds cannot exceed the captured amount                                               |
| Inventory      | Available inventory cannot become negative                                                               |
| Ticket booking | One seat cannot have two active confirmed bookings                                                       |
| Subscription   | One invoice must not be generated twice for the same subscription and billing period                     |
| Loyalty        | The same qualifying order must not earn points twice                                                     |
| Coupon         | Redemption limits must not be exceeded                                                                   |
| Marketplace    | Seller allocations and platform fees must reconcile with the collected amount and applicable adjustments |

My recommendation: Identify these invariants before discussing the database, caching, or microservices. They should drive your architecture.

## Step 3: Define state machines

Do not use one generic `status` field to represent multiple independent business processes.

For example, an e-commerce order can be:

```
CREATED
  |
  v
INVENTORY_RESERVED
  |
  v
PAYMENT_PENDING
  |
  +----> PAYMENT_FAILED
  |          |
  |          v
  |     CANCELLATION_PENDING
  |          |
  |          v
  |       CANCELLED
  |
  v
PAYMENT_SUCCESS
  |
  v
CONFIRMED
  |
  v
FULFILLMENT
  |
  v
DELIVERED
```

The payment itself has a separate lifecycle. A refund has another lifecycle. This separation prevents one process's status from incorrectly determining another process's status.

## Step 4: Define transaction boundaries

Ask these questions:

1. Which operations must happen atomically?
2. Which operations can happen asynchronously?
3. Which operations cross service or external-system boundaries?
4. Where do I need compensation?
5. What happens when I cannot determine whether an operation succeeded?

For example:

| Workflow                                         | Preferred approach                                      |
| ------------------------------------------------ | ------------------------------------------------------- |
| Debit and credit within the same ledger database | Single ACID database transaction                        |
| Reserve the last available product               | Atomic database operation                               |
| Create a payment operation and publish an event  | Database transaction with transactional outbox          |
| Checkout across inventory and payment services   | Saga                                                    |
| Flight + hotel + cab booking                     | Orchestrated saga                                       |
| External bank transfer                           | Persisted asynchronous state machine and reconciliation |
| Refund from an external payment provider         | Idempotent asynchronous workflow                        |

## Step 5: Walk through failures explicitly

For every design, simulate these scenarios:

- The request arrives twice.
- The same operation is processed concurrently.
- The service crashes immediately after a database commit.
- The database commits but event publication fails.
- The message is delivered twice.
- The external provider succeeds but its response is lost.
- Webhooks arrive late or out of order.
- The customer cancels while the transaction is processing.
- A worker processes an operation after its reservation expires.

A design that handles only the happy path is incomplete for this category.

# 10. My recommended shortlist

You do not need to design every listed system independently to become strong in this pattern. Many share the same underlying architecture.

I would consider the following 12 problems essential preparation:

| Priority | Problem                          | Why I recommend it                                                 |
| -------- | -------------------------------- | ------------------------------------------------------------------ |
| 1        | Payment Processing System        | Foundation for reliable external financial operations              |
| 2        | E-commerce Checkout              | Combines multiple transactional services                           |
| 3        | Inventory Reservation            | Teaches concurrency and scarce-resource allocation                 |
| 4        | Digital Wallet                   | Introduces ledger-based financial correctness                      |
| 5        | P2P Money Transfer               | Tests atomic debit-credit and distributed transfer handling        |
| 6        | Double-Entry Ledger              | Teaches the financial accounting foundation                        |
| 7        | Refund and Chargeback System     | Tests reversals and long-running financial workflows               |
| 8        | Subscription Billing             | Tests recurring transactions and scheduled processing              |
| 9        | Flight / Event Ticket Booking    | Tests reservation, contention, and expiration                      |
| 10       | Multi-Service Travel Booking     | Tests complex sagas and compensation                               |
| 11       | Marketplace Payments and Payouts | Tests fund allocation and settlement                               |
| 12       | Payment Reconciliation           | Tests how the system detects and repairs financial inconsistencies |

One important recommendation: Do not treat payment gateway, wallet, bank transfer, and ledger as four interchangeable problems. They are related, but each tests a different layer:

- Payment gateway: moving money through external payment providers.
- Wallet: maintaining stored value and spending balances.
- Bank transfer: coordinating debit and credit across accounts or institutions.
- Ledger: maintaining the authoritative accounting record of financial activity.

You should be able to design each independently and then explain how they integrate.

The strongest preparation strategy is to master these four, followed by checkout, inventory reservation, refunds, and subscription billing. With those foundations, most of the remaining problems become variations involving a different business domain, a different resource, or a more complicated workflow.
