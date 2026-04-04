
'use server';
/**
 * @fileOverview A project simulation AI agent for AgriShare.
 * Analyzes resources and domain type to estimate feasibility and profitability.
 */

import { ai } from '@/ai/genkit';
import { z } from 'genkit';

const ProjectSimulationInputSchema = z.object({
  domain: z.enum(['LIVESTOCK', 'AGRICULTURE']).describe('The domain of the project.'),
  type: z.string().describe('The specific type (e.g., Poultry, Pigs, Corn).'),
  budget: z.number().describe('Initial investment budget in FCFA.'),
  landSize: z.number().describe('Available land size in square meters.'),
  hasStructures: z.boolean().describe('Whether farm buildings/structures already exist.'),
  description: z.string().optional().describe('Additional context about available resources.'),
});
export type ProjectSimulationInput = z.infer<typeof ProjectSimulationInputSchema>;

const ProjectSimulationOutputSchema = z.object({
  isFeasible: z.boolean().describe('Whether the project is feasible with the given resources.'),
  estimatedStartupCost: z.number().describe('Estimated cost to launch.'),
  estimatedMonthlyRevenue: z.number().describe('Estimated monthly income after stabilization.'),
  roiPeriodMonths: z.number().describe('Estimated months to break even.'),
  feasibilityReport: z.string().describe('Detailed explanation of the analysis.'),
  recommendations: z.array(z.string()).describe('List of actionable advice.'),
  alternativeProposal: z.object({
    suggestedType: z.string().optional(),
    reason: z.string().optional(),
  }).optional().describe('If not feasible, a better alternative project.'),
});
export type ProjectSimulationOutput = z.infer<typeof ProjectSimulationOutputSchema>;

export async function simulateProject(input: ProjectSimulationInput): Promise<ProjectSimulationOutput> {
  return projectSimulationFlow(input);
}

const prompt = ai.definePrompt({
  name: 'projectSimulationPrompt',
  input: { schema: ProjectSimulationInputSchema },
  output: { schema: ProjectSimulationOutputSchema },
  prompt: `You are an expert agricultural consultant in Cameroon. 
Evaluate a potential {{{domain}}} project focusing on {{{type}}}.

User Resources:
- Budget: {{{budget}}} FCFA
- Land Size: {{{landSize}}} m²
- Existing Structures: {{#if hasStructures}}Yes{{else}}No{{/if}}
- Extra Info: {{{description}}}

Task:
1. Calculate estimated startup costs for this type of project in the Cameroonian market.
2. Estimate monthly revenue based on standard yields (e.g., for poultry, consider egg laying rates; for corn, consider seasonal harvest).
3. Determine feasibility. If the budget or land is too small, mark as not feasible and propose a better alternative (e.g., if Poultry is too expensive, suggest Rabbits).
4. Provide clear recommendations for success.

Focus on transparency and accuracy for investors. Use FCFA for all financial calculations.`,
});

const projectSimulationFlow = ai.defineFlow(
  {
    name: 'projectSimulationFlow',
    inputSchema: ProjectSimulationInputSchema,
    outputSchema: ProjectSimulationOutputSchema,
  },
  async (input) => {
    const { output } = await prompt(input);
    return output!;
  }
);
