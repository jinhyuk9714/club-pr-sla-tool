package com.club.sla.ops;

import com.club.sla.delivery.OutboundDeliveryJobRepository;
import com.club.sla.delivery.OutboundDeliveryJobStatus;
import com.club.sla.installation.GithubInstallationRepository;
import com.club.sla.installation.GithubInstallationStatus;
import com.club.sla.installation.InstallationSettings;
import com.club.sla.installation.InstallationSettingsRepository;
import com.club.sla.notify.DeadLetterReplayStatus;
import com.club.sla.notify.DeadLetterRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OpsOverviewService {

  private final GithubInstallationRepository githubInstallationRepository;
  private final InstallationSettingsRepository installationSettingsRepository;
  private final OutboundDeliveryJobRepository outboundDeliveryJobRepository;
  private final DeadLetterRepository deadLetterRepository;

  public OpsOverviewService(
      GithubInstallationRepository githubInstallationRepository,
      InstallationSettingsRepository installationSettingsRepository,
      OutboundDeliveryJobRepository outboundDeliveryJobRepository,
      DeadLetterRepository deadLetterRepository) {
    this.githubInstallationRepository = githubInstallationRepository;
    this.installationSettingsRepository = installationSettingsRepository;
    this.outboundDeliveryJobRepository = outboundDeliveryJobRepository;
    this.deadLetterRepository = deadLetterRepository;
  }

  public OpsOverviewView loadOverview() {
    Map<Long, InstallationSettings> settingsByInstallationId =
        installationSettingsRepository.findAll().stream()
            .collect(
                Collectors.toMap(InstallationSettings::getInstallationId, Function.identity()));
    long activeInstallations =
        githubInstallationRepository.findAll().stream()
            .filter(installation -> installation.getStatus() == GithubInstallationStatus.ACTIVE)
            .count();
    long configuredInstallations =
        githubInstallationRepository.findAll().stream()
            .filter(installation -> installation.getStatus() == GithubInstallationStatus.ACTIVE)
            .filter(
                installation -> {
                  InstallationSettings settings =
                      settingsByInstallationId.get(installation.getInstallationId());
                  return settings != null && settings.isConfigured();
                })
            .count();
    long misconfiguredInstallations = activeInstallations - configuredInstallations;
    long pendingOutboundJobs =
        outboundDeliveryJobRepository.findAll().stream()
            .filter(job -> job.getStatus() == OutboundDeliveryJobStatus.PENDING)
            .count();
    long deadOutboundJobs =
        outboundDeliveryJobRepository.findAll().stream()
            .filter(job -> job.getStatus() == OutboundDeliveryJobStatus.DEAD)
            .count();
    long pendingDeadLetters =
        deadLetterRepository.findAll().stream()
            .filter(deadLetter -> deadLetter.getReplayStatus() == DeadLetterReplayStatus.PENDING)
            .count();

    return new OpsOverviewView(
        activeInstallations,
        configuredInstallations,
        misconfiguredInstallations,
        pendingOutboundJobs,
        deadOutboundJobs,
        pendingDeadLetters);
  }
}
