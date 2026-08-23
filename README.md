Fabrication & Quotation Management System

A full-stack, enterprise-style web application designed to streamline administrative workflows, track raw material inventory, manage client directories, and automate project quotations for a fabrication and ironworks business.

**TECH STACK**

- Backend Language: Java 25
- Web Server / Container: Apache Tomcat 11 (Jakarta Servlet 6.1 API)
- Database: PostgreSQL 18
- Database Connectivity: JDBC (Java Database Connectivity) via native SQL queries and PreparedStatement
- Frontend: HTML5, CSS3 (Industrial Navy & Steel Design System), Vanilla JavaScript (ES6+ with fetch API)
- Build Tool: Maven

**SYSTEM ARCHITECTURE & WORKFLOW**

The application follows a strict Model-Servlet-Database architecture, ensuring clear separation of concerns between client requests, server-side business logic, and persistent storage.

    [ Browser Dashboard ]
    
        │(HTTP GET / POST via JavaScript fetch)
        ▼
    [ Tomcat 11 Web Server ]
    
        │ (Routing via @WebServlet annotations)
        ▼
    [ Java 25 Servlets ]
    
        │ (JDBC Driver via DriverManager)
        ▼
    [ PostgreSQL Database ] (Clients, Materials, Quotations)

**HOW THE QUOTATION & BUSINESS LOGIC WORKS**

Unlike basic CRUD to-do applications, this system handles relational database mapping and financial calculations:

1. **Relational Client Binding:** When a quote is generated, the Quotations table uses a foreign key constraint (client_id INT REFERENCES clients(id)) to securely link the project directly to a registered customer.
2. **Server-Side Cost Calculation:** The quotation engine reads the incoming JSON payload containing the client ID, project title, and estimated labor cost.
3. **Data Integrity & Security:** All database queries utilize JDBC PreparedStatement parameters (?) rather than raw string concatenation, completely protecting the system against SQL injection attacks.
4. **Audit-Ready Display:** The front-end dynamically fetches joined data (using SQL JOIN statements across clients and quotations) to render active project totals formatted in Philippine Pesos ($\text{₱}$).

**PROJECT STRUCTURE**

        fabrication-quotation-system/
        │
        ├── src/
        │   ├── main/
        │   │   ├── java/
        │   │   │   └── org/example/
        │   │   │       ├── DatabaseConnection.java   # JDBC connection configuration with Tomcat driver loading
        │   │   │       ├── Client.java               # Client data model
        │   │   │       ├── ClientServlet.java        # Handles GET/POST for clients
        │   │   │       ├── Material.java             # Material inventory model
        │   │   │       ├── MaterialServlet.java      # Handles GET/POST for raw materials
        │   │   │       └── QuotationServlet.java     # Core quotation engine & listing
        │   │   │
        │   │   └── webapp/
        │   │       ├── index.html                    # Industrial-themed dashboard UI
        │   │       ├── style.css                     # Custom CSS design system
        │   │       └── script.js                     # Frontend async API communication
        │   │
        └── pom.xml                                   # Maven dependencies (Jakarta Servlet & PostgreSQL JDBC)

**GETTING STARTED & SETUP**
1. **Database Setup (PostgreSQL)**
   Open pgAdmin, create a new database named fab_db, and run the following schema initialization script:

        CREATE TABLE clients (
        id SERIAL PRIMARY KEY,
        client_name VARCHAR(100) NOT NULL,
        contact_number VARCHAR(20),
        address TEXT
        );
        
        CREATE TABLE materials (
        id SERIAL PRIMARY KEY,
        material_name VARCHAR(100) NOT NULL,
        unit_cost DECIMAL(10, 2) NOT NULL
        );
        
        CREATE TABLE quotations (
        id SERIAL PRIMARY KEY,
        client_id INT REFERENCES clients(id),
        project_title VARCHAR(150) NOT NULL,
        labor_cost DECIMAL(10, 2) DEFAULT 0.00,
        total_amount DECIMAL(12, 2) DEFAULT 0.00,
        status VARCHAR(20) DEFAULT 'PENDING',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
**2. Configure Database Credentials** - 
Open DatabaseConnection.java in your project and update your local PostgreSQL username and password:

    private static final String USER = "postgres";
    private static final String PASSWORD = "YOUR_POSTGRES_PASSWORD";

**3. Run on Apache Tomcat 11**
Open the project in IntelliJ IDEA.

* Ensure your Maven dependencies are loaded.
* Configure a Tomcat 11 Local run configuration pointing your artifact to the exploded WAR deployment with an Application Context of /.
* Start the server and navigate to http://localhost:8080/.

**AUTHOR**
- Ismael Ibn Mohammad Ahajan Canseno
