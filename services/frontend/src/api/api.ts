'use client';

import { apiClient } from './client';
import type { components } from './schema';

type CreateProjectRequest    = components['schemas']['CreateProjectRequest'];
type ProjectDto              = components['schemas']['ProjectDto'];
type InvestorDto             = components['schemas']['InvestorDto'];
type OperationDto            = components['schemas']['OperationDto'];
type ProductionDayDto        = components['schemas']['ProductionDayDto'];
type StockInventoryDto       = components['schemas']['StockInventoryDto'];
type CapitalDistributionDto  = components['schemas']['CapitalDistributionDto'];

export const api = {
  projects: {
    getAll:   ()                               => apiClient.GET('/api/projects'),
    getById:  (id: number)                     => apiClient.GET('/api/projects/{id}',    { params: { path: { id } } }),
    create:   (body: CreateProjectRequest)     => apiClient.POST('/api/projects',         { body }),
    update:   (id: number, body: ProjectDto)   => apiClient.PUT('/api/projects/{id}',    { params: { path: { id } }, body }),
    delete:   (id: number)                     => apiClient.DELETE('/api/projects/{id}', { params: { path: { id } } }),
  },

  investors: {
    getAll:   ()                               => apiClient.GET('/api/investors'),
    getById:  (id: number)                     => apiClient.GET('/api/investors/{id}',    { params: { path: { id } } }),
    create:   (body: InvestorDto)              => apiClient.POST('/api/investors',         { body }),
    update:   (id: number, body: InvestorDto)  => apiClient.PUT('/api/investors/{id}',    { params: { path: { id } }, body }),
    delete:   (id: number)                     => apiClient.DELETE('/api/investors/{id}', { params: { path: { id } } }),
  },

  operations: {
    getAll:   ()                                 => apiClient.GET('/api/operations'),
    getById:  (id: number)                       => apiClient.GET('/api/operations/{id}',    { params: { path: { id } } }),
    create:   (body: OperationDto)               => apiClient.POST('/api/operations',         { body }),
    update:   (id: number, body: OperationDto)   => apiClient.PUT('/api/operations/{id}',    { params: { path: { id } }, body }),
    delete:   (id: number)                       => apiClient.DELETE('/api/operations/{id}', { params: { path: { id } } }),
  },

  productionDays: {
    getAll:   ()                                    => apiClient.GET('/api/production-days'),
    getById:  (id: number)                          => apiClient.GET('/api/production-days/{id}',    { params: { path: { id } } }),
    create:   (body: ProductionDayDto)              => apiClient.POST('/api/production-days',         { body }),
    update:   (id: number, body: ProductionDayDto)  => apiClient.PUT('/api/production-days/{id}',    { params: { path: { id } }, body }),
    delete:   (id: number)                          => apiClient.DELETE('/api/production-days/{id}', { params: { path: { id } } }),
  },

  stockInventories: {
    getAll:   ()                                       => apiClient.GET('/api/stock-inventories'),
    getById:  (id: number)                             => apiClient.GET('/api/stock-inventories/{id}',    { params: { path: { id } } }),
    create:   (body: StockInventoryDto)                => apiClient.POST('/api/stock-inventories',         { body }),
    update:   (id: number, body: StockInventoryDto)    => apiClient.PUT('/api/stock-inventories/{id}',    { params: { path: { id } }, body }),
    delete:   (id: number)                             => apiClient.DELETE('/api/stock-inventories/{id}', { params: { path: { id } } }),
  },

  capitalDistributions: {
    getAll:   ()                                            => apiClient.GET('/api/capital-distributions'),
    getById:  (id: number)                                  => apiClient.GET('/api/capital-distributions/{id}',    { params: { path: { id } } }),
    create:   (body: CapitalDistributionDto)                => apiClient.POST('/api/capital-distributions',         { body }),
    update:   (id: number, body: CapitalDistributionDto)    => apiClient.PUT('/api/capital-distributions/{id}',    { params: { path: { id } }, body }),
    delete:   (id: number)                                  => apiClient.DELETE('/api/capital-distributions/{id}', { params: { path: { id } } }),
  },

  auth: {
    me: () => apiClient.GET('/api/auth/me'),
  },
};
