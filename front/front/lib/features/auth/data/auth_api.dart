import 'package:dio/dio.dart';
import 'models/auth_response.dart';

class AuthApi {
  final Dio _dio;
  AuthApi(this._dio);

  Future<AuthResponse> register(String email, String password) async {
    final res = await _dio.post(
      "/api/auth/register",
      data: {"email": email, "password": password},
    );
    return AuthResponse.fromJson(res.data);
  }

  Future<AuthResponse> login(String email, String password) async {
    final res = await _dio.post(
      "/api/auth/login",
      data: {"email": email, "password": password},
    );
    return AuthResponse.fromJson(res.data);
  }
}