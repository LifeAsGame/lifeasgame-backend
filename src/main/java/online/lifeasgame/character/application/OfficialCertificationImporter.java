package online.lifeasgame.character.application;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import online.lifeasgame.character.domain.repository.CertificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfficialCertificationImporter {
    private final CertificationRepository certifications;

    public record Entry(String sourceCode, String name, String issuingAgency, String majorCode, String majorName,
                        String minorCode, String minorName, String administeringAgency, String detail,
                        String detailStatus, String sourceUrl) {}
    public record PageEvidence(int page, int pageSize, int returnedCount, int totalCount) {}
    public record Manifest(boolean complete, int expectedCount, Instant fetchedAt, String listingSourceUrl,
                           List<PageEvidence> pages, List<Entry> items) {}
    public record Report(int validated, int created, int updated, boolean applied) {}

    @Transactional
    public Report importManifest(Manifest manifest, boolean apply) {
        validate(manifest);
        if (!apply) return new Report(manifest.items().size(), 0, 0, false);
        int created = 0;
        int updated = 0;
        for (Entry entry : manifest.items()) {
            Certification certification = certifications.findByProviderAndSourceCode("HRDK", entry.sourceCode())
                    .orElse(null);
            if (certification == null) {
                certification = Certification.official();
                created++;
            } else {
                updated++;
            }
            certification.importOfficial(entry.sourceCode(), entry.name().strip(),
                    entry.issuingAgency() == null ? null : entry.issuingAgency().strip(),
                    entry.majorCode(), entry.majorName(), entry.minorCode(), entry.minorName(),
                    entry.administeringAgency(), entry.detail(), entry.detailStatus(), entry.sourceUrl(),
                    manifest.fetchedAt());
            certifications.save(certification);
        }
        return new Report(manifest.items().size(), created, updated, true);
    }

    private static void validate(Manifest manifest) {
        if (manifest == null || !manifest.complete() || manifest.fetchedAt() == null || manifest.items() == null
                || manifest.items().isEmpty() || manifest.expectedCount() != manifest.items().size()
                || !officialUrl(manifest.listingSourceUrl())) {
            throw new IllegalArgumentException("Incomplete official catalog manifest");
        }
        if (manifest.pages() == null || manifest.pages().isEmpty()) {
            throw new IllegalArgumentException("Missing source page evidence");
        }
        int returned = 0;
        for (int index = 0; index < manifest.pages().size(); index++) {
            PageEvidence page = manifest.pages().get(index);
            if (page == null || page.page() != index + 1 || page.pageSize() < 1
                    || page.returnedCount() < 1 || page.returnedCount() > page.pageSize()
                    || index < manifest.pages().size() - 1 && page.returnedCount() != page.pageSize()
                    || page.totalCount() != manifest.expectedCount()) {
                throw new IllegalArgumentException("Missing or inconsistent source page");
            }
            returned += page.returnedCount();
        }
        if (returned != manifest.expectedCount()) throw new IllegalArgumentException("Incomplete source pages");
        HashSet<String> codes = new HashSet<>();
        for (Entry entry : manifest.items()) {
            if (entry == null || !text(entry.sourceCode(), 64) || !codes.add(entry.sourceCode())
                    || !text(entry.name(), 255)
                    || entry.issuingAgency() != null && !text(entry.issuingAgency(), 255)
                    || !text(entry.majorCode(), 32) || !text(entry.majorName(), 120)
                    || !text(entry.minorCode(), 32) || !text(entry.minorName(), 120)
                    || entry.administeringAgency() != null && !text(entry.administeringAgency(), 255)
                    || !officialUrl(entry.sourceUrl())
                    || entry.detailStatus() == null
                    || !List.of("BASIC", "COMPLETE").contains(entry.detailStatus())
                    || entry.detail() != null && (entry.detail().length() > 4000 || entry.detail().contains("<")
                            || entry.detail().contains(">"))) {
                throw new IllegalArgumentException("Invalid or duplicated official catalog entry");
            }
        }
    }

    private static boolean text(String text, int maxLength) {
        return text != null && !text.isBlank() && text.length() <= maxLength;
    }

    private static boolean officialUrl(String url) {
        if (url == null || url.length() > 500) return false;
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            return "https".equals(uri.getScheme()) && host != null
                    && (host.equals("hrdkorea.or.kr") || host.endsWith(".hrdkorea.or.kr")
                        || host.equals("q-net.or.kr") || host.endsWith(".q-net.or.kr")
                        || host.equals("data.go.kr") || host.endsWith(".data.go.kr"))
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
