import request from 'supertest';
import { createApp } from '../app';
import { pool } from '../database/pool';

const app = createApp();

describe('Kynectia Admin API Integration Tests', () => {
  let authToken: string;

  afterAll(async () => {
    await pool.end();
  });

  describe('1. Health and Public Routes', () => {
    it('GET /api/health should return healthy status', async () => {
      const res = await request(app).get('/api/health');
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.status).toBe('healthy');
    });

    it('GET /api/v1/unknown should return 404', async () => {
      const res = await request(app).get('/api/v1/unknown-endpoint');
      expect(res.status).toBe(404);
      expect(res.body.success).toBe(false);
    });
  });

  describe('2. Authentication Flow', () => {
    it('POST /api/v1/auth/login with wrong credentials should return 401', async () => {
      const res = await request(app)
        .post('/api/v1/auth/login')
        .send({ email: 'admin@kynectia.com', password: 'WrongPassword123!' });
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
    });

    it('POST /api/v1/auth/login with valid superadmin credentials should succeed and return JWT', async () => {
      const res = await request(app)
        .post('/api/v1/auth/login')
        .send({ email: 'admin@kynectia.com', password: 'AdminKynectia2026!' });
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.token).toBeDefined();
      expect(res.body.data.admin.email).toBe('admin@kynectia.com');
      authToken = res.body.data.token;
    });

    it('GET /api/v1/auth/me should return current admin profile', async () => {
      const res = await request(app)
        .get('/api/v1/auth/me')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.email).toBe('admin@kynectia.com');
      expect(res.body.data.roleName).toBe('superadmin');
    });

    it('GET /api/v1/auth/me without token should return 401', async () => {
      const res = await request(app).get('/api/v1/auth/me');
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
    });
  });

  describe('3. Dashboard Stats from PostgreSQL', () => {
    it('GET /api/v1/dashboard/stats should return real metrics and aggregate charts', async () => {
      const res = await request(app)
        .get('/api/v1/dashboard/stats')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);

      const overview = res.body.data.overview;
      expect(overview.totalUsers).toBeGreaterThanOrEqual(1);
      expect(overview.totalExercises).toBeGreaterThanOrEqual(1);
      expect(overview.totalMuscleGroups).toBeGreaterThanOrEqual(1);
      expect(overview.totalEquipment).toBeGreaterThanOrEqual(1);
      expect(overview.totalRoutines).toBeGreaterThanOrEqual(1);
      expect(overview.totalPlans).toBeGreaterThanOrEqual(1);
      expect(overview.activeAds).toBeGreaterThanOrEqual(1);

      expect(res.body.data.charts).toBeDefined();
    });
  });

  describe('4. Administrative Modules CRUD Operations', () => {
    it('GET /api/v1/users should return paginated users list', async () => {
      const res = await request(app)
        .get('/api/v1/users')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(Array.isArray(res.body.data)).toBe(true);
      expect(res.body.pagination).toBeDefined();
    });

    it('GET /api/v1/muscles should return muscle groups with exercise count', async () => {
      const res = await request(app)
        .get('/api/v1/muscles')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
    });

    it('GET /api/v1/equipment should return sports equipment', async () => {
      const res = await request(app)
        .get('/api/v1/equipment')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
    });

    it('GET /api/v1/exercises should return catalog with primary and secondary muscles', async () => {
      const res = await request(app)
        .get('/api/v1/exercises')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
      expect(res.body.data[0].primary_muscle_name).toBeDefined();
    });

    it('GET /api/v1/workouts should return predefined routines', async () => {
      const res = await request(app)
        .get('/api/v1/workouts')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
    });

    it('GET /api/v1/plans should return commercial plans', async () => {
      const res = await request(app)
        .get('/api/v1/plans')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
    });

    it('GET /api/v1/ads should return active advertisements', async () => {
      const res = await request(app)
        .get('/api/v1/ads')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
    });

    it('GET /api/v1/audit should return administrative audit logs', async () => {
      const res = await request(app)
        .get('/api/v1/audit')
        .set('Authorization', `Bearer ${authToken}`);
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
    });
  });
});
