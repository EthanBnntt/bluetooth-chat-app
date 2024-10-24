import React, { useState, useEffect } from "react";
import { StyleSheet, View, Button, Text} from "react-native";
import AsyncStorage from '@react-native-async-storage/async-storage';

const WelcomeScreen = () => {
    const [hasSeen, setHasSeen] = useState(false);
  
    useEffect(() => {
      const checkSeen = async () => {
        try {
          const seen = await AsyncStorage.getItem('hasSeenOneTimeScreen');
          if (seen === 'true') {
            setHasSeen(true);
          }
        } catch (error) {
          console.error('Error checking AsyncStorage:', error);
        }
      };
  
      checkSeen();
    }, []);
  
    const markAsSeen = async () => {
      try {
        await AsyncStorage.setItem('hasSeenOneTimeScreen', 'true');
        setHasSeen(true);
      } catch (error) {
        console.error('Error setting AsyncStorage:', error);
      }
    };
  
    if (hasSeen) {
      return null; // Don't render the screen
    }

    return (
        <View style={styles.container}>
          <Text style={styles.title}>Welcome to the App!</Text>
          {/* Add your content here */}
          <Button title="Let's Go!" onPress={markAsSeen} />
        </View>
      );
    };
    
    const styles = StyleSheet.create({
      container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
      },
      title: {
        fontSize: 24,
        fontWeight: 'bold',
      },
    });

    export default WelcomeScreen;