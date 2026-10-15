# MultiEvent

MultiEvent is a Spring Boot event-management and ticketing application with PostgreSQL, Thymeleaf, Mailpit-ready email delivery, and PayPal-ready payment configuration.

## Overview

MultiEvent is a Spring Boot web application focused on event management,
with attendee-facing event discovery and ticketing workflows. Visitors
can browse and filter events, view event details and ticket options, and
proceed toward checkout. The application also includes account
registration and supporting backend configuration for security,
persistence, payments, and email delivery.

**Live demo:**
[nure-multievent-pale.onrender.com](https://nure-multievent-pale.onrender.com)

## Features

### Event discovery and management

-   Browse published events and view event details.
-   Search and filter events by supported criteria, such as keyword,
    category, city, date, and sort order.
-   Present, add, edit and delete event descriptions, dates, locations, images, and ticket
    options.

### Accounts and access control

-   Account registration and login pages.
-   Spring Security-based authentication and access control.

### Ticketing and checkout

-   Display multiple ticket types and their prices.
-   Show ticket availability and per-order limits where configured.
-   Continue from an event page into the checkout workflow.
-   PayPal integration is configured for sandbox use in the deployed
    environment; do not treat live payments as available unless they
    have been explicitly tested and enabled.

### Supporting services

-   PostgreSQL persistence with Flyway database migrations.
-   SMTP email configuration through environment variables.
-   Docker-based local and deployment configuration.
-   Application health endpoint through Spring Boot Actuator.

## Technology stack

| Area                               | Technologies                    |
| ---------------------------------- |:-------------------------------:|
| Language                           | Java 21                         |
| Backend                            | Spring Boot 3.5.5, Spring MVC   |
|  Server-rendered UI                | Thymeleaf, Thymeleaf Spring Security extras |
|  Security                           | Spring Security |
|  Persistence                        | Spring Data JPA, Hibernate, PostgreSQL |
|  Database migrations                | Flyway |
|  Email                              | Spring Boot Mail, SMTP configuration |
|  Payments                           | PayPal integration configured for sandbox mode in the deployment |
|  Build                              | Maven Wrapper |
|  Containers and deployment          | Docker, Docker Compose, Render |
|  Testing dependencies               | Spring Boot Test, Spring Security Test, H2, Testcontainers |
  -----------------------------------------------------------------------


## Architecture

MultiEvent is a server-rendered Spring Boot application. The browser
interacts with the web layer, application logic coordinates the use
cases, and persistence is handled through JPA and PostgreSQL. External
payment and email services are configured separately.

## Academic context

Developed as an academic project at **Kharkiv National University of
Radio Electronics (NURE)**.
