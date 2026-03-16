import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../../vehicles_details/data/docs_api.dart';
import '../../vehicles_details/data/records_api.dart';
import '../../vehicles_details/data/reminders_api.dart';
import '../../vehicles_details/presentation/vehicle_details_screen.dart';
import '../data/vehicle_api.dart';
import 'vin_input_screen.dart';
import 'vehicle_edit_screen.dart';

class MyVehiclesScreen extends StatefulWidget {
  final VehicleApi api;
  final ApiClient apiClient;

  const MyVehiclesScreen({super.key, required this.api, required this.apiClient});

  @override
  State<MyVehiclesScreen> createState() => _MyVehiclesScreenState();
}

class _MyVehiclesScreenState extends State<MyVehiclesScreen> {
  bool loading = true;
  String? error;
  List<Map<String, dynamic>> vehicles = [];

  Future<void> load() async {
    setState(() { loading = true; error = null; });
    try {
      vehicles = await widget.api.listAccessibleVehicles();
    } catch (e) {
      error = "Nepavyko užkrauti mašinų";
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> addVehicle() async {
    final changed = await Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => VinInputScreen(api: widget.api),
    ));
    if (changed == true) await load();
  }

  Future<void> openDetails(Map<String, dynamic> v) async {
    final id = (v["id"] as num).toInt();
    final docsApi = DocsApi(widget.apiClient.dio);
    final recApi = RecordsApi(widget.apiClient.dio);
    final remApi = RemindersApi(widget.apiClient.dio);

    await Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => VehicleDetailsScreen(
        vehicleId: id,
        vehicleApi: widget.api,
        docsApi: docsApi,
        recordsApi: recApi,
        remindersApi: remApi,
      ),
    ));

    await load();
  }

  Future<void> editVehicle(Map<String, dynamic> v) async {
    final canEdit = v["canEdit"] == true;
    if (!canEdit) return;

    final id = (v["id"] as num).toInt();
    try {
      final details = await widget.api.getVehicleDetails(id);
      final changed = await Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => VehicleEditScreen(
          api: widget.api,
          mode: VehicleEditMode.edit,
          existingVehicle: details,
        ),
      ));
      if (changed == true) await load();
    } catch (_) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Nepavyko atidaryti redagavimo.")),
      );
    }
  }

  Future<void> deleteVehicle(Map<String, dynamic> v) async {
    final canDelete = v["canDelete"] == true;
    if (!canDelete) return;

    final id = (v["id"] as num).toInt();
    try {
      await widget.api.deleteVehicle(id);
      await load();
    } catch (_) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Nepavyko ištrinti.")),
      );
    }
  }

  Future<void> useVehicle(Map<String, dynamic> v) async {
    final id = (v["id"] as num).toInt();
    try {
      await widget.api.useVehicle(id);
      await load();
    } catch (_) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Nepavyko nustatyti kaip naudojamos.")),
      );
    }
  }

  Future<void> unuse() async {
    try {
      await widget.api.unuseVehicle();
      await load();
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
          ? Center(child: Text(error!))
          : vehicles.isEmpty
          ? const Center(child: Text("Nėra automobilių. Pridėk pirmą!"))
          : ListView.builder(
        itemCount: vehicles.length,
        itemBuilder: (_, i) {
          final v = vehicles[i];
          final title = "${v["make"] ?? ""} ${v["model"] ?? ""} (${v["modelYear"] ?? ""})";
          final vin = (v["vin"] == null || v["vin"].toString().isEmpty) ? "-" : v["vin"].toString();
          final odo = v["odometerKm"]?.toString() ?? "-";
          final inGroup = v["groupId"] != null;
          final ownerEmail = v["ownerEmail"]?.toString() ?? "unknown";
          final canEdit = v["canEdit"] == true;
          final canDelete = v["canDelete"] == true;
          final activeByMe = v["activeByMe"] == true;
          final activeByEmail = v["activeByEmail"]?.toString();
          final usedByOther = activeByEmail != null && !activeByMe;

          Widget useBtn;
          if (activeByMe) {
            useBtn = TextButton(onPressed: unuse, child: const Text("Nebenaudoti"));
          } else if (usedByOther) {
            useBtn = Text("Naudoja: $activeByEmail", style: const TextStyle(fontSize: 12));
          } else {
            useBtn = TextButton(onPressed: () => useVehicle(v), child: const Text("Naudoti"));
          }

          return ListTile(
            title: Text(title),
            subtitle: Text(
              "VIN: $vin • Rida: $odo km"
                  "${inGroup ? " • Grupėje" : ""}"
                  "${canEdit ? "" : " • Owner: $ownerEmail"}",
            ),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  icon: const Icon(Icons.description),
                  onPressed: () => openDetails(v),
                ),
                useBtn,
                if (canEdit || canDelete)
                  PopupMenuButton<String>(
                    onSelected: (x) {
                      if (x == "edit") editVehicle(v);
                      if (x == "del") deleteVehicle(v);
                    },
                    itemBuilder: (_) => [
                      if (canEdit) const PopupMenuItem(value: "edit", child: Text("Redaguoti")),
                      if (canDelete) const PopupMenuItem(value: "del", child: Text("Pašalinti")),
                    ],
                  ),
              ],
            ),
            onTap: () => openDetails(v),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: addVehicle,
        child: const Icon(Icons.add),
      ),
    );
  }
}