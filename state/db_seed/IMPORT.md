Instruções para importar os dados de seed para o MongoDB local

Arquivos:
- seed_mercados.json  → coleção `mercados`
- seed_ofertas.json   → coleção `ofertas`

Ambiente de exemplo: MongoDB rodando em `mongodb://localhost:27017/wofertas`

Import usando `mongoimport` (JSON array):

Windows (PowerShell):

```
# Importa mercados
mongoimport --uri "mongodb://localhost:27017/wofertas" --collection mercados --drop --file "state\\db_seed\\seed_mercados.json" --jsonArray

# Importa ofertas
mongoimport --uri "mongodb://localhost:27017/wofertas" --collection ofertas --drop --file "state\\db_seed\\seed_ofertas.json" --jsonArray
```

Linux/macOS:

```
# supondo que esteja na raiz do projeto
mongoimport --uri "mongodb://localhost:27017/wofertas" --collection mercados --drop --file state/db_seed/seed_mercados.json --jsonArray
mongoimport --uri "mongodb://localhost:27017/wofertas" --collection ofertas --drop --file state/db_seed/seed_ofertas.json --jsonArray
```

Notas:
- `--drop` apaga a coleção antes de importar; remova essa flag se quiser apenas acrescentar.
- Os `_id` nos arquivos usam formato Extended JSON (MongoDB) — `mongoimport` reconhecerá corretamente.
- Se estiver usando `mongosh`/`mongo` preferido, também é possível inserir via script JavaScript.

Exemplo rápido via `mongosh` (inserção manual):

```
use wofertas
const mercados = cat('state/db_seed/seed_mercados.json')
// No mongosh você pode usar JSON.parse e inserir, ou ajustar conforme preferir.
```
