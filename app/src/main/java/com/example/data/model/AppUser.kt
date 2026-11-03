package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_users")
data class AppUser(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,       // ইউজার নেম (যেমন: Conock)
    val password: String,       // পাসওয়ার্ড (যেমন: 1219)
    val fullName: String,       // পুরো নাম
    val role: String = "ADMIN", // ADMIN, MANAGER, STAFF
    val phone: String = "",     // মোবাইল নম্বর
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
