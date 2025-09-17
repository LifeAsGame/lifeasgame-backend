package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.CertificationResult;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationReader CertificationReader;

    public List<CertificationResult.CertificationInfo> getCertifications(List<String> categories) {
        List<Certification> Certifications = CertificationReader.getCertifications(CertificationCategory.parse(categories));
        return CertificationResult.CertificationInfo.fromList(Certifications);
    }
}
