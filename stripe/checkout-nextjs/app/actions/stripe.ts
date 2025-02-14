'use server';

import { stripe } from "@/lib/stripe";
import { headers } from "next/headers";

export async function fetchClientSecret() {
    const headersList = await headers();
    const origin = headersList.get('origin');

    const session = await stripe.checkout.sessions.create({
        ui_mode: 'embedded',
        line_items: [
            { price: 'price_1QrYzdQNrIGfMhzQLFlERGDl', quantity: 1 },
        ],
        mode: 'payment',
        return_url: `${origin}/payment/success?session_id={CHECKOUT_SESSION_ID}`,
    })

    if (!session.client_secret) throw new Error('Unable to get client_secret')
    return session.client_secret;
};
