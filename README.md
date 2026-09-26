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
- **Maven 3.9+**
- **MongoDB** (local or Atlas) – optional if you run the server with `--in-memory`

## Running

### 1. Start the server
```bash
# Using a local MongoDB on mongodb://localhost:27017
mvn compile exec:java

# Using MongoDB Atlas or another instance
JESMS_MONGO_URI="mongodb+srv://<user>:<password>@<cluster>/" mvn compile exec:java

# Without MongoDB (data is lost when the server stops)
mvn compile exec:java -Dexec.args="--in-memory"
```

| Option | Environment variable | Default |
|---|---|---|
| `--port N` | `JESMS_PORT` | `9999` |
| `--mongo-uri URI` | `JESMS_MONGO_URI` | `mongodb://localhost:27017` |
| `--db NAME` | `JESMS_DB_NAME` | `JeSMS` |
| `--in-memory` | – | off |

Never commit database passwords; pass them through `JESMS_MONGO_URI` (e.g. from a local `.env` file, which is git-ignored).

### 2. Start one or more clients
```bash
mvn javafx:run                                            # connects to localhost:9999
mvn javafx:run -Djavafx.args="--host 192.168.1.20 --port 9999"
```
`JESMS_HOST` and `JESMS_PORT` can be used instead of the arguments.

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
common/   Shared model (ChatMessage, Conversation, NetworkMessage) and Protocol (flags, payloads, validation)
server/   Server (connection handling and request logic)
server/db Database interface, MongoDatabase, InMemoryDatabase, PasswordHasher
client/   ClientApp, ClientNetworking, JeSMSController, dialogs, scenes/ and ui_components/
```

### Notes
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
