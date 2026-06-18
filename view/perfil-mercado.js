const BASE_URL = window.AppConfig?.API_URL || "https://wofertas-production.up.railway.app";
const token    = localStorage.getItem("token");
const mercId   = localStorage.getItem("id");

if (!token || !mercId) { window.location.href = "login.html"; }

let mercadoAtual = null;

// ── Estado do mapa de perfil ─────────────────────────────────────
let leafletMapPerfil = null;
let pinAtualPerfil   = null;
let latPerfilTemp    = null;
let lonPerfilTemp    = null;

// ── Carrega o perfil da API ──────────────────────────────────────
async function carregarPerfil() {
    try {
        const res  = await fetch(`${BASE_URL}/mercados/${mercId}`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const m = await res.json();
        mercadoAtual = m;

        document.getElementById("nome").value      = m.nome     || "";
        document.getElementById("telefone").value  = m.telefone || "";
        document.getElementById("endereco").value  = m.endereco || "";
        document.getElementById("email").value     = m.email    || "";
        document.getElementById("cnpj").value      = m.cnpj     || "";
        if(document.getElementById("senha")) document.getElementById("senha").value = "";

        document.getElementById("nomeDisplay").textContent  = m.nome || "Mercado";
        document.getElementById("emailDisplay").textContent = m.email || "—";
        document.getElementById("avatarLetter").textContent = (m.nome || "S")[0].toUpperCase();

        if (m.imagemLogo && m.imagemLogo.trim() !== "") {
            const img = document.getElementById("avatarImg");
            img.src = m.imagemLogo.startsWith("data:") ? m.imagemLogo : `data:image/jpeg;base64,${m.imagemLogo}`;
            img.style.display  = "block";
            document.getElementById("avatarLetter").style.display = "none";
        }

        // Exibe status da localização
        atualizarStatusLocalizacao(m.latitude, m.longitude);

        // Pré-carrega as coordenadas atuais
        if (m.latitude != null && m.longitude != null) {
            latPerfilTemp = m.latitude;
            lonPerfilTemp = m.longitude;
            document.getElementById("latitudePerfil").value  = m.latitude;
            document.getElementById("longitudePerfil").value = m.longitude;

            // Atualiza botão para refletir que já tem localização
            const btn = document.getElementById("btnAbrirMapaPerfil");
            btn.classList.add("confirmed");
            btn.innerHTML = `✅ Localização salva — clique para ajustar`;
        }

    } catch (e) {
        toast("Erro ao carregar perfil.", "error");
    }
}

function atualizarStatusLocalizacao(lat, lon) {
    const el = document.getElementById("locStatus");
    if (!el) return;
    if (lat != null && lon != null) {
        el.textContent = `✅ Localização salva: (${parseFloat(lat).toFixed(4)}, ${parseFloat(lon).toFixed(4)})`;
        el.className = "loc-status ok";
    } else {
        el.textContent = "⚠️ Sem localização — seus clientes não encontrarão você no mapa!";
        el.className = "loc-status";
        el.style.color = "#fca5a5";
    }
}

// ── Logo upload ──────────────────────────────────────────────────
function handleLogoUpload(input) {
    const file = input.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) { toast("Imagem muito grande (máx 5MB)", "error"); return; }
    const reader = new FileReader();
    reader.onload = e => {
        const b64 = e.target.result;
        document.getElementById("logoB64").value = b64;
        const img = document.getElementById("avatarImg");
        img.src = b64;
        img.style.display = "block";
        document.getElementById("avatarLetter").style.display = "none";
        toast("Logo selecionada — clique em Salvar para confirmar", "info");
    };
    reader.readAsDataURL(file);
}

// ── Salvar perfil ────────────────────────────────────────────────
async function salvarPerfil() {
    const nome     = document.getElementById("nome").value.trim();
    const telefone = document.getElementById("telefone").value.trim();
    const endereco = document.getElementById("endereco").value.trim();
    const b64      = document.getElementById("logoB64").value;
    const latInput = document.getElementById("latitudePerfil").value;
    const lonInput = document.getElementById("longitudePerfil").value;

    const senhaInput = document.getElementById("senha");
    const senhaRaw   = senhaInput ? senhaInput.value.trim() : "";
    const senhaFinal = senhaRaw === "" ? null : senhaRaw;

    if (!nome)     { toast("Nome da loja obrigatório", "error"); return; }
    if (!endereco) { toast("Endereço obrigatório", "error");     return; }
    if (senhaFinal !== null && senhaFinal.length < 6) {
        toast("A nova senha deve ter no mínimo 6 caracteres", "error");
        return;
    }

    // Coordenadas: prioriza o que o usuário selecionou no mapa, senão mantém o que veio da API
    const latFinal = latInput ? parseFloat(latInput) : (mercadoAtual?.latitude || null);
    const lonFinal = lonInput ? parseFloat(lonInput) : (mercadoAtual?.longitude || null);

    const btn = document.getElementById("btnSalvar");
    btn.disabled = true;
    btn.innerHTML = "Salvando...";

    try {
        const res = await fetch(`${BASE_URL}/mercados/${mercId}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({
                nome,
                telefone,
                endereco,
                email:      mercadoAtual?.email || "",
                cnpj:       mercadoAtual?.cnpj  || "",
                senha:      senhaFinal,
                imagemLogo: b64 || mercadoAtual?.imagemLogo || "",
                latitude:   latFinal,
                longitude:  lonFinal,
            })
        });

        if (res.ok) {
            toast("Perfil atualizado com sucesso! ✅");
            if(senhaInput) senhaInput.value = "";
            await carregarPerfil();
        } else {
            const errData = await res.json().catch(() => ({}));
            toast(errData.message || "Erro ao salvar.", "error");
        }
    } catch (e) {
        toast("Falha ao conectar.", "error");
    } finally {
        btn.disabled = false;
        btn.innerHTML = "Salvar alterações";
    }
}

// ── Modal do mapa (perfil) ───────────────────────────────────────
const modalMapaPerfil      = document.getElementById("modalMapaPerfil");
const btnAbrirMapaPerfil   = document.getElementById("btnAbrirMapaPerfil");
const btnCancelarMapaPerfil= document.getElementById("btnCancelarMapaPerfil");
const btnConfirmarMapaPerfil= document.getElementById("btnConfirmarMapaPerfil");
const coordsDisplayPerfil  = document.getElementById("coordsDisplayPerfil");
const mapaSearchInputPerfil= document.getElementById("mapaSearchInputPerfil");

btnAbrirMapaPerfil.addEventListener("click", () => {
    modalMapaPerfil.classList.add("open");

    // Pré-preenche a busca com o endereço atual
    const enderecoAtual = document.getElementById("endereco")?.value.trim();
    if (enderecoAtual && mapaSearchInputPerfil && !mapaSearchInputPerfil.value) {
        mapaSearchInputPerfil.value = enderecoAtual;
    }

    if (!leafletMapPerfil) {
        inicializarMapaPerfil();
    } else {
        setTimeout(() => leafletMapPerfil.invalidateSize(), 100);
    }

    // Se já tem coordenadas, posiciona o pin
    if (latPerfilTemp !== null && pinAtualPerfil === null) {
        setTimeout(() => {
            leafletMapPerfil.setView([latPerfilTemp, lonPerfilTemp], 16);
            posicionarPinPerfil(latPerfilTemp, lonPerfilTemp);
        }, 300);
    } else if (latPerfilTemp !== null) {
        atualizarDisplayCoordenadas(latPerfilTemp, lonPerfilTemp);
        btnConfirmarMapaPerfil.disabled = false;
    } else if (enderecoAtual) {
        // Busca automática pelo endereço se não tiver coordenadas
        setTimeout(() => buscarEnderecoPerfil(enderecoAtual), 500);
    }
});

btnCancelarMapaPerfil.addEventListener("click", () => {
    modalMapaPerfil.classList.remove("open");
});

btnConfirmarMapaPerfil.addEventListener("click", () => {
    if (latPerfilTemp === null) return;

    document.getElementById("latitudePerfil").value  = latPerfilTemp;
    document.getElementById("longitudePerfil").value = lonPerfilTemp;

    btnAbrirMapaPerfil.innerHTML = `✅ Localização selecionada (${latPerfilTemp.toFixed(4)}, ${lonPerfilTemp.toFixed(4)})`;
    btnAbrirMapaPerfil.classList.add("confirmed");

    const statusEl = document.getElementById("locStatus");
    if (statusEl) {
        statusEl.textContent = `📍 Nova localização: (${latPerfilTemp.toFixed(4)}, ${lonPerfilTemp.toFixed(4)}) — salve para confirmar`;
        statusEl.className = "loc-status ok";
    }

    modalMapaPerfil.classList.remove("open");
});

modalMapaPerfil.addEventListener("click", (e) => {
    if (e.target === modalMapaPerfil) modalMapaPerfil.classList.remove("open");
});

function inicializarMapaPerfil() {
    const latInicial = latPerfilTemp ?? -27.6453;
    const lonInicial = lonPerfilTemp ?? -48.6693;

    leafletMapPerfil = L.map("leafletMapPerfil").setView([latInicial, lonInicial], latPerfilTemp ? 16 : 14);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 19,
    }).addTo(leafletMapPerfil);

    leafletMapPerfil.on("click", (e) => {
        const { lat, lng } = e.latlng;
        posicionarPinPerfil(lat, lng);
    });

    // Se já tem coordenadas salvas, coloca o pin imediatamente
    if (latPerfilTemp !== null) {
        posicionarPinPerfil(latPerfilTemp, lonPerfilTemp);
    }
}

function posicionarPinPerfil(lat, lng) {
    latPerfilTemp = parseFloat(parseFloat(lat).toFixed(6));
    lonPerfilTemp = parseFloat(parseFloat(lng).toFixed(6));

    if (pinAtualPerfil) leafletMapPerfil.removeLayer(pinAtualPerfil);

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

    pinAtualPerfil = L.marker([lat, lng], { icon: icone, draggable: true })
        .addTo(leafletMapPerfil)
        .bindPopup("📍 Localização do mercado")
        .openPopup();

    pinAtualPerfil.on("dragend", (e) => {
        const pos = e.target.getLatLng();
        latPerfilTemp = parseFloat(pos.lat.toFixed(6));
        lonPerfilTemp = parseFloat(pos.lng.toFixed(6));
        atualizarDisplayCoordenadas(latPerfilTemp, lonPerfilTemp);
    });

    atualizarDisplayCoordenadas(lat, lng);
    btnConfirmarMapaPerfil.disabled = false;
}

function atualizarDisplayCoordenadas(lat, lng) {
    coordsDisplayPerfil.textContent = `📍 Lat: ${parseFloat(lat).toFixed(6)}  |  Lng: ${parseFloat(lng).toFixed(6)}`;
    coordsDisplayPerfil.classList.add("has-pin");
}

async function buscarEnderecoPerfil(query) {
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
            leafletMapPerfil.setView([lat, lon], 17);
            posicionarPinPerfil(lat, lon);
        } else {
            alert("Endereço não encontrado. Clique no mapa para marcar a localização.");
        }
    } catch (e) {
        alert("Erro ao buscar endereço. Clique diretamente no mapa.");
    }
}

document.getElementById("btnBuscarEnderecoPerfil").addEventListener("click", () => {
    buscarEnderecoPerfil(mapaSearchInputPerfil.value.trim());
});

mapaSearchInputPerfil.addEventListener("keydown", (e) => {
    if (e.key === "Enter") buscarEnderecoPerfil(mapaSearchInputPerfil.value.trim());
});

document.getElementById("btnMinhaLocalizacaoPerfil").addEventListener("click", () => {
    if (!navigator.geolocation) { alert("Seu navegador não suporta geolocalização."); return; }
    navigator.geolocation.getCurrentPosition(
        (pos) => {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            leafletMapPerfil.setView([lat, lng], 17);
            posicionarPinPerfil(lat, lng);
        },
        () => { alert("Não foi possível obter sua localização. Permita o acesso e tente novamente."); }
    );
});

// ── Toast ────────────────────────────────────────────────────────
function toast(msg, type = "success") {
    const container = document.getElementById("toastContainer");
    if(!container) return;
    const el = document.createElement("div");
    el.className = `toast toast-${type}`;
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(() => el.remove(), 3200);
}

carregarPerfil();
