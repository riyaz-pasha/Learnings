document.addEventListener("DOMContentLoaded", async () => {
    const { publishableKey } = await fetch("/config").then(res => res.json());

    if (!publishableKey) {
        alert('Please set your Stripe publishable API key in the .env file');
    }

    const stripe = String(publishableKey);

    const { clientSecret } = await fetch("/create-payment-intent", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
    }).then(res => res.json());

    const elements = stripe.elements({ clientSecret });
    const paymentElement = elements.create("payment");
    paymentElement.mount("#payment-element");

    const form = document.getElementById("payment-form");
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const { error } = stripe.confirmPayment({
            elements,
            confirmParams: {
                return_url: window.location.href.split("?")[0] + "complete.html",
            }
        })

        if (error) {
            const errorMessageElement = document.getElementById("error-message")
            errorMessageElement.innerText = error.message;
        }
    })
})
