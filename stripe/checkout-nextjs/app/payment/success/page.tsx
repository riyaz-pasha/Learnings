import { stripe } from "@/lib/stripe";
import Link from "next/link";
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
                <div>
                    <p>
                        We appreciate your business! A confirmation email will be sent to{' '}
                        {customerEmail}. If you have any questions, please email{' '}
                    </p>
                    <a href="mailto:orders@example.com">orders@example.com</a>.
                </div>


                <div
                    className="mt-10"
                >
                    <Link
                        href={"/"}
                        className="focus:outline-none text-white bg-purple-700 hover:bg-purple-800 focus:ring-4 focus:ring-purple-300 font-medium rounded-lg text-sm px-5 py-2.5 mb-2 dark:bg-purple-600 dark:hover:bg-purple-700 dark:focus:ring-purple-900"
                    >
                        Go to Home!
                    </Link>
                </div>
            </section>
        )
    }

    return <div>
        <h1>Success</h1>
    </div>
}