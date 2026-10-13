package online.lifeasgame.character.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.OfficialCertificationImporter;
import online.lifeasgame.character.application.OfficialCertificationImporter.Manifest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty("catalog.hrdk.import-file")
@ConditionalOnNotWebApplication
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OfficialCertificationImportCommand implements ApplicationRunner {
    private final ObjectMapper mapper;
    private final OfficialCertificationImporter importer;
    private final ConfigurableApplicationContext context;

    @Value("${catalog.hrdk.import-file}")
    private String path;

    @Value("${catalog.hrdk.apply:false}")
    private boolean apply;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        Path file = Path.of(path);
        if (!Files.isRegularFile(file) || Files.size(file) > 5_000_000) {
            throw new IllegalArgumentException("Official catalog manifest missing or oversized");
        }
        Manifest manifest = mapper.readValue(Files.readAllBytes(file), Manifest.class);
        OfficialCertificationImporter.Report report = importer.importManifest(manifest, apply);
        System.out.printf("HRDK catalog: validated=%d created=%d updated=%d applied=%b%n",
                report.validated(), report.created(), report.updated(), report.applied());
        // This dedicated command must exit before unrelated startup runners mutate data.
        System.exit(SpringApplication.exit(context));
    }
}
