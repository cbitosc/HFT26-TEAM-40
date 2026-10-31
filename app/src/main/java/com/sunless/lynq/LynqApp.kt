package com.sunless.lynq

import android.app.Application
import com.sunless.lynq.crypto.CryptoManager
import com.sunless.lynq.data.storage.LynqDatabase
import com.sunless.lynq.domain.model.Peer
import com.sunless.lynq.engine.MessageRouter
import com.sunless.lynq.location.LocationProvider
import com.sunless.lynq.network.NearbyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LynqApp : Application() {

    lateinit var database: LynqDatabase private set
    lateinit var cryptoManager: CryptoManager private set
    lateinit var nearbyManager: NearbyManager private set
    lateinit var router: MessageRouter private set
    lateinit var locationProvider: LocationProvider private set
    lateinit var aiPreferences: com.sunless.lynq.ai.AiPreferences private set
    lateinit var aiClient: com.sunless.lynq.ai.FeatherlessAiClient private set

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = LynqDatabase(this)
        cryptoManager = CryptoManager(this)
        aiPreferences = com.sunless.lynq.ai.AiPreferences(this)
        aiClient = com.sunless.lynq.ai.FeatherlessAiClient(aiPreferences)
        val myDeviceName = com.sunless.lynq.util.DeviceNameHelper.getDeviceName(this)
        nearbyManager = NearbyManager(
            context = this,
            localDeviceId = cryptoManager.deviceId,
            initialLocalDeviceName = myDeviceName,
        )
        router = MessageRouter(
            database = database,
            cryptoManager = cryptoManager,
            nearbyManager = nearbyManager,
            initialMyName = myDeviceName,
        )
        locationProvider = LocationProvider(this)

        // Clean any legacy demo or phantom unverified peers from database so live mesh only represents actual devices
        appScope.launch {
            database.removeDemoPeers()
            database.purgePhantomPeers()
        }
    }

    companion object {
        lateinit var instance: LynqApp private set
    }
}
