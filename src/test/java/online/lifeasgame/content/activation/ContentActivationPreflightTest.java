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
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("AUTH-CONT-001 v1.0.0 activation preflight")
class ContentActivationPreflightTest {

    private static final Path AUTHORITY = Path.of(
            ".codex-local/issue-333/authority"
    );
    private static final Path CONTENT = AUTHORITY.resolve("content");

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("승인된 snapshot을 검증하면")
    class ApprovedSnapshot {

        @Test
        @DisplayName("manifest/reference는 유효하고 payload/runtime은 별도 미완료 상태다")
        void reportsSeparateOutcomes() {
            var report = validate(AUTHORITY);

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
            assertThat(report.missingCopyPayloads())
                    .hasSize(58)
                    .contains(new ContentActivationPreflight.ContentIdentity(
                            "COPY",
                            "quest.q_record_first_trace.title",
                            "1"
                    ));
        }

        @Test
        @DisplayName("optional gated candidate를 active grant로 승격하지 않는다")
        void permitsOptionalGatedCandidateWithoutActivation() {
            var manifest = ContentActivationPreflight.readCsv(
                    CONTENT.resolve(
                            "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv"
                    )
            );
            var references = ContentActivationPreflight.readCsv(
                    CONTENT.resolve("02_REFERENCE_INTEGRITY_MATRIX.csv")
            );

            assertThat(manifest.rows()).anySatisfy(row -> {
                assertThat(row).containsEntry("contentType", "ACHIEVEMENT");
                assertThat(row).containsEntry("stableCode", "ACH_FIRST_LIFELOG");
                assertThat(row).containsEntry("gated", "true");
            });
            assertThat(references.rows()).anySatisfy(row -> {
                assertThat(row).containsEntry("sourceCode", "Q_RECORD_FIRST_TRACE");
                assertThat(row).containsEntry("referenceType", "ACHIEVEMENT_CANDIDATE");
                assertThat(row).containsEntry("targetCode", "ACH_FIRST_LIFELOG");
                assertThat(row).containsEntry("requiredForActive", "false");
            });
            assertThatCode(() -> validate(AUTHORITY)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("외부 authority edge는 content row 누락과 구분한다")
        void classifiesExternalAuthorityEdges() {
            var report = validate(AUTHORITY);

            assertThat(report.externalReferences()).anySatisfy(edge -> {
                assertThat(edge.target().contentType())
                        .isEqualTo("QUEST_FAMILY_TAXONOMY");
                assertThat(edge.classification()).isEqualTo(
                        ContentActivationPreflight
                                .ExternalReferenceClassification
                                .DECLARED_EXTERNAL_AUTHORITY
                );
            });
            assertThat(report.externalReferences()).anySatisfy(edge -> {
                assertThat(edge.target().contentType())
                        .isEqualTo("EQUIPMENT_SLOT_AUTHORITY");
                assertThat(edge.classification()).isEqualTo(
                        ContentActivationPreflight
                                .ExternalReferenceClassification
                                .DELIVERED_AUTHORITY_INPUT
                );
            });
        }

    }

    @Nested
    @DisplayName("manifest invariant가 깨지면")
    class InvalidManifest {

        @Test
        @DisplayName("boolean 대소문자나 truthy 값을 허용하지 않는다")
        void rejectsNonCanonicalBoolean() throws IOException {
            Path snapshot = copySnapshot();
            mutateAndRehash(
                    snapshot,
                    "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv",
                    content -> replaceRequired(
                            content,
                            ",false,true,false,GATED,",
                            ",false,TRUE,false,GATED,"
                    )
            );

            assertThatThrownBy(() -> validate(snapshot))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("Invalid boolean gated=TRUE");
        }

        @Test
        @DisplayName("active/gated/deferred 중 둘 이상을 허용하지 않는다")
        void rejectsContradictoryState() throws IOException {
            Path snapshot = copySnapshot();
            mutateAndRehash(
                    snapshot,
                    "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv",
                    content -> replaceRequired(
                            content,
                            ",false,true,false,GATED,",
                            ",true,true,false,GATED,"
                    )
            );

            assertThatThrownBy(() -> validate(snapshot))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("Exactly one state");
        }

        @Test
        @DisplayName("동일 stable identity 중복을 거부한다")
        void rejectsDuplicateIdentity() throws IOException {
            Path snapshot = copySnapshot();
            mutateAndRehash(
                    snapshot,
                    "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv",
                    content -> content + content.lines()
                            .filter(line -> line.startsWith(
                                    "QUEST,Q_RECORD_FIRST_TRACE,1,"
                            ))
                            .findFirst()
                            .orElseThrow()
                            + System.lineSeparator()
            );

            assertThatThrownBy(() -> validate(snapshot))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("Duplicate stable identity")
                    .hasMessageContaining("Q_RECORD_FIRST_TRACE@1");
        }

        @Test
        @DisplayName("canonical capability manifest에 없는 ID를 거부한다")
        void rejectsUnknownCapability() throws IOException {
            Path snapshot = copySnapshot();
            mutateAndRehash(
                    snapshot,
                    "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv",
                    content -> replaceRequired(
                            content,
                            "CFC-JNY-001",
                            "CFC-UNKNOWN-333"
                    )
            );

            assertThatThrownBy(() -> validate(snapshot))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("Unknown capabilityId CFC-UNKNOWN-333");
        }

        @Test
        @DisplayName("malformed sortOrder와 availability window를 결정적으로 거부한다")
        void rejectsMalformedScalarFields() throws IOException {
            Path badSort = copySnapshot("bad-sort");
            mutateAndRehash(
                    badSort,
                    "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv",
                    content -> replaceRequired(
                            content,
                            "CONSUMER_FIRST_COMPLETION_AUTHORITY_GATED,1,RP_NONE",
                            "CONSUMER_FIRST_COMPLETION_AUTHORITY_GATED,NaN,RP_NONE"
                    )
            );
            Path badWindow = copySnapshot("bad-window");
            mutateAndRehash(
                    badWindow,
                    "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv",
                    content -> replaceRequired(
                            content,
                            "CFC-JNY-001;CFC-JNY-002;CFC-JNY-003,,,"
                                    + "CONSUMER_FIRST_COMPLETION_AUTHORITY_GATED",
                            "CFC-JNY-001;CFC-JNY-002;CFC-JNY-003,not-an-instant,,"
                                    + "CONSUMER_FIRST_COMPLETION_AUTHORITY_GATED"
                    )
            );

            assertThatThrownBy(() -> validate(badSort))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("Malformed numeric value");
            assertThatThrownBy(() -> validate(badWindow))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("Malformed availability start");
        }

        @Test
        @DisplayName("replacement는 matrix의 explicit type/code/version 선언을 요구한다")
        void rejectsImplicitReplacement() throws IOException {
            Path snapshot = copySnapshot();
            mutateAndRehash(
                    snapshot,
                    "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv",
                    content -> replaceRequired(
                            content,
                            ",GATED,,,P0,CFC-JNY-001",
                            ",GATED,Q_RECORD_FIRST_TRACE,1,P0,CFC-JNY-001"
                    )
            );

            assertThatThrownBy(() -> validate(snapshot))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining(
                            "Replacement requires one explicit typed declaration"
                    );
        }
    }

    @Nested
    @DisplayName("reference declaration이 깨지면")
    class InvalidReference {

        @Test
        @DisplayName("RESOLVED인 active internal edge의 missing target을 거부한다")
        void rejectsMissingActiveInternalTarget() throws IOException {
            Path snapshot = copySnapshot();
            mutateAndRehash(
                    snapshot,
                    "02_REFERENCE_INTEGRITY_MATRIX.csv",
                    content -> replaceRequired(
                            content,
                            "COPY,quest.q_record_first_trace.accepted,1,true,RESOLVED",
                            "COPY,missing.copy.key,1,true,RESOLVED"
                    )
            );

            assertThatThrownBy(() -> validate(snapshot))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("Resolved manifest target is missing")
                    .hasMessageContaining("missing.copy.key@1");
        }

        @Test
        @DisplayName("definitionVersion을 숫자로 정규화하지 않고 exact string으로 비교한다")
        void keepsVersionIdentityStrict() throws IOException {
            Path snapshot = copySnapshot();
            mutateAndRehash(
                    snapshot,
                    "02_REFERENCE_INTEGRITY_MATRIX.csv",
                    content -> replaceRequired(
                            content,
                            "COPY,quest.q_record_first_trace.accepted,1,true,RESOLVED",
                            "COPY,quest.q_record_first_trace.accepted,1.0.0,true,RESOLVED"
                    )
            );

            assertThatThrownBy(() -> validate(snapshot))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("Resolved manifest target is missing")
                    .hasMessageContaining("@1.0.0");
        }
    }

    @Nested
    @DisplayName("snapshot delivery boundary를 검증하면")
    class SnapshotBoundary {

        @Test
        @DisplayName("authority file의 byte tampering을 hash gate에서 거부한다")
        void rejectsTamperedBytes() throws IOException {
            Path snapshot = copySnapshot();
            Path verdict = snapshot.resolve(
                    "content/00_EXECUTIVE_ACTIVATION_VERDICT.txt"
            );
            Files.writeString(
                    verdict,
                    Files.readString(verdict, StandardCharsets.UTF_8) + " ",
                    StandardCharsets.UTF_8
            );

            assertThatThrownBy(() -> validate(snapshot))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("SHA-256 mismatch")
                    .hasMessageContaining("00_EXECUTIVE_ACTIVATION_VERDICT.txt");
        }

        @Test
        @DisplayName("checksum path가 supplied root 밖으로 나갈 수 없다")
        void rejectsPathEscape() throws IOException {
            Path snapshot = copySnapshot();
            Path checksums = snapshot.resolve("SHA256SUMS.txt");
            String content = Files.readString(checksums, StandardCharsets.UTF_8);
            Files.writeString(
                    checksums,
                    replaceRequired(
                            content,
                            "  content/00_EXECUTIVE_ACTIVATION_VERDICT.txt",
                            "  ../00_EXECUTIVE_ACTIVATION_VERDICT.txt"
                    ),
                    StandardCharsets.UTF_8
            );

            assertThatThrownBy(() -> validate(snapshot))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("Authority path escapes root");
        }

        @Test
        @DisplayName("UTF-8 BOM과 quoted delimiter, escaped quote, multiline을 보존해 읽는다")
        void parsesRequiredCsvForms() throws IOException {
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

            Path duplicateHeader = tempDir.resolve("duplicate-header.csv");
            Files.writeString(
                    duplicateHeader,
                    "identity,identity\none,two\n",
                    StandardCharsets.UTF_8
            );
            assertThatThrownBy(() -> ContentActivationPreflight.readCsv(
                    duplicateHeader
            ))
                    .isInstanceOf(ContentActivationPreflight.PreflightException.class)
                    .hasMessageContaining("duplicate");
        }

        @Test
        @DisplayName("runtime package와 migration을 추가하지 않는다")
        void remainsTestOnlyWithoutRuntimeWiring() throws IOException {
            assertThat(Path.of("src/main/java/online/lifeasgame/content"))
                    .doesNotExist();
            try (var migrations = Files.list(Path.of(
                    "src/main/resources/db/migration"
            ))) {
                assertThat(migrations.map(path -> path.getFileName().toString()))
                        .noneMatch(name -> name.contains("content_activation"));
            }
        }
    }

    private ContentActivationPreflight.PreflightReport validate(Path authority) {
        assumeTrue(
                Files.isDirectory(authority),
                "Issue #333 local authority input is required"
        );
        return ContentActivationPreflight.validate(authority);
    }

    private Path copySnapshot() throws IOException {
        return copySnapshot("snapshot");
    }

    private Path copySnapshot(String name) throws IOException {
        assumeTrue(
                Files.isDirectory(AUTHORITY),
                "Issue #333 local authority input is required"
        );
        Path target = tempDir.resolve(name);
        try (var files = Files.walk(AUTHORITY)) {
            for (Path source : files.toList()) {
                Path destination = target.resolve(AUTHORITY.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(source, destination);
                }
            }
        }
        return target;
    }

    private void mutateAndRehash(
            Path snapshot,
            String fileName,
            UnaryOperator<String> mutation
    ) throws IOException {
        String relativeName = "content/" + fileName;
        Path file = snapshot.resolve(relativeName);
        String before = Files.readString(file, StandardCharsets.UTF_8);
        String after = mutation.apply(before);
        assertThat(after).isNotEqualTo(before);
        Files.writeString(file, after, StandardCharsets.UTF_8);

        Path checksums = snapshot.resolve("SHA256SUMS.txt");
        String marker = "  " + relativeName;
        String beforeChecksums = Files.readString(
                checksums,
                StandardCharsets.UTF_8
        );
        String updatedHash = sha256(file);
        String afterChecksums = beforeChecksums.lines()
                .map(line -> line.endsWith(marker)
                        ? updatedHash + marker
                        : line)
                .collect(Collectors.joining(System.lineSeparator()))
                + System.lineSeparator();
        assertThat(afterChecksums).isNotEqualTo(beforeChecksums);
        Files.writeString(checksums, afterChecksums, StandardCharsets.UTF_8);
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(path));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
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
