package com.example.learning.qualifier;

import com.example.learning.common.ConsoleColor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class PhonePePaymentService implements PaymentService {

    @Override
    public void pay(double amount) {
        ConsoleColor.info("Paid " + amount + " using PhonePe");
    }

    @PostConstruct
    public void init() {
        ConsoleColor.info("InitializingBean: PhonePePaymentService");
    }

}
