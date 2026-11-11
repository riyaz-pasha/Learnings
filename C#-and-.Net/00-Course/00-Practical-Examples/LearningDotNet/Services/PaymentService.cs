using LearningDotNet.Models;

namespace LearningDotNet.Services;

public class PaymentService
{
    public void ProcessPayment(Payment payment)
    {
        Console.WriteLine($"Processing Payment of {payment.Amount}");
        payment.Status = "Completed";
    }
}
