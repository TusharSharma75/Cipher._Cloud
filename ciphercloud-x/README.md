# CipherCloud X

**Enterprise-Grade Secure Cloud Storage System**

CipherCloud X is a production-ready, secure cloud storage solution featuring end-to-end encryption, multi-cloud redundancy, and enterprise access control. Built with Java 21, Spring Boot 3.x, and React, it demonstrates hybrid cryptography, tamper-proof integrity, and scalable architecture.

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)
![License](https://img.shields.io/badge/license-MIT-blue)

## Features

### Security
- **Hybrid Encryption**: AES-256 GCM + RSA-2048 key wrapping
- **File Integrity**: SHA-256 hash verification on upload/download
- **Authentication**: JWT with access & refresh tokens
- **Two-Factor Authentication**: OTP via email with backup codes
- **Role-Based Access Control**: ADMIN, USER, GUEST roles
- **Account Security**: Rate limiting, account lockout on brute force

### Storage
- **Multi-Cloud Support**: Backblaze B2 (primary), AWS S3 (secondary), Local fallback
- **Automatic Failover**: Seamless fallback if primary storage fails
- **File Versioning**: Track and rollback to previous versions
- **Secure Sharing**: Public/private links with optional passwords & expiry

### Management
- **Quota Management**: Per-user storage limits
- **Audit Logging**: Complete activity tracking with IP & device info
- **Analytics Dashboard**: Real-time insights on usage, trends, and security
- **Admin Controls**: User management, quota adjustment, role assignment

## Tech Stack

### Backend
- **Java 21** (LTS)
- **Spring Boot 3.2.5**
- **Spring Security 6.x**
- **JPA/Hibernate**
- **MySQL 8+** (or H2 for development)
- **Maven**

### Frontend
- **React 18**
- **TypeScript**
- **Vite**
- **Tailwind CSS**
- **shadcn/ui**

### Security Libraries
- **JJWT** - JSON Web Token implementation
- **BCrypt** - Password hashing
- **AWS SDK** - S3 integration
- **Backblaze B2 SDK** - B2 cloud storage

## Project Structure

```
ciphercloud-x/
├── backend/
│   ├── src/main/java/com/ciphercloudx/
│   │   ├── controller/       # REST API endpoints
│   │   ├── service/          # Business logic
│   │   ├── repository/       # Data access layer
│   │   ├── entity/           # JPA entities
│   │   ├── security/         # JWT, authentication
│   │   ├── encryption/       # Hybrid encryption service
│   │   ├── storage/          # Storage provider abstraction
│   │   ├── audit/            # Activity logging
│   │   ├── analytics/        # Dashboard analytics
│   │   ├── quota/            # Quota management
│   │   ├── dto/              # Data transfer objects
│   │   ├── exception/        # Custom exceptions
│   │   └── config/           # Configuration classes
│   └── src/main/resources/
│       └── application.properties
├── frontend/
│   ├── src/
│   │   ├── components/       # React components
│   │   ├── pages/            # Page components
│   │   ├── services/         # API services
│   │   ├── contexts/         # React contexts
│   │   └── types/            # TypeScript types
│   └── dist/                 # Production build
└── README.md
```

## Quick Start

### Prerequisites
- Java 21+
- Node.js 20+
- MySQL 8+ (or use H2 for development)
- Maven 3.8+

### Backend Setup

1. **Configure Database** (optional - uses H2 by default for dev)
   ```properties
   # application-dev.properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ciphercloudx
   spring.datasource.username=root
   spring.datasource.password=yourpassword
   ```

2. **Configure JWT Secret**
   ```bash
   export JWT_SECRET=$(openssl rand -base64 64)
   ```

3. **Run the Backend**
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   The backend will be available at `http://localhost:8080`

### Frontend Setup

1. **Install Dependencies**
   ```bash
   cd frontend
   npm install
   ```

2. **Configure API URL**
   ```bash
   export VITE_API_URL=http://localhost:8080/api
   ```

3. **Run the Frontend**
   ```bash
   npm run dev
   ```

   The frontend will be available at `http://localhost:5173`

### Production Build

**Backend:**
```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/ciphercloud-x-1.0.0.jar
```

**Frontend:**
```bash
cd frontend
npm run build
# Serve the dist/ folder with any static file server
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Base64-encoded JWT signing key | Auto-generated |
| `RSA_PRIVATE_KEY` | Base64-encoded RSA private key | Auto-generated |
| `RSA_PUBLIC_KEY` | Base64-encoded RSA public key | Auto-generated |
| `B2_APPLICATION_KEY_ID` | Backblaze B2 key ID | - |
| `B2_APPLICATION_KEY` | Backblaze B2 application key | - |
| `B2_BUCKET_NAME` | Backblaze B2 bucket name | - |
| `AWS_ACCESS_KEY_ID` | AWS access key | - |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | - |
| `AWS_REGION` | AWS region | us-east-1 |
| `AWS_BUCKET_NAME` | S3 bucket name | - |

### Storage Providers

Configure in `application.properties`:

```properties
# Primary storage: LOCAL, BACKBLAZE_B2, or AWS_S3
storage.primary-provider=LOCAL
storage.backup-provider=
storage.enable-replication=false
```

## API Documentation

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signup` | Register new user |
| POST | `/api/auth/login` | Authenticate user |
| POST | `/api/auth/verify-otp` | Verify 2FA code |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Logout user |

### File Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/files/upload` | Upload file |
| GET | `/api/files` | List user files |
| GET | `/api/files/{id}` | Get file metadata |
| GET | `/api/files/{id}/download` | Download file |
| DELETE | `/api/files/{id}` | Delete file |
| GET | `/api/files/{id}/versions` | Get file versions |
| POST | `/api/files/{id}/rollback` | Rollback to version |

### Admin Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/analytics/dashboard` | Get analytics |
| PUT | `/api/admin/quota` | Update user quota |
| GET | `/api/users` | List all users |

## Security Architecture

### Hybrid Encryption Flow

```
Upload:
1. Generate random AES-256 key
2. Encrypt file with AES-GCM
3. Encrypt AES key with RSA public key
4. Store encrypted file → Cloud
5. Store encrypted AES key → Database

Download:
1. Fetch encrypted file from Cloud
2. Decrypt AES key with RSA private key
3. Decrypt file with AES key
4. Verify SHA-256 integrity
5. Return file if valid
```

### Authentication Flow

```
1. User submits credentials
2. BCrypt password verification
3. If 2FA enabled → Generate & send OTP
4. Verify OTP
5. Issue JWT access + refresh tokens
6. Token blacklist on logout
```

## Development

### Running Tests

```bash
# Backend tests
cd backend
./mvnw test

# Frontend tests
cd frontend
npm test
```

### Code Style

- **Backend**: Follows Google Java Style Guide
- **Frontend**: ESLint + Prettier configuration included

## Deployment

### Docker (Coming Soon)

```bash
docker-compose up -d
```

### Kubernetes (Coming Soon)

```bash
kubectl apply -f k8s/
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Spring Boot team for the excellent framework
- shadcn/ui for beautiful React components
- Backblaze and AWS for cloud storage APIs

## Support

For issues and feature requests, please use the GitHub issue tracker.

---

**CipherCloud X** - Secure Cloud Storage with Zero Trust Security
