import express from 'express';
import bodyParser from 'body-parser';
import cors from 'cors';
import sequelize from './config/database.js';
import * as offerRoutesModule from './routes/offerRoutes.js'; // ALTERAÇÃO: Importa o módulo inteiro
import path from 'path';
import 'dotenv/config'; 

// No modo ES Modules, __dirname não existe, então precisamos defini-lo
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const app = express();
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Garante que o UPLOAD_DIR seja resolvido corretamente
const uploadDir = process.env.UPLOAD_DIR || path.join(__dirname, 'uploads');
app.use('/uploads', express.static(uploadDir));

// ALTERAÇÃO: Acessa o objeto exportado (que geralmente é o 'default' ou o módulo inteiro)
const offerRoutes = offerRoutesModule.default || offerRoutesModule; 
app.use('/api', offerRoutes);

app.get('/', (req, res) => res.json({ app: 'wofertas-api', status: 'ok' }));

const PORT = process.env.PORT || 3000;
(async () => {
  try {
    // Tenta autenticar a conexão com o banco de dados (SQLite)
    await sequelize.authenticate();
    console.log('Database connection established successfully.');

    // Sincroniza os modelos com o banco de dados
    await sequelize.sync();
    console.log('Database synchronized successfully.');

    // Inicia o servidor HTTP
    app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
  } catch (err) {
    // Captura e exibe o erro completo de inicialização (seja DB ou Firebase)
    console.error('Startup error: The application failed to start due to a configuration or database error.', err);
  }
})();
