import express, { Application } from "express";

export const app: Application = express();

app.use(express.json());

