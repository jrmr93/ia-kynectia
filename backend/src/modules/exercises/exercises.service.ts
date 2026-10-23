import fs from 'fs';
import path from 'path';
import { pool, withTransaction } from '../../database/pool';
import { PaginationMeta } from '../../utils/response';
import { recordAuditLog } from '../../utils/logger';

export interface ExerciseFilterOptions {
  search?: string;
  difficultyLevel?: string;
  exerciseType?: string;
  publicationStatus?: string;
  muscleGroupId?: number;
  equipmentId?: number;
  isActive?: boolean;
  page?: number;
  limit?: number;
}

export interface CreateExerciseDto {
  name: string;
  description?: string;
  executionInstructions?: string;
  difficultyLevel: 'beginner' | 'intermediate' | 'advanced';
  exerciseType: 'strength' | 'cardio' | 'flexibility' | 'hiit' | 'plyometric';
  demoImageUrl?: string;
  demoVideoUrl?: string;
  safetyRecommendations?: string;
  publicationStatus?: 'draft' | 'published' | 'archived';
  isActive?: boolean;
  primaryMuscleGroupId: number;
  secondaryMuscleGroupIds?: number[];
  equipmentIds?: number[];
}

export class ExercisesService {
  static async list(options: ExerciseFilterOptions) {
    const page = Math.max(1, options.page || 1);
    const limit = Math.min(100, Math.max(1, options.limit || 10));
    const offset = (page - 1) * limit;

    const conditions: string[] = ['e.deleted_at IS NULL'];
    const params: any[] = [];

    if (options.search) {
      params.push(`%${options.search.trim().toLowerCase()}%`);
      conditions.push(`(LOWER(e.name) LIKE $${params.length} OR LOWER(e.description) LIKE $${params.length})`);
    }

    if (options.difficultyLevel) {
      params.push(options.difficultyLevel);
      conditions.push(`e.difficulty_level = $${params.length}`);
    }

    if (options.exerciseType) {
      params.push(options.exerciseType);
      conditions.push(`e.exercise_type = $${params.length}`);
    }

    if (options.publicationStatus) {
      params.push(options.publicationStatus);
      conditions.push(`e.publication_status = $${params.length}`);
    }

    if (options.isActive !== undefined) {
      params.push(options.isActive);
      conditions.push(`e.is_active = $${params.length}`);
    }

    if (options.muscleGroupId) {
      params.push(options.muscleGroupId);
      conditions.push(`EXISTS (SELECT 1 FROM exercise_muscles em WHERE em.exercise_id = e.id AND em.muscle_group_id = $${params.length})`);
    }

    if (options.equipmentId) {
      params.push(options.equipmentId);
      conditions.push(`EXISTS (SELECT 1 FROM exercise_equipment ee WHERE ee.exercise_id = e.id AND ee.equipment_id = $${params.length})`);
    }

    const whereClause = `WHERE ${conditions.join(' AND ')}`;

    const countQuery = `SELECT COUNT(*)::int as total FROM exercises e ${whereClause};`;
    const { rows: countRows } = await pool.query(countQuery, params);
    const total = countRows[0].total;

    const query = `
      SELECT e.id, e.name, e.description, e.execution_instructions, e.difficulty_level,
             e.exercise_type, e.demo_image_url, e.demo_video_url, e.safety_recommendations,
             e.publication_status, e.is_active, e.created_at, e.updated_at,
             (SELECT m.name FROM exercise_muscles em
              JOIN muscle_groups m ON em.muscle_group_id = m.id
              WHERE em.exercise_id = e.id AND em.is_primary = TRUE LIMIT 1) as primary_muscle_name,
             (SELECT m.id FROM exercise_muscles em
              JOIN muscle_groups m ON em.muscle_group_id = m.id
              WHERE em.exercise_id = e.id AND em.is_primary = TRUE LIMIT 1) as primary_muscle_id,
             COALESCE((
               SELECT json_agg(json_build_object('id', m.id, 'name', m.name))
               FROM exercise_muscles em
               JOIN muscle_groups m ON em.muscle_group_id = m.id
               WHERE em.exercise_id = e.id AND em.is_primary = FALSE
             ), '[]'::json) as secondary_muscles,
             COALESCE((
               SELECT json_agg(json_build_object('id', eq.id, 'name', eq.name, 'category', eq.category))
               FROM exercise_equipment ee
               JOIN equipment eq ON ee.equipment_id = eq.id
               WHERE ee.exercise_id = e.id
             ), '[]'::json) as equipment,
             (SELECT COUNT(*)::int FROM routine_exercises re
              JOIN workout_routines wr ON re.routine_id = wr.id AND wr.deleted_at IS NULL
              WHERE re.exercise_id = e.id) as routines_count
      FROM exercises e
      ${whereClause}
      ORDER BY e.created_at DESC
      LIMIT $${params.length + 1} OFFSET $${params.length + 2};
    `;

    const { rows } = await pool.query(query, [...params, limit, offset]);

    const pagination: PaginationMeta = {
      page,
      limit,
      total,
      totalPages: Math.ceil(total / limit),
    };

    return { exercises: rows, pagination };
  }

  static async getById(id: number) {
    const query = `
      SELECT e.*,
             (SELECT json_build_object('id', m.id, 'name', m.name)
              FROM exercise_muscles em
              JOIN muscle_groups m ON em.muscle_group_id = m.id
              WHERE em.exercise_id = e.id AND em.is_primary = TRUE LIMIT 1) as primary_muscle,
             COALESCE((
               SELECT json_agg(json_build_object('id', m.id, 'name', m.name))
               FROM exercise_muscles em
               JOIN muscle_groups m ON em.muscle_group_id = m.id
               WHERE em.exercise_id = e.id AND em.is_primary = FALSE
             ), '[]'::json) as secondary_muscles,
             COALESCE((
               SELECT json_agg(json_build_object('id', eq.id, 'name', eq.name, 'category', eq.category))
               FROM exercise_equipment ee
               JOIN equipment eq ON ee.equipment_id = eq.id
               WHERE ee.exercise_id = e.id
             ), '[]'::json) as equipment,
             COALESCE((
               SELECT json_agg(json_build_object('routineId', wr.id, 'routineName', wr.name, 'sets', re.sets, 'reps', re.reps))
               FROM routine_exercises re
               JOIN workout_routines wr ON re.routine_id = wr.id AND wr.deleted_at IS NULL
               WHERE re.exercise_id = e.id
             ), '[]'::json) as active_routines
      FROM exercises e
      WHERE e.id = $1 AND e.deleted_at IS NULL;
    `;

    const { rows } = await pool.query(query, [id]);
    if (rows.length === 0) {
      throw { statusCode: 404, message: 'Ejercicio no encontrado.' };
    }
    return rows[0];
  }

  static async create(dto: CreateExerciseDto, adminId?: number, adminEmail?: string) {
    return await withTransaction(async (client) => {
      const insertQuery = `
        INSERT INTO exercises (
          name, description, execution_instructions, difficulty_level,
          exercise_type, demo_image_url, demo_video_url, safety_recommendations,
          publication_status, is_active
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
        RETURNING *;
      `;
      const values = [
        dto.name.trim(),
        dto.description || null,
        dto.executionInstructions || null,
        dto.difficultyLevel,
        dto.exerciseType,
        dto.demoImageUrl || null,
        dto.demoVideoUrl || null,
        dto.safetyRecommendations || null,
        dto.publicationStatus || 'published',
        dto.isActive !== undefined ? dto.isActive : true,
      ];

      const { rows } = await client.query(insertQuery, values);
      const createdExercise = rows[0];
      const exerciseId = createdExercise.id;

      // 1. Grupo primario
      await client.query(
        `INSERT INTO exercise_muscles (exercise_id, muscle_group_id, is_primary)
         VALUES ($1, $2, TRUE);`,
        [exerciseId, dto.primaryMuscleGroupId]
      );

      // 2. Grupos secundarios
      if (dto.secondaryMuscleGroupIds && dto.secondaryMuscleGroupIds.length > 0) {
        for (const smId of dto.secondaryMuscleGroupIds) {
          if (smId !== dto.primaryMuscleGroupId) {
            await client.query(
              `INSERT INTO exercise_muscles (exercise_id, muscle_group_id, is_primary)
               VALUES ($1, $2, FALSE)
               ON CONFLICT DO NOTHING;`,
              [exerciseId, smId]
            );
          }
        }
      }

      // 3. Implementos
      if (dto.equipmentIds && dto.equipmentIds.length > 0) {
        for (const eqId of dto.equipmentIds) {
          await client.query(
            `INSERT INTO exercise_equipment (exercise_id, equipment_id)
             VALUES ($1, $2)
             ON CONFLICT DO NOTHING;`,
            [exerciseId, eqId]
          );
        }
      }

      await recordAuditLog({
        adminId,
        adminEmail,
        action: 'CREATE',
        entityType: 'exercises',
        entityId: exerciseId,
        newValues: dto,
      });

      return createdExercise;
    });
  }

  static async update(id: number, dto: Partial<CreateExerciseDto>, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    return await withTransaction(async (client) => {
      const updateQuery = `
        UPDATE exercises
        SET name = COALESCE($1, name),
            description = COALESCE($2, description),
            execution_instructions = COALESCE($3, execution_instructions),
            difficulty_level = COALESCE($4, difficulty_level),
            exercise_type = COALESCE($5, exercise_type),
            demo_image_url = COALESCE($6, demo_image_url),
            demo_video_url = COALESCE($7, demo_video_url),
            safety_recommendations = COALESCE($8, safety_recommendations),
            publication_status = COALESCE($9, publication_status),
            is_active = COALESCE($10, is_active)
        WHERE id = $11 AND deleted_at IS NULL
        RETURNING *;
      `;
      const values = [
        dto.name ? dto.name.trim() : null,
        dto.description,
        dto.executionInstructions,
        dto.difficultyLevel,
        dto.exerciseType,
        dto.demoImageUrl,
        dto.demoVideoUrl,
        dto.safetyRecommendations,
        dto.publicationStatus,
        dto.isActive,
        id,
      ];

      const { rows } = await client.query(updateQuery, values);
      const updatedExercise = rows[0];

      // Sincronizar grupos musculares si se enviaron
      if (dto.primaryMuscleGroupId !== undefined || dto.secondaryMuscleGroupIds !== undefined) {
        await client.query('DELETE FROM exercise_muscles WHERE exercise_id = $1;', [id]);

        const primaryId = dto.primaryMuscleGroupId || existing.primary_muscle?.id;
        if (primaryId) {
          await client.query(
            `INSERT INTO exercise_muscles (exercise_id, muscle_group_id, is_primary)
             VALUES ($1, $2, TRUE);`,
            [id, primaryId]
          );
        }

        const secondaries = dto.secondaryMuscleGroupIds || (existing.secondary_muscles ? existing.secondary_muscles.map((m: any) => m.id) : []);
        for (const smId of secondaries) {
          if (smId !== primaryId) {
            await client.query(
              `INSERT INTO exercise_muscles (exercise_id, muscle_group_id, is_primary)
               VALUES ($1, $2, FALSE)
               ON CONFLICT DO NOTHING;`,
              [id, smId]
            );
          }
        }
      }

      // Sincronizar implementos si se enviaron
      if (dto.equipmentIds !== undefined) {
        await client.query('DELETE FROM exercise_equipment WHERE exercise_id = $1;', [id]);
        for (const eqId of dto.equipmentIds) {
          await client.query(
            `INSERT INTO exercise_equipment (exercise_id, equipment_id)
             VALUES ($1, $2)
             ON CONFLICT DO NOTHING;`,
            [id, eqId]
          );
        }
      }

      await recordAuditLog({
        adminId,
        adminEmail,
        action: 'UPDATE',
        entityType: 'exercises',
        entityId: id,
        oldValues: existing,
        newValues: dto,
      });

      // Si el video fue cambiado o eliminado, eliminar archivo físico previo de almacenamiento
      if (dto.demoVideoUrl !== undefined && existing.demo_video_url && existing.demo_video_url !== dto.demoVideoUrl) {
        await deletePhysicalVideoFile(existing.demo_video_url);
      }

      return updatedExercise;
    });
  }

  static async toggleActive(id: number, isActive: boolean, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);
    const { rows } = await pool.query(
      'UPDATE exercises SET is_active = $1 WHERE id = $2 AND deleted_at IS NULL RETURNING id, name, is_active;',
      [isActive, id]
    );

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'STATUS_CHANGE',
      entityType: 'exercises',
      entityId: id,
      oldValues: { isActive: existing.is_active },
      newValues: { isActive },
    });

    return rows[0];
  }

  static async delete(id: number, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);

    // Verificar si está en uso en rutinas publicadas
    const checkQuery = `
      SELECT COUNT(*)::int as count
      FROM routine_exercises re
      JOIN workout_routines wr ON re.routine_id = wr.id AND wr.deleted_at IS NULL
      WHERE re.exercise_id = $1;
    `;
    const { rows: depRows } = await pool.query(checkQuery, [id]);

    if (depRows[0].count > 0) {
      throw {
        statusCode: 400,
        message: `El ejercicio "${existing.name}" está vinculado a ${depRows[0].count} rutina(s) activa(s). Desactívalo o archívalo en su lugar para proteger los registros históricos.`,
      };
    }

    // Si tiene un archivo de video almacenado localmente, eliminarlo físicamente para liberar disco
    if (existing.demo_video_url) {
      await deletePhysicalVideoFile(existing.demo_video_url);
    }

    await pool.query('UPDATE exercises SET deleted_at = NOW(), is_active = FALSE WHERE id = $1;', [id]);

    await recordAuditLog({
      adminId,
      adminEmail,
      action: 'DELETE',
      entityType: 'exercises',
      entityId: id,
      oldValues: existing,
      newValues: { deleted_at: new Date() },
    });

    return { message: 'Ejercicio eliminado exitosamente y sus recursos físicos liberados.' };
  }

  static async deleteExerciseVideo(id: number, adminId?: number, adminEmail?: string) {
    const existing = await this.getById(id);
    if (existing.demo_video_url) {
      await deletePhysicalVideoFile(existing.demo_video_url);
      await pool.query('UPDATE exercises SET demo_video_url = NULL WHERE id = $1', [id]);
      await recordAuditLog({
        adminId,
        adminEmail,
        action: 'UPDATE',
        entityType: 'exercises',
        entityId: id,
        oldValues: { demo_video_url: existing.demo_video_url },
        newValues: { demo_video_url: null },
      });
    }
    return { message: 'Video eliminado del ejercicio y liberado de la ruta de almacenamiento.' };
  }

  static async deleteVideoFile(videoUrl: string) {
    const deleted = await deletePhysicalVideoFile(videoUrl);
    await pool.query('UPDATE exercises SET demo_video_url = NULL WHERE demo_video_url = $1', [videoUrl]);
    return { deleted, message: 'Archivo de video eliminado permanentemente del servidor.' };
  }
}

export const deletePhysicalVideoFile = async (fileUrlOrPath?: string | null): Promise<boolean> => {
  if (!fileUrlOrPath) return false;
  try {
    const match = fileUrlOrPath.match(/\/uploads\/videos\/([a-zA-Z0-9_\-\.]+)/);
    if (!match) return false;
    const filename = match[1];
    const safeFilename = path.basename(filename);
    const fullPath = path.resolve(__dirname, '../../../uploads/videos', safeFilename);
    if (fs.existsSync(fullPath)) {
      fs.unlinkSync(fullPath);
      await pool.query('DELETE FROM media_assets WHERE file_path LIKE $1', [`%${safeFilename}`]);
      return true;
    }
  } catch (error) {
    console.error('Error al eliminar archivo de video físico:', error);
  }
  return false;
};
