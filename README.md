# Educode - Programming Learning Platform

## ⚠️ Important Notice

**This is a monorepo that aggregates filtered code from private repositories. The code version may lag behind the actual development in private repos.**

**Це монорепозиторій, який агрегує відфільтрований код з приватних репозиторіїв. Версія коду може відставати від актуального коду в приватних репозиторіях.**

For access to the complete implementation, please contact: **[bogdan040275@ukr.net]**

---

## Overview

Educode is a web-based platform for learning programming through practical challenges. It features secure code execution in isolated Docker containers, flexible task verification systems, and hierarchical access control for educational organizations.

**Note:** Some implementation details have been removed or simplified in this public version.

## Features

### Backend (Java Spring)
- **Secure Code Execution**: 
  - Isolated Docker containers with strict security policies
  - Supports 22 programming language variants
  - Memory, CPU, and I/O limits
  - Network isolation and restricted syscalls
- **Task Verification**:
  - Static checking (exact/normalized output matching)
  - Dynamic checkers (custom evaluation programs)
  - Interactive problems (real-time communication via FIFOs)
- **Authentication & Authorization**:
  - JWT-based access/refresh token system
  - Token rotation with session tracking
  - Context-aware role-based access control (RBAC)
- **Hierarchical Organization**:
  - TreeNode structure (organizations → courses → groups)
  - Role inheritance and permissions
  - Flexible invitation system
- **Database**: MySQL with JPA/Hibernate

### Frontend (Vue.js)
- **Responsive Design**: Optimized for mobile, tablet, and desktop
- **User Dashboard**: Tasks, solutions, progress tracking
- **Task Management**:
  - Rich HTML editor for descriptions
  - Test case configuration
  - Support for static, dynamic, and interactive tasks
- **Group & Homework System**: Scoped task visibility
- **Dynamic Themes**: Light, dark, and system-based

### Infrastructure
- **Docker**: 22 unified execution environments with standardized entrypoints
- **Queue System**: Manages parallel execution limits and load balancing
- **Real-time Logging**: Structured log collection for task execution

## Tech Stack

**Backend:**
- Java Spring Boot
- Spring Data JPA
- MySQL
- JWT Authentication

**Frontend:**
- Vue.js 3
- Vuex (State Management)
- Vue Router
- Axios

**Infrastructure:**
- Docker (Alpine-based images)
- Named Pipes (FIFO) for interactive tasks
- Queue-based execution management

## Project Structure
```
educode/
├── backend/          # Java Spring Boot API
│   ├── src/
│   ├── pom.xml
│   └── ...
├── frontend/         # Vue.js application
│   ├── src/
│   ├── package.json
│   └── ...
└── docker/           # Container configurations
    ├── images/
    └── build_images.sh
```

## Installation

### Prerequisites
- Java 17+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- Docker 20.10+

### 1. Clone the Repository
```bash
git clone https://github.com/bogdan-od/educode.git
cd educode
```

### 2. Backend Setup

**Configure Database and Security:**
Edit `backend/src/main/resources/application.properties`:
```properties
# Database configuration
spring.datasource.url=jdbc:mysql://localhost:3306/educode
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT Secret (IMPORTANT: Change this!)
jwt.secret=your-secure-random-secret-key-here
```

**⚠️ Security Note:** Always replace the default `jwt.secret` with a strong random key in production!

**Configure CORS:**
In `backend/src/main/java/com/educode/educodeApi/config/WebConfig.java`, replace:
```java
.allowedOrigins("*")
```
with:
```java
.allowedOrigins("http://localhost:9000")  // your frontend URL
```

**Build and Run:**
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### 3. Frontend Setup

**Configure API URL:**
Edit `frontend/.env`:
```
VUE_APP_API_URL=http://localhost:8080
```

**Install Dependencies and Run:**
```bash
cd frontend
npm install
npm run serve
```

The frontend will be available at `http://localhost:9000`

### 4. Docker Setup

**Build Execution Images:**
```bash
cd docker
./build_images.sh
```

This will create 22 Docker images for different programming languages.

## Usage

1. Navigate to `http://localhost:9000`
2. Register a new account or log in
3. Create or join a TreeNode (organization/course/group)
4. Browse available tasks or create new ones
5. Submit solutions and receive instant feedback

## Important Notes

### Limited Implementation

This public repository contains architectural code and interfaces, but certain production components are not fully implemented:

- Advanced sandbox security mechanisms
- Complete queue balancing algorithms
- Full interactive task execution engine
- Production-grade resource monitoring

Methods related to these features may throw `UnsupportedOperationException` or contain simplified logic.

### Monorepo Structure

This repository aggregates code from multiple private repositories:
- `educode-backend` (private)
- `educode-frontend` (private)
- `educode-docker` (private)

Code is filtered and synchronized periodically. Some implementation details are intentionally omitted. **The version in this repository may lag behind active development.**

## Security Features

- Docker containers run with `--network none`
- seccomp and AppArmor profiles (basic implementation included)
- JWT token validation and secure session management
- Input sanitization and validation
- Rate limiting and resource quotas

## Database Schema

The platform uses a hierarchical data model:
- **TreeNode**: Organizations, courses, groups
- **TreeNodeMember**: User membership with contextual roles
- **Task (Node)**: Programming challenges
- **Homework**: Task assignments scoped to groups
- **Checker**: Custom evaluation programs
- **Decision**: Solution submissions and verdicts

## License

Copyright (c) 2025

This code is provided as-is for reference purposes.

**RESTRICTIONS:**
- Not licensed for commercial use
- No warranty or support provided
- Redistribution requires explicit permission

## Contributing

This is a snapshot repository. Contributions are not accepted in this public version.

For inquiries or access to complete implementation: **[bogdan040275@ukr.net]**

---

**Built for educational excellence** 🎓💻
