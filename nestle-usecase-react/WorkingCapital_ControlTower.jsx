import React, { useState, useMemo, useCallback } from 'react';

// ============================================================================
// WORKING CAPITAL DECISION CONTROL TOWER
// Fact-Based / Datomic Architecture with Temporal Immutability
// ============================================================================

// ----------------------------------------------------------------------------
// TEMPORAL DATA MODEL - Historical versions of each observation
// Every fact is an immutable datom with transaction time and valid time
// ----------------------------------------------------------------------------

const createHistoricalObservations = () => ({
  // SUPPLIER OTIF - degraded over Q4
  supplier_otif_dist: [
    { value: 0.94, tx_time: '2025-09-01T08:00:00Z', valid_time: '2025-08-31T00:00:00Z', source: 'Vendor Mgmt', note: 'Q3 close - strong performance' },
    { value: 0.91, tx_time: '2025-09-15T09:30:00Z', valid_time: '2025-09-14T00:00:00Z', source: 'Vendor Mgmt', note: 'Early Q4 - slight decline' },
    { value: 0.87, tx_time: '2025-10-01T10:00:00Z', valid_time: '2025-09-30T00:00:00Z', source: 'Vendor Mgmt', note: 'ACME capacity issues reported' },
    { value: 0.82, tx_time: '2025-10-15T08:45:00Z', valid_time: '2025-10-14T00:00:00Z', source: 'Vendor Mgmt', note: 'Continued degradation' },
    { value: 0.78, tx_time: '2025-11-01T09:00:00Z', valid_time: '2025-10-31T00:00:00Z', source: 'Vendor Mgmt', note: 'Critical level reached' },
    { value: 0.78, tx_time: '2025-11-23T09:00:00Z', valid_time: '2025-11-22T00:00:00Z', source: 'Vendor Mgmt', note: 'Current state - action required' },
  ],
  forecast_error_dist: [
    { value: 0.03, tx_time: '2025-09-01T08:00:00Z', valid_time: '2025-08-31T00:00:00Z', source: 'Analytics', note: 'Q3 close - within tolerance' },
    { value: 0.05, tx_time: '2025-09-15T14:00:00Z', valid_time: '2025-09-14T00:00:00Z', source: 'Analytics', note: 'Slight increase' },
    { value: 0.08, tx_time: '2025-10-01T11:00:00Z', valid_time: '2025-09-30T00:00:00Z', source: 'Analytics', note: 'Promo uplift underestimated' },
    { value: 0.10, tx_time: '2025-10-15T13:30:00Z', valid_time: '2025-10-14T00:00:00Z', source: 'Analytics', note: 'Pattern continuing' },
    { value: 0.12, tx_time: '2025-11-01T10:00:00Z', valid_time: '2025-10-31T00:00:00Z', source: 'Analytics', note: 'Systematic bias identified' },
    { value: 0.12, tx_time: '2025-11-18T14:30:00Z', valid_time: '2025-11-17T00:00:00Z', source: 'Analytics', note: 'Current state' },
  ],
  consensus_forecast_qty: [
    { value: 48000, tx_time: '2025-09-01T08:00:00Z', valid_time: '2025-09-01T00:00:00Z', source: 'S&OP/IBP', note: 'Q4 initial forecast' },
    { value: 50000, tx_time: '2025-09-15T16:00:00Z', valid_time: '2025-09-15T00:00:00Z', source: 'S&OP/IBP', note: 'S&OP cycle revision' },
    { value: 52000, tx_time: '2025-10-01T09:00:00Z', valid_time: '2025-10-01T00:00:00Z', source: 'S&OP/IBP', note: 'October S&OP' },
    { value: 55000, tx_time: '2025-10-15T15:00:00Z', valid_time: '2025-10-15T00:00:00Z', source: 'S&OP/IBP', note: 'Holiday demand signal' },
    { value: 55000, tx_time: '2025-11-20T09:00:00Z', valid_time: '2025-11-20T00:00:00Z', source: 'S&OP/IBP', note: 'Current consensus' },
  ],
  planned_production_qty: [
    { value: 48000, tx_time: '2025-09-01T08:00:00Z', valid_time: '2025-09-01T00:00:00Z', source: 'SAP/APO', note: 'Initial Q4 plan' },
    { value: 50000, tx_time: '2025-09-20T10:00:00Z', valid_time: '2025-09-20T00:00:00Z', source: 'SAP/APO', note: 'Aligned with forecast' },
    { value: 52000, tx_time: '2025-10-05T11:00:00Z', valid_time: '2025-10-05T00:00:00Z', source: 'SAP/APO', note: 'October revision' },
    { value: 55000, tx_time: '2025-10-20T14:00:00Z', valid_time: '2025-10-20T00:00:00Z', source: 'SAP/APO', note: 'Current plan' },
    { value: 55000, tx_time: '2025-11-15T08:00:00Z', valid_time: '2025-11-15T00:00:00Z', source: 'SAP/APO', note: 'Confirmed' },
  ],
  open_po_qty_rpm: [
    { value: 75000, tx_time: '2025-09-01T08:00:00Z', valid_time: '2025-09-01T00:00:00Z', source: 'ERP MM', note: 'Q4 opening POs' },
    { value: 82000, tx_time: '2025-09-15T09:00:00Z', valid_time: '2025-09-15T00:00:00Z', source: 'ERP MM', note: 'Additional POs placed' },
    { value: 88000, tx_time: '2025-10-01T08:00:00Z', valid_time: '2025-10-01T00:00:00Z', source: 'ERP MM', note: 'October orders' },
    { value: 95000, tx_time: '2025-10-15T10:00:00Z', valid_time: '2025-10-15T00:00:00Z', source: 'ERP MM', note: 'Buffer stock orders' },
    { value: 95000, tx_time: '2025-11-20T12:00:00Z', valid_time: '2025-11-20T00:00:00Z', source: 'ERP MM', note: 'Current state' },
  ],
  mfg_adherence_pct: [
    { value: 0.94, tx_time: '2025-09-01T08:00:00Z', valid_time: '2025-08-31T00:00:00Z', source: 'MES', note: 'Q3 average' },
    { value: 0.93, tx_time: '2025-10-01T08:00:00Z', valid_time: '2025-09-30T00:00:00Z', source: 'MES', note: 'September actual' },
    { value: 0.92, tx_time: '2025-11-01T08:00:00Z', valid_time: '2025-10-31T00:00:00Z', source: 'MES', note: 'October actual' },
    { value: 0.92, tx_time: '2025-11-22T16:00:00Z', valid_time: '2025-11-21T00:00:00Z', source: 'MES', note: 'Current' },
  ],
  order_variability_cv: [
    { value: 0.30, tx_time: '2025-09-01T08:00:00Z', valid_time: '2025-09-01T00:00:00Z', source: 'ERP Sales', note: 'Q3 baseline' },
    { value: 0.35, tx_time: '2025-11-19T11:00:00Z', valid_time: '2025-11-18T00:00:00Z', source: 'ERP Sales', note: 'Q4 increased variability' },
  ],
  fg_opening_stock: [
    { value: 38000, tx_time: '2025-09-01T00:00:00Z', valid_time: '2025-09-01T00:00:00Z', source: 'WMS', note: 'Q4 opening' },
    { value: 40000, tx_time: '2025-10-01T00:00:00Z', valid_time: '2025-10-01T00:00:00Z', source: 'WMS', note: 'October opening' },
    { value: 42000, tx_time: '2025-11-01T00:00:00Z', valid_time: '2025-11-01T00:00:00Z', source: 'WMS', note: 'November opening' },
    { value: 42000, tx_time: '2025-11-25T00:00:00Z', valid_time: '2025-11-25T00:00:00Z', source: 'WMS', note: 'Current' },
  ],
  fg_blocked_stock_proj: [
    { value: 2500, tx_time: '2025-09-01T08:00:00Z', valid_time: '2025-09-01T00:00:00Z', source: 'Quality/WMS', note: 'Q4 opening' },
    { value: 3000, tx_time: '2025-10-15T10:00:00Z', valid_time: '2025-10-15T00:00:00Z', source: 'Quality/WMS', note: 'Quality hold increase' },
    { value: 3500, tx_time: '2025-11-24T10:00:00Z', valid_time: '2025-11-24T00:00:00Z', source: 'Quality/WMS', note: 'Current' },
  ],
  rpm_opening_stock: [
    { value: 165000, tx_time: '2025-09-01T00:00:00Z', valid_time: '2025-09-01T00:00:00Z', source: 'WMS', note: 'Q4 opening' },
    { value: 172000, tx_time: '2025-10-01T00:00:00Z', valid_time: '2025-10-01T00:00:00Z', source: 'WMS', note: 'October' },
    { value: 180000, tx_time: '2025-11-01T00:00:00Z', valid_time: '2025-11-01T00:00:00Z', source: 'WMS', note: 'November' },
    { value: 180000, tx_time: '2025-11-25T00:00:00Z', valid_time: '2025-11-25T00:00:00Z', source: 'WMS', note: 'Current' },
  ],
  fg_unit_cost: [{ value: 45, tx_time: '2025-01-01T00:00:00Z', valid_time: '2025-01-01T00:00:00Z', source: 'Finance', note: 'Annual rate' }],
  rpm_unit_cost: [{ value: 8.5, tx_time: '2025-01-01T00:00:00Z', valid_time: '2025-01-01T00:00:00Z', source: 'Finance', note: 'Annual rate' }],
  production_lot_size: [{ value: 5000, tx_time: '2025-01-01T00:00:00Z', valid_time: '2025-01-01T00:00:00Z', source: 'Master Data', note: 'Standard' }],
  rpm_consumption_ratio: [{ value: 2.5, tx_time: '2025-01-01T00:00:00Z', valid_time: '2025-01-01T00:00:00Z', source: 'PLM/BOM', note: 'BOM standard' }],
  lead_time_variability: [{ value: 3.5, tx_time: '2025-11-22T14:00:00Z', valid_time: '2025-11-22T00:00:00Z', source: 'Logistics', note: 'Current' }],
  material_shelf_life: [{ value: 180, tx_time: '2025-01-01T00:00:00Z', valid_time: '2025-01-01T00:00:00Z', source: 'Master Data', note: 'Standard' }],
  target_cof_pct: [{ value: 0.95, tx_time: '2025-10-01T00:00:00Z', valid_time: '2025-10-01T00:00:00Z', source: 'Strategy', note: 'Q4 target' }],
  cost_of_failure_unit: [{ value: 125, tx_time: '2025-01-01T00:00:00Z', valid_time: '2025-01-01T00:00:00Z', source: 'Finance', note: 'Annual estimate' }],
  holding_cost_rate: [{ value: 0.18, tx_time: '2025-01-01T00:00:00Z', valid_time: '2025-01-01T00:00:00Z', source: 'Finance', note: 'Annual rate' }],
});

// Historical decisions for time travel
const createHistoricalDecisions = () => [
  {
    id: 'decision-2025-09-15-001',
    timestamp: '2025-09-15T16:30:00Z',
    scenario: 'Q4 Initial Planning',
    participants: ['S&OP', 'Supply Chain', 'Finance'],
    factsKnownAt: { supplier_otif_dist: 0.91, forecast_error_dist: 0.05, consensus_forecast_qty: 50000 },
    outcome: 'Approved Q4 production plan at 50,000 cases',
    status: 'executed',
  },
  {
    id: 'decision-2025-10-15-001',
    timestamp: '2025-10-15T15:45:00Z',
    scenario: 'Mid-Q4 Adjustment',
    participants: ['S&OP', 'Procurement', 'Analytics'],
    factsKnownAt: { supplier_otif_dist: 0.82, forecast_error_dist: 0.10, consensus_forecast_qty: 55000 },
    outcome: 'Increased forecast to 55K, flagged supplier risk',
    status: 'executed',
  },
  {
    id: 'decision-2025-11-01-001',
    timestamp: '2025-11-01T11:00:00Z',
    scenario: 'Supplier Risk Escalation',
    participants: ['Procurement', 'Vendor Mgmt', 'S&OP'],
    factsKnownAt: { supplier_otif_dist: 0.78, forecast_error_dist: 0.12, open_po_qty_rpm: 95000 },
    outcome: 'Initiated alternate supplier qualification',
    status: 'in-progress',
  },
];

// Get observation value as-of a specific transaction time
const getObservationAsOf = (history, txTime) => {
  const asOfDate = new Date(txTime);
  const validFacts = history.filter(h => new Date(h.tx_time) <= asOfDate);
  if (validFacts.length === 0) return null;
  return validFacts[validFacts.length - 1];
};

// Get all observations as-of a specific time
const getStateAsOf = (historicalObs, txTime) => {
  const state = {};
  for (const [key, history] of Object.entries(historicalObs)) {
    const fact = getObservationAsOf(history, txTime);
    if (fact) {
      state[key] = { value: fact.value, unit: '', source: fact.source, label: key, txTime: fact.tx_time, note: fact.note };
    }
  }
  return state;
};

// ----------------------------------------------------------------------------
// CURRENT STATE - Initial observations from latest historical values
// ----------------------------------------------------------------------------

const createInitialObservations = () => ({
  consensus_forecast_qty: { value: 55000, unit: 'cases', source: 'S&OP/IBP', label: 'Consensus Forecast', txTime: '2025-11-20T09:00:00Z' },
  forecast_error_dist: { value: 0.12, unit: 'ratio', source: 'Analytics', label: 'Forecast Error', txTime: '2025-11-18T14:30:00Z' },
  order_variability_cv: { value: 0.35, unit: 'ratio', source: 'ERP Sales', label: 'Order Variability CV', txTime: '2025-11-19T11:00:00Z' },
  planned_production_qty: { value: 55000, unit: 'cases', source: 'SAP/APO', label: 'Planned Production', txTime: '2025-11-15T08:00:00Z' },
  mfg_adherence_pct: { value: 0.92, unit: 'ratio', source: 'MES', label: 'Mfg Adherence', txTime: '2025-11-22T16:00:00Z' },
  production_lot_size: { value: 5000, unit: 'cases', source: 'Master Data', label: 'Production Lot Size', txTime: '2025-01-01T00:00:00Z' },
  rpm_consumption_ratio: { value: 2.5, unit: 'units/case', source: 'PLM/BOM', label: 'RPM Consumption Ratio', txTime: '2025-01-01T00:00:00Z' },
  fg_opening_stock: { value: 42000, unit: 'cases', source: 'WMS', label: 'FG Opening Stock', txTime: '2025-11-25T00:00:00Z' },
  fg_blocked_stock_proj: { value: 3500, unit: 'cases', source: 'Quality/WMS', label: 'FG Blocked Stock', txTime: '2025-11-24T10:00:00Z' },
  rpm_opening_stock: { value: 180000, unit: 'units', source: 'WMS', label: 'RPM Opening Stock', txTime: '2025-11-25T00:00:00Z' },
  fg_unit_cost: { value: 45, unit: '$/case', source: 'Finance', label: 'FG Unit Cost', txTime: '2025-11-01T00:00:00Z' },
  rpm_unit_cost: { value: 8.5, unit: '$/unit', source: 'Finance', label: 'RPM Unit Cost', txTime: '2025-11-01T00:00:00Z' },
  open_po_qty_rpm: { value: 95000, unit: 'units', source: 'ERP MM', label: 'Open PO Qty (RPM)', txTime: '2025-11-20T12:00:00Z' },
  supplier_otif_dist: { value: 0.78, unit: 'ratio', source: 'Vendor Mgmt', label: 'Supplier OTIF', txTime: '2025-11-23T09:00:00Z' },
  lead_time_variability: { value: 3.5, unit: 'days', source: 'Logistics', label: 'Lead Time Variability', txTime: '2025-11-22T14:00:00Z' },
  material_shelf_life: { value: 180, unit: 'days', source: 'Master Data', label: 'Material Shelf Life', txTime: '2025-01-01T00:00:00Z' },
  target_cof_pct: { value: 0.95, unit: 'ratio', source: 'Strategy', label: 'Target COF %', txTime: '2025-10-01T00:00:00Z' },
  cost_of_failure_unit: { value: 125, unit: '$/case', source: 'Finance', label: 'Cost of Failure', txTime: '2025-11-01T00:00:00Z' },
  holding_cost_rate: { value: 0.18, unit: 'ratio/year', source: 'Finance', label: 'Holding Cost Rate', txTime: '2025-11-01T00:00:00Z' },
});

// ----------------------------------------------------------------------------
// DEPENDENCY GRAPH ENGINE
// Pure functions for calculation propagation
// ----------------------------------------------------------------------------

const dependencyGraph = {
  // Level 0 - Outputs
  total_inventory_value: { level: 0, label: 'Total Inventory Value', type: 'output', dependencies: ['fg_inventory_value', 'rpm_inventory_value'], formula: 'fg_inventory_value + rpm_inventory_value' },
  service_risk: { level: 0, label: 'Service Risk', type: 'output', dependencies: ['fg_inventory_position', 'demand_distribution', 'target_cof_pct'], formula: 'P(FG_available < Demand) | target_cof' },
  cash_impact: { level: 0, label: 'Cash Impact', type: 'output', dependencies: ['total_inventory_value', 'holding_cost_rate', 'service_risk', 'cost_of_failure_unit', 'consensus_forecast_qty'], formula: 'holding_cost + stockout_exposure' },
  
  // Level 1 - Inventory Pools
  fg_inventory_value: { level: 1, label: 'FG Inventory Value', type: 'derived', dependencies: ['fg_inventory_position', 'fg_unit_cost'], formula: 'fg_position × fg_unit_cost' },
  rpm_inventory_value: { level: 1, label: 'RPM Inventory Value', type: 'derived', dependencies: ['rpm_inventory_position', 'rpm_unit_cost'], formula: 'rpm_position × rpm_unit_cost' },
  fg_inventory_position: { level: 1, label: 'FG Inventory Position', type: 'derived', dependencies: ['fg_opening_stock', 'fg_inflows', 'fg_outflows', 'fg_blocked_stock_proj'], formula: 'opening + inflows - outflows - blocked' },
  rpm_inventory_position: { level: 1, label: 'RPM Inventory Position', type: 'derived', dependencies: ['rpm_opening_stock', 'rpm_inflows', 'rpm_outflows'], formula: 'opening + inflows - outflows' },
  
  // Level 2 - Flow Components
  fg_inflows: { level: 2, label: 'FG Inflows (Production)', type: 'derived', dependencies: ['planned_production_qty', 'mfg_adherence_pct', 'rpm_availability'], formula: 'min(plan × adherence, rpm_avail ÷ bom)' },
  fg_outflows: { level: 2, label: 'FG Outflows (Demand)', type: 'derived', dependencies: ['demand_distribution'], formula: 'demand_mean' },
  rpm_inflows: { level: 2, label: 'RPM Inflows (Supplier)', type: 'derived', dependencies: ['open_po_qty_rpm', 'supplier_otif_dist'], formula: 'open_po × otif' },
  rpm_outflows: { level: 2, label: 'RPM Outflows (Consumption)', type: 'derived', dependencies: ['fg_inflows', 'rpm_consumption_ratio'], formula: 'fg_inflows × bom_ratio' },
  rpm_availability: { level: 2, label: 'RPM Availability', type: 'derived', dependencies: ['rpm_opening_stock', 'rpm_inflows'], formula: 'rpm_opening + rpm_inflows' },
  demand_distribution: { level: 2, label: 'Demand Distribution', type: 'derived', dependencies: ['consensus_forecast_qty', 'forecast_error_dist', 'order_variability_cv'], formula: 'N(forecast × (1 + error), forecast × cv)' },
  
  // Level 3 - Observations (leaf nodes)
  consensus_forecast_qty: { level: 3, label: 'Consensus Forecast', type: 'observation', dependencies: [] },
  forecast_error_dist: { level: 3, label: 'Forecast Error', type: 'observation', dependencies: [] },
  order_variability_cv: { level: 3, label: 'Order Variability CV', type: 'observation', dependencies: [] },
  planned_production_qty: { level: 3, label: 'Planned Production', type: 'observation', dependencies: [] },
  mfg_adherence_pct: { level: 3, label: 'Mfg Adherence', type: 'observation', dependencies: [] },
  rpm_consumption_ratio: { level: 3, label: 'RPM Consumption Ratio', type: 'observation', dependencies: [] },
  fg_opening_stock: { level: 3, label: 'FG Opening Stock', type: 'observation', dependencies: [] },
  fg_blocked_stock_proj: { level: 3, label: 'FG Blocked Stock', type: 'observation', dependencies: [] },
  rpm_opening_stock: { level: 3, label: 'RPM Opening Stock', type: 'observation', dependencies: [] },
  fg_unit_cost: { level: 3, label: 'FG Unit Cost', type: 'observation', dependencies: [] },
  rpm_unit_cost: { level: 3, label: 'RPM Unit Cost', type: 'observation', dependencies: [] },
  open_po_qty_rpm: { level: 3, label: 'Open PO Qty (RPM)', type: 'observation', dependencies: [] },
  supplier_otif_dist: { level: 3, label: 'Supplier OTIF', type: 'observation', dependencies: [] },
  lead_time_variability: { level: 3, label: 'Lead Time Variability', type: 'observation', dependencies: [] },
  material_shelf_life: { level: 3, label: 'Material Shelf Life', type: 'observation', dependencies: [] },
  target_cof_pct: { level: 3, label: 'Target COF %', type: 'observation', dependencies: [] },
  cost_of_failure_unit: { level: 3, label: 'Cost of Failure', type: 'observation', dependencies: [] },
  holding_cost_rate: { level: 3, label: 'Holding Cost Rate', type: 'observation', dependencies: [] },
};

// Get all ancestors (upstream dependencies) of a node
const getAncestors = (nodeId, visited = new Set()) => {
  if (visited.has(nodeId)) return [];
  visited.add(nodeId);
  const node = dependencyGraph[nodeId];
  if (!node) return [];
  let ancestors = [...node.dependencies];
  for (const dep of node.dependencies) {
    ancestors = [...ancestors, ...getAncestors(dep, visited)];
  }
  return [...new Set(ancestors)];
};

// Get all descendants (downstream dependents) of a node
const getDescendants = (nodeId) => {
  const descendants = [];
  for (const [id, node] of Object.entries(dependencyGraph)) {
    if (node.dependencies.includes(nodeId)) {
      descendants.push(id);
      descendants.push(...getDescendants(id));
    }
  }
  return [...new Set(descendants)];
};

// Calculate derived values through the dependency graph
const calculateDerivedValues = (obs) => {
  const demand_mean = obs.consensus_forecast_qty.value * (1 + obs.forecast_error_dist.value);
  const demand_std = obs.consensus_forecast_qty.value * obs.order_variability_cv.value;
  const rpm_inflows = obs.open_po_qty_rpm.value * obs.supplier_otif_dist.value;
  const rpm_available = obs.rpm_opening_stock.value + rpm_inflows;
  const max_production_from_rpm = rpm_available / obs.rpm_consumption_ratio.value;
  const fg_inflows = Math.min(obs.planned_production_qty.value * obs.mfg_adherence_pct.value, max_production_from_rpm);
  const fg_outflows = demand_mean;
  const rpm_outflows = fg_inflows * obs.rpm_consumption_ratio.value;
  const fg_inventory_position = obs.fg_opening_stock.value + fg_inflows - fg_outflows - obs.fg_blocked_stock_proj.value;
  const rpm_inventory_position = obs.rpm_opening_stock.value + rpm_inflows - rpm_outflows;
  const fg_inventory_value = Math.max(0, fg_inventory_position) * obs.fg_unit_cost.value;
  const rpm_inventory_value = Math.max(0, rpm_inventory_position) * obs.rpm_unit_cost.value;
  const total_inventory_value = fg_inventory_value + rpm_inventory_value;
  const available_fg = Math.max(0, fg_inventory_position);
  const z_score = (demand_mean - available_fg) / (demand_std || 1);
  const service_risk = Math.min(0.99, Math.max(0.01, 0.5 * (1 + erf(z_score / Math.sqrt(2)))));
  const holding_cost = total_inventory_value * (obs.holding_cost_rate.value / 12);
  const stockout_cost = service_risk * obs.cost_of_failure_unit.value * demand_mean * 0.1;
  const cash_impact = holding_cost + stockout_cost;
  
  return {
    demand_distribution: { mean: demand_mean, std: demand_std },
    fg_inflows, fg_outflows, rpm_inflows, rpm_outflows, rpm_available, max_production_from_rpm,
    fg_inventory_position, rpm_inventory_position, fg_inventory_value, rpm_inventory_value,
    total_inventory_value, service_risk, cash_impact, holding_cost, stockout_cost,
  };
};

function erf(x) {
  const a1 = 0.254829592, a2 = -0.284496736, a3 = 1.421413741, a4 = -1.453152027, a5 = 1.061405429, p = 0.3275911;
  const sign = x < 0 ? -1 : 1;
  x = Math.abs(x);
  const t = 1.0 / (1.0 + p * x);
  const y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
  return sign * y;
}

// ----------------------------------------------------------------------------
// RACI STAKEHOLDER DATA
// ----------------------------------------------------------------------------

const stakeholders = {
  sop: { id: 'sop', name: 'S&OP', role: 'Accountable', color: '#1e40af', expertise: ['Forecast', 'Safety Stock', 'Production Plan'] },
  supply_chain: { id: 'supply_chain', name: 'Supply Chain', role: 'Responsible', color: '#047857', expertise: ['Production', 'Inventory'] },
  procurement: { id: 'procurement', name: 'Procurement', role: 'Responsible', color: '#7c3aed', expertise: ['PO Management', 'Supplier Relations'] },
  finance: { id: 'finance', name: 'Finance', role: 'Accountable', color: '#b45309', expertise: ['Cost Parameters', 'Working Capital'] },
  sales: { id: 'sales', name: 'Sales', role: 'Responsible', color: '#be185d', expertise: ['Demand Shaping', 'Allocation'] },
  analytics: { id: 'analytics', name: 'Analytics', role: 'Responsible', color: '#0891b2', expertise: ['Forecast Accuracy', 'Analysis'] },
  quality: { id: 'quality', name: 'Quality', role: 'Consulted', color: '#64748b', expertise: ['Blocked Stock', 'Shelf Life'] },
  vendor_mgmt: { id: 'vendor_mgmt', name: 'Vendor Mgmt', role: 'Consulted', color: '#475569', expertise: ['Supplier Performance'] },
};

// ----------------------------------------------------------------------------
// ACTION IMPLICATIONS
// ----------------------------------------------------------------------------

const actionImplications = {
  supplier_otif_dist: [
    { action: 'Expedite open POs', owner: 'procurement', urgency: 'high' },
    { action: 'Qualify alternate suppliers', owner: 'procurement', urgency: 'medium' },
    { action: 'Renegotiate SLAs', owner: 'vendor_mgmt', urgency: 'medium' },
  ],
  forecast_error_dist: [
    { action: 'Review forecast methodology', owner: 'sop', urgency: 'medium' },
    { action: 'Add demand sensing signals', owner: 'analytics', urgency: 'medium' },
    { action: 'Reduce systematic bias', owner: 'analytics', urgency: 'high' },
  ],
  planned_production_qty: [
    { action: 'Add overtime/shifts', owner: 'supply_chain', urgency: 'high' },
    { action: 'Defer maintenance', owner: 'supply_chain', urgency: 'medium' },
    { action: 'Check RPM availability', owner: 'procurement', urgency: 'high' },
  ],
  open_po_qty_rpm: [
    { action: 'Place new POs', owner: 'procurement', urgency: 'high' },
    { action: 'Expedite existing orders', owner: 'procurement', urgency: 'high' },
    { action: 'Negotiate MOQ reduction', owner: 'procurement', urgency: 'medium' },
  ],
  target_cof_pct: [
    { action: 'Accept higher stockout risk', owner: 'sop', urgency: 'low' },
    { action: 'Requires Strategy approval', owner: 'sop', urgency: 'high' },
  ],
  mfg_adherence_pct: [
    { action: 'Investigate production issues', owner: 'supply_chain', urgency: 'high' },
    { action: 'Review maintenance schedule', owner: 'supply_chain', urgency: 'medium' },
  ],
};

// ----------------------------------------------------------------------------
// MAIN CONTROL TOWER COMPONENT
// ----------------------------------------------------------------------------

const WorkingCapitalControlTower = () => {
  const [historicalObs] = useState(createHistoricalObservations);
  const [historicalDecisions, setHistoricalDecisions] = useState(createHistoricalDecisions);
  const [observations, setObservations] = useState(createInitialObservations);
  const [scenarioObservations, setScenarioObservations] = useState(null);
  const [activeView, setActiveView] = useState('dashboard');
  const [selectedKPI, setSelectedKPI] = useState(null);
  const [selectedLineageNode, setSelectedLineageNode] = useState(null);
  const [decisionRoomParticipants, setDecisionRoomParticipants] = useState(['sop', 'procurement']);
  const [scenarioName, setScenarioName] = useState('');
  const [decisions, setDecisions] = useState([]);
  
  // Time travel state
  const [asOfDate, setAsOfDate] = useState(new Date().toISOString());
  const [selectedDecision, setSelectedDecision] = useState(null);
  
  const currentState = useMemo(() => calculateDerivedValues(observations), [observations]);
  const scenarioState = useMemo(() => scenarioObservations ? calculateDerivedValues(scenarioObservations) : null, [scenarioObservations]);
  
  // As-of state for time travel
  const asOfObservations = useMemo(() => getStateAsOf(historicalObs, asOfDate), [historicalObs, asOfDate]);
  const asOfState = useMemo(() => {
    if (Object.keys(asOfObservations).length === 0) return null;
    return calculateDerivedValues(asOfObservations);
  }, [asOfObservations]);
  
  const targets = {
    total_inventory_value: { value: 4250000, tolerance: 0.10 },
    service_risk: { value: 0.05, tolerance: 0.02 },
    cash_impact: { value: 100000, tolerance: 0.15 },
  };
  
  const startScenario = useCallback(() => {
    setScenarioObservations({ ...observations });
    setActiveView('scenario');
  }, [observations]);
  
  const updateScenarioObservation = useCallback((key, newValue) => {
    setScenarioObservations(prev => ({ ...prev, [key]: { ...prev[key], value: newValue } }));
  }, []);
  
  const resetScenario = useCallback(() => setScenarioObservations({ ...observations }), [observations]);
  
  const applyScenario = useCallback(() => {
    if (scenarioObservations) {
      setObservations(scenarioObservations);
      setScenarioObservations(null);
      setActiveView('dashboard');
    }
  }, [scenarioObservations]);
  
  const recordDecision = useCallback(() => {
    const newDecision = {
      id: `decision-${Date.now()}`,
      timestamp: new Date().toISOString(),
      scenario: scenarioName || 'Unnamed Scenario',
      participants: decisionRoomParticipants.map(p => stakeholders[p].name),
      baselineKPIs: { inventory: currentState.total_inventory_value, serviceRisk: currentState.service_risk, cashImpact: currentState.cash_impact },
      scenarioKPIs: scenarioState ? { inventory: scenarioState.total_inventory_value, serviceRisk: scenarioState.service_risk, cashImpact: scenarioState.cash_impact } : null,
      factsKnownAt: { supplier_otif_dist: observations.supplier_otif_dist.value, forecast_error_dist: observations.forecast_error_dist.value, consensus_forecast_qty: observations.consensus_forecast_qty.value },
      status: 'pending',
    };
    setDecisions(prev => [newDecision, ...prev]);
    setHistoricalDecisions(prev => [...prev, newDecision]);
    setScenarioName('');
  }, [scenarioName, decisionRoomParticipants, currentState, scenarioState, observations]);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Header */}
      <header className="border-b border-slate-800 bg-slate-900">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center">
                <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <div>
                <h1 className="text-lg font-semibold tracking-tight text-white">Working Capital Control Tower</h1>
                <p className="text-xs text-slate-400">Fact-Based Decision Intelligence • Temporal Immutability</p>
              </div>
            </div>
            
            <nav className="flex items-center gap-1 bg-slate-800 rounded-lg p-1">
              {[
                { id: 'dashboard', label: 'Dashboard', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
                { id: 'lineage', label: 'Lineage', icon: 'M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z' },
                { id: 'scenario', label: 'Scenario & Decide', icon: 'M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01' },
                { id: 'timetravel', label: '⏱ Time Travel', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
              ].map(nav => (
                <button
                  key={nav.id}
                  onClick={() => nav.id === 'scenario' ? startScenario() : setActiveView(nav.id)}
                  className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-all ${
                    activeView === nav.id ? 'bg-cyan-600 text-white' : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700'
                  }`}
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d={nav.icon} />
                  </svg>
                  {nav.label}
                </button>
              ))}
            </nav>
            
            <div className="flex items-center gap-3">
              <div className="text-right">
                <div className="text-xs text-slate-500">Last Update</div>
                <div className="text-sm text-slate-300 font-mono">{new Date().toLocaleTimeString()}</div>
              </div>
              <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-6 py-8">
        {activeView === 'dashboard' && (
          <DashboardView currentState={currentState} observations={observations} targets={targets}
            onSelectKPI={setSelectedKPI} selectedKPI={selectedKPI} onStartScenario={startScenario} />
        )}
        {activeView === 'lineage' && (
          <LineageView currentState={currentState} observations={observations}
            selectedNode={selectedLineageNode} onSelectNode={setSelectedLineageNode} />
        )}
        {activeView === 'scenario' && (
          <ScenarioAndDecisionView observations={observations} scenarioObservations={scenarioObservations}
            currentState={currentState} scenarioState={scenarioState} targets={targets}
            onUpdateObservation={updateScenarioObservation} onReset={resetScenario} onApply={applyScenario}
            actionImplications={actionImplications} stakeholders={stakeholders}
            participants={decisionRoomParticipants}
            onToggleParticipant={(id) => setDecisionRoomParticipants(prev => prev.includes(id) ? prev.filter(p => p !== id) : [...prev, id])}
            scenarioName={scenarioName} onScenarioNameChange={setScenarioName}
            onRecordDecision={recordDecision} decisions={decisions} />
        )}
        {activeView === 'timetravel' && (
          <TimeTravelView historicalObs={historicalObs} historicalDecisions={historicalDecisions}
            asOfDate={asOfDate} setAsOfDate={setAsOfDate} asOfObservations={asOfObservations}
            asOfState={asOfState} currentState={currentState} observations={observations}
            selectedDecision={selectedDecision} setSelectedDecision={setSelectedDecision} targets={targets} />
        )}
      </main>
    </div>
  );
};

// ----------------------------------------------------------------------------
// DASHBOARD VIEW
// ----------------------------------------------------------------------------

const DashboardView = ({ currentState, observations, targets, onSelectKPI, selectedKPI, onStartScenario }) => {
  const kpis = [
    { id: 'total_inventory_value', label: 'Total Inventory Value', value: currentState.total_inventory_value,
      format: (v) => `$${(v / 1000000).toFixed(2)}M`, target: targets.total_inventory_value.value,
      targetFormat: (v) => `$${(v / 1000000).toFixed(2)}M`, tolerance: targets.total_inventory_value.tolerance, riskDirection: 'high',
      icon: 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4',
      trend: [3.8, 4.0, 4.2, 4.1, 4.3, 4.2, 4.4, currentState.total_inventory_value / 1000000] },
    { id: 'service_risk', label: 'Service Risk', value: currentState.service_risk,
      format: (v) => `${(v * 100).toFixed(1)}%`, target: targets.service_risk.value,
      targetFormat: (v) => `<${(v * 100).toFixed(0)}%`, tolerance: targets.service_risk.tolerance, riskDirection: 'high',
      icon: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
      trend: [0.03, 0.04, 0.05, 0.08, 0.10, 0.12, 0.14, currentState.service_risk] },
    { id: 'cash_impact', label: 'Cash Impact', value: currentState.cash_impact,
      format: (v) => `$${(v / 1000).toFixed(0)}K`, target: targets.cash_impact.value,
      targetFormat: (v) => `<$${(v / 1000).toFixed(0)}K`, tolerance: targets.cash_impact.tolerance, riskDirection: 'high',
      icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
      trend: [80, 85, 90, 95, 105, 120, 150, currentState.cash_impact / 1000] },
  ];

  return (
    <div className="space-y-8">
      <div className="grid grid-cols-3 gap-6">
        {kpis.map((kpi) => (
          <KPICard key={kpi.id} kpi={kpi} isSelected={selectedKPI === kpi.id}
            onClick={() => onSelectKPI(kpi.id === selectedKPI ? null : kpi.id)} />
        ))}
      </div>
      {selectedKPI && (
        <WhyExplainer kpiId={selectedKPI} currentState={currentState} observations={observations} onSimulateFix={onStartScenario} />
      )}
      <div className="grid grid-cols-2 gap-6">
        <InventoryPoolCard title="Finished Goods" position={currentState.fg_inventory_position}
          value={currentState.fg_inventory_value} inflows={currentState.fg_inflows}
          outflows={currentState.fg_outflows} unit="cases" />
        <InventoryPoolCard title="Raw & Pack Materials" position={currentState.rpm_inventory_position}
          value={currentState.rpm_inventory_value} inflows={currentState.rpm_inflows}
          outflows={currentState.rpm_outflows} unit="units" />
      </div>
      <div className="grid grid-cols-4 gap-4">
        <StatCard label="Demand Mean" value={currentState.demand_distribution.mean.toLocaleString()} unit="cases" />
        <StatCard label="Production Capacity" value={currentState.fg_inflows.toLocaleString()} unit="cases" />
        <StatCard label="Supplier OTIF" value={`${(observations.supplier_otif_dist.value * 100).toFixed(0)}%`} status={observations.supplier_otif_dist.value < 0.85 ? 'warning' : 'good'} />
        <StatCard label="Forecast Error" value={`+${(observations.forecast_error_dist.value * 100).toFixed(0)}%`} status={observations.forecast_error_dist.value > 0.05 ? 'warning' : 'good'} />
      </div>
    </div>
  );
};

// ----------------------------------------------------------------------------
// KPI CARD COMPONENT
// ----------------------------------------------------------------------------

const KPICard = ({ kpi, isSelected, onClick }) => {
  const variance = (kpi.value - kpi.target) / kpi.target;
  const isOffTarget = Math.abs(variance) > kpi.tolerance;
  const isWarning = Math.abs(variance) > kpi.tolerance * 0.5 && !isOffTarget;
  let status = 'good';
  if (isOffTarget) status = 'danger';
  else if (isWarning) status = 'warning';
  
  const statusColors = {
    good: { bg: 'bg-emerald-950', border: 'border-emerald-600', text: 'text-emerald-400', glow: 'shadow-emerald-500/20' },
    warning: { bg: 'bg-amber-950', border: 'border-amber-600', text: 'text-amber-400', glow: 'shadow-amber-500/20' },
    danger: { bg: 'bg-red-950', border: 'border-red-600', text: 'text-red-400', glow: 'shadow-red-500/20' },
  };
  const colors = statusColors[status];

  return (
    <button onClick={onClick}
      className={`relative group text-left p-6 rounded-2xl border-2 transition-all duration-300 ${colors.bg} ${colors.border} ${
        isSelected ? `ring-2 ring-offset-2 ring-offset-slate-950 ring-cyan-500 shadow-lg ${colors.glow}` : 'hover:scale-[1.02]'}`}>
      <div className={`absolute top-4 right-4 w-3 h-3 rounded-full ${status === 'good' ? 'bg-emerald-500' : status === 'warning' ? 'bg-amber-500 animate-pulse' : 'bg-red-500 animate-pulse'}`} />
      <div className={`w-12 h-12 rounded-xl bg-slate-800 flex items-center justify-center mb-4 ${colors.text}`}>
        <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d={kpi.icon} />
        </svg>
      </div>
      <div className="text-sm text-slate-400 mb-1">{kpi.label}</div>
      <div className={`text-3xl font-bold tracking-tight mb-2 ${colors.text}`}>{kpi.format(kpi.value)}</div>
      <div className="flex items-center gap-3 text-sm">
        <span className="text-slate-500">Target: {kpi.targetFormat(kpi.target)}</span>
        <span className={`font-medium ${variance > 0 ? (kpi.riskDirection === 'high' ? 'text-red-400' : 'text-emerald-400') : (kpi.riskDirection === 'high' ? 'text-emerald-400' : 'text-red-400')}`}>
          {variance > 0 ? '▲' : '▼'} {Math.abs(variance * 100).toFixed(1)}%
        </span>
      </div>
      <div className="mt-4 h-12"><Sparkline data={kpi.trend} color={colors.text.replace('text-', '')} /></div>
      <div className="absolute bottom-4 right-4 text-xs text-slate-600 opacity-0 group-hover:opacity-100 transition-opacity">Click to analyze →</div>
    </button>
  );
};

// ----------------------------------------------------------------------------
// SPARKLINE COMPONENT
// ----------------------------------------------------------------------------

const Sparkline = ({ data, color }) => {
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  const points = data.map((v, i) => {
    const x = (i / (data.length - 1)) * 100;
    const y = 100 - ((v - min) / range) * 80 - 10;
    return `${x},${y}`;
  }).join(' ');
  const colorMap = { 'emerald-400': '#34d399', 'amber-400': '#fbbf24', 'red-400': '#f87171' };
  const strokeColor = colorMap[color] || '#94a3b8';
  
  return (
    <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="w-full h-full">
      <defs>
        <linearGradient id={`grad-${color}`} x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor={strokeColor} stopOpacity="0.3" />
          <stop offset="100%" stopColor={strokeColor} stopOpacity="0" />
        </linearGradient>
      </defs>
      <polygon points={`0,100 ${points} 100,100`} fill={`url(#grad-${color})`} />
      <polyline points={points} fill="none" stroke={strokeColor} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx={(data.length - 1) / (data.length - 1) * 100} cy={100 - ((data[data.length - 1] - min) / range) * 80 - 10} r="3" fill={strokeColor} />
    </svg>
  );
};

// ----------------------------------------------------------------------------
// WHY EXPLAINER COMPONENT
// ----------------------------------------------------------------------------

const WhyExplainer = ({ kpiId, currentState, observations, onSimulateFix }) => {
  const explanations = {
    total_inventory_value: {
      title: 'Total Inventory Value Analysis',
      summary: `Inventory value of $${(currentState.total_inventory_value / 1000000).toFixed(2)}M is above target due to combination of supply and demand factors.`,
      rootCauses: [
        { contribution: 55, title: 'FG Inventory Buildup', detail: `FG position at ${currentState.fg_inventory_position.toLocaleString()} cases`, source: observations.fg_opening_stock },
        { contribution: 30, title: 'RPM Excess Stock', detail: `RPM position at ${currentState.rpm_inventory_position.toLocaleString()} units`, source: observations.rpm_opening_stock },
        { contribution: 15, title: 'Unit Cost Pressure', detail: `FG unit cost at $${observations.fg_unit_cost.value}/case`, source: observations.fg_unit_cost },
      ],
      actions: [
        { text: 'Accelerate sales promotions', owner: 'Sales' },
        { text: 'Reduce production plan', owner: 'S&OP' },
        { text: 'Defer incoming POs', owner: 'Procurement' },
      ],
    },
    service_risk: {
      title: 'Service Risk Analysis',
      summary: `Service risk at ${(currentState.service_risk * 100).toFixed(1)}% indicates high probability of missing customer fulfillment targets.`,
      rootCauses: [
        { contribution: 70, title: 'Supply Constraint', detail: `Supplier OTIF at ${(observations.supplier_otif_dist.value * 100).toFixed(0)}% (target: 92%)`, source: observations.supplier_otif_dist, supplier: 'ACME Corp' },
        { contribution: 30, title: 'Demand Spike', detail: `Forecast error running +${(observations.forecast_error_dist.value * 100).toFixed(0)}%`, source: observations.forecast_error_dist },
      ],
      actions: [
        { text: 'Expedite open POs with ACME Corp', owner: 'Procurement' },
        { text: 'Qualify alternate supplier for Component X', owner: 'Procurement' },
        { text: 'Review promo forecast methodology', owner: 'S&OP' },
      ],
    },
    cash_impact: {
      title: 'Cash Impact Analysis',
      summary: `Net cash exposure of $${(currentState.cash_impact / 1000).toFixed(0)}K driven by holding costs and stockout risk.`,
      rootCauses: [
        { contribution: 60, title: 'Holding Costs', detail: `$${(currentState.holding_cost / 1000).toFixed(0)}K monthly holding cost`, source: observations.holding_cost_rate },
        { contribution: 40, title: 'Stockout Exposure', detail: `$${(currentState.stockout_cost / 1000).toFixed(0)}K potential stockout cost`, source: observations.cost_of_failure_unit },
      ],
      actions: [
        { text: 'Reduce safety stock levels', owner: 'S&OP' },
        { text: 'Negotiate consignment terms', owner: 'Procurement' },
        { text: 'Review cost of failure assumptions', owner: 'Finance' },
      ],
    },
  };
  const exp = explanations[kpiId];
  if (!exp) return null;

  return (
    <div className="bg-slate-900 rounded-2xl border border-slate-800 overflow-hidden">
      <div className="px-6 py-4 border-b border-slate-800 bg-red-950">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-red-900 flex items-center justify-center">
            <svg className="w-5 h-5 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <div>
            <h3 className="font-semibold text-white">{exp.title}</h3>
            <p className="text-sm text-slate-400">{exp.summary}</p>
          </div>
        </div>
      </div>
      <div className="p-6 grid grid-cols-2 gap-6">
        <div>
          <h4 className="text-sm font-medium text-slate-300 mb-4 flex items-center gap-2">
            <svg className="w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Root Cause Analysis
          </h4>
          <div className="space-y-4">
            {exp.rootCauses.map((cause, i) => (
              <div key={i} className="relative pl-4">
                <div className="absolute left-0 top-1 w-1 h-full bg-gradient-to-b from-red-500 to-transparent rounded-full" style={{ height: '80%' }} />
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs font-bold text-red-400 bg-red-900 px-2 py-0.5 rounded-full">{cause.contribution}%</span>
                  <span className="text-sm font-medium text-white">{cause.title}</span>
                </div>
                <p className="text-sm text-slate-400 mb-1">{cause.detail}</p>
                <div className="flex items-center gap-2 text-xs text-slate-500">
                  <span>Source: {cause.source.source}</span>
                  <span>•</span>
                  <span>Recorded: {new Date(cause.source.txTime).toLocaleDateString()}</span>
                </div>
                {cause.supplier && (
                  <div className="mt-2 text-xs text-amber-400 bg-amber-900 px-2 py-1 rounded inline-block">
                    Affected Supplier: {cause.supplier}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
        <div>
          <h4 className="text-sm font-medium text-slate-300 mb-4 flex items-center gap-2">
            <svg className="w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
            </svg>
            Recommended Actions
          </h4>
          <div className="space-y-3">
            {exp.actions.map((action, i) => (
              <div key={i} className="flex items-center gap-3 p-3 bg-slate-800 rounded-lg border border-slate-700">
                <div className="w-6 h-6 rounded-full bg-cyan-900 flex items-center justify-center text-cyan-400 text-xs font-bold">{i + 1}</div>
                <div className="flex-1">
                  <p className="text-sm text-white">{action.text}</p>
                  <p className="text-xs text-slate-500">Owner: {action.owner}</p>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-6 flex gap-3">
            <button onClick={onSimulateFix}
              className="flex-1 px-4 py-2 bg-cyan-600 text-white rounded-lg text-sm font-medium hover:bg-cyan-700 transition-colors flex items-center justify-center gap-2">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
              </svg>
              Simulate Fix
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// ----------------------------------------------------------------------------
// INVENTORY POOL CARD
// ----------------------------------------------------------------------------

const InventoryPoolCard = ({ title, position, value, inflows, outflows, unit }) => (
  <div className="bg-slate-900 rounded-xl border border-slate-800 p-5">
    <div className="flex items-center justify-between mb-4">
      <h3 className="font-medium text-white">{title}</h3>
      <span className="text-lg font-bold text-cyan-400">${(value / 1000000).toFixed(2)}M</span>
    </div>
    <div className="flex items-center gap-4 mb-4">
      <div className="flex-1 text-center p-3 bg-emerald-950 rounded-lg border border-emerald-800">
        <div className="text-xs text-emerald-400 mb-1">Inflows</div>
        <div className="text-lg font-semibold text-emerald-300">+{inflows.toLocaleString()}</div>
        <div className="text-xs text-slate-500">{unit}</div>
      </div>
      <div className="text-2xl text-slate-600">→</div>
      <div className="flex-1 text-center p-3 bg-slate-800 rounded-lg border border-slate-700">
        <div className="text-xs text-slate-400 mb-1">Position</div>
        <div className="text-lg font-semibold text-white">{position.toLocaleString()}</div>
        <div className="text-xs text-slate-500">{unit}</div>
      </div>
      <div className="text-2xl text-slate-600">→</div>
      <div className="flex-1 text-center p-3 bg-red-950 rounded-lg border border-red-800">
        <div className="text-xs text-red-400 mb-1">Outflows</div>
        <div className="text-lg font-semibold text-red-300">-{Math.round(outflows).toLocaleString()}</div>
        <div className="text-xs text-slate-500">{unit}</div>
      </div>
    </div>
    <div className="h-2 bg-slate-800 rounded-full overflow-hidden">
      <div className="h-full bg-gradient-to-r from-emerald-500 to-cyan-500 rounded-full transition-all duration-500"
        style={{ width: `${Math.min(100, (position / (inflows + position)) * 100)}%` }} />
    </div>
  </div>
);

// ----------------------------------------------------------------------------
// STAT CARD
// ----------------------------------------------------------------------------

const StatCard = ({ label, value, unit, status = 'neutral' }) => {
  const statusColors = { good: 'text-emerald-400', warning: 'text-amber-400', neutral: 'text-slate-300' };
  return (
    <div className="bg-slate-900 rounded-lg border border-slate-800 p-4">
      <div className="text-xs text-slate-500 mb-1">{label}</div>
      <div className={`text-xl font-semibold ${statusColors[status]}`}>{value}</div>
      {unit && <div className="text-xs text-slate-600">{unit}</div>}
    </div>
  );
};

// ----------------------------------------------------------------------------
// LINEAGE VIEW - Interactive Dependency Graph
// ----------------------------------------------------------------------------

const LineageView = ({ currentState, observations, selectedNode, onSelectNode }) => {
  const getNodeValue = (nodeId) => {
    if (observations[nodeId]) {
      const obs = observations[nodeId];
      if (nodeId.includes('pct') || nodeId.includes('dist') || nodeId.includes('cv') || nodeId.includes('rate')) return `${(obs.value * 100).toFixed(0)}%`;
      if (nodeId.includes('cost') || nodeId.includes('failure')) return `$${obs.value}`;
      return obs.value.toLocaleString();
    }
    switch (nodeId) {
      case 'total_inventory_value': return `$${(currentState.total_inventory_value/1e6).toFixed(2)}M`;
      case 'service_risk': return `${(currentState.service_risk*100).toFixed(1)}%`;
      case 'cash_impact': return `$${(currentState.cash_impact/1e3).toFixed(0)}K`;
      case 'fg_inventory_value': return `$${(currentState.fg_inventory_value/1e6).toFixed(2)}M`;
      case 'rpm_inventory_value': return `$${(currentState.rpm_inventory_value/1e6).toFixed(2)}M`;
      case 'fg_inventory_position': return currentState.fg_inventory_position.toLocaleString();
      case 'rpm_inventory_position': return currentState.rpm_inventory_position.toLocaleString();
      case 'fg_inflows': return currentState.fg_inflows.toLocaleString();
      case 'fg_outflows': return Math.round(currentState.fg_outflows).toLocaleString();
      case 'rpm_inflows': return currentState.rpm_inflows.toLocaleString();
      case 'rpm_outflows': return Math.round(currentState.rpm_outflows).toLocaleString();
      case 'rpm_availability': return currentState.rpm_available.toLocaleString();
      case 'demand_distribution': return `μ=${Math.round(currentState.demand_distribution.mean).toLocaleString()}`;
      default: return '—';
    }
  };

  const ancestors = selectedNode ? getAncestors(selectedNode) : [];
  const descendants = selectedNode ? getDescendants(selectedNode) : [];
  const highlightedNodes = selectedNode ? [selectedNode, ...ancestors, ...descendants] : [];

  const typeStyles = {
    output: { bg: 'bg-cyan-900', border: 'border-cyan-500', text: 'text-cyan-300', dimBg: 'bg-cyan-950', dimBorder: 'border-cyan-900' },
    derived: { bg: 'bg-violet-900', border: 'border-violet-500', text: 'text-violet-300', dimBg: 'bg-violet-950', dimBorder: 'border-violet-900' },
    observation: { bg: 'bg-amber-900', border: 'border-amber-500', text: 'text-amber-300', dimBg: 'bg-amber-950', dimBorder: 'border-amber-900' },
  };

  const levels = [
    { level: 0, label: 'Level 0: Outputs', nodes: ['total_inventory_value', 'service_risk', 'cash_impact'] },
    { level: 1, label: 'Level 1: Inventory Pools', nodes: ['fg_inventory_value', 'rpm_inventory_value', 'fg_inventory_position', 'rpm_inventory_position'] },
    { level: 2, label: 'Level 2: Flow Components', nodes: ['fg_inflows', 'fg_outflows', 'rpm_inflows', 'rpm_outflows', 'rpm_availability', 'demand_distribution'] },
    { level: 3, label: 'Level 3: Observations', nodes: Object.keys(dependencyGraph).filter(k => dependencyGraph[k].level === 3) },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-white">Decision Lineage Explorer</h2>
          <p className="text-sm text-slate-400">Click any node to trace its dependencies and see what it affects</p>
        </div>
        <div className="flex gap-4">
          {Object.entries(typeStyles).map(([type, styles]) => (
            <div key={type} className="flex items-center gap-2">
              <div className={`w-4 h-4 rounded ${styles.bg} border-2 ${styles.border}`} />
              <span className="text-xs text-slate-400 capitalize">{type}</span>
            </div>
          ))}
        </div>
      </div>

      {selectedNode && (
        <div className="bg-slate-900 rounded-xl border border-cyan-700 p-4">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-3">
              <h3 className="font-medium text-white">{dependencyGraph[selectedNode]?.label || selectedNode}</h3>
              <span className={`text-xs px-2 py-0.5 rounded ${typeStyles[dependencyGraph[selectedNode]?.type]?.bg} ${typeStyles[dependencyGraph[selectedNode]?.type]?.text}`}>
                {dependencyGraph[selectedNode]?.type}
              </span>
            </div>
            <button onClick={() => onSelectNode(null)} className="text-slate-400 hover:text-white">
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <div className="text-xs text-slate-500 mb-1">Current Value</div>
              <div className="text-lg font-bold text-white">{getNodeValue(selectedNode)}</div>
            </div>
            {dependencyGraph[selectedNode]?.formula && (
              <div>
                <div className="text-xs text-slate-500 mb-1">Formula</div>
                <div className="text-sm text-slate-300 font-mono">{dependencyGraph[selectedNode].formula}</div>
              </div>
            )}
            {observations[selectedNode] && (
              <div>
                <div className="text-xs text-slate-500 mb-1">Source</div>
                <div className="text-sm text-slate-300">{observations[selectedNode].source}</div>
              </div>
            )}
          </div>
          <div className="mt-4 pt-4 border-t border-slate-800">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <div className="text-xs text-slate-500 mb-2">Depends On ({ancestors.length} nodes)</div>
                <div className="flex flex-wrap gap-1">
                  {dependencyGraph[selectedNode]?.dependencies.map(dep => (
                    <button key={dep} onClick={() => onSelectNode(dep)}
                      className="text-xs px-2 py-1 bg-slate-800 rounded text-cyan-400 hover:bg-slate-700">
                      {dependencyGraph[dep]?.label || dep}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <div className="text-xs text-slate-500 mb-2">Affects ({descendants.length} nodes)</div>
                <div className="flex flex-wrap gap-1">
                  {descendants.slice(0, 5).map(dep => (
                    <button key={dep} onClick={() => onSelectNode(dep)}
                      className="text-xs px-2 py-1 bg-slate-800 rounded text-violet-400 hover:bg-slate-700">
                      {dependencyGraph[dep]?.label || dep}
                    </button>
                  ))}
                  {descendants.length > 5 && <span className="text-xs text-slate-500">+{descendants.length - 5} more</span>}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="bg-slate-900 rounded-2xl border border-slate-800 p-8">
        <div className="space-y-8">
          {levels.map(({ level, label, nodes }) => (
            <div key={level}>
              <div className="text-xs text-slate-500 mb-3 font-medium">{label}</div>
              <div className="flex flex-wrap gap-3">
                {nodes.map(nodeId => {
                  const node = dependencyGraph[nodeId];
                  if (!node) return null;
                  const styles = typeStyles[node.type];
                  const isSelected = selectedNode === nodeId;
                  const isHighlighted = highlightedNodes.includes(nodeId);
                  const isAncestor = ancestors.includes(nodeId);
                  const isDescendant = descendants.includes(nodeId);
                  const isDimmed = selectedNode && !isHighlighted;
                  
                  return (
                    <button key={nodeId} onClick={() => onSelectNode(isSelected ? null : nodeId)}
                      className={`px-4 py-3 rounded-lg border-2 transition-all ${
                        isDimmed ? `${styles.dimBg} ${styles.dimBorder} opacity-40` : `${styles.bg} ${styles.border}`
                      } ${isSelected ? 'ring-2 ring-offset-2 ring-offset-slate-900 ring-white' : ''} ${
                        isAncestor ? 'ring-2 ring-cyan-400' : ''} ${isDescendant ? 'ring-2 ring-violet-400' : ''}`}>
                      <div className={`text-sm font-medium ${isDimmed ? 'text-slate-600' : styles.text}`}>{node.label}</div>
                      <div className={`text-lg font-bold ${isDimmed ? 'text-slate-600' : 'text-white'}`}>{getNodeValue(nodeId)}</div>
                      {observations[nodeId] && (
                        <div className={`text-xs ${isDimmed ? 'text-slate-700' : 'text-slate-500'}`}>{observations[nodeId].source}</div>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>

      {selectedNode && (
        <div className="flex items-center justify-center gap-6 text-xs text-slate-500">
          <div className="flex items-center gap-2"><div className="w-3 h-3 rounded ring-2 ring-cyan-400 bg-slate-800" /><span>Dependencies (upstream)</span></div>
          <div className="flex items-center gap-2"><div className="w-3 h-3 rounded ring-2 ring-violet-400 bg-slate-800" /><span>Affected (downstream)</span></div>
          <div className="flex items-center gap-2"><div className="w-3 h-3 rounded ring-2 ring-white bg-slate-800" /><span>Selected</span></div>
        </div>
      )}
    </div>
  );
};

// ----------------------------------------------------------------------------
// SCENARIO & DECISION VIEW (INTEGRATED)
// ----------------------------------------------------------------------------

const ScenarioAndDecisionView = ({ observations, scenarioObservations, currentState, scenarioState, targets,
  onUpdateObservation, onReset, onApply, actionImplications, stakeholders, participants, onToggleParticipant,
  scenarioName, onScenarioNameChange, onRecordDecision, decisions }) => {
  const [activeCategory, setActiveCategory] = useState('supply');
  
  const categories = {
    demand: { label: 'Demand', observations: ['consensus_forecast_qty', 'forecast_error_dist', 'order_variability_cv'] },
    supply: { label: 'Supply', observations: ['supplier_otif_dist', 'open_po_qty_rpm', 'lead_time_variability'] },
    production: { label: 'Production', observations: ['planned_production_qty', 'mfg_adherence_pct'] },
    policy: { label: 'Policy', observations: ['target_cof_pct', 'holding_cost_rate', 'cost_of_failure_unit'] },
  };

  const sliderConfig = {
    consensus_forecast_qty: { min: 30000, max: 80000, step: 1000 },
    forecast_error_dist: { min: -0.2, max: 0.3, step: 0.01 },
    order_variability_cv: { min: 0.1, max: 0.6, step: 0.01 },
    supplier_otif_dist: { min: 0.5, max: 1.0, step: 0.01 },
    open_po_qty_rpm: { min: 50000, max: 150000, step: 5000 },
    lead_time_variability: { min: 1, max: 7, step: 0.5 },
    planned_production_qty: { min: 30000, max: 70000, step: 1000 },
    mfg_adherence_pct: { min: 0.7, max: 1.0, step: 0.01 },
    target_cof_pct: { min: 0.85, max: 0.99, step: 0.01 },
    holding_cost_rate: { min: 0.1, max: 0.3, step: 0.01 },
    cost_of_failure_unit: { min: 50, max: 200, step: 5 },
  };

  const formatValue = (key, value) => {
    if (key.includes('pct') || key.includes('dist') || key.includes('cv') || key.includes('rate')) return `${(value * 100).toFixed(0)}%`;
    if (key.includes('cost') || key.includes('failure')) return `$${value}`;
    return value.toLocaleString();
  };

  const changedObservations = scenarioObservations ? Object.keys(scenarioObservations).filter(
    key => scenarioObservations[key].value !== observations[key].value
  ) : [];

  if (!scenarioObservations) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-white">Scenario Simulator & Decision Room</h2>
          <p className="text-sm text-slate-400">Adjust observations, see impact, and record decisions with stakeholders</p>
        </div>
        <div className="flex gap-3">
          <button onClick={onReset} className="px-4 py-2 text-sm text-slate-400 hover:text-white transition-colors">Reset</button>
          <button onClick={onApply} disabled={changedObservations.length === 0}
            className="px-4 py-2 bg-cyan-600 text-white rounded-lg text-sm font-medium hover:bg-cyan-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            Apply Scenario
          </button>
        </div>
      </div>

      {/* KPI Impact Summary */}
      <div className="bg-slate-900 rounded-xl border border-slate-800 p-6">
        <h3 className="text-sm font-medium text-slate-300 mb-4">Impact on Key Metrics</h3>
        {scenarioState && (
          <div className="grid grid-cols-3 gap-6">
            <ImpactCard label="Total Inventory $" current={currentState.total_inventory_value} scenario={scenarioState.total_inventory_value} format={(v) => `$${(v/1e6).toFixed(2)}M`} target={targets.total_inventory_value.value} />
            <ImpactCard label="Service Risk" current={currentState.service_risk} scenario={scenarioState.service_risk} format={(v) => `${(v*100).toFixed(1)}%`} target={targets.service_risk.value} />
            <ImpactCard label="Cash Impact" current={currentState.cash_impact} scenario={scenarioState.cash_impact} format={(v) => `$${(v/1e3).toFixed(0)}K`} target={targets.cash_impact.value} />
          </div>
        )}
      </div>

      <div className="grid grid-cols-3 gap-6">
        {/* Sliders - 2 columns */}
        <div className="col-span-2 space-y-4">
          <div className="flex gap-2 bg-slate-900 rounded-lg p-1">
            {Object.entries(categories).map(([key, cat]) => (
              <button key={key} onClick={() => setActiveCategory(key)}
                className={`flex-1 px-4 py-2 rounded-md text-sm font-medium transition-all ${
                  activeCategory === key ? 'bg-slate-700 text-white' : 'text-slate-400 hover:text-white'}`}>
                {cat.label}
              </button>
            ))}
          </div>

          <div className="bg-slate-900 rounded-xl border border-slate-800 p-6 space-y-6">
            {categories[activeCategory].observations.map(key => {
              if (!scenarioObservations[key]) return null;
              const obs = scenarioObservations[key];
              const baseline = observations[key].value;
              const config = sliderConfig[key];
              const changed = obs.value !== baseline;

              return (
                <div key={key} className={`p-4 rounded-lg transition-all ${changed ? 'bg-cyan-950 border border-cyan-800' : 'bg-slate-800'}`}>
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <div className="text-sm font-medium text-white">{obs.label}</div>
                      <div className="text-xs text-slate-500">{obs.source}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-lg font-bold text-white">{formatValue(key, obs.value)}</div>
                      {changed && (
                        <div className={`text-xs ${obs.value > baseline ? 'text-emerald-400' : 'text-red-400'}`}>
                          {obs.value > baseline ? '▲' : '▼'} from {formatValue(key, baseline)}
                        </div>
                      )}
                    </div>
                  </div>
                  <input type="range" min={config.min} max={config.max} step={config.step} value={obs.value}
                    onChange={(e) => onUpdateObservation(key, parseFloat(e.target.value))}
                    className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-cyan-500" />
                  <div className="flex justify-between text-xs text-slate-600 mt-1">
                    <span>{formatValue(key, config.min)}</span>
                    <span>{formatValue(key, config.max)}</span>
                  </div>
                </div>
              );
            })}
          </div>

          {changedObservations.length > 0 && (
            <div className="bg-slate-900 rounded-xl border border-slate-800 p-6">
              <h3 className="text-sm font-medium text-slate-300 mb-4">Required Actions to Achieve This Scenario</h3>
              <div className="grid grid-cols-2 gap-3">
                {changedObservations.flatMap(key => 
                  (actionImplications[key] || []).map((action, i) => (
                    <div key={`${key}-${i}`} className="flex items-start gap-3 p-3 bg-slate-800 rounded-lg">
                      <div className={`w-2 h-2 rounded-full mt-1.5 ${
                        action.urgency === 'high' ? 'bg-red-500' : action.urgency === 'medium' ? 'bg-amber-500' : 'bg-slate-500'
                      }`} />
                      <div>
                        <div className="text-sm text-white">{action.text}</div>
                        <div className="text-xs text-slate-500">Owner: {stakeholders[action.owner]?.name || action.owner}</div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* Decision Panel - 1 column */}
        <div className="space-y-4">
          <div className="bg-slate-900 rounded-xl border border-slate-800 p-6">
            <h3 className="text-sm font-medium text-slate-300 mb-4">Record Decision</h3>
            <div className="space-y-4">
              <div>
                <label className="text-xs text-slate-500 block mb-1">Scenario Name</label>
                <input type="text" value={scenarioName} onChange={(e) => onScenarioNameChange(e.target.value)}
                  placeholder="e.g., Q4 Supplier Risk Mitigation"
                  className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500" />
              </div>
              <div>
                <label className="text-xs text-slate-500 block mb-2">Participants (RACI)</label>
                <div className="grid grid-cols-4 gap-2">
                  {Object.values(stakeholders).map(s => {
                    const isActive = participants.includes(s.id);
                    return (
                      <button key={s.id} onClick={() => onToggleParticipant(s.id)}
                        className={`p-2 rounded-lg border transition-all ${
                          isActive ? 'border-cyan-500 bg-cyan-950' : 'border-slate-700 bg-slate-800 hover:border-slate-600'
                        }`} title={s.name}>
                        <div className="w-8 h-8 rounded-full mx-auto flex items-center justify-center text-white font-bold text-xs"
                          style={{ backgroundColor: s.color }}>
                          {s.name.slice(0, 2).toUpperCase()}
                        </div>
                        <div className="text-xs text-center mt-1 text-slate-400 truncate">{s.name}</div>
                      </button>
                    );
                  })}
                </div>
              </div>
              <button onClick={onRecordDecision}
                disabled={!scenarioName || participants.length === 0 || changedObservations.length === 0}
                className="w-full py-3 bg-gradient-to-r from-cyan-600 to-blue-600 text-white rounded-lg font-semibold hover:from-cyan-700 hover:to-blue-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Record Decision
              </button>
            </div>
          </div>

          <div className="bg-slate-900 rounded-xl border border-slate-800 p-6">
            <h3 className="text-sm font-medium text-slate-300 mb-4">Recent Decisions</h3>
            {decisions.length === 0 ? (
              <div className="text-center py-6 text-slate-500">
                <svg className="w-10 h-10 mx-auto mb-2 text-slate-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
                <p className="text-sm">No decisions recorded yet</p>
              </div>
            ) : (
              <div className="space-y-3 max-h-64 overflow-y-auto">
                {decisions.map(d => (
                  <div key={d.id} className="p-3 bg-slate-800 rounded-lg border border-slate-700">
                    <div className="text-sm font-medium text-white mb-1">{d.scenario}</div>
                    <div className="text-xs text-slate-500 mb-2">{new Date(d.timestamp).toLocaleString()}</div>
                    <div className="flex flex-wrap gap-1">
                      {d.participants.map(p => (
                        <span key={p} className="text-xs bg-slate-700 px-2 py-0.5 rounded text-slate-300">{p}</span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

// ----------------------------------------------------------------------------
// IMPACT CARD COMPONENT
// ----------------------------------------------------------------------------

const ImpactCard = ({ label, current, scenario, format, target }) => {
  const delta = scenario - current;
  const deltaPercent = (delta / current) * 100;
  const isGood = delta < 0;
  const meetsTarget = scenario <= target * 1.1;

  return (
    <div className={`p-4 rounded-lg border ${meetsTarget ? 'bg-emerald-950 border-emerald-800' : 'bg-slate-800 border-slate-700'}`}>
      <div className="text-xs text-slate-400 mb-2">{label}</div>
      <div className="flex items-end justify-between">
        <div>
          <div className="text-sm text-slate-500 line-through">{format(current)}</div>
          <div className={`text-2xl font-bold ${isGood ? 'text-emerald-400' : 'text-red-400'}`}>{format(scenario)}</div>
        </div>
        <div className={`text-right ${isGood ? 'text-emerald-400' : 'text-red-400'}`}>
          <div className="text-lg font-bold">{isGood ? '↓' : '↑'}</div>
          <div className="text-sm">{Math.abs(deltaPercent).toFixed(1)}%</div>
        </div>
      </div>
      {meetsTarget && (
        <div className="mt-2 text-xs text-emerald-400 flex items-center gap-1">
          <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
          Within target
        </div>
      )}
    </div>
  );
};

// ----------------------------------------------------------------------------
// TIME TRAVEL VIEW
// ----------------------------------------------------------------------------

const TimeTravelView = ({ historicalObs, historicalDecisions, asOfDate, setAsOfDate, asOfObservations, asOfState, currentState, observations, selectedDecision, setSelectedDecision, targets }) => {
  const [selectedFact, setSelectedFact] = useState('supplier_otif_dist');
  
  const timelineStart = new Date('2025-09-01T00:00:00Z');
  const timelineEnd = new Date();
  const totalMs = timelineEnd - timelineStart;
  
  const getPosition = (date) => ((new Date(date) - timelineStart) / totalMs) * 100;
  const formatDate = (iso) => new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  const formatDateTime = (iso) => new Date(iso).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

  const keyFacts = ['supplier_otif_dist', 'forecast_error_dist', 'consensus_forecast_qty', 'planned_production_qty'];
  
  const formatFactValue = (key, value) => {
    if (key.includes('pct') || key.includes('dist') || key.includes('cv') || key.includes('rate')) return `${(value * 100).toFixed(0)}%`;
    if (key.includes('cost') || key.includes('failure')) return `$${value}`;
    return value.toLocaleString();
  };
  
  const getLabel = (key) => dependencyGraph[key]?.label || key;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-white flex items-center gap-2">
          <span>⏱</span> Time Travel — Decision Lineage Explorer
        </h2>
        <p className="text-sm text-slate-400">Query the system as it was at any point in time. See what facts were known when decisions were made.</p>
      </div>

      {/* Timeline Selector */}
      <div className="bg-slate-900 rounded-xl border border-slate-800 p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-medium text-slate-300">Select Point in Time</h3>
          <div className="text-sm text-cyan-400 font-mono">As-Of: {formatDateTime(asOfDate)}</div>
        </div>
        
        <div className="relative h-24 mb-4">
          <div className="absolute top-8 left-0 right-0 h-1 bg-slate-700 rounded-full" />
          <div className="absolute top-6 w-4 h-4 bg-cyan-500 rounded-full border-2 border-white transform -translate-x-1/2 z-20"
            style={{ left: `${getPosition(asOfDate)}%` }} />
          
          {historicalDecisions.map((d, i) => (
            <button key={d.id} onClick={() => { setAsOfDate(d.timestamp); setSelectedDecision(d); }}
              className={`absolute top-5 w-6 h-6 rounded-full border-2 transform -translate-x-1/2 z-10 flex items-center justify-center text-xs font-bold transition-all ${
                selectedDecision?.id === d.id ? 'bg-amber-500 border-amber-300 scale-125' : 'bg-slate-700 border-slate-500 hover:bg-slate-600'
              }`} style={{ left: `${getPosition(d.timestamp)}%` }} title={d.scenario}>
              D{i + 1}
            </button>
          ))}
          
          {historicalObs[selectedFact]?.map((fact, i) => (
            <div key={i} className="absolute top-12 w-2 h-2 bg-violet-500 rounded-full transform -translate-x-1/2"
              style={{ left: `${getPosition(fact.tx_time)}%` }} title={`${getLabel(selectedFact)}: ${formatFactValue(selectedFact, fact.value)}`} />
          ))}
          
          <div className="absolute top-16 left-0 text-xs text-slate-500">Sep 1</div>
          <div className="absolute top-16 left-1/4 text-xs text-slate-500 transform -translate-x-1/2">Oct 1</div>
          <div className="absolute top-16 left-1/2 text-xs text-slate-500 transform -translate-x-1/2">Oct 15</div>
          <div className="absolute top-16 left-3/4 text-xs text-slate-500 transform -translate-x-1/2">Nov 1</div>
          <div className="absolute top-16 right-0 text-xs text-slate-500">Now</div>
        </div>
        
        <input type="range" min={timelineStart.getTime()} max={timelineEnd.getTime()} value={new Date(asOfDate).getTime()}
          onChange={(e) => { setAsOfDate(new Date(parseInt(e.target.value)).toISOString()); setSelectedDecision(null); }}
          className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-cyan-500" />
        
        <div className="flex gap-2 mt-4">
          {historicalDecisions.map((d, i) => (
            <button key={d.id} onClick={() => { setAsOfDate(d.timestamp); setSelectedDecision(d); }}
              className={`px-3 py-1 text-xs rounded-lg transition-all ${
                selectedDecision?.id === d.id ? 'bg-amber-600 text-white' : 'bg-slate-800 text-slate-400 hover:bg-slate-700'
              }`}>
              Decision {i + 1}: {formatDate(d.timestamp)}
            </button>
          ))}
          <button onClick={() => { setAsOfDate(new Date().toISOString()); setSelectedDecision(null); }}
            className="px-3 py-1 text-xs bg-cyan-900 text-cyan-400 rounded-lg hover:bg-cyan-800">
            Jump to Now
          </button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6">
        {/* Left: State Comparison */}
        <div className="space-y-4">
          <div className="bg-slate-900 rounded-xl border border-slate-800 p-6">
            <h3 className="text-sm font-medium text-slate-300 mb-4">KPIs: Then vs Now</h3>
            {asOfState && (
              <div className="space-y-4">
                <ComparisonRow label="Total Inventory $" then={asOfState.total_inventory_value} now={currentState.total_inventory_value} format={(v) => `$${(v/1e6).toFixed(2)}M`} />
                <ComparisonRow label="Service Risk" then={asOfState.service_risk} now={currentState.service_risk} format={(v) => `${(v*100).toFixed(1)}%`} />
                <ComparisonRow label="Cash Impact" then={asOfState.cash_impact} now={currentState.cash_impact} format={(v) => `$${(v/1e3).toFixed(0)}K`} />
              </div>
            )}
          </div>

          <div className="bg-slate-900 rounded-xl border border-slate-800 p-6">
            <h3 className="text-sm font-medium text-slate-300 mb-4">Facts Known As-Of {formatDateTime(asOfDate)}</h3>
            <div className="space-y-3">
              {keyFacts.map(key => {
                const thenFact = asOfObservations[key];
                const nowFact = observations[key];
                if (!thenFact) return null;
                return (
                  <div key={key} className="flex items-center justify-between p-3 bg-slate-800 rounded-lg">
                    <div>
                      <div className="text-sm text-white">{getLabel(key)}</div>
                      <div className="text-xs text-slate-500">{thenFact.source}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-lg font-bold text-amber-400">{formatFactValue(key, thenFact.value)}</div>
                      <div className="text-xs text-slate-500">Now: {formatFactValue(key, nowFact.value)}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Right: Decision Context & Fact Evolution */}
        <div className="space-y-4">
          {selectedDecision && (
            <div className="bg-amber-950 rounded-xl border border-amber-700 p-6">
              <div className="flex items-center gap-2 mb-4">
                <div className="w-8 h-8 rounded-full bg-amber-600 flex items-center justify-center text-white font-bold">
                  D{historicalDecisions.findIndex(d => d.id === selectedDecision.id) + 1}
                </div>
                <div>
                  <h3 className="font-medium text-white">{selectedDecision.scenario}</h3>
                  <div className="text-xs text-amber-400">{formatDateTime(selectedDecision.timestamp)}</div>
                </div>
              </div>
              <div className="space-y-3">
                <div>
                  <div className="text-xs text-slate-400 mb-1">Participants</div>
                  <div className="flex gap-1">
                    {selectedDecision.participants.map(p => (
                      <span key={p} className="text-xs bg-slate-800 px-2 py-1 rounded text-slate-300">{p}</span>
                    ))}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-slate-400 mb-1">Facts Known at Decision Time</div>
                  <div className="grid grid-cols-2 gap-2">
                    {Object.entries(selectedDecision.factsKnownAt).map(([key, value]) => (
                      <div key={key} className="text-xs bg-slate-800 p-2 rounded">
                        <span className="text-slate-400">{getLabel(key)}:</span>
                        <span className="text-white ml-1 font-medium">{formatFactValue(key, value)}</span>
                      </div>
                    ))}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-slate-400 mb-1">Outcome</div>
                  <div className="text-sm text-white">{selectedDecision.outcome}</div>
                </div>
                <div className={`text-xs px-2 py-1 rounded inline-block ${
                  selectedDecision.status === 'executed' ? 'bg-emerald-900 text-emerald-400' :
                  selectedDecision.status === 'in-progress' ? 'bg-amber-900 text-amber-400' : 'bg-slate-700 text-slate-400'
                }`}>{selectedDecision.status}</div>
              </div>
            </div>
          )}

          <div className="bg-slate-900 rounded-xl border border-slate-800 p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-medium text-slate-300">Fact Evolution Over Time</h3>
              <select value={selectedFact} onChange={(e) => setSelectedFact(e.target.value)}
                className="text-sm bg-slate-800 border border-slate-700 rounded px-2 py-1 text-white">
                {keyFacts.map(key => (<option key={key} value={key}>{getLabel(key)}</option>))}
              </select>
            </div>
            <div className="space-y-2 max-h-64 overflow-y-auto">
              {historicalObs[selectedFact]?.slice().reverse().map((fact, i, arr) => {
                const isCurrentAsOf = new Date(fact.tx_time) <= new Date(asOfDate) && 
                  (i === 0 || new Date(arr[i-1]?.tx_time) > new Date(asOfDate));
                return (
                  <div key={i} className={`p-3 rounded-lg border ${
                    isCurrentAsOf ? 'bg-cyan-950 border-cyan-700' : 
                    new Date(fact.tx_time) <= new Date(asOfDate) ? 'bg-slate-800 border-slate-700' : 'bg-slate-900 border-slate-800 opacity-50'
                  }`}>
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="text-lg font-bold text-white">{formatFactValue(selectedFact, fact.value)}</div>
                        <div className="text-xs text-slate-500">{fact.note}</div>
                      </div>
                      <div className="text-right">
                        <div className="text-xs text-slate-400">Recorded</div>
                        <div className="text-xs text-slate-300">{formatDateTime(fact.tx_time)}</div>
                      </div>
                    </div>
                    {isCurrentAsOf && <div className="mt-2 text-xs text-cyan-400">← Value known at selected time</div>}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {/* Query Examples */}
      <div className="bg-slate-900 rounded-xl border border-slate-800 p-6">
        <h3 className="text-sm font-medium text-slate-300 mb-4">Example Temporal Queries</h3>
        <div className="grid grid-cols-3 gap-4">
          <QueryCard question="What did we know when we made the Q4 Initial Planning decision?"
            action={() => { setAsOfDate('2025-09-15T16:30:00Z'); setSelectedDecision(historicalDecisions[0]); }} />
          <QueryCard question="What was the Supplier OTIF as we knew it on October 15th?"
            action={() => { setAsOfDate('2025-10-15T00:00:00Z'); setSelectedDecision(null); setSelectedFact('supplier_otif_dist'); }} />
          <QueryCard question="How did the forecast evolve from September to November?"
            action={() => { setAsOfDate('2025-11-01T00:00:00Z'); setSelectedDecision(null); setSelectedFact('consensus_forecast_qty'); }} />
        </div>
      </div>
    </div>
  );
};

const ComparisonRow = ({ label, then, now, format }) => {
  const delta = now - then;
  const deltaPercent = then !== 0 ? (delta / then) * 100 : 0;
  return (
    <div className="flex items-center justify-between p-3 bg-slate-800 rounded-lg">
      <div className="text-sm text-slate-300">{label}</div>
      <div className="flex items-center gap-4">
        <div className="text-right">
          <div className="text-xs text-slate-500">Then</div>
          <div className="text-sm font-medium text-amber-400">{format(then)}</div>
        </div>
        <div className="text-slate-600">→</div>
        <div className="text-right">
          <div className="text-xs text-slate-500">Now</div>
          <div className="text-sm font-medium text-cyan-400">{format(now)}</div>
        </div>
        <div className={`text-xs font-medium ${delta > 0 ? 'text-red-400' : 'text-emerald-400'}`}>
          {delta > 0 ? '+' : ''}{deltaPercent.toFixed(1)}%
        </div>
      </div>
    </div>
  );
};

const QueryCard = ({ question, action }) => (
  <button onClick={action}
    className="p-4 bg-slate-800 rounded-lg border border-slate-700 text-left hover:border-cyan-700 transition-all group">
    <div className="text-sm text-white group-hover:text-cyan-400">{question}</div>
    <div className="text-xs text-slate-500 mt-2 group-hover:text-slate-400">Click to query →</div>
  </button>
);

export default WorkingCapitalControlTower;
