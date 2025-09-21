package io.heckel.ntfy.app

import android.app.Application
import io.heckel.ntfy.R
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.db.Subscription
import io.heckel.ntfy.db.User
import io.heckel.ntfy.firebase.FirebaseMessenger
import io.heckel.ntfy.util.Log
import io.heckel.ntfy.util.randomSubscriptionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Application : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val repository by lazy {
        val repository = Repository.getInstance(applicationContext)
        if (repository.getRecordLogs()) {
            Log.setRecord(true)
        }
        repository
    }

    override fun onCreate() {
        super.onCreate()
        initializeDefaults()
    }

    private fun initializeDefaults() {
        val baseUrl = getString(R.string.app_base_url)
        applicationScope.launch {
            val repo = repository
            ensureDefaultUser(repo, baseUrl)
            ensureDefaultSubscription(repo, baseUrl)
        }
    }

    private suspend fun ensureDefaultUser(repository: Repository, baseUrl: String) {
        val existingUser = repository.getUser(baseUrl)
        if (existingUser == null) {
            repository.addUser(User(baseUrl, DEFAULT_USERNAME, DEFAULT_PASSWORD))
        }
    }

    private suspend fun ensureDefaultSubscription(repository: Repository, baseUrl: String) {
        val existingSubscription = repository.getSubscription(baseUrl, DEFAULT_CHANNEL_TOPIC)
        if (existingSubscription == null) {
            val subscription = Subscription(
                id = randomSubscriptionId(),
                baseUrl = baseUrl,
                topic = DEFAULT_CHANNEL_TOPIC,
                instant = true,
                mutedUntil = 0,
                minPriority = Repository.MIN_PRIORITY_USE_GLOBAL,
                autoDelete = Repository.AUTO_DELETE_USE_GLOBAL,
                insistent = Repository.INSISTENT_MAX_PRIORITY_USE_GLOBAL,
                lastNotificationId = null,
                icon = null,
                upAppId = null,
                upConnectorToken = null,
                displayName = null,
                dedicatedChannels = false
            ).copy(lastActive = System.currentTimeMillis() / 1000)
            repository.addSubscription(subscription)
            FirebaseMessenger().subscribe(DEFAULT_CHANNEL_TOPIC)
        }
    }

    companion object {
        private const val DEFAULT_CHANNEL_TOPIC = "peringatan_gempa_darurat_xyz"
        private const val DEFAULT_USERNAME = "vito100"
        private const val DEFAULT_PASSWORD = "Archerc80new"
    }
}
