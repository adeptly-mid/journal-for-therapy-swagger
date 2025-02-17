# DynamoDB Schema Design

This document outlines the updated DynamoDB schema design for an application that manages clients, therapists, their mappings, journal entries, sessions, messages, and logs journal-access requests.

---

## 1. Clients Table

**Table Name:** `Clients`

**Primary Key**

- **Partition Key:** `clientId` (String)

**Attributes**

- `clientId` (String)
- `email` (String)
- `name` (String)
- `passwordHash` (String)

**Global Secondary Index**

- **Name:** `GSI_Clients_ByEmail`
- **Partition Key:** `email` (String)
- **Purpose:** Allows login/authentication by email

---

## 2. Therapists Table

**Table Name:** `Therapists`

**Primary Key**

- **Partition Key:** `therapistId` (String)

**Attributes**

- `therapistId` (String)
- `email` (String)
- `name` (String)
- `specialization` (String)
- `passwordHash` (String)

**Global Secondary Indexes**

1. **GSI_Therapists_ByEmail**

   - **Partition Key:** `email` (String)
   - **Purpose:** Lookup by email for authentication

2. **GSI_Therapists_BySpecialization**
   - **Partition Key:** `specialization` (String)
   - **Purpose:** Query therapists by specialization

---

## 3. ClientTherapistMapping Table

**Table Name:** `ClientTherapistMapping`

**Primary Key**

- **Partition Key:** `clientId` (String)
- **Sort Key:** `therapistId` (String)

**Attributes**

- `status` (String)

**Global Secondary Index**

- **Name:** `GSI_Mapping_TherapistId`
- **Partition Key:** `therapistId`
- **Sort Key:** `clientId`
- **Purpose:** Lists all clients for a specific therapist

---

## 4. Journal Table

**Table Name:** `Journal`

**Primary Key**

- **Partition Key:** `clientId` (String)
- **Sort Key:** `journalEntryId` (String)

**Attributes**

- `timeOfEmotion` (String) – ISO-8601 date/time
- `feeling` (String)
- `intensity` (Number)

**Local Secondary Indexes**

1. **LSI_Journal_Time**

   - **Partition Key:** `clientId`
   - **Sort Key:** `timeOfEmotion`
   - **Purpose:** Enables chronological queries (e.g., “All entries for this client in time order”)

2. **LSI_Journal_ByFeeling**
   - **Partition Key:** `clientId`
   - **Sort Key:** `feeling`
   - **Purpose:** Allows filtering journal entries per client by `feeling`

---

## 5. Session Table

**Table Name:** `Session`

**Primary Key**

- **Partition Key:** `sessionId` (String)

**Attributes**

- `sessionId` (String)
- `therapistId` (String)
- `clientId` (String)
- `title` (String)
- `privateNotes` (String)
- `sharedNotes` (String)
- `status` (String)
- `startTime` (String) – ISO-8601 date/time
- `endTime` (String) – ISO-8601 date/time

**Global Secondary Indexes**

1. **GSI_Session_ByTherapist**

   - **Partition Key:** `therapistId`
   - **Sort Key:** `sessionId`
   - **Purpose:** Lists all sessions for a given therapist

2. **GSI_Session_ByClient**
   - **Partition Key:** `clientId`
   - **Sort Key:** `startTime`
   - **Purpose:** Enables a client to list their sessions with options to filter by therapist and session timings

---

## 6. Messages Table

**Table Name:** `Messages`

**Primary Key**

- **Partition Key:** `conversationId` (String)
- **Sort Key:** `createdAt` (String, ISO-8601 date/time)

**Attributes**

- `messageId` (String)
- `content` (String)
- `sender` (String)
- `receiver` (String)
- `clientId` (String)
- `therapistId` (String)

**Global Secondary Indexes**

1. **GSI_Messages_ByClient**

   - **Partition Key:** `clientId`
   - **Sort Key:** `createdAt`
   - **Purpose:** Allows a client to list all conversation messages across multiple therapists

2. **GSI_Messages_ByTherapist**
   - **Partition Key:** `therapistId`
   - **Sort Key:** `createdAt`
   - **Purpose:** Allows a therapist to list all conversation messages across multiple clients

---

## 7. JournalAccessLogs Table

**Table Name:** `JournalAccessLogs`

**Primary Key**

- **Partition Key:** `clientId` (String)
- **Sort Key:** `accessTimestamp` (String, ISO-8601 date/time)

**Attributes**

- `logId` (String)
- `clientId` (String)
- `userId` (String)
- `accessType` (String)
- `journalEntryId` (String)
- `details` (Map)

---

## 8. API Usage

### Clients

- **Sign Up**
  - `PutItem` in `Clients`
- **Login**
  - `Query` using `GSI_Clients_ByEmail`
- **Get Client**
  - `GetItem` by `clientId`

### Therapists

- **Sign Up**
  - `PutItem` in `Therapists`
- **Login**
  - `Query` using `GSI_Therapists_ByEmail`
- **Get Therapist**
  - `GetItem` by `therapistId`
- **Search by Specialization**
  - `Query` using `GSI_Therapists_BySpecialization`

### Client-Therapist Mapping

- **List Therapists for a Client**
  - `Query` by `clientId`
- **List Clients for a Therapist**
  - `Query` using `GSI_Mapping_TherapistId` by `therapistId`
- **Map / Unmap**
  - `PutItem` or `DeleteItem` in `ClientTherapistMapping`

### Journal

- **List Journal Entries**
  - `Query` the `Journal` table by `clientId`
- **Add Journal Entry**
  - `PutItem` with `clientId` and `journalEntryId`
- **Query by Time**
  - Use `LSI_Journal_Time`
- **Query by Feeling**
  - Use `LSI_Journal_ByFeeling`

### Session

- **Create Session**
  - `PutItem` in `Session` (include `clientId`, `therapistId`, `startTime`, and `endTime`)
- **List All Sessions**
  - `Scan` or use a specific index as needed
- **Get Session**
  - `GetItem` by `sessionId`
- **Update Session**
  - `UpdateItem` by `sessionId`
- **List Sessions by Therapist**
  - `Query` using `GSI_Session_ByTherapist`
- **List Sessions by Client with Filters**
  - `Query` using `GSI_Session_ByClient` and apply filter expressions for `therapistId` and session timings

### Messages

- **List Messages in a Conversation**
  - `Query` by `conversationId`
- **Send Message**
  - `PutItem` with the appropriate `conversationId` and `createdAt` timestamp
- **List All Conversations for a Client**
  - `Query` using `GSI_Messages_ByClient`
- **List All Conversations for a Therapist**
  - `Query` using `GSI_Messages_ByTherapist`

### JournalAccessLogs

- **Log a Journal Access Request**
  - `PutItem` in `JournalAccessLogs`
- **List Journal Access Logs for a Client**
  - `Query` by `clientId`
