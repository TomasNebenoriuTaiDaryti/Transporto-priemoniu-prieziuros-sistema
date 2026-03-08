import 'package:flutter/material.dart';
import 'core/network/api_client.dart';
import 'core/storage/token_storage.dart';
import 'features/auth/data/auth_api.dart';
import 'features/auth/data/auth_repo.dart';
import 'features/auth/presentation/auth_gate.dart';
import 'features/auth/presentation/login_screen.dart';

void main() {
  final tokenStorage = TokenStorage();
  final apiClient = ApiClient(tokenStorage);
  final authApi = AuthApi(apiClient.dio);
  final authRepo = AuthRepo(authApi, tokenStorage);

  runApp(App(
    authRepo: authRepo,
    apiClient: apiClient,
    tokenStorage: tokenStorage,
  ));
}

class App extends StatelessWidget {
  final AuthRepo authRepo;
  final ApiClient apiClient;
  final TokenStorage tokenStorage;

  const App({
    super.key,
    required this.authRepo,
    required this.apiClient,
    required this.tokenStorage,
  });

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: "TPPS",
      theme: ThemeData(useMaterial3: true),
      initialRoute: '/home',
      routes: {
        '/home': (_) => AuthGate(repo: authRepo, apiClient: apiClient, tokenStorage: tokenStorage),
        '/login': (_) => LoginScreen(repo: authRepo),
      },
    );
  }
}