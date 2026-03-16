import 'package:flutter/material.dart';
import '../data/docs_api.dart';

class AddDocumentScreen extends StatefulWidget {
  final int vehicleId;
  final DocsApi docsApi;

  const AddDocumentScreen({super.key, required this.vehicleId, required this.docsApi});

  @override
  State<AddDocumentScreen> createState() => _AddDocumentScreenState();
}

class _AddDocumentScreenState extends State<AddDocumentScreen> {
  String type = "INSURANCE";
  final titleCtrl = TextEditingController();
  final descCtrl = TextEditingController();
  final priceCtrl = TextEditingController(text: "0");
  final fineAmountCtrl = TextEditingController();
  final fineReasonCtrl = TextEditingController();
  late DateTime start;
  late DateTime end;
  late DateTime fineDate;
  bool loading = false;

  @override
  void initState() {
    super.initState();
    _setDefaultsForType(type);
  }

  void _setDefaultsForType(String t) {
    final now = DateTime.now();
    if (t == "INSURANCE") {
      start = DateTime(now.year, now.month, now.day);
      end = DateTime(now.year + 1, now.month, now.day);
      priceCtrl.text = priceCtrl.text.isEmpty ? "0" : priceCtrl.text;
    } else if (t == "INSPECTION") {
      start = DateTime(now.year, now.month, now.day);
      end = DateTime(now.year + 2, now.month, now.day);
    } else if (t == "FINE") {
      fineDate = DateTime(now.year, now.month, now.day);
    } else {
      start = DateTime(now.year, now.month, now.day);
      end = DateTime(now.year, now.month, now.day);
    }
  }

  Future<void> pickStart() async {
    final d = await showDatePicker(context: context, firstDate: DateTime(2000), lastDate: DateTime(2100), initialDate: start);
    if (d != null) setState(() => start = d);
  }

  Future<void> pickEnd() async {
    final d = await showDatePicker(context: context, firstDate: DateTime(2000), lastDate: DateTime(2100), initialDate: end);
    if (d != null) setState(() => end = d);
  }

  Future<void> pickFineDate() async {
    final d = await showDatePicker(context: context, firstDate: DateTime(2000), lastDate: DateTime(2100), initialDate: fineDate);
    if (d != null) setState(() => fineDate = d);
  }

  Future<void> save() async {
    setState(() => loading = true);
    try {
      if (type == "INSURANCE") {
        await widget.docsApi.addInsurance(
          widget.vehicleId,
          start,
          end,
          double.tryParse(priceCtrl.text.trim()) ?? 0,
        );
      } else if (type == "INSPECTION") {
        await widget.docsApi.addInspection(widget.vehicleId, start, end);
      } else if (type == "FINE") {
        await widget.docsApi.addFine(
          widget.vehicleId,
          fineDate,
          double.tryParse(fineAmountCtrl.text.trim()) ?? 0,
          fineReasonCtrl.text.trim().isEmpty ? "Nenurodyta" : fineReasonCtrl.text.trim(),
        );
      } else {
        await widget.docsApi.addOther(
          widget.vehicleId,
          titleCtrl.text.trim().isEmpty ? "Kitas dokumentas" : titleCtrl.text.trim(),
          descCtrl.text.trim().isEmpty ? null : descCtrl.text.trim(),
          null,
        );
      }

      if (!mounted) return;
      Navigator.of(context).pop(true);
    } finally {
      if (mounted) setState(() => loading = false);
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Pridėti dokumentą")),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: ListView(
          children: [
            DropdownButton<String>(
              value: type,
              items: const [
                DropdownMenuItem(value: "INSURANCE", child: Text("Draudimas")),
                DropdownMenuItem(value: "INSPECTION", child: Text("Techninė apžiūra")),
                DropdownMenuItem(value: "FINE", child: Text("Bauda")),
                DropdownMenuItem(value: "OTHER", child: Text("Kita")),
              ],
              onChanged: (v) {
                setState(() {
                  type = v!;
                  _setDefaultsForType(type);
                });
              },
            ),
            const SizedBox(height: 12),

            if (type == "INSURANCE" || type == "INSPECTION") ...[
              ListTile(
                title: Text("Nuo: ${start.toIso8601String().substring(0,10)}"),
                trailing: TextButton(onPressed: pickStart, child: const Text("Keisti")),
              ),
              ListTile(
                title: Text("Iki: ${end.toIso8601String().substring(0,10)}"),
                trailing: TextButton(onPressed: pickEnd, child: const Text("Keisti")),
              ),
              if (type == "INSURANCE")
                TextField(controller: priceCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "Kaina (EUR)")),
            ],

            if (type == "FINE") ...[
              ListTile(
                title: Text("Data: ${fineDate.toIso8601String().substring(0,10)}"),
                trailing: TextButton(onPressed: pickFineDate, child: const Text("Keisti")),
              ),
              TextField(controller: fineAmountCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "Suma (EUR)")),
              TextField(controller: fineReasonCtrl, decoration: const InputDecoration(labelText: "Už ką skirta")),
            ],

            if (type == "OTHER") ...[
              TextField(controller: titleCtrl, decoration: const InputDecoration(labelText: "Pavadinimas")),
              TextField(controller: descCtrl, decoration: const InputDecoration(labelText: "Aprašymas")),
              const SizedBox(height: 6),
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