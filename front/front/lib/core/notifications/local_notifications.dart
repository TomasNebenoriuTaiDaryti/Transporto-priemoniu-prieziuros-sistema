import 'package:flutter/services.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

class LocalNotifications {
  static final FlutterLocalNotificationsPlugin _plugin =
  FlutterLocalNotificationsPlugin();
  static bool _initialized = false;

  static Future<void> init() async {
    if (_initialized) return;

    tz.initializeTimeZones();
    tz.setLocalLocation(tz.getLocation('Europe/Vilnius'));

    const android = AndroidInitializationSettings('@mipmap/ic_launcher');
    const initSettings = InitializationSettings(android: android);
    await _plugin.initialize(initSettings);
    await _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()?.requestNotificationsPermission();
    _initialized = true;
  }

  static NotificationDetails _details() {
    return const NotificationDetails(android: AndroidNotificationDetails('tpps_pranesimai', 'TPPS Pranesimai', channelDescription: 'Masinos pranesimai', importance: Importance.max, priority: Priority.high,),);
  }

  static Future<void> scheduleAt({
    required int id,
    required DateTime when,
    required String title,
    required String body,
  }) async {
    final tzWhen = tz.TZDateTime.from(when.toLocal(), tz.local);
    try {
      await _plugin.zonedSchedule(id, title, body, tzWhen, _details(), androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle, uiLocalNotificationDateInterpretation:
      UILocalNotificationDateInterpretation.absoluteTime, matchDateTimeComponents: null,);
    } on PlatformException catch (e) {
      if (e.code == 'exact_alarms_not_permitted') {
        await _plugin.zonedSchedule(id, title, body, tzWhen, _details(), androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle, uiLocalNotificationDateInterpretation:
        UILocalNotificationDateInterpretation.absoluteTime, matchDateTimeComponents: null,);
        return;
      }
      rethrow;
    }
  }

  static Future<void> cancelAll() async {
    await _plugin.cancelAll();
  }
}