package com.anonymous.bluetoothchatapp

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import java.util.ArrayList

import com.anonymous.bluetoothchatapp.NearbyConnectionModule

class NearbyConnectionPackage : ReactPackage {
  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
    val modules = ArrayList<NativeModule>()
    modules.add(NearbyConnectionModule(reactContext))
    return modules
  }

  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    return emptyList()
  }
}