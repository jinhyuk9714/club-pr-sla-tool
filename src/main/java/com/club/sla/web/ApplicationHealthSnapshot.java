package com.club.sla.web;

public record ApplicationHealthSnapshot(String status, String database, String migrations) {}
