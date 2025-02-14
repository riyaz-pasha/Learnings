declare namespace NodeJS {
    export interface ProcessEnv extends Partial<Record<string, string>> {
      STRIPE_SECRET_KEY: string;
      NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY: string;
      STRIPE_WEBHOOK_SECRET?: string;
    }
  }
  