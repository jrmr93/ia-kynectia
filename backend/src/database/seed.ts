import bcrypt from 'bcrypt';
import { pool, withTransaction } from './pool';
import { env } from '../config/env';

export const runSeed = async () => {
  console.log('🌱 Iniciando carga de datos semilla (Seeders) en PostgreSQL...');

  await withTransaction(async (client) => {
    // 1. Roles
    const rolesData = [
      { name: 'superadmin', description: 'Acceso total y control administrativo absoluto de la plataforma' },
      { name: 'trainer', description: 'Entrenador administrativo con gestión de ejercicios, rutinas y usuarios' },
      { name: 'editor', description: 'Editor de contenidos de marketing, multimedia y anuncios' },
    ];

    const roleMap: Record<string, number> = {};
    for (const r of rolesData) {
      const res = await client.query(
        `INSERT INTO roles (name, description)
         VALUES ($1, $2)
         ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description
         RETURNING id, name;`,
        [r.name, r.description]
      );
      roleMap[res.rows[0].name] = res.rows[0].id;
    }

    // 2. Permisos
    const permissionsData = [
      { code: 'users:manage', description: 'Crear, editar, activar y desactivar usuarios', module: 'users' },
      { code: 'muscles:manage', description: 'Gestionar grupos musculares', module: 'muscles' },
      { code: 'exercises:manage', description: 'Gestionar catálogo de ejercicios', module: 'exercises' },
      { code: 'equipment:manage', description: 'Gestionar implementos deportivos', module: 'equipment' },
      { code: 'routines:manage', description: 'Crear, editar y publicar rutinas', module: 'routines' },
      { code: 'plans:manage', description: 'Gestionar planes comerciales y precios', module: 'plans' },
      { code: 'ads:manage', description: 'Gestionar anuncios y publicidad', module: 'ads' },
      { code: 'audit:view', description: 'Visualizar registros de auditoría', module: 'audit' },
    ];

    for (const p of permissionsData) {
      const res = await client.query(
        `INSERT INTO permissions (code, description, module)
         VALUES ($1, $2, $3)
         ON CONFLICT (code) DO NOTHING
         RETURNING id;`,
        [p.code, p.description, p.module]
      );
      const permId = res.rows[0]?.id;
      if (permId && roleMap['superadmin']) {
        await client.query(
          `INSERT INTO role_permissions (role_id, permission_id)
           VALUES ($1, $2)
           ON CONFLICT DO NOTHING;`,
          [roleMap['superadmin'], permId]
        );
      }
    }

    // 3. Administrador por defecto
    const adminEmail = 'admin@kynectia.com';
    const adminPassword = 'AdminKynectia2026!';
    const passwordHash = await bcrypt.hash(adminPassword, env.BCRYPT_SALT_ROUNDS);

    await client.query(
      `INSERT INTO admin_users (full_name, email, password_hash, role_id, is_active)
       VALUES ($1, $2, $3, $4, TRUE)
       ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash, is_active = TRUE
       RETURNING id;`,
      ['Administrador General Kynectia', adminEmail, passwordHash, roleMap['superadmin']]
    );
    console.log(`👤 Superadministrador listo: ${adminEmail} (Contraseña: ${adminPassword})`);

    // 4. Grupos Musculares
    const muscleGroupsData = [
      { name: 'Pectorales', description: 'Músculos pectorales mayor y menor, base de empujes de torso.', image_url: 'https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=600' },
      { name: 'Dorsales y Espalda', description: 'Dorsal ancho, trapecios, romboides y erectores espinales.', image_url: 'https://images.unsplash.com/photo-1603287681836-b174ce5074c2?w=600' },
      { name: 'Cuádriceps', description: 'Parte frontal del muslo, principal extensor de rodilla.', image_url: 'https://images.unsplash.com/photo-1434725039720-aaad6dd32dfe?w=600' },
      { name: 'Isquiotibiales', description: 'Parte posterior del muslo, flexores de rodilla y extensores de cadera.', image_url: 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600' },
      { name: 'Deltoides (Hombros)', description: 'Cabezas anterior, lateral y posterior del deltoides.', image_url: 'https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=600' },
      { name: 'Bíceps', description: 'Músculo bíceps braquial y braquial anterior, flexores de codo.', image_url: 'https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=600' },
      { name: 'Tríceps', description: 'Extensor principal del codo en sus tres cabezas.', image_url: 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600' },
      { name: 'Glúteos', description: 'Glúteo mayor, medio y menor, clave en potencia de cadera.', image_url: 'https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=600' },
      { name: 'Abdomen y Core', description: 'Recto abdominal, oblicuos y transverso abdominal.', image_url: 'https://images.unsplash.com/photo-1518611012118-696072aa579a?w=600' },
      { name: 'Pantorrillas', description: 'Gastrocnemio y sóleo, flexores plantares.', image_url: 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600' },
    ];

    const muscleMap: Record<string, number> = {};
    for (const mg of muscleGroupsData) {
      const res = await client.query(
        `INSERT INTO muscle_groups (name, description, image_url, is_active)
         VALUES ($1, $2, $3, TRUE)
         ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description, image_url = EXCLUDED.image_url
         RETURNING id, name;`,
        [mg.name, mg.description, mg.image_url]
      );
      muscleMap[res.rows[0].name] = res.rows[0].id;
    }

    // 5. Implementos Deportivos
    const equipmentData = [
      { name: 'Mancuernas', description: 'Pesas libres individuales para trabajo unilateral y estabilización.', category: 'free_weights', image_url: 'https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=600' },
      { name: 'Barra Olímpica', description: 'Barra de 20 kg estándar para levantamientos compuestos.', category: 'free_weights', image_url: 'https://images.unsplash.com/photo-1517963879433-6ad2b056d712?w=600' },
      { name: 'Banco Ajustable', description: 'Banco de entrenamiento plano, inclinado y declinado.', category: 'accessories', image_url: 'https://images.unsplash.com/photo-1540497077202-7c8a3999166f?w=600' },
      { name: 'Polea Doble / Crossover', description: 'Sistema de cables de tensión constante y altura regulable.', category: 'machines', image_url: 'https://images.unsplash.com/photo-1534367507873-d2d7e24c797f?w=600' },
      { name: 'Bandas de Resistencia', description: 'Bandas elásticas de tensión variable para activación y sobrecarga.', category: 'resistance', image_url: 'https://images.unsplash.com/photo-1598971639058-fab3c3109a00?w=600' },
      { name: 'Barra de Dominadas', description: 'Estructura rígida para tracciones con peso corporal.', category: 'bodyweight', image_url: 'https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=600' },
      { name: 'Prensa de Piernas', description: 'Máquina guiada a 45 grados para tren inferior.', category: 'machines', image_url: 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600' },
      { name: 'Colchoneta Fitness', description: 'Superficie acolchada antideslizante para ejercicios de suelo y core.', category: 'accessories', image_url: 'https://images.unsplash.com/photo-1518611012118-696072aa579a?w=600' },
    ];

    const equipmentMap: Record<string, number> = {};
    for (const eq of equipmentData) {
      const res = await client.query(
        `INSERT INTO equipment (name, description, category, image_url, is_active)
         VALUES ($1, $2, $3, $4, TRUE)
         ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description, category = EXCLUDED.category
         RETURNING id, name;`,
        [eq.name, eq.description, eq.category, eq.image_url]
      );
      equipmentMap[res.rows[0].name] = res.rows[0].id;
    }

    // 6. Ejercicios Maestros
    const exercisesData = [
      {
        name: 'Press de Banca Plano con Barra',
        description: 'Ejercicio compuesto fundamental de empuje horizontal para desarrollo pectoral y de tríceps.',
        execution_instructions: '1. Acuéstate en el banco con los pies plantados.\n2. Agarra la barra con apertura ligeramente mayor al ancho de hombros.\n3. Desciende la barra con control hacia el esternón medio.\n4. Empuja con fuerza manteniendo las escápulas retraídas.',
        difficulty_level: 'intermediate',
        exercise_type: 'strength',
        demo_image_url: 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600',
        demo_video_url: 'https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_1mb.mp4',
        safety_recommendations: 'No permitas que las muñecas se doblen hacia atrás. Usa cierres de seguridad.',
        publication_status: 'published',
        primary_muscle: 'Pectorales',
        secondary_muscles: ['Tríceps', 'Deltoides (Hombros)'],
        equipment: ['Barra Olímpica', 'Banco Ajustable'],
      },
      {
        name: 'Sentadilla Trasera con Barra',
        description: 'Rey de los ejercicios de tren inferior para cuádriceps, glúteos y estabilidad del tronco.',
        execution_instructions: '1. Coloca la barra sobre los trapecios.\n2. Abre los pies a la anchura de hombros.\n3. Flexiona caderas y rodillas manteniendo el pecho erguido.\n4. Desciende hasta romper el paralelo y sube expulsando el aire.',
        difficulty_level: 'advanced',
        exercise_type: 'strength',
        demo_image_url: 'https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=600',
        demo_video_url: 'https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_1mb.mp4',
        safety_recommendations: 'Mantén los talones firmes en el suelo en todo el recorrido.',
        publication_status: 'published',
        primary_muscle: 'Cuádriceps',
        secondary_muscles: ['Glúteos', 'Isquiotibiales', 'Abdomen y Core'],
        equipment: ['Barra Olímpica'],
      },
      {
        name: 'Dominadas Pronas (Pull-ups)',
        description: 'Tracción vertical con autocarga para ensanchamiento de dorsales y fuerza funcional.',
        execution_instructions: '1. Sujétate de la barra con agarre prono amplio.\n2. Inicia el movimiento deprimiendo las escápulas.\n3. Lleva la barbilla por encima de la barra.\n4. Baja de forma controlada hasta la extensión completa.',
        difficulty_level: 'intermediate',
        exercise_type: 'strength',
        demo_image_url: 'https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=600',
        demo_video_url: '',
        safety_recommendations: 'Evita el balanceo excesivo del cuerpo.',
        publication_status: 'published',
        primary_muscle: 'Dorsales y Espalda',
        secondary_muscles: ['Bíceps'],
        equipment: ['Barra de Dominadas'],
      },
      {
        name: 'Press Militar con Mancuernas Sentado',
        description: 'Empuje vertical para hipertrofia deltoidea y estabilidad escapular.',
        execution_instructions: '1. Siéntate en banco a 90 grados.\n2. Eleva mancuernas al nivel de orejas con codos a 45 grados respecto al torso.\n3. Empuja hacia arriba sin bloquear completamente los codos.',
        difficulty_level: 'beginner',
        exercise_type: 'strength',
        demo_image_url: 'https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=600',
        demo_video_url: '',
        safety_recommendations: 'No arquees la zona lumbar contra el respaldo.',
        publication_status: 'published',
        primary_muscle: 'Deltoides (Hombros)',
        secondary_muscles: ['Tríceps'],
        equipment: ['Mancuernas', 'Banco Ajustable'],
      },
      {
        name: 'Curl de Bíceps con Mancuernas',
        description: 'Aislamiento de bíceps con rotación supinadora para pico de contracción.',
        execution_instructions: '1. De pie con mancuernas a los lados.\n2. Flexiona los codos girando las palmas hacia arriba.\n3. Aprieta en la cima y desciende en 3 segundos.',
        difficulty_level: 'beginner',
        exercise_type: 'strength',
        demo_image_url: 'https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=600',
        demo_video_url: '',
        safety_recommendations: 'Mantén los codos pegados a las costillas sin impulsarte con la cadera.',
        publication_status: 'published',
        primary_muscle: 'Bíceps',
        secondary_muscles: [],
        equipment: ['Mancuernas'],
      },
      {
        name: 'Plancha Abdominal Isométrica',
        description: 'Contracción isométrica global del core para estabilidad lumbo-pélvica.',
        execution_instructions: '1. Apoya antebrazos y puntas de los pies en la colchoneta.\n2. Alinea cabeza, hombros, cadera y tobillos.\n3. Contrae glúteos y abdomen fuertemente.',
        difficulty_level: 'beginner',
        exercise_type: 'flexibility',
        demo_image_url: 'https://images.unsplash.com/photo-1518611012118-696072aa579a?w=600',
        demo_video_url: '',
        safety_recommendations: 'Evita que la cadera caiga o se eleve en forma de pirámide.',
        publication_status: 'published',
        primary_muscle: 'Abdomen y Core',
        secondary_muscles: ['Deltoides (Hombros)', 'Glúteos'],
        equipment: ['Colchoneta Fitness'],
      },
    ];

    const exerciseMap: Record<string, number> = {};
    for (const ex of exercisesData) {
      const res = await client.query(
        `INSERT INTO exercises (
          name, description, execution_instructions, difficulty_level,
          exercise_type, demo_image_url, demo_video_url, safety_recommendations,
          publication_status, is_active
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, TRUE)
        ON CONFLICT (name) DO UPDATE SET
          description = EXCLUDED.description,
          execution_instructions = EXCLUDED.execution_instructions,
          difficulty_level = EXCLUDED.difficulty_level,
          exercise_type = EXCLUDED.exercise_type,
          publication_status = EXCLUDED.publication_status
        RETURNING id, name;`,
        [
          ex.name,
          ex.description,
          ex.execution_instructions,
          ex.difficulty_level,
          ex.exercise_type,
          ex.demo_image_url,
          ex.demo_video_url,
          ex.safety_recommendations,
          ex.publication_status,
        ]
      );
      const exId = res.rows[0].id;
      exerciseMap[ex.name] = exId;

      // Grupo primario
      if (muscleMap[ex.primary_muscle]) {
        await client.query(
          `INSERT INTO exercise_muscles (exercise_id, muscle_group_id, is_primary)
           VALUES ($1, $2, TRUE)
           ON CONFLICT (exercise_id, muscle_group_id) DO UPDATE SET is_primary = TRUE;`,
          [exId, muscleMap[ex.primary_muscle]]
        );
      }

      // Grupos secundarios
      for (const secMuscle of ex.secondary_muscles) {
        if (muscleMap[secMuscle]) {
          await client.query(
            `INSERT INTO exercise_muscles (exercise_id, muscle_group_id, is_primary)
             VALUES ($1, $2, FALSE)
             ON CONFLICT (exercise_id, muscle_group_id) DO UPDATE SET is_primary = FALSE;`,
            [exId, muscleMap[secMuscle]]
          );
        }
      }

      // Implementos
      for (const eqName of ex.equipment) {
        if (equipmentMap[eqName]) {
          await client.query(
            `INSERT INTO exercise_equipment (exercise_id, equipment_id)
             VALUES ($1, $2)
             ON CONFLICT (exercise_id, equipment_id) DO NOTHING;`,
            [exId, equipmentMap[eqName]]
          );
        }
      }
    }

    // 7. Rutinas de Entrenamiento
    const routinesData = [
      {
        name: 'Torso Potencia y Fuerza Hipertrófica',
        description: 'Rutina completa orientada a desarrollo de pecho, espalda superior, hombros y brazos.',
        cover_image_url: 'https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800',
        goal: 'hypertrophy',
        difficulty_level: 'intermediate',
        category: 'upper_lower',
        estimated_duration_minutes: 55,
        publication_status: 'published',
        exercises: [
          { name: 'Press de Banca Plano con Barra', seq: 1, sets: 4, reps: 8, rest: 90, weight: 60.0, notes: 'Calentar 2 series previas sin fatiga.' },
          { name: 'Dominadas Pronas (Pull-ups)', seq: 2, sets: 4, reps: 8, rest: 90, weight: 0.0, notes: 'Añadir lastre si completas 10 repeticiones.' },
          { name: 'Press Militar con Mancuernas Sentado', seq: 3, sets: 3, reps: 10, rest: 60, weight: 16.0, notes: 'Control excéntrico de 2 segundos.' },
          { name: 'Curl de Bíceps con Mancuernas', seq: 4, sets: 3, reps: 12, rest: 45, weight: 12.0, notes: 'Supinación completa al final.' },
        ],
      },
      {
        name: 'Pierna & Core Constructor',
        description: 'Enfocada en fuerza de cuádriceps, glúteos y solidez abdominal.',
        cover_image_url: 'https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=800',
        goal: 'strength',
        difficulty_level: 'advanced',
        category: 'lower_body',
        estimated_duration_minutes: 50,
        publication_status: 'published',
        exercises: [
          { name: 'Sentadilla Trasera con Barra', seq: 1, sets: 5, reps: 5, rest: 120, weight: 80.0, notes: 'Profundidad estricta paralela.' },
          { name: 'Plancha Abdominal Isométrica', seq: 2, sets: 4, reps: 1, rest: 60, weight: 0.0, notes: 'Mantener 45 segundos por serie.' },
        ],
      },
    ];

    for (const r of routinesData) {
      const res = await client.query(
        `INSERT INTO workout_routines (
          name, description, cover_image_url, goal, difficulty_level,
          category, estimated_duration_minutes, publication_status, version, is_active
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 1, TRUE)
        RETURNING id;`,
        [r.name, r.description, r.cover_image_url, r.goal, r.difficulty_level, r.category, r.estimated_duration_minutes, r.publication_status]
      );
      const routineId = res.rows[0].id;

      for (const exConfig of r.exercises) {
        const exId = exerciseMap[exConfig.name];
        if (exId) {
          await client.query(
            `INSERT INTO routine_exercises (
              routine_id, exercise_id, sequence_order, sets, reps, duration_seconds, rest_seconds, recommended_weight_kg, specific_instructions
            ) VALUES ($1, $2, $3, $4, $5, 0, $6, $7, $8);`,
            [routineId, exId, exConfig.seq, exConfig.sets, exConfig.reps, exConfig.rest, exConfig.weight, exConfig.notes]
          );
        }
      }
    }

    // 8. Planes Comerciales
    const plansData = [
      {
        name: 'Plan Gratuito Starter',
        description: 'Acceso básico al catálogo general de ejercicios y rutinas introductorias.',
        billing_cycle: 'free',
        price: 0.00,
        currency: 'USD',
        benefits: JSON.stringify(['Acceso a 50 ejercicios básicos', 'Rutinas predeterminadas de cuerpo completo', 'Registro de progreso inicial']),
        discount_percent: 0.00,
      },
      {
        name: 'Plan Mensual Pro',
        description: 'Acceso ilimitado a todas las rutinas avanzadas y catálogo completo con videos 4K.',
        billing_cycle: 'monthly',
        price: 14.99,
        currency: 'USD',
        benefits: JSON.stringify(['Todo del Plan Starter', 'Catálogo ilimitado de ejercicios', 'Creación ilimitada de rutinas personalizadas', 'Soporte prioritario']),
        discount_percent: 10.00,
      },
      {
        name: 'Plan Anual VIP Elite',
        description: 'Ahorro del 35% con acceso premium continuo durante 12 meses.',
        billing_cycle: 'annual',
        price: 119.99,
        currency: 'USD',
        benefits: JSON.stringify(['Todas las características Pro', 'Descuento anual garantizado', 'Acceso anticipado a nuevas rutinas', 'Planes nutricionales integrados']),
        discount_percent: 35.00,
      },
    ];

    const planMap: Record<string, number> = {};
    for (const pl of plansData) {
      const res = await client.query(
        `INSERT INTO plans (name, description, billing_cycle, price, currency, benefits, is_active, discount_percent)
         VALUES ($1, $2, $3, $4, $5, $6, TRUE, $7)
         ON CONFLICT (name) DO UPDATE SET price = EXCLUDED.price, benefits = EXCLUDED.benefits
         RETURNING id, name;`,
        [pl.name, pl.description, pl.billing_cycle, pl.price, pl.currency, pl.benefits, pl.discount_percent]
      );
      planMap[res.rows[0].name] = res.rows[0].id;
    }

    // 9. Usuarios Atletas de Demostración
    const demoUsers = [
      { full_name: 'Carlos Mendoza', email: 'carlos.mendoza@email.com', phone: '+593981234567', gender: 'male', height_cm: 178, weight_kg: 82.5, goal: 'hypertrophy' },
      { full_name: 'Valeria Rivas', email: 'valeria.rivas@email.com', phone: '+593987654321', gender: 'female', height_cm: 165, weight_kg: 58.0, goal: 'toning' },
      { full_name: 'Mateo Morales', email: 'mateo.morales@email.com', phone: '+593991122334', gender: 'male', height_cm: 184, weight_kg: 89.0, goal: 'strength' },
      { full_name: 'Lucía Fernández', email: 'lucia.fernandez@email.com', phone: '+593976543210', gender: 'female', height_cm: 160, weight_kg: 54.0, goal: 'fat_loss' },
    ];

    for (const u of demoUsers) {
      const res = await client.query(
        `INSERT INTO users (full_name, email, phone, gender, height_cm, weight_kg, fitness_goal, is_active, email_verified)
         VALUES ($1, $2, $3, $4, $5, $6, $7, TRUE, TRUE)
         ON CONFLICT (email) DO UPDATE SET phone = EXCLUDED.phone
         RETURNING id;`,
        [u.full_name, u.email, u.phone, u.gender, u.height_cm, u.weight_kg, u.goal]
      );
      const userId = res.rows[0].id;

      // Asignar una suscripción activa
      if (planMap['Plan Mensual Pro']) {
        const endDate = new Date();
        endDate.setDate(endDate.getDate() + 30);
        await client.query(
          `INSERT INTO user_subscriptions (user_id, plan_id, status, start_date, end_date, price_paid, currency)
           VALUES ($1, $2, 'active', NOW(), $3, 14.99, 'USD')
           ON CONFLICT DO NOTHING;`,
          [userId, planMap['Plan Mensual Pro'], endDate]
        );
      }
    }

    // 10. Anuncios y Publicidad
    const adsData = [
      {
        title: 'Nueva Colección Ropa Técnica Kynectia Apparel',
        description: 'Prendas transpirables de compresión máxima diseñadas para atletas de alto rendimiento.',
        media_url: 'https://images.unsplash.com/photo-1518611012118-696072aa579a?w=1000',
        destination_url: 'https://kynectia.com/store/apparel',
        placement: 'banner_home',
        priority: 1,
        starts_at: new Date(),
        ends_at: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
      },
      {
        title: 'Proteína Whey Isolate Kynectia Labs - 20% OFF',
        description: 'Acelera tu recuperación con la proteína microfiltrada de mayor biodisponibilidad.',
        media_url: 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=1000',
        destination_url: 'https://kynectia.com/store/supplements',
        placement: 'interstitial_workout',
        priority: 2,
        starts_at: new Date(),
        ends_at: new Date(Date.now() + 60 * 24 * 60 * 60 * 1000),
      },
    ];

    for (const ad of adsData) {
      await client.query(
        `INSERT INTO ads (title, description, media_url, destination_url, placement, priority, starts_at, ends_at, is_active)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, TRUE);`,
        [ad.title, ad.description, ad.media_url, ad.destination_url, ad.placement, ad.priority, ad.starts_at, ad.ends_at]
      );
    }
  });

  console.log('✅ Seeders ejecutados exitosamente con datos reales para todos los módulos.');
};

if (require.main === module) {
  runSeed()
    .then(async () => {
      await pool.end();
      process.exit(0);
    })
    .catch(async (err) => {
      console.error('❌ Error en seeder:', err);
      await pool.end();
      process.exit(1);
    });
}
