package com.example.back.Unit.vehicle;

import com.example.back.vehicle.domain.ActiveVehicle;
import com.example.back.vehicle.domain.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class VehicleDomainTest {

    @Test
    @DisplayName("Konstruktorius nustato savininką ir pradinę ridą")
    void constructorSetsOwnerAndDefaultOdometer() {
        Vehicle vehicle = new Vehicle(10L);

        assertThat(vehicle.getOwnerUserId()).isEqualTo(10L);
        assertThat(vehicle.getOdometerKm()).isZero();
    }

    @Test
    @DisplayName("Tekstiniai laukai apkarpomi ir tuščios reikšmės tampa null")
    void stringFieldsNormalizeBlankToNull() {
        Vehicle vehicle = new Vehicle(10L);

        vehicle.setMake("   ");
        vehicle.setModel("");
        vehicle.setFuelType(" DIESEL ");
        vehicle.setTransmission(" Manual ");
        vehicle.setDrive(" FWD ");
        vehicle.setBody(" Hatchback ");
        vehicle.setPlantCountry(" Germany ");
        vehicle.setManufacturer(" VW ");

        assertThat(vehicle.getMake()).isNull();
        assertThat(vehicle.getModel()).isNull();
        assertThat(vehicle.getFuelType()).isEqualTo("DIESEL");
        assertThat(vehicle.getTransmission()).isEqualTo("Manual");
        assertThat(vehicle.getDrive()).isEqualTo("FWD");
        assertThat(vehicle.getBody()).isEqualTo("Hatchback");
        assertThat(vehicle.getPlantCountry()).isEqualTo("Germany");
        assertThat(vehicle.getManufacturer()).isEqualTo("VW");
    }

    @Test
    @DisplayName("Rida nenustatoma kai perduodama null")
    void odometerIgnoresNull() {
        Vehicle vehicle = new Vehicle(10L);

        vehicle.setOdometerKm(null);

        assertThat(vehicle.getOdometerKm()).isZero();
    }

    @Test
    @DisplayName("Rida negali būti neigiama")
    void odometerCannotBeNegative() {
        Vehicle vehicle = new Vehicle(10L);

        assertThatThrownBy(() -> vehicle.setOdometerKm(-1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Odometer km negali būti neigiamas");
    }

    @Test
    @DisplayName("Variklio darbinis tūris gali būti null")
    void engineDisplacementCanBeNull() {
        Vehicle vehicle = new Vehicle(10L);

        vehicle.setEngineDisplacementCc(null);

        assertThat(vehicle.getEngineDisplacementCc()).isNull();
    }

    @Test
    @DisplayName("Variklio darbinis tūris turi būti validus")
    void engineDisplacementMustBeValid() {
        Vehicle vehicle = new Vehicle(10L);

        assertThatThrownBy(() -> vehicle.setEngineDisplacementCc(300)).isInstanceOf(IllegalArgumentException.class).hasMessage("Engine displacement cc neteisingas");
        assertThatThrownBy(() -> vehicle.setEngineDisplacementCc(10001)).isInstanceOf(IllegalArgumentException.class).hasMessage("Engine displacement cc neteisingas");

        vehicle.setEngineDisplacementCc(1900);

        assertThat(vehicle.getEngineDisplacementCc()).isEqualTo(1900);
    }

    @Test
    @DisplayName("Durų skaičius gali būti null arba validus")
    void doorsCanBeNullOrValid() {
        Vehicle vehicle = new Vehicle(10L);

        vehicle.setDoors(null);
        assertThat(vehicle.getDoors()).isNull();

        vehicle.setDoors(5);
        assertThat(vehicle.getDoors()).isEqualTo(5);
    }

    @Test
    @DisplayName("Durų skaičius turi būti intervale")
    void doorsMustBeInRange() {
        Vehicle vehicle = new Vehicle(10L);

        assertThatThrownBy(() -> vehicle.setDoors(0)).isInstanceOf(IllegalArgumentException.class).hasMessage("Doors neteisingas");
        assertThatThrownBy(() -> vehicle.setDoors(9)).isInstanceOf(IllegalArgumentException.class).hasMessage("Doors neteisingas");
    }

    @Test
    @DisplayName("Sėdynių skaičius gali būti null arba validus")
    void seatsCanBeNullOrValid() {
        Vehicle vehicle = new Vehicle(10L);

        vehicle.setSeats(null);
        assertThat(vehicle.getSeats()).isNull();

        vehicle.setSeats(5);
        assertThat(vehicle.getSeats()).isEqualTo(5);
    }

    @Test
    @DisplayName("Sėdynių skaičius turi būti intervale")
    void seatsMustBeInRange() {
        Vehicle vehicle = new Vehicle(10L);

        assertThatThrownBy(() -> vehicle.setSeats(0)).isInstanceOf(IllegalArgumentException.class).hasMessage("Seats neteisingas");
        assertThatThrownBy(() -> vehicle.setSeats(21)).isInstanceOf(IllegalArgumentException.class).hasMessage("Seats neteisingas");
    }

    @Test
    @DisplayName("Visi paprasti setteriai išsaugo reikšmes")
    void simpleSettersStoreValues() {
        Vehicle vehicle = new Vehicle(10L);
        BigDecimal co2 = new BigDecimal("120.50");

        vehicle.setId(1L);
        vehicle.setGroupId(2L);
        vehicle.setVin("VIN");
        vehicle.setModelYear(2020);
        vehicle.setCo2Gkm(co2);

        assertThat(vehicle.getId()).isEqualTo(1L);
        assertThat(vehicle.getGroupId()).isEqualTo(2L);
        assertThat(vehicle.getVin()).isEqualTo("VIN");
        assertThat(vehicle.getModelYear()).isEqualTo(2020);
        assertThat(vehicle.getCo2Gkm()).isEqualTo(co2);
        assertThat(vehicle.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Aktyvi mašina saugo vartotoją, mašiną ir aktyvavimo laiką")
    void activeVehicleStoresFields() {
        ActiveVehicle activeVehicle = new ActiveVehicle(10L, 1L);

        assertThat(activeVehicle.getUserId()).isEqualTo(10L);
        assertThat(activeVehicle.getVehicleId()).isEqualTo(1L);
        assertThat(activeVehicle.getActivatedAt()).isNotNull();

        activeVehicle.setVehicleId(2L);

        assertThat(activeVehicle.getVehicleId()).isEqualTo(2L);
    }
}