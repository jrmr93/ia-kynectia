import { pool } from '../../database/pool';

export class DashboardService {
  static async getSummaryStats() {
    // Consulta atómica de conteos reales desde PostgreSQL
    const statsQuery = `
      SELECT
        (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL)::int AS total_users,
        (SELECT COUNT(*) FROM users WHERE is_active = TRUE AND deleted_at IS NULL)::int AS active_users,
        (SELECT COUNT(*) FROM exercises WHERE deleted_at IS NULL)::int AS total_exercises,
        (SELECT COUNT(*) FROM exercises WHERE publication_status = 'published' AND is_active = TRUE AND deleted_at IS NULL)::int AS published_exercises,
        (SELECT COUNT(*) FROM muscle_groups WHERE deleted_at IS NULL)::int AS total_muscles,
        (SELECT COUNT(*) FROM equipment WHERE deleted_at IS NULL)::int AS total_equipment,
        (SELECT COUNT(*) FROM workout_routines WHERE deleted_at IS NULL)::int AS total_routines,
        (SELECT COUNT(*) FROM workout_routines WHERE publication_status = 'published' AND is_active = TRUE AND deleted_at IS NULL)::int AS published_routines,
        (SELECT COUNT(*) FROM plans WHERE deleted_at IS NULL)::int AS total_plans,
        (SELECT COUNT(*) FROM plans WHERE is_active = TRUE AND deleted_at IS NULL)::int AS active_plans,
        (SELECT COUNT(*) FROM user_subscriptions WHERE status = 'active' AND end_date >= NOW())::int AS active_subscriptions,
        (SELECT COUNT(*) FROM ads WHERE is_active = TRUE AND starts_at <= NOW() AND ends_at >= NOW() AND deleted_at IS NULL)::int AS active_ads;
    `;

    const { rows } = await pool.query(statsQuery);
    const counts = rows[0];

    // Ejercicios por dificultad
    const difficultyQuery = `
      SELECT difficulty_level, COUNT(*)::int as count
      FROM exercises
      WHERE deleted_at IS NULL
      GROUP BY difficulty_level
      ORDER BY count DESC;
    `;
    const { rows: difficultyRows } = await pool.query(difficultyQuery);

    // Ejercicios por tipo de entrenamiento
    const typeQuery = `
      SELECT exercise_type, COUNT(*)::int as count
      FROM exercises
      WHERE deleted_at IS NULL
      GROUP BY exercise_type
      ORDER BY count DESC;
    `;
    const { rows: typeRows } = await pool.query(typeQuery);

    // Suscripciones por plan
    const subsByPlanQuery = `
      SELECT p.name as plan_name, p.billing_cycle, COUNT(s.id)::int as count
      FROM plans p
      LEFT JOIN user_subscriptions s ON p.id = s.plan_id AND s.status = 'active'
      WHERE p.deleted_at IS NULL
      GROUP BY p.id, p.name, p.billing_cycle
      ORDER BY count DESC;
    `;
    const { rows: subsByPlanRows } = await pool.query(subsByPlanQuery);

    // Últimos logs de auditoría
    const recentActivityQuery = `
      SELECT id, admin_email, action, entity_type, entity_id, created_at
      FROM audit_logs
      ORDER BY created_at DESC
      LIMIT 6;
    `;
    const { rows: activityRows } = await pool.query(recentActivityQuery);

    return {
      overview: {
        totalUsers: counts.total_users,
        activeUsers: counts.active_users,
        totalExercises: counts.total_exercises,
        publishedExercises: counts.published_exercises,
        totalMuscleGroups: counts.total_muscles,
        totalEquipment: counts.total_equipment,
        totalRoutines: counts.total_routines,
        publishedRoutines: counts.published_routines,
        totalPlans: counts.total_plans,
        activePlans: counts.active_plans,
        activeSubscriptions: counts.active_subscriptions,
        activeAds: counts.active_ads,
      },
      charts: {
        exercisesByDifficulty: difficultyRows,
        exercisesByType: typeRows,
        subscriptionsByPlan: subsByPlanRows,
      },
      recentActivity: activityRows,
    };
  }
}
