// FILE: app/src/main/java/com/masud/moneymanager/data/AccountDao.kt
package com.masud.moneymanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Insert
    suspend fun insertAccount(account: AccountEntity): Long

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)

    @Query("SELECT * FROM account_entries WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun observeEntries(accountId: Long): Flow<List<AccountEntryEntity>>

    @Insert
    suspend fun insertEntry(entry: AccountEntryEntity)

    @Query("DELETE FROM account_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM account_entries WHERE accountId = :accountId")
    fun observeTotal(accountId: Long): Flow<Double>
}
