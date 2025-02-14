import 'server-only';

import Stripe from 'stripe';

if (!process?.env?.STRIPE_SECRET_KEY) throw new Error("Missing STRIPE_SECRET_KEY!");
const secretKey = process.env.STRIPE_SECRET_KEY;
export const stripe = new Stripe(secretKey);
