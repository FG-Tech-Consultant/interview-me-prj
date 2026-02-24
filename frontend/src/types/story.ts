export interface StoryResponse {
  id: number;
  experienceProjectId: number;
  title: string;
  situation: string;
  task: string;
  action: string;
  result: string;
  metrics: Record<string, unknown> | null;
  visibility: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateStoryRequest {
  title: string;
  situation: string;
  task: string;
  action: string;
  result: string;
  metrics?: Record<string, unknown>;
  visibility?: string;
}

export interface UpdateStoryRequest {
  title: string;
  situation: string;
  task: string;
  action: string;
  result: string;
  metrics?: Record<string, unknown>;
  visibility?: string;
  version: number;
}
