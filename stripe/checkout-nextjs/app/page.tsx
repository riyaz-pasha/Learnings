import Link from "next/link";

export default function Home() {
  return (
    <div className="grid grid-cols-1 gap-20">
      <Link href={"/stripe-hosted"}>Stripe Hosted</Link>
    </div>
  );
}
