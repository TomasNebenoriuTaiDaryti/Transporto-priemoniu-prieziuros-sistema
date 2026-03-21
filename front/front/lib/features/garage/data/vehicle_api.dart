import 'package:dio/dio.dart';

class VehicleApi {
  final Dio _dio;
  VehicleApi(this._dio);

  Future<Map<String, dynamic>> previewVehicle({required String vin}) async {
    final res = await _dio.post("/api/vehicles/preview", data: {"vin": vin});
    return (res.data as Map).cast<String, dynamic>();
  }

  Future<Map<String, dynamic>> createPersonalVehicle({required Map<String, dynamic> data}) async {
    final res = await _dio.post("/api/vehicles", data: data);
    return (res.data as Map).cast<String, dynamic>();
  }

  Future<List<Map<String, dynamic>>> listAccessibleVehicles() async {
    final res = await _dio.get("/api/vehicles/accessible");
    final list = (res.data as List);
    return list.map((e) => (e as Map).cast<String, dynamic>()).toList();
  }

  Future<List<Map<String, dynamic>>> listMyVehicles() async {
    final res = await _dio.get("/api/vehicles/my");
    final list = (res.data as List);
    return list.map((e) => (e as Map).cast<String, dynamic>()).toList();
  }

  Future<Map<String, dynamic>> getVehicleDetails(int id) async {
    final res = await _dio.get("/api/vehicles/$id");
    return (res.data as Map).cast<String, dynamic>();
  }

  Future<Map<String, dynamic>> updateVehicle(int id, Map<String, dynamic> data) async {
    final res = await _dio.put("/api/vehicles/$id", data: data);
    return (res.data as Map).cast<String, dynamic>();
  }

  Future<void> deleteVehicle(int id) async {
    await _dio.delete("/api/vehicles/$id");
  }

  Future<List<Map<String, dynamic>>> listGroupVehicles(int groupId) async {
    final res = await _dio.get("/api/vehicles/group/$groupId");
    final list = (res.data as List);
    return list.map((e) => (e as Map).cast<String, dynamic>()).toList();
  }

  Future<void> useVehicle(int id) async {
    await _dio.post("/api/vehicles/$id/use");
  }

  Future<void> unuseVehicle() async {
    await _dio.post("/api/vehicles/unuse");
  }

  Future<void> appendOdometerKm(int vehicleId, int deltaKm) async {
    await _dio.post("/api/vehicles/$vehicleId/odometer/append", data: {
      "deltaKm": deltaKm,
    });
  }
}