import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../../../core/storage/token_storage.dart';
import '../../garage/data/vehicle_api.dart';
import '../../garage/presentation/my_vehicles_screen.dart';
import '../../groups/data/groups_api.dart';
import '../../groups/presentation/groups_screen.dart';
import '../../stats/data/stats_api.dart';
import '../../stats/presentation/stats_screen.dart';
import '../../ai_chat/data/ai_chat_api.dart';
import '../../ai_chat/presentation/ai_chat_screen.dart';
import '../data/auth_repo.dart';
import 'login_screen.dart';

class AuthGate extends StatelessWidget {
  final AuthRepo repo;
  final ApiClient apiClient;
  final TokenStorage tokenStorage;

  const AuthGate({
    super.key,
    required this.repo,
    required this.apiClient,
    required this.tokenStorage,
  });

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool>(
      future: repo.isLoggedIn(),
      builder: (context, snap) {
        if (!snap.hasData) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }
        if (!snap.data!) return LoginScreen(repo: repo);

        final vehicleApi = VehicleApi(apiClient.dio);
        final groupsApi = GroupsApi(apiClient.dio);
        final statsApi = StatsApi(apiClient.dio);
        final aiChatApi = AiChatApi(apiClient.dio);

        return _HomeTabs(
          vehicleApi: vehicleApi,
          groupsApi: groupsApi,
          statsApi: statsApi,
          aiChatApi: aiChatApi,
          tokenStorage: tokenStorage,
          apiClient: apiClient,
        );
      },
    );
  }
}

class _HomeTabs extends StatefulWidget {
  final VehicleApi vehicleApi;
  final GroupsApi groupsApi;
  final StatsApi statsApi;
  final AiChatApi aiChatApi;
  final TokenStorage tokenStorage;
  final ApiClient apiClient;

  const _HomeTabs({
    required this.vehicleApi,
    required this.groupsApi,
    required this.statsApi,
    required this.aiChatApi,
    required this.tokenStorage,
    required this.apiClient,
  });

  @override
  State<_HomeTabs> createState() => _HomeTabsState();
}

class _HomeTabsState extends State<_HomeTabs> {
  int idx = 0;

  Future<void> logout() async {
    await widget.tokenStorage.clear();
    if (!mounted) return;
    Navigator.of(context).pushNamedAndRemoveUntil('/login', (r) => false);
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      MyVehiclesScreen(api: widget.vehicleApi, apiClient: widget.apiClient),
      GroupsScreen(api: widget.groupsApi, vehicleApi: widget.vehicleApi),
      StatsScreen(vehicleApi: widget.vehicleApi, statsApi: widget.statsApi),
      AiChatScreen(vehicleApi: widget.vehicleApi, aiChatApi: widget.aiChatApi),
    ];

    return Scaffold(
      appBar: AppBar(
        title: Text(idx == 0 ? "TPPS • Mano mašinos" : idx == 1 ? "TPPS • Grupės" : idx == 2 ? "TPPS • Statistika" : "TPPS • AI chat",),
        actions: [
          IconButton(onPressed: logout, icon: const Icon(Icons.logout)),
        ],
      ),
      body: pages[idx],
      bottomNavigationBar: NavigationBar(
        selectedIndex: idx,
        onDestinationSelected: (v) => setState(() => idx = v),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.directions_car), label: "Garažas"),
          NavigationDestination(icon: Icon(Icons.groups), label: "Grupės"),
          NavigationDestination(icon: Icon(Icons.bar_chart), label: "Statistika"),
          NavigationDestination(icon: Icon(Icons.smart_toy), label: "AI"),
        ],
      ),
    );
  }
}