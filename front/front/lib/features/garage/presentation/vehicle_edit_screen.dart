import 'package:flutter/material.dart';
import '../data/vehicle_api.dart';

enum VehicleEditMode { create, edit }

class VehicleEditScreen extends StatefulWidget {
  final VehicleApi api;

  final VehicleEditMode mode;

  final Map<String, dynamic>? initialPreview;
  final Map<String, dynamic>? existingVehicle;

  const VehicleEditScreen({
    super.key,
    required this.api,
    required this.mode,
    this.initialPreview,
    this.existingVehicle,
  });

  @override
  State<VehicleEditScreen> createState() => _VehicleEditScreenState();
}

class _VehicleEditScreenState extends State<VehicleEditScreen> {
  static const fuelOptions = ["PETROL","DIESEL","LPG","CNG","HYBRID","PHEV","EV","OTHER"];

  final _formKey = GlobalKey<FormState>();
  bool _submitted = false;
  bool loading = false;
  String? error;

  String? vin;

  late final TextEditingController makeCtrl;
  late final TextEditingController modelCtrl;
  late final TextEditingController yearCtrl;
  late final TextEditingController ccCtrl;
  String? selectedFuel;
  String? fuelRaw;
  late final TextEditingController odometerCtrl;

  late final TextEditingController transmissionCtrl;
  late final TextEditingController driveCtrl;
  late final TextEditingController bodyCtrl;
  late final TextEditingController doorsCtrl;
  late final TextEditingController seatsCtrl;
  late final TextEditingController co2Ctrl;
  late final TextEditingController plantCtrl;
  late final TextEditingController manufacturerCtrl;

  @override
  void initState() {
    super.initState();

    final data = widget.mode == VehicleEditMode.create
        ? (widget.initialPreview ?? {})
        : (widget.existingVehicle ?? {});

    vin = data["vin"]?.toString();

    makeCtrl = TextEditingController(text: _s(data["make"]));
    modelCtrl = TextEditingController(text: _s(data["model"]));
    yearCtrl = TextEditingController(text: _s(data["modelYear"]));
    ccCtrl = TextEditingController(text: _s(data["engineDisplacementCc"]));

    fuelRaw = _s(data["fuelTypeRaw"]);
    final preselected = data["fuelType"]?.toString();
    selectedFuel = fuelOptions.contains(preselected) ? preselected : null;

    odometerCtrl = TextEditingController(text: _s(data["odometerKm"]));

    transmissionCtrl = TextEditingController(text: _s(data["transmission"]));
    driveCtrl = TextEditingController(text: _s(data["drive"]));
    bodyCtrl = TextEditingController(text: _s(data["body"]));
    doorsCtrl = TextEditingController(text: _s(data["doors"]));
    seatsCtrl = TextEditingController(text: _s(data["seats"]));
    co2Ctrl = TextEditingController(text: _s(data["co2Gkm"]));
    plantCtrl = TextEditingController(text: _s(data["plantCountry"]));
    manufacturerCtrl = TextEditingController(text: _s(data["manufacturer"]));

    WidgetsBinding.instance.addPostFrameCallback((_) {
      setState(() => _submitted = true);
      _formKey.currentState?.validate();
    });

    for (final c in [
      makeCtrl, modelCtrl, yearCtrl, ccCtrl, odometerCtrl,
      transmissionCtrl, driveCtrl, bodyCtrl, doorsCtrl, seatsCtrl,
      co2Ctrl, plantCtrl, manufacturerCtrl
    ]) {
      c.addListener(() {
        setState(() {});
        if (_submitted) _formKey.currentState?.validate();
      });
    }
  }

  @override
  void dispose() {
    makeCtrl.dispose();
    modelCtrl.dispose();
    yearCtrl.dispose();
    ccCtrl.dispose();
    odometerCtrl.dispose();
    transmissionCtrl.dispose();
    driveCtrl.dispose();
    bodyCtrl.dispose();
    doorsCtrl.dispose();
    seatsCtrl.dispose();
    co2Ctrl.dispose();
    plantCtrl.dispose();
    manufacturerCtrl.dispose();
    super.dispose();
  }

  String _s(dynamic v) => v == null ? "" : v.toString();
  int? _toInt(String s) => s.trim().isEmpty ? null : int.tryParse(s.trim());
  double? _toDouble(String s) => s.trim().isEmpty ? null : double.tryParse(s.trim().replaceAll(",", "."));

  String? _reqText(String? v) {
    if (!_submitted) return null;
    if (v == null || v.trim().isEmpty) return "Privalomas laukas";
    return null;
  }

  String? _reqYear(String? v) {
    if (!_submitted) return null;
    if (v == null || v.trim().isEmpty) return "Privalomas laukas";
    final n = int.tryParse(v.trim());
    if (n == null) return "Įvesk skaičių";
    if (n < 1950 || n > 2100) return "Netinkami metai";
    return null;
  }

  String? _reqCc(String? v) {
    if (!_submitted) return null;
    if (v == null || v.trim().isEmpty) return "Privalomas laukas";
    final n = int.tryParse(v.trim());
    if (n == null) return "Įvesk skaičių";
    if (n < 300 || n > 10000) return "Keistas cc (patikrink)";
    return null;
  }

  String? _reqOdo(String? v) {
    if (!_submitted) return null;
    if (v == null || v.trim().isEmpty) return "Privalomas laukas";
    final n = int.tryParse(v.trim());
    if (n == null) return "Įvesk skaičių";
    if (n < 0) return "Negali būti neigiama";
    return null;
  }

  bool get _validNow {
    final makeOk = makeCtrl.text.trim().isNotEmpty;
    final modelOk = modelCtrl.text.trim().isNotEmpty;
    final year = int.tryParse(yearCtrl.text.trim());
    final cc = int.tryParse(ccCtrl.text.trim());
    final odo = int.tryParse(odometerCtrl.text.trim());
    final fuelOk = selectedFuel != null;
    return makeOk && modelOk && year != null && cc != null && odo != null && fuelOk;
  }

  Future<void> _save() async {
    setState(() { _submitted = true; error = null; });
    final ok = _formKey.currentState?.validate() ?? false;
    if (!ok || selectedFuel == null) return;

    setState(() => loading = true);
    try {
      final payload = {
        "make": makeCtrl.text.trim(),
        "model": modelCtrl.text.trim(),
        "modelYear": _toInt(yearCtrl.text),
        "engineDisplacementCc": _toInt(ccCtrl.text),
        "fuelType": selectedFuel,
        "odometerKm": _toInt(odometerCtrl.text),
        "transmission": transmissionCtrl.text.trim().isEmpty ? null : transmissionCtrl.text.trim(),
        "drive": driveCtrl.text.trim().isEmpty ? null : driveCtrl.text.trim(),
        "body": bodyCtrl.text.trim().isEmpty ? null : bodyCtrl.text.trim(),
        "doors": _toInt(doorsCtrl.text),
        "seats": _toInt(seatsCtrl.text),
        "co2Gkm": _toDouble(co2Ctrl.text),
        "plantCountry": plantCtrl.text.trim().isEmpty ? null : plantCtrl.text.trim(),
        "manufacturer": manufacturerCtrl.text.trim().isEmpty ? null : manufacturerCtrl.text.trim(),
      };

      if (widget.mode == VehicleEditMode.create) {
        final createPayload = <String, dynamic>{...payload};
        if (vin != null && vin!.trim().isNotEmpty) {
          createPayload["vin"] = vin;
        } else {
          createPayload["vin"] = null;
        }
        await widget.api.createPersonalVehicle(data: createPayload);
      } else {
        final id = (widget.existingVehicle!["id"] as num).toInt();
        await widget.api.updateVehicle(id, payload);
      }

      if (!mounted) return;
      Navigator.of(context).pop(true);
    } catch (e) {
      setState(() => error = "Nepavyko išsaugoti.");
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  InputDecoration _dec(String label) => const InputDecoration(border: OutlineInputBorder()).copyWith(labelText: label);

  @override
  Widget build(BuildContext context) {
    final title = widget.mode == VehicleEditMode.create ? "Nauja mašina" : "Redaguoti mašiną";
    final vinLabel = (vin == null || vin!.isEmpty) ? "—" : vin!;
    return Scaffold(
      appBar: AppBar(title: Text("$title • VIN: $vinLabel")),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          autovalidateMode: AutovalidateMode.always,
          child: ListView(
            children: [
              const Text("Privalomi laukai pažymėti *", style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 12),

              TextFormField(controller: makeCtrl, decoration: _dec("Gamintojas (Make) *"), validator: _reqText),
              const SizedBox(height: 10),
              TextFormField(controller: modelCtrl, decoration: _dec("Modelis (Model) *"), validator: _reqText),
              const SizedBox(height: 10),
              TextFormField(controller: yearCtrl, keyboardType: TextInputType.number, decoration: _dec("Metai (Model Year) *"), validator: _reqYear),
              const SizedBox(height: 10),
              TextFormField(controller: ccCtrl, keyboardType: TextInputType.number, decoration: _dec("Variklio tūris (cc) *"), validator: _reqCc),
              const SizedBox(height: 10),

              DropdownButtonFormField<String>(
                initialValue: selectedFuel,
                decoration: _dec("Kuro tipas (Fuel Type) *"),
                items: fuelOptions.map((v) => DropdownMenuItem(value: v, child: Text(v))).toList(),
                onChanged: (v) => setState(() => selectedFuel = v),
                validator: (v) {
                  if (!_submitted) return null;
                  if (v == null || v.isEmpty) return "Privalomas laukas";
                  return null;
                },
              ),
              if ((fuelRaw ?? "").trim().isNotEmpty) ...[
                const SizedBox(height: 6),
                Text("Iš VIN: $fuelRaw", style: const TextStyle(fontSize: 12, color: Colors.black54)),
              ],
              const SizedBox(height: 10),

              TextFormField(controller: odometerCtrl, keyboardType: TextInputType.number, decoration: _dec("Rida (km) *"), validator: _reqOdo),

              const SizedBox(height: 14),
              TextFormField(controller: transmissionCtrl, decoration: _dec("Pavarų dėžė (Transmission)")),
              const SizedBox(height: 10),
              TextFormField(controller: driveCtrl, decoration: _dec("Pavara (Drive)")),
              const SizedBox(height: 10),
              TextFormField(controller: bodyCtrl, decoration: _dec("Kėbulas (Body)")),
              const SizedBox(height: 10),
              TextFormField(controller: doorsCtrl, keyboardType: TextInputType.number, decoration: _dec("Durys (Doors)")),
              const SizedBox(height: 10),
              TextFormField(controller: seatsCtrl, keyboardType: TextInputType.number, decoration: _dec("Sėdynės (Seats)")),
              const SizedBox(height: 10),
              TextFormField(controller: co2Ctrl, keyboardType: TextInputType.number, decoration: _dec("CO2 (g/km)")),
              const SizedBox(height: 10),
              TextFormField(controller: plantCtrl, decoration: _dec("Gamyklos šalis (Plant Country)")),
              const SizedBox(height: 10),
              TextFormField(controller: manufacturerCtrl, decoration: _dec("Gamintojas (Manufacturer)")),

              const SizedBox(height: 14),
              if (error != null) Text(error!, style: const TextStyle(color: Colors.red)),
              const SizedBox(height: 6),

              ElevatedButton(
                onPressed: (loading || !_validNow) ? null : _save,
                child: loading
                    ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator())
                    : const Text("Išsaugoti"),
              ),
            ],
          ),
        ),
      ),
    );
  }
}