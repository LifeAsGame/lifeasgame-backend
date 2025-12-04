package online.lifeasgame.economy.application.port;

import online.lifeasgame.economy.domain.Currency;

public interface PaymentGateway {
    /**
     * 실제 결제 서비스(Toss 등)를 호출해서 충전 요청을 검증한다.
     * 구현체는 서명 검증/상태 조회 등을 수행해야 한다.
     */
    boolean confirmCharge(String paymentKey, String orderId, long amount, Currency currency);
}
