import 'package:flutter/material.dart';
import '../data/auth_repo.dart';
import 'register_screen.dart';

class LoginScreen extends StatefulWidget {
  final AuthRepo repo;
  const LoginScreen({super.key, required this.repo});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final emailCtrl = TextEditingController();
  final passCtrl = TextEditingController();
  String? error;
  bool loading = false;

  Future<void> onLogin() async {
    setState(() { loading = true; error = null; });
    try {
      await widget.repo.login(emailCtrl.text.trim(), passCtrl.text);
      if (!mounted) return;
      Navigator.of(context).pushNamedAndRemoveUntil('/home', (route) => false);
    } catch (e) {
      setState(() => error = "Nepavyko prisijungti (patikrink email/slaptažodį)");
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Prisijungimas")),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            TextField(controller: emailCtrl, decoration: const InputDecoration(labelText: "Email")),
            TextField(controller: passCtrl, obscureText: true, decoration: const InputDecoration(labelText: "Slaptažodis")),
            const SizedBox(height: 12),
            if (error != null) Text(error!, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: loading ? null : onLogin,
              child: loading ? const CircularProgressIndicator() : const Text("Prisijungti"),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => RegisterScreen(repo: widget.repo),
                ));
              },
              child: const Text("Neturi paskyros? Registruokis"),
            )
          ],
        ),
      ),
    );
  }
}