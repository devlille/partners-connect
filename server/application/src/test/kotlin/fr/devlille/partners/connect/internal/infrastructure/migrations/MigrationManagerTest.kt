package fr.devlille.partners.connect.internal.infrastructure.migrations

import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MigrationManagerTest {
    
    @Test
    fun `migration manager applies migrations in order`() = testApplication {
        application {
            moduleMocked()
        }
        
        val database = Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        
        val testMigration1 = TestMigration("001_first", "First migration")
        val testMigration2 = TestMigration("002_second", "Second migration")
        val migrations = listOf(testMigration2, testMigration1) // Intentionally out of order
        
        val manager = MigrationManager(migrations)
        
        manager.migrate(database)
        
        val appliedMigrations = manager.getAppliedMigrations(database)
        assertEquals(2, appliedMigrations.size)
        assertEquals("001_first", appliedMigrations[0]) // Should be applied first despite being last in list
        assertEquals("002_second", appliedMigrations[1])
        
        assertTrue(testMigration1.wasApplied)
        assertTrue(testMigration2.wasApplied)
    }
    
    @Test
    fun `migration manager skips already applied migrations`() = testApplication {
        application {
            moduleMocked()
        }
        
        val database = Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        
        val testMigration = TestMigration("001_test", "Test migration")
        val manager = MigrationManager(listOf(testMigration))
        
        // Apply migrations first time
        manager.migrate(database)
        assertTrue(testMigration.wasApplied)
        
        // Reset flag and apply again
        testMigration.wasApplied = false
        manager.migrate(database)
        assertFalse(testMigration.wasApplied) // Should not be applied again
        
        val appliedMigrations = manager.getAppliedMigrations(database)
        assertEquals(1, appliedMigrations.size)
        assertEquals("001_test", appliedMigrations[0])
    }
    
    @Test
    fun `isPendingMigrations returns correct status`() = testApplication {
        application {
            moduleMocked()
        }
        
        val database = Database.connect(
            url = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        
        val testMigration = TestMigration("001_test", "Test migration")
        val manager = MigrationManager(listOf(testMigration))
        
        assertTrue(manager.isPendingMigrations(database))
        
        manager.migrate(database)
        
        assertFalse(manager.isPendingMigrations(database))
    }
}

private class TestMigration(
    override val id: String,
    override val description: String,
) : Migration {
    var wasApplied = false
    
    override fun up() {
        wasApplied = true
    }
}