# Pastebin Application

## Overview

The Pastebin Application is a web-based platform where users can create and share pastes (short text documents). Users can set an expiration time for their pastes, and the application will notify them if their paste is about to expire. Additionally, users can add comments to pastes and view pastes by their unique URLs.

## Technologies Used

- **Backend:** Spring Boot (Java)
- **Frontend:** React (JavaScript)
- **Database:**
  - **SQL:** (for metadata)
  - **NoSQL:** MongoDB (for paste content)
- **Email Service:** Spring Boot Email Service
- **Scheduled Tasks:** Spring Boot Scheduler

## Features

- Create a new paste with title, content, username, email, and expiration time.
- View a paste by its unique URL.
- Add comments to a paste.
- View all comments associated with a paste.
- Notifications via email when a paste is about to expire.
- Automatic removal of expired pastes.

## Prerequisites

- Java 11 or higher
- Node.js and npm (for the React frontend)
- MongoDB (for NoSQL data storage)
- SQL data storage

## Setup

### Backend (Spring Boot)



1. **Navigate to the Backend Directory:**

    ```bash
    cd pastebin
    ```

2. **Build the Project:**

    Ensure you have Maven installed. Build the project using:

    ```bash
    mvn clean install
    ```

3. **Run the Application:**

    ```bash
    mvn spring-boot:run
    ```

    The backend server will start on `http://localhost:8080`.

### Frontend (React)

1. **Navigate to the Frontend Directory:**

    ```bash
    cd frontend
    ```

2. **Install Dependencies:**

    ```bash
    npm install
    ```

3. **Start the Development Server:**

    ```bash
    npm start
    ```

    The frontend will be available at `http://localhost:5173`.

## API Endpoints

### Paste Management

- **Create a Paste**

  `POST /api/paste`

  **Request Body:**

  ```json
  {
    "content": "string",
    "title": "string",
    "username": "string",
    "email": "string",
    "expirationTime": 60
  }
  


