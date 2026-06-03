package com.example.wofertas.network

/**
 * Configuração de ambientes para a API.
 * Use para alternar entre emulador, dispositivo físico e produção.
 */
enum class ApiEnvironment(val baseUrl: String, val description: String) {
    /**
     * Emulador Android Studio rodando no PC.
     * Use: http://10.0.2.2:8080/
     */
    EMULATOR("http://10.0.2.2:8080/", "Android Emulator"),

    /**
     * Dispositivo físico na MESMA rede WiFi (trocar IP).
     * Exemplo: http://192.168.1.100:8080/
     * Para encontrar o IP da sua máquina:
     *   - No Windows: ipconfig (procure por "IPv4 Address")
     *   - No Mac/Linux: ifconfig ou ip addr
     */
    PHYSICAL_DEVICE("http://192.168.3.177:8080/", "Physical Device (WiFi)"),

    /**
     * Servidor em produção (mudar para seu domínio real).
     */
    PRODUCTION("https://wofertas-api.com/", "Production Server"),

    /**
     * Localhost (apenas para testes no desktop).
     */
    LOCALHOST("http://localhost:8080/", "Localhost"),

    /**
     * Staging/Teste em servidor remoto.
     */
    STAGING("https://staging-wofertas-api.com/", "Staging Server");

    companion object {
        /**
         * Ambiente padrão (alterar conforme necessário).
         */
        fun default(): ApiEnvironment = EMULATOR

        /**
         * Obter ambiente pela URL.
         */
        fun fromUrl(url: String): ApiEnvironment? {
            return entries.find { it.baseUrl.equals(url, ignoreCase = true) }
        }
    }
}
