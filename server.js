import express from "express";
import cors from "cors";
import tabloidesRoutes from "./routes/tabloides.js";

const app = express();
app.use(cors());
app.use(express.json());

// Rotas
app.use("/tabloides", tabloidesRoutes);

// Rota raiz (teste rápido)
app.get("/", (req, res) => {
  res.json({ message: "🚀 API Wofertas rodando!" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Servidor rodando na porta ${PORT}`));
