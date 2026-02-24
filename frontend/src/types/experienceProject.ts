export interface ProjectResponse {
  id: number;
  jobExperienceId: number;
  title: string;
  context: string | null;
  role: string | null;
  teamSize: number | null;
  techStack: string[] | null;
  architectureType: string | null;
  metrics: Record<string, unknown> | null;
  outcomes: string | null;
  visibility: string;
  storyCount: number;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateProjectRequest {
  title: string;
  context?: string;
  role?: string;
  teamSize?: number;
  techStack?: string[];
  architectureType?: string;
  metrics?: Record<string, unknown>;
  outcomes?: string;
  visibility?: string;
}

export interface UpdateProjectRequest {
  title: string;
  context?: string;
  role?: string;
  teamSize?: number;
  techStack?: string[];
  architectureType?: string;
  metrics?: Record<string, unknown>;
  outcomes?: string;
  visibility?: string;
  version: number;
}
