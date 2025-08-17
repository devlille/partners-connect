# Database Migrations

This document explains how to create and manage database migrations in the partners-connect application.

## Overview

The application now uses a custom migration system built on top of Exposed ORM. This system allows contributors to specify SQL migration instructions between deployments with table changes.

## Migration System Architecture

### Core Components

1. **Migration Interface** (`Migration.kt`) - Defines the contract for all migrations
2. **MigrationManager** (`MigrationManager.kt`) - Manages migration execution and tracking
3. **MigrationsTable** (`MigrationsTable.kt`) - Database table to track applied migrations
4. **MigrationRegistry** (`MigrationRegistry.kt`) - Central registry of all migrations

### Migration Tracking

The system automatically creates a `schema_migrations` table to track which migrations have been applied:
- `migration_id` - The unique ID of the migration (format: YYYYMMDD_HHMMSS_description)
- `description` - Human-readable description of the migration
- `applied_at` - Timestamp when the migration was applied

## How to Create a New Migration

### Step 1: Create Migration File

Create a new migration file in `server/application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/`:

```kotlin
package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddUserPreferencesMigration : Migration {
    override val id = "20241220_140000_add_user_preferences_table"
    override val description = "Add user_preferences table to store user settings"
    
    override fun up() {
        SchemaUtils.create(UserPreferencesTable)
    }
    
    override fun down() {
        SchemaUtils.drop(UserPreferencesTable)
    }
}
```

### Step 2: Define Table Schema (if adding new tables)

If your migration creates new tables, define them in the same file:

```kotlin
private object UserPreferencesTable : UUIDTable("user_preferences") {
    val userId = reference("user_id", UsersTable)
    val theme = varchar("theme", 50).default("light")
    val language = varchar("language", 10).default("en")
    val notificationsEnabled = bool("notifications_enabled").default(true)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
```

### Step 3: Register Migration

Add your migration to `MigrationRegistry.kt`:

```kotlin
val allMigrations: List<Migration> = listOf(
    InitialSchemaMigration,
    AddUserPreferencesMigration, // Add your migration here
    // Add new migrations here in chronological order
)
```

### Step 4: Test Your Migration

Create tests for your migration to ensure it works correctly:

```kotlin
@Test
fun `migration creates user preferences table`() = testApplication {
    // Test implementation
}
```

## Migration Naming Convention

Use the following format for migration IDs: `YYYYMMDD_HHMMSS_description`

- **YYYYMMDD_HHMMSS**: UTC timestamp when you create the migration
- **description**: Short, descriptive name using snake_case

Examples:
- `20241220_140000_add_user_preferences_table`
- `20241220_141500_add_email_index_to_users`
- `20241220_143000_update_company_table_structure`

## Migration Types

### Table Creation
```kotlin
override fun up() {
    SchemaUtils.create(NewTable)
}
```

### Table Modification
```kotlin
override fun up() {
    SchemaUtils.addMissingColumnsStatements(ExistingTable).forEach { statement ->
        exec(statement)
    }
}
```

### Raw SQL (for complex changes)
```kotlin
override fun up() {
    exec("ALTER TABLE users ADD CONSTRAINT email_format CHECK (email LIKE '%@%')")
}
```

### Data Migration
```kotlin
override fun up() {
    // Update existing data
    UsersTable.update({ UsersTable.status.isNull() }) {
        it[status] = "active"
    }
}
```

## Migration Execution

### Automatic Execution

Migrations are automatically applied when the application starts up. The `MigrationManager` is called during database configuration in `App.kt`.

### Manual Execution (for debugging)

You can manually run migrations using the `MigrationManager`:

```kotlin
val manager = MigrationRegistry.createManager()
manager.migrate(database)
```

### Checking Migration Status

```kotlin
val appliedMigrations = manager.getAppliedMigrations(database)
val hasPendingMigrations = manager.isPendingMigrations(database)
```

## Best Practices

### 1. Make Migrations Additive
- Prefer adding new columns rather than modifying existing ones
- Use nullable columns or default values for new columns
- Avoid dropping columns immediately (deprecate first)

### 2. Test Migrations Thoroughly
- Test on a copy of production data
- Verify both `up()` and `down()` methods work
- Test migration rollback scenarios

### 3. Keep Migrations Small
- One logical change per migration
- Avoid combining unrelated schema changes
- Consider data volume for large table modifications

### 4. Handle Rollbacks
- Implement `down()` methods when possible
- Document when rollbacks are not supported
- Consider data loss implications

### 5. Use Transactions
- Migrations run within transactions automatically
- Keep migrations fast to avoid long-running transactions
- Be aware of database-specific transaction limitations

## Troubleshooting

### Migration Failed
1. Check application logs for error details
2. Verify migration syntax and table references
3. Check database permissions
4. Test migration on development database first

### Rolling Back Migrations
Currently, the system doesn't support automatic rollbacks. If you need to rollback:
1. Implement the `down()` method in your migration
2. Manually apply the rollback logic
3. Remove the migration record from `schema_migrations` table

### Development Workflow
1. Create and test migration locally
2. Apply to staging environment
3. Verify application works with new schema
4. Deploy to production

## Migration vs. Flyway Integration

This implementation provides a native Exposed-based migration system. For more complex migration needs, consider integrating with Flyway as mentioned in the original issue. The current system is designed to be simple and integrated with the existing Exposed ORM setup.