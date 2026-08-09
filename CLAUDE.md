# AI_AGENT.md

# AI Agent Instructions

You are an AI software engineer working on this repository.

Before making any changes, read this file completely.

Your responsibility is NOT only to generate code.

Your responsibility is to maintain a clean, scalable, production-like codebase that follows the architecture and objectives of this project.

---

# Project Overview

Project Name

PUBG Insight – AI-powered Performance Analytics Platform

This project is developed as the final individual assignment for a Cloud Computing course.

The goal is to demonstrate the integration of AWS cloud services, external APIs, and AI services into a practical cloud-native application.

This project consists of **two repositories**.

Backend Repository

```
pubg-insight-backend
```

Frontend Repository

```
pubg-insight-frontend
```

Both repositories represent one single system.

Always consider how your changes affect the other repository.

---

# Project Goal

The objective is to transform PUBG gameplay statistics into meaningful performance insights.

This project is NOT

- a chatbot
- a PUBG clone
- a statistics viewer

Instead, it is a cloud analytics platform.

---

# Core Features

The application consists of five core features.

1. Player Search

Search PUBG players using the official PUBG Developer API.

2. Match Analytics

Display player statistics including

- kills
- damage
- survival time
- placement
- headshot rate
- win rate

3. AI Insights

Generate

- Performance Summary
- Strengths
- Weaknesses
- Recommendations

using Gemini API.

The AI must receive processed metrics instead of raw telemetry.

4. Analysis History

Store historical analysis.

5. Analytics Dashboard

Display trends using historical data.

---

# Architecture

```
React Frontend

        │

        ▼

Spring Boot Backend

        │

        ├────────► PUBG API

        ├────────► Gemini API

        └────────► AWS
                    │
        DynamoDB
        Amazon S3
        Amazon Athena
```

Frontend NEVER communicates directly with external services.

Everything must go through the backend.

---

# Backend Responsibilities

The backend owns

- REST APIs
- Business Logic
- External APIs
- Authentication
- AWS Integration
- Data Processing

Business logic belongs inside Services.

Controllers should remain thin.

External APIs belong inside client/.

---

# Frontend Responsibilities

The frontend owns

- UI
- Routing
- Components
- Charts
- API Communication

The frontend should never contain business logic.

The frontend should never communicate directly with PUBG API or Gemini API.

---

# Technology Stack

Backend

- Java 21
- Spring Boot 3
- Maven

Frontend

- React
- TypeScript
- Vite
- Material UI

Cloud

- Elastic Beanstalk
- API Gateway
- Lambda
- DynamoDB
- Amazon S3
- Amazon Athena

Third-party

- PUBG Developer API
- Gemini API

---

# Coding Principles

Always follow these principles.

- Keep code simple.
- Prefer readability over cleverness.
- Keep Controllers thin.
- Keep Components small.
- Separate responsibilities clearly.
- Avoid duplicated code.
- Avoid unnecessary abstractions.
- Write self-explanatory code.
- Follow SOLID principles where appropriate.
- Prefer composition over inheritance.

---

# Before Writing Code

Always ask yourself

1.

Does this belong in the backend or frontend?

2.

Does this violate separation of concerns?

3.

Can this be reused?

4.

Is there already an existing implementation?

5.

Will this make future AWS integration easier?

---

# When Implementing Features

Always implement features incrementally.

Preferred order

1.

DTO

↓

2.

External Client

↓

3.

Service

↓

4.

Controller

↓

5.

Frontend API

↓

6.

Frontend UI

Never implement UI before the backend API is stable.

---

# AWS Principles

AWS services should be used only when appropriate.

Never introduce additional AWS services unless there is a clear justification.

Every AWS service should solve a real problem.

---

# AI Integration Principles

Gemini is an assistant.

Gemini is NOT the business logic.

Backend computes gameplay metrics.

Gemini only converts structured metrics into natural language insights.

Example

Backend

```
Average Damage = 520

KD = 2.1

Headshot Rate = 21%
```

↓

Gemini

```
Strengths

Weaknesses

Recommendations
```

Never send unnecessary data to Gemini.

---

# Project Status

Current phase

Project Initialization

Completed

- Backend initialized
- Frontend initialized
- FE ↔ BE connection established

Next milestone

PUBG API Integration

---

# Out of Scope

Do not implement

- AI model training
- PUBG cheats
- Real-time multiplayer
- Mobile application
- Desktop application

---

# If You Are Unsure

Never guess.

Instead

- explain assumptions
- propose alternatives
- ask for clarification

---

# Response Style

When helping with this repository

- explain architectural decisions
- keep answers concise
- produce production-quality code
- avoid unnecessary dependencies
- avoid overengineering

Always optimize for maintainability instead of short-term convenience.

## Repository Awareness

This repository is only one part of the complete system.

Whenever implementing a feature, always consider:

- Does the backend API need to change?
- Does the frontend need to consume this API?
- Will this affect AWS integration?
- Will this impact deployment?

Never make repository-local decisions that break the overall system architecture.
