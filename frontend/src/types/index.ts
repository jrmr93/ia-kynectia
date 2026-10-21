export interface AdminUser {
  id: number;
  fullName: string;
  email: string;
  roleId: number;
  roleName: string;
  permissions: string[];
}

export interface User {
  id: number;
  full_name: string;
  email: string;
  phone?: string;
  avatar_url?: string;
  birth_date?: string;
  gender: string;
  height_cm?: number;
  weight_kg?: number;
  fitness_goal?: string;
  is_active: boolean;
  email_verified: boolean;
  active_plan_name?: string;
  created_at: string;
  updated_at: string;
  subscriptions?: any[];
}

export interface MuscleGroup {
  id: number;
  name: string;
  description?: string;
  image_url?: string;
  is_active: boolean;
  exercises_count?: number;
  created_at: string;
  updated_at: string;
  related_exercises?: any[];
}

export interface Equipment {
  id: number;
  name: string;
  description?: string;
  category: 'free_weights' | 'machines' | 'bodyweight' | 'accessories' | 'resistance';
  image_url?: string;
  is_active: boolean;
  exercises_count?: number;
  created_at: string;
  updated_at: string;
  related_exercises?: any[];
}

export interface Exercise {
  id: number;
  name: string;
  description?: string;
  execution_instructions?: string;
  difficulty_level: 'beginner' | 'intermediate' | 'advanced';
  exercise_type: 'strength' | 'cardio' | 'flexibility' | 'hiit' | 'plyometric';
  demo_image_url?: string;
  demo_video_url?: string;
  safety_recommendations?: string;
  publication_status: 'draft' | 'published' | 'archived';
  is_active: boolean;
  primary_muscle_name?: string;
  primary_muscle_id?: number;
  secondary_muscles?: { id: number; name: string }[];
  equipment?: { id: number; name: string; category: string }[];
  routines_count?: number;
  created_at: string;
  updated_at: string;
}

export interface RoutineExercise {
  id?: number;
  exercise_id: number;
  exercise_name?: string;
  sequence_order: number;
  sets: number;
  reps: number;
  duration_seconds: number;
  rest_seconds: number;
  recommended_weight_kg?: number;
  specific_instructions?: string;
}

export interface WorkoutRoutine {
  id: number;
  name: string;
  description?: string;
  cover_image_url?: string;
  goal: string;
  difficulty_level: 'beginner' | 'intermediate' | 'advanced';
  category: string;
  estimated_duration_minutes: number;
  publication_status: 'draft' | 'published' | 'archived';
  version: number;
  is_active: boolean;
  exercises_count?: number;
  exercises?: RoutineExercise[];
  created_at: string;
  updated_at: string;
}

export interface Plan {
  id: number;
  name: string;
  description?: string;
  billing_cycle: 'monthly' | 'quarterly' | 'biannual' | 'annual' | 'free';
  price: number | string;
  currency: string;
  benefits: string[];
  is_active: boolean;
  discount_percent: number | string;
  promo_end_date?: string;
  active_subscriptions_count?: number;
  created_at: string;
  updated_at: string;
}

export interface Ad {
  id: number;
  title: string;
  description?: string;
  media_url: string;
  destination_url?: string;
  placement: 'banner_home' | 'interstitial_workout' | 'sidebar' | 'footer';
  priority: number;
  starts_at: string;
  ends_at: string;
  is_active: boolean;
  impressions_count: number;
  clicks_count: number;
  created_at: string;
  updated_at: string;
}

export interface AuditLog {
  id: number;
  admin_id: number;
  admin_email: string;
  action: string;
  entity_type: string;
  entity_id: string;
  old_values?: any;
  new_values?: any;
  ip_address?: string;
  user_agent?: string;
  created_at: string;
}

export interface DashboardStats {
  overview: {
    totalUsers: number;
    activeUsers: number;
    totalExercises: number;
    publishedExercises: number;
    totalMuscleGroups: number;
    totalEquipment: number;
    totalRoutines: number;
    publishedRoutines: number;
    totalPlans: number;
    activePlans: number;
    activeSubscriptions: number;
    activeAds: number;
  };
  charts: {
    exercisesByDifficulty: { difficulty_level: string; count: number }[];
    exercisesByType: { exercise_type: string; count: number }[];
    subscriptionsByPlan: { plan_name: string; billing_cycle: string; count: number }[];
  };
  recentActivity: AuditLog[];
}
