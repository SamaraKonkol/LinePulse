import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { IncidentTrendPoint } from './types/api';
import './trend-chart.css';

type Props = {
  data: IncidentTrendPoint[];
  loading: boolean;
};

function IncidentTrendChart({ data, loading }: Props) {
  const chartData = data.map((point) => ({
    ...point,
    label: new Date(`${point.date}T12:00:00`).toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', ''),
  }));

  return (
    <section className="panel trend-panel">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Últimos 7 dias</span>
          <h2>Ocorrências registradas</h2>
        </div>
        <span className="trend-total">{data.reduce((total, point) => total + point.incidents, 0)} no período</span>
      </div>

      {loading ? (
        <div className="chart-loading">Carregando histórico...</div>
      ) : (
        <div className="chart-container">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 8, right: 4, left: -22, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e7ece8" />
              <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: '#748085', fontSize: 12 }} />
              <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: '#748085', fontSize: 12 }} />
              <Tooltip cursor={{ fill: '#fbf7f2' }} contentStyle={{ border: '1px solid #e2c2a9', borderRadius: 8, boxShadow: 'none' }} />
              <Bar dataKey="incidents" name="Ocorrências" fill="#b87333" radius={[5, 5, 0, 0]} maxBarSize={46} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  );
}

export default IncidentTrendChart;
