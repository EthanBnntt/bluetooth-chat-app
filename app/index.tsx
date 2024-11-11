import React, { useState, useEffect, useRef } from 'react';
import { Buffer } from 'buffer';
import { View, Text, StyleSheet, TextInput, Button, ScrollView, Platform, PermissionsAndroid, Alert } from 'react-native';

import { NativeModules, NativeEventEmitter } from 'react-native';
const { NearbyConnectionModule } = NativeModules;
const eventEmitter = new NativeEventEmitter(NearbyConnectionModule);

const SERVICE_UUID = "babcd153-53bf-4067-a559-8955afa63c2e";
const APP_IDENTIFIER = "BluetoothChatApp";

export default function ChatPage() {
  const [messages, setMessages] = useState<string[]>([]);
  const [input, setInput] = useState<string>('');
  const scrollViewRef = useRef<ScrollView>(null);
  const [connectedDevices, setConnectedDevices] = useState<string[]>([]);
  const [isPermissionGranted, setIsPermissionGranted] = useState(false);
  const [isAdvertising, setIsAdvertising] = useState(false);

  // Requests Bluetooth and Location permissions for Nearby Connections
  const requestNearbyPermissions = async () => {
    if (Platform.OS !== 'android') {
      setIsPermissionGranted(true);
      return true;
    }

    try {
      // Permissions for Android 12 and above
      if (Platform.Version >= 31) {
        const permissions = [
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          PermissionsAndroid.PERMISSIONS.NEARBY_WIFI_DEVICES,
        ];

        const grantedPermissions = await Promise.all(
          permissions.map(permission => 
            PermissionsAndroid.request(permission)
          )
        );

        const allGranted = grantedPermissions.every(
          result => result === PermissionsAndroid.RESULTS.GRANTED
        );

        if (allGranted) {
          setIsPermissionGranted(true);
          return true;
        } else {
          Alert.alert(
            "Permissions Required", 
            "Please grant all Bluetooth and location permissions to use Nearby Connections."
          );
          return false;
        }
      } else {
        const locationPermission = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
        );

        if (locationPermission === PermissionsAndroid.RESULTS.GRANTED) {
          setIsPermissionGranted(true);
          return true;
        } else {
          Alert.alert(
            "Location Permission Required", 
            "Location permission is needed for Bluetooth discovery."
          );
          return false;
        }
      }
    } catch (error) {
      console.error("Permission request error:", error);
      Alert.alert("Error", "Failed to request permissions");
      return false;
    }
  };

  // Initialize Nearby Connections
  useEffect(() => {
    const initializeConnections = async () => {
      const permissionsGranted = await requestNearbyPermissions();
      if (permissionsGranted) {
        try {
          if (!isAdvertising) {         
            // Start advertising
            await NearbyConnectionModule.startAdvertising(
              SERVICE_UUID, 
              APP_IDENTIFIER
            );
          }    

          // Start discovery
          await NearbyConnectionModule.startDiscovery(
            SERVICE_UUID
          );

          console.log("Advertising and Discovery started");
        } catch (error) {
          console.error("Nearby Connections initialization error:", error);
        }
      }
    };
  
    initializeConnections();
  }, []);

  useEffect(() => {    
    // Handled when a device is connected
    const deviceConnectedListener = eventEmitter.addListener(
      'ON_DEVICE_CONNECTED', 
      (event) => {
        // ? Place all the code for handling the connected device here
        console.log("Device connected:", event.endpointId);
        setConnectedDevices(prev => [...prev, event.endpointId]);
      }
    );

    const messageReceivedListener = eventEmitter.addListener(
      'ON_MESSAGE_RECEIVED', 
      (event) => {
        // ? Place all the code for handling the received message here
        const decodedMessage = decodeBase64(event.message);
        setMessages(prev => [...prev, `Received: ${decodedMessage}`]);
      }
    );

    
    const deviceDiscoveredListener = eventEmitter.addListener(
      'ON_DEVICE_DISCOVERED', 
      (event) => {
        console.log("New device discovered:", event.endpointId);
        
        // Check if the device is already in the connected devices list
        if (!connectedDevices.includes(event.endpointId)) {
          // Attempt to connect to the newly discovered device
          NearbyConnectionModule.requestConnection(
            SERVICE_UUID, 
            event.endpointId
          ).then(() => {
            // Device connection request successful
            setConnectedDevices(prev => [...prev, event.endpointId]);
          }).catch(() => {
            console.error("Failed to connect to discovered device.");
          });
        }
      }
    );

    return () => {
      deviceConnectedListener.remove();
      messageReceivedListener.remove();
      deviceDiscoveredListener.remove();
    };
  }, []);

  const broadcastMessage = async (message: string) => {
    try {
      // Send to all connected devices
      for (const endpointId of connectedDevices) {
        try {
          await NearbyConnectionModule.sendMessage(
            endpointId, 
            encodeBase64(message)
          );
        } catch (error) {
          console.error("Failed to send message to", endpointId, error);
          // Remove the disconnected device from the connected devices list
          setConnectedDevices(prev => prev.filter(id => id !== endpointId));
          console.log("Device removed from connected devices:", endpointId);
        }
      }
      setMessages(prev => [...prev, `You: ${message}`]);
    } catch (error) {
      console.log
      console.error("Message broadcast error:", error);
    }
  };

  const handleSend = () => {
    if (input.trim()) {
      console.log("Sending message:", input);
      broadcastMessage(input);
      setInput('');
    }
  };

  useEffect(() => {
    if (scrollViewRef.current) {
      scrollViewRef.current.scrollToEnd({ animated: true });
    }
  }, [messages]);

  return (
    <View style={styles.container}>
      {!isPermissionGranted && (
        <View style={styles.permissionBanner}>
          <Text style={styles.permissionText}>
            Waiting for Bluetooth and Location Permissions...
          </Text>
          <Button 
            title="Request Permissions" 
            onPress={requestNearbyPermissions} 
          />
        </View>
      )}
      <View style={styles.messagesContainer}>
        <ScrollView
          ref={scrollViewRef}
          contentContainerStyle={{ flexGrow: 1, justifyContent: 'flex-end' }}
        >
          {messages.map((message, index) => (
            <Text key={index} style={styles.message}>
              {message}
            </Text>
          ))}
        </ScrollView>
      </View>
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          value={input}
          onChangeText={setInput}
          placeholder="Type a message"
          placeholderTextColor="#888"
        />
        <Button title="Send" onPress={handleSend} color="#007AFF" />
      </View>
    </View>
  );
};

function encodeBase64(str: string): string {
  return Buffer.from(str, 'utf-8').toString('base64');
}

function decodeBase64(str: string): string {
  return Buffer.from(str, 'base64').toString('utf-8');
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9f9f9',
  },
  messagesContainer: {
    flex: 1,
    padding: 10,
  },
  message: {
    padding: 15,
    backgroundColor: '#0078fe',
    borderRadius: 20,
    marginVertical: 5,
    maxWidth: '100%',
    color: '#fff',
  },
  inputContainer: {
    flexDirection: 'row',
    padding: 10,
    borderTopWidth: 1,
    borderColor: '#ddd',
    backgroundColor: '#fff',
  },
  input: {
    flex: 1,
    borderRadius: 20,
    padding: 10,
    marginRight: 10,
    backgroundColor: '#fff',
  },
  permissionBanner: {
    backgroundColor: '#ffcc00',
    padding: 15,
    alignItems: 'center',
  },
  permissionText: {
    color: '#333',
    marginBottom: 10,
    textAlign: 'center',
  },

});