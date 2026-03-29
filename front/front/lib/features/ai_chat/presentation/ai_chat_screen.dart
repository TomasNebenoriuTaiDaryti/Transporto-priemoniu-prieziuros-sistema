import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../../garage/data/vehicle_api.dart';
import '../data/ai_chat_api.dart';

class AiChatScreen extends StatefulWidget {
  final VehicleApi vehicleApi;
  final AiChatApi aiChatApi;

  const AiChatScreen({
    super.key,
    required this.vehicleApi,
    required this.aiChatApi,
  });

  @override
  State<AiChatScreen> createState() => _AiChatScreenState();
}

class _AiChatScreenState extends State<AiChatScreen> {
  bool loading = true;
  bool sending = false;
  String? error;

  List<Map<String, dynamic>> vehicles = [];
  int? selectedVehicleId;
  final messageCtrl = TextEditingController();
  final ScrollController _chatScrollController = ScrollController();

  final List<Map<String, String>> messages = [];

  final Map<String, List<String>> quickTopics = const {
    "Išlaidos": [
      "Ką rodo mano automobilio išlaidos?",
      "Ar mano degalų išlaidos atrodo didelės?",
      "Kur išleidžiu daugiausia pinigų šiam automobiliui?",
      "Ar pagal įrašus šis automobilis brangiai išlaikomas?"
    ],
    "Priežiūra": [
      "Ar pagal įrašus greitai reikės keisti tepalus?",
      "Kada dažniausiai keičiami filtrai ir tepalai?",
      "Į ką atkreipti dėmesį pagal mano turimus priežiūros įrašus?",
      "Kokius profilaktinius darbus verta planuoti artimiausiu metu?",
      "Pagal standartus kada dažniausiai keičiami tepalai?",
      "Pagal standartus kada dažniausiai keičiami filtrai?"
    ],
    "Stabdžiai": [
      "Kaip pačiam patikrinti stabdžių būklę?",
      "Ką gali reikšti vibracija stabdant?",
      "Kaip atskirti ar problema gali būti stabdžiuose, ar pakaboje?",
      "Kokie požymiai rodo, kad važiuoti gali būti nesaugu dėl stabdžių?"
    ],
    "Kėbulas": [
      "Kaip įvertinti kėbulo būklę prieš rimtesnį remontą?",
      "Kaip pastebėti rūdis ir koroziją kėbule?",
      "Ką vizualiai tikrinti perkant ar apžiūrint automobilio kėbulą?",
      "Kaip suprasti, ar kėbulas galėjo būti po remonto ar avarijos?"
    ],
    "Pakaba ir vairas": [
      "Kaip patikrinti ar pakaba gali būti susidėvėjusi?",
      "Ką reikėtų pasitikrinti, jei automobilis traukia į šoną?",
      "Ką gali reikšti bildesys važiuojant per nelygumus?",
      "Kaip suprasti, ar problema gali būti vairo sistemoje?"
    ],
    "Padangos": [
      "Kaip suprasti, ar padangos dar geros būklės?",
      "Ką tikrinti vizualiai ant padangų?",
      "Kada padangos gali būti nesaugios važiuoti?",
      "Kaip suprasti, ar padangos dyla netolygiai?"
    ],
    "Variklis": [
      "Ką gali reikšti užsidegusi variklio lemputė?",
      "Kokie požymiai rodo, kad tepalus reikia keisti kuo greičiau?",
      "Ką gali reikšti neįprastas variklio garsas?",
      "Ką pirmiausia pasitikrinti, jei užsidega įspėjamoji lemputė?"
    ],
    "Degalai": [
      "Ką galima suprasti iš mano degalų pylimo istorijos?",
      "Ar mano kuro sąnaudos atrodo normalios?",
      "Kodėl gali padidėti degalų sąnaudos?",
      "Kaip vertinti degalų kainų pokyčius šiame automobilyje?"
    ],
  };

  String? selectedTopic;

  @override
  void initState() {
    super.initState();
    loadVehicles();
  }

  @override
  void dispose() {
    messageCtrl.dispose();
    _chatScrollController.dispose();
    super.dispose();
  }

  Future<void> loadVehicles() async {
    setState(() {
      loading = true;
      error = null;
    });

    try {
      vehicles = await widget.vehicleApi.listAccessibleVehicles();
      if (vehicles.isNotEmpty) {
        selectedVehicleId = (vehicles.first["id"] as num).toInt();
      }
    } catch (e) {
      error = e.toString();
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  void _scrollChatToBottom({bool animated = true}) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_chatScrollController.hasClients) return;
      final target = _chatScrollController.position.maxScrollExtent;
      if (animated) {
        _chatScrollController.animateTo(
          target,
          duration: const Duration(milliseconds: 250),
          curve: Curves.easeOut,
        );
      } else {
        _chatScrollController.jumpTo(target);
      }
    });
  }

  List<Map<String, String>> _historyForApi() {
    return messages
        .map((m) => {
      "role": m["role"] ?? "user",
      "text": m["text"] ?? "",
    })
        .toList();
  }

  Future<void> sendMessage(String text) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty || selectedVehicleId == null || sending) return;

    final historyBeforeNewMessage = _historyForApi();

    setState(() {
      sending = true;
      messages.add({"role": "user", "text": trimmed});
    });

    messageCtrl.clear();
    _scrollChatToBottom();

    try {
      final answer = await widget.aiChatApi.ask(
        vehicleId: selectedVehicleId!,
        message: trimmed,
        history: historyBeforeNewMessage,
      );

      setState(() {
        messages.add({"role": "assistant", "text": answer});
      });
      _scrollChatToBottom();
    } catch (e) {
      setState(() {
        messages.add({
          "role": "assistant",
          "text": "Nepavyko gauti atsakymo. ${e.toString()}",
        });
      });
      _scrollChatToBottom();
    } finally {
      if (mounted) {
        setState(() => sending = false);
      }
    }
  }

  Future<void> pickDashboardImage() async {
    if (selectedVehicleId == null || sending) return;

    final picker = ImagePicker();
    final file = await picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 90,
    );

    if (file == null) return;

    setState(() {
      sending = true;
      messages.add({
        "role": "user",
        "text": "Įkėliau skydelio nuotrauką. Paaiškink, kokios lemputės matomos ir ką jos gali reikšti."
      });
    });
    _scrollChatToBottom();

    try {
      final answer = await widget.aiChatApi.analyzeDashboardImage(
        vehicleId: selectedVehicleId!,
        imagePath: file.path,
      );

      setState(() {
        messages.add({"role": "assistant", "text": answer});
      });
      _scrollChatToBottom();
    } catch (e) {
      setState(() {
        messages.add({
          "role": "assistant",
          "text": "Nepavyko išanalizuoti nuotraukos. ${e.toString()}",
        });
      });
      _scrollChatToBottom();
    } finally {
      if (mounted) {
        setState(() => sending = false);
      }
    }
  }

  Widget _buildTopicTabs() {
    final topics = quickTopics.keys.toList();

    return SizedBox(
      height: 42,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: topics.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (_, i) {
          final topic = topics[i];
          final selected = selectedTopic == topic;

          return ChoiceChip(
            label: Text(topic),
            selected: selected,
            selectedColor: Colors.grey.shade300,
            backgroundColor: Colors.grey.shade100,
            labelStyle: const TextStyle(color: Colors.black87),
            side: BorderSide(color: Colors.grey.shade400),
            onSelected: (_) {
              setState(() {
                selectedTopic = selected ? null : topic;
              });
            },
          );
        },
      ),
    );
  }

  Widget _buildQuestionList() {
    if (selectedTopic == null) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: Colors.grey.shade100,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.grey.shade300),
        ),
        child: const Text(
          "Pasirink temą ir pamatysi siūlomus klausimus.",
          style: TextStyle(fontSize: 12),
        ),
      );
    }

    final questions = quickTopics[selectedTopic] ?? [];

    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: SizedBox(
        height: 132,
        child: Scrollbar(
          child: ListView.separated(
            itemCount: questions.length,
            separatorBuilder: (_, _) => const SizedBox(height: 8),
            itemBuilder: (_, i) {
              final q = questions[i];
              return OutlinedButton(
                style: OutlinedButton.styleFrom(
                  alignment: Alignment.centerLeft,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                  side: BorderSide(color: Colors.grey.shade400),
                  backgroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
                onPressed: sending ? null : () => sendMessage(q),
                child: Text(
                  q,
                  style: const TextStyle(
                    fontSize: 13,
                    color: Colors.black87,
                    height: 1.3,
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }

  Widget _buildChatInfo() {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Row(
        children: [
          Icon(Icons.smart_toy_outlined, size: 16, color: Colors.grey.shade700),
          const SizedBox(width: 8),
          const Expanded(
            child: Text(
              "Kalbiesi su dirbtiniu intelektu.",
              style: TextStyle(
                fontSize: 11.5,
                color: Colors.black87,
                height: 1.2,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildUploadButton() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: OutlinedButton.icon(
        onPressed: sending ? null : pickDashboardImage,
        icon: const Icon(Icons.photo_library_outlined),
        label: const Text("Įkelti skydelio nuotrauką"),
        style: OutlinedButton.styleFrom(
          alignment: Alignment.centerLeft,
          side: BorderSide(color: Colors.grey.shade400),
          backgroundColor: Colors.white,
          foregroundColor: Colors.black87,
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
        ),
      ),
    );
  }

  Widget _buildMessageBubble(Map<String, String> msg) {
    final isUser = msg["role"] == "user";

    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        constraints: const BoxConstraints(maxWidth: 340),
        decoration: BoxDecoration(
          color: isUser ? Colors.grey.shade200 : Colors.white,
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(14),
            topRight: const Radius.circular(14),
            bottomLeft: Radius.circular(isUser ? 14 : 4),
            bottomRight: Radius.circular(isUser ? 4 : 14),
          ),
          border: Border.all(color: Colors.grey.shade300),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.03),
              blurRadius: 4,
              offset: const Offset(0, 1),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment:
          isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
          children: [
            Text(
              isUser ? "Tu" : "AI asistentas",
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: Colors.grey.shade700,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              msg["text"] ?? "",
              style: const TextStyle(
                fontSize: 14,
                height: 1.4,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInputBox() {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
        decoration: BoxDecoration(
          color: Colors.white,
          border: Border(
            top: BorderSide(color: Colors.grey.shade300),
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: messageCtrl,
                enabled: !sending,
                minLines: 1,
                maxLines: 4,
                decoration: InputDecoration(
                  hintText: "Paklausk apie automobilį...",
                  filled: true,
                  fillColor: Colors.grey.shade100,
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 12,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: BorderSide(color: Colors.grey.shade300),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: BorderSide(color: Colors.grey.shade300),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: BorderSide(color: Colors.grey.shade500),
                  ),
                ),
                onSubmitted: sendMessage,
              ),
            ),
            const SizedBox(width: 8),
            Material(
              color: Colors.grey.shade700,
              borderRadius: BorderRadius.circular(14),
              child: InkWell(
                borderRadius: BorderRadius.circular(14),
                onTap: sending ? null : () => sendMessage(messageCtrl.text),
                child: SizedBox(
                  width: 48,
                  height: 48,
                  child: Center(
                    child: sending
                        ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                        : const Icon(Icons.send, color: Colors.white),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTopSection() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 10),
      child: Column(
        children: [
          DropdownButtonFormField<int>(
            initialValue: selectedVehicleId,
            decoration: const InputDecoration(
              labelText: "Pasirink automobilį",
              border: OutlineInputBorder(),
            ),
            items: vehicles.map((v) {
              final id = (v["id"] as num).toInt();
              final label = "${v["make"]} ${v["model"]} (${v["modelYear"]})";
              return DropdownMenuItem<int>(
                value: id,
                child: Text(label),
              );
            }).toList(),
            onChanged: sending
                ? null
                : (v) {
              setState(() {
                selectedVehicleId = v;
                messages.clear();
              });
            },
          ),
          const SizedBox(height: 12),
          _buildTopicTabs(),
          const SizedBox(height: 12),
          _buildQuestionList(),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
          ? Center(child: Text(error!))
          : Column(
        children: [
          _buildTopSection(),
          Expanded(
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 12),
              padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
              decoration: BoxDecoration(
                color: Colors.grey.shade50,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.grey.shade300),
              ),
              child: Column(
                children: [
                  _buildChatInfo(),
                  _buildUploadButton(),
                  Expanded(
                    child: messages.isEmpty
                        ? const Center(
                      child: Padding(
                        padding: EdgeInsets.all(24),
                        child: Text(
                          "Pasirink temą, įkelk skydelio nuotrauką arba užduok savo klausimą apie automobilį, jo išlaidas, dokumentus ar priežiūrą.",
                          textAlign: TextAlign.center,
                        ),
                      ),
                    )
                        : ListView.builder(
                      controller: _chatScrollController,
                      itemCount: messages.length,
                      itemBuilder: (_, i) => _buildMessageBubble(messages[i]),
                    ),
                  ),
                ],
              ),
            ),
          ),
          _buildInputBox(),
        ],
      ),
    );
  }
}