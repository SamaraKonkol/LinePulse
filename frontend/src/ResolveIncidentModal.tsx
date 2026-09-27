import { FormEvent, useState } from 'react';
import type { Incident } from './types/api';

function ResolveIncidentModal({ incident, loading, onClose, onSubmit }: { incident: Incident; loading: boolean; onClose: () => void; onSubmit: (draft: { rootCause: string; solution: string }) => void }) {
  const [rootCause, setRootCause] = useState('');
  const [solution, setSolution] = useState('');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit({ rootCause, solution });
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="resolve-incident-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div><span className="eyebrow">Diagnóstico técnico</span><h2 id="resolve-incident-title">Resolver {incident.assetCode}</h2></div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">×</button>
        </div>
        <p className="modal-context">{incident.title}</p>
        <form className="incident-form" onSubmit={handleSubmit}>
          <label>Causa raiz<textarea value={rootCause} onChange={(event) => setRootCause(event.target.value)} rows={4} minLength={3} required placeholder="Ex.: rolamento com desgaste acima do limite" /></label>
          <label>Solução aplicada<textarea value={solution} onChange={(event) => setSolution(event.target.value)} rows={4} minLength={3} required placeholder="Ex.: substituição do rolamento e reaperto do conjunto" /></label>
          <div className="modal-actions"><button type="button" className="secondary-button" onClick={onClose}>Cancelar</button><button className="primary-button" disabled={loading}>{loading ? 'Salvando...' : 'Concluir ocorrência'}</button></div>
        </form>
      </section>
    </div>
  );
}

export default ResolveIncidentModal;
