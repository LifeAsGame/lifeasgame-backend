package online.lifeasgame.role.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoleEvent transaction contract")
class RoleEventTransactionContractTest {

    @Nested
    @DisplayName("RoleEvent runtime을 변경할 때")
    class WriteBoundary {

        @Test
        @DisplayName("writer는 기존 transaction을 필수로 요구한다")
        void writerRequiresTransaction() {
            Transactional transactional =
                    RoleEventWriter.class.getAnnotation(Transactional.class);

            assertThat(transactional.propagation())
                    .isEqualTo(Propagation.MANDATORY);
        }

        @Test
        @DisplayName("locking read는 새 transaction을 만들지 않고 기존 write transaction을 요구한다")
        void lockingReadRequiresExistingTransaction() throws Exception {
            Method method = RoleEventReader.class.getDeclaredMethod(
                    "getOwnedForUpdate",
                    Long.class,
                    Long.class,
                    Long.class
            );
            Transactional transactional =
                    method.getAnnotation(Transactional.class);

            assertThat(transactional.propagation())
                    .isEqualTo(Propagation.MANDATORY);
            assertThat(transactional.readOnly()).isFalse();
        }
    }
}
