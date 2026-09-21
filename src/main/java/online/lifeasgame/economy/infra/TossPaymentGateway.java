package online.lifeasgame.economy.infra;

import online.lifeasgame.economy.application.port.PaymentGateway;
import online.lifeasgame.economy.domain.Currency;
import org.springframework.stereotype.Component;

@Component
public class TossPaymentGateway implements PaymentGateway {

    @Override
    public boolean confirmCharge(String paymentKey, String orderId, long amount, Currency currency) {
        // Reject charges until payment provider verification is implemented.
        return false;
    }
}
