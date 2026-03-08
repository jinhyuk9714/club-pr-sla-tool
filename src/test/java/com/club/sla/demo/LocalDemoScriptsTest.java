package com.club.sla.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LocalDemoScriptsTest {

  private final Path repoRoot = Path.of("").toAbsolutePath();

  @Test
  void dockerComposeSupportsOverridablePostgresPort() throws IOException {
    String composeFile = Files.readString(repoRoot.resolve("docker-compose.yml"));

    assertThat(composeFile).contains("\"${POSTGRES_PORT:-5432}:5432\"");
  }

  @Test
  void gitignoreExcludesDemoRuntimeDirectory() throws IOException {
    String gitignore = Files.readString(repoRoot.resolve(".gitignore"));

    assertThat(gitignore).contains(".demo/");
  }

  @Test
  void providesLocalDemoScripts() {
    assertThat(repoRoot.resolve("scripts/demo-up.sh")).exists();
    assertThat(repoRoot.resolve("scripts/demo-down.sh")).exists();
    assertThat(repoRoot.resolve("scripts/demo-status.sh")).exists();
  }

  @Test
  void demoUpScriptCapturesNgrokAndRuntimeState() throws IOException {
    String demoUp = Files.readString(repoRoot.resolve("scripts/demo-up.sh"));
    String demoCommon = Files.readString(repoRoot.resolve("scripts/demo-common.sh"));

    assertThat(demoCommon).contains("127.0.0.1:4040/api/tunnels");
    assertThat(demoCommon).contains(".demo");
    assertThat(demoUp).contains("SPRING_DATASOURCE_URL");
    assertThat(demoUp).contains("SERVER_PORT");
    assertThat(demoUp).contains("Homepage URL:");
    assertThat(demoUp).contains("Webhook URL:");
  }

  @Test
  void demoUpScriptReclaimsDefaultNgrokPortFromExistingNgrokProcess() throws IOException {
    String demoUp = Files.readString(repoRoot.resolve("scripts/demo-up.sh"));
    String demoCommon = Files.readString(repoRoot.resolve("scripts/demo-common.sh"));

    assertThat(demoCommon).contains("ps -p");
    assertThat(demoCommon).contains("kill");
    assertThat(demoUp).contains("release_ngrok_web_port");
  }

  @Test
  void demoUpScriptLaunchesStableJarProcessForStatusTracking() throws IOException {
    String demoUp = Files.readString(repoRoot.resolve("scripts/demo-up.sh"));

    assertThat(demoUp).contains("bootJar");
    assertThat(demoUp).contains("java -jar");
    assertThat(demoUp).contains("build/libs");
  }

  @Test
  void localDemoRunbookDocumentsRepeatableDemoFlow() throws IOException {
    String runbook = Files.readString(repoRoot.resolve("docs/runbooks/local-demo.md"));

    assertThat(runbook).contains("./scripts/demo-up.sh");
    assertThat(runbook).contains("./scripts/demo-status.sh");
    assertThat(runbook).contains("./scripts/demo-down.sh");
    assertThat(runbook).contains("Club PR SLA — On track");
  }
}
