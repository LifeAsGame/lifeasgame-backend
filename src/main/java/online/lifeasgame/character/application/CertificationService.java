package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.CertificationCommand;
import online.lifeasgame.character.application.result.CertificationResult;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationReader certificationReader;
    private final CertificationWriter certificationWriter;

    public List<CertificationResult.Info> getCertifications(List<String> categories) {
        List<Certification> Certifications = certificationReader.getCertifications(CertificationCategory.parse(categories));
        return CertificationResult.Info.fromList(Certifications);
    }

    @Transactional
    public CertificationResult.Info create(CertificationCommand.Create command) {
        Certification certification = certificationWriter.create(
                Certification.of(
                        command.name(),
                        command.issuer(),
                        CertificationCategory.parse(command.category())
                )
        );

        return CertificationResult.Info.of(
                certification.getId(),
                certification.getName(),
                certification.getIssuer(),
                certification.getCategory().name()
        );
    }
}
