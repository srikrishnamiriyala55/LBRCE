# LBRCE Bus Transportation Management System

Web application for managing LBRCE student bus applications, allocations, fees, digital passes, transfers, complaints, notifications and transport administration.

## Technology

- Backend: Java 21, Spring Boot 4.1.1, Spring MVC, Spring Security, Spring Data JPA and MySQL 8
- Authentication: stateless JWT authentication with BCrypt password hashes and role-based authorization
- Frontend: React 18, Vite 5, React Router, Axios and Tailwind CSS
- Responsive UI: mobile navigation, adaptive forms/cards/modals and horizontally scrollable data tables
- Reports: Apache POI (`.xlsx`) and OpenPDF (`.pdf`)
- Bus-pass verification: ZXing QR codes

## Roles and workflows

### Student

- Create a student account from the login page; new accounts remain pending until Admin activation
- View and update permitted profile fields
- View active buses and boarding points
- Submit one bus application for the active academic year
- Track application history and status
- View fee balance and initiate an eligible payment
- View the active digital bus pass after reaching the configured payment threshold
- Request a bus transfer only after receiving an active allocation
- Raise complaints only after receiving an active allocation
- View and mark personal notifications as read

### In-charge

- Access only the bus assigned to the authenticated In-charge
- View assigned-bus capacity and dashboard statistics
- Approve or reject applications belonging to the assigned bus
- View assigned students and manage unique seat numbers
- Perform the old-bus release and new-bus acceptance stages of transfers
- Manage complaints raised by students on the assigned bus
- View assigned-bus fees, passes and notifications
- Filter assigned-bus student reports and export the results to Excel or PDF

In-charge authorization is enforced by the backend. Changing frontend URLs or request parameters does not grant access to another bus.

### Admin

- View organization-wide dashboard statistics
- Manage buses, routes and boarding points
- Activate or deactivate routes and boarding points with relationship safety checks
- Create and manage In-charges and assign at most one bus per In-charge
- Create, activate or deactivate student accounts
- Review all applications, fees, payments, passes, transfers, complaints and notifications
- Manage academic years
- Inspect audit logs and individual student transport history
- Filter organization-wide student transport reports and export them to Excel or PDF

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
- Complaints require an active approved allocation.
- Payments require an active allocation and cannot exceed the remaining fee balance.
- A bus pass is generated when the student pays at least 50% of the applicable fee.
- Active allocations prevent unsafe bus, route or boarding-point deactivation.
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
|       |   |   |   |-- DataInitializer.java  # Optional local demo-data initializer
|       |   |   |   `-- SecurityConfig.java   # Stateless JWT, CORS and role security
|       |   |   |-- controller/
|       |   |   |   |-- AdminController.java    # Admin REST endpoints
|       |   |   |   |-- AuthController.java     # Login and student registration
|       |   |   |   |-- InchargeController.java # Assigned-bus In-charge endpoints
|       |   |   |   |-- PublicController.java   # Public pass verification
|       |   |   |   `-- StudentController.java  # Student REST endpoints
|       |   |   |-- dto/
|       |   |   |   |-- request/
|       |   |   |   |   |-- AssignInchargeRequest.java      # In-charge assignment payload
|       |   |   |   |   |-- BusApplicationRequest.java      # Bus application payload
|       |   |   |   |   |-- ComplaintRequest.java           # Complaint submission payload
|       |   |   |   |   |-- CreateAcademicYearRequest.java  # Academic-year payload
|       |   |   |   |   |-- CreateBoardingPointRequest.java # Boarding-point payload
|       |   |   |   |   |-- CreateBusRequest.java           # Bus creation/update payload
|       |   |   |   |   |-- CreateInchargeRequest.java      # In-charge creation/update payload
|       |   |   |   |   |-- CreateRouteRequest.java         # Route creation/update payload
|       |   |   |   |   |-- CreateStudentRequest.java       # Admin student-creation payload
|       |   |   |   |   |-- LoginRequest.java               # Login credentials payload
|       |   |   |   |   |-- PaymentRequest.java             # Student payment payload
|       |   |   |   |   |-- SeatAssignmentRequest.java      # Seat assignment payload
|       |   |   |   |   |-- StatusUpdateRequest.java        # Status/remarks update payload
|       |   |   |   |   |-- StudentRegistrationRequest.java # Public student signup payload
|       |   |   |   |   |-- TransferRequestDto.java         # Bus-transfer request payload
|       |   |   |   |   `-- TransportReportFilter.java      # Report filter parameters
|       |   |   |   `-- response/
|       |   |   |       |-- ApplicationResponse.java       # Application API view
|       |   |   |       |-- BoardingPointResponse.java     # Boarding-point API view
|       |   |   |       |-- BusResponse.java               # Bus API view
|       |   |   |       |-- ComplaintResponse.java         # Complaint API view
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
|       |   |   |   |-- Bus.java                 # Vehicle, route and capacity entity
|       |   |   |   |-- BusApplication.java      # Student bus application entity
|       |   |   |   |-- BusPass.java             # Digital pass and verification entity
|       |   |   |   |-- Complaint.java           # Complaint workflow entity
|       |   |   |   |-- Fee.java                 # Student transport fee entity
|       |   |   |   |-- Incharge.java            # In-charge account entity
|       |   |   |   |-- Notification.java        # User notification entity
|       |   |   |   |-- Payment.java             # Payment transaction entity
|       |   |   |   |-- Route.java               # Transport route entity
|       |   |   |   |-- Student.java             # Student account/profile entity
|       |   |   |   |-- TransferRequest.java     # Two-stage transfer entity
|       |   |   |   `-- TransportAllocation.java # Active student/bus allocation
|       |   |   |-- enums/
|       |   |   |   |-- ApplicationStatus.java # Application lifecycle values
|       |   |   |   |-- ComplaintCategory.java # Complaint category values
|       |   |   |   |-- ComplaintStatus.java   # Complaint lifecycle values
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
|       |   |   |   `-- ResourceNotFoundException.java # Missing record error
|       |   |   |-- repository/
|       |   |   |   |-- AcademicYearRepository.java       # Academic-year queries
|       |   |   |   |-- AdminRepository.java              # Admin account queries
|       |   |   |   |-- AuditLogRepository.java           # Audit-log queries
|       |   |   |   |-- BoardingPointsRepository.java     # Boarding-point queries
|       |   |   |   |-- BusApplicationRepository.java     # Application queries and locks
|       |   |   |   |-- BusPassRepository.java            # Bus-pass queries
|       |   |   |   |-- BusRepository.java                # Bus queries and locks
|       |   |   |   |-- ComplaintRepository.java          # Complaint queries and locks
|       |   |   |   |-- FeeRepository.java                # Fee queries and locks
|       |   |   |   |-- InchargeRepository.java           # In-charge queries and locks
|       |   |   |   |-- NotificationRepository.java       # Notification queries
|       |   |   |   |-- PaymentRepository.java            # Payment queries and locks
|       |   |   |   |-- RouteRepository.java              # Route queries
|       |   |   |   |-- StudentRepository.java            # Student and report queries
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
|       |   |       |-- ComplaintService.java          # Complaint ownership and lifecycle
|       |   |       |-- FeeService.java                # Payments, fees and pass eligibility
|       |   |       |-- InchargeOperationsService.java # Assigned-bus seat operations
|       |   |       |-- InchargeService.java           # In-charge service contract
|       |   |       |-- InchargeServiceImpl.java       # In-charge account implementation
|       |   |       |-- NotificationService.java       # Notification creation/read state
|       |   |       |-- PassService.java               # Pass retrieval and verification
|       |   |       |-- RouteService.java              # Route management logic
|       |   |       |-- StudentService.java            # Student service contract
|       |   |       |-- StudentServiceImpl.java        # Student profile and dashboard logic
|       |   |       |-- TransferService.java           # Two-stage transfer workflow
|       |   |       `-- TransportReportService.java    # Filtered Excel/PDF report generation
|       |   `-- resources/
|       |       `-- application.properties             # Ports, DB, JWT, CORS and feature settings
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
|       |   `-- reports/
|       |       `-- TransportReportTable.jsx         # Filters, table and Excel/PDF downloads
|       |-- contexts/
|       |   `-- AuthContext.jsx                      # Login state and JWT persistence
|       |-- pages/
|       |   |-- Login.jsx                            # Login and student signup screen
|       |   |-- admin/
|       |   |   |-- AcademicYearPage.jsx            # Academic-year management
|       |   |   |-- AdminApplicationsPage.jsx        # All bus applications
|       |   |   |-- AdminComplaintsPage.jsx          # Organization complaint management
|       |   |   |-- AdminDashboardPage.jsx           # Admin metrics dashboard
|       |   |   |-- AdminDataPage.jsx                # Payments/passes/transfers/notifications lists
|       |   |   |-- AdminLayout.jsx                  # Admin navigation configuration
|       |   |   |-- AdminProfilePage.jsx             # Admin account information
|       |   |   |-- AdminReportsPage.jsx             # Global reports and exports
|       |   |   |-- AuditLogsPage.jsx                # Audit history viewer
|       |   |   |-- BusManagementPage.jsx            # Bus and boarding-point management
|       |   |   |-- FeeManagementPage.jsx            # Organization fee records
|       |   |   |-- InchargeManagementPage.jsx       # In-charge accounts and assignments
|       |   |   |-- RouteManagementPage.jsx          # Route creation and status management
|       |   |   `-- StudentManagementPage.jsx        # Student accounts and activation
|       |   |-- incharge/
|       |   |   |-- ApplicationManagementPage.jsx    # Assigned-bus approvals/rejections
|       |   |   |-- InchargeComplaintsPage.jsx       # Assigned-bus complaint workflow
|       |   |   |-- InchargeDashboardPage.jsx        # Assigned-bus dashboard metrics
|       |   |   |-- InchargeDataPage.jsx             # Fees, passes and notifications lists
|       |   |   |-- InchargeLayout.jsx               # In-charge navigation configuration
|       |   |   |-- InchargeProfilePage.jsx          # In-charge and assigned-bus profile
|       |   |   |-- InchargeReportsPage.jsx          # Scoped reports and exports
|       |   |   |-- StudentListPage.jsx              # Assigned students and seats
|       |   |   `-- TransferManagementPage.jsx       # Transfer release/acceptance decisions
|       |   `-- student/
|       |       |-- ApplicationHistoryPage.jsx       # Student application history
|       |       |-- BusApplicationPage.jsx           # New bus application form
|       |       |-- BusPassPage.jsx                  # Digital pass and QR display
|       |       |-- ComplaintsPage.jsx               # Submit and track complaints
|       |       |-- FeeViewPage.jsx                  # Fee balance and payment initiation
|       |       |-- NotificationsPage.jsx            # Personal notifications
|       |       |-- StudentDashboardPage.jsx         # Student status dashboard
|       |       |-- StudentLayout.jsx                # Student navigation configuration
|       |       |-- StudentProfilePage.jsx           # Student profile view/edit
|       |       `-- TransferPage.jsx                 # Submit and track transfers
|       `-- utils/
|           `-- axios.js                             # API base URL, JWT and 401 handling
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
CREATE DATABASE btms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

The repository does not contain database backup or manual SQL files. The backend connects to the existing `btms` schema through JPA.

For local development, `JPA_DDL_AUTO=update` can create or update mapped tables. For staging and production, use `JPA_DDL_AUTO=validate` and apply reviewed, version-controlled migrations through the deployment process.

## Backend configuration

Set environment variables instead of committing credentials:

```env
DB_URL=jdbc:mysql://localhost:3306/btms?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
DB_USERNAME=your_database_user
DB_PASSWORD=your_database_password
JWT_SECRET=use-a-long-random-secret-of-at-least-256-bits
JWT_EXPIRATION=86400000
CORS_ALLOWED_ORIGINS=http://localhost:3000
JPA_DDL_AUTO=validate
PAYMENTS_AUTO_CONFIRM=false
BTMS_SEED_ENABLED=false
```

`PAYMENTS_AUTO_CONFIRM` must remain `false` outside explicit local demonstrations. `BTMS_SEED_ENABLED` must remain `false` for a real college database.

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

## Frontend setup

```bash
cd frontend
npm install
npm run dev
```

The development UI runs at `http://localhost:3000` and proxies `/api` requests to the backend. For a separately hosted backend, configure:

```env
VITE_API_URL=https://your-api-host.example/api
```

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

1. Integrate a real payment gateway with signed webhooks and reconciliation.
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
