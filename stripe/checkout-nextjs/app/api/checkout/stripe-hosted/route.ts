import { stripe } from "@/lib/stripe";
import { headers } from "next/headers";
import { NextResponse } from "next/server";

export async function POST() {
    try {
        const headersList = await headers();
        const origin = headersList.get('origin');

        const session = await stripe.checkout.sessions.create({
            line_items: [
                { price: 'price_1QrYzdQNrIGfMhzQLFlERGDl', quantity: 1 },
            ],
            mode: 'payment',
            success_url: `${origin}/payment/success`,
            cancel_url: `${origin}?canceled=true`,
        });

        return NextResponse.redirect(session?.url ?? origin ?? '', 303); // Use only NextResponse.redirect
    } catch (err) {
        console.log("🚀 ~ POST ~ err:", err)
        return NextResponse.json(
            { error: err?.message },
            { status: err?.statusCode || 500 }
        )
    }
}
