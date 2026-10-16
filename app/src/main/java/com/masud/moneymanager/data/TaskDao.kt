// FILE: app/src/main/java/com/masud/moneymanager/data/TaskDao.kt
package com.masud.moneymanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isDone ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun getById(id: Long): TaskEntity?

    @Insert suspend fun insert(task: TaskEntity): Long

    @Update suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks") suspend fun deleteAll()
}
