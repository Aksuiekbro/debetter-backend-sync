# Concurrent credential operations: independent design review

Reviewer: token_concurrency_review (implementation-independent subagent)
Scope: pending concurrency amendment only; the previously approved migration/helper design remains approved.
Score: 9.3 / 10
Status: APPROVED (including the explicit refinements below)

The shared JPA transaction and consistent user-row-before-token-write ordering are appropriate. Issuance serialized by stable user ID can either precede rename and be revoked, or follow rename and reject the stale principal. Restoration can safely use the token username to locate a row only when it rereads the token under that lock before resolving the user. The proposal preserves the superclass remember-me authentication type and does not require privilege promotion. No new lock-order cycle is apparent in these paths.

## Approved refinements

1. Preserve theft cleanup as well as expiry cleanup. The installed Spring Security 7.0.0-M2 `AbstractRememberMeServices.autoLogin` catches ordinary remember-me failures but explicitly rethrows `CookieTheftException`. `PersistentTokenBasedRememberMeServices.processAutoLoginCookie` deletes the user's tokens before throwing that exception. Merely wrapping `super.autoLogin` in `TransactionTemplate` therefore rolls back theft revocation. Catch this specific exception inside the transaction, return it as an outcome so revocation commits, then rethrow it after transaction completion. Do not catch database/transaction failures as successful outcomes. Prove mismatch revocation remains committed and the request remains unauthenticated.

2. Make the issuance comparison read actual database state. A pessimistic-lock entity query does not refresh an already-managed `User`; login may retain that entity in the request persistence context. Capture the authenticated name before the operation, then either read a fresh scalar username under the stable-ID lock or explicitly refresh the locked entity before comparing. Refresh must not silently change the comparison input through principal/entity aliasing. Add coverage with a preloaded managed principal and a rename committed by a separate transaction, in addition to the proposed detached stale-principal and lock-order tests.

3. Base expiry cleanup on a token reread after acquiring the username lock. Do not use the initial token snapshot for deletion: rename and old-name reuse may occur between the preliminary read and lock acquisition. A disappeared token must cause rejection without deleting the replacement account's tokens. This also applies to any other pre-superclass token cleanup introduced by the amendment.

## Evidence and verification boundary

The implementer accepted all three refinements before application changes: capture/rethrow `CookieTheftException` outside the transaction after committed cleanup; snapshot `auth.getName()` and query a fresh scalar database username after the stable-ID row lock; and reread the token under the username lock before evaluating expiry. The proposed managed-principal regression explicitly binds an open EntityManager, preloads the principal, commits rename on another thread, and invokes the real remember-me service with that stale principal. These concrete refinements resolve the initial review findings. There are no outstanding design must-fixes.

Reviewed the amendment, these explicit implementer refinements, the root account-profile design and prior token-addendum approval, current `UserService`, `UserRepository`, `JsonRememberMeServices`, `AuthProvider`, `AuthController`, `SecurityConfig`, and the `User` mapping. Verified the installed Spring Security JAR's actual superclass exception handling with `javap`; no network source or speculative framework assumption was used. No application edits or tests were performed by this reviewer. This is design approval; implementation verification still requires the targeted real-JDBC regression tests.

## Logout refinement: independent follow-up design approval

Score: 9.3 / 10. Status: APPROVED before implementation.

The implementer proposed extending the same transaction and lock order to logout after code review identified a stale-name revocation race. For a `User` session principal, lock that stable user ID and read its current scalar database username before revoking tokens. For cookie-only or other principal types, decode/read the token, lock the user by the token username, then reread and verify the series/token before deletion. Missing or invalid tokens must not select any account for revocation. Cancel the cookie in `finally`, and do not call the superclass logout method because it independently removes tokens using the potentially stale authentication name.

This closes both the stale-session-name and cookie-snapshot races while retaining the existing all-tokens logout behavior for the actual account. The user-row-before-token order remains consistent with rename, issuance, and restoration. Tests must exercise a stale session after rename/name reuse and cookie-only logout blocked by a concurrent rename. The stale-session case should show the original account's current-name tokens revoked and the replacement account's tokens retained and usable. This approval concerns the concrete design; implementation and regression verification remain required.
