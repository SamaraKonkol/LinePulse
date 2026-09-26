import axios from 'axios';

export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === 'string' && message.trim()) return message;

    const status = error.response?.status;
    if (status === 401) return 'Sua sessão não é mais válida. Entre novamente no LinePulse.';
    if (status === 403) return 'O backend recusou esta ação por falta de permissão para o usuário atual.';
    if (status === 404) return 'Este recurso não existe na API publicada. O frontend e o backend podem estar em versões diferentes.';
    if (status === 502 || status === 503 || status === 504) return 'A API está iniciando ou temporariamente indisponível. Tente novamente em alguns instantes.';
    if (status && status >= 500) return `A API retornou um erro interno (${status}).`;

    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      return 'A API demorou mais de um minuto para responder. O serviço pode estar acordando no Render.';
    }
    if (!error.response) return 'Não foi possível conectar à API. Verifique se o serviço do Render está online.';
  }
  return fallback;
}
