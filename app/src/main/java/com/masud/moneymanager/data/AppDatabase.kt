// FILE: app/src/main/java/com/masud/moneymanager/data/AppDatabase.kt
package com.masud.moneymanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        LoanEntity::class,
        LoanRepaymentEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        AccountEntryEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun loanDao(): LoanDao

    abstract fun categoryDao(): CategoryDao

    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val defaultExpense =
            listOf(
                "Food",
                "Transport",
                "Shopping",
                "Bills & Utilities",
                "Health",
                "Education",
                "Entertainment",
                "Rent",
                "General",
                "Other",
            )
        private val defaultIncome =
            listOf(
                "Salary",
                "Business",
                "Freelance",
                "Investment",
                "Gift",
                "Other",
            )

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `loans` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `personName` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `principalAmount` REAL NOT NULL,
                            `note` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `loan_repayments` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `loanId` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `note` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`loanId`) REFERENCES `loans`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_loan_repayments_loanId` " +
                            "ON `loan_repayments` (`loanId`)",
                    )
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `categories` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name_type` " +
                            "ON `categories` (`name`, `type`)",
                    )
                    val now = System.currentTimeMillis()
                    defaultExpense.forEach { name ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO `categories` (`name`, `type`, `createdAt`) VALUES (?, 'EXPENSE', ?)",
                            arrayOf(name, now),
                        )
                    }
                    defaultIncome.forEach { name ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO `categories` (`name`, `type`, `createdAt`) VALUES (?, 'INCOME', ?)",
                            arrayOf(name, now),
                        )
                    }
                }
            }

        private val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `accounts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `account_entries` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `accountId` INTEGER NOT NULL,
                            `amount` REAL NOT NULL,
                            `note` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_account_entries_accountId` " +
                            "ON `account_entries` (`accountId`)",
                    )
                }
            }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "money_manager.db",
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(
                        object : Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                CoroutineScope(Dispatchers.IO).launch {
                                    getInstance(context).let { instance ->
                                        if (instance.categoryDao().count() == 0) {
                                            val now = System.currentTimeMillis()
                                            defaultExpense.forEach {
                                                instance.categoryDao().insert(
                                                    CategoryEntity(name = it, type = "EXPENSE", createdAt = now),
                                                )
                                            }
                                            defaultIncome.forEach {
                                                instance.categoryDao().insert(
                                                    CategoryEntity(name = it, type = "INCOME", createdAt = now),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    ).build()
                    .also { INSTANCE = it }
            }
    }
}
