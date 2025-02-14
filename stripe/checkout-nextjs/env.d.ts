declare namespace NodeJS {
    export interface ProcessEnv extends Partial<Record<string, string>> {
      STRIPE_SECRET_KEY: string;
    }
  }
  