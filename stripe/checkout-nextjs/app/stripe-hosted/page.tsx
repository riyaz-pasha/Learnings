export default async function StripeHostedCheckoutPage({ searchParams }: { searchParams: Promise<{ canceled: boolean }> }) {
    const { canceled } = await searchParams;
    if (canceled) {
        alert('Order-canceled')
    }

    return (
        <form
            action={"/api/checkout/stripe-hosted"}
            method="POST"
        >
            <section>
                <button
                    type="submit"
                    role="link"
                    className="focus:outline-none text-white bg-purple-700 hover:bg-purple-800 focus:ring-4 focus:ring-purple-300 font-medium rounded-lg text-sm px-5 py-2.5 mb-2 dark:bg-purple-600 dark:hover:bg-purple-700 dark:focus:ring-purple-900">
                    Stripe Hosted Checkout
                </button>

            </section>
        </form>
    )
}
