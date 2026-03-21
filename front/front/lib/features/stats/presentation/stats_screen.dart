import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../garage/data/vehicle_api.dart';
import '../data/stats_api.dart';

class StatsScreen extends StatefulWidget {
  final VehicleApi vehicleApi;
  final StatsApi statsApi;

  const StatsScreen({
    super.key,
    required this.vehicleApi,
    required this.statsApi,
  });

  @override
  State<StatsScreen> createState() => _StatsScreenState();
}

class _StatsScreenState extends State<StatsScreen> {
  bool loading = true;
  String? error;

  List<Map<String, dynamic>> vehicles = [];
  int? selectedVehicleId;
  Map<String, dynamic>? stats;

  Future<void> loadVehicles() async {
    vehicles = await widget.vehicleApi.listAccessibleVehicles();
    if (vehicles.isNotEmpty && selectedVehicleId == null) {
      selectedVehicleId = (vehicles.first["id"] as num).toInt();
    }
  }

  Future<void> loadStats() async {
    if (selectedVehicleId == null) return;
    setState(() {
      loading = true;
      error = null;
    });

    try {
      stats = await widget.statsApi.vehicleStats(selectedVehicleId!);
    } catch (e) {
      error = e.toString();
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    () async {
      try {
        await loadVehicles();
        await loadStats();
      } catch (e) {
        setState(() {
          loading = false;
          error = e.toString();
        });
      }
    }();
  }

  String _shortDate(String raw) {
    if (raw.length >= 10) {
      return raw.substring(5, 10);
    }
    return raw;
  }

  String _shortMonth(String raw) {
    if (raw.length >= 7) {
      final year = raw.substring(2, 4);
      final month = raw.substring(5, 7);
      return "$month/$year";
    }
    return raw;
  }

  List<Map<String, dynamic>> _sortedByMonth(List raw) {
    final list = raw.map((e) => (e as Map).cast<String, dynamic>()).toList();
    list.sort((a, b) => (a["month"] ?? "").toString().compareTo((b["month"] ?? "").toString()));
    return list;
  }

  List<Map<String, dynamic>> _sortedByDate(List raw) {
    final list = raw.map((e) => (e as Map).cast<String, dynamic>()).toList();
    list.sort((a, b) => (a["date"] ?? "").toString().compareTo((b["date"] ?? "").toString()));
    return list;
  }

  double _labelInterval(int length) {
    if (length <= 6) return 1;
    if (length <= 12) return 2;
    if (length <= 24) return 3;
    return 4;
  }

  @override
  Widget build(BuildContext context) {
    final monthly = _sortedByMonth((stats?["monthlyCosts"] as List?) ?? []);
    final byType = ((stats?["expenseByType"] as List?) ?? []).map((e) => (e as Map).cast<String, dynamic>()).toList();
    final fuel = _sortedByDate((stats?["fuelHistory"] as List?) ?? []);
    final monthlyFuelAvg = _sortedByMonth((stats?["monthlyFuelAverages"] as List?) ?? []);

    return Scaffold(
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
          ? Center(child: Text(error!))
          : ListView(
        padding: const EdgeInsets.all(16),
        children: [
          DropdownButtonFormField<int>(
            value: selectedVehicleId,
            decoration: const InputDecoration(labelText: "Pasirink automobilį"),
            items: vehicles.map((v) {
              final id = (v["id"] as num).toInt();
              final label = "${v["make"]} ${v["model"]} (${v["modelYear"]})";
              return DropdownMenuItem(value: id, child: Text(label));
            }).toList(),
            onChanged: (v) async {
              setState(() => selectedVehicleId = v);
              await loadStats();
            },
          ),
          const SizedBox(height: 20),

          const Text("1. Išlaidos pagal mėnesį", style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          SizedBox(
            height: 220,
            child: monthly.isEmpty
                ? const Center(child: Text("Nėra duomenų"))
                : BarChart(
              BarChartData(
                titlesData: FlTitlesData(
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 36,
                      interval: _labelInterval(monthly.length),
                      getTitlesWidget: (value, meta) {
                        final i = value.toInt();
                        if (i < 0 || i >= monthly.length) return const SizedBox();
                        return Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Text(
                            _shortMonth((monthly[i]["month"] ?? "").toString()),
                            style: const TextStyle(fontSize: 10),
                          ),
                        );
                      },
                    ),
                  ),
                  topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                ),
                barGroups: List.generate(monthly.length, (i) {
                  final amount = double.tryParse(monthly[i]["amount"].toString()) ?? 0;
                  return BarChartGroupData(
                    x: i,
                    barRods: [BarChartRodData(toY: amount)],
                  );
                }),
              ),
            ),
          ),

          const SizedBox(height: 24),
          const Text("2. Išlaidos pagal tipą", style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          SizedBox(
            height: 240,
            child: byType.isEmpty
                ? const Center(child: Text("Nėra duomenų"))
                : PieChart(
              PieChartData(
                sections: List.generate(byType.length, (i) {
                  final amount = double.tryParse(byType[i]["amount"].toString()) ?? 0;
                  return PieChartSectionData(
                    value: amount,
                    title: "${byType[i]["type"]}\n${amount.toStringAsFixed(0)}€",
                    radius: 90,
                  );
                }),
              ),
            ),
          ),

          const SizedBox(height: 24),
          const Text("3. Degalų istorija (€ / l)", style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          SizedBox(
            height: 240,
            child: fuel.isEmpty
                ? const Center(child: Text("Nėra degalų įrašų"))
                : LineChart(
              LineChartData(
                minX: 0,
                maxX: (fuel.length - 1).toDouble(),
                titlesData: FlTitlesData(
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 36,
                      interval: _labelInterval(fuel.length),
                      getTitlesWidget: (value, meta) {
                        final i = value.toInt();
                        if (i < 0 || i >= fuel.length) return const SizedBox();
                        return Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Text(
                            _shortDate((fuel[i]["date"] ?? "").toString()),
                            style: const TextStyle(fontSize: 10),
                          ),
                        );
                      },
                    ),
                  ),
                  topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                ),
                lineBarsData: [
                  LineChartBarData(
                    spots: List.generate(fuel.length, (i) {
                      final ppl = double.tryParse(fuel[i]["pricePerLiter"].toString()) ?? 0;
                      return FlSpot(i.toDouble(), ppl);
                    }),
                    isCurved: true,
                    dotData: const FlDotData(show: true),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          ...fuel.map((f) => ListTile(
            dense: true,
            title: Text("${f["date"]} • ${f["liters"]} l"),
            subtitle: Text("Kaina: ${f["cost"]} € • €/l: ${f["pricePerLiter"]}"),
          )),

          const SizedBox(height: 24),
          const Text("4. Vidutinė kuro kaina per mėnesį", style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          SizedBox(
            height: 240,
            child: monthlyFuelAvg.isEmpty
                ? const Center(child: Text("Nėra degalų duomenų"))
                : BarChart(
              BarChartData(
                titlesData: FlTitlesData(
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 36,
                      interval: _labelInterval(monthlyFuelAvg.length),
                      getTitlesWidget: (value, meta) {
                        final i = value.toInt();
                        if (i < 0 || i >= monthlyFuelAvg.length) {
                          return const SizedBox();
                        }
                        return Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Text(
                            _shortMonth((monthlyFuelAvg[i]["month"] ?? "").toString()),
                            style: const TextStyle(fontSize: 10),
                          ),
                        );
                      },
                    ),
                  ),
                  topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                ),
                barGroups: List.generate(monthlyFuelAvg.length, (i) {
                  final avg = double.tryParse(monthlyFuelAvg[i]["avgPricePerLiter"].toString()) ?? 0;
                  return BarChartGroupData(
                    x: i,
                    barRods: [BarChartRodData(toY: avg)],
                  );
                }),
              ),
            ),
          ),
        ],
      ),
    );
  }
}