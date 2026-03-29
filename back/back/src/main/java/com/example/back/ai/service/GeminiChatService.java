package com.example.back.ai.service;

import com.example.back.ai.dto.AiChatResponse;
import com.example.back.ai.dto.AiImageAnalysisResponse;
import com.example.back.ai.dto.HistoryMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GeminiChatService {
    private final VehicleAiContextService vehicleAiContextService;
    private final RestClient restClient;

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.model}")
    private String model;

    public GeminiChatService(VehicleAiContextService vehicleAiContextService, @Value("${ai.gemini.base-url}") String baseUrl) {
        this.vehicleAiContextService = vehicleAiContextService;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public AiChatResponse ask(Long userId, Long vehicleId, String userQuestion, List<HistoryMessage> history) {
        String context = vehicleAiContextService.buildSanitizedContext(userId, vehicleId);

        String systemInstruction = """
                Tu esi transporto priemonės priežiūros AI asistentas kalbantis lietuvių kalba.
                Naudok tik pateiktą automobilio kontekstą ir bendras automobilių priežiūros žinias.
                NENAUDOK ir nebandyk atspėti asmens tapatybės.
                Neminėk, kad turi VIN ar asmens duomenų, nes jų nėra.

                Bendros atsakymo taisyklės:
                - Atsakyk glaustai, aiškiai, praktiškai ir TIK paprastu tekstu.
                - NENAUDOK markdown formatavimo.
                - NENAUDOK žvaigždučių, antraščių, kodų blokų ar lentelių.
                - Jei reikia sąrašo, rašyk paprastas eilutes su brūkšniu.
                - Atsižvelk į ankstesnes šio pokalbio žinutes.
                - Jei duomenų nepakanka, taip ir pasakyk.

                Jei klausimas apie išlaidas:
                - remkis tik pateiktomis sumomis, dokumentais ir įrašais
                - nekurk neegzistuojančių skaičių
                - jei įmanoma, nurodyk kas atrodo brangiausia arba dažniausia

                Jei klausimas apie tai, kada paprastai reikia keisti tepalus, filtrus, stabdžius, padangas ar kitus eksploatacinius dalykus:
                - jei turi automobilio duomenų ar įrašų, remkis jais
                - jei tikslių duomenų trūksta, pateik apytikslius intervalus pagal bendrą praktiką
                - aiškiai pasakyk, kad tai yra bendri intervalai, kurie gali skirtis pagal gamintoją, variklį ir eksploatacijos sąlygas
                - pavyzdžiai, kuriuos gali naudoti kai trūksta tikslių duomenų:
                  tepalai dažnai kas 10 000-15 000 km arba apie 1 kartą per metus
                  tepalo filtras dažniausiai keičiamas kartu su tepalais
                  oro filtras dažnai kas 15 000-30 000 km
                  salono filtras dažnai kas 10 000-20 000 km arba kartą per metus
                  stabdžių kaladėlės dažnai apie 20 000-50 000 km
                  stabdžių diskai dažnai apie 50 000-100 000 km
                  padangų būklė vertinama pagal protektorių, senėjimą ir netolygų dėvėjimąsi; senesnės nei apie 5-6 metų padangos dažnai jau reikalauja didesnio dėmesio
                - jei automobilio požymiai rodo didesnę riziką, rekomenduok tikrinti anksčiau

                Jei klausimas apie automobilio dalį ar problemą, pvz. stabdžius, kėbulą, pakabą, vairą, variklį,
                filtrus, tepalus, padangas, išmetimą, akumuliatorių, aušinimą, lemputes prietaisų skydelyje,
                arba bendrus automobilio simptomus, tada atsakymą struktūruok taip:
                - Trumpai paaiškink, kas tai yra / ką tai gali reikšti
                - Ką žmogus gali įvertinti vizualiai
                - Ką žmogus gali įvertinti fiziškai
                - Ką žmogus gali įvertinti klausant arba važiuojant
                - Kokie požymiai rodo, kad galima dar stebėti
                - Kokie požymiai rodo, kad reikia kuo greičiau į servisą
                - Kokie požymiai reiškia, kad gali būti nesaugu toliau važiuoti

                Jei klausimas apie dashboard lemputes:
                - paaiškink ką dažniausiai reiškia
                - nurodyk ką pirmiausia pasitikrinti

                Jei klausimas apie stabdžius:
                - būtinai paminėk pedalą, stabdymo efektyvumą, vibraciją, cypimą, metalo garsą, traukimą į šoną, skysčio lygį

                Jei klausimas apie kėbulą:
                - būtinai paminėk rūdis, dažų pažeidimus, tarpų nelygumą, koroziją slenksčiuose/arkose/dugne,
                  drėgmės patekimo požymius, po remonto likusius nelygumus

                Neduok pavojingų ar pernelyg užtikrintų diagnozių. Kalbėk kaip pagalbininkas, ne kaip garantuojantis serviso meistras.
                """;

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", """
                SISTEMOS TAISYKLĖS:
                %s

                AUTOMOBILIO KONTEKSTAS:
                %s
                """.formatted(systemInstruction, context)));

        if (history != null && !history.isEmpty()) {
            StringBuilder historyText = new StringBuilder();
            historyText.append("ANKSTESNIS POKALBIS:\n");
            for (HistoryMessage msg : history) {
                String role = "assistant".equalsIgnoreCase(msg.role()) ? "assistant" : "user";
                historyText.append(role).append(": ").append(msg.text()).append("\n");
            }
            parts.add(Map.of("text", historyText.toString()));
        }

        parts.add(Map.of("text", "NAUJAS VARTOTOJO KLAUSIMAS:\n" + userQuestion));
        Map<String, Object> payload = Map.of("contents", List.of(Map.of("parts", parts)));
        Map response = restClient.post().uri(uriBuilder -> uriBuilder.path("/v1beta/models/{model}:generateContent").queryParam("key", apiKey).build(model)).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
        String answer = sanitizePlainText(extractText(response));

        return new AiChatResponse(answer == null || answer.isBlank() ? "Nepavyko gauti atsakymo. Bandyk paklausti kitaip." : answer);
    }

    public AiImageAnalysisResponse analyzeDashboardImage(Long userId, Long vehicleId, byte[] imageBytes, String mimeType
    ) {
        String context = vehicleAiContextService.buildSanitizedContext(userId, vehicleId);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        String prompt = """
                Tu esi transporto priemonės priežiūros AI asistentas kalbantis lietuvių kalba.

                Užduotis:
                - pažiūrėk į automobilio skydelio nuotrauką
                - įvardyk, kokios lemputės ar perspėjimai tikėtina matomi
                - jei neaišku, taip ir pasakyk
                - remkis matoma nuotrauka ir pateiktu automobilio kontekstu
                - nebandyk identifikuoti žmogaus
                - neieškok VIN ar asmens duomenų

                Atsakymo formatas:
                - Kokios lemputės ar perspėjimai matosi
                - Ką jos dažniausiai reiškia
                - Ką verta patikrinti pirmiausia
                - Kada gali būti nesaugu toliau važiuoti

                Rašyk tik paprastu tekstu lietuviškai, be žymėjimų.

                Automobilio kontekstas:
                %s
                """.formatted(context);

        Map<String, Object> payload = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("inline_data", Map.of("mime_type", mimeType, "data", base64)), Map.of("text", prompt)))));
        Map response = restClient.post().uri(uriBuilder -> uriBuilder.path("/v1beta/models/{model}:generateContent").queryParam("key", apiKey).build(model)).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
        String answer = sanitizePlainText(extractText(response));

        return new AiImageAnalysisResponse(answer == null || answer.isBlank() ? "Nepavyko atpažinti nuotraukos. Pabandyk įkelti ryškesnę skydelio nuotrauką." : answer);
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map response) {
        if (response == null) return null;

        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) return null;

        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidateMap)) return null;

        Object contentObj = candidateMap.get("content");
        if (!(contentObj instanceof Map<?, ?> contentMap)) return null;

        Object partsObj = contentMap.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) return null;

        Object firstPart = parts.get(0);
        if (!(firstPart instanceof Map<?, ?> partMap)) return null;

        Object textObj = partMap.get("text");
        return textObj == null ? null : textObj.toString();
    }

    private String sanitizePlainText(String text) {
        if (text == null) return null;

        String cleaned = text.replace("```", "").replace("**", "").replace("__", "").replace("#", "").replace("`", "").replaceAll("\\n{3,}", "\n\n").trim();
        cleaned = cleaned.replaceAll("^\\s*\\*\\s+", "- ");
        cleaned = cleaned.replaceAll("(?m)^\\s*\\*\\s+", "- ");

        return cleaned;
    }
}
