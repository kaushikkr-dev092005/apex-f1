package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.model.*
import com.example.data.repository.F1Repository
import com.example.data.race.*
import com.example.data.fantasy.FantasyEngine
import com.example.data.notifications.*
import com.example.data.community.UnconfiguredVoiceService
import com.example.platform.AndroidNotificationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.data.explore.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class F1ViewModel(application: Application) : AndroidViewModel(application) {
    private val publicData = PublicF1Repository(AppDatabase.getDatabase(application).dataCacheDao())
    private val _experience = MutableStateFlow<String?>(null)
    val experience = _experience.asStateFlow()
    fun openExperience(route: String?) { _experience.value = route }
    var circuitCatalog by mutableStateOf<List<Circuit2027>>(emptyList())
        private set
    var catalogError by mutableStateOf<String?>(null)
        private set
    var verifiedStandings by mutableStateOf(LoadState<StandingSnapshot>())
        private set
    var careerResults by mutableStateOf(LoadState<List<SeasonResult>>())
        private set
    var careerKey by mutableStateOf("")
        private set
    var archiveDriver by mutableStateOf<StandingRow?>(null)
    var strategyPlan by mutableStateOf(StrategyPlan())
        private set
    fun updateStrategy(plan: StrategyPlan) { strategyPlan = plan }
    var rivalryResults by mutableStateOf(LoadState<RivalrySnapshot>())
        private set
    var rivalryKey by mutableStateOf("")
        private set
    var replayClip by mutableStateOf(PublicF1Repository.demoReplay())
        private set
    var replayLoading by mutableStateOf(false)
        private set
    var replayError by mutableStateOf<String?>(null)
        private set
    var replaySeekMs by mutableStateOf(0L)
        private set
    var replayRequestId by mutableIntStateOf(0)
        private set
    private var careerJob: Job? = null
    private var rivalryJob: Job? = null
    private val prefs = application.getSharedPreferences("apex_preferences", 0)
    private val repository = F1Repository(AppDatabase.getDatabase(application))
    val drivers = repository.getDrivers()
    val constructors = repository.getConstructors()
    val circuits = repository.getCircuits()
    private val provider = DemoRaceProvider()
    val race = RaceStore(provider, drivers.map { it.id })
    val voice = UnconfiguredVoiceService()
    private val notifier = AndroidNotificationService(application)
    private val notificationPolicy = NotificationPolicy()
    private val feedbackChannel = Channel<String>(Channel.BUFFERED)
    val feedback = feedbackChannel.receiveAsFlow()
    private val _appearance = MutableStateFlow(prefs.getString("appearance", "dark") ?: "dark")
    val appearance = _appearance.asStateFlow()
    var aiEndpoint by mutableStateOf(prefs.getString("ai_gateway", "").orEmpty())
        private set
    var aiToken by mutableStateOf("")
        private set
    var aiInterpretation by mutableStateOf<AiInterpretation?>(null)
        private set
    var aiError by mutableStateOf<String?>(null)
        private set
    var aiLoading by mutableStateOf(false)
        private set
    private var aiJob: Job? = null
    private var aiSequence = 0
    fun configureAi(endpoint: String, token: String) {
        if(endpoint.isNotBlank()&&!GroundedAiClient.validBaseUrl(endpoint)){message("Use an HTTPS gateway URL without credentials or query parameters.");return}
        if(token.trim().any {it.code !in 33..126}||token.trim().length>512){message("Gateway token must contain only printable characters without spaces.");return}
        aiEndpoint=endpoint.trim().trimEnd('/');aiToken=token.trim()
        prefs.edit().putString("ai_gateway",aiEndpoint).apply()
        clearAi();message(if(aiEndpoint.isEmpty())"AI gateway disabled."else"Gateway configuration saved. Access token stays in memory only.")
    }
    fun clearAi() {aiSequence++;aiJob?.cancel();aiInterpretation=null;aiError=null;aiLoading=false}
    fun askAi(answer: PitWallAnswer) {
        clearAi()
        if(aiEndpoint.isBlank()||aiToken.isBlank()){aiError="Configure your HTTPS gateway and an access token first.";return}
        val sequence=aiSequence;aiLoading=true
        aiJob=viewModelScope.launch {
            try {val result=GroundedAiClient.ask(aiEndpoint,aiToken,answer);if(sequence==aiSequence)aiInterpretation=result}
            catch(cancelled:CancellationException){throw cancelled}
            catch(failure:Exception){if(sequence==aiSequence)aiError=failure.message?:"AI service unavailable"}
            finally {if(sequence==aiSequence)aiLoading=false}
        }
    }
    private val _selectedDriver = MutableStateFlow("leclerc")
    val selectedDriver = _selectedDriver.asStateFlow()
    private val _deepLink = MutableStateFlow<String?>(null)
    val deepLink = _deepLink.asStateFlow()
    private val _news = MutableStateFlow<List<NewsEntity>>(emptyList())
    val news = _news.asStateFlow()
    private val _newsError = MutableStateFlow(false)
    val newsError = _newsError.asStateFlow()
    private var newsJob: Job? = null
    private val _team = MutableStateFlow<FantasyTeamEntity?>(null)
    val fantasyTeam = _team.asStateFlow()
    var draftTeamName by mutableStateOf("Scuderia APEX")
    var draftDrivers by mutableStateOf<List<String>>(emptyList())
    var draftConstructor by mutableStateOf<String?>(null)
    var captain by mutableStateOf(prefs.getString("captain", "") ?: "")
        private set
    var draftCaptain by mutableStateOf(captain)
        private set
    var savingTeam by mutableStateOf(false)
        private set
    val draftBudgetUsed get() = draftDrivers.sumOf { id -> drivers.firstOrNull { it.id == id }?.cost ?: 0.0 } + (constructors.firstOrNull { it.id == draftConstructor }?.cost ?: 0.0)
    val draftBudgetRemaining get() = 100.0 - draftBudgetUsed
    private val _channel = MutableStateFlow("Race room")
    val selectedChannel = _channel.asStateFlow()
    private val _messages = MutableStateFlow<List<ChannelMessageEntity>>(emptyList())
    val chatMessages = _messages.asStateFlow()
    private val _readIds = MutableStateFlow(prefs.getStringSet("read_articles", emptySet())?.toSet() ?: emptySet())
    val readIds = _readIds.asStateFlow()
    private val _alerts = MutableStateFlow(loadAlerts())
    val alerts = _alerts.asStateFlow()
    var previewRoom by mutableStateOf<String?>(null)
        private set
    var leagueCode by mutableStateOf(prefs.getString("league_code", "") ?: "")
        private set
    var leagueName by mutableStateOf(prefs.getString("league_name", "") ?: "")
        private set
    var reducedMotion by mutableStateOf(prefs.getBoolean("reduced_motion", false))
        private set
    var largeText by mutableStateOf(prefs.getBoolean("large_text", false))
        private set
    var localNotifications by mutableStateOf(prefs.getBoolean("local_notifications", false))
        private set
    var blockedUsers by mutableStateOf(prefs.getStringSet("blocked_users", emptySet())?.toSet() ?: emptySet())
        private set
    var transferHistory by mutableStateOf(prefs.getString("transfer_history", "") ?: "")
        private set

    init {
        loadCircuitCatalog()
        refreshNews()
        viewModelScope.launch {
            repository.getFantasyTeam().catch { message("Could not load your team. Try again.") }.collect { team ->
                _team.value = team
                if (team != null) {
                    draftTeamName = team.teamName
                    draftDrivers = listOf(team.driver1Id, team.driver2Id)
                    draftConstructor = team.constructorId
                    if (captain !in draftDrivers) persistCaptain(team.driver1Id)
                }
            }
        }
        viewModelScope.launch {
            _channel.collectLatest { channel ->
                repository.getChannelMessages(channel).catch { message("Local chat is unavailable.") }.collect { _messages.value = it }
            }
        }
        viewModelScope.launch {
            race.events.collect { events ->
                events.firstOrNull()?.let { event ->
                    val now = System.currentTimeMillis()
                    if (localNotifications && notificationPolicy.shouldSend(event, _alerts.value, now, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))) notifier.notify(event)
                }
            }
        }
    }

    fun loadCircuitCatalog() {
        catalogError = null
        viewModelScope.launch {
            try {
                circuitCatalog = withContext(Dispatchers.IO) {
                    getApplication<Application>().assets.open("circuits-2027.json").bufferedReader().use { CircuitCatalog.parse(it.readText()) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { catalogError = "Circuit collection could not be loaded. Please retry." }
        }
    }
    fun loadVerifiedStandings(force: Boolean = false) {
        if (verifiedStandings.loading) return
        verifiedStandings = verifiedStandings.copy(loading = true, error = null)
        viewModelScope.launch {
            try { verifiedStandings = LoadState(snapshot = publicData.standings(force)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { verifiedStandings = verifiedStandings.copy(loading = false, error = failure.message ?: "Standings unavailable") }
        }
    }
    fun loadCareer(id: String, season: Int, force: Boolean = false) {
        val key = "$id/$season"
        if (!force && key == careerKey && (careerResults.loading || careerResults.snapshot != null)) return
        careerJob?.cancel()
        careerResults = if (key == careerKey) careerResults.copy(loading = true, error = null) else LoadState(loading = true)
        careerKey = key
        careerJob = viewModelScope.launch {
            try { careerResults = LoadState(snapshot = publicData.results(id, season, force)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { careerResults = careerResults.copy(loading = false, error = failure.message ?: "Career results unavailable") }
        }
    }
    fun loadRivalry(left: String, right: String, season: Int) {
        val key = "$left/$right/$season"
        if (key == rivalryKey && (rivalryResults.loading || rivalryResults.snapshot != null)) return
        rivalryJob?.cancel(); rivalryKey = key; rivalryResults = LoadState(loading = true)
        rivalryJob = viewModelScope.launch {
            try {
                val a = publicData.results(left, season); delay(400)
                val b = publicData.results(right, season); delay(400)
                val qa = publicData.qualifying(left, season); delay(400)
                val qb = publicData.qualifying(right, season)
                val differences = qa.data.mapNotNull { q -> qb.data.find { it.round == q.round }?.q1Ms?.let { other -> q.q1Ms?.let { it - other } } }.sorted()
                val median = if (differences.isEmpty()) null else if (differences.size % 2 == 1) differences[differences.size / 2].toDouble() else (differences[differences.size/2-1]+differences[differences.size/2])/2.0
                rivalryResults = LoadState(snapshot = DataSnapshot(RivalrySnapshot(a.data,b.data,median,differences.size),"https://api.jolpi.ca/ergast/f1/$season/",minOf(a.fetchedAt,b.fetchedAt,qa.fetchedAt,qb.fetchedAt),a.cached||b.cached||qa.cached||qb.cached,a.stale||b.stale||qa.stale||qb.stale))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { rivalryResults = LoadState(error = failure.message ?: "Comparison unavailable") }
        }
    }
    fun loadArchive() {
        if (replayLoading) return
        replayLoading = true; replayError = null
        viewModelScope.launch {
            try { replayClip = publicData.monacoArchive() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { replayError = failure.message ?: "Archive is unavailable. The selected demonstration remains labeled." }
            finally { replayLoading = false }
        }
    }
    fun useDemoReplay() { replayClip = PublicF1Repository.demoReplay(); replayError = null;replaySeekMs=0L;replayRequestId++ }
    fun requestReplayAt(timeMs:Long,recorded:Boolean) {
        replaySeekMs=timeMs.coerceIn(0L,180_000L);replayRequestId++
        if(recorded)loadArchive()else {replayClip=PublicF1Repository.demoReplay();replayError=null}
    }
    fun predictionVote(question: String): String? = prefs.getString("prediction_$question", null)
    fun savePrediction(question: String, choice: String) { prefs.edit().putString("prediction_$question", choice).apply(); message("Prediction saved on this device.") }

    fun message(text: String) { feedbackChannel.trySend(text) }
    fun setForeground(active: Boolean) { if (active) race.start(viewModelScope) else race.stop() }
    fun reconnect() { provider.disconnected = false; race.reconnect(viewModelScope) }
    fun interruptFeed() { provider.disconnected = true; message("Demo connection interrupted. Restore it with Reconnect.") }
    fun selectDriver(id: String) { if (drivers.any { it.id == id }) _selectedDriver.value = id }
    fun route(link: String) { _deepLink.value = link }
    fun consumeDeepLink() { _deepLink.value = null }
    fun setAppearance(value: String) {
        if (value !in listOf("dark", "light", "system")) return
        _appearance.value = value
        prefs.edit().putString("appearance", value).apply()
    }
    fun updateReducedMotion(value: Boolean) { reducedMotion = value; prefs.edit().putBoolean("reduced_motion", value).apply() }
    fun updateLargeText(value: Boolean) { largeText = value; prefs.edit().putBoolean("large_text", value).apply() }
    fun followDriver(id: String) { saveAlerts(_alerts.value.copy(followedDrivers = _alerts.value.followedDrivers.let { if (id in it) it - id else it + id })) }
    fun followConstructor(id: String) { saveAlerts(_alerts.value.copy(followedConstructors = _alerts.value.followedConstructors.let { if (id in it) it - id else it + id })) }
    fun toggleCategory(id: String) { saveAlerts(_alerts.value.copy(enabled = _alerts.value.enabled.let { if (id in it) it - id else it + id })) }
    fun updateAlertPreferences(p: AlertPreferences) = saveAlerts(p)
    fun updateLocalNotifications(value: Boolean) { localNotifications = value; prefs.edit().putBoolean("local_notifications", value).apply() }
    fun testNotification(): Boolean {
        val e = RaceEvent("test-" + System.currentTimeMillis(), EventKind.RedFlag, "Demo red flag", "Local test alert. Tap to open race control.", 24, System.currentTimeMillis())
        return notifier.notify(e).also { if (!it) message("Notifications are disabled. Allow them in Android settings.") }
    }

    fun refreshNews() {
        newsJob?.cancel()
        _newsError.value = false
        newsJob = viewModelScope.launch {
            repository.getNews().catch { android.util.Log.w("APEX", "Local journal load failed", it); _newsError.value = true }.collect { _news.value = it }
        }
    }
    fun toggleNewsBookmark(article: NewsEntity) {
        viewModelScope.launch {
            runCatching { repository.bookmarkNews(article.id, !article.isBookmarked) }.onFailure { message("Could not save article. Please retry.") }
        }
    }
    fun markRead(id: String) { _readIds.value = _readIds.value + id; prefs.edit().putStringSet("read_articles", _readIds.value).apply() }

    fun toggleDraftDriver(id: String) {
        if (drivers.none { it.id == id }) return
        draftDrivers = if (id in draftDrivers) draftDrivers - id else if (draftDrivers.size < 2) draftDrivers + id else {
            message("Remove a driver before adding another."); return
        }
        if (draftCaptain !in draftDrivers) draftCaptain = draftDrivers.firstOrNull().orEmpty()
    }
    fun selectDraftConstructor(id: String) { draftConstructor = if (draftConstructor == id) null else id }
    private fun persistCaptain(id: String) { captain = id; draftCaptain = id; prefs.edit().putString("captain", id).apply() }
    fun chooseDraftCaptain(id: String) { if (id in draftDrivers) draftCaptain = id }
    fun restoreDraft() { _team.value?.let { draftTeamName = it.teamName; draftDrivers = listOf(it.driver1Id, it.driver2Id); draftConstructor = it.constructorId; draftCaptain = captain } }
    fun saveFantasyTeam() {
        val error = FantasyEngine.validate(draftTeamName, draftDrivers, draftConstructor, drivers, constructors)
        if (error != null) { message(error); return }
        if (savingTeam) return
        val name = draftTeamName.trim()
        val ids = draftDrivers.toList()
        val constructor = draftConstructor!!
        val budget = draftBudgetRemaining
        val selectedCaptain = draftCaptain.takeIf { it in ids } ?: ids.first()
        savingTeam = true
        viewModelScope.launch {
            try {
                val points = ids.sumOf { FantasyEngine.score(it, it == selectedCaptain) } + FantasyEngine.constructorScore(constructor, drivers)
                repository.saveFantasyTeam(name, ids[0], ids[1], constructor, budget, points)
                persistCaptain(selectedCaptain)
                val change = "Exhibition roster saved · " + ids.joinToString(" / ") { id -> drivers.first { it.id == id }.code }
                transferHistory = (listOf(change) + transferHistory.lines().filter { it.isNotBlank() }).take(10).joinToString("\n")
                prefs.edit().putString("transfer_history", transferHistory).apply()
                message("Team saved on this device. Exhibition scoring is ready.")
            } catch (_: Exception) { message("Could not save your team. Please retry.") }
            finally { savingTeam = false }
        }
    }
    fun createLeague(name: String) {
        if (name.trim().length !in 2..30) { message("League name must be 2–30 characters."); return }
        leagueName = name.trim()
        leagueCode = "APX-" + kotlin.math.abs(name.hashCode().toLong()).toString(36).uppercase().padStart(6, '0').take(6)
        prefs.edit().putString("league_code", leagueCode).putString("league_name", leagueName).apply()
        message("Local demo league created. Share its exhibition invite.")
    }
    fun joinLeague(code: String) {
        val normalized = code.trim().uppercase()
        if (!normalized.matches(Regex("APX-[A-Z0-9]{6}"))) { message("Use an invite like APX-MONACO."); return }
        leagueCode = normalized
        leagueName = "Paddock friends"
        prefs.edit().putString("league_code", leagueCode).putString("league_name", leagueName).apply()
        message("Joined a local league preview. Online leagues require a backend.")
    }

    fun selectChannel(value: String) { _channel.value = value }
    fun postChatMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (clean.length > 500) { message("Keep messages under 500 characters."); return }
        viewModelScope.launch {
            runCatching { repository.sendChannelMessage(_channel.value, "You", clean, "#E49AA8") }
                .onFailure { message("Message could not be saved. Please retry.") }
        }
    }
    fun previewVoice(room: String) { previewRoom = room }
    fun leaveVoice() { previewRoom = null }
    fun blockUser(user: String) { blockedUsers = blockedUsers + user; prefs.edit().putStringSet("blocked_users", blockedUsers).apply(); message("User blocked on this device.") }
    fun reportMessage(id: String) { prefs.edit().putBoolean("report_$id", true).apply(); message("Report saved locally. Moderation delivery needs a community server.") }

    private fun loadAlerts() = AlertPreferences(
        enabled = prefs.getStringSet("alert_categories", setOf("race", "red", "safety", "fastest", "finish"))?.toSet() ?: emptySet(),
        followedDrivers = prefs.getStringSet("followed_drivers", setOf("leclerc"))?.toSet() ?: emptySet(),
        followedConstructors = prefs.getStringSet("followed_constructors", setOf("ferrari"))?.toSet() ?: emptySet(),
        quietEnabled = prefs.getBoolean("quiet_enabled", false),
        quietStart = prefs.getInt("quiet_start", 23), quietEnd = prefs.getInt("quiet_end", 7),
        digest = prefs.getBoolean("digest", false), throttle = prefs.getBoolean("throttle", true)
    )
    private fun saveAlerts(p: AlertPreferences) {
        _alerts.value = p
        prefs.edit().putStringSet("alert_categories", p.enabled).putStringSet("followed_drivers", p.followedDrivers)
            .putStringSet("followed_constructors", p.followedConstructors).putBoolean("quiet_enabled", p.quietEnabled)
            .putInt("quiet_start", p.quietStart).putInt("quiet_end", p.quietEnd)
            .putBoolean("digest", p.digest).putBoolean("throttle", p.throttle).apply()
    }
    override fun onCleared() { race.stop(); super.onCleared() }
}
