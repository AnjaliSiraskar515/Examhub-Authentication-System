package com.example.examauth.dto;

import java.util.Map;

public class SystemSettingsUpdateRequestDTO {
    private SystemSettingsDTO settings;
    private Map<String, String> rawSettings;

    public SystemSettingsDTO getSettings() {
        return settings;
    }

    public void setSettings(SystemSettingsDTO settings) {
        this.settings = settings;
    }

    public Map<String, String> getRawSettings() {
        return rawSettings;
    }

    public void setRawSettings(Map<String, String> rawSettings) {
        this.rawSettings = rawSettings;
    }
}
