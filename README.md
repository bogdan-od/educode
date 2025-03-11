# Educode - Programming Learning Platform

## Overview
This project is a web application for learning programming through practical challenges. It combines a secure backend built with **Java Spring** and a modern frontend powered by **Vue.js**. The platform uses **Docker** containers to safely execute code in multiple programming languages.

## Features
### Backend:
- **Secure Code Execution**: 
  - Isolated Docker containers for each code execution.
  - Supports 22 programming language variants with custom Docker images.
- **Authentication and Authorization**:
  - access/refresh tokens.
  - Role-based access control.
- **Real-time Testing**:
  - Server-Sent Events (SSE) for live updates during task testing.
- **Database**: MySQL, integrated using JPA.

### Frontend:
- **Responsive Design**: Adapts to mobile, tablet, and desktop.
- **User Dashboard**: View tasks, solutions, and leaderboard rankings.
- **Task Creation**:
  - Rich HTML editor for descriptions.
  - Configurable time limits and test cases.
- **Dynamic Themes**: Light, dark, and system-based.

### Security Features:
- Docker containers executed with `--network none`.
- Validation and token management with secure session handling.

## Tech Stack
- **Backend**: Java Spring (REST API, Security, SSE)
- **Frontend**: Vue.js, Vuex, Vue Router, Axios
- **Database**: MySQL
- **Containerization**: Docker (Alpine-based images)

## Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/bogdan-od/educode.git

## Backend Setup:
- Install Java 17+ and Maven.
- Configure application.properties for your MySQL database.
- Build and run:
    ```bash
    mvn clean install
    java -jar target/your-app.jar
    // In backend\src\main\java\com\educode\educodeApi\config\WebConfig.java replace .allowedOrigins("*") with .allowedOrigins("your-frontend-url")
## Frontend Setup:
- Install Node.js and npm.
- Change the VUA_APP_API_URL (./frontend/.env) environment variable to the URL of your API
- Navigate to the frontend directory and install dependencies:
    ```bash
    npm install
    npm run serve
## Docker Setup:
- Build required images:
    ```bash
    ./docker-images/build_images.sh
## Run the Application:
- Access the frontend at http://localhost:9000.