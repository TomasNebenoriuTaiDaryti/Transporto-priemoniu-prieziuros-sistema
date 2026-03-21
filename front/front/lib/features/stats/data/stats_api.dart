import 'package:dio/dio.dart';

class StatsApi {
  final Dio _dio;
  StatsApi(this._dio);

  Future<Map<String, dynamic>> vehicleStats(int vehicleId) async {
    final res = await _dio.get("/api/stats/vehicles/$vehicleId");
    return (res.data as Map).cast<String, dynamic>();
  }
}