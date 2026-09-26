import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Search } from 'lucide-react';
import { useMemo, useState } from 'react';
import { updateMachineStatus } from './services/api';
import type { Machine, MachineStatus } from './types/api';
import { getApiErrorMessage } from './utils/apiError';
import './machines.css';

type Props = {
  machines: Machine[];
  canManage: boolean;
  isAdmin: boolean;
  onSelect: (machine: Machine) => void;
};

const statusLabel: Record<MachineStatus, string> = {
  RUNNING: 'Operando',
  STOPPED: 'Parada',
  MAINTENANCE: 'Manutenção',
  INACTIVE: 'Inativa',
};

function MachinesPanel({ machines, canManage, isAdmin, onSelect }: Props) {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [lineFilter, setLineFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | MachineStatus>('ALL');
  const [manufacturerFilter, setManufacturerFilter] = useState('ALL');
  const mutation = useMutation({
    mutationFn: ({ machineId, status }: { machineId: string; status: MachineStatus }) => updateMachineStatus(machineId, status),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['machines'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
        queryClient.invalidateQueries({ queryKey: ['audit-events'] }),
      ]);
    },
  });

  const lines = [...new Set(machines.map((machine) => machine.productionLine))].sort();
  const manufacturers = [...new Set(machines.map((machine) => machine.manufacturer).filter((value): value is string => Boolean(value)))].sort();
  const filteredMachines = useMemo(() => {
    const term = search.trim().toLowerCase();
    return machines.filter((machine) => {
      const haystack = [machine.assetCode, machine.name, machine.plant, machine.sector, machine.productionLine, machine.manufacturer ?? '', machine.model ?? ''].join(' ').toLowerCase();
      return (!term || haystack.includes(term))
        && (lineFilter === 'ALL' || machine.productionLine === lineFilter)
        && (statusFilter === 'ALL' || machine.status === statusFilter)
        && (manufacturerFilter === 'ALL' || machine.manufacturer === manufacturerFilter);
    });
  }, [machines, search, lineFilter, statusFilter, manufacturerFilter]);

  const availableStatuses: MachineStatus[] = isAdmin ? ['RUNNING', 'STOPPED', 'MAINTENANCE', 'INACTIVE'] : ['RUNNING', 'STOPPED', 'MAINTENANCE'];

  return (
    <section className="panel machines-panel" id="machines">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Ativos industriais</span>
          <h2>Máquinas</h2>
        </div>
        <span className="machine-count">{filteredMachines.length} de {machines.length}</span>
      </div>

      <div className="machine-filters">
        <label className="machine-search"><Search size={15} /><input type="search" placeholder="Buscar ativo, máquina, linha ou modelo" value={search} onChange={(event) => setSearch(event.target.value)} /></label>
        <select value={lineFilter} onChange={(event) => setLineFilter(event.target.value)}><option value="ALL">Todas as linhas</option>{lines.map((line) => <option key={line}>{line}</option>)}</select>
        <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as 'ALL' | MachineStatus)}><option value="ALL">Todos os status</option>{Object.entries(statusLabel).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
        <select value={manufacturerFilter} onChange={(event) => setManufacturerFilter(event.target.value)}><option value="ALL">Todos os fabricantes</option>{manufacturers.map((manufacturer) => <option key={manufacturer}>{manufacturer}</option>)}</select>
      </div>

      {machines.length === 0 && <div className="empty-state">Nenhuma máquina cadastrada.</div>}
      {machines.length > 0 && filteredMachines.length === 0 && <div className="empty-state">Nenhuma máquina encontrada com esses filtros.</div>}

      <div className="machine-table-wrap">
        <table className="machine-table">
          <thead><tr><th>Ativo</th><th>Localização</th><th>Fabricante</th><th>Status</th><th /></tr></thead>
          <tbody>
            {filteredMachines.map((machine) => (
              <tr key={machine.id}>
                <td><strong>{machine.assetCode}</strong><span>{machine.name}</span></td>
                <td><strong>{machine.productionLine}</strong><span>{machine.plant} · {machine.sector}</span></td>
                <td>{machine.manufacturer ?? '—'}</td>
                <td>
                  {canManage && (isAdmin || machine.status !== 'INACTIVE') ? (
                    <select className={`machine-status status-${machine.status.toLowerCase()}`} value={machine.status} disabled={mutation.isPending} onChange={(event) => mutation.mutate({ machineId: machine.id, status: event.target.value as MachineStatus })}>
                      {availableStatuses.map((value) => <option key={value} value={value}>{statusLabel[value]}</option>)}
                    </select>
                  ) : <span className={`machine-status-label status-${machine.status.toLowerCase()}`}>{statusLabel[machine.status]}</span>}
                </td>
                <td><button type="button" className="machine-detail-button" onClick={() => onSelect(machine)}>Detalhes</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {mutation.isError && <div className="inline-error">{getApiErrorMessage(mutation.error, 'Não foi possível atualizar o status da máquina.')}</div>}
    </section>
  );
}

export default MachinesPanel;
