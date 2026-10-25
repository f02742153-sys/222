/* HEADSHOTERZ — Frontend (conecta na API Flask/SQLite) */
let filtroAtual = 'todos';
const $ = id => document.getElementById(id);
const escapeHtml = s => String(s).replace(/[&<>"']/g, m => (
  {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]
));

function showScreen(id){
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  $(id).classList.add('active');
  window.scrollTo({top:0,behavior:'smooth'});
}

/* ---------- CADASTRO ---------- */
$('form-cadastro').addEventListener('submit', async e => {
  e.preventDefault();
  const nome   = $('nome').value.trim();
  const idJogo = $('idJogo').value.trim();
  const funcao = $('funcao').value;
  if (!nome || !idJogo || !funcao) return;

  const btn = e.target.querySelector('button[type=submit]');
  btn.disabled = true;
  btn.textContent = 'ENVIANDO...';

  try {
    const res = await fetch('/api/pedidos', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({nome, idJogo, funcao})
    });
    if (!res.ok) throw new Error();
    $('form-cadastro').reset();
    showScreen('screen-confirmacao');
  } catch {
    alert('Erro ao enviar. Tente novamente.');
  } finally {
    btn.disabled = false;
    btn.textContent = 'ENVIAR PEDIDO';
  }
});

$('btn-voltar').addEventListener('click', () => showScreen('screen-cadastro'));

/* ---------- LOGIN ---------- */
$('link-admin').addEventListener('click', e => {
  e.preventDefault();
  $('login-error').textContent = '';
  $('senha').value = '';
  showScreen('screen-login');
});

$('btn-cancelar-login').addEventListener('click', () => showScreen('screen-cadastro'));

$('btn-login').addEventListener('click', async () => {
  const card = document.querySelector('.login-card');
  try {
    const res = await fetch('/api/login', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({senha: $('senha').value})
    });
    if (!res.ok) throw new Error();
    $('senha').value = '';
    $('login-error').textContent = '';
    await renderPainel();
    showScreen('screen-painel');
  } catch {
    $('login-error').textContent = '❌ Senha incorreta!';
    card.classList.remove('shake');
    void card.offsetWidth;
    card.classList.add('shake');
  }
});

$('senha').addEventListener('keydown', e => { if (e.key === 'Enter') $('btn-login').click(); });

/* ---------- PAINEL ---------- */
async function renderPainel(){
  const res = await fetch('/api/pedidos');
  const lista = await res.json();

  $('stat-total').textContent     = lista.length;
  $('stat-pendentes').textContent = lista.filter(p => p.status === 'pendente').length;
  $('stat-aceitos').textContent   = lista.filter(p => p.status === 'aceito').length;
  $('stat-recusados').textContent = lista.filter(p => p.status === 'recusado').length;

  const filtrados = filtroAtual === 'todos'
    ? lista
    : lista.filter(p => p.status === filtroAtual);

  const container = $('lista-pedidos');

  if (filtrados.length === 0){
    container.innerHTML = `<div class="empty">Nenhum pedido ${filtroAtual === 'todos' ? '' : filtroAtual + ' '}por aqui.</div>`;
    return;
  }

  container.innerHTML = filtrados.map(p => `
    <div class="pedido ${p.status}">
      <span class="badge badge-${p.status}">${p.status}</span>
      <div class="pedido-info">
        <h3>${escapeHtml(p.nome)}</h3>
        <p><strong>ID:</strong> ${escapeHtml(p.id_jogo)}</p>
        <p><strong>Função:</strong> ${escapeHtml(p.funcao)}</p>
      </div>
      <div class="pedido-actions">
        <button class="btn-accept" data-id="${p.id}" data-action="aceito"
          ${p.status !== 'pendente' ? 'disabled' : ''}>✓ Aceitar</button>
        <button class="btn-reject" data-id="${p.id}" data-action="recusado"
          ${p.status !== 'pendente' ? 'disabled' : ''}>✕ Recusar</button>
      </div>
    </div>
  `).join('');
}

$('lista-pedidos').addEventListener('click', async e => {
  const btn = e.target.closest('button[data-id]');
  if (!btn) return;
  const id = btn.dataset.id;
  const status = btn.dataset.action;
  await fetch(`/api/pedidos/${id}`, {
    method: 'PATCH',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({status})
  });
  await renderPainel();
});

document.querySelectorAll('.filtro').forEach(btn => {
  btn.addEventListener('click', async () => {
    document.querySelectorAll('.filtro').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    filtroAtual = btn.dataset.filtro;
    await renderPainel();
  });
});

$('btn-sair').addEventListener('click', () => showScreen('screen-cadastro'));