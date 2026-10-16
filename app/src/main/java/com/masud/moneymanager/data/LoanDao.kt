
package com.masud.moneymanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans ORDER BY createdAt DESC, id DESC")
    fun observeAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :loanId LIMIT 1")
    suspend fun getLoanById(loanId: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLoans(loans: List<LoanEntity>)

    @Query("DELETE FROM loans WHERE id = :loanId")
    suspend fun deleteLoan(loanId: Long)

    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId ORDER BY createdAt DESC, id DESC")
    fun observeRepayments(loanId: Long): Flow<List<LoanRepaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRepayment(repayment: LoanRepaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRepayments(repayments: List<LoanRepaymentEntity>)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM loan_repayments WHERE loanId = :loanId")
    suspend fun getTotalRepaid(loanId: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM loan_repayments")
    suspend fun getAllRepaymentsTotal(): Double

    @Query("SELECT COALESCE(SUM(principalAmount), 0) FROM loans WHERE type = :type")
    suspend fun getTotalPrincipal(type: String): Double

    @Query("DELETE FROM loan_repayments")
    suspend fun deleteAllRepayments()

    @Query("DELETE FROM loans")
    suspend fun deleteAllLoans()
}
