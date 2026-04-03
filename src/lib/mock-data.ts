export type Role = 'ADMIN' | 'INVESTOR' | 'MANAGER' | 'EMPLOYEE';

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
}

export interface Investment {
  id: string;
  userId: string;
  userName: string;
  amount: number;
  date: string;
}

export interface Transaction {
  id: string;
  type: 'INCOME' | 'EXPENSE';
  category: string;
  amount: number;
  date: string;
  description: string;
}

export interface ProductionRecord {
  id: string;
  date: string;
  eggsProduced: number;
  mortality: number;
  chickenCount: number;
}

export const MOCK_USERS: User[] = [
  { id: '1', name: 'Jean-Pierre Ndongo', email: 'jean@agrishare.cm', role: 'ADMIN' },
  { id: '2', name: 'Marie Tchakoute', email: 'marie@invest.cm', role: 'INVESTOR' },
  { id: '3', name: 'Samuel Eto', email: 'sam@manager.cm', role: 'MANAGER' },
  { id: '4', name: 'Alioum Boukar', email: 'ali@farm.cm', role: 'EMPLOYEE' },
];

export const MOCK_INVESTMENTS: Investment[] = [
  { id: 'i1', userId: '1', userName: 'Jean-Pierre Ndongo', amount: 5000000, date: '2023-01-15' },
  { id: 'i2', userId: '2', userName: 'Marie Tchakoute', amount: 3000000, date: '2023-03-20' },
  { id: 'i3', userId: '1', userName: 'Jean-Pierre Ndongo', amount: 2000000, date: '2023-06-10' },
];

export const MOCK_TRANSACTIONS: Transaction[] = [
  { id: 't1', type: 'INCOME', category: 'Vente d\'œufs', amount: 150000, date: '2024-05-15', description: 'Vente de 50 plateaux' },
  { id: 't2', type: 'EXPENSE', category: 'Alimentation', amount: 45000, date: '2024-05-14', description: 'Sac de démarrage 50kg' },
  { id: 't3', type: 'EXPENSE', category: 'Salaires', amount: 80000, date: '2024-05-01', description: 'Salaire mensuel employé' },
  { id: 't4', type: 'INCOME', category: 'Vente de poules', amount: 1200000, date: '2024-04-20', description: 'Vente poules réformées' },
  { id: 't5', type: 'EXPENSE', category: 'Vaccins', amount: 25000, date: '2024-05-10', description: 'Campagne Gumboro' },
];

export const MOCK_PRODUCTION: ProductionRecord[] = [
  { id: 'p1', date: '2024-05-15', eggsProduced: 450, mortality: 2, chickenCount: 500 },
  { id: 'p2', date: '2024-05-16', eggsProduced: 442, mortality: 0, chickenCount: 498 },
  { id: 'p3', date: '2024-05-17', eggsProduced: 435, mortality: 1, chickenCount: 498 },
  { id: 'p4', date: '2024-05-18', eggsProduced: 460, mortality: 0, chickenCount: 497 },
  { id: 'p5', date: '2024-05-19', eggsProduced: 455, mortality: 0, chickenCount: 497 },
  { id: 'p6', date: '2024-05-20', eggsProduced: 440, mortality: 3, chickenCount: 497 },
  { id: 'p7', date: '2024-05-21', eggsProduced: 420, mortality: 1, chickenCount: 494 },
];