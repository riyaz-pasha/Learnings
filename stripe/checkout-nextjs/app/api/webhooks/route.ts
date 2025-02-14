import { stripe } from "@/lib/stripe";
import { headers } from "next/headers";
import { NextResponse } from "next/server";

export async function POST(req: Request) {
    const reqBody = await req.text();
    const reqHeaders = await headers();
    const signtature = reqHeaders.get('stripe-signature');
    if (!signtature) {
        throw new Error("Stripe stripe-signature required in the headers");
    }

    const webhookSecret = process?.env?.STRIPE_WEBHOOK_SECRET;
    if (!webhookSecret) {
        throw new Error("Missing STRIPE_WEBHOOK_SECRET, required to listen webhook events from strip");
    }

    const event = stripe.webhooks.constructEvent(reqBody, signtature, webhookSecret);

    console.log(`Recieved ${event.type} event`)

    return NextResponse.json({ message: 'Received' }, { status: 200 })
}
