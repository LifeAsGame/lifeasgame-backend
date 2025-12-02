package online.lifeasgame.economy.infra;

import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.economy.application.port.PaymentGateway;
import online.lifeasgame.economy.domain.Currency;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TossPaymentGateway implements PaymentGateway {
    @Override
    public boolean confirmCharge(String paymentKey, String orderId, long amount, Currency currency) {
        log.info("Confirming payment: key={} orderId={} amount={} currency={}", paymentKey, orderId, amount, currency);
        return paymentKey != null && !paymentKey.isBlank() && orderId != null && !orderId.isBlank() && amount > 0;
    }
}
