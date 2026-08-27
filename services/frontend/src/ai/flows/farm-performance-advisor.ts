'use server';
/**
 * @fileOverview This file implements a Genkit flow that analyzes farm production
 * and financial data to provide performance insights and actionable recommendations.
 *
 * - farmPerformanceAdvisor - A function that handles the farm performance analysis process.
 * - FarmPerformanceAdvisorInput - The input type for the farmPerformanceAdvisor function.
 * - FarmPerformanceAdvisorOutput - The return type for the farmPerformanceAdvisor function.
 */

import { ai } from '@/ai/genkit';
import { z } from 'genkit';

const FarmPerformanceAdvisorInputSchema = z.object({
  startDate: z
    .string()
    .datetime()
    .describe('The start date of the analysis period.'),
  endDate: z
    .string()
    .datetime()
    .describe('The end date of the analysis period.'),
  eggProductionData: z
    .array(
      z.object({
        date: z.string().datetime().describe('The date of the egg production record.'),
        eggsProduced: z.number().describe('The number of eggs produced on this date.'),
      })
    )
    .describe('Daily egg production data.'),
  mortalityData: z
    .array(
      z.object({
        date: z.string().datetime().describe('The date of the mortality record.'),
        mortalityCount: z.number().describe('The number of mortalities on this date.'),
      })
    )
    .describe('Daily chicken mortality data.'),
  expenseData: z
    .array(
      z.object({
        date: z.string().datetime().describe('The date of the expense.'),
        category: z.string().describe('The category of the expense (e.g., feed, medication).'),
        amount: z.number().describe('The amount of the expense.'),
      })
    )
    .describe('Financial expense transactions.'),
  revenueData: z
    .array(
      z.object({
        date: z.string().datetime().describe('The date of the revenue.'),
        category: z.string().describe('The category of the revenue (e.g., egg sales, chicken sales).'),
        amount: z.number().describe('The amount of the revenue.'),
      })
    )
    .describe('Financial revenue transactions.'),
  currentChickenCount: z
    .number()
    .describe('The current total number of chickens on the farm.'),
});
export type FarmPerformanceAdvisorInput = z.infer<
  typeof FarmPerformanceAdvisorInputSchema
>;

const FarmPerformanceAdvisorOutputSchema = z.object({
  summary: z
    .string()
    .describe("A concise summary of the farm's performance during the analyzed period."),
  insights: z
    .array(z.string())
    .describe(
      "Key observations and trends identified from the data, e.g., 'Egg production shows a 5% decline compared to the previous week.'"
    ),
  recommendations: z
    .array(z.string())
    .describe(
      "Actionable advice to optimize farm performance, e.g., 'Consider checking feed quality or reviewing the vaccination schedule for potential issues.'"
    ),
});
export type FarmPerformanceAdvisorOutput = z.infer<
  typeof FarmPerformanceAdvisorOutputSchema
>;

export async function farmPerformanceAdvisor(
  input: FarmPerformanceAdvisorInput
): Promise<FarmPerformanceAdvisorOutput> {
  return farmPerformanceAdvisorFlow(input);
}

const prompt = ai.definePrompt({
  name: 'farmPerformanceAdvisorPrompt',
  input: { schema: FarmPerformanceAdvisorInputSchema },
  output: { schema: FarmPerformanceAdvisorOutputSchema },
  prompt: `You are an expert farm performance advisor for a layer hen farm in Cameroon. Your task is to analyze the provided production and financial data for the period from {{{startDate}}} to {{{endDate}}} and provide intelligent insights and actionable recommendations to optimize farm performance and profitability.

Analyze the following data:

Current number of chickens: {{{currentChickenCount}}}

Egg Production Data (date, eggsProduced):
{{#each eggProductionData}}
- Date: {{{date}}}, Eggs Produced: {{{eggsProduced}}}
{{/each}}

Mortality Data (date, mortalityCount):
{{#each mortalityData}}
- Date: {{{date}}}, Mortality Count: {{{mortalityCount}}}
{{/each}}

Expense Data (date, category, amount):
{{#each expenseData}}
- Date: {{{date}}}, Category: {{{category}}}, Amount: {{{amount}}}
{{/each}}

Revenue Data (date, category, amount):
{{#each revenueData}}
- Date: {{{date}}}, Category: {{{category}}}, Amount: {{{amount}}}
{{/each}}

Based on this data, identify key trends, potential issues (e.g., significant drops in production, rises in mortality, unusual expenses, or revenue changes), and provide concrete, actionable recommendations for improvement or cost savings.

Ensure your response is structured according to the output schema. Focus on providing clear, concise, and helpful advice that a farm manager can act upon.`,
});

const farmPerformanceAdvisorFlow = ai.defineFlow(
  {
    name: 'farmPerformanceAdvisorFlow',
    inputSchema: FarmPerformanceAdvisorInputSchema,
    outputSchema: FarmPerformanceAdvisorOutputSchema,
  },
  async (input) => {
    const { output } = await prompt(input);
    return output!;
  }
);
