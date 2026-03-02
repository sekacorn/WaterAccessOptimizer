# WaterAccessOptimizer - Setup Instructions (No Docker Required)

## Prerequisites

Before running the application, ensure you have the following installed:

1. **Java 17+** (for backend services)
   - Download: https://adoptium.net/
   - Verify: `java -version`

2. **Maven 3.9+** (for building Spring Boot services)
   - Download: https://maven.apache.org/download.cgi
   - Verify: `mvn -version`

3. **Node.js 18+** (for frontend)
   - Download: https://nodejs.org/
   - Verify: `node --version`

4. **Python 3.8+** (for AI model)
   - Download: https://www.python.org/downloads/
   - Verify: `python --version`

5. **PostgreSQL 15+** (database)
   - Download: https://www.postgresql.org/download/
   - Create database: `createdb wateraccess`
   - Create user: `CREATE USER wateradmin WITH PASSWORD 'waterpass123';`
   - Grant privileges: `GRANT ALL PRIVILEGES ON DATABASE wateraccess TO wateradmin;`

6. **Redis** (optional - for caching)
   - Download: https://redis.io/download
   - Or skip if not using caching features

## Quick Start

### Option 1: Automated Startup (Linux/Mac)

1. **Make scripts executable:**
   ```bash
   chmod +x start-services.sh stop-services.sh
   ```

2. **Set up environment variables (optional):**
   ```bash
   export DB_PASSWORD=waterpass123
   export JWT_SECRET=your-secret-key-minimum-256-bits-long-please-change-this
   export LLM_API_KEY=your-openai-api-key  # Optional
   ```

3. **Start all services:**
   ```bash
   ./start-services.sh
   ```

4. **Access the application:**
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8080

5. **Stop all services:**
   ```bash
   ./stop-services.sh
   ```

### Option 2: Manual Startup (Windows/Linux/Mac)

#### Step 1: Build Backend Services

```bash
# Build all backend services
cd backend/water-integrator && mvn clean package -DskipTests && cd ../..
cd backend/water-visualizer && mvn clean package -DskipTests && cd ../..
cd backend/user-session && mvn clean package -DskipTests && cd ../..
cd backend/llm-service && mvn clean package -DskipTests && cd ../..
cd backend/collaboration-service && mvn clean package -DskipTests && cd ../..
cd backend/api-gateway && mvn clean package -DskipTests && cd ../..
```

#### Step 2: Install Frontend Dependencies

```bash
cd frontend
npm install
cd ..
```

#### Step 3: Install AI Model Dependencies

```bash
cd ai-model
pip install -r requirements.txt
cd ..
```

#### Step 4: Start Services (in separate terminals)

**Terminal 1 - Water Integrator:**
```bash
cd backend/water-integrator
java -jar target/*.jar
```

**Terminal 2 - Water Visualizer:**
```bash
cd backend/water-visualizer
java -jar target/*.jar
```

**Terminal 3 - User Session:**
```bash
cd backend/user-session
java -jar target/*.jar
```

**Terminal 4 - LLM Service:**
```bash
cd backend/llm-service
java -jar target/*.jar
```

**Terminal 5 - Collaboration Service:**
```bash
cd backend/collaboration-service
java -jar target/*.jar
```

**Terminal 6 - API Gateway:**
```bash
cd backend/api-gateway
java -jar target/*.jar
```

**Terminal 7 - AI Model:**
```bash
cd ai-model
python water_predictor.py
```

**Terminal 8 - Frontend:**
```bash
cd frontend
npm run dev
```

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| Frontend | 3000 | http://localhost:3000 |
| API Gateway | 8080 | http://localhost:8080 |
| Water Integrator | 8081 | http://localhost:8081 |
| Water Visualizer | 8082 | http://localhost:8082 |
| User Session | 8083 | http://localhost:8083 |
| LLM Service | 8084 | http://localhost:8084 |
| Collaboration | 8085 | http://localhost:8085 |
| AI Model | 8000 | http://localhost:8000 |

## Environment Variables

Create a `.env` file in the root directory with the following variables:

```env
# Database
DB_PASSWORD=waterpass123
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wateraccess
SPRING_DATASOURCE_USERNAME=wateradmin

# JWT
JWT_SECRET=your-secret-key-change-in-production-minimum-256-bits-long

# LLM (Optional - for natural language features)
LLM_API_KEY=your-openai-api-key
LLM_API_URL=https://api.openai.com/v1

# Redis (Optional)
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

## Database Setup

1. **Install PostgreSQL**

2. **Create database and user:**
   ```sql
   CREATE DATABASE wateraccess;
   CREATE USER wateradmin WITH PASSWORD 'waterpass123';
   GRANT ALL PRIVILEGES ON DATABASE wateraccess TO wateradmin;
   ```

3. **Run schema (if schema.sql exists):**
   ```bash
   psql -U wateradmin -d wateraccess -f database/postgres/schema.sql
   ```

## Troubleshooting

### Port Already in Use

If you get "port already in use" errors:

```bash
# Linux/Mac
lsof -i :8080  # Find process using port
kill -9 <PID>  # Kill the process

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Build Failures

If Maven builds fail:

```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Try building with verbose output
mvn clean package -DskipTests -X
```

### Database Connection Issues

- Ensure PostgreSQL is running: `pg_ctl status`
- Check connection: `psql -U wateradmin -d wateraccess`
- Verify credentials in application.yml files

### Frontend Not Starting

```bash
# Clear node modules and reinstall
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

## Development Mode

For development with hot-reload:

**Backend (Spring Boot DevTools):**
- Add spring-boot-devtools to pom.xml
- Changes to Java files will auto-reload

**Frontend:**
- `npm run dev` already includes hot-reload
- Changes to React files will auto-refresh

## Production Deployment

For production:

1. Build optimized frontend:
   ```bash
   cd frontend
   npm run build
   ```

2. Use production profiles for backend:
   ```bash
   java -jar target/*.jar --spring.profiles.active=prod
   ```

3. Set secure environment variables
4. Enable HTTPS/TLS
5. Use production database
6. Enable authentication/authorization

## License

This project is dual-licensed. See LICENSE.md for details.

## Support

For issues and questions:
- Email: sekacorn@gmail.com
- GitHub Issues: (your-repo-url)
