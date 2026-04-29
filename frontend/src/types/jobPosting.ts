export interface JobPosting {
  id: number;
  title: string;
  slug: string;
  description: string;
  requirements: string | null;
  benefits: string | null;
  location: string | null;
  workModel: string | null;
  salaryRange: string | null;
  experienceLevel: string | null;
  status: string;
  requiredSkills: string[];
  niceToHaveSkills: string[];
  createdAt: string;
}

export interface CreateJobPostingRequest {
  title: string;
  description: string;
  requirements?: string;
  benefits?: string;
  location?: string;
  workModel?: string;
  salaryRange?: string;
  experienceLevel?: string;
  requiredSkills?: string[];
  niceToHaveSkills?: string[];
}

export interface UpdateJobPostingRequest {
  title?: string;
  description?: string;
  requirements?: string;
  benefits?: string;
  location?: string;
  workModel?: string;
  salaryRange?: string;
  experienceLevel?: string;
  status?: string;
  requiredSkills?: string[];
  niceToHaveSkills?: string[];
}

export interface AiJobDescriptionRequest {
  title: string;
  experienceLevel?: string;
  workModel?: string;
  requiredSkills?: string[];
  additionalContext?: string;
}

export interface AiJobDescriptionResponse {
  description: string;
  requirements: string;
  benefits: string;
  suggestedSkills: string[];
  suggestedNiceToHave: string[];
}
