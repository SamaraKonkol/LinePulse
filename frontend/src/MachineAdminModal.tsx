import { FormEvent, useMemo, useState } from 'react';
import './incident-modal.css';
import type { Machine, MachineStatus, ProductionLine } from './types/api';

export type MachineDraft = {
  productionLineId: string;
  name: string;
  assetCode: string;
  manufacturer?: string;
  model?: string;
  serialNumber?: string;
  status: MachineStatus;
  installedAt?: string;
};

type Props = {
  machine?: Machine | null;
  productionLines: ProductionLine[];
  loading: boolean;
  onClose: () => void;
  onSubmit: (draft: MachineDraft) => void;
};

function MachineAdminModal({ machine, productionLines, loading, onClose, onSubmit }: Props) {
  const activeLines = useMemo(() => productionLines.filter((line) => line.active || line.id === machine?.productionLineId), [productionLines, machine]);
  const [productionLineId, setProductionLineId] = useState(machine?.productionLineId ?? activeLines[0]?.id ?? '');
  const [name, setName] = useState(machine?.name ?? '');
  const [assetCode, setAssetCode] = useState(machine?.assetCode ?? '');
  const [manufacturer, setManufacturer] = useState(machine?.manufacturer ?? '');
  const [model, setModel] = useState(machine?.model ?? '');
  const [serialNumber, setSerialNumber] = useState(machine?.serialNumber ?? '');
  const [status, setStatus] = useState<MachineStatus>(machine?.status ?? 'RUNNING');
  const [installedAt, setInstalledAt] = useState(machine?.installedAt ?? '');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit({ productionLineId, name, assetCode, manufacturer: manufacturer || undefined, model: model || undefined, serialNumber: serialNumber || undefined, status, installedAt: installedAt || undefined });
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="machine-admin-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div><span className="eyebrow">Administração de ativos</span><h2 id="machine-admin-title">{machine ? 'Editar máquina' : 'Cadastrar máquina'}</h2></div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">×</button>
        </div>

        <form className="incident-form" onSubmit={handleSubmit}>
          <label>
            Planta / setor / linha
            <select value={productionLineId} onChange={(event) => setProductionLineId(event.target.value)} required>
              {activeLines.map((line) => <option key={line.id} value={line.id}>{line.plant} · {line.sector} · {line.code} — {line.name}</option>)}
            </select>
          </label>
          <label>Código do ativo<input value={assetCode} onChange={(event) => setAssetCode(event.target.value)} required maxLength={80} placeholder="Ex.: PRENSA-01" /></label>
          <label>Nome<input value={name} onChange={(event) => setName(event.target.value)} required maxLength={160} placeholder="Ex.: Prensa hidráulica" /></label>
          <label>Fabricante<input value={manufacturer} onChange={(event) => setManufacturer(event.target.value)} maxLength={160} /></label>
          <label>Modelo<input value={model} onChange={(event) => setModel(event.target.value)} maxLength={160} /></label>
          <label>Número de série<input value={serialNumber} onChange={(event) => setSerialNumber(event.target.value)} maxLength={160} /></label>
          {!machine && <label>Status inicial<select value={status} onChange={(event) => setStatus(event.target.value as MachineStatus)}><option value="RUNNING">Operando</option><option value="STOPPED">Parada</option><option value="MAINTENANCE">Manutenção</option><option value="INACTIVE">Inativa</option></select></label>}
          <label>Data de instalação<input type="date" value={installedAt} onChange={(event) => setInstalledAt(event.target.value)} /></label>
          <div className="modal-actions"><button type="button" className="secondary-button" onClick={onClose}>Cancelar</button><button className="primary-button" disabled={loading || !productionLineId}>{loading ? 'Salvando...' : machine ? 'Salvar alterações' : 'Cadastrar máquina'}</button></div>
        </form>
      </section>
    </div>
  );
}

export default MachineAdminModal;
