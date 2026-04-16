package nus_iss.LAPS.dto;

public record ApiResponse(boolean success, String message, Object data) {}
