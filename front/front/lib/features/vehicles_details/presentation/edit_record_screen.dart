import 'dart:convert';
import 'package:flutter/material.dart';
import '../data/records_api.dart';

class EditRecordScreen extends StatefulWidget {
  final int vehicleId;
  final Map<String, dynamic> record;
  final RecordsApi recordsApi;
  const EditRecordScreen({
    super.key,
    required this.vehicleId,
    required this.record,
    required this.recordsApi,
  });

  @override
  State<EditRecordScreen> createState() => _EditRecordScreenState();
}

class _EditRecordScreenState extends State<EditRecordScreen> {
  late String kind;
  final titleCtrl = TextEditingController();
  final descCtrl = TextEditingController();
  final dateCtrl = TextEditingController();
  final odoCtrl = TextEditingController();
  final priceCtrl = TextEditingController();
  final litersCtrl = TextEditingController();
  String tireType = "ALL_SEASON";
  final tireAgeCtrl = TextEditingController();
  bool loading = false;

  @override
  void initState() {
    super.initState();
    kind = widget.record["kind"].toString();
    titleCtrl.text = (widget.record["title"] ?? "").toString();
    descCtrl.text = (widget.record["description"] ?? "").toString();
    final performed = DateTime.tryParse(widget.record["performedAt"].toString()) ?? DateTime.now();
    dateCtrl.text = performed.toIso8601String().substring(0, 10);
    odoCtrl.text = (widget.record["odometerKm"] ?? "").toString();
    priceCtrl.text = (widget.record["totalCost"] ?? "0").toString();
    final metaRaw = (widget.record["metaJson"] ?? "{}").toString();
    try {
      final meta = jsonDecode(metaRaw) as Map<String, dynamic>;
      if (meta["tireType"] != null) tireType = meta["tireType"].toString();
      if (meta["tireAgeYears"] != null) tireAgeCtrl.text = meta["tireAgeYears"].toString();
      if (meta["liters"] != null) litersCtrl.text = meta["liters"].toString();
    } catch (_) {}
  }

  @override
  void dispose() {
    titleCtrl.dispose();
    descCtrl.dispose();
    dateCtrl.dispose();
    odoCtrl.dispose();
    priceCtrl.dispose();
    litersCtrl.dispose();
    tireAgeCtrl.dispose();
    super.dispose();
  }

  Future<void> pickDate() async {
    final init = DateTime.tryParse(dateCtrl.text) ?? DateTime.now();
    final d = await showDatePicker(context: context, firstDate: DateTime(2000), lastDate: DateTime(2100), initialDate: init);
    if (d != null) setState(() => dateCtrl.text = d.toIso8601String().substring(0,10));
  }

  Future<void> save() async {
    setState(() => loading = true);
    try {
      final id = (widget.record["id"] as num).toInt();
      final odo = int.tryParse(odoCtrl.text.trim()) ?? 0;
      final performedAt = DateTime.parse("${dateCtrl.text}T09:00:00Z").toUtc().toIso8601String();

      await widget.recordsApi.update(
        widget.vehicleId,
        id,
        kind: kind,
        title: kind == "OTHER" ? (titleCtrl.text.trim().isEmpty ? "Kitas įrašas" : titleCtrl.text.trim()) : null,
        description: descCtrl.text.trim().isEmpty ? null : descCtrl.text.trim(),
        performedAt: performedAt,
        odometerKm: odo,
        totalCost: double.tryParse(priceCtrl.text.trim()) ?? 0,
        currency: "EUR",
        tireType: kind == "TIRES" ? tireType : null,
        tireAgeYears: kind == "TIRES" ? int.tryParse(tireAgeCtrl.text.trim()) : null,
        liters: kind == "FUEL" ? double.tryParse(litersCtrl.text.trim()) : null,
      );
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Redaguoti įrašą")),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: ListView(
          children: [
            DropdownButton<String>(
              value: kind,
              items: const [
                DropdownMenuItem(value: "OIL", child: Text("Tepalai + filtras")),
                DropdownMenuItem(value: "TIRES", child: Text("Padangos")),
                DropdownMenuItem(value: "BRAKES", child: Text("Stabdžiai")),
                DropdownMenuItem(value: "SERVICE", child: Text("Apsilankymas servise")),
                DropdownMenuItem(value: "FUEL", child: Text("Degalų pylimas")),
                DropdownMenuItem(value: "OTHER", child: Text("Kita")),
              ],
              onChanged: (v) => setState(() => kind = v!),
            ),

            const SizedBox(height: 10),

            if (kind == "OTHER")
              TextField(controller: titleCtrl, decoration: const InputDecoration(labelText: "Pavadinimas")),

            TextField(controller: descCtrl, decoration: const InputDecoration(labelText: "Pastabos (nebūtina)")),

            const SizedBox(height: 10),
            ListTile(
              title: Text("Data: ${dateCtrl.text}"),
              trailing: TextButton(onPressed: pickDate, child: const Text("Keisti")),
            ),
            if (kind != "TIRES" && kind != "FUEL")
              TextField(controller: odoCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "Rida (km)")),

            TextField(controller: priceCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "Kaina (EUR)")),

            if (kind == "FUEL")
              TextField(controller: litersCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "Kiek litrų")),

            if (kind == "TIRES") ...[
              const SizedBox(height: 12),
              DropdownButton<String>(
                value: tireType,
                items: const [
                  DropdownMenuItem(value: "SEASONAL", child: Text("Sezoninės")),
                  DropdownMenuItem(value: "ALL_SEASON", child: Text("Universalios")),
                ],
                onChanged: (v) => setState(() => tireType = v!),
              ),
              TextField(
                controller: tireAgeCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: "Padangų amžius (metais)"),
              ),
            ],

            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: loading ? null : save,
              child: loading ? const CircularProgressIndicator() : const Text("Išsaugoti"),
            )
          ],
        ),
      ),
    );
  }
}