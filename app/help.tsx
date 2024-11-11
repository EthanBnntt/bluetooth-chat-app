import React from 'react';
import { Text, View, StyleSheet } from 'react-native';

export default function HelpPage() {
    return (
        <View style={styles.container}>
            <Text style={styles.helpText}>
                Welcome to Blue Chat! Make sure you have Bluetooth enabled in settings
                and are in the radius of nearby users!
            </Text>
        </View>
    );
}
const styles = StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: '#f9f9f9',
      justifyContent: 'center',
      alignItems: 'center',
      padding: 20,
    },
    helpText: {
        textAlign: 'center',
        fontSize: 20,
    }
});
