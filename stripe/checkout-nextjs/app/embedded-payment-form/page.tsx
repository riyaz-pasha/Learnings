"use client";

import { EmbeddedCheckout, EmbeddedCheckoutProvider } from "@stripe/react-stripe-js";
import { loadStripe } from "@stripe/stripe-js";
import { fetchClientSecret } from "../actions/stripe";

const stripe = loadStripe(process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY);

export default function EmbeddedPaymentFormPage() {
    return (
        <div>
            <EmbeddedCheckoutProvider
                stripe={stripe}
                options={{
                    fetchClientSecret: fetchClientSecret,
                }}>
                <EmbeddedCheckout />
            </EmbeddedCheckoutProvider>
        </div>
    )
}
