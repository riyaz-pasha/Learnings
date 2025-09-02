package com.example.learning.qualifier;

import com.example.learning.common.ConsoleColor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(
            @Qualifier("phonePePaymentService") PaymentService paymentService1,
            PaymentService paymentService2
    ) {
        this.paymentService = paymentService1;
        ConsoleColor.info("paymentService1: " + paymentService1);
        ConsoleColor.info("paymentService2: " + paymentService2);
    }

}
