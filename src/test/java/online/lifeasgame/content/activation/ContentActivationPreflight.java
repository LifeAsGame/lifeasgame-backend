package online.lifeasgame.content.activation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ContentActivationPreflight {

    private static final Pattern CHECKSUM_LINE = Pattern.compile(
            "^([0-9a-f]{64})  (.+)$"
    );
    private static final String CONTENT = "content";
    private static final String MANIFEST =
            "01_CONSUMER_RUNTIME_ACTIVATION_MANIFEST.csv";
    private static final String REFERENCES =
            "02_REFERENCE_INTEGRITY_MATRIX.csv";
    private static final String COPY_ADDENDUM =
            "07_COPY_AUTHORITY_ADDENDUM.csv";
    private static final String CAPABILITY_MANIFEST =
            "capability/01_CONSUMER_FIRST_COMPLETION_CAPABILITY_MANIFEST.csv";
    private static final String EQUIPMENT_MANIFEST =
            "equipment/01_EQUIPMENT_SLOT_MANIFEST.csv";

    private static final Set<String> MANIFEST_COLUMNS = Set.of(
            "contentType", "stableCode", "definitionVersion",
            "activationWave", "active", "gated", "deferred",
            "lifecycleStatus", "replacementCode", "replacementVersion",
            "priority", "capabilityId", "availabilityStartAt",
            "availabilityEndAt", "releaseCohort", "sortOrder",
            "rewardProfileCode", "itemCode", "equipmentSlotCode",
            "copyKeyReferences", "referenceIntegrityStatus",
            "decisionRationale", "sourceRevision", "approvedBy"
    );
    private static final Set<String> REFERENCE_COLUMNS = Set.of(
            "sourceType", "sourceCode", "sourceVersion", "referenceType",
            "targetType", "targetCode", "targetVersion",
            "requiredForActive", "status", "failureReason",
            "owningAuthority"
    );
    private static final Set<String> COPY_COLUMNS = Set.of(
            "copyKey", "sourceType", "sourceCode", "locale", "text",
            "definitionVersion", "lifecycleStatus", "parameterNames",
            "maxLength", "sensitiveContextRule", "sourceRevision",
            "approvedBy"
    );
    private static final Set<String> CAPABILITY_COLUMNS = Set.of(
            "capabilityId", "firstCompletionClass", "p0OrP1",
            "primaryOwner", "sourceRevision", "approvedBy"
    );
    private static final Set<String> EQUIPMENT_COLUMNS = Set.of(
            "slotCode", "displayName", "category", "semanticRole",
            "p0OrLater", "sortOrder", "enabled", "definitionVersion",
            "lifecycleStatus", "introducedActivationWave",
            "replacementSlotCode", "sourceRevision", "approvedBy"
    );

    private static final Set<ContentIdentity> ACTIVE_QUESTS = Set.of(
            id("QUEST", "Q_RECORD_FIRST_TRACE", "1"),
            id("QUEST", "Q_RECORD_THREE_TRACES", "1"),
            id("QUEST", "Q_RECORD_WEEKLY_LOOKBACK", "1"),
            id("QUEST", "Q_GROWTH_ONE_FOCUS", "1"),
            id("QUEST", "Q_RECOVERY_REST_TEN", "1")
    );
    private static final Set<ContentIdentity> ACTIVE_ROUTES = Set.of(
            id("QUEST_ROUTE", "ROUTE_RECORD_START", "1")
    );
    private static final Set<ContentIdentity> ACTIVE_ROUTE_STEPS = Set.of(
            id("ROUTE_STEP", "RS_RECORD_01_LEAVE_TRACE", "1"),
            id("ROUTE_STEP", "RS_RECORD_02_CONNECT_TRACES", "1"),
            id("ROUTE_STEP", "RS_RECORD_03_LOOK_BACK", "1")
    );
    private static final Set<ContentIdentity> ACTIVE_REWARD_DEFINITIONS =
            Set.of(
                    id("REWARD_DEFINITION", "EXP_PLAYER", "1"),
                    id("REWARD_DEFINITION", "ITEM_DEFINITION", "1")
            );
    private static final Set<ContentIdentity> ACTIVE_REWARD_PROFILES = Set.of(
            id("REWARD_PROFILE", "RP_NONE", "1"),
            id("REWARD_PROFILE", "RP_EXP_TINY_10", "1"),
            id("REWARD_PROFILE", "RP_EXP_AND_ITEM_FIRST_STEP_20", "1")
    );
    private static final Set<ContentIdentity> ACTIVE_ITEMS = Set.of(
            id("ITEM", "IT_FIRST_STEP_FRAGMENT", "1")
    );
    private static final Set<ContentIdentity> ACTIVE_EVENTS = Set.of(
            id("EVENT", "LifeLogRecorded", "1"),
            id("EVENT", "QuestCompleted", "1"),
            id("EVENT", "ItemRewardClaimed", "1")
    );
    private static final Set<String> ZERO_ACTIVE_FAMILIES = Set.of(
            "EQUIPMENT", "CONSUMABLE", "ITEM_SET", "ACHIEVEMENT", "TITLE",
            "REAL_COLLECTION_TYPE"
    );

    private ContentActivationPreflight() {
    }

    static PreflightReport validate(
            Path authorityRoot
    ) {
        Path root = authorityRoot.toAbsolutePath().normalize();
        validateAuthorityFiles(root);
        Path contentRoot = root.resolve(CONTENT);
        Path capabilityManifest = root.resolve(CAPABILITY_MANIFEST);
        Path equipmentManifest = root.resolve(EQUIPMENT_MANIFEST);

        Map<String, CapabilityRow> capabilities = readCapabilities(
                capabilityManifest
        );
        List<ManifestRow> manifest = readManifest(
                contentRoot.resolve(MANIFEST),
                capabilities
        );
        validateApprovedActiveSet(manifest);
        validateEquipmentAuthority(manifest, equipmentManifest);

        ReferenceResult referenceResult = validateReferences(
                contentRoot.resolve(REFERENCES),
                manifest
        );
        CopyResult copyResult = validateCopyPayload(
                contentRoot.resolve(COPY_ADDENDUM),
                manifest
        );

        long active = manifest.stream().filter(ManifestRow::active).count();
        long gated = manifest.stream().filter(ManifestRow::gated).count();
        long deferred = manifest.stream().filter(ManifestRow::deferred).count();

        return new PreflightReport(
                AuthoritySnapshotValidation.MANIFEST_VALID,
                ReferenceDeclarationValidation.REFERENCE_DECLARATIONS_VALID,
                PayloadDeliveryStatus.PAYLOAD_DELIVERY_INCOMPLETE,
                RuntimeVerificationStatus.RUNTIME_NOT_VERIFIED,
                manifest.size(),
                (int) active,
                (int) gated,
                (int) deferred,
                referenceResult.total(),
                referenceResult.activeRequiredResolved(),
                referenceResult.activeRequiredNotApplicable(),
                copyResult.delivered(),
                copyResult.missing(),
                referenceResult.externalReferences()
        );
    }

    static CsvTable readCsv(Path path) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setAllowMissingColumnNames(false)
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
                .get();
        try (Reader reader = utf8BomAwareReader(path);
             CSVParser parser = format.parse(reader)) {
            List<String> headers = List.copyOf(parser.getHeaderNames());
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    fail(path + ": inconsistent column count at record "
                            + record.getRecordNumber());
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : headers) {
                    row.put(header, record.get(header));
                }
                rows.add(Map.copyOf(row));
            }
            return new CsvTable(headers, List.copyOf(rows));
        } catch (IOException | IllegalArgumentException exception) {
            throw new PreflightException(
                    "Cannot parse CSV " + path + ": " + exception.getMessage(),
                    exception
            );
        }
    }

    private static BufferedReader utf8BomAwareReader(Path path)
            throws IOException {
        BufferedReader reader = Files.newBufferedReader(
                path,
                StandardCharsets.UTF_8
        );
        reader.mark(1);
        if (reader.read() != '\uFEFF') {
            reader.reset();
        }
        return reader;
    }

    private static void validateAuthorityFiles(Path root) {
        Path checksumFile = root.resolve("SHA256SUMS.txt");
        Set<Path> expectedFiles = new HashSet<>();
        try {
            for (String line : Files.readAllLines(
                    checksumFile,
                    StandardCharsets.UTF_8
            )) {
                Matcher matcher = CHECKSUM_LINE.matcher(line);
                if (!matcher.matches()) {
                    fail("Malformed SHA256SUMS line: " + line);
                }
                Path file = resolveWithin(root, matcher.group(2));
                Path relative = root.relativize(file);
                if (!expectedFiles.add(relative)) {
                    fail("Duplicate SHA256SUMS path: " + relative);
                }
                requireHash(file, matcher.group(1));
            }
        } catch (IOException exception) {
            throw new PreflightException(
                    "Cannot read authority checksums " + checksumFile,
                    exception
            );
        }
        if (expectedFiles.size() != 21
                || countUnder(expectedFiles, "content") != 8
                || countUnder(expectedFiles, "capability") != 6
                || countUnder(expectedFiles, "equipment") != 7) {
            fail("Authority checksum inventory must be content=8, capability=6, "
                    + "equipment=7");
        }

        Set<Path> actualFiles = new HashSet<>();
        for (String directory : List.of("content", "capability", "equipment")) {
            Path path = root.resolve(directory);
            try (var files = Files.walk(path)) {
                files.filter(Files::isRegularFile)
                        .map(root::relativize)
                        .forEach(actualFiles::add);
            } catch (IOException exception) {
                throw new PreflightException(
                        "Cannot inspect authority directory " + path,
                        exception
                );
            }
        }
        if (!actualFiles.equals(expectedFiles)) {
            fail("Authority file inventory differs from SHA256SUMS");
        }
    }

    private static long countUnder(Set<Path> paths, String directory) {
        return paths.stream()
                .filter(path -> path.getNameCount() > 1)
                .filter(path -> path.getName(0).toString().equals(directory))
                .count();
    }

    private static Path resolveWithin(Path root, String relativeName) {
        Path relative = Path.of(relativeName);
        if (relative.isAbsolute()) {
            fail("Authority path must be relative: " + relativeName);
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            fail("Authority path escapes root: " + relativeName);
        }
        try {
            Path realRoot = root.toRealPath();
            Path realFile = resolved.toRealPath();
            if (!realFile.startsWith(realRoot)) {
                fail("Authority path escapes root through a link: " + relativeName);
            }
        } catch (IOException exception) {
            throw new PreflightException(
                    "Cannot resolve authority file " + relativeName,
                    exception
            );
        }
        return resolved;
    }

    private static Map<String, CapabilityRow> readCapabilities(Path path) {
        CsvTable table = readCsv(path);
        requireHeaders(path, table.headers(), CAPABILITY_COLUMNS);
        Map<String, CapabilityRow> rows = new HashMap<>();
        for (Map<String, String> row : table.rows()) {
            CapabilityRow capability = new CapabilityRow(
                    requireText(row, "capabilityId"),
                    requireText(row, "firstCompletionClass"),
                    requireText(row, "p0OrP1"),
                    requireText(row, "primaryOwner")
            );
            requireText(row, "sourceRevision");
            requireText(row, "approvedBy");
            if (!Set.of(
                    "REQUIRED_ACTIVE", "REQUIRED_READ_ONLY", "GATED", "DEFERRED"
            ).contains(capability.firstCompletionClass())) {
                fail("Invalid capability class for " + capability.id());
            }
            if (rows.putIfAbsent(capability.id(), capability) != null) {
                fail("Duplicate capabilityId: " + capability.id());
            }
        }
        return Map.copyOf(rows);
    }

    private static List<ManifestRow> readManifest(
            Path path,
            Map<String, CapabilityRow> capabilities
    ) {
        CsvTable table = readCsv(path);
        requireHeaders(path, table.headers(), MANIFEST_COLUMNS);
        List<ManifestRow> rows = new ArrayList<>();
        Set<ContentIdentity> identities = new HashSet<>();
        for (Map<String, String> raw : table.rows()) {
            ContentIdentity identity = id(
                    requireText(raw, "contentType"),
                    requireText(raw, "stableCode"),
                    requireText(raw, "definitionVersion")
            );
            boolean active = strictBoolean(raw, "active", identity);
            boolean gated = strictBoolean(raw, "gated", identity);
            boolean deferred = strictBoolean(raw, "deferred", identity);
            if ((active ? 1 : 0) + (gated ? 1 : 0) + (deferred ? 1 : 0) != 1) {
                fail("Exactly one state is required for " + identity);
            }
            String expectedLifecycle = active
                    ? "ACTIVE"
                    : gated ? "GATED" : "DEFERRED";
            String lifecycle = requireText(raw, "lifecycleStatus");
            if (!lifecycle.equals(expectedLifecycle)) {
                fail("Lifecycle/state mismatch for " + identity);
            }
            if (!identities.add(identity)) {
                fail("Duplicate stable identity: " + identity);
            }
            requireText(raw, "sourceRevision");
            requireText(raw, "approvedBy");
            requireText(raw, "releaseCohort");
            int sortOrder = parseSortOrder(raw.get("sortOrder"), identity);
            validateWindow(raw.get("availabilityStartAt"),
                    raw.get("availabilityEndAt"), identity);
            validateReplacement(raw, identity);
            String referenceIntegrity = requireText(
                    raw,
                    "referenceIntegrityStatus"
            );
            if (!Set.of("RESOLVED", "PENDING_AUTHORITY")
                    .contains(referenceIntegrity)
                    || (active && !referenceIntegrity.equals("RESOLVED"))) {
                fail("Invalid manifest reference status for " + identity);
            }

            String binding = requireText(raw, "capabilityId");
            for (String capabilityId : binding.split(";", -1)) {
                if (capabilityId.isEmpty() || !capabilities.containsKey(capabilityId)) {
                    fail("Unknown capabilityId " + capabilityId + " for " + identity);
                }
            }

            rows.add(new ManifestRow(
                    identity,
                    active,
                    gated,
                    deferred,
                    lifecycle,
                    raw.get("equipmentSlotCode"),
                    raw.get("replacementCode"),
                    raw.get("replacementVersion"),
                    sortOrder,
                    raw.get("rewardProfileCode"),
                    raw.get("itemCode")
            ));
        }
        return List.copyOf(rows);
    }

    private static void validateApprovedActiveSet(List<ManifestRow> rows) {
        requireExactActive(rows, "QUEST", ACTIVE_QUESTS);
        requireExactActive(rows, "QUEST_ROUTE", ACTIVE_ROUTES);
        requireExactActive(rows, "ROUTE_STEP", ACTIVE_ROUTE_STEPS);
        requireExactActive(
                rows,
                "REWARD_DEFINITION",
                ACTIVE_REWARD_DEFINITIONS
        );
        requireExactActive(rows, "REWARD_PROFILE", ACTIVE_REWARD_PROFILES);
        requireExactActive(rows, "ITEM", ACTIVE_ITEMS);
        requireExactActive(rows, "EVENT", ACTIVE_EVENTS);
        for (String family : ZERO_ACTIVE_FAMILIES) {
            requireExactActive(rows, family, Set.of());
        }
        if (activeIdentities(rows, "COPY").size() != 74) {
            fail("Active COPY set must contain exactly 74 identities");
        }

        requirePayloadFields(
                rows,
                id("QUEST", "Q_RECORD_FIRST_TRACE", "1"),
                10,
                "RP_EXP_TINY_10",
                ""
        );
        requirePayloadFields(
                rows,
                id("QUEST", "Q_RECORD_THREE_TRACES", "1"),
                20,
                "RP_EXP_AND_ITEM_FIRST_STEP_20",
                "IT_FIRST_STEP_FRAGMENT"
        );
        for (ContentIdentity quest : Set.of(
                id("QUEST", "Q_RECORD_WEEKLY_LOOKBACK", "1"),
                id("QUEST", "Q_GROWTH_ONE_FOCUS", "1"),
                id("QUEST", "Q_RECOVERY_REST_TEN", "1")
        )) {
            requirePayloadFields(rows, quest, switch (quest.stableCode()) {
                case "Q_RECORD_WEEKLY_LOOKBACK" -> 30;
                case "Q_GROWTH_ONE_FOCUS" -> 40;
                default -> 50;
            }, "RP_NONE", "");
        }
        requirePayloadFields(
                rows,
                id("QUEST_ROUTE", "ROUTE_RECORD_START", "1"),
                10,
                "RP_NONE",
                ""
        );
        requirePayloadFields(
                rows,
                id("ROUTE_STEP", "RS_RECORD_01_LEAVE_TRACE", "1"),
                1,
                "",
                ""
        );
        requirePayloadFields(
                rows,
                id("ROUTE_STEP", "RS_RECORD_02_CONNECT_TRACES", "1"),
                2,
                "",
                ""
        );
        requirePayloadFields(
                rows,
                id("ROUTE_STEP", "RS_RECORD_03_LOOK_BACK", "1"),
                3,
                "",
                ""
        );
    }

    private static void requirePayloadFields(
            List<ManifestRow> rows,
            ContentIdentity identity,
            int sortOrder,
            String rewardProfileCode,
            String itemCode
    ) {
        ManifestRow row = rows.stream()
                .filter(candidate -> candidate.identity().equals(identity))
                .findFirst()
                .orElseThrow(() -> new PreflightException(
                        "Missing approved identity " + identity
                ));
        if (row.sortOrder() != sortOrder
                || !row.rewardProfileCode().equals(rewardProfileCode)
                || !row.itemCode().equals(itemCode)) {
            fail("Approved payload fields differ for " + identity);
        }
    }

    private static void requireExactActive(
            List<ManifestRow> rows,
            String type,
            Set<ContentIdentity> expected
    ) {
        Set<ContentIdentity> actual = activeIdentities(rows, type);
        if (!actual.equals(expected)) {
            fail("Unexpected active " + type + " set: " + new TreeSet<>(actual));
        }
    }

    private static Set<ContentIdentity> activeIdentities(
            List<ManifestRow> rows,
            String type
    ) {
        Set<ContentIdentity> identities = new HashSet<>();
        rows.stream()
                .filter(ManifestRow::active)
                .map(ManifestRow::identity)
                .filter(identity -> identity.contentType().equals(type))
                .forEach(identities::add);
        return Set.copyOf(identities);
    }

    private static void validateEquipmentAuthority(
            List<ManifestRow> manifest,
            Path equipmentManifest
    ) {
        CsvTable table = readCsv(equipmentManifest);
        requireHeaders(equipmentManifest, table.headers(), EQUIPMENT_COLUMNS);
        Map<ContentIdentity, ManifestRow> manifestSlots = new HashMap<>();
        manifest.stream()
                .filter(row -> row.identity().contentType().equals("EQUIPMENT_SLOT"))
                .forEach(row -> manifestSlots.put(row.identity(), row));

        Set<ContentIdentity> equipmentIdentities = new HashSet<>();
        for (Map<String, String> raw : table.rows()) {
            ContentIdentity identity = id(
                    "EQUIPMENT_SLOT",
                    requireText(raw, "slotCode"),
                    requireText(raw, "definitionVersion")
            );
            if (!equipmentIdentities.add(identity)) {
                fail("Duplicate Equipment Slot identity: " + identity);
            }
            boolean enabled = strictBoolean(raw, "enabled", identity);
            String lifecycle = requireText(raw, "lifecycleStatus");
            if (enabled != lifecycle.equals("ACTIVE")) {
                fail("Equipment enabled/lifecycle mismatch for " + identity);
            }
            parseSortOrder(raw.get("sortOrder"), identity);
            requireText(raw, "sourceRevision");
            requireText(raw, "approvedBy");

            ManifestRow content = manifestSlots.get(identity);
            if (content == null
                    || content.active() != enabled
                    || content.gated() == enabled
                    || !content.lifecycle().equals(lifecycle)
                    || !identity.stableCode().equals(content.equipmentSlotCode())) {
                fail("Content/Equipment authority mismatch for " + identity);
            }
        }
        if (equipmentIdentities.size() != 17
                || !equipmentIdentities.equals(manifestSlots.keySet())) {
            fail("Equipment authority must match all 17 manifest slot identities");
        }
    }

    private static ReferenceResult validateReferences(
            Path path,
            List<ManifestRow> manifest
    ) {
        CsvTable table = readCsv(path);
        requireHeaders(path, table.headers(), REFERENCE_COLUMNS);
        Map<ContentIdentity, ManifestRow> manifestByIdentity = new HashMap<>();
        Set<String> manifestTypes = new HashSet<>();
        for (ManifestRow row : manifest) {
            manifestByIdentity.put(row.identity(), row);
            manifestTypes.add(row.identity().contentType());
        }

        Set<String> declarationKeys = new HashSet<>();
        int activeRequiredResolved = 0;
        int activeRequiredNotApplicable = 0;
        List<ExternalReference> external = new ArrayList<>();
        Map<ContentIdentity, List<ContentIdentity>> replacementTargets =
                new HashMap<>();
        Map<ContentIdentity, Set<ContentIdentity>> requiredQuestTargets =
                new HashMap<>();
        Set<ContentIdentity> activeRouteSteps = new HashSet<>();
        for (Map<String, String> raw : table.rows()) {
            ContentIdentity source = id(
                    requireText(raw, "sourceType"),
                    requireText(raw, "sourceCode"),
                    requireText(raw, "sourceVersion")
            );
            ContentIdentity target = id(
                    requireText(raw, "targetType"),
                    requireText(raw, "targetCode"),
                    requireText(raw, "targetVersion")
            );
            String referenceType = requireText(raw, "referenceType");
            boolean required = strictBoolean(raw, "requiredForActive", source);
            String status = requireText(raw, "status");
            if (!Set.of("RESOLVED", "NOT_APPLICABLE", "PENDING_AUTHORITY")
                    .contains(status)) {
                fail("Invalid reference status for " + source + " -> " + target);
            }
            String declarationKey = source + "|" + referenceType + "|" + target;
            if (!declarationKeys.add(declarationKey)) {
                fail("Duplicate reference declaration: " + declarationKey);
            }
            if (referenceType.equals("REPLACED_BY")) {
                replacementTargets.computeIfAbsent(
                        source,
                        ignored -> new ArrayList<>()
                ).add(target);
            }
            if (required && status.equals("RESOLVED")
                    && referenceType.equals("REQUIRED_QUEST")
                    && ACTIVE_ROUTE_STEPS.contains(source)) {
                requiredQuestTargets.computeIfAbsent(
                        source,
                        ignored -> new HashSet<>()
                ).add(target);
            }
            if (required && status.equals("RESOLVED")
                    && referenceType.equals("ROUTE_STEP")
                    && source.equals(id(
                            "QUEST_ROUTE",
                            "ROUTE_RECORD_START",
                            "1"
                    ))) {
                activeRouteSteps.add(target);
            }

            ManifestRow sourceRow = manifestByIdentity.get(source);
            if (manifestTypes.contains(source.contentType()) && sourceRow == null) {
                fail("Reference source is missing from manifest: " + source);
            }
            boolean activeSource = sourceRow != null && sourceRow.active();
            if (activeSource && required) {
                if (status.equals("RESOLVED")) {
                    activeRequiredResolved++;
                } else if (status.equals("NOT_APPLICABLE")) {
                    activeRequiredNotApplicable++;
                } else {
                    fail("Active required reference is unresolved: "
                            + declarationKey);
                }
            }

            ManifestRow targetRow = manifestByIdentity.get(target);
            if (targetRow != null) {
                if (activeSource && required && !targetRow.active()) {
                    fail("Active required internal target is not active: " + target);
                }
                continue;
            }

            if (manifestTypes.contains(target.contentType())) {
                if (status.equals("RESOLVED") || activeSource) {
                    fail("Resolved manifest target is missing: " + target
                            + " from " + source);
                }
                if (!status.equals("PENDING_AUTHORITY")
                        || raw.get("failureReason").isBlank()
                        || raw.get("owningAuthority").isBlank()) {
                    fail("Missing manifest target lacks owned pending dependency: "
                            + target);
                }
                external.add(new ExternalReference(
                        source,
                        target,
                        ExternalReferenceClassification
                                .EXTERNALLY_OWNED_PENDING_DEPENDENCY,
                        raw.get("owningAuthority")
                ));
                continue;
            }

            ExternalReferenceClassification classification =
                    classifyExternal(raw, target);
            external.add(new ExternalReference(
                    source,
                    target,
                    classification,
                    requireText(raw, "owningAuthority")
            ));
        }
        validateReplacementDeclarations(manifest, replacementTargets);
        validateRouteReferences(requiredQuestTargets, activeRouteSteps);
        external.sort(Comparator
                .comparing((ExternalReference edge) -> edge.target().toString())
                .thenComparing(edge -> edge.source().toString()));
        return new ReferenceResult(
                table.rows().size(),
                activeRequiredResolved,
                activeRequiredNotApplicable,
                List.copyOf(external)
        );
    }

    private static void validateRouteReferences(
            Map<ContentIdentity, Set<ContentIdentity>> requiredQuestTargets,
            Set<ContentIdentity> activeRouteSteps
    ) {
        Map<ContentIdentity, Set<ContentIdentity>> expectedQuestTargets = Map.of(
                id("ROUTE_STEP", "RS_RECORD_01_LEAVE_TRACE", "1"),
                Set.of(id("QUEST", "Q_RECORD_FIRST_TRACE", "1")),
                id("ROUTE_STEP", "RS_RECORD_02_CONNECT_TRACES", "1"),
                Set.of(id("QUEST", "Q_RECORD_THREE_TRACES", "1")),
                id("ROUTE_STEP", "RS_RECORD_03_LOOK_BACK", "1"),
                Set.of(id("QUEST", "Q_RECORD_WEEKLY_LOOKBACK", "1"))
        );
        if (!requiredQuestTargets.equals(expectedQuestTargets)
                || !activeRouteSteps.equals(ACTIVE_ROUTE_STEPS)) {
            fail("Approved Route/Step reference chain differs");
        }
    }

    private static void validateReplacementDeclarations(
            List<ManifestRow> manifest,
            Map<ContentIdentity, List<ContentIdentity>> replacementTargets
    ) {
        for (ManifestRow row : manifest) {
            if (row.replacementCode().isEmpty()) {
                continue;
            }
            List<ContentIdentity> targets = replacementTargets.getOrDefault(
                    row.identity(),
                    List.of()
            );
            if (targets.size() != 1
                    || !targets.getFirst().stableCode()
                    .equals(row.replacementCode())
                    || !targets.getFirst().definitionVersion()
                    .equals(row.replacementVersion())) {
                fail("Replacement requires one explicit typed declaration for "
                        + row.identity());
            }
        }
    }

    private static ExternalReferenceClassification classifyExternal(
            Map<String, String> raw,
            ContentIdentity target
    ) {
        if (target.contentType().equals("CAPABILITY_MANIFEST")) {
            requireExternalIdentity(
                    target,
                    "AUTH-CFC-001",
                    "v1"
            );
            return ExternalReferenceClassification.DELIVERED_AUTHORITY_INPUT;
        }
        if (target.contentType().equals("EQUIPMENT_SLOT_AUTHORITY")) {
            requireExternalIdentity(
                    target,
                    "LAG-EQSA",
                    "1.0.0"
            );
            return ExternalReferenceClassification.DELIVERED_AUTHORITY_INPUT;
        }
        if (raw.get("status").equals("NOT_APPLICABLE")) {
            return ExternalReferenceClassification.NOT_APPLICABLE;
        }
        return ExternalReferenceClassification.DECLARED_EXTERNAL_AUTHORITY;
    }

    private static void requireExternalIdentity(
            ContentIdentity target,
            String expectedCode,
            String expectedVersion
    ) {
        if (!target.stableCode().equals(expectedCode)
                || !target.definitionVersion().equals(expectedVersion)) {
            fail("External authority identity mismatch: " + target);
        }
    }

    private static CopyResult validateCopyPayload(
            Path path,
            List<ManifestRow> manifest
    ) {
        CsvTable table = readCsv(path);
        requireHeaders(path, table.headers(), COPY_COLUMNS);
        Set<ContentIdentity> activeCopy = activeIdentities(manifest, "COPY");
        Set<ContentIdentity> delivered = new HashSet<>();
        for (Map<String, String> raw : table.rows()) {
            ContentIdentity identity = id(
                    "COPY",
                    requireText(raw, "copyKey"),
                    requireText(raw, "definitionVersion")
            );
            if (!delivered.add(identity)) {
                fail("Duplicate Copy payload identity: " + identity);
            }
            if (!activeCopy.contains(identity)) {
                fail("Copy addendum row is not an active manifest identity: "
                        + identity);
            }
            requireText(raw, "text");
            requireText(raw, "sourceRevision");
            requireText(raw, "approvedBy");
            if (!requireText(raw, "lifecycleStatus").equals("ACTIVE")) {
                fail("Copy addendum row must be ACTIVE: " + identity);
            }
            parseSortOrder(raw.get("maxLength"), identity);
        }
        Set<ContentIdentity> missing = new TreeSet<>(activeCopy);
        missing.removeAll(delivered);
        return new CopyResult(delivered.size(), List.copyOf(missing));
    }

    private static void validateReplacement(
            Map<String, String> raw,
            ContentIdentity identity
    ) {
        boolean codePresent = !raw.get("replacementCode").isEmpty();
        boolean versionPresent = !raw.get("replacementVersion").isEmpty();
        if (codePresent != versionPresent) {
            fail("Replacement code/version must be supplied together for " + identity);
        }
    }

    private static void validateWindow(
            String startText,
            String endText,
            ContentIdentity identity
    ) {
        Instant start = parseOptionalInstant(startText, "start", identity);
        Instant end = parseOptionalInstant(endText, "end", identity);
        if (start != null && end != null && !start.isBefore(end)) {
            fail("Availability window must have start before end for " + identity);
        }
    }

    private static Instant parseOptionalInstant(
            String value,
            String field,
            ContentIdentity identity
    ) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new PreflightException(
                    "Malformed availability " + field + " for " + identity,
                    exception
            );
        }
    }

    private static int parseSortOrder(String value, ContentIdentity identity) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                fail("sortOrder must be non-negative for " + identity);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new PreflightException(
                    "Malformed numeric value for " + identity + ": " + value,
                    exception
            );
        }
    }

    private static boolean strictBoolean(
            Map<String, String> row,
            String field,
            ContentIdentity identity
    ) {
        String value = Objects.requireNonNull(row.get(field), field);
        if (value.equals("true")) {
            return true;
        }
        if (value.equals("false")) {
            return false;
        }
        fail("Invalid boolean " + field + "=" + value + " for " + identity);
        return false;
    }

    private static void requireHeaders(
            Path path,
            List<String> actual,
            Set<String> required
    ) {
        Set<String> missing = new TreeSet<>(required);
        missing.removeAll(actual);
        if (!missing.isEmpty()) {
            fail(path + ": missing required headers " + missing);
        }
    }

    private static String requireText(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) {
            fail("Required field is blank: " + field);
        }
        return value;
    }

    private static void requireHash(Path path, String expected) {
        String actual;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            actual = HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new PreflightException("Cannot hash " + path, exception);
        }
        if (!actual.equals(expected)) {
            fail("SHA-256 mismatch for " + path.getFileName()
                    + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static ContentIdentity id(String type, String code, String version) {
        return new ContentIdentity(type, code, version);
    }

    private static void fail(String message) {
        throw new PreflightException(message);
    }

    enum AuthoritySnapshotValidation {
        MANIFEST_VALID
    }

    enum ReferenceDeclarationValidation {
        REFERENCE_DECLARATIONS_VALID
    }

    enum PayloadDeliveryStatus {
        PAYLOAD_DELIVERY_INCOMPLETE
    }

    enum RuntimeVerificationStatus {
        RUNTIME_NOT_VERIFIED
    }

    enum ExternalReferenceClassification {
        DELIVERED_AUTHORITY_INPUT,
        DECLARED_EXTERNAL_AUTHORITY,
        EXTERNALLY_OWNED_PENDING_DEPENDENCY,
        NOT_APPLICABLE
    }

    record PreflightReport(
            AuthoritySnapshotValidation authoritySnapshotValidation,
            ReferenceDeclarationValidation referenceDeclarationValidation,
            PayloadDeliveryStatus payloadDeliveryStatus,
            RuntimeVerificationStatus runtimeVerificationStatus,
            int manifestRows,
            int activeRows,
            int gatedRows,
            int deferredRows,
            int referenceRows,
            int activeRequiredReferencesResolved,
            int activeRequiredReferencesNotApplicable,
            int deliveredCopyPayloads,
            List<ContentIdentity> missingCopyPayloads,
            List<ExternalReference> externalReferences
    ) {
    }

    record CsvTable(List<String> headers, List<Map<String, String>> rows) {
    }

    record ContentIdentity(
            String contentType,
            String stableCode,
            String definitionVersion
    ) implements Comparable<ContentIdentity> {

        @Override
        public int compareTo(ContentIdentity other) {
            int type = contentType.compareTo(other.contentType);
            if (type != 0) {
                return type;
            }
            int code = stableCode.compareTo(other.stableCode);
            return code != 0
                    ? code
                    : definitionVersion.compareTo(other.definitionVersion);
        }

        @Override
        public String toString() {
            return contentType + ":" + stableCode + "@" + definitionVersion;
        }
    }

    record ExternalReference(
            ContentIdentity source,
            ContentIdentity target,
            ExternalReferenceClassification classification,
            String owningAuthority
    ) {
    }

    private record CapabilityRow(
            String id,
            String firstCompletionClass,
            String tier,
            String owner
    ) {
    }

    private record ManifestRow(
            ContentIdentity identity,
            boolean active,
            boolean gated,
            boolean deferred,
            String lifecycle,
            String equipmentSlotCode,
            String replacementCode,
            String replacementVersion,
            int sortOrder,
            String rewardProfileCode,
            String itemCode
    ) {
    }

    private record ReferenceResult(
            int total,
            int activeRequiredResolved,
            int activeRequiredNotApplicable,
            List<ExternalReference> externalReferences
    ) {
    }

    private record CopyResult(
            int delivered,
            List<ContentIdentity> missing
    ) {
    }

    static final class PreflightException extends RuntimeException {

        PreflightException(String message) {
            super(message);
        }

        PreflightException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
