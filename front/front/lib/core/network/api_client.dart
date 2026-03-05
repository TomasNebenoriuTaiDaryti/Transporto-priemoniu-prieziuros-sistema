import 'package:dio/dio.dart';
import '../config/app_config.dart';
import '../storage/token_storage.dart';

class ApiClient {
  final Dio dio;
  ApiClient._(this.dio);

  factory ApiClient(TokenStorage tokenStorage) {
    final dio = Dio(BaseOptions(baseUrl: AppConfig.baseUrl));

    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await tokenStorage.read();
        if (token != null && token.isNotEmpty) {
          options.headers["Authorization"] = "Bearer $token";
        }
        handler.next(options);
      },
    ));

    return ApiClient._(dio);
  }
}