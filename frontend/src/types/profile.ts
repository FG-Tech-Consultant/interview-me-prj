export interface Profile {
  id: number;
  tenantId: number;
  userId: number;
  fullName: string;
  headline: string | null;
  summary: string | null;
  location: string | null;
  languages: string[];
  professionalLinks: Record<string, string>;
  careerPreferences: Record<string, unknown>;
  defaultVisibility: string;
  slug: string | null;
  slugChangeCount: number;
  viewCount: number;
  version: number;
  createdAt: string;
  updatedAt: string;
  jobs: JobExperience[];
  education: Education[];
}

export interface JobExperience {
  id: number;
  tenantId: number;
  profileId: number;
  company: string;
  role: string;
  startDate: string;
  endDate: string | null;
  isCurrent: boolean;
  location: string | null;
  employmentType: string | null;
  responsibilities: string | null;
  achievements: string | null;
  metrics: Record<string, unknown>;
  visibility: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface Education {
  id: number;
  tenantId: number;
  profileId: number;
  degree: string;
  institution: string;
  startDate: string | null;
  endDate: string;
  fieldOfStudy: string | null;
  gpa: string | null;
  notes: string | null;
  visibility: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProfileRequest {
  fullName: string;
  headline?: string;
  summary?: string;
  location?: string;
  languages?: string[];
  professionalLinks?: Record<string, string>;
  careerPreferences?: Record<string, unknown>;
  defaultVisibility?: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  headline?: string;
  summary?: string;
  location?: string;
  languages?: string[];
  professionalLinks?: Record<string, string>;
  careerPreferences?: Record<string, unknown>;
  defaultVisibility?: string;
  version: number;
}

export interface CreateJobExperienceRequest {
  company: string;
  role: string;
  startDate: string;
  endDate?: string;
  isCurrent?: boolean;
  location?: string;
  employmentType?: string;
  responsibilities?: string;
  achievements?: string;
  metrics?: Record<string, unknown>;
  visibility?: string;
}

export interface UpdateJobExperienceRequest {
  company: string;
  role: string;
  startDate: string;
  endDate?: string;
  isCurrent?: boolean;
  location?: string;
  employmentType?: string;
  responsibilities?: string;
  achievements?: string;
  metrics?: Record<string, unknown>;
  visibility?: string;
  version: number;
}

export interface CreateEducationRequest {
  degree: string;
  institution: string;
  startDate?: string;
  endDate: string;
  fieldOfStudy?: string;
  gpa?: string;
  notes?: string;
  visibility?: string;
}

export interface UpdateEducationRequest {
  degree: string;
  institution: string;
  startDate?: string;
  endDate: string;
  fieldOfStudy?: string;
  gpa?: string;
  notes?: string;
  visibility?: string;
  version: number;
}
