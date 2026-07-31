package online.lifeasgame.quest.application.automation;

import online.lifeasgame.quest.domain.QuestCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestSignalFingerprint")
class QuestSignalFingerprintTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T01:02:03.123456Z");

    private final QuestSignalFingerprint fingerprint =
            new QuestSignalFingerprint();

    @Nested
    @DisplayName("같은 의미의 Payload를 canonicalize할 때")
    class CanonicalizeSamePayload {

        @Test
        @DisplayName("Attributes 입력 순서와 Map 구현이 달라도 같은 SHA-256을 만든다")
        void ignoresAttributeOrder() {
            Map<String, Object> firstAttributes = new LinkedHashMap<>();
            firstAttributes.put("category", "BOOK");
            firstAttributes.put(
                    "metadata",
                    Map.of("second", 2L, "first", 1)
            );
            firstAttributes.put("tags", List.of("quest", "daily"));

            Map<String, Object> secondAttributes = new LinkedHashMap<>();
            secondAttributes.put("tags", List.of("quest", "daily"));
            secondAttributes.put(
                    "metadata",
                    new LinkedHashMap<>(Map.of("first", 1L, "second", 2))
            );
            secondAttributes.put("category", "BOOK");

            String first = fingerprint.fingerprint(signal(firstAttributes, 1));
            String second = fingerprint.fingerprint(signal(secondAttributes, 1));

            assertThat(first)
                    .hasSize(64)
                    .matches("[0-9a-f]{64}")
                    .isEqualTo(second);
            assertThat(fingerprint.legacyFingerprint(
                    signal(firstAttributes, 1)
            )).isEqualTo(fingerprint.legacyFingerprint(
                    signal(secondAttributes, 1)
            ));
        }
    }

    @Nested
    @DisplayName("Legacy Receipt와 비교할 때")
    class LegacyCompatibility {

        @Test
        @DisplayName("PR 이전 canonical schema의 고정 SHA-256을 재현한다")
        void matchesLegacyFixture() {
            QuestSignal signal = signal(
                    Map.of("category", "BOOK"),
                    1
            );
            String legacy = fingerprint.legacyFingerprint(signal);

            assertThat(legacy)
                    .isEqualTo(
                            "506fb834d4304f55afbb1660ebbd1cd90"
                                    + "eeb0663e7b417425138090addcc2b4d"
                    )
                    .isNotEqualTo(fingerprint.fingerprint(signal));
        }
    }

    @Nested
    @DisplayName("Payload 의미가 달라질 때")
    class DistinguishDifferentPayload {

        @Test
        @DisplayName("Progress 또는 Attributes가 다르면 다른 Hash를 만든다")
        void changesFingerprint() {
            String original = fingerprint.fingerprint(
                    signal(Map.of("category", "BOOK"), 1)
            );
            String differentProgress = fingerprint.fingerprint(
                    signal(Map.of("category", "BOOK"), 2)
            );
            String differentAttributes = fingerprint.fingerprint(
                    signal(Map.of("category", "GAME"), 1)
            );

            assertThat(original)
                    .isNotEqualTo(differentProgress)
                    .isNotEqualTo(differentAttributes);
        }

        @Test
        @DisplayName("발생 시각이 다르면 다른 Source Payload로 식별한다")
        void includesOccurredAt() {
            QuestSignal first = signal(Map.of(), 1);
            QuestSignal second = QuestSignal.addProgress(
                            QuestCode.COLLECTION_HUNTER_10,
                            195L,
                            1
                    )
                    .occurredAt(OCCURRED_AT.plusSeconds(1))
                    .correlationId("source:collection:195")
                    .build();

            assertThat(fingerprint.fingerprint(first))
                    .isNotEqualTo(fingerprint.fingerprint(second));
        }

        @Test
        @DisplayName("Acceptance policy와 periodKey를 각각 semantic 값에 포함한다")
        void includesAcceptanceContext() {
            QuestSignal autoCreate = signal(Map.of(), 1);
            QuestSignal existingOnly = QuestSignal.addProgress(
                            QuestCode.COLLECTION_HUNTER_10,
                            195L,
                            1
                    )
                    .occurredAt(OCCURRED_AT)
                    .correlationId("source:collection:195")
                    .acceptancePolicy(
                            QuestSignalAcceptancePolicy.EXISTING_ONLY
                    )
                    .build();
            QuestSignal withPeriodKey = QuestSignal.addProgress(
                            QuestCode.COLLECTION_HUNTER_10,
                            195L,
                            1
                    )
                    .occurredAt(OCCURRED_AT)
                    .correlationId("source:collection:195")
                    .periodKey("2026-W30")
                    .build();

            assertThat(fingerprint.fingerprint(existingOnly))
                    .isNotEqualTo(fingerprint.fingerprint(autoCreate));
            assertThat(fingerprint.fingerprint(withPeriodKey))
                    .isNotEqualTo(fingerprint.fingerprint(autoCreate));
            assertThat(fingerprint.legacyFingerprint(existingOnly))
                    .isEqualTo(fingerprint.legacyFingerprint(autoCreate))
                    .isEqualTo(
                            fingerprint.legacyFingerprint(withPeriodKey)
                    );
        }
    }

    private QuestSignal signal(
            Map<String, Object> attributes,
            int delta
    ) {
        return QuestSignal.addProgress(
                        QuestCode.COLLECTION_HUNTER_10,
                        195L,
                        delta
                )
                .occurredAt(OCCURRED_AT)
                .correlationId("source:collection:195")
                .attributes(attributes)
                .build();
    }
}
