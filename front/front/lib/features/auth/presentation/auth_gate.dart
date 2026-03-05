import 'package:flutter/material.dart';
import '../data/auth_repo.dart';
import 'login_screen.dart';

class AuthGate extends StatelessWidget {
  final AuthRepo repo;
  const AuthGate({super.key, required this.repo});

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool>(
      future: repo.isLoggedIn(),
      builder: (context, snap) {
        if (!snap.hasData) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }
        final loggedIn = snap.data!;
        if (!loggedIn) return LoginScreen(repo: repo);
        return Scaffold(
          appBar: AppBar(title: const Text("TPPS")),
          body: const Center(child: Text("Prisijungta")),
        );
      },
    );
  }
}