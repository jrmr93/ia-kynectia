import { pool, withTransaction } from '../../database/pool';
import { PaginationMeta } from '../../utils/response';
import { recordAuditLog } from '../../utils/logger';

export interface WorkoutFilterOptions {
  search?: string;
  difficultyLevel?: string;
  category?: string;
  publicationStatus?: string;
  goal?: string;
  isActive?: boolean;
  page?: number;
  limit?: number;
}

export interface RoutineExerciseItem {
  exerciseId: number;
  sequenceOrder: number;
  sets: number;
  reps: number;
  durationSeconds?: number;
  restSeconds: number;
  recommendedWeightKg?: number;
  specificInstructions?: string;
}

export interface CreateWorkoutDto {
  name: string;
  description?: string;
  coverImageUrl?: string;
  goal: string;
  difficultyLevel: 'beginner' | 'intermediate' | 'advanced';
  category: string;
  estimatedDurationMinutes: number;
  publicationStatus?: 'draft' | 'published' | 'archived';
  isActive?: boolean;
  exercises: RoutineExerciseItem[];
}

export class WorkoutsService {
  static async list(options: WorkoutFilterOptions) {
    const page = Math.max(1, options.page || 1);
    const limit = Math.min(100, Math.max(1, options.limit || 10));
    const offset = (page - 1) * limit;

    const conditions: string[] = ['wr.deleted_at IS NULL'];
    const params: any[] = [];

    if (options.search) {
      params.push(`%${options.search.trim().toLowerCase()}%`);
      conditions.push(`(LOWER(wr.name) LIKE $${params.length} OR LOWER(wr.description) LIKE $${params.length})`);
    }

    if (options.difficultyLevel) {
      params.push(options.difficultyLevel);
      conditions.push(`wr.difficulty_level = $${params.length}`);
    }

    if (options.category) {
      params.push(options.category);
      conditions.push(`wr.category = $${params.length}`);
    }

    if (options.publicationStatus) {
      params.push(options.publicationStatus);
      conditions.push(`wr.publication_status = $${params.length}`);
    }

    if (options.goal) {
      params.push(options.goal);
      conditions.push(`wr.goal = $${params.length}`);
    }

    if (options.isActive !== undefined) {
      params.push(options.isActive);
      conditions.push(`wr.is_active = $${params.length}`);
    }

    const whereClause = `WHERE ${conditions.join(' AND ')}`;

    const countQuery = `SELECT COUNT(*)::int as total FROM workout_routines wr ${whereClause};`;
    const { rows: countRows } = await pool.query(countQuery, params);
    const total = countRows[0].total;

    const query = `
      SELECT wr.id, wr.name, wr.description, wr.cover_image_url, wr.goal,
             wr.difficulty_level, wr.category, wr.estimated_duration_minutes,
             wr.publication_status, wr.version, wr.is_active, wr.created_at, wr.updated_at,
             (SELECT COUNT(*)::int FROM routine_exercises re WHERE re.routine_id = wr.id) as exercises_count
      FROM workout_routines wr
      ${whereClause}
      ORDER BY wr.created_at DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2};
    `;

    const { rows } = await pool.query(query, [...params, limit, offset]);

    const pagination: PaginationMeta = {
      page,
      limit,
      total,
      totalPages: Math.ceil(total / limit),
    };

    return { routines: rows, pagination };
  }

  static async getById(id: number) {
    const routineQuery = `
      SELECT wr.*
      FROM workout_routines wr
      WHERE wr.id = $1 AND wr.deleted_at IS NULL;
    `;
    const { rows: routineRows } = await pool.query(routineQuery, [id]);
    if (routineRows.length === 0) {
      throw { statusCode: 404, message: 'Rutina no encontrada.' };
    }

    const exercisesQuery = `
      SELECT re.id, re.sequence_order, re.sets, re.reps, re.duration_seconds,
             re.rest_seconds, re.recommended_weight_kg, re.specific_instructions,
             e.id as exercise_id, e.name as exercise_name, e.difficulty_level,
             e.exercise_type, e.demo_image_url, e.demo_video_url
      FROM routine_exercises re
      JOIN exercises e ON re.exercise_id = e.id
      WHERE re.routine_id = $1
      ORDER BY re.sequence_order ASC;
    `;
    const { rows: exerciseRows } = await pool.query(exercisesQuery, [id]);

    return {
      ...routineRows[0],
      exercises: exerciseRows,
    };
  }

  static async create(dto: CreateWorkoutDto, adminId?: number, adminEmail?: string) {
    return await withTransaction(async (client) => {
      const insertQuery = `
        INSERT INTO workout_routines (
          name, description, cover_image_url, goal, difficulty_level,
          category, estimated_duration_minutes, publication_status, version, is_active
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 1, $9)
        RETURNING *;
      `;
      const values = [
        dto.name.trim(),
        dto.description || null,
        dto.coverImageUrl || null,
        dto.goal,
        dto.difficultyLevel,
        dto.category,
        dto.estimatedDurationMinutes,
        dto.publicationStatus || 'draft',
        dto.isActive !== undefined ? dto.isActive : true,
      ];

      const { rows } = await client.query(insertQuery, values);
      const createdRoutine = rows[0];
      const routineId = createdRoutine.id;

      if (dto.exercises && dto.exercises.length > 0) {
        for (let i = 0; i < dto.exercises.length; i++) {
          const item = dto.exercises[i];
          await client.query(
            `INSERT INTO routine_exercises (
              routine_id, exercise_id, sequence_order, sets, reps, duration_seconds, rest_seconds, recommended_weight_kg, specific_instructions
            ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9);`,
            [
              routineId,
              item.exerciseId,
              item.sequenceOrder || i + 1,
              item.sets || 3,
              item.reps || 10,
              item.durationSeconds || 0,
              item.restSeconds || 60,
              item.recommendedWeightKg || null,
              item.specificInstructions || null,
            ]
          );
        }
      }

      await recordAuditLog({
        adminId,
        adminEmail,
        action: 'CREATE',
        entityType: 'workout_routines',
        entityId: routineId,
        newValues: dto,
      });

      return createdRoutine;
    });
  }

  static async update(id: number, dto: Partial<CreateWorkoutDto>, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    return await withTransaction(async (client) => {
      // Incrementar versión si ya estaba publicada para asegurar versionado
      const newVersion = existing.publication_status === 'published' ? existing.version + 1 : existing.version;

      const updateQuery = `
        UPDATE workout_routines
        SET name = COALESCE($1, name),
            description = COALESCE($2, description),
            cover_image_url = COALESCE($3, cover_image_url),
            goal = COALESCE($4, goal),
            difficulty_level = COALESCE($5, difficulty_level),
            category = COALESCE($6, category),
            estimated_duration_minutes = COALESCE($7, estimated_duration_minutes),
            publication_status = COALESCE($8, publication_status),
            is_active = COALESCE($9, is_active),
            version = $10
        WHERE id = $11 AND deleted_at IS NULL
        RETURNING *;
      `;
      const values = [
        dto.name ? dto.name.trim() : null,
        dto.description,
        dto.coverImageUrl,
        dto.goal,
        dto.difficultyLevel,
        dto.category,
        dto.estimatedDurationMinutes,
        dto.publicationStatus,
        dto.isActive,
        newVersion,
        id,
      ];

      const { rows } = await client.query(updateQuery, values);
      const updatedRoutine = rows[0];

      if (dto.exercises !== undefined) {
        await client.query('DELETE FROM routine_exercises WHERE routine_id = $1;', [id]);
        for (let i = 0; i < dto.exercises.length; i++) {
          const item = dto.exercises[i];
          await client.query(
            `INSERT INTO routine_exercises (
              routine_id, exercise_id, sequence_order, sets, reps, duration_seconds, rest_seconds, recommended_weight_kg, specific_instructions
            ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9);`,
            [
              id,
              item.exerciseId,
              item.sequenceOrder || i + 1,
              item.sets || 3,
              item.reps || 10,
              item.durationSeconds || 0,
              item.restSeconds || 60,
              item.recommendedWeightKg || null,
              item.specificInstructions || null,
            ]
          );
        }
      }

      await recordAuditLog({
        adminId,
        adminEmail,
        action: 'UPDATE',
        entityType: 'workout_routines',
        entityId: id,
        oldValues: existing,
        newValues: { ...dto, version: newVersion },
      });

      return updatedRoutine;
    });
  }

  static async setPublicationStatus(id: number, status: 'draft' | 'published' | 'archived', adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);
    const { rows } = await pool.query(
      'UPDATE workout_routines SET publication_status = $1 WHERE id = $2 AND deleted_at IS NULL RETURNING id, name, publication_status, version;',
      [status, id]
    );

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'PUBLISH',
      entityType: 'workout_routines',
      entityId: id,
      oldValues: { publicationStatus: existing.publication_status },
      newValues: { publicationStatus: status },
    });

    return rows[0];
  }

  static async delete(id: number, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    await pool.query('UPDATE workout_routines SET deleted_at = NOW(), is_active = FALSE WHERE id = $1;', [id]);

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'DELETE',
      entityType: 'workout_routines',
      entityId: id,
      oldValues: existing,
      newValues: { deleted_at: new Date() },
    });

    return { message: 'Rutina eliminada exitosamente (soft-delete).' };
  }
}
