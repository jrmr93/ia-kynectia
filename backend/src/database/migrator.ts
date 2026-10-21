import fs from 'fs';
import path from 'path';
import { pool } from './pool';

const MIGRATIONS_DIR = path.resolve(__dirname, '../../migrations');

const ensureMigrationsTable = async () => {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS _migrations (
      id SERIAL PRIMARY KEY,
      name VARCHAR(255) UNIQUE NOT NULL,
      executed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );
  `);
};

export const runMigrationsUp = async () => {
  console.log('🔄 Verificando y ejecutando migraciones pendientes...');
  await ensureMigrationsTable();

  const files = fs
    .readdirSync(MIGRATIONS_DIR)
    .filter((file) => file.endsWith('.sql') && !file.endsWith('_down.sql'))
    .sort();

  const { rows: executedRows } = await pool.query<{ name: string }>(
    'SELECT name FROM _migrations'
  );
  const executedSet = new Set(executedRows.map((r) => r.name));

  let appliedCount = 0;
  for (const file of files) {
    if (!executedSet.has(file)) {
      console.log(`➡️  Aplicando migración: ${file}`);
      const filePath = path.join(MIGRATIONS_DIR, file);
      const sql = fs.readFileSync(filePath, 'utf-8');

      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        await client.query(sql);
        await client.query('INSERT INTO _migrations (name) VALUES ($1)', [file]);
        await client.query('COMMIT');
        appliedCount++;
        console.log(`✅ Migración completada: ${file}`);
      } catch (error) {
        await client.query('ROLLBACK');
        console.error(`❌ Error en la migración ${file}:`, error);
        throw error;
      } finally {
        client.release();
      }
    }
  }

  if (appliedCount === 0) {
    console.log('✨ La base de datos ya está al día. No hay migraciones pendientes.');
  } else {
    console.log(`🎉 Se aplicaron ${appliedCount} migraciones con éxito.`);
  }
};

export const runMigrationsDown = async () => {
  console.log('⚠️ Revirtiendo última migración...');
  await ensureMigrationsTable();

  const { rows } = await pool.query<{ id: number; name: string }>(
    'SELECT id, name FROM _migrations ORDER BY id DESC LIMIT 1'
  );

  if (rows.length === 0) {
    console.log('ℹ️ No hay migraciones registradas para revertir.');
    return;
  }

  const lastMigration = rows[0].name;
  const downFile = lastMigration.replace('.sql', '_down.sql');
  const downFilePath = path.join(MIGRATIONS_DIR, downFile);

  if (!fs.existsSync(downFilePath)) {
    throw new Error(`Archivo de reversión no encontrado: ${downFile}`);
  }

  console.log(`⬅️ Revirtiendo: ${lastMigration} utilizando ${downFile}`);
  const sql = fs.readFileSync(downFilePath, 'utf-8');

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await client.query(sql);
    await client.query('DELETE FROM _migrations WHERE id = $1', [rows[0].id]);
    await client.query('COMMIT');
    console.log(`✅ Reversión completada con éxito.`);
  } catch (error) {
    await client.query('ROLLBACK');
    console.error(`❌ Error al revertir la migración:`, error);
    throw error;
  } finally {
    client.release();
  }
};

if (require.main === module) {
  const action = process.argv[2] || 'up';
  const runner = action === 'down' ? runMigrationsDown : runMigrationsUp;
  runner()
    .then(async () => {
      await pool.end();
      process.exit(0);
    })
    .catch(async (err) => {
      console.error(err);
      await pool.end();
      process.exit(1);
    });
}
