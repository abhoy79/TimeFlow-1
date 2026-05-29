package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dueDate: Long, // timestamp
    val priority: String, // "High", "Medium", "Low"
    val notes: String = "",
    val isCompleted: Boolean = false,
    val project: String = "Inbox"
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val streak: Int = 0,
    val lastCheckIn: Long = 0L, // timestamp
    val historyDates: String = "" // comma-separated yyyy-MM-dd dates
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val activityName: String = "",
    val category: String = "Work"
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "",
    val avatarId: Int = 1, // index for avatar selection
    val isDarkTheme: Boolean = false,
    val isPremium: Boolean = true
)
