package com.example.back.Unit.vehicle.vin;

import com.example.back.vehicle.vin.dto.VincarioDecodeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VincarioDecodeResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("DecodeItem klasė yra tiesiogiai naudojama")
    void decodeItemClassIsDirectlyUsed() {
        VincarioDecodeResponse.DecodeItem item = new VincarioDecodeResponse.DecodeItem();

        item.label = "Make";
        item.value = "Volkswagen";

        assertThat(item).isInstanceOf(VincarioDecodeResponse.DecodeItem.class);
        assertThat(item.label).isEqualTo("Make");
        assertThat(item.value).isEqualTo("Volkswagen");
    }

    @Test
    @DisplayName("DecodeItem klasė yra sukuriama per Jackson")
    void decodeItemClassIsCreatedByJackson() throws Exception {
        String json = "{\"decode\":[{\"label\":\"Make\",\"value\":\"Volkswagen\"},{\"label\":\"Model Year\",\"value\":2000}]}";

        VincarioDecodeResponse response = objectMapper.readValue(json, VincarioDecodeResponse.class);

        assertThat(response.decode).hasSize(2);
        assertThat(response.decode.get(0)).isInstanceOf(VincarioDecodeResponse.DecodeItem.class);
        assertThat(response.decode.get(0).label).isEqualTo("Make");
        assertThat(response.decode.get(0).value).isEqualTo("Volkswagen");
        assertThat(response.decode.get(1).label).isEqualTo("Model Year");
        assertThat(response.decode.get(1).value).isEqualTo(2000);
        assertThat(response.make()).isEqualTo("Volkswagen");
        assertThat(response.modelYear()).isEqualTo(2000);
    }

    @Test
    @DisplayName("Nežinomi JSON laukai yra ignoruojami")
    void unknownJsonFieldsAreIgnored() throws Exception {
        String json = "{\"unknown\":\"ignored\",\"decode\":[{\"label\":\"Make\",\"value\":\"Volkswagen\",\"extra\":\"ignored\"}]}";

        VincarioDecodeResponse response = objectMapper.readValue(json, VincarioDecodeResponse.class);

        assertThat(response.make()).isEqualTo("Volkswagen");
    }

    @Test
    @DisplayName("Tekstinės reikšmės nuskaitomos iš decode sąrašo")
    void stringValuesAreReadFromDecodeList() {
        VincarioDecodeResponse response = response(item("VIN", "WVWZZZ1JZXW000001"), item("Make", "Volkswagen"), item("Model", "Golf"), item("Manufacturer", "VW AG"), item("Plant Country", "Germany"), item("Body", "Hatchback"), item("Drive", "FWD"));

        assertThat(response.vin()).isEqualTo("WVWZZZ1JZXW000001");
        assertThat(response.make()).isEqualTo("Volkswagen");
        assertThat(response.model()).isEqualTo("Golf");
        assertThat(response.manufacturer()).isEqualTo("VW AG");
        assertThat(response.plantCountry()).isEqualTo("Germany");
        assertThat(response.body()).isEqualTo("Hatchback");
        assertThat(response.drive()).isEqualTo("FWD");
    }

    @Test
    @DisplayName("Sąrašo reikšmė grąžina pirmą elementą")
    void listValueReturnsFirstElement() {
        VincarioDecodeResponse response = response(item("Make", List.of("Volkswagen", "Audi")));

        assertThat(response.make()).isEqualTo("Volkswagen");
    }

    @Test
    @DisplayName("Tuščias sąrašas grąžina null")
    void emptyListReturnsNull() {
        VincarioDecodeResponse response = response(item("Make", List.of()));

        assertThat(response.make()).isNull();
    }

    @Test
    @DisplayName("Sąrašas su null pirmu elementu grąžina null")
    void listWithNullFirstElementReturnsNull() {
        VincarioDecodeResponse response = response(item("Make", Arrays.asList((Object) null)));

        assertThat(response.make()).isNull();
    }

    @Test
    @DisplayName("Null decode sąrašas grąžina null reikšmes")
    void nullDecodeReturnsNullValues() {
        VincarioDecodeResponse response = new VincarioDecodeResponse();

        assertThat(response.make()).isNull();
        assertThat(response.modelYear()).isNull();
        assertThat(response.co2Gkm()).isNull();
        assertThat(response.engineDisplacementCc()).isNull();
    }

    @Test
    @DisplayName("Tuščias label masyvas grąžina null")
    void emptyLabelsReturnNull() {
        VincarioDecodeResponse response = response(item("Make", "Volkswagen"));

        assertThat(response.getString()).isNull();
    }

    @Test
    @DisplayName("Null label masyvas grąžina null")
    void nullLabelsReturnNull() {
        VincarioDecodeResponse response = response(item("Make", "Volkswagen"));

        assertThat(response.getString((String[]) null)).isNull();
    }

    @Test
    @DisplayName("Null ieškomas label yra praleidžiamas")
    void nullWantedLabelIsSkipped() {
        VincarioDecodeResponse response = response(item("Make", "Volkswagen"));

        assertThat(response.getString(null, "Make")).isEqualTo("Volkswagen");
    }

    @Test
    @DisplayName("Null decode item yra praleidžiamas")
    void nullDecodeItemIsSkipped() {
        VincarioDecodeResponse response = new VincarioDecodeResponse();
        response.decode = Arrays.asList(null, item("Make", "Volkswagen"));

        assertThat(response.make()).isEqualTo("Volkswagen");
    }

    @Test
    @DisplayName("Decode item su null label yra praleidžiamas")
    void decodeItemWithNullLabelIsSkipped() {
        VincarioDecodeResponse response = response(item(null, "ignored"), item("Make", "Volkswagen"));

        assertThat(response.make()).isEqualTo("Volkswagen");
    }

    @Test
    @DisplayName("Decode item su null value yra praleidžiamas")
    void decodeItemWithNullValueIsSkipped() {
        VincarioDecodeResponse response = response(item("Fuel Type - Primary", null), item("Fuel Type", "Diesel"));

        assertThat(response.fuelType()).isEqualTo("Diesel");
    }

    @Test
    @DisplayName("Model year randamas pagal alternatyvius label")
    void modelYearIsReadFromAlternativeLabels() {
        VincarioDecodeResponse first = response(item("Model Year", 2020));
        VincarioDecodeResponse second = response(item("Year", "2021"));
        VincarioDecodeResponse third = response(item("Model year", "2022"));

        assertThat(first.modelYear()).isEqualTo(2020);
        assertThat(second.modelYear()).isEqualTo(2021);
        assertThat(third.modelYear()).isEqualTo(2022);
    }

    @Test
    @DisplayName("Sveikasis skaičius iš Number nuskaitomas teisingai")
    void integerIsReadFromNumber() {
        VincarioDecodeResponse response = response(item("Number of Doors", 5));

        assertThat(response.doors()).isEqualTo(5);
    }

    @Test
    @DisplayName("Sveikasis skaičius iš teksto su papildomais simboliais nuskaitomas teisingai")
    void integerIsReadFromTextWithExtraCharacters() {
        VincarioDecodeResponse response = response(item("Number of Doors", "5 doors"), item("Number of Seats", "seats 7"));

        assertThat(response.doors()).isEqualTo(5);
        assertThat(response.seats()).isEqualTo(7);
    }

    @Test
    @DisplayName("Netinkamas sveikasis skaičius grąžina null")
    void invalidIntegerReturnsNull() {
        VincarioDecodeResponse response = response(item("Number of Doors", "abc"));

        assertThat(response.doors()).isNull();
    }

    @Test
    @DisplayName("Tuščias sveikojo skaičiaus tekstas grąžina null")
    void blankIntegerTextReturnsNull() {
        VincarioDecodeResponse response = response(item("Number of Doors", "   "));

        assertThat(response.doors()).isNull();
    }

    @Test
    @DisplayName("Per didelis sveikasis skaičius grąžina null")
    void tooLargeIntegerReturnsNull() {
        VincarioDecodeResponse response = response(item("Number of Doors", "999999999999999999999"));

        assertThat(response.doors()).isNull();
    }

    @Test
    @DisplayName("Double reikšmė nuskaitoma iš Number")
    void doubleIsReadFromNumber() {
        VincarioDecodeResponse response = response(item("Average CO2 Emission (g/km)", 140.5));

        assertThat(response.co2Gkm()).isEqualTo(140.5);
    }

    @Test
    @DisplayName("Double reikšmė nuskaitoma iš teksto su kableliu")
    void doubleIsReadFromTextWithComma() {
        VincarioDecodeResponse response = response(item("CO2 Emission (g/km)", "130,7"));

        assertThat(response.co2Gkm()).isEqualTo(130.7);
    }

    @Test
    @DisplayName("CO2 reikšmė randama pagal WLTP label")
    void co2IsReadFromWltpLabel() {
        VincarioDecodeResponse response = response(item("CO2 Emission (g/km) (WLTP)", "125.5"));

        assertThat(response.co2Gkm()).isEqualTo(125.5);
    }

    @Test
    @DisplayName("Netinkama double reikšmė grąžina null")
    void invalidDoubleReturnsNull() {
        VincarioDecodeResponse response = response(item("CO2 Emission (g/km)", "abc"));

        assertThat(response.co2Gkm()).isNull();
    }

    @Test
    @DisplayName("Tuščias double tekstas grąžina null")
    void blankDoubleTextReturnsNull() {
        VincarioDecodeResponse response = response(item("CO2 Emission (g/km)", "   "));

        assertThat(response.co2Gkm()).isNull();
    }

    @Test
    @DisplayName("Transmission naudoja pagrindinį label")
    void transmissionUsesPrimaryLabel() {
        VincarioDecodeResponse response = response(item("Transmission", "Manual"), item("Transmission (full)", "Automatic"));

        assertThat(response.transmission()).isEqualTo("Manual");
    }

    @Test
    @DisplayName("Transmission naudoja atsarginį label")
    void transmissionUsesFallbackLabel() {
        VincarioDecodeResponse response = response(item("Transmission (full)", "Automatic"));

        assertThat(response.transmission()).isEqualTo("Automatic");
    }

    @Test
    @DisplayName("Fuel type naudoja primary label")
    void fuelTypeUsesPrimaryLabel() {
        VincarioDecodeResponse response = response(item("Fuel Type - Primary", "Diesel"), item("Fuel Type", "Gasoline"), item("Fuel Type - Secondary", "Electric"));

        assertThat(response.fuelType()).isEqualTo("Diesel");
    }

    @Test
    @DisplayName("Fuel type naudoja antrą label")
    void fuelTypeUsesSecondLabel() {
        VincarioDecodeResponse response = response(item("Fuel Type", "Gasoline"), item("Fuel Type - Secondary", "Electric"));

        assertThat(response.fuelType()).isEqualTo("Gasoline");
    }

    @Test
    @DisplayName("Fuel type naudoja atsarginius label")
    void fuelTypeUsesFallbackLabels() {
        VincarioDecodeResponse first = response(item("Fuel Type - Secondary", "Electric"));
        VincarioDecodeResponse second = response(item("Fuel", "Hybrid"));

        assertThat(first.fuelType()).isEqualTo("Electric");
        assertThat(second.fuelType()).isEqualTo("Hybrid");
    }

    @Test
    @DisplayName("Variklio kubatūra randama pagal cc label")
    void engineDisplacementIsReadFromCcLabel() {
        VincarioDecodeResponse response = response(item("Engine Displacement (ccm)", "1896 ccm"));

        assertThat(response.engineDisplacementCc()).isEqualTo(1896);
    }

    @Test
    @DisplayName("Variklio kubatūra randama pagal alternatyvius cc label")
    void engineDisplacementIsReadFromAlternativeCcLabels() {
        VincarioDecodeResponse first = response(item("Engine Displacement (cc)", "1900"));
        VincarioDecodeResponse second = response(item("Engine Displacement (ccm.)", "1901"));
        VincarioDecodeResponse third = response(item("Engine Displacement (cm3)", "1902"));
        VincarioDecodeResponse fourth = response(item("Engine Displacement (ccm³)", "1903"));
        VincarioDecodeResponse fifth = response(item("Engine Displacement (ccm3)", "1904"));

        assertThat(first.engineDisplacementCc()).isEqualTo(1900);
        assertThat(second.engineDisplacementCc()).isEqualTo(1901);
        assertThat(third.engineDisplacementCc()).isEqualTo(1902);
        assertThat(fourth.engineDisplacementCc()).isEqualTo(1903);
        assertThat(fifth.engineDisplacementCc()).isEqualTo(1904);
    }

    @Test
    @DisplayName("Variklio kubatūra apskaičiuojama iš litrų")
    void engineDisplacementIsCalculatedFromLiters() {
        VincarioDecodeResponse first = response(item("Engine Displacement (l)", "1.9"));
        VincarioDecodeResponse second = response(item("Engine Displacement (L)", "2,0"));

        assertThat(first.engineDisplacementCc()).isEqualTo(1900);
        assertThat(second.engineDisplacementCc()).isEqualTo(2000);
    }

    @Test
    @DisplayName("Variklio kubatūra apskaičiuojama iš pilno variklio aprašymo")
    void engineDisplacementIsCalculatedFromEngineFull() {
        VincarioDecodeResponse response = response(item("Engine (full)", "2.0 TDI"));

        assertThat(response.engineDisplacementCc()).isEqualTo(2000);
        assertThat(response.engineFull()).isEqualTo("2.0 TDI");
    }

    @Test
    @DisplayName("Netinkamas pilnas variklio aprašymas grąžina null kubatūrą")
    void invalidEngineFullReturnsNullDisplacement() {
        VincarioDecodeResponse response = response(item("Engine (full)", "TDI engine"));

        assertThat(response.engineDisplacementCc()).isNull();
    }

    @Test
    @DisplayName("Pilnas variklio aprašymas su netinkamu double grąžina null kubatūrą")
    void invalidEngineFullDoubleReturnsNullDisplacement() {
        VincarioDecodeResponse response = response(item("Engine (full)", "1.2.3 TDI"));

        assertThat(response.engineDisplacementCc()).isNull();
    }

    @Test
    @DisplayName("Papildomi variklio laukai nuskaitomi teisingai")
    void additionalEngineFieldsAreReadCorrectly() {
        VincarioDecodeResponse response = response(item("Engine Cylinders", "4"), item("Engine Power (kW)", "85 kW"));

        assertThat(response.engineCylinders()).isEqualTo(4);
        assertThat(response.enginePowerKw()).isEqualTo(85);
    }

    private static VincarioDecodeResponse response(VincarioDecodeResponse.DecodeItem... items) {
        VincarioDecodeResponse response = new VincarioDecodeResponse();
        response.decode = List.of(items);
        return response;
    }

    private static VincarioDecodeResponse.DecodeItem item(String label, Object value) {
        VincarioDecodeResponse.DecodeItem item = new VincarioDecodeResponse.DecodeItem();
        item.label = label;
        item.value = value;
        return item;
    }
}