import 'package:flutter/material.dart';
import '../../garage/data/vehicle_api.dart';
import '../../../core/notifications/local_notifications.dart';
import '../data/docs_api.dart';
import '../data/records_api.dart';
import '../data/reminders_api.dart';
import 'add_document_screen.dart';
import 'add_record_screen.dart';
import 'edit_document_screen.dart';
import 'edit_record_screen.dart';

class VehicleDetailsScreen extends StatefulWidget {
  final int vehicleId;
  final VehicleApi vehicleApi;
  final DocsApi docsApi;
  final RecordsApi recordsApi;
  final RemindersApi remindersApi;
  const VehicleDetailsScreen({
    super.key,
    required this.vehicleId,
    required this.vehicleApi,
    required this.docsApi,
    required this.recordsApi,
    required this.remindersApi,
  });

  @override
  State<VehicleDetailsScreen> createState() => _VehicleDetailsScreenState();
}

class _VehicleDetailsScreenState extends State<VehicleDetailsScreen> {
  bool loading = true;
  String? error;
  Map<String, dynamic>? vehicle;
  List<Map<String, dynamic>> docs = [];
  List<Map<String, dynamic>> records = [];
  List<Map<String, dynamic>> reminders = [];

  Future<void> _scheduleLocalNotifications() async {
    await LocalNotifications.cancelAll();

    int id = 1000;
    for (final r in reminders) {
      final title = (r["title"] ?? "Priminimas").toString();
      final msg = (r["message"] ?? "").toString();

      final dueAt = r["due_at"];
      if (dueAt != null) {
        final when = DateTime.tryParse(dueAt.toString());
        if (when != null && when.isAfter(DateTime.now())) {
          await LocalNotifications.scheduleAt(id: id++, when: when, title: title, body: msg);
        }
      }
    }
  }

  Future<void> load() async {
    setState(() { loading = true; error = null; });
    try {
      vehicle = await widget.vehicleApi.getVehicleDetails(widget.vehicleId);
      docs = await widget.docsApi.list(widget.vehicleId);
      records = await widget.recordsApi.list(widget.vehicleId);

      final canManage = vehicle?["canManageReminders"] == true;
      if (canManage) {
        reminders = await widget.remindersApi.list(widget.vehicleId);
        await _scheduleLocalNotifications();
      } else {
        reminders = [];
      }
    } catch (e) {
      error = "Nepavyko užkrauti";
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  Widget build(BuildContext context) {
    final title = vehicle == null ? "Automobilis" : "${vehicle!["make"]} ${vehicle!["model"]}";
    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
          IconButton(onPressed: load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
          ? Center(child: Text(error!))
          : DefaultTabController(
        length: 3,
        child: Column(
          children: [
            const TabBar(tabs: [
              Tab(text: "Dokumentai"),
              Tab(text: "Įrašai"),
              Tab(text: "Priminimai"),
            ]),
            Expanded(
              child: TabBarView(
                children: [
                  _docsTab(),
                  _recordsTab(),
                  _remindersTab(),
                ],
              ),
            )
          ],
        ),
      ),
    );
  }

  Widget _docsTab() {
    return Column(
      children: [
        Expanded(
          child: docs.isEmpty
              ? const Center(child: Text("Nėra dokumentų"))
              : ListView.builder(
            itemCount: docs.length,
            itemBuilder: (_, i) {
              final d = docs[i];
              final canEdit = d["canEdit"] == true;
              final type = d["type"].toString();
              final title = d["title"].toString();
              final end = d["endDate"]?.toString() ?? "-";
              return ListTile(
                title: Text("$type • $title"),
                subtitle: Text("Galioja iki: $end"),
                trailing: canEdit
                    ? Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.edit),
                      onPressed: () async {
                        final changed = await Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) => EditDocumentScreen(
                            vehicleId: widget.vehicleId,
                            doc: d,
                            docsApi: widget.docsApi,
                          ),
                        ));
                        if (changed == true) await load();
                      },
                    ),
                    IconButton(
                      icon: const Icon(Icons.delete),
                      onPressed: () async {
                        await widget.docsApi.delete(widget.vehicleId, d["id"].toString());
                        await load();
                      },
                    ),
                  ],
                )
                    : null,
              );
            },
          ),
        ),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: ElevatedButton(
              onPressed: () async {
                final changed = await Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => AddDocumentScreen(vehicleId: widget.vehicleId, docsApi: widget.docsApi),
                ));
                if (changed == true) await load();
              },
              child: const Text("Pridėti dokumentą"),
            ),
          ),
        )
      ],
    );
  }

  Widget _recordsTab() {
    return Column(
      children: [
        Expanded(
          child: records.isEmpty
              ? const Center(child: Text("Nėra įrašų"))
              : ListView.builder(
            itemCount: records.length,
            itemBuilder: (_, i) {
              final r = records[i];
              final canEdit = r["canEdit"] == true;
              final kind = r["kind"].toString();
              final title = r["title"].toString();
              final odo = r["odometerKm"]?.toString() ?? "-";
              return ListTile(
                title: Text("$kind • $title"),
                subtitle: Text("Rida: $odo km"),
                trailing: canEdit
                    ? Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.edit),
                      onPressed: () async {
                        final changed = await Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) => EditRecordScreen(
                            vehicleId: widget.vehicleId,
                            record: r,
                            recordsApi: widget.recordsApi,
                          ),
                        ));
                        if (changed == true) await load();
                      },
                    ),
                    IconButton(
                      icon: const Icon(Icons.delete),
                      onPressed: () async {
                        await widget.recordsApi.delete(widget.vehicleId, (r["id"] as num).toInt());
                        await load();
                      },
                    ),
                  ],
                )
                    : null,
              );
            },
          ),
        ),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: ElevatedButton(
              onPressed: () async {
                final changed = await Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => AddRecordScreen(vehicleId: widget.vehicleId, recordsApi: widget.recordsApi),
                ));
                if (changed == true) await load();
              },
              child: const Text("Pridėti įrašą"),
            ),
          ),
        )
      ],
    );
  }

  Widget _remindersTab() {
    return reminders.isEmpty
        ? const Center(child: Text("Nėra priminimų"))
        : ListView.builder(
      itemCount: reminders.length,
      itemBuilder: (_, i) {
        final r = reminders[i];
        final title = (r["title"] ?? "").toString();
        final msg = (r["message"] ?? "").toString();
        final dueAt = r["due_at"]?.toString();
        final dueKm = r["due_odometer_km"]?.toString();
        return ListTile(
          title: Text(title),
          subtitle: Text("$msg\nDue: ${dueAt ?? "-"}  KM: ${dueKm ?? "-"}"),
        );
      },
    );
  }
}