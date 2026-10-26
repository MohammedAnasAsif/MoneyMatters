package io.github.mohammedanasasif.moneymatters.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
    suspend fun getAll(): List<Transaction>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT COUNT(*) FROM transactions WHERE upiRef = :ref")
    suspend fun countByRef(ref: String): Int

    @Query(
        "SELECT * FROM transactions WHERE source != 'MANUAL_CASH' AND type = :type " +
            "AND amountPaise = :amountPaise AND timestamp BETWEEN :from AND :to"
    )
    suspend fun findNear(amountPaise: Long, type: String, from: Long, to: Long): List<Transaction>
}
