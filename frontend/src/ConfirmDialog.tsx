type Props = {
  title: string;
  description: string;
  confirmLabel: string;
  busy?: boolean;
  destructive?: boolean;
  onConfirm: () => void;
  onClose: () => void;
};

function ConfirmDialog({ title, description, confirmLabel, busy = false, destructive = false, onConfirm, onClose }: Props) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <span className="eyebrow">Confirmação</span>
            <h2 id="confirm-dialog-title">{title}</h2>
          </div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">×</button>
        </div>
        <p className="confirm-dialog-copy">{description}</p>
        <div className="modal-actions">
          <button type="button" className="secondary-button" onClick={onClose} disabled={busy}>Cancelar</button>
          <button type="button" className={destructive ? 'danger-button' : 'primary-button'} onClick={onConfirm} disabled={busy}>
            {busy ? 'Processando...' : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}

export default ConfirmDialog;
