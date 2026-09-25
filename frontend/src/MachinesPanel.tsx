import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateMachineStatus } from './services/api';
import type { Machine, MachineStatus } from './types/api';
import './machines.css';

type Props = {
  machines: Machine[];
  canManage: boolean;
};

const statusLabel: Record<MachineStatus, string> = {
  RUNNING: 'Operando',
  STOPPED: 'Parada',
  MAINTENANCE: 'Manutenção',
  INACTIVE: 'Inativa',
};

function MachinesPanel({ machines, canManage }: Props) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: ({ machineId, status }: { machineId: string; status: MachineStatus }) => updateMachineStatus(machineId, status),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['machines'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ]);
    },
  });

  return (
    <section className="panel machines-panel" id="machines">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Ativos industriais</span>
          <h2>Máquinas</h2>
        </div>
        <span className="machine-count">{machines.length} cadastradas</span>
      </div>

      {machines.length === 0 && <div className="empty-state">Nenhuma máquina cadastrada.</div>}

      <div className="machine-table-wrap">
        <table className="machine-table">
          <thead>
            <tr>
              <th>Ativo</th>
              <th>Linha</th>
              <th>Fabricante</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {machines.map((machine) => (
              <tr key={machine.id}>
                <td>
                  <strong>{machine.assetCode}</strong>
                  <span>{machine.name}</span>
                </td>
                <td>{machine.productionLine}</td>
                <td>{machine.manufacturer ?? '—'}</td>
                <td>
                  {canManage ? (
                    <select
                      className={`machine-status status-${machine.status.toLowerCase()}`}
                      value={machine.status}
                      disabled={mutation.isPending}
                      onChange={(event) => mutation.mutate({ machineId: machine.id, status: event.target.value as MachineStatus })}
                    >
                      {Object.entries(statusLabel).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select>
                  ) : (
                    <span className={`machine-status-label status-${machine.status.toLowerCase()}`}>{statusLabel[machine.status]}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {mutation.isError && <div className="inline-error">Não foi possível atualizar o status da máquina.</div>}
    </section>
  );
}

export default MachinesPanel;
