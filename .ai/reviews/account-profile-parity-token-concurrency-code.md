# Concurrent credential operations: independent code review

Reviewer: token_concurrency_review
Scope: final token helper, including approved issuance/restoration/logout concurrency amendments and their seven regressions
Score: 9.4 / 10
Status: APPROVED

The implementation follows the independently approved amendment. `onLoginSuccess` snapshots the authenticated name, locks the stable user ID, then obtains a scalar database username rather than trusting a potentially stale managed entity. Token creation happens while that same transaction holds the lock. It therefore cannot recreate old-name tokens after a committed rename.

`autoLogin` wraps the complete superclass authentication operation in the shared transaction. Restoration reads the token only to locate the user lock, rereads after acquiring it, and performs expiry cleanup and superclass validation while holding the lock. An intervening rename revokes the token before this second read. The exceptional theft outcome is captured inside the transaction and rethrown only after cleanup commits, avoiding rollback of mismatch revocation. Ordinary invalid/expired-cookie outcomes still cancel the cookie. The superclass constructs remember-me authentication without promoting its trust level.

`logout` now uses the stable ID of a session principal, locks that account, and reads its fresh scalar username before removing tokens. Cookie-only logout locks from the preliminary token identity and rereads/verifies the actual token under the lock before removal. It never delegates to the superclass's stale-name removal, and cookie cancellation happens in `finally`. This resolves the adjacent logout race identified in the first review without changing the account selected for valid session logout.

The repository's pessimistic selects target only the root user entity and do not add a fetch graph to the locking query. Rename, issuance, restoration, and logout acquire a user row before token writes, so no new opposite lock order appears within the reviewed paths. Security configuration injects the existing application transaction manager and the JDBC token repository using the shared DataSource.

## Regression review and observed evidence

The new tests use the real configured service, database tokens, and repository locks. They cover a detached stale principal after rename/name reuse, a deliberately bound EntityManager with an already-managed stale principal, theft cleanup, issuance blocked by a concurrent rename, restoration blocked by a concurrent rename, stale-session logout after name reuse, and cookie-only logout blocked by rename. The stale-session test proves that the original account's current-name tokens are revoked while the replacement account's cookie remains usable and resolves to the replacement's stable ID. The concurrency helper acquires the holder lock first, starts a worker, asserts that it cannot complete while the lock is held, commits rename, then bounds worker completion. Existing expiry and remember-me trust tests cover the adjacent behavior.

The theft regression invokes the real service, asserts the superclass's `CookieTheftException`, and then verifies canceled cookie plus committed deletion. The installed Spring Security 7.0.0-M2 filter lets this exception escape `autoLogin`; bytecode was inspected, so the test does not claim an HTTP 4xx contract that the application does not provide.

Independently inspected `.ai/runs/account-profile-parity-focused.log` and the Surefire account report: the final focused run completed with BUILD SUCCESS on 2026-09-08 at 22:38:09 +04:00. All 48 tests passed with zero failures, errors, or skips: 41 account HTTP/persistence tests, three provider tests, three existing service tests, and one standalone migration/JDBC test. This reviewer did not rerun Maven or edit application code.

## Verification boundary

No outstanding implementation must-fix was found in the final token helper. The application integration fixture uses native H2 for the generated full schema; the standalone migration test uses H2 PostgreSQL mode with the real JDBC repository and validates migration reruns plus token create/read/update/delete. These are not a live PostgreSQL concurrency test. This verdict covers the token helper and its supporting locking/configuration changes; the broader account/API/frontend review remains separate.
