package online.lifeasgame.content.activation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("content activation input preflight")
class ContentActivationPreflightTest {

    private static final String INPUT_ENV = "CONTENT_ACTIVATION_INPUT";
    private static final String MANIFEST_FILE =
            "content/01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv";
    private static final String REFERENCES_FILE =
            "content/02_REFERENCE_INTEGRITY_MATRIX.csv";
    private static final String COPY_FILE =
            "content/07_COPY_AUTHORITY_ADDENDUM.csv";
    private static final String CAPABILITY_FILE =
            "capability/01_CONSUMER_FIRST_COMPLETION_CAPABILITY_MANIFEST.csv";

    private static final String MANIFEST_HEADER = String.join(",",
            "contentType", "stableCode", "definitionVersion",
            "activationWave", "active", "gated", "deferred",
            "lifecycleStatus", "replacementCode", "replacementVersion",
            "priority", "capabilityId", "availabilityStartAt",
            "availabilityEndAt", "releaseCohort", "sortOrder",
            "rewardProfileCode", "itemCode", "equipmentSlotCode",
            "copyKeyReferences", "referenceIntegrityStatus",
            "decisionRationale", "sourceRevision", "approvedBy"
    );
    private static final String REFERENCE_HEADER = String.join(",",
            "sourceType", "sourceCode", "sourceVersion", "referenceType",
            "targetType", "targetCode", "targetVersion",
            "requiredForActive", "status", "failureReason",
            "owningAuthority"
    );
    private static final String COPY_HEADER = String.join(",",
            "copyKey", "sourceType", "sourceCode", "locale", "text",
            "definitionVersion", "lifecycleStatus", "parameterNames",
            "maxLength", "sensitiveContextRule", "sourceRevision",
            "approvedBy"
    );
    private static final String CAPABILITY_HEADER = String.join(",",
            "capabilityId", "firstCompletionClass", "p0OrP1",
            "primaryOwner", "sourceRevision", "approvedBy"
    );

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("self-contained input을 검증하면")
    class SelfContainedInput {

        @Test
        @DisplayName("manifest/reference와 payload/runtime 결과를 분리한다")
        void reportsSeparateOutcomes() throws IOException {
            var report = validateFixture(createFixture("valid"));

            assertThat(report.authoritySnapshotValidation())
                    .isEqualTo(ContentActivationPreflight
                            .AuthoritySnapshotValidation.MANIFEST_VALID);
            assertThat(report.referenceDeclarationValidation())
                    .isEqualTo(ContentActivationPreflight
                            .ReferenceDeclarationValidation
                            .REFERENCE_DECLARATIONS_VALID);
            assertThat(report.payloadDeliveryStatus())
                    .isEqualTo(ContentActivationPreflight
                            .PayloadDeliveryStatus.PAYLOAD_DELIVERY_INCOMPLETE);
            assertThat(report.runtimeVerificationStatus())
                    .isEqualTo(ContentActivationPreflight
                            .RuntimeVerificationStatus.RUNTIME_NOT_VERIFIED);
            assertThat(report)
                    .extracting(
                            ContentActivationPreflight.PreflightReport::manifestRows,
                            ContentActivationPreflight.PreflightReport::activeRows,
                            ContentActivationPreflight.PreflightReport::gatedRows,
                            ContentActivationPreflight.PreflightReport::referenceRows,
                            ContentActivationPreflight.PreflightReport
                                    ::activeRequiredReferencesResolved,
                            ContentActivationPreflight.PreflightReport
                                    ::deliveredCopyPayloads
                    )
                    .containsExactly(5, 2, 3, 3, 1, 0);
            assertThat(report.missingCopyPayloads()).containsExactly(
                    new ContentActivationPreflight.ContentIdentity(
                            "COPY",
                            "copy.active.title",
                            "1"
                    )
            );
        }

        @Test
        @DisplayName("optional gated target을 active로 승격하지 않는다")
        void permitsOptionalGatedTarget() throws IOException {
            var report = validateFixture(createFixture("optional-gated"));

            assertThat(report.activeRows()).isEqualTo(2);
            assertThat(report.gatedRows()).isEqualTo(3);
        }

        @Test
        @DisplayName("strict boolean, exclusive state와 lifecycle을 강제한다")
        void rejectsInvalidStateSemantics() throws IOException {
            Path badBoolean = createFixture("bad-boolean");
            mutateAndRehash(badBoolean, MANIFEST_FILE, content -> replaceRequired(
                    content,
                    "Q_ACTIVE,1,WAVE,true,false,false,ACTIVE",
                    "Q_ACTIVE,1,WAVE,TRUE,false,false,ACTIVE"
            ));
            Path badState = createFixture("bad-state");
            mutateAndRehash(badState, MANIFEST_FILE, content -> replaceRequired(
                    content,
                    "Q_ACTIVE,1,WAVE,true,false,false,ACTIVE",
                    "Q_ACTIVE,1,WAVE,true,true,false,ACTIVE"
            ));
            Path badLifecycle = createFixture("bad-lifecycle");
            mutateAndRehash(
                    badLifecycle,
                    MANIFEST_FILE,
                    content -> replaceRequired(
                            content,
                            "Q_ACTIVE,1,WAVE,true,false,false,ACTIVE",
                            "Q_ACTIVE,1,WAVE,true,false,false,GATED"
                    )
            );

            assertThatThrownBy(() -> validateFixture(badBoolean))
                    .hasMessageContaining("Invalid boolean active=TRUE");
            assertThatThrownBy(() -> validateFixture(badState))
                    .hasMessageContaining("Exactly one state");
            assertThatThrownBy(() -> validateFixture(badLifecycle))
                    .hasMessageContaining("Lifecycle/state mismatch");
        }

        @Test
        @DisplayName("stable identity 중복과 version 정규화를 거부한다")
        void keepsStableIdentityExact() throws IOException {
            Path duplicate = createFixture("duplicate");
            mutateAndRehash(duplicate, MANIFEST_FILE, content -> content
                    + content.lines()
                    .filter(line -> line.startsWith("QUEST,Q_ACTIVE,1,"))
                    .findFirst()
                    .orElseThrow()
                    + System.lineSeparator());
            Path changedVersion = createFixture("changed-version");
            mutateAndRehash(
                    changedVersion,
                    REFERENCES_FILE,
                    content -> replaceRequired(
                            content,
                            "COPY,copy.active.title,1,true,RESOLVED",
                            "COPY,copy.active.title,1.0.0,true,RESOLVED"
                    )
            );

            assertThatThrownBy(() -> validateFixture(duplicate))
                    .hasMessageContaining("Duplicate stable identity")
                    .hasMessageContaining("Q_ACTIVE@1");
            assertThatThrownBy(() -> validateFixture(changedVersion))
                    .hasMessageContaining("Resolved manifest target is missing")
                    .hasMessageContaining("@1.0.0");
        }

        @Test
        @DisplayName("capability, 숫자와 window를 결정적으로 검증한다")
        void rejectsMalformedBindingsAndScalars() throws IOException {
            Path unknownCapability = createFixture("unknown-capability");
            mutateAndRehash(
                    unknownCapability,
                    MANIFEST_FILE,
                    content -> replaceRequired(
                            content,
                            "CFC-TEST-001",
                            "CFC-UNKNOWN"
                    )
            );
            Path badSort = createFixture("bad-sort");
            mutateAndRehash(badSort, MANIFEST_FILE, content -> replaceRequired(
                    content,
                    "TEST_COHORT,10,,,,,RESOLVED",
                    "TEST_COHORT,NaN,,,,,RESOLVED"
            ));
            Path badWindow = createFixture("bad-window");
            mutateAndRehash(badWindow, MANIFEST_FILE, content -> replaceRequired(
                    content,
                    "CFC-TEST-001,,,TEST_COHORT",
                    "CFC-TEST-001,not-an-instant,,TEST_COHORT"
            ));

            assertThatThrownBy(() -> validateFixture(unknownCapability))
                    .hasMessageContaining("Unknown capabilityId CFC-UNKNOWN");
            assertThatThrownBy(() -> validateFixture(badSort))
                    .hasMessageContaining("Malformed numeric value");
            assertThatThrownBy(() -> validateFixture(badWindow))
                    .hasMessageContaining("Malformed availability start");
        }

        @Test
        @DisplayName("required internal target과 replacement declaration을 강제한다")
        void rejectsBrokenReferences() throws IOException {
            Path missingTarget = createFixture("missing-target");
            mutateAndRehash(
                    missingTarget,
                    REFERENCES_FILE,
                    content -> replaceRequired(
                            content,
                            "COPY,copy.active.title,1,true,RESOLVED",
                            "COPY,copy.missing.title,1,true,RESOLVED"
                    )
            );
            Path missingReplacement = createFixture("missing-replacement");
            mutateAndRehash(
                    missingReplacement,
                    REFERENCES_FILE,
                    content -> content.lines()
                            .filter(line -> !line.contains(",REPLACED_BY,"))
                            .collect(Collectors.joining(System.lineSeparator()))
                            + System.lineSeparator()
            );

            assertThatThrownBy(() -> validateFixture(missingTarget))
                    .hasMessageContaining("Resolved manifest target is missing")
                    .hasMessageContaining("copy.missing.title@1");
            assertThatThrownBy(() -> validateFixture(missingReplacement))
                    .hasMessageContaining(
                            "Replacement requires one explicit typed declaration"
                    );
        }
    }

    @Nested
    @DisplayName("input delivery boundary를 검증하면")
    class InputBoundary {

        @Test
        @DisplayName("checksum과 실제 bytes가 다르면 거부한다")
        void rejectsTamperedBytes() throws IOException {
            Path fixture = createFixture("tampered");
            Path manifest = fixture.resolve(MANIFEST_FILE);
            Files.writeString(
                    manifest,
                    Files.readString(manifest, StandardCharsets.UTF_8) + " ",
                    StandardCharsets.UTF_8
            );

            assertThatThrownBy(() -> validateFixture(fixture))
                    .hasMessageContaining("SHA-256 mismatch")
                    .hasMessageContaining(
                            "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv"
                    );
        }

        @Test
        @DisplayName("checksum path가 supplied root 밖으로 나갈 수 없다")
        void rejectsPathEscape() throws IOException {
            Path fixture = createFixture("path-escape");
            Files.writeString(
                    fixture.resolve("SHA256SUMS.txt"),
                    "0".repeat(64) + "  ../outside.csv\n",
                    StandardCharsets.UTF_8
            );

            assertThatThrownBy(() -> validateFixture(fixture))
                    .hasMessageContaining("Authority path escapes root");
        }

        @Test
        @DisplayName("UTF-8 BOM, quoted delimiter, escaped quote와 multiline을 읽는다")
        void parsesCsvForms() throws IOException {
            Path csv = tempDir.resolve("quoted.csv");
            Files.writeString(
                    csv,
                    "\uFEFFidentity,text\n"
                            + "one,\"delimiter, escaped \"\"quote\"\"\nnext line\"\n",
                    StandardCharsets.UTF_8
            );

            var table = ContentActivationPreflight.readCsv(csv);

            assertThat(table.headers()).containsExactly("identity", "text");
            assertThat(table.rows()).containsExactly(Map.of(
                    "identity", "one",
                    "text", "delimiter, escaped \"quote\"\nnext line"
            ));
        }

        @Test
        @DisplayName("duplicate header를 거부한다")
        void rejectsDuplicateHeader() throws IOException {
            Path csv = tempDir.resolve("duplicate-header.csv");
            Files.writeString(
                    csv,
                    "identity,identity\none,two\n",
                    StandardCharsets.UTF_8
            );

            assertThatThrownBy(() -> ContentActivationPreflight.readCsv(csv))
                    .hasMessageContaining("duplicate");
        }

        @Test
        @DisplayName("external input 미지정과 invalid 지정을 구분한다")
        void distinguishesAbsentAndInvalidExternalInput() throws IOException {
            assertThat(requestedExternalInput(null)).isEmpty();
            assertThat(requestedExternalInput("  ")).isEmpty();
            assertThatThrownBy(() -> requestedExternalInput(
                    tempDir.resolve("missing").toString()
            )).hasMessageContaining(INPUT_ENV + " directory does not exist");

            Path invalid = Files.createDirectory(tempDir.resolve("invalid"));
            assertThatThrownBy(() -> ContentActivationPreflight.validate(invalid))
                    .hasMessageContaining("Cannot read authority checksums");
        }
    }

    @Nested
    @DisplayName("explicit external snapshot을 요청하면")
    class ExternalSnapshot {

        @Test
        @DisplayName("accepted full input을 승인 전용 정책까지 검증한다")
        void validatesAcceptedInput() {
            Optional<Path> requested = requestedExternalInput(
                    System.getenv(INPUT_ENV)
            );
            assumeTrue(
                    requested.isPresent(),
                    INPUT_ENV + " is not set; full snapshot validation is opt-in"
            );

            var report = ContentActivationPreflight.validate(
                    requested.orElseThrow()
            );

            assertThat(report)
                    .extracting(
                            ContentActivationPreflight.PreflightReport::manifestRows,
                            ContentActivationPreflight.PreflightReport::activeRows,
                            ContentActivationPreflight.PreflightReport::gatedRows,
                            ContentActivationPreflight.PreflightReport::deferredRows,
                            ContentActivationPreflight.PreflightReport::referenceRows,
                            ContentActivationPreflight.PreflightReport
                                    ::activeRequiredReferencesResolved,
                            ContentActivationPreflight.PreflightReport
                                    ::activeRequiredReferencesNotApplicable,
                            ContentActivationPreflight.PreflightReport
                                    ::deliveredCopyPayloads
                    )
                    .containsExactly(796, 102, 449, 245, 2203, 195, 3, 16);
            assertThat(report.missingCopyPayloads()).hasSize(58);
        }
    }

    private ContentActivationPreflight.PreflightReport validateFixture(Path root) {
        return ContentActivationPreflight.validateGenericInput(root);
    }

    private Path createFixture(String name) throws IOException {
        Path root = tempDir.resolve(name);
        write(root, CAPABILITY_FILE, CAPABILITY_HEADER + "\n"
                + "CFC-TEST-001,REQUIRED_ACTIVE,P0,Content,TEST,tester\n");
        write(root, MANIFEST_FILE, MANIFEST_HEADER + "\n"
                + manifestRow("QUEST", "Q_ACTIVE", "1", "ACTIVE", "", "", 10)
                + manifestRow(
                        "COPY",
                        "copy.active.title",
                        "1",
                        "ACTIVE",
                        "",
                        "",
                        20
                )
                + manifestRow(
                        "ACHIEVEMENT",
                        "ACH_OPTIONAL",
                        "1",
                        "GATED",
                        "",
                        "",
                        30
                )
                + manifestRow(
                        "QUEST",
                        "Q_OLD",
                        "1",
                        "GATED",
                        "Q_NEW",
                        "1",
                        40
                )
                + manifestRow("QUEST", "Q_NEW", "1", "GATED", "", "", 50));
        write(root, REFERENCES_FILE, REFERENCE_HEADER + "\n"
                + "QUEST,Q_ACTIVE,1,COPY,COPY,copy.active.title,1,"
                + "true,RESOLVED,,Content\n"
                + "QUEST,Q_ACTIVE,1,ACHIEVEMENT_CANDIDATE,ACHIEVEMENT,"
                + "ACH_OPTIONAL,1,false,RESOLVED,,Content\n"
                + "QUEST,Q_OLD,1,REPLACED_BY,QUEST,Q_NEW,1,"
                + "false,RESOLVED,,Content\n");
        write(root, COPY_FILE, COPY_HEADER + "\n");
        writeChecksums(root);
        return root;
    }

    private String manifestRow(
            String type,
            String code,
            String version,
            String lifecycle,
            String replacementCode,
            String replacementVersion,
            int sortOrder
    ) {
        boolean active = lifecycle.equals("ACTIVE");
        boolean gated = lifecycle.equals("GATED");
        return String.join(",",
                type,
                code,
                version,
                "WAVE",
                Boolean.toString(active),
                Boolean.toString(gated),
                Boolean.toString(!active && !gated),
                lifecycle,
                replacementCode,
                replacementVersion,
                "P0",
                "CFC-TEST-001",
                "",
                "",
                "TEST_COHORT",
                Integer.toString(sortOrder),
                "",
                "",
                "",
                "",
                "RESOLVED",
                "fixture",
                "TEST",
                "tester"
        ) + System.lineSeparator();
    }

    private Optional<Path> requestedExternalInput(String configured) {
        if (configured == null || configured.isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    INPUT_ENV + " directory does not exist: " + path
            );
        }
        return Optional.of(path);
    }

    private void mutateAndRehash(
            Path root,
            String relativeName,
            UnaryOperator<String> mutation
    ) throws IOException {
        Path file = root.resolve(relativeName);
        String before = Files.readString(file, StandardCharsets.UTF_8);
        String after = mutation.apply(before);
        assertThat(after).isNotEqualTo(before);
        Files.writeString(file, after, StandardCharsets.UTF_8);
        writeChecksums(root);
    }

    private void writeChecksums(Path root) throws IOException {
        List<Path> files;
        try (var paths = Files.walk(root)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString()
                            .equals("SHA256SUMS.txt"))
                    .sorted()
                    .toList();
        }
        String checksums = files.stream()
                .map(path -> sha256(path) + "  " + root.relativize(path))
                .collect(Collectors.joining(System.lineSeparator()))
                + System.lineSeparator();
        Files.writeString(
                root.resolve("SHA256SUMS.txt"),
                checksums,
                StandardCharsets.UTF_8
        );
    }

    private void write(Path root, String relativeName, String content)
            throws IOException {
        Path file = root.resolve(relativeName);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(path));
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String replaceRequired(
            String content,
            String target,
            String replacement
    ) {
        assertThat(content).contains(target);
        return content.replaceFirst(
                java.util.regex.Pattern.quote(target),
                java.util.regex.Matcher.quoteReplacement(replacement)
        );
    }
}
