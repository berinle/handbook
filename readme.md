## Pre-reqs
To run this application locally, you'll need:

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 15+ with pgvector extension installed


## Getting up and running locally
1. From the root of the project, run 
   ```shell
   ./mvnw spring-boot:run
   ```

## Getting up and running in TPCF
1. Create a PostgreSQL user-provided service with pgvector:
   ```shell
   cf create-service postgres on-demand-postgres-db handbook-db
   
   # if you prefer to BYO database
   cf create-user-provided-service handbook-db -p '{"jdbcUrl": "jdbc:postgresql://your-host:5432/yourdb", "username": "your-username", "password": "your-password"}'

   cf create-service genai nomic-embed-text nomic
   cf create-service genai gemma2:2b gemma2
   ```

2. Deploy the application:
   ```shell
   ./mvnw clean package -DskipTests
   cf push 
   ```

3. Access the application:
   ```shell
   cf apps # Get the route/URL
   ```
   Then open the URL in your browser to access the handbook application.
