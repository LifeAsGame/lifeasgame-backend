package online.lifeasgame.economy.infra;

import online.lifeasgame.economy.application.port.PaymentGateway;
import online.lifeasgame.economy.domain.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TossPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentGateway.class);
    @Override
    public boolean confirmCharge(String paymentKey, String orderId, long amount, Currency currency) {
        log.info("Confirming payment: key={} orderId={} amount={} currency={}", paymentKey, orderId, amount, currency);
        return paymentKey != null && !paymentKey.isBlank() && orderId != null && !orderId.isBlank() && amount > 0;
    }
}
