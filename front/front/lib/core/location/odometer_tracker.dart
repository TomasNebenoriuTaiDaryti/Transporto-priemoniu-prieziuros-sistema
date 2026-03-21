import 'dart:async';
import 'package:geolocator/geolocator.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../features/garage/data/vehicle_api.dart';

class OdometerTracker {
  OdometerTracker._();
  static final OdometerTracker instance = OdometerTracker._();
  StreamSubscription<Position>? _sub;
  int? _vehicleId;
  double _pendingMeters = 0;
  double? _lastLat;
  double? _lastLng;
  bool _running = false;
  static const _kVehicleId = "odometer_vehicle_id";
  static const _kPendingMeters = "odometer_pending_meters";
  static const _kLastLat = "odometer_last_lat";
  static const _kLastLng = "odometer_last_lng";

  Future<void> start({required int vehicleId, required VehicleApi api}) async {
    if (_running && _vehicleId == vehicleId) return;

    await stop();
    _vehicleId = vehicleId;
    _running = true;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt(_kVehicleId, vehicleId);

    final enabled = await Geolocator.isLocationServiceEnabled();
    if (!enabled) {
      _running = false;
      throw Exception("Įjunk Location (GPS) telefone");
    }

    var perm = await Geolocator.checkPermission();

    if (perm == LocationPermission.denied) {
      perm = await Geolocator.requestPermission();
    }

    if (perm == LocationPermission.denied || perm == LocationPermission.deniedForever) {
      _running = false;
      throw Exception("Nėra leidimo naudoti GPS");
    }
    _pendingMeters = prefs.getDouble(_kPendingMeters) ?? 0.0;
    _lastLat = prefs.getDouble(_kLastLat);
    _lastLng = prefs.getDouble(_kLastLng);
    const settings = LocationSettings(
      accuracy: LocationAccuracy.high,
      distanceFilter: 10,
    );

    _sub = Geolocator.getPositionStream(locationSettings: settings).listen(
          (pos) async {
        if (!_running || _vehicleId == null) return;

        if (_lastLat == null || _lastLng == null) {
          _lastLat = pos.latitude;
          _lastLng = pos.longitude;
          await _persistLast(_lastLat!, _lastLng!);
          return;
        }

        if (pos.speed < 0.5) {
          _lastLat = pos.latitude;
          _lastLng = pos.longitude;
          await _persistLast(_lastLat!, _lastLng!);
          return;
        }

        final dist = Geolocator.distanceBetween(
          _lastLat!,
          _lastLng!,
          pos.latitude,
          pos.longitude,
        );

        if (dist.isNaN || dist <= 0 || dist > 300) {
          _lastLat = pos.latitude;
          _lastLng = pos.longitude;
          await _persistLast(_lastLat!, _lastLng!);
          return;
        }
        _pendingMeters += dist;
        //print("GPS dist=${dist.toStringAsFixed(1)}m pending=${_pendingMeters.toStringAsFixed(1)}m speed=${pos.speed}");
        _lastLat = pos.latitude;
        _lastLng = pos.longitude;
        await _persistPending(_pendingMeters);
        await _persistLast(_lastLat!, _lastLng!);
        final deltaKm = (_pendingMeters / 1000).floor();

        if (deltaKm >= 1) {
          _pendingMeters -= deltaKm * 1000;
          await _persistPending(_pendingMeters);

          try {
            await api.appendOdometerKm(_vehicleId!, deltaKm);
          } catch (_) {
            _pendingMeters += deltaKm * 1000;
            await _persistPending(_pendingMeters);
          }
        }
      },
      onError: (_) {},
    );
  }

  Future<void> stop() async {
    _running = false;
    _vehicleId = null;
    await _sub?.cancel();
    _sub = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_kVehicleId);
    await prefs.remove(_kPendingMeters);
    await prefs.remove(_kLastLat);
    await prefs.remove(_kLastLng);
    _pendingMeters = 0;
    _lastLat = null;
    _lastLng = null;
  }

  Future<void> _persistPending(double meters) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble(_kPendingMeters, meters);
  }

  Future<void> _persistLast(double lat, double lng) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble(_kLastLat, lat);
    await prefs.setDouble(_kLastLng, lng);
  }
}