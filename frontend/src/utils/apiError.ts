import axios from 'axios';

export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === 'string' && message.trim()) return message;
    if (error.code === 'ECONNABORTED') return 'A API demorou demais para responder. Tente novamente em alguns instantes.';
    if (!error.response) return 'Não foi possível conectar à API. Verifique o serviço e tente novamente.';
  }
  return fallback;
}
