import { stripe } from "@/lib/stripe";
import { redirect } from "next/navigation";

type SuccessPageProps = {
    searchParams: Promise<{
        session_id: string
    }>
}

export default async function SuccessPage({ searchParams }: SuccessPageProps) {
    const { session_id } = await searchParams;
    console.log("🚀 ~ SuccessPage ~ session_id:", session_id)

    if (!session_id) {
        throw new Error("Please provide a valid session_id");
    }

    const {
        status,
        customer_details: { email: customerEmail },
    } = await stripe.checkout.sessions.retrieve(session_id, {
        expand: ['line_items', 'payment_intent'],
    });

    if (status === 'open') {
        return redirect('/')
    }


    if (status === 'complete') {
        return (
            <section id="success">
                <p>
                    We appreciate your business! A confirmation email will be sent to{' '}
                    {customerEmail}. If you have any questions, please email{' '}
                </p>
                <a href="mailto:orders@example.com">orders@example.com</a>.
            </section>
        )
    }

    return <div>
        <h1>Success</h1>
    </div>
}