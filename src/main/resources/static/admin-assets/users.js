import {api, registerShutdownButton, showMessage} from './common.js';

const pageSize = 20;
const rows = document.getElementById('user-rows');
const roleNames = new Map();
let page = 0;
let pageCount = 0;
let selectedUser = null;
let currentUserId = null;

function selectedRoles(container) {
  return [...container.querySelectorAll('input[name="roleCodes"]:checked')].map(input => input.value);
}

function renderRoleOptions(container, selected = []) {
  container.replaceChildren();
  for (const [code, name] of roleNames) {
    const label = document.createElement('label');
    label.className = 'role-option';
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.name = 'roleCodes';
    checkbox.value = code;
    checkbox.checked = selected.includes(code);
    const text = document.createElement('span');
    text.textContent = name;
    label.append(checkbox, text);
    container.append(label);
  }
}

function textCell(value) {
  const cell = document.createElement('td');
  cell.textContent = value || '—';
  return cell;
}

function renderUsers(result) {
  rows.replaceChildren();
  for (const user of result.content) {
    const row = document.createElement('tr');
    if (selectedUser?.id === user.id) row.classList.add('selected-row');
    row.append(
      textCell(user.login),
      textCell(user.displayName),
      textCell(user.roleCodes.map(code => roleNames.get(code) || code).join(', ')),
      textCell(user.enabled ? 'Активен' : 'Отключён'),
      textCell(user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—')
    );
    const actions = document.createElement('td');
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'button subtle';
    button.textContent = 'Управлять';
    button.addEventListener('click', () => openEditor(user));
    actions.append(button);
    row.append(actions);
    rows.append(row);
  }

  document.getElementById('list-empty').hidden = result.content.length !== 0;
  document.getElementById('list-summary').textContent = `Найдено: ${result.totalElements}`;
  pageCount = result.totalPages;
  document.getElementById('page-label').textContent = pageCount
    ? `Страница ${result.page + 1} из ${pageCount}` : 'Страница 0 из 0';
  document.getElementById('previous-page').disabled = page <= 0;
  document.getElementById('next-page').disabled = page + 1 >= pageCount;
}

async function loadUsers() {
  const params = new URLSearchParams({page: String(page), size: String(pageSize)});
  const search = document.getElementById('search').value.trim();
  const enabled = document.getElementById('enabled-filter').value;
  if (search) params.set('search', search);
  if (enabled) params.set('enabled', enabled);
  const result = await api(`/api/users?${params}`);
  renderUsers(result);
}

function openEditor(user) {
  selectedUser = user;
  document.getElementById('edit-panel').hidden = false;
  document.getElementById('edit-user-heading').textContent = `Пользователь: ${user.login}`;
  document.getElementById('edit-login').value = user.login;
  document.getElementById('edit-name').value = user.displayName || '';
  renderRoleOptions(document.getElementById('edit-roles'), user.roleCodes);
  const toggle = document.getElementById('toggle-enabled');
  toggle.textContent = user.enabled ? 'Отключить пользователя' : 'Включить пользователя';
  document.getElementById('enabled-label').textContent = user.enabled
    ? 'Учётная запись активна' : 'Учётная запись отключена';
  document.getElementById('reset-password').value = '';
  loadUsers().catch(error => showMessage(error.message));
  document.getElementById('edit-panel').scrollIntoView({behavior: 'smooth', block: 'start'});
}

function formData(form) {
  return Object.fromEntries(new FormData(form));
}

document.getElementById('filters').addEventListener('submit', async event => {
  event.preventDefault();
  page = 0;
  try {
    await loadUsers();
  } catch (error) {
    showMessage(error.message);
  }
});

document.getElementById('previous-page').addEventListener('click', async () => {
  page = Math.max(0, page - 1);
  try { await loadUsers(); } catch (error) { showMessage(error.message); }
});

document.getElementById('next-page').addEventListener('click', async () => {
  if (page + 1 >= pageCount) return;
  page += 1;
  try { await loadUsers(); } catch (error) { showMessage(error.message); }
});

document.getElementById('show-create').addEventListener('click', () => {
  document.getElementById('create-panel').hidden = false;
  document.getElementById('create-panel').scrollIntoView({behavior: 'smooth', block: 'start'});
});

document.getElementById('cancel-create').addEventListener('click', () => {
  document.getElementById('create-panel').hidden = true;
  document.getElementById('create-form').reset();
});

document.getElementById('create-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const roles = selectedRoles(document.getElementById('create-roles'));
  if (roles.length === 0) {
    showMessage('Выберите хотя бы одну роль.');
    return;
  }
  const values = formData(form);
  const submit = form.querySelector('[type="submit"]');
  submit.disabled = true;
  try {
    const created = await api('/api/users', {method: 'POST', body: {
      login: values.login.trim(),
      displayName: values.displayName.trim() || null,
      password: values.password,
      roleCodes: roles,
      enabled: document.getElementById('create-enabled').checked
    }});
    form.reset();
    document.getElementById('create-enabled').checked = true;
    renderRoleOptions(document.getElementById('create-roles'), roleNames.size ? [roleNames.keys().next().value] : []);
    document.getElementById('create-panel').hidden = true;
    page = 0;
    await loadUsers();
    showMessage(`Пользователь ${created.login} создан.`, true);
  } catch (error) {
    showMessage(error.message);
  } finally {
    submit.disabled = false;
  }
});

document.getElementById('edit-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (!selectedUser) return;
  const values = formData(event.currentTarget);
  try {
    selectedUser = await api(`/api/users/${selectedUser.id}`, {method: 'PUT', body: {
      login: values.login.trim(),
      displayName: values.displayName.trim() || null
    }});
    await loadUsers();
    openEditor(selectedUser);
    showMessage('Профиль пользователя сохранён.', true);
  } catch (error) {
    showMessage(error.message);
  }
});

document.getElementById('save-roles').addEventListener('click', async event => {
  if (!selectedUser) return;
  const button = event.currentTarget;
  const roles = selectedRoles(document.getElementById('edit-roles'));
  if (roles.length === 0) {
    showMessage('У пользователя должна быть хотя бы одна роль.');
    return;
  }
  const isCurrentUser = selectedUser.id === currentUserId;
  if (isCurrentUser && !window.confirm('Изменение ролей завершит ваши активные сеансы. Продолжить?')) return;
  button.disabled = true;
  try {
    selectedUser = await api(`/api/users/${selectedUser.id}/roles`, {method: 'PUT', body: {roleCodes: roles}});
    if (isCurrentUser) {
      showMessage('Ваши роли сохранены. Выполняется выход из системы.', true);
      window.setTimeout(() => location.assign('/login'), 1200);
      return;
    }
    await loadUsers();
    openEditor(selectedUser);
    showMessage('Роли пользователя сохранены.', true);
  } catch (error) {
    showMessage(error.message);
  } finally {
    button.disabled = false;
  }
});

document.getElementById('toggle-enabled').addEventListener('click', async event => {
  if (!selectedUser) return;
  const button = event.currentTarget;
  const enabled = !selectedUser.enabled;
  const isCurrentUser = selectedUser.id === currentUserId;
  if (!window.confirm(`${enabled ? 'Включить' : 'Отключить'} пользователя ${selectedUser.login}?`)) return;
  button.disabled = true;
  try {
    selectedUser = await api(`/api/users/${selectedUser.id}/enabled`, {method: 'PUT', body: {enabled}});
    if (!enabled && isCurrentUser) {
      showMessage('Ваша учётная запись отключена. Выполняется выход из системы.', true);
      window.setTimeout(() => location.assign('/login'), 1200);
      return;
    }
    await loadUsers();
    openEditor(selectedUser);
    showMessage(`Пользователь ${enabled ? 'включён' : 'отключён'}.`, true);
  } catch (error) {
    showMessage(error.message);
  } finally {
    button.disabled = false;
  }
});

document.getElementById('reset-password-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (!selectedUser) return;
  const form = event.currentTarget;
  const values = formData(form);
  const isCurrentUser = selectedUser.id === currentUserId;
  if (!window.confirm(`Сбросить пароль пользователя ${selectedUser.login}? Его активные сеансы завершатся.`)) return;
  try {
    await api(`/api/users/${selectedUser.id}/password`, {method: 'PUT', body: {newPassword: values.newPassword}});
    form.reset();
    showMessage('Пароль сброшен, активные сеансы пользователя завершены.', true);
    if (isCurrentUser) window.setTimeout(() => location.assign('/login'), 1200);
  } catch (error) {
    showMessage(error.message);
  }
});

document.getElementById('logout-user-sessions').addEventListener('click', async () => {
  if (!selectedUser) return;
  const isCurrentUser = selectedUser.id === currentUserId;
  if (!window.confirm(`Завершить все активные сеансы пользователя ${selectedUser.login}?`)) return;
  try {
    await api(`/api/users/${selectedUser.id}/logout-all`, {method: 'POST'});
    showMessage('Все активные сеансы пользователя завершены.', true);
    if (isCurrentUser) window.setTimeout(() => location.assign('/login'), 1200);
  } catch (error) {
    showMessage(error.message);
  }
});

document.getElementById('close-edit').addEventListener('click', () => {
  document.getElementById('edit-panel').hidden = true;
  selectedUser = null;
  loadUsers().catch(error => showMessage(error.message));
});

document.getElementById('own-password-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const values = formData(form);
  try {
    await api('/api/users/me/password', {method: 'PUT', body: values});
    form.reset();
    showMessage('Ваш пароль изменён. Выполняется выход из системы.', true);
    window.setTimeout(() => location.assign('/login'), 1200);
  } catch (error) {
    showMessage(error.message);
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

document.getElementById('restart-server').addEventListener('click', async event => {
  if (!window.confirm('Перезапустить сервер? Текущие сессии завершатся.')) return;
  const button = event.currentTarget;
  button.disabled = true;
  try {
    await api('/api/admin/restart', {method: 'POST'});
    showMessage('Сервер перезапускается. Войдите снова через несколько секунд.', true);
    window.setTimeout(() => location.assign('/login'), 3500);
  } catch (error) {
    showMessage(error.message);
    button.disabled = false;
  }
});

registerShutdownButton();

async function initialize() {
  try {
    const [roles, me] = await Promise.all([api('/api/roles'), api('/api/users/me')]);
    currentUserId = me.id;
    roles.forEach(role => roleNames.set(role.code,
      role.code === 'ADMIN' ? 'Администратор' : role.name));
    renderRoleOptions(document.getElementById('create-roles'), roles.length ? [roles[0].code] : []);
    renderRoleOptions(document.getElementById('edit-roles'));
    await loadUsers();
  } catch (error) {
    showMessage(error.message);
  }
}

initialize();
