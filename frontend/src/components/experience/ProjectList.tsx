import React, { useState } from 'react';
import { useProjects, useDeleteProject } from '../../hooks/useProjects';
import { ProjectForm } from './ProjectForm';
import { StoryList } from './StoryList';
import type { ProjectResponse } from '../../types/experienceProject';

interface ProjectListProps {
  jobExperienceId: number;
}

export const ProjectList: React.FC<ProjectListProps> = ({ jobExperienceId }) => {
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const { data: projects, isLoading, error } = useProjects(jobExperienceId);
  const deleteMutation = useDeleteProject();

  const handleDelete = (projectId: number) => {
    if (window.confirm('Are you sure? This will also delete all stories under this project.')) {
      deleteMutation.mutate({ projectId, jobId: jobExperienceId });
    }
  };

  if (isLoading) return <div className="text-sm text-gray-500">Loading projects...</div>;
  if (error) return <div className="text-sm text-red-600">Error loading projects.</div>;

  return (
    <div className="space-y-3 mt-3">
      {!isAdding && !editingId && (
        <button
          onClick={() => setIsAdding(true)}
          className="px-3 py-1 text-sm bg-green-600 text-white rounded hover:bg-green-700"
        >
          + Add Project
        </button>
      )}

      {isAdding && (
        <div className="border border-gray-300 rounded-lg p-4 bg-gray-50">
          <h4 className="text-md font-semibold mb-3">Add New Project</h4>
          <ProjectForm
            jobExperienceId={jobExperienceId}
            onSuccess={() => setIsAdding(false)}
            onCancel={() => setIsAdding(false)}
          />
        </div>
      )}

      {projects && projects.length > 0 ? (
        projects.map((project) => (
          <div key={project.id} className="border border-gray-200 rounded-lg p-4">
            {editingId === project.id ? (
              <ProjectForm
                jobExperienceId={jobExperienceId}
                project={project}
                onSuccess={() => setEditingId(null)}
                onCancel={() => setEditingId(null)}
              />
            ) : (
              <ProjectCard
                project={project}
                isExpanded={expandedId === project.id}
                onToggleExpand={() =>
                  setExpandedId(expandedId === project.id ? null : project.id)
                }
                onEdit={() => {
                  setEditingId(project.id);
                  setIsAdding(false);
                }}
                onDelete={() => handleDelete(project.id)}
              />
            )}
          </div>
        ))
      ) : (
        !isAdding && (
          <p className="text-sm text-gray-500">
            No projects yet. Add your first project to document your experience.
          </p>
        )
      )}
    </div>
  );
};

interface ProjectCardProps {
  project: ProjectResponse;
  isExpanded: boolean;
  onToggleExpand: () => void;
  onEdit: () => void;
  onDelete: () => void;
}

const ProjectCard: React.FC<ProjectCardProps> = ({
  project,
  isExpanded,
  onToggleExpand,
  onEdit,
  onDelete,
}) => {
  return (
    <div>
      <div className="flex justify-between items-start">
        <div className="flex-1 cursor-pointer" onClick={onToggleExpand}>
          <h4 className="text-lg font-semibold">{project.title}</h4>
          <div className="flex flex-wrap gap-2 mt-1">
            {project.role && (
              <span className="text-sm text-gray-600">{project.role}</span>
            )}
            {project.teamSize && (
              <span className="text-sm text-gray-500">Team: {project.teamSize}</span>
            )}
            {project.architectureType && (
              <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                {project.architectureType}
              </span>
            )}
            <span className={`text-xs px-2 py-0.5 rounded ${
              project.visibility === 'public'
                ? 'bg-green-100 text-green-700'
                : 'bg-gray-100 text-gray-600'
            }`}>
              {project.visibility}
            </span>
            <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">
              {project.storyCount} {project.storyCount === 1 ? 'story' : 'stories'}
            </span>
          </div>
          {project.techStack && project.techStack.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-2">
              {project.techStack.map((tech) => (
                <span
                  key={tech}
                  className="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded"
                >
                  {tech}
                </span>
              ))}
            </div>
          )}
        </div>
        <div className="flex space-x-2 ml-3">
          <button
            onClick={onEdit}
            className="px-2 py-1 text-xs bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
          >
            Edit
          </button>
          <button
            onClick={onDelete}
            className="px-2 py-1 text-xs bg-red-600 text-white rounded hover:bg-red-700"
          >
            Delete
          </button>
        </div>
      </div>

      {isExpanded && (
        <div className="mt-3 pt-3 border-t border-gray-200">
          {project.context && (
            <div className="mb-2">
              <span className="text-sm font-medium text-gray-700">Context: </span>
              <span className="text-sm text-gray-600">{project.context}</span>
            </div>
          )}
          {project.outcomes && (
            <div className="mb-2">
              <span className="text-sm font-medium text-gray-700">Outcomes: </span>
              <span className="text-sm text-gray-600">{project.outcomes}</span>
            </div>
          )}
          {project.metrics && Object.keys(project.metrics).length > 0 && (
            <div className="mb-3">
              <span className="text-sm font-medium text-gray-700">Metrics: </span>
              <div className="flex flex-wrap gap-2 mt-1">
                {Object.entries(project.metrics).map(([key, val]) => (
                  <span key={key} className="text-xs bg-purple-50 text-purple-700 px-2 py-0.5 rounded">
                    {key}: {String(val)}
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="mt-3">
            <h5 className="text-sm font-semibold text-gray-700 mb-2">
              Stories ({project.storyCount})
            </h5>
            <StoryList experienceProjectId={project.id} />
          </div>
        </div>
      )}
    </div>
  );
};
