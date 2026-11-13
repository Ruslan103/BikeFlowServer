let csrf;

export function showMessage(text, success = false) {
  const message = document.getElementById('message');
  message.textContent = text;
  message.className = success ? 'message success' : 'message';
  message.hidden = false;
}

export async function refreshCsrf() {
  const response = await fetch('/api/auth/csrf', {credentials: 'same-origin', cache: 'no-store'});
  if (!response.ok) {
    if (response.status === 401 && location.pathname !== '/login') location.assign('/login');
    throw new Error('Не удалось получить токен безопасности. Обновите страницу.');
  }
  csrf = await response.json();
}

export async function api(path, {method = 'GET', body} = {}) {
  const headers = {Accept: 'application/json'};
  if (method !== 'GET') {
    await refreshCsrf();
    headers[csrf.headerName] = csrf.token;
  }
  const isFormData = body instanceof FormData;
  if (body !== undefined && !isFormData) headers['Content-Type'] = 'application/json';
  const response = await fetch(path, {
    method, headers, credentials: 'same-origin', cache: 'no-store',
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body)
  });
  if (response.status === 401 && location.pathname !== '/login') {
    location.assign('/login');
    throw new Error('Войдите в кабинет повторно.');
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    if (response.status === 401) throw new Error('Неверный логин или пароль.');
    if (response.status === 403) throw new Error('Доступ запрещён. Нужна роль администратора; при истечении сессии войдите повторно.');
    throw new Error(problem.detail || 'Не удалось выполнить запрос. Попробуйте ещё раз.');
  }
  return response.status === 204 ? null : response.json();
}

export async function download(path, filename) {
  await refreshCsrf();
  const response = await fetch(path, {
    headers: {Accept: 'application/x-pem-file, application/json'},
    credentials: 'same-origin', cache: 'no-store'
  });
  if (response.status === 401) {
    location.assign('/login');
    throw new Error('Войдите в кабинет повторно.');
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || 'Не удалось скачать сертификат.');
  }
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  return response.headers.get('X-Certificate-SHA256');
}

export function registerShutdownButton() {
  const button = document.getElementById('shutdown-server');
  if (!button) return;

  button.addEventListener('click', async () => {
    if (!window.confirm('Выключить сервер? Он перестанет принимать запросы.')) return;
    button.disabled = true;
    try {
      await api('/api/admin/shutdown', {method: 'POST'});
      button.textContent = 'Сервер выключается…';
      showMessage('Сервер выключается. Чтобы запустить его снова, используйте IDEA или команду запуска JAR.', true);
    } catch (error) {
      showMessage(error instanceof TypeError ? 'Сервер недоступен.' : error.message);
      button.disabled = false;
    }
  });
}
