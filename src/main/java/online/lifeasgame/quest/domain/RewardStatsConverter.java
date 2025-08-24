package online.lifeasgame.quest.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter
class RewardStatsConverter implements AttributeConverter<RewardStats, String> {

    private static final ObjectMapper M = new ObjectMapper();
    private static final TypeReference<Map<String, Integer>> T = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(RewardStats attr) {
        try {
            if (attr == null) {
                return null;
            }
            return M.writeValueAsString(attr.stats());
        } catch (Exception e) {
            throw new IllegalArgumentException("serialize rewardStats", e);
        }
    }

    @Override
    public RewardStats convertToEntityAttribute(String db) {
        try {
            if (db == null || db.isBlank()) {
                return RewardStats.empty();
            }
            return new RewardStats(M.readValue(db, T));
        } catch (Exception e) {
            throw new IllegalArgumentException("deserialize rewardStats", e);
        }
    }
}
