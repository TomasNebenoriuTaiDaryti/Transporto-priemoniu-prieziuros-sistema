import '../../../core/storage/token_storage.dart';
import 'auth_api.dart';

class AuthRepo {
  final AuthApi _api;
  final TokenStorage _tokenStorage;

  AuthRepo(this._api, this._tokenStorage);

  Future<void> register(String email, String password) async {
    final resp = await _api.register(email, password);
    await _tokenStorage.save(resp.accessToken);
  }

  Future<void> login(String email, String password) async {
    final resp = await _api.login(email, password);
    await _tokenStorage.save(resp.accessToken);
  }

  Future<void> logout() => _tokenStorage.clear();

  Future<bool> isLoggedIn() async {
    final t = await _tokenStorage.read();
    return t != null && t.isNotEmpty;
  }
}