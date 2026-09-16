package com.team.cops_and_robbers.common.fcm;

import java.util.List;
import java.util.Map;

public record FcmMessage(List<String> tokens, String title, String body, Map<String, String> data, String collapseKey) {

    public FcmMessage(List<String> tokens, String title, String body, Map<String, String> data) {
        this(tokens, title, body, data, null);
    }
}
