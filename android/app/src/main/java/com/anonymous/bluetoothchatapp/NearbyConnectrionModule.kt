package com.anonymous.bluetoothchatapp

import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.LifecycleEventListener
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class NearbyConnectionModule(private val reactContext: ReactApplicationContext) : 
    ReactContextBaseJavaModule(reactContext), LifecycleEventListener {

    private val connectionStrategy = Strategy.P2P_CLUSTER
    private val connectedEndpoints = mutableSetOf<String>()
    private var isAdvertising = false
    private var isDiscovering = false

    companion object {
        const val MODULE_NAME = "NearbyConnectionModule"
        const val ON_DEVICE_CONNECTED = "ON_DEVICE_CONNECTED"
        const val ON_MESSAGE_RECEIVED = "ON_MESSAGE_RECEIVED"
        const val SERVICE_UUID = "babcd153-53bf-4067-a559-8955afa63c2e"
        const val APP_IDENTIFIER = "BluetoothChatApp"
    }

    init {
        reactContext.addLifecycleEventListener(this)
    }

    override fun getName() = MODULE_NAME

    private fun sendEvent(eventName: String, params: WritableMap) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    // https://developers.google.com/nearby/connections/android/discover-devices
    @ReactMethod
    fun startAdvertising(serviceId: String, username: String, promise: Promise) {
        if (isAdvertising) {
            promise.reject("ADVERTISING_ERROR", "Advertising is already started")
            return
        }

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(connectionStrategy)
            .build()

        // Try to stop advertising if it's already started
        Nearby.getConnectionsClient(reactContext).stopAdvertising()

        Nearby.getConnectionsClient(reactContext)
            .startAdvertising(
                username,
                serviceId,
                connectionLifecycleCallback,
                advertisingOptions
            )
            .addOnSuccessListener {
                isAdvertising = true
                promise.resolve("Advertising started")
            }
            .addOnFailureListener { exception: Exception ->
                promise.reject("ADVERTISING_ERROR", exception.message ?: "Unknown error")
            }
    }

    // https://developers.google.com/nearby/connections/android/discover-devices
    @ReactMethod
    fun startDiscovery(serviceId: String, promise: Promise) {
        if (isDiscovering) {
            promise.reject("DISCOVERY_ERROR", "Discovery is already started")
            return
        }
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(connectionStrategy)
            .build()

        Nearby.getConnectionsClient(reactContext).stopDiscovery()

        Nearby.getConnectionsClient(reactContext)
            .startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                discoveryOptions
            )
            .addOnSuccessListener {
                isDiscovering = true
                promise.resolve("Discovery started")
            }
            .addOnFailureListener { exception: Exception ->
                promise.reject("DISCOVERY_ERROR", exception.message ?: "Unknown error")
            }
    }

    @ReactMethod
    fun sendMessage(endpointId: String, message: String, promise: Promise) {
        Nearby.getConnectionsClient(reactContext)
            .sendPayload(
                endpointId, 
                Payload.fromBytes(message.toByteArray())
            )
            .addOnSuccessListener {
                promise.resolve("Message sent")
            }
            .addOnFailureListener { exception ->
                promise.reject("SEND_MESSAGE_ERROR", exception.message ?: "Message send failed")
            }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Nearby.getConnectionsClient(reactContext)
                .acceptConnection(endpointId, payloadCallback)

            val params = Arguments.createMap().apply {
                putString("endpointId", endpointId)
                putString("deviceName", connectionInfo.endpointName)
            }
            sendEvent(ON_DEVICE_CONNECTED, params)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpoints.add(endpointId)
                    Log.d(MODULE_NAME, "Connected to $endpointId")
                }
                else -> {
                    Log.e(MODULE_NAME, "Failed to connect to endpoint: $endpointId")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            Log.d(MODULE_NAME, "Lost connection to: $endpointId")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Nearby.getConnectionsClient(reactContext)
                .requestConnection(
                    "CurrentUser", 
                    endpointId, 
                    connectionLifecycleCallback
                )
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(MODULE_NAME, "Lost connection to: $endpointId")
        }
    }

    // https://developers.google.com/nearby/connections/android/exchange-data
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val message = String(bytes)
                val params = Arguments.createMap().apply {
                    putString("endpointId", endpointId)
                    putString("message", message)
                }
                sendEvent(ON_MESSAGE_RECEIVED, params)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    // No idea if this code is necessary, but it got some warnings to go away.
    override fun onHostResume() {
        // Handle host resume
    }

    override fun onHostPause() {
        // Handle host pause
    }

    override fun onHostDestroy() {
        // Handle host destroy
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Set up any upstream listeners or background tasks as necessary
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Remove upstream listeners, stop unnecessary background tasks
    }
}