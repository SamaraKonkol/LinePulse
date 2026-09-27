import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, Check, Factory, Network, Pencil, Plus, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { createPlant, createProductionLine, createSector, getPlants, getSectors, getStructureLines, updatePlant, updatePlantStatus, updateProductionLine, updateProductionLineStatus, updateSector, updateSectorStatus } from './services/api';
import { getApiErrorMessage } from './utils/apiError';

type EditState =
  | { type: 'plant'; id: string; name: string; code: string }
  | { type: 'sector'; id: string; plantId: string; name: string; code: string }
  | { type: 'line'; id: string; sectorId: string; name: string; code: string }
  | null;

function StructureAdminPanel() {
  const queryClient = useQueryClient();
  const plantsQuery = useQuery({ queryKey: ['admin-plants'], queryFn: getPlants });
  const sectorsQuery = useQuery({ queryKey: ['admin-sectors'], queryFn: getSectors });
  const linesQuery = useQuery({ queryKey: ['admin-structure-lines'], queryFn: getStructureLines });
  const [plantDraft, setPlantDraft] = useState({ name: '', code: '' });
  const [sectorDraft, setSectorDraft] = useState({ plantId: '', name: '', code: '' });
  const [lineDraft, setLineDraft] = useState({ sectorId: '', name: '', code: '' });
  const [editing, setEditing] = useState<EditState>(null);

  const plants = plantsQuery.data ?? [];
  const sectors = sectorsQuery.data ?? [];
  const lines = linesQuery.data ?? [];
  const activePlants = plants.filter((plant) => plant.active);
  const activeSectors = sectors.filter((sector) => sector.active && activePlants.some((plant) => plant.id === sector.plantId));

  useEffect(() => {
    if (!sectorDraft.plantId && activePlants[0]) setSectorDraft((draft) => ({ ...draft, plantId: activePlants[0].id }));
  }, [activePlants, sectorDraft.plantId]);

  useEffect(() => {
    if (!lineDraft.sectorId && activeSectors[0]) setLineDraft((draft) => ({ ...draft, sectorId: activeSectors[0].id }));
  }, [activeSectors, lineDraft.sectorId]);

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['admin-plants'] }),
      queryClient.invalidateQueries({ queryKey: ['admin-sectors'] }),
      queryClient.invalidateQueries({ queryKey: ['admin-structure-lines'] }),
      queryClient.invalidateQueries({ queryKey: ['admin-production-lines'] }),
      queryClient.invalidateQueries({ queryKey: ['machines'] }),
      queryClient.invalidateQueries({ queryKey: ['audit-events'] }),
    ]);
  };

  const plantMutation = useMutation({ mutationFn: createPlant, onSuccess: async () => { setPlantDraft({ name: '', code: '' }); await refresh(); } });
  const sectorMutation = useMutation({ mutationFn: createSector, onSuccess: async () => { setSectorDraft((draft) => ({ ...draft, name: '', code: '' })); await refresh(); } });
  const lineMutation = useMutation({ mutationFn: createProductionLine, onSuccess: async () => { setLineDraft((draft) => ({ ...draft, name: '', code: '' })); await refresh(); } });
  const updatePlantMutation = useMutation({ mutationFn: ({ id, name, code }: { id: string; name: string; code: string }) => updatePlant(id, { name, code }), onSuccess: async () => { setEditing(null); await refresh(); } });
  const updateSectorMutation = useMutation({ mutationFn: ({ id, plantId, name, code }: { id: string; plantId: string; name: string; code: string }) => updateSector(id, { plantId, name, code }), onSuccess: async () => { setEditing(null); await refresh(); } });
  const updateLineMutation = useMutation({ mutationFn: ({ id, sectorId, name, code }: { id: string; sectorId: string; name: string; code: string }) => updateProductionLine(id, { sectorId, name, code }), onSuccess: async () => { setEditing(null); await refresh(); } });
  const plantStatusMutation = useMutation({ mutationFn: ({ id, active }: { id: string; active: boolean }) => updatePlantStatus(id, active), onSuccess: refresh });
  const sectorStatusMutation = useMutation({ mutationFn: ({ id, active }: { id: string; active: boolean }) => updateSectorStatus(id, active), onSuccess: refresh });
  const lineStatusMutation = useMutation({ mutationFn: ({ id, active }: { id: string; active: boolean }) => updateProductionLineStatus(id, active), onSuccess: refresh });

  const errors = [plantMutation.error, sectorMutation.error, lineMutation.error, updatePlantMutation.error, updateSectorMutation.error, updateLineMutation.error, plantStatusMutation.error, sectorStatusMutation.error, lineStatusMutation.error].filter(Boolean);
  const loading = plantsQuery.isLoading || sectorsQuery.isLoading || linesQuery.isLoading;

  function saveEdit() {
    if (!editing) return;
    if (editing.type === 'plant') updatePlantMutation.mutate(editing);
    if (editing.type === 'sector') updateSectorMutation.mutate(editing);
    if (editing.type === 'line') updateLineMutation.mutate(editing);
  }

  return (
    <article className="panel admin-card structure-card">
      <div className="panel-heading">
        <div><span className="eyebrow">Hierarquia operacional</span><h3>Planta · Setor · Linha</h3></div>
        <Network size={20} />
      </div>

      {errors.length > 0 && <div className="inline-error">{getApiErrorMessage(errors[0], 'Não foi possível atualizar a estrutura industrial.')}</div>}
      {loading && <div className="empty-state">Carregando estrutura...</div>}

      <div className="structure-grid">
        <section className="structure-column">
          <div className="structure-title"><Building2 size={17} /><strong>Plantas</strong></div>
          <form className="structure-form" onSubmit={(event) => { event.preventDefault(); plantMutation.mutate({ name: plantDraft.name, code: plantDraft.code }); }}>
            <input placeholder="Nome da planta" value={plantDraft.name} onChange={(event) => setPlantDraft({ ...plantDraft, name: event.target.value })} required />
            <div className="structure-form-row"><input placeholder="Código" value={plantDraft.code} onChange={(event) => setPlantDraft({ ...plantDraft, code: event.target.value.toUpperCase() })} required /><button className="admin-icon-button" title="Adicionar planta"><Plus size={16} /></button></div>
          </form>
          <div className="structure-list">{plants.map((plant) => editing?.type === 'plant' && editing.id === plant.id ? (
            <div className="structure-edit" key={plant.id}>
              <input value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} />
              <input value={editing.code} onChange={(event) => setEditing({ ...editing, code: event.target.value.toUpperCase() })} />
              <div><button type="button" onClick={saveEdit}><Check size={14} /></button><button type="button" onClick={() => setEditing(null)}><X size={14} /></button></div>
            </div>
          ) : (
            <div className={`structure-item ${!plant.active ? 'inactive' : ''}`} key={plant.id}><span><strong>{plant.code}</strong>{plant.name}</span><div className="structure-actions"><button type="button" className="structure-edit-button" onClick={() => setEditing({ type: 'plant', id: plant.id, name: plant.name, code: plant.code })}><Pencil size={13} /></button><button type="button" onClick={() => plantStatusMutation.mutate({ id: plant.id, active: !plant.active })}>{plant.active ? 'Desativar' : 'Ativar'}</button></div></div>
          ))}</div>
        </section>

        <section className="structure-column">
          <div className="structure-title"><Factory size={17} /><strong>Setores</strong></div>
          <form className="structure-form" onSubmit={(event) => { event.preventDefault(); sectorMutation.mutate(sectorDraft); }}>
            <select value={sectorDraft.plantId} onChange={(event) => setSectorDraft({ ...sectorDraft, plantId: event.target.value })} required><option value="">Selecione a planta</option>{activePlants.map((plant) => <option key={plant.id} value={plant.id}>{plant.code} · {plant.name}</option>)}</select>
            <input placeholder="Nome do setor" value={sectorDraft.name} onChange={(event) => setSectorDraft({ ...sectorDraft, name: event.target.value })} required />
            <div className="structure-form-row"><input placeholder="Código" value={sectorDraft.code} onChange={(event) => setSectorDraft({ ...sectorDraft, code: event.target.value.toUpperCase() })} required /><button className="admin-icon-button" title="Adicionar setor"><Plus size={16} /></button></div>
          </form>
          <div className="structure-list">{sectors.map((sector) => editing?.type === 'sector' && editing.id === sector.id ? (
            <div className="structure-edit" key={sector.id}>
              <select value={editing.plantId} onChange={(event) => setEditing({ ...editing, plantId: event.target.value })}>{activePlants.map((plant) => <option key={plant.id} value={plant.id}>{plant.code} · {plant.name}</option>)}</select>
              <input value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} />
              <input value={editing.code} onChange={(event) => setEditing({ ...editing, code: event.target.value.toUpperCase() })} />
              <div><button type="button" onClick={saveEdit}><Check size={14} /></button><button type="button" onClick={() => setEditing(null)}><X size={14} /></button></div>
            </div>
          ) : (
            <div className={`structure-item ${!sector.active ? 'inactive' : ''}`} key={sector.id}><span><strong>{sector.code}</strong>{sector.plant} · {sector.name}</span><div className="structure-actions"><button type="button" className="structure-edit-button" onClick={() => setEditing({ type: 'sector', id: sector.id, plantId: sector.plantId, name: sector.name, code: sector.code })}><Pencil size={13} /></button><button type="button" onClick={() => sectorStatusMutation.mutate({ id: sector.id, active: !sector.active })}>{sector.active ? 'Desativar' : 'Ativar'}</button></div></div>
          ))}</div>
        </section>

        <section className="structure-column">
          <div className="structure-title"><Network size={17} /><strong>Linhas</strong></div>
          <form className="structure-form" onSubmit={(event) => { event.preventDefault(); lineMutation.mutate(lineDraft); }}>
            <select value={lineDraft.sectorId} onChange={(event) => setLineDraft({ ...lineDraft, sectorId: event.target.value })} required><option value="">Selecione o setor</option>{activeSectors.map((sector) => <option key={sector.id} value={sector.id}>{sector.plant} · {sector.name}</option>)}</select>
            <input placeholder="Nome da linha" value={lineDraft.name} onChange={(event) => setLineDraft({ ...lineDraft, name: event.target.value })} required />
            <div className="structure-form-row"><input placeholder="Código" value={lineDraft.code} onChange={(event) => setLineDraft({ ...lineDraft, code: event.target.value.toUpperCase() })} required /><button className="admin-icon-button" title="Adicionar linha"><Plus size={16} /></button></div>
          </form>
          <div className="structure-list">{lines.map((line) => editing?.type === 'line' && editing.id === line.id ? (
            <div className="structure-edit" key={line.id}>
              <select value={editing.sectorId} onChange={(event) => setEditing({ ...editing, sectorId: event.target.value })}>{activeSectors.map((sector) => <option key={sector.id} value={sector.id}>{sector.plant} · {sector.name}</option>)}</select>
              <input value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} />
              <input value={editing.code} onChange={(event) => setEditing({ ...editing, code: event.target.value.toUpperCase() })} />
              <div><button type="button" onClick={saveEdit}><Check size={14} /></button><button type="button" onClick={() => setEditing(null)}><X size={14} /></button></div>
            </div>
          ) : (
            <div className={`structure-item ${!line.active ? 'inactive' : ''}`} key={line.id}><span><strong>{line.code}</strong>{line.sector} · {line.name}</span><div className="structure-actions"><button type="button" className="structure-edit-button" onClick={() => setEditing({ type: 'line', id: line.id, sectorId: line.sectorId, name: line.name, code: line.code })}><Pencil size={13} /></button><button type="button" onClick={() => lineStatusMutation.mutate({ id: line.id, active: !line.active })}>{line.active ? 'Desativar' : 'Ativar'}</button></div></div>
          ))}</div>
        </section>
      </div>
    </article>
  );
}

export default StructureAdminPanel;
