# CloudStore — Serverless Self-Storage

CloudStore is a serverless self-storage application built with Java and AWS. The goal is to create a system where users can view storage units, authenticate securely, and eventually book storage space and manage stored files.

The project is being developed step by step, with an emphasis on simplicity, testing, and cloud security.

## Project Status

**Current stage:** Backend development and AWS SAM configuration.

- [x] Java 21 Maven project setup
- [x] Basic AWS Lambda handler
- [x] Storage-unit API handler
- [x] DynamoDB repository implementation
- [x] DynamoDB table configuration in AWS SAM
- [x] Amazon Cognito User Pool configuration
- [x] API Gateway configuration
- [x] Unit tests passing
- [x] AWS SAM template validation
- [ ] Booking functionality
- [ ] S3 file storage
- [ ] SNS notifications
- [ ] Frontend using S3 and CloudFront
- [ ] Deployment and live AWS testing

**Note:** The project has not yet been deployed to AWS. The current results confirm that the automated tests and SAM template validation pass locally.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend programming language |
| Maven | Dependency management and build tool |
| AWS Lambda | Serverless backend functions |
| Amazon API Gateway | HTTP API endpoints |
| Amazon DynamoDB | NoSQL storage for storage-unit data |
| Amazon Cognito | User authentication configuration |
| AWS SAM | Serverless application configuration and deployment |
| JUnit 5 | Unit testing |
| Mockito | Mocking dependencies during tests |
| Git and GitHub | Version control and source code hosting |

## Current Features

### 1. Hello API

A basic Lambda function that returns a JSON response.

- **Method:** GET
- **Path:** `/hello`
- **Purpose:** Verify that the basic Lambda and API Gateway configuration works.

### 2. Storage Units API

A Lambda handler for retrieving storage-unit information.

- **Method:** GET
- **Path:** `/units`
- **Purpose:** Retrieve storage-unit data through a repository.
- **Data source:** DynamoDB repository implementation.
- **Authentication:** Configured to use an Amazon Cognito User Pool authorizer.

The API configuration still needs to be tested after deployment to AWS.

### 3. DynamoDB Configuration

The AWS SAM template defines a DynamoDB table for storage units.

The table uses `id` as its string partition key.

### 4. Authentication Configuration

The AWS SAM template defines an Amazon Cognito User Pool and a User Pool Client.

The `/units` endpoint is configured to use the Cognito authorizer. Live authentication has not yet been verified against a deployed API.

## Project Structure

The main application code is organised into handlers, models, and repositories.

```text
serverless-self-storage/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── cloudstore/
│   │               ├── handlers/
│   │               │   ├── HelloHandler.java
│   │               │   └── GetUnitsHandler.java
│   │               ├── models/
│   │               │   └── StorageUnit.java
│   │               └── repositories/
│   │                   └── DynamoDbStorageUnitRepository.java
│   └── test/
│       └── java/
│           └── com/
│               └── cloudstore/
│                   ├── handlers/
│                   └── repositories/
├── template.yaml
├── pom.xml
└── README.md
```

*This is the intended overview of the main project files. Check the actual folder names in your repository if they differ.*

## Prerequisites

Install the following tools before working on the project:

- Java Development Kit (JDK)
- Maven
- AWS CLI
- AWS SAM CLI
- Git

Docker can also be useful for local SAM testing.

## Running the Tests

Open a terminal and navigate to the project directory:

```bash
cd ~/IdeaProjects/serverless-self-storage
```

Run the automated tests:

```bash
mvn clean test
```

The current test suite contains four tests covering the Lambda handlers and DynamoDB repository.

**Latest verified result:** 4 tests run, 0 failures, 0 errors.

## Validating the AWS SAM Template

Run:

```bash
sam validate --lint --region af-south-1
```

The template currently passes SAM validation and linting for the Cape Town AWS region.

## Building the Application

To build the project with AWS SAM, run:

```bash
sam build
```

This builds the application locally. It does not deploy resources to AWS.

## Deployment Status

CloudStore has not yet been deployed to AWS.

Before deployment, configure valid AWS credentials and confirm the AWS account and region. Then build, deploy, and test the application using the appropriate AWS SAM commands.

Deployment and live API testing remain outstanding.

## Planned Development

The next development stages are:

1. Complete and verify Cognito authentication.
2. Expand storage-unit functionality.
3. Implement storage-unit booking.
4. Add S3 file storage.
5. Add SNS notifications.
6. Develop a frontend hosted with S3 and CloudFront.
7. Deploy and test the complete application on AWS.


WTC-EG3B6BZH
