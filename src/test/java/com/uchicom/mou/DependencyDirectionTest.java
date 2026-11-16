// (C) 2026 uchicom
package com.uchicom.mou;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** mou のコアが JDK と mou 自身以外を参照しないことを検証する（設計書 §6 依存方向）。 */
class DependencyDirectionTest {

  private static final Path CORE_ROOT = Path.of("src/main/java/com/uchicom/mou");
  private static final Path SUPPORT_ROOT = CORE_ROOT.resolve("support");

  @Test
  void coreImportsOnlyJdkAndMou() throws IOException {
    var violations = new ArrayList<String>();
    try (Stream<Path> files = Files.walk(CORE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        if (file.startsWith(SUPPORT_ROOT)) {
          continue;
        }
        collectViolations(file, violations);
      }
    }
    assertThat(violations).isEmpty();
  }

  private static void collectViolations(Path file, List<String> violations) throws IOException {
    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      String text = line.strip();
      if (!text.startsWith("import ")) {
        continue;
      }
      String name = text.substring("import ".length()).replace("static ", "").strip();
      if (!isAllowed(name)) {
        violations.add(file + ": " + text);
      }
    }
  }

  static boolean isAllowed(String importName) {
    return importName.startsWith("java.") || importName.startsWith("com.uchicom.mou.");
  }

  @Test
  void jdkAndMouAreAllowed() {
    assertThat(isAllowed("java.util.List")).isTrue();
    assertThat(isAllowed("com.uchicom.mou.sim.SimClock")).isTrue();
  }

  @Test
  void ankoAndExternalLibrariesAreRejected() {
    assertThat(isAllowed("com.uchicom.pj.Main")).isFalse();
    assertThat(isAllowed("com.uchicom.util.ResourceUtil")).isFalse();
    assertThat(isAllowed("com.uchicom.zouni.ZouniParameter")).isFalse();
    assertThat(isAllowed("com.fasterxml.jackson.databind.ObjectMapper")).isFalse();
  }
}
