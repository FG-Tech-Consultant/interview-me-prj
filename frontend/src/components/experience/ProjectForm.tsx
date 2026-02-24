import React, { useState } from 'react';
import { useCreateProject, useUpdateProject } from '../../hooks/useProjects';
import { MetricsEditor } from './MetricsEditor';
import type { ProjectResponse, CreateProjectRequest, UpdateProjectRequest } from '../../types/experienceProject';

interface ProjectFormProps {
  jobExperienceId: number;
  project?: ProjectResponse;
  onSuccess: () => void;
  onCancel: () => void;
}

export const ProjectForm: React.FC<ProjectFormProps> = ({
  jobExperienceId,
  project,
  onSuccess,
  onCancel,
}) => {
  const isEdit = !!project;
  const [title, setTitle] = useState(project?.title || '');
  const [context, setContext] = useState(project?.context || '');
  const [role, setRole] = useState(project?.role || '');
  const [teamSize, setTeamSize] = useState<string>(project?.teamSize?.toString() || '');
  const [techStackInput, setTechStackInput] = useState(project?.techStack?.join(', ') || '');
  const [architectureType, setArchitectureType] = useState(project?.architectureType || '');
  const [metrics, setMetrics] = useState<Record<string, unknown>>(
    (project?.metrics as Record<string, unknown>) || {}
  );
  const [outcomes, setOutcomes] = useState(project?.outcomes || '');
  const [visibility, setVisibility] = useState(project?.visibility || 'private');

  const createMutation = useCreateProject();
  const updateMutation = useUpdateProject();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const techStack = techStackInput
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    if (isEdit && project) {
      const data: UpdateProjectRequest = {
        title,
        context: context || undefined,
        role: role || undefined,
        teamSize: teamSize ? parseInt(teamSize, 10) : undefined,
        techStack: techStack.length > 0 ? techStack : undefined,
        architectureType: architectureType || undefined,
        metrics: Object.keys(metrics).length > 0 ? metrics : undefined,
        outcomes: outcomes || undefined,
        visibility,
        version: project.version,
      };
      updateMutation.mutate(
        { projectId: project.id, jobId: jobExperienceId, data },
        { onSuccess }
      );
    } else {
      const data: CreateProjectRequest = {
        title,
        context: context || undefined,
        role: role || undefined,
        teamSize: teamSize ? parseInt(teamSize, 10) : undefined,
        techStack: techStack.length > 0 ? techStack : undefined,
        architectureType: architectureType || undefined,
        metrics: Object.keys(metrics).length > 0 ? metrics : undefined,
        outcomes: outcomes || undefined,
        visibility,
      };
      createMutation.mutate({ jobId: jobExperienceId, data }, { onSuccess });
    }
  };

  const isLoading = createMutation.isPending || updateMutation.isPending;

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700">Title *</label>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          maxLength={255}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">Context</label>
        <textarea
          value={context}
          onChange={(e) => setContext(e.target.value)}
          maxLength={5000}
          rows={3}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
          placeholder="Project background and business context..."
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700">Your Role</label>
          <input
            type="text"
            value={role}
            onChange={(e) => setRole(e.target.value)}
            maxLength={255}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">Team Size</label>
          <input
            type="number"
            value={teamSize}
            onChange={(e) => setTeamSize(e.target.value)}
            min={1}
            max={1000}
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">Tech Stack (comma-separated)</label>
        <input
          type="text"
          value={techStackInput}
          onChange={(e) => setTechStackInput(e.target.value)}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
          placeholder="React, Spring Boot, PostgreSQL..."
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">Architecture Type</label>
        <input
          type="text"
          value={architectureType}
          onChange={(e) => setArchitectureType(e.target.value)}
          maxLength={100}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
          placeholder="Microservices, Monolith, Serverless..."
        />
      </div>

      <MetricsEditor value={metrics} onChange={setMetrics} />

      <div>
        <label className="block text-sm font-medium text-gray-700">Outcomes</label>
        <textarea
          value={outcomes}
          onChange={(e) => setOutcomes(e.target.value)}
          maxLength={5000}
          rows={3}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
          placeholder="Key results and impact..."
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">Visibility</label>
        <select
          value={visibility}
          onChange={(e) => setVisibility(e.target.value)}
          className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
        >
          <option value="private">Private</option>
          <option value="public">Public</option>
        </select>
      </div>

      <div className="flex space-x-3">
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
        >
          {isLoading ? 'Saving...' : isEdit ? 'Update Project' : 'Create Project'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
        >
          Cancel
        </button>
      </div>
    </form>
  );
};
