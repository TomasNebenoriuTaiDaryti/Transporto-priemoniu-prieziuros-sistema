import 'package:flutter/material.dart';
import '../data/vehicle_api.dart';
import 'vehicle_edit_screen.dart';

class VinInputScreen extends StatefulWidget {
  final VehicleApi api;
  const VinInputScreen({super.key, required this.api});

  @override
  State<VinInputScreen> createState() => _VinInputScreenState();
}

class _VinInputScreenState extends State<VinInputScreen> {
  final vinCtrl = TextEditingController();
  bool loading = false;
  String? error;

  Future<void> onNext() async {
    setState(() { loading = true; error = null; });
    try {
      final vin = vinCtrl.text.trim().toUpperCase();
      final preview = await widget.api.previewVehicle(vin: vin);

      if (!mounted) return;

      final changed = await Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => VehicleEditScreen(
          api: widget.api,
          mode: VehicleEditMode.create,
          initialPreview: preview,
        ),
      ));

      if (changed == true && mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      setState(() => error = "VIN netinkamas arba jau naudojamas tavo garaže.");
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> onManual() async {
    final preview = {
      "vin": null,
      "make": null,
      "model": null,
      "modelYear": null,
      "engineDisplacementCc": null,
      "transmission": null,
      "fuelTypeRaw": null,
      "fuelType": null,
      "drive": null,
      "body": null,
      "doors": null,
      "seats": null,
      "co2Gkm": null,
      "plantCountry": null,
      "manufacturer": null,
    };

    final changed = await Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => VehicleEditScreen(
        api: widget.api,
        mode: VehicleEditMode.create,
        initialPreview: preview,
      ),
    ));

    if (changed == true && mounted) {
      Navigator.of(context).pop(true);
    }
  }

  @override
  void dispose() {
    vinCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Pridėti automobilį")),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            TextField(
              controller: vinCtrl,
              decoration: const InputDecoration(labelText: "VIN (17 simbolių)"),
              textCapitalization: TextCapitalization.characters,
            ),
            const SizedBox(height: 12),
            if (error != null) Text(error!, style: const TextStyle(color: Colors.red)),
            ElevatedButton(
              onPressed: loading ? null : onNext,
              child: loading ? const CircularProgressIndicator() : const Text("Tęsti su VIN"),
            ),
            const SizedBox(height: 10),
            OutlinedButton(
              onPressed: loading ? null : onManual,
              child: const Text("Neturiu VIN – suvesiu ranka"),
            ),
          ],
        ),
      ),
    );
  }
}