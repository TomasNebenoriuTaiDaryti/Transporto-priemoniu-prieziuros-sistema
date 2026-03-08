import 'package:dio/dio.dart';

class GroupsApi {
  final Dio _dio;
  GroupsApi(this._dio);

  Future<List<Map<String, dynamic>>> myGroups() async {
    final res = await _dio.get("/api/groups");
    final list = (res.data as List);
    return list.map((e) => (e as Map).cast<String, dynamic>()).toList();
  }

  Future<Map<String, dynamic>> createGroup(String name) async {
    final res = await _dio.post("/api/groups", data: {"name": name});
    return (res.data as Map).cast<String, dynamic>();
  }

  Future<Map<String, dynamic>> renameGroup(int groupId, String name) async {
    final res = await _dio.patch("/api/groups/$groupId", data: {"name": name});
    return (res.data as Map).cast<String, dynamic>();
  }

  Future<String> invite(int groupId, String email) async {
    final res = await _dio.post("/api/groups/$groupId/invite", data: {"email": email});
    return (res.data as Map)["message"].toString();
  }

  Future<List<Map<String, dynamic>>> members(int groupId) async {
    final res = await _dio.get("/api/groups/$groupId/members");
    final list = (res.data as List);
    return list.map((e) => (e as Map).cast<String, dynamic>()).toList();
  }

  Future<void> removeMember(int groupId, int memberUserId) async {
    await _dio.delete("/api/groups/$groupId/members/$memberUserId");
  }

  Future<void> leaveGroup(int groupId) async {
    await _dio.post("/api/groups/$groupId/leave");
  }

  Future<void> deleteGroup(int groupId) async {
    await _dio.delete("/api/groups/$groupId");
  }

  Future<void> addVehicleToGroup(int groupId, int vehicleId) async {
    await _dio.post("/api/groups/$groupId/vehicles/$vehicleId");
  }

  Future<void> removeVehicleFromGroup(int groupId, int vehicleId) async {
    await _dio.delete("/api/groups/$groupId/vehicles/$vehicleId");
  }
}