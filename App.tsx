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
  NativeModules,
  Alert,
  ScrollView,
} from 'react-native';

const { NeonBackend } = NativeModules;

export default function App(): React.JSX.Element {
  const [isSplash, setIsSplash] = useState(true);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionToken, setSessionToken] = useState('');
  const [neonConfig, setNeonConfig] = useState<any>(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsSplash(false);
    }, 2200);
    return () => clearTimeout(timer);
  }, []);

  const handleLogin = async () => {
    if (!email.trim() || !password.trim()) {
      Alert.alert('Required', 'Please enter both Email and Password');
      return;
    }
    setLoading(true);
    try {
      if (NeonBackend) {
        const token = await NeonBackend.nativeLogin(email.trim(), password);
        const config = await NeonBackend.getNeonConfig();
        setSessionToken(token);
        setNeonConfig(config);
      } else {
        setSessionToken('neon_local_session_' + Date.now());
        setNeonConfig({
          endpoint: 'https://br-cold-tree-ay0sxicu.storage.c-5.us-east-2.aws.neon.tech',
          region: 'us-east-2',
          accessKey: 'nak_live_8bff28857082488eb6ba25c7006aabec',
          aiKey: 'nt_live_8bff28857082_cgP7mO4L61b7sp2hOX608out2L7pDjjo',
        });
      }
      setIsLoggedIn(true);
    } catch (err: any) {
      Alert.alert('Login Failed', err?.message || 'Authentication error');
    } finally {
      setLoading(false);
    }
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
          <Text style={styles.splashSubtitle}>Powered by Neon Native Core</Text>
          <ActivityIndicator size="small" color="#00E5FF" style={{ marginTop: 28 }} />
        </View>
      </SafeAreaView>
    );
  }

  if (isLoggedIn) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#0B0F19" />
        <ScrollView contentContainerStyle={styles.dashboard}>
          <View style={styles.dashHeader}>
            <Text style={styles.welcomeText}>Neon Console</Text>
            <Text style={styles.instructionText}>Connected via C++ JNI Native Core</Text>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>User Session</Text>
            <Text style={styles.cardText}>User: {email}</Text>
            <Text style={styles.cardText}>Session Token: {sessionToken.substring(0, 24)}...</Text>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>Neon Storage & Gateway</Text>
            <Text style={styles.cardText}>Endpoint: {neonConfig?.endpoint}</Text>
            <Text style={styles.cardText}>Region: {neonConfig?.region}</Text>
            <Text style={styles.cardText}>Access Key: {neonConfig?.accessKey?.substring(0, 12)}***</Text>
            <Text style={styles.statusBadge}>Status: Active & Linked</Text>
          </View>

          <TouchableOpacity
            style={styles.loginBtn}
            onPress={() => {
              setIsLoggedIn(false);
              setPassword('');
            }}
          >
            <Text style={styles.loginBtnText}>Log Out</Text>
          </TouchableOpacity>
        </ScrollView>
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
          <Text style={styles.welcomeText}>MCOS Sign In</Text>
          <Text style={styles.instructionText}>Neon Database & Native Storage Auth</Text>
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
              <Text style={styles.loginBtnText}>Sign In with Neon</Text>
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
  dashboard: {
    padding: 24,
    gap: 16,
  },
  dashHeader: {
    marginBottom: 12,
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
  card: {
    backgroundColor: '#111827',
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
    borderColor: '#1F2937',
    gap: 8,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#00E5FF',
    marginBottom: 4,
  },
  cardText: {
    color: '#D1D5DB',
    fontSize: 13,
  },
  statusBadge: {
    color: '#10B981',
    fontWeight: '700',
    marginTop: 4,
  },
});
