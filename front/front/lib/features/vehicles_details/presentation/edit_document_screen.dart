import 'dart:convert';
import 'package:flutter/material.dart';
import '../data/docs_api.dart';

class EditDocumentScreen extends StatefulWidget {
  final int vehicleId;
  final Map<String, dynamic> doc;
  final DocsApi docsApi;

  const EditDocumentScreen({
    super.key,
    required this.vehicleId,
    required this.doc,
    required this.docsApi,
  });

  @override
  State<EditDocumentScreen> createState() => _EditDocumentScreenState();
}

class _EditDocumentScreenState extends State<EditDocumentScreen> {
  late final String docId;
  late final String type;
  final titleCtrl = TextEditingController();
  final descCtrl = TextEditingController();
  DateTime? start;
  DateTime? end;
  final priceCtrl = TextEditingController();
  final fineAmountCtrl = TextEditingController();
  final fineReasonCtrl = TextEditingController();
  DateTime? fineDate;

  bool loading = false;

  @override
  void initState() {
    super.initState();
    docId = widget.doc["id"].toString();
    type = widget.doc["type"].toString();
    titleCtrl.text = (widget.doc["title"] ?? "").toString();
    descCtrl.text = (widget.doc["description"] ?? "").toString();
    final s = widget.doc["startDate"];
    final e = widget.doc["endDate"];
    if (s != null) start = DateTime.tryParse(s.toString());
    if (e != null) end = DateTime.tryParse(e.toString());
    final metaRaw = (widget.doc["metaJson"] ?? "{}").toString();
    Map<String, dynamic> meta;
    try { meta = jsonDecode(metaRaw) as Map<String, dynamic>; } catch (_) { meta = {}; }

    if (type == "INSURANCE") {
      priceCtrl.text = (meta["price"] ?? "").toString();
    }
    if (type == "FINE") {
      fineAmountCtrl.text = (meta["amount"] ?? "").toString();
      fineReasonCtrl.text = (meta["reason"] ?? "").toString();
      if (start != null) fineDate = start;
    }
  }

  @override
  void dispose() {
    titleCtrl.dispose();
    descCtrl.dispose();
    priceCtrl.dispose();
    fineAmountCtrl.dispose();
    fineReasonCtrl.dispose();
    super.dispose();
  }

  Future<void> pickStart() async {
    final d = await showDatePicker(context: context, firstDate: DateTime(2000), lastDate: DateTime(2100), initialDate: start ?? DateTime.now());
    if (d != null) setState(() => start = d);
  }

  Future<void> pickEnd() async {
    final d = await showDatePicker(context: context, firstDate: DateTime(2000), lastDate: DateTime(2100), initialDate: end ?? DateTime.now());
    if (d != null) setState(() => end = d);
  }

  Future<void> pickFineDate() async {
    final d = await showDatePicker(context: context, firstDate: DateTime(2000), lastDate: DateTime(2100), initialDate: fineDate ?? DateTime.now());
    if (d != null) setState(() => fineDate = d);
  }

  Future<void> save() async {
    setState(() => loading = true);
    try {
      Map<String, dynamic> meta = {};
      if (type == "INSURANCE") {
        meta["price"] = double.tryParse(priceCtrl.text.trim()) ?? 0;
      }
      if (type == "FINE") {
        meta["amount"] = double.tryParse(fineAmountCtrl.text.trim()) ?? 0;
        meta["reason"] = fineReasonCtrl.text.trim().isEmpty ? "Nenurodyta" : fineReasonCtrl.text.trim();
      }
      DateTime? realStart = start;
      DateTime? realEnd = end;
      if (type == "FINE") {
        realStart = fineDate ?? start;
        realEnd = null;
      }

      await widget.docsApi.update(
        widget.vehicleId,
        docId,
        title: titleCtrl.text.trim().isEmpty ? "Dokumentas" : titleCtrl.text.trim(),
        description: descCtrl.text.trim().isEmpty ? null : descCtrl.text.trim(),
        startDate: realStart,
        endDate: realEnd,
        metaJson: jsonEncode(meta),
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
      appBar: AppBar(title: Text("Redaguoti: $type")),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: ListView(
          children: [
            TextField(controller: titleCtrl, decoration: const InputDecoration(labelText: "Pavadinimas")),
            TextField(controller: descCtrl, decoration: const InputDecoration(labelText: "Aprašymas")),

            const SizedBox(height: 12),

            if (type == "INSURANCE" || type == "INSPECTION") ...[
              ListTile(
                title: Text("Nuo: ${start?.toIso8601String().substring(0,10) ?? '-'}"),
                trailing: TextButton(onPressed: pickStart, child: const Text("Keisti")),
              ),
              ListTile(
                title: Text("Iki: ${end?.toIso8601String().substring(0,10) ?? '-'}"),
                trailing: TextButton(onPressed: pickEnd, child: const Text("Keisti")),
              ),
              if (type == "INSURANCE")
                TextField(controller: priceCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "Kaina (EUR)")),
            ],

            if (type == "FINE") ...[
              ListTile(
                title: Text("Data: ${fineDate?.toIso8601String().substring(0,10) ?? '-'}"),
                trailing: TextButton(onPressed: pickFineDate, child: const Text("Keisti")),
              ),
              TextField(controller: fineAmountCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "Suma (EUR)")),
              TextField(controller: fineReasonCtrl, decoration: const InputDecoration(labelText: "Už ką skirta")),
            ],

            if (type == "OTHER") ...[
              ListTile(
                title: Text("Galioja iki (nebūtina): ${end?.toIso8601String().substring(0,10) ?? '-'}"),
                trailing: TextButton(onPressed: pickEnd, child: const Text("Keisti")),
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