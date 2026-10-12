# LBRCE Bus Transportation Management System

Web application for managing LBRCE student bus applications, allocations, fees, digital passes, transfers, notifications and transport administration.

## Technology

- Backend: Java 21, Spring Boot 4.1.1, Spring MVC, Spring Security, Spring Data JPA and MySQL 8
- Authentication: stateless JWT authentication, BCrypt password hashes, login throttling and role-based authorization
- Frontend: React 18, Vite 5, React Router, Axios and Tailwind CSS
- Responsive UI: mobile navigation, adaptive forms/cards/modals and horizontally scrollable data tables
- Reports: Apache POI (`.xlsx`) and OpenPDF (`.pdf`)
- Bus-pass verification: ZXing QR codes

## Roles and workflows

### Student

- Register with complete personal, academic, parent/contact and photo details
- Verify email by OTP during registration when `OTP_ENABLED=true`; OTPs expire after five minutes
- View a read-only profile, including the registered photo
- View active buses and boarding points
- Submit a new or renewal bus application for each academic year; the application contains only bus and boarding-point choices
- Track application and transfer history by academic year
- View fee balance and initiate an eligible payment
- View the active digital bus pass after reaching the configured payment threshold
- Request a bus transfer only after receiving an active allocation
- View and mark personal notifications as read

### In-charge

- Sign in with the assigned ID and password; email OTP is supported when enabled
- Access only the bus assigned to the authenticated In-charge
- View assigned-bus capacity and dashboard statistics
- Approve or reject applications belonging to the assigned bus
- View assigned students and their transport, fee, and pass status
- Perform the old-bus release and new-bus acceptance stages of transfers
- Edit complete profile details for students allocated to the assigned bus
- View assigned-bus fees, passes and notifications
- Filter assigned-bus student reports and export the results to Excel or PDF

In-charge authorization is enforced by the backend. Changing frontend URLs or request parameters does not grant access to another bus.

### Admin

- Sign in with the assigned ID and password; email OTP is supported when enabled
- View organization-wide dashboard statistics
- Manage buses with starting/ending points and bus-specific boarding points
- Activate or deactivate buses and boarding points with relationship safety checks
- Create and manage In-charges and assign at most one bus per In-charge
- Create, edit, activate, deactivate or permanently delete student accounts
- Edit or delete eligible student applications and review fees, payments, passes, transfers and notifications
- Search passes by pass ID, student name, roll number or bus number
- Manage academic years
- Inspect audit logs and individual student transport history
- Filter organization-wide student transport reports and export them to Excel or PDF

## Academic-year records

The system stores a student's branch, study year and semester separately for every academic year in `student_academic_details`. The student account remains permanent, while a new year never overwrites historical academic data.

- Admin creates academic years and marks exactly one as active.
- Students submit a new or renewal application for the active academic year.
- Allocations, fees, payments, passes, transfers and reports are associated with an academic year.
- Admin and In-charge operational pages provide academic-year filters. Reports require an academic-year selection.
- Student application and transfer history can be filtered by academic year.
- Student fee history includes the individual payment transactions recorded for each academic year.
- Historical applications and transfers are read-only; workflow decisions are permitted only for the active academic year.
- The included rollover migration preserves 2025-26 as history, creates student academic snapshots for 2026-27 and activates 2026-27.

## Transport report filters

Admin reports support bus number, student name, roll number, department, academic year, semester, payment status, account status, application status, transfer status and minimum remaining fee.

In-charge reports provide the applicable student, academic, payment, application, transfer and remaining-fee filters. Results are always restricted to the authenticated In-charge's assigned bus.

Excel and PDF downloads use the active filters. A maximum of 10,000 records can be exported at once.

## Business rules

- Only one academic year can be active at a time.
- A student can have only one active transport allocation for an academic year.
- A student with an active allocation cannot submit another normal bus application.
- Transfers require an active allocation and follow old In-charge approval followed by new In-charge approval.
- Approval and transfer operations lock relevant database rows to prevent duplicate allocations and overbooking.
- Payments require an active allocation and cannot exceed the remaining fee balance.
- A bus pass is generated when the student pays at least 50% of the applicable fee.
- Active allocations prevent unsafe bus or boarding-point deactivation.
- Sensitive administrative and workflow actions are recorded in the audit log.

## Project structure

```text
LBRCE/
|-- .gitignore                         # Project-wide generated-file and secret exclusions
|-- .github/modernize/java-upgrade/    # Java modernization helper configuration
|   |-- .gitignore                     # Modernization output exclusions
|   `-- hooks/scripts/
|       |-- recordToolUse.ps1          # Windows modernization hook
|       `-- recordToolUse.sh           # Unix modernization hook
|-- backend/                           # Spring Boot REST API
|   |-- .gitignore                     # Maven, IDE and backend exclusions
|   |-- .mvn/wrapper/
|   |   `-- maven-wrapper.properties   # Maven Wrapper version and download settings
|   |-- mvnw                           # Maven Wrapper for Unix systems
|   |-- mvnw.cmd                       # Maven Wrapper for Windows
|   |-- pom.xml                        # Java dependencies and Maven build configuration
|   `-- src/
|       |-- main/
|       |   |-- java/com/web/sms/
|       |   |   |-- BtmsApplication.java       # Spring Boot entry point
|       |   |   |-- config/
|       |   |   |   |-- CacheControlFilter.java # API cache-control response headers
|       |   |   |   |-- DataInitializer.java  # Optional local demo-data initializer
|       |   |   |   `-- SecurityConfig.java   # Stateless JWT, CORS and role security
|       |   |   |-- controller/
|       |   |   |   |-- AdminController.java    # Admin REST endpoints
|       |   |   |   |-- AuthController.java     # Login and student registration
|       |   |   |   |-- InchargeController.java # Assigned-bus In-charge endpoints
|       |   |   |   |-- PublicController.java   # Public pass verification
|       |   |   |   |-- SpaController.java      # React route fallback for production
|       |   |   |   `-- StudentController.java  # Student REST endpoints
|       |   |   |-- dto/
|       |   |   |   |-- request/
|       |   |   |   |   |-- AssignInchargeRequest.java      # In-charge assignment payload
|       |   |   |   |   |-- AdminManualPaymentRequest.java   # Admin-recorded fee payment payload
|       |   |   |   |   |-- BusApplicationRequest.java      # Bus application payload
|       |   |   |   |   |-- CreateAcademicYearRequest.java  # Academic-year payload
|       |   |   |   |   |-- CreateBoardingPointRequest.java # Boarding-point payload
|       |   |   |   |   |-- CreateBusRequest.java           # Bus creation/update payload
|       |   |   |   |   |-- CreateInchargeRequest.java      # In-charge creation/update payload
|       |   |   |   |   |-- CreateStudentRequest.java       # Admin student-creation payload
|       |   |   |   |   |-- LoginOtpVerificationRequest.java # Login OTP verification payload
|       |   |   |   |   |-- LoginRequest.java               # Login credentials payload
|       |   |   |   |   |-- OtpRequest.java                 # OTP send/resend payload
|       |   |   |   |   |-- PaymentRequest.java             # Student payment payload
|       |   |   |   |   |-- ReorderBoardingPointsRequest.java # Complete bus-stop sequence payload
|       |   |   |   |   |-- StatusUpdateRequest.java        # Status/remarks update payload
|       |   |   |   |   |-- StudentRegistrationRequest.java # Public student signup payload
|       |   |   |   |   |-- StudentAcademicDetailsRequest.java # Per-year student data payload
|       |   |   |   |   |-- TransferRequestDto.java         # Bus-transfer request payload
|       |   |   |   |   |-- TransportReportFilter.java      # Report filter parameters
|       |   |   |   |   |-- UpdateApplicationRequest.java   # Admin application-correction payload
|       |   |   |   |   |-- UpdateBoardingPointRequest.java # Boarding-point and fee edit payload
|       |   |   |   |   `-- UpdateStudentRequest.java       # Complete student profile edit payload
|       |   |   |   `-- response/
|       |   |   |       |-- ApplicationResponse.java       # Application API view
|       |   |   |       |-- BoardingPointResponse.java     # Boarding-point API view
|       |   |   |       |-- BusResponse.java               # Bus API view
|       |   |   |       |-- DashboardResponse.java         # Role dashboard metrics
|       |   |   |       |-- FeeResponse.java               # Fee API view
|       |   |   |       |-- InchargeStudentResponse.java   # Assigned-student view
|       |   |   |       |-- LoginResponse.java             # JWT and user login result
|       |   |   |       |-- NotificationResponse.java      # Notification API view
|       |   |   |       |-- PageResponse.java              # Generic pagination view
|       |   |   |       |-- PassResponse.java              # Digital bus-pass view
|       |   |   |       |-- PaymentResponse.java           # Payment API view
|       |   |   |       |-- StudentProfileResponse.java    # Student profile view
|       |   |   |       |-- TransferResponse.java          # Transfer workflow view
|       |   |   |       `-- TransportReportRow.java        # Student report row projection
|       |   |   |-- entity/
|       |   |   |   |-- AcademicYear.java        # Academic-year database entity
|       |   |   |   |-- Admin.java               # Admin account entity
|       |   |   |   |-- AuditLog.java            # Security/business audit entity
|       |   |   |   |-- BoardingPoints.java      # Bus boarding-point entity
|       |   |   |   |-- Bus.java                 # Vehicle, endpoints and capacity entity
|       |   |   |   |-- BusApplication.java      # Student bus application entity
|       |   |   |   |-- BusPass.java             # Digital pass and verification entity
|       |   |   |   |-- Fee.java                 # Student transport fee entity
|       |   |   |   |-- Incharge.java            # In-charge account entity
|       |   |   |   |-- Notification.java        # User notification entity
|       |   |   |   |-- Payment.java             # Payment transaction entity
|       |   |   |   |-- Student.java             # Student account/profile entity
|       |   |   |   |-- StudentAcademicDetails.java # Per-year academic record entity
|       |   |   |   |-- TransferRequest.java     # Two-stage transfer entity
|       |   |   |   `-- TransportAllocation.java # Active student/bus allocation
|       |   |   |-- enums/
|       |   |   |   |-- ApplicationStatus.java # Application lifecycle values
|       |   |   |   |-- EntityStatus.java      # Shared active/inactive values
|       |   |   |   |-- PassStatus.java        # Pass lifecycle values
|       |   |   |   |-- PaymentStatus.java     # Payment lifecycle values
|       |   |   |   |-- Role.java              # STUDENT, INCHARGE and ADMIN roles
|       |   |   |   `-- TransferStatus.java    # Transfer workflow values
|       |   |   |-- exception/
|       |   |   |   |-- ApiErrorResponse.java        # Standard REST error body
|       |   |   |   |-- BadRequestException.java     # Invalid business request error
|       |   |   |   |-- ForbiddenException.java      # Resource authorization error
|       |   |   |   |-- GlobalExceptionHandler.java # Central exception-to-response mapping
|       |   |   |   |-- ResourceNotFoundException.java # Missing record error
|       |   |   |   `-- TooManyRequestsException.java # Login-rate-limit response
|       |   |   |-- repository/
|       |   |   |   |-- AcademicYearRepository.java       # Academic-year queries
|       |   |   |   |-- AdminRepository.java              # Admin account queries
|       |   |   |   |-- AuditLogRepository.java           # Audit-log queries
|       |   |   |   |-- BoardingPointsRepository.java     # Boarding-point queries
|       |   |   |   |-- BusApplicationRepository.java     # Application queries and locks
|       |   |   |   |-- BusPassRepository.java            # Bus-pass queries
|       |   |   |   |-- BusRepository.java                # Bus queries and locks
|       |   |   |   |-- FeeRepository.java                # Fee queries and locks
|       |   |   |   |-- InchargeRepository.java           # In-charge queries and locks
|       |   |   |   |-- NotificationRepository.java       # Notification queries
|       |   |   |   |-- PaymentRepository.java            # Payment queries and locks
|       |   |   |   |-- StudentRepository.java            # Student and report queries
|       |   |   |   |-- StudentAcademicDetailsRepository.java # Per-year academic queries
|       |   |   |   |-- TransferRequestRepository.java    # Transfer queries and locks
|       |   |   |   `-- TransportAllocationRepository.java # Allocation queries and locks
|       |   |   |-- security/
|       |   |   |   |-- CustomUserDetailsService.java # Loads accounts from all three roles
|       |   |   |   |-- JwtAuthenticationFilter.java  # Validates JWTs on API requests
|       |   |   |   |-- JwtTokenProvider.java         # Creates and parses JWTs
|       |   |   |   `-- UserPrincipal.java            # Authenticated-user representation
|       |   |   `-- service/
|       |   |       |-- AdminService.java              # Admin workflows and metrics
|       |   |       |-- ApplicationService.java        # Application approval/allocation rules
|       |   |       |-- AuditService.java              # Audit-event recording
|       |   |       |-- AuthService.java               # Login and student signup logic
|       |   |       |-- BusService.java                # Bus service contract
|       |   |       |-- BusServiceImpl.java            # Bus management implementation
|       |   |       |-- FeeService.java                # Payments, fees and pass eligibility
|       |   |       |-- InchargeOperationsService.java # Assigned-bus student operations
|       |   |       |-- InchargeService.java           # In-charge service contract
|       |   |       |-- InchargeServiceImpl.java       # In-charge account implementation
|       |   |       |-- LoginAttemptService.java        # Brute-force login protection
|       |   |       |-- NotificationService.java       # Notification creation/read state
|       |   |       |-- OtpService.java                # Server-side OTP provider integration
|       |   |       |-- PassService.java               # Pass retrieval and verification
|       |   |       |-- StudentService.java            # Student service contract
|       |   |       |-- StudentAcademicDetailsService.java # Per-year academic record service
|       |   |       |-- StudentServiceImpl.java        # Student profile and dashboard logic
|       |   |       |-- TransferService.java           # Two-stage transfer workflow
|       |   |       `-- TransportReportService.java    # Filtered Excel/PDF report generation
|       |   `-- resources/
|       |       |-- application.properties             # Ports, DB, JWT, OTP, CORS and feature settings
|       |       `-- db/migration/
|       |           |-- V1__add_student_academic_details.sql # Introduces per-year student academic records
|       |           `-- V2__activate_2026_27_academic_year.sql # Safe 2026-27 rollover and active-year migration
|       `-- test/java/com/web/sms/
|           `-- BusManagementApplicationTests.java     # Spring context integration test
|-- frontend/                                          # React single-page application
|   |-- index.html                                     # Vite HTML entry document
|   |-- package.json                                   # Frontend scripts and dependencies
|   |-- package-lock.json                              # Reproducible npm dependency lock
|   |-- postcss.config.js                              # PostCSS/Tailwind processing
|   |-- tailwind.config.js                             # Tailwind content and theme settings
|   |-- vite.config.js                                 # Dev server, port and API proxy
|   |-- public/
|   |   `-- logo.jpg                                  # College logo served at /logo.jpg
|   `-- src/
|       |-- App.jsx                                   # Role routes and protected layouts
|       |-- main.jsx                                  # React application bootstrap
|       |-- index.css                                 # Global and Tailwind styles
|       |-- components/
|       |   |-- ProtectedRoute.jsx                    # Login and role route guard
|       |   |-- applications/
|       |   |   `-- StudentApplicationDetailModal.jsx # Application review detail modal
|       |   |-- common/
|       |   |   |-- ConfirmDialog.jsx                # Reusable confirmation modal
|       |   |   |-- DashboardCard.jsx                # Dashboard statistic card
|       |   |   |-- DataTable.jsx                    # Reusable loading/empty data table
|       |   |   |-- EmptyState.jsx                   # Empty-result presentation
|       |   |   |-- LoadingSpinner.jsx               # Loading indicator
|       |   |   |-- Modal.jsx                        # Accessible modal container
|       |   |   |-- Navbar.jsx                       # User header and logout control
|       |   |   |-- Pagination.jsx                   # Previous/next page controls
|       |   |   |-- Sidebar.jsx                      # Role-specific navigation links
|       |   |   |-- StatusBadge.jsx                  # Colored workflow status label
|       |   |   `-- Toast.jsx                        # Toast provider and notifications
|       |   |-- layout/
|       |   |   `-- DashboardLayout.jsx              # Shared navbar/sidebar page frame
|       |   |-- passes/
|       |   |   |-- PassCard.jsx                     # Shared pass layout used by every role
|       |   |   `-- PassViewerModal.jsx              # Role-scoped bus-pass preview and PDF download
|       |   |-- reports/
|       |   |   `-- TransportReportTable.jsx         # Filters, table and Excel/PDF downloads
|       |   `-- students/
|       |       |-- InchargeStudentDetailModal.jsx    # Assigned-student detail modal
|       |       |-- StudentDetailModal.jsx            # Admin student detail/history modal
|       |       `-- StudentFormModal.jsx              # Shared complete student create/edit form
|       |-- contexts/
|       |   `-- AuthContext.jsx                      # Login state and JWT persistence
|       |-- pages/
|       |   |-- Login.jsx                            # Login, OTP and student signup screen
|       |   |-- VerifyPassPage.jsx                   # Public QR bus-pass verification screen
|       |   |-- admin/
|       |   |   |-- AcademicYearPage.jsx            # Academic-year management
|       |   |   |-- AdminApplicationsPage.jsx        # All bus applications
|       |   |   |-- AdminDashboardPage.jsx           # Admin metrics dashboard
|       |   |   |-- AdminDataPage.jsx                # Payments/passes/transfers/notifications lists
|       |   |   |-- AdminLayout.jsx                  # Admin navigation configuration
|       |   |   |-- AdminProfilePage.jsx             # Admin account information
|       |   |   |-- AdminReportsPage.jsx             # Global reports and exports
|       |   |   |-- AuditLogsPage.jsx                # Audit history viewer
|       |   |   |-- BusManagementPage.jsx            # Bus fleet and In-charge assignment management
|       |   |   |-- BoardingPointManagementPage.jsx  # Boarding-point fees, filters and actions
|       |   |   |-- FeeManagementPage.jsx            # Organization fee records
|       |   |   |-- InchargeManagementPage.jsx       # In-charge accounts and assignments
|       |   |   `-- StudentManagementPage.jsx        # Student accounts and activation
|       |   |-- incharge/
|       |   |   |-- ApplicationManagementPage.jsx    # Assigned-bus approvals/rejections
|       |   |   |-- InchargeDashboardPage.jsx        # Assigned-bus dashboard metrics
|       |   |   |-- InchargeDataPage.jsx             # Fees, passes and notifications lists
|       |   |   |-- InchargeLayout.jsx               # In-charge navigation configuration
|       |   |   |-- InchargeProfilePage.jsx          # In-charge and assigned-bus profile
|       |   |   |-- InchargeReportsPage.jsx          # Scoped reports and exports
|       |   |   |-- StudentListPage.jsx              # Assigned student details
|       |   |   `-- TransferManagementPage.jsx       # Transfer release/acceptance decisions
|       |   `-- student/
|       |       |-- ApplicationHistoryPage.jsx       # Student application history
|       |       |-- BusApplicationPage.jsx           # New bus application form
|       |       |-- BusPassPage.jsx                  # Digital pass and QR display
|       |       |-- FeeViewPage.jsx                  # Fee balance and payment initiation
|       |       |-- NotificationsPage.jsx            # Personal notifications
|       |       |-- StudentDashboardPage.jsx         # Student status dashboard
|       |       |-- StudentLayout.jsx                # Student navigation configuration
|       |       |-- StudentProfilePage.jsx           # Read-only student profile view
|       |       `-- TransferPage.jsx                 # Submit and track transfers
|       `-- utils/
|           |-- academicOptions.js                   # Standard academic dropdown values
|           |-- axios.js                             # API base URL, JWT and 401 handling
|           |-- currency.js                          # Indian-rupee formatting
|           `-- pdfDownload.js                       # Browser PDF-download helper
`-- README.md                                        # Setup, architecture and workflow guide
```

The active logo is `frontend/public/logo.jpg` and is served by Vite as `/logo.jpg`.

Generated folders are intentionally excluded from the tree: `frontend/node_modules`, `frontend/dist`, `backend/.maven-repo` and `backend/target`.

## Prerequisites

- Java 21
- Maven 3.9 or the included Maven wrapper
- Node.js 18 or newer
- MySQL 8

## Database setup

Create the database before starting the backend:

```sql
CREATE DATABASE lbrcebtms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

The repository does not contain database backups or ad-hoc SQL files. The backend connects to the configured schema through JPA and Flyway. Production uses these 15 application tables:

`academic_year`, `admin`, `audit_log`, `boarding_points`, `bus`, `bus_application`, `bus_pass`, `fee`, `incharge`, `notification`, `payment`, `student`, `student_academic_details`, `transfer_request`, and `transport_allocation`.

Flyway also owns the technical `flyway_schema_history` table. Do not delete it or the versioned migration files: they record which schema changes were safely applied and are needed when deploying to a new database.

Legacy authentication, faculty, registration, complaint and transfer tables have been migrated or retired and removed. Student account deletion is an explicit admin-only permanent operation that first removes the student's dependent transport records in foreign-key-safe order; ordinary account suspension remains available through status changes.

For local development, staging and production, use `JPA_DDL_AUTO=validate`. Schema changes must be reviewed, version-controlled Flyway migrations; do not rely on automatic Hibernate schema updates.

## Backend configuration

Set environment variables instead of committing credentials:

```env
DB_URL=jdbc:mysql://localhost:3306/lbrcebtms?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
DB_USERNAME=your_database_user
DB_PASSWORD=your_database_password
SERVER_PORT=8089
JWT_SECRET=use-a-long-random-secret-of-at-least-256-bits
JWT_EXPIRATION=86400000
CORS_ALLOWED_ORIGINS=http://localhost:3000
CORS_MAX_AGE_SECONDS=3600
BCRYPT_STRENGTH=10
LOGIN_MAX_FAILURES=5
LOGIN_BLOCK_MINUTES=15
JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
RAZORPAY_BASE_URL=https://api.razorpay.com
RAZORPAY_KEY_ID=set-only-in-deployment-secrets
RAZORPAY_KEY_SECRET=set-only-in-deployment-secrets
RAZORPAY_WEBHOOK_SECRET=set-only-in-deployment-secrets
BTMS_SEED_ENABLED=false
BTMS_SEED_ADMIN_PASSWORD=replace-only-when-local-seeding-is-enabled
BTMS_SEED_INCHARGE_PASSWORD=replace-only-when-local-seeding-is-enabled
BTMS_SEED_STUDENT_PASSWORD=replace-only-when-local-seeding-is-enabled
PASS_VERIFICATION_URL=http://localhost:8089/api/public/verify-pass?token=
OTP_ENABLED=false
OTP_BASE_URL=https://your-otp-service.example
OTP_API_KEY=set-only-in-deployment-secrets
```

`BTMS_SEED_ENABLED` must remain `false` for a real college database. Razorpay credentials and webhook secrets must be stored only in local `.env` files or deployment environment variables; never commit them.

Set `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` as deployment secrets. Local database defaults exist only for developer convenience and must not be used in deployed environments. The login limiter temporarily blocks repeated failures from the same login-ID/address pair.

To enable OTP, set `OTP_ENABLED=true` and configure `OTP_BASE_URL` and `OTP_API_KEY` only in deployment secrets. The browser never receives the OTP provider key.

### Razorpay payment setup

Student payments use server-created Razorpay orders. The backend validates the checkout signature, verifies the order, amount and currency with Razorpay, captures an authorized payment when required, and only then updates the fee balance. The database and all application screens store amounts in Indian rupees; conversion to paise happens only at the Razorpay API boundary.

Configure this webhook in the Razorpay Dashboard:

```text
https://lbrce-btms-web.onrender.com/api/public/payments/razorpay/webhook
```

Subscribe to `payment.captured`, `payment.failed`, and `order.paid`, and enter the same secret as `RAZORPAY_WEBHOOK_SECRET` in Render. Configure the account to auto-capture payments where available; the backend also attempts capture for an authorized payment. Checkout callbacks and signed webhooks are idempotent, so a successful fee is credited only once.

Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API runs at `http://localhost:8089`.

If Maven reports a locked dependency JAR on Windows, stop the previous backend process and any Java build still running in VS Code, then rerun the command. Do not delete migrations or source files to work around a file lock.

## Frontend setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runtime and development values are maintained in `frontend/.env`. Copy `frontend/.env.example` when preparing another environment:

```env
VITE_API_URL=/api
VITE_DEV_HOST=localhost
VITE_DEV_PORT=3000
VITE_API_PROXY_TARGET=http://localhost:8089
VITE_API_TIMEOUT_MS=45000
VITE_TOAST_DURATION_MS=3000
```

The development UI uses `VITE_DEV_HOST` and `VITE_DEV_PORT`, and proxies `/api` to `VITE_API_PROXY_TARGET`. For a separately hosted backend, set `VITE_API_URL` to its complete API URL.

Create a production bundle with `npm run build`.

## API groups

- `/api/auth/**` - login and student registration
- `/api/public/**` - public bus-pass verification
- `/api/student/**` - authenticated Student operations
- `/api/incharge/**` - authenticated In-charge operations restricted to the assigned bus
- `/api/admin/**` - authenticated Admin operations

The frontend calls API paths through `frontend/src/utils/axios.js`, which attaches the JWT access token.

## Verification

Run backend tests:

```bash
cd backend
./mvnw test
```

Build the frontend:

```bash
cd frontend
npm run build
```

## Production checklist

1. Replace Razorpay test credentials with live credentials after college approval, verify the production webhook, and add a daily settlement-reconciliation report.
2. Implement password reset/change and optional OTP or MFA.
3. Remove all default database and JWT secrets from production configuration.
4. Use reviewed schema migrations and `JPA_DDL_AUTO=validate`.
5. Add refresh-token rotation or server-side token revocation and stronger browser token protection.
6. Configure HTTPS, a reverse proxy, monitoring, structured logs and health alerts.
7. Configure automated database backups and periodically test restoration.
8. Add service, repository, authorization, concurrency and end-to-end tests.
9. Integrate student identity and academic data with the college ERP.
10. Add vehicle documents, maintenance, drivers, attendance and emergency workflows as required.

## Security notes

- Never commit `.env` files, real passwords, private keys, production JWT secrets or exported student reports.
- Do not enable demonstration seed data or automatic payment confirmation in production.
- Keep the In-charge bus scope enforced in backend services and repository queries.
- Treat generated Excel/PDF reports as confidential student data.
- Review audit logs regularly and use least-privilege database credentials.

# Production data and academic-year guarantees

The production model treats applications, allocations, fees, payments, passes,
transfers and audit events as historical records. Administrator "delete" actions
archive students and cancel applications; they do not erase linked history.

Flyway migrations `V3` and `V4` add immutable application/allocation snapshots
and academic-year-specific bus in-charge assignment history. Take and verify a
database backup before deploying a release that applies migrations. Never remove
`flyway_schema_history`, and keep `JPA_DDL_AUTO=validate` in production.

Creating a new academic year is an annual rollover operation. It expires the old
operational allocation/pass state, preserves old fees and transactions, creates
the next academic snapshot for eligible students, marks completed programmes as
graduated, and requires students to submit a new bus application.

The public `/api/public/health` readiness endpoint returns HTTP 200 only when the
application can query MySQL. Render uses this endpoint for deployment health.

Before applying migrations to production:

1. Test them against a restored production backup in staging.
2. Verify student/application/allocation/fee/pass counts before and after.
3. Verify historical passes for at least two academic years.
4. Verify each in-charge can access only their assigned academic-year data.
5. Keep the pre-deployment backup until reconciliation is signed off.
