import 'package:flutter/material.dart';
import 'core/network/api_client.dart';
import 'core/storage/token_storage.dart';
import 'features/auth/data/auth_api.dart';
import 'features/auth/data/auth_repo.dart';
import 'features/auth/presentation/auth_gate.dart';

void main() {
  final tokenStorage = TokenStorage();
  final apiClient = ApiClient(tokenStorage);
  final authApi = AuthApi(apiClient.dio);
  final authRepo = AuthRepo(authApi, tokenStorage);

  runApp(App(authRepo: authRepo));
}

class App extends StatelessWidget {
  final AuthRepo authRepo;
  const App({super.key, required this.authRepo});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: "TPPS",
      theme: ThemeData(useMaterial3: true),
      home: AuthGate(repo: authRepo),
    );
  }
}