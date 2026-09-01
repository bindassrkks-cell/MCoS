import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TextInput,
  TouchableOpacity,
  SafeAreaView,
  StatusBar,
  ActivityIndicator,
  Alert,
} from 'react-native';

export default function App(): React.JSX.Element {
  const [isSplash, setIsSplash] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsSplash(false);
    }, 2400);
    return () => clearTimeout(timer);
  }, []);

  const handleLogin = () => {
    if (!email.trim() || !password.trim()) {
      Alert.alert('Authentication', 'Please enter your email and password');
      return;
    }
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      Alert.alert('MCOS Console', 'Login Successful! Welcome to MCOS.');
    }, 1200);
  };

  if (isSplash) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#0B0F19" />
        <View style={styles.splashContent}>
          <View style={styles.logoBadge}>
            <Text style={styles.logoBadgeText}>MC</Text>
          </View>
          <Text style={styles.splashTitle}>MCOS</Text>
          <Text style={styles.splashSubtitle}>Next-Gen Mobile Platform</Text>
          <ActivityIndicator size="small" color="#00E5FF" style={{ marginTop: 28 }} />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#0B0F19" />
      <View style={styles.loginWrapper}>
        <View style={styles.headerArea}>
          <View style={styles.smallBadge}>
            <Text style={styles.smallBadgeText}>MC</Text>
          </View>
          <Text style={styles.welcomeText}>Welcome to MCOS</Text>
          <Text style={styles.instructionText}>Sign in to access your console</Text>
        </View>

        <View style={styles.formArea}>
          <Text style={styles.label}>Email Address</Text>
          <TextInput
            style={styles.input}
            placeholder="user@mcos.io"
            placeholderTextColor="#4B5563"
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            keyboardType="email-address"
          />

          <Text style={styles.label}>Password</Text>
          <TextInput
            style={styles.input}
            placeholder="••••••••••••"
            placeholderTextColor="#4B5563"
            secureTextEntry
            value={password}
            onChangeText={setPassword}
          />

          <TouchableOpacity style={styles.loginBtn} onPress={handleLogin} activeOpacity={0.85}>
            {loading ? (
              <ActivityIndicator color="#0B0F19" />
            ) : (
              <Text style={styles.loginBtnText}>Sign In</Text>
            )}
          </TouchableOpacity>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0B0F19',
  },
  splashContent: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoBadge: {
    width: 96,
    height: 96,
    borderRadius: 24,
    backgroundColor: '#111827',
    borderWidth: 2,
    borderColor: '#00E5FF',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
  },
  logoBadgeText: {
    color: '#00E5FF',
    fontSize: 38,
    fontWeight: '900',
  },
  splashTitle: {
    fontSize: 34,
    fontWeight: '900',
    color: '#FFFFFF',
    letterSpacing: 6,
  },
  splashSubtitle: {
    fontSize: 14,
    color: '#9CA3AF',
    marginTop: 8,
  },
  loginWrapper: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 28,
  },
  headerArea: {
    marginBottom: 32,
  },
  smallBadge: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: '#111827',
    borderWidth: 1.5,
    borderColor: '#00E5FF',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  smallBadgeText: {
    color: '#00E5FF',
    fontSize: 18,
    fontWeight: '800',
  },
  welcomeText: {
    fontSize: 28,
    fontWeight: '800',
    color: '#FFFFFF',
  },
  instructionText: {
    fontSize: 14,
    color: '#9CA3AF',
    marginTop: 6,
  },
  formArea: {
    gap: 8,
  },
  label: {
    color: '#D1D5DB',
    fontSize: 13,
    fontWeight: '600',
    marginTop: 10,
    marginBottom: 4,
  },
  input: {
    backgroundColor: '#111827',
    borderWidth: 1,
    borderColor: '#1F2937',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    color: '#FFFFFF',
    fontSize: 15,
  },
  loginBtn: {
    backgroundColor: '#00E5FF',
    paddingVertical: 15,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 24,
  },
  loginBtnText: {
    color: '#0B0F19',
    fontSize: 16,
    fontWeight: '700',
  },
});
