# Student Management System

> A comprehensive Java + MySQL desktop application built with MVC architecture and Java Swing GUI.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Setup and Installation](#setup-and-installation)
5. [Running the Application](#running-the-application)
6. [Features Summary](#features-summary)
7. [Grading Scale](#grading-scale)
8. [File Storage Output](#file-storage-output)

---

## Project Overview

The Student Management System (SMS) is a full-featured university administration tool that manages:
- **Students** – registration, course allocation, score entry, result slips
- **Lecturers** – registration, course assignment, course search
- **Courses** – catalogue management, allocation to students and lecturers
- **Library** – book inventory, borrowing, returning, reservations, overdue tracking

---

## Technology Stack

| Layer       | Technology                          |
|-------------|-------------------------------------|
| Language    | Java 17+                            |
| GUI         | Java Swing                          |
| Database    | MySQL 8.0+                          |
| JDBC Driver | MySQL Connector/J 8.0+              |
| Architecture| MVC (Model-View-Controller)         |
| Storage     | MySQL + File System (CSV/TXT)       |

---

## Project Structure

```
StudentManagementSystem/
├── src/
│   ├── db/
│   │   └── DatabaseConnection.java      # Singleton DB connection
│   ├── model/
│   │   ├── Person.java                  # Abstract base class
│   │   ├── Student.java                 # Student entity
│   │   ├── Lecturer.java                # Lecturer entity
│   │   ├── Course.java                  # Course entity
│   │   ├── Score.java                   # Score with grade computation
│   │   ├── Book.java                    # Book entity
│   │   ├── BorrowingRecord.java         # Borrowing record entity
│   │   ├── StudentDAO.java              # Student data access
│   │   ├── LecturerDAO.java             # Lecturer data access
│   │   ├── CourseDAO.java               # Course data access
│   │   ├── ScoreDAO.java                # Score data access
│   │   └── BookDAO.java                 # Book/library data access
│   ├── controller/
│   │   ├── StudentController.java       # Student business logic
│   │   ├── LecturerController.java      # Lecturer business logic
│   │   ├── CourseController.java        # Course business logic
│   │   └── LibraryController.java       # Library business logic
│   ├── view/
│   │   ├── MainFrame.java               # Main application window
│   │   ├── UITheme.java                 # Centralised styling
│   │   ├── StudentPanel.java            # Student management tab
│   │   ├── LecturerPanel.java           # Lecturer management tab
│   │   ├── CoursePanel.java             # Course management tab
│   │   ├── LibraryPanel.java            # Library management tab
│   │   └── ResultSlipPanel.java         # Result slip generator
│   └── util/
│       ├── InputValidator.java          # Input validation utility
│       └── FileStorageManager.java      # File-based persistence
├── sql/
│   └── schema.sql                       # Full MySQL schema + sample data
├── diagrams/
│   ├── class_diagram.png                # UML Class Diagram
│   ├── er_diagram.png                   # Entity-Relationship Diagram
│   └── mvc_diagram.png                  # MVC Architecture Diagram
├── data/                                # Auto-created at runtime (file storage)
├── README.md
└── DOCUMENTATION.md                     # Full academic documentation
```

---

## Setup and Installation

### 1. Database Setup

```bash
# Log into MySQL
mysql -u root -p

# Run the schema script
source /path/to/StudentManagementSystem/sql/schema.sql
```

This creates the database, all tables, views, stored procedures, indexes, and sample data.

### 2. Configure Database Connection

Edit `src/db/DatabaseConnection.java` and update the credentials:

```java
private static final String URL      = "jdbc:mysql://localhost:3306/student_management_system?useSSL=false&serverTimezone=UTC";
private static final String USER     = "your_username";
private static final String PASSWORD = "your_password";
```

### 3. Add MySQL JDBC Driver

Download **MySQL Connector/J** from https://dev.mysql.com/downloads/connector/j/ and add the JAR to your classpath.

### 4. Compile the Project

```bash
# From the project root
javac -cp ".:mysql-connector-j-8.x.x.jar" -d out \
  src/db/*.java \
  src/model/*.java \
  src/util/*.java \
  src/controller/*.java \
  src/view/*.java
```

---

## Running the Application

```bash
java -cp ".:out:mysql-connector-j-8.x.x.jar" view.MainFrame
```

On Windows, replace `:` with `;` in the classpath.

---

## Features Summary

### Student Module
- Register new students with full personal details
- Allocate exactly 5 courses per semester (enforced by stored procedure)
- Enter CAT (max 30) and Exam (max 70) scores
- Auto-compute total score, grade, and averages
- Generate and save formatted result slips

### Lecturer Module
- Register lecturers with department and specialization
- Assign courses to lecturers
- Search assigned courses by code or title

### Course Module
- Add, update, and delete courses
- Search by code or title
- Allocate courses to students and lecturers

### Library Module
- Real-time book search (fires on every keystroke)
- View available, borrowed, and overdue copies
- Borrow books (auto-checks availability)
- Return books (auto-calculates fine at KES 5/day)
- Reserve books when no copies are available
- View full borrowing history per student

### Result Slip
- Enter registration number to generate a formatted slip
- Colour-coded grade table (A=green, B=blue, C=yellow, D=orange, F=red)
- Displays average score and average grade
- Save slip to file system

---

## Grading Scale

| Grade | Score Range | Remark         | Grade Point |
|-------|-------------|----------------|-------------|
| A     | 70 – 100    | Distinction    | 4.0         |
| B     | 60 – 69     | Credit         | 3.0         |
| C     | 50 – 59     | Pass           | 2.0         |
| D     | 40 – 49     | Marginal Pass  | 1.0         |
| F     | 0 – 39      | Fail           | 0.0         |

---

## File Storage Output

The system writes to the `data/` directory at runtime:

| File                         | Contents                              |
|------------------------------|---------------------------------------|
| `students.csv`               | All registered students               |
| `lecturers.csv`              | All registered lecturers              |
| `courses.csv`                | All courses                           |
| `scores.csv`                 | All score records                     |
| `books.csv`                  | Book inventory                        |
| `borrowing_records.csv`      | All borrow/return transactions        |
| `result_slip_<regNo>.txt`    | Formatted result slip per student     |
| `system.log`                 | Timestamped activity log              |
