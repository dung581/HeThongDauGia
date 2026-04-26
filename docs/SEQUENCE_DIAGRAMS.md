# BidNow — Sequence Diagrams

> All 16 system flows: View → Controller → Service → Repository → Entity → Database
>
> **Render tip (VSCode):** Install the **"Markdown Preview Mermaid Support"** extension by Matt Bierner, then open preview with `Ctrl+Shift+V`.

---

## Table of Contents

| # | Scenario |
|---|----------|
| 01 | [User Login](#01-user-login) |
| 02 | [User Register](#02-user-register) |
| 03 | [Seller Uploads Item](#03-seller-uploads-item) |
| 04 | [Admin Approves / Rejects Item](#04-admin-approves--rejects-item) |
| 05 | [Admin Creates Auction Session](#05-admin-creates-auction-session) |
| 06 | [User Joins Session](#06-user-joins-session) |
| 07 | [User Places Manual Bid](#07-user-places-manual-bid) |
| 08 | [Auto Bid Triggered](#08-auto-bid-triggered) |
| 09 | [User Configures Auto Bid](#09-user-configures-auto-bid) |
| 10 | [User Deposits Funds](#10-user-deposits-funds) |
| 11 | [Stake Locked When Bid Placed](#11-stake-locked-when-bid-placed) |
| 12 | [Stake Released When Outbid](#12-stake-released-when-outbid) |
| 13 | [Session Timer Expires — Winner Declared](#13-session-timer-expires--winner-declared) |
| 14 | [Winner Balance Deducted](#14-winner-balance-deducted) |
| 15 | [Losing Stakes Released to All Bidders](#15-losing-stakes-released-to-all-bidders) |
| 16 | [User Views Account Detail](#16-user-views-account-detail) |

---

## 01 User Login

```mermaid
sequenceDiagram
    actor User
    participant View as login.fxml
    participant Controller as LoginController
    participant Service as AuthService
    participant Repo as UserRepository
    participant DB as Database
    participant Entity as User

    User->>View: enters username + password
    User->>View: clicks Sign in

    View->>Controller: onSignInClick(ActionEvent)
    Controller->>View: usernameField.getText()
    View-->>Controller: "minh_99"
    Controller->>View: passwordField.getText()
    View-->>Controller: "secret123"

    Controller->>Service: login(username, password)

    Service->>Repo: findByUsername("minh_99")
    Repo->>DB: SELECT * FROM user WHERE username = ?
    DB-->>Repo: ResultSet row
    Repo->>Entity: new User(id, username, password, role, fullname)
    Entity-->>Repo: user object
    Repo-->>Service: User

    Service->>Service: PasswordUtil.verify(plain, user.getPassword())

    alt password matches
        Service-->>Controller: User
        Controller->>Controller: AppContext.setCurrentUser(user)
        Controller->>View: NavigationUtil.switchScene(dashboard.fxml)
        View-->>User: Dashboard screen
    else wrong credentials
        Service-->>Controller: throws AuthException
        Controller->>View: AlertUtil.showError("Invalid credentials")
        View-->>User: error alert
    end
```

---

## 02 User Register

```mermaid
sequenceDiagram
    actor User
    participant View as login.fxml
    participant Controller as LoginController
    participant Service as AuthService
    participant URepo as UserRepository
    participant ARepo as AccountRepository
    participant DB as Database
    participant UEntity as User
    participant AEntity as Account

    User->>View: fills username, password, fullname, selects role
    User->>View: clicks Create account

    View->>Controller: onRegisterClick(ActionEvent)
    Controller->>View: read all form fields
    View-->>Controller: username, password, fullname, role

    Controller->>Service: register(username, password, fullname, role)

    Service->>URepo: findByUsername(username)
    URepo->>DB: SELECT * FROM user WHERE username = ?
    DB-->>URepo: empty ResultSet
    URepo-->>Service: null

    Service->>Service: PasswordUtil.hash(password)
    Service->>UEntity: new User(username, hashedPw, role, fullname)
    UEntity-->>Service: user object

    Service->>URepo: save(user)
    URepo->>DB: INSERT INTO user VALUES (?, ?, ?, ?, ?)
    DB-->>URepo: generated id
    URepo-->>Service: User (with id)

    Service->>AEntity: new Account(userId, balance=0, lockedBalance=0)
    AEntity-->>Service: account object
    Service->>ARepo: save(account)
    ARepo->>DB: INSERT INTO account VALUES (?, ?, ?, ?)
    DB-->>ARepo: ok
    ARepo-->>Service: Account

    Service-->>Controller: User (registered)
    Controller->>View: NavigationUtil.switchScene(dashboard.fxml)
    View-->>User: Dashboard screen

    alt username already taken
        Service-->>Controller: throws DuplicateUserException
        Controller->>View: AlertUtil.showError("Username already exists")
        View-->>User: error alert
    end
```

---

## 03 Seller Uploads Item

```mermaid
sequenceDiagram
    actor Seller
    participant View as upload-item.fxml
    participant Controller as UploadItemController
    participant Service as ItemService
    participant Repo as ItemRepository
    participant DB as Database
    participant Entity as Item

    Seller->>View: fills item name, description, start price, category
    Seller->>View: clicks Submit for review

    View->>Controller: onSubmitClick(ActionEvent)
    Controller->>View: read all form fields
    View-->>Controller: name, description, beginPrice, category

    Controller->>Controller: validate fields not empty

    alt validation passes
        Controller->>Service: upload(name, description, beginPrice, category, ownerUserId)

        Service->>Entity: new Item(ownerUserId, beginPrice, status=PENDING)
        Entity-->>Service: item object

        Service->>Repo: save(item)
        Repo->>DB: INSERT INTO items (owner_user_id, begin_price, status) VALUES (?, ?, 'PENDING')
        DB-->>Repo: generated id
        Repo-->>Service: Item (with id)

        Service-->>Controller: Item
        Controller->>View: AlertUtil.showSuccess("Item submitted for review")
        Controller->>View: NavigationUtil.switchScene(item-browse.fxml)
        View-->>Seller: Browse items screen
    else validation fails
        Controller->>View: AlertUtil.showError("Please fill all fields")
        View-->>Seller: error alert
    end
```

---

## 04 Admin Approves / Rejects Item

```mermaid
sequenceDiagram
    actor Admin
    participant View as item-browse.fxml
    participant Controller as ItemBrowseController
    participant Service as ItemService
    participant Repo as ItemRepository
    participant DB as Database
    participant Entity as Item

    Admin->>View: views pending items list
    View->>Controller: initialize()
    Controller->>Service: listPending()
    Service->>Repo: findByStatus(PENDING)
    Repo->>DB: SELECT * FROM items WHERE status = 'PENDING'
    DB-->>Repo: ResultSet rows
    Repo->>Entity: new Item(...) for each row
    Entity-->>Repo: item objects
    Repo-->>Service: List<Item>
    Service-->>Controller: List<Item>
    Controller->>View: render item cards
    View-->>Admin: pending item list

    Admin->>View: clicks Approve on an item

    View->>Controller: onApproveClick(itemId)
    Controller->>Service: approve(itemId)

    Service->>Repo: findById(itemId)
    Repo->>DB: SELECT * FROM items WHERE id = ?
    DB-->>Repo: ResultSet row
    Repo->>Entity: new Item(...)
    Entity-->>Repo: item
    Repo-->>Service: Item

    Service->>Repo: updateStatus(itemId, PENDING)
    Repo->>DB: UPDATE items SET status = 'PENDING' WHERE id = ?
    DB-->>Repo: rows affected
    Repo-->>Service: void

    Service-->>Controller: void
    Controller->>View: refresh item list
    View-->>Admin: updated list

    alt Admin clicks Reject
        Controller->>Service: reject(itemId)
        Service->>Repo: updateStatus(itemId, SOLD)
        Repo->>DB: UPDATE items SET status = 'SOLD' WHERE id = ?
        DB-->>Repo: ok
        Service-->>Controller: void
        Controller->>View: AlertUtil.showSuccess("Item rejected")
        View-->>Admin: updated list
    end
```

---

## 05 Admin Creates Auction Session

```mermaid
sequenceDiagram
    actor Admin
    participant View as session-list.fxml
    participant Controller as SessionListController
    participant SessSvc as SessionService
    participant ItemSvc as ItemService
    participant SessRepo as SessionRepository
    participant ItemRepo as ItemRepository
    participant DB as Database
    participant SessEntity as Session
    participant ItemEntity as Item

    Admin->>View: selects an approved item, sets end time
    Admin->>View: clicks Create session

    View->>Controller: onCreateSessionClick(itemId, endTime)

    Controller->>ItemSvc: getById(itemId)
    ItemSvc->>ItemRepo: findById(itemId)
    ItemRepo->>DB: SELECT * FROM items WHERE id = ?
    DB-->>ItemRepo: ResultSet
    ItemRepo->>ItemEntity: new Item(...)
    ItemEntity-->>ItemRepo: item
    ItemRepo-->>ItemSvc: Item
    ItemSvc-->>Controller: Item

    Controller->>SessSvc: createSession(itemId, item.getBeginPrice(), endTime)

    SessSvc->>SessEntity: new Session(itemId, currentPrice=beginPrice, availabilityTime=endTime)
    SessEntity-->>SessSvc: session object

    SessSvc->>SessRepo: save(session)
    SessRepo->>DB: INSERT INTO session (item_id, current_price, availability_time) VALUES (?, ?, ?)
    DB-->>SessRepo: generated id
    SessRepo-->>SessSvc: Session (with id)

    SessSvc-->>Controller: Session
    Controller->>View: AlertUtil.showSuccess("Session created")
    Controller->>View: refresh session list
    View-->>Admin: updated session list with new session
```

---

## 06 User Joins Session

```mermaid
sequenceDiagram
    actor User
    participant ListView as session-list.fxml
    participant DetailView as session-detail.fxml
    participant Controller as SessionDetailController
    participant SessSvc as SessionService
    participant BidSvc as BidService
    participant AccSvc as AccountService
    participant SessRepo as SessionRepository
    participant BidRepo as BidRepository
    participant AccRepo as AccountRepository
    participant DB as Database

    User->>ListView: clicks Join session on a session card
    ListView->>Controller: onJoinClick(sessionId)

    Controller->>SessSvc: getById(sessionId)
    SessSvc->>SessRepo: findById(sessionId)
    SessRepo->>DB: SELECT * FROM session WHERE id = ?
    DB-->>SessRepo: ResultSet
    SessRepo-->>SessSvc: Session
    SessSvc-->>Controller: Session

    Controller->>BidSvc: getHistory(session.getItemId())
    BidSvc->>BidRepo: findByItemId(itemId)
    BidRepo->>DB: SELECT * FROM bid WHERE item_id = ? ORDER BY price DESC
    DB-->>BidRepo: ResultSet rows
    BidRepo-->>BidSvc: List<Bid>
    BidSvc-->>Controller: List<Bid>

    Controller->>AccSvc: getAvailable(currentUserId)
    AccSvc->>AccRepo: findByUserId(userId)
    AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
    DB-->>AccRepo: ResultSet
    AccRepo-->>AccSvc: Account
    AccSvc-->>Controller: availableBalance

    Controller->>DetailView: NavigationUtil.switchScene(session-detail.fxml)
    Controller->>DetailView: populate item name, current price, bid feed, balance
    Controller->>DetailView: start countdown timer (availabilityTime)
    DetailView-->>User: Session detail screen with live countdown
```

---

## 07 User Places Manual Bid

```mermaid
sequenceDiagram
    actor User
    participant View as session-detail.fxml
    participant Controller as SessionDetailController
    participant BidSvc as BidService
    participant StakeSvc as StakeService
    participant AccSvc as AccountService
    participant BidRepo as BidRepository
    participant StakeRepo as StakeRepository
    participant AccRepo as AccountRepository
    participant SessRepo as SessionRepository
    participant DB as Database
    participant BidEntity as Bid
    participant StakeEntity as Stake

    User->>View: enters bid amount
    User->>View: clicks Place Bid Now

    View->>Controller: onPlaceBidClick(ActionEvent)
    Controller->>View: bidAmountField.getText()
    View-->>Controller: 3300000

    Controller->>BidSvc: placeBid(userId, itemId, price=3300000)

    BidSvc->>SessRepo: findByItemId(itemId)
    SessRepo->>DB: SELECT * FROM session WHERE item_id = ?
    DB-->>SessRepo: ResultSet
    SessRepo-->>BidSvc: Session

    BidSvc->>BidSvc: validate price > session.getCurrentPrice()

    alt price is valid
        BidSvc->>AccSvc: getAvailable(userId)
        AccSvc->>AccRepo: findByUserId(userId)
        AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
        DB-->>AccRepo: ResultSet
        AccRepo-->>AccSvc: Account
        AccSvc-->>BidSvc: availableBalance

        BidSvc->>BidSvc: validate availableBalance >= price

        BidSvc->>StakeSvc: createStake(userId, itemId, amount=3300000)
        StakeSvc->>AccSvc: lockFunds(userId, 3300000)
        AccSvc->>AccRepo: lockBalance(userId, 3300000)
        AccRepo->>DB: UPDATE account SET locked_balance = locked_balance + ? WHERE user_id = ?
        DB-->>AccRepo: ok

        StakeSvc->>StakeEntity: new Stake(userId, itemId, amount=3300000)
        StakeEntity-->>StakeSvc: stake object
        StakeSvc->>StakeRepo: save(stake)
        StakeRepo->>DB: INSERT INTO stake (user_id, locked_items_id, amount) VALUES (?, ?, ?)
        DB-->>StakeRepo: ok

        BidSvc->>BidEntity: new Bid(userId, itemId, price=3300000)
        BidEntity-->>BidSvc: bid object
        BidSvc->>BidRepo: save(bid)
        BidRepo->>DB: INSERT INTO bid (user_id, item_id, price) VALUES (?, ?, ?)
        DB-->>BidRepo: generated id

        BidSvc->>SessRepo: updateCurrentPrice(sessionId, 3300000, userId)
        SessRepo->>DB: UPDATE session SET current_price = ?, current_user_id = ? WHERE id = ?
        DB-->>SessRepo: ok

        BidSvc-->>Controller: Bid
        Controller->>View: refresh currentPriceLabel
        Controller->>View: refresh bid feed
        Controller->>View: refresh balance display
        View-->>User: updated session detail
    else price too low
        BidSvc-->>Controller: throws InvalidBidException
        Controller->>View: AlertUtil.showError("Bid must exceed current price")
        View-->>User: error alert
    else insufficient balance
        BidSvc-->>Controller: throws InsufficientFundsException
        Controller->>View: AlertUtil.showError("Insufficient available balance")
        View-->>User: error alert
    end
```

---

## 08 Auto Bid Triggered

```mermaid
sequenceDiagram
    participant BidSvc as BidService
    participant AutoSvc as AutobidService
    participant StakeSvc as StakeService
    participant AccSvc as AccountService
    participant AutoRepo as AutobidRepository
    participant BidRepo as BidRepository
    participant StakeRepo as StakeRepository
    participant AccRepo as AccountRepository
    participant SessRepo as SessionRepository
    participant DB as Database
    participant BidEntity as Bid
    participant StakeEntity as Stake

    Note over BidSvc: Triggered after a manual bid is saved (Diagram 07)

    BidSvc->>AutoSvc: processAutobids(itemId, newCurrentPrice=3300000)

    AutoSvc->>AutoRepo: findActiveByItem(itemId)
    AutoRepo->>DB: SELECT * FROM auto_bid WHERE item_id = ? AND is_active = true
    DB-->>AutoRepo: ResultSet rows
    AutoRepo-->>AutoSvc: List<Autobid>

    loop for each active Autobid (excluding the bidder just placed)
        AutoSvc->>AutoSvc: check Autobid.getMaxPrice() > newCurrentPrice

        alt maxPrice allows a counter bid
            AutoSvc->>AccSvc: getAvailable(Autobid.getUserId())
            AccSvc->>AccRepo: findByUserId(userId)
            AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
            DB-->>AccRepo: ResultSet
            AccRepo-->>AccSvc: Account
            AccSvc-->>AutoSvc: availableBalance

            AutoSvc->>AutoSvc: nextBid = newCurrentPrice + increment (100000)
            AutoSvc->>AutoSvc: validate nextBid <= maxPrice AND availableBalance >= nextBid

            AutoSvc->>StakeSvc: releaseStake(userId, itemId)
            StakeSvc->>StakeRepo: findByUserAndItem(userId, itemId)
            StakeRepo->>DB: SELECT * FROM stake WHERE user_id = ? AND locked_items_id = ?
            DB-->>StakeRepo: Stake
            StakeSvc->>AccSvc: releaseFunds(userId, oldAmount)
            AccSvc->>AccRepo: unlockBalance(userId, oldAmount)
            AccRepo->>DB: UPDATE account SET locked_balance = locked_balance - ? WHERE user_id = ?
            DB-->>AccRepo: ok
            StakeSvc->>StakeRepo: delete(stakeId)
            StakeRepo->>DB: DELETE FROM stake WHERE id = ?
            DB-->>StakeRepo: ok

            AutoSvc->>StakeSvc: createStake(userId, itemId, nextBid)
            StakeSvc->>AccSvc: lockFunds(userId, nextBid)
            AccSvc->>AccRepo: lockBalance(userId, nextBid)
            AccRepo->>DB: UPDATE account SET locked_balance = locked_balance + ? WHERE user_id = ?
            DB-->>AccRepo: ok
            StakeSvc->>StakeEntity: new Stake(userId, itemId, nextBid)
            StakeSvc->>StakeRepo: save(stake)
            StakeRepo->>DB: INSERT INTO stake VALUES (?, ?, ?)
            DB-->>StakeRepo: ok

            AutoSvc->>BidEntity: new Bid(userId, itemId, nextBid)
            AutoSvc->>BidRepo: save(bid)
            BidRepo->>DB: INSERT INTO bid VALUES (?, ?, ?)
            DB-->>BidRepo: ok

            AutoSvc->>SessRepo: updateCurrentPrice(sessionId, nextBid, userId)
            SessRepo->>DB: UPDATE session SET current_price = ?, current_user_id = ? WHERE id = ?
            DB-->>SessRepo: ok
        else maxPrice exceeded
            AutoSvc->>AutoRepo: setActive(AutobidId, false)
            AutoRepo->>DB: UPDATE auto_bid SET is_active = false WHERE id = ?
            DB-->>AutoRepo: ok
            Note over AutoSvc: auto bid deactivated — max price reached
        end
    end

    AutoSvc-->>BidSvc: void
```

---

## 09 User Configures Auto Bid

```mermaid
sequenceDiagram
    actor User
    participant View as session-detail.fxml
    participant Controller as SessionDetailController
    participant AutoSvc as AutobidService
    participant AccSvc as AccountService
    participant AutoRepo as AutobidRepository
    participant AccRepo as AccountRepository
    participant DB as Database
    participant AutoEntity as Autobid

    User->>View: enters max price, selects increment
    User->>View: clicks Activate Auto Bid

    View->>Controller: onActivateAutobidClick(ActionEvent)
    Controller->>View: autoMaxField.getText()
    View-->>Controller: 5000000
    Controller->>View: incrementComboBox.getValue()
    View-->>Controller: 100000

    Controller->>AccSvc: getAvailable(currentUserId)
    AccSvc->>AccRepo: findByUserId(userId)
    AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
    DB-->>AccRepo: ResultSet
    AccRepo-->>AccSvc: Account
    AccSvc-->>Controller: availableBalance

    Controller->>Controller: validate maxPrice <= availableBalance

    alt validation passes
        Controller->>AutoSvc: configure(userId, itemId, maxPrice=5000000)

        AutoSvc->>AutoRepo: findByUserAndItem(userId, itemId)
        AutoRepo->>DB: SELECT * FROM auto_bid WHERE user_id = ? AND item_id = ?
        DB-->>AutoRepo: ResultSet

        alt auto bid record exists
            AutoRepo-->>AutoSvc: Autobid
            AutoSvc->>AutoRepo: save(updated maxPrice, is_active=true)
            AutoRepo->>DB: UPDATE auto_bid SET max_price = ?, is_active = true WHERE id = ?
            DB-->>AutoRepo: ok
        else no record yet
            AutoRepo-->>AutoSvc: null
            AutoSvc->>AutoEntity: new Autobid(userId, itemId, maxPrice=5000000, isActive=true)
            AutoEntity-->>AutoSvc: Autobid object
            AutoSvc->>AutoRepo: save(Autobid)
            AutoRepo->>DB: INSERT INTO auto_bid (user_id, item_id, max_price, is_active) VALUES (?, ?, ?, true)
            DB-->>AutoRepo: generated id
        end

        AutoSvc-->>Controller: Autobid
        Controller->>View: AlertUtil.showSuccess("Auto bid activated")
        Controller->>View: update auto bid status display
        View-->>User: auto bid active confirmation
    else insufficient balance
        Controller->>View: AlertUtil.showError("Max price exceeds available balance")
        View-->>User: error alert
    end
```

---

## 10 User Deposits Funds

```mermaid
sequenceDiagram
    actor User
    participant View as deposit.fxml
    participant Controller as DepositController
    participant AccSvc as AccountService
    participant AccRepo as AccountRepository
    participant DB as Database
    participant AccEntity as Account

    User->>View: selects payment method
    User->>View: enters deposit amount (5000000)
    User->>View: clicks Confirm deposit

    View->>Controller: onConfirmDepositClick(ActionEvent)
    Controller->>View: depositAmountField.getText()
    View-->>Controller: 5000000
    Controller->>View: methodGroup.getSelectedToggle()
    View-->>Controller: "Bank Card"

    Controller->>Controller: validate amount > 0

    alt amount valid
        Controller->>AccSvc: deposit(userId, amount=5000000)

        AccSvc->>AccRepo: findByUserId(userId)
        AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
        DB-->>AccRepo: ResultSet
        AccRepo->>AccEntity: new Account(id, userId, balance, lockedBalance)
        AccEntity-->>AccRepo: account
        AccRepo-->>AccSvc: Account

        AccSvc->>AccRepo: updateBalance(userId, newBalance = balance + 5000000)
        AccRepo->>DB: UPDATE account SET balance = balance + ? WHERE user_id = ?
        DB-->>AccRepo: rows affected
        AccRepo-->>AccSvc: void

        AccSvc-->>Controller: void
        Controller->>View: update balanceLabel, availableLabel
        Controller->>View: AlertUtil.showSuccess("Deposit successful")
        View-->>User: updated balance display
    else amount <= 0
        Controller->>View: AlertUtil.showError("Amount must be greater than zero")
        View-->>User: error alert
    end
```

---

## 11 Stake Locked When Bid Placed

```mermaid
sequenceDiagram
    participant BidSvc as BidService
    participant StakeSvc as StakeService
    participant AccSvc as AccountService
    participant StakeRepo as StakeRepository
    participant AccRepo as AccountRepository
    participant DB as Database
    participant StakeEntity as Stake
    participant AccEntity as Account

    Note over BidSvc: Called inside placeBid() after price validation (Diagram 07)

    BidSvc->>StakeSvc: createStake(userId, itemId, amount=3300000)

    StakeSvc->>AccSvc: getAvailable(userId)
    AccSvc->>AccRepo: findByUserId(userId)
    AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
    DB-->>AccRepo: ResultSet
    AccRepo->>AccEntity: new Account(...)
    AccEntity-->>AccRepo: account
    AccRepo-->>AccSvc: Account
    AccSvc-->>StakeSvc: availableBalance = 7200000

    StakeSvc->>StakeSvc: validate amount <= availableBalance

    StakeSvc->>AccSvc: lockFunds(userId, 3300000)
    AccSvc->>AccRepo: lockBalance(userId, 3300000)
    AccRepo->>DB: UPDATE account SET locked_balance = locked_balance + 3300000 WHERE user_id = ?
    DB-->>AccRepo: ok
    AccRepo-->>AccSvc: void
    AccSvc-->>StakeSvc: void

    StakeSvc->>StakeEntity: new Stake(lockedItemsId=itemId, userId, amount=3300000)
    StakeEntity-->>StakeSvc: stake object

    StakeSvc->>StakeRepo: save(stake)
    StakeRepo->>DB: INSERT INTO stake (locked_items_id, user_id, amount) VALUES (?, ?, ?)
    DB-->>StakeRepo: generated id
    StakeRepo-->>StakeSvc: Stake (with id)

    StakeSvc-->>BidSvc: Stake

    Note over AccRepo: account.locked_balance += 3300000
    Note over AccRepo: account.balance unchanged (total stays same)
    Note over StakeRepo: stake record created for audit trail
```

---

## 12 Stake Released When Outbid

```mermaid
sequenceDiagram
    participant BidSvc as BidService
    participant StakeSvc as StakeService
    participant AccSvc as AccountService
    participant StakeRepo as StakeRepository
    participant AccRepo as AccountRepository
    participant DB as Database
    participant AccEntity as Account

    Note over BidSvc: Called after a new highest bid is saved — previous leader is now outbid

    BidSvc->>StakeSvc: releaseStake(outbidUserId, itemId)

    StakeSvc->>StakeRepo: findByUserAndItem(outbidUserId, itemId)
    StakeRepo->>DB: SELECT * FROM stake WHERE user_id = ? AND locked_items_id = ?
    DB-->>StakeRepo: ResultSet row
    StakeRepo-->>StakeSvc: Stake (amount = 3200000)

    StakeSvc->>AccSvc: releaseFunds(outbidUserId, amount=3200000)
    AccSvc->>AccRepo: findByUserId(outbidUserId)
    AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
    DB-->>AccRepo: ResultSet
    AccRepo->>AccEntity: new Account(...)
    AccEntity-->>AccRepo: account
    AccRepo-->>AccSvc: Account

    AccSvc->>AccRepo: unlockBalance(outbidUserId, 3200000)
    AccRepo->>DB: UPDATE account SET locked_balance = locked_balance - 3200000 WHERE user_id = ?
    DB-->>AccRepo: ok
    AccRepo-->>AccSvc: void
    AccSvc-->>StakeSvc: void

    StakeSvc->>StakeRepo: delete(stakeId)
    StakeRepo->>DB: DELETE FROM stake WHERE id = ?
    DB-->>StakeRepo: ok
    StakeRepo-->>StakeSvc: void

    StakeSvc-->>BidSvc: void

    Note over AccRepo: locked_balance -= 3200000 (funds returned to available)
    Note over StakeRepo: stake record deleted — no longer locked
```

---

## 13 Session Timer Expires — Winner Declared

```mermaid
sequenceDiagram
    participant Timer as CountdownTimer
    participant Controller as SessionDetailController
    participant SessSvc as SessionService
    participant BidSvc as BidService
    participant SessRepo as SessionRepository
    participant BidRepo as BidRepository
    participant DB as Database

    Timer->>Timer: secondsLeft reaches 0
    Timer->>Controller: onSessionExpired(sessionId)

    Controller->>SessSvc: closeSession(sessionId)

    SessSvc->>SessRepo: findById(sessionId)
    SessRepo->>DB: SELECT * FROM session WHERE id = ?
    DB-->>SessRepo: ResultSet
    SessRepo-->>SessSvc: Session

    SessSvc->>BidSvc: getTopBid(session.getItemId())
    BidSvc->>BidRepo: findTopBid(itemId)
    BidRepo->>DB: SELECT * FROM bid WHERE item_id = ? ORDER BY price DESC LIMIT 1
    DB-->>BidRepo: ResultSet row
    BidRepo-->>BidSvc: Bid (highest)
    BidSvc-->>SessSvc: Bid

    alt bids exist — winner found
        SessSvc->>SessSvc: winnerId = topBid.getUserId()
        SessSvc->>SessRepo: close(sessionId)
        SessRepo->>DB: UPDATE session SET current_user_id = winnerId WHERE id = ?
        DB-->>SessRepo: ok

        SessSvc-->>Controller: winnerId
        Controller->>Controller: NavigationUtil.switchScene(winner.fxml)
        Controller->>Controller: pass winnerId + sessionId to WinnerController
    else no bids — session ends with no winner
        SessSvc->>SessRepo: close(sessionId)
        SessRepo->>DB: UPDATE session SET current_user_id = NULL WHERE id = ?
        DB-->>SessRepo: ok
        SessSvc-->>Controller: null
        Controller->>Controller: AlertUtil.showError("Session ended with no bids")
    end
```

---

## 14 Winner Balance Deducted

```mermaid
sequenceDiagram
    actor Winner
    participant View as winner.fxml
    participant Controller as WinnerController
    participant SessSvc as SessionService
    participant AccSvc as AccountService
    participant StakeSvc as StakeService
    participant ItemSvc as ItemService
    participant AccRepo as AccountRepository
    participant StakeRepo as StakeRepository
    participant ItemRepo as ItemRepository
    participant DB as Database

    Note over Controller: Reached from Diagram 13 after winner is declared

    Controller->>SessSvc: getById(sessionId)
    SessSvc-->>Controller: Session (currentUserId = winnerId, currentPrice = 3200000)

    Controller->>AccSvc: getBalance(winnerId)
    AccSvc->>AccRepo: findByUserId(winnerId)
    AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
    DB-->>AccRepo: ResultSet
    AccRepo-->>AccSvc: Account
    AccSvc-->>Controller: Account

    Controller->>AccSvc: deductWinningBid(winnerId, winningPrice=3200000)
    AccSvc->>AccRepo: findByUserId(winnerId)
    AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
    DB-->>AccRepo: Account

    AccSvc->>AccRepo: updateBalance(winnerId, balance - 3200000)
    AccRepo->>DB: UPDATE account SET balance = balance - 3200000 WHERE user_id = ?
    DB-->>AccRepo: ok

    AccSvc->>AccRepo: unlockBalance(winnerId, 3200000)
    AccRepo->>DB: UPDATE account SET locked_balance = locked_balance - 3200000 WHERE user_id = ?
    DB-->>AccRepo: ok
    AccRepo-->>AccSvc: void
    AccSvc-->>Controller: void

    Controller->>StakeSvc: releaseStake(winnerId, itemId)
    StakeSvc->>StakeRepo: findByUserAndItem(winnerId, itemId)
    StakeRepo->>DB: SELECT * FROM stake WHERE user_id = ? AND locked_items_id = ?
    DB-->>StakeRepo: Stake
    StakeSvc->>StakeRepo: delete(stakeId)
    StakeRepo->>DB: DELETE FROM stake WHERE id = ?
    DB-->>StakeRepo: ok

    Controller->>ItemSvc: markSold(itemId)
    ItemSvc->>ItemRepo: updateStatus(itemId, SOLD)
    ItemRepo->>DB: UPDATE items SET status = 'SOLD' WHERE id = ?
    DB-->>ItemRepo: ok

    Controller->>View: populate winner details (item name, winning price, session info)
    Controller->>View: show settled confirmation banner
    View-->>Winner: Winner screen — no payment needed
```

---

## 15 Losing Stakes Released to All Bidders

```mermaid
sequenceDiagram
    participant SessSvc as SessionService
    participant StakeSvc as StakeService
    participant AccSvc as AccountService
    participant StakeRepo as StakeRepository
    participant AccRepo as AccountRepository
    participant DB as Database

    Note over SessSvc: Called inside closeSession() after winner is declared (Diagram 13)

    SessSvc->>StakeSvc: releaseAll(itemId, excludeUserId=winnerId)

    StakeSvc->>StakeRepo: findByItemId(itemId)
    StakeRepo->>DB: SELECT * FROM stake WHERE locked_items_id = ?
    DB-->>StakeRepo: ResultSet rows
    StakeRepo-->>StakeSvc: List<Stake> (all non-winner stakes)

    loop for each Stake (losing bidder)
        StakeSvc->>AccSvc: releaseFunds(stake.getUserId(), stake.getAmount())

        AccSvc->>AccRepo: findByUserId(userId)
        AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
        DB-->>AccRepo: ResultSet
        AccRepo-->>AccSvc: Account

        AccSvc->>AccRepo: unlockBalance(userId, stake.getAmount())
        AccRepo->>DB: UPDATE account SET locked_balance = locked_balance - ? WHERE user_id = ?
        DB-->>AccRepo: ok
        AccRepo-->>AccSvc: void
        AccSvc-->>StakeSvc: void

        StakeSvc->>StakeRepo: delete(stake.getId())
        StakeRepo->>DB: DELETE FROM stake WHERE id = ?
        DB-->>StakeRepo: ok
    end

    StakeSvc-->>SessSvc: void

    Note over AccRepo: All losing bidders: locked_balance restored to available
    Note over StakeRepo: All losing stake records deleted — clean state
```

---

## 16 User Views Account Detail

```mermaid
sequenceDiagram
    actor User
    participant View as account.fxml
    participant Controller as AccountController
    participant AccSvc as AccountService
    participant StakeSvc as StakeService
    participant AccRepo as AccountRepository
    participant StakeRepo as StakeRepository
    participant BidRepo as BidRepository
    participant DB as Database
    participant AccEntity as Account
    participant StakeEntity as Stake

    User->>View: clicks My Account in sidebar

    View->>Controller: initialize()

    Controller->>AccSvc: getBalance(currentUserId)
    AccSvc->>AccRepo: findByUserId(userId)
    AccRepo->>DB: SELECT * FROM account WHERE user_id = ?
    DB-->>AccRepo: ResultSet row
    AccRepo->>AccEntity: new Account(id, userId, balance, lockedBalance)
    AccEntity-->>AccRepo: account
    AccRepo-->>AccSvc: Account
    AccSvc-->>Controller: Account

    Controller->>Controller: availableBalance = balance - lockedBalance

    Controller->>StakeSvc: getUserStakes(currentUserId)
    StakeSvc->>StakeRepo: findByUserId(userId)
    StakeRepo->>DB: SELECT * FROM stake WHERE user_id = ?
    DB-->>StakeRepo: ResultSet rows
    StakeRepo->>StakeEntity: new Stake(...) for each row
    StakeEntity-->>StakeRepo: stake objects
    StakeRepo-->>StakeSvc: List<Stake>
    StakeSvc-->>Controller: List<Stake>

    Controller->>BidRepo: findByUserId(userId)
    BidRepo->>DB: SELECT b.*, s.current_price FROM bid b JOIN session s ON b.item_id = s.item_id WHERE b.user_id = ? ORDER BY b.price DESC
    DB-->>BidRepo: ResultSet rows
    BidRepo-->>Controller: List<Bid> with session context

    Controller->>View: set totalBalanceLabel = balance
    Controller->>View: set pendingAmountLabel = lockedBalance
    Controller->>View: set availableLabel = availableBalance
    Controller->>View: render pendingBidsList (stake per active session)
    Controller->>View: render transactionsList (bid history)

    View-->>User: Account detail screen
    Note over View: Total = Available + Pending (locked)
```

---

*BidNow — JavaFX + JDBC + Maven · 16 Sequence Diagrams*
