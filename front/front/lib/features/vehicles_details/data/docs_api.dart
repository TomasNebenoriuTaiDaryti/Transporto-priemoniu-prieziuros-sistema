import 'package:dio/dio.dart';

class DocsApi {
  final Dio _dio;
  DocsApi(this._dio);

  Future<List<Map<String, dynamic>>> list(int vehicleId) async {
    final res = await _dio.get("/api/vehicles/$vehicleId/documents");
    final list = (res.data as List);
    return list.map((e) => (e as Map).cast<String, dynamic>()).toList();
  }

  Future<void> addInsurance(int vehicleId, DateTime start, DateTime end, double price) async {
    await _dio.post("/api/vehicles/$vehicleId/documents/insurance", data: {
      "startDate": _d(start),
      "endDate": _d(end),
      "price": price,
    });
  }

  Future<void> addInspection(int vehicleId, DateTime start, DateTime end) async {
    await _dio.post("/api/vehicles/$vehicleId/documents/inspection", data: {
      "startDate": _d(start),
      "endDate": _d(end),
    });
  }

  Future<void> addFine(int vehicleId, DateTime date, double amount, String reason) async {
    await _dio.post("/api/vehicles/$vehicleId/documents/fine", data: {
      "date": _d(date),
      "amount": amount,
      "reason": reason,
    });
  }

  Future<void> addOther(int vehicleId, String title, String? description, DateTime? end) async {
    await _dio.post("/api/vehicles/$vehicleId/documents/other", data: {
      "title": title,
      "description": description,
      "endDate": end == null ? null : _d(end),
    });
  }

  Future<void> update(
      int vehicleId,
      String docId, {
        required String title,
        String? description,
        DateTime? startDate,
        DateTime? endDate,
        required String metaJson,
      }) async {
    await _dio.put("/api/vehicles/$vehicleId/documents/$docId", data: {
      "title": title,
      "description": description,
      "startDate": startDate == null ? null : _d(startDate),
      "endDate": endDate == null ? null : _d(endDate),
      "metaJson": metaJson,
    });
  }

  Future<void> delete(int vehicleId, String docId) async {
    await _dio.delete("/api/vehicles/$vehicleId/documents/$docId");
  }

  String _d(DateTime dt) => dt.toIso8601String().substring(0, 10);
}