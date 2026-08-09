package online.lifeasgame.lifelog.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.query.ExerciseQuery;
import online.lifeasgame.lifelog.application.query.MediaLogQuery;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
import online.lifeasgame.lifelog.quick.application.QuickRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LifeLogArchitectureAlignmentTest {

    private static final Long PLAYER_ID = 241L;

    @Test
    void simpleFacadesAreRemoved() {
        for (String className : Set.of(
                "online.lifeasgame.lifelog.application.CollectionLogFacade",
                "online.lifeasgame.lifelog.application.ExerciseLogFacade",
                "online.lifeasgame.lifelog.application.MediaLogFacade",
                "online.lifeasgame.lifelog.quick.application.QuickRecordFacade"
        )) {
            assertThatThrownBy(() -> Class.forName(className))
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Test
    void selfIdentityLivesInApplicationServicesAndQueries() {
        for (Class<?> type : List.of(
                CollectionLogService.class,
                ExerciseLogService.class,
                MediaLogService.class,
                CollectionLogQueryService.class,
                ExerciseLogQueryService.class,
                MediaLogQueryService.class,
                QuickRecordService.class
        )) {
            assertThat(fieldTypes(type)).contains(CurrentPlayerAccessor.class);
        }
    }

    @Test
    void queryServicesAreReadOnlyAndResolveCurrentPlayer() {
        CollectionLogReader collectionReader = mock(CollectionLogReader.class);
        ExerciseLogReader exerciseReader = mock(ExerciseLogReader.class);
        MediaLogReader mediaReader = mock(MediaLogReader.class);
        CurrentPlayerAccessor accessor = mock(CurrentPlayerAccessor.class);
        given(accessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
        given(collectionReader.recent(PLAYER_ID, 5)).willReturn(List.of());
        given(exerciseReader.search(
                PLAYER_ID, null, null, null, 0, 20
        )).willReturn(List.of());
        given(mediaReader.search(
                PLAYER_ID, null, null, null, 0, 20
        )).willReturn(List.of());

        assertThat(new CollectionLogQueryService(collectionReader, accessor)
                .recent(5)).isEmpty();
        assertThat(new ExerciseLogQueryService(exerciseReader, accessor)
                .search(new ExerciseQuery.Search(null, null, null, 0, 20)))
                .isEmpty();
        assertThat(new MediaLogQueryService(mediaReader, accessor)
                .search(new MediaLogQuery.Search(null, null, null, 0, 20)))
                .isEmpty();

        verify(collectionReader).recent(PLAYER_ID, 5);
        verify(exerciseReader).search(PLAYER_ID, null, null, null, 0, 20);
        verify(mediaReader).search(PLAYER_ID, null, null, null, 0, 20);

        for (Class<?> type : List.of(
                CollectionLogQueryService.class,
                ExerciseLogQueryService.class,
                MediaLogQueryService.class
        )) {
            Transactional transactional = type.getAnnotation(Transactional.class);
            assertThat(transactional).isNotNull();
            assertThat(transactional.readOnly()).isTrue();
        }
    }

    @Test
    void adminExplicitPlayerPathsRemainAvailable() throws Exception {
        assertMethod(CollectionLogService.class, "create", Long.class);
        assertMethod(ExerciseLogService.class, "create", Long.class);
        assertMethod(MediaLogService.class, "create", Long.class);
        assertMethod(CollectionLogQueryService.class, "recent", Long.class);
        assertMethod(ExerciseLogQueryService.class, "recent", Long.class);
        assertMethod(MediaLogQueryService.class, "recent", Long.class);
    }

    @Test
    void registrarExposesNoRepositoryContract() {
        assertThat(Arrays.stream(LifeLogRecordRegistrar.class.getDeclaredMethods())
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .flatMap(method -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(method.getReturnType()),
                        Arrays.stream(method.getParameterTypes())
                ))
                .map(Class::getPackageName))
                .noneMatch(name -> name.contains(".repository") || name.contains(".infra"));
    }

    private static Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(field -> field.getType())
                .collect(java.util.stream.Collectors.toSet());
    }

    private static void assertMethod(
            Class<?> type,
            String methodName,
            Class<?> firstParameter
    ) {
        assertThat(Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .map(Method::getParameterTypes)
                .filter(parameters -> parameters.length > 0)
                .map(parameters -> parameters[0])
                .anyMatch(firstParameter::equals))
                .isTrue();
    }
}
