package com.kalsym.ekedai.model.dao;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExtraData {
    private String name;
    private String gender;
    private String nationality;
    private String email;
    private String channel;
    private String deviceToken;
    private String dob;

    public ExtraData() {}

    public ExtraData(JsonNode extraDataNode) {
        this.name = extraDataNode.has("name") ? extraDataNode.get("name").asText() : null;
        this.gender = extraDataNode.has("gender") ? extraDataNode.get("gender").asText() : null;
        this.nationality = extraDataNode.has("nationality") ? extraDataNode.get("nationality").asText() : null;
        this.email = extraDataNode.has("email") ? extraDataNode.get("email").asText() : null;
        this.channel = extraDataNode.has("channel") ? extraDataNode.get("channel").asText() : null;
        this.deviceToken = extraDataNode.has("deviceToken") ? extraDataNode.get("deviceToken").asText() : null;
        this.dob = extraDataNode.has("dob") ? extraDataNode.get("dob").asText() : null;
    }
}
