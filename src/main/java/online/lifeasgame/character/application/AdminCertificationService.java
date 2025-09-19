package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.AdminCertificationCommand;
import online.lifeasgame.character.application.result.AdminCertificationResult;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.CertificationCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCertificationService {

    private final CertificationWriter CertificationWriter;

    @Transactional
    public AdminCertificationResult.CertificationInfo create(AdminCertificationCommand.CreateCertification command) {
        Certification certification = CertificationWriter.create(
                Certification.of(
                        command.name(),
                        command.issuer(),
                        CertificationCategory.parse(command.category())
                )
        );

        return AdminCertificationResult.CertificationInfo.of(
                certification.getName(),
                certification.getIssuer(),
                certification.getCategory().name()
        );
    }
}
