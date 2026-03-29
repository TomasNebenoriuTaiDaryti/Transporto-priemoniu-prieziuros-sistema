import 'package:dio/dio.dart';

class AiChatApi {
  final Dio _dio;
  AiChatApi(this._dio);

  Future<String> ask({
    required int vehicleId,
    required String message,
    required List<Map<String, String>> history,
  }) async {
    final res = await _dio.post(
      "/api/ai/chat",
      data: {
        "vehicleId": vehicleId,
        "message": message,
        "history": history.map((m) => {
          "role": m["role"] ?? "user",
          "text": m["text"] ?? "",
        }).toList(),
      },
    );

    final map = (res.data as Map).cast<String, dynamic>();
    return (map["answer"] ?? "").toString();
  }

  Future<String> analyzeDashboardImage({
    required int vehicleId,
    required String imagePath,
  }) async {
    final form = FormData.fromMap({
      "vehicleId": vehicleId.toString(),
      "image": await MultipartFile.fromFile(imagePath),
    });

    final res = await _dio.post(
      "/api/ai/dashboard-image",
      data: form,
    );

    final map = (res.data as Map).cast<String, dynamic>();
    return (map["answer"] ?? "").toString();
  }
}