import 'package:dio/dio.dart';


class RemindersApi {
  final Dio _dio;
  RemindersApi(this._dio);

  Future<List<Map<String, dynamic>>> list(int vehicleId) async {
    final res = await _dio.get("/api/vehicles/$vehicleId/reminders");
    final list = (res.data as List);
    return list.map((e) => (e as Map).cast<String, dynamic>()).toList();
  }
}