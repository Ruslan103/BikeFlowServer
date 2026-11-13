import {api, showMessage} from './common.js';

const form = document.getElementById('login-form');
const submit = document.getElementById('submit');
form.addEventListener('submit', async event => {
  event.preventDefault();
  submit.disabled = true;
  try {
    const account = await api('/api/auth/login', {
      method: 'POST',
      body: {login: form.elements.login.value, password: form.elements.password.value}
    });
    form.elements.password.value = '';
    if (!account.roleCodes.includes('ADMIN')) {
      await api('/api/auth/logout', {method: 'POST'});
      throw new Error('Вход в кабинет доступен только администратору.');
    }
    location.assign('/admin');
  } catch (error) {
    showMessage(error instanceof TypeError ? 'Сервер недоступен. Проверьте подключение.' : error.message);
  } finally {
    submit.disabled = false;
  }
});
