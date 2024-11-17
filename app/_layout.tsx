import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { useFonts } from "expo-font";
import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import React, { useEffect } from "react";

import { useColorScheme } from "@/hooks/useColorScheme";

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const [loaded] = useFonts({
    SpaceMono: require('../assets/fonts/SpaceMono-Regular.ttf'),
  });

  useEffect(() => {
    if (loaded) {
      SplashScreen.hideAsync();
    }
  }, [loaded]);

  if (!loaded) {
    return null;
  }

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <Stack>
        <Stack.Screen name="index" 
          options={{
            title: 'Set Username',
            headerBackVisible: false,
          }} 
        />
        <Stack.Screen 
          name="chat" 
          options={{
            title: 'Chat',
            headerBackVisible: false,
          }} 
        />
        <Stack.Screen name="+not-found" />
        <Stack.Screen name="help" />
      </Stack>
    </ThemeProvider>
  );
}
