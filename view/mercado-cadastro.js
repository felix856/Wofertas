// â”€â”€ ConfiguraÃ§Ã£o de Base URL â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
const BASE_URL_CADASTRO = (() => {
  if (window.AppConfig?.API_URL) return window.AppConfig.API_URL;
  const stored = localStorage.getItem("wof_base_url");
  if (stored) return stored;
  return window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
    ? "http://localhost:8080"
    : "https://wofertas-production.up.railway.app";
})();

// â”€â”€ Estado global â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
let logoBase64 = null;
let latSelecionada = null;
let lonSelecionada = null;
let leafletMap = null;
let pinAtual = null;

// â”€â”€ Logo upload â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
const logoInput = document.getElementById("logoInput");
if (logoInput) {
  logoInput.addEventListener("change", () => {
    const file = logoInput.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      alert("Imagem muito grande! MÃ¡ximo permitido: 5 MB.");
      logoInput.value = "";
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => { logoBase64 = e.target.result; };
    reader.readAsDataURL(file);
  });
}

// â”€â”€ Modal do mapa â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
const modalMapa = document.getElementById("modalMapa");
const btnAbrirMapa = document.getElementById("btnAbrirMapa");
const btnCancelarMapa = document.getElementById("btnCancelarMapa");
const btnConfirmarMapa = document.getElementById("btnConfirmarMapa");
const coordsDisplay = document.getElementById("coordsDisplay");
const mapaSearchInput = document.getElementById("mapaSearchInput");

// Abre o modal e inicializa o mapa
btnAbrirMapa.addEventListener("click", () => {
  modalMapa.classList.add("open");

  // PrÃ©-preenche o campo de busca com o endereÃ§o digitado
  const enderecoDigitado = document.getElementById("endereco")?.value.trim();
  if (enderecoDigitado && mapaSearchInput) {
    mapaSearchInput.value = enderecoDigitado;
  }

  // Inicializa o mapa apenas uma vez
  if (!leafletMap) {
    inicializarMapa();
  } else {
    setTimeout(() => leafletMap.invalidateSize(), 100);
  }

  // Se jÃ¡ tem pin, mostra as coordenadas
  if (latSelecionada !== null) {
    atualizarDisplayCoordenadas(latSelecionada, lonSelecionada);
    btnConfirmarMapa.disabled = false;
  }

  // Busca automÃ¡tica se o campo de endereÃ§o estiver preenchido e nÃ£o hÃ¡ pin
  if (enderecoDigitado && latSelecionada === null) {
    setTimeout(() => buscarEnderecÐ¾NoMapa(enderecoDigitado), 500);
  }
});

// Fecha o modal sem salvar
btnCancelarMapa.addEventListener("click", () => {
  modalMapa.classList.remove("open");
});

// Confirma a localizaÃ§Ã£o selecionada
btnConfirmarMapa.addEventListener("click", () => {
  if (latSelecionada === null) return;

  document.getElementById("latitude").value = latSelecionada;
  document.getElementById("longitude").value = lonSelecionada;

  // Atualiza o botÃ£o para mostrar que foi confirmado
  btnAbrirMapa.innerHTML = `âœ… LocalizaÃ§Ã£o confirmada (${latSelecionada.toFixed(4)}, ${lonSelecionada.toFixed(4)})`;
  btnAbrirMapa.classList.add("confirmed");

  modalMapa.classList.remove("open");
});

// Fechar clicando fora do card
modalMapa.addEventListener("click", (e) => {
  if (e.target === modalMapa) {
    modalMapa.classList.remove("open");
  }
});

// â”€â”€ Inicializar Leaflet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
function inicializarMapa() {
  // Centro padrÃ£o: Brasil central (ajusta para PalhoÃ§a/SC se quiser)
  const latInicial = -27.6453;
  const lonInicial = -48.6693;

  leafletMap = L.map("leafletMap").setView([latInicial, lonInicial], 14);

  // Tiles OpenStreetMap (gratuito, sem chave)
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: 'Â© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    maxZoom: 19,
  }).addTo(leafletMap);

  // Clique no mapa para posicionar o pin
  leafletMap.on("click", (e) => {
    const { lat, lng } = e.latlng;
    posicionarPin(lat, lng);
  });
}

// â”€â”€ Posicionar pin â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
function posicionarPin(lat, lng) {
  latSelecionada = parseFloat(lat.toFixed(6));
  lonSelecionada = parseFloat(lng.toFixed(6));

  // Remove pin anterior
  if (pinAtual) {
    leafletMap.removeLayer(pinAtual);
  }

  // Ãcone personalizado vermelho/amarelo
  const icone = L.divIcon({
    className: "",
    html: `<div style="
      width:36px;height:36px;
      background:linear-gradient(135deg,#f5c84a,#e9b62f);
      border-radius:50% 50% 50% 0;
      transform:rotate(-45deg);
      border:3px solid #fff;
      box-shadow:0 4px 16px rgba(0,0,0,.35);
    "></div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -36],
  });

  pinAtual = L.marker([lat, lng], { icon: icone, draggable: true })
    .addTo(leafletMap)
    .bindPopup("ðŸ“ LocalizaÃ§Ã£o do mercado")
    .openPopup();

  // Permite arrastar o pin para ajuste fino
  pinAtual.on("dragend", (e) => {
    const pos = e.target.getLatLng();
    latSelecionada = parseFloat(pos.lat.toFixed(6));
    lonSelecionada = parseFloat(pos.lng.toFixed(6));
    atualizarDisplayCoordenadas(latSelecionada, lonSelecionada);
  });

  atualizarDisplayCoordenadas(lat, lng);
  btnConfirmarMapa.disabled = false;
}

function atualizarDisplayCoordenadas(lat, lng) {
  coordsDisplay.textContent = `ðŸ“ Lat: ${parseFloat(lat).toFixed(6)}  |  Lng: ${parseFloat(lng).toFixed(6)}`;
  coordsDisplay.classList.add("has-pin");
}

// â”€â”€ Busca de endereÃ§o (Nominatim) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
async function buscarEnderecÐ¾NoMapa(query) {
  if (!query) return;
  try {
    const encoded = encodeURIComponent(query);
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encoded}`,
      { headers: { "Accept-Language": "pt-BR" } }
    );
    const data = await res.json();
    if (data && data.length > 0) {
      const lat = parseFloat(data[0].lat);
      const lon = parseFloat(data[0].lon);
      leafletMap.setView([lat, lon], 17);
      posicionarPin(lat, lon);
    } else {
      alert("EndereÃ§o nÃ£o encontrado. Tente ser mais especÃ­fico ou clique diretamente no mapa.");
    }
  } catch (e) {
    alert("Erro ao buscar endereÃ§o. Clique diretamente no mapa para posicionar.");
  }
}

document.getElementById("btnBuscarEndereco").addEventListener("click", () => {
  buscarEnderecÐ¾NoMapa(mapaSearchInput.value.trim());
});

mapaSearchInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") buscarEnderecÐ¾NoMapa(mapaSearchInput.value.trim());
});

// â”€â”€ BotÃ£o: minha localizaÃ§Ã£o atual â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
document.getElementById("btnMinhaLocalizacao").addEventListener("click", () => {
  if (!navigator.geolocation) {
    alert("Seu navegador nÃ£o suporta geolocalizaÃ§Ã£o.");
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      leafletMap.setView([lat, lng], 17);
      posicionarPin(lat, lng);
    },
    () => {
      alert("NÃ£o foi possÃ­vel obter sua localizaÃ§Ã£o. Permita o acesso e tente novamente.");
    }
  );
});

// â”€â”€ Cadastro â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
const btnCadastrar = document.getElementById("btnCadastrar");
if (btnCadastrar) {
  btnCadastrar.addEventListener("click", async () => {

    const nome     = document.getElementById("nome")?.value.trim() || "";
    const cnpj     = (document.getElementById("cnpj")?.value.trim() || "").replace(/\D/g, "");
    const endereco = document.getElementById("endereco")?.value.trim() || "";
    const email    = document.getElementById("email")?.value.trim() || "";
    const senha    = document.getElementById("senha")?.value.trim() || "";
    const telefone = (document.getElementById("telefone")?.value.trim() || "").replace(/\D/g, "");
    const lat      = document.getElementById("latitude").value;
    const lon      = document.getElementById("longitude").value;

    if (!nome || !cnpj || !endereco || !email || !senha || !telefone) {
      alert("Preencha todos os campos obrigatÃ³rios!");
      return;
    }

    if (!lat || !lon) {
      alert("Por favor, selecione a localizaÃ§Ã£o do seu mercado no mapa antes de cadastrar.\nClique no botÃ£o 'Selecionar localizaÃ§Ã£o no mapa'.");
      return;
    }

    const mercado = {
      nome,
      cnpj,
      endereco,
      email,
      senha,
      telefone,
      imagemLogo: logoBase64 || "",
      latitude:  parseFloat(lat),
      longitude: parseFloat(lon),
    };

    btnCadastrar.disabled = true;
    btnCadastrar.textContent = "Cadastrando...";

    try {
      const response = await fetch(`${BASE_URL_CADASTRO}/mercados`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(mercado),
      });

      if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        alert("Erro ao cadastrar: " + (err.message || response.status));
        return;
      }

      alert("Mercado cadastrado com sucesso! âœ…\nFaÃ§a login para acessar o painel.");
      window.location.href = "login.html";

    } catch (error) {
      console.error(error);
      alert("Falha ao conectar com o servidor.");
    } finally {
      btnCadastrar.disabled = false;
      btnCadastrar.textContent = "Cadastrar mercado";
    }
  });
}
