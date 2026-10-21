import { createApp } from './app';
import { env } from './config/env';
import { pool } from './database/pool';

const startServer = async () => {
  try {
    // Probar conexión a PostgreSQL
    const { rows } = await pool.query('SELECT NOW() as current_time, current_database() as db_name;');
    console.log(`🔌 Conectado exitosamente a PostgreSQL [BD: ${rows[0].db_name}] a las ${rows[0].current_time}`);

    const app = createApp();

    const server = app.listen(env.PORT, () => {
      console.log(`🚀 Kynectia Admin API ejecutándose en http://localhost:${env.PORT}`);
      console.log(`📌 Healthcheck: http://localhost:${env.PORT}/api/health`);
      console.log(`🛡️ Entorno: ${env.NODE_ENV}`);
    });

    // Cierre ordenado
    const handleShutdown = async (signal: string) => {
      console.log(`\n🛑 Señal ${signal} recibida. Cerrando servidor y liberando conexiones...`);
      server.close(async () => {
        await pool.end();
        console.log('✅ Conexiones liberadas. Servidor detenido.');
        process.exit(0);
      });
    };

    process.on('SIGTERM', () => handleShutdown('SIGTERM'));
    process.on('SIGINT', () => handleShutdown('SIGINT'));
  } catch (error) {
    console.error('❌ Error fatal al iniciar el servidor:', error);
    process.exit(1);
  }
};

startServer();
