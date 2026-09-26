# JeSMS – Java Messaging App

A real-time messaging application with a JavaFX desktop client and a socket server backed by **MongoDB**.

- **Server:** accepts client connections, authenticates users, stores users/conversations in MongoDB and routes messages to online users in real time.
- **Client:** a **JavaFX** desktop app for registering, logging in and chatting.

## Features
- [x] Server–client architecture over TCP sockets
- [x] MongoDB backend for storing user and message data (plus an in-memory mode for development)
- [x] User registration, login, logout and password change, with salted PBKDF2 password hashing
- [x] Direct messages and named group chats
- [x] Real-time delivery of messages, edits, deletions and new conversations to online users
- [x] Edit and delete your own messages (right-click a message you sent)
- [x] Read receipts ("Seen" / "Seen by N") and unread message counts
- [x] Add members to group chats and leave conversations
- [x] Message history loaded from the database on login
- [x] Conversation filtering and sorting by most recent activity

## Requirements
- **Java 21+**
- **Maven 3.9+** (bundled with IntelliJ IDEA, so only needed separately for the command line)
- **MongoDB** (local or Atlas) – optional if you run the server in in-memory mode

## Configuration (.env)
Settings, including the database password, live in a `.env` file in the project root. It is git-ignored so secrets never get committed.

1. Copy the template: `cp .env.example .env` (or copy and rename it in IntelliJ).
2. Set `JESMS_MONGO_URI` to your connection string, including the password. For Atlas, copy it from *Connect > Drivers*.
   If the password contains `@ : / ? # %`, URL-encode those characters.

| Setting | Used by | Default | Command-line override |
|---|---|---|---|
| `JESMS_MONGO_URI` | server | `mongodb://localhost:27017` | `--mongo-uri URI` |
| `JESMS_DB_NAME` | server | `JeSMS` | `--db NAME` |
| `JESMS_PORT` | server and client | `9999` | `--port N` |
| `JESMS_HOST` | client | `localhost` | `--host HOST` |

Precedence: command-line argument, then environment variable, then `.env`, then the default.
The `.env` file is read from the working directory, which is the project root in both IntelliJ and Maven.
The server also accepts `--in-memory` to run without MongoDB (data is lost when it stops).

Never put the password in source code, `.env.example`, IntelliJ run configurations or commit messages.

## Running in IntelliJ IDEA
1. **Open the project:** *File > Open*, select the project folder (the one containing `pom.xml`) and trust the project. IntelliJ imports it as a Maven project; if prompted, click *Load Maven Project*.
2. **Set the JDK:** *File > Project Structure > Project*, set *SDK* to Java 21 or newer (use *Add SDK > Download JDK* if you don't have one) and *Language level* to 21.
3. **Create your `.env`** as described above.
4. **Run the server:** choose **Server** in the run configuration dropdown (top right) and click Run. Use **Server (in-memory)** to try the app without MongoDB.
   The console should show `[START] Server started on port 9999...`.
5. **Run a client:** choose **Client** and click Run. To open several clients (to chat between accounts), go to
   *Run > Edit Configurations > Client > Modify options* and tick *Allow multiple instances*, then run it again.
6. **Run the tests:** right-click `src/test/java` and choose *Run 'All Tests'*.

The run configurations are shared in `.idea/runConfigurations`. The client is launched through `ClientLauncher` because running
`ClientApp` directly from an IDE fails with *"JavaFX runtime components are missing"*.
If the configurations don't appear, open the Maven tool window (right edge), click *Reload All Maven Projects*, and restart IntelliJ.

## Running from the command line
```bash
# Server (reads .env)
mvn compile exec:java
# Server without MongoDB
mvn compile exec:java -Dexec.args="--in-memory"

# Client (run once per window you want)
mvn javafx:run
mvn javafx:run -Djavafx.args="--host 192.168.1.20 --port 9999"
```

## Using the app
- **Register** an account (usernames: 5–15 letters, numbers or `_`; passwords: 5–64 characters), then **Login**.
- Sidebar: new conversation, filter conversations, settings (change password), and log out.
- Click a conversation to open it; this marks its messages as read. Press **Enter** to send, **Shift+Enter** for a new line.
- Right-click one of your messages to **Edit** or **Delete** it.
- In a group chat use **+** in the header to add a member; use the leave button to leave any conversation.

## Tests
```bash
mvn test
```
The integration tests start a real server with in-memory storage and exercise the protocol over sockets
(accounts, conversations, real-time delivery, edit/delete permissions, read receipts, membership changes and input filtering).

## Project structure
```
common/   Shared model (ChatMessage, Conversation, NetworkMessage), Protocol (flags, payloads, validation) and Config (.env loading)
server/   Server (connection handling and request logic)
server/db Database interface, MongoDatabase, InMemoryDatabase, PasswordHasher
client/   ClientApp (+ ClientLauncher for IDEs), ClientNetworking, JeSMSController, dialogs, scenes/ and ui_components/
```

### Notes
- The server prints `[CONFIG] Loaded settings from .../.env` when it finds your `.env`; it never prints its values.
- Accounts created before password hashing was added (plain-text passwords) still log in and are upgraded to a hash automatically on their next login.
- Only the shared model classes can be deserialized by the server; anything else closes the connection.

## Technologies Used
- **Java 21**
- **JavaFX** (Client UI)
- **MongoDB Java Driver** (Database)
- **JUnit 5** (Tests)
- **Maven** (Build tool)

## Contributors


| Contributor | GitHub |
|-------------|--------|
| [![Christian](https://github.com/ChristianGleitzman.png?size=50)](https://github.com/ChristianGleitzman) | [Christian](https://github.com/ChristianGleitzman) |
| [![Iker](https://github.com/Iks31.png?size=50)](https://github.com/Iks31) | [Iker](https://github.com/Iks31) |
