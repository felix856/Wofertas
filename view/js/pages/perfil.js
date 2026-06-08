/**
 * Perfil Mercado page script
 * Carrega perfil do usuário/mercado autenticado
 */

async function initPerfilPage() {
  try {
    const container = document.getElementById('perfilContainer') || document.body;
    const res = await usuariosAPI.getPerfil();

    if (res.error) {
      notificationsService.error(res.error);
      return;
    }

    const usuario = res.data;
    const card = document.createElement('div');
    card.className = 'card';
    card.innerHTML = `
      <h2>${usuario.nome || 'Perfil'}</h2>
      <p><strong>Email:</strong> ${usuario.email || '-'}</p>
      <p><strong>Telefone:</strong> ${usuario.telefone || '-'}</p>
      <p><strong>Endereço:</strong> ${usuario.endereco || '-'}</p>
    `;

    container.appendChild(card);
  } catch (err) {
    console.error('Erro ao carregar perfil:', err);
    notificationsService.error('Erro ao carregar perfil');
  }
}

window.addEventListener('modulesLoaded', initPerfilPage);
if (window.usuariosAPI) initPerfilPage();
