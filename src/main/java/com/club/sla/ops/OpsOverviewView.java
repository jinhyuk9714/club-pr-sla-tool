package com.club.sla.ops;

public record OpsOverviewView(
    long activeInstallations,
    long configuredInstallations,
    long misconfiguredInstallations,
    long pendingOutboundJobs,
    long deadOutboundJobs,
    long pendingDeadLetters) {}
