# DynamoDB Schema Design

## This document outlines a DynamoDB schema design for an application that manages clients, therapists, their mappings, journal entries, sessions, and messages.

## Table of Contents

1. [Clients Table](#1-clients-table)
2. [Therapists Table](#2-therapists-table)
3. [ClientTherapistMapping Table](#3-clienttherapistmapping-table)
4. [Journal Table](#4-journal-table)
5. [Session Table](#5-session-table)
6. [Messages Table](#6-messages-table)
7. [API Usage](#7-api-usage)

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

### Global Secondary Indexes

1. **By Email**

   - **Name:** `GSI_Therapists_ByEmail`
   - **Partition Key:** `email` (String)
   - **Purpose:** Lookup by email for authentication

2. **By Specialization**
   - **Name:** `GSI_Therapists_BySpecialization`
   - **Partition Key:** `specialization` (String)
   - **Purpose:** Query therapists by specialization

---

## 3. ClientTherapistMapping Table

**Table Name:** `ClientTherapistMapping`

**Primary Key**

- **Partition Key:** `clientId` (String)
- **Sort Key:** `therapistId` (String)

**Attributes**

- `status` (String) – e.g., active, pending, blocked

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

### Local Secondary Index

#### LSI: Journal Time

- **Name:** `LSI_Journal_Time`
- **Partition Key:** `clientId`
- **Sort Key:** `timeOfEmotion`
- **Purpose:** Enables chronological queries (e.g., “All entries for this client in time order”).

#### LSI: Emotion

- **Name:** `LSI_Journal_ByFeeling`
- **Partition Key:** `clientId` (same as the table)
- **Sort Key:** `feeling` (String)
- **Purpose:** Allows filtering journal entries **per client** by `feeling`. For example:  
   “Show me all entries for a specific `clientId` where `feeling` = 'happy'.”

## 5. Session Table

**Table Name:** `Session`

**Primary Key**

- **Partition Key:** `sessionId` (String)

**Attributes**

- `therapistId` (String)
- `title` (String)
- `privateNotes` (String)
- `sharedNotes` (String)
- `status` (String) – e.g., pending, approved, rejected, open, closed

**Global Secondary Index**

- **Name:** `GSI_Session_ByTherapist`
- **Partition Key:** `therapistId`
- **Sort Key:** `sessionId` (or another attribute)
- **Purpose:** Lists all sessions for a given therapist

---

## 6. Messages Table

**Table Name:** `Messages`

**Primary Key**

- **Partition Key:** `"CLIENT#<clientId>#THERAPIST#<therapistId>"` (String)
- **Sort Key:** `createdAt` (String, ISO-8601 date/time)

**Attributes**

- `content` (String)
- `sender` (String) – "client" or "therapist"
- `receiver` (String)
- `messageId` (String)

All messages between one client and therapist are stored in the same partition, sorted by creation time.

---

## 7. API Usage

### Clients

- **Sign Up**
  - `PutItem` in `Clients`
- **Login**
  - `Query` `GSI_Clients_ByEmail`
- **Get Client**
  - `GetItem` by `clientId`

### Therapists

- **Sign Up**
  - `PutItem` in `Therapists`
- **Login**
  - `Query` `GSI_Therapists_ByEmail`
- **Get Therapist**
  - `GetItem` by `therapistId`
- **Search by Specialization**
  - `Query` `GSI_Therapists_BySpecialization`

### Client-Therapist Mapping

- **List Therapists for a Client**
  - `Query` `ClientTherapistMapping` by `clientId`
- **List Clients for a Therapist**
  - `Query` `GSI_Mapping_TherapistId` by `therapistId`
- **Map / Unmap**
  - `PutItem` or `DeleteItem` in `ClientTherapistMapping`

### Journal

- **List Journal Entries**
  - `Query` `Journal` by `clientId`
- **Add Journal Entry**
  - `PutItem` with `PK=clientId`, `SK=journalEntryId`
- **Optional**: Query by Time or Feeling
  - **LSI_Journal_Time** (`timeOfEmotion`)
  - **GSI_Journal_ByFeeling** (`feeling`)

### Session

- **Create Session**
  - `PutItem` in `Session`
- **List All Sessions**
  - `Scan` or a specific index
- **Get Session**
  - `GetItem` by `sessionId`
- **Update Session**
  - `UpdateItem` by `sessionId`
- **List Sessions by Therapist**
  - `Query` `GSI_Session_ByTherapist`

### Messages

- **List Messages**
  - `Query` `Messages` by the composite partition key `"CLIENT#<clientId>#THERAPIST#<therapistId>"`
- **Send Message**
  - `PutItem` with that same partition key and a new timestamp as the sort key

---
