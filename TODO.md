# TODO

Working in this order. Each phase builds on the one before it.


## 1. Platform repo (northbank-platform)
- [ ] Add the fail-fast guard `${VAR:?message}` to every secret reference in `docker-compose.yml`
- [ ] Pin the Kafka UI image to a release tag instead of `latest`
- [ ] Set explicit retention per topic in `kafka/topics.txt`
- [ ] Move the inline healthcheck script into `kafka/healthcheck.sh` (optional)
- [ ] Split dev-only settings (loopback port, `EXTERNAL` listener, UI) into an override file when production takes shape

## 2. Kafka security: TLS and credentials
- [ ] Create a private CA for dev and issue the broker certificate
- [ ] Switch the client listeners to `SASL_SSL`
- [ ] Update the healthcheck, `kafka-init` and the Kafka UI to use TLS
- [ ] Give customer-service its truststore and the `SASL_SSL` settings, locally and in the container
- [ ] Authenticate the controller listener and remove `User:ANONYMOUS` from `super.users`
- [ ] Choose SCRAM or OAuth for rotatable credentials, before account-service adds a second Kafka user
- [ ] Document secrets handling and rotation
- [ ] Update the "No TLS" limitation in the README and `architecture.md` once it is done

## 3. Observability
- [ ] Add Actuator and Micrometer
- [ ] Metrics and alerts: outbox backlog size and oldest age, quarantined count, publish rate and failures
- [ ] Replace `KafkaConnectivityCheck` with a proper health indicator
- [ ] Review log volume during a Kafka outage (the client still logs about three lines per reconnect)

## 4. Decisions to settle before the first consumer
- [ ] Customers and the admin created before the outbox existed have no events: choose a snapshot event, a backfill, or recreating dev data
- [ ] Single relay instance only: design per-customer locking before running more than one
- [ ] Relay throughput is about 50 events per second at the defaults: raise the batch size or keep draining while batches are full if volume needs it

## 5. Account-service (new repo)
- [ ] Own Postgres container on port 5434, own database
- [ ] Idempotent consumer building a local copy of customers from `customer.*` events
- [ ] Dead-letter topic, event validation, its own Kafka user and ACLs
- [ ] REST: open account (customer id from the JWT), get and list, balance, admin freeze and close
- [ ] Money as `NUMERIC(19,2)` in GBP, optimistic locking
- [ ] Then payment, fraud and notification services, and the gateway with OAuth2 (Keycloak)

## 6. Tests (last)
- [ ] Outbox calls verified at every call site: success captures the event fields, failures verify `never()`, save happens before record (use `ArgumentCaptor` and `InOrder`)
    - [ ] `register()`: duplicate email and duplicate phone record nothing
    - [ ] `AdminSeeder`: records once when it creates an admin, nothing when one already exists
    - [ ] Admin `changeStatus`: forbidden transition, not found and acting on own account record nothing
    - [ ] `deactivate()`: wrong password, not the owner and a suspended account record nothing
    - [ ] `reactivate()`: bad credentials and an account that is not deactivated record nothing
- [ ] `CustomerServiceImplTest`: add `@Mock OutboxService outboxService` (fails with a `NullPointerException` today)
- [ ] `AdminSeederTest`: assert `outboxService.record` is called (the file compiles already)
- [ ] Integration test: set `kafka.connectivity-check.enabled`, `outbox.relay.enabled` and `outbox.maintenance.enabled` to false; assert the outbox row after register (topic, type, key, no personal data); atomicity (a failed save leaves no row)
- [ ] `OutboxService` throws outside a transaction (`MANDATORY`)
- [ ] `OutboxRelay`: backoff, quarantine, per-customer blocking, the publish log line
- [ ] `OutboxMaintenance`: purge and the alerts
- [ ] Event contract: pin the JSON field names and the `occurredAt` format
- [ ] `Utils`: 86400 seconds prints `24h 0m 0s`
- [ ] Testcontainers Kafka test for the real publish path