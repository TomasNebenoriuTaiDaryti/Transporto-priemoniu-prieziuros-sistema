package com.example.back.Unit.group;

import com.example.back.group.dto.GroupDtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class GroupDtosTest {

    @Test
    @DisplayName("CreateGroupRequest saugo pavadinima")
    void createGroupRequestStoresName() {
        GroupDtos.CreateGroupRequest request = new GroupDtos.CreateGroupRequest("Family");

        assertThat(request.name()).isEqualTo("Family");
    }

    @Test
    @DisplayName("UpdateGroupRequest saugo pavadinima")
    void updateGroupRequestStoresName() {
        GroupDtos.UpdateGroupRequest request = new GroupDtos.UpdateGroupRequest("New family");

        assertThat(request.name()).isEqualTo("New family");
    }

    @Test
    @DisplayName("InviteRequest saugo email")
    void inviteRequestStoresEmail() {
        GroupDtos.InviteRequest request = new GroupDtos.InviteRequest("user@test.com");

        assertThat(request.email()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("GroupResponse saugo laukus")
    void groupResponseStoresFields() {
        GroupDtos.GroupResponse response = new GroupDtos.GroupResponse(1L, "Family", "OWNER", 10L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Family");
        assertThat(response.myRole()).isEqualTo("OWNER");
        assertThat(response.ownerUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("MemberResponse saugo laukus")
    void memberResponseStoresFields() {
        GroupDtos.MemberResponse response = new GroupDtos.MemberResponse(10L, "user@test.com", "MEMBER");

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.role()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("SimpleMessage saugo zinute")
    void simpleMessageStoresMessage() {
        GroupDtos.SimpleMessage message = new GroupDtos.SimpleMessage("ok");

        assertThat(message.message()).isEqualTo("ok");
    }

    @Test
    @DisplayName("Privatus konstruktorius gali buti iskviestas refleksija")
    void privateConstructorCanBeCalledByReflection() throws Exception {
        Constructor<GroupDtos> constructor = GroupDtos.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        GroupDtos result = constructor.newInstance();

        assertThat(result).isNotNull();
    }
}