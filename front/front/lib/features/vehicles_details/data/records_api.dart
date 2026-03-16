import 'package:dio/dio.dart';

class RecordsApi {
  final Dio _dio;
  RecordsApi(this._dio);

  Future<List<Map<String, dynamic>>> list(int vehicleId) async {
    final res = await _dio.get("/api/vehicles/$vehicleId/records");
    final list = (res.data as List);
    return list.map((e) => (e as Map).cast<String, dynamic>()).toList();
  }

  Future<void> add(
      int vehicleId,
      String kind, {
        String? title,
        String? description,
        String? performedAt,
        int? odometerKm,
        double? totalCost,
        String? currency,
        String? tireType,
        int? tireAgeYears,
      }) async {
    await _dio.post("/api/vehicles/$vehicleId/records", data: {
      "kind": kind,
      "title": title,
      "description": description,
      "performedAt": performedAt,
      "odometerKm": odometerKm,
      "totalCost": totalCost ?? 0,
      "currency": currency ?? "EUR",
      "tireType": tireType,
      "tireAgeYears": tireAgeYears,
    });
  }

  Future<void> update(
      int vehicleId,
      int recordId, {
        required String kind,
        String? title,
        String? description,
        required String performedAt,
        required int odometerKm,
        required double totalCost,
        required String currency,
        String? tireType,
        int? tireAgeYears,
      }) async {
    await _dio.put("/api/vehicles/$vehicleId/records/$recordId", data: {
      "kind": kind,
      "title": title,
      "description": description,
      "performedAt": performedAt,
      "odometerKm": odometerKm,
      "totalCost": totalCost,
      "currency": currency,
      "tireType": tireType,
      "tireAgeYears": tireAgeYears,
    });
  }

  Future<void> delete(int vehicleId, int recordId) async {
    await _dio.delete("/api/vehicles/$vehicleId/records/$recordId");
  }
}