package com.example.examauth.dto;

public class ActivityLogDTO {
    private String time;
    private String user;
    private String action;
    private String module;
    private String status; // success, danger, warning

    public ActivityLogDTO(String time, String user, String action, String module, String status) {
        this.time = time;
        this.user = user;
        this.action = action;
        this.module = module;
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
