package online.lifeasgame.person.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.person.api.mapper.PersonWebMapper;
import online.lifeasgame.person.api.request.PersonRequest;
import online.lifeasgame.person.api.response.PersonResponse;
import online.lifeasgame.person.api.spec.PersonApiSpecV1;
import online.lifeasgame.person.application.PersonFacade;
import online.lifeasgame.person.application.result.PersonResult;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/persons")
public class PersonController implements PersonApiSpecV1 {

    private final PersonFacade facade;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<PersonResponse.Detail>> create(
            @Valid @RequestBody PersonRequest.Create request
    ) {
        PersonResult.Detail result = facade.create(
                PersonWebMapper.toCreateCommand(request)
        );
        return ApiResponses.created(
                URI.create("/api/v1/persons/" + result.id()),
                PersonWebMapper.toDetail(result)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonResponse.Detail>>> list() {
        return ApiResponses.ok(facade.list().stream()
                .map(PersonWebMapper::toDetail)
                .toList());
    }

    @Override
    @GetMapping("/{personId}")
    public ResponseEntity<ApiResponse<PersonResponse.Detail>> detail(
            @PathVariable Long personId
    ) {
        return ApiResponses.ok(PersonWebMapper.toDetail(facade.detail(personId)));
    }

    @Override
    @PutMapping("/{personId}")
    public ResponseEntity<ApiResponse<PersonResponse.Detail>> update(
            @PathVariable Long personId,
            @Valid @RequestBody PersonRequest.Update request
    ) {
        return ApiResponses.ok(PersonWebMapper.toDetail(
                facade.update(personId, PersonWebMapper.toUpdateCommand(request))
        ));
    }

    @Override
    @DeleteMapping("/{personId}")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable Long personId) {
        facade.archive(personId);
        return ApiResponses.noContent();
    }
}
