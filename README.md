# PUBG Insight Backend

Backend service for **PUBG Insight – AI-powered Performance Analytics Platform**.

This repository contains the REST API, business logic, integrations with AWS services, PUBG Developer API, and Gemini API.

---

# Project Overview

PUBG Insight is a cloud-native analytics platform that transforms PUBG gameplay statistics into meaningful performance insights.

Instead of simply displaying raw statistics, the backend retrieves player data, processes gameplay metrics, generates AI-assisted summaries, and stores historical analysis for future analytics.

---

# Responsibilities

The backend is responsible for:

- Exposing REST APIs
- Integrating with the PUBG Developer API
- Processing gameplay statistics
- Calling the Gemini API for AI insights
- Managing historical analysis data
- Communicating with AWS services
- Providing analytics data for the frontend dashboard

---

# Technology Stack

| Category | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Build Tool | Maven |
| REST API | Spring Web |
| Validation | Jakarta Validation |
| AWS SDK | AWS SDK v2 |
| Database | DynamoDB |
| Storage | Amazon S3 |
| Deployment | AWS Elastic Beanstalk |
| AI | Google Gemini API |
| External API | PUBG Developer API |

---

# High-Level Architecture

```
Frontend (React)
        │
        ▼
Spring Boot REST API
        │
        ├── PUBG Developer API
        ├── Gemini API
        ├── DynamoDB
        ├── Amazon S3
        └── Amazon Athena (future analytics)
```

---

# Planned Package Structure

```
src/main/java/com/pubginsight

├── config
├── controller
├── dto
├── exception
├── model
├── repository
├── service
├── client
│   ├── pubg
│   └── gemini
└── util
```

---

# Development Workflow

```
Client Request
        │
        ▼
Controller
        │
        ▼
Service
        │
        ├── PUBG API
        ├── Gemini API
        ├── DynamoDB
        └── S3
        │
        ▼
Response DTO
```

---

# Current Development Roadmap

- [ ] Initialize Spring Boot project
- [ ] Configure project structure
- [ ] Health Check endpoint
- [ ] PUBG API integration
- [ ] AI integration
- [ ] DynamoDB integration
- [ ] S3 integration
- [ ] Athena integration
- [ ] Authentication
- [ ] Deployment

---

# Environment Variables

```
PUBG_API_KEY=

GEMINI_API_KEY=

AWS_REGION=

AWS_ACCESS_KEY_ID=

AWS_SECRET_ACCESS_KEY=

AWS_S3_BUCKET=

AWS_DYNAMODB_TABLE=
```

---

# Running the Project

```bash
./mvnw spring-boot:run
```

Backend runs on

```
http://localhost:8080
```

---

# Project Status

🚧 Under Development