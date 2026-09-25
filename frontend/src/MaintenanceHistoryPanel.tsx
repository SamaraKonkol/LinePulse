import { useMemo, useState } from 'react';
import type { MaintenanceType, WorkOrder, WorkOrderPriority, WorkOrderStatus } from './types/api';

type Props = {
  orders: WorkOrder[];
  onClose: () => void;
};

const statusLabel: Record<WorkOrderStatus, string> = {
  OPEN: 'Aberta',
  IN_PROGRESS: 'Em andamento',
  COMPLETED: 'Concluída',
  CANCELLED: 'Cancelada',
};

const typeLabel: Record<MaintenanceType, string> = {
  CORRECTIVE: 'Corretiva',
  PREVENTIVE: 'Preventiva',
  INSPECTION: 'Inspeção',
};

const priorityLabel: Record<WorkOrderPriority, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
  CRITICAL: 'Crítica',
};

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString('pt-BR') : '—';
}

function MaintenanceHistoryPanel({ orders, onClose }: Props) {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<'ALL' | WorkOrderStatus>('ALL');
  const [type, setType] = useState<'ALL' | MaintenanceType>('ALL');
  const [priority, setPriority] = useState<'ALL' | WorkOrderPriority>('ALL');

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return orders.filter((order) => {
      const matchesSearch = !term || [order.assetCode, order.machineName, order.title, order.description]
        .some((value) => value.toLowerCase().includes(term));
      const matchesStatus = status === 'ALL' || order.status === status;
      const matchesType = type === 'ALL' || order.type === type;
      const matchesPriority = priority === 'ALL' || order.priority === priority;
      return matchesSearch && matchesStatus && matchesType && matchesPriority;
    });
  }, [orders, priority, search, status, type]);

  return (
    <section className="panel maintenance-history-panel">
      <div className="panel-heading maintenance-history-heading">
        <div>
          <span className="eyebrow">Histórico</span>
          <h2>Ordens de manutenção</h2>
          <small>{filtered.length} de {orders.length} registros</small>
        </div>
        <button className="text-button" type="button" onClick={onClose}>Fechar</button>
      </div>

      <div className="maintenance-filters">
        <input
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Buscar ativo, máquina ou serviço"
        />
        <select value={status} onChange={(event) => setStatus(event.target.value as 'ALL' | WorkOrderStatus)}>
          <option value="ALL">Todos os status</option>
          <option value="OPEN">Aberta</option>
          <option value="IN_PROGRESS">Em andamento</option>
          <option value="COMPLETED">Concluída</option>
          <option value="CANCELLED">Cancelada</option>
        </select>
        <select value={type} onChange={(event) => setType(event.target.value as 'ALL' | MaintenanceType)}>
          <option value="ALL">Todos os tipos</option>
          <option value="CORRECTIVE">Corretiva</option>
          <option value="PREVENTIVE">Preventiva</option>
          <option value="INSPECTION">Inspeção</option>
        </select>
        <select value={priority} onChange={(event) => setPriority(event.target.value as 'ALL' | WorkOrderPriority)}>
          <option value="ALL">Todas as prioridades</option>
          <option value="CRITICAL">Crítica</option>
          <option value="HIGH">Alta</option>
          <option value="MEDIUM">Média</option>
          <option value="LOW">Baixa</option>
        </select>
      </div>

      {filtered.length === 0 && <div className="empty-state">Nenhuma ordem encontrada com esses filtros.</div>}

      {filtered.length > 0 && (
        <div className="maintenance-history-table-wrap">
          <table className="maintenance-history-table">
            <thead>
              <tr>
                <th>Ativo / serviço</th>
                <th>Tipo</th>
                <th>Prioridade</th>
                <th>Status</th>
                <th>Início</th>
                <th>Conclusão</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((order) => (
                <tr key={order.id}>
                  <td>
                    <strong>{order.assetCode} · {order.machineName}</strong>
                    <span>{order.title}</span>
                  </td>
                  <td>{typeLabel[order.type]}</td>
                  <td><span className={`history-priority ${order.priority.toLowerCase()}`}>{priorityLabel[order.priority]}</span></td>
                  <td><span className={`maintenance-status ${order.status.toLowerCase()}`}>{statusLabel[order.status]}</span></td>
                  <td>{formatDate(order.startedAt)}</td>
                  <td>{formatDate(order.completedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default MaintenanceHistoryPanel;
