package com.anonymous.bluetoothchatapp

import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class NearbyConnectionModule(private val reactContext: ReactApplicationContext) : 
    ReactContextBaseJavaModule(reactContext) {

    private val connectionStrategy = Strategy.P2P_STAR
    private val connectedEndpoints = mutableSetOf<String>()
    private var isAdvertising = false

    companion object {
        const val MODULE_NAME = "NearbyConnectionModule"
        const val ON_DEVICE_CONNECTED = "ON_DEVICE_CONNECTED"
        const val ON_MESSAGE_RECEIVED = "ON_MESSAGE_RECEIVED"
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
        // ! I have no idea why it gives the "you're already advertising" error
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
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(connectionStrategy)
            .build()

        Nearby.getConnectionsClient(reactApplicationContext).stopDiscovery()

        Nearby.getConnectionsClient(reactApplicationContext)
            .startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                discoveryOptions
            )
            .addOnSuccessListener {
                promise.resolve("Discovery started")
            }
            .addOnFailureListener { exception ->
                promise.reject("DISCOVERY_ERROR", exception.message ?: "Unknown error")
            }
    }

    @ReactMethod
    fun sendMessage(endpointId: String, message: String, promise: Promise) {
        Nearby.getConnectionsClient(reactApplicationContext)
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

    // https://developers.google.com/nearby/connections/android/manage-connections
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Nearby.getConnectionsClient(reactApplicationContext)
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

    // https://developers.google.com/nearby/connections/android/manage-connections
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Nearby.getConnectionsClient(reactApplicationContext)
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
}