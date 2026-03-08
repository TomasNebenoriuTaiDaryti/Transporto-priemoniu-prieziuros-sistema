import 'package:flutter/material.dart';
import '../data/auth_repo.dart';

class RegisterScreen extends StatefulWidget {
  final AuthRepo repo;
  const RegisterScreen({super.key, required this.repo});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final emailCtrl = TextEditingController();
  final passCtrl = TextEditingController();
  String? error;
  bool loading = false;

  Future<void> onRegister() async {
    setState(() { loading = true; error = null; });
    try {
      await widget.repo.register(emailCtrl.text.trim(), passCtrl.text);
      if (!mounted) return;
      Navigator.of(context).pushNamedAndRemoveUntil('/home', (r) => false);
    } catch (e) {
      setState(() => error = "Registracija nepavyko (email jau naudojamas)");
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Registracija")),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            TextField(controller: emailCtrl, decoration: const InputDecoration(labelText: "Email")),
            TextField(controller: passCtrl, obscureText: true, decoration: const InputDecoration(labelText: "Slaptažodis (min 6)")),
            const SizedBox(height: 12),
            if (error != null) Text(error!, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: loading ? null : onRegister,
              child: loading ? const CircularProgressIndicator() : const Text("Sukurti paskyrą"),
            ),
          ],
        ),
      ),
    );
  }
}