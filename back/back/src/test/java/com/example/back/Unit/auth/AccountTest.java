package com.example.back.Unit.auth;

import com.example.back.auth.domain.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    @DisplayName("Konstruktorius issaugo email ir password hash")
    void constructorStoresLowercaseEmailAndPasswordHash() {
        Account account = new Account("USER@Test.com", "hash");

        assertThat(account.getId()).isNull();
        assertThat(account.getEmail()).isEqualTo("user@test.com");
        assertThat(account.getPasswordHash()).isEqualTo("hash");
        assertThat(account.getCreatedAt()).isNotNull();
    }
}