# Online Project Management Tool

A web-based Project Management System developed using Java, Jakarta Servlets, JSP, JDBC, MySQL, HTML, CSS, and JavaScript.

The application demonstrates object-oriented programming, MVC architecture, collections and generics, multithreading, synchronization, database operations, and automated testing.

## Features

* User authentication and session management
* Role-based access control
* Project creation and management
* Task assignment, filtering, and status updates
* Team management
* Dashboard and reports
* In-app notifications
* Profile management
* JDBC-based MySQL persistence
* In-memory mode for testing
* Automated unit and integration tests

## Technology Stack

| Technology       | Requirement                                  |
| ---------------- | -------------------------------------------- |
| Java             | JDK 17 or compatible newer version           |
| Build tool       | Maven Wrapper included                       |
| Web server       | Apache Tomcat 10.1+                          |
| Database         | MySQL 8.x for JDBC mode                      |
| Web technologies | Jakarta Servlets, JSP, HTML, CSS, JavaScript |
| Testing          | JUnit 5                                      |

## Prerequisites

Before running the application, install:

1. JDK 17 or a compatible newer JDK.
2. Apache Tomcat 10.1 or later.
3. MySQL 8.x if using JDBC persistence.
4. Git, if cloning the repository.

Verify Java installation:

```powershell
java -version
```

The project includes `mvnw.cmd`, so a separate Maven installation is not required on Windows.

## 1. Download the Project

Clone the repository:

```powershell
git clone https://github.com/DivsAndDigits/Java-Project-Management-System.git
cd Java-Project-Management-System
```

Alternatively, download the repository as a ZIP file from GitHub and extract it.

Open the extracted project folder in VS Code.

## 2. Build and Test

Open a terminal in the directory containing `pom.xml` and `mvnw.cmd`.

Run:

```powershell
.\mvnw.cmd clean test
```

To build the deployable application:

```powershell
.\mvnw.cmd clean package
```

The WAR file should be generated at:

```text
target/project-management-tool.war
```

The build runs the configured automated tests. A successful build should end with `BUILD SUCCESS`.

## 3. Configure MySQL

Start MySQL Server if you intend to use JDBC persistence.

The project provides its database schema at:

```text
src/main/resources/schema.sql
```

Open the schema file in MySQL Workbench or another MySQL client and execute its SQL statements.

The schema creates the `project_management` database and the tables required by the application.

### Configure database credentials

Check the configuration file:

```text
src/main/resources/db.properties
```

Configure the database URL, username, password, and persistence mode according to your local environment.

Example configuration:

```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/project_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=YOUR_MYSQL_USERNAME
db.password=YOUR_MYSQL_PASSWORD
dao.mode=jdbc
```

Replace the username and password with your own MySQL credentials.

**Important:** Never commit real database passwords or other secrets to GitHub.

The application also supports an in-memory persistence mode for appropriate testing and development scenarios. Use the mode supported by the application's configuration and required workflow.

## 4. Deploy on Apache Tomcat

After building the project, locate:

```text
target/project-management-tool.war
```

Copy the WAR file into the `webapps` directory of your Tomcat installation.

For example, if Tomcat is installed at `C:\apache-tomcat-10.1.x`, run:

```powershell
Copy-Item ".\target\project-management-tool.war" "C:\apache-tomcat-10.1.x\webapps\project-management-tool.war"
```

Replace the example path with the actual Tomcat installation directory.

Start Tomcat:

```powershell
& "C:\apache-tomcat-10.1.x\bin\startup.bat"
```

Open the application in your browser:

```text
http://localhost:8080/project-management-tool/
```

If Tomcat is already running, rebuild and redeploy the WAR as required. Check the Tomcat logs if deployment fails.

## 5. Database and Runtime Notes

* JDBC mode requires a reachable MySQL server and valid database credentials.
* In-memory mode is useful for supported development and testing workflows; data stored only in memory is not persistent across application restarts.
* Each computer must have its own Java and Tomcat configuration.
* MySQL must be configured separately on computers using JDBC persistence.
* Do not assume that a successful page load alone verifies every database operation.

## 6. Automated Testing

Run all configured tests:

```powershell
.\mvnw.cmd clean test
```

Build the WAR file and run the tests:

```powershell
.\mvnw.cmd clean package
```

The test suite covers supported application workflows, services, data-access architecture, collections and generics, multithreading, synchronization, and other implemented features.

## 7. Troubleshooting

### `mvn` is not recognized

Use the included Maven Wrapper:

```powershell
.\mvnw.cmd clean package
```

### `CATALINA_HOME` is not defined

Set it to the actual Tomcat installation directory in PowerShell:

```powershell
$env:CATALINA_HOME = "C:\path\to\apache-tomcat-10.1.x"
```

Then start Tomcat:

```powershell
& "$env:CATALINA_HOME\bin\startup.bat"
```

### The application does not open

1. Confirm that Tomcat has started.
2. Check whether port 8080 is available.
3. Check the Tomcat logs for deployment errors.
4. Confirm that `project-management-tool.war` is inside `webapps`.
5. Verify that the application context path is correct.

### Database connection fails

1. Confirm that MySQL is running.
2. Verify the database name, username, and password.
3. Confirm that the schema has been created.
4. Check the application's database configuration and Tomcat logs.

## Project Repository

GitHub: https://github.com/DivsAndDigits/Java-Project-Management-System

## Conclusion

The Online Project Management Tool demonstrates a layered Java web application using Jakarta Servlets, JSP, JDBC, and MySQL, with additional implementations of object-oriented programming, collections, generics, multithreading, synchronization, and automated testing.
