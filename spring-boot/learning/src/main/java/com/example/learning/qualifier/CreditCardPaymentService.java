package com.example.learning.qualifier;

import com.example.learning.common.ConsoleColor;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class CreditCardPaymentService implements PaymentService {

    @Override
    public void pay(double amount) {
        ConsoleColor.info("Paid " + amount + " using Credit Card");
    }

    @PostConstruct
    public void init() {
        ConsoleColor.info("InitializingBean: CreditCardPaymentService");
    }

}
