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
        List<Certification> certifications = certificationReader.getByCategories(
                CertificationCategory.parse(categories)
        );

        return CertificationResult.Info.fromList(certifications);
    }

    @Transactional
    public CertificationResult.Info create(CertificationCommand.Create command) {
        Certification certification = certificationWriter.create(
                Certification.create(
                        command.name(),
                        command.issuer(),
                        CertificationCategory.parse(command.category())
                )
        );

        return new CertificationResult.Info(
                certification.getId(),
                certification.getName(),
                certification.getIssuer(),
                certification.getCategory().name()
        );
    }

    public CertificationResult.Info getCertification(Long certificationId) {
        Certification certification = certificationReader.getByIdOrThrow(certificationId);
        return CertificationResult.Info.from(certification);
    }

    public CertificationResult.Info update(Long certificationId, CertificationCommand.Update command) {
        CertificationCategory category = CertificationCategory.parse(command.category());

        Certification certification = certificationReader.getByIdOrThrow(certificationId);
        certification.update(
                command.name(),
                command.issuer(),
                category
        );

        return CertificationResult.Info.from(certification);
    }
}
