# Customer Service: Event contract

## Topics
| Topic | Key | Partitions | Event |
|---|---|---|---|
| `customer.registered` | customer id | 3 | `CustomerRegistered` |
| `customer.status-changed` | customer id | 3 | `CustomerStatusChanged` |

Topics are created by the platform repository. Services cannot create them. Retention is the Kafka
default (7 days) and is not yet set explicitly. Consumers that rebuild state must not rely on old messages.

## Fields
All fields are required.

**`CustomerRegistered`**
| Field | Type | Notes |
|---|---|---|
| `eventId` | UUID | Unique per event. Consumers deduplicate on it. |
| `eventType` | string | Always `CustomerRegistered` |
| `schemaVersion` | integer | Currently `1` |
| `occurredAt` | string | UTC instant, ISO-8601 |
| `customerId` | UUID | Also the message key |
| `status` | string | `ACTIVE`, `DEACTIVATED`, `SUSPENDED` or `CLOSED` |

**`CustomerStatusChanged`** has the same envelope, plus `oldStatus` and `newStatus` instead of `status`.

No personal data is ever included: no name, email, date of birth or address.

## Guarantees
- **At least once.** The same event can arrive twice. Deduplicate on `eventId`.
- **Ordered per customer.** All events for a customer use one partition, in the order they happened.
  There is no ordering across customers.
- **Eventually consistent.** An event can arrive some seconds after the change, or minutes after a Kafka outage.

## Rules for consumers
1. Deduplicate on `eventId`, in the same transaction as your own update.
2. Check `eventType`. An unexpected type means something is wrong: send it to your dead-letter topic.
3. Ignore fields you do not know. New optional fields can be added within a schema version.
4. Do not reject a higher `schemaVersion` outright. Process it if you can, otherwise dead-letter it.
5. Never apply an event older than one you have already applied for the same customer.

## Changing the contract
Only add optional fields. Renaming or removing a field, or changing its meaning, needs a new `schemaVersion`
and a migration period in which both versions are published.

## Access
`customer_service` may write `customer.*` topics and nothing else. Consumers get their own credentials with
read access to the topics they need and to their own consumer group.

## Known gaps
- Customers created before the outbox existed have no `CustomerRegistered` event.
- Re-publishing (a manual operation) can replay old events. Consumers must tolerate that.
- Role changes are not published. Services read the role from the JWT.