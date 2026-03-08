import 'package:flutter/material.dart';
import '../../garage/data/vehicle_api.dart';
import '../data/groups_api.dart';
import 'group_details_screen.dart';

class GroupsScreen extends StatefulWidget {
  final GroupsApi api;
  final VehicleApi vehicleApi;
  const GroupsScreen({super.key, required this.api, required this.vehicleApi});

  @override
  State<GroupsScreen> createState() => _GroupsScreenState();
}

class _GroupsScreenState extends State<GroupsScreen> {
  bool loading = true;
  String? error;
  List<Map<String, dynamic>> groups = [];

  Future<void> load() async {
    setState(() { loading = true; error = null; });
    try {
      groups = await widget.api.myGroups();
    } catch (e) {
      error = "Nepavyko užkrauti grupių";
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> createGroup() async {
    final ctrl = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text("Sukurti grupę"),
        content: TextField(controller: ctrl, decoration: const InputDecoration(labelText: "Pavadinimas")),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text("Atšaukti")),
          ElevatedButton(onPressed: () => Navigator.pop(context, true), child: const Text("Sukurti")),
        ],
      ),
    );
    if (ok == true) {
      await widget.api.createGroup(ctrl.text.trim());
      await load();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Grupės"),
        actions: [
          IconButton(onPressed: load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
          ? Center(child: Text(error!))
          : ListView.builder(
        itemCount: groups.length,
        itemBuilder: (_, i) {
          final g = groups[i];
          return ListTile(
            title: Text(g["name"].toString()),
            subtitle: Text("Role: ${g["myRole"]}"),
            onTap: () async {
              final changed = await Navigator.of(context).push(MaterialPageRoute(
                builder: (_) => GroupDetailsScreen(
                  groupsApi: widget.api,
                  vehicleApi: widget.vehicleApi,
                  group: g,
                ),
              ));
              if (changed == true) await load();
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: createGroup,
        child: const Icon(Icons.group_add),
      ),
    );
  }
}