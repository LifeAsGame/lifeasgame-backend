package online.lifeasgame.social.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import online.lifeasgame.social.domain.PersonalGroupType;

public final class PersonalRoleGroupResult {
    private PersonalRoleGroupResult() {}

    public record Identity(Long linkId, Long roleId, PersonalGroupType groupType, Long groupId) {}
    public record Group(PersonalGroupType groupType, Long groupId, String name, String memberRole) {}
    public enum Access { AVAILABLE, UNAVAILABLE }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Link(Long linkId, Long roleId, PersonalGroupType groupType, Long groupId, Group group) {
        @com.fasterxml.jackson.annotation.JsonProperty
        public Access access() { return group == null ? Access.UNAVAILABLE : Access.AVAILABLE; }
    }
}
