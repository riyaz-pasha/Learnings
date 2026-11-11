using LearningDotNet.Models;
using LearningDotNet.Services;

var paymentService = new PaymentService();
var payment = new Payment
{
    Id = 1,
    Amount = 1500
};

paymentService.ProcessPayment(payment);

Console.WriteLine($"Payment Status: {payment.Status}");
