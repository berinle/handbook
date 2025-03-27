## Pre-reqs
To run this application locally, you'll need:

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 15+ with pgvector extension installed
- Access to the embedding API endpoint (configured via `embedding.endpoint` property)

### PostgreSQL Setup

1. Install PostgreSQL 15 or higher
2. Enable pgvector extension:
   ```sql
   CREATE EXTENSION vector;
   ```
3. Create a database for the application
4. Create the table needed by the application
   ```sql
   CREATE TABLE IF NOT EXISTS handbook_chunks (
    id SERIAL PRIMARY KEY,
    chunk_text TEXT NOT NULL,
    embedding VECTOR(768));
   ```
5. Configure database connection properties in application.properties/yaml


## Getting up and running locally
1. From the root of the project, run 
   ```shell
   ./mvnw spring-boot:run
   ```

## Getting up and running in TPCF
1. Create a PostgreSQL user-provided service with pgvector:
   ```shell
   cf create-user-provided-service handbook-db -p '{"jdbcUrl": "jdbc:postgresql://your-host:5432/yourdb", "username": "your-username", "password": "your-password"}'

   cf create-service genai nomic-embed-text nomic
   cf create-service genai gemma2:2b gemma2
   ```

2. Deploy the application:
   ```shell
   ./mvnw clean package
   cf push handbook -p target/handbook-0.0.1-SNAPSHOT.jar --no-start
   ```

3. Bind the database service:
   ```shell
   cf bind-service handbook handbook-db
   ```

4. Set the embedding API endpoint:
   ```shell
   cf set-env handbook embedding.endpoint "https://your-embedding-api-endpoint"
   ```

5. Start the application:
   ```shell
   cf start handbook
   ```

6. Access the application:
   ```shell
   cf apps # Get the route/URL
   ```
   Then open the URL in your browser to access the handbook application.
