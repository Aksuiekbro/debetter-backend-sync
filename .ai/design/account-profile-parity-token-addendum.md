# Token storage and session addendum

Author: backend_implementation. Scope: backend #17–19.

The repository configures JdbcTokenRepositoryImpl but has no tracked persistent_logins table creation. Add an idempotent Liquibase SQL migration included by the master changelog: persistent_logins(username VARCHAR(64) NOT NULL, series VARCHAR(64) PRIMARY KEY, token VARCHAR(64) NOT NULL, last_used TIMESTAMP NOT NULL), plus a username index. CREATE TABLE/INDEX IF NOT EXISTS preserves compatible existing token tables and data. No startup create-table flag or test mock substitutes for production migration.

Use a narrow JsonRememberMeServices subclass of the existing persistent service. Its rememberMeRequested hook reads a server request attribute populated only after the JSON controller has authenticated an explicit rememberMe=true request; query parameters cannot opt users in. Keep duration 30 days. Configure the filter's remember-me provider and custom service with the same required configured key; otherwise valid cookie tokens cannot authenticate. Preserve the JDBC repository and the configured application DataSource.

Inject the persistent token repository into UserService and revoke old-username tokens inside the same @Transactional update as the username change, after preflight account/password checks. JPA and JDBC share the configured DataSource/JpaTransactionManager, so conflicts and JDBC failure roll back both. Lock the updated user row to serialize concurrent renames. Avoid a SecurityConfig constructor dependency cycle by method-injecting its filter dependencies.

A narrow session helper refreshes only a matching stable user ID after the mutation has committed. Preserve authorities, details, and authentication type (remember-me remains remember-me). Clear the current cookie on self rename with loginFail; do not issue replacement tokens without opt-in. Other-user/admin edits leave the actor's session/cookie unchanged.

Spring's logout filter runs before remember-me authentication; therefore a cookie-only logout may have no Authentication. The subclass will validate the supplied persistent cookie against JDBC before revoking its tokens in this case, then use normal cookie cancellation. Invalid cookies never select a user for revocation.

Verification uses full Spring HTTP/security/persistence tests with real generated mapper, BCrypt, JPA, and JdbcTokenRepositoryImpl and test-only identities; execute the actual migration in the H2 PostgreSQL-mode test database. Cover JSON opt-in/false/query mismatch, cookie-only authentication, expiry, both session and cookie-only logout, rename + old-name reuse, principal trust/admin stability, rollback, and safe invalid-login JSON. Verify the SQL migration can rerun without losing live tokens. Docker-backed PostgreSQL tests remain optional only if already supported by the environment.

## Concurrent credential operations amendment (independently approved)

A login may authenticate the old principal before a rename commits and issue its cookie afterward. To prevent token creation under a released name, the custom remember-me helper will use a TransactionTemplate with the same JpaTransactionManager, lock the authenticated stable user ID before token creation, and compare the currently stored username to the principal username. A stale principal gets no token and its cookie is cleared. Token creation under that lock either precedes rename (and is revoked by it) or sees the renamed identity and is rejected.

Cookie restoration will similarly run in that transaction, read the token's username, lock the corresponding user row, and then call the superclass cookie processor which rereads the token before resolving the user. If rename ran first, the token is absent after the lock; if cookie authentication ran first, it resolves the original identity while rename waits. A missing row is rejected. Both locks use root-user-only selects, avoiding PostgreSQL FOR UPDATE against nullable fetch joins. Token deletion remains after these locks where relevant.

Tests will exercise a stale already-authenticated principal after a committed rename and name reuse using the real configured remember-me service and real JDBC tokens. A deterministic concurrent transaction test will hold the user row, start a token issuance attempt, perform rename/revoke under the holder transaction, commit, and verify issuance finishes without an old-name token. Timeout-bounded latches/futures provide ordering and expose deadlocks rather than sleeping.

### Independent review refinements

The transactional entry point for cookie restoration is public autoLogin wrapping the superclass call. Spring catches ordinary authentication failures inside that call, but rethrows CookieTheftException after deleting mismatched tokens. Catch only CookieTheftException inside the transaction and return it as an outcome; commit token cleanup, then rethrow outside. Database failures still escape and roll back. Expiry is evaluated from the token reread after the user-row lock, never the pre-lock snapshot.

Issuance must snapshot authentication.getName() before locking. JPA can return a stale already-managed login principal from the locking query under OSIV, so compare that captured name to a fresh scalar username query after the root-ID row lock. Do not trust the username on the entity returned by the locking query. Add a regression with an explicitly bound open EntityManager and preloaded managed authenticated principal, rename committed on another thread, and real helper issuance using that stale principal. The old name must receive no token.

### Logout identity refinement

Independent code review identified that logout can carry an old session username, or a token snapshot taken before rename. Serialize logout through the same transaction/user-row-first order. For a User session principal, lock the stable ID and obtain a fresh scalar current username before revoking that account's tokens. For cookie-only logout (or an unrecognized principal type), read/decode the supplied token, lock its username's user row, reread the token after locking, and compare the token value before selecting an account to revoke. An absent or invalid token selects nobody. Cancel the cookie in finally; do not call the superclass's stale-username-based token removal. Add real stale-session logout after old-name reuse and bounded cookie-only logout waiting for rename regressions. This protects account isolation while retaining normal logout behavior.


### Expiration isolation review correction

The broader independent build review found that removing all username tokens on ordinary expiry invalidates fresh remembered logins on other devices. Preserve the superclass's post-lock token reread, expiration rejection, and cookie cancellation without proactive token-row deletion. Account-wide revocation remains limited to explicit logout, rename, and theft detection. A real-JDBC two-token test expires only A, rejects/cancels A, and proves B still authenticates its original stable account without a session.
