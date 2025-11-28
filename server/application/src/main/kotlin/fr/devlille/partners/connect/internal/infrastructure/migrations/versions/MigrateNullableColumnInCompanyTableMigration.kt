package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object MigrateNullableColumnInCompanyTableMigration : Migration {
    override val id = "20251128_nullable_column_in_company_table"
    override val description = "Make certain columns nullable in companies table"

    override fun up() {
        transaction {
            exec("ALTER TABLE companies ALTER COLUMN site_url DROP NOT NULL;")
            exec("ALTER TABLE companies ALTER COLUMN address DROP NOT NULL;")
            exec("ALTER TABLE companies ALTER COLUMN city DROP NOT NULL;")
            exec("ALTER TABLE companies ALTER COLUMN zip_code DROP NOT NULL;")
            exec("ALTER TABLE companies ALTER COLUMN country DROP NOT NULL;")
            exec("ALTER TABLE companies ALTER COLUMN siret DROP NOT NULL;")
            exec("ALTER TABLE companies ALTER COLUMN vat DROP NOT NULL;")
        }
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported - would require dropping column which could cause data loss",
        )
    }
}
