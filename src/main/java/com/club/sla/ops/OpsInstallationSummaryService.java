package com.club.sla.ops;

import com.club.sla.installation.GithubInstallationRepository;
import com.club.sla.installation.GithubInstallationRepositoryEntryRepository;
import com.club.sla.installation.InstallationSettings;
import com.club.sla.installation.InstallationSettingsRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class OpsInstallationSummaryService {

  private final GithubInstallationRepository githubInstallationRepository;
  private final InstallationSettingsRepository installationSettingsRepository;
  private final GithubInstallationRepositoryEntryRepository
      githubInstallationRepositoryEntryRepository;

  public OpsInstallationSummaryService(
      GithubInstallationRepository githubInstallationRepository,
      InstallationSettingsRepository installationSettingsRepository,
      GithubInstallationRepositoryEntryRepository githubInstallationRepositoryEntryRepository) {
    this.githubInstallationRepository = githubInstallationRepository;
    this.installationSettingsRepository = installationSettingsRepository;
    this.githubInstallationRepositoryEntryRepository = githubInstallationRepositoryEntryRepository;
  }

  public java.util.List<OpsInstallationSummaryView> listInstallations() {
    Map<Long, InstallationSettings> settingsByInstallationId =
        installationSettingsRepository.findAll().stream()
            .collect(
                Collectors.toMap(InstallationSettings::getInstallationId, Function.identity()));
    Map<Long, Long> activeRepositoryCountsByInstallationId =
        githubInstallationRepositoryEntryRepository.findAll().stream()
            .filter(entry -> entry.isActive())
            .collect(
                Collectors.groupingBy(entry -> entry.getInstallationId(), Collectors.counting()));

    return githubInstallationRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
        .map(
            installation -> {
              InstallationSettings settings =
                  settingsByInstallationId.get(installation.getInstallationId());
              boolean configured = settings != null && settings.isConfigured();
              long activeRepositoryCount =
                  activeRepositoryCountsByInstallationId.getOrDefault(
                      installation.getInstallationId(), 0L);
              return new OpsInstallationSummaryView(
                  installation.getInstallationId(),
                  installation.getAccountLogin(),
                  installation.getStatus(),
                  configured,
                  activeRepositoryCount,
                  installation.getUpdatedAt());
            })
        .toList();
  }
}
