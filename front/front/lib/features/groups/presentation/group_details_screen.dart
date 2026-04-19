import 'package:flutter/material.dart';
import '../../garage/data/vehicle_api.dart';
import '../data/groups_api.dart';

class GroupDetailsScreen extends StatefulWidget {
  final GroupsApi groupsApi;
  final VehicleApi vehicleApi;
  final Map<String, dynamic> group;

  const GroupDetailsScreen({
    super.key,
    required this.groupsApi,
    required this.vehicleApi,
    required this.group,
  });

  @override
  State<GroupDetailsScreen> createState() => _GroupDetailsScreenState();
}

class _GroupDetailsScreenState extends State<GroupDetailsScreen> {
  bool loading = true;
  String? error;

  late int groupId;
  late String name;
  late String myRole;

  List<Map<String, dynamic>> vehicles = [];
  List<Map<String, dynamic>> myVehicles = [];
  List<Map<String, dynamic>> members = [];

  bool membersExpanded = false;

  bool get isOwner => myRole == "OWNER";

  Future<void> load() async {
    setState(() { loading = true; error = null; });
    try {
      vehicles = await widget.vehicleApi.listGroupVehicles(groupId);
      myVehicles = await widget.vehicleApi.listMyVehicles();
      members = await widget.groupsApi.members(groupId);
    } catch (e) {
      error = "Nepavyko užkrauti grupės";
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    groupId = (widget.group["id"] as num).toInt();
    name = widget.group["name"].toString();
    myRole = widget.group["myRole"].toString();
    load();
  }

  Future<void> rename() async {
    final ctrl = TextEditingController(text: name);
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text("Redaguoti grupę"),
        content: TextField(controller: ctrl, decoration: const InputDecoration(labelText: "Pavadinimas")),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text("Atšaukti")),
          ElevatedButton(onPressed: () => Navigator.pop(context, true), child: const Text("Išsaugoti")),
        ],
      ),
    );
    if (ok == true) {
      final updated = await widget.groupsApi.renameGroup(groupId, ctrl.text.trim());
      setState(() => name = updated["name"].toString());
    }
  }

  Future<void> invite() async {
    final ctrl = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text("Pakviesti narį (demo)"),
        content: TextField(controller: ctrl, decoration: const InputDecoration(labelText: "El. paštas")),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text("Atšaukti")),
          ElevatedButton(onPressed: () => Navigator.pop(context, true), child: const Text("Siųsti")),
        ],
      ),
    );
    if (ok == true) {
      try {
        final msg = await widget.groupsApi.invite(groupId, ctrl.text.trim());
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
        await load();
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Vartotojas neegzistuoja arba nepavyko pakviesti.")),
        );
      }
    }
  }

  Future<void> addVehicleToGroup() async {
    final choices = myVehicles.where((v) => v["groupId"] == null).toList();
    if (choices.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Nėra laisvų tavo mašinų. Pirma pridėk į savo garažą.")),
      );
      return;
    }

    int selectedId = (choices.first["id"] as num).toInt();

    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text("Pridėti mašiną į grupę"),
        content: StatefulBuilder(
          builder: (context, setLocal) => DropdownButton<int>(
            value: selectedId,
            items: choices.map((v) {
              final id = (v["id"] as num).toInt();
              final label = "${v["make"]} ${v["model"]} (${v["modelYear"]})";
              return DropdownMenuItem(value: id, child: Text(label));
            }).toList(),
            onChanged: (v) => setLocal(() => selectedId = v!),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text("Atšaukti")),
          ElevatedButton(onPressed: () => Navigator.pop(context, true), child: const Text("Pridėti")),
        ],
      ),
    );

    if (ok == true) {
      await widget.groupsApi.addVehicleToGroup(groupId, selectedId);
      await load();
    }
  }

  Future<void> removeVehicle(Map<String, dynamic> v) async {
    final id = (v["id"] as num).toInt();
    await widget.groupsApi.removeVehicleFromGroup(groupId, id);
    await load();
  }

  Future<void> removeMember(Map<String, dynamic> m) async {
    final userId = (m["userId"] as num).toInt();
    try {
      await widget.groupsApi.removeMember(groupId, userId);
      await load();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Nepavyko pašalinti nario.")),
      );
    }
  }

  Future<void> leaveOrDelete() async {
    if (isOwner) {
      final ok = await showDialog<bool>(
        context: context,
        builder: (_) => AlertDialog(
          title: const Text("Ištrinti grupę?"),
          content: const Text("Savininkas negali išeiti. Galima tik ištrinti grupę."),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context, false), child: const Text("Atšaukti")),
            ElevatedButton(onPressed: () => Navigator.pop(context, true), child: const Text("Ištrinti")),
          ],
        ),
      );
      if (ok == true) {
        await widget.groupsApi.deleteGroup(groupId);
        if (!mounted) return;
        Navigator.pop(context, true);
      }
    } else {
      await widget.groupsApi.leaveGroup(groupId);
      if (!mounted) return;
      Navigator.pop(context, true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(name),
        actions: [
          IconButton(onPressed: load, icon: const Icon(Icons.refresh)),
          if (isOwner) IconButton(onPressed: rename, icon: const Icon(Icons.edit)),
          if (isOwner) IconButton(onPressed: invite, icon: const Icon(Icons.person_add)),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
          ? Center(child: Text(error!))
          : ListView(
        padding: const EdgeInsets.all(12),
        children: [
          ExpansionTile(
            initiallyExpanded: membersExpanded,
            onExpansionChanged: (v) => setState(() => membersExpanded = v),
            title: Text("Nariai (${members.length})", style: const TextStyle(fontWeight: FontWeight.bold)),
            children: members.map((m) {
              final email = m["email"].toString();
              final role = m["role"].toString();
              final roleLabel =
              role == "OWNER" ? "Savininkas" :
              role == "MEMBER" ? "Narys" :
              role;
              final isOwnerRow = role == "OWNER";
              return ListTile(
                title: Text(email),
                subtitle: Text(roleLabel),
                trailing: isOwner && !isOwnerRow
                    ? IconButton(
                  icon: const Icon(Icons.person_remove),
                  onPressed: () => removeMember(m),
                )
                    : null,
              );
            }).toList(),
          ),
          const Divider(),

          const Text("Mašinos grupėje", style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          if (vehicles.isEmpty) const Text("Grupėje nėra mašinų"),
          ...vehicles.map((v) {
            final title = "${v["make"] ?? ""} ${v["model"] ?? ""} (${v["modelYear"] ?? ""})";
            return ListTile(
              title: Text(title),
              subtitle: Text("VIN: ${v["vin"] ?? "-"} • Rida: ${v["odometerKm"]} km"),
              trailing: isOwner
                  ? IconButton(
                icon: const Icon(Icons.delete),
                onPressed: () => removeVehicle(v),
              )
                  : null,
            );
          }).toList(),
        ],
      ),
      floatingActionButton: isOwner
          ? FloatingActionButton(
        onPressed: addVehicleToGroup,
        child: const Icon(Icons.add),
      )
          : null,
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: ElevatedButton(
            onPressed: leaveOrDelete,
            child: Text(isOwner ? "Ištrinti grupę" : "Išeiti iš grupės"),
          ),
        ),
      ),
    );
  }
}