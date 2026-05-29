package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TimeFlowRepository(private val database: AppDatabase) {
    val taskDao = database.taskDao()
    val habitDao = database.habitDao()
    val focusSessionDao = database.focusSessionDao()
    val userProfileDao = database.userProfileDao()

    // Task flows and actions
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun deleteTaskById(id: Int) = taskDao.deleteTaskById(id)

    // Habit flows and actions
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)
    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)
    suspend fun deleteHabitById(id: Int) = habitDao.deleteHabitById(id)

    // FocusSession flows and actions
    val allSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessions()
    suspend fun insertSession(session: FocusSession) = focusSessionDao.insertSession(session)
    suspend fun clearSessions() = focusSessionDao.clearAllSessions()

    // UserProfile flow and actions
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()
    suspend fun getUserProfileOneShot(): UserProfile? = userProfileDao.getUserProfileOneShot()
    suspend fun insertUserProfile(profile: UserProfile) = userProfileDao.insertUserProfile(profile)

    // Helper to prepopulate database if completely empty
    suspend fun prepopulateIfEmpty() {
        // Prepopulate default profile if empty
        val existingProfile = userProfileDao.getUserProfileOneShot()
        if (existingProfile == null) {
            userProfileDao.insertUserProfile(
                UserProfile(
                    id = 1,
                    username = "",
                    avatarId = 1,
                    isDarkTheme = false,
                    isPremium = true
                )
            )
        }

        // Prepopulate a couple of habits if database is completely empty
        val habits = habitDao.getAllHabits().firstOrNull() ?: emptyList()
        if (habits.isEmpty()) {
            habitDao.insertHabit(Habit(name = "Morning Meditation", streak = 5, historyDates = ""))
            habitDao.insertHabit(Habit(name = "Read 10 Pages", streak = 12, historyDates = ""))
            habitDao.insertHabit(Habit(name = "Drink 8 Glass Water", streak = 3, historyDates = ""))
        }

        // Prepopulate tasks if empty
        val tasks = taskDao.getAllTasks().firstOrNull() ?: emptyList()
        if (tasks.isEmpty()) {
            val now = System.currentTimeMillis()
            taskDao.insertTask(Task(title = "Welcome to TimeFlow! 👋", dueDate = now, priority = "High", notes = "This is a tasks dashboard. Double-tap or toggle checkmark to complete tasks! Enjoy organization.", project = "Inbox"))
            taskDao.insertTask(Task(title = "Review PRD Document 📝", dueDate = now, priority = "Medium", notes = "Review TimeFlow Product Requirements Document and implement core features.", project = "Study"))
            taskDao.insertTask(Task(title = "Design clean Blue & White theme 🎨", dueDate = now, priority = "High", notes = "Setup Material 3 color schemes perfectly.", project = "Work"))
            taskDao.insertTask(Task(title = "Mock Sync with Google Calendar 🗓️", dueDate = now + 86400000, priority = "Low", notes = "Sync high level events for tomorrow.", project = "Personal"))
        }

        // Prepopulate a few sessions for reports
        val sessions = focusSessionDao.getAllSessions().firstOrNull() ?: emptyList()
        if (sessions.isEmpty()) {
            val hour = 3600000L
            val day = 86400000L
            focusSessionDao.insertSession(FocusSession(durationMinutes = 25, timestamp = System.currentTimeMillis() - day, activityName = "Review Code", category = "Work"))
            focusSessionDao.insertSession(FocusSession(durationMinutes = 25, timestamp = System.currentTimeMillis() - day + 2 * hour, activityName = "Meditation", category = "Personal"))
            focusSessionDao.insertSession(FocusSession(durationMinutes = 50, timestamp = System.currentTimeMillis() - 2 * day, activityName = "Study Android", category = "Study"))
            focusSessionDao.insertSession(FocusSession(durationMinutes = 25, timestamp = System.currentTimeMillis() - 3 * day, activityName = "UX Design Research", category = "Work"))
        }
    }
}
