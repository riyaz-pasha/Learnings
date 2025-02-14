import Link from "next/link";

type HomeProps = {
  searchParams: Promise<{
    canceled: boolean;
  }>;
};

export default async function Home({ searchParams }: HomeProps) {
  const { canceled } = await searchParams;

  return (
    <div className="grid grid-cols-1 gap-20">
      <Link href={"/stripe-hosted"}>Stripe Hosted</Link>
      {canceled && <div className="text-red-700"> Order Cancelled </div>}
    </div>
  );
}
