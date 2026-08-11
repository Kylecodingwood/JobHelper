package com.jobhelper.shared.web;

public record ErrorBody(String code, String message, Object details, String requestId) {
}
