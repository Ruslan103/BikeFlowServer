import {api, registerShutdownButton, showMessage} from './common.js';

const output = document.getElementById('log-output');
const status = document.getElementById('log-status');
const empty = document.getElementById('logs-empty');
const refresh = document.getElementById('refresh-logs');

async function loadLogs() {
  refresh.disabled = true;
  try {
    const result = await api('/api/admin/logs');
    output.textContent = result.lines.join('\n');
    empty.hidden = result.available;
    output.hidden = !result.available;
    status.textContent = result.available
      ? `Обновлено: ${new Date().toLocaleTimeString()}`
      : 'Файл логов ещё не создан';
  } catch (error) {
    showMessage(error instanceof TypeError ? 'Не удалось получить логи сервера.' : error.message);
  } finally {
    refresh.disabled = false;
  }
}

refresh.addEventListener('click', loadLogs);

document.getElementById('logout').addEventListener('click', async () => {
  try {
    await api('/api/auth/logout', {method: 'POST'});
    location.assign('/login');
  } catch (error) {
    showMessage(error.message);
  }
});

registerShutdownButton();

loadLogs();
api('/api/admin/settings').then(settings => {
  document.getElementById('users-nav').hidden = settings.setupMode;
}).catch(() => {});
window.setInterval(() => {
  if (!document.hidden) loadLogs();
}, 5000);
