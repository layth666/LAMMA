# PI_DEV – Event Management Platform

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-blue)
![Maven](https://img.shields.io/badge/Maven-Build-red)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue)
![Status](https://img.shields.io/badge/Project-Academic-green)

---

## Overview

This project was developed as part of the **PIDEV – 3rd Year Engineering Program at Esprit School of Engineering (Academic Year 2025–2026)**.

**PI_DEV** is a modular desktop application that allows users to manage events and interact with several services related to those events.
The platform integrates multiple modules such as user management, event organization, sponsorship, blog interactions, transport services, and subscription management.

The application was built using **JavaFX for the graphical interface** and **MySQL for data persistence**, following a **layered architecture**.

---

## Features

### User Management

* User registration and authentication
* Profile management
* User role management (admin / user)

### Event Management

* Create events
* Modify events
* Delete events
* Display event list
* Manage event programs and schedules

### Sponsoring

* Manage sponsors
* Associate sponsors with events
* Track sponsorship activities

### Blog / Interactions

* Publish blog posts
* Comment and interact with content
* Share experiences related to events

### Transport and Equipment

* Organize carpooling for event participants
* Equipment store for buying or renting equipment

### Subscription and Restoration

* Manage user subscriptions
* Restaurant and food services related to events

---

## Tech Stack

### Frontend

* JavaFX
* FXML
* Scene Builder
* CSS

### Backend

* Java
* MySQL
* Maven

---

## Architecture

The application follows a **3-layer architecture**:

### Presentation Layer

Handles the graphical interface and user interactions.

* JavaFX UI
* FXML layouts
* Controllers

### Business Logic Layer

Contains the core application logic.

* Services
* Data processing
* Validation

### Data Layer

Responsible for managing the data model and persistence.

* Entities
* Database access
* MySQL

---

## Project Structure

```
PI_DEV
│
├── src
│   ├── controllers
│   │    ├── user
│   │    ├── evenement
│   │    ├── sponsoring
│   │    ├── blog
│   │    ├── transport
│   │    └── abonnement
│   │
│   ├── entities
│   │
│   ├── services
│   │
│   ├── utils
│   │
│   └── resources
│        ├── fxml
│        ├── css
│        └── images
│
├── pom.xml
└── README.md
```

---

## Contributors

| Contributor         | Module                       |
| ------------------- | ---------------------------- |
| **Saif**            | User Management              |
| **Feryel Lamouchi** | Event Management             |
| **Mouheb**          | Sponsoring                   |
| **Layth**           | Blog / Interactions          |
| **Weal**            | Transport and Equipment      |
| **Aycha**           | Subscription and Restoration |

---

## Academic Context

Developed at **Esprit School of Engineering – Tunisia**
PIDEV – 3rd Year Engineering Program | 2025–2026

This project was developed as part of a **collaborative academic project**, applying software engineering practices such as:

* modular design
* layered architecture
* Git version control
* teamwork and agile development

---

## Getting Started

### 1️⃣ Clone the repository

```bash
git clone https://github.com/layth666/LAMMA
```

### 2️⃣ Open the project

Open the project using **IntelliJ IDEA**.

### 3️⃣ Install dependencies

```bash
mvn clean install
```

### 4️⃣ Run the application

```bash
mvn javafx:run
```

---

## Acknowledgments

This project was developed as part of the **PIDEV academic project** at **Esprit School of Engineering**.
It aims to apply software engineering concepts in a real-world collaborative development environment.
