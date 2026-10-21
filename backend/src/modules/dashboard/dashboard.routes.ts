import { Router } from 'express';
import { DashboardController } from './dashboard.controller';
import { requireAuth } from '../../middleware/auth';

const router = Router();

router.get('/stats', requireAuth, DashboardController.getStats);

export default router;
