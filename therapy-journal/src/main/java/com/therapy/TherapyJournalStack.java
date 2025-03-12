package com.therapy;

import java.util.Map;

import software.constructs.Construct;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.LocalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.ProjectionType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

public class TherapyJournalStack extends Stack {

  public TherapyJournalStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public TherapyJournalStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    // ------------------------------
    // DynamoDB Tables
    // ------------------------------
    // Clients Table with GSI on email
    Table clientsTable = Table.Builder.create(this, "ClientsTable")
        .tableName("Clients")
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .partitionKey(Attribute.builder()
            .name("clientId")
            .type(AttributeType.STRING)
            .build())
        .build();

    clientsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
        .indexName("GSI_Clients_ByEmail")
        .partitionKey(Attribute.builder()
            .name("email")
            .type(AttributeType.STRING)
            .build())
        .projectionType(ProjectionType.ALL)
        .build());

    // Therapists Table with GSI on email
    Table therapistsTable = Table.Builder.create(this, "TherapistsTable")
        .tableName("Therapists")
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .partitionKey(Attribute.builder()
            .name("therapistId")
            .type(AttributeType.STRING)
            .build())
        .build();

    therapistsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
        .indexName("GSI_Therapists_ByEmail")
        .partitionKey(Attribute.builder()
            .name("email")
            .type(AttributeType.STRING)
            .build())
        .projectionType(ProjectionType.ALL)
        .build());

    // Client-Therapist Mapping Table with GSI on therapistId
    Table mappingTable = Table.Builder.create(this, "MappingTable")
        .tableName("ClientTherapistMapping")
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .partitionKey(Attribute.builder()
            .name("clientId")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("therapistId")
            .type(AttributeType.STRING)
            .build())
        .build();

    mappingTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
        .indexName("GSI_Mapping_TherapistId")
        .partitionKey(Attribute.builder()
            .name("therapistId")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("clientId")
            .type(AttributeType.STRING)
            .build())
        .projectionType(ProjectionType.ALL)
        .build());

    // Journal Table with two LSIs: time and feeling
    Table journalTable = Table.Builder.create(this, "JournalTable")
        .tableName("Journal")
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .partitionKey(Attribute.builder()
            .name("clientId")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("journalEntryId")
            .type(AttributeType.STRING)
            .build())
        .build();

    journalTable.addLocalSecondaryIndex(LocalSecondaryIndexProps.builder()
        .indexName("LSI_Journal_Time")
        .sortKey(Attribute.builder()
            .name("timeOfEmotion")
            .type(AttributeType.STRING)
            .build())
        .projectionType(ProjectionType.ALL)
        .build());

    journalTable.addLocalSecondaryIndex(LocalSecondaryIndexProps.builder()
        .indexName("LSI_Journal_ByFeeling")
        .sortKey(Attribute.builder()
            .name("feeling")
            .type(AttributeType.STRING)
            .build())
        .projectionType(ProjectionType.ALL)
        .build());

    // JournalAccessLogs Table
    Table journalAccessLogsTable = Table.Builder.create(this, "JournalAccessLogsTable")
        .tableName("JournalAccessLogs")
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .partitionKey(Attribute.builder()
            .name("clientId")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("accessTimestamp")
            .type(AttributeType.STRING)
            .build())
        .build();

    // JournalAccessStatus Table
    Table journalAccessStatusTable = Table.Builder.create(this, "JournalAccessStatusTable")
        .tableName("JournalAccessStatus")
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .partitionKey(Attribute.builder()
            .name("clientId")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("therapistId")
            .type(AttributeType.STRING)
            .build())
        .build();

    // Session Table with GSIs
    Table sessionTable = Table.Builder.create(this, "SessionTable")
        .tableName("Session")
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .partitionKey(Attribute.builder()
            .name("sessionId")
            .type(AttributeType.STRING)
            .build())
        .build();

    sessionTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
        .indexName("GSI_Session_ByTherapist")
        .partitionKey(Attribute.builder()
            .name("therapistId")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("sessionId")
            .type(AttributeType.STRING)
            .build())
        .projectionType(ProjectionType.ALL)
        .build());

    sessionTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
        .indexName("GSI_Session_ByClient")
        .partitionKey(Attribute.builder()
            .name("clientId")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("startTime")
            .type(AttributeType.STRING)
            .build())
        .projectionType(ProjectionType.ALL)
        .build());

    // Messages Table
    Table messagesTable = Table.Builder.create(this, "MessagesTable")
        .tableName("Messages")
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .partitionKey(Attribute.builder()
            .name("conversationId")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("createdAt")
            .type(AttributeType.STRING)
            .build())
        .build();

    // ------------------------------
    // Lambda Functions
    // ------------------------------
    // Helper method is omitted in this file for brevity; each function is defined
    // below.

    Function createClientLambda = Function.Builder.create(this, "CreateClientLambda")
        .functionName("CreateClientLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.auth.CreateClientHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("CLIENTS_TABLE", clientsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function clientloginLambda = Function.Builder.create(this, "ClientLoginLambda")
        .functionName("ClientLoginLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.auth.ClientLoginHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("CLIENTS_TABLE", clientsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function createTherapistLambda = Function.Builder.create(this, "CreateTherapistLambda")
        .functionName("CreateTherapistLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.auth.CreateTherapistHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("THERAPISTS_TABLE", therapistsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function therapistloginLambda = Function.Builder.create(this, "TherapistLoginLambda")
        .functionName("TherapistLoginLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.auth.TherapistLoginHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("THERAPISTS_TABLE", therapistsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function getClientLambda = Function.Builder.create(this, "GetClientLambda")
        .functionName("GetClientLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.client.GetClientHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("CLIENTS_TABLE", clientsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function getTherapistLambda = Function.Builder.create(this, "GetTherapistLambda")
        .functionName("GetTherapistLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.therapist.GetTherapistHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("THERAPISTS_TABLE", therapistsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    // Mapping Lambdas
    Function listTherapistsForClientLambda = Function.Builder.create(this, "ListTherapistsForClientLambda")
        .functionName("ListTherapistsForClientLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.mapping.ListTherapistsForClientHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(
            Map.of("MAPPING_TABLE", mappingTable.getTableName(), "THERAPISTS_TABLE", therapistsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function mapTherapistToClientLambda = Function.Builder.create(this, "MapTherapistToClientLambda")
        .functionName("MapTherapistToClientLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.mapping.MapTherapistToClientHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("MAPPING_TABLE", mappingTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function removeTherapistFromClientLambda = Function.Builder.create(this, "RemoveTherapistFromClientLambda")
        .functionName("RemoveTherapistFromClientLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.mapping.RemoveTherapistFromClientHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("MAPPING_TABLE", mappingTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function listClientsForTherapistLambda = Function.Builder.create(this, "ListClientsForTherapistLambda")
        .functionName("ListClientsForTherapistLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.mapping.ListClientsForTherapistHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("MAPPING_TABLE", mappingTable.getTableName(), "CLIENTS_TABLE", clientsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function requestMappingLambda = Function.Builder.create(this, "RequestMappingLambda")
        .functionName("RequestMappingLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.mapping.RequestMappingHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("MAPPING_TABLE", mappingTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function removeClientFromTherapistLambda = Function.Builder.create(this, "RemoveClientFromTherapistLambda")
        .functionName("RemoveClientFromTherapistLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.mapping.RemoveClientFromTherapistHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("MAPPING_TABLE", mappingTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    // Journaling Lambdas
    Function listJournalEntryForClientLambda = Function.Builder.create(this, "ListJournalEntryForClientLambda")
        .functionName("ListJournalEntryForClientLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.journal.ListJournalEntryForClientHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("JOURNAL_TABLE", journalTable.getTableName(), "ACCESS_LOGS_TABLE",
            journalAccessLogsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function createJournalEntryLambda = Function.Builder.create(this, "CreateJournalEntryLambda")
        .functionName("CreateJournalEntryLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.journal.CreateJournalEntryHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("JOURNAL_TABLE", journalTable.getTableName(),
            "ACCESS_LOGS_TABLE", journalAccessLogsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function requestJournalAccessLambda = Function.Builder.create(this, "RequestJournalAccessLambda")
        .functionName("RequestJournalAccessLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.journal.RequestJournalAccessHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("ACCESS_LOGS_TABLE", journalAccessLogsTable.getTableName(),
            "JOURNAL_ACCESS_STATUS_TABLE", journalAccessStatusTable.getTableName(),
            "CLIENTS_TABLE", clientsTable.getTableName(),
            "THERAPISTS_TABLE", therapistsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function resolveTherapistAccessRequestLambda = Function.Builder.create(this, "ResolveTherapistAccessRequestLambda")
        .functionName("ResolveTherapistAccessRequestLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.journal.ResolveTherapistAccessRequestHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("ACCESS_LOGS_TABLE", journalAccessLogsTable.getTableName(),
            "JOURNAL_ACCESS_STATUS_TABLE", journalAccessStatusTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function therapistViewJournalLambda = Function.Builder.create(this, "TherapistViewJournalLambda")
        .functionName("TherapistViewJournalLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.journal.TherapistViewJournalHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of(
            "JOURNAL_TABLE", journalTable.getTableName(),
            "JOURNAL_ACCESS_STATUS_TABLE", journalAccessStatusTable.getTableName(),
            "ACCESS_LOGS_TABLE", journalAccessLogsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    // Session Lambdas (Legacy endpoints under /session)
    Function createSessionLambda = Function.Builder.create(this, "CreateSessionLambda")
        .functionName("CreateSessionLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.sessions.CreateSessionHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("SESSION_TABLE", sessionTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function listSessionsLambda = Function.Builder.create(this, "ListSessionsLambda")
        .functionName("ListSessionsLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.sessions.ListSessionsHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("SESSION_TABLE", sessionTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function getSessionLambda = Function.Builder.create(this, "GetSessionLambda")
        .functionName("GetSessionLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.sessions.GetSessionHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("SESSION_TABLE", sessionTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function updateSessionLambda = Function.Builder.create(this, "UpdateSessionLambda")
        .functionName("UpdateSessionLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.sessions.UpdateSessionHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("SESSION_TABLE", sessionTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function appointmentRequestLambda = Function.Builder.create(this, "AppointmentRequestLambda")
        .functionName("AppointmentRequestLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.sessions.AppointmentRequestHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("SESSION_TABLE", sessionTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    // New Session Endpoints under /therapists/{therapistId}/sessions
    Function createSessionForTherapistLambda = Function.Builder.create(this, "CreateSessionForTherapistLambda")
        .functionName("CreateSessionForTherapistLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.therapist.CreateSessionForTherapistHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("SESSION_TABLE", sessionTable.getTableName(),
            "THERAPISTS_TABLE", therapistsTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function listSessionsForTherapistLambda = Function.Builder.create(this, "ListSessionsForTherapistLambda")
        .functionName("ListSessionsForTherapistLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.therapist.ListSessionsForTherapistHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("SESSION_TABLE", sessionTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function getSessionForTherapistLambda = Function.Builder.create(this, "GetSessionForTherapistLambda")
        .functionName("GetSessionForTherapistLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.therapist.GetSessionForTherapistHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("SESSION_TABLE", sessionTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function updateSessionForTherapistLambda = Function.Builder.create(this, "UpdateSessionForTherapistLambda")
        .functionName("UpdateSessionForTherapistLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.therapist.UpdateSessionForTherapistHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("SESSION_TABLE", sessionTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    // Messaging Lambdas
    Function clientGetMessagesLambda = Function.Builder.create(this, "ClientGetMessagesLambda")
        .functionName("ClientGetMessagesLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.messages.ClientGetMessagesHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("MESSAGES_TABLE", messagesTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function clientSendMessageLambda = Function.Builder.create(this, "ClientSendMessageLambda")
        .functionName("ClientSendMessageLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.messages.ClientSendMessageHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("MESSAGES_TABLE", messagesTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function therapistGetMessagesLambda = Function.Builder.create(this, "TherapistGetMessagesLambda")
        .functionName("TherapistGetMessagesLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.messages.TherapistGetMessagesHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("MESSAGES_TABLE", messagesTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function therapistSendMessageLambda = Function.Builder.create(this, "TherapistSendMessageLambda")
        .functionName("TherapistSendMessageLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.messages.TherapistSendMessageHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("MESSAGES_TABLE", messagesTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    // Search Lambdas
    Function clientSearchLambda = Function.Builder.create(this, "ClientSearchLambda")
        .functionName("ClientSearchLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.search.ClientSearchHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("JOURNAL_TABLE", journalTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    Function therapistSearchLambda = Function.Builder.create(this, "TherapistSearchLambda")
        .functionName("TherapistSearchLambda")
        .runtime(Runtime.JAVA_17)
        .handler("com.lambda.search.TherapistSearchHandler::handleRequest")
        .code(Code.fromAsset("target/therapy-journal-0.1.jar"))
        .environment(Map.of("JOURNAL_TABLE", journalTable.getTableName(),
            "JOURNAL_ACCESS_STATUS_TABLE", journalAccessStatusTable.getTableName()))
        .timeout(Duration.seconds(30))
        .memorySize(512)
        .build();

    // ------------------------------
    // Grant Table Permissions
    // ------------------------------
    // Clients & Therapists
    clientsTable.grantReadWriteData(createClientLambda);
    clientsTable.grantReadWriteData(clientloginLambda);
    clientsTable.grantReadData(getClientLambda);
    clientsTable.grantReadData(listTherapistsForClientLambda);
    clientsTable.grantReadWriteData(requestJournalAccessLambda);
    clientsTable.grantReadData(listClientsForTherapistLambda);

    therapistsTable.grantReadWriteData(createTherapistLambda);
    therapistsTable.grantReadWriteData(therapistloginLambda);
    therapistsTable.grantReadData(getTherapistLambda);
    therapistsTable.grantReadData(listTherapistsForClientLambda);
    therapistsTable.grantReadWriteData(requestJournalAccessLambda);

    // Mapping Table
    mappingTable.grantReadWriteData(listTherapistsForClientLambda);
    mappingTable.grantReadWriteData(mapTherapistToClientLambda);
    mappingTable.grantReadWriteData(removeTherapistFromClientLambda);
    mappingTable.grantReadWriteData(listClientsForTherapistLambda);
    mappingTable.grantReadWriteData(requestMappingLambda);
    mappingTable.grantReadWriteData(removeClientFromTherapistLambda);

    // Journal Table
    journalTable.grantReadData(listJournalEntryForClientLambda);
    journalTable.grantReadWriteData(createJournalEntryLambda);
    journalTable.grantReadData(therapistViewJournalLambda);
    journalTable.grantReadData(clientSearchLambda);
    journalTable.grantReadData(therapistSearchLambda);

    // JournalAccessLogs Table
    journalAccessLogsTable.grantReadWriteData(requestJournalAccessLambda);
    journalAccessLogsTable.grantReadWriteData(resolveTherapistAccessRequestLambda);
    journalAccessLogsTable.grantReadWriteData(listJournalEntryForClientLambda);
    journalAccessLogsTable.grantReadWriteData(createJournalEntryLambda);
    journalAccessLogsTable.grantReadWriteData(therapistViewJournalLambda);
    // JournalAccessStatus Table
    journalAccessStatusTable.grantReadWriteData(requestJournalAccessLambda);
    journalAccessStatusTable.grantReadWriteData(resolveTherapistAccessRequestLambda);
    journalAccessStatusTable.grantReadWriteData(therapistViewJournalLambda);
    journalAccessStatusTable.grantReadData(therapistSearchLambda);

    // Session Table
    sessionTable.grantReadWriteData(createSessionLambda);
    sessionTable.grantReadWriteData(listSessionsLambda);
    sessionTable.grantReadWriteData(getSessionLambda);
    sessionTable.grantReadWriteData(updateSessionLambda);
    sessionTable.grantReadWriteData(appointmentRequestLambda);
    sessionTable.grantReadWriteData(createSessionForTherapistLambda);
    sessionTable.grantReadWriteData(listSessionsForTherapistLambda);
    sessionTable.grantReadWriteData(getSessionForTherapistLambda);
    sessionTable.grantReadWriteData(updateSessionForTherapistLambda);

    // Messages Table
    messagesTable.grantReadWriteData(clientGetMessagesLambda);
    messagesTable.grantReadWriteData(clientSendMessageLambda);
    messagesTable.grantReadWriteData(therapistGetMessagesLambda);
    messagesTable.grantReadWriteData(therapistSendMessageLambda);

    // ------------------------------
    // API Gateway Endpoints
    // ------------------------------
    RestApi api = RestApi.Builder.create(this, "TherapyJournalAPI")
        .restApiName("Therapy Journal Service")
        .build();

    // --- Auth Endpoints (/auth) ---
    Resource auth = api.getRoot().addResource("auth");
    Resource clientAuth = auth.addResource("client");
    Resource therapistAuth = auth.addResource("therapist");

    Resource clientsignup = clientAuth.addResource("signup");
    clientsignup.addMethod("POST", new LambdaIntegration(createClientLambda));

    Resource clientlogin = clientAuth.addResource("login");
    clientlogin.addMethod("POST", new LambdaIntegration(clientloginLambda));

    Resource therapistsignup = therapistAuth.addResource("signup");
    therapistsignup.addMethod("POST", new LambdaIntegration(createTherapistLambda));

    Resource therapistlogin = therapistAuth.addResource("login");
    therapistlogin.addMethod("POST", new LambdaIntegration(therapistloginLambda));

    // --- Client & Therapist GET (/clients/{clientId}, /therapists/{therapistId})
    Resource clientsResource = api.getRoot().addResource("clients");
    Resource clientById = clientsResource.addResource("{clientId}");
    clientById.addMethod("GET", new LambdaIntegration(getClientLambda));

    Resource therapistsResource = api.getRoot().addResource("therapists");
    Resource therapistById = therapistsResource.addResource("{therapistId}");
    therapistById.addMethod("GET", new LambdaIntegration(getTherapistLambda));

    // --- New Therapist Sessions Endpoints (/therapists/{therapistId}/sessions)
    Resource therapistSessions = therapistById.addResource("sessions");
    therapistSessions.addMethod("POST", new LambdaIntegration(createSessionForTherapistLambda));
    therapistSessions.addMethod("GET", new LambdaIntegration(listSessionsForTherapistLambda));
    Resource therapistSessionById = therapistSessions.addResource("{sessionId}");
    therapistSessionById.addMethod("GET", new LambdaIntegration(getSessionForTherapistLambda));
    therapistSessionById.addMethod("PUT", new LambdaIntegration(updateSessionForTherapistLambda));

    // --- Mapping Endpoints (/mapping) ---
    Resource mapping = api.getRoot().addResource("mapping");
    Resource clientMapping = mapping.addResource("client");
    Resource clientResource = clientMapping.addResource("{clientId}");
    Resource therapistForClient = clientResource.addResource("therapist");
    therapistForClient.addMethod("GET", new LambdaIntegration(listTherapistsForClientLambda));
    therapistForClient.addMethod("POST", new LambdaIntegration(mapTherapistToClientLambda));
    Resource therapistForClientById = therapistForClient.addResource("{therapistId}");
    therapistForClientById.addMethod("DELETE", new LambdaIntegration(removeTherapistFromClientLambda));

    Resource therapistMapping = mapping.addResource("therapist");
    Resource therapistResource = therapistMapping.addResource("{therapistId}");
    Resource clientForTherapist = therapistResource.addResource("client");
    clientForTherapist.addMethod("GET", new LambdaIntegration(listClientsForTherapistLambda));
    Resource clientForTherapistById = clientForTherapist.addResource("{clientId}");
    clientForTherapistById.addMethod("POST", new LambdaIntegration(requestMappingLambda));
    clientForTherapistById.addMethod("DELETE", new LambdaIntegration(removeClientFromTherapistLambda));

    // --- Journaling Endpoints (/journal) ---
    Resource journal = api.getRoot().addResource("journal");
    Resource journalClient = journal.addResource("client");
    Resource journalClientId = journalClient.addResource("{clientId}");
    journalClientId.addMethod("GET", new LambdaIntegration(listJournalEntryForClientLambda));
    journalClientId.addMethod("POST", new LambdaIntegration(createJournalEntryLambda));

    Resource journalClientTherapist = journalClientId.addResource("therapist");
    Resource journalClientTherapistId = journalClientTherapist.addResource("{therapistId}");
    Resource journalClientTherapistAccess = journalClientTherapistId.addResource("access");
    journalClientTherapistAccess.addMethod("PATCH", new LambdaIntegration(resolveTherapistAccessRequestLambda));

    Resource journalTherapist = journal.addResource("therapist");
    Resource journalTherapistId = journalTherapist.addResource("{therapistId}");
    Resource journalTherapistClient = journalTherapistId.addResource("client");
    Resource journalTherapistClientId = journalTherapistClient.addResource("{clientId}");
    journalTherapistClientId.addMethod("GET", new LambdaIntegration(therapistViewJournalLambda));
    Resource journalTherapistClientRequestAccess = journalTherapistClientId.addResource("request-access");
    journalTherapistClientRequestAccess.addMethod("POST", new LambdaIntegration(requestJournalAccessLambda));

    // --- Legacy Session Endpoints (/session) ---
    Resource sessionResource = api.getRoot().addResource("session");
    sessionResource.addMethod("POST", new LambdaIntegration(createSessionLambda));
    sessionResource.addMethod("GET", new LambdaIntegration(listSessionsLambda));
    Resource sessionById = sessionResource.addResource("{sessionId}");
    sessionById.addMethod("GET", new LambdaIntegration(getSessionLambda));
    sessionById.addMethod("PUT", new LambdaIntegration(updateSessionLambda));
    Resource appointmentResource = sessionById.addResource("appointment");
    appointmentResource.addMethod("POST", new LambdaIntegration(appointmentRequestLambda));

    // --- Messages Endpoints ---
    Resource messages = api.getRoot().addResource("messages");
    Resource messagesClient = messages.addResource("client");
    Resource messagesClientId = messagesClient.addResource("{clientId}");
    Resource messagesClientTherapist = messagesClientId.addResource("therapist");
    Resource messagesClientTherapistId = messagesClientTherapist.addResource("{therapistId}");
    messagesClientTherapistId.addMethod("GET", new LambdaIntegration(clientGetMessagesLambda));
    messagesClientTherapistId.addMethod("POST", new LambdaIntegration(clientSendMessageLambda));

    Resource messagesTherapist = messages.addResource("therapist");
    Resource messagesTherapistId = messagesTherapist.addResource("{therapistId}");
    Resource messagesTherapistClient = messagesTherapistId.addResource("client");
    Resource messagesTherapistClientId = messagesTherapistClient.addResource("{clientId}");
    messagesTherapistClientId.addMethod("GET", new LambdaIntegration(therapistGetMessagesLambda));
    messagesTherapistClientId.addMethod("POST", new LambdaIntegration(therapistSendMessageLambda));

    // --- Search Endpoints ---
    Resource search = api.getRoot().addResource("search");
    Resource searchClients = search.addResource("clients");
    Resource searchClientsClientId = searchClients.addResource("{clientId}");
    searchClientsClientId.addMethod("GET", new LambdaIntegration(clientSearchLambda));

    Resource searchTherapists = search.addResource("therapists");
    Resource searchTherapistsTherapistId = searchTherapists.addResource("{therapistId}");
    searchTherapistsTherapistId.addMethod("GET", new LambdaIntegration(therapistSearchLambda));
  }
}
