export interface PublicProfileResponse {
  slug: string;
  fullName: string;
  headline: string;
  summary: string | null;
  location: string | null;
  languages: string[] | null;
  professionalLinks: Record<string, string> | null;
  skills: PublicSkillResponse[];
  jobs: PublicJobResponse[];
  education: PublicEducationResponse[];
  seo: SeoMetadata;
}

export interface PublicSkillResponse {
  skillName: string;
  category: string;
  proficiencyDepth: number;
  yearsOfExperience: number;
  lastUsedDate: string | null;
}

export interface PublicJobResponse {
  company: string;
  role: string;
  startDate: string;
  endDate: string | null;
  isCurrent: boolean;
  location: string | null;
  employmentType: string | null;
  responsibilities: string | null;
  achievements: string | null;
  metrics: Record<string, unknown> | null;
  projects: PublicProjectResponse[];
}

export interface PublicEducationResponse {
  degree: string;
  institution: string;
  startDate: string | null;
  endDate: string;
  fieldOfStudy: string | null;
}

export interface PublicProjectResponse {
  title: string;
  context: string | null;
  role: string | null;
  teamSize: number | null;
  techStack: string[] | null;
  architectureType: string | null;
  metrics: Record<string, unknown> | null;
  outcomes: string | null;
  linkedSkills: string[];
  stories: PublicStoryResponse[];
}

export interface PublicStoryResponse {
  title: string;
  situation: string;
  task: string;
  action: string;
  result: string;
  metrics: Record<string, unknown> | null;
  linkedSkills: string[];
}

export interface SeoMetadata {
  title: string;
  description: string;
  canonicalUrl: string;
  keywords: string[];
}

export interface SlugCheckResponse {
  slug: string;
  available: boolean;
  suggestions: string[];
  changeCost: number;
}
