import { Router } from 'express';
import { AuditController } from './audit.controller';
import { requireAuth, requirePermission } from '../../middleware/auth';

const router = Router();

router.use(requireAuth);
router.get('/', requirePermission('audit:view'), AuditController.list);

export default router;
