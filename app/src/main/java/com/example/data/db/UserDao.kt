package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppUser
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users ORDER BY role = 'ADMIN' DESC, fullName ASC")
    fun getAllUsers(): Flow<List<AppUser>>

    @Query("SELECT * FROM app_users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): AppUser?

    @Query("SELECT * FROM app_users WHERE username = :username AND password = :password AND isActive = 1 LIMIT 1")
    suspend fun authenticate(username: String, password: String): AppUser?

    @Query("SELECT COUNT(*) FROM app_users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: AppUser): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<AppUser>)

    @Update
    suspend fun updateUser(user: AppUser)

    @Delete
    suspend fun deleteUser(user: AppUser)

    @Query("DELETE FROM app_users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("DELETE FROM app_users")
    suspend fun deleteAllUsers()
}
