package com.example.back.Unit.group;

import com.example.back.group.domain.AppGroup;
import com.example.back.group.domain.GroupMember;
import com.example.back.group.domain.GroupMemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GroupDomainTest {

    @Test
    @DisplayName("AppGroup saugo pavadinima ir savininka")
    void appGroupStoresNameAndOwner() {
        AppGroup group = new AppGroup("Family", 10L);

        assertThat(group.getId()).isNull();
        assertThat(group.getName()).isEqualTo("Family");
        assertThat(group.getOwnerUserId()).isEqualTo(10L);

        group.setName("New family");

        assertThat(group.getName()).isEqualTo("New family");
    }

    @Test
    @DisplayName("GroupMember saugo grupe vartotoja ir role")
    void groupMemberStoresGroupUserAndRole() {
        GroupMember member = new GroupMember(1L, 10L, "OWNER");

        assertThat(member.getGroupId()).isEqualTo(1L);
        assertThat(member.getUserId()).isEqualTo(10L);
        assertThat(member.getRole()).isEqualTo("OWNER");

        member.setRole("MEMBER");

        assertThat(member.getRole()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("GroupMemberId equals ir hashCode veikia teisingai")
    void groupMemberIdEqualsAndHashCodeWorkCorrectly() {
        GroupMemberId first = new GroupMemberId(1L, 10L);
        GroupMemberId second = new GroupMemberId(1L, 10L);
        GroupMemberId differentGroup = new GroupMemberId(2L, 10L);
        GroupMemberId differentUser = new GroupMemberId(1L, 20L);

        assertThat(first).isEqualTo(first);
        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(differentGroup);
        assertThat(first).isNotEqualTo(differentUser);
        assertThat(first).isNotEqualTo("not-id");
        assertThat(first).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Tuscias GroupMemberId konstruktorius sukuria objekta")
    void emptyGroupMemberIdConstructorCreatesObject() {
        GroupMemberId id = new GroupMemberId();

        assertThat(id).isNotNull();
    }
}