package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TimeFlowTab {
    TODAY, HABITS, FOCUS, ANALYTICS, PROFILE
}

class TimeFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TimeFlowRepository
    private val sharedPrefs = application.getSharedPreferences("TimeFlowPrefs", android.content.Context.MODE_PRIVATE)

    private val _isOnboardingCompleted = MutableStateFlow(sharedPrefs.getBoolean("onboarding_completed", false))
    val isOnboardingCompleted = _isOnboardingCompleted.asStateFlow()

    fun completeOnboarding() {
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
        _isOnboardingCompleted.value = true
    }

    // Base flows
    val tasks: StateFlow<List<Task>>
    val habits: StateFlow<List<Habit>>
    val sessions: StateFlow<List<FocusSession>>
    val profile: StateFlow<UserProfile?>

    // Screen State
    var _currentTab = MutableStateFlow(TimeFlowTab.TODAY)
    val currentTab = _currentTab.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn = _isUserLoggedIn.asStateFlow()

    private val _isPrivacyPolicyVisible = MutableStateFlow(false)
    val isPrivacyPolicyVisible = _isPrivacyPolicyVisible.asStateFlow()

    // Pomodoro Timer State
    private val _timerMinutesLeft = MutableStateFlow(25)
    val timerMinutesLeft = _timerMinutesLeft.asStateFlow()

    private val _timerSecondsLeft = MutableStateFlow(0)
    val timerSecondsLeft = _timerSecondsLeft.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _timerTotalDurationMinutes = MutableStateFlow(25)
    val timerTotalDurationMinutes = _timerTotalDurationMinutes.asStateFlow()

    private val _timerLabel = MutableStateFlow("General Focus")
    val timerLabel = _timerLabel.asStateFlow()

    private val _timerCategory = MutableStateFlow("Work")
    val timerCategory = _timerCategory.asStateFlow()

    private val _ambientSound = MutableStateFlow("None")
    val ambientSound = _ambientSound.asStateFlow()

    private var timerJob: Job? = null

    // AI Daily Planner Recommendation state
    private val _aiPlanText = MutableStateFlow<String?>(null)
    val aiPlanText = _aiPlanText.asStateFlow()

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating = _isAiGenerating.asStateFlow()

    // UI Message state
    private val _uiEventMessage = MutableSharedFlow<String>()
    val uiEventMessage = _uiEventMessage.asSharedFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TimeFlowRepository(database)

        tasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        habits = repository.allHabits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        sessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        profile = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Initialize state & prepopulate
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            // Set user authenticated by default if profile already exists (silent auto sign-in)
            val prof = repository.getUserProfileOneShot()
            if (prof != null) {
                _isUserLoggedIn.value = true
            }
        }
    }

    // Authentication Actions
    fun loginWithGoogle() {
        viewModelScope.launch {
            // Guarantee fresh profile on first login
            var prof = repository.getUserProfileOneShot()
            if (prof == null) {
                prof = UserProfile(
                    id = 1,
                    username = "",
                    avatarId = 1,
                    isDarkTheme = false,
                    isPremium = true
                )
                repository.insertUserProfile(prof)
            }
            _isUserLoggedIn.value = true
            val displayName = if (prof.username.isBlank()) "User" else prof.username
            _uiEventMessage.emit("Successfully signed in as $displayName (dpallab224@gmail.com)")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isUserLoggedIn.value = false
            _uiEventMessage.emit("Successfully signed out")
        }
    }

    // Toggle Tab / Screen Transitions
    fun selectTab(tab: TimeFlowTab) {
        _currentTab.value = tab
    }

    fun setPrivacyPolicyVisible(visible: Boolean) {
        _isPrivacyPolicyVisible.value = visible
    }

    // Profile updates
    fun updateProfile(newName: String, avatarId: Int) {
        viewModelScope.launch {
            val current = repository.getUserProfileOneShot() ?: UserProfile(id = 1)
            val updated = current.copy(username = newName.trim(), avatarId = avatarId)
            repository.insertUserProfile(updated)
            _uiEventMessage.emit("Profile updated successfully!")
        }
    }

    fun toggleThemeSetting(isDark: Boolean) {
        viewModelScope.launch {
            val current = repository.getUserProfileOneShot() ?: UserProfile(id = 1)
            repository.insertUserProfile(current.copy(isDarkTheme = isDark))
        }
    }

    fun togglePremiumStatus() {
        viewModelScope.launch {
            val current = repository.getUserProfileOneShot() ?: UserProfile(id = 1)
            val newStatus = !current.isPremium
            repository.insertUserProfile(current.copy(isPremium = newStatus))
            if (newStatus) {
                _uiEventMessage.emit("👑 Premium Activated! Features Unlocked!")
            } else {
                _uiEventMessage.emit("Back to Free Tier model")
            }
        }
    }

    // Task Actions
    fun addTask(title: String, dueDate: Long, priority: String, notes: String, project: String) {
        viewModelScope.launch {
            if (title.isBlank()) return@launch
            val task = Task(
                title = title,
                dueDate = dueDate,
                priority = priority,
                notes = notes,
                project = project
            )
            repository.insertTask(task)
            _uiEventMessage.emit("Task created: $title")
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            if (updated.isCompleted) {
                _uiEventMessage.emit("Task completed! 🎉")
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _uiEventMessage.emit("Task removed")
        }
    }

    // Habit Actions
    fun addHabit(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch

            val isPremium = true
            val currentHabitsCount = habits.value.size

            if (!isPremium && currentHabitsCount >= 3) {
                _uiEventMessage.emit("⚠️ Free limit reached (Max 3 habits). Upgrade to Premium for unlimited habits!")
                return@launch
            }

            val habit = Habit(name = name)
            repository.insertHabit(habit)
            _uiEventMessage.emit("Habit added: $name")
        }
    }

    fun checkInHabit(habit: Habit) {
        viewModelScope.launch {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val historyList = habit.historyDates.split(",").filter { it.isNotBlank() }.toMutableList()

            if (historyList.contains(todayDate)) {
                _uiEventMessage.emit("Habit already checked in for today!")
                return@launch
            }

            historyList.add(todayDate)
            val newHistory = historyList.joinToString(",")
            val newStreak = habit.streak + 1

            val updated = habit.copy(
                streak = newStreak,
                lastCheckIn = System.currentTimeMillis(),
                historyDates = newHistory
            )
            repository.updateHabit(updated)
            _uiEventMessage.emit("streak set to $newStreak days for ${habit.name}! Keep it up! 🔥")
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            _uiEventMessage.emit("Habit deleted")
        }
    }

    // Focus Session / Pomodoro Actions
    fun startTimerJob(customMinutes: Int? = null) {
        val duration = customMinutes ?: _timerTotalDurationMinutes.value
        _timerTotalDurationMinutes.value = duration
        _timerMinutesLeft.value = duration
        _timerSecondsLeft.value = 0
        _isTimerRunning.value = true

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                delay(1000L)
                val sec = _timerSecondsLeft.value
                val min = _timerMinutesLeft.value

                if (sec > 0) {
                    _timerSecondsLeft.value = sec - 1
                } else if (min > 0) {
                    _timerMinutesLeft.value = min - 1
                    _timerSecondsLeft.value = 59
                } else {
                    // Timer complete!
                    _isTimerRunning.value = false
                    saveFocusSession()
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerMinutesLeft.value = _timerTotalDurationMinutes.value
        _timerSecondsLeft.value = 0
    }

    fun setTimerConfig(minutes: Int, label: String, category: String) {
        _timerTotalDurationMinutes.value = minutes
        _timerMinutesLeft.value = minutes
        _timerSecondsLeft.value = 0
        _timerLabel.value = label
        _timerCategory.value = category
    }

    fun changeAmbientSound(sound: String) {
        _ambientSound.value = sound
    }

    private suspend fun saveFocusSession() {
        val duration = _timerTotalDurationMinutes.value
        val label = _timerLabel.value
        val category = _timerCategory.value

        val session = FocusSession(
            durationMinutes = duration,
            activityName = label.ifBlank { "Focus Session" },
            category = category
        )
        repository.insertSession(session)
        _uiEventMessage.emit("Focus session complete! Saved $duration mins of focus 🏆")
    }

    // AI Daily Planner Generative Call
    fun generateAiPlan() {
        viewModelScope.launch {
            _isAiGenerating.value = true
            _uiEventMessage.emit("Analyzing tasks and generating your daily focus plan...")
            
            // Collect list of tasks
            val activeTasks = tasks.value
            val plan = GeminiClient.getDailyPlan(activeTasks)
            
            _aiPlanText.value = plan
            _isAiGenerating.value = false
            _uiEventMessage.emit("AI daily focus plan loaded!")
        }
    }

    fun clearAiPlan() {
        _aiPlanText.value = null
    }

    // Google Calendar Sync Mock
    fun triggerGoogleCalendarSync() {
        viewModelScope.launch {
            _uiEventMessage.emit("🔄 Synchronizing with Google Calendar...")
            delay(1500L) // smooth simulated sync animation
            
            // Pull in 2-3 mock events from Google Calendar that are injected dynamically
            val now = System.currentTimeMillis()
            val events = listOf(
                Task(title = "📅 Sync: Project Sync with Google Team", dueDate = now + 14400000, priority = "Medium", notes = "Imported from Google Calendar", project = "Work"),
                Task(title = "📅 Sync: Dentist appointment 🦷", dueDate = now + 28800000, priority = "Low", notes = "Imported from Google Calendar", project = "Personal")
            )
            events.forEach { repository.insertTask(it) }
            _uiEventMessage.emit("Two-way calendar sync completed! 2 events imported.")
        }
    }
}
