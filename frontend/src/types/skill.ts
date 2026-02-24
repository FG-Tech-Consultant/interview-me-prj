export interface SkillDto {
  id: number;
  name: string;
  category: string;
  description: string | null;
  tags: string[] | null;
  isActive: boolean;
}

export interface UserSkillDto {
  id: number;
  skill: SkillDto;
  yearsOfExperience: number;
  proficiencyDepth: number;
  lastUsedDate: string | null;
  confidenceLevel: string;
  tags: string[] | null;
  visibility: string;
  createdAt: string;
  updatedAt: string;
}

export interface AddUserSkillRequest {
  skillId: number;
  yearsOfExperience?: number;
  proficiencyDepth: number;
  lastUsedDate?: string;
  confidenceLevel?: string;
  tags?: string[];
  visibility?: string;
}

export interface UpdateUserSkillRequest {
  yearsOfExperience?: number;
  proficiencyDepth?: number;
  lastUsedDate?: string;
  confidenceLevel?: string;
  tags?: string[];
  visibility?: string;
}

export interface CreateSkillRequest {
  name: string;
  category: string;
  description?: string;
  tags?: string[];
}

export interface UpdateSkillRequest {
  name?: string;
  category?: string;
  description?: string;
  tags?: string[];
}

export type UserSkillsGrouped = Record<string, UserSkillDto[]>;

export const PROFICIENCY_LABELS: Record<number, string> = {
  1: 'Beginner',
  2: 'Intermediate',
  3: 'Proficient',
  4: 'Advanced',
  5: 'Expert',
};

export const SKILL_CATEGORIES = [
  'Languages',
  'Frameworks',
  'Cloud',
  'Databases',
  'Messaging',
  'Observability',
  'Methodologies',
  'Domains',
] as const;
