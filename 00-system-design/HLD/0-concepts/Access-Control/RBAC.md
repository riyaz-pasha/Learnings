Absolutely. Let’s learn **Role-Based Access Control (RBAC)** from the ground up, in the same way we approached HTTP and TLS: **why the problem exists → naive solutions → problems → RBAC → how it works internally → database design → APIs → implementation → advanced concepts → real-world pitfalls**.

# RBAC — Complete Hands-On Course

We'll build toward a realistic application such as:

```text
Company Application
│
├── Organization
│   ├── Admin
│   ├── Manager
│   └── Employee
│
├── Projects
├── Documents
└── Reports
```

By the end, you should understand things like:

```text
User
  ↓
Role
  ↓
Permissions
  ↓
Can perform action?
```

and, more importantly, **why the system is designed this way**.

---

# Course Roadmap

We'll go step by step.

### Part 1 — Access Control Fundamentals

1. What is authorization?
2. Authentication vs authorization
3. The actual access-control problem
4. Naive approaches
5. Why roles exist
6. What is RBAC?
7. Users, roles and permissions
8. RBAC request flow

### Part 2 — Building RBAC

9. Role → permission relationship
10. Database design
11. Role assignment
12. Permission checking
13. API design
14. Middleware / filters
15. Implementing RBAC in a backend
16. JWT and RBAC

### Part 3 — Real-World RBAC

17. Multiple roles per user
18. Role hierarchy
19. Resource-level authorization
20. Tenant / organization RBAC
21. Ownership checks
22. RBAC + ABAC
23. Dynamic permissions
24. Temporary roles
25. Service-to-service authorization

### Part 4 — Advanced Topics

26. Least privilege
27. Separation of duties
28. Permission inheritance
29. Deny vs allow
30. Permission conflicts
31. RBAC caching
32. Revoking access
33. Distributed systems
34. Audit logging
35. Security pitfalls
36. RBAC vs ACL vs ABAC
37. Production architecture

We'll use code and database examples along the way.

---

# 1. First: What Problem Are We Solving?

Imagine we have an application:

```text
Acme Company

Alice
Bob
Charlie
David
```

And the application contains:

```text
Employees
Projects
Reports
Invoices
```

Suppose Alice is an administrator.

She should be able to:

```text
Create employee
Delete employee
Create project
Delete project
View reports
```

Bob is a manager:

```text
View employees
Create project
View reports
```

Charlie is an employee:

```text
View own profile
View assigned projects
```

David is an accountant:

```text
View invoices
Create invoice
```

Now the application receives:

```http
DELETE /employees/123
```

The server needs to answer:

> **Is this user allowed to perform this operation?**

That's **authorization**.

---

# 2. Authentication vs Authorization

This distinction is extremely important.

### Authentication

Authentication answers:

> **Who are you?**

For example:

```text
User logs in
      ↓
username/password
      ↓
identity verified
      ↓
User = Alice
```

Usually represented as:

```text
user_id = 123
```

---

### Authorization

Authorization answers:

> **What are you allowed to do?**

For example:

```text
Alice
  ↓
Admin
  ↓
DELETE employees
  ↓
YES
```

So:

```text
Authentication
    ↓
Who are you?

Authorization
    ↓
What can you do?
```

A user can be authenticated but not authorized.

For example:

```text
Charlie is authenticated.
```

But:

```http
DELETE /employees/123
```

might produce:

```http
403 Forbidden
```

because Charlie doesn't have permission.

---

# 3. The Simplest Possible Authorization

Imagine we have this:

```java
if (user.getId() == 1) {
    allow();
}
```

Maybe Alice has ID `1`.

This technically works.

But imagine 10,000 users.

We'd end up with:

```java
if (user.getId() == 1) {
    ...
} else if (user.getId() == 2) {
    ...
} else if (user.getId() == 3) {
    ...
}
```

Obviously terrible.

---

# 4. Let's Improve It

Instead of asking:

> Is Alice allowed?

we could ask:

> What role does Alice have?

For example:

```text
Alice → ADMIN
Bob   → MANAGER
Charlie → EMPLOYEE
David → ACCOUNTANT
```

Then:

```java
if (user.getRole() == ADMIN) {
    allow();
}
```

Better.

But there's still a problem.

---

# 5. Roles Alone Aren't Enough

Suppose we have:

```text
ADMIN
MANAGER
EMPLOYEE
ACCOUNTANT
```

And endpoints:

```text
POST   /employees
GET    /employees
DELETE /employees/{id}

POST   /projects
GET    /projects

GET    /reports
```

We might write:

```java
if (role == ADMIN) {
    allow();
}
```

But what about managers?

Maybe managers can:

```text
GET /employees
POST /projects
GET /reports
```

but cannot:

```text
DELETE /employees
```

So we need something more precise.

That leads us to **permissions**.

---

# 6. What Is a Permission?

A permission represents a specific capability.

For example:

```text
employee:read
employee:create
employee:update
employee:delete

project:read
project:create
project:update
project:delete

report:read

invoice:read
invoice:create
invoice:update
```

Now we can say:

```text
Alice
  ↓
ADMIN
  ↓
employee:create
employee:read
employee:update
employee:delete
project:create
project:read
project:update
project:delete
report:read
...
```

Bob:

```text
MANAGER
  ↓
employee:read
project:create
project:read
report:read
```

Charlie:

```text
EMPLOYEE
  ↓
employee:read
project:read
```

David:

```text
ACCOUNTANT
  ↓
invoice:read
invoice:create
invoice:update
```

Now we have the basic RBAC model.

---

# 7. The Core Idea of RBAC

RBAC stands for:

> **Role-Based Access Control**

The basic relationship is:

```text
User
  │
  │ assigned to
  ↓
Role
  │
  │ contains
  ↓
Permissions
```

For example:

```text
Alice
  │
  ▼
ADMIN
  │
  ├── employee:create
  ├── employee:read
  ├── employee:update
  ├── employee:delete
  ├── project:create
  ├── project:read
  └── report:read
```

So when Alice sends:

```http
DELETE /employees/123
```

the system effectively asks:

```text
Who is Alice?
       ↓
Alice = user 123

What roles does Alice have?
       ↓
ADMIN

What permissions does ADMIN have?
       ↓
employee:delete

Does Alice have employee:delete?
       ↓
YES
```

Therefore:

```http
204 No Content
```

---

# 8. Why Not Give Permissions Directly to Users?

We could do:

```text
Alice
 ├── employee:create
 ├── employee:read
 ├── employee:update
 ├── employee:delete
 ├── project:create
 └── report:read
```

Bob:

```text
Bob
 ├── employee:read
 ├── project:create
 └── report:read
```

This works.

But imagine:

```text
1,000,000 users
```

and we want to change the manager permission.

For example:

```text
MANAGER
```

should now receive:

```text
project:update
```

Without roles, we'd potentially need to update thousands of users.

With RBAC:

```text
MANAGER
    ↓
add project:update
```

Every manager automatically receives it.

That's one of the biggest benefits of RBAC.

---

# 9. RBAC Gives Us Indirection

Instead of:

```text
User → Permissions
```

we have:

```text
User → Role → Permissions
```

That middle layer is extremely useful.

Think of a role as a **named bundle of permissions**.

For example:

```text
ADMIN
```

is essentially:

```text
{
    employee:create,
    employee:read,
    employee:update,
    employee:delete,

    project:create,
    project:read,
    project:update,
    project:delete,

    report:read
}
```

And:

```text
MANAGER
```

might be:

```text
{
    employee:read,

    project:create,
    project:read,
    project:update,

    report:read
}
```

---

# 10. The Three Core Entities

At minimum, RBAC has:

```text
User
Role
Permission
```

Conceptually:

```text
USER
----
id
name


ROLE
----
id
name


PERMISSION
----------
id
name
```

But there's an important question.

Can one user have multiple roles?

Usually, **yes**.

For example:

```text
Alice
 ├── MANAGER
 └── PROJECT_AUDITOR
```

So:

```text
User ←→ Role
```

is typically a **many-to-many** relationship.

Likewise, one role has many permissions and one permission can belong to many roles:

```text
Role ←→ Permission
```

is also many-to-many.

Therefore the conceptual model becomes:

```text
              ┌──────────────┐
              │     User     │
              └──────┬───────┘
                     │
                     │ M:N
                     ▼
              ┌──────────────┐
              │     Role     │
              └──────┬───────┘
                     │
                     │ M:N
                     ▼
              ┌──────────────┐
              │  Permission  │
              └──────────────┘
```

---

# 11. Database Design

A simple relational implementation could look like this.

### users

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
```

### roles

```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);
```

### permissions

```sql
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
);
```

Now the relationships.

### user_roles

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

And:

### role_permissions

```sql
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);
```

So:

```text
users
  │
  ▼
user_roles
  │
  ▼
roles
  │
  ▼
role_permissions
  │
  ▼
permissions
```

---

# 12. Example Data

Users:

```text
1 | Alice
2 | Bob
3 | Charlie
```

Roles:

```text
1 | ADMIN
2 | MANAGER
3 | EMPLOYEE
```

Permissions:

```text
1 | employee:read
2 | employee:create
3 | employee:update
4 | employee:delete
5 | project:read
6 | project:create
7 | project:update
```

User roles:

```text
Alice   → ADMIN
Bob     → MANAGER
Charlie → EMPLOYEE
```

Role permissions:

```text
ADMIN
 ├── employee:read
 ├── employee:create
 ├── employee:update
 ├── employee:delete
 ├── project:read
 ├── project:create
 └── project:update

MANAGER
 ├── employee:read
 ├── project:read
 ├── project:create
 └── project:update

EMPLOYEE
 ├── employee:read
 └── project:read
```

---

# 13. What Happens During an API Request?

Suppose Charlie sends:

```http
GET /projects/42
Authorization: Bearer <token>
```

The request travels through the application.

Conceptually:

```text
HTTP Request
     │
     ▼
Authentication
     │
     ▼
Who is the user?
     │
     ▼
Charlie
     │
     ▼
Load roles
     │
     ▼
EMPLOYEE
     │
     ▼
Load permissions
     │
     ▼
project:read
     │
     ▼
Required permission?
     │
     ▼
project:read
     │
     ▼
MATCH
     │
     ▼
Controller
```

The important point is:

> **Authentication identifies the principal; authorization decides whether that principal may perform the requested operation.**

---

# 14. What If the User Doesn't Have Permission?

Suppose Charlie calls:

```http
DELETE /employees/123
```

Required permission:

```text
employee:delete
```

Charlie has:

```text
employee:read
project:read
```

No match.

Therefore:

```http
403 Forbidden
```

This distinction is important:

### 401 Unauthorized

Usually means:

> You haven't successfully authenticated.

Example:

```text
Missing/invalid access token
```

### 403 Forbidden

Means:

> We know who you are, but you're not allowed to do this.

Example:

```text
Authenticated user
+
missing employee:delete
=
403
```

---

# 15. The Fundamental Authorization Function

We can reduce RBAC to a simple function:

```text
isAllowed(user, permission)
```

For example:

```java
boolean isAllowed(User user, String permission) {
    return user.getRoles()
               .stream()
               .flatMap(role -> role.getPermissions().stream())
               .anyMatch(p -> p.getName().equals(permission));
}
```

Conceptually:

```text
isAllowed(
    Alice,
    "employee:delete"
)
```

returns:

```text
true
```

while:

```text
isAllowed(
    Charlie,
    "employee:delete"
)
```

returns:

```text
false
```

This tiny concept is at the heart of RBAC.

---

# 16. One Important Thing: Roles Are Not Permissions

This is a common beginner mistake.

Don't design authorization like:

```java
if (user.getRole().equals("ADMIN")) {
    ...
}
```

everywhere in your application.

Prefer:

```java
if (authorizationService.hasPermission(
        user,
        "employee:delete"
)) {
    ...
}
```

Why?

Because permissions represent **capabilities**, while roles represent **bundles of capabilities**.

For example:

Today:

```text
ADMIN → employee:delete
```

Tomorrow you might create:

```text
HR_ADMIN → employee:delete
```

The application endpoint shouldn't care.

It should only care:

```text
Does the caller have employee:delete?
```

This makes authorization much more flexible.

---

# 17. The Mental Model

Keep this model in your head:

```text
                 AUTHENTICATION
                       │
                       ▼
                    User
                       │
                       │
                       ▼
                     Roles
                       │
                       │
                       ▼
                  Permissions
                       │
                       │
                       ▼
                Authorization
                       │
                 ┌─────┴─────┐
                 │           │
                YES          NO
                 │           │
                 ▼           ▼
              Allow         403
```

Or even more simply:

```text
WHO?
 ↓
User

WHAT ROLE?
 ↓
Role

WHAT CAN THAT ROLE DO?
 ↓
Permissions

DOES REQUIRED PERMISSION EXIST?
 ↓
YES → Allow
NO  → Reject
```

---

# 18. But We Have a Big Problem

Imagine:

```text
Alice = MANAGER
```

Manager has:

```text
project:update
```

Alice sends:

```http
PUT /projects/999
```

RBAC says:

```text
Alice has project:update
```

Therefore:

```text
ALLOW
```

But what if project `999` belongs to a completely different company?

For example:

```text
Company A
    Alice

Company B
    Project 999
```

Alice technically has:

```text
project:update
```

but she shouldn't be able to update **that particular project**.

This exposes a crucial limitation:

> **A permission often isn't enough.**

We also need to consider the **resource and context**.

That leads us into:

```text
RBAC
   ↓
Resource-level authorization
   ↓
Multi-tenancy
   ↓
Ownership
   ↓
ABAC
```

That's where authorization becomes much more interesting.

---

# Next

In the next lesson we'll build the **actual RBAC authorization system**:

```text
HTTP Request
      ↓
JWT
      ↓
User
      ↓
Roles
      ↓
Permissions
      ↓
Middleware / Filter
      ↓
Controller
```

We'll also implement something like:

```java
@RequirePermission("project:update")
```

and understand **exactly where the permission check should happen**, rather than just putting `if` statements throughout the application.

---

# Lesson 2 — Building the RBAC Authorization Flow

Now let's turn the concepts from the previous lesson into an actual backend flow.

Our goal is to understand **where RBAC lives inside a real HTTP request**.

---

# 1. Our Application

Imagine we have:

```text
Company Management API
```

with these endpoints:

```http
GET    /employees
POST   /employees
PUT    /employees/{id}
DELETE /employees/{id}

GET    /projects
POST   /projects
PUT    /projects/{id}
DELETE /projects/{id}
```

And these permissions:

```text
employee:read
employee:create
employee:update
employee:delete

project:read
project:create
project:update
project:delete
```

Our roles:

```text
ADMIN
MANAGER
EMPLOYEE
```

For example:

```text
ADMIN
 ├── employee:read
 ├── employee:create
 ├── employee:update
 ├── employee:delete
 ├── project:read
 ├── project:create
 ├── project:update
 └── project:delete

MANAGER
 ├── employee:read
 ├── project:read
 ├── project:create
 └── project:update

EMPLOYEE
 ├── employee:read
 └── project:read
```

---

# 2. Start With an HTTP Request

Alice sends:

```http
POST /projects
Authorization: Bearer eyJ...
Content-Type: application/json

{
    "name": "New Project"
}
```

There are actually **two separate security questions** here.

### Question 1

Who is Alice?

That's authentication.

### Question 2

Can Alice create projects?

That's authorization.

So the request flow becomes:

```text
HTTP Request
     │
     ▼
Authentication
     │
     ▼
Identify Alice
     │
     ▼
Authorization
     │
     ▼
Can Alice create project?
     │
 ┌───┴────┐
 YES      NO
 │         │
 ▼         ▼
Controller 403
```

---

# 3. Where Does the User Come From?

Usually we're using an access token.

For example:

```http
Authorization: Bearer eyJhbGciOi...
```

The token might contain something like:

```json
{
  "sub": "123",
  "iss": "https://auth.example.com",
  "exp": 1790000000
}
```

The important part for our discussion is:

```text
sub = 123
```

This tells our application:

> The authenticated principal is user `123`.

The authentication layer validates the token and establishes something like:

```java
AuthenticatedUser user = new AuthenticatedUser(123);
```

From this point onward, authorization can use:

```text
userId = 123
```

---

# 4. Authentication Should Happen Before Authorization

This ordering is fundamental:

```text
Authentication
       ↓
Authorization
```

Not:

```text
Authorization
       ↓
Authentication
```

Because authorization needs to know:

> **Who are we authorizing?**

For example:

```text
Request
  ↓
Access token
  ↓
Validate token
  ↓
User ID = 123
  ↓
Check permissions for user 123
```

---

# 5. Where Should We Put RBAC Logic?

A beginner might write:

```java
@PostMapping("/projects")
public Project createProject(Authentication authentication) {

    if (!authentication.hasRole("ADMIN")) {
        throw new ForbiddenException();
    }

    // create project
}
```

This works.

But imagine 100 endpoints.

You start getting:

```java
if (!admin) ...
if (!manager) ...
if (!admin) ...
if (!admin && !manager) ...
```

Authorization logic becomes scattered everywhere.

That's undesirable.

Instead, we want a centralized authorization layer.

---

# 6. Authorization Middleware / Filter

Conceptually:

```text
                    HTTP Request
                         │
                         ▼
                Authentication
                         │
                         ▼
                Authorization
                    Middleware
                         │
              ┌──────────┴──────────┐
              │                     │
           Allowed                Denied
              │                     │
              ▼                     ▼
         Controller                403
```

The controller should ideally focus on business logic:

```java
createProject(request);
```

rather than:

```java
checkRole();
checkPermission();
checkSomethingElse();
createProject();
```

---

# 7. Permission-Based Endpoint Protection

Instead of saying:

```text
Only ADMIN can access this endpoint
```

we say:

```text
Caller must have project:create
```

For example:

```java
@RequirePermission("project:create")
@PostMapping("/projects")
public Project createProject(...) {
    ...
}
```

This is a much better abstraction.

The endpoint declares:

> "This operation requires this capability."

It doesn't care which role provides that capability.

---

# 8. Why Is That Better?

Suppose initially:

```text
ADMIN → project:create
```

Later the business decides:

```text
PROJECT_MANAGER → project:create
```

Nothing needs to change in the controller.

The endpoint still says:

```java
@RequirePermission("project:create")
```

Only the RBAC configuration changes:

```text
ADMIN
    ↓
project:create

PROJECT_MANAGER
    ↓
project:create
```

That's an important design principle:

> **Business operations should depend on permissions, not hard-coded roles.**

---

# 9. A Simple Authorization Service

We can create:

```java
class AuthorizationService {

    boolean hasPermission(
        User user,
        String requiredPermission
    ) {
        ...
    }
}
```

Conceptually:

```java
boolean hasPermission(
    User user,
    String requiredPermission
) {
    return user.getRoles()
        .stream()
        .flatMap(role -> role.getPermissions().stream())
        .anyMatch(permission ->
            permission.getName().equals(requiredPermission)
        );
}
```

For Alice:

```text
hasPermission(
    Alice,
    "project:create"
)
```

Result:

```text
true
```

For Charlie:

```text
hasPermission(
    Charlie,
    "project:create"
)
```

Result:

```text
false
```

---

# 10. What Does `@RequirePermission` Actually Do?

Imagine:

```java
@RequirePermission("project:create")
@PostMapping("/projects")
public Project createProject(...) {
    ...
}
```

That annotation itself doesn't magically provide security.

Some framework infrastructure needs to inspect it.

Conceptually:

```text
Request
   ↓
Find endpoint
   ↓
Does endpoint require permission?
   ↓
"project:create"
   ↓
Who is the authenticated user?
   ↓
Alice
   ↓
Does Alice have project:create?
   ↓
YES
   ↓
Execute controller
```

If:

```text
NO
```

then:

```http
403 Forbidden
```

---

# 11. A More Concrete Java Design

We could define:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();
}
```

Then:

```java
@RequirePermission("project:create")
@PostMapping("/projects")
public Project createProject(...) {
    ...
}
```

And an interceptor/aspect/filter could do:

```java
String requiredPermission =
    annotation.value();

User currentUser =
    authenticationContext.currentUser();

if (!authorizationService.hasPermission(
        currentUser,
        requiredPermission)) {

    throw new ForbiddenException();
}
```

The controller only runs if authorization succeeds.

---

# 12. Important: Authentication Context

We don't want to pass the user manually through every method:

```java
createProject(user, request);
updateProject(user, request);
deleteProject(user, request);
```

Instead, the authenticated principal is usually placed into the request/security context.

Conceptually:

```text
HTTP Request
     │
     ▼
Authentication
     │
     ▼
Security Context
     │
     └── currentUser = Alice
```

Then authorization can access:

```java
securityContext.currentUser();
```

---

# 13. Complete Request Flow

Let's put everything together.

Alice sends:

```http
POST /projects
Authorization: Bearer <token>
```

### Step 1 — Receive request

```text
POST /projects
```

### Step 2 — Extract token

```text
Authorization: Bearer <token>
```

### Step 3 — Authenticate

Validate:

```text
signature
expiration
issuer
audience
etc.
```

Then determine:

```text
userId = 123
```

### Step 4 — Establish principal

```text
Current User = Alice
```

### Step 5 — Determine required permission

Endpoint says:

```text
POST /projects
       ↓
project:create
```

### Step 6 — Resolve Alice's roles

```text
Alice
  ↓
MANAGER
```

### Step 7 — Resolve permissions

```text
MANAGER
  ↓
employee:read
project:read
project:create
project:update
```

### Step 8 — Check permission

```text
Required:
project:create

User has:
project:create
```

Match.

### Step 9 — Allow request

```text
Controller
   ↓
createProject()
```

---

# 14. What If Bob Doesn't Have It?

Suppose Charlie calls:

```http
POST /projects
```

Charlie:

```text
EMPLOYEE
```

Employee permissions:

```text
employee:read
project:read
```

Required:

```text
project:create
```

No match.

So:

```text
Authorization
     ↓
DENY
     ↓
403 Forbidden
```

The controller should **never execute**.

That's important.

You don't want:

```text
Controller
   ↓
Start creating project
   ↓
Check permission
   ↓
Oops, forbidden
```

Authorization should happen before the protected operation.

---

# 15. Roles vs Permissions in APIs

Consider these two approaches.

### Approach A

```java
@RequireRole("ADMIN")
```

### Approach B

```java
@RequirePermission("employee:delete")
```

Approach B gives you more flexibility.

Imagine:

```text
ADMIN
HR_ADMIN
EMPLOYEE_MANAGER
```

all need:

```text
employee:delete
```

If your endpoint requires:

```text
ADMIN
```

you now have to modify the endpoint whenever another role needs access.

If it requires:

```text
employee:delete
```

the endpoint doesn't change.

Therefore the application authorization layer can be centered around:

```text
PERMISSIONS
```

with roles acting as configuration.

---

# 16. But There Is Another Problem

Consider:

```text
Bob = MANAGER
```

And:

```text
MANAGER
    ↓
project:update
```

Bob sends:

```http
PUT /projects/123
```

Our RBAC system says:

```text
Bob has project:update
```

So:

```text
ALLOW
```

But suppose:

```text
Project 123 belongs to Company B
Bob belongs to Company A
```

We don't want Bob modifying it.

RBAC alone doesn't express:

```text
Bob can update projects
BUT
only projects belonging to his company.
```

This is where **resource-level authorization** comes in.

---

# 17. Permission vs Resource Access

These are different questions.

### Question 1

Does Bob have the capability?

```text
project:update
```

RBAC can answer this.

### Question 2

Can Bob update **this particular project**?

```text
project 123
```

Now we need additional context.

For example:

```text
Bob
  │
  ├── has project:update
  │
  └── belongs to Company A

Project 123
  │
  └── belongs to Company B
```

Therefore:

```text
Permission = YES
Resource access = NO
```

Final decision:

```text
DENY
```

This distinction is one of the most important concepts in real-world authorization.

---

# 18. The Two-Layer Authorization Model

A common architecture becomes:

```text
                 Request
                    │
                    ▼
             Authentication
                    │
                    ▼
            Is user authenticated?
                    │
                    ▼
             Permission Check
                    │
                    ▼
             Has project:update?
                    │
                    ▼
             Resource Check
                    │
                    ▼
        Does this project belong to
        something the user can access?
                    │
             ┌──────┴──────┐
             │             │
            YES           NO
             │             │
             ▼             ▼
          Allow           403
```

So:

```text
Authorization
    =
Permission
+
Context
```

This takes us beyond basic RBAC.

---

# 19. Example: Ownership

Suppose we have:

```text
GET /documents/123
```

Permission:

```text
document:read
```

But employees should only see their own documents.

So:

```text
Charlie
   ↓
document:read
```

doesn't automatically mean:

```text
Charlie can read every document.
```

Instead:

```text
Charlie can read documents
WHERE document.owner_id = Charlie.id
```

Now the authorization rule is:

```text
hasPermission(user, "document:read")
AND
document.owner_id == user.id
```

This is no longer pure RBAC.

It's **RBAC + resource/ownership rules**.

---

# 20. Why This Matters

A common production mistake is:

```java
if (hasPermission(user, "document:read")) {
    return document;
}
```

That might accidentally expose:

```text
every user's documents
```

when the intended rule was:

```text
users can read their own documents
```

So whenever you design authorization, ask two separate questions:

```text
1. Can this type of operation be performed by this user?

2. Can it be performed on THIS particular resource?
```

---

# 21. A Useful Authorization Vocabulary

You'll encounter these terms frequently.

### Principal

The entity making the request.

Usually:

```text
User
```

but it could also be:

```text
Service
Application
Machine
```

---

### Resource

The thing being accessed.

Examples:

```text
Project
Document
Invoice
Employee
Account
```

---

### Action

What the principal wants to do.

Examples:

```text
read
create
update
delete
approve
publish
download
```

---

### Permission

A combination of resource + action.

Examples:

```text
project:read
project:update
invoice:approve
document:download
```

---

### Role

A named collection of permissions.

Example:

```text
MANAGER
```

---

### Authorization Policy

The rule determining whether the action is allowed.

For simple RBAC:

```text
user has required permission
```

For more complex authorization:

```text
user has permission
AND
user belongs to same organization
AND
resource is active
AND
user owns resource
```

---

# 22. The Bigger Picture

At this point, our model is:

```text
                     ┌─────────────┐
                     │    User     │
                     └──────┬──────┘
                            │
                         Roles
                            │
                            ▼
                     Permissions
                            │
                            ▼
                     ┌─────────────┐
Request ────────────►│ Authorization│
                     └──────┬──────┘
                            │
                    Permission check
                            │
                            ▼
                     Resource check
                            │
                       ┌────┴────┐
                       │         │
                      YES        NO
                       │         │
                       ▼         ▼
                    Business    403
                     logic
```

---

# 23. One More Important Design Decision

Where should roles and permissions come from?

There are several possibilities.

### Option 1 — Database

```text
User
 ↓
DB
 ↓
Roles
 ↓
Permissions
```

Flexible, but potentially requires database lookups.

### Option 2 — JWT claims

Token might contain:

```json
{
  "sub": "123",
  "roles": ["MANAGER"],
  "permissions": [
    "project:read",
    "project:create"
  ]
}
```

Fast, but introduces an important problem:

> What happens if we remove Bob's permission while his JWT is still valid?

We'll explore this carefully.

### Option 3 — Cache

```text
Request
  ↓
Authorization
  ↓
Redis/cache
  ↓
Roles/permissions
```

Can reduce database load.

Real systems often combine these approaches.

---

# 24. The Next Problem: JWT + RBAC

This is where authentication and authorization become tightly connected.

Imagine:

```text
10:00 AM
Bob has MANAGER role
```

He logs in.

His JWT contains:

```json
{
  "sub": "bob",
  "roles": ["MANAGER"]
}
```

At:

```text
10:05 AM
```

an administrator removes Bob's manager role.

Database now says:

```text
Bob
  ↓
EMPLOYEE
```

But Bob's existing JWT still says:

```text
MANAGER
```

What happens?

If we blindly trust the JWT:

```text
Bob still appears to be MANAGER
```

even though his role was revoked.

This is the beginning of the **authorization state / token freshness problem**.

Next we'll explore:

```text
JWT
  ↓
roles vs permissions
  ↓
stale authorization
  ↓
revocation
  ↓
access token lifetime
  ↓
refresh tokens
  ↓
token introspection
  ↓
caching
```

That will connect this RBAC course directly to the OAuth/JWT concepts you've been learning.


---

# Lesson 3 — RBAC + JWT: The Stale Permission Problem

Now we get into one of the most important real-world RBAC problems.

We know:

```text
Authentication
    ↓
JWT
    ↓
User identity
    ↓
RBAC
    ↓
Permissions
```

But **where do the roles and permissions come from?**

And what happens when someone's permissions change while they already have a valid token?

---

# 1. The Simple Approach

Suppose Bob logs in.

The database says:

```text
Bob
  ↓
MANAGER
```

And:

```text
MANAGER
  ↓
project:read
project:create
project:update
```

We issue a JWT:

```json
{
  "sub": "bob-123",
  "roles": ["MANAGER"]
}
```

Now Bob calls:

```http
POST /projects
Authorization: Bearer <token>
```

The server can do:

```text
JWT
 ↓
roles = MANAGER
 ↓
MANAGER → project:create
 ↓
ALLOW
```

Very convenient.

But now we have a problem.

---

# 2. Someone Removes Bob's Role

At 10:00:

```text
Bob → MANAGER
```

At 10:05:

```text
Admin removes MANAGER
```

Database:

```text
Bob → EMPLOYEE
```

But Bob's JWT still contains:

```json
{
  "sub": "bob-123",
  "roles": ["MANAGER"]
}
```

Suppose the JWT expires in:

```text
1 hour
```

Then Bob potentially continues using:

```text
MANAGER
```

for the remainder of that token's lifetime.

This is the **stale authorization problem**.

---

# 3. Why Does This Happen?

Because a JWT is generally a **snapshot of claims at token issuance time**.

At:

```text
10:00
```

the token says:

```text
roles = MANAGER
```

Changing the database at:

```text
10:05
```

doesn't magically modify an already-issued signed token.

JWTs aren't normally live database pointers.

Think of it as:

```text
Database
   │
   │ at login
   ▼
JWT snapshot
```

not:

```text
JWT ───────► live database
```

---

# 4. Three Common Designs

There are several ways to solve this.

### Design A

Put roles/permissions in JWT.

```text
JWT
 ↓
roles/permissions
 ↓
authorize
```

### Design B

Put only user identity in JWT.

```text
JWT
 ↓
user_id
 ↓
database/cache
 ↓
roles/permissions
 ↓
authorize
```

### Design C

Hybrid.

```text
JWT
 ↓
identity + coarse claims
 ↓
database/cache for sensitive/current authorization
```

Each design has tradeoffs.

---

# 5. Design A — Roles Inside JWT

Example:

```json
{
  "sub": "123",
  "roles": [
    "MANAGER"
  ],
  "exp": 1790000000
}
```

The API can authorize without querying the database.

Flow:

```text
Request
   ↓
JWT validation
   ↓
roles = MANAGER
   ↓
permissions
   ↓
ALLOW/DENY
```

### Benefit

Very fast.

You avoid:

```text
DB query
```

on every request.

---

# 6. But Revocation Becomes Difficult

Suppose:

```text
10:00
Bob → MANAGER
```

JWT:

```text
expires 11:00
```

At:

```text
10:05
```

Bob's manager role is revoked.

The API receives:

```text
JWT says MANAGER
```

and has no reason to know the database changed.

So:

```text
10:05 → revoked
10:06 → old JWT still accepted
...
10:59 → potentially still accepted
11:00 → token expires
```

This isn't necessarily a bug.

It's a consequence of the chosen token architecture.

---

# 7. Short-Lived Access Tokens

One solution is to make access tokens short-lived.

For example:

```text
Access token lifetime = 5 minutes
```

Then:

```text
10:00
JWT issued

10:05
JWT expires
```

If Bob's role was revoked at:

```text
10:02
```

the old token can only remain usable for a limited period.

But there's a tradeoff.

Short-lived tokens mean clients need new access tokens more frequently.

That's where refresh tokens come in.

---

# 8. Access Token vs Refresh Token

Conceptually:

```text
             Authentication
                   │
             ┌─────┴─────┐
             │           │
             ▼           ▼
       Access Token   Refresh Token
       short-lived    longer-lived
```

The access token is used for:

```text
API requests
```

The refresh token is used to obtain:

```text
new access token
```

For example:

```text
Access token
    ↓
5 minutes

Refresh token
    ↓
days/weeks
```

This allows:

```text
short API authorization lifetime
+
longer user session
```

---

# 9. But There's Another Question

Suppose Bob's access token expires.

He uses the refresh token:

```http
POST /oauth/token
```

The authorization server can now check current state:

```text
Does Bob still exist?
Is the refresh token valid?
Has the session been revoked?
Is Bob disabled?
```

It can then issue a new access token based on current authorization state.

So:

```text
10:00
Bob = MANAGER
 ↓
access token

10:02
Bob = EMPLOYEE

10:05
access token expires

10:05
refresh
 ↓
current authorization state
 ↓
new token says EMPLOYEE
```

This reduces the stale window.

---

# 10. But What If We Need Immediate Revocation?

Imagine an administrator disables an account because of a security incident.

Waiting five minutes might still be undesirable.

Now we need a stronger mechanism.

For example:

```text
Every request
    ↓
Validate JWT
    ↓
Check server-side authorization/session state
```

or:

```text
Very short access-token lifetime
+
server-side revocation mechanism
```

or an authorization service that evaluates current state.

The correct architecture depends on the application's security requirements.

---

# 11. Don't Put Every Permission Into the JWT Automatically

Imagine an application with:

```text
2,000 permissions
```

A JWT containing:

```json
{
  "permissions": [
    "employee:read",
    "employee:create",
    "employee:update",
    "...",
    "..."
  ]
}
```

could become unnecessarily large.

Remember:

> JWTs are sent with API requests.

For example:

```http
Authorization: Bearer <large-token>
```

If the token becomes large, you're repeatedly transmitting that data.

So many systems keep JWT claims relatively small.

For example:

```json
{
  "sub": "123",
  "iss": "auth.example.com",
  "aud": "api.example.com",
  "exp": 1790000000
}
```

Then authorization can resolve current permissions elsewhere.

---

# 12. Identity vs Authorization State

This distinction is extremely useful.

### Identity information

Things like:

```text
sub
issuer
audience
```

answer:

> Who is this token about?

### Authorization information

Things like:

```text
roles
permissions
organization membership
```

answer:

> What can this principal do?

Authorization state tends to change more frequently.

Therefore:

```text
Identity
    ↓
relatively stable

Authorization
    ↓
potentially dynamic
```

This is one reason you shouldn't blindly put all authorization information into long-lived tokens.

---

# 13. A Practical Architecture

A common design is:

```text
                     ┌──────────────┐
                     │ Auth Server  │
                     └──────┬───────┘
                            │
                      Access Token
                            │
                            ▼
Client ───────────────► API Server
                            │
                            ▼
                       Validate JWT
                            │
                            ▼
                       user_id = 123
                            │
                            ▼
                    Authorization Service
                            │
                       ┌────┴─────┐
                       │          │
                    Cache       Database
                       │          │
                       └────┬─────┘
                            ▼
                       Permissions
                            │
                            ▼
                      Allow / Deny
```

The cache could be something like Redis.

---

# 14. Why Cache?

Suppose our API receives:

```text
10,000 requests/sec
```

and every request performs:

```text
SELECT roles...
SELECT permissions...
```

That could create significant database load.

Instead:

```text
Request
  ↓
Authorization cache
```

For example:

```text
user:123:permissions
```

might contain:

```text
employee:read
project:read
project:create
```

Then:

```text
Request
 ↓
Redis
 ↓
permission lookup
```

can be much cheaper than repeatedly querying the relational database.

---

# 15. But Cache Creates Another Problem

Suppose:

```text
Redis:
Bob → MANAGER
```

Then:

```text
Database:
Bob → EMPLOYEE
```

If the cache still says:

```text
MANAGER
```

we've recreated the stale authorization problem.

So whenever you introduce caching, you must think about:

> **How quickly does authorization state need to become consistent?**

---

# 16. Cache Invalidation

Suppose Bob's role changes.

The application performs:

```text
UPDATE user_roles
```

Then it should invalidate:

```text
user:123:permissions
```

Conceptually:

```text
Role changed
    │
    ├── Update DB
    │
    └── Invalidate cache
             │
             ▼
       Next request
             │
             ▼
       Load fresh roles
             │
             ▼
       Populate cache
```

This is one of the classic distributed-systems problems:

> **Keeping cached authorization state synchronized with the source of truth.**

---

# 17. Role Changes vs Permission Changes

There's another subtle case.

Suppose:

```text
MANAGER
    ↓
project:create
```

Bob:

```text
Bob → MANAGER
```

Now an administrator changes the role itself:

```text
MANAGER
    ↓
remove project:create
```

Every manager is affected.

So invalidating only Bob's cache may not be enough.

Potentially:

```text
MANAGER changed
   ↓
invalidate all users with MANAGER
```

That can be expensive.

A more scalable strategy might use:

```text
role version
```

or:

```text
authorization version
```

We'll come back to this in the advanced section.

---

# 18. A Simple Versioning Strategy

Suppose a user has:

```text
authorization_version = 7
```

Token:

```json
{
  "sub": "123",
  "authz_ver": 7
}
```

Database:

```text
user 123 → authz_ver = 8
```

Now the server sees:

```text
token version = 7
database version = 8
```

Therefore:

```text
old authorization state
```

and it can reject or force reauthorization.

This is one possible revocation mechanism.

There are many variations.

---

# 19. Don't Confuse JWT Signature Validation With Authorization

This is a very important distinction.

Suppose we have:

```text
JWT signature valid
```

That proves something like:

> The token was issued/signed by the expected issuer and hasn't been altered.

It does **not automatically mean**:

> This user is currently allowed to perform this operation.

So:

```text
Valid JWT
      ≠
Authorized request
```

Instead:

```text
Valid JWT
    ↓
Authenticated principal
    ↓
Authorization policy
    ↓
Authorized?
```

---

# 20. Example

Suppose this token is perfectly valid:

```json
{
  "sub": "123",
  "roles": ["MANAGER"],
  "exp": 1790000000
}
```

Signature:

```text
VALID
```

But suppose:

```text
Bob's account has been disabled.
```

Depending on the architecture, the token may still cryptographically validate.

Therefore:

```text
Cryptographically valid
```

doesn't necessarily mean:

```text
Currently authorized
```

This distinction becomes extremely important in security architecture.

---

# 21. The Four Different Checks

When you receive an access token, conceptually there can be several checks:

```text
                    Token
                      │
          ┌───────────┼───────────┐
          │           │           │
          ▼           ▼           ▼
       Signature    Expiry     Issuer/Audience
          │           │           │
          └───────────┼───────────┘
                      ▼
                Authentication
                      │
                      ▼
               Authorization
                      │
                      ▼
             Permission / Policy
```

And possibly:

```text
                      ▼
               Resource access
```

So:

```text
Authentication
    +
Authorization
    +
Resource-level checks
```

are separate concepts.

---

# 22. RBAC Does Not Mean "Put Roles in JWT"

This is worth emphasizing.

RBAC is an **authorization model**.

JWT is a **token format**.

OAuth is an **authorization framework/protocol family**.

They solve different problems.

You can have:

```text
RBAC + JWT
```

but also:

```text
RBAC + opaque tokens
```

or:

```text
RBAC + server sessions
```

or:

```text
RBAC + OAuth access tokens
```

So don't mentally combine them into one thing.

Think:

```text
Authentication mechanism
        +
Token/session mechanism
        +
Authorization model
```

---

# 23. Where We Are Now

We have built this conceptual architecture:

```text
Client
  │
  │ Access Token
  ▼
API
  │
  ▼
Authenticate
  │
  ▼
Principal
  │
  ▼
RBAC
  │
  ▼
Role
  │
  ▼
Permissions
  │
  ▼
Resource / Context Checks
  │
  ▼
Allow / Deny
```

And we've learned an important production concern:

```text
Authorization state changes
        ↓
JWT/cache may be stale
        ↓
Need revocation / expiration / refresh / invalidation strategy
```

---

# 24. One More Major RBAC Concept: Multiple Roles

Now let's tackle a subtle question.

Suppose Alice has:

```text
MANAGER
AUDITOR
```

What permissions does she have?

Maybe:

```text
MANAGER
 ├── employee:read
 ├── project:create
 └── project:update

AUDITOR
 ├── project:read
 └── report:read
```

Does Alice get:

```text
employee:read
project:create
project:update
project:read
report:read
```

Usually, under basic RBAC:

> **Yes — effective permissions are the union of permissions granted by her assigned roles.**

So:

```text
EffectivePermissions(Alice)
 =
Permissions(MANAGER)
 ∪
Permissions(AUDITOR)
```

But this raises another question:

> What happens when one role says "allow" and another role says "deny"?

That leads us into **deny rules, role hierarchy, permission inheritance, separation of duties, and why real-world authorization systems become more complicated than simple RBAC**.

**Next lesson: Multiple roles, role hierarchy, deny rules, and permission conflicts.**

---

# Lesson 4 — Multiple Roles, Role Hierarchies, and Permission Conflicts

Now let's go deeper into the actual **RBAC model**.

So far we've mostly assumed:

```text
User → Role → Permissions
```

But real applications quickly encounter questions like:

* Can a user have multiple roles?
* What happens when roles overlap?
* Can one role inherit another?
* What happens when one rule allows something and another denies it?
* Should `ADMIN` automatically have everything?
* Can a user have incompatible roles?

Let's build these concepts one at a time.

---

# 1. Multiple Roles Per User

Suppose we have:

```text
Alice
 ├── MANAGER
 └── AUDITOR
```

And:

```text
MANAGER
 ├── employee:read
 ├── project:read
 ├── project:create
 └── project:update
```

```text
AUDITOR
 ├── project:read
 ├── report:read
 └── audit:read
```

What permissions does Alice have?

We normally take the **union**:

```text
MANAGER permissions
        +
AUDITOR permissions
        ↓
Effective permissions
```

Therefore:

```text
Alice
 ├── employee:read
 ├── project:read
 ├── project:create
 ├── project:update
 ├── report:read
 └── audit:read
```

Mathematically:

```text
EffectivePermissions(user)
    =
Union(Permissions(role))
```

For Alice:

```text
P(Alice)
 =
P(MANAGER)
 ∪
P(AUDITOR)
```

---

# 2. Why Multiple Roles?

Because real-world responsibilities don't always fit into one role.

For example:

```text
Alice
```

might be:

```text
Engineering Manager
+
Security Auditor
```

Bob:

```text
Developer
+
Release Manager
```

Charlie:

```text
Support Agent
+
Billing Specialist
```

Instead of creating roles like:

```text
ENGINEERING_MANAGER_SECURITY_AUDITOR
```

we can compose roles:

```text
ENGINEERING_MANAGER
SECURITY_AUDITOR
```

This is called **role composition**.

---

# 3. The Role Explosion Problem

Imagine we only allow one role per user.

We might create:

```text
DEVELOPER
MANAGER
AUDITOR
DEVELOPER_MANAGER
DEVELOPER_AUDITOR
MANAGER_AUDITOR
DEVELOPER_MANAGER_AUDITOR
```

As combinations increase, the number of roles can explode.

With multiple roles:

```text
User
 ├── DEVELOPER
 ├── MANAGER
 └── AUDITOR
```

is much cleaner.

This is one reason many RBAC systems support:

```text
User ←→ Role
```

as many-to-many.

---

# 4. Role Hierarchy

Now suppose:

```text
EMPLOYEE
MANAGER
ADMIN
```

It may be natural to say:

```text
ADMIN
  ↓
MANAGER
  ↓
EMPLOYEE
```

Meaning:

```text
ADMIN inherits MANAGER
MANAGER inherits EMPLOYEE
```

So:

```text
EMPLOYEE
 ├── employee:read
 └── project:read
```

Then:

```text
MANAGER
 ├── inherited:
 │    employee:read
 │    project:read
 │
 ├── employee:update
 ├── project:create
 └── project:update
```

And:

```text
ADMIN
 ├── everything from MANAGER
 ├── employee:create
 ├── employee:delete
 ├── project:delete
 └── ...
```

---

# 5. Why Role Hierarchy?

Without hierarchy, we might explicitly assign:

```text
MANAGER
 ├── employee:read
 ├── project:read
 ├── employee:update
 ├── project:create
 └── project:update
```

Then ADMIN needs all of those again:

```text
ADMIN
 ├── employee:read
 ├── project:read
 ├── employee:update
 ├── project:create
 ├── project:update
 ├── employee:create
 ├── employee:delete
 └── ...
```

With inheritance:

```text
ADMIN
   ↓
MANAGER
   ↓
EMPLOYEE
```

you can define incremental permissions.

---

# 6. Important: Hierarchy Is Not Always Appropriate

Don't automatically assume:

```text
ADMIN > MANAGER > EMPLOYEE
```

is universally correct.

For example:

```text
ACCOUNTANT
SECURITY_AUDITOR
ENGINEER
```

might have completely different responsibilities.

You could have:

```text
ENGINEER
    ↓
SENIOR_ENGINEER
```

while:

```text
ACCOUNTANT
    ↓
SENIOR_ACCOUNTANT
```

is an entirely separate hierarchy.

So role hierarchy represents a **business relationship between roles**, not simply organizational seniority.

---

# 7. Hierarchical RBAC

A useful mental model is:

```text
              ADMIN
             /     \
            /       \
       MANAGER     AUDITOR
          |
          |
      EMPLOYEE
```

If ADMIN inherits MANAGER and MANAGER inherits EMPLOYEE:

```text
Permissions(ADMIN)
 =
OwnPermissions(ADMIN)
 ∪
Permissions(MANAGER)
 ∪
Permissions(EMPLOYEE)
```

---

# 8. A More Formal Definition

Let:

```text
r1 ≥ r2
```

mean:

> Role `r1` inherits the permissions of role `r2`.

Then:

```text
Permissions(r1)
 =
OwnPermissions(r1)
 ∪
InheritedPermissions(r2)
```

For:

```text
ADMIN ≥ MANAGER
MANAGER ≥ EMPLOYEE
```

we get:

```text
Permissions(ADMIN)
 =
Own(ADMIN)
 ∪
Own(MANAGER)
 ∪
Own(EMPLOYEE)
```

---

# 9. Database Representation

We could represent hierarchy using:

```sql
CREATE TABLE role_inheritance (
    parent_role_id BIGINT NOT NULL,
    child_role_id BIGINT NOT NULL,

    PRIMARY KEY (parent_role_id, child_role_id),

    FOREIGN KEY (parent_role_id)
        REFERENCES roles(id),

    FOREIGN KEY (child_role_id)
        REFERENCES roles(id)
);
```

For example:

```text
parent        child
-------------------------
ADMIN         MANAGER
MANAGER       EMPLOYEE
```

Meaning:

```text
ADMIN inherits MANAGER
MANAGER inherits EMPLOYEE
```

Be careful with terminology here: teams sometimes reverse the column names. The important thing is to define the semantics explicitly.

---

# 10. Avoid Cycles

Suppose someone accidentally creates:

```text
ADMIN → MANAGER
MANAGER → EMPLOYEE
EMPLOYEE → ADMIN
```

Now we have:

```text
ADMIN
 ↓
MANAGER
 ↓
EMPLOYEE
 ↓
ADMIN
```

That's a cycle.

Permission resolution can become:

```text
ADMIN
 ↓
MANAGER
 ↓
EMPLOYEE
 ↓
ADMIN
 ↓
MANAGER
 ↓
...
```

Therefore role hierarchies should normally be treated as a **directed acyclic graph (DAG)**.

When adding an inheritance relationship:

```text
A → B
```

you should verify that it doesn't introduce a cycle.

---

# 11. Now the Interesting Part: Allow vs Deny

Suppose Alice has:

```text
MANAGER
```

and:

```text
MANAGER
    ↓
project:update
```

So:

```text
ALLOW project:update
```

Now suppose another policy says:

```text
AUDITOR
    ↓
DENY project:update
```

And Alice has both:

```text
MANAGER
AUDITOR
```

What should happen?

We have:

```text
MANAGER
   ↓
ALLOW project:update

AUDITOR
   ↓
DENY project:update
```

Now what?

---

# 12. Don't Assume "Deny Always Wins"

Different authorization systems use different models.

There isn't a universal RBAC rule saying:

```text
DENY > ALLOW
```

or:

```text
ALLOW > DENY
```

You need to define the policy semantics explicitly.

Possible models include:

### Model A — Allow-only RBAC

Roles only grant permissions.

```text
role → permission
```

There is no explicit deny.

This is actually a very clean model.

If the user doesn't have the permission:

```text
DENY
```

Otherwise:

```text
ALLOW
```

---

### Model B — Explicit Deny

You have:

```text
ALLOW
DENY
```

and need precedence rules.

For example:

```text
explicit DENY
    >
explicit ALLOW
```

But this is now more complex.

---

# 13. Why Allow-Only RBAC Is Often Easier

Suppose permissions are:

```text
project:read
project:update
project:delete
```

A user's effective permissions are simply:

```text
Union(all granted permissions)
```

Then:

```text
if permission exists:
    allow
else:
    deny
```

Very easy to reason about.

There is no question like:

```text
Which deny wins?
Which role takes precedence?
Did the inherited role override the explicit deny?
```

This simplicity is valuable.

---

# 14. But Real Systems Need Exceptions

Imagine:

```text
MANAGER
    ↓
project:delete
```

But your organization has a policy:

> Managers can delete projects except production projects.

Now simple RBAC isn't enough.

You need contextual rules:

```text
user has project:delete
AND
project.environment != production
```

Now authorization depends on the resource.

This is moving toward **attribute/context-based authorization**.

---

# 15. RBAC + Resource Rules

Consider:

```text
Alice
 └── MANAGER
       └── project:update
```

Alice wants to update:

```text
Project #123
```

We can evaluate:

```text
1. Does Alice have project:update?
2. Does Alice have access to Project #123?
```

So:

```java
boolean allowed =
    hasPermission(user, "project:update")
    && canAccessProject(user, project);
```

This is a very common production pattern.

---

# 16. Organization/Tenant Boundaries

Now let's introduce a multi-tenant application.

```text
Company A
 ├── Alice
 ├── Bob
 └── Project 1

Company B
 ├── Charlie
 └── Project 2
```

Alice:

```text
role = MANAGER
permission = project:update
```

Charlie:

```text
role = MANAGER
permission = project:update
```

Both have the same RBAC permission.

But:

```text
Alice → Project 1
```

should be allowed.

While:

```text
Alice → Project 2
```

should be denied.

So the real rule becomes:

```text
hasPermission(user, "project:update")
AND
user.organization_id == project.organization_id
```

Notice something important:

> **RBAC tells us what kind of operation the user can perform. Tenant/resource authorization tells us where that operation can be performed.**

---

# 17. Role Scope

This leads to another important concept.

A role doesn't necessarily need to be global.

Suppose Alice manages two organizations:

```text
Alice
 ├── ADMIN @ Company A
 └── EMPLOYEE @ Company B
```

So we can't simply store:

```text
Alice → ADMIN
```

because that would imply ADMIN everywhere.

Instead:

```text
user_roles

user_id | role_id | organization_id
------------------------------------
Alice   | ADMIN   | Company A
Alice   | EMPLOYEE| Company B
```

Now the role assignment itself has scope.

This is extremely common in SaaS applications.

---

# 18. Scoped RBAC

Think of:

```text
User
 ↓
Role
 ↓
Scope
```

For example:

```text
Alice
  ↓
MANAGER
  ↓
Organization A
```

Therefore:

```text
Alice
```

can have:

```text
project:update
```

but only inside:

```text
Organization A
```

This is sometimes called **tenant-scoped RBAC** or **resource-scoped RBAC**, depending on the exact model.

---

# 19. Example

Suppose:

```text
Alice
```

has:

```text
MANAGER @ Engineering
```

and:

```text
EMPLOYEE @ Finance
```

Request:

```http
PUT /organizations/engineering/projects/123
```

Authorization:

```text
Alice
 ↓
MANAGER
 ↓
Engineering
 ↓
project:update
```

Allowed.

But:

```http
PUT /organizations/finance/projects/456
```

Alice's Finance role is:

```text
EMPLOYEE
```

and EMPLOYEE doesn't have:

```text
project:update
```

Therefore:

```text
403
```

---

# 20. Scope Changes Our Database

Instead of:

```sql
CREATE TABLE user_roles (
    user_id BIGINT,
    role_id BIGINT
);
```

we may have:

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,

    PRIMARY KEY (
        user_id,
        role_id,
        organization_id
    )
);
```

Now a role assignment is not merely:

```text
Alice → MANAGER
```

but:

```text
Alice → MANAGER → Organization A
```

That's a much more powerful model.

---

# 21. A Very Important Security Rule

Never rely on the client to tell you:

```json
{
  "organizationId": "company-b"
}
```

and blindly trust it for authorization.

For example, don't do:

```text
Client says:
organizationId = B

Server:
Okay, user belongs to B.
```

Instead, the server must establish the relationship from trusted state:

```text
Authenticated user
       ↓
Server-side membership
       ↓
Organization
       ↓
Role
       ↓
Permission
```

The client can request a resource.

It does not get to decide what it is authorized to access.

---

# 22. The Authorization Decision

We can now make our authorization function much more realistic.

Instead of:

```java
isAllowed(user, "project:update")
```

we might have:

```java
isAllowed(
    user,
    "project:update",
    project
)
```

Internally:

```java
boolean isAllowed(
    User user,
    Permission permission,
    Project project
) {

    return hasPermission(user, permission)
        && belongsToSameOrganization(user, project);
}
```

This is a much more production-like authorization model.

---

# 23. RBAC Is Becoming a Policy Engine

Notice what happened.

We started with:

```text
User → Role → Permission
```

Then:

```text
User → Role → Permission
                  +
              Resource
```

Then:

```text
User
Role
Permission
Resource
Organization
Action
```

Our decision starts looking like:

```text
Can(
    principal,
    perform action,
    on resource,
    under context
)?
```

For example:

```text
Can(
    Alice,
    update,
    Project 123,
    Organization A
)?
```

This is the foundation of more advanced authorization models.

---

# 24. RBAC vs ABAC

You'll often hear:

```text
RBAC
ABAC
```

### RBAC

Decision primarily based on roles:

```text
User
 ↓
Role
 ↓
Permission
```

### ABAC

Decision based on attributes.

For example:

```text
User.department == "Engineering"
AND
Resource.department == "Engineering"
AND
Action == "read"
```

Or:

```text
User.clearance >= Resource.classification
```

Or:

```text
Request.ipAddress ∈ corporateNetwork
```

ABAC can consider many attributes.

---

# 25. Most Real Systems Combine Them

A realistic rule might be:

```text
User has project:update
AND
User belongs to project's organization
AND
User is active
AND
Project is not archived
```

We can think of it as:

```text
RBAC
 +
resource authorization
 +
contextual policy
```

rather than trying to force everything into roles.

---

# 26. Another Important Concept: Separation of Duties

Suppose a financial application has:

```text
PAYMENT_CREATOR
PAYMENT_APPROVER
```

You might deliberately prevent the same person from having both roles.

Why?

Because the business rule is:

```text
Person A creates payment
Person B approves payment
```

not:

```text
Person A creates
      ↓
Person A approves
```

This is called **Separation of Duties (SoD)**.

---

# 27. Static Separation of Duties

Suppose:

```text
Alice
```

has:

```text
PAYMENT_CREATOR
```

The system should prevent:

```text
Alice → PAYMENT_APPROVER
```

at role-assignment time.

So:

```text
Assign role
   ↓
Check conflicting roles
   ↓
Conflict?
 ┌─┴─┐
YES  NO
 │    │
Reject Assign
```

---

# 28. Dynamic Separation of Duties

Sometimes the roles themselves can coexist.

But a user cannot perform both operations within the same workflow.

For example:

```text
Alice
 ├── PAYMENT_CREATOR
 └── PAYMENT_APPROVER
```

Maybe that's allowed.

But for one specific payment:

```text
Alice creates payment
```

then:

```text
Alice cannot approve that same payment
```

Even though she has both permissions.

Now the rule is contextual:

```text
creator_id != approver_id
```

This is another example of why simple RBAC eventually needs contextual policies.

---

# 29. Our RBAC Model Has Evolved

We started with:

```text
User
 ↓
Role
 ↓
Permission
```

Then:

```text
User
 ↓
Multiple Roles
 ↓
Permissions
```

Then:

```text
Role
 ↓
Role Hierarchy
 ↓
Inherited Permissions
```

Then:

```text
Permission
 +
Resource
 +
Organization
 +
Context
```

Then:

```text
Separation of Duties
```

We're gradually moving from simple RBAC toward a general authorization system.

---

# 30. The Mental Model to Keep

Don't think:

```text
if ADMIN then allow
```

Think:

```text
                 Request
                    │
                    ▼
                Principal
                    │
                    ▼
                  Roles
                    │
                    ▼
              Permissions
                    │
                    ▼
              Required Action
                    │
                    ▼
              Resource Context
                    │
                    ▼
                 Policy
                    │
              ┌─────┴─────┐
              │           │
            ALLOW        DENY
```

And remember:

```text
Role
≠
Permission
```

and:

```text
Permission
≠
Complete authorization decision
```

A permission says:

> "This principal has this capability."

A complete authorization decision may additionally need to say:

> "This principal may exercise that capability **on this resource, in this organization, under these conditions**."

---

# Next Lesson

Next we'll get hands-on with **database design and SQL**.

We'll build the complete schema:

```text
users
roles
permissions
user_roles
role_permissions
role_inheritance
organizations
organization_memberships
```

Then we'll write actual SQL queries for questions such as:

```sql
-- What roles does Alice have?

-- What permissions does Alice effectively have?

-- Does Alice have project:update?

-- What permissions does a MANAGER inherit?

-- Who has a particular permission?

-- Remove a role safely

-- Prevent duplicate/conflicting assignments
```

We'll also look at **how to efficiently query RBAC at scale**, including indexes and caching.


---

# Lesson 5 — RBAC Database Design + SQL

Now let's build the RBAC model we have been discussing into an actual relational database.

We'll use **PostgreSQL-style SQL**, but the concepts apply to MySQL and other relational databases too.

Our target system will support:

* users
* organizations/tenants
* roles
* permissions
* users having multiple roles
* organization-scoped roles
* role inheritance
* efficient permission checks

---

# 1. Start With the Simplest Model

The basic RBAC model is:

```text
users
  │
  │
  ▼
user_roles
  │
  ▼
roles
  │
  ▼
role_permissions
  │
  ▼
permissions
```

The tables are:

```text
users
roles
permissions
user_roles
role_permissions
```

Let's create those first.

---

# 2. `users`

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Example:

```text
id | name    | email
---+---------+-------------------
1  | Alice   | alice@example.com
2  | Bob     | bob@example.com
3  | Charlie | charlie@example.com
```

---

# 3. `roles`

```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);
```

Example:

```text
1 | ADMIN
2 | MANAGER
3 | EMPLOYEE
4 | AUDITOR
```

A role is essentially a **named collection of permissions**.

---

# 4. `permissions`

```sql
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT
);
```

Examples:

```text
employee:read
employee:create
employee:update
employee:delete

project:read
project:create
project:update
project:delete

report:read
```

A useful naming convention is:

```text
resource:action
```

For example:

```text
project:read
project:create
invoice:approve
document:download
```

This isn't required by RBAC itself; it's simply a clean convention.

---

# 5. `user_roles`

A user can have multiple roles.

A role can belong to many users.

Therefore:

```text
User ←→ Role
```

is many-to-many.

We need a junction table:

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),

    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);
```

Now:

```text
Alice → MANAGER
Alice → AUDITOR
```

would be:

```text
user_id | role_id
--------+--------
1       | 2
1       | 4
```

---

# 6. `role_permissions`

A role can have many permissions.

A permission can belong to many roles.

Therefore:

```text
Role ←→ Permission
```

is also many-to-many.

```sql
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE
);
```

Now the complete basic schema is:

```text
                 users
                   │
                   │
              user_roles
                   │
                   ▼
                 roles
                   │
              role_permissions
                   │
                   ▼
              permissions
```

---

# 7. Insert Some Data

Let's create roles:

```sql
INSERT INTO roles (name)
VALUES
    ('ADMIN'),
    ('MANAGER'),
    ('EMPLOYEE'),
    ('AUDITOR');
```

Permissions:

```sql
INSERT INTO permissions (name)
VALUES
    ('employee:read'),
    ('employee:create'),
    ('employee:update'),
    ('employee:delete'),
    ('project:read'),
    ('project:create'),
    ('project:update'),
    ('project:delete'),
    ('report:read'),
    ('audit:read');
```

Users:

```sql
INSERT INTO users (name, email)
VALUES
    ('Alice', 'alice@example.com'),
    ('Bob', 'bob@example.com'),
    ('Charlie', 'charlie@example.com');
```

---

# 8. Assign Roles

Suppose:

```text
Alice → ADMIN
Bob → MANAGER
Charlie → EMPLOYEE
```

We could insert using IDs:

```sql
INSERT INTO user_roles (user_id, role_id)
VALUES
    (1, 1),
    (2, 2),
    (3, 3);
```

But in real application code, don't assume role IDs.

IDs can differ between environments.

Instead, you might do:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT
    1,
    id
FROM roles
WHERE name = 'ADMIN';
```

This is safer for seed/migration logic.

---

# 9. Assign Permissions to Roles

Let's say:

### EMPLOYEE

```text
employee:read
project:read
```

### MANAGER

```text
employee:read
project:read
project:create
project:update
```

### ADMIN

Everything.

We can insert using names.

For EMPLOYEE:

```sql
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'EMPLOYEE'
  AND p.name IN (
      'employee:read',
      'project:read'
  );
```

---

# 10. Manager Permissions

```sql
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.name IN (
      'employee:read',
      'project:read',
      'project:create',
      'project:update'
  );
```

---

# 11. Auditor Permissions

```sql
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'AUDITOR'
  AND p.name IN (
      'project:read',
      'report:read',
      'audit:read'
  );
```

---

# 12. Query: What Roles Does Alice Have?

Simple:

```sql
SELECT r.name
FROM roles r
JOIN user_roles ur
    ON ur.role_id = r.id
WHERE ur.user_id = 1;
```

Result:

```text
ADMIN
```

If Alice has two roles:

```text
MANAGER
AUDITOR
```

you'd get:

```text
MANAGER
AUDITOR
```

---

# 13. Query: What Permissions Does Alice Have?

Now we traverse:

```text
Alice
 ↓
user_roles
 ↓
roles
 ↓
role_permissions
 ↓
permissions
```

SQL:

```sql
SELECT DISTINCT p.name
FROM permissions p
JOIN role_permissions rp
    ON rp.permission_id = p.id
JOIN user_roles ur
    ON ur.role_id = rp.role_id
WHERE ur.user_id = 1;
```

`DISTINCT` matters because Alice might have two roles containing the same permission.

For example:

```text
MANAGER → project:read
AUDITOR → project:read
```

We only want:

```text
project:read
```

once.

---

# 14. The Most Important Query

Suppose the API needs to determine:

> Does Alice have `project:update`?

We don't actually need to load every permission.

We can ask the database directly:

```sql
SELECT EXISTS (
    SELECT 1
    FROM user_roles ur
    JOIN role_permissions rp
        ON rp.role_id = ur.role_id
    JOIN permissions p
        ON p.id = rp.permission_id
    WHERE ur.user_id = 1
      AND p.name = 'project:update'
);
```

Result:

```text
true
```

or:

```text
false
```

This is a very useful authorization query.

---

# 15. Why `EXISTS`?

We're asking:

> Does at least one matching permission exist?

We don't care how many.

So:

```sql
EXISTS (...)
```

expresses the intent very clearly.

Conceptually:

```text
Find one matching permission
       ↓
Found?
 ┌─────┴─────┐
 YES         NO
  ↓           ↓
true        false
```

The database can often stop looking once it finds a matching row.

---

# 16. Query: Who Has a Permission?

Suppose security asks:

> Who can delete employees?

```sql
SELECT DISTINCT u.id, u.name
FROM users u
JOIN user_roles ur
    ON ur.user_id = u.id
JOIN role_permissions rp
    ON rp.role_id = ur.role_id
JOIN permissions p
    ON p.id = rp.permission_id
WHERE p.name = 'employee:delete';
```

This is useful for auditing.

---

# 17. Query: Which Roles Have a Permission?

Maybe we don't care about users.

```sql
SELECT r.name
FROM roles r
JOIN role_permissions rp
    ON rp.role_id = r.id
JOIN permissions p
    ON p.id = rp.permission_id
WHERE p.name = 'project:update';
```

Result:

```text
MANAGER
ADMIN
```

---

# 18. A Very Important Database Principle

Don't store:

```text
user.permissions = "project:read,project:update,..."
```

inside the `users` table.

Avoid things like:

```text
permissions VARCHAR
```

containing:

```text
"project:read,project:update,employee:read"
```

That creates problems with:

* querying
* uniqueness
* referential integrity
* updates
* indexing
* auditing

Instead, normalize the relationships:

```text
users
roles
permissions
user_roles
role_permissions
```

Relational databases are particularly good at this kind of relationship.

---

# 19. Now Add Organizations

Let's make the system multi-tenant.

```text
Company A
 ├── Alice
 └── Bob

Company B
 └── Charlie
```

Create:

```sql
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
```

Now:

```text
organizations
```

contains:

```text
1 | Company A
2 | Company B
```

---

# 20. Organization Membership

We shouldn't necessarily put:

```text
organization_id
```

directly on `users`.

Why?

Because a user might belong to multiple organizations.

Instead:

```sql
CREATE TABLE organization_memberships (
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    PRIMARY KEY (organization_id, user_id),

    FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);
```

Now:

```text
Alice → Company A
Alice → Company B
```

is possible.

---

# 21. Scoped Roles

Now we need to answer:

> What role does Alice have **inside Company A**?

This is different from:

> What roles does Alice have globally?

So `user_roles` becomes scoped.

A practical design:

```sql
CREATE TABLE organization_user_roles (
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (
        organization_id,
        user_id,
        role_id
    ),

    FOREIGN KEY (organization_id, user_id)
        REFERENCES organization_memberships(
            organization_id,
            user_id
        )
        ON DELETE CASCADE,

    FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);
```

Now we can represent:

```text
Alice
 ├── ADMIN   @ Company A
 └── EMPLOYEE @ Company B
```

---

# 22. Why This Is Important

Imagine Alice sends:

```http
GET /organizations/2/projects
```

The server must not simply ask:

```sql
SELECT role
FROM user_roles
WHERE user_id = 1;
```

because Alice may have different roles in different organizations.

Instead:

```sql
SELECT r.name
FROM organization_user_roles our
JOIN roles r
    ON r.id = our.role_id
WHERE our.organization_id = 2
  AND our.user_id = 1;
```

Now the authorization context is:

```text
Alice
+
Company B
```

rather than just:

```text
Alice
```

---

# 23. Effective Permission Query for a Tenant

Suppose:

```text
Alice
ADMIN @ Company A
EMPLOYEE @ Company B
```

We want to know:

> Can Alice create a project in Company B?

```sql
SELECT EXISTS (
    SELECT 1
    FROM organization_user_roles our
    JOIN role_permissions rp
        ON rp.role_id = our.role_id
    JOIN permissions p
        ON p.id = rp.permission_id
    WHERE our.organization_id = 2
      AND our.user_id = 1
      AND p.name = 'project:create'
);
```

This is much safer than checking Alice's global roles.

---

# 24. Why the Organization Must Be Part of the Authorization Query

This is a serious security issue.

Bad:

```sql
WHERE user_id = ?
  AND permission = ?
```

Potentially dangerous in a multi-tenant system.

Better:

```sql
WHERE user_id = ?
  AND organization_id = ?
  AND permission = ?
```

Because authorization must establish:

```text
User
  ↓
belongs to organization
  ↓
has role in organization
  ↓
role grants permission
```

---

# 25. Add Indexes

Our authorization queries will run frequently.

Indexes matter.

For:

```sql
user_roles
```

we already have:

```sql
PRIMARY KEY (user_id, role_id)
```

which helps queries starting with `user_id`.

For:

```sql
role_permissions
```

we have:

```sql
PRIMARY KEY (role_id, permission_id)
```

which helps:

```text
role → permissions
```

But we may also need the reverse lookup:

```text
permission → roles
```

So we can add:

```sql
CREATE INDEX idx_role_permissions_permission
ON role_permissions(permission_id, role_id);
```

---

# 26. Organization Role Index

Our primary key:

```sql
PRIMARY KEY (
    organization_id,
    user_id,
    role_id
)
```

works well for:

```text
organization + user
```

queries.

If we frequently ask:

```text
What organizations does this user belong to?
```

we should add:

```sql
CREATE INDEX idx_org_user_roles_user
ON organization_user_roles(user_id, organization_id);
```

Index design should follow your actual query patterns.

---

# 27. Don't Over-Index

It's tempting to create:

```text
index_user
index_role
index_permission
index_org
index_user_role
index_role_permission
...
```

everywhere.

But indexes have costs:

```text
INSERT
UPDATE
DELETE
```

must maintain them.

So the principle is:

> Index the access paths your authorization queries actually use.

---

# 28. Role Hierarchy in SQL

Now let's add role inheritance.

```sql
CREATE TABLE role_inheritance (
    parent_role_id BIGINT NOT NULL,
    child_role_id BIGINT NOT NULL,

    PRIMARY KEY (
        parent_role_id,
        child_role_id
    ),

    FOREIGN KEY (parent_role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    FOREIGN KEY (child_role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CHECK (parent_role_id <> child_role_id)
);
```

For example:

```text
ADMIN → MANAGER
MANAGER → EMPLOYEE
```

Depending on naming conventions, you could instead call these:

```text
inheriting_role
inherited_role
```

which may be less ambiguous.

---

# 29. The Problem With Recursive Role Hierarchies

Suppose:

```text
ADMIN
 ↓
MANAGER
 ↓
EMPLOYEE
```

To calculate ADMIN's permissions, we need:

```text
ADMIN permissions
+
MANAGER permissions
+
EMPLOYEE permissions
```

That's a recursive graph traversal.

In PostgreSQL, recursive CTEs can handle this.

For example:

```sql
WITH RECURSIVE role_tree AS (
    SELECT
        id
    FROM roles
    WHERE name = 'ADMIN'

    UNION

    SELECT
        ri.child_role_id
    FROM role_inheritance ri
    JOIN role_tree rt
        ON ri.parent_role_id = rt.id
)
SELECT *
FROM role_tree;
```

This gives the roles reachable from ADMIN.

---

# 30. Then Get Permissions

We can combine that with `role_permissions`.

Conceptually:

```text
ADMIN
 ↓
role_tree
 ↓
ADMIN
MANAGER
EMPLOYEE
 ↓
role_permissions
 ↓
permissions
```

SQL:

```sql
WITH RECURSIVE role_tree AS (
    SELECT id
    FROM roles
    WHERE name = 'ADMIN'

    UNION

    SELECT ri.child_role_id
    FROM role_inheritance ri
    JOIN role_tree rt
        ON ri.parent_role_id = rt.id
)
SELECT DISTINCT p.name
FROM role_tree rt
JOIN role_permissions rp
    ON rp.role_id = rt.id
JOIN permissions p
    ON p.id = rp.permission_id;
```

This is a powerful query.

---

# 31. But Do We Really Want Recursive Queries on Every Request?

Probably not.

Imagine:

```text
100,000 API requests/sec
```

and every request performs:

```text
recursive role traversal
+
permission lookup
```

That isn't a great architecture.

Instead, role hierarchies are often:

* resolved ahead of time
* cached
* flattened
* represented using another hierarchy structure
* evaluated by a dedicated authorization layer

For example, we might precompute:

```text
ADMIN
 ↓
effective permissions
```

and cache:

```text
role:ADMIN:permissions
```

Then runtime authorization is much simpler.

---

# 32. Static vs Dynamic Authorization Data

This gives us another useful distinction.

### Relatively static

```text
roles
permissions
role hierarchy
```

These don't necessarily change frequently.

### Dynamic

```text
user role assignments
organization membership
account status
resource ownership
```

These may change more often.

That means caching strategies can differ.

For example:

```text
Role permissions
    ↓
longer cache lifetime

User's current roles
    ↓
shorter cache lifetime / invalidation
```

---

# 33. Transactional Role Assignment

Suppose an administrator wants to assign:

```text
MANAGER
```

to Bob.

We might need to:

1. verify Bob exists
2. verify organization membership
3. verify role exists
4. verify no conflicting role
5. insert role assignment
6. invalidate authorization cache

These operations should be carefully coordinated.

For example:

```text
BEGIN

verify membership
verify role
check constraints

INSERT organization_user_roles ...

COMMIT

invalidate cache
```

The exact cache/event strategy depends on the system.

---

# 34. Why Constraints Matter

Suppose you don't have:

```sql
PRIMARY KEY (
    organization_id,
    user_id,
    role_id
)
```

Then you could accidentally insert:

```text
Alice → MANAGER → Company A
Alice → MANAGER → Company A
Alice → MANAGER → Company A
```

three times.

The application might not notice immediately.

Database constraints should enforce invariants whenever practical.

---

# 35. Database Is Not the Whole Authorization System

This is an important architectural distinction.

Your database might store:

```text
who has what role
what permissions roles have
```

But your application still needs to enforce:

```text
Can this request perform this action?
```

So:

```text
Database
   ↓
Authorization state
```

while:

```text
Authorization service
   ↓
Authorization decision
```

They're related, but not identical.

---

# 36. Our Complete Data Model

At this point, we have:

```text
                       users
                         │
                         │
              organization_memberships
                         │
                         ▼
                  organizations
                         │
                         │
               organization_user_roles
                         │
                         ▼
                       roles
                      /     \
                     /       \
                    ▼         ▼
          role_inheritance   role_permissions
                                  │
                                  ▼
                             permissions
```

And runtime:

```text
HTTP request
     ↓
Authenticated user
     ↓
Organization context
     ↓
Role assignment
     ↓
Effective permissions
     ↓
Resource/context rules
     ↓
ALLOW / DENY
```

---

# 37. A Real Authorization Query

Let's say we have:

```text
user_id = 123
organization_id = 456
permission = "project:update"
```

The question is:

> Does user 123 have `project:update` within organization 456?

The simplest non-hierarchical query is:

```sql
SELECT EXISTS (
    SELECT 1
    FROM organization_user_roles our
    JOIN role_permissions rp
        ON rp.role_id = our.role_id
    JOIN permissions p
        ON p.id = rp.permission_id
    WHERE our.organization_id = 456
      AND our.user_id = 123
      AND p.name = 'project:update'
);
```

That's essentially your RBAC authorization engine in SQL form.

---

# 38. One More Important Optimization

Don't necessarily do this on every request:

```text
Request
 ↓
SELECT roles
 ↓
SELECT permissions
 ↓
construct objects
 ↓
check permission
```

Instead, your authorization layer can expose:

```java
authorizationService.hasPermission(
    userId,
    organizationId,
    "project:update"
);
```

Internally it can use:

```text
L1 memory cache
       ↓
Redis
       ↓
Database
```

depending on your architecture.

The controller shouldn't care.

---

# 39. What Should the Controller Know?

Ideally:

```java
@RequirePermission("project:update")
public Project updateProject(...) {
    ...
}
```

The controller knows:

```text
"This operation requires project:update."
```

It doesn't need to know:

```text
Alice → MANAGER
MANAGER → role_id 27
role_id 27 → permission_id 43
Redis key = ...
```

That's authorization infrastructure.

Keeping these concerns separate makes the system easier to maintain.

---

# 40. The Architecture We've Built

Think of the system as four layers:

```text
┌─────────────────────────────────┐
│          HTTP / API             │
│                                 │
│  @RequirePermission(...)        │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│       Authorization Layer       │
│                                 │
│  hasPermission(...)             │
│  canAccessResource(...)         │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│     Authorization State         │
│                                 │
│ roles                            │
│ permissions                      │
│ memberships                      │
│ assignments                      │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│       Database / Cache          │
└─────────────────────────────────┘
```

This separation becomes very valuable as the application grows.

---

# 41. What We Haven't Solved Yet

There are still several difficult problems.

For example:

### Problem 1 — Resource ownership

```text
Bob has project:update

Can Bob update Project 123?
```

Not necessarily.

---

### Problem 2 — Tenant isolation

```text
Bob is MANAGER in Company A.

Can he update Company B's project?
```

No.

---

### Problem 3 — Role revocation

```text
Bob's MANAGER role is removed.

How quickly should that take effect?
```

Depends on token/cache architecture.

---

### Problem 4 — Conflicting permissions

```text
Role A → ALLOW
Role B → DENY
```

What is the policy?

---

### Problem 5 — Hierarchies

```text
ADMIN
 ↓
MANAGER
 ↓
EMPLOYEE
```

How do we efficiently calculate inherited permissions?

---

### Problem 6 — Separation of duties

```text
User can create payments.

Can the same user approve their own payment?
```

Maybe not.

These problems push us beyond simple RBAC.

---

# Next Lesson — Hands-On Authorization Middleware

Next we'll leave the database for a moment and implement the actual **request-time authorization layer**.

We'll build this:

```text
POST /projects
        │
        ▼
Authentication Filter
        │
        ▼
SecurityContext
        │
        ▼
@RequirePermission("project:create")
        │
        ▼
AuthorizationInterceptor
        │
        ▼
AuthorizationService
        │
        ├── has permission?
        │
        ├── correct organization?
        │
        └── resource accessible?
        │
      ┌─┴─┐
     YES  NO
      │    │
      ▼    ▼
Controller 403
```

We'll also distinguish **filter vs interceptor vs middleware vs controller-level checks**, because understanding *where* authorization belongs is just as important as understanding RBAC itself.


---

# Lesson 6 — Where Should Authorization Happen?

Now let's connect everything we've learned to an actual HTTP backend.

The key question is:

> **At what point in the request lifecycle should we perform authorization?**

This matters because putting authorization in the wrong place can create security holes or make the system impossible to maintain.

---

# 1. Our Target Architecture

We want something like:

```text
HTTP Request
     │
     ▼
Authentication
     │
     ▼
Security Context
     │
     ▼
Authorization
     │
     ▼
Controller
     │
     ▼
Business Logic
     │
     ▼
Database
```

For example:

```http
PUT /projects/123
Authorization: Bearer <token>
```

should flow through:

```text
Token
 ↓
Who is the user?
 ↓
What permission is required?
 ↓
Does user have it?
 ↓
Can user access Project 123?
 ↓
Execute update
```

---

# 2. First, Separate the Security Layers

A typical web application has several layers.

```text
┌─────────────────────────────┐
│ HTTP                        │
├─────────────────────────────┤
│ Authentication              │
├─────────────────────────────┤
│ Authorization               │
├─────────────────────────────┤
│ Controller                  │
├─────────────────────────────┤
│ Service / Business Logic    │
├─────────────────────────────┤
│ Repository / Database       │
└─────────────────────────────┘
```

Each layer has a different responsibility.

---

# 3. Authentication Filter

The authentication layer answers:

> Who is making this request?

For JWT authentication:

```text
Authorization: Bearer eyJ...
```

The authentication filter might:

```text
1. Extract token
2. Validate signature
3. Validate expiration
4. Validate issuer/audience as applicable
5. Extract subject
6. Establish authenticated principal
```

Conceptually:

```java
Authentication authentication =
    authenticate(token);

SecurityContext.setAuthentication(authentication);
```

After that:

```text
current user = Alice
```

---

# 4. Authorization Comes After Authentication

Now authorization asks:

> Is Alice allowed to perform this operation?

For:

```http
DELETE /employees/123
```

we might require:

```text
employee:delete
```

So:

```text
Authentication
      ↓
Alice
      ↓
Authorization
      ↓
employee:delete?
```

---

# 5. Three Common Places for Authorization

You'll commonly see authorization implemented using:

1. **Middleware / filters**
2. **Controller/interceptor/method authorization**
3. **Service-layer authorization**

They're not necessarily mutually exclusive.

Each is useful for different types of checks.

---

# 6. Middleware / Filter Authorization

Imagine:

```text
POST /projects
```

requires:

```text
project:create
```

A filter can inspect the request:

```text
Request
  ↓
Authentication
  ↓
Authorization Filter
  ↓
Controller
```

The filter could determine:

```text
POST /projects
       ↓
project:create
```

and then check:

```text
currentUser.hasPermission("project:create")
```

If false:

```http
403 Forbidden
```

The controller isn't invoked.

---

# 7. Why Filters Are Useful

They are excellent for **coarse-grained authorization**.

For example:

```text
GET /admin/users
```

requires:

```text
admin:user:read
```

A filter can reject unauthorized requests before application logic executes.

This gives you:

```text
centralized enforcement
```

instead of:

```text
every controller remembering to check authorization
```

---

# 8. But Filters Have a Limitation

Consider:

```http
PUT /projects/123
```

Suppose the user has:

```text
project:update
```

But the rule is:

> Users may update projects only within their own organization.

The filter sees:

```text
PUT /projects/123
```

but may not yet have loaded:

```text
Project 123
```

Therefore it might not know:

```text
project.organization_id
```

This is a **resource-level authorization** problem.

---

# 9. Controller-Level Authorization

We could declare:

```java
@RequirePermission("project:update")
@PutMapping("/projects/{id}")
public Project updateProject(
    @PathVariable Long id,
    ...
) {
    ...
}
```

Now the endpoint explicitly declares:

```text
Required permission:
project:update
```

This is much cleaner than:

```java
if (!user.hasRole("MANAGER")) {
    throw ...
}
```

---

# 10. Why Permission Instead of Role?

Suppose:

```text
MANAGER → project:update
ADMIN   → project:update
PROJECT_EDITOR → project:update
```

If we write:

```java
@RequireRole("MANAGER")
```

then:

```text
ADMIN
```

wouldn't necessarily work unless explicitly handled.

But:

```java
@RequirePermission("project:update")
```

works for any role that grants the permission.

This keeps your endpoint independent of role configuration.

---

# 11. The Annotation Is Metadata

Remember:

```java
@RequirePermission("project:update")
```

doesn't itself perform the check.

It's essentially metadata saying:

```text
This method requires:
project:update
```

Some infrastructure needs to enforce it.

For example:

```text
Spring Security
    ↓
Method Security
    ↓
Authorization Manager
```

or your own:

```text
Interceptor
    ↓
AuthorizationService
```

---

# 12. Conceptual Interceptor

Imagine:

```java
public Object invoke(Method method, Object[] args) {

    RequirePermission annotation =
        method.getAnnotation(RequirePermission.class);

    String permission =
        annotation.value();

    User user =
        securityContext.currentUser();

    if (!authorizationService.hasPermission(
            user,
            permission)) {

        throw new ForbiddenException();
    }

    return invokeActualMethod(method, args);
}
```

The important sequence is:

```text
Find required permission
        ↓
Get current user
        ↓
Check permission
        ↓
Reject OR invoke method
```

---

# 13. Request Flow

Suppose:

```java
@RequirePermission("project:update")
@PutMapping("/projects/{id}")
public Project updateProject(...) {
    ...
}
```

Request:

```http
PUT /projects/123
```

Flow:

```text
                     Request
                        │
                        ▼
                JWT Authentication
                        │
                        ▼
                  User = Alice
                        │
                        ▼
              Method Authorization
                        │
                        ▼
             Required permission:
                project:update
                        │
                        ▼
             AuthorizationService
                        │
                ┌───────┴────────┐
                │                │
              YES                NO
                │                │
                ▼                ▼
          Controller             403
                │
                ▼
        updateProject()
```

---

# 14. But There's Still a Problem

Suppose Alice has:

```text
project:update
```

So method-level authorization passes.

Now:

```text
PUT /projects/123
```

loads:

```text
Project 123
```

and discovers:

```text
organization_id = 999
```

Alice belongs to:

```text
organization_id = 100
```

So:

```text
Permission check → PASS
Resource check → FAIL
```

Therefore we need another authorization layer.

---

# 15. Resource-Level Authorization

Now the service might do:

```java
public Project updateProject(
    User user,
    Long projectId,
    UpdateProjectRequest request
) {

    Project project =
        projectRepository.findById(projectId);

    authorizationService.checkCanUpdate(
        user,
        project
    );

    return update(project, request);
}
```

The policy might be:

```java
boolean canUpdate(User user, Project project) {

    return hasPermission(user, "project:update")
        && project.organizationId()
            .equals(user.organizationId());
}
```

Now we're checking the actual resource.

---

# 16. Two Authorization Layers

This gives us:

```text
Layer 1
────────
Can this user perform this type of operation?

        ↓

project:update


Layer 2
────────
Can this user perform it on THIS resource?

        ↓

Project 123 belongs to user's organization
```

Together:

```text
Authorization
=
Capability
+
Resource access
```

---

# 17. Why Not Put Everything in the Controller?

You could:

```java
@PutMapping("/projects/{id}")
public Project updateProject(...) {

    if (!hasPermission(...)) {
        throw ...
    }

    Project project = repository.findById(id);

    if (!sameOrganization(...)) {
        throw ...
    }

    ...
}
```

But now every controller becomes full of security code.

A better separation is:

```text
Controller
    ↓
Service
    ↓
Authorization policy
    ↓
Business operation
```

For example:

```java
authorizationService.checkCanUpdateProject(
    user,
    project
);
```

---

# 18. But Don't Blindly Trust the Service Layer Either

There's an important security principle here:

> **Authorization should be enforced at a boundary where it cannot accidentally be skipped.**

Suppose today:

```text
Controller A
    ↓
Service
    ↓
authorization check
```

But tomorrow another endpoint calls the same business operation through:

```text
Controller B
    ↓
Service
```

and forgets authorization.

You could accidentally create a bypass.

That's why many systems combine:

```text
coarse-grained authorization
+
resource-level authorization
+
database/query-level isolation where appropriate
```

---

# 19. Authorization Should Be "Fail Closed"

This is an extremely important principle.

Suppose something goes wrong:

```text
Could not load permissions
```

Should we do:

```text
ALLOW
```

or:

```text
DENY
```

Generally:

```text
DENY
```

In other words:

```text
Unable to establish authorization
        ↓
Don't assume permission
        ↓
Reject
```

This is called **fail closed**.

---

# 20. Example

Suppose:

```text
Redis unavailable
```

and you normally get:

```text
user:123 → permissions
```

Don't do:

```java
if (permissions == null) {
    return true;
}
```

That would be catastrophic.

Instead:

```java
if (permissions == null) {
    throw new AuthorizationUnavailableException();
}
```

Then the application can return an appropriate error rather than accidentally granting access.

---

# 21. Don't Confuse 401 and 403

Again, because this becomes important in middleware.

### No valid authentication

```text
401 Unauthorized
```

Example:

```text
No token
Invalid token
Expired token
```

Conceptually:

```text
Who are you?
    ↓
I don't have a valid answer.
```

### Valid authentication, insufficient permission

```text
403 Forbidden
```

Conceptually:

```text
I know who you are.
But you're not allowed to do this.
```

So:

```text
401 → authentication problem
403 → authorization problem
```

---

# 22. Don't Leak Too Much Information

Suppose:

```http
GET /projects/999
```

and Project 999 doesn't belong to Alice.

Should the API return:

```http
403 Forbidden
```

or:

```http
404 Not Found
```

There isn't one universal answer.

Sometimes returning `404` is useful to avoid revealing whether a resource exists.

For example:

```text
Alice asks for:
GET /projects/999
```

If the system returns:

```text
403
```

Alice learns:

> Project 999 exists, but I can't access it.

Returning:

```text
404
```

can instead make the resource appear nonexistent from Alice's perspective.

This is often called **resource existence hiding**.

The right choice depends on the application's security and API semantics.

---

# 23. Authorization vs Business Rules

Another subtle distinction.

Suppose:

```text
Alice has project:update
```

and:

```text
Project 123 belongs to Alice's organization.
```

Authorization passes.

But the project is:

```text
status = ARCHIVED
```

and the business rule says:

> Archived projects cannot be modified.

That's not necessarily an RBAC problem.

It's a **business rule**:

```text
project.status != ARCHIVED
```

So don't put every rule into RBAC.

Think:

```text
Authorization
    ↓
Is Alice allowed to attempt this operation?

Business logic
    ↓
Is this operation valid right now?
```

---

# 24. A Realistic Update Flow

Suppose:

```http
PUT /projects/123
```

Request flow:

```text
1. Authenticate Alice

2. Check:
   project:update

3. Load Project 123

4. Check:
   Alice can access Project 123

5. Check business rules:
   Project isn't archived

6. Update project
```

Visualized:

```text
Request
  │
  ▼
Authentication
  │
  ▼
Permission Check
  │
  ▼
Load Resource
  │
  ▼
Resource Authorization
  │
  ▼
Business Validation
  │
  ▼
Update
```

This separation is extremely useful.

---

# 25. What About `DELETE`?

Consider:

```http
DELETE /projects/123
```

We might require:

```text
project:delete
```

Then resource authorization:

```text
project.organization_id == user's organization_id
```

Then business rules:

```text
project.status != COMPLETED
```

So:

```text
Permission
   AND
Resource access
   AND
Business rule
```

must all pass.

---

# 26. A Better Authorization API

Instead of exposing dozens of scattered checks:

```java
hasPermission(...)
hasRole(...)
belongsToOrg(...)
isOwner(...)
...
```

you can create domain-oriented methods:

```java
authorizationService.canUpdateProject(
    user,
    project
);
```

or:

```java
authorizationService.requireCanUpdateProject(
    user,
    project
);
```

Internally:

```text
canUpdateProject
    │
    ├── project:update permission
    ├── organization membership
    ├── resource state
    └── other relevant policy
```

This makes the policy easier to understand and test.

---

# 27. But Be Careful With "God Authorization Services"

Don't create:

```java
AuthorizationService
```

with:

```java
canDoEverything(...)
```

containing hundreds of unrelated conditions.

For larger systems, policies can be separated:

```text
ProjectAuthorization
DocumentAuthorization
InvoiceAuthorization
EmployeeAuthorization
```

For example:

```java
projectAuthorization.requireCanUpdate(
    user,
    project
);
```

This keeps resource-specific rules close to the resource domain.

---

# 28. Testing Authorization

Authorization deserves its own tests.

For:

```text
project:update
```

test at least:

```text
MANAGER + same organization → allowed
MANAGER + different organization → denied
EMPLOYEE + same organization → denied
ADMIN + same organization → allowed
unauthenticated → 401
```

And don't only test happy paths.

Security bugs often appear in:

```text
wrong tenant
wrong resource owner
missing role
revoked role
expired token
multiple roles
```

---

# 29. A Very Important Test

Always test for **horizontal privilege escalation**.

Example:

```text
Alice owns Project 1
Bob owns Project 2
```

Alice requests:

```http
PUT /projects/2
```

Alice may have:

```text
project:update
```

but must not automatically be able to update Bob's project.

That's a classic authorization failure.

---

# 30. Another Important Test: Vertical Privilege Escalation

Suppose:

```text
EMPLOYEE
```

can:

```text
project:read
```

but not:

```text
project:delete
```

Test:

```http
DELETE /projects/123
```

and verify:

```http
403 Forbidden
```

This checks that ordinary users cannot perform privileged operations.

---

# 31. The Complete Mental Model

At this point, think about an authorization decision as:

```text
Can the principal perform this action
on this resource
within this scope
under this context?
```

For example:

```text
Principal:
    Alice

Action:
    update

Resource:
    Project 123

Scope:
    Company A

Context:
    Project is active
```

Then evaluate:

```text
Alice authenticated?
       ↓
      YES

Has project:update?
       ↓
      YES

Member of Company A?
       ↓
      YES

Project 123 belongs to Company A?
       ↓
      YES

Project is editable?
       ↓
      YES

             ↓
           ALLOW
```

That's much closer to real production authorization than:

```text
if (role == ADMIN)
```

---

# 32. Where RBAC Fits

It's useful to see RBAC as one component:

```text
                 Authorization
                      │
          ┌───────────┼───────────┐
          │           │           │
          ▼           ▼           ▼
         RBAC       Ownership    Tenant
          │           │           │
          └───────────┼───────────┘
                      │
                      ▼
                Policy Decision
                      │
                 ┌────┴────┐
                 ▼         ▼
               ALLOW      DENY
```

RBAC answers:

> What capabilities does this user's role grant?

It doesn't necessarily answer every question about a particular resource.

---

# 33. What Comes Next?

We've now covered:

```text
✓ Authentication vs authorization
✓ Users
✓ Roles
✓ Permissions
✓ Multiple roles
✓ Role hierarchy
✓ Scoped roles
✓ Organizations
✓ Resource authorization
✓ Ownership
✓ Separation of duties
✓ JWT + RBAC
✓ Revocation
✓ Caching
✓ Database design
✓ SQL permission checks
✓ Middleware
✓ Interceptors
✓ Service-level authorization
✓ 401 vs 403
✓ Fail-closed behavior
```

The next major concept is **ABAC — Attribute-Based Access Control**.

We'll start from the exact problem:

> "RBAC is becoming full of roles because our authorization rules depend on things like department, ownership, location, resource state, time, and data classification."

Then we'll see how ABAC solves that with:

```text
Subject attributes
+
Resource attributes
+
Action
+
Environment/context
        ↓
      Policy
        ↓
   ALLOW / DENY
```

We'll build concrete examples and compare:

```text
RBAC vs ACL vs ABAC
```

because understanding **when RBAC is enough and when it isn't** is one of the most important parts of learning authorization.

