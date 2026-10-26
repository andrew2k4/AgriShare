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
type CreateContributionRequest = components['schemas']['CreateContributionRequest'];
type ProposeDistributionRequest = components['schemas']['ProposeDistributionRequest'];
type SetFinancialParameterRequest = components['schemas']['SetFinancialParameterRequest'];
type RequestWithdrawalRequest = components['schemas']['RequestWithdrawalRequest'];
type CreateAssetRequest = components['schemas']['CreateAssetRequest'];
type CreateLeaseRequest = components['schemas']['CreateLeaseRequest'];
type ScheduleRepaymentRequest = components['schemas']['ScheduleRepaymentRequest'];
type DisposeAssetRequest = components['schemas']['DisposeAssetRequest'];

export const api = {
  projects: {
    getAll:   ()                               => apiClient.GET('/api/projects'),
    getById:  (id: number)                     => apiClient.GET('/api/projects/{id}',    { params: { path: { id } } }),
    create:   (body: CreateProjectRequest)     => apiClient.POST('/api/projects',         { body }),
    update:   (id: number, body: ProjectDto)   => apiClient.PUT('/api/projects/{id}',    { params: { path: { id } }, body }),
    delete:   (id: number)                     => apiClient.DELETE('/api/projects/{id}', { params: { path: { id } } }),
    close:    (id: number)                     => apiClient.POST('/api/projects/{id}/close', { params: { path: { id } } }),
    simulate: (projectId: number, monthlyRatePercent?: number) =>
      apiClient.GET('/api/projects/{projectId}/simulate', { params: { path: { projectId }, query: { monthlyRatePercent } } }),
    financialSummary: (projectId: number) =>
      apiClient.GET('/api/projects/{projectId}/financial-summary', { params: { path: { projectId } } }),
    roi: (projectId: number, investorId: number) =>
      apiClient.GET('/api/projects/{projectId}/investors/{investorId}/roi', { params: { path: { projectId, investorId } } }),
  },

  investors: {
    getAll:   ()                               => apiClient.GET('/api/investors'),
    getById:  (id: number)                     => apiClient.GET('/api/investors/{id}',    { params: { path: { id } } }),
    create:   (body: InvestorDto)              => apiClient.POST('/api/investors',         { body }),
    update:   (id: number, body: InvestorDto)  => apiClient.PUT('/api/investors/{id}',    { params: { path: { id } }, body }),
    delete:   (id: number)                     => apiClient.DELETE('/api/investors/{id}', { params: { path: { id } } }),
  },

  operations: {
    getAll:       ()                                 => apiClient.GET('/api/operations'),
    getByProject: (projectId: number)                => apiClient.GET('/api/operations/by-project/{projectId}', { params: { path: { projectId } } }),
    getById:      (id: number)                       => apiClient.GET('/api/operations/{id}',    { params: { path: { id } } }),
    create:       (body: OperationDto)               => apiClient.POST('/api/operations',         { body }),
    update:       (id: number, body: OperationDto)   => apiClient.PUT('/api/operations/{id}',    { params: { path: { id } }, body }),
    delete:       (id: number)                       => apiClient.DELETE('/api/operations/{id}', { params: { path: { id } } }),
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

  contributions: {
    getByProject:  (projectId: number)                          => apiClient.GET('/api/projects/{projectId}/contributions', { params: { path: { projectId } } }),
    getByInvestor: (investorId: number)                         => apiClient.GET('/api/investors/{investorId}/contributions', { params: { path: { investorId } } }),
    create:        (projectId: number, body: CreateContributionRequest) => apiClient.POST('/api/projects/{projectId}/contributions', { params: { path: { projectId } }, body }),
  },

  distributions: {
    getByProject: (projectId: number)                => apiClient.GET('/api/projects/{projectId}/distributions', { params: { path: { projectId } } }),
    getById:      (id: number)                        => apiClient.GET('/api/distributions/{id}', { params: { path: { id } } }),
    propose:      (projectId: number, body: ProposeDistributionRequest) => apiClient.POST('/api/projects/{projectId}/distributions', { params: { path: { projectId } }, body }),
    validate:     (id: number)                        => apiClient.POST('/api/distributions/{id}/validate', { params: { path: { id } } }),
    reject:       (id: number)                        => apiClient.POST('/api/distributions/{id}/reject', { params: { path: { id } } }),
  },

  withdrawals: {
    getByProject: (projectId: number)                => apiClient.GET('/api/projects/{projectId}/withdrawals', { params: { path: { projectId } } }),
    request:      (contributionId: number, body: RequestWithdrawalRequest) => apiClient.POST('/api/contributions/{contributionId}/withdrawals', { params: { path: { contributionId } }, body }),
    validate:     (id: number)                        => apiClient.POST('/api/withdrawals/{id}/validate', { params: { path: { id } } }),
    reject:       (id: number)                        => apiClient.POST('/api/withdrawals/{id}/reject', { params: { path: { id } } }),
  },

  assets: {
    getByProject: (projectId: number)                  => apiClient.GET('/api/projects/{projectId}/assets', { params: { path: { projectId } } }),
    getById:      (id: number)                          => apiClient.GET('/api/assets/{id}', { params: { path: { id } } }),
    create:       (projectId: number, body: CreateAssetRequest) => apiClient.POST('/api/projects/{projectId}/assets', { params: { path: { projectId } }, body }),
    dispose:      (id: number, body: DisposeAssetRequest) => apiClient.POST('/api/assets/{id}/dispose', { params: { path: { id } }, body }),
  },

  leases: {
    getByAsset: (assetId: number)                      => apiClient.GET('/api/assets/{assetId}/leases', { params: { path: { assetId } } }),
    create:     (assetId: number, body: CreateLeaseRequest) => apiClient.POST('/api/assets/{assetId}/leases', { params: { path: { assetId } }, body }),
    postRent:   (id: number)                            => apiClient.POST('/api/leases/{id}/post-rent', { params: { path: { id } } }),
    end:        (id: number)                            => apiClient.POST('/api/leases/{id}/end', { params: { path: { id } } }),
  },

  repayments: {
    getByContribution: (contributionId: number)        => apiClient.GET('/api/contributions/{contributionId}/repayments', { params: { path: { contributionId } } }),
    schedule:          (contributionId: number, body: ScheduleRepaymentRequest) => apiClient.POST('/api/contributions/{contributionId}/repayments', { params: { path: { contributionId } }, body }),
    pay:               (id: number)                     => apiClient.POST('/api/repayments/{id}/pay', { params: { path: { id } } }),
  },

  financialParameters: {
    listGlobalDefaults: () => apiClient.GET('/api/admin/financial-parameters'),
    set: (body: SetFinancialParameterRequest) => apiClient.PUT('/api/admin/financial-parameters', { body }),
  },

  auth: {
    me: () => apiClient.GET('/api/auth/me'),
  },
};
