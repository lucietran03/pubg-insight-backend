# PROJECT CONTEXT

> This file provides the complete project context for both human developers and AI coding agents.
>
> Before making any code changes, please read this document completely.

---

# Project

PUBG Insight – AI-powered Performance Analytics Platform

This project is developed as the final individual assignment for a Cloud Computing course.

The objective is to demonstrate the appropriate integration of AWS cloud services, external APIs, and AI services into a practical cloud-native application.

This project is **NOT** an AI chatbot.

This project is **NOT** a PUBG statistics viewer.

The primary objective is to build a cloud analytics platform that transforms raw PUBG gameplay statistics into meaningful insights using cloud computing technologies.

---

# Repository Structure

This project consists of **two independent repositories**.

## Backend Repository

Repository:

```
pubg-insight-backend
```

Responsibilities:

- REST API
- Business Logic
- PUBG API Integration
- Gemini API Integration
- AWS Services
- Data Processing
- Historical Analysis
- Authentication
- Deployment

Technology:

- Java 21
- Spring Boot 3
- Maven

---

## Frontend Repository

Repository:

```
pubg-insight-frontend
```

Responsibilities:

- User Interface
- API Communication
- Data Visualization
- Dashboard
- Charts
- Routing

Technology

- React
- TypeScript
- Vite
- Material UI

---

# High-Level Architecture

```
                User
                  │
                  ▼
        React Frontend
                  │
                  ▼
      Spring Boot Backend
                  │
      ┌───────────┼────────────┐
      ▼           ▼            ▼
 PUBG API     Gemini API     AWS
                              │
                  DynamoDB / S3 / Athena
```

Frontend NEVER communicates directly with PUBG API or Gemini API.

All business logic must be implemented in the backend.

---

# Core Features

The application consists of five core features.

## Feature 1

Player Search

Retrieve player profile using PUBG Developer API.

---

## Feature 2

Match Analytics

Display player statistics such as

- Damage
- Kills
- Survival Time
- Placement
- Headshot Rate
- Win Rate

---

## Feature 3

AI Insights

Generate

- Performance Summary
- Strengths
- Weaknesses
- Recommendations

using Gemini API.

The AI receives processed gameplay metrics instead of raw match data.

---

## Feature 4

Analysis History

Store historical analysis.

Users can revisit previous reports.

---

## Feature 5

Analytics Dashboard

Visualize

- Damage Trend
- KD Trend
- Win Rate Trend
- Headshot Trend

using historical data.

---

# AWS Services

The project is planned to integrate the following AWS services.

| Service           | Purpose                |
| ----------------- | ---------------------- |
| Elastic Beanstalk | Deploy backend         |
| API Gateway       | Public API             |
| Lambda            | Background processing  |
| DynamoDB          | Historical analysis    |
| Amazon S3         | Cache reports & assets |
| Amazon Athena     | Analytics queries      |

Do not introduce additional AWS services unless necessary.

---

# Third-party APIs

## PUBG Developer API

Purpose

- Player Search
- Match History
- Season Statistics

---

## Google Gemini API

Purpose

Generate natural language insights.

Gemini should NEVER receive raw telemetry.

Only aggregated metrics should be sent.

---

# Backend Responsibilities

The backend owns

- Business Logic
- Data Processing
- Authentication
- External APIs
- AWS Integration

Controllers should remain thin.

Business logic belongs inside Services.

External APIs belong inside client/.

---

# Frontend Responsibilities

The frontend owns

- UI
- Routing
- State
- Visualization

The frontend should never contain business logic.

The frontend should never communicate directly with PUBG API or Gemini API.

All communication goes through the backend REST API.

---

# Coding Principles

Follow these principles.

- Keep Controllers thin.
- Keep Components small.
- Business logic belongs in Services.
- Reusable code belongs in Utilities.
- Never duplicate code.
- Prefer composition over inheritance.
- Prefer readability over clever code.

---

# Current Status

Completed

- Project initialization
- Backend initialization
- Frontend initialization
- Health endpoint
- Frontend ↔ Backend connection

Next milestone

- PUBG API Integration

---

# Out of Scope

Do NOT implement

- Machine Learning training
- Custom AI models
- PUBG cheats
- Real-time multiplayer
- Desktop applications
- Mobile applications

---

# AI Agent Instructions

Before writing code:

1. Read this file.
2. Preserve the existing architecture.
3. Do not change technologies without justification.
4. Prefer incremental changes.
5. Keep commits small.
6. Explain major architectural decisions.
7. Do not introduce unnecessary dependencies.
8. Do not refactor unrelated code.

The goal is to maintain a clean, production-like codebase throughout the assignment.
