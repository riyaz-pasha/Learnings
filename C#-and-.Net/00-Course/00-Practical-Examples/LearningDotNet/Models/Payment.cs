namespace LearningDotNet.Models;

public class Payment
{
    public int Id { get; set; }

    public decimal Amount { get; set; }

    public string Status { get; set; } = "Pending";
}
