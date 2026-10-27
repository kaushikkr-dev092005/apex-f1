package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ChannelMessageEntity
import com.example.data.database.FantasyTeamEntity
import com.example.data.database.NewsEntity
import com.example.data.mock.MockData
import com.example.data.model.AlertType
import com.example.data.model.Circuit
import com.example.data.model.Constructor
import com.example.data.model.Driver
import com.example.data.model.NotificationAlert
import com.example.data.model.TelemetryData
import com.example.data.repository.F1Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class F1ViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: F1Repository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = F1Repository(database)
    }

    // Static metadata
    val drivers: List<Driver> = repository.getDrivers()
    val constructors: List<Constructor> = repository.getConstructors()
    val circuits: List<Circuit> = repository.getCircuits()

    // Screen States
    private val _selectedCircuit = MutableStateFlow(circuits.first())
    val selectedCircuit: StateFlow<Circuit> = _selectedCircuit.asStateFlow()

    private val _news = MutableStateFlow<List<NewsEntity>>(emptyList())
    val news: StateFlow<List<NewsEntity>> = _news.asStateFlow()

    private val _fantasyTeam = MutableStateFlow<FantasyTeamEntity?>(null)
    val fantasyTeam: StateFlow<FantasyTeamEntity?> = _fantasyTeam.asStateFlow()

    // Temporary/Draft Fantasy selections
    var draftTeamName by mutableStateOf("My Apex Racing")
    var draftDriver1Id by mutableStateOf<String?>(null)
    var draftDriver2Id by mutableStateOf<String?>(null)
    var draftConstructorId by mutableStateOf<String?>(null)

    val draftBudgetUsed: Double
        get() {
            var cost = 0.0
            draftDriver1Id?.let { id -> cost += drivers.find { it.id == id }?.cost ?: 0.0 }
            draftDriver2Id?.let { id -> cost += drivers.find { it.id == id }?.cost ?: 0.0 }
            draftConstructorId?.let { id -> cost += constructors.find { it.id == id }?.cost ?: 0.0 }
            return cost
        }

    val draftBudgetRemaining: Double
        get() = 100.0 - draftBudgetUsed

    // Chat / Fan Channels
    private val _selectedChannel = MutableStateFlow("General Fan Voice")
    val selectedChannel: StateFlow<String> = _selectedChannel.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChannelMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChannelMessageEntity>> = _chatMessages.asStateFlow()

    // Telemetry Driver selection
    private val _selectedTelemetryDriver = MutableStateFlow(drivers.first())
    val selectedTelemetryDriver: StateFlow<Driver> = _selectedTelemetryDriver.asStateFlow()

    private val _telemetryState = MutableStateFlow<TelemetryData?>(null)
    val telemetryState: StateFlow<TelemetryData?> = _telemetryState.asStateFlow()

    // Push Notifications Feed
    private val _activeNotification = MutableStateFlow<NotificationAlert?>(null)
    val activeNotification: StateFlow<NotificationAlert?> = _activeNotification.asStateFlow()

    private val _notificationHistory = MutableStateFlow<List<NotificationAlert>>(emptyList())
    val notificationHistory: StateFlow<List<NotificationAlert>> = _notificationHistory.asStateFlow()

    // Social Sharing action helper
    private val _socialShareEvent = MutableSharedFlow<String>()
    val socialShareEvent: SharedFlow<String> = _socialShareEvent.asSharedFlow()

    // Fan Voice Connection State
    private val _voiceConnected = MutableStateFlow(false)
    val voiceConnected: StateFlow<Boolean> = _voiceConnected.asStateFlow()

    private val _activeVoiceSpeakers = MutableStateFlow<List<String>>(emptyList())
    val activeVoiceSpeakers: StateFlow<List<String>> = _activeVoiceSpeakers.asStateFlow()

    // Settings / Theme Preference
    private val _darkThemeMode = MutableStateFlow(true) // Start dark by default as per F1 styling guidelines
    val darkThemeMode: StateFlow<Boolean> = _darkThemeMode.asStateFlow()

    private var telemetryJob: Job? = null
    private var notificationJob: Job? = null
    private var activeSpeakerJob: Job? = null

    init {
        // Collect news from Room
        viewModelScope.launch {
            repository.getNews().collect {
                _news.value = it
            }
        }

        // Collect fantasy team from Room
        viewModelScope.launch {
            repository.getFantasyTeam().collect { team ->
                _fantasyTeam.value = team
                if (team != null) {
                    draftTeamName = team.teamName
                    draftDriver1Id = team.driver1Id
                    draftDriver2Id = team.driver2Id
                    draftConstructorId = team.constructorId
                }
            }
        }

        // Observe chat messages
        viewModelScope.launch {
            _selectedChannel.collectLatest { channel ->
                repository.getChannelMessages(channel).collect { list ->
                    _chatMessages.value = list
                }
            }
        }

        // Start real-time telemetry stream for the default driver
        startTelemetryStream()

        // Start the background race occurrences (push notifications simulation)
        startPushNotificationSimulation()
    }

    fun selectCircuit(circuit: Circuit) {
        _selectedCircuit.value = circuit
    }

    fun selectChannel(channel: String) {
        _selectedChannel.value = channel
    }

    fun setDarkThemeMode(enabled: Boolean) {
        _darkThemeMode.value = enabled
    }

    fun toggleNewsBookmark(newsId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.bookmarkNews(newsId, !currentStatus)
        }
    }

    fun likeNews(newsId: String) {
        viewModelScope.launch {
            repository.likeNews(newsId)
        }
    }

    // Fan Chats
    fun postChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendChannelMessage(
                channelId = _selectedChannel.value,
                username = "ApexFan_" + Random.nextInt(100, 999),
                messageText = text,
                colorHex = listOf("#E10600", "#00D2BE", "#FFEC00", "#FF8000", "#FF80B0").random()
            )
        }
    }

    // Fantasy Operations
    fun toggleDraftDriver(driverId: String) {
        if (draftDriver1Id == driverId) {
            draftDriver1Id = null
        } else if (draftDriver2Id == driverId) {
            draftDriver2Id = null
        } else {
            // Assign to empty slot or replace the first slot
            if (draftDriver1Id == null) {
                draftDriver1Id = driverId
            } else if (draftDriver2Id == null) {
                draftDriver2Id = driverId
            } else {
                draftDriver1Id = driverId // Replace
            }
        }
    }

    fun selectDraftConstructor(constructorId: String) {
        draftConstructorId = if (draftConstructorId == constructorId) null else constructorId
    }

    fun saveFantasyTeam() {
        viewModelScope.launch {
            val d1 = draftDriver1Id ?: return@launch
            val d2 = draftDriver2Id ?: return@launch
            val cId = draftConstructorId ?: return@launch
            
            repository.saveFantasyTeam(
                teamName = draftTeamName,
                driver1Id = d1,
                driver2Id = d2,
                constructorId = cId,
                budgetRemaining = draftBudgetRemaining
            )
            triggerInAppPushNotification(
                title = "F1 Fantasy Active!",
                message = "Your team '$draftTeamName' has been locked into the Global Fan League. Good luck!",
                type = AlertType.LAPUPDATE
            )
        }
    }

    fun clearFantasyTeam() {
        viewModelScope.launch {
            repository.clearFantasyTeam()
            draftDriver1Id = null
            draftDriver2Id = null
            draftConstructorId = null
        }
    }

    // Telemetry Driver Control
    fun setTelemetryDriver(driver: Driver) {
        _selectedTelemetryDriver.value = driver
        startTelemetryStream()
    }

    private fun startTelemetryStream() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            repository.getTelemetryStream(_selectedTelemetryDriver.value.id).collect { data ->
                _telemetryState.value = data
            }
        }
    }

    // Toggle Voice Channel Connection
    fun toggleVoiceChannel() {
        val isConnecting = !_voiceConnected.value
        _voiceConnected.value = isConnecting

        if (isConnecting) {
            // Generate some random speaker simulation
            activeSpeakerJob = viewModelScope.launch {
                val potentialSpeakers = listOf("Paddock_Guru", "LeclercFansOnly", "SuperMax33", "Hamiltonian44", "BoxBoxBox")
                while (true) {
                    delay(Random.nextLong(2000, 5000))
                    val speakingCount = Random.nextInt(0, 3)
                    _activeVoiceSpeakers.value = potentialSpeakers.shuffled().take(speakingCount)
                }
            }
            triggerInAppPushNotification(
                title = "Voice Connected",
                message = "You have joined the '${_selectedChannel.value}' audio space. Plug in and chat live!",
                type = AlertType.GREENFLAG
            )
        } else {
            activeSpeakerJob?.cancel()
            _activeVoiceSpeakers.value = emptyList()
        }
    }

    // Social Sharing Helper
    fun shareRaceHighlight(highlightText: String) {
        viewModelScope.launch {
            _socialShareEvent.emit(highlightText)
        }
    }

    // Push Notifications Simulation
    private fun startPushNotificationSimulation() {
        notificationJob = viewModelScope.launch {
            delay(5000) // initial delay
            val mockNotifications = listOf(
                NotificationAlert(
                    id = "alert_1",
                    title = "🔴 RED FLAG Sector 2",
                    message = "Williams of Logan Sargeant hit the barriers at turn 4. Session suspended.",
                    type = AlertType.REDFLAG
                ),
                NotificationAlert(
                    id = "alert_2",
                    title = "⏱️ Purple Sector 1",
                    message = "Max Verstappen clocks a blistering 28.114s, the fastest Sector 1 of the race!",
                    type = AlertType.LAPUPDATE
                ),
                NotificationAlert(
                    id = "alert_3",
                    title = "⚡ DRS Enabled",
                    message = "Race Control has enabled the Drag Reduction System as track dry line widens.",
                    type = AlertType.DRS
                ),
                NotificationAlert(
                    id = "alert_4",
                    title = "🏆 Critical Overtake",
                    message = "Lando Norris performs a daring dive down the inside at Vale to steal P1 from Verstappen!",
                    type = AlertType.OVERTAKE
                ),
                NotificationAlert(
                    id = "alert_5",
                    title = "🟡 Yellow Flag Sector 3",
                    message = "Aston Martin of Fernando Alonso spun at turn 16. Sector 3 yellow flags active.",
                    type = AlertType.YELLOWFLAG
                ),
                NotificationAlert(
                    id = "alert_6",
                    title = "🟢 Track Clear",
                    message = "Yellow flags cleared in Sector 3. Drivers green to go full speed.",
                    type = AlertType.GREENFLAG
                ),
                NotificationAlert(
                    id = "alert_7",
                    title = "⏱️ FASTEST LAP",
                    message = "Charles Leclerc sets the overall fastest lap: 1:16.890 (+1 Championship Point).",
                    type = AlertType.LAPUPDATE
                )
            )

            var index = 0
            while (true) {
                val currentAlert = mockNotifications[index % mockNotifications.size]
                val freshAlert = currentAlert.copy(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis()
                )
                triggerInAppPushNotification(freshAlert.title, freshAlert.message, freshAlert.type)
                
                index++
                // Post occasional bot messages in Chat as events unfold to make the chat feel real-time and hyperactive!
                val sampleBotMessages = listOf(
                    "OMG DID YOU SEE THAT?? Lando is flying!!!",
                    "Max's sector 1 was insane, Red Bull speed is back",
                    "Red flag is bad for Sainz's strategy, he just pitted",
                    "DRS enabled, let the slipstream battles begin!",
                    "Who's watching the telemetry? Verstappen's throttle trace is wild through Becketts",
                    "What a move by Leclerc! Clean racing",
                    "Is anybody in the fantasy top 100? My drivers are falling behind!"
                )
                val botUser = listOf("F1_Stats_Bot", "Hamilton_Crew", "Tifosi_Steve", "NorrisFanatic", "Apex_Pundit").random()
                repository.sendChannelMessage(
                    channelId = _selectedChannel.value,
                    username = botUser,
                    messageText = sampleBotMessages.random(),
                    colorHex = "#27F4D2"
                )

                delay(18000) // New notification popup every 18 seconds!
            }
        }
    }

    private fun triggerInAppPushNotification(title: String, message: String, type: AlertType) {
        val alert = NotificationAlert(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = type
        )
        viewModelScope.launch {
            _activeNotification.value = alert
            _notificationHistory.value = listOf(alert) + _notificationHistory.value.take(25) // Keep last 25
            
            // Notification toast remains visible for 4.5 seconds
            delay(4500)
            if (_activeNotification.value?.id == alert.id) {
                _activeNotification.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryJob?.cancel()
        notificationJob?.cancel()
        activeSpeakerJob?.cancel()
    }
}
