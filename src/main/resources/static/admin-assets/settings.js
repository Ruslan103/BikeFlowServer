import {api, download, registerShutdownButton, showMessage} from './common.js';

const form = document.getElementById('settings-form');
const save = document.getElementById('save');
const secrets = [
  ['database-password', 'databasePassword', 'databasePasswordConfigured', 'clearDatabasePassword'],
  ['trello-api-key', 'trelloApiKey', 'trelloApiKeyConfigured', 'clearTrelloApiKey'],
  ['trello-token', 'trelloToken', 'trelloTokenConfigured', 'clearTrelloToken']
];

function render(settings) {
  form.elements.databaseUrl.value = settings.databaseUrl;
  form.elements.databaseUsername.value = settings.databaseUsername || 'postgres';
  document.getElementById('setup-notice').hidden = !settings.setupMode;
  document.getElementById('users-nav').hidden = settings.setupMode;
  document.getElementById('mode').textContent = settings.setupMode ? 'Первичная настройка' : 'Администратор';
  document.getElementById('restart-notice').hidden = settings.setupMode || !settings.databaseRestartRequired;
  for (const [id, , configured] of secrets) {
    const input = document.getElementById(id);
    const status = document.getElementById(id + '-status');
    input.value = '';
    input.disabled = false;
    input.placeholder = settings[configured] ? 'Оставьте пустым, чтобы сохранить' : 'Не задано';
    status.textContent = settings[configured] ? 'Сохранён' : 'Не задан';
    status.className = settings[configured] ? 'secret-status configured' : 'secret-status';
    document.getElementById('clear-' + id).checked = false;
    document.getElementById('clear-' + id + '-row').hidden = !settings[configured];
  }
}

function appendAddress(container, address) {
  const item = document.createElement('li');
  const value = document.createElement('code');
  value.textContent = address;
  const copy = document.createElement('button');
  copy.type = 'button';
  copy.className = 'button subtle address-copy';
  copy.textContent = 'Скопировать';
  copy.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(address);
      copy.textContent = 'Скопировано';
      window.setTimeout(() => copy.textContent = 'Скопировать', 1500);
    } catch {
      copy.textContent = 'Не удалось скопировать';
    }
  });
  item.append(value, copy);
  container.append(item);
}

async function loadServerAddresses() {
  const local = document.getElementById('local-addresses');
  const publicAddress = document.getElementById('public-address');
  const refresh = document.getElementById('refresh-addresses');
  refresh.disabled = true;
  local.replaceChildren();
  publicAddress.textContent = 'Определяем адрес…';
  try {
    const addresses = await api('/api/admin/server-addresses');
    if (addresses.localBaseUrls.length) {
      addresses.localBaseUrls.forEach(address => appendAddress(local, address));
    } else {
      local.textContent = 'Локальные IPv4-адреса не обнаружены.';
    }
    publicAddress.replaceChildren();
    if (addresses.publicBaseUrl) {
      appendAddress(publicAddress, addresses.publicBaseUrl);
    } else {
      publicAddress.textContent = 'Не удалось определить. Проверьте подключение сервера к интернету и нажмите «Обновить».';
    }
  } catch (error) {
    local.textContent = 'Не удалось загрузить адреса сервера.';
    publicAddress.textContent = error.message;
  } finally {
    refresh.disabled = false;
  }
}

document.getElementById('refresh-addresses').addEventListener('click', loadServerAddresses);
loadServerAddresses();

for (const [id] of secrets) {
  document.getElementById('clear-' + id).addEventListener('change', event => {
    const input = document.getElementById(id);
    input.disabled = event.target.checked;
    if (input.disabled) input.value = '';
  });
}

form.addEventListener('submit', async event => {
  event.preventDefault();
  save.disabled = true;
  const body = {
    databaseUrl: form.elements.databaseUrl.value.trim(),
    databaseUsername: form.elements.databaseUsername.value.trim()
  };
  for (const [id, field, , clearField] of secrets) {
    body[field] = document.getElementById(id).value || null;
    body[clearField] = document.getElementById('clear-' + id).checked;
  }
  try {
    const settings = await api('/api/admin/settings', {method: 'PUT', body});
    render(settings);
    if (settings.loginAgainRequired) {
      showMessage('Подключение переключено. Войдите снова в целевой базе данных.', true);
      window.setTimeout(() => location.assign('/login'), 900);
    } else showMessage(settings.setupMode
      ? 'Настройки сохранены. Нажмите «Перезапустить сервер», чтобы запустить API.'
      : 'Настройки сохранены.' + (settings.databaseRestartRequired
          ? ' Запустите приложение в обычном режиме для подключения к базе.' : ''), true);
  } catch (error) {
    showMessage(error instanceof TypeError ? 'Сервер недоступен. Изменения не подтверждены.' : error.message);
  } finally {
    save.disabled = false;
  }
});

document.getElementById('logout').addEventListener('click', async () => {
  try {
    await api('/api/auth/logout', {method: 'POST'});
    location.assign('/login');
  } catch (error) {
    showMessage(error.message);
  }
});

registerShutdownButton();

function splitNames(value) {
  return value.split(',').map(name => name.trim()).filter(Boolean);
}

document.getElementById('issue-certificate').addEventListener('click', async event => {
  const button = event.currentTarget;
  button.disabled = true;
  try {
    const result = await api('/api/admin/tls/certificate/issue', {
      method: 'POST',
      body: {
        dnsNames: splitNames(document.getElementById('tls-dns-names').value),
        ipAddresses: splitNames(document.getElementById('tls-ip-addresses').value)
      }
    });
    document.getElementById('tls-dns-names').value = result.dnsNames.join(', ');
    document.getElementById('tls-ip-addresses').value = result.ipAddresses.join(', ');
    showMessage(`${result.message} SHA-256: ${result.sha256Fingerprint}`, true);
  } catch (error) {
    showMessage(error.message);
  } finally {
    button.disabled = false;
  }
});

document.getElementById('download-certificate').addEventListener('click', async event => {
  const button = event.currentTarget;
  button.disabled = true;
  try {
    const fingerprint = await download('/api/admin/tls/certificate', 'server-tls.crt');
    showMessage(`Публичный сертификат скачан. Отпечаток SHA-256: ${fingerprint}`, true);
  } catch (error) {
    showMessage(error.message);
  } finally {
    button.disabled = false;
  }
});

document.getElementById('upload-certificate').addEventListener('click', async event => {
  const file = document.getElementById('tls-keystore').files[0];
  const password = document.getElementById('tls-keystore-password').value;
  if (!file || !password) {
    showMessage('Выберите файл .p12 и введите его пароль.');
    return;
  }
  if (!window.confirm('Заменить сохранённый сертификат содержимым выбранного .p12? Он станет активным после перезапуска сервера.')) return;
  const button = event.currentTarget;
  button.disabled = true;
  const body = new FormData();
  body.append('certificate', file);
  body.append('password', password);
  try {
    const result = await api('/api/admin/tls/certificate/upload', {method: 'POST', body});
    document.getElementById('tls-keystore').value = '';
    document.getElementById('tls-keystore-password').value = '';
    showMessage(`${result.message} SHA-256: ${result.sha256Fingerprint}`, true);
  } catch (error) {
    showMessage(error.message);
  } finally {
    button.disabled = false;
  }
});

document.getElementById('restart-server').addEventListener('click', async event => {
  if (!window.confirm('Перезапустить сервер? Текущие сессии будут завершены.')) return;
  event.currentTarget.disabled = true;
  try {
    await api('/api/admin/restart', {method: 'POST'});
    showMessage('Сервер перезапускается. Через несколько секунд войдите снова.', true);
    window.setTimeout(() => location.assign('/login'), 3500);
  } catch (error) {
    showMessage(error instanceof TypeError ? 'Сервер недоступен.' : error.message);
    event.currentTarget.disabled = false;
  }
});

try {
  render(await api('/api/admin/settings'));
  save.disabled = false;
} catch (error) {
  showMessage(error instanceof TypeError ? 'Не удалось связаться с сервером. Обновите страницу.' : error.message);
}
