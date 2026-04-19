# BidNow — JavaFX + JDBC + Maven Project Structure

> **Stack:** Java 21 · JavaFX 21 · JDBC (raw) · Maven · FXML · SQLite / PostgreSQL

---

## Table of Contents

- [Database Schema](#database-schema)
- [Folder Structure](#folder-structure)
- [Layer Breakdown](#layer-breakdown)
  - [Entity](#entity-layer)
  - [Repository](#repository-layer)
  - [Service](#service-layer)
  - [Controller](#controller-layer)
  - [FXML Views](#fxml-view-layer)
  - [Util](#util-layer)
  - [Config](#config-layer)
- [File Reference](#file-reference)
- [Dependency Flow](#dependency-flow)

---

## Database Schema

From the ERD image, 7 tables:

| Table       | Key Columns                                                       | Purpose                              |
|-------------|-------------------------------------------------------------------|--------------------------------------|
| `user`      | `id`, `username`, `password`, `role`, `fullname`                 | All user accounts                    |
| `account`   | `id`, `user_id`, `balance`, `locked_balance`                     | Wallet per user                      |
| `items`     | `id`, `owner_user_id`, `begin_price`, `status`                   | Items listed for auction             |
| `session`   | `id`, `item_id`, `current_user_id`, `current_price`, `availability_time` | Active auction session       |
| `bid`       | `id`, `user_id`, `item_id`, `price`                              | Each manual bid placed               |
| `stake`     | `id`, `locked_items_id`, `user_id`, `amount`                     | Locked funds when a user bids        |
| `auto_bid`  | `id`, `user_id`, `item_id`, `max_price`, `is_active`             | Auto-bid configuration per user/item |

---

## Folder Structure

```
bidnow/
├── pom.xml                                          # Maven build — JavaFX + JDBC dependencies
├── schema.sql                                       # DDL: CREATE TABLE for all 7 tables
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── bidnow/
│   │   │           │
│   │   │           ├── BidNowApp.java               # Application entry point (extends Application)
│   │   │           │
│   │   │           ├── config/
│   │   │           │   ├── DatabaseConfig.java       # JDBC connection pool / DataSource setup
│   │   │           │   └── AppContext.java           # Manual DI — wires repos, services, controllers
│   │   │           │
│   │   │           ├── entity/                       # Plain POJOs — one class per DB table
│   │   │           │   ├── User.java                 # id, username, password, role, fullname
│   │   │           │   ├── Account.java              # id, userId, balance, lockedBalance
│   │   │           │   ├── Item.java                 # id, ownerUserId, beginPrice, status
│   │   │           │   ├── Session.java              # id, itemId, currentUserId, currentPrice, availabilityTime
│   │   │           │   ├── Bid.java                  # id, userId, itemId, price
│   │   │           │   ├── Stake.java                # id, lockedItemsId, userId, amount
│   │   │           │   └── AutoBid.java              # id, userId, itemId, maxPrice, isActive
│   │   │           │
│   │   │           ├── enums/
│   │   │           │   ├── UserRole.java             # ADMIN, USER, SELLER
│   │   │           │   └── ItemStatus.java           # PENDING, SOLD
│   │   │           │
│   │   │           ├── repository/                   # Raw JDBC — PreparedStatement only, no business logic
│   │   │           │   ├── UserRepository.java       # findById, findByUsername, save, update, delete
│   │   │           │   ├── AccountRepository.java    # findByUserId, updateBalance, lockBalance, unlockBalance
│   │   │           │   ├── ItemRepository.java       # findAll, findPending, findById, save, updateStatus
│   │   │           │   ├── SessionRepository.java    # findAll, findActive, findById, save, updatePrice, close
│   │   │           │   ├── BidRepository.java        # findBySessionId, findTopBid, findByUserId, save
│   │   │           │   ├── StakeRepository.java      # findByUserId, findBySessionId, save, delete, release
│   │   │           │   └── AutoBidRepository.java    # findByUserAndItem, findActiveBySession, save, setActive
│   │   │           │
│   │   │           ├── service/                      # Business logic — calls repositories, enforces rules
│   │   │           │   ├── AuthService.java          # login, register, logout, validateRole
│   │   │           │   ├── AccountService.java       # getBalance, deposit, lockFunds, releaseFunds, getAvailable
│   │   │           │   ├── ItemService.java          # upload, approve, reject, listPending, listAll, getById
│   │   │           │   ├── SessionService.java       # createSession, getActive, getById, closeSession, declareWinner
│   │   │           │   ├── BidService.java           # placeBid, validateAmount, getHistory, processAutoBids
│   │   │           │   ├── StakeService.java         # createStake, releaseStake, releaseAll, getUserStakes
│   │   │           │   └── AutoBidService.java       # configure, activate, deactivate, trigger, getByUser
│   │   │           │
│   │   │           ├── controller/                   # JavaFX FXML controllers — one per screen
│   │   │           │   ├── LoginController.java      # → login.fxml       | AuthService
│   │   │           │   ├── DashboardController.java  # → dashboard.fxml   | SessionService, AccountService
│   │   │           │   ├── SessionListController.java     # → session-list.fxml    | SessionService
│   │   │           │   ├── SessionDetailController.java   # → session-detail.fxml  | BidService, StakeService, AutoBidService
│   │   │           │   ├── ItemBrowseController.java      # → item-browse.fxml     | ItemService, SessionService
│   │   │           │   ├── UploadItemController.java      # → upload-item.fxml     | ItemService
│   │   │           │   ├── DepositController.java         # → deposit.fxml         | AccountService
│   │   │           │   ├── AccountController.java         # → account.fxml         | AccountService, StakeService
│   │   │           │   └── WinnerController.java          # → winner.fxml          | SessionService, AccountService
│   │   │           │
│   │   │           └── util/                         # Shared helpers — no dependencies on other layers
│   │   │               ├── CurrencyFormatter.java    # formatVND(long amount) → "₫ 3,200,000"
│   │   │               ├── DateTimeUtil.java          # formatRelative("2m ago"), formatClock("09:41")
│   │   │               ├── PasswordUtil.java          # hash(plain), verify(plain, hashed) — BCrypt
│   │   │               ├── NavigationUtil.java        # loadFxml(path), switchScene(stage, fxml)
│   │   │               └── AlertUtil.java             # showError(msg), showSuccess(msg), showConfirm(msg)
│   │   │
│   │   └── resources/
│   │       └── com/
│   │           └── bidnow/
│   │               │
│   │               ├── fxml/                          # FXML view files — one per screen
│   │               │   ├── login.fxml                 # fx:controller="...LoginController"
│   │               │   ├── dashboard.fxml             # fx:controller="...DashboardController"
│   │               │   ├── session-list.fxml          # fx:controller="...SessionListController"
│   │               │   ├── session-detail.fxml        # fx:controller="...SessionDetailController"
│   │               │   ├── item-browse.fxml           # fx:controller="...ItemBrowseController"
│   │               │   ├── upload-item.fxml           # fx:controller="...UploadItemController"
│   │               │   ├── deposit.fxml               # fx:controller="...DepositController"
│   │               │   ├── account.fxml               # fx:controller="...AccountController"
│   │               │   └── winner.fxml                # fx:controller="...WinnerController"
│   │               │
│   │               ├── css/
│   │               │   ├── theme.css                  # Global dark theme CSS variables
│   │               │   └── components.css             # Card, badge, button, input styles
│   │               │
│   │               ├── images/
│   │               │   └── logo.png                   # App icon / taskbar icon
│   │               │
│   │               ├── database.properties            # jdbc.url, jdbc.user, jdbc.password
│   │               └── app.properties                 # app.name=BidNow, app.version=1.0.0
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── bidnow/
│                   ├── repository/
│                   │   ├── UserRepositoryTest.java    # JDBC integration tests (in-memory H2)
│                   │   ├── AccountRepositoryTest.java
│                   │   └── BidRepositoryTest.java
│                   └── service/
│                       ├── AuthServiceTest.java       # Unit tests with mocked repos
│                       ├── BidServiceTest.java        # Stake lock / release logic
│                       └── AutoBidServiceTest.java    # Auto-bid trigger logic
```

---

## Layer Breakdown

### Entity Layer

> Location: `src/main/java/com/bidnow/entity/`

Plain Java POJOs. No annotations. Fields match database column names (camelCase in Java, snake_case in DB).

| File | Maps to Table | Fields |
|------|---------------|--------|
| `User.java` | `user` | `int id`, `String username`, `String password`, `UserRole role`, `String fullname` |
| `Account.java` | `account` | `int id`, `int userId`, `long balance`, `long lockedBalance` |
| `Item.java` | `items` | `int id`, `int ownerUserId`, `long beginPrice`, `ItemStatus status` |
| `Session.java` | `session` | `int id`, `int itemId`, `int currentUserId`, `long currentPrice`, `LocalDateTime availabilityTime` |
| `Bid.java` | `bid` | `int id`, `int userId`, `int itemId`, `long price` |
| `Stake.java` | `stake` | `int id`, `int lockedItemsId`, `int userId`, `long amount` |
| `AutoBid.java` | `auto_bid` | `int id`, `int userId`, `int itemId`, `long maxPrice`, `boolean isActive` |

**Tip:** Add a no-arg constructor and getters/setters to each entity, or use Java Records for read-only data transfer.

---

### Repository Layer

> Location: `src/main/java/com/bidnow/repository/`

Each repository receives a `Connection` (or `DataSource`) from `DatabaseConfig` via constructor injection. Uses `PreparedStatement` for all queries. Returns entity objects or `List<Entity>`. Never contains business rules.

| File | Key Methods |
|------|-------------|
| `UserRepository` | `findById(int id)`, `findByUsername(String u)`, `save(User u)`, `update(User u)` |
| `AccountRepository` | `findByUserId(int uid)`, `updateBalance(int uid, long amount)`, `lockBalance(int uid, long amount)`, `unlockBalance(int uid, long amount)` |
| `ItemRepository` | `findAll()`, `findByStatus(ItemStatus s)`, `findById(int id)`, `save(Item i)`, `updateStatus(int id, ItemStatus s)` |
| `SessionRepository` | `findAll()`, `findActive()`, `findById(int id)`, `save(Session s)`, `updateCurrentPrice(int id, long price, int userId)`, `close(int id)` |
| `BidRepository` | `findByItemId(int itemId)`, `findTopBid(int itemId)`, `findByUserId(int uid)`, `save(Bid b)` |
| `StakeRepository` | `findByUserId(int uid)`, `findByItemId(int itemId)`, `save(Stake s)`, `delete(int id)`, `releaseAll(int itemId)` |
| `AutoBidRepository` | `findByUserAndItem(int uid, int itemId)`, `findActiveByItem(int itemId)`, `save(AutoBid a)`, `setActive(int id, boolean active)` |

---

### Service Layer

> Location: `src/main/java/com/bidnow/service/`

Business logic lives here. Services receive repositories via constructor. Controllers call only services — never repositories directly.

#### `AuthService`
```
login(username, password)      → User       — verify hash, return user or throw
register(username, ...)        → User       — check duplicate, hash password, save
validateRole(user, role)       → boolean    — check user.role matches required
```

#### `AccountService`
```
getBalance(userId)             → Account    — total + locked + available
deposit(userId, amount)        → void       — add to balance
lockFunds(userId, amount)      → void       — move balance → lockedBalance (when bid placed)
releaseFunds(userId, amount)   → void       — move lockedBalance → balance (when outbid)
getAvailable(userId)           → long       — balance - lockedBalance
```

#### `ItemService`
```
upload(Item item)              → Item       — save with status=PENDING
approve(itemId)                → void       — set status=PENDING (ready for session)
listPending()                  → List<Item> — all PENDING items
getById(itemId)                → Item
```

#### `SessionService`
```
createSession(itemId, endTime) → Session    — admin creates auction session
getActive()                    → List<Session>
getById(sessionId)             → Session
closeSession(sessionId)        → void       — declares winner, deducts balance, releases other stakes
declareWinner(sessionId)       → User       — top bidder at close time
```

#### `BidService`
```
placeBid(userId, itemId, price)  → Bid      — validate > current price, lock funds via StakeService,
                                              save bid, update session.currentPrice
getHistory(itemId)               → List<Bid>
processAutoBids(itemId, newPrice)→ void     — check all active AutoBids, trigger counterbid if maxPrice allows
```

#### `StakeService`
```
createStake(userId, itemId, amount) → Stake  — call AccountService.lockFunds, save stake record
releaseStake(stakeId)               → void   — call AccountService.releaseFunds, delete stake
releaseAll(itemId)                  → void   — release all stakes for losing bidders after session closes
getUserStakes(userId)               → List<Stake>
```

#### `AutoBidService`
```
configure(userId, itemId, maxPrice) → AutoBid — save or update auto_bid record
activate(autoBidId)                 → void    — set is_active = true
deactivate(autoBidId)               → void    — set is_active = false
trigger(itemId, currentPrice)       → void    — called by BidService; auto-place next bid if maxPrice allows
getByUser(userId)                   → List<AutoBid>
```

---

### Controller Layer

> Location: `src/main/java/com/bidnow/controller/`

One controller per FXML screen. Each controller:
- Declares `@FXML` fields matching `fx:id` in the `.fxml` file
- Receives services via `AppContext` (called before the scene is shown)
- Calls only service methods — never SQL or repositories
- Uses `NavigationUtil.switchScene()` to navigate between screens

| Controller | FXML | Services used |
|-----------|------|---------------|
| `LoginController` | `login.fxml` | `AuthService` |
| `DashboardController` | `dashboard.fxml` | `SessionService`, `AccountService` |
| `SessionListController` | `session-list.fxml` | `SessionService` |
| `SessionDetailController` | `session-detail.fxml` | `BidService`, `StakeService`, `AutoBidService`, `AccountService` |
| `ItemBrowseController` | `item-browse.fxml` | `ItemService`, `SessionService` |
| `UploadItemController` | `upload-item.fxml` | `ItemService` |
| `DepositController` | `deposit.fxml` | `AccountService` |
| `AccountController` | `account.fxml` | `AccountService`, `StakeService` |
| `WinnerController` | `winner.fxml` | `SessionService`, `AccountService` |

---

### FXML View Layer

> Location: `src/main/resources/com/bidnow/fxml/`

Each `.fxml` file declares the layout using JavaFX scene graph elements. The `fx:controller` attribute links it to the matching controller class.

| FXML file | Screen | Key `fx:id` fields (examples) |
|-----------|--------|-------------------------------|
| `login.fxml` | Login | `usernameField`, `passwordField`, `roleToggle`, `signInBtn` |
| `dashboard.fxml` | Dashboard | `balanceLabel`, `pendingLabel`, `availableLabel`, `mainContent` |
| `session-list.fxml` | Session list | `sessionsContainer`, `refreshBtn`, `statLiveLabel` |
| `session-detail.fxml` | Session detail + Countdown | `itemNameLabel`, `countdownLabel`, `currentPriceLabel`, `bidAmountField`, `placeBidBtn`, `autoMaxField`, `feedContainer` |
| `item-browse.fxml` | Browse items | `itemsGrid`, `searchField`, `filterAllBtn`, `filterPendingBtn` |
| `upload-item.fxml` | Upload item | `nameField`, `descriptionArea`, `startPriceField`, `categoryField`, `submitBtn` |
| `deposit.fxml` | Deposit | `currentBalanceLabel`, `depositAmountField`, `methodGroup`, `confirmBtn`, `afterDepositLabel` |
| `account.fxml` | Account detail | `fullNameLabel`, `totalBalanceLabel`, `pendingAmountLabel`, `availableLabel`, `pendingBidsList`, `transactionsList` |
| `winner.fxml` | Winner | `itemNameLabel`, `winningPriceLabel`, `sessionIdLabel`, `browsMoreBtn` |

---

### Util Layer

> Location: `src/main/java/com/bidnow/util/`

Stateless helper classes. No dependencies on other layers.

| File | Responsibility |
|------|---------------|
| `CurrencyFormatter.java` | `formatVND(long amount)` → `"₫ 3,200,000"` |
| `DateTimeUtil.java` | `formatRelative(LocalDateTime)` → `"2m ago"`, `formatClock(int seconds)` → `"08:24"` |
| `PasswordUtil.java` | `hash(String plain)`, `verify(String plain, String hashed)` — use jBCrypt |
| `NavigationUtil.java` | `loadFxml(String path)` → `Parent`, `switchScene(Stage stage, String fxml)` |
| `AlertUtil.java` | `showError(String msg)`, `showSuccess(String msg)`, `showConfirm(String msg)` → `boolean` |

---

### Config Layer

> Location: `src/main/java/com/bidnow/config/`

#### `DatabaseConfig.java`

Reads `database.properties` and provides a `Connection`:

```java
// Usage
Connection conn = DatabaseConfig.getConnection();
```

Properties file (`src/main/resources/com/bidnow/database.properties`):
```properties
jdbc.url=jdbc:sqlite:bidnow.db
# or for PostgreSQL:
# jdbc.url=jdbc:postgresql://localhost:5432/bidnow
jdbc.user=bidnow_user
jdbc.password=secret
```

#### `AppContext.java`

Manual dependency injection — creates all objects once at app startup:

```java
// Startup sequence in BidNowApp.java
AppContext ctx = new AppContext();          // creates DataSource
ctx.init();                                // creates all repos and services

// Then pass ctx into each controller before showing the scene
loginController.setContext(ctx);
```

---

## File Reference

Quick lookup — every `.java` file and what it does:

```
BidNowApp.java                — main(), loads login.fxml, sets up stage

config/
  DatabaseConfig.java         — getConnection() using database.properties
  AppContext.java             — holds all repo + service instances

entity/
  User.java                   — POJO: id, username, password, role, fullname
  Account.java                — POJO: id, userId, balance, lockedBalance
  Item.java                   — POJO: id, ownerUserId, beginPrice, status
  Session.java                — POJO: id, itemId, currentUserId, currentPrice, availabilityTime
  Bid.java                    — POJO: id, userId, itemId, price
  Stake.java                  — POJO: id, lockedItemsId, userId, amount
  AutoBid.java                — POJO: id, userId, itemId, maxPrice, isActive

enums/
  UserRole.java               — ADMIN, USER, SELLER
  ItemStatus.java             — PENDING, SOLD

repository/
  UserRepository.java         — CRUD for user table
  AccountRepository.java      — read/update account balances
  ItemRepository.java         — CRUD + status filter for items table
  SessionRepository.java      — CRUD + active filter + price update
  BidRepository.java          — insert bid, query top bid
  StakeRepository.java        — insert/delete stakes, release by item
  AutoBidRepository.java      — insert/toggle auto_bid records

service/
  AuthService.java            — login, register, role check
  AccountService.java         — deposit, lockFunds, releaseFunds, getAvailable
  ItemService.java            — upload, approve, list
  SessionService.java         — create session, close, declare winner
  BidService.java             — placeBid (validates + locks stake), getHistory, trigger auto-bids
  StakeService.java           — createStake, releaseStake, releaseAll
  AutoBidService.java         — configure, activate, trigger counterbid

controller/
  LoginController.java        — handles login.fxml, calls AuthService
  DashboardController.java    — topbar balance display
  SessionListController.java  — renders session cards with countdown
  SessionDetailController.java— 3-column layout, bid feed, countdown timer
  ItemBrowseController.java   — item grid with status filter
  UploadItemController.java   — upload form, calls ItemService.upload
  DepositController.java      — deposit form, calls AccountService.deposit
  AccountController.java      — balance cards, pending bids, tx history
  WinnerController.java       — trophy screen after session closes

util/
  CurrencyFormatter.java      — formatVND(long)
  DateTimeUtil.java           — formatRelative, formatClock
  PasswordUtil.java           — hash, verify
  NavigationUtil.java         — switchScene, loadFxml
  AlertUtil.java              — showError, showSuccess, showConfirm
```

---

## Dependency Flow

```
FXML (.fxml)
  └── Controller  (.java — @FXML bindings, event handlers)
        └── Service  (.java — business rules, transactions)
              └── Repository  (.java — SQL queries, ResultSet mapping)
                    └── Entity  (.java — plain POJOs)
                    └── DatabaseConfig  (.java — JDBC Connection)

Util  ← used by Controller and Service (no upward deps)
Enums ← used by Entity, Repository, Service (no upward deps)
```

**Rules:**
- Controllers never import Repository classes
- Services never import Controller classes
- Entities and Enums have zero dependencies on other project classes
- Util classes have zero dependencies on other project classes
- `AppContext` is the only class allowed to import from all layers (it wires them together)

---

## pom.xml Dependencies

```xml
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>21.0.2</version>
    </dependency>

    <!-- JDBC driver — choose one -->
    <!-- SQLite (easiest for local dev) -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.45.1.0</version>
    </dependency>
    <!-- PostgreSQL (for production) -->
    <!--
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.3</version>
    </dependency>
    -->

    <!-- Password hashing -->
    <dependency>
        <groupId>org.mindrot</groupId>
        <artifactId>jbcrypt</artifactId>
        <version>0.4</version>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <version>2.2.224</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

*Generated for BidNow — JavaFX Desktop Auction Platform*